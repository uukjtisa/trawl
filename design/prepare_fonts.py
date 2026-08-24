# Step 6: prepare the bundled type.
#
# WHY BUNDLED AT ALL: this Huawei has no Google Play Services, so Android's downloadable-fonts
# provider cannot resolve. A font requested that way would silently fall back to the system face
# and the whole design would render in Roboto. Bundling is the only option that actually works
# on the target device.
#
# WHY VARIABLE-BUT-PINNED: the mockup uses SEVEN body weights (450, 500, 550, 600, 650, 700, 800).
# Static instances would force each of those to snap to the nearest of four, losing the design's
# deliberate half-steps. Keeping the wght axis preserves them exactly on API 26+. The other axes
# are pinned: Fraunces ships SOFT, WONK and opsz that the design never varies, and Inter's opsz
# likewise -- carrying axes nothing sets is dead weight in every APK.
import io, os, shutil, sys
from fontTools import ttLib
from fontTools.varLib import instancer

SCRATCH = os.path.dirname(os.path.abspath(__file__))
FONT_DIR = r"D:\Android-programs\Trawl\app\src\main\res\font"
LIC_DIR = r"D:\Android-programs\Trawl\licenses"
os.makedirs(FONT_DIR, exist_ok=True)
os.makedirs(LIC_DIR, exist_ok=True)


def axes_of(path):
    f = ttLib.TTFont(path)
    return {a.axisTag: (a.minValue, a.defaultValue, a.maxValue)
            for a in f["fvar"].axes} if "fvar" in f else {}


JOBS = [
    # (source, output resource name, axes to pin)
    ("Inter-var.ttf", "inter", {"opsz": 14}),
    # opsz 32: Fraunces is only used for display text at roughly 20-30sp, and the optical size
    # axis is what keeps a serif from looking spindly at those sizes. Browsers pick this
    # automatically via font-optical-sizing; Android does not, so it is chosen here.
    ("Fraunces-var.ttf", "fraunces", {"SOFT": 0, "WONK": 0, "opsz": 32}),
]

for src, name, pins in JOBS:
    p = os.path.join(SCRATCH, src)
    before = os.path.getsize(p)
    print("%s  axes: %s" % (src, axes_of(p)))
    font = ttLib.TTFont(p)
    font = instancer.instantiateVariableFont(font, pins, inplace=False, updateFontNames=False)
    out = os.path.join(FONT_DIR, name + ".ttf")
    font.save(out)
    after = os.path.getsize(out)
    print("  -> %-14s %6.0f KB -> %6.0f KB   remaining axes: %s\n"
          % (name + ".ttf", before / 1024, after / 1024, list(axes_of(out))))

# OFL requires the licence travel with the font.
import urllib.request
for slug, fname in (("inter", "Inter-OFL.txt"), ("fraunces", "Fraunces-OFL.txt")):
    url = "https://raw.githubusercontent.com/google/fonts/main/ofl/%s/OFL.txt" % slug
    dst = os.path.join(LIC_DIR, fname)
    urllib.request.urlretrieve(url, dst)
    print("licence: %s (%d bytes)" % (fname, os.path.getsize(dst)))
