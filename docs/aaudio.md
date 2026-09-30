# Experimental AAudio playback

Enable **Audio Settings → Use experimental low-latency audio** to try native
AAudio stereo playback on Android 8 or newer. This is off by default. System
equalizer support must be disabled. Surround configurations and unavailable or
failed AAudio setup/start use the existing AudioTrack renderer.

This is based on Oliver Donnellan's native AAudio proposal, commit
`fa0a309513bc48bf95a9a01ee3b2a14be9a1e04e`:
https://github.com/ClassicOldSong/moonlight-android/pull/567

The port adds explicit opt-in, a stereo guard, start-failure fallback and
serialized lifecycle handling. It does not force HDMI over the system-selected
output. Closing the native stream precedes releasing callback storage. A bounded
ring discards old samples on overflow and fills silence on starvation; a small
buffer is not a guarantee of glitch-free or synchronized playback.

Earlier locally modified v12.1 builds on Fire TV and onn measured shorter native
audio output queues and eventually achieved user-reported near-sync. Receiver
restart and settling affected those observations. They do not validate this
refactored v12.2 port, surround sound, or all output routes.

Unit tests cover backend selection and lifecycle/fallback behavior. Before
upstream submission, hardware validation must include repeated sessions, output
changes, Bluetooth/USB, effects and surround fallback. Disable the option to use
stock audio behavior. No server changes are required.
