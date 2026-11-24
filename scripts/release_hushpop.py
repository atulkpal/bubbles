"""Hushpop relaunch on Google Play in ONE edit (all-or-nothing):
  - upload releases/hushpop-v1.3-code4.aab
  - internal + production tracks -> versionCode 4 (full rollout), release notes EN/HI
  - store listing: title, short + full description (en-US, hi-IN)
  - graphics (en-US): icon, feature graphic, 8 phone screenshots (old ones deleted)
Run: python -I scripts/release_hushpop.py [--dry-run]
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import play_api as p  # noqa: E402
from googleapiclient.http import MediaFileUpload  # noqa: E402

ROOT = p.ROOT
AAB = os.path.join(ROOT, "releases", "hushpop-v1.3-code4.aab")
ASSETS = os.path.join(ROOT, "play-listing-assets", "hushpop")
DRY = "--dry-run" in sys.argv

LISTINGS = {
    "en-US": {
        "title": "Hushpop: Bubble Pop & Relax",
        "shortDescription": "Pop soap bubbles to relax. Soft ASMR pops, breathe mode, offline. No timer.",
        "fullDescription": """Need a quiet minute? Pop real-looking soap bubbles and let your shoulders drop. Hushpop is a calm, satisfying bubble pop game for stress relief, with soft ASMR pop sounds and gentle haptics on every touch.

• Float: no timer, no fail. Soap bubbles rise through a quiet night room and drift away. Pop them whenever you like.
• Hold to blow your own. Hold anywhere and a bubble grows under your finger. Let go and watch it float off.
• Breathe mode. 1, 3 or 5 minutes of guided breathing: breathe in as the bubble grows, out as it rises, then pop it.
• Soft, satisfying sound. Wet pops pitched by bubble size that climb a little melody, over rain on the window.
• Soap Shelf. Every pop fills a jar. A full jar unlocks a new soap with its own colours, room and sound: Lavender Dusk, Monsoon Glass, Chai Steam, Firefly Night, Sea Glass and Marigold.
• Soap of the day. A different soap each day, free for everyone.
• Kind streak. Missed a day? Your count never resets.
• Challenge. 20 timed levels for when you want a goal, never under 30 seconds.
• Offline, no account, about 11 MB.
• Supporter Pack (optional, one-time): removes every ad and adds the Moonlit Pearl soap.

Stress relief, anti stress, relaxing game, satisfying ASMR bubble popping, calm, zen, mindful breathing. Free, works without wifi.""",
    },
    "hi-IN": {
        "title": "Hushpop: बबल पॉप और रिलैक्स",
        "shortDescription": "साबुन के बुलबुले फोड़ें और रिलैक्स करें। ASMR आवाज़ें, ब्रीद मोड, ऑफलाइन।",
        "fullDescription": """थोड़ा सुकून चाहिए? असली जैसे दिखने वाले साबुन के बुलबुले फोड़ें और तनाव को जाने दें। Hushpop एक शांत, संतोष देने वाला बबल पॉप गेम है, जिसमें हर टच पर नरम ASMR आवाज़ और हल्का वाइब्रेशन है।

