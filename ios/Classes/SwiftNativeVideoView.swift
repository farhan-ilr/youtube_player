import UIKit
import WebKit
import Flutter

// MARK: - The actual platform view implementation
class SwiftNativeVideoView: NSObject, FlutterPlatformView, WKNavigationDelegate, WKUIDelegate {

    private let frame: CGRect
    private let viewId: Int64
    private let webView: WKWebView
    private let progressView: UIProgressView
    private let methodChannel: FlutterMethodChannel
    private var pollingTimer: Timer?
    private var webViewState: Any?
    private var fullscreenContainer: UIView?
    private var customView: UIView?

    init(
        frame: CGRect,
        viewIdentifier viewId: Int64,
        arguments args: [String: Any]?,
        messenger: FlutterBinaryMessenger
    ) {
        self.frame = frame
        self.viewId = viewId

        // Create method channel
        self.methodChannel = FlutterMethodChannel(name: "video_player", binaryMessenger: messenger)

        // Setup WebView configuration
        let configuration = WKWebViewConfiguration()
        configuration.allowsInlineMediaPlayback = true
        if #available(iOS 10.0, *) {
            configuration.mediaTypesRequiringUserActionForPlayback = []
        }

        // User content controller for JS messages
        let userContentController = WKUserContentController()
        configuration.userContentController = userContentController

        // Create WebView and progress bar
        self.webView = WKWebView(frame: frame, configuration: configuration)
        self.progressView = UIProgressView(progressViewStyle: .default)
        self.progressView.frame = CGRect(x: 0, y: 0, width: frame.width, height: 2)

        super.init()

        setupWebView(args: args)
    }

    private func setupWebView(args: [String: Any]?) {
        webView.navigationDelegate = self
        webView.uiDelegate = self

        // Load YouTube URL
        if let url = args?["url"] as? String, let nsUrl = URL(string: url) {
            webView.load(URLRequest(url: nsUrl))
        }

        // Start polling video position
        startPollingVideoPosition()
    }

    func view() -> UIView {
        let containerView = UIView(frame: frame)
        containerView.addSubview(webView)
        containerView.addSubview(progressView)

        webView.translatesAutoresizingMaskIntoConstraints = false
        progressView.translatesAutoresizingMaskIntoConstraints = false

        NSLayoutConstraint.activate([
            webView.topAnchor.constraint(equalTo: containerView.topAnchor),
            webView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            webView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            webView.bottomAnchor.constraint(equalTo: containerView.bottomAnchor),

            progressView.topAnchor.constraint(equalTo: containerView.topAnchor),
            progressView.leadingAnchor.constraint(equalTo: containerView.leadingAnchor),
            progressView.trailingAnchor.constraint(equalTo: containerView.trailingAnchor),
            progressView.heightAnchor.constraint(equalToConstant: 2),
        ])

        return containerView
    }

    // MARK: - WKNavigationDelegate
    func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
        progressView.isHidden = true

        // Inject custom JavaScript
        let jsCode = """
            function hideElements() {
                var elementsToHide = [
                    ".ytp-fullscreen-button",
                     ".ytp-button",
                     ".ytp-large-play-button",
                     ".ytp-large-play-button-red-bg",
                     ".yt-uix-sessionlink",
                     ".ytp-youtube-button",
                     ".ytp-chrome-top",
                     ".ytp-show-cards-title",
                     ".ytp-player-content.videowall-endscreen",
                     ".html5-endscreen",
                     ".ytp-endscreen-paginate.ytp-show-tiles",
                     ".annotation-type-custom",
                     ".annotation",
                     ".iv-branding",
                     ".branding-img-container",
                     ".ytp-title-text",
                     ".ytp-title-channel",
                     ".fullscreen-more-videos-endpoint",
                     ".watch-on-youtube-button-wrapper",
                     ".icon-share_arrow",
                     ".ytmVideoInfoChannelContainer",
                     ".ytmVideoInfoVideoTitleContainer",
                     ".ytmVideoInfoLogoEnabled",
                     ".ytmVideoInfoVideoTitle",
                     ".ytmVideoInfoChannelTitle",
                     ".fullscreen-action-menu"
                ];
                elementsToHide.forEach(selector => {
                    document.querySelectorAll(selector).forEach(el => el.style.display = 'none');
                });
            }
            setInterval(hideElements, 500);

            var video = document.querySelector('video');
            if (video) {
                video.addEventListener('timeupdate', function() {
                    window.webkit.messageHandlers.iOS.postMessage({
                        action: "onPositionChanged",
                        position: video.currentTime
                    });
                });
            }
        """

        webView.evaluateJavaScript(jsCode) { (_, error) in
            if let error = error {
                print("JS Injection Error: \(error)")
            }
        }
    }

    // MARK: - WKUIDelegate
    func webView(
        _ webView: WKWebView,
        createWebViewWith configuration: WKWebViewConfiguration,
        for navigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures
    ) -> WKWebView? {
        return enterFullscreen(with: configuration)
    }

    private func enterFullscreen(with configuration: WKWebViewConfiguration) -> WKWebView? {
        guard let rootViewController = UIApplication.shared.windows.first?.rootViewController else {
            return nil
        }

        fullscreenContainer = UIView(frame: rootViewController.view.bounds)
        guard let fullscreenContainer = fullscreenContainer else { return nil }

        let fullscreenWebView = WKWebView(frame: fullscreenContainer.bounds, configuration: configuration)
        fullscreenWebView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        fullscreenWebView.navigationDelegate = self
        fullscreenWebView.uiDelegate = self

        fullscreenContainer.addSubview(fullscreenWebView)
        rootViewController.view.addSubview(fullscreenContainer)

        webView.isHidden = true
        customView = fullscreenWebView

        return fullscreenWebView
    }

    func exitFullscreen() {
        fullscreenContainer?.removeFromSuperview()
        fullscreenContainer = nil
        customView = nil
        webView.isHidden = false
    }

    // MARK: - Video position polling
    private func startPollingVideoPosition() {
        pollingTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            guard let self = self else { return }
            self.webView.evaluateJavaScript("document.querySelector('video')?.currentTime?.toString() ?? '0'") {
                (result, _) in
                if let positionString = result as? String, let position = Double(positionString) {
                    self.methodChannel.invokeMethod("updatePosition", arguments: position)
                }
            }
        }
    }

    func dispose() {
        pollingTimer?.invalidate()
        pollingTimer = nil
        webView.stopLoading()
        if #available(iOS 14.0, *) {
            webView.configuration.userContentController.removeAllScriptMessageHandlers()
        }
    }
}
