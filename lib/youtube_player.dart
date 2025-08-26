import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class YoutubePlayerWeb extends StatelessWidget {
  final String url;

  const YoutubePlayerWeb({Key? key, required this.url}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    if (defaultTargetPlatform == TargetPlatform.android) {
      return AndroidView(viewType: "youtube_player_web_view", creationParams: {"url": url}, creationParamsCodec: const StandardMessageCodec());
    } else if (defaultTargetPlatform == TargetPlatform.iOS) {
      return UiKitView(viewType: "native_video_view", creationParams: {"url": url}, creationParamsCodec: const StandardMessageCodec());
    }
    return Text("YoutubePlayerWeb is not supported on this platform.");
  }

  Future<String?> getPlatformVersion() async {
    return "1.0.0";

  }
}
