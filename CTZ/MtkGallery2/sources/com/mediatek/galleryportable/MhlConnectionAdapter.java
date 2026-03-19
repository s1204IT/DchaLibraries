package com.mediatek.galleryportable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;

public class MhlConnectionAdapter {
    private BroadcastReceiver mMhlReceiver;
    private StateChangeListener mStateChangeListener;
    private static boolean sIsMhlApiExisted = false;
    private static boolean sHasChecked = false;

    public interface StateChangeListener {
    }

    private static boolean isMhlApiExisted() {
        if (!sHasChecked) {
            SystemClock.elapsedRealtime();
            try {
                Intent.class.getDeclaredField("ACTION_HDMI_PLUG");
                sIsMhlApiExisted = true;
            } catch (NoSuchFieldException e) {
                sIsMhlApiExisted = false;
                Log.e("VP_MhlAdapter", e.toString());
            }
            sHasChecked = true;
            Log.d("VP_MhlAdapter", "isMhlApiExisted, sIsMhlApiExisted = " + sIsMhlApiExisted);
        }
        return sIsMhlApiExisted;
    }

    public MhlConnectionAdapter(StateChangeListener stateChangeListener) {
        this.mStateChangeListener = stateChangeListener;
        if (isMhlApiExisted()) {
            this.mMhlReceiver = new MhlBroadcastReceiver();
        }
    }

    public void registerReceiver(Context context) {
        if (isMhlApiExisted() && this.mMhlReceiver != null) {
            Log.v("VP_MhlAdapter", "registerReceiver");
            IntentFilter mhlFilter = new IntentFilter(getIntentConst("ACTION_HDMI_PLUG"));
            context.registerReceiver(this.mMhlReceiver, mhlFilter);
        }
    }

    public void unRegisterReceiver(Context context) {
        if (isMhlApiExisted() && this.mMhlReceiver != null) {
            Log.v("VP_MhlAdapter", "unRegisterReceiver");
            try {
                context.unregisterReceiver(this.mMhlReceiver);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        }
    }

    class MhlBroadcastReceiver extends BroadcastReceiver {
        MhlBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.v("VP_MhlAdapter", "mMhlReceiver onReceive action: " + action);
        }
    }

    private static String getIntentConst(String field) {
        try {
            return (String) Intent.class.getField(field).get(null);
        } catch (Exception e) {
            android.util.Log.w("getConst occur error:", e);
            return null;
        }
    }
}
