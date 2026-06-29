#!/usr/bin/env python3
import argparse
import json
import re
from pathlib import Path


def write_result(payload: dict) -> None:
    print(json.dumps(payload))


def text_for_answer(answers, label):
    normalized = (label or "").lower()
    for item in answers:
        question = (item.get("question") or "").lower()
        if question and (question in normalized or normalized in question):
            return item.get("answer") or ""
    if "salary" in normalized:
        return next((item.get("answer") for item in answers if "salary" in (item.get("question") or "").lower()), "")
    if "authorization" in normalized or "sponsor" in normalized or "visa" in normalized:
        return next((item.get("answer") for item in answers if "authorization" in (item.get("question") or "").lower()), "")
    if "why" in normalized or "company" in normalized:
        return next((item.get("answer") for item in answers if "why" in (item.get("question") or "").lower()), "")
    if "about" in normalized or "summary" in normalized:
        return next((item.get("answer") for item in answers if "yourself" in (item.get("question") or "").lower()), "")
    return ""


def best_option(label, options):
    normalized = (label or "").lower()
    for option in options:
        value = (option or "").lower()
        if not value:
            continue
        if ("yes" in value and any(token in normalized for token in ["authorized", "eligible", "work"])) or ("no" in value and "sponsor" in normalized):
            return option
        if "united states" in value or value == "usa":
            return option
        if "texas" in value or value == "tx":
            return option
        if "remote" in value:
            return option
    return options[0] if options else ""


def field_label(page, locator):
    try:
        attrs = []
        for attr in ["aria-label", "placeholder", "name", "id"]:
            value = locator.get_attribute(attr, timeout=500)
            if value:
                attrs.append(value)
        element_id = locator.get_attribute("id", timeout=500)
        if element_id:
            label = page.locator(f"label[for='{element_id}']").first
            if label.count() > 0:
                attrs.append(label.inner_text(timeout=500))
        return " ".join(attrs)
    except Exception:
        return ""


def has_human_gate(page):
    text = page.locator("body").inner_text(timeout=3000).lower()
    if "captcha" in text or "recaptcha" in text:
        return "CAPTCHA"
    if "multi-factor" in text or "mfa" in text or "verification code" in text:
        return "MFA"
    return None


