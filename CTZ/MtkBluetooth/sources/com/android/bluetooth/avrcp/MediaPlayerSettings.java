package com.android.bluetooth.avrcp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;
import com.android.bluetooth.avrcp.AvrcpTargetService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MediaPlayerSettings {
    public static final int ATTRIBUTE_EQUALIZER = 1;
    public static final int ATTRIBUTE_NOTSUPPORTED = -1;
    public static final int ATTRIBUTE_REPEATMODE = 2;
    public static final int ATTRIBUTE_SCANMODE = 4;
    public static final int ATTRIBUTE_SHUFFLEMODE = 3;
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final String BLUETOOTH_PERM = "android.permission.BLUETOOTH";
    private static final String CMDGET = "get";
    private static final String CMDSET = "set";
    private static final String COMMAND = "command";
    private static final String EXTRA_ATTIBUTE_ID_ARRAY = "Attributes";
    private static final String EXTRA_ATTRIBUTE_ID = "Attribute";
    private static final String EXTRA_ATTRIBUTE_STRING_ARRAY = "AttributeStrings";
    private static final String EXTRA_ATTRIB_VALUE_PAIRS = "AttribValuePairs";
    private static final String EXTRA_GET_COMMAND = "commandExtra";
    private static final String EXTRA_GET_RESPONSE = "Response";
    private static final String EXTRA_VALUE_ID_ARRAY = "Values";
    private static final String EXTRA_VALUE_STRING_ARRAY = "ValueStrings";
    private static final int GET_ATTRIBUTE_IDS = 0;
    private static final int GET_ATTRIBUTE_TEXT = 2;
    private static final int GET_ATTRIBUTE_VALUES = 4;
    private static final int GET_INVALID = 255;
    private static final int GET_VALUE_IDS = 1;
    private static final int GET_VALUE_TEXT = 3;
    private static final int INTERNAL_ERROR = 3;
    private static final int MESSAGE_PLAYERSETTINGS_TIMEOUT = 602;
    private static final int NOTIFY_ATTRIBUTE_VALUES = 5;
    public static final int NUMPLAYER_ATTRIBUTE = 2;
    private static final int OPERATION_SUCCESSFUL = 4;
    private static final String PLAYERSETTINGS_REQUEST = "org.codeaurora.music.playersettingsrequest";
    private static final String PLAYERSETTINGS_RESPONSE = "org.codeaurora.music.playersettingsresponse";
    private static final int SET_ATTRIBUTE_VALUES = 6;
    private static final String TAG = "NewAvrcpMediaPlayerSettings";
    public static final int VALUE_INVALID = 0;
    public static final int VALUE_REPEATMODE_ALL = 3;
    public static final int VALUE_REPEATMODE_OFF = 1;
    public static final int VALUE_REPEATMODE_SINGLE = 2;
    public static final int VALUE_SHUFFLEMODE_ALL = 2;
    public static final int VALUE_SHUFFLEMODE_OFF = 1;
    private Context mContext;
    private Handler mHandler;
    private Looper mLooper;
    private ArrayList<Integer> mPendingCmds;
    private ArrayList<Integer> mPendingSetAttributes;
    private PlayerSettings mPlayerSettings;
    private AvrcpTargetService.ListCallback mPlayerSettingsCb;
    private localPlayerSettings settingValues;
    private static final boolean DEBUG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static boolean isAppSettingOn = false;
    private static final String[] valTextRepeate = {"Off", "Single track", "All tracks"};
    private static final String[] valTextShuffle = {"Off", "All tracks"};
    private static final byte[] valTextRepeateIds = {1, 2, 3};
    private static final byte[] valTextShuffleIds = {1, 2};
    private Map<Integer, Object> mCallbacks = new HashMap();
    private byte[] def_attrib = {2, 3};
    private byte[] value_repmode = {1, 2, 3};
    private byte[] value_shufmode = {1, 2};
    private byte[] value_default = {0};
    private final String UPDATE_ATTRIBUTES = "UpdateSupportedAttributes";
    private final String UPDATE_VALUES = "UpdateSupportedValues";
    private final String UPDATE_ATTRIB_VALUE = "UpdateCurrentValues";
    private final String UPDATE_ATTRIB_TEXT = "UpdateAttributesText";
    private final String UPDATE_VALUE_TEXT = "UpdateValuesText";
    private BroadcastReceiver mPlayerSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MediaPlayerSettings.PLAYERSETTINGS_RESPONSE)) {
                int intExtra = intent.getIntExtra(MediaPlayerSettings.EXTRA_GET_RESPONSE, 255);
                boolean z = false;
                MediaPlayerSettings.d("PLAYERSETTINGS_RESPONSE");
                synchronized (MediaPlayerSettings.this.mPendingCmds) {
                    Integer num = new Integer(intExtra);
                    if (MediaPlayerSettings.this.mPendingCmds.contains(num)) {
                        if (intExtra == 6) {
                            MediaPlayerSettings.d("Response received for SET_ATTRIBUTE_VALUES");
                            z = true;
                        }
                        MediaPlayerSettings.this.mHandler.removeMessages(MediaPlayerSettings.MESSAGE_PLAYERSETTINGS_TIMEOUT);
                        MediaPlayerSettings.this.mPendingCmds.remove(num);
                    }
                }
                MediaPlayerSettings.d("getResponse " + intExtra);
                if (intExtra != 6) {
                    switch (intExtra) {
                        case 0:
                            byte[] byteArrayExtra = intent.getByteArrayExtra(MediaPlayerSettings.EXTRA_ATTIBUTE_ID_ARRAY);
                            byte length = (byte) byteArrayExtra.length;
                            MediaPlayerSettings.d("GET_ATTRIBUTE_IDS " + ((int) length));
                            MediaPlayerSettings.this.getListPlayerAttributeRsp(length, byteArrayExtra);
                            return;
                        case 1:
                            byte[] byteArrayExtra2 = intent.getByteArrayExtra(MediaPlayerSettings.EXTRA_VALUE_ID_ARRAY);
                            byte length2 = (byte) byteArrayExtra2.length;
                            MediaPlayerSettings.d("GET_VALUE_IDS " + ((int) length2));
                            MediaPlayerSettings.this.getListPlayerAttributeValuesRsp(length2, byteArrayExtra2);
                            return;
                        case 2:
                            MediaPlayerSettings.d("GET_ATTRIBUTE_TEXT");
                            String[] stringArrayExtra = intent.getStringArrayExtra(MediaPlayerSettings.EXTRA_ATTRIBUTE_STRING_ARRAY);
                            MediaPlayerSettings.this.getPlayerAttributeTextRsp(MediaPlayerSettings.this.mPlayerSettings.attrIds.length, MediaPlayerSettings.this.mPlayerSettings.attrIds, stringArrayExtra.length, stringArrayExtra);
                            MediaPlayerSettings.d("mPlayerSettings.attrIds " + MediaPlayerSettings.this.mPlayerSettings.attrIds.length);
                            return;
                        case 3:
                            MediaPlayerSettings.d("GET_VALUE_TEXT");
                            String[] stringArrayExtra2 = intent.getStringArrayExtra(MediaPlayerSettings.EXTRA_VALUE_STRING_ARRAY);
                            MediaPlayerSettings.this.getPlayerAttributeTextValueRsp(MediaPlayerSettings.this.mPlayerSettings.attrIds.length, MediaPlayerSettings.this.mPlayerSettings.attrIds, stringArrayExtra2.length, stringArrayExtra2);
                            return;
                        case 4:
                            byte[] byteArrayExtra3 = intent.getByteArrayExtra(MediaPlayerSettings.EXTRA_ATTRIB_VALUE_PAIRS);
                            MediaPlayerSettings.this.updateLocalPlayerSettings(byteArrayExtra3);
                            byte length3 = (byte) byteArrayExtra3.length;
                            MediaPlayerSettings.d("GET_ATTRIBUTE_VALUES " + ((int) length3));
                            MediaPlayerSettings.this.getPlayerAttributeValueRsp(length3, byteArrayExtra3);
                            return;
                        default:
                            return;
                    }
                }
                byte[] byteArrayExtra4 = intent.getByteArrayExtra(MediaPlayerSettings.EXTRA_ATTRIB_VALUE_PAIRS);
                MediaPlayerSettings.this.updateLocalPlayerSettings(byteArrayExtra4);
                Log.v(MediaPlayerSettings.TAG, "SET_ATTRIBUTE_VALUES: " + z);
                if (z) {
                    Log.v(MediaPlayerSettings.TAG, "Respond to SET_ATTRIBUTE_VALUES request");
                    if (MediaPlayerSettings.this.checkPlayerAttributeResponse(byteArrayExtra4)) {
                        MediaPlayerSettings.this.setPlayerAppSettingRsp(4);
                    } else {
                        MediaPlayerSettings.this.setPlayerAppSettingRsp(3);
                    }
                }
                MediaPlayerSettings.d("Send Player app attribute changed update");
                MediaPlayerSettings.this.sendPlayerAppSettingsUpdate(true);
            }
        }
    };

    interface GetAppSettingChangeCallback {
        void run(byte b, byte[] bArr);
    }

    interface GetListPlayerAttributeCallback {
        void run(byte b, byte[] bArr);
    }

    interface GetListPlayerAttributeValuesCallback {
        void run(byte b, byte[] bArr);
    }

    interface GetPlayerAttributeTextCallback {
        void run(int i, byte[] bArr, int i2, String[] strArr);
    }

    interface GetPlayerAttributeValueCallback {
        void run(byte b, byte[] bArr);
    }

    interface GetPlayerAttributeValueTextCallback {
        void run(int i, byte[] bArr, int i2, String[] strArr);
    }

    interface PlayerSettingsUpdateCallback {
        void run(boolean z);
    }

    interface SetPlayerAppSettingCallback {
        void run(int i);
    }

    private class PlayerSettings {
        public byte attr;
        public byte[] attrIds;
        public String path;

        private PlayerSettings() {
        }
    }

    private class localPlayerSettings {
        public byte eq_value;
        public byte repeat_value;
        public byte scan_value;
        public byte shuffle_value;

        private localPlayerSettings() {
            this.eq_value = (byte) 1;
            this.repeat_value = (byte) 1;
            this.shuffle_value = (byte) 1;
            this.scan_value = (byte) 1;
        }
    }

    MediaPlayerSettings(Looper looper, Context context) {
        this.mPlayerSettings = new PlayerSettings();
        this.settingValues = new localPlayerSettings();
        Log.v(TAG, "Create MediaPlayerSettings");
        this.mLooper = looper;
        this.mContext = context;
        this.mPendingCmds = new ArrayList<>();
        this.mPendingSetAttributes = new ArrayList<>();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAYERSETTINGS_RESPONSE);
        try {
            context.registerReceiver(this.mPlayerSettingsReceiver, intentFilter);
        } catch (Exception e) {
            Log.e(TAG, "Unable to register Avrcp player app settings receiver", e);
        }
        this.mHandler = new PlayerSettingsMessageHandler(looper);
    }

    void init(AvrcpTargetService.ListCallback listCallback) {
        Log.v(TAG, "Initializing MediaPlayerSettings");
        this.mPlayerSettingsCb = listCallback;
    }

    private final class PlayerSettingsMessageHandler extends Handler {
        private static final int MESSAGE_PLAYERSETTINGS_TIMEOUT = 602;

        PlayerSettingsMessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == MESSAGE_PLAYERSETTINGS_TIMEOUT) {
                synchronized (MediaPlayerSettings.this.mPendingCmds) {
                    Integer num = new Integer(message.arg1);
                    if (MediaPlayerSettings.this.mPendingCmds.contains(num)) {
                        MediaPlayerSettings.this.mPendingCmds.remove(num);
                        MediaPlayerSettings.d("handle Message MESSAGE_PLAYERSETTINGS_TIMEOUT");
                        int i = message.arg1;
                        if (i != 6) {
                            switch (i) {
                                case 0:
                                    MediaPlayerSettings.d("GET_ATTRIBUTE_IDS timeout");
                                    MediaPlayerSettings.this.getListPlayerAttributeRsp((byte) MediaPlayerSettings.this.def_attrib.length, MediaPlayerSettings.this.def_attrib);
                                    return;
                                case 1:
                                    MediaPlayerSettings.d("GET_VALUE_IDS timeout");
                                    switch (MediaPlayerSettings.this.mPlayerSettings.attr) {
                                        case 2:
                                            MediaPlayerSettings.this.getListPlayerAttributeValuesRsp((byte) MediaPlayerSettings.this.value_repmode.length, MediaPlayerSettings.this.value_repmode);
                                            return;
                                        case 3:
                                            MediaPlayerSettings.this.getListPlayerAttributeValuesRsp((byte) MediaPlayerSettings.this.value_shufmode.length, MediaPlayerSettings.this.value_shufmode);
                                            return;
                                        default:
                                            MediaPlayerSettings.this.getListPlayerAttributeValuesRsp((byte) MediaPlayerSettings.this.value_default.length, MediaPlayerSettings.this.value_default);
                                            return;
                                    }
                                case 2:
                                    MediaPlayerSettings.d("GET_ATTRIBUTE_TEXT timeout");
                                    String[] strArr = new String[MediaPlayerSettings.this.mPlayerSettings.attrIds.length];
                                    for (int i2 = 0; i2 < MediaPlayerSettings.this.mPlayerSettings.attrIds.length; i2++) {
                                        switch (MediaPlayerSettings.this.mPlayerSettings.attrIds[i2]) {
                                            case 2:
                                                strArr[i2] = "Repeat";
                                                break;
                                            case 3:
                                                strArr[i2] = "Shuffle";
                                                break;
                                        }
                                    }
                                    MediaPlayerSettings.this.getPlayerAttributeTextRsp(MediaPlayerSettings.this.mPlayerSettings.attrIds.length, MediaPlayerSettings.this.mPlayerSettings.attrIds, strArr.length, strArr);
                                    return;
                                case 3:
                                    MediaPlayerSettings.d("GET_VALUE_TEXT timeout");
                                    switch (message.arg2) {
                                        case 2:
                                            MediaPlayerSettings.this.getPlayerAttributeTextValueRsp(MediaPlayerSettings.valTextRepeateIds.length, MediaPlayerSettings.valTextRepeateIds, MediaPlayerSettings.valTextRepeate.length, MediaPlayerSettings.valTextRepeate);
                                            return;
                                        case 3:
                                            MediaPlayerSettings.this.getPlayerAttributeTextValueRsp(MediaPlayerSettings.valTextShuffleIds.length, MediaPlayerSettings.valTextShuffleIds, MediaPlayerSettings.valTextShuffle.length, MediaPlayerSettings.valTextShuffle);
                                            return;
                                        default:
                                            return;
                                    }
                                case 4:
                                    MediaPlayerSettings.d("GET_ATTRIBUTE_VALUES timeout");
                                    byte[] bArr = new byte[MediaPlayerSettings.this.mPlayerSettings.attrIds.length * 2];
                                    int i3 = 0;
                                    for (int i4 = 0; i4 < MediaPlayerSettings.this.mPlayerSettings.attrIds.length; i4++) {
                                        int i5 = i3 + 1;
                                        bArr[i3] = MediaPlayerSettings.this.mPlayerSettings.attrIds[i4];
                                        if (MediaPlayerSettings.this.mPlayerSettings.attrIds[i4] != 2) {
                                            if (MediaPlayerSettings.this.mPlayerSettings.attrIds[i4] == 3) {
                                                i3 = i5 + 1;
                                                bArr[i5] = MediaPlayerSettings.this.settingValues.shuffle_value;
                                            } else {
                                                i3 = i5 + 1;
                                                bArr[i5] = 0;
                                            }
                                        } else {
                                            i3 = i5 + 1;
                                            bArr[i5] = MediaPlayerSettings.this.settingValues.repeat_value;
                                        }
                                    }
                                    MediaPlayerSettings.this.getPlayerAttributeValueRsp((byte) bArr.length, bArr);
                                    return;
                                default:
                                    return;
                            }
                        }
                        MediaPlayerSettings.d("SET_ATTRIBUTE_VALUES timeout");
                        MediaPlayerSettings.this.setPlayerAppSettingRsp(4);
                        return;
                    }
                    return;
                }
            }
            Log.wtf(MediaPlayerSettings.TAG, "Unknown message on timeout handler: " + message.what);
        }
    }

    void cleanup() {
        d("cleanup");
        try {
            this.mContext.unregisterReceiver(this.mPlayerSettingsReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Unable to unregister Avrcp receiver", e);
        }
        this.mCallbacks.clear();
    }

    private void sendPlayerAppSettingsUpdate(boolean z) {
        if (this.mPlayerSettingsCb == null || !isAppSettingOn) {
            return;
        }
        this.mPlayerSettingsCb.run(true);
        d("sendPlayerAppSettingsUpdate");
    }

    private void updateLocalPlayerSettings(byte[] bArr) {
        d("updateLocalPlayerSettings");
        for (int i = 0; i < bArr.length; i += 2) {
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ");
            sb.append((int) bArr[i]);
            sb.append(" Value: ");
            int i2 = i + 1;
            sb.append((int) bArr[i2]);
            d(sb.toString());
            switch (bArr[i]) {
                case 1:
                    this.settingValues.eq_value = bArr[i2];
                    break;
                case 2:
                    this.settingValues.repeat_value = bArr[i2];
                    break;
                case 3:
                    this.settingValues.shuffle_value = bArr[i2];
                    break;
                case 4:
                    this.settingValues.scan_value = bArr[i2];
                    break;
            }
        }
    }

    private boolean checkPlayerAttributeResponse(byte[] bArr) {
        d("checkPlayerAttributeResponse");
        boolean z = false;
        for (int i = 0; i < bArr.length; i += 2) {
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ");
            sb.append((int) bArr[i]);
            sb.append(" Value: ");
            int i2 = i + 1;
            sb.append((int) bArr[i2]);
            d(sb.toString());
            switch (bArr[i]) {
                case 1:
                    if (this.mPendingSetAttributes.contains(new Integer(1))) {
                        if (bArr[i2] == -1) {
                            z = false;
                        } else {
                            z = true;
                        }
                    }
                    break;
                case 2:
                    if (!this.mPendingSetAttributes.contains(new Integer(2))) {
                        break;
                    } else if (bArr[i2] == -1) {
                    }
                    break;
                case 3:
                    if (!this.mPendingSetAttributes.contains(new Integer(3))) {
                        break;
                    } else if (bArr[i2] == -1) {
                    }
                    break;
            }
        }
        this.mPendingSetAttributes.clear();
        return z;
    }

    public void getListPlayerAttribute(GetListPlayerAttributeCallback getListPlayerAttributeCallback) {
        d("getListPlayerAttribute");
        isAppSettingOn = true;
        d("isAppSettingOn = " + isAppSettingOn);
        Intent intent = new Intent(PLAYERSETTINGS_REQUEST);
        intent.putExtra(COMMAND, CMDGET);
        intent.putExtra(EXTRA_GET_COMMAND, 0);
        intent.setPackage("com.android.bbkmusic");
        this.mContext.sendBroadcast(intent, "android.permission.BLUETOOTH");
        Message messageObtainMessage = this.mHandler.obtainMessage(MESSAGE_PLAYERSETTINGS_TIMEOUT, 0, 0);
        this.mPendingCmds.add(new Integer(messageObtainMessage.arg1));
        this.mCallbacks.put(0, getListPlayerAttributeCallback);
        this.mHandler.sendMessageDelayed(messageObtainMessage, 130L);
    }

    private void getListPlayerAttributeRsp(byte b, byte[] bArr) {
        GetListPlayerAttributeCallback getListPlayerAttributeCallback = (GetListPlayerAttributeCallback) this.mCallbacks.get(0);
        if (getListPlayerAttributeCallback != null) {
            getListPlayerAttributeCallback.run(b, bArr);
            d("getListPlayerAttributeRsp");
        }
    }

    public void getListPlayerAttributeValues(byte b, GetListPlayerAttributeValuesCallback getListPlayerAttributeValuesCallback) {
        d("getListPlayerAttributeValues");
        Intent intent = new Intent(PLAYERSETTINGS_REQUEST);
        intent.putExtra(COMMAND, CMDGET);
        intent.putExtra(EXTRA_GET_COMMAND, 1);
        intent.putExtra(EXTRA_ATTRIBUTE_ID, b);
        intent.setPackage("com.android.bbkmusic");
        this.mContext.sendBroadcast(intent, "android.permission.BLUETOOTH");
        this.mPlayerSettings.attr = b;
        Message messageObtainMessage = this.mHandler.obtainMessage();
        messageObtainMessage.what = MESSAGE_PLAYERSETTINGS_TIMEOUT;
        messageObtainMessage.arg1 = 1;
        this.mPendingCmds.add(new Integer(messageObtainMessage.arg1));
        this.mCallbacks.put(1, getListPlayerAttributeValuesCallback);
        this.mHandler.sendMessageDelayed(messageObtainMessage, 130L);
    }

    private void getListPlayerAttributeValuesRsp(byte b, byte[] bArr) {
        GetListPlayerAttributeValuesCallback getListPlayerAttributeValuesCallback = (GetListPlayerAttributeValuesCallback) this.mCallbacks.get(1);
        if (getListPlayerAttributeValuesCallback != null) {
            getListPlayerAttributeValuesCallback.run(b, bArr);
            d("getListPlayerAttributeValuesRsp");
        }
    }

    public void getPlayerAttributeValue(byte b, int[] iArr, GetPlayerAttributeValueCallback getPlayerAttributeValueCallback) {
        d("getPlayerAttributeValue attr " + ((int) b));
        byte[] bArr = new byte[b];
        for (int i = 0; i < b; i++) {
            bArr[i] = (byte) iArr[i];
        }
        this.mPlayerSettings.attrIds = new byte[b];
        for (int i2 = 0; i2 < b; i2++) {
            this.mPlayerSettings.attrIds[i2] = bArr[i2];
        }
        Intent intent = new Intent(PLAYERSETTINGS_REQUEST);
        intent.putExtra(COMMAND, CMDGET);
        intent.putExtra(EXTRA_GET_COMMAND, 4);
        intent.putExtra(EXTRA_ATTIBUTE_ID_ARRAY, bArr);
        intent.setPackage("com.android.bbkmusic");
        this.mContext.sendBroadcast(intent, "android.permission.BLUETOOTH");
        Message messageObtainMessage = this.mHandler.obtainMessage();
        messageObtainMessage.what = MESSAGE_PLAYERSETTINGS_TIMEOUT;
        messageObtainMessage.arg1 = 4;
        this.mPendingCmds.add(new Integer(messageObtainMessage.arg1));
        this.mCallbacks.put(4, getPlayerAttributeValueCallback);
        this.mHandler.sendMessageDelayed(messageObtainMessage, 130L);
    }

    private void getPlayerAttributeValueRsp(byte b, byte[] bArr) {
        GetPlayerAttributeValueCallback getPlayerAttributeValueCallback = (GetPlayerAttributeValueCallback) this.mCallbacks.get(4);
        if (getPlayerAttributeValueCallback != null) {
            getPlayerAttributeValueCallback.run(b, bArr);
            d("getPlayerAttributeValueRsp");
        }
    }

    public void setPlayerAppSetting(byte b, byte[] bArr, byte[] bArr2, SetPlayerAppSettingCallback setPlayerAppSettingCallback) {
        d("setPlayerAppSetting num " + ((int) b));
        byte[] bArr3 = new byte[b * 2];
        int i = 0;
        while (i < b) {
            bArr3[i] = bArr[i];
            int i2 = i + 1;
            bArr3[i2] = bArr2[i];
            this.mPendingSetAttributes.add(new Integer(bArr[i]));
            i = i2;
        }
        Intent intent = new Intent(PLAYERSETTINGS_REQUEST);
        intent.putExtra(COMMAND, CMDSET);
        intent.putExtra(EXTRA_ATTRIB_VALUE_PAIRS, bArr3);
        intent.setPackage("com.android.bbkmusic");
        updateLocalPlayerSettings(bArr3);
        this.mContext.sendBroadcast(intent, "android.permission.BLUETOOTH");
        Message messageObtainMessage = this.mHandler.obtainMessage();
        messageObtainMessage.what = MESSAGE_PLAYERSETTINGS_TIMEOUT;
        messageObtainMessage.arg1 = 6;
        this.mPendingCmds.add(new Integer(messageObtainMessage.arg1));
        this.mCallbacks.put(6, setPlayerAppSettingCallback);
        this.mHandler.sendMessageDelayed(messageObtainMessage, 500L);
    }

    private void setPlayerAppSettingRsp(int i) {
        SetPlayerAppSettingCallback setPlayerAppSettingCallback = (SetPlayerAppSettingCallback) this.mCallbacks.get(6);
        if (setPlayerAppSettingCallback != null) {
            setPlayerAppSettingCallback.run(i);
            d("setPlayerAppSettingRsp");
        }
    }

    public void getPlayerAttributeText(byte b, byte[] bArr, GetPlayerAttributeTextCallback getPlayerAttributeTextCallback) {
        d("getplayerattribute_text" + ((int) b) + "attrIDsNum" + bArr.length);
        Intent intent = new Intent(PLAYERSETTINGS_REQUEST);
        Message messageObtainMessage = this.mHandler.obtainMessage();
        intent.putExtra(COMMAND, CMDGET);
        intent.putExtra(EXTRA_GET_COMMAND, 2);
        intent.putExtra(EXTRA_ATTIBUTE_ID_ARRAY, bArr);
        intent.setPackage("com.android.bbkmusic");
        this.mPlayerSettings.attrIds = new byte[b];
        for (int i = 0; i < b; i++) {
            this.mPlayerSettings.attrIds[i] = bArr[i];
        }
        this.mContext.sendBroadcast(intent, "android.permission.BLUETOOTH");
        messageObtainMessage.what = MESSAGE_PLAYERSETTINGS_TIMEOUT;
        messageObtainMessage.arg1 = 2;
        this.mPendingCmds.add(new Integer(messageObtainMessage.arg1));
        this.mCallbacks.put(2, getPlayerAttributeTextCallback);
        this.mHandler.sendMessageDelayed(messageObtainMessage, 130L);
    }

    private void getPlayerAttributeTextRsp(int i, byte[] bArr, int i2, String[] strArr) {
        GetPlayerAttributeTextCallback getPlayerAttributeTextCallback = (GetPlayerAttributeTextCallback) this.mCallbacks.get(2);
        if (getPlayerAttributeTextCallback != null) {
            getPlayerAttributeTextCallback.run(i, bArr, i2, strArr);
            d("getPlayerAttributeTextRsp");
        }
    }

    public void getPlayerAttributeTextValue(byte b, byte b2, byte[] bArr, GetPlayerAttributeValueTextCallback getPlayerAttributeValueTextCallback) {
        d("getplayervalue_text id" + ((int) b) + "numValue" + ((int) b2) + "value.lenght" + bArr.length);
        Intent intent = new Intent(PLAYERSETTINGS_REQUEST);
        Message messageObtainMessage = this.mHandler.obtainMessage();
        intent.putExtra(COMMAND, CMDGET);
        intent.putExtra(EXTRA_GET_COMMAND, 3);
        intent.putExtra(EXTRA_ATTRIBUTE_ID, b);
        intent.putExtra(EXTRA_VALUE_ID_ARRAY, bArr);
        intent.setPackage("com.android.bbkmusic");
        this.mPlayerSettings.attrIds = new byte[b2];
        for (int i = 0; i < b2; i++) {
            this.mPlayerSettings.attrIds[i] = bArr[i];
        }
        this.mContext.sendBroadcast(intent, "android.permission.BLUETOOTH");
        messageObtainMessage.what = MESSAGE_PLAYERSETTINGS_TIMEOUT;
        messageObtainMessage.arg1 = 3;
        this.mPendingCmds.add(new Integer(messageObtainMessage.arg1));
        this.mCallbacks.put(3, getPlayerAttributeValueTextCallback);
        this.mHandler.sendMessageDelayed(messageObtainMessage, 130L);
    }

    private void getPlayerAttributeTextValueRsp(int i, byte[] bArr, int i2, String[] strArr) {
        GetPlayerAttributeValueTextCallback getPlayerAttributeValueTextCallback = (GetPlayerAttributeValueTextCallback) this.mCallbacks.get(3);
        if (getPlayerAttributeValueTextCallback != null) {
            getPlayerAttributeValueTextCallback.run(i, bArr, i2, strArr);
            d("getPlayerAttributeTextValueRsp");
        }
    }

    public void getAppSettingChange(GetAppSettingChangeCallback getAppSettingChangeCallback) {
        d("getAppSettingChange");
        getAppSettingChangeCallback.run((byte) 4, new byte[]{2, this.settingValues.repeat_value, 3, this.settingValues.shuffle_value});
    }

    private static void d(String str) {
        if (DEBUG) {
            Log.d(TAG, str);
        }
    }
}
