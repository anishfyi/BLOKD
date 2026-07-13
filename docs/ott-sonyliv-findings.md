# OTT ad delivery: SonyLIV investigation

Status: not yet run. Blocked on a test rig (stock phone only, see below).

## Goal

Determine how SonyLIV delivers its in stream ads on Android, so we know whether any of them are blockable at the DNS layer.

Two possibilities:

1. Separate domain ads. Ad segments come from ad hostnames distinct from the content CDN. These are blockable, and any such hostnames get added to the block list.
2. Server side ad insertion (SSAI). Ad segments are stitched into the same manifest and served from the same CDN and hostname as the show. These are not blockable on device, because an ad segment is indistinguishable from a content segment to a network filter.

## Method (best effort on a non rooted phone)

1. Decompile the SonyLIV APK with apktool.
2. Add a `network_security_config.xml` that trusts user added CAs and reference it from the manifest.
3. Attempt to neutralize OkHttp certificate pinning in smali.
4. Re-sign with `apksigner` and install.
5. Run mitmproxy with its CA installed as a user certificate and record traffic while an ad plays.
6. If traffic is captured, inspect the HLS or DASH manifest for ad markers (`EXT-X-DISCONTINUITY`, SCTE-35, `CUE-OUT` and `CUE-IN`) and compare ad segment hostnames against content segment hostnames.

## Expected blockers on a stock phone

This most likely does not run to completion, for concrete reasons:

- Play Integrity or SafetyNet. Re-signing changes the APK signature, so the integrity verdict fails and the app refuses to play video. Stop here.
- Native pinning. If the pin lives in native code rather than OkHttp, apktool cannot strip it and the TLS handshake to the content servers fails. Stop here.
- Widevine DRM. A modified or re-signed app may drop below the DRM security level the stream requires, so playback fails independent of any ad question.

Each stop condition is a finding in itself: it means observation is not possible on a stock, non rooted device, which is the honest answer to record.

## If it does run

- If ads come from a separable hostname, add those hostnames to the seed block list in `BlocklistRepository` and to the app block lists. Cheap win.
- If ads are SSAI, document that they are unbeatable on device and note that the only reliable path to no ads is the platform's ad free tier. Some SonyLIV and JioHotstar paid tiers include ads by design.

## Reproducing later

A rooted device or rooted emulator plus Frida bypasses the pinning and integrity problems and is the realistic way to actually observe this. That is the recommended next step if the investigation is picked back up.
