package com.android.bluetooth.hfp;

import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import com.android.bluetooth.R;
import com.android.bluetooth.Utils;
import com.android.internal.telephony.GsmAlphabet;
import java.util.HashMap;

public class AtPhonebook {
    private static final String ACCESS_AUTHORITY_CLASS = "com.android.settings.bluetooth.BluetoothPermissionRequest";
    private static final String ACCESS_AUTHORITY_PACKAGE = "com.android.settings";
    private static final String BLUETOOTH_ADMIN_PERM = "android.permission.BLUETOOTH_ADMIN";
    private static final boolean DBG = false;
    private static final String INCOMING_CALL_WHERE = "type=1";
    private static final int MAX_PHONEBOOK_SIZE = 16384;
    private static final String MISSED_CALL_WHERE = "type=3";
    private static final String OUTGOING_CALL_WHERE = "type=2";
    private static final String TAG = "BluetoothAtPhonebook";
    private static final String VISIBLE_PHONEBOOK_WHERE = "in_visible_group=1";
    private boolean mCheckingAccessPermission;
    private ContentResolver mContentResolver;
    private Context mContext;
    private int mCpbrIndex1;
    private int mCpbrIndex2;
    private String mCurrentPhonebook;
    private HeadsetStateMachine mStateMachine;
    private static final String[] CALLS_PROJECTION = {"_id", "number", "presentation"};
    private static final String[] PHONES_PROJECTION = {"_id", "display_name", "data1", "data2"};
    private String mCharacterSet = "UTF-8";
    private final HashMap<String, PhonebookResult> mPhonebooks = new HashMap<>(4);
    final int TYPE_UNKNOWN = -1;
    final int TYPE_READ = 0;
    final int TYPE_SET = 1;
    final int TYPE_TEST = 2;

    private class PhonebookResult {
        public Cursor cursor;
        public int nameColumn;
        public int numberColumn;
        public int numberPresentationColumn;
        public int typeColumn;

        private PhonebookResult() {
        }
    }

    public AtPhonebook(Context context, HeadsetStateMachine headsetState) {
        this.mContext = context;
        this.mContentResolver = context.getContentResolver();
        this.mStateMachine = headsetState;
        this.mPhonebooks.put("DC", new PhonebookResult());
        this.mPhonebooks.put("RC", new PhonebookResult());
        this.mPhonebooks.put("MC", new PhonebookResult());
        this.mPhonebooks.put("ME", new PhonebookResult());
        this.mCurrentPhonebook = "ME";
        this.mCpbrIndex2 = -1;
        this.mCpbrIndex1 = -1;
        this.mCheckingAccessPermission = DBG;
    }

    public void cleanup() {
        this.mPhonebooks.clear();
    }

    public String getLastDialledNumber() {
        String[] projection = {"number"};
        Cursor cursor = this.mContentResolver.query(CallLog.Calls.CONTENT_URI, projection, OUTGOING_CALL_WHERE, null, "date DESC LIMIT 1");
        if (cursor == null) {
            return null;
        }
        if (cursor.getCount() < 1) {
            cursor.close();
            return null;
        }
        cursor.moveToNext();
        int column = cursor.getColumnIndexOrThrow("number");
        String number = cursor.getString(column);
        cursor.close();
        return number;
    }

    public String getDialedNumber(int index) {
        String[] projection = {"number"};
        Cursor cursor = this.mContentResolver.query(CallLog.Calls.CONTENT_URI, projection, OUTGOING_CALL_WHERE, null, "date DESC LIMIT " + index);
        if (cursor == null) {
            Log.e(TAG, "cursor is null in getDialedNumber");
            return null;
        }
        if (cursor.getCount() < 1) {
            Log.d(TAG, "result number is " + cursor.getCount() + ", close the cursor");
            cursor.close();
            return null;
        }
        cursor.moveToLast();
        int column = cursor.getColumnIndexOrThrow("number");
        String number = cursor.getString(column);
        cursor.close();
        return number;
    }

