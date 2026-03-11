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

public class NfcHandler implements NfcAdapter.CreateNdefMessageCallback {
    final Controller mController;
    Tab mCurrentTab;
    final Handler mHandler = new Handler(this) {
        final NfcHandler this$0;

        {
            this.this$0 = this;
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 100) {
                this.this$0.mIsPrivate = this.this$0.mCurrentTab.getWebView().isPrivateBrowsingEnabled();
                this.this$0.mPrivateBrowsingSignal.countDown();
            }
        }
    };
    boolean mIsPrivate;
    CountDownLatch mPrivateBrowsingSignal;

    public NfcHandler(Controller controller) {
        this.mController = controller;
    }

    public static void register(Activity activity, Controller controller) {
        NfcAdapter defaultAdapter = NfcAdapter.getDefaultAdapter(activity.getApplicationContext());
        if (defaultAdapter == null) {
            return;
        }
        defaultAdapter.setNdefPushMessageCallback(controller != null ? new NfcHandler(controller) : null, activity, new Activity[0]);
    }

    public static void unregister(Activity activity) {
        register(activity, null);
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent nfcEvent) {
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
