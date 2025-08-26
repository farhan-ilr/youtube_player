import Flutter
import UIKit

public class YoutubePlayerPlugin: NSObject, FlutterPlugin {
    public static func register(with registrar: FlutterPluginRegistrar) {
        let factory = SwiftNativeVideoViewFactory(messenger: registrar.messenger())
        registrar.register(factory, withId: "native_video_view")
    }
}
