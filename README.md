# Velora

A clean, modern media player for Android. Plays audio and video from your local storage with a liquid-glass UI, synchronized lyrics, playlist management.

---

## Features

### Playback
- Audio and video playback powered by **ExoPlayer / Media3**
- Background playback with a persistent **foreground notification** (Media Session)
- Adjustable **playback speed** (0.25× – 2×)
- Configurable **skip interval** (skip forward / back)
- **Shuffle** mode and manual **queue** management
- Lock-screen / wake-lock support — keeps playing with the screen off

### Library
- Scans device storage for all audio and video files
- Filter by **All / Audio / Video / Playlists**
- **Favorites** playlist (built-in, heart-button toggle with animated splash)
- Create, rename, merge, and delete custom playlists
- Long-press any item to add it to a playlist, hide it from the library, or remove an imported file
- **Hidden items** — remove clutter without deleting files

### ZIP Import
- Open a `.zip` file to bulk-import audio/video into a new or existing playlist
- Zip is unpacked to app-private storage; embedded album art and metadata are preserved

### Lyrics
- Synchronized lyrics via sidecar **`.lrc`** or **`.srt`** files placed next to the media file
- Auto-detected at playback start; active line highlighted in real time

### UI / Design
- **Liquid Glass** surface components — frosted-glass cards with blur and shimmer effects
- **Material You** dynamic colour theming (toggleable via Settings)
- Full **landscape / portrait** support with `fullSensor` orientation
- Real-time **waveform visualiser** using the Android `Visualizer` API
- Album art / video thumbnail extraction via Coil

### Settings
- Toggle Material You theming
- Adjust skip interval
- App version info

---
