import { readFile, stat } from "node:fs/promises";
import crypto from "node:crypto";
import path from "node:path";

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

async function playApiMultipart(method, url, accessToken, formData) {
  const response = await fetch(url, {
    method,
    headers: {
      authorization: `Bearer ${accessToken}`,
      ...formData.getHeaders(),
    },
    body: formData.getBuffer(),
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

console.log(`Uploading ${aabPath} to internal testing track for ${packageName}...`);

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
console.log(`Upload URL: ${uploadUrl}`);

const uploadResponse = await fetch(uploadUrl, {
  method: "POST",
  headers: {
    authorization: `Bearer ${accessToken}`,
    "content-type": "application/octet-stream",
  },
  body: aabBuffer,
});

const uploadText = await uploadResponse.text();
console.log(`Upload response status: ${uploadResponse.status}`);

let uploadData;
try {
  uploadData = uploadText ? JSON.parse(uploadText) : {};
} catch (e) {
  console.error("Failed to parse upload response as JSON");
  try {
    await playApi("DELETE", `${baseUrl}/edits/${editId}`, accessToken);
  } catch (e) {}
  throw new Error(`AAB upload returned non-JSON response (${uploadResponse.status}). Response: ${uploadText.substring(0, 200)}`);
}

if (!uploadResponse.ok) {
  console.error("Upload failed:", JSON.stringify(uploadData, null, 2));
  try {
    await playApi("DELETE", `${baseUrl}/edits/${editId}`, accessToken);
  } catch (e) {}
  throw new Error(`AAB upload failed ${uploadResponse.status}: ${uploadData?.error?.message || uploadText}`);
}

console.log(`Upload successful. Version code: ${uploadData.versionCode}`);

// 3. Get existing tracks
console.log("Listing existing tracks...");
let existingTracks;
try {
  existingTracks = await playApi("GET", `${baseUrl}/edits/${editId}/tracks`, accessToken);
  console.log(`Found ${existingTracks.tracks?.length || 0} tracks:`, existingTracks.tracks?.map(t => t.track).join(", ") || "none");
} catch (e) {
  console.warn("Could not list tracks:", e.message);
  existingTracks = { tracks: [] };
}

// 4. Update or create internal testing track
const trackId = "internal";
const trackExists = existingTracks.tracks?.some(t => t.track === trackId);

const releaseBody = {
  versionCodes: [String(uploadData.versionCode)],
  status: "completed",
  name: "Internal Testing - v1.0",
};

let updatedTrack;
if (trackExists) {
  console.log(`Track ${trackId} exists, updating...`);
  updatedTrack = await playApi(
    "PUT",
    `${baseUrl}/edits/${editId}/tracks/${trackId}`,
    accessToken,
    {
      releases: [releaseBody],
    },
  );
} else {
  console.log(`Track ${trackId} not found, creating...`);
  updatedTrack = await playApi(
    "PUT",
    `${baseUrl}/edits/${editId}/tracks/${trackId}`,
    accessToken,
    {
      releases: [releaseBody],
    },
  );
}
console.log(`Track updated. Releases: ${updatedTrack.releases?.length || 0}`);

// 5. Validate edit
console.log("Validating edit...");
try {
  await playApi("POST", `${baseUrl}/edits/${editId}:validate`, accessToken);
  console.log("Edit validated successfully.");
} catch (e) {
  console.warn("Validation warning:", e.message);
}

// 6. Commit edit
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

console.log("\nDone! AAB uploaded to internal testing track.");
console.log(`- Package: ${packageName}`);
console.log(`- Version code: ${uploadData.versionCode}`);
console.log(`- Track: internalTesting`);
