import { readFile, stat } from "node:fs/promises";
import crypto from "node:crypto";

const packageName = process.argv[2] || "com.ashwathai.bubbles";
const aabPath = process.argv[3] || "app/build/outputs/bundle/release/app-release.aab";
const credentialsPath = process.argv[4] || "play-account.json";

const credentials = JSON.parse(await readFile(credentialsPath, "utf8"));

function base64url(input) {
  return Buffer.from(input)
    .toString("base64")
    .replace(/=/g, "")
    .replace(/\+/g, "-")
    .replace(/\//g, "_");
}

async function getAccessToken() {
  const now = Math.floor(Date.now() / 1000);
  const header = base64url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = base64url(
    JSON.stringify({
      iss: credentials.client_email,
      scope: "https://www.googleapis.com/auth/androidpublisher",
      aud: "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    }),
  );
  const unsignedJwt = `${header}.${claim}`;
  const signature = crypto.sign("RSA-SHA256", Buffer.from(unsignedJwt), credentials.private_key);
  const jwt = `${unsignedJwt}.${base64url(signature)}`;

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "content-type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  });

  if (!response.ok) {
    throw new Error(`Token request failed ${response.status}: ${await response.text()}`);
  }

  return (await response.json()).access_token;
}

async function playApi(method, url, accessToken, body) {
  const response = await fetch(url, {
    method,
    headers: {
      authorization: `Bearer ${accessToken}`,
      ...(body ? { "content-type": "application/json; charset=utf-8" } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : {};

  if (!response.ok) {
    const message = data?.error?.message || text;
    const error = new Error(`Play API ${method} ${url} failed ${response.status}: ${message}`);
    error.messageText = message;
    throw error;
  }

  return data;
}

// --- Main ---

console.log(`Uploading ${aabPath} to closed testing (alpha) for ${packageName}...`);

const aabInfo = await stat(aabPath);
console.log(`AAB size: ${(aabInfo.size / 1024 / 1024).toFixed(2)} MB`);

const accessToken = await getAccessToken();
console.log("Access token obtained.");

const encodedPackage = encodeURIComponent(packageName);
const baseUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodedPackage}`;

// 1. Create edit
console.log("Creating Play edit...");
const edit = await playApi("POST", `${baseUrl}/edits`, accessToken);
const editId = edit.id;
console.log(`Edit ID: ${editId}`);

// 2. Upload AAB
console.log("Uploading AAB bundle...");
const aabBuffer = await readFile(aabPath);

const uploadUrl = `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${encodedPackage}/edits/${editId}/bundles?uploadType=media`;

const uploadResponse = await fetch(uploadUrl, {
  method: "POST",
  headers: {
    authorization: `Bearer ${accessToken}`,
    "content-type": "application/octet-stream",
  },
  body: aabBuffer,
});

const uploadText = await uploadResponse.text();
let uploadData;
try {
  uploadData = uploadText ? JSON.parse(uploadText) : {};
} catch (e) {
  try { await playApi("DELETE", `${baseUrl}/edits/${editId}`, accessToken); } catch (e) {}
  throw new Error(`AAB upload returned non-JSON (${uploadResponse.status})`);
}

if (!uploadResponse.ok) {
  try { await playApi("DELETE", `${baseUrl}/edits/${editId}`, accessToken); } catch (e) {}
  throw new Error(`AAB upload failed ${uploadResponse.status}: ${uploadData?.error?.message || uploadText}`);
}

console.log(`Upload successful. Version code: ${uploadData.versionCode}`);

// 3. Update alpha (closed testing) track with release notes
console.log("Updating alpha track with release notes...");
const trackBody = {
  releases: [
    {
      versionCodes: [String(uploadData.versionCode)],
      status: "completed",
      name: "Bubbles v1.2",
      releaseNotes: [
        {
          language: "en-US",
          text: "What's New in v1.2:\n\n• Star ratings — finish fast to earn ★★★\n• Achievement moment on every level clear\n• Continue & level select — resume or replay any cleared level\n• 100 levels with bosses, wind & gravity\n• About section in Settings\n\nFixes:\n• Level completion is now reliable\n• Ad pacing corrected for levels 11+",
        },
      ],
    },
  ],
};

const alphaTrack = await playApi(
  "PUT",
  `${baseUrl}/edits/${editId}/tracks/alpha`,
  accessToken,
  trackBody,
);
console.log(`Alpha track updated. Releases: ${alphaTrack.releases?.length || 0}`);

// 4. Validate
console.log("Validating edit...");
try {
  await playApi("POST", `${baseUrl}/edits/${editId}:validate`, accessToken);
  console.log("Edit validated.");
} catch (e) {
  console.warn("Validation warning:", e.message);
}

// 5. Commit
console.log("Committing edit...");
try {
  const commit = await playApi("POST", `${baseUrl}/edits/${editId}:commit`, accessToken);
  console.log(`Committed edit: ${commit.id}`);
} catch (e) {
  if (e.messageText?.includes("changesNotSentForReview must not be set")) {
    const commit = await playApi("POST", `${baseUrl}/edits/${editId}:commit`, accessToken);
    console.log(`Committed edit: ${commit.id}`);
  } else {
    throw e;
  }
}

console.log("\nDone! AAB uploaded to closed testing (alpha).");
console.log(`- Package: ${packageName}`);
console.log(`- Version code: ${uploadData.versionCode}`);
console.log(`- Track: alpha (closed testing)`);
