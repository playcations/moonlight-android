# Moonlight Dev build

This fork combines two focused branches based on official Moonlight Android
v12.2 (upstream b48494cb96bff23d8886c4775cc4f39a1075495d):

- `feature/aaudio-playback`: opt-in stereo AAudio and AudioTrack fallback.
- `feature/aaudio-route-recovery`: bounded output-disconnection recovery, stacked
  on the playback branch. Review its diff against the playback branch.
- `dev`: merges both, plus development-only app identity changes.

The non-root debug APK is **Moonlight Dev**, package
`com.playcations.moonlight.dev`, version `12.2-aaudio-dev.1`. It installs separately
from official Moonlight and the previous Moonlight Audio Test application.
Pairing and preferences must be configured separately. It is not an official
Moonlight release and does not contain the earlier packet/audio trace patches.

## Build

Initialize submodules, install upstream's Android SDK/NDK requirements, and run:

```sh
git submodule update --init --recursive
./gradlew --no-daemon --max-workers=2 :app:testNonRootDebugUnitTest :app:assembleNonRootDebug :app:lintNonRootDebug
```

Output: `app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk`.
The APK is signed with the builder's local Android debug key. Retain that key for
compatible upgrades of this development installation; do not commit signing keys.
The source commit and an APK SHA-256 should accompany any shared build.

## Use

Pair with the existing Sunshine host. In Audio Settings enable **Use experimental
low-latency audio**, choose stereo and disable system equalizer support. Start
with H.264, 1080p60 and 10 Mbps for comparison with the prior TV setup. Disabling
the new setting uses AudioTrack. No server changes or firmware modifications
are required.

## Validation limits

Local compilation, native builds for all four ABIs, 14 lifecycle/fallback unit
tests and Android lint passed for the combined feature code. The packaged dev
build must also pass the same checks before distribution. These are not audio
hardware or end-to-end latency tests. No installed devices were accessed during
this work. The prior prototype's listening results do not prove identical
behavior after this cleanup and upstream upgrade. Reconnect/output-route and
sustained-play testing remain necessary before an upstream PR is ready.

Authorship and renderer details: [AAudio](aaudio.md).
