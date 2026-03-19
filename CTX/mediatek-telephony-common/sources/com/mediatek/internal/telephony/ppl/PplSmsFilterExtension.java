package com.mediatek.internal.telephony.ppl;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.telephony.PhoneNumberUtils;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import com.mediatek.internal.telephony.datasub.DataSubConstants;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import mediatek.telephony.MtkSmsMessage;
import vendor.mediatek.hardware.pplagent.V1_0.IPplAgent;

public class PplSmsFilterExtension extends ContextWrapper implements IPplSmsFilter {
    public static final String INSTRUCTION_KEY_FROM = "From";
    public static final String INSTRUCTION_KEY_SIM_ID = "SimId";
    public static final String INSTRUCTION_KEY_TO = "To";
    public static final String INSTRUCTION_KEY_TYPE = "Type";
    public static final String INTENT_REMOTE_INSTRUCTION_RECEIVED = "com.mediatek.ppl.REMOTE_INSTRUCTION_RECEIVED";
    private static final String TAG = "PPL/PplSmsFilterExtension";
    public static final boolean USER_LOAD = TextUtils.equals(Build.TYPE, DataSubConstants.REASON_MOBILE_DATA_ENABLE_USER);
    private IPplAgent mAgent;
    private final boolean mEnabled;
    private final PplMessageManager mMessageManager;

    public PplSmsFilterExtension(Context context) {
        super(context);
        Log.d(TAG, "PplSmsFilterExtension enter");
        if (!"1".equals(SystemProperties.get("ro.vendor.mtk_privacy_protection_lock"))) {
            this.mAgent = null;
            this.mMessageManager = null;
            this.mEnabled = false;
            return;
        }
        try {
            this.mAgent = IPplAgent.getService();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get PPLAgent", e);
        }
        if (this.mAgent == null) {
            Log.e(TAG, "mAgent is null!");
            this.mMessageManager = null;
            this.mEnabled = false;
        } else {
            this.mMessageManager = new PplMessageManager(context);
            this.mEnabled = true;
            Log.d(TAG, "PplSmsFilterExtension exit");
        }
    }

    private void convertArrayListToByteArray(ArrayList<Byte> arrayList, byte[] bArr) {
        for (int i = 0; i < arrayList.size() && i < bArr.length; i++) {
            bArr[i] = arrayList.get(i).byteValue();
        }
    }

