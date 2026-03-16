package android.telephony;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.MediaStore;
import com.android.internal.telephony.Dsds;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.ISub;
import com.android.internal.telephony.ITelephonyRegistry;
import com.android.internal.telephony.PhoneConstants;
import java.util.ArrayList;
import java.util.List;

public class SubscriptionManager {
    public static final String CARRIER_NAME = "carrier_name";
    public static final String COLOR = "color";
    public static final int COLOR_1 = 0;
    public static final int COLOR_2 = 1;
    public static final int COLOR_3 = 2;
    public static final int COLOR_4 = 3;
    public static final int COLOR_DEFAULT = 0;
    public static final Uri CONTENT_URI = Uri.parse("content://telephony/siminfo");
    public static final String DATA_ROAMING = "data_roaming";
    public static final int DATA_ROAMING_DEFAULT = 0;
    public static final int DATA_ROAMING_DISABLE = 0;
    public static final int DATA_ROAMING_ENABLE = 1;
    private static final boolean DBG = false;
    public static final int DEFAULT_NAME_RES = 17039374;
    public static final int DEFAULT_PHONE_INDEX = Integer.MAX_VALUE;
    public static final int DEFAULT_SIM_SLOT_INDEX = Integer.MAX_VALUE;
    public static final int DEFAULT_SUBSCRIPTION_ID = Integer.MAX_VALUE;
    public static final String DISPLAY_NAME = "display_name";
    public static final int DISPLAY_NUMBER_DEFAULT = 1;
    public static final int DISPLAY_NUMBER_FIRST = 1;
    public static final String DISPLAY_NUMBER_FORMAT = "display_number_format";
    public static final int DISPLAY_NUMBER_LAST = 2;
    public static final int DISPLAY_NUMBER_NONE = 0;
    public static final int DUMMY_SUBSCRIPTION_ID_BASE = -2;
    public static final String ICC_ID = "icc_id";
    public static final int INVALID_PHONE_INDEX = -1;
    public static final int INVALID_SIM_SLOT_INDEX = -1;
    public static final int INVALID_SUBSCRIPTION_ID = -1;
    private static final String LOG_TAG = "SubscriptionManager";
    public static final int MAX_SUBSCRIPTION_ID_VALUE = 2147483646;
    public static final String MCC = "mcc";
    public static final int MIN_SUBSCRIPTION_ID_VALUE = 0;
    public static final String MNC = "mnc";
    public static final String NAME_SOURCE = "name_source";
    public static final int NAME_SOURCE_DEFAULT_SOURCE = 0;
    public static final int NAME_SOURCE_SIM_SOURCE = 1;
    public static final int NAME_SOURCE_UNDEFINDED = -1;
    public static final int NAME_SOURCE_USER_INPUT = 2;
    public static final String NUMBER = "number";
    public static final int SIM_NOT_INSERTED = -1;
    public static final String SIM_SLOT_INDEX = "sim_id";
    public static final String SUB_DEFAULT_CHANGED_ACTION = "android.intent.action.SUB_DEFAULT_CHANGED";
    public static final String UNIQUE_KEY_SUBSCRIPTION_ID = "_id";
    private static final boolean VDBG = false;
    private final Context mContext;

