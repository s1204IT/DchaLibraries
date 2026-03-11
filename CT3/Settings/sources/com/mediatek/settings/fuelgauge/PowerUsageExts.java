package com.mediatek.settings.fuelgauge;

import android.content.Context;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.view.IWindowManager;
import android.view.WindowManagerGlobal;
import com.android.settings.R;
import com.mediatek.perfservice.PerfServiceWrapper;
import com.mediatek.settings.ext.DefaultWfcSettingsExt;

public class PowerUsageExts implements Preference.OnPreferenceChangeListener {
    private SwitchPreference mBgPowerSavingPrf;
    private Context mContext;
    private MainHandler mMainHandler;
    private ListPreference mPerformancePower;
    private SwitchPreference mResolutionSwitchEnabled;

    public boolean onPowerUsageExtItemsClick(Preference preference) {
        if ("background_power_saving".equals(preference.getKey())) {
            if (preference instanceof SwitchPreference) {
                SwitchPreference pref = (SwitchPreference) preference;
                int bgState = pref.isChecked() ? 1 : 0;
                Log.d("PowerUsageExts", "background power saving state: " + bgState);
                Settings.System.putInt(this.mContext.getContentResolver(), "background_power_saving_enable", bgState);
                if (this.mBgPowerSavingPrf != null) {
                    this.mBgPowerSavingPrf.setChecked(pref.isChecked());
                    return true;
                }
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if ("performance_and_power".equals(preference.getKey())) {
            Log.d("PowerUsageExts", "onPreferenceChange KEY_PERFORMANCE_AND_POWER ");
            setPerformaceAndPowerType(Integer.parseInt(newValue.toString()));
            return true;
        }
        if (this.mResolutionSwitchEnabled != preference) {
            return false;
        }
        this.mResolutionSwitchEnabled.setEnabled(false);
        if (!this.mMainHandler.hasMessages(1, newValue)) {
            Message msg = this.mMainHandler.obtainMessage(1, newValue);
            this.mMainHandler.sendMessageDelayed(msg, 500L);
        }
        return true;
    }

    private void setPerformaceAndPowerType(int type) {
        Log.d("PowerUsageExts", "setPerformaceAndPowerType : " + type);
        PerfServiceWrapper perfServiceWrapper = new PerfServiceWrapper((Context) null);
        switch (type) {
            case DefaultWfcSettingsExt.RESUME:
                perfServiceWrapper.notifyUserStatus(6, 1);
                break;
            case DefaultWfcSettingsExt.PAUSE:
                perfServiceWrapper.notifyUserStatus(6, 0);
                break;
        }
        this.mPerformancePower.setSummary(this.mContext.getResources().getStringArray(R.array.performance_and_power_entries)[type]);
        this.mPerformancePower.setValueIndex(type);
        SystemProperties.set("persist.sys.power.mode", String.valueOf(type));
    }

    class MainHandler extends Handler {
        final PowerUsageExts this$0;

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DefaultWfcSettingsExt.PAUSE:
                    this.this$0.setResolution(((Boolean) msg.obj).booleanValue());
                    break;
            }
        }
    }

    public static boolean isResulotionSwitchEnabled(Context context) {
        int pos;
        String sizeStr = Settings.Global.getString(context.getContentResolver(), "display_size_forced");
        if (sizeStr == null || sizeStr.length() <= 0 || (pos = sizeStr.indexOf(44)) <= 0 || sizeStr.lastIndexOf(44) != pos) {
            return false;
        }
        try {
            int width = Integer.parseInt(sizeStr.substring(0, pos));
            Integer.parseInt(sizeStr.substring(pos + 1));
            return width == 720;
        } catch (NumberFormatException e) {
            Log.w("PowerUsageExts", "Unable to get the display width or height");
            return false;
        }
    }

    public void setResolution(boolean isSwitch) {
        Log.d("PowerUsageExts", "resolution switch state: " + isSwitch);
        int baseDensity = -1;
        IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
        try {
            baseDensity = wm.getBaseDisplayDensity(0);
        } catch (RemoteException e) {
            Log.w("PowerUsageExts", "Unable to get the display density");
        }
        Log.d("PowerUsageExts", "baseDensity: " + baseDensity);
        Point size = new Point();
        try {
            wm.getInitialDisplaySize(0, size);
        } catch (RemoteException e2) {
            Log.w("PowerUsageExts", "Unable to get the display size");
        }
        if (isSwitch) {
            int newDesity = (baseDensity * 720) / size.x;
            Log.d("PowerUsageExts", "switch on newDesity: " + newDesity);
            setForcedDisplaySizeDensity(0, 720, 1280, newDesity);
        } else {
            int newDesity2 = (size.x * baseDensity) / 720;
            Log.d("PowerUsageExts", "switch off newDesity: " + newDesity2);
            setForcedDisplaySizeDensity(0, size.x, size.y, newDesity2);
        }
        this.mResolutionSwitchEnabled.setEnabled(true);
    }

    private void setForcedDisplaySizeDensity(final int displayId, final int width, final int height, final int density) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    IWindowManager wm = WindowManagerGlobal.getWindowManagerService();
                    wm.setForcedDisplaySize(displayId, width, height);
                    wm.setForcedDisplayDensity(displayId, density);
                } catch (RemoteException e) {
                    Log.w("PowerUsageExts", "Unable to save forced display density setting");
                }
            }
        });
    }
}
