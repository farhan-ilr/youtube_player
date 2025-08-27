# youtube_player

A Flutter plugin to play YouTube videos using a native WebView (iOS + Android).

## Features
- Play YouTube videos inside Flutter
- Hide YouTube branding and controls
- Track playback position

## Installation
Add this to your `pubspec.yaml`:
```yaml
dependencies:
  youtube_player: ^0.0.1


import 'package:youtube_player/youtube_player.dart';

YoutubePlayerView(
  url: "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  onPositionChanged: (position) {
    print("Video position: $position");
  },
)


<uses-permission android:name="android.permission.INTERNET"/>
