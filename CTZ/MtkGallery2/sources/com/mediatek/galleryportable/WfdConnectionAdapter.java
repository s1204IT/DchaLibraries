package com.mediatek.galleryportable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.display.DisplayManager;
import android.hardware.display.WifiDisplayStatus;
import android.os.SystemClock;
import android.provider.Settings;

public class WfdConnectionAdapter {
    private StateChangeListener mStateChangeListener;
    private BroadcastReceiver mWfdReceiver;
    private static boolean sIsWfdApiExisted = false;
    private static boolean sHasChecked = false;
    private static String sSavingOption = null;
    private static String sSavingDelay = null;

    public interface StateChangeListener {
        void stateConnected();

        void stateNotConnected();
    }

    private static boolean isWfdApiExisted() {
        if (!sHasChecked) {
            SystemClock.elapsedRealtime();
            try {
                Class<?> clazz = WfdConnectionAdapter.class.getClassLoader().loadClass("com.mediatek.provider.MtkSettingsExt$Global");
                if (clazz != null) {
                    clazz.getDeclaredField("WIFI_DISPLAY_POWER_SAVING_OPTION");
                    Object name = clazz.getField("WIFI_DISPLAY_POWER_SAVING_OPTION").get(null);
                    if (name != null) {
                        sSavingOption = (String) name;
                    }
                }
                if (clazz != null) {
                    clazz.getDeclaredField("WIFI_DISPLAY_POWER_SAVING_DELAY");
                    Object name2 = clazz.getField("WIFI_DISPLAY_POWER_SAVING_DELAY").get(null);
                    if (name2 != null) {
                        sSavingDelay = (String) name2;
                    }
                }
                Class<?> clazz2 = WfdConnectionAdapter.class.getClassLoader().loadClass("android.hardware.display.DisplayManager");
                if (clazz2 != null) {
                    clazz2.getDeclaredMethod("getWifiDisplayStatus", new Class[0]);
                }
                if (clazz2 != null) {
                    clazz2.getDeclaredField("ACTION_WIFI_DISPLAY_STATUS_CHANGED");
                }
                if (clazz2 != null) {
                    clazz2.getDeclaredField("EXTRA_WIFI_DISPLAY_STATUS");
                }
                Class<?> clazz3 = WfdConnectionAdapter.class.getClassLoader().loadClass("android.hardware.display.WifiDisplayStatus");
                if (clazz3 != null) {
                    clazz3.getDeclaredMethod("getActiveDisplayState", new Class[0]);
                }
                if (clazz3 != null) {
                    clazz3.getDeclaredField("DISPLAY_STATE_CONNECTED");
                }
                if (clazz3 != null) {
                    clazz3.getDeclaredField("DISPLAY_STATE_NOT_CONNECTED");
                }
                sIsWfdApiExisted = true;
            } catch (ClassNotFoundException e) {
                sIsWfdApiExisted = false;
                Log.e("Gallery2/WfdConnectionAdapter", e.toString());
            } catch (IllegalAccessException e2) {
                sIsWfdApiExisted = false;
                Log.e("Gallery2/WfdConnectionAdapter", e2.toString());
            } catch (NoSuchFieldException e3) {
                sIsWfdApiExisted = false;
                Log.e("Gallery2/WfdConnectionAdapter", e3.toString());
            } catch (NoSuchMethodException e4) {
                sIsWfdApiExisted = false;
                Log.e("Gallery2/WfdConnectionAdapter", e4.toString());
            }
            sHasChecked = true;
            Log.d("Gallery2/WfdConnectionAdapter", "isWfdApiExisted, mIsWfdApiExisted = " + sIsWfdApiExisted);
        }
        return sIsWfdApiExisted;
    }

    public WfdConnectionAdapter(StateChangeListener stateChangeListener) {
        this.mStateChangeListener = stateChangeListener;
        if (isWfdApiExisted()) {
            this.mWfdReceiver = new WfdBroadcastReceiver();
        }
    }

    public static int getPowerSavingMode(Context context) {
        int mode = 0;
        if (isWfdApiExisted() && sSavingOption != null) {
            mode = Settings.Global.getInt(context.getContentResolver(), sSavingOption, 0);
        }
        Log.v("Gallery2/WfdConnectionAdapter", "getPowerSavingMode() mode = " + mode);
        return mode;
    }

    public static int getPowerSavingDelay(Context context) {
        int delayTime = 0;
        if (isWfdApiExisted() && sSavingDelay != null) {
            delayTime = Settings.Global.getInt(context.getContentResolver(), sSavingDelay, 0);
        }
        Log.v("Gallery2/WfdConnectionAdapter", "getDelayTime(): " + delayTime);
        return delayTime;
    }

    public void registerReceiver(Context context) {
        if (isWfdApiExisted() && this.mWfdReceiver != null) {
            Log.v("Gallery2/WfdConnectionAdapter", "registerReceiver");
            IntentFilter filter = new IntentFilter("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED");
            context.registerReceiver(this.mWfdReceiver, filter);
        }
    }

    public void unRegisterReceiver(Context context) {
        if (isWfdApiExisted() && this.mWfdReceiver != null) {
            Log.v("Gallery2/WfdConnectionAdapter", "unRegisterReceiver");
            try {
                context.unregisterReceiver(this.mWfdReceiver);
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        }
    }

    public static boolean isWfdSupported(Context context) {
        boolean connected = false;
        if (isWfdApiExisted()) {
            DisplayManager mDisplayManager = (DisplayManager) context.getSystemService("display");
            WifiDisplayStatus mWfdStatus = mDisplayManager.getWifiDisplayStatus();
            int activityDisplayState = mWfdStatus.getActiveDisplayState();
            connected = activityDisplayState == 2;
        }
        Log.d("Gallery2/WfdConnectionAdapter", "isWfdSupported(): " + connected);
        return connected;
    }

    class WfdBroadcastReceiver extends BroadcastReceiver {
        WfdBroadcastReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            WifiDisplayStatus status;
            String action = intent.getAction();
            Log.v("Gallery2/WfdConnectionAdapter", "WfdBroadcastReceiver onReceive action: " + action);
            if (action != null && action.equals("android.hardware.display.action.WIFI_DISPLAY_STATUS_CHANGED") && (status = intent.getParcelableExtra("android.hardware.display.extra.WIFI_DISPLAY_STATUS")) != null) {
                int state = status.getActiveDisplayState();
                Log.v("Gallery2/WfdConnectionAdapter", "WfdBroadcastReceiver onReceive wfd State: " + state);
                if (state == 0) {
                    if (WfdConnectionAdapter.this.mStateChangeListener != null) {
                        WfdConnectionAdapter.this.mStateChangeListener.stateNotConnected();
                    }
                } else if (state == 2 && WfdConnectionAdapter.this.mStateChangeListener != null) {
                    WfdConnectionAdapter.this.mStateChangeListener.stateConnected();
                }
            }
        }
    }
}
