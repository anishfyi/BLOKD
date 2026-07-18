# BLOKD v1.2.0 - Anti-bypass (tougher blocking)

Date: 2026-07-19
Author: anishfyi

## Problem

A DNS blocker can only act on DNS it sees. After the v1.1.x fallback fix,
ads still get through in two situations:

1. **Encrypted DNS by the app/browser.** Chrome "Secure DNS", Firefox DoH, and
   some apps send DNS-over-HTTPS/TLS straight to their own resolver on port
   443/853. BLOKD never sees those lookups.
2. **Hardcoded plaintext resolver IPs.** An app that sends DNS straight to
   `8.8.8.8:53` bypasses BLOKD, because the TUN only routes BLOKD's sentinel DNS
   address into the tunnel.

## Goal

Close both holes so far more DNS is forced through BLOKD's filter, matching what
AdGuard/NextDNS do. Ship as v1.2.0. Same-domain / SSAI ads remain out of scope
(unbeatable at the DNS layer, already documented).

## Component 1: Block DoH/DoT bootstraps (`block/`)

- Add an always-on `BlocklistSources.ANTI_BYPASS` constant: curated well-known
  encrypted-DNS endpoint hostnames (`dns.google`, `cloudflare-dns.com` [parent
  match covers `chrome.`/`mozilla.`/`security.`/`family.`/`1dot1dot1dot1.`],
  `one.one.one.one`, Quad9 `dns.quad9.net` + numbered, `doh.opendns.com`,
  `dns.nextdns.io`, `doh.dns.sb`, ControlD, plus the Firefox canary
  `use-application-dns.net` whose NXDOMAIN makes Firefox auto-disable DoH).
  BLOKD's own upstream hostnames (AdGuard, Mullvad) are deliberately excluded -
  they are reached by IP, and they are filtering resolvers, not a bypass.
- `BlocklistRepository.apply()` unions `ANTI_BYPASS` into the block set on every
  path (bundled, cached, updated), so it works offline and on first run.
- Remove the DoH endpoints (`dns.google`, `one.one.one.one`, `cloudflare-dns.com`)
  from `CONNECTIVITY_ALLOW` - the allow list wins in `DnsFilter`, so leaving them
  would defeat the feature. Keep the real captive-portal hosts.
- Add HaGeZi `doh-onlydomains.txt` as a best-effort remote source in both modes,
  wrapped in the existing per-source `runCatching` so a bad URL is harmless. The
  bundled seed is authoritative; the remote is a bonus.

Effect: an app that tries its own DoH gets NXDOMAIN on the bootstrap and falls
back to plaintext DNS, which BLOKD filters.

## Component 2: Intercept hardcoded plaintext DNS (`vpn/`)

- Add `BlokdVpnService.PUBLIC_DNS_V4` / `PUBLIC_DNS_V6` constants (Google,
  Cloudflare, Quad9, OpenDNS) and `addRoute()` each into the TUN in
  `startTunnel()`.
- Those UDP:53 queries now enter the tunnel; `PacketProcessor` (already handles
  UDP:53 to any destination and swaps src/dst on the reply) re-resolves them
  through the encrypted upstream. BLOKD's own upstream sockets are `protect()`-ed
  so they bypass the tunnel - no resolver loop.
- Bonus: non-DNS traffic to these IPs (e.g. DoH over 443, DoT over 853 to
  `1.1.1.1`/`8.8.8.8`) enters the tunnel and is dropped by `PacketProcessor`
  (it only answers UDP:53), which further blocks DoH/DoT aimed at a known IP.
  Trade-off: the `1.1.1.1` / `8.8.8.8` marketing pages become unreachable while
  protection is on. Acceptable and on-theme.
- Do not route AdGuard (`94.140.x`) or Mullvad (`194.242.2.x`) - BLOKD's own
  upstreams.

## Component 3: Release

- `versionCode` 7 -> 8, `versionName` 1.1.3 -> 1.2.0.
- README "What's new in v1.2.0" + limitations refresh.
- Bundles the already-tested v1.1.x encrypted-only fallback fix
  (`ResolverChain` / `ResolverTier`, Mullvad DoT fallback, `DEGRADED` health).
- Commit on `feature/anti-bypass-v1.2.0`, merge to `main`, tag `v1.2.0`, push;
  `release.yml` builds and publishes the signed APK.

## Testing (TDD)

Pure-JVM, unit-testable:
- `DnsFilter` blocks every `ANTI_BYPASS` host and their subdomains (parent match).
- `CONNECTIVITY_ALLOW` contains no encrypted-DNS endpoint.
- Firefox canary `use-application-dns.net` is blocked.

Component 2 routing is Android `VpnService.Builder` glue, verified by the full
`gradle :app:testDebugUnitTest` + `assembleRelease` build.

## Non-goals

Same-domain / SSAI ad stripping, per-app rules, a full DoH client, blocking
VPN/proxy/TOR bypass (only encrypted DNS), MITM.
