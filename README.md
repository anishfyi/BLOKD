<div align="center">
  <img src="docs/logo.svg" alt="BLOKD" width="96" height="96" />
  <h1>BLOKD</h1>
  <p><b>A no-root Android ad and tracker blocker.</b></p>
</div>

It runs a local VPN on your device and answers DNS for known ad and tracker domains with NXDOMAIN, so those hosts never load, across every app and browser.

## Pointers

- No root, no account, no traffic leaves your device except normal DNS lookups.
- Blocks ad and tracker domains system wide using public block lists (HaGeZi and OISD), updatable in app.
- Optional encrypted AdGuard DNS-over-TLS adds a second filtering layer.
- One main button starts every enabled protection layer, with live blocked and allowed counters.

## Install

1. Download the latest signed `BLOKD-vX.Y.Z.apk` from [Releases](https://github.com/anishfyi/BLOKD/releases/latest).
2. Open it and allow install from your browser or files app.
3. Launch BLOKD, flip the switch, and approve the VPN prompt Android shows.

Play Protect may show a standard "unknown developer" notice for any sideloaded app. The APK is release signed, so it installs and updates normally once you continue past that notice.

## Build from source

```
gradle :app:assembleDebug
```

Release builds are signed by CI from repository secrets. See `.github/workflows/release.yml`. To sign locally, copy `keystore.properties.example` to `keystore.properties` and point it at your keystore.

## Limitations

DNS blocking cannot remove ads that are stitched into a video stream from the same server as the content (server side ad insertion), which is what most large streaming apps now use for in stream ads. It also cannot see apps that use encrypted DNS (DoH or DoT) or hardcoded resolver IPs. See `docs/ott-sonyliv-findings.md`.

## Star

If BLOKD is useful to you, please star the repo. It genuinely helps.

## License and author

MIT. Author: [anishfyi](https://github.com/anishfyi).

Site: https://anishfyi.github.io/BLOKD/
