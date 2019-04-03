package com.yaninfo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    private WebView mWebView;
    private Button mRefresh;
    private Button mBack;
    private TextView mText;
    private WebSettings mSetting;
    private String mUrl;

    private String downloadUrl = "http://s1.music.126.net/download/android/CloudMusic_2.8.1_official_4.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        mWebView.loadUrl("https://www.wandoujia.com/");
        mWebView.getSettings().setJavaScriptEnabled(true);

        // 设置点击链接，在本页面打开
        mWebView.setWebViewClient(new WebViewClient(){

            // 错误时自定义页面
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
            }
        });

        // 改标题
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);

                mText.setText(title);
            }
        });

        // 下载监听
        mWebView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                Log.e("####对应的url####", "" + url);
                // 启动线程
                // 截取url，这里是根据豌豆荚的特点来截取的
                int i = url.indexOf('?');
                mUrl = url.substring(0, i);
                 if (mUrl.endsWith(".apk")) {
                     DownThread downThread = new DownThread(mUrl);
                     Thread thread = new Thread(downThread);
                     thread.start();
                 }
            }
        });

        // 按钮监听
        mRefresh.setOnClickListener(new MyListener());
        mBack.setOnClickListener(new MyListener());

    }

    /**
     * 按钮监听
     */
    private class MyListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {

            switch (v.getId()) {
                case R.id.refresh:
                    mWebView.reload();
                    break;
                case R.id.back:
                    // 清除缓存
                    mSetting.setCacheMode(WebSettings.LOAD_NO_CACHE);
                    finish();
                    break;
                default:
                    break;
            }

        }
    }

    /**
     * 初始化控件
     */
    void init() {
        mWebView = findViewById(R.id.webView);
        mRefresh = findViewById(R.id.refresh);
        mBack = findViewById(R.id.back);
        mText = findViewById(R.id.text);
    }

}
