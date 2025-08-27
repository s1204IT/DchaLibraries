package com.android.browser;

import android.app.Activity;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import java.util.concurrent.CountDownLatch;

/* loaded from: classes.dex */
public class NfcHandler implements NfcAdapter.CreateNdefMessageCallback {
    final Controller mController;
    Tab mCurrentTab;
    final Handler mHandler = new Handler() { // from class: com.android.browser.NfcHandler.1
        AnonymousClass1() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (message.what == 100) {
                NfcHandler.this.mIsPrivate = NfcHandler.this.mCurrentTab.getWebView().isPrivateBrowsingEnabled();
                NfcHandler.this.mPrivateBrowsingSignal.countDown();
            }
        }
    };
    boolean mIsPrivate;
    CountDownLatch mPrivateBrowsingSignal;

    public static void register(Activity activity, Controller controller) {
        NfcAdapter defaultAdapter = NfcAdapter.getDefaultAdapter(activity.getApplicationContext());
        if (defaultAdapter == null) {
            return;
        }
        NfcHandler nfcHandler = null;
        if (controller != null) {
            nfcHandler = new NfcHandler(controller);
        }
        defaultAdapter.setNdefPushMessageCallback(nfcHandler, activity, new Activity[0]);
    }

    public static void unregister(Activity activity) {
        register(activity, null);
    }

    public NfcHandler(Controller controller) {
        this.mController = controller;
    }

    /* renamed from: com.android.browser.NfcHandler$1 */
    class AnonymousClass1 extends Handler {
        AnonymousClass1() {
        }

        @Override // android.os.Handler
        public void handleMessage(Message message) {
            if (message.what == 100) {
                NfcHandler.this.mIsPrivate = NfcHandler.this.mCurrentTab.getWebView().isPrivateBrowsingEnabled();
                NfcHandler.this.mPrivateBrowsingSignal.countDown();
            }
        }
    }

    @Override // android.nfc.NfcAdapter.CreateNdefMessageCallback
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) throws InterruptedException {
        String url;
        this.mCurrentTab = this.mController.getCurrentTab();
        if (this.mCurrentTab != null && this.mCurrentTab.getWebView() != null) {
            this.mPrivateBrowsingSignal = new CountDownLatch(1);
            this.mHandler.sendMessage(this.mHandler.obtainMessage(100));
            try {
                this.mPrivateBrowsingSignal.await();
            } catch (InterruptedException e) {
                return null;
            }
        }
        if (this.mCurrentTab == null || this.mIsPrivate || (url = this.mCurrentTab.getUrl()) == null) {
            return null;
        }
        try {
            return new NdefMessage(NdefRecord.createUri(url), new NdefRecord[0]);
        } catch (IllegalArgumentException e2) {
            Log.e("BrowserNfcHandler", "IllegalArgumentException creating URI NdefRecord", e2);
            return null;
        }
    }
}
