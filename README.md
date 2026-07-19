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

## What's new in v1.1.3

- **560k+ bundled blocklist** (HaGeZi + OISD) on first install, with daily CDN refresh via WorkManager
- **Dual-stack IPv4/IPv6 VPN:** fixes false "no internet" warnings while browsing still works
- **Standard / Berserk modes:** Berserk adds the full HaGeZi Pro list (aggressive DNS blocking, not full-tunnel)
- **CNAME cloaking filter:** blocks upstream answers that chain to tracker domains
- **Async resolver** with AdGuard DoT + system DNS fallback; SERVFAIL instead of silent drops
- **Redesigned UI:** health banner, mode cards, live session stats

Site: https://anishfyi.github.io/BLOKD/ · [Modes](docs/modes.html) · [Install](docs/install.html) · [Limitations](docs/limitations.html)

## Pointers

- No root, no account. DNS queries go to your chosen upstream (AdGuard DoT or system DNS).
- Blocks ad and tracker domains system-wide using public block lists, updatable in app.
- Optional encrypted AdGuard DNS-over-TLS adds a second filtering layer.
- One main button starts every enabled protection layer, with live blocked and allowed counters.

## Install

1. Download [BLOKD-v1.1.3.apk](https://github.com/anishfyi/BLOKD/releases/download/v1.1.3/BLOKD-v1.1.3.apk) or browse [all releases](https://github.com/anishfyi/BLOKD/releases/tag/v1.1.3).
2. Open it and allow install from your browser or files app.
3. Launch BLOKD, pick Standard or Berserk, flip the switch, and approve the VPN prompt.

Play Protect may show a standard "unknown developer" notice for any sideloaded app. The APK is release signed, so it installs and updates normally once you continue past that notice.

## Build from source

```
gradle :app:assembleDebug
```

Release builds are signed by CI from repository secrets. See `.github/workflows/release.yml`. To sign locally, copy `keystore.properties.example` to `keystore.properties` and point it at your keystore.

## Limitations

DNS blocking cannot remove ads stitched into a video stream from the same server as the content (server-side ad insertion). It also cannot see apps that use encrypted DNS (DoH/DoT) or hardcoded resolver IPs. Berserk in v1.1.0 means bigger blocklists, not full-tunnel interception. See [limitations](docs/limitations.html) and `docs/ott-sonyliv-findings.md`.

## Star

If BLOKD is useful to you, please star the repo. It genuinely helps.

## License and author

MIT. Author: [anishfyi](https://github.com/anishfyi).
