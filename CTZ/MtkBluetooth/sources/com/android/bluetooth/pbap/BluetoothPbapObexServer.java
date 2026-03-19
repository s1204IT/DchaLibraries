package com.android.bluetooth.pbap;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.os.UserManager;
import android.provider.CallLog;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import com.android.bluetooth.opp.BluetoothShare;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.obex.ApplicationParameter;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ServerRequestHandler;

public class BluetoothPbapObexServer extends ServerRequestHandler {
    private static final int CALLLOG_NUM_LIMIT = 50;
    private static final String CCH = "cch";
    private static final boolean D = true;
    private static final String ICH = "ich";
    public static final long INVALID_VALUE_PARAMETER = -1;
    private static final String MCH = "mch";
    private static final int NEED_SEND_BODY = -1;
    private static final String OCH = "och";
    public static final int ORDER_BY_ALPHABETICAL = 1;
    public static final int ORDER_BY_INDEXED = 0;
    private static final String PB = "pb";
    private static final String SIM1 = "SIM1";
    private static final String TAG = "BluetoothPbapObexServer";
    private static final String TYPE_LISTING = "x-bt/vcard-listing";
    private static final String TYPE_PB = "x-bt/phonebook";
    private static final String TYPE_VCARD = "x-bt/vcard";
    private static final int UUID_LENGTH = 16;
    private static final int VCARD_NAME_SUFFIX_LENGTH = 5;
    private Handler mCallback;
    private AppParamValue mConnAppParamValue;
    private Context mContext;
    private PbapStateMachine mStateMachine;
    private BluetoothPbapVcardManager mVcardManager;
    private static final boolean V = BluetoothPbapService.VERBOSE;
    private static final byte[] PBAP_TARGET = {121, 97, 53, -16, -16, -59, 17, -40, 9, 102, 8, 0, 32, 12, -102, 102};
    private static final String TELECOM_PATH = "/telecom";
    private static final String PB_PATH = "/telecom/pb";
    private static final String ICH_PATH = "/telecom/ich";
    private static final String OCH_PATH = "/telecom/och";
    private static final String MCH_PATH = "/telecom/mch";
    private static final String CCH_PATH = "/telecom/cch";
    private static final String[] LEGAL_PATH = {TELECOM_PATH, PB_PATH, ICH_PATH, OCH_PATH, MCH_PATH, CCH_PATH};
    private static final String[] LEGAL_PATH_WITH_SIM = {TELECOM_PATH, PB_PATH, ICH_PATH, OCH_PATH, MCH_PATH, CCH_PATH, "/SIM1", "/SIM1/telecom", "/SIM1/telecom/ich", "/SIM1/telecom/och", "/SIM1/telecom/mch", "/SIM1/telecom/cch", "/SIM1/telecom/pb"};
    public static boolean sIsAborted = false;
    private boolean mNeedPhonebookSize = false;
    private boolean mNeedNewMissedCallsNum = false;
    private boolean mVcardSelector = false;
    private String mCurrentPath = "";
    private int mOrderBy = 0;
    private long mDatabaseIdentifierLow = -1;
    private long mDatabaseIdentifierHigh = -1;
    private long mFolderVersionCounterbitMask = 8;
    private long mDatabaseIdentifierBitMask = 4;

    public static class ContentType {
        public static final int COMBINED_CALL_HISTORY = 5;
        public static final int INCOMING_CALL_HISTORY = 2;
        public static final int MISSED_CALL_HISTORY = 4;
        public static final int OUTGOING_CALL_HISTORY = 3;
        public static final int PHONEBOOK = 1;
    }

    public BluetoothPbapObexServer(Handler handler, Context context, PbapStateMachine pbapStateMachine) {
        this.mCallback = null;
        this.mCallback = handler;
        this.mContext = context;
        this.mVcardManager = new BluetoothPbapVcardManager(this.mContext);
        this.mStateMachine = pbapStateMachine;
    }

    public int onConnect(HeaderSet headerSet, HeaderSet headerSet2) {
        if (V) {
            logHeader(headerSet);
        }
        notifyUpdateWakeLock();
        try {
            byte[] bArr = (byte[]) headerSet.getHeader(70);
            if (bArr == null) {
                return 198;
            }
            Log.d(TAG, "onConnect(): uuid=" + Arrays.toString(bArr));
            if (bArr.length != 16) {
                Log.w(TAG, "Wrong UUID length");
                return 198;
            }
            for (int i = 0; i < 16; i++) {
                if (bArr[i] != PBAP_TARGET[i]) {
                    Log.w(TAG, "Wrong UUID");
                    return 198;
                }
            }
            headerSet2.setHeader(74, bArr);
            try {
                byte[] bArr2 = (byte[]) headerSet.getHeader(74);
                if (bArr2 != null) {
                    Log.d(TAG, "onConnect(): remote=" + Arrays.toString(bArr2));
                    headerSet2.setHeader(70, bArr2);
                }
                try {
                    this.mConnAppParamValue = new AppParamValue();
                    byte[] bArr3 = (byte[]) headerSet.getHeader(76);
                    if (bArr3 != null) {
                        if (!parseApplicationParameter(bArr3, this.mConnAppParamValue)) {
                            return BluetoothShare.STATUS_RUNNING;
                        }
                    }
                    if (V) {
                        Log.v(TAG, "onConnect(): uuid is ok, will send out MSG_SESSION_ESTABLISHED msg.");
                        return 160;
                    }
                    return 160;
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                    return 208;
                }
            } catch (IOException e2) {
                Log.e(TAG, e2.toString());
                return 208;
            }
        } catch (IOException e3) {
            Log.e(TAG, e3.toString());
            return 208;
        }
    }