def run_application(payload_path: Path) -> int:
    payload = json.loads(payload_path.read_text())
    output = Path(payload["outputFolder"])
    output.mkdir(parents=True, exist_ok=True)
    screenshots_dir = output / "screenshots"
    screenshots_dir.mkdir(parents=True, exist_ok=True)
    actions = []
    screenshots = []

    try:
        from playwright.sync_api import sync_playwright
    except Exception as exc:
        write_result({
            "status": "PAUSED_FOR_MANUAL_REVIEW",
            "pauseReason": "Playwright is not installed",
            "actions": actions,
            "screenshots": screenshots,
            "fallback": True,
            "error": str(exc),
        })
        return 0

    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=False)
            page = browser.new_page(viewport={"width": 1440, "height": 1200})
            page.goto(payload["url"], wait_until="domcontentloaded", timeout=45000)
            actions.append(f"Opened {payload['url']}")

            apply_links = page.get_by_role("link", name=re.compile("apply", re.I))
            apply_buttons = page.get_by_role("button", name=re.compile("apply", re.I))
            if apply_links.count() > 0:
                apply_links.first.click(timeout=5000)
                actions.append("Clicked apply link")
                page.wait_for_load_state("domcontentloaded", timeout=15000)
            elif apply_buttons.count() > 0:
                apply_buttons.first.click(timeout=5000)
                actions.append("Clicked apply button")
                page.wait_for_timeout(1500)

            gate = has_human_gate(page)
            if gate:
                screenshot = screenshots_dir / "human-gate.png"
                page.screenshot(path=str(screenshot), full_page=True)
                screenshots.append(str(screenshot))
                browser.close()
                write_result({
                    "status": "PAUSED_FOR_HUMAN",
                    "pauseReason": gate,
                    "actions": actions,
                    "screenshots": screenshots,
                    "fallback": False,
                    "error": None,
                })
                return 0

            resume_path = payload.get("resumePath")
            cover_path = payload.get("coverLetterPath")
            file_inputs = page.locator("input[type='file']")
            for index in range(file_inputs.count()):
                field = file_inputs.nth(index)
                try:
                    label = field_label(page, field).lower()
                    if cover_path and "cover" in label:
                        field.set_input_files(cover_path)
                        actions.append("Uploaded cover letter")
                    elif resume_path:
                        field.set_input_files(resume_path)
                        actions.append("Uploaded resume")
                except Exception as exc:
                    actions.append(f"Skipped file upload field: {exc}")

            answers = payload.get("answers") or []
            for selector in ["textarea", "input[type='text']", "input[type='email']", "input[type='tel']"]:
                fields = page.locator(selector)
                for index in range(min(fields.count(), 40)):
                    field = fields.nth(index)
                    try:
                        if not field.is_visible(timeout=500) or field.input_value(timeout=500):
                            continue
                        label = field_label(page, field)
                        answer = text_for_answer(answers, label)
                        if answer:
                            field.fill(answer[:3500], timeout=1500)
                            actions.append(f"Filled field: {label[:80]}")
                    except Exception:
                        continue

            selects = page.locator("select")
            for index in range(min(selects.count(), 30)):
                field = selects.nth(index)
                try:
                    if not field.is_visible(timeout=500):
                        continue
                    label = field_label(page, field)
                    options = field.locator("option").all_inner_texts(timeout=1000)
                    choice = best_option(label, options)
                    if choice:
                        field.select_option(label=choice, timeout=1500)
                        actions.append(f"Selected option: {label[:80]} -> {choice[:60]}")
                except Exception:
                    continue

            for selector in ["input[type='checkbox']", "input[type='radio']"]:
                fields = page.locator(selector)
                for index in range(min(fields.count(), 50)):
                    field = fields.nth(index)
                    try:
                        if not field.is_visible(timeout=500) or field.is_checked(timeout=500):
                            continue
                        label = field_label(page, field).lower()
                        if any(token in label for token in ["agree", "consent", "authorize", "eligible", "remote", "terms"]) and not any(token in label for token in ["no ", "not ", "decline"]):
                            field.check(timeout=1500)
                            actions.append(f"Checked field: {label[:80]}")
                    except Exception:
                        continue

            final_submit = page.get_by_role("button", name=re.compile("submit|send application|finish", re.I))
            screenshot = screenshots_dir / "before-final-review.png"
            page.screenshot(path=str(screenshot), full_page=True)
            screenshots.append(str(screenshot))
            status = "PAUSED_BEFORE_FINAL_SUBMISSION" if final_submit.count() > 0 else "PAUSED_FOR_MANUAL_REVIEW"
            reason = "Final submission requires approval" if final_submit.count() > 0 else "Review remaining fields manually"
            browser.close()
            write_result({
                "status": status,
                "pauseReason": reason,
                "actions": actions,
                "screenshots": screenshots,
                "fallback": False,
                "error": None,
            })
            return 0
    except Exception as exc:
        screenshot = screenshots_dir / "automation-error.png"
        try:
            page.screenshot(path=str(screenshot), full_page=True)
            screenshots.append(str(screenshot))
        except Exception:
            pass
        write_result({
            "status": "PAUSED_FOR_MANUAL_REVIEW",
            "pauseReason": "Unexpected browser automation error",
            "actions": actions,
            "screenshots": screenshots,
            "fallback": True,
            "error": str(exc),
        })
        return 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--url")
    parser.add_argument("--output")
    parser.add_argument("--application")
    args = parser.parse_args()

    if args.application:
        return run_application(Path(args.application))

    if not args.url or not args.output:
        parser.error("--url and --output are required unless --application is used")

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
            for _ in range(4):
                page.mouse.wheel(0, 1800)
                page.wait_for_timeout(700)
            title = page.title()
            text = page.locator("body").inner_text(timeout=5000)
            screenshot = output / "images" / "page.png"
            screenshot.parent.mkdir(parents=True, exist_ok=True)
            page.screenshot(path=str(screenshot), full_page=True)
            image_urls = page.eval_on_selector_all("img", "(imgs) => imgs.map(img => img.currentSrc || img.src).filter(Boolean).slice(0, 40)")
            links = page.eval_on_selector_all("a", "(anchors) => anchors.map(a => ({href: a.href, text: (a.innerText || a.textContent || '').trim()})).filter(a => a.href && a.text).slice(0, 300)")
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
        "structured": {"imageUrls": image_urls, "links": links},
        "fallback": False,
        "error": None,
    }))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
