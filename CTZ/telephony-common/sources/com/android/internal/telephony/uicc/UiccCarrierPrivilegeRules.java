package com.android.internal.telephony.uicc;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.os.AsyncResult;
import android.os.Binder;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.UiccAccessRule;
import android.text.TextUtils;
import com.android.internal.telephony.CommandException;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class UiccCarrierPrivilegeRules extends Handler {
    private static final int ARAD = 0;
    private static final String ARAD_AID = "A00000015144414300";
    private static final int ARAM = 1;
    private static final String ARAM_AID = "A00000015141434C00";
    private static final String CARRIER_PRIVILEGE_AID = "FFFFFFFFFFFF";
    private static final int CLA = 128;
    private static final int COMMAND = 202;
    private static final String DATA = "";
    private static final boolean DBG = false;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 3;
    protected static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 1;
    private static final int EVENT_PKCS15_READ_DONE = 4;
    private static final int EVENT_TRANSMIT_LOGICAL_CHANNEL_DONE = 2;
    private static final String LOG_TAG = "UiccCarrierPrivilegeRules";
    private static final int MAX_RETRY = 1;
    private static final int P1 = 255;
    private static final int P2 = 64;
    private static final int P2_EXTENDED_DATA = 96;
    private static final int P3 = 0;
    private static final int RETRY_INTERVAL_MS = 10000;
    protected static final int STATE_ERROR = 2;
    private static final int STATE_LOADED = 1;
    private static final int STATE_LOADING = 0;
    private static final String TAG_AID_REF_DO = "4F";
    private static final String TAG_ALL_REF_AR_DO = "FF40";
    private static final String TAG_AR_DO = "E3";
    private static final String TAG_DEVICE_APP_ID_REF_DO = "C1";
    private static final String TAG_PERM_AR_DO = "DB";
    private static final String TAG_PKG_REF_DO = "CA";
    private static final String TAG_REF_AR_DO = "E2";
    private static final String TAG_REF_DO = "E1";
    private int mAIDInUse;
    private List<UiccAccessRule> mAccessRules;
    private int mChannelId;
    private Message mLoadedCallback;
    private int mRetryCount;
    private String mRules;
    private AtomicInteger mState;
    private String mStatusMessage;
    private UiccPkcs15 mUiccPkcs15;
    private UiccProfile mUiccProfile;
    private boolean mCheckedRules = false;
    private final Runnable mRetryRunnable = new Runnable() {
        @Override
        public void run() {
            UiccCarrierPrivilegeRules.this.openChannel(UiccCarrierPrivilegeRules.this.mAIDInUse);
        }
    };

    public static class TLV {
        private static final int SINGLE_BYTE_MAX_LENGTH = 128;
        private Integer length;
        private String lengthBytes;
        private String tag;
        private String value;

        public TLV(String str) {
            this.tag = str;
        }

        public String getValue() {
            return this.value == null ? UiccCarrierPrivilegeRules.DATA : this.value;
        }

        public String parseLength(String str) {
            int length = this.tag.length();
            int i = length + 2;
            int i2 = Integer.parseInt(str.substring(length, i), 16);
            if (i2 < 128) {
                this.length = Integer.valueOf(i2 * 2);
                this.lengthBytes = str.substring(length, i);
            } else {
                int i3 = ((i2 - 128) * 2) + i;
                this.length = Integer.valueOf(Integer.parseInt(str.substring(i, i3), 16) * 2);
                this.lengthBytes = str.substring(length, i3);
            }
            UiccCarrierPrivilegeRules.log("TLV parseLength length=" + this.length + "lenghtBytes: " + this.lengthBytes);
            return this.lengthBytes;
        }

        public String parse(String str, boolean z) {
            UiccCarrierPrivilegeRules.log("Parse TLV: " + this.tag);
            if (!str.startsWith(this.tag)) {
                throw new IllegalArgumentException("Tags don't match.");
            }
            int length = this.tag.length();
            if (length + 2 > str.length()) {
                throw new IllegalArgumentException("No length.");
            }
            parseLength(str);
            int length2 = length + this.lengthBytes.length();
            UiccCarrierPrivilegeRules.log("index=" + length2 + " length=" + this.length + "data.length=" + str.length());
            int length3 = str.length() - (this.length.intValue() + length2);
            if (length3 < 0) {
                throw new IllegalArgumentException("Not enough data.");
            }
            if (z && length3 != 0) {
                throw new IllegalArgumentException("Did not consume all.");
            }
            this.value = str.substring(length2, this.length.intValue() + length2);
            UiccCarrierPrivilegeRules.log("Got TLV: " + this.tag + "," + this.length + "," + this.value);
            return str.substring(length2 + this.length.intValue());
        }
    }

    private void openChannel(int i) {
        this.mUiccProfile.iccOpenLogicalChannel(i == 0 ? ARAD_AID : ARAM_AID, 0, obtainMessage(1, 0, i, null));
    }

    public UiccCarrierPrivilegeRules(UiccProfile uiccProfile, Message message) {
        log("Creating UiccCarrierPrivilegeRules");
        this.mUiccProfile = uiccProfile;
        this.mState = new AtomicInteger(0);
        this.mStatusMessage = "Not loaded.";
        this.mLoadedCallback = message;
        this.mRules = DATA;
        this.mAccessRules = new ArrayList();
        this.mAIDInUse = 0;
        openChannel(this.mAIDInUse);
    }

    public boolean areCarrierPriviligeRulesLoaded() {
        return this.mState.get() != 0;
    }

    public boolean hasCarrierPrivilegeRules() {
        return (this.mState.get() == 0 || this.mAccessRules == null || this.mAccessRules.size() <= 0) ? false : true;
    }

    public List<String> getPackageNames() {
        ArrayList arrayList = new ArrayList();
        if (this.mAccessRules != null) {
            for (UiccAccessRule uiccAccessRule : this.mAccessRules) {
                if (!TextUtils.isEmpty(uiccAccessRule.getPackageName())) {
                    arrayList.add(uiccAccessRule.getPackageName());
                }
            }
        }
        return arrayList;
    }

    public List<UiccAccessRule> getAccessRules() {
        if (this.mAccessRules == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(this.mAccessRules);
    }

    public int getCarrierPrivilegeStatus(Signature signature, String str) {
        int i = this.mState.get();
        if (i == 0) {
            return -1;
        }
        if (i == 2) {
            return -2;
        }
        Iterator<UiccAccessRule> it = this.mAccessRules.iterator();
        while (it.hasNext()) {
            int carrierPrivilegeStatus = it.next().getCarrierPrivilegeStatus(signature, str);
            if (carrierPrivilegeStatus != 0) {
                return carrierPrivilegeStatus;
            }
        }
        return 0;
    }

    public int getCarrierPrivilegeStatus(PackageManager packageManager, String str) {
        try {
            if (!hasCarrierPrivilegeRules()) {
                int i = this.mState.get();
                if (i == 0) {
                    return -1;
                }
                if (i != 2) {
                    return 0;
                }
                return -2;
            }
            return getCarrierPrivilegeStatus(packageManager.getPackageInfo(str, 32832));
        } catch (PackageManager.NameNotFoundException e) {
            log("Package " + str + " not found for carrier privilege status check");
            return 0;
        }
    }

    public int getCarrierPrivilegeStatus(PackageInfo packageInfo) {
        int i = this.mState.get();
        if (i == 0) {
            return -1;
        }
        if (i == 2) {
            return -2;
        }
        Iterator<UiccAccessRule> it = this.mAccessRules.iterator();
        while (it.hasNext()) {
            int carrierPrivilegeStatus = it.next().getCarrierPrivilegeStatus(packageInfo);
            if (carrierPrivilegeStatus != 0) {
                return carrierPrivilegeStatus;
            }
        }
        return 0;
    }

    public int getCarrierPrivilegeStatusForCurrentTransaction(PackageManager packageManager) {
        return getCarrierPrivilegeStatusForUid(packageManager, Binder.getCallingUid());
    }

    public int getCarrierPrivilegeStatusForUid(PackageManager packageManager, int i) {
        for (String str : packageManager.getPackagesForUid(i)) {
            int carrierPrivilegeStatus = getCarrierPrivilegeStatus(packageManager, str);
            if (carrierPrivilegeStatus != 0) {
                return carrierPrivilegeStatus;
            }
        }
        return 0;
    }

    public List<String> getCarrierPackageNamesForIntent(PackageManager packageManager, Intent intent) {
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        arrayList2.addAll(packageManager.queryBroadcastReceivers(intent, 0));
        arrayList2.addAll(packageManager.queryIntentContentProviders(intent, 0));
        arrayList2.addAll(packageManager.queryIntentActivities(intent, 0));
        arrayList2.addAll(packageManager.queryIntentServices(intent, 0));
        Iterator it = arrayList2.iterator();
        while (it.hasNext()) {
            String packageName = getPackageName((ResolveInfo) it.next());
            if (packageName != null) {
                int carrierPrivilegeStatus = getCarrierPrivilegeStatus(packageManager, packageName);
                if (carrierPrivilegeStatus == 1) {
                    arrayList.add(packageName);
                } else if (carrierPrivilegeStatus != 0) {
                    return null;
                }
            }
        }
        return arrayList;
    }

    private String getPackageName(ResolveInfo resolveInfo) {
        if (resolveInfo.activityInfo != null) {
            return resolveInfo.activityInfo.packageName;
        }
        if (resolveInfo.serviceInfo != null) {
            return resolveInfo.serviceInfo.packageName;
        }
        if (resolveInfo.providerInfo != null) {
            return resolveInfo.providerInfo.packageName;
        }
        return null;
    }

    @Override
    public void handleMessage(Message message) {
        this.mAIDInUse = message.arg2;
        switch (message.what) {
            case 1:
                log("EVENT_OPEN_LOGICAL_CHANNEL_DONE");
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception == null && asyncResult.result != null) {
                    this.mChannelId = ((int[]) asyncResult.result)[0];
                    this.mUiccProfile.iccTransmitApduLogicalChannel(this.mChannelId, 128, COMMAND, 255, 64, 0, DATA, obtainMessage(2, this.mChannelId, this.mAIDInUse));
                } else if ((asyncResult.exception instanceof CommandException) && this.mRetryCount < 1 && ((CommandException) asyncResult.exception).getCommandError() == CommandException.Error.MISSING_RESOURCE) {
                    this.mRetryCount++;
                    removeCallbacks(this.mRetryRunnable);
                    postDelayed(this.mRetryRunnable, 10000L);
                } else {
                    if (this.mAIDInUse == 0) {
                        this.mRules = DATA;
                        openChannel(1);
                    }
                    if (this.mAIDInUse == 1) {
                        if (this.mCheckedRules) {
                            updateState(1, "Success!");
                        } else {
                            log("No ARA, try ARF next.");
                            this.mUiccPkcs15 = new UiccPkcs15(this.mUiccProfile, obtainMessage(4));
                        }
                    }
                }
                break;
            case 2:
                log("EVENT_TRANSMIT_LOGICAL_CHANNEL_DONE");
                AsyncResult asyncResult2 = (AsyncResult) message.obj;
                if (asyncResult2.exception == null && asyncResult2.result != null) {
                    IccIoResult iccIoResult = (IccIoResult) asyncResult2.result;
                    if (iccIoResult.sw1 == 144 && iccIoResult.sw2 == 0 && iccIoResult.payload != null && iccIoResult.payload.length > 0) {
                        try {
                            this.mRules += IccUtils.bytesToHexString(iccIoResult.payload).toUpperCase(Locale.US);
                            if (isDataComplete()) {
                                this.mAccessRules.addAll(parseRules(this.mRules));
                                if (this.mAIDInUse == 0) {
                                    this.mCheckedRules = true;
                                } else {
                                    updateState(1, "Success!");
                                }
                            } else {
                                this.mUiccProfile.iccTransmitApduLogicalChannel(this.mChannelId, 128, COMMAND, 255, 96, 0, DATA, obtainMessage(2, this.mChannelId, this.mAIDInUse));
                                break;
                            }
                        } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                            if (this.mAIDInUse == 1) {
                                updateState(2, "Error parsing rules: " + e);
                            }
                        }
                    } else if (this.mAIDInUse == 1) {
                        updateState(2, "Invalid response: payload=" + iccIoResult.payload + " sw1=" + iccIoResult.sw1 + " sw2=" + iccIoResult.sw2);
                    }
                } else if (this.mAIDInUse == 1) {
                    updateState(2, "Error reading value from SIM.");
                }
                this.mUiccProfile.iccCloseLogicalChannel(this.mChannelId, obtainMessage(3, 0, this.mAIDInUse));
                this.mChannelId = -1;
                break;
            case 3:
                log("EVENT_CLOSE_LOGICAL_CHANNEL_DONE");
                if (this.mAIDInUse == 0) {
                    this.mRules = DATA;
                    openChannel(1);
                }
                break;
            case 4:
                log("EVENT_PKCS15_READ_DONE");
                if (this.mUiccPkcs15 == null || this.mUiccPkcs15.getRules() == null) {
                    updateState(2, "No ARA or ARF.");
                } else {
                    Iterator<String> it = this.mUiccPkcs15.getRules().iterator();
                    while (it.hasNext()) {
                        this.mAccessRules.add(new UiccAccessRule(IccUtils.hexStringToBytes(it.next()), DATA, 0L));
                    }
                    updateState(1, "Success!");
                }
                break;
            default:
                Rlog.e(LOG_TAG, "Unknown event " + message.what);
                break;
        }
    }

    private boolean isDataComplete() {
        log("isDataComplete mRules:" + this.mRules);
        if (this.mRules.startsWith(TAG_ALL_REF_AR_DO)) {
            TLV tlv = new TLV(TAG_ALL_REF_AR_DO);
            String length = tlv.parseLength(this.mRules);
            log("isDataComplete lengthBytes: " + length);
            if (this.mRules.length() == TAG_ALL_REF_AR_DO.length() + length.length() + tlv.length.intValue()) {
                log("isDataComplete yes");
                return true;
            }
            log("isDataComplete no");
            return false;
        }
        throw new IllegalArgumentException("Tags don't match.");
    }

    private static List<UiccAccessRule> parseRules(String str) {
        log("Got rules: " + str);
        TLV tlv = new TLV(TAG_ALL_REF_AR_DO);
        tlv.parse(str, true);
        String str2 = tlv.value;
        ArrayList arrayList = new ArrayList();
        while (!str2.isEmpty()) {
            TLV tlv2 = new TLV(TAG_REF_AR_DO);
            str2 = tlv2.parse(str2, false);
            UiccAccessRule refArdo = parseRefArdo(tlv2.value);
            if (refArdo == null) {
                Rlog.e(LOG_TAG, "Skip unrecognized rule." + tlv2.value);
            } else {
                arrayList.add(refArdo);
            }
        }
        return arrayList;
    }

    private static UiccAccessRule parseRefArdo(String str) {
        String str2;
        String str3;
        String str4;
        log("Got rule: " + str);
        String str5 = null;
        String str6 = null;
        while (!str.isEmpty()) {
            if (str.startsWith(TAG_REF_DO)) {
                TLV tlv = new TLV(TAG_REF_DO);
                str = tlv.parse(str, false);
                TLV tlv2 = new TLV(TAG_DEVICE_APP_ID_REF_DO);
                if (!tlv.value.startsWith(TAG_AID_REF_DO)) {
                    if (tlv.value.startsWith(TAG_DEVICE_APP_ID_REF_DO)) {
                        str2 = tlv2.parse(tlv.value, false);
                        str3 = tlv2.value;
                    } else {
                        return null;
                    }
                } else {
                    TLV tlv3 = new TLV(TAG_AID_REF_DO);
                    String str7 = tlv3.parse(tlv.value, false);
                    if (!tlv3.lengthBytes.equals("06") || !tlv3.value.equals(CARRIER_PRIVILEGE_AID) || str7.isEmpty() || !str7.startsWith(TAG_DEVICE_APP_ID_REF_DO)) {
                        return null;
                    }
                    str2 = tlv2.parse(str7, false);
                    str3 = tlv2.value;
                }
                if (!str2.isEmpty()) {
                    if (!str2.startsWith(TAG_PKG_REF_DO)) {
                        return null;
                    }
                    TLV tlv4 = new TLV(TAG_PKG_REF_DO);
                    tlv4.parse(str2, true);
                    str4 = new String(IccUtils.hexStringToBytes(tlv4.value));
                } else {
                    str4 = null;
                }
                String str8 = str3;
                str6 = str4;
                str5 = str8;
            } else if (str.startsWith(TAG_AR_DO)) {
                TLV tlv5 = new TLV(TAG_AR_DO);
                str = tlv5.parse(str, false);
                String str9 = tlv5.value;
                while (!str9.isEmpty() && !str9.startsWith(TAG_PERM_AR_DO)) {
                    str9 = new TLV(str9.substring(0, 2)).parse(str9, false);
                }
                if (str9.isEmpty()) {
                    return null;
                }
                new TLV(TAG_PERM_AR_DO).parse(str9, true);
            } else {
                throw new RuntimeException("Invalid Rule type");
            }
        }
        return new UiccAccessRule(IccUtils.hexStringToBytes(str5), str6, 0L);
    }

    protected void updateState(int i, String str) {
        this.mState.set(i);
        if (this.mLoadedCallback != null) {
            this.mLoadedCallback.sendToTarget();
        }
        this.mStatusMessage = str;
    }

    protected static void log(String str) {
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("UiccCarrierPrivilegeRules: " + this);
        printWriter.println(" mState=" + getStateString(this.mState.get()));
        printWriter.println(" mStatusMessage='" + this.mStatusMessage + "'");
        if (this.mAccessRules != null) {
            printWriter.println(" mAccessRules: ");
            Iterator<UiccAccessRule> it = this.mAccessRules.iterator();
            while (it.hasNext()) {
                printWriter.println("  rule='" + it.next() + "'");
            }
        } else {
            printWriter.println(" mAccessRules: null");
        }
        if (this.mUiccPkcs15 != null) {
            printWriter.println(" mUiccPkcs15: " + this.mUiccPkcs15);
            this.mUiccPkcs15.dump(fileDescriptor, printWriter, strArr);
        } else {
            printWriter.println(" mUiccPkcs15: null");
        }
        printWriter.flush();
    }

    private String getStateString(int i) {
        switch (i) {
            case 0:
                return "STATE_LOADING";
            case 1:
                return "STATE_LOADED";
            case 2:
                return "STATE_ERROR";
            default:
                return "UNKNOWN";
        }
    }
}
