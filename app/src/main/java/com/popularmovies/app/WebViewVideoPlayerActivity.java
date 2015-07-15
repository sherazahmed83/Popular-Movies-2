package com.popularmovies.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

/**
 * Created by Sheraz on 7/15/2015.
 */
public class WebViewVideoPlayerActivity extends Activity {
    public static final int USER_MOBILE = 0;
    public static final int USER_DESKTOP = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_webview);
        String videoId = getIntent().getStringExtra(MovieDetailsActivityFragment.MOVIE_ID);
        final WebView video = (WebView) findViewById(R.id.videoView);
        video.getSettings().setJavaScriptEnabled(true);
        video.getSettings().setPluginState(WebSettings.PluginState.ON);
//                video.getSettings().setUserAgent(USER_MOBILE);
        video.setWebChromeClient(new WebChromeClient() {
        });

//youtube video url
////http://www.youtube.com/watch?v=WM5HccvYYQg

        final String mimeType = "text/html";
        final String encoding = "UTF-8";
        String html = Utility.getVideoHTML(videoId);
        video.loadDataWithBaseURL("", html, mimeType, encoding, "");
    }

}