    public byte[] readControlData() {
        if (this.mAgent == null) {
            Log.e(TAG, "[writeControlData] mAgent is null !!!");
            return null;
        }
        try {
            ArrayList<Byte> controlData = this.mAgent.readControlData();
            byte[] bArr = new byte[controlData.size()];
            convertArrayListToByteArray(controlData, bArr);
            return bArr;
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean pplFilter(Bundle bundle) {
        String messageBody;
        String originatingAddress;
        String destinationAddress;
        if (!this.mEnabled) {
            Log.d(TAG, "pplFilter returns false: feature not enabled");
            return false;
        }
        String string = bundle.getString(IPplSmsFilter.KEY_FORMAT);
        boolean z = bundle.getInt(IPplSmsFilter.KEY_SMS_TYPE) == 1;
        int i = bundle.getInt(IPplSmsFilter.KEY_SUB_ID);
        int slotIndex = SubscriptionManager.getSlotIndex(i);
        Log.d(TAG, "pplFilter: subId = " + i + ". simId = " + slotIndex);
        Object[] objArr = (Object[]) bundle.getSerializable(IPplSmsFilter.KEY_PDUS);
        if (objArr == null) {
            messageBody = bundle.getString(IPplSmsFilter.KEY_MSG_CONTENT);
            originatingAddress = bundle.getString(IPplSmsFilter.KEY_SRC_ADDR);
            destinationAddress = bundle.getString(IPplSmsFilter.KEY_DST_ADDR);
            Log.d(TAG, "pplFilter: Read msg directly:" + stringForSecureSms(messageBody));
        } else {
            byte[][] bArr = new byte[objArr.length][];
            for (int i2 = 0; i2 < objArr.length; i2++) {
                bArr[i2] = (byte[]) objArr[i2];
            }
            int length = bArr.length;
            if (length > 1) {
                Log.d(TAG, "pplFilter return false: ppl sms is short msg, count should <= 1 ");
                return false;
            }
            MtkSmsMessage[] mtkSmsMessageArr = new MtkSmsMessage[length];
            for (int i3 = 0; i3 < length; i3++) {
                mtkSmsMessageArr[i3] = MtkSmsMessage.createFromPdu(bArr[i3], string);
            }
            Log.d(TAG, "pplFilter: pdus length " + bArr.length);
            if (mtkSmsMessageArr[0] == null) {
                Log.d(TAG, "pplFilter returns false: message is null");
                return false;
            }
            messageBody = mtkSmsMessageArr[0].getMessageBody();
            Log.d(TAG, "pplFilter: message content is " + stringForSecureSms(messageBody));
            originatingAddress = mtkSmsMessageArr[0].getOriginatingAddress();
            destinationAddress = mtkSmsMessageArr[0].getDestinationAddress();
        }
        if (messageBody == null) {
            Log.d(TAG, "pplFilter returns false: content is null");
            return false;
        }
        PplControlData pplControlDataBuildControlData = PplControlData.buildControlData(readControlData());
        if (pplControlDataBuildControlData == null || !pplControlDataBuildControlData.isEnabled()) {
            Log.d(TAG, "pplFilter returns false: control data is null or ppl is not enabled");
            return false;
        }
        if (z) {
            Log.d(TAG, "pplFilter: dst is " + stringForSecureNumber(destinationAddress));
            if (!matchNumber(destinationAddress, pplControlDataBuildControlData.TrustedNumberList)) {
                Log.d(TAG, "pplFilter returns false: MO number does not match");
                return false;
            }
        } else {
            Log.d(TAG, "pplFilter: src is " + stringForSecureNumber(originatingAddress));
            if (!matchNumber(originatingAddress, pplControlDataBuildControlData.TrustedNumberList)) {
                Log.d(TAG, "pplFilter returns false: MT number does not match");
                return false;
            }
        }
        byte messageType = this.mMessageManager.getMessageType(messageBody);
        if (messageType == -1) {
            Log.d(TAG, "pplFilter returns false: message is not matched");
            return false;
        }
        if (z) {
            if (messageType == 1 || messageType == 3 || messageType == 5 || messageType == 7) {
                Log.d(TAG, "pplFilter returns false: ignore MO command: " + ((int) messageType));
                return false;
            }
        } else if (messageType == 0 || messageType == 2 || messageType == 4 || messageType == 6 || messageType == 8 || messageType == 9 || messageType == 10 || messageType == 11) {
            Log.d(TAG, "pplFilter returns false: ignore MT command: " + ((int) messageType));
            return false;
        }
        Intent intent = new Intent(INTENT_REMOTE_INSTRUCTION_RECEIVED);
        intent.setClassName("com.mediatek.ppl", "com.mediatek.ppl.PplService");
        intent.putExtra(INSTRUCTION_KEY_TYPE, messageType);
        intent.putExtra(INSTRUCTION_KEY_SIM_ID, slotIndex);
        if (z) {
            intent.putExtra(INSTRUCTION_KEY_TO, destinationAddress);
        } else {
            intent.putExtra(INSTRUCTION_KEY_FROM, originatingAddress);
        }
        Log.d(TAG, "start PPL Service");
        startService(intent);
        return true;
    }

    private boolean matchNumber(String str, List<String> list) {
        if (str != null && list != null) {
            Iterator<String> it = list.iterator();
            while (it.hasNext()) {
                if (PhoneNumberUtils.compare(it.next(), str)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    private String stringForSecureNumber(String str) {
        if (USER_LOAD || TextUtils.isEmpty(str)) {
            return "";
        }
        int length = str.length();
        if (length >= 11) {
            return "*******" + str.substring(7, length);
        }
        if (length <= 4) {
            return str.replaceAll("\\w", "*");
        }
        return "***" + str.substring(3, length);
    }

    public static String stringForSecureSms(String str) {
        if (USER_LOAD || TextUtils.isEmpty(str)) {
            return "";
        }
        if (str.length() >= 6) {
            return str.substring(0, 5) + "......";
        }
        return str.replaceAll("\\w", "*");
    }
}