    public void onDisconnect(HeaderSet headerSet, HeaderSet headerSet2) {
        Log.d(TAG, "onDisconnect(): enter");
        if (V) {
            logHeader(headerSet);
        }
        notifyUpdateWakeLock();
        headerSet2.responseCode = 160;
    }

    public int onAbort(HeaderSet headerSet, HeaderSet headerSet2) {
        Log.d(TAG, "onAbort(): enter.");
        notifyUpdateWakeLock();
        sIsAborted = true;
        return 160;
    }

    public int onPut(Operation operation) {
        Log.d(TAG, "onPut(): not support PUT request.");
        notifyUpdateWakeLock();
        return BluetoothShare.STATUS_RUNNING;
    }

    public int onDelete(HeaderSet headerSet, HeaderSet headerSet2) {
        Log.d(TAG, "onDelete(): not support PUT request.");
        notifyUpdateWakeLock();
        return BluetoothShare.STATUS_RUNNING;
    }

    public int onSetPath(HeaderSet headerSet, HeaderSet headerSet2, boolean z, boolean z2) {
        if (V) {
            logHeader(headerSet);
        }
        Log.d(TAG, "before setPath, mCurrentPath ==  " + this.mCurrentPath);
        notifyUpdateWakeLock();
        String strSubstring = this.mCurrentPath;
        try {
            String str = (String) headerSet.getHeader(1);
            Log.d(TAG, "backup=" + z + " create=" + z2 + " name=" + str);
            if (z) {
                if (strSubstring.length() != 0) {
                    strSubstring = strSubstring.substring(0, strSubstring.lastIndexOf("/"));
                }
            } else if (str == null) {
                strSubstring = "";
            } else {
                strSubstring = strSubstring + "/" + str;
            }
            if (strSubstring.length() != 0 && !isLegalPath(strSubstring)) {
                if (z2) {
                    Log.w(TAG, "path create is forbidden!");
                    return 195;
                }
                Log.w(TAG, "path is not legal");
                return 196;
            }
            this.mCurrentPath = strSubstring;
            if (V) {
                Log.v(TAG, "after setPath, mCurrentPath ==  " + this.mCurrentPath);
                return 160;
            }
            return 160;
        } catch (IOException e) {
            Log.e(TAG, "Get name header fail");
            return 208;
        }
    }

    public void onClose() {
        this.mStateMachine.sendMessage(3);
    }

    public int onGet(Operation operation) {
        boolean z;
        notifyUpdateWakeLock();
        sIsAborted = false;
        HeaderSet headerSet = new HeaderSet();
        AppParamValue appParamValue = new AppParamValue();
        try {
            HeaderSet receivedHeader = operation.getReceivedHeader();
            String str = (String) receivedHeader.getHeader(66);
            String str2 = (String) receivedHeader.getHeader(1);
            byte[] bArr = (byte[]) receivedHeader.getHeader(76);
            if (V) {
                logHeader(receivedHeader);
            }
            Log.d(TAG, "OnGet type is " + str + "; name is " + str2);
            if (str == null) {
                return 198;
            }
            if (!UserManager.get(this.mContext).isUserUnlocked()) {
                Log.e(TAG, "Storage locked, " + str + " failed");
                return 211;
            }
            if (!TextUtils.isEmpty(str2)) {
                z = true;
            } else {
                z = false;
            }
            if (z && (!z || !str.equals(TYPE_VCARD))) {
                if (str2.contains(SIM1.subSequence(0, SIM1.length()))) {
                    Log.w(TAG, "Not support access SIM card info!");
                    return 198;
                }
                if (isNameMatchTarget(str2, PB)) {
                    appParamValue.needTag = 1;
                    Log.v(TAG, "download phonebook request");
                } else if (isNameMatchTarget(str2, ICH)) {
                    appParamValue.needTag = 2;
                    appParamValue.callHistoryVersionCounter = this.mVcardManager.getCallHistoryPrimaryFolderVersion(2);
                    Log.v(TAG, "download incoming calls request");
                } else if (isNameMatchTarget(str2, OCH)) {
                    appParamValue.needTag = 3;
                    appParamValue.callHistoryVersionCounter = this.mVcardManager.getCallHistoryPrimaryFolderVersion(3);
                    Log.v(TAG, "download outgoing calls request");
                } else if (isNameMatchTarget(str2, MCH)) {
                    appParamValue.needTag = 4;
                    appParamValue.callHistoryVersionCounter = this.mVcardManager.getCallHistoryPrimaryFolderVersion(4);
                    this.mNeedNewMissedCallsNum = true;
                    Log.v(TAG, "download missed calls request");
                } else if (isNameMatchTarget(str2, CCH)) {
                    appParamValue.needTag = 5;
                    appParamValue.callHistoryVersionCounter = this.mVcardManager.getCallHistoryPrimaryFolderVersion(5);
                    Log.v(TAG, "download combined calls request");
                } else {
                    Log.w(TAG, "Input name doesn't contain valid info!!!");
                    return 196;
                }
            } else {
                Log.d(TAG, "Guess what carkit actually want from current path (" + this.mCurrentPath + ")");
                if (this.mCurrentPath.equals(PB_PATH)) {
                    appParamValue.needTag = 1;
                } else if (this.mCurrentPath.equals(ICH_PATH)) {
                    appParamValue.needTag = 2;
                } else if (this.mCurrentPath.equals(OCH_PATH)) {
                    appParamValue.needTag = 3;
                } else if (this.mCurrentPath.equals(MCH_PATH)) {
                    appParamValue.needTag = 4;
                    this.mNeedNewMissedCallsNum = true;
                } else if (this.mCurrentPath.equals(CCH_PATH)) {
                    appParamValue.needTag = 5;
                } else if (this.mCurrentPath.equals(TELECOM_PATH)) {
                    if (!z && str.equals(TYPE_LISTING)) {
                        Log.e(TAG, "invalid vcard listing request in default folder");
                        return 196;
                    }
                } else {
                    Log.w(TAG, "mCurrentpath is not valid path!!!");
                    return 198;
                }
                Log.v(TAG, "onGet(): appParamValue.needTag=" + appParamValue.needTag);
            }
            if (bArr != null && !parseApplicationParameter(bArr, appParamValue)) {
                return BluetoothShare.STATUS_RUNNING;
            }
            if (str.equals(TYPE_LISTING)) {
                return pullVcardListing(bArr, appParamValue, headerSet, operation, str2);
            }
            if (str.equals(TYPE_VCARD)) {
                return pullVcardEntry(bArr, appParamValue, operation, headerSet, str2, this.mCurrentPath);
            }
            if (str.equals(TYPE_PB)) {
                return pullPhonebook(bArr, appParamValue, headerSet, operation, str2);
            }
            Log.w(TAG, "unknown type request!!!");
            return 198;
        } catch (IOException e) {
            Log.e(TAG, "request headers error");
            return 208;
        }
    }

