package com.youtube.farhan.youtube_player_native

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.*
import android.webkit.*
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import kotlinx.coroutines.*

class NativeVideoView(
    context: Context,
    messenger: BinaryMessenger,
    id: Int,
    creationParams: Map<String?, Any?>?
) : PlatformView {

    private val view: View = LayoutInflater.from(context).inflate(R.layout.native_video_view, null, false)
    private val webView: WebView = view.findViewById(R.id.webView)
    private val progressBar: ProgressBar = view.findViewById(R.id.loadingIndicator)
    private val methodChannel = MethodChannel(messenger, "video_player")
    private val fullscreenContainer = FrameLayout(context)
    private val mainLayout: FrameLayout = FrameLayout(context)
    private var customView: View? = null
    private var webViewBundle: Bundle? = null
    private var playingPositionJob: Job? = null

    init {
        setupWebView(context, creationParams)
        startPollingVideoPosition()
    }

    private fun setupWebView(context: Context, creationParams: Map<String?, Any?>?) {
        webView.settings.javaScriptEnabled = true
        webView.settings.mediaPlaybackRequiresUserGesture = false

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onPositionChanged(position: Double) {
                CoroutineScope(Dispatchers.Main).launch {
                    methodChannel.invokeMethod("onPositionChanged", position)
                }
            }
        }, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                injectJavaScript()
                progressBar.visibility = View.GONE
                webView.visibility = View.VISIBLE
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                if (customView != null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                fullscreenContainer.addView(view)
                fullscreenContainer.visibility = View.VISIBLE
                webView.visibility = View.GONE

                val activity = context as? AppCompatActivity
                activity?.supportActionBar?.hide()
                activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                activity?.window?.decorView?.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_FULLSCREEN)
            }

            override fun onHideCustomView() {
                customView?.let {
                    fullscreenContainer.removeView(it)
                    customView = null
                }
                fullscreenContainer.visibility = View.GONE

                val heightInDp = 250
                val heightInPixels = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, heightInDp.toFloat(), view.resources.displayMetrics
                ).toInt()
                mainLayout.layoutParams.height = heightInPixels
                webView.visibility = View.VISIBLE

                val activity = context as? AppCompatActivity
                activity?.supportActionBar?.show()
                activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                activity?.window?.decorView?.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            }
        }

        webViewBundle?.let { webView.restoreState(it) }
            ?: webView.loadUrl(creationParams?.get("url") as? String ?: "")
    }

    private fun injectJavaScript() {
        val jsCode = """
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
                    ".ytp-chrome-top-buttons",
                    ".ytp-gradient-bottom",
                    ".ytp-svg-shadow",
                    ".fullscreen-more-videos-endpoint",
                    ".watch-on-youtube-button-wrapper",
                    ".icon-share_arrow",
                    ".ytmVideoInfoChannelContainer",
                    ".ytmVideoInfoVideoTitleContainer",
                    ".ytmVideoInfoLogoEnabled",
                    ".ytmVideoInfoVideoTitle",
                    ".ytmVideoInfoChannelTitle",
                    ".fullscreen-action-menu",
                    ".fullscreen-icon"
                ];
                
                elementsToHide.forEach(selector => {
                    document.querySelectorAll(selector).forEach(el => {
                        el.style.display = 'none';
                    });
                });
                
                // Hide specific menu items
                var menuItems = document.querySelectorAll(".ytp-menuitem");
                if (menuItems.length > 4) {
                    if (menuItems[4]) menuItems[4].style.display = 'none';
                    if (menuItems[5]) menuItems[5].style.display = 'none';
                }
            }

            function hidePauseOverlay() {
                var overlays = document.getElementsByClassName("ytp-pause-overlay-container");
                for (var i = 0; i < overlays.length; i++) {
                    overlays[i].style.display = 'none';
                }
            }

            var video = document.querySelector('video');
            if (video) {
                video.addEventListener('timeupdate', function() {
                    Android.onPositionChanged(video.currentTime);
                });
                video.addEventListener('pause', hidePauseOverlay);
                video.addEventListener('play', function() {
                    setTimeout(hidePauseOverlay, 500);
                    setInterval(hideElements, 500);
                });
            }

            hideElements();
            hidePauseOverlay();
        """.trimIndent()

        webView.evaluateJavascript(jsCode) { result ->
            println("JavaScript injection result: $result")
        }
    }
    private fun startPollingVideoPosition() {
        playingPositionJob = CoroutineScope(Dispatchers.Main).launch {
            while (true) {
                try {
                    webView.evaluateJavascript("document.querySelector('video')?.currentTime.toString();") { result ->
                        result?.toDoubleOrNull()?.let {
                            methodChannel.invokeMethod("updatePosition", it)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(500)
            }
        }
    }

    fun saveState() {
        webViewBundle = Bundle()
        webView.saveState(webViewBundle!!)
    }

    fun restoreState() {
        webViewBundle?.let { webView.restoreState(it) }
    }

    override fun getView(): View = view

    override fun dispose() {
        playingPositionJob?.cancel()
    }
}
class NativeVideoViewFactory(private val messenger: BinaryMessenger) :
    PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, id: Int, creationParams: Any?): PlatformView {
        return NativeVideoView(context, messenger, id, creationParams as Map<String?, Any?>?)
    }
}