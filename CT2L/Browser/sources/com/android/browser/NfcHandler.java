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
    final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 100) {
                NfcHandler.this.mIsPrivate = NfcHandler.this.mCurrentTab.getWebView().isPrivateBrowsingEnabled();
                NfcHandler.this.mPrivateBrowsingSignal.countDown();
            }
        }
    };
    boolean mIsPrivate;
    CountDownLatch mPrivateBrowsingSignal;

    public static void register(Activity activity, Controller controller) {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity.getApplicationContext());
        if (adapter != null) {
            NfcHandler handler = null;
            if (controller != null) {
                handler = new NfcHandler(controller);
            }
            adapter.setNdefPushMessageCallback(handler, activity, new Activity[0]);
        }
    }

    public static void unregister(Activity activity) {
        register(activity, null);
    }

    public NfcHandler(Controller controller) {
        this.mController = controller;
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        String currentUrl;
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
        if (this.mCurrentTab == null || this.mIsPrivate || (currentUrl = this.mCurrentTab.getUrl()) == null) {
            return null;
        }
        try {
            return new NdefMessage(NdefRecord.createUri(currentUrl), new NdefRecord[0]);
        } catch (IllegalArgumentException e2) {
            Log.e("BrowserNfcHandler", "IllegalArgumentException creating URI NdefRecord", e2);
            return null;
        }
    }
}
