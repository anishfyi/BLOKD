<div align="center">
  <picture>
    <source media="(prefers-color-scheme: dark)" srcset="docs/blokd-wordmark-dark.svg">
    <img alt="BLOKD" src="docs/blokd-wordmark-light.svg" width="240">
  </picture>
  <p><b>A no-root Android ad and tracker blocker.</b></p>
</div>

---

It runs a local DNS-only VPN on your device and answers DNS for known ad and tracker domains with NXDOMAIN, so those hosts never load across every app and browser.

---

## What's new in v1.5.0

- **Anti-bypass:** blocks apps' and browsers' own DoH/DoT resolvers and routes hardcoded public DNS (8.8.8.8, 1.1.1.1, Quad9, OpenDNS) into the tunnel, so far fewer apps can escape filtering
- **Encrypted-only fallback:** AdGuard DoT with an independent Mullvad DoT backup, so protection never silently drops to unfiltered DNS; a degraded state shows when it falls back
- **In-app allowlist:** add or remove domains you never want blocked, right from the app
- **DNS cache** for faster repeat lookups, plus **pooled DoT connections** so a page's lookups run in parallel
- **560k+ bundled blocklist** (HaGeZi + OISD), daily CDN refresh, Standard / Berserk modes, dual-stack IPv4/IPv6, CNAME-cloak filter
- **New "B." app icon** and refreshed UI

Site: https://anishfyi.github.io/BLOKD/ · [Modes](docs/modes.html) · [Install](docs/install.html) · [Limitations](docs/limitations.html)

## Pointers

- No root, no account. Encrypted AdGuard DNS-over-TLS by default, with an independent encrypted fallback.
- Blocks ad and tracker domains system-wide using public block lists, updatable in app.
- Anti-bypass: blocks apps' own DoH/DoT and intercepts hardcoded public resolvers.
- One main button starts protection, with an in-app allowlist and live blocked and allowed counters.

## Install

1. Download [BLOKD-v1.5.0.apk](https://github.com/anishfyi/BLOKD/releases/download/v1.5.0/BLOKD-v1.5.0.apk) or browse [all releases](https://github.com/anishfyi/BLOKD/releases/tag/v1.5.0).
2. Open it and allow install from your browser or files app.
3. Launch BLOKD, pick Standard or Berserk, flip the switch, and approve the VPN prompt.

Play Protect may show a standard "unknown developer" notice for any sideloaded app. The APK is release signed, so it installs and updates normally once you continue past that notice.

## Build from source

```
gradle :app:assembleDebug
```

Release builds are signed by CI from repository secrets. See `.github/workflows/release.yml`. To sign locally, copy `keystore.properties.example` to `keystore.properties` and point it at your keystore.

## Limitations

DNS blocking cannot remove ads stitched into a video stream from the same server as the content (server-side ad insertion). BLOKD now blocks known DoH/DoT resolvers and intercepts common hardcoded resolver IPs, but a custom or obscure encrypted resolver can still evade it. Berserk means bigger blocklists, not full-tunnel interception. See [limitations](docs/limitations.html) and `docs/ott-sonyliv-findings.md`.

## Star

If BLOKD is useful to you, please star the repo. It genuinely helps.

## License and author

MIT. Author: [anishfyi](https://github.com/anishfyi).