    public static class OnSubscriptionsChangedListener {
        public static final String PERMISSION_ON_SUBSCRIPTIONS_CHANGED = "android.permission.READ_PHONE_STATE";
        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                OnSubscriptionsChangedListener.this.onSubscriptionsChanged();
            }
        };
        IOnSubscriptionsChangedListener callback = new IOnSubscriptionsChangedListener.Stub() {
            @Override
            public void onSubscriptionsChanged() {
                OnSubscriptionsChangedListener.this.mHandler.sendEmptyMessage(0);
            }
        };

        public void onSubscriptionsChanged() {
        }

        private void log(String s) {
            Rlog.d(SubscriptionManager.LOG_TAG, s);
        }
    }

    public SubscriptionManager(Context context) {
        this.mContext = context;
    }

    public static SubscriptionManager from(Context context) {
        return (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
    }

    public void addOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener) {
        String pkgForDebug = this.mContext != null ? this.mContext.getPackageName() : MediaStore.UNKNOWN_STRING;
        try {
            ITelephonyRegistry tr = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
            if (tr != null) {
                tr.addOnSubscriptionsChangedListener(pkgForDebug, listener.callback);
            }
        } catch (RemoteException e) {
        }
    }

    public void removeOnSubscriptionsChangedListener(OnSubscriptionsChangedListener listener) {
        String pkgForDebug = this.mContext != null ? this.mContext.getPackageName() : MediaStore.UNKNOWN_STRING;
        try {
            ITelephonyRegistry tr = ITelephonyRegistry.Stub.asInterface(ServiceManager.getService("telephony.registry"));
            if (tr != null) {
                tr.removeOnSubscriptionsChangedListener(pkgForDebug, listener.callback);
            }
        } catch (RemoteException e) {
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfo(int subId) {
        if (!isValidSubscriptionId(subId)) {
            logd("[getActiveSubscriptionInfo]- invalid subId");
            return null;
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return null;
            }
            SubscriptionInfo subInfo = iSub.getActiveSubscriptionInfo(subId);
            return subInfo;
        } catch (RemoteException e) {
            return null;
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfoForIccIndex(String iccId) {
        if (iccId == null) {
            logd("[getActiveSubscriptionInfoForIccIndex]- null iccid");
            return null;
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return null;
            }
            SubscriptionInfo result = iSub.getActiveSubscriptionInfoForIccId(iccId);
            return result;
        } catch (RemoteException e) {
            return null;
        }
    }

    public SubscriptionInfo getActiveSubscriptionInfoForSimSlotIndex(int slotIdx) {
        if (!isValidSlotId(slotIdx)) {
            logd("[getActiveSubscriptionInfoForSimSlotIndex]- invalid slotIdx");
            return null;
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return null;
            }
            SubscriptionInfo result = iSub.getActiveSubscriptionInfoForSimSlotIndex(slotIdx);
            return result;
        } catch (RemoteException e) {
            return null;
        }
    }

    public List<SubscriptionInfo> getAllSubscriptionInfoList() {
        List<SubscriptionInfo> result = null;
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub != null) {
                result = iSub.getAllSubInfoList();
            }
        } catch (RemoteException e) {
        }
        if (result == null) {
            return new ArrayList<>();
        }
        return result;
    }

    public List<SubscriptionInfo> getActiveSubscriptionInfoList() {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return null;
            }
            List<SubscriptionInfo> result = iSub.getActiveSubscriptionInfoList();
            return result;
        } catch (RemoteException e) {
            return null;
        }
    }

    public int getAllSubscriptionInfoCount() {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return 0;
            }
            int result = iSub.getAllSubInfoCount();
            return result;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int getActiveSubscriptionInfoCount() {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return 0;
            }
            int result = iSub.getActiveSubInfoCount();
            return result;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int getActiveSubscriptionInfoCountMax() {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return 0;
            }
            int result = iSub.getActiveSubInfoCountMax();
            return result;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public Uri addSubscriptionInfoRecord(String iccId, int slotId) {
        if (iccId == null) {
            logd("[addSubscriptionInfoRecord]- null iccId");
        }
        if (!isValidSlotId(slotId)) {
            logd("[addSubscriptionInfoRecord]- invalid slotId");
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub != null) {
                iSub.addSubInfoRecord(iccId, slotId);
                return null;
            }
            return null;
        } catch (RemoteException e) {
            return null;
        }
    }

    public int setIconTint(int tint, int subId) {
        if (!isValidSubscriptionId(subId)) {
            logd("[setIconTint]- fail");
            return -1;
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return 0;
            }
            int result = iSub.setIconTint(tint, subId);
            return result;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int setDisplayName(String displayName, int subId) {
        return setDisplayName(displayName, subId, -1L);
    }

    public int setDisplayName(String displayName, int subId, long nameSource) {
        if (!isValidSubscriptionId(subId)) {
            logd("[setDisplayName]- fail");
            return -1;
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return 0;
            }
            int result = iSub.setDisplayNameUsingSrc(displayName, subId, nameSource);
            return result;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int setDisplayNumber(String number, int subId) {
        if (number == null || !isValidSubscriptionId(subId)) {
            logd("[setDisplayNumber]- fail");
            return -1;
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return 0;
            }
            int result = iSub.setDisplayNumber(number, subId);
            return result;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public int setDataRoaming(int roaming, int subId) {
        if (roaming < 0 || !isValidSubscriptionId(subId)) {
            logd("[setDataRoaming]- fail");
            return -1;
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return 0;
            }
            int result = iSub.setDataRoaming(roaming, subId);
            return result;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public static int getSlotId(int subId) {
        if (!isValidSubscriptionId(subId)) {
            logd("[getSlotId]- fail");
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return -1;
            }
            int result = iSub.getSlotId(subId);
            return result;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static int[] getSubId(int slotId) {
        if (!isValidSlotId(slotId)) {
            logd("[getSubId]- fail");
            return null;
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return null;
            }
            int[] subId = iSub.getSubId(slotId);
            return subId;
        } catch (RemoteException e) {
            return null;
        }
    }

    public static int getPhoneId(int subId) {
        if (!isValidSubscriptionId(subId)) {
            logd("[getPhoneId]- fail");
            return -1;
        }
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return -1;
            }
            int result = iSub.getPhoneId(subId);
            return result;
        } catch (RemoteException e) {
            return -1;
        }
    }

    private static void logd(String msg) {
        Rlog.d(LOG_TAG, msg);
    }

    public static int getDefaultSubId() {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return -1;
            }
            int subId = iSub.getDefaultSubId();
            return subId;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public static int getDefaultVoiceSubId() {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return -1;
            }
            int subId = iSub.getDefaultVoiceSubId();
            return subId;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void setDefaultVoiceSubId(int subId) {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub != null) {
                iSub.setDefaultVoiceSubId(subId);
            }
        } catch (RemoteException e) {
        }
    }

    public SubscriptionInfo getDefaultVoiceSubscriptionInfo() {
        return getActiveSubscriptionInfo(getDefaultVoiceSubId());
    }

    public static int getDefaultVoicePhoneId() {
        return getPhoneId(getDefaultVoiceSubId());
    }

    public static int getDefaultSmsSubId() {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return -1;
            }
            int subId = iSub.getDefaultSmsSubId();
            return subId;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void setDefaultSmsSubId(int subId) {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub != null) {
                iSub.setDefaultSmsSubId(subId);
            }
        } catch (RemoteException e) {
        }
    }

    public SubscriptionInfo getDefaultSmsSubscriptionInfo() {
        return getActiveSubscriptionInfo(getDefaultSmsSubId());
    }

    public int getDefaultSmsPhoneId() {
        return getPhoneId(getDefaultSmsSubId());
    }

    public static int getDefaultDataSubId() {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub == null) {
                return -1;
            }
            int subId = iSub.getDefaultDataSubId();
            return subId;
        } catch (RemoteException e) {
            return -1;
        }
    }

    public void setDefaultDataSubId(int subId) {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub != null) {
                iSub.setDefaultDataSubId(subId);
            }
        } catch (RemoteException e) {
        }
    }

    public SubscriptionInfo getDefaultDataSubscriptionInfo() {
        return getActiveSubscriptionInfo(getDefaultDataSubId());
    }

    public int getDefaultDataPhoneId() {
        return getPhoneId(getDefaultDataSubId());
    }

    public void clearSubscriptionInfo() {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub != null) {
                iSub.clearSubInfo();
            }
        } catch (RemoteException e) {
        }
    }

    public boolean allDefaultsSelected() {
        return isValidSubscriptionId(getDefaultDataSubId()) && isValidSubscriptionId(getDefaultSmsSubId()) && isValidSubscriptionId(getDefaultVoiceSubId());
    }

    public void clearDefaultsForInactiveSubIds() {
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub != null) {
                iSub.clearDefaultsForInactiveSubIds();
            }
        } catch (RemoteException e) {
        }
    }

    public static boolean isValidSubscriptionId(int subId) {
        return subId > -1;
    }

    public static boolean isUsableSubIdValue(int subId) {
        return subId >= 0 && subId <= 2147483646;
    }

    public static boolean isValidSlotId(int slotId) {
        return slotId >= 0 && slotId < TelephonyManager.getDefault().getSimCount();
    }

    public static boolean isValidPhoneId(int phoneId) {
        return phoneId >= 0 && phoneId < TelephonyManager.getDefault().getPhoneCount();
    }

    public static void putPhoneIdAndSubIdExtra(Intent intent, int phoneId) {
        int[] subIds = getSubId(phoneId);
        if (subIds != null && subIds.length > 0) {
            putPhoneIdAndSubIdExtra(intent, phoneId, subIds[0]);
        } else {
            logd("putPhoneIdAndSubIdExtra: no valid subs");
        }
    }

    public static void putPhoneIdAndSubIdExtra(Intent intent, int phoneId, int subId) {
        intent.putExtra(PhoneConstants.SUBSCRIPTION_KEY, subId);
        intent.putExtra("phone", phoneId);
        intent.putExtra(PhoneConstants.SLOT_KEY, phoneId);
    }

    public int[] getActiveSubscriptionIdList() {
        int[] subId = null;
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub != null) {
                subId = iSub.getActiveSubIdList();
            }
        } catch (RemoteException e) {
        }
        if (subId == null) {
            return new int[0];
        }
        return subId;
    }

    public boolean isNetworkRoaming(int subId) {
        int phoneId = getPhoneId(subId);
        if (phoneId < 0) {
            return false;
        }
        return TelephonyManager.getDefault().isNetworkRoaming(subId);
    }

    public static int getSimStateForSubscriber(int subId) {
        int simState;
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            simState = iSub.getSimStateForSubscriber(subId);
        } catch (RemoteException e) {
            simState = 0;
        }
        logd("getSimStateForSubscriber: simState=" + simState + " subId=" + subId);
        return simState;
    }

    public static int getDataActivePhoneId() {
        int simId = Dsds.defaultSimId().ordinal();
        try {
            ISub iSub = ISub.Stub.asInterface(ServiceManager.getService("isub"));
            if (iSub != null) {
                return iSub.getDataActivePhoneId();
            }
            return simId;
        } catch (RemoteException e) {
            return simId;
        }
    }
}
