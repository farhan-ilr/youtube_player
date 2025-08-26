# youtube_player

A Youtube player


##  ADD THIS IN THE INFO PLIST

<key>NSAppTransportSecurity</key>
<dict>
  <!-- Allow all HTTPS (needed for YouTube embed URLs) -->
<key>NSAllowsArbitraryLoads</key>
<true/>
<key>NSAllowsArbitraryLoadsInWebContent</key>
<true/>
</dict>

<!-- Allow inline <video> playback instead of forcing fullscreen -->
<key>WebKitAllowsInlineMediaPlayback</key>
<true/>

<!-- Optional: if you want audio to keep playing in background -->
<key>UIBackgroundModes</key>
<array>
<string>audio</string>
</array>

## Getting Started

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/to/develop-plugins),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

