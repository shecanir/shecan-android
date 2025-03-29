package ir.shecan.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import ir.shecan.BuildConfig;
import ir.shecan.Shecan;
import ir.shecan.R;

/**
 * Shecan Project
 *
 * @author iTX Technologies
 * @link https://itxtech.org
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
public class TestDomainFragment extends ToolbarFragment {
    private WebView mWebView = null;

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled", "addJavascriptInterface"})
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_domain_test, container, false);

        mWebView = new WebView(Shecan.getInstance());
        ((ViewGroup) view.findViewById(R.id.fragment_domain_test)).addView(mWebView);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setBackgroundColor(0);
        mWebView.addJavascriptInterface(this, "JavascriptInterface");

        mWebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                return true;
            }
        });

        mWebView.loadUrl("https://shecan.ir");

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {//for better compatibility
                if (!url.startsWith("https://shecan.ir")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                try {
                    mWebView.loadUrl("javascript:changeVersionInfo('" + Shecan.getInstance().getPackageManager().getPackageInfo(Shecan.getInstance().getPackageName(), 0).versionName + "', '" + BuildConfig.BUILD_TIME + "', '" + BuildConfig.GIT_COMMIT + "')");
                } catch (Exception e) {
                    Log.e("DAboutActivity", e.toString());
                }
            }
        });
        return view;
    }

    @Override
    public void checkStatus() {
        menu.findItem(R.id.nav_about).setChecked(true);
        toolbar.setTitle(R.string.action_about);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mWebView != null) {
            Log.d("DAboutActivity", "onDestroy");

            mWebView.removeAllViews();
            mWebView.setWebViewClient(null);
            ((ViewGroup) mWebView.getParent()).removeView(mWebView);
            mWebView.setTag(null);
            mWebView.clearHistory();
            mWebView.destroy();
            mWebView = null;
        }
    }
}
