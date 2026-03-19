package com.android.internal.telephony;

import android.os.Build;
import android.os.PersistableBundle;
import android.os.SystemProperties;
import android.telephony.CarrierConfigManager;
import android.telephony.Rlog;
import android.text.TextUtils;
import android.util.Pair;
import com.android.internal.telephony.dataconnection.ApnSetting;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class RetryManager {
    public static final boolean DBG = true;
    private static final long DEFAULT_APN_RETRY_AFTER_DISCONNECT_DELAY = 10000;
    private static final String DEFAULT_DATA_RETRY_CONFIG = "max_retries=3, 5000, 5000, 5000";
    private static final long DEFAULT_INTER_APN_DELAY = 20000;
    private static final long DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING = 3000;
    public static final String LOG_TAG = "RetryManager";
    protected static int MAX_SAME_APN_RETRY = 3;
    public static final long NO_RETRY = -1;
    public static final long NO_SUGGESTED_RETRY_DELAY = -2;
    private static final String OTHERS_APN_TYPE = "others";
    public static final boolean VDBG = false;
    private long mApnRetryAfterDisconnectDelay;
    protected String mApnType;
    private String mConfig;
    protected long mFailFastInterApnDelay;
    protected long mInterApnDelay;
    protected int mMaxRetryCount;
    protected Phone mPhone;
    protected long mModemSuggestedDelay = -2;
    protected int mSameApnRetryCount = 0;
    private ArrayList<RetryRec> mRetryArray = new ArrayList<>();
    protected boolean mRetryForever = false;
    protected int mRetryCount = 0;
    private Random mRng = new Random();
    protected ArrayList<ApnSetting> mWaitingApns = null;
    protected int mCurrentApnIndex = -1;

    private static class RetryRec {
        int mDelayTime;
        int mRandomizationTime;

        RetryRec(int i, int i2) {
            this.mDelayTime = i;
            this.mRandomizationTime = i2;
        }
    }

    public RetryManager(Phone phone, String str) {
        this.mPhone = phone;
        this.mApnType = str;
    }

    protected boolean configure(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            str = str.substring(1, str.length() - 1);
        }
        reset();
        log("configure: '" + str + "'");
        this.mConfig = str;
        if (!TextUtils.isEmpty(str)) {
            String[] strArrSplit = str.split(",");
            int iIntValue = 0;
            for (int i = 0; i < strArrSplit.length; i++) {
                String[] strArrSplit2 = strArrSplit[i].split("=", 2);
                strArrSplit2[0] = strArrSplit2[0].trim();
                if (strArrSplit2.length > 1) {
                    strArrSplit2[1] = strArrSplit2[1].trim();
                    if (TextUtils.equals(strArrSplit2[0], "default_randomization")) {
                        Pair<Boolean, Integer> nonNegativeInt = parseNonNegativeInt(strArrSplit2[0], strArrSplit2[1]);
                        if (!((Boolean) nonNegativeInt.first).booleanValue()) {
                            return false;
                        }
                        iIntValue = ((Integer) nonNegativeInt.second).intValue();
                    } else if (TextUtils.equals(strArrSplit2[0], "max_retries")) {
                        if (TextUtils.equals("infinite", strArrSplit2[1])) {
                            this.mRetryForever = true;
                        } else {
                            Pair<Boolean, Integer> nonNegativeInt2 = parseNonNegativeInt(strArrSplit2[0], strArrSplit2[1]);
                            if (!((Boolean) nonNegativeInt2.first).booleanValue()) {
                                return false;
                            }
                            this.mMaxRetryCount = ((Integer) nonNegativeInt2.second).intValue();
                        }
                    } else {
                        Rlog.e(LOG_TAG, "Unrecognized configuration name value pair: " + strArrSplit[i]);
                        return false;
                    }
                } else {
                    String[] strArrSplit3 = strArrSplit[i].split(":", 2);
                    strArrSplit3[0] = strArrSplit3[0].trim();
                    RetryRec retryRec = new RetryRec(0, 0);
                    Pair<Boolean, Integer> nonNegativeInt3 = parseNonNegativeInt("delayTime", strArrSplit3[0]);
                    if (!((Boolean) nonNegativeInt3.first).booleanValue()) {
                        return false;
                    }
                    retryRec.mDelayTime = ((Integer) nonNegativeInt3.second).intValue();
                    if (strArrSplit3.length > 1) {
                        strArrSplit3[1] = strArrSplit3[1].trim();
                        Pair<Boolean, Integer> nonNegativeInt4 = parseNonNegativeInt("randomizationTime", strArrSplit3[1]);
                        if (!((Boolean) nonNegativeInt4.first).booleanValue()) {
                            return false;
                        }
                        retryRec.mRandomizationTime = ((Integer) nonNegativeInt4.second).intValue();
                    } else {
                        retryRec.mRandomizationTime = iIntValue;
                    }
                    this.mRetryArray.add(retryRec);
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

    protected void configureRetry() {
        String str;
        String str2;
        try {
            if (Build.IS_DEBUGGABLE) {
                String str3 = SystemProperties.get("test.data_retry_config");
                if (!TextUtils.isEmpty(str3)) {
                    configure(str3);
                    return;
                }
            }
            PersistableBundle configForSubId = ((CarrierConfigManager) this.mPhone.getContext().getSystemService("carrier_config")).getConfigForSubId(this.mPhone.getSubId());
            this.mInterApnDelay = configForSubId.getLong("carrier_data_call_apn_delay_default_long", DEFAULT_INTER_APN_DELAY);
            this.mFailFastInterApnDelay = configForSubId.getLong("carrier_data_call_apn_delay_faster_long", DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING);
            this.mApnRetryAfterDisconnectDelay = configForSubId.getLong("carrier_data_call_apn_retry_after_disconnect_long", DEFAULT_APN_RETRY_AFTER_DISCONNECT_DELAY);
            String[] stringArray = configForSubId.getStringArray("carrier_data_call_retry_config_strings");
            str = null;
            if (stringArray != null) {
                int length = stringArray.length;
                str2 = null;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    }
                    String str4 = stringArray[i];
                    if (!TextUtils.isEmpty(str4)) {
                        String[] strArrSplit = str4.split(":", 2);
                        if (strArrSplit.length == 2) {
                            String strTrim = strArrSplit[0].trim();
                            if (strTrim.equals(this.mApnType)) {
                                str = strArrSplit[1];
                                break;
                            } else if (strTrim.equals(OTHERS_APN_TYPE)) {
                                str2 = strArrSplit[1];
                            }
                        } else {
                            continue;
                        }
                    }
                    i++;
                }
            } else {
                str2 = null;
            }
            if (str == null) {
                if (str2 == null) {
                    log("Invalid APN retry configuration!. Use the default one now.");
                    str = DEFAULT_DATA_RETRY_CONFIG;
                } else {
                    str = str2;
                }
            }
        } catch (NullPointerException e) {
            log("Failed to read configuration! Use the hardcoded default value.");
            this.mInterApnDelay = DEFAULT_INTER_APN_DELAY;
            this.mFailFastInterApnDelay = DEFAULT_INTER_APN_DELAY_FOR_PROVISIONING;
            str = DEFAULT_DATA_RETRY_CONFIG;
        }
        configure(str);
    }

    protected int getRetryTimer() {
        int size;
        int iNextRandomizationTime;
        if (this.mRetryCount < this.mRetryArray.size()) {
            size = this.mRetryCount;
        } else {
            size = this.mRetryArray.size() - 1;
        }
        if (size >= 0 && size < this.mRetryArray.size()) {
            iNextRandomizationTime = this.mRetryArray.get(size).mDelayTime + nextRandomizationTime(size);
        } else {
            iNextRandomizationTime = 0;
        }
        log("getRetryTimer: " + iNextRandomizationTime);
        return iNextRandomizationTime;
    }

    private Pair<Boolean, Integer> parseNonNegativeInt(String str, String str2) {
        try {
            int i = Integer.parseInt(str2);
            return new Pair<>(Boolean.valueOf(validateNonNegativeInt(str, i)), Integer.valueOf(i));
        } catch (NumberFormatException e) {
            Rlog.e(LOG_TAG, str + " bad value: " + str2, e);
            return new Pair<>(false, 0);
        }
    }

    private boolean validateNonNegativeInt(String str, int i) {
        if (i < 0) {
            Rlog.e(LOG_TAG, str + " bad value: is < 0");
            return false;
        }
        return true;
    }

    private int nextRandomizationTime(int i) {
        int i2 = this.mRetryArray.get(i).mRandomizationTime;
        if (i2 == 0) {
            return 0;
        }
        return this.mRng.nextInt(i2);
    }

    public ApnSetting getNextApnSetting() {
        if (this.mWaitingApns == null || this.mWaitingApns.size() == 0) {
            log("Waiting APN list is null or empty.");
            return null;
        }
        if (this.mModemSuggestedDelay != -2 && this.mSameApnRetryCount < MAX_SAME_APN_RETRY) {
            this.mSameApnRetryCount++;
            return this.mWaitingApns.get(this.mCurrentApnIndex);
        }
        this.mSameApnRetryCount = 0;
        int i = this.mCurrentApnIndex;
        do {
            i++;
            if (i == this.mWaitingApns.size()) {
                i = 0;
            }
            if (!this.mWaitingApns.get(i).permanentFailed) {
                this.mCurrentApnIndex = i;
                return this.mWaitingApns.get(this.mCurrentApnIndex);
            }
        } while (i != this.mCurrentApnIndex);
        return null;
    }

    public long getDelayForNextApn(boolean z) {
        long retryTimer;
        if (this.mWaitingApns == null || this.mWaitingApns.size() == 0) {
            log("Waiting APN list is null or empty.");
            return -1L;
        }
        if (this.mModemSuggestedDelay == -1) {
            log("Modem suggested not retrying.");
            return -1L;
        }
        if (this.mModemSuggestedDelay != -2 && this.mSameApnRetryCount < MAX_SAME_APN_RETRY) {
            log("Modem suggested retry in " + this.mModemSuggestedDelay + " ms.");
            return this.mModemSuggestedDelay;
        }
        int i = this.mCurrentApnIndex;
        do {
            i++;
            if (i >= this.mWaitingApns.size()) {
                i = 0;
            }
            if (!this.mWaitingApns.get(i).permanentFailed) {
                if (i <= this.mCurrentApnIndex) {
                    if (!this.mRetryForever && this.mRetryCount + 1 > this.mMaxRetryCount) {
                        log("Reached maximum retry count " + this.mMaxRetryCount + ".");
                        return -1L;
                    }
                    retryTimer = getRetryTimer();
                    this.mRetryCount++;
                } else {
                    retryTimer = this.mInterApnDelay;
                }
                if (z && retryTimer > this.mFailFastInterApnDelay) {
                    return this.mFailFastInterApnDelay;
                }
                return retryTimer;
            }
        } while (i != this.mCurrentApnIndex);
        log("All APNs have permanently failed.");
        return -1L;
    }

    public void markApnPermanentFailed(ApnSetting apnSetting) {
        if (apnSetting != null) {
            apnSetting.permanentFailed = true;
        }
    }

    private void reset() {
        this.mMaxRetryCount = 0;
        this.mRetryCount = 0;
        this.mCurrentApnIndex = -1;
        this.mSameApnRetryCount = 0;
        this.mModemSuggestedDelay = -2L;
        this.mRetryArray.clear();
    }

    public void setWaitingApns(ArrayList<ApnSetting> arrayList) {
        if (arrayList == null) {
            log("No waiting APNs provided");
            return;
        }
        this.mWaitingApns = arrayList;
        configureRetry();
        Iterator<ApnSetting> it = this.mWaitingApns.iterator();
        while (it.hasNext()) {
            it.next().permanentFailed = false;
        }
        log("Setting " + this.mWaitingApns.size() + " waiting APNs.");
    }

    public ArrayList<ApnSetting> getWaitingApns() {
        return this.mWaitingApns;
    }

    public void setModemSuggestedDelay(long j) {
        this.mModemSuggestedDelay = j;
    }

    public long getRetryAfterDisconnectDelay() {
        return this.mApnRetryAfterDisconnectDelay;
    }

    public String toString() {
        if (this.mConfig == null) {
            return "";
        }
        return "RetryManager: mApnType=" + this.mApnType + " mRetryCount=" + this.mRetryCount + " mMaxRetryCount=" + this.mMaxRetryCount + " mCurrentApnIndex=" + this.mCurrentApnIndex + " mSameApnRtryCount=" + this.mSameApnRetryCount + " mModemSuggestedDelay=" + this.mModemSuggestedDelay + " mRetryForever=" + this.mRetryForever + " mInterApnDelay=" + this.mInterApnDelay + " mApnRetryAfterDisconnectDelay=" + this.mApnRetryAfterDisconnectDelay + " mConfig={" + this.mConfig + "}";
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, "[" + this.mApnType + "] " + str);
    }
}