    public boolean getCheckingAccessPermission() {
        return this.mCheckingAccessPermission;
    }

    public void setCheckingAccessPermission(boolean checkingAccessPermission) {
        this.mCheckingAccessPermission = checkingAccessPermission;
    }

    public void setCpbrIndex(int cpbrIndex) {
        this.mCpbrIndex2 = cpbrIndex;
        this.mCpbrIndex1 = cpbrIndex;
    }

    private byte[] getByteAddress(BluetoothDevice device) {
        return Utils.getBytesFromAddress(device.getAddress());
    }

    public void handleCscsCommand(String atString, int type, BluetoothDevice device) {
        log("handleCscsCommand - atString = " + atString);
        int atCommandResult = 0;
        int atCommandErrorCode = -1;
        String atCommandResponse = null;
        switch (type) {
            case 0:
                log("handleCscsCommand - Read Command");
                atCommandResponse = "+CSCS: \"" + this.mCharacterSet + "\"";
                atCommandResult = 1;
                break;
            case 1:
                log("handleCscsCommand - Set Command");
                String[] args = atString.split("=");
                if (args.length < 2 || !(args[1] instanceof String)) {
                    this.mStateMachine.atResponseCodeNative(0, -1, getByteAddress(device));
                } else {
                    String characterSet = atString.split("=")[1].replace("\"", "");
                    if (characterSet.equals("GSM") || characterSet.equals("IRA") || characterSet.equals("UTF-8") || characterSet.equals("UTF8")) {
                        this.mCharacterSet = characterSet;
                        atCommandResult = 1;
                    } else {
                        atCommandErrorCode = 4;
                    }
                }
                break;
            case 2:
                log("handleCscsCommand - Test Command");
                atCommandResponse = "+CSCS: (\"UTF-8\",\"IRA\",\"GSM\")";
                atCommandResult = 1;
                break;
            default:
                log("handleCscsCommand - Invalid chars");
                atCommandErrorCode = 25;
                break;
        }
        if (atCommandResponse != null) {
            this.mStateMachine.atResponseStringNative(atCommandResponse, getByteAddress(device));
        }
        this.mStateMachine.atResponseCodeNative(atCommandResult, atCommandErrorCode, getByteAddress(device));
    }

    public void handleCpbsCommand(String atString, int type, BluetoothDevice device) {
        log("handleCpbsCommand - atString = " + atString);
        int atCommandResult = 0;
        int atCommandErrorCode = -1;
        String atCommandResponse = null;
        switch (type) {
            case 0:
                log("handleCpbsCommand - read command");
                if ("SM".equals(this.mCurrentPhonebook)) {
                    atCommandResponse = "+CPBS: \"SM\",0," + getMaxPhoneBookSize(0);
                    atCommandResult = 1;
                } else {
                    PhonebookResult pbr = getPhonebookResult(this.mCurrentPhonebook, true);
                    if (pbr == null) {
                        atCommandErrorCode = 4;
                    } else {
                        int size = pbr.cursor.getCount();
                        atCommandResponse = "+CPBS: \"" + this.mCurrentPhonebook + "\"," + size + "," + getMaxPhoneBookSize(size);
                        pbr.cursor.close();
                        pbr.cursor = null;
                        atCommandResult = 1;
                    }
                }
                break;
            case 1:
                log("handleCpbsCommand - set command");
                String[] args = atString.split("=");
                if (args.length < 2 || !(args[1] instanceof String)) {
                    atCommandErrorCode = 4;
                } else {
                    String pb = args[1].trim();
                    while (pb.endsWith("\"")) {
                        pb = pb.substring(0, pb.length() - 1);
                    }
                    while (pb.startsWith("\"")) {
                        pb = pb.substring(1, pb.length());
                    }
                    if (getPhonebookResult(pb, DBG) == null && !"SM".equals(pb)) {
                        atCommandErrorCode = 3;
                    } else {
                        this.mCurrentPhonebook = pb;
                        atCommandResult = 1;
                    }
                }
                break;
            case 2:
                log("handleCpbsCommand - test command");
                atCommandResponse = "+CPBS: (\"ME\",\"SM\",\"DC\",\"RC\",\"MC\")";
                atCommandResult = 1;
                break;
            default:
                log("handleCpbsCommand - invalid chars");
                atCommandErrorCode = 25;
                break;
        }
        if (atCommandResponse != null) {
            this.mStateMachine.atResponseStringNative(atCommandResponse, getByteAddress(device));
        }
        this.mStateMachine.atResponseCodeNative(atCommandResult, atCommandErrorCode, getByteAddress(device));
    }

