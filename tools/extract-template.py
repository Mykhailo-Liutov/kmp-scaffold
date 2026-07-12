#!/usr/bin/env python3
"""Authoring tool: (re)build ``template/`` from a clean golden KMP/CMP source repo.

Produces a *sanitized literal copy* of the golden project — no tokenization (the golden
literals act as tokens; see scripts/lib_tokens.py). Secrets, build output, VCS state and
the generated Xcode project are excluded; real Firebase client config is replaced with
safe placeholders.

Usage:
    tools/extract-template.py /path/to/golden-source [--out template]

After running, add Firebase sentinel comments to the template (see README "Maintaining
the template"); generate.py strips sentinel-wrapped regions when Firebase is disabled.
"""

from __future__ import annotations

import argparse
import os
import shutil
import sys
from pathlib import Path

# Directory names pruned anywhere in the tree.
PRUNE_DIRS = {
    "build", ".gradle", ".git", ".idea", ".kotlin", ".claude", "captures",
    "xcuserdata", "DerivedData", "Pods", "vendor",
}

# Exact relative files to skip (secrets / machine-local).
SKIP_FILES = {
    "local.properties", "keystore.properties", "release.jks", ".DS_Store",
}

# Extensions never copied (keystores other than the committed debug one).
SKIP_EXTS = {".jks", ".keystore"}
KEEP_KEYSTORE = "debug.keystore"

PLACEHOLDER_GOOGLE_SERVICES = """{
  "project_info": {
    "project_number": "000000000000",
    "project_id": "REPLACE-WITH-YOUR-FIREBASE-PROJECT-ID",
    "storage_bucket": "REPLACE-WITH-YOUR-FIREBASE-PROJECT-ID.appspot.com"
  },
  "client": [
    PLACEHOLDER_CLIENTS
  ],
  "configuration_version": "1"
}
"""

PLACEHOLDER_PLIST = """<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
	<!-- PLACEHOLDER Firebase config. Replace with the real GoogleService-Info.plist
	     from the Firebase console for this bundle id, or disable Firebase. -->
	<key>API_KEY</key>
	<string>REPLACE_WITH_YOUR_API_KEY</string>
	<key>GCM_SENDER_ID</key>
	<string>000000000000</string>
	<key>BUNDLE_ID</key>
	<string>BUNDLE_ID_PLACEHOLDER</string>
	<key>PROJECT_ID</key>
	<string>REPLACE-WITH-YOUR-FIREBASE-PROJECT-ID</string>
	<key>STORAGE_BUCKET</key>
	<string>REPLACE-WITH-YOUR-FIREBASE-PROJECT-ID.appspot.com</string>
	<key>IS_ADS_ENABLED</key>
	<false/>
	<key>IS_ANALYTICS_ENABLED</key>
	<false/>
	<key>IS_APPINVITE_ENABLED</key>
	<true/>
	<key>IS_GCM_ENABLED</key>
	<true/>
	<key>IS_SIGNIN_ENABLED</key>
	<true/>
	<key>GOOGLE_APP_ID</key>
	<string>1:000000000000:ios:0000000000000000000000</string>
</dict>
</plist>
"""

# Base package the extracted template must use. If your golden source uses a different
# package, re-tokenize the output to this neutral identity (see scripts/lib_tokens.py).
GOLDEN_BASE_PKG = "com.acmecorp.acmeapp"


def _client(package_name: str) -> str:
    return (
        '    {\n'
        '      "client_info": {\n'
        '        "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",\n'
        '        "android_client_info": { "package_name": "%s" }\n'
        '      },\n'
        '      "oauth_client": [],\n'
        '      "api_key": [ { "current_key": "REPLACE_WITH_YOUR_API_KEY" } ],\n'
        '      "services": { "appinvite_service": { "other_platform_oauth_client": [] } }\n'
        '    }' % package_name
    )


def _google_services_for(flavor: str) -> str:
    # applicationId variants the AGP build produces for this flavor.
    if flavor == "stage":
        pkgs = [f"{GOLDEN_BASE_PKG}.stage", f"{GOLDEN_BASE_PKG}.stage.debug"]
    else:
        pkgs = [GOLDEN_BASE_PKG, f"{GOLDEN_BASE_PKG}.debug"]
    clients = ",\n".join(_client(p) for p in pkgs)
    return PLACEHOLDER_GOOGLE_SERVICES.replace("    PLACEHOLDER_CLIENTS", clients)


def _plist_for(flavor: str) -> str:
    bundle = GOLDEN_BASE_PKG if flavor == "prod" else f"{GOLDEN_BASE_PKG}.stage"
    return PLACEHOLDER_PLIST.replace("BUNDLE_ID_PLACEHOLDER", bundle)


def should_skip(rel: Path) -> bool:
    name = rel.name
    if name in SKIP_FILES:
        return True
    if name.endswith(tuple(SKIP_EXTS)) and name != KEEP_KEYSTORE:
        return True
    # Generated Xcode project is recreated by `xcodegen generate`.
    if any(part.endswith(".xcodeproj") for part in rel.parts):
        return True
    return False


def is_placeholder_target(rel: Path) -> str | None:
    name = rel.name
    parts = rel.parts
    if name == "google-services.json":
        return "stage" if "stage" in parts else "prod"
    if name == "GoogleService-Info.plist":
        return "stage" if "stage" in parts else "prod"
    return None


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("source", help="path to a clean golden KMP/CMP source repo")
    ap.add_argument("--out", default=None, help="output template dir (default: ../template)")
    args = ap.parse_args()

    src = Path(args.source).resolve()
    out = Path(args.out).resolve() if args.out else (Path(__file__).resolve().parent.parent / "template")
    if not src.is_dir():
        print(f"error: source not a directory: {src}", file=sys.stderr)
        return 2

    if out.exists():
        # Refuse to recursively delete anything that doesn't look like a template
        # output dir — a mistyped --out must not nuke an arbitrary directory.
        plugin_root = Path(__file__).resolve().parent.parent
        if out in (Path("/"), Path.home()) or out == plugin_root or src in (out, *out.parents):
            print(f"error: refusing to delete {out}", file=sys.stderr)
            return 2
        if not (out / "settings.gradle.kts").exists():
            print(
                f"error: {out} exists but doesn't look like a previous template "
                "(no settings.gradle.kts) — delete it yourself if intended",
                file=sys.stderr,
            )
            return 2
        shutil.rmtree(out)
    out.mkdir(parents=True)

    copied = placeholders = 0
    for dirpath, dirnames, filenames in os.walk(src):
        dirnames[:] = [d for d in dirnames if d not in PRUNE_DIRS and not d.endswith(".xcodeproj")]
        for fn in filenames:
            abs_path = Path(dirpath) / fn
            rel = abs_path.relative_to(src)
            if should_skip(rel):
                continue
            dest = out / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            flavor = is_placeholder_target(rel)
            if flavor and rel.name == "google-services.json":
                dest.write_text(_google_services_for(flavor), encoding="utf-8")
                placeholders += 1
            elif flavor and rel.name == "GoogleService-Info.plist":
                dest.write_text(_plist_for(flavor), encoding="utf-8")
                placeholders += 1
            else:
                shutil.copy2(abs_path, dest)
                copied += 1

    print(f"template written to {out}")
    print(f"  files copied:      {copied}")
    print(f"  placeholders made: {placeholders} (google-services.json / GoogleService-Info.plist)")
    print("\nNext: add Firebase sentinel comments to the template (see README).")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
