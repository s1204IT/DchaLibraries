package com.mediatek.galleryfeature.pq;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class PQBroadcastReceiver extends BroadcastReceiver {
    private static final String PQACTION = "com.mediatek.gallery.action.ReloadImage";
    private static PQBroadcastReceiver sReceiver;
    private PQListener mListener = null;

    public interface PQListener {
        void onPQEffect();
    }

    public static PQBroadcastReceiver getReceiver() {
        if (sReceiver == null) {
            sReceiver = new PQBroadcastReceiver();
        }
        return sReceiver;
    }

    public static void registerReceiver(Context context) {
        if (getReceiver() != null) {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(PQACTION);
            context.registerReceiver(getReceiver(), intentFilter);
        }
    }

    public static void unregisterReceiver(Context context) {
        if (getReceiver() != null) {
            context.unregisterReceiver(getReceiver());
        }
    }

    public static void setListener(PQListener pQListener) {
        if (getReceiver() != null) {
            getReceiver().mListener = pQListener;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (PQACTION.equalsIgnoreCase(intent.getAction()) && this.mListener != null) {
            this.mListener.onPQEffect();
        }
    }
}
