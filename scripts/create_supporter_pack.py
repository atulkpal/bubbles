"""Create/update the Supporter Pack one-time product (product id stays "remove_ads", which the app's
BillingManager already sells) with region prices from $2.99 and India pinned to Rs 99, then activate it.

Run: python -I scripts/create_supporter_pack.py
"""
import json
import sys

sys.path.insert(0, __file__.rsplit("\\", 1)[0] if "\\" in __file__ else __file__.rsplit("/", 1)[0])
import play_api as p  # noqa: E402

PRODUCT_ID = "remove_ads"
OPTION_ID = "base"
TITLE = "Supporter Pack"
DESCRIPTION = "Removes every ad for good and unlocks the Moonlit Pearl soap. Thank you!"


def main():
    s = p.service()
    m = s.monetization()
    conv = m.convertRegionPrices(packageName=p.PKG, body={
        "price": {"currencyCode": "USD", "units": "2", "nanos": 990000000}
    }).execute()
    version = conv["regionVersion"]["version"]
    configs = []
    for region, info in conv.get("convertedRegionPrices", {}).items():
        price = info["price"]
        if region == "IN":
            price = {"currencyCode": "INR", "units": "99", "nanos": 0}
        configs.append({"regionCode": region, "price": price, "availability": "AVAILABLE"})
    if not any(c["regionCode"] == "IN" for c in configs):
        configs.append({"regionCode": "IN", "price": {"currencyCode": "INR", "units": "99"}, "availability": "AVAILABLE"})
    body = {
        "packageName": p.PKG,
        "productId": PRODUCT_ID,
        "listings": [{"languageCode": "en-US", "title": TITLE, "description": DESCRIPTION}],
        "purchaseOptions": [{
            "purchaseOptionId": OPTION_ID,
            "buyOption": {"legacyCompatible": True, "multiQuantityEnabled": False},
            "regionalPricingAndAvailabilityConfigs": configs,
        }],
    }
    r = m.onetimeproducts().patch(
        packageName=p.PKG, productId=PRODUCT_ID, body=body, allowMissing=True,
        regionsVersion_version=version, updateMask="listings,purchaseOptions",
    ).execute()
    print("saved:", r["productId"], "| regions:", len(configs))
    a = m.onetimeproducts().purchaseOptions().batchUpdateStates(
        packageName=p.PKG, productId=PRODUCT_ID,
        body={"requests": [{"activatePurchaseOptionRequest": {
            "packageName": p.PKG, "productId": PRODUCT_ID, "purchaseOptionId": OPTION_ID}}]},
    ).execute()
    state = a.get("oneTimeProducts", [{}])[0].get("purchaseOptions", [{}])[0].get("state")
    print("purchase option state:", state)
    got = m.onetimeproducts().get(packageName=p.PKG, productId=PRODUCT_ID).execute()
    prices = {c["regionCode"]: c["price"] for c in got["purchaseOptions"][0]["regionalPricingAndAvailabilityConfigs"]}
    print("IN:", prices.get("IN"), "| US:", prices.get("US"))


if __name__ == "__main__":
    main()
