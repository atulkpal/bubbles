#!/usr/bin/env python3
"""Create 'remove_ads' in-app product on Google Play Console via new Monetization API."""

import json
import sys
import time
from google.oauth2 import service_account
from google.auth.transport.requests import AuthorizedSession

SCOPES = ["https://www.googleapis.com/auth/androidpublisher"]
SERVICE_ACCOUNT_FILE = "play-account.json"
PACKAGE_NAME = "com.ashwathai.bubbles"
PRODUCT_ID = "remove_ads"
API_ROOT = (
    "https://androidpublisher.googleapis.com/androidpublisher"
    "/v3/applications/" + PACKAGE_NAME
)
FALLBACK_REGIONS_VERSION = "2024/12/02"


def to_money(price_micros_str, currency_code):
    """Convert price micros string to Money object."""
    micros = int(price_micros_str)
    units = micros // 1_000_000
    nanos = (micros % 1_000_000) * 1000
    return {
        "currencyCode": currency_code,
        "units": str(units),
        "nanos": nanos,
    }


def convert_region_prices(session, money):
    """Convert a single price to regional pricing matrix."""
    url = API_ROOT + "/pricing:convertRegionPrices"
    resp = session.post(url, json={"price": money})
    resp.raise_for_status()
    return resp.json()


def upsert_product(session, product_id, conversion, listings, money):
    """Create or update a one-time product. Returns the purchaseOptionId."""
    url = API_ROOT + "/onetimeproducts/" + product_id

    region_version = (conversion.get("regionVersion") or {}).get("version") or FALLBACK_REGIONS_VERSION
    converted_prices = conversion.get("convertedRegionPrices", {})

    # Build regional configs from converted prices
    regional_configs = []
    for region_code, price_info in converted_prices.items():
        price = price_info.get("price", {})
        if price:
            regional_configs.append({
                "regionCode": region_code,
                "price": {
                    "currencyCode": price.get("currencyCode", "USD"),
                    "units": price.get("units", "1"),
                    "nanos": int(price.get("nanos", 0)),
                },
                "availability": "AVAILABLE",
            })

    # If no regional configs, use US default
    if not regional_configs:
        regional_configs = [{
            "regionCode": "US",
            "price": money,
            "availability": "AVAILABLE",
        }]

    # Build listings
    listing_list = []
    for lang, listing_data in listings.items():
        listing_list.append({
            "languageCode": lang,
            "title": listing_data["title"],
            "description": listing_data["description"],
        })

    option_id = "base"

    product_body = {
        "packageName": PACKAGE_NAME,
        "productId": product_id,
        "listings": listing_list,
        "purchaseOptions": [
            {
                "purchaseOptionId": option_id,
                "buyOption": {
                    "legacyCompatible": True,
                    "multiQuantityEnabled": False,
                },
                "regionalPricingAndAvailabilityConfigs": regional_configs,
            }
        ],        }

    resp = session.patch(
        url,
        json=product_body,
        params={
            "allowMissing": "true",
            "regionsVersion.version": region_version,
            "updateMask": "listings,purchaseOptions",
        },
    )
    if resp.status_code != 200:
        print(f"DEBUG: Status {resp.status_code}")
        print(f"DEBUG: Response {resp.text}")
        print(f"DEBUG: Request body:")
        print(json.dumps(product_body, indent=2))
        resp.raise_for_status()
    return option_id


def activate_options(session, products_to_activate):
    """Activate purchase options for products."""
    for product_id, option_id in products_to_activate:
        url = API_ROOT + "/oneTimeProducts/" + product_id + "/purchaseOptions:batchUpdateStates"
        body = {
            "requests": [
                {
                    "productId": product_id,
                    "purchaseOptionId": option_id,
                    "newState": "ACTIVE",
                }
            ]
        }
        resp = session.post(url, json=body)
        if resp.status_code == 200:
            print(f"  Activated {product_id}/{option_id}")
        else:
            print(f"  WARNING: Activation failed for {product_id}: {resp.status_code} {resp.text}")


def main():
    # Authenticate
    creds = service_account.Credentials.from_service_account_file(
        SERVICE_ACCOUNT_FILE, scopes=SCOPES
    )
    session = AuthorizedSession(creds)
    print("Authenticated successfully")

    # Step 1: Check if product already exists
    print(f"Checking if product '{PRODUCT_ID}' already exists...")
    check_url = API_ROOT + "/monetization/onetimeproducts/" + PRODUCT_ID
    try:
        resp = session.get(check_url)
        if resp.status_code == 200:
            print("Product already exists:")
            print(json.dumps(resp.json(), indent=2))
            return
    except Exception:
        pass

    # Alternative check via the correct URL
    check_url2 = API_ROOT + "/oneTimeProducts/" + PRODUCT_ID
    try:
        resp = session.get(check_url2)
        if resp.status_code == 200:
            print("Product already exists:")
            print(json.dumps(resp.json(), indent=2))
            return
    except Exception:
        pass

    print("Product not found, creating new one...")

    # Step 2: Convert price to regional pricing
    print("Converting price to regional pricing...")
    money = to_money("1990000", "USD")  # $1.99
    try:
        conversion = convert_region_prices(session, money)
        converted_prices = conversion.get("convertedRegionPrices", {})
        region_version = (conversion.get("regionVersion") or {}).get("version")
        print(f"Got {len(converted_prices)} regional prices, version: {region_version}")
    except Exception as e:
        print(f"ERROR: Price conversion failed: {e}")
        sys.exit(1)

    # Step 3: Create the product
    print(f"Creating in-app product '{PRODUCT_ID}'...")
    listings = {
        "en-US": {
            "title": "Remove Ads",
            "description": "Remove all advertisements from Bubbles permanently. Enjoy an uninterrupted bubble-popping experience!",
        }
    }
    try:
        option_id = upsert_product(session, PRODUCT_ID, conversion, listings, money)
        print(f"Product created (DRAFT state), purchaseOptionId: {option_id}")
    except Exception as e:
        print(f"ERROR creating product: {e}")
        sys.exit(1)

    # Step 4: Activate the product
    print("Activating product...")
    time.sleep(1)  # Small delay
    activate_options(session, [(PRODUCT_ID, option_id)])

    # Final verification
    print("\nVerifying final product state...")
    try:
        resp = session.get(API_ROOT + "/oneTimeProducts/" + PRODUCT_ID)
        if resp.status_code == 200:
            print(json.dumps(resp.json(), indent=2))
        else:
            print(f"Verification failed: {resp.status_code}")
    except Exception as e:
        print(f"Verification failed: {e}")


if __name__ == "__main__":
    main()
