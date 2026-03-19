package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemProperties;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.bluetooth.map.BluetoothMapContentObserver;
import com.android.bluetooth.util.DevicePolicyUtils;
import com.android.internal.telephony.GsmAlphabet;
import java.util.HashMap;

public class AtPhonebook {
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final String INCOMING_CALL_WHERE = "type=1";
    private static final int MAX_PHONEBOOK_SIZE = 16384;
    private static final String MISSED_CALL_WHERE = "type=3";
    private static final String OUTGOING_CALL_WHERE = "type=2";
    private static final String TAG = "BluetoothAtPhonebook";
    static final int TYPE_READ = 0;
    static final int TYPE_SET = 1;
    static final int TYPE_TEST = 2;
    static final int TYPE_UNKNOWN = -1;
    private boolean mCheckingAccessPermission;
    private ContentResolver mContentResolver;
    private Context mContext;
    private int mCpbrIndex1;
    private int mCpbrIndex2;
    private String mCurrentPhonebook;
    private HeadsetNativeInterface mNativeInterface;
    private final String mPairingPackage;
    private static final boolean DBG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    private static final String[] CALLS_PROJECTION = {"_id", "number", "presentation"};
    private static final String[] PHONES_PROJECTION = {"_id", "display_name", "data1", "data2"};
    private String mCharacterSet = "UTF-8";
    private final HashMap<String, PhonebookResult> mPhonebooks = new HashMap<>(4);

    private class PhonebookResult {
        public Cursor cursor;
        public int nameColumn;
        public int numberColumn;
        public int numberPresentationColumn;
        public int typeColumn;

        private PhonebookResult() {
        }
    }

    public AtPhonebook(Context context, HeadsetNativeInterface headsetNativeInterface) {
        this.mContext = context;
        this.mPairingPackage = context.getString(R.string.pairing_ui_package);
        this.mContentResolver = context.getContentResolver();
        this.mNativeInterface = headsetNativeInterface;
        this.mPhonebooks.put("DC", new PhonebookResult());
        this.mPhonebooks.put("RC", new PhonebookResult());
        this.mPhonebooks.put("MC", new PhonebookResult());
        this.mPhonebooks.put("ME", new PhonebookResult());
        this.mCurrentPhonebook = "ME";
        this.mCpbrIndex2 = -1;
        this.mCpbrIndex1 = -1;
    }

    public void cleanup() {
        this.mPhonebooks.clear();
    }

