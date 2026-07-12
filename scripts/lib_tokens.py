"""Identity derivation + ordered literal->value replacement for the golden template.

The bundled ``template/`` is a sanitized *literal* copy of the golden project, so the
golden literals themselves act as tokens. Generation replaces them, in a fixed order
that avoids substring collisions, with the user's chosen identity.

Golden identity (the literals being replaced):
    base package   com.acmecorp.acmeapp
    project name   AcmeApp           (Gradle rootProject.name, Xcode name, scheme prefix)
    class prefix   Acme                  (AcmeTheme, AcmeApplication)
    plugin prefix  acme                  (acme.kmp.library, acme.feature, acme.room, ...)
    slug           acmeapp           (residual: keyAlias, schema dir segment)
    org segment    acmecorp         (residual)
    display        Acme App / Acme App Stage
    base url       https://dummyjson.com/
"""

from __future__ import annotations

import re
from dataclasses import dataclass

GOLDEN_BASE_PKG = "com.acmecorp.acmeapp"
GOLDEN_PROJECT_NAME = "AcmeApp"
GOLDEN_PREFIX_PASCAL = "Acme"
GOLDEN_PREFIX_LOWER = "acme"
GOLDEN_SLUG = "acmeapp"
GOLDEN_ORG_SEGMENT = "acmecorp"
GOLDEN_DISPLAY = "Acme App"
GOLDEN_DISPLAY_STAGE = "Acme App Stage"
GOLDEN_BASE_URL = "https://dummyjson.com/"

# The golden package as a directory chain (separate path segments, not dotted).
GOLDEN_PKG_PATH = "com/acmecorp/acmeapp"


@dataclass
class Identity:
    group: str            # reverse-domain, e.g. "com.acme"
    project_name: str     # PascalCase, e.g. "FitTrack"
    slug: str             # lowercase alnum package segment, e.g. "fittrack"
    prefix_lower: str     # gradle plugin-id prefix, e.g. "ft"
    prefix_pascal: str    # class prefix, e.g. "Ft"
    app_display: str      # human name, e.g. "FitTrack"
    base_url: str         # AppConfig base url

    @property
    def base_pkg(self) -> str:
        return f"{self.group}.{self.slug}"

    @property
    def base_pkg_path(self) -> str:
        return self.base_pkg.replace(".", "/")

    @property
    def org_segment(self) -> str:
        return self.group.split(".")[-1]


def _pascal_case(name: str) -> str:
    parts = re.split(r"[^A-Za-z0-9]+", name)
    parts = [p for p in parts if p]
    if not parts:
        return "App"
    # Preserve existing internal capitalization for single CamelCase tokens.
    if len(parts) == 1 and any(c.isupper() for c in parts[0][1:]):
        return parts[0][0].upper() + parts[0][1:]
    return "".join(p[:1].upper() + p[1:] for p in parts)


def _slug(name: str) -> str:
    s = re.sub(r"[^A-Za-z0-9]+", "", name).lower()
    return s or "app"


def _derive_prefix(project_name: str, slug: str) -> str:
    initials = "".join(c for c in project_name if c.isupper()).lower()
    if len(initials) >= 2:
        prefix = initials
    else:
        prefix = slug[:3]
    prefix = re.sub(r"[^a-z0-9]", "", prefix)
    if not prefix or not prefix[0].isalpha():
        prefix = ("a" + prefix)[:3]
    return prefix


def derive_identity(
    group: str,
    app_name: str,
    base_url: str = GOLDEN_BASE_URL,
    slug: str | None = None,
    prefix: str | None = None,
    app_display: str | None = None,
) -> Identity:
    group = group.strip().lower().strip(".")
    project_name = _pascal_case(app_name)
    slug = (slug or _slug(app_name)).lower()
    prefix_lower = (prefix or _derive_prefix(project_name, slug)).lower()
    prefix_pascal = prefix_lower[:1].upper() + prefix_lower[1:]
    return Identity(
        group=group,
        project_name=project_name,
        slug=slug,
        prefix_lower=prefix_lower,
        prefix_pascal=prefix_pascal,
        app_display=(app_display or app_name).strip(),
        base_url=base_url,
    )


def content_replacements(idn: Identity) -> list[tuple[str, str]]:
    """Ordered (old, new) pairs for file *contents*. Order matters: longer / more
    specific literals first so a later rule never rewrites a substring of an earlier
    result (e.g. ``Acme`` is a substring of ``AcmeApp``)."""
    repls = [
        (GOLDEN_BASE_PKG, idn.base_pkg),
        (GOLDEN_DISPLAY_STAGE, f"{idn.app_display} Stage"),
        (GOLDEN_DISPLAY, idn.app_display),
        (GOLDEN_PROJECT_NAME, idn.project_name),
        (GOLDEN_PREFIX_PASCAL, idn.prefix_pascal),
        (GOLDEN_PREFIX_LOWER + ".", idn.prefix_lower + "."),
        (GOLDEN_SLUG, idn.slug),
        (GOLDEN_ORG_SEGMENT, idn.org_segment),
    ]
    if idn.base_url != GOLDEN_BASE_URL:
        repls.append((GOLDEN_BASE_URL, idn.base_url))
    return repls


def apply_replacements(text: str, repls: list[tuple[str, str]]) -> str:
    for old, new in repls:
        text = text.replace(old, new)
    return text


def transform_relpath(rel_posix: str, idn: Identity) -> str:
    """Map a template-relative path to the generated path: relocate the package dir
    chain, then apply the dotted/name replacements (catches the Room schema dir whose
    name embeds the dotted package, and any identity literal in a file name)."""
    rel_posix = rel_posix.replace(GOLDEN_PKG_PATH, idn.base_pkg_path)
    return apply_replacements(rel_posix, content_replacements(idn))
