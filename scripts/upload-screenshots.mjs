import { readFile } from "node:fs/promises";
import crypto from "node:crypto";
import path from "node:path";

const packageName = process.argv[2] || "com.ashwathai.bubbles";
const language = process.argv[3] || "en-US";
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

// Screenshots to upload (phoneScreenshots type)
const screenshots = [
  "play-listing-assets/phone-01-start-play.png",
  "play-listing-assets/phone-02-gameplay-play.png",
];

// --- Main ---

console.log(`Uploading screenshots for ${packageName} (${language})...`);

const accessToken = await getAccessToken();
const encodedPackage = encodeURIComponent(packageName);
const encodedLanguage = encodeURIComponent(language);
const baseUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodedPackage}`;

// 1. Create edit
console.log("Creating Play edit...");
const edit = await playApi("POST", `${baseUrl}/edits`, accessToken);
const editId = edit.id;
console.log(`Edit ID: ${editId}`);

// 2. Delete existing phone screenshots first
console.log("Deleting existing phone screenshots...");
try {
  const existingImages = await playApi(
    "GET",
    `${baseUrl}/edits/${editId}/listings/${encodedLanguage}/phoneScreenshots`,
    accessToken,
  );
  if (existingImages.images && existingImages.images.length > 0) {
    for (const img of existingImages.images) {
      console.log(`  Deleting screenshot: ${img.id}`);
      await playApi(
        "DELETE",
        `${baseUrl}/edits/${editId}/listings/${encodedLanguage}/phoneScreenshots/${img.id}`,
        accessToken,
      );
    }
  }
} catch (e) {
  console.log("  No existing screenshots to delete.");
}

// 3. Upload each screenshot
for (let i = 0; i < screenshots.length; i++) {
  const filePath = screenshots[i];
  const fileBuffer = await readFile(filePath);
  const fileName = path.basename(filePath);

  console.log(`Uploading screenshot ${i + 1}/${screenshots.length}: ${fileName} (${(fileBuffer.length / 1024).toFixed(0)} KB)...`);

  const uploadUrl = `https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/${encodedPackage}/edits/${editId}/listings/${encodedLanguage}/phoneScreenshots?uploadType=media`;

  const response = await fetch(uploadUrl, {
    method: "POST",
    headers: {
      authorization: `Bearer ${accessToken}`,
      "content-type": "image/png",
    },
    body: fileBuffer,
  });

  const text = await response.text();
  const data = text ? JSON.parse(text) : {};

  if (!response.ok) {
    console.error(`  Failed: ${data?.error?.message || text}`);
  } else {
    console.log(`  Uploaded: ${data.image?.id || "ok"}`);
  }
}

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

console.log("\nDone! Screenshots uploaded.");
