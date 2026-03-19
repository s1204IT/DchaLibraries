package com.android.internal.telephony;

import android.os.Build;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.mediatek.internal.telephony.ImsSwitchController;
import com.mediatek.internal.telephony.dataconnection.DcFailCauseManager;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Random;

public class RetryManager {
    public static final boolean DBG = true;
    private static final String DEFAULT_DATA_RETRY_CONFIG = "default_randomization=2000,5000,10000,20000,40000,80000:5000,160000:5000,320000:5000,640000:5000,1280000:5000,1800000:5000";
    private static final long DEFAULT_INTER_APN_DELAY = 20000;
    private static final long DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING = 3000;
    private static final String IMS_DATA_RETRY_CONFIG = "max_retries=0, 5000, 5000, 5000";
    public static final String LOG_TAG = "RetryManager";
    private static final int MAX_SAME_APN_RETRY = 3;
    public static final long NO_RETRY = -1;
    public static final long NO_SUGGESTED_RETRY_DELAY = -2;
    private static final String OTHERS_DATA_RETRY_CONFIG = "max_retries=3, 5000, 5000, 5000";
    public static final boolean VDBG = false;
    private String mApnType;
    private String mConfig;
    private DcFailCauseManager mDcFcMgr;
    private long mFailFastInterApnDelay;
    private long mInterApnDelay;
    private int mMaxRetryCount;
    private Phone mPhone;
    private long mModemSuggestedDelay = -2;
    private int mSameApnRetryCount = 0;
    private ArrayList<RetryRec> mRetryArray = new ArrayList<>();
    private boolean mRetryForever = false;
    private int mRetryCount = 0;
    private Random mRng = new Random();
    private ArrayList<ApnSetting> mWaitingApns = null;
    private int mCurrentApnIndex = -1;

    private static class RetryRec {
        int mDelayTime;
        int mRandomizationTime;

        RetryRec(int delayTime, int randomizationTime) {
            this.mDelayTime = delayTime;
            this.mRandomizationTime = randomizationTime;
        }
    }

    public RetryManager(Phone phone, String apnType) {
        this.mPhone = phone;
        this.mApnType = apnType;
        this.mDcFcMgr = DcFailCauseManager.getInstance(this.mPhone);
    }

    private boolean configure(String configStr) {
        if (configStr.startsWith("\"") && configStr.endsWith("\"")) {
            configStr = configStr.substring(1, configStr.length() - 1);
        }
        reset();
        log("configure: '" + configStr + "'");
        this.mConfig = configStr;
        if (!TextUtils.isEmpty(configStr)) {
            int defaultRandomization = 0;
            String[] strArray = configStr.split(",");
            for (int i = 0; i < strArray.length; i++) {
                String[] splitStr = strArray[i].split("=", 2);
                splitStr[0] = splitStr[0].trim();
                if (splitStr.length > 1) {
                    splitStr[1] = splitStr[1].trim();
                    if (TextUtils.equals(splitStr[0], "default_randomization")) {
                        Pair<Boolean, Integer> value = parseNonNegativeInt(splitStr[0], splitStr[1]);
                        if (!((Boolean) value.first).booleanValue()) {
                            return false;
                        }
                        defaultRandomization = ((Integer) value.second).intValue();
                    } else if (TextUtils.equals(splitStr[0], "max_retries")) {
                        if (TextUtils.equals("infinite", splitStr[1])) {
                            this.mRetryForever = true;
                        } else {
                            Pair<Boolean, Integer> value2 = parseNonNegativeInt(splitStr[0], splitStr[1]);
                            if (!((Boolean) value2.first).booleanValue()) {
                                return false;
                            }
                            this.mMaxRetryCount = ((Integer) value2.second).intValue();
                        }
                    } else {
                        Rlog.e(LOG_TAG, "Unrecognized configuration name value pair: " + strArray[i]);
                        return false;
                    }
                } else {
                    String[] splitStr2 = strArray[i].split(":", 2);
                    splitStr2[0] = splitStr2[0].trim();
                    RetryRec rr = new RetryRec(0, 0);
                    Pair<Boolean, Integer> value3 = parseNonNegativeInt("delayTime", splitStr2[0]);
                    if (!((Boolean) value3.first).booleanValue()) {
                        return false;
                    }
                    rr.mDelayTime = ((Integer) value3.second).intValue();
                    if (splitStr2.length > 1) {
                        splitStr2[1] = splitStr2[1].trim();
                        Pair<Boolean, Integer> value4 = parseNonNegativeInt("randomizationTime", splitStr2[1]);
                        if (!((Boolean) value4.first).booleanValue()) {
                            return false;
                        }
                        rr.mRandomizationTime = ((Integer) value4.second).intValue();
                    } else {
                        rr.mRandomizationTime = defaultRandomization;
                    }
                    this.mRetryArray.add(rr);
                }
            }
            if (this.mRetryArray.size() > this.mMaxRetryCount) {
                this.mMaxRetryCount = this.mRetryArray.size();
            }
        } else {
            log("configure: cleared");
        }
        return true;
    }

