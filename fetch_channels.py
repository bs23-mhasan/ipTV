#!/usr/bin/env python3
"""
Build bangla_channels.json from iptv-org playlists + your own GitHub repo.

Usage:
    python fetch_channels.py
    python fetch_channels.py --min-res 1080 --out channels.json

No third-party dependencies (Python 3.8+).
"""
import argparse
import json
import re
import sys
import urllib.request
from collections import Counter

# ---------------------------------------------------------------- CONFIG ----

# iptv-org playlists. Each has a primary URL and a fallback (repo gh-pages branch).
IPTV_ORG_SOURCES = [
    "countries/bd.m3u",      # Bangladesh
    "languages/ben.m3u",     # Bengali (adds Indian Bangla channels)
    "categories/sports.m3u"  # Sports
]
IPTV_ORG_PRIMARY = "https://iptv-org.github.io/iptv/{}"
IPTV_ORG_FALLBACK = "https://raw.githubusercontent.com/iptv-org/iptv/gh-pages/{}"

# YOUR repo: raw URLs to .m3u / .m3u8 / .json files. Replace with your own.
# Entries from here win over iptv-org when the stream URL is the same.
MY_REPO_SOURCES = [
    # "https://raw.githubusercontent.com/<user>/<repo>/main/playlist.m3u",
    # "https://raw.githubusercontent.com/<user>/<repo>/main/channels.json",
]

# Filters (applied to iptv-org sources only; your repo's entries are kept as-is
# unless you pass --filter-mine)
KEEP_COUNTRIES = {"BD", "IN"}
MIN_RESOLUTION = 720

COUNTRY_NAMES = {"BD": "Bangladesh", "IN": "India"}
# ---------------------------------------------------------------------------

ATTR = re.compile(r'([\w-]+)="([^"]*)"')


def fetch(url, timeout=30):
    req = urllib.request.Request(url, headers={"User-Agent": "bangla-iptv-fetch/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read().decode("utf-8", errors="replace")


def fetch_iptv_org(path):
    for tpl in (IPTV_ORG_PRIMARY, IPTV_ORG_FALLBACK):
        try:
            return fetch(tpl.format(path))
        except Exception as e:  # network / DNS / 404
            print(f"  ! {tpl.format(path)} failed: {e}", file=sys.stderr)
    return None


def parse_m3u(text):
    channels, cur = [], None
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("#EXTINF"):
            head, _, name = line.partition(",")
            a = dict(ATTR.findall(head))
            name = name.strip()
            m = re.search(r"\.([a-z]{2})(?:@|$)", a.get("tvg-id", ""))
            code = (m.group(1) if m else a.get("tvg-country", "")[:2]).upper()
            q = re.search(r"\((\d{3,4})p\)", name)
            cur = {
                "name": name,
                "id": a.get("tvg-id", ""),
                "country": COUNTRY_NAMES.get(code, code),
                "_code": code,
                "resolution": int(q.group(1)) if q else None,
                "logo": a.get("tvg-logo", ""),
                "group": a.get("group-title", ""),
                "geoBlocked": "[Geo-blocked]" in name,
                "url": "",
            }
        elif line.startswith("#EXTVLCOPT:http-user-agent=") and cur:
            cur["userAgent"] = line.split("=", 1)[1]
        elif line.startswith("#EXTVLCOPT:http-referrer=") and cur:
            cur["referrer"] = line.split("=", 1)[1]
        elif line and not line.startswith("#") and cur:
            cur["url"] = line
            channels.append(cur)
            cur = None
    return channels


def parse_json(text):
    data = json.loads(text)
    items = data["channels"] if isinstance(data, dict) else data
    out = []
    for c in items:
        if not c.get("url"):
            continue
        c = dict(c)
        c.setdefault("name", "")
        c.setdefault("id", "")
        c.setdefault("country", "")
        c.setdefault("resolution", None)
        c.setdefault("logo", "")
        c.setdefault("group", "")
        c.setdefault("geoBlocked", "[Geo-blocked]" in c["name"])
        c["_code"] = next((k for k, v in COUNTRY_NAMES.items() if v == c["country"]), "")
        out.append(c)
    return out


def load_source(url_or_path, is_iptv_org):
    text = fetch_iptv_org(url_or_path) if is_iptv_org else fetch(url_or_path)
    if text is None:
        return []
    if not is_iptv_org and url_or_path.lower().endswith(".json"):
        return parse_json(text)
    return parse_m3u(text)


def passes_filters(c, min_res):
    return c["_code"] in KEEP_COUNTRIES and (c["resolution"] or 0) >= min_res


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="bangla_channels.json")
    ap.add_argument("--min-res", type=int, default=MIN_RESOLUTION)
    ap.add_argument("--filter-mine", action="store_true",
                    help="also apply country/resolution filters to your repo's entries")
    args = ap.parse_args()

    merged, seen = [], set()

    def add(items, apply_filter):
        for c in items:
            if c["url"] in seen:
                continue
            if apply_filter and not passes_filters(c, args.min_res):
                continue
            seen.add(c["url"])
            merged.append(c)

    # 1) your repo first, so your entries win on duplicates
    for url in MY_REPO_SOURCES:
        print(f"Fetching (mine) {url}")
        try:
            add(load_source(url, False), args.filter_mine)
        except Exception as e:
            print(f"  ! skipped: {e}", file=sys.stderr)

    # 2) iptv-org
    for path in IPTV_ORG_SOURCES:
        print(f"Fetching (iptv-org) {path}")
        add(load_source(path, True), True)

    for c in merged:
        c.pop("_code", None)
    merged.sort(key=lambda c: (c["country"], c["name"].lower()))

    result = {
        "total": len(merged),
        "byCountry": dict(Counter(c["country"] or "Unknown" for c in merged)),
        "channels": merged,
    }
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print(f"Wrote {len(merged)} channels -> {args.out}")


if __name__ == "__main__":
    main()
