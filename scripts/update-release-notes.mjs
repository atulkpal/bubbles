import { readFile } from "node:fs/promises";
import crypto from "node:crypto";

const packageName = process.argv[2] || "com.ashwathai.bubbles";
const credentialsPath = process.argv[3] || "play-account.json";

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

console.log(`Adding release notes to internal testing track for ${packageName}...`);

const accessToken = await getAccessToken();
const encodedPackage = encodeURIComponent(packageName);
const baseUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodedPackage}`;

// 1. Create edit
console.log("Creating Play edit...");
const edit = await playApi("POST", `${baseUrl}/edits`, accessToken);
const editId = edit.id;
console.log(`Edit ID: ${editId}`);

// 2. Get current internal track
console.log("Getting internal track...");
const track = await playApi("GET", `${baseUrl}/edits/${editId}/tracks/internal`, accessToken);

if (!track.releases || track.releases.length === 0) {
  console.error("No releases found on internal track");
  process.exit(1);
}

const release = track.releases[0];
console.log(`Current release: ${release.name}, version codes: ${release.versionCodes?.join(", ")}`);

// 3. Update release with notes
console.log("Adding release notes...");
const updatedTrack = await playApi(
  "PUT",
  `${baseUrl}/edits/${editId}/tracks/internal`,
  accessToken,
  {
    releases: [
      {
        versionCodes: release.versionCodes,
        status: "completed",
        name: "Bubbles v1.0 - Initial Release",
        releaseNotes: [
          {
            language: "en-US",
            text: "Initial release of Bubbles!\n\n• Adventure mode with increasing challenge\n• Zen mode for relaxed play\n• Daily Challenge scoring\n• Power-ups: slow motion, freeze, multi-pop, prism boosts\n• Coins, upgrades, skins, themes, and prestige progression\n• Sound, haptics, and reduced-motion settings",
          },
        ],
      },
    ],
  },
);
console.log(`Track updated with release notes.`);

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

console.log("\nDone! Release notes added to internal testing track.");
