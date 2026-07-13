# BLOKD design

Date: 2026-07-12
Author: anishfyi

## What it is

A no-root Android ad and tracker blocker, in two parts:

- Module A (ships): a DNS-filtering local VPN that blocks ad and tracker domains system wide.
- Module B (research spike): an investigation into how SonyLIV serves its ads, to see if any are blockable.

## Why

Paid OTT subscriptions still serve ads. Part of that is plan tier (several OTT paid tiers include ads by design), and part is server side ad insertion that no on-device blocker can strip. This project builds the blocker that is genuinely achievable, and records honestly what is not.

## Module A: DNS-filtering core

Native Kotlin app using Android `VpnService`, no root.

Flow:

1. `BlokdVpnService` stands up a TUN interface, registers a sentinel DNS address (`10.111.222.3`), and routes only that address into the tunnel. All other traffic flows normally, so no full IP stack is needed.
2. `PacketProcessor` parses each captured IPv4 UDP DNS packet, reads the queried name, and dispatches.
3. `DnsFilter` checks the name (and its parent domains) against the block set and the allow set. Blocked names get a locally built NXDOMAIN reply. Allowed names are forwarded by `UpstreamResolver` over a `protect()`-ed socket to 1.1.1.1.
4. `BlocklistRepository` seeds a small built-in list on first run, and updates from HaGeZi and OISD on demand, caching to disk.
5. Compose UI: one toggle (fires the VPN consent prompt), live blocked and allowed counters, and an allow list.

Components and responsibilities:

- `DnsCodec` (pure JVM): extract question name, build NXDOMAIN reply.
- `DnsFilter` (pure JVM): block or allow decision with parent-domain matching, atomic set swaps for lock-free reads.
- `HostsParser` (pure JVM): parse hosts and plain-domain lists.
- `UpstreamResolver`: forward allowed queries upstream over UDP.
- `PacketProcessor`: IPv4 UDP framing, checksums, request-to-reply.
- `BlocklistRepository`: sources, caching, allow list persistence.
- `StatsCounter` and `VpnController`: counters and observable UI state.
- `BlokdVpnService`: lifecycle, TUN, foreground notification, threads.
- `MainActivity` and `BlokdApp`: consent flow and UI.

Stated limits: no effect on encrypted DNS (DoH or DoT), hardcoded resolver IPs, or SSAI. On "undetectable": at the DNS layer a blocked host looks like an offline host, and the app uses no root and announces nothing. Some apps notice a failed ad call and nag anyway, which DNS blocking cannot fix.

## Module B: SonyLIV spike

Best-effort, likely blocked on a stock phone by Play Integrity, native pinning, or Widevine DRM. Documented in `docs/ott-sonyliv-findings.md`. Deliverable is that findings doc, plus any separable ad hostnames folded into the block list.

## Build, sign, release

- Kotlin, Jetpack Compose, Gradle KTS, minSdk 26, target and compile SDK 35, AGP 8.7, Kotlin 2.0.21.
- Release builds are signed from `keystore.properties` (local) or CI secrets. GitHub Actions builds and publishes a signed APK to a GitHub release on any `v*` tag.
- License MIT, author anishfyi. GitHub Pages served from `docs/`.

## Testing

TDD on the pure logic: `DnsCodec`, `DnsFilter`, `HostsParser`. `VpnService` gets a manual instrumented smoke test (toggle on, confirm a known ad domain returns NXDOMAIN).

## v1 non-goals

Connection-level firewall, per-app rules, DoH client, IPv6 handling beyond basic, iOS, Play Store publishing, any productionized MITM.
