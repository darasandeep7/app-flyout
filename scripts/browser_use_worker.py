#!/usr/bin/env python3
import argparse
import json
from pathlib import Path


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()

    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)

    try:
        from playwright.sync_api import sync_playwright
    except Exception as exc:
        print(json.dumps({
            "url": args.url,
            "title": "",
            "visibleText": "",
            "screenshots": [],
            "images": [],
            "structured": {},
            "fallback": True,
            "error": f"Playwright is not installed: {exc}",
        }))
        return 0

    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page(viewport={"width": 1440, "height": 1200})
            page.goto(args.url, wait_until="networkidle", timeout=30000)
            title = page.title()
            text = page.locator("body").inner_text(timeout=5000)
            screenshot = output / "images" / "page.png"
            screenshot.parent.mkdir(parents=True, exist_ok=True)
            page.screenshot(path=str(screenshot), full_page=True)
            image_urls = page.eval_on_selector_all("img", "(imgs) => imgs.map(img => img.currentSrc || img.src).filter(Boolean).slice(0, 40)")
            browser.close()
    except Exception as exc:
        print(json.dumps({
            "url": args.url,
            "title": "",
            "visibleText": "",
            "screenshots": [],
            "images": [],
            "structured": {},
            "fallback": True,
            "error": f"Playwright browser run failed: {exc}",
        }))
        return 0

    print(json.dumps({
        "url": args.url,
        "title": title,
        "visibleText": text[:20000],
        "screenshots": [str(screenshot)],
        "images": [],
        "structured": {"imageUrls": image_urls},
        "fallback": False,
        "error": None,
    }))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