    private void configureRetry(boolean forDefault) {
        String configString;
        try {
            if (Build.IS_DEBUGGABLE) {
                String config = SystemProperties.get("test.data_retry_config");
                if (!TextUtils.isEmpty(config)) {
                    configure(config);
                    return;
                }
            }
            CarrierConfigManager configManager = (CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config");
            PersistableBundle b = configManager.getConfigForSubId(this.mPhone.getSubId());
            this.mInterApnDelay = b.getLong("carrier_data_call_apn_delay_default_long", DEFAULT_INTER_APN_DELAY);
            this.mFailFastInterApnDelay = b.getLong("carrier_data_call_apn_delay_faster_long", DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING);
            if (forDefault) {
                configString = b.getString("carrier_data_call_retry_config_default_string", DEFAULT_DATA_RETRY_CONFIG);
            } else {
                configString = b.getString("carrier_data_call_retry_config_others_string", OTHERS_DATA_RETRY_CONFIG);
            }
        } catch (NullPointerException e) {
            log("Failed to read configuration! Use the hardcoded default value.");
            this.mInterApnDelay = DEFAULT_INTER_APN_DELAY;
            this.mFailFastInterApnDelay = DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING;
            configString = forDefault ? DEFAULT_DATA_RETRY_CONFIG : OTHERS_DATA_RETRY_CONFIG;
        }
        if (this.mDcFcMgr != null && this.mDcFcMgr.isSpecificNetworkAndSimOperator(DcFailCauseManager.Operator.OP19)) {
            configString = forDefault ? DcFailCauseManager.DEFAULT_DATA_RETRY_CONFIG_OP19 : OTHERS_DATA_RETRY_CONFIG;
        }
        if (forDefault && this.mDcFcMgr != null && this.mDcFcMgr.isSpecificNetworkOperator(DcFailCauseManager.Operator.OP12)) {
            configString = DcFailCauseManager.DEFAULT_DATA_RETRY_CONFIG_OP12;
        }
        if (TextUtils.equals(ImsSwitchController.IMS_SERVICE, this.mApnType) || TextUtils.equals("emergency", this.mApnType)) {
            log("configureRetry: IMS/EIMS, no retry by mobile. ");
            configString = IMS_DATA_RETRY_CONFIG;
        }
        configure(configString);
    }

    private int getRetryTimer() {
        int index;
        int retVal;
        if (this.mRetryCount < this.mRetryArray.size()) {
            index = this.mRetryCount;
        } else {
            index = this.mRetryArray.size() - 1;
        }
        if (index >= 0 && index < this.mRetryArray.size()) {
            retVal = this.mRetryArray.get(index).mDelayTime + nextRandomizationTime(index);
        } else {
            retVal = 0;
        }
        log("getRetryTimer: " + retVal);
        return retVal;
    }

    public int getRetryCount() {
        log("getRetryCount: " + this.mRetryCount);
        return this.mRetryCount;
    }

    private Pair<Boolean, Integer> parseNonNegativeInt(String name, String stringValue) {
        try {
            int value = Integer.parseInt(stringValue);
            Pair<Boolean, Integer> retVal = new Pair<>(Boolean.valueOf(validateNonNegativeInt(name, value)), Integer.valueOf(value));
            return retVal;
        } catch (NumberFormatException e) {
            Rlog.e(LOG_TAG, name + " bad value: " + stringValue, e);
            Pair<Boolean, Integer> retVal2 = new Pair<>(false, 0);
            return retVal2;
        }
    }

    private boolean validateNonNegativeInt(String name, int value) {
        if (value < 0) {
            Rlog.e(LOG_TAG, name + " bad value: is < 0");
            return false;
        }
        return true;
    }

    private int nextRandomizationTime(int index) {
        int randomTime = this.mRetryArray.get(index).mRandomizationTime;
        if (randomTime == 0) {
            return 0;
        }
        return this.mRng.nextInt(randomTime);
    }

    public ApnSetting getNextApnSetting() {
        if (this.mWaitingApns == null || this.mWaitingApns.size() == 0) {
            log("Waiting APN list is null or empty.");
            return null;
        }
        if (this.mModemSuggestedDelay != -2 && this.mSameApnRetryCount < 3) {
            this.mSameApnRetryCount++;
            return this.mWaitingApns.get(this.mCurrentApnIndex);
        }
        this.mSameApnRetryCount = 0;
        int index = this.mCurrentApnIndex;
        do {
            index++;
            if (index == this.mWaitingApns.size()) {
                index = 0;
            }
            if (!this.mWaitingApns.get(index).permanentFailed) {
                this.mCurrentApnIndex = index;
                return this.mWaitingApns.get(this.mCurrentApnIndex);
            }
        } while (index != this.mCurrentApnIndex);
        return null;
    }

    public long getDelayForNextApn(boolean failFastEnabled) {
        long delay;
        if (this.mWaitingApns == null || this.mWaitingApns.size() == 0) {
            log("Waiting APN list is null or empty.");
            return -1L;
        }
        if (this.mModemSuggestedDelay == -1) {
            log("Modem suggested not retrying.");
            return -1L;
        }
        if (this.mModemSuggestedDelay != -2 && this.mSameApnRetryCount < 3) {
            log("Modem suggested retry in " + this.mModemSuggestedDelay + " ms.");
            return this.mModemSuggestedDelay;
        }
        int index = this.mCurrentApnIndex;
        do {
            index++;
            if (index >= this.mWaitingApns.size()) {
                index = 0;
            }
            if (!this.mWaitingApns.get(index).permanentFailed) {
                if (index <= this.mCurrentApnIndex) {
                    if (!this.mRetryForever && this.mRetryCount + 1 > this.mMaxRetryCount) {
                        log("Reached maximum retry count " + this.mMaxRetryCount + ".");
                        return -1L;
                    }
                    delay = getRetryTimer();
                    this.mRetryCount++;
                } else {
                    delay = this.mInterApnDelay;
                }
                if (failFastEnabled && delay > this.mFailFastInterApnDelay) {
                    long delay2 = this.mFailFastInterApnDelay;
                    return delay2;
                }
                return delay;
            }
        } while (index != this.mCurrentApnIndex);
        log("All APNs have permanently failed.");
        return -1L;
    }

    public void markApnPermanentFailed(ApnSetting apn) {
        if (apn == null) {
            return;
        }
        apn.permanentFailed = true;
    }

    private void reset() {
        this.mMaxRetryCount = 0;
        this.mRetryCount = 0;
        this.mCurrentApnIndex = -1;
        this.mSameApnRetryCount = 0;
        this.mModemSuggestedDelay = -2L;
        this.mRetryArray.clear();
    }

    public void setWaitingApns(ArrayList<ApnSetting> waitingApns) {
        if (waitingApns == null) {
            log("No waiting APNs provided");
            return;
        }
        this.mWaitingApns = waitingApns;
        configureRetry(this.mApnType.equals("default"));
        for (ApnSetting apn : this.mWaitingApns) {
            apn.permanentFailed = false;
        }
        log("Setting " + this.mWaitingApns.size() + " waiting APNs.");
    }

    public ArrayList<ApnSetting> getWaitingApns() {
        return this.mWaitingApns;
    }

    public void setModemSuggestedDelay(long delay) {
        this.mModemSuggestedDelay = delay;
    }

    public long getInterApnDelay(boolean failFastEnabled) {
        return failFastEnabled ? this.mFailFastInterApnDelay : this.mInterApnDelay;
    }

    public String toString() {
        return "mApnType=" + this.mApnType + " mRetryCount=" + this.mRetryCount + " mMaxRetryCount=" + this.mMaxRetryCount + " mCurrentApnIndex=" + this.mCurrentApnIndex + " mSameApnRtryCount=" + this.mSameApnRetryCount + " mModemSuggestedDelay=" + this.mModemSuggestedDelay + " mRetryForever=" + this.mRetryForever + " mConfig={" + this.mConfig + "}";
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("  RetryManager");
        pw.println("***************************************");
        pw.println("    config = " + this.mConfig);
        pw.println("    mApnType = " + this.mApnType);
        pw.println("    mCurrentApnIndex = " + this.mCurrentApnIndex);
        pw.println("    mRetryCount = " + this.mRetryCount);
        pw.println("    mMaxRetryCount = " + this.mMaxRetryCount);
        pw.println("    mSameApnRetryCount = " + this.mSameApnRetryCount);
        pw.println("    mModemSuggestedDelay = " + this.mModemSuggestedDelay);
        if (this.mWaitingApns != null) {
            pw.println("    APN list: ");
            for (int i = 0; i < this.mWaitingApns.size(); i++) {
                pw.println("      [" + i + "]=" + this.mWaitingApns.get(i));
            }
        }
        pw.println("***************************************");
        pw.flush();
    }

    private void log(String s) {
        Rlog.d(LOG_TAG, "[" + this.mApnType + "] " + s);
    }
}
