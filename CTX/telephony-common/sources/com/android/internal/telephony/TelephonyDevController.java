package com.android.internal.telephony;

import android.R;
import android.content.res.Resources;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TelephonyDevController extends Handler {
    protected static final boolean DBG = true;
    private static final int EVENT_HARDWARE_CONFIG_CHANGED = 1;
    protected static final String LOG_TAG = "TDC";
    private static final Object mLock = new Object();
    protected static ArrayList<HardwareConfig> mModems = new ArrayList<>();
    protected static ArrayList<HardwareConfig> mSims = new ArrayList<>();
    private static Message sRilHardwareConfig;
    private static TelephonyDevController sTelephonyDevController;

    protected static void logd(String str) {
        Rlog.d(LOG_TAG, str);
    }

    protected static void loge(String str) {
        Rlog.e(LOG_TAG, str);
    }

    public static TelephonyDevController create() {
        TelephonyDevController telephonyDevController;
        synchronized (mLock) {
            if (sTelephonyDevController != null) {
                throw new RuntimeException("TelephonyDevController already created!?!");
            }
            try {
                Class<?> cls = Class.forName("com.mediatek.internal.telephony.MtkTelephonyDevController", false, ClassLoader.getSystemClassLoader());
                Rlog.d(LOG_TAG, "class = " + cls);
                Constructor<?> constructor = cls.getConstructor(new Class[0]);
                Rlog.d(LOG_TAG, "constructor function = " + constructor);
                sTelephonyDevController = (TelephonyDevController) constructor.newInstance(new Object[0]);
            } catch (Exception e) {
                Rlog.e(LOG_TAG, "No MtkTelephonyDevController! Used AOSP for instead!");
                sTelephonyDevController = new TelephonyDevController();
            }
            telephonyDevController = sTelephonyDevController;
        }
        return telephonyDevController;
    }

    public static TelephonyDevController getInstance() {
        TelephonyDevController telephonyDevController;
        synchronized (mLock) {
            if (sTelephonyDevController == null) {
                throw new RuntimeException("TelephonyDevController not yet created!?!");
            }
            telephonyDevController = sTelephonyDevController;
        }
        return telephonyDevController;
    }

    protected void initFromResource() {
        String[] stringArray = Resources.getSystem().getStringArray(R.array.config_deviceStatesOnWhichToWakeUp);
        if (stringArray != null) {
            for (String str : stringArray) {
                HardwareConfig hardwareConfig = new HardwareConfig(str);
                if (hardwareConfig.type == 0) {
                    updateOrInsert(hardwareConfig, mModems);
                } else if (hardwareConfig.type == 1) {
                    updateOrInsert(hardwareConfig, mSims);
                }
            }
        }
    }

    public TelephonyDevController() {
        initFromResource();
        mModems.trimToSize();
        mSims.trimToSize();
    }

    public void registerRIL(CommandsInterface commandsInterface) {
        commandsInterface.getHardwareConfig(sRilHardwareConfig);
        if (sRilHardwareConfig != null) {
            AsyncResult asyncResult = (AsyncResult) sRilHardwareConfig.obj;
            if (asyncResult.exception == null) {
                handleGetHardwareConfigChanged(asyncResult);
            }
        }
        commandsInterface.registerForHardwareConfigChanged(sTelephonyDevController, 1, null);
    }

    public static void unregisterRIL(CommandsInterface commandsInterface) {
        commandsInterface.unregisterForHardwareConfigChanged(sTelephonyDevController);
    }

    @Override
    public void handleMessage(Message message) {
        if (message.what == 1) {
            logd("handleMessage: received EVENT_HARDWARE_CONFIG_CHANGED");
            handleGetHardwareConfigChanged((AsyncResult) message.obj);
        } else {
            loge("handleMessage: Unknown Event " + message.what);
        }
    }

    protected static void updateOrInsert(HardwareConfig hardwareConfig, ArrayList<HardwareConfig> arrayList) {
        synchronized (mLock) {
            int size = arrayList.size();
            int i = 0;
            while (true) {
                if (i >= size) {
                    break;
                }
                HardwareConfig hardwareConfig2 = arrayList.get(i);
                if (hardwareConfig2.uuid.compareTo(hardwareConfig.uuid) != 0) {
                    i++;
                } else {
                    logd("updateOrInsert: removing: " + hardwareConfig2);
                    arrayList.remove(i);
                    break;
                }
            }
            logd("updateOrInsert: inserting: " + hardwareConfig);
            arrayList.add(hardwareConfig);
        }
    }

    protected void handleGetHardwareConfigChanged(AsyncResult asyncResult) {
        if (asyncResult.exception == null && asyncResult.result != null) {
            List list = (List) asyncResult.result;
            for (int i = 0; i < list.size(); i++) {
                HardwareConfig hardwareConfig = (HardwareConfig) list.get(i);
                if (hardwareConfig != null) {
                    if (hardwareConfig.type == 0) {
                        updateOrInsert(hardwareConfig, mModems);
                    } else if (hardwareConfig.type == 1) {
                        updateOrInsert(hardwareConfig, mSims);
                    }
                }
            }
            return;
        }
        loge("handleGetHardwareConfigChanged - returned an error.");
    }

    public static int getModemCount() {
        int size;
        synchronized (mLock) {
            size = mModems.size();
            logd("getModemCount: " + size);
        }
        return size;
    }

    public HardwareConfig getModem(int i) {
        synchronized (mLock) {
            if (mModems.isEmpty()) {
                loge("getModem: no registered modem device?!?");
                return null;
            }
            if (i > getModemCount()) {
                loge("getModem: out-of-bounds access for modem device " + i + " max: " + getModemCount());
                return null;
            }
            logd("getModem: " + i);
            return mModems.get(i);
        }
    }

    public int getSimCount() {
        int size;
        synchronized (mLock) {
            size = mSims.size();
            logd("getSimCount: " + size);
        }
        return size;
    }

    public HardwareConfig getSim(int i) {
        synchronized (mLock) {
            if (mSims.isEmpty()) {
                loge("getSim: no registered sim device?!?");
                return null;
            }
            if (i > getSimCount()) {
                loge("getSim: out-of-bounds access for sim device " + i + " max: " + getSimCount());
                return null;
            }
            logd("getSim: " + i);
            return mSims.get(i);
        }
    }

    public HardwareConfig getModemForSim(int i) {
        synchronized (mLock) {
            if (!mModems.isEmpty() && !mSims.isEmpty()) {
                if (i > getSimCount()) {
                    loge("getModemForSim: out-of-bounds access for sim device " + i + " max: " + getSimCount());
                    return null;
                }
                logd("getModemForSim " + i);
                HardwareConfig sim = getSim(i);
                for (HardwareConfig hardwareConfig : mModems) {
                    if (hardwareConfig.uuid.equals(sim.modemUuid)) {
                        return hardwareConfig;
                    }
                }
                return null;
            }
            loge("getModemForSim: no registered modem/sim device?!?");
            return null;
        }
    }

    public ArrayList<HardwareConfig> getAllSimsForModem(int i) {
        synchronized (mLock) {
            if (mSims.isEmpty()) {
                loge("getAllSimsForModem: no registered sim device?!?");
                return null;
            }
            if (i > getModemCount()) {
                loge("getAllSimsForModem: out-of-bounds access for modem device " + i + " max: " + getModemCount());
                return null;
            }
            logd("getAllSimsForModem " + i);
            ArrayList<HardwareConfig> arrayList = new ArrayList<>();
            HardwareConfig modem = getModem(i);
            for (HardwareConfig hardwareConfig : mSims) {
                if (hardwareConfig.modemUuid.equals(modem.uuid)) {
                    arrayList.add(hardwareConfig);
                }
            }
            return arrayList;
        }
    }

    public ArrayList<HardwareConfig> getAllModems() {
        ArrayList<HardwareConfig> arrayList;
        synchronized (mLock) {
            arrayList = new ArrayList<>();
            if (mModems.isEmpty()) {
                logd("getAllModems: empty list.");
            } else {
                Iterator<HardwareConfig> it = mModems.iterator();
                while (it.hasNext()) {
                    arrayList.add(it.next());
                }
            }
        }
        return arrayList;
    }

    public ArrayList<HardwareConfig> getAllSims() {
        ArrayList<HardwareConfig> arrayList;
        synchronized (mLock) {
            arrayList = new ArrayList<>();
            if (mSims.isEmpty()) {
                logd("getAllSims: empty list.");
            } else {
                Iterator<HardwareConfig> it = mSims.iterator();
                while (it.hasNext()) {
                    arrayList.add(it.next());
                }
            }
        }
        return arrayList;
    }
}
