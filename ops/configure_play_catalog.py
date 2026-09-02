"""Idempotently configure ReplayLead's Google Play monetization catalog.

The service-account key stays in ops/private and is never printed. Run without
--apply for a read-only status check; pass --apply to create/update products.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path
from typing import Any

from google.auth.transport.requests import AuthorizedSession
from google.oauth2 import service_account


PACKAGE = "com.replaylead.app"
API_ROOT = "https://androidpublisher.googleapis.com/androidpublisher/v3"
KEY_FILE = Path(__file__).resolve().parent / "private" / "revenuecat-key.json"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"


def session() -> AuthorizedSession:
    credentials = service_account.Credentials.from_service_account_file(
        KEY_FILE, scopes=[SCOPE]
    )
    return AuthorizedSession(credentials)


def request(
    client: AuthorizedSession,
    method: str,
    url: str,
    *,
    params: dict[str, Any] | None = None,
    body: dict[str, Any] | None = None,
) -> dict[str, Any]:
    response = client.request(method, url, params=params, json=body, timeout=60)
    if not response.ok:
        try:
            detail = response.json()
        except ValueError:
            detail = {"message": response.text[:1000]}
        raise RuntimeError(
            f"Google Play API {method} failed ({response.status_code}): "
            f"{json.dumps(detail, ensure_ascii=False)}"
        )
    return response.json() if response.content else {}


def get_optional(
    client: AuthorizedSession, url: str
) -> dict[str, Any] | None:
    response = client.get(url, timeout=60)
    if response.status_code == 404:
        return None
    if not response.ok:
        try:
            detail = response.json()
        except ValueError:
            detail = {"message": response.text[:1000]}
        raise RuntimeError(
            f"Google Play API GET failed ({response.status_code}): "
            f"{json.dumps(detail, ensure_ascii=False)}"
        )
    return response.json()


def money_from_cents(currency: str, cents: int) -> dict[str, Any]:
    return {
        "currencyCode": currency,
        "units": str(cents // 100),
        "nanos": (cents % 100) * 10_000_000,
    }


def converted_prices(
    client: AuthorizedSession, usd_cents: int
) -> dict[str, Any]:
    return request(
        client,
        "POST",
        f"{API_ROOT}/applications/{PACKAGE}/pricing:convertRegionPrices",
        body={"price": money_from_cents("USD", usd_cents)},
    )


def base_plan(
    plan_id: str, period: str, conversion: dict[str, Any]
) -> dict[str, Any]:
    regional = [
        {
            "regionCode": value["regionCode"],
            "newSubscriberAvailability": True,
            "price": value["price"],
        }
        for value in conversion["convertedRegionPrices"].values()
    ]
    other = conversion["convertedOtherRegionsPrice"]
    return {
        "basePlanId": plan_id,
        "regionalConfigs": regional,
        "otherRegionsConfig": {
            "usdPrice": other["usdPrice"],
            "eurPrice": other["eurPrice"],
            "newSubscriberAvailability": True,
        },
        "autoRenewingBasePlanType": {
            "billingPeriodDuration": period,
            "resubscribeState": "RESUBSCRIBE_STATE_ACTIVE",
        },
    }


def subscription_url(product_id: str) -> str:
    return f"{API_ROOT}/applications/{PACKAGE}/subscriptions/{product_id}"


def ensure_subscription(
    client: AuthorizedSession,
    *,
    product_id: str,
    title: str,
    plan_id: str,
    period: str,
    usd_cents: int,
    apply: bool,
) -> dict[str, Any] | None:
    current = get_optional(client, subscription_url(product_id))
    plans = current.get("basePlans", []) if current else []
    matching = next((p for p in plans if p.get("basePlanId") == plan_id), None)

    if current and matching:
        return current
    if not apply:
        return current

    conversion = converted_prices(client, usd_cents)
    new_plan = base_plan(plan_id, period, conversion)
    region_version = conversion["regionVersion"]["version"]

    if current:
        body = {
            "packageName": PACKAGE,
            "productId": product_id,
            "basePlans": [*plans, new_plan],
        }
        request(
            client,
            "PATCH",
            subscription_url(product_id),
            params={
                "updateMask": "basePlans",
                "regionsVersion.version": region_version,
            },
            body=body,
        )
    else:
        body = {
            "packageName": PACKAGE,
            "productId": product_id,
            "basePlans": [new_plan],
            "listings": [
                {
                    "languageCode": "en-US",
                    "title": title,
                    "description": (
                        "ReplayLead Pro with unlimited AI-powered rehearsal, "
                        "rewind history, and advanced scenarios."
                    ),
                    "benefits": [
                        "Unlimited AI coaching",
                        "Full rewind history",
                        "Advanced rehearsal scenarios",
                    ],
                }
            ],
        }
        request(
            client,
            "POST",
            f"{API_ROOT}/applications/{PACKAGE}/subscriptions",
            params={
                "productId": product_id,
                "regionsVersion.version": region_version,
            },
            body=body,
        )

    created = request(client, "GET", subscription_url(product_id))
    created_plan = next(
        p for p in created.get("basePlans", []) if p.get("basePlanId") == plan_id
    )
    if created_plan.get("state") == "DRAFT":
        request(
            client,
            "POST",
            f"{subscription_url(product_id)}/basePlans/{plan_id}:activate",
            body={},
        )
    return request(client, "GET", subscription_url(product_id))


def one_time_url(product_id: str) -> str:
    return f"{API_ROOT}/applications/{PACKAGE}/oneTimeProducts/{product_id}"


def ensure_lifetime(
    client: AuthorizedSession, *, usd_cents: int, apply: bool
) -> dict[str, Any] | None:
    product_id = "replaylead_pro_lifetime"
    purchase_option_id = "lifetime"
    current = get_optional(client, one_time_url(product_id))
    option = None
    if current:
        option = next(
            (
                item
                for item in current.get("purchaseOptions", [])
                if item.get("purchaseOptionId") == purchase_option_id
            ),
            None,
        )
    if current and option:
        return current
    if not apply:
        return current

    conversion = converted_prices(client, usd_cents)
    regional = [
        {
            "regionCode": value["regionCode"],
            "price": value["price"],
            "availability": "AVAILABLE",
        }
        for value in conversion["convertedRegionPrices"].values()
    ]
    other = conversion["convertedOtherRegionsPrice"]
    body = {
        "packageName": PACKAGE,
        "productId": product_id,
        "listings": [
            {
                "languageCode": "en-US",
                "title": "ReplayLead Pro Lifetime",
                "description": "Lifetime access to ReplayLead Pro features.",
            }
        ],
        "purchaseOptions": [
            {
                "purchaseOptionId": purchase_option_id,
                "regionalPricingAndAvailabilityConfigs": regional,
                "newRegionsConfig": {
                    "usdPrice": other["usdPrice"],
                    "eurPrice": other["eurPrice"],
                    "availability": "AVAILABLE",
                },
                "buyOption": {
                    "legacyCompatible": True,
                    "multiQuantityEnabled": False,
                },
            }
        ],
    }
    version = conversion["regionVersion"]["version"]
    request(
        client,
        "PATCH",
        one_time_url(product_id),
        params={
            "updateMask": "listings,purchaseOptions",
            "regionsVersion.version": version,
            "allowMissing": "true",
        },
        body=body,
    )
    created = request(client, "GET", one_time_url(product_id))
    created_option = next(
        item
        for item in created.get("purchaseOptions", [])
        if item.get("purchaseOptionId") == purchase_option_id
    )
    if created_option.get("state") == "DRAFT":
        request(
            client,
            "POST",
            f"{one_time_url(product_id)}/purchaseOptions:batchUpdateStates",
            body={
                "requests": [
                    {
                        "activatePurchaseOptionRequest": {
                            "packageName": PACKAGE,
                            "productId": product_id,
                            "purchaseOptionId": purchase_option_id,
                        }
                    }
                ]
            },
        )
    return request(client, "GET", one_time_url(product_id))


def summarize_subscription(resource: dict[str, Any] | None) -> dict[str, Any]:
    if not resource:
        return {"exists": False}
    return {
        "exists": True,
        "productId": resource.get("productId"),
        "title": (resource.get("listings") or [{}])[0].get("title"),
        "basePlans": [
            {
                "basePlanId": plan.get("basePlanId"),
                "state": plan.get("state"),
                "period": plan.get("autoRenewingBasePlanType", {}).get(
                    "billingPeriodDuration"
                ),
                "regions": len(plan.get("regionalConfigs", [])),
            }
            for plan in resource.get("basePlans", [])
        ],
    }


def summarize_lifetime(resource: dict[str, Any] | None) -> dict[str, Any]:
    if not resource:
        return {"exists": False}
    return {
        "exists": True,
        "productId": resource.get("productId"),
        "title": (resource.get("listings") or [{}])[0].get("title"),
        "purchaseOptions": [
            {
                "purchaseOptionId": option.get("purchaseOptionId"),
                "state": option.get("state"),
                "regions": len(
                    option.get("regionalPricingAndAvailabilityConfigs", [])
                ),
            }
            for option in resource.get("purchaseOptions", [])
        ],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--apply", action="store_true", help="Create/update and activate products"
    )
    args = parser.parse_args()
    client = session()
    monthly = ensure_subscription(
        client,
        product_id="replaylead_pro_monthly",
        title="ReplayLead Pro Monthly",
        plan_id="monthly",
        period="P1M",
        usd_cents=999,
        apply=args.apply,
    )
    yearly = ensure_subscription(
        client,
        product_id="replaylead_pro_yearly",
        title="ReplayLead Pro Yearly",
        plan_id="yearly",
        period="P1Y",
        usd_cents=7999,
        apply=args.apply,
    )
    lifetime = ensure_lifetime(client, usd_cents=9999, apply=args.apply)
    print(
        json.dumps(
            {
                "mode": "apply" if args.apply else "read-only",
                "monthly": summarize_subscription(monthly),
                "yearly": summarize_subscription(yearly),
                "lifetime": summarize_lifetime(lifetime),
            },
            ensure_ascii=False,
            indent=2,
        )
    )


if __name__ == "__main__":
    main()