    public void handleCpbrCommand(String atString, int type, BluetoothDevice remoteDevice) {
        int index2;
        int size;
        log("handleCpbrCommand - atString = " + atString);
        switch (type) {
            case 0:
            case 1:
                log("handleCpbrCommand - set/read command");
                if (this.mCpbrIndex1 != -1) {
                    this.mStateMachine.atResponseCodeNative(0, 3, getByteAddress(remoteDevice));
                } else if (atString.split("=").length < 2) {
                    this.mStateMachine.atResponseCodeNative(0, -1, getByteAddress(remoteDevice));
                } else {
                    String atCommand = atString.split("=")[1];
                    String[] indices = atCommand.split(",");
                    for (int i = 0; i < indices.length; i++) {
                        indices[i] = indices[i].replace(';', ' ').trim();
                    }
                    try {
                        int index1 = Integer.parseInt(indices[0]);
                        if (indices.length == 1) {
                            index2 = index1;
                        } else {
                            index2 = Integer.parseInt(indices[1]);
                        }
                        this.mCpbrIndex1 = index1;
                        this.mCpbrIndex2 = index2;
                        this.mCheckingAccessPermission = true;
                        int permission = checkAccessPermission(remoteDevice);
                        if (permission == 1) {
                            this.mCheckingAccessPermission = DBG;
                            int atCommandResult = processCpbrCommand(remoteDevice);
                            this.mCpbrIndex2 = -1;
                            this.mCpbrIndex1 = -1;
                            this.mStateMachine.atResponseCodeNative(atCommandResult, -1, getByteAddress(remoteDevice));
                        } else if (permission == 2) {
                            this.mCheckingAccessPermission = DBG;
                            this.mCpbrIndex2 = -1;
                            this.mCpbrIndex1 = -1;
                            this.mStateMachine.atResponseCodeNative(0, 0, getByteAddress(remoteDevice));
                        }
                    } catch (Exception e) {
                        log("handleCpbrCommand - exception - invalid chars: " + e.toString());
                        this.mStateMachine.atResponseCodeNative(0, 25, getByteAddress(remoteDevice));
                        return;
                    }
                }
                break;
            case 2:
                log("handleCpbrCommand - test command");
                if ("SM".equals(this.mCurrentPhonebook)) {
                    size = 0;
                } else {
                    PhonebookResult pbr = getPhonebookResult(this.mCurrentPhonebook, true);
                    if (pbr == null) {
                        this.mStateMachine.atResponseCodeNative(0, 3, getByteAddress(remoteDevice));
                    } else {
                        size = pbr.cursor.getCount();
                        log("handleCpbrCommand - size = " + size);
                        pbr.cursor.close();
                        pbr.cursor = null;
                    }
                }
                if (size == 0) {
                    size = 1;
                }
                String atCommandResponse = "+CPBR: (1-" + size + "),30,30";
                if (atCommandResponse != null) {
                    this.mStateMachine.atResponseStringNative(atCommandResponse, getByteAddress(remoteDevice));
                }
                this.mStateMachine.atResponseCodeNative(1, -1, getByteAddress(remoteDevice));
                break;
            default:
                log("handleCpbrCommand - invalid chars");
                this.mStateMachine.atResponseCodeNative(0, 25, getByteAddress(remoteDevice));
                break;
        }
    }

