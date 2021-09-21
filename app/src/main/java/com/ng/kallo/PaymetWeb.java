package com.ng.kallo;


import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.webkit.SslErrorHandler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


public class PaymetWeb extends AppCompatActivity {
    private Context mContext;
    private WebView wvPayment;
    private ImageView IVback;
    private static String url = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paymet_web);
        mContext = PaymetWeb.this;

        String url = "https://kallo.ng/pay/cGate?planid=" + getIntent().getStringExtra("planid") + "&userid=" + getIntent().getStringExtra("userid");
        Log.d("url", url);
        if (getIntent().hasExtra(url)) {
            url = getIntent().getStringExtra(url);

        }

        wvPayment = (WebView) findViewById(R.id.wvPayment);


        WebSettings settings = wvPayment.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        wvPayment.loadUrl(url);

        wvPayment.setWebViewClient(new SSLTolerentWebViewClient());
        init();
    }

    private void init() {
        IVback = (ImageView) findViewById(R.id.IVback);
        IVback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.startAnimation(AnimationUtils.loadAnimation(mContext, R.anim.fade_in_center));
                finish();
            }
        });
    }

    private class SSLTolerentWebViewClient extends WebViewClient {

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            super.onReceivedSslError(view, handler, error);
            // this will ignore the Ssl error and will go forward to your site
            handler.proceed();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            // TODO Auto-generated method stub
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return super.shouldOverrideUrlLoading(view, url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
//            ProjectUtils.pauseProgressDialog();
            //Page load finished
            if (url.equals("https://www.kallo.ng/my-account/subscription")) {
                Toast.makeText(mContext, "Payment successful", Toast.LENGTH_SHORT).show();
                super.onPageFinished(view, "https://www.kallo.ng/my-account/subscription");
                finish();

                wvPayment.clearCache(true);

                wvPayment.clearHistory();

                wvPayment.destroy();


//            else if (url.equals(furl)) {
//                Toast.makeText(mContext, "Payment fail.", Toast.LENGTH_SHORT).show();
//                //view.loadUrl("https://www.youtube.com");
//                super.onPageFinished(view, furl);
//                finish();
//                wvPayment.clearCache(true);
//
//                wvPayment.clearHistory();
//
//                wvPayment.destroy();
            }else {
                super.onPageFinished(view, url);
            }

        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            // TODO Auto-generated method stub
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
    }


}