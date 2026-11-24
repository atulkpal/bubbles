"""Small Google Play Developer API helper for the Hushpop relaunch.

Usage (run from repo root; uses play-account.json, which is git-ignored):
  python -I scripts/play_api.py check
  python -I scripts/play_api.py listing-get
  ... see main() for commands.

Every write happens inside an "edit" that is only committed at the end, so a failed step changes nothing live.
"""
import json
import os
import sys

from google.oauth2 import service_account
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

PKG = "com.ashwathai.bubbles"
ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
CREDS = os.path.join(ROOT, "play-account.json")


def service():
    c = service_account.Credentials.from_service_account_file(
        CREDS, scopes=["https://www.googleapis.com/auth/androidpublisher"])
    return build("androidpublisher", "v3", credentials=c, cache_discovery=False)


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    cmd = sys.argv[1]
    s = service()
    if cmd == "check":
        e = s.edits().insert(packageName=PKG, body={}).execute()
        eid = e["id"]
        det = s.edits().details().get(packageName=PKG, editId=eid).execute()
        print("defaultLanguage:", det.get("defaultLanguage"), "| contact:", "set" if det.get("contactEmail") else "none")
        listings = s.edits().listings().list(packageName=PKG, editId=eid).execute().get("listings", [])
        for l in listings:
            print("listing", l["language"], "|", l.get("title"), "|", l.get("shortDescription"))
        tracks = s.edits().tracks().list(packageName=PKG, editId=eid).execute().get("tracks", [])
        for t in tracks:
            for r in t.get("releases", []):
                print("track", t["track"], "|", r.get("status"), "| codes", r.get("versionCodes"), "|", r.get("name"))
        s.edits().delete(packageName=PKG, editId=eid).execute()
    elif cmd == "products":
        try:
            r = s.inappproducts().list(packageName=PKG).execute()
            print(json.dumps(r, indent=1)[:3000])
        except Exception as ex:  # noqa
            print("inappproducts:", ex)
    else:
        raise SystemExit("unknown command")


if __name__ == "__main__":
    main()