    private synchronized PhonebookResult getPhonebookResult(String pb, boolean force) {
        PhonebookResult pbr;
        if (pb == null) {
            pbr = null;
        } else {
            pbr = this.mPhonebooks.get(pb);
            if (pbr == null) {
                pbr = new PhonebookResult();
            }
            if (force || pbr.cursor == null) {
                if (!queryPhonebook(pb, pbr)) {
                    pbr = null;
                }
            }
        }
        return pbr;
    }

    private synchronized boolean queryPhonebook(String pb, PhonebookResult pbr) {
        boolean z;
        String where;
        boolean ancillaryPhonebook = true;
        if (pb.equals("ME")) {
            ancillaryPhonebook = DBG;
            where = VISIBLE_PHONEBOOK_WHERE;
        } else if (pb.equals("DC")) {
            where = OUTGOING_CALL_WHERE;
        } else if (pb.equals("RC")) {
            where = INCOMING_CALL_WHERE;
        } else if (pb.equals("MC")) {
            where = MISSED_CALL_WHERE;
        } else {
            z = false;
        }
        if (pbr.cursor != null) {
            pbr.cursor.close();
            pbr.cursor = null;
        }
        if (ancillaryPhonebook) {
            pbr.cursor = this.mContentResolver.query(CallLog.Calls.CONTENT_URI, CALLS_PROJECTION, where, null, "date DESC LIMIT 16384");
            if (pbr.cursor == null) {
                z = false;
            } else {
                pbr.numberColumn = pbr.cursor.getColumnIndexOrThrow("number");
                pbr.numberPresentationColumn = pbr.cursor.getColumnIndexOrThrow("presentation");
                pbr.typeColumn = -1;
                pbr.nameColumn = -1;
                Log.i(TAG, "Refreshed phonebook " + pb + " with " + pbr.cursor.getCount() + " results");
                z = true;
            }
        } else {
            pbr.cursor = this.mContentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, PHONES_PROJECTION, where, null, "data1 LIMIT 16384");
            if (pbr.cursor == null) {
                z = false;
            } else {
                pbr.numberColumn = pbr.cursor.getColumnIndex("data1");
                pbr.numberPresentationColumn = -1;
                pbr.typeColumn = pbr.cursor.getColumnIndex("data2");
                pbr.nameColumn = pbr.cursor.getColumnIndex("display_name");
                Log.i(TAG, "Refreshed phonebook " + pb + " with " + pbr.cursor.getCount() + " results");
                z = true;
            }
        }
        return z;
    }

    synchronized void resetAtState() {
        this.mCharacterSet = "UTF-8";
        this.mCpbrIndex2 = -1;
        this.mCpbrIndex1 = -1;
        this.mCheckingAccessPermission = DBG;
    }

    private synchronized int getMaxPhoneBookSize(int currSize) {
        int iRoundUpToPowerOfTwo;
        synchronized (this) {
            int maxSize = currSize >= 100 ? currSize : 100;
            iRoundUpToPowerOfTwo = roundUpToPowerOfTwo(maxSize + (maxSize / 2));
        }
        return iRoundUpToPowerOfTwo;
    }

    private int roundUpToPowerOfTwo(int x) {
        int x2 = x | (x >> 1);
        int x3 = x2 | (x2 >> 2);
        int x4 = x3 | (x3 >> 4);
        int x5 = x4 | (x4 >> 8);
        return (x5 | (x5 >> 16)) + 1;
    }

    int processCpbrCommand(BluetoothDevice device) {
        log("processCpbrCommand");
        new StringBuilder();
        if ("SM".equals(this.mCurrentPhonebook)) {
            return 1;
        }
        PhonebookResult pbr = getPhonebookResult(this.mCurrentPhonebook, true);
        if (pbr == null) {
            return 0;
        }
        if (pbr.cursor.getCount() == 0 || this.mCpbrIndex1 <= 0 || this.mCpbrIndex2 < this.mCpbrIndex1 || this.mCpbrIndex2 > pbr.cursor.getCount() || this.mCpbrIndex1 > pbr.cursor.getCount()) {
            return 1;
        }
        pbr.cursor.moveToPosition(this.mCpbrIndex1 - 1);
        log("mCpbrIndex1 = " + this.mCpbrIndex1 + " and mCpbrIndex2 = " + this.mCpbrIndex2);
        for (int index = this.mCpbrIndex1; index <= this.mCpbrIndex2; index++) {
            String number = pbr.cursor.getString(pbr.numberColumn);
            String name = null;
            if (pbr.nameColumn == -1 && number != null && number.length() > 0) {
                Cursor c = this.mContentResolver.query(Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, number), new String[]{"display_name", "type"}, null, null, null);
                if (c != null) {
                    if (c.moveToFirst()) {
                        name = c.getString(0);
                        c.getInt(1);
                    }
                    c.close();
                }
            } else if (pbr.nameColumn != -1) {
                name = pbr.cursor.getString(pbr.nameColumn);
            } else {
                log("processCpbrCommand: empty name and number");
            }
            if (name == null) {
                name = "";
            }
            String name2 = name.trim();
            if (name2.length() > 28) {
                name2 = name2.substring(0, 28);
            }
            if (pbr.typeColumn != -1) {
                int type = pbr.cursor.getInt(pbr.typeColumn);
                name2 = name2 + "/" + getPhoneType(type);
            }
            if (number == null) {
                number = "";
            }
            int regionType = PhoneNumberUtils.toaFromString(number);
            String number2 = PhoneNumberUtils.stripSeparators(number.trim());
            if (number2.length() > 30) {
                number2 = number2.substring(0, 30);
            }
            int numberPresentation = 1;
            if (pbr.numberPresentationColumn != -1) {
                numberPresentation = pbr.cursor.getInt(pbr.numberPresentationColumn);
            }
            if (numberPresentation != 1) {
                number2 = "";
                name2 = this.mContext.getString(R.string.unknownNumber);
            }
            if (!name2.equals("") && this.mCharacterSet.equals("GSM")) {
                byte[] nameByte = GsmAlphabet.stringToGsm8BitPacked(name2);
                if (nameByte == null) {
                    name2 = this.mContext.getString(R.string.unknownNumber);
                } else {
                    name2 = new String(nameByte);
                }
            }
            String record = ("+CPBR: " + index + ",\"" + number2 + "\"," + regionType + ",\"" + name2 + "\"") + "\r\n\r\n";
            log("processCpbrCommand - atCommandResponse = " + record);
            this.mStateMachine.atResponseStringNative(record, getByteAddress(device));
            if (!pbr.cursor.moveToNext()) {
                break;
            }
        }
        if (pbr != null && pbr.cursor != null) {
            pbr.cursor.close();
            pbr.cursor = null;
        }
        return 1;
    }

    private int checkAccessPermission(BluetoothDevice remoteDevice) {
        log("checkAccessPermission");
        int permission = remoteDevice.getPhonebookAccessPermission();
        if (permission == 0) {
            log("checkAccessPermission - ACTION_CONNECTION_ACCESS_REQUEST");
            Intent intent = new Intent("android.bluetooth.device.action.CONNECTION_ACCESS_REQUEST");
            intent.setClassName(ACCESS_AUTHORITY_PACKAGE, ACCESS_AUTHORITY_CLASS);
            intent.putExtra("android.bluetooth.device.extra.ACCESS_REQUEST_TYPE", 2);
            intent.putExtra("android.bluetooth.device.extra.DEVICE", remoteDevice);
            this.mContext.sendOrderedBroadcast(intent, "android.permission.BLUETOOTH_ADMIN");
        }
        return permission;
    }

    private static String getPhoneType(int type) {
        switch (type) {
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

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}
