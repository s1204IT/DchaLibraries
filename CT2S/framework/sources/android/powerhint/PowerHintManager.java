package android.powerhint;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.powerhint.IPowerHintService;
import android.util.Log;

public class PowerHintManager {
    private static final boolean DEBUG = false;
    public static final String HINT_APP_LAUNCH = "app_launch";
    public static final String HINT_BOOT = "boot";
    public static final String HINT_BROWSER = "browser";
    public static final String HINT_CAMERA = "camera";
    public static final String HINT_FM = "fm";
    public static final String HINT_FOREGROUND_TASK = "foreground_task";
    public static final String HINT_INPUT = "input";
    public static final String HINT_MEDIA = "media";
    public static final String HINT_PHONE = "phone";
    public static final String HINT_ROTATION = "rotation";
    public static final String HINT_SCREEN = "screen";
    public static final String HINT_SSG_BOOSTER = "ssg_booster";
    public static final String HINT_THERMAL = "thermal";
    public static final String HINT_VIDEO_START = "video_start";
    public static final String HINT_WFD = "wfd";
    public static final String HINT_WIFI = "wifi";
    private static final String TAG = "PowerHintManager";
    private IPowerHintService mPowerHintService = IPowerHintService.Stub.asInterface(ServiceManager.getService("PowerHintService"));

    public PowerHintManager() {
        if (this.mPowerHintService == null) {
            Log.e(TAG, "PowerHintService not ready!!!");
        }
    }

    public int sendPowerHint(String hint, int state) {
        PowerHintData data = new PowerHintData(hint, 0.0d, -1, new IntParcelable(state));
        return sendPowerHintImpl(data);
    }

    public int sendPowerHint(String hint, String state) {
        PowerHintData data = new PowerHintData(hint, 0.0d, -1, new StringParcelable(state));
        return sendPowerHintImpl(data);
    }

    public int sendPowerHint(String hint, Parcelable state) {
        PowerHintData data = new PowerHintData(hint, 0.0d, -1, state);
        return sendPowerHintImpl(data);
    }

    public int sendPowerHint(String hint, Parcel state) {
        PowerHintData data = new PowerHintData(hint, 0.0d, -1, state);
        return sendPowerHintImpl(data);
    }

    private int sendPowerHintImpl(PowerHintData data) {
        if (this.mPowerHintService == null) {
            Log.e(TAG, "PowerHintService not ready, ignore sendPowerHintImpl.");
            return -1;
        }
        try {
            int res = this.mPowerHintService.sendPowerHint(data);
            return res;
        } catch (RemoteException e) {
            Log.e(TAG, "sendPowerHintImpl: drop power hint.");
            return -1;
        }
    }

    public int sendDurablePowerHint(String hint, double timerId, int duration, int state) {
        PowerHintData data = new PowerHintData(hint, timerId, duration, new IntParcelable(state));
        return sendDurablePowerHintImpl(data);
    }

    public int sendDurablePowerHint(String hint, double timerId, int duration, String state) {
        PowerHintData data = new PowerHintData(hint, timerId, duration, new StringParcelable(state));
        return sendDurablePowerHintImpl(data);
    }

    public int sendDurablePowerHint(String hint, double timerId, int duration, Parcel state) {
        PowerHintData data = new PowerHintData(hint, timerId, duration, state);
        return sendDurablePowerHintImpl(data);
    }

    public int sendDurablePowerHint(String hint, double timerId, int duration, Parcelable state) {
        PowerHintData data = new PowerHintData(hint, timerId, duration, state);
        return sendDurablePowerHintImpl(data);
    }

    private int sendDurablePowerHintImpl(PowerHintData data) {
        if (this.mPowerHintService == null) {
            Log.e(TAG, "PowerHintService not ready, ignore sendDurablePowerHintImpl.");
            return -1;
        }
        try {
            int res = this.mPowerHintService.sendDurablePowerHint(data);
            return res;
        } catch (RemoteException e) {
            Log.e(TAG, "sendDurablePowerHintImpl: drop power hint.");
            return -1;
        }
    }

    public double obtainDurablePowerHintTimer() {
        if (this.mPowerHintService == null) {
            Log.e(TAG, "PowerHintService not ready, ignore obtainDurablePowerHintTimer.");
            return -1.0d;
        }
        try {
            double timerId = this.mPowerHintService.obtainDurablePowerHintTimer();
            return timerId;
        } catch (RemoteException e) {
            Log.e(TAG, "obtainDurablePowerHintTimer: drop timer obtain request.");
            return -1.0d;
        }
    }

    public int cancelDurablePowerHint(String hint, double timerId) {
        if (this.mPowerHintService == null) {
            Log.e(TAG, "PowerHintService not ready, ignore cancelDurablePowerHint.");
            return -1;
        }
        try {
            PowerHintData data = new PowerHintData(hint, timerId, 0);
            int ret = this.mPowerHintService.cancelDurablePowerHint(data);
            return ret;
        } catch (RemoteException e) {
            Log.e(TAG, "cancelDurablePowerHint: drop timer cancel request.");
            return -1;
        }
    }
}
