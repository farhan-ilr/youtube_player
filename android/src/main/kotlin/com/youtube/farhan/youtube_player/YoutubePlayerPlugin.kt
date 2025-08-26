package com.youtube.farhan.youtube_player

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger

class YoutubePlayerPlugin: FlutterPlugin {
  private lateinit var messenger: BinaryMessenger

  override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    messenger = binding.binaryMessenger
    binding
      .platformViewRegistry
      .registerViewFactory(
        "youtube_player_web_view",
        NativeVideoViewFactory(messenger)
      )
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {}
}

