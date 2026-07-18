# Launcher icon

The BLOKD adaptive launcher icon is a typographic monogram: a capital **B** with a hairline shield behind it.

## Typeface

The letterform is inspired by **[Bodoni Moda](https://github.com/indestructible-type/Bodoni)** (Bold / 700), a high-contrast Didone serif released under the **SIL Open Font License 1.1**. Paths in `app/src/main/res/drawable/ic_launcher_foreground.xml` were traced from that face; the font file itself is not bundled in the app.

Palette: warm ivory `#F3EEE6` on near-black `#0E0D0C`.

## Adaptive icon layout

| Resource | Role |
|---|---|
| `drawable/ic_launcher_foreground.xml` | Monogram + hairline shield |
| `drawable/ic_launcher_background.xml` | Solid background plate |
| `mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon |
| `mipmap-anydpi-v26/ic_launcher_round.xml` | Round adaptive icon |
| `values/colors.xml` → `ic_launcher_background` | Background color |

`minSdk` is 26, so the `anydpi-v26` adaptive definitions are sufficient for the store build.

## Regenerating raster mipmaps

Vector adaptive layers are the source of truth. If you need density-specific raster mipmaps (Play asset packs, older tooling, or marketing exports), regenerate them in Android Studio:

**File → New → Image Asset → Launcher Icons (Adaptive and Legacy)**

Point the foreground at `ic_launcher_foreground` (or export a 1080×1080 PNG from `docs/logo.svg`) and set the background color to `#0E0D0C`. That writes `mipmap-hdpi` … `mipmap-xxxhdpi` PNGs; the vector adaptive XML can stay as-is.

Web and README logos use `docs/logo.svg` (inline-friendly) with `docs/logo.png` as a raster fallback.