• फ्लोट: कोई टाइमर नहीं, कोई हार नहीं। बुलबुले धीरे-धीरे ऊपर उठते हैं; जब मन करे, फोड़ें।
• अपना बुलबुला खुद फुलाएँ। स्क्रीन पर उंगली रखे रहें, बुलबुला बड़ा होगा; छोड़ें और उसे उड़ते देखें।
• ब्रीद मोड। 1, 3 या 5 मिनट की सांस: बुलबुला बढ़े तो सांस अंदर, ऊपर उठे तो बाहर, फिर फोड़ें।
• मीठी आवाज़ें। बुलबुले के आकार के हिसाब से पॉप की धुन, साथ में खिड़की पर बारिश।
• सोप शेल्फ। हर पॉप से जार भरता है; भरा जार नया साबुन खोलता है: लैवेंडर डस्क, मॉनसून ग्लास, चाय स्टीम, फायरफ्लाई नाइट, सी ग्लास और गेंदा (मैरीगोल्ड)।
• आज का साबुन: हर दिन एक नया, सबके लिए मुफ़्त।
• एक दिन छूट गया? आपकी गिनती कभी शून्य नहीं होती।
• चैलेंज: 20 टाइम्ड लेवल, कोई भी 30 सेकंड से कम नहीं।
• ऑफलाइन, बिना अकाउंट, लगभग 11 MB।
• सपोर्टर पैक (वैकल्पिक, एक बार): सारे विज्ञापन हटाता है और मूनलिट पर्ल साबुन देता है।""",
    },
}

NOTES = [
    {"language": "en-US", "text": "Bubbles is now Hushpop: real soap bubbles, soft ASMR pops, hold to blow your own, a guided Breathe mode and a Soap Shelf to collect. Calm Float mode has no timer; Challenge keeps 20 levels. No ads in your first sessions. Thank you for playing!"},
    {"language": "hi-IN", "text": "Bubbles अब Hushpop है: असली जैसे साबुन के बुलबुले, नरम ASMR आवाज़ें, अपना बुलबुला खुद फुलाएँ, ब्रीद मोड और सोप शेल्फ। फ्लोट मोड में कोई टाइमर नहीं; चैलेंज में 20 लेवल।"},
]


def main():
    sys.stdout.reconfigure(encoding="utf-8")
    for lang, l in LISTINGS.items():
        assert len(l["title"]) <= 30, (lang, len(l["title"]))
        assert len(l["shortDescription"]) <= 80, (lang, len(l["shortDescription"]))
        assert len(l["fullDescription"]) <= 4000, (lang, len(l["fullDescription"]))
    for n in NOTES:
        assert len(n["text"]) <= 500, n["language"]
    shots = sorted(f for f in os.listdir(ASSETS) if f.startswith("phone-") and f.endswith(".png"))
    assert len(shots) == 8, shots
    assert os.path.exists(AAB)

    s = p.service()
    edits = s.edits()
    eid = edits.insert(packageName=p.PKG, body={}).execute()["id"]
    try:
        b = edits.bundles().upload(packageName=p.PKG, editId=eid,
                                   media_body=MediaFileUpload(AAB, mimetype="application/octet-stream", resumable=True)).execute()
        code = b["versionCode"]
        print("uploaded bundle versionCode", code)
        release = {"name": "Hushpop 1.3", "versionCodes": [str(code)], "status": "completed", "releaseNotes": NOTES}
        for track in ("internal", "production"):
            edits.tracks().update(packageName=p.PKG, editId=eid, track=track,
                                  body={"track": track, "releases": [release]}).execute()
            print("track", track, "-> code", code)
        for lang, l in LISTINGS.items():
            edits.listings().update(packageName=p.PKG, editId=eid, language=lang, body={"language": lang, **l}).execute()
            print("listing", lang, "->", l["title"])
        lang = "en-US"
        edits.images().deleteall(packageName=p.PKG, editId=eid, language=lang, imageType="phoneScreenshots").execute()
        for f in shots:
            edits.images().upload(packageName=p.PKG, editId=eid, language=lang, imageType="phoneScreenshots",
                                  media_body=MediaFileUpload(os.path.join(ASSETS, f), mimetype="image/png")).execute()
        print("screenshots:", len(shots))
        for img_type, f in (("icon", "icon-512.png"), ("featureGraphic", "feature-graphic.png")):
            edits.images().deleteall(packageName=p.PKG, editId=eid, language=lang, imageType=img_type).execute()
            edits.images().upload(packageName=p.PKG, editId=eid, language=lang, imageType=img_type,
                                  media_body=MediaFileUpload(os.path.join(ASSETS, f), mimetype="image/png")).execute()
            print(img_type, "replaced")
        if DRY:
            edits.validate(packageName=p.PKG, editId=eid).execute()
            print("DRY RUN: validated, discarding edit")
            edits.delete(packageName=p.PKG, editId=eid).execute()
        else:
            r = edits.commit(packageName=p.PKG, editId=eid).execute()
            print("COMMITTED edit", r.get("id"))
    except Exception:
        try:
            edits.delete(packageName=p.PKG, editId=eid).execute()
        except Exception:
            pass
        raise


if __name__ == "__main__":
    main()
