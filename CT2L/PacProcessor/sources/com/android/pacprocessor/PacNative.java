package com.android.pacprocessor;

import android.util.Log;

public class PacNative {
    private boolean mIsActive;

    private native boolean createV8ParserNativeLocked();

    private native boolean destroyV8ParserNativeLocked();

    private native String makeProxyRequestNativeLocked(String str, String str2);

    private native boolean setProxyScriptNativeLocked(String str);

    static {
        System.loadLibrary("jni_pacprocessor");
    }

    PacNative() {
    }

    public synchronized boolean startPacSupport() {
        boolean z = true;
        synchronized (this) {
            if (createV8ParserNativeLocked()) {
                Log.e("PacProxy", "Unable to Create v8 Proxy Parser.");
            } else {
                this.mIsActive = true;
                z = false;
            }
        }
        return z;
    }

    public synchronized boolean stopPacSupport() {
        boolean z = false;
        synchronized (this) {
            if (this.mIsActive) {
                if (destroyV8ParserNativeLocked()) {
                    Log.e("PacProxy", "Unable to Destroy v8 Proxy Parser.");
                    z = true;
                } else {
                    this.mIsActive = false;
                }
            }
        }
        return z;
    }

    public synchronized boolean setCurrentProxyScript(String script) {
        boolean z;
        if (setProxyScriptNativeLocked(script)) {
            Log.e("PacProxy", "Unable to parse proxy script.");
            z = true;
        } else {
            z = false;
        }
        return z;
    }

    public synchronized String makeProxyRequest(String url, String host) {
        String ret;
        ret = makeProxyRequestNativeLocked(url, host);
        if (ret == null || ret.length() == 0) {
            Log.e("PacProxy", "v8 Proxy request failed.");
            ret = null;
        }
        return ret;
    }
}
