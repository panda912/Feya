/*
 * Copyright (c) 2017. Kaede (kidhaibara@gmail.com) All Rights Reserved.
 */

package me.kaede.feya.webview;

import android.app.Activity;
import android.os.SystemClock;
import android.webkit.JavascriptInterface;

/**
 * @author Kaede
 * @since date 16/8/22
 */
public class JavaScriptBridge {
    Activity mActivity;

    public JavaScriptBridge(Activity activity) {
        mActivity = activity;
    }

    @JavascriptInterface
    public void closeBrowser() {
        if (mActivity != null) mActivity.finish();
    }

    @JavascriptInterface
    public long getCurrentTime() {
        return SystemClock.elapsedRealtime();
    }

    public void onActivityDestoryed() {
        mActivity = null;
    }
}