    public String getLastDialledNumber() {
        Cursor cursorQuery = this.mContentResolver.query(CallLog.Calls.CONTENT_URI, new String[]{"number"}, OUTGOING_CALL_WHERE, null, "date DESC LIMIT 1");
        if (cursorQuery == null) {
            Log.w(TAG, "getLastDialledNumber, cursor is null");
            return null;
        }
        if (cursorQuery.getCount() < 1) {
            cursorQuery.close();
            Log.w(TAG, "getLastDialledNumber, cursor.getCount is 0");
            return null;
        }
        cursorQuery.moveToNext();
        String string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("number"));
        cursorQuery.close();
        return string;
    }

    public boolean getCheckingAccessPermission() {
        return this.mCheckingAccessPermission;
    }

    public void setCheckingAccessPermission(boolean z) {
        this.mCheckingAccessPermission = z;
    }

    public void setCpbrIndex(int i) {
        this.mCpbrIndex2 = i;
        this.mCpbrIndex1 = i;
    }

    private byte[] getByteAddress(BluetoothDevice bluetoothDevice) {
        return Utils.getBytesFromAddress(bluetoothDevice.getAddress());
    }

    public void handleCscsCommand(String str, int i, BluetoothDevice bluetoothDevice) {
        log("handleCscsCommand - atString = " + str);
        int i2 = 1;
        int i3 = -1;
        String str2 = null;
        switch (i) {
            case 0:
                log("handleCscsCommand - Read Command");
                str2 = "+CSCS: \"" + this.mCharacterSet + "\"";
                break;
            case 1:
                log("handleCscsCommand - Set Command");
                String[] strArrSplit = str.split("=");
                if (strArrSplit.length < 2 || strArrSplit[1] == null) {
                    this.mNativeInterface.atResponseCode(bluetoothDevice, 0, -1);
                } else {
                    String strReplace = str.split("=")[1].replace("\"", "");
                    if (strReplace.equals("GSM") || strReplace.equals("IRA") || strReplace.equals("UTF-8") || strReplace.equals("UTF8")) {
                        this.mCharacterSet = strReplace;
                    } else {
                        i3 = 4;
                    }
                }
                i2 = 0;
                break;
            case 2:
                log("handleCscsCommand - Test Command");
                str2 = "+CSCS: (\"UTF-8\",\"IRA\",\"GSM\")";
                break;
            default:
                log("handleCscsCommand - Invalid chars");
                i3 = 25;
                i2 = 0;
                break;
        }
        if (str2 != null) {
            this.mNativeInterface.atResponseString(bluetoothDevice, str2);
        }
        this.mNativeInterface.atResponseCode(bluetoothDevice, i2, i3);
    }

    public void handleCpbsCommand(String str, int i, BluetoothDevice bluetoothDevice) {
        log("handleCpbsCommand - atString = " + str);
        int i2 = 4;
        String str2 = null;
        int i3 = 0;
        switch (i) {
            case 0:
                log("handleCpbsCommand - read command");
                if ("SM".equals(this.mCurrentPhonebook)) {
                    str2 = "+CPBS: \"SM\",0," + getMaxPhoneBookSize(0);
                } else {
                    PhonebookResult phonebookResult = getPhonebookResult(this.mCurrentPhonebook, true);
                    if (phonebookResult != null) {
                        int count = phonebookResult.cursor.getCount();
                        String str3 = "+CPBS: \"" + this.mCurrentPhonebook + "\"," + count + "," + getMaxPhoneBookSize(count);
                        phonebookResult.cursor.close();
                        phonebookResult.cursor = null;
                        str2 = str3;
                    }
                }
                i3 = 1;
                i2 = -1;
                break;
            case 1:
                log("handleCpbsCommand - set command");
                String[] strArrSplit = str.split("=");
                if (strArrSplit.length >= 2 && strArrSplit[1] != null) {
                    String strTrim = strArrSplit[1].trim();
                    while (strTrim.endsWith("\"")) {
                        strTrim = strTrim.substring(0, strTrim.length() - 1);
                    }
                    while (strTrim.startsWith("\"")) {
                        strTrim = strTrim.substring(1, strTrim.length());
                    }
                    if (getPhonebookResult(strTrim, false) == null && !"SM".equals(strTrim)) {
                        if (DBG) {
                            log("Dont know phonebook: '" + strTrim + "'");
                        }
                        i2 = 3;
                    } else {
                        this.mCurrentPhonebook = strTrim;
                        i3 = 1;
                        i2 = -1;
                    }
                }
                break;
            case 2:
                log("handleCpbsCommand - test command");
                str2 = "+CPBS: (\"ME\",\"SM\",\"DC\",\"RC\",\"MC\")";
                i3 = 1;
                i2 = -1;
                break;
            default:
                log("handleCpbsCommand - invalid chars");
                i2 = 25;
                break;
        }
        if (str2 != null) {
            this.mNativeInterface.atResponseString(bluetoothDevice, str2);
        }
        this.mNativeInterface.atResponseCode(bluetoothDevice, i3, i2);
    }

    void handleCpbrCommand(String str, int i, BluetoothDevice bluetoothDevice) {
        int i2;
        log("handleCpbrCommand - atString = " + str);
        int count = 0;
        switch (i) {
            case 0:
            case 1:
                log("handleCpbrCommand - set/read command");
                if (this.mCpbrIndex1 != -1) {
                    this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 3);
                } else if (str.split("=").length < 2) {
                    this.mNativeInterface.atResponseCode(bluetoothDevice, 0, -1);
                } else {
                    String[] strArrSplit = str.split("=")[1].split(",");
                    for (int i3 = 0; i3 < strArrSplit.length; i3++) {
                        strArrSplit[i3] = strArrSplit[i3].replace(';', ' ').trim();
                    }
                    try {
                        int i4 = Integer.parseInt(strArrSplit[0]);
                        if (strArrSplit.length != 1) {
                            i2 = Integer.parseInt(strArrSplit[1]);
                        } else {
                            i2 = i4;
                        }
                        this.mCpbrIndex1 = i4;
                        this.mCpbrIndex2 = i2;
                        this.mCheckingAccessPermission = true;
                        int iCheckAccessPermission = checkAccessPermission(bluetoothDevice);
                        if (iCheckAccessPermission == 1) {
                            this.mCheckingAccessPermission = false;
                            int iProcessCpbrCommand = processCpbrCommand(bluetoothDevice);
                            this.mCpbrIndex2 = -1;
                            this.mCpbrIndex1 = -1;
                            this.mNativeInterface.atResponseCode(bluetoothDevice, iProcessCpbrCommand, -1);
                        } else if (iCheckAccessPermission == 2) {
                            this.mCheckingAccessPermission = false;
                            this.mCpbrIndex2 = -1;
                            this.mCpbrIndex1 = -1;
                            this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 0);
                        }
                    } catch (Exception e) {
                        log("handleCpbrCommand - exception - invalid chars: " + e.toString());
                        this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 25);
                    }
                }
                break;
            case 2:
                log("handleCpbrCommand - test command");
                if (!"SM".equals(this.mCurrentPhonebook)) {
                    PhonebookResult phonebookResult = getPhonebookResult(this.mCurrentPhonebook, true);
                    if (phonebookResult == null) {
                        this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 3);
                    } else {
                        count = phonebookResult.cursor.getCount();
                        log("handleCpbrCommand - size = " + count);
                        phonebookResult.cursor.close();
                        phonebookResult.cursor = null;
                    }
                }
                if (count == 0) {
                    count = 1;
                }
                this.mNativeInterface.atResponseString(bluetoothDevice, "+CPBR: (1-" + count + "),30,30");
                this.mNativeInterface.atResponseCode(bluetoothDevice, 1, -1);
                break;
            default:
                log("handleCpbrCommand - invalid chars");
                this.mNativeInterface.atResponseCode(bluetoothDevice, 0, 25);
                break;
        }
    }

    private synchronized PhonebookResult getPhonebookResult(String str, boolean z) {
        if (str == null) {
            return null;
        }
        PhonebookResult phonebookResult = this.mPhonebooks.get(str);
        if (phonebookResult == null) {
            phonebookResult = new PhonebookResult();
        }
        if (z || phonebookResult.cursor == null) {
            if (!queryPhonebook(str, phonebookResult)) {
                return null;
            }
        }
        return phonebookResult;
    }

    private synchronized boolean queryPhonebook(String str, PhonebookResult phonebookResult) {
        String str2;
        String str3;
        boolean z;
        if (!str.equals("ME")) {
            if (str.equals("DC")) {
                str2 = OUTGOING_CALL_WHERE;
            } else if (str.equals("RC")) {
                str2 = INCOMING_CALL_WHERE;
            } else {
                if (!str.equals("MC")) {
                    return false;
                }
                str2 = MISSED_CALL_WHERE;
            }
            str3 = str2;
            z = true;
        } else {
            str3 = null;
            z = false;
        }
        if (phonebookResult.cursor != null) {
            phonebookResult.cursor.close();
            phonebookResult.cursor = null;
        }
        if (z) {
            phonebookResult.cursor = this.mContentResolver.query(CallLog.Calls.CONTENT_URI, CALLS_PROJECTION, str3, null, "date DESC LIMIT 16384");
            if (phonebookResult.cursor == null) {
                return false;
            }
            phonebookResult.numberColumn = phonebookResult.cursor.getColumnIndexOrThrow("number");
            phonebookResult.numberPresentationColumn = phonebookResult.cursor.getColumnIndexOrThrow("presentation");
            phonebookResult.typeColumn = -1;
            phonebookResult.nameColumn = -1;
        } else {
            phonebookResult.cursor = this.mContentResolver.query(DevicePolicyUtils.getEnterprisePhoneUri(this.mContext), PHONES_PROJECTION, str3, null, "data1 LIMIT 16384");
            if (phonebookResult.cursor == null) {
                return false;
            }
            phonebookResult.numberColumn = phonebookResult.cursor.getColumnIndex("data1");
            phonebookResult.numberPresentationColumn = -1;
            phonebookResult.typeColumn = phonebookResult.cursor.getColumnIndex("data2");
            phonebookResult.nameColumn = phonebookResult.cursor.getColumnIndex("display_name");
        }
        Log.i(TAG, "Refreshed phonebook " + str + " with " + phonebookResult.cursor.getCount() + " results");
        return true;
    }

    synchronized void resetAtState() {
        this.mCharacterSet = "UTF-8";
        this.mCpbrIndex2 = -1;
        this.mCpbrIndex1 = -1;
        this.mCheckingAccessPermission = false;
    }

    private synchronized int getMaxPhoneBookSize(int i) {
        if (i < 100) {
            i = 100;
        }
        return roundUpToPowerOfTwo(i + (i / 2));
    }

    private int roundUpToPowerOfTwo(int i) {
        int i2 = i | (i >> 1);
        int i3 = i2 | (i2 >> 2);
        int i4 = i3 | (i3 >> 4);
        int i5 = i4 | (i4 >> 8);
        return (i5 | (i5 >> 16)) + 1;
    }

    int processCpbrCommand(BluetoothDevice bluetoothDevice) {
        String string;
        int i;
        String string2;
        log("processCpbrCommand");
        new StringBuilder();
        if ("SM".equals(this.mCurrentPhonebook)) {
            return 1;
        }
        PhonebookResult phonebookResult = getPhonebookResult(this.mCurrentPhonebook, true);
        if (phonebookResult == null) {
            Log.e(TAG, "pbr is null");
            return 0;
        }
        if (phonebookResult.cursor.getCount() == 0 || this.mCpbrIndex1 <= 0 || this.mCpbrIndex2 < this.mCpbrIndex1 || this.mCpbrIndex1 > phonebookResult.cursor.getCount()) {
            Log.e(TAG, "Invalid request or no results, returning");
            return 1;
        }
        if (this.mCpbrIndex2 > phonebookResult.cursor.getCount()) {
            Log.w(TAG, "max index requested is greater than number of records available, resetting it");
            this.mCpbrIndex2 = phonebookResult.cursor.getCount();
        }
        phonebookResult.cursor.moveToPosition(this.mCpbrIndex1 - 1);
        log("mCpbrIndex1 = " + this.mCpbrIndex1 + " and mCpbrIndex2 = " + this.mCpbrIndex2);
        for (int i2 = this.mCpbrIndex1; i2 <= this.mCpbrIndex2; i2++) {
            String string3 = phonebookResult.cursor.getString(phonebookResult.numberColumn);
            if (phonebookResult.nameColumn == -1 && string3 != null && string3.length() > 0) {
                Cursor cursorQuery = this.mContentResolver.query(Uri.withAppendedPath(ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI, string3), new String[]{"display_name", BluetoothMapContentObserver.EXTRA_MESSAGE_SENT_MSG_TYPE}, null, null, null);
                if (cursorQuery != null) {
                    if (cursorQuery.moveToFirst()) {
                        string2 = cursorQuery.getString(0);
                        cursorQuery.getInt(1);
                    } else {
                        string2 = null;
                    }
                    cursorQuery.close();
                } else {
                    string2 = null;
                }
                if (DBG && string2 == null) {
                    log("Caller ID lookup failed for " + string3);
                }
                string = string2;
            } else if (phonebookResult.nameColumn != -1) {
                string = phonebookResult.cursor.getString(phonebookResult.nameColumn);
            } else {
                log("processCpbrCommand: empty name and number");
                string = null;
            }
            if (string == null) {
                string = "";
            }
            String strTrim = string.trim();
            if (strTrim.length() > 28) {
                strTrim = strTrim.substring(0, 28);
            }
            if (phonebookResult.typeColumn != -1) {
                strTrim = strTrim + "/" + getPhoneType(phonebookResult.cursor.getInt(phonebookResult.typeColumn));
            }
            if (string3 == null) {
                string3 = "";
            }
            int i3 = PhoneNumberUtils.toaFromString(string3);
            String strStripSeparators = PhoneNumberUtils.stripSeparators(string3.trim());
            if (strStripSeparators.length() > 30) {
                strStripSeparators = strStripSeparators.substring(0, 30);
            }
            if (phonebookResult.numberPresentationColumn != -1) {
                i = phonebookResult.cursor.getInt(phonebookResult.numberPresentationColumn);
            } else {
                i = 1;
            }
            if (i != 1) {
                strStripSeparators = "";
                strTrim = this.mContext.getString(R.string.unknownNumber);
            }
            if (!strTrim.isEmpty() && this.mCharacterSet.equals("GSM")) {
                byte[] bArrStringToGsm8BitPacked = GsmAlphabet.stringToGsm8BitPacked(strTrim);
                if (bArrStringToGsm8BitPacked == null) {
                    strTrim = this.mContext.getString(R.string.unknownNumber);
                } else {
                    strTrim = new String(bArrStringToGsm8BitPacked);
                }
            }
            this.mNativeInterface.atResponseString(bluetoothDevice, ("+CPBR: " + i2 + ",\"" + strStripSeparators + "\"," + i3 + ",\"" + strTrim + "\"") + "\r\n\r\n");
            if (!phonebookResult.cursor.moveToNext()) {
                break;
            }
        }
        if (phonebookResult.cursor != null) {
            phonebookResult.cursor.close();
            phonebookResult.cursor = null;
        }
        return 1;
    }

    private int checkAccessPermission(BluetoothDevice bluetoothDevice) {
        log("checkAccessPermission");
        int phonebookAccessPermission = bluetoothDevice.getPhonebookAccessPermission();
        if (phonebookAccessPermission == 0) {
            log("checkAccessPermission - ACTION_CONNECTION_ACCESS_REQUEST");
            Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST");
            intent.setPackage(this.mPairingPackage);
            intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
            intent.putExtra("android.bluetooth.device.extra.DEVICE", bluetoothDevice);
            this.mContext.sendOrderedBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
        }
        return phonebookAccessPermission;
    }

    private static String getPhoneType(int i) {
        switch (i) {
            case 1:
                return "H";
            case 2:
                return "M";
            case 3:
                return "W";
            case 4:
            case 5:
                return "F";
            default:
                return "O";
        }
    }

    private static void log(String str) {
        Log.d(TAG, str);
    }
}
