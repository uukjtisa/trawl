#!/usr/bin/env python3
"""Prove the Kotlin palettes are a byte-exact transcription of the mockup's CSS tokens.

The design contract says the mockup wins where it and the app disagree. That is only enforceable
if someone actually compares them, and a human comparing 7 themes x 18 hex values by eye will
miss a digit. So this compares them mechanically.

Run from anywhere:  python design/verify_tokens.py
Exit code 0 = every token matches. Non-zero = a transcription error, printed per token.
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
MOCK = os.path.join(HERE, "v0.1.0-baseline-mockup-ui.html")
KT = os.path.join(HERE, os.pardir, "app", "src", "main", "java", "com", "junkfood",
                  "seal", "ui", "theme", "TrawlThemes.kt")

# CSS custom property -> the Kotlin Palette field that must carry it.
TOKEN_MAP = {
    "bg": "bg", "surface": "surface", "surfvar": "surfVar", "surfcon": "surfCon",
    "surfhigh": "surfHigh", "primary": "primary", "primary2": "primary2", "accent": "accent",
    "text": "text", "text2": "text2", "outline": "outline", "onprimary": "onPrimary",
    "a1": "a1", "a2": "a2", "a3": "a3", "mote": "mote",
}
THEME_TO_PALETTE = {
    "ember": "EmberPalette", "hearth": "HearthPalette", "grove": "GrovePalette",
    "plum": "PlumPalette", "snow": "SnowPalette", "slate": "SlatePalette",
    "sealplus": "SealPlusPalette",
}


def css_themes(text):
    """{theme: {token: '#rrggbb'}} from the .screen[data-theme=...] blocks."""
    out = {}
    for m in re.finditer(r'\.screen\[data-theme="(\w+)"\]\s*\{(.*?)\}', text, re.S):
        name, body = m.group(1), m.group(2)
        toks = {}
        for tm in re.finditer(r"--([a-z0-9]+)\s*:\s*([^;}]+)", body):
            toks[tm.group(1)] = tm.group(2).strip()
        out[name] = toks
    return out


def kt_palettes(text):
    """{PaletteName: {field: 0xAARRGGBB int}}."""
    out = {}
    for m in re.finditer(r"private val (\w+Palette) = Palette\((.*?)\n\)", text, re.S):
        name, body = m.group(1), m.group(2)
        fields = {}
        for fm in re.finditer(r"(\w+)\s*=\s*(0x[0-9A-Fa-f]{8})", body):
            fields[fm.group(1)] = int(fm.group(2), 16)
        out[name] = fields
    return out


def css_hex_to_argb(v):
    """'#RRGGBB' or the 3-digit shorthand '#RGB' -> 0xFFRRGGBB.

    The shorthand matters: sealplus writes --onprimary as #fff. Treating that as unparseable
    would report a mismatch on a value that is in fact correct, which is worse than no check --
    a verifier that cries wolf gets switched off.
    """
    v = v.strip()
    if re.fullmatch(r"#[0-9A-Fa-f]{6}", v):
        return 0xFF000000 | int(v[1:], 16)
    if re.fullmatch(r"#[0-9A-Fa-f]{3}", v):
        r, g, b = v[1], v[2], v[3]
        return 0xFF000000 | int(r + r + g + g + b + b, 16)
    return None


def main():
    mock = open(MOCK, encoding="utf-8").read()
    kt = open(KT, encoding="utf-8").read()
    themes, palettes = css_themes(mock), kt_palettes(kt)

    problems, checked = [], 0
    for theme, pal_name in THEME_TO_PALETTE.items():
        if theme not in themes:
            problems.append("mockup has no theme block for %r" % theme)
            continue
        if pal_name not in palettes:
            problems.append("TrawlThemes.kt has no %s" % pal_name)
            continue
        css, kot = themes[theme], palettes[pal_name]
        for css_key, kt_key in TOKEN_MAP.items():
            if css_key not in css:
                problems.append("%s: mockup missing --%s" % (theme, css_key))
                continue
            want = css_hex_to_argb(css[css_key])
            if want is None:
                problems.append("%s: --%s is not plain hex (%r)" % (theme, css_key, css[css_key]))
                continue
            got = kot.get(kt_key)
            checked += 1
            if got is None:
                problems.append("%s: Kotlin missing %s" % (theme, kt_key))
            elif got != want:
                problems.append("%s.%s: mockup --%s = #%06X but Kotlin = 0x%08X"
                                % (pal_name, kt_key, css_key, want & 0xFFFFFF, got))

    print("compared %d tokens across %d themes" % (checked, len(THEME_TO_PALETTE)))
    if problems:
        print("\nMISMATCHES (%d):" % len(problems))
        for p in problems:
            print("  -", p)
        return 1
    print("all tokens match the mockup exactly")
    return 0


if __name__ == "__main__":
    sys.exit(main())
