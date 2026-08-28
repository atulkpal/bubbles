import { readFile } from "node:fs/promises";
import crypto from "node:crypto";

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

const listing = {
  language,
  title: "Bubbles",
  shortDescription: "A relaxing bubble popping game with levels, power-ups, coins, and zen mode",
  fullDescription: `Bubbles is a casual bubble popping game built for quick sessions and calm focus.

Clear bubbles before the timer runs out, chain combos, avoid bombs, and unlock power-ups as each level gets faster. Adventure mode gives you structured stages, Zen mode lets you pop without pressure, and Daily Challenge gives you a fresh timed run to revisit.

Features:
- Fast, simple tap controls
- Adventure levels with increasing challenge
- Zen mode for relaxed play
- Daily Challenge scoring
- Power-ups including slow motion, freeze, multi-pop, and prism boosts
- Coins, upgrades, skins, themes, and prestige progression
- Sound, haptics, and reduced-motion settings

Bubbles is designed for short breaks, focused play, and satisfying bubble pops without complicated menus or long sessions.`,
  video: "",
};

const accessToken = await getAccessToken();
const encodedPackage = encodeURIComponent(packageName);
const encodedLanguage = encodeURIComponent(language);
const baseUrl = `https://androidpublisher.googleapis.com/androidpublisher/v3/applications/${encodedPackage}`;

console.log(`Creating Play edit for ${packageName}...`);
const edit = await playApi("POST", `${baseUrl}/edits`, accessToken);
const editId = edit.id;
console.log(`Edit ID: ${editId}`);

console.log(`Updating ${language} listing text...`);
const updatedListing = await playApi(
  "PUT",
  `${baseUrl}/edits/${editId}/listings/${encodedLanguage}`,
  accessToken,
  listing,
);

console.log("Validating edit...");
await playApi("POST", `${baseUrl}/edits/${editId}:validate`, accessToken);

console.log("Committing edit without sending changes for review...");
let commit;
try {
  commit = await playApi("POST", `${baseUrl}/edits/${editId}:commit?changesNotSentForReview=true`, accessToken);
} catch (error) {
  if (error.messageText?.includes("changesNotSentForReview must not be set")) {
    console.log("Retrying commit without changesNotSentForReview because this app auto-manages review behavior...");
    commit = await playApi("POST", `${baseUrl}/edits/${editId}:commit`, accessToken);
  } else {
    throw error;
  }
}

console.log(`Committed Play edit: ${commit.id}`);
console.log(`Updated title: ${updatedListing.title}`);
console.log(`Updated short description length: ${updatedListing.shortDescription.length}`);
console.log(`Updated full description length: ${updatedListing.fullDescription.length}`);