    private boolean isNameMatchTarget(String str, String str2) {
        if (str == null) {
            return false;
        }
        if (str.endsWith(".vcf")) {
            str = str.substring(0, str.length() - ".vcf".length());
        }
        for (String str3 : str.split("/")) {
            if (str3.equals(str2)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLegalPath(String str) {
        if (str.length() == 0) {
            return true;
        }
        for (int i = 0; i < LEGAL_PATH.length; i++) {
            if (str.equals(LEGAL_PATH[i])) {
                return true;
            }
        }
        return false;
    }

    private class AppParamValue {
        public byte[] callHistoryVersionCounter;
        public int maxListCount = 65535;
        public int listStartOffset = 0;
        public String searchValue = "";
        public String searchAttr = "";
        public String order = "";
        public int needTag = 0;
        public boolean vcard21 = true;
        public boolean ignorefilter = true;
        public String vCardSelectorOperator = "0";
        public byte[] propertySelector = {0, 0, 0, 0, 0, 0, 0, 0};
        public byte[] vCardSelector = {0, 0, 0, 0, 0, 0, 0, 0};
        public byte[] supportedFeature = {0, 0, 0, 0};

        AppParamValue() {
        }

        public void dump() {
            Log.i(BluetoothPbapObexServer.TAG, "maxListCount=" + this.maxListCount + " listStartOffset=" + this.listStartOffset + " searchValue=" + this.searchValue + " searchAttr=" + this.searchAttr + " needTag=" + this.needTag + " vcard21=" + this.vcard21 + " order=" + this.order + "vcardselector=" + this.vCardSelector + "vcardselop=" + this.vCardSelectorOperator);
        }
    }

    private boolean parseApplicationParameter(byte[] bArr, AppParamValue appParamValue) {
        int i = 0;
        boolean z = true;
        while (i < bArr.length && z) {
            byte b = bArr[i];
            if (b == 12) {
                int i2 = i + 2;
                for (int i3 = 0; i3 < 8; i3++) {
                    int i4 = i2 + i3;
                    if (bArr[i4] != 0) {
                        this.mVcardSelector = true;
                        appParamValue.vCardSelector[i3] = bArr[i4];
                    }
                }
                i = i2 + 8;
            } else if (b == 14) {
                int i5 = i + 2;
                appParamValue.vCardSelectorOperator = Byte.toString(bArr[i5]);
                i = i5 + 1;
            } else if (b == 16) {
                int i6 = i + 2;
                for (int i7 = 0; i7 < 4; i7++) {
                    int i8 = i6 + i7;
                    if (bArr[i8] != 0) {
                        appParamValue.supportedFeature[i7] = bArr[i8];
                    }
                }
                i = i6 + 4;
            } else {
                switch (b) {
                    case 1:
                        int i9 = i + 2;
                        appParamValue.order = Byte.toString(bArr[i9]);
                        i = i9 + 1;
                        continue;
                    case 2:
                        i++;
                        byte b2 = bArr[i];
                        if (b2 != 0) {
                            int i10 = i + b2;
                            if (bArr[i10] == 0) {
                                appParamValue.searchValue = new String(bArr, i + 1, b2 - 1);
                            } else {
                                appParamValue.searchValue = new String(bArr, i + 1, (int) b2);
                            }
                            i = i10 + 1;
                        }
                        break;
                    case 3:
                        int i11 = i + 2;
                        appParamValue.searchAttr = Byte.toString(bArr[i11]);
                        i = i11 + 1;
                        continue;
                    case 4:
                        int i12 = i + 2;
                        if (bArr[i12] == 0 && bArr[i12 + 1] == 0) {
                            this.mNeedPhonebookSize = true;
                        } else {
                            appParamValue.maxListCount = ((bArr[i12] & 255) * 256) + (bArr[i12 + 1] & 255);
                        }
                        i = i12 + 2;
                        continue;
                    case 5:
                        int i13 = i + 2;
                        appParamValue.listStartOffset = ((bArr[i13] & 255) * 256) + (bArr[i13 + 1] & 255);
                        i = i13 + 2;
                        continue;
                    case 6:
                        int i14 = i + 2;
                        for (int i15 = 0; i15 < 8; i15++) {
                            int i16 = i14 + i15;
                            if (bArr[i16] != 0) {
                                appParamValue.ignorefilter = false;
                                appParamValue.propertySelector[i15] = bArr[i16];
                            }
                        }
                        i = i14 + 8;
                        continue;
                    case 7:
                        int i17 = i + 2;
                        if (bArr[i17] != 0) {
                            appParamValue.vcard21 = false;
                        }
                        i = i17 + 1;
                        continue;
                    default:
                        Log.e(TAG, "Parse Application Parameter error");
                        break;
                }
                z = false;
            }
        }
        appParamValue.dump();
        return z;
    }

    private int sendVcardListingXml(AppParamValue appParamValue, Operation operation, int i, int i2) {
        String str;
        int size;
        int iCreateList;
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"?>");
        sb.append("<!DOCTYPE vcard-listing SYSTEM \"vcard-listing.dtd\">");
        sb.append("<vCard-listing version=\"1.0\">");
        if (appParamValue.needTag == 1) {
            String str2 = "";
            if (appParamValue.searchAttr.equals("0")) {
                str2 = "name";
            } else if (appParamValue.searchAttr.equals("1")) {
                str2 = "number";
            }
            String str3 = str2;
            if (str3.length() <= 0) {
                return 204;
            }
            iCreateList = createList(appParamValue, i, i2, sb, str3);
        } else {
            if (appParamValue.searchAttr.equals("0")) {
                str = "name";
            } else {
                if (!appParamValue.searchAttr.equals("1")) {
                    return 204;
                }
                str = "number";
            }
            SparseArray<String> sparseArrayLoadCallHistoryList = this.mVcardManager.loadCallHistoryList(appParamValue.needTag, appParamValue.searchValue, str);
            if (sparseArrayLoadCallHistoryList.size() >= appParamValue.maxListCount) {
                size = appParamValue.maxListCount;
            } else {
                size = sparseArrayLoadCallHistoryList.size();
            }
            int i3 = appParamValue.listStartOffset;
            int size2 = i3 + size;
            if (size2 > sparseArrayLoadCallHistoryList.size()) {
                size2 = sparseArrayLoadCallHistoryList.size();
            }
            Log.d(TAG, "call log list, size=" + size + " offset=" + appParamValue.listStartOffset);
            while (i3 < size2) {
                writeVCardEntry(sparseArrayLoadCallHistoryList.keyAt(i3), sparseArrayLoadCallHistoryList.valueAt(i3), sb);
                i3++;
            }
            iCreateList = 0;
        }
        sb.append("</vCard-listing>");
        Log.d(TAG, "itemsFound =" + iCreateList);
        return pushBytes(operation, sb.toString());
    }

    private int createList(AppParamValue appParamValue, int i, int i2, StringBuilder sb, String str) {
        ArrayList<String> phonebookNameList;
        int i3;
        if (this.mVcardSelector) {
            phonebookNameList = this.mVcardManager.getSelectedPhonebookNameList(this.mOrderBy, appParamValue.vcard21, i, i2, appParamValue.vCardSelector, appParamValue.vCardSelectorOperator);
        } else {
            phonebookNameList = this.mVcardManager.getPhonebookNameList(this.mOrderBy);
        }
        int size = phonebookNameList.size() >= appParamValue.maxListCount ? appParamValue.maxListCount : phonebookNameList.size();
        int size2 = phonebookNameList.size();
        String lowerCase = "";
        Log.d(TAG, "search by " + str + ", requestSize=" + size + " offset=" + appParamValue.listStartOffset + " searchValue=" + appParamValue.searchValue);
        if (str.equals("number")) {
            ArrayList<String> contactNamesByNumber = this.mVcardManager.getContactNamesByNumber(appParamValue.searchValue);
            if (this.mOrderBy == 1) {
                Collections.sort(contactNamesByNumber);
            }
            i3 = 0;
            for (int i4 = 0; i4 < contactNamesByNumber.size(); i4++) {
                String strTrim = contactNamesByNumber.get(i4).trim();
                Log.d(TAG, "compareValue=" + strTrim);
                for (int i5 = appParamValue.listStartOffset; i5 < size2 && i3 < size; i5++) {
                    String strSubstring = phonebookNameList.get(i5);
                    if (V) {
                        Log.d(TAG, "currentValue=" + strSubstring);
                    }
                    if (strSubstring.equals(strTrim)) {
                        i3++;
                        if (strSubstring.contains(",")) {
                            strSubstring = strSubstring.substring(0, strSubstring.lastIndexOf(44));
                        }
                        writeVCardEntry(i5, strSubstring, sb);
                    }
                }
                if (i3 >= size) {
                    break;
                }
            }
        } else {
            if (appParamValue.searchValue != null) {
                lowerCase = appParamValue.searchValue.trim().toLowerCase();
            }
            i3 = 0;
            for (int i6 = appParamValue.listStartOffset; i6 < size2 && i3 < size; i6++) {
                String strSubstring2 = phonebookNameList.get(i6);
                if (strSubstring2.contains(",")) {
                    strSubstring2 = strSubstring2.substring(0, strSubstring2.lastIndexOf(44));
                }
                if (appParamValue.searchValue.isEmpty() || strSubstring2.toLowerCase().startsWith(lowerCase)) {
                    i3++;
                    writeVCardEntry(i6, strSubstring2, sb);
                }
            }
        }
        return i3;
    }

    private int pushHeader(Operation operation, HeaderSet headerSet) throws Throwable {
        OutputStream outputStreamOpenOutputStream;
        Log.d(TAG, "Push Header");
        Log.d(TAG, headerSet.toString());
        int i = 208;
        OutputStream outputStream = null;
        try {
            try {
                operation.sendHeaders(headerSet);
                outputStreamOpenOutputStream = operation.openOutputStream();
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            outputStreamOpenOutputStream.flush();
            boolean zCloseStream = closeStream(outputStreamOpenOutputStream, operation);
            operation = zCloseStream;
            if (zCloseStream) {
                i = 160;
                operation = zCloseStream;
            }
        } catch (IOException e2) {
            outputStream = outputStreamOpenOutputStream;
            e = e2;
            Log.e(TAG, e.toString());
            boolean zCloseStream2 = closeStream(outputStream, operation);
            operation = zCloseStream2;
            if (!zCloseStream2) {
            }
        } catch (Throwable th2) {
            th = th2;
            outputStream = outputStreamOpenOutputStream;
            if (!closeStream(outputStream, operation)) {
            }
            throw th;
        }
        return i;
    }

    private int pushBytes(Operation operation, String str) {
        OutputStream outputStreamOpenOutputStream;
        int i = 160;
        if (str == null) {
            Log.w(TAG, "vcardString is null!");
            return 160;
        }
        try {
            outputStreamOpenOutputStream = operation.openOutputStream();
        } catch (IOException e) {
            e = e;
            outputStreamOpenOutputStream = null;
        }
        try {
            outputStreamOpenOutputStream.write(str.getBytes());
            if (V) {
                Log.v(TAG, "Send Data complete!");
            }
        } catch (IOException e2) {
            e = e2;
            Log.e(TAG, "open/write outputstrem failed" + e.toString());
            i = 208;
        }
        if (closeStream(outputStreamOpenOutputStream, operation)) {
            return i;
        }
        return 208;
    }

    private int handleAppParaForResponse(AppParamValue appParamValue, int i, HeaderSet headerSet, Operation operation, String str) {
        boolean zCheckPbapFeatureSupport;
        boolean zCheckPbapFeatureSupport2;
        int count;
        int count2;
        byte[] bArr = new byte[1];
        ApplicationParameter applicationParameter = new ApplicationParameter();
        if (isNameMatchTarget(str, MCH) || isNameMatchTarget(str, ICH) || isNameMatchTarget(str, OCH) || isNameMatchTarget(str, CCH)) {
            zCheckPbapFeatureSupport = checkPbapFeatureSupport(this.mFolderVersionCounterbitMask);
        } else {
            zCheckPbapFeatureSupport = false;
        }
        if (isNameMatchTarget(str, PB)) {
            zCheckPbapFeatureSupport2 = checkPbapFeatureSupport(this.mFolderVersionCounterbitMask);
        } else {
            zCheckPbapFeatureSupport2 = false;
        }
        if (this.mNeedPhonebookSize) {
            Log.d(TAG, "Need Phonebook size in response header.");
            this.mNeedPhonebookSize = false;
            applicationParameter.addAPPHeader((byte) 8, (byte) 2, new byte[]{(byte) ((i / 256) & 255), (byte) ((i % 256) & 255)});
            if (this.mNeedNewMissedCallsNum) {
                this.mNeedNewMissedCallsNum = false;
                Cursor cursorQuery = this.mContext.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, "type = 3 AND new = 1", null, "date DESC");
                if (cursorQuery != null) {
                    count2 = cursorQuery.getCount();
                    cursorQuery.close();
                } else {
                    count2 = 0;
                }
                if (count2 <= 0) {
                    count2 = 0;
                }
                bArr[0] = (byte) count2;
                Log.d(TAG, "handleAppParaForResponse(): mNeedNewMissedCallsNum=true,  num= " + count2);
            }
            if (checkPbapFeatureSupport(this.mDatabaseIdentifierBitMask)) {
                setDbCounters(applicationParameter);
            }
            if (zCheckPbapFeatureSupport2) {
                setFolderVersionCounters(applicationParameter);
            }
            if (zCheckPbapFeatureSupport) {
                setCallversionCounters(applicationParameter, appParamValue);
            }
            headerSet.setHeader(76, applicationParameter.getAPPparam());
            Log.d(TAG, "Send back Phonebook size only, without body info! Size= " + i);
            return pushHeader(operation, headerSet);
        }
        if (this.mNeedNewMissedCallsNum) {
            Log.d(TAG, "Need new missed call num in response header.");
            this.mNeedNewMissedCallsNum = false;
            Cursor cursorQuery2 = this.mContext.getContentResolver().query(CallLog.Calls.CONTENT_URI, null, "type = 3 AND new = 1", null, "date DESC");
            if (cursorQuery2 != null) {
                count = cursorQuery2.getCount();
                cursorQuery2.close();
            } else {
                count = 0;
            }
            if (count <= 0) {
                count = 0;
            }
            bArr[0] = (byte) count;
            Log.d(TAG, "handleAppParaForResponse(): mNeedNewMissedCallsNum=true,  num= " + count);
            applicationParameter.addAPPHeader((byte) 9, (byte) 1, bArr);
            headerSet.setHeader(76, applicationParameter.getAPPparam());
            Log.d(TAG, "handleAppParaForResponse(): mNeedNewMissedCallsNum=true,  num= " + count);
            try {
                operation.sendHeaders(headerSet);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
                return 208;
            }
        }
        if (checkPbapFeatureSupport(this.mDatabaseIdentifierBitMask)) {
            setDbCounters(applicationParameter);
            headerSet.setHeader(76, applicationParameter.getAPPparam());
            try {
                operation.sendHeaders(headerSet);
            } catch (IOException e2) {
                Log.e(TAG, e2.toString());
                return 208;
            }
        }
        if (zCheckPbapFeatureSupport2) {
            setFolderVersionCounters(applicationParameter);
            headerSet.setHeader(76, applicationParameter.getAPPparam());
            try {
                operation.sendHeaders(headerSet);
            } catch (IOException e3) {
                Log.e(TAG, e3.toString());
                return 208;
            }
        }
        if (zCheckPbapFeatureSupport) {
            setCallversionCounters(applicationParameter, appParamValue);
            headerSet.setHeader(76, applicationParameter.getAPPparam());
            try {
                operation.sendHeaders(headerSet);
                return -1;
            } catch (IOException e4) {
                Log.e(TAG, e4.toString());
                return 208;
            }
        }
        return -1;
    }

    private int pullVcardListing(byte[] bArr, AppParamValue appParamValue, HeaderSet headerSet, Operation operation, String str) {
        String strTrim = appParamValue.searchAttr.trim();
        if (strTrim == null || strTrim.length() == 0) {
            appParamValue.searchAttr = "0";
            Log.d(TAG, "searchAttr is not set by PCE, assume search by name by default");
        } else {
            if (!strTrim.equals("0") && !strTrim.equals("1")) {
                Log.w(TAG, "search attr not supported");
                if (!strTrim.equals("2")) {
                    return 204;
                }
                Log.w(TAG, "do not support search by sound");
                return 209;
            }
            Log.i(TAG, "searchAttr is valid: " + strTrim);
        }
        int phonebookSize = this.mVcardManager.getPhonebookSize(appParamValue.needTag);
        int iHandleAppParaForResponse = handleAppParaForResponse(appParamValue, phonebookSize, headerSet, operation, str);
        if (iHandleAppParaForResponse != -1) {
            operation.noBodyHeader();
            return iHandleAppParaForResponse;
        }
        if (phonebookSize == 0) {
            Log.d(TAG, "PhonebookSize is 0, return.");
            return 160;
        }
        String strTrim2 = appParamValue.order.trim();
        if (TextUtils.isEmpty(strTrim2)) {
            strTrim2 = "0";
            Log.d(TAG, "Order parameter is not set by PCE. Assume order by 'Indexed' by default");
        } else {
            if (!strTrim2.equals("0") && !strTrim2.equals("1")) {
                Log.d(TAG, "Order parameter is not supported: " + appParamValue.order);
                if (!strTrim2.equals("2")) {
                    return 204;
                }
                Log.w(TAG, "Do not support order by sound");
                return 209;
            }
            Log.i(TAG, "Order parameter is valid: " + strTrim2);
        }
        if (strTrim2.equals("0")) {
            this.mOrderBy = 0;
        } else if (strTrim2.equals("1")) {
            this.mOrderBy = 1;
        }
        return sendVcardListingXml(appParamValue, operation, iHandleAppParaForResponse, phonebookSize);
    }

    private int pullVcardEntry(byte[] bArr, AppParamValue appParamValue, Operation operation, HeaderSet headerSet, String str, String str2) {
        if (str == null || str.length() < 5) {
            Log.d(TAG, "Name is Null, or the length of name < 5 !");
            return 198;
        }
        int i = 0;
        String strSubstring = str.substring(0, (str.length() - 5) + 1);
        if (strSubstring.trim().length() != 0) {
            try {
                i = Integer.parseInt(strSubstring);
            } catch (NumberFormatException e) {
                Log.e(TAG, "catch number format exception " + e.toString());
                return 198;
            }
        }
        int i2 = i;
        int phonebookSize = this.mVcardManager.getPhonebookSize(appParamValue.needTag);
        int iHandleAppParaForResponse = handleAppParaForResponse(appParamValue, phonebookSize, headerSet, operation, str);
        if (phonebookSize == 0) {
            Log.d(TAG, "PhonebookSize is 0, return.");
            return 196;
        }
        boolean z = appParamValue.vcard21;
        if (appParamValue.needTag == 0) {
            Log.w(TAG, "wrong path!");
            return 198;
        }
        if (appParamValue.needTag == 1) {
            if (i2 < 0 || i2 >= phonebookSize) {
                Log.w(TAG, "The requested vcard is not acceptable! name= " + str);
                return 196;
            }
            if (i2 == 0) {
                return pushBytes(operation, this.mVcardManager.getOwnerPhoneNumberVcard(z, appParamValue.ignorefilter ? null : appParamValue.propertySelector));
            }
            return this.mVcardManager.composeAndSendPhonebookOneVcard(operation, i2, z, null, this.mOrderBy, appParamValue.ignorefilter, appParamValue.propertySelector);
        }
        if (i2 <= 0 || i2 > phonebookSize) {
            Log.w(TAG, "The requested vcard is not acceptable! name= " + str);
            return 196;
        }
        if (i2 >= 1) {
            return this.mVcardManager.composeAndSendSelectedCallLogVcards(appParamValue.needTag, operation, i2, i2, z, iHandleAppParaForResponse, phonebookSize, appParamValue.ignorefilter, appParamValue.propertySelector, appParamValue.vCardSelector, appParamValue.vCardSelectorOperator, this.mVcardSelector);
        }
        return 160;
    }

    private int pullPhonebook(byte[] bArr, AppParamValue appParamValue, HeaderSet headerSet, Operation operation, String str) {
        int iIndexOf;
        if (str != null && (iIndexOf = str.indexOf(".")) >= 0 && iIndexOf <= str.length() && !str.regionMatches(iIndexOf + 1, "vcf", 0, "vcf".length())) {
            Log.w(TAG, "name is not .vcf");
            return 198;
        }
        int phonebookSize = this.mVcardManager.getPhonebookSize(appParamValue.needTag);
        int iHandleAppParaForResponse = handleAppParaForResponse(appParamValue, phonebookSize, headerSet, operation, str);
        if (iHandleAppParaForResponse != -1) {
            operation.noBodyHeader();
            return iHandleAppParaForResponse;
        }
        if (phonebookSize == 0) {
            Log.d(TAG, "PhonebookSize is 0, return.");
            return 160;
        }
        int i = phonebookSize >= appParamValue.maxListCount ? appParamValue.maxListCount : phonebookSize;
        int i2 = appParamValue.listStartOffset;
        if (i2 < 0 || i2 >= phonebookSize) {
            Log.w(TAG, "listStartOffset is not correct! " + i2);
            return 160;
        }
        if (appParamValue.needTag != 1 && i > CALLLOG_NUM_LIMIT) {
            i = CALLLOG_NUM_LIMIT;
        }
        int i3 = (i2 + i) - 1;
        int i4 = phonebookSize - 1;
        int i5 = i3 > i4 ? i4 : i3;
        Log.d(TAG, "pullPhonebook(): requestSize=" + i + " startPoint=" + i2 + " endPoint=" + i5);
        boolean z = appParamValue.vcard21;
        if (appParamValue.needTag == 1) {
            if (i2 == 0) {
                String ownerPhoneNumberVcard = this.mVcardManager.getOwnerPhoneNumberVcard(z, appParamValue.ignorefilter ? null : appParamValue.propertySelector);
                if (i5 == 0) {
                    return pushBytes(operation, ownerPhoneNumberVcard);
                }
                return this.mVcardManager.composeAndSendPhonebookVcards(operation, 1, i5, z, ownerPhoneNumberVcard, iHandleAppParaForResponse, phonebookSize, appParamValue.ignorefilter, appParamValue.propertySelector, appParamValue.vCardSelector, appParamValue.vCardSelectorOperator, this.mVcardSelector);
            }
            return this.mVcardManager.composeAndSendPhonebookVcards(operation, i2, i5, z, null, iHandleAppParaForResponse, phonebookSize, appParamValue.ignorefilter, appParamValue.propertySelector, appParamValue.vCardSelector, appParamValue.vCardSelectorOperator, this.mVcardSelector);
        }
        return this.mVcardManager.composeAndSendSelectedCallLogVcards(appParamValue.needTag, operation, i2 + 1, i5 + 1, z, iHandleAppParaForResponse, phonebookSize, appParamValue.ignorefilter, appParamValue.propertySelector, appParamValue.vCardSelector, appParamValue.vCardSelectorOperator, this.mVcardSelector);
    }

    public static boolean closeStream(OutputStream outputStream, Operation operation) {
        boolean z;
        if (outputStream != null) {
            try {
                outputStream.close();
                z = true;
            } catch (IOException e) {
                Log.e(TAG, "outputStream close failed" + e.toString());
                z = false;
            }
        } else {
            z = true;
        }
        if (operation != null) {
            try {
                operation.close();
                return z;
            } catch (IOException e2) {
                Log.e(TAG, "operation close failed" + e2.toString());
                return false;
            }
        }
        return z;
    }

    public final void onAuthenticationFailure(byte[] bArr) {
    }

    public static final String createSelectionPara(int i) {
        String str;
        switch (i) {
            case 2:
                str = "(type=1 OR type=5)";
                break;
            case 3:
                str = "type=2";
                break;
            case 4:
                str = "type=3";
                break;
            default:
                str = null;
                break;
        }
        if (V) {
            Log.v(TAG, "Call log selection: " + str);
        }
        return str;
    }

    private void xmlEncode(String str, StringBuilder sb) {
        if (str == null) {
            return;
        }
        StringCharacterIterator stringCharacterIterator = new StringCharacterIterator(str);
        for (char cCurrent = stringCharacterIterator.current(); cCurrent != 65535; cCurrent = stringCharacterIterator.next()) {
            if (cCurrent == '<') {
                sb.append("&lt;");
            } else if (cCurrent == '>') {
                sb.append("&gt;");
            } else if (cCurrent == '\"') {
                sb.append("&quot;");
            } else if (cCurrent == '\'') {
                sb.append("&#039;");
            } else if (cCurrent == '&') {
                sb.append("&amp;");
            } else {
                sb.append(cCurrent);
            }
        }
    }

    private void writeVCardEntry(int i, String str, StringBuilder sb) {
        sb.append("<card handle=\"");
        sb.append(i);
        sb.append(".vcf\" name=\"");
        xmlEncode(str, sb);
        sb.append("\"/>");
    }

    private void notifyUpdateWakeLock() {
        Message messageObtain = Message.obtain(this.mCallback);
        messageObtain.what = 5004;
        messageObtain.sendToTarget();
    }

    public static final void logHeader(HeaderSet headerSet) {
        Log.v(TAG, "Dumping HeaderSet " + headerSet.toString());
        try {
            Log.v(TAG, "COUNT : " + headerSet.getHeader(BluetoothShare.STATUS_RUNNING));
            Log.v(TAG, "NAME : " + headerSet.getHeader(1));
            Log.v(TAG, "TYPE : " + headerSet.getHeader(66));
            Log.v(TAG, "LENGTH : " + headerSet.getHeader(195));
            Log.v(TAG, "TIME_ISO_8601 : " + headerSet.getHeader(68));
            Log.v(TAG, "TIME_4_BYTE : " + headerSet.getHeader(196));
            Log.v(TAG, "DESCRIPTION : " + headerSet.getHeader(5));
            Log.v(TAG, "TARGET : " + headerSet.getHeader(70));
            Log.v(TAG, "HTTP : " + headerSet.getHeader(71));
            Log.v(TAG, "WHO : " + headerSet.getHeader(74));
            Log.v(TAG, "OBJECT_CLASS : " + headerSet.getHeader(79));
            Log.v(TAG, "APPLICATION_PARAMETER : " + headerSet.getHeader(76));
        } catch (IOException e) {
            Log.e(TAG, "dump HeaderSet error " + e);
        }
    }

    private void setDbCounters(ApplicationParameter applicationParameter) {
        applicationParameter.addAPPHeader((byte) 13, (byte) 16, getDatabaseIdentifier());
    }

    private void setFolderVersionCounters(ApplicationParameter applicationParameter) {
        applicationParameter.addAPPHeader((byte) 10, (byte) 16, getPBPrimaryFolderVersion());
        applicationParameter.addAPPHeader((byte) 11, (byte) 16, getPBSecondaryFolderVersion());
    }

    private void setCallversionCounters(ApplicationParameter applicationParameter, AppParamValue appParamValue) {
        applicationParameter.addAPPHeader((byte) 10, (byte) 16, appParamValue.callHistoryVersionCounter);
        applicationParameter.addAPPHeader((byte) 11, (byte) 16, appParamValue.callHistoryVersionCounter);
    }

    private byte[] getDatabaseIdentifier() {
        this.mDatabaseIdentifierHigh = 0L;
        this.mDatabaseIdentifierLow = BluetoothPbapUtils.sDbIdentifier.get();
        if (this.mDatabaseIdentifierLow != -1 && this.mDatabaseIdentifierHigh != -1) {
            ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
            byteBufferAllocate.putLong(this.mDatabaseIdentifierHigh);
            byteBufferAllocate.putLong(this.mDatabaseIdentifierLow);
            return byteBufferAllocate.array();
        }
        return null;
    }

    private byte[] getPBPrimaryFolderVersion() {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
        byteBufferAllocate.putLong(0L);
        Log.d(TAG, "primaryVersionCounter is " + BluetoothPbapUtils.sPrimaryVersionCounter);
        byteBufferAllocate.putLong(BluetoothPbapUtils.sPrimaryVersionCounter);
        return byteBufferAllocate.array();
    }

    private byte[] getPBSecondaryFolderVersion() {
        ByteBuffer byteBufferAllocate = ByteBuffer.allocate(16);
        byteBufferAllocate.putLong(0L);
        Log.d(TAG, "secondaryVersionCounter is " + BluetoothPbapUtils.sSecondaryVersionCounter);
        byteBufferAllocate.putLong(BluetoothPbapUtils.sSecondaryVersionCounter);
        return byteBufferAllocate.array();
    }

    private boolean checkPbapFeatureSupport(long j) {
        Log.d(TAG, "checkPbapFeatureSupport featureBitMask is " + j);
        return (j & ((long) ByteBuffer.wrap(this.mConnAppParamValue.supportedFeature).getInt())) != 0;
    }
}
