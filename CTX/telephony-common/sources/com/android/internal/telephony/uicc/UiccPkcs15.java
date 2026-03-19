package com.android.internal.telephony.uicc;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import com.android.internal.telephony.uicc.UiccCarrierPrivilegeRules;
import com.google.android.mms.pdu.PduHeaders;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class UiccPkcs15 extends Handler {
    private static final String CARRIER_RULE_AID = "FFFFFFFFFFFF";
    private static final boolean DBG = true;
    private static final int EVENT_CLOSE_LOGICAL_CHANNEL_DONE = 7;
    private static final int EVENT_LOAD_ACCF_DONE = 6;
    private static final int EVENT_LOAD_ACMF_DONE = 4;
    private static final int EVENT_LOAD_ACRF_DONE = 5;
    private static final int EVENT_LOAD_DODF_DONE = 3;
    private static final int EVENT_LOAD_ODF_DONE = 2;
    private static final int EVENT_SELECT_PKCS15_DONE = 1;
    private static final String ID_ACRF = "4300";
    private static final String LOG_TAG = "UiccPkcs15";
    private static final String TAG_ASN_OCTET_STRING = "04";
    private static final String TAG_ASN_SEQUENCE = "30";
    private static final String TAG_TARGET_AID = "A0";
    private FileHandler mFh;
    private Message mLoadedCallback;
    private Pkcs15Selector mPkcs15Selector;
    private UiccProfile mUiccProfile;
    private int mChannelId = -1;
    private List<String> mRules = new ArrayList();

    private class FileHandler extends Handler {
        protected static final int EVENT_READ_BINARY_DONE = 102;
        protected static final int EVENT_SELECT_FILE_DONE = 101;
        private Message mCallback;
        private String mFileId;
        private final String mPkcs15Path;

        public FileHandler(String str) {
            UiccPkcs15.log("Creating FileHandler, pkcs15Path: " + str);
            this.mPkcs15Path = str;
        }

        public boolean loadFile(String str, Message message) {
            UiccPkcs15.log("loadFile: " + str);
            if (str == null || message == null) {
                return false;
            }
            this.mFileId = str;
            this.mCallback = message;
            selectFile();
            return true;
        }

        private void selectFile() {
            if (UiccPkcs15.this.mChannelId < 0) {
                UiccPkcs15.log("EF based");
            } else {
                UiccPkcs15.this.mUiccProfile.iccTransmitApduLogicalChannel(UiccPkcs15.this.mChannelId, 0, PduHeaders.MM_FLAGS, 0, 4, 2, this.mFileId, obtainMessage(101));
            }
        }

        private void readBinary() {
            if (UiccPkcs15.this.mChannelId < 0) {
                UiccPkcs15.log("EF based");
            } else {
                UiccPkcs15.this.mUiccProfile.iccTransmitApduLogicalChannel(UiccPkcs15.this.mChannelId, 0, PduHeaders.ADDITIONAL_HEADERS, 0, 0, 0, "", obtainMessage(102));
            }
        }

        @Override
        public void handleMessage(Message message) {
            UiccPkcs15.log("handleMessage: " + message.what);
            AsyncResult asyncResult = (AsyncResult) message.obj;
            IccException iccException = null;
            if (asyncResult.exception != null || asyncResult.result == null) {
                UiccPkcs15.log("Error: " + asyncResult.exception);
                AsyncResult.forMessage(this.mCallback, (Object) null, asyncResult.exception);
                this.mCallback.sendToTarget();
                return;
            }
            switch (message.what) {
                case 101:
                    readBinary();
                    break;
                case 102:
                    IccIoResult iccIoResult = (IccIoResult) asyncResult.result;
                    String upperCase = IccUtils.bytesToHexString(iccIoResult.payload).toUpperCase(Locale.US);
                    UiccPkcs15.log("IccIoResult: " + iccIoResult + " payload: " + upperCase);
                    Message message2 = this.mCallback;
                    if (upperCase == null) {
                        iccException = new IccException("Error: null response for " + this.mFileId);
                    }
                    AsyncResult.forMessage(message2, upperCase, iccException);
                    this.mCallback.sendToTarget();
                    break;
                default:
                    UiccPkcs15.log("Unknown event" + message.what);
                    break;
            }
        }
    }

    private class Pkcs15Selector extends Handler {
        private static final int EVENT_OPEN_LOGICAL_CHANNEL_DONE = 201;
        private static final String PKCS15_AID = "A000000063504B43532D3135";
        private Message mCallback;

        public Pkcs15Selector(Message message) {
            this.mCallback = message;
            UiccPkcs15.this.mUiccProfile.iccOpenLogicalChannel(PKCS15_AID, 4, obtainMessage(EVENT_OPEN_LOGICAL_CHANNEL_DONE));
        }

        @Override
        public void handleMessage(Message message) {
            UiccPkcs15.log("handleMessage: " + message.what);
            if (message.what == EVENT_OPEN_LOGICAL_CHANNEL_DONE) {
                AsyncResult asyncResult = (AsyncResult) message.obj;
                if (asyncResult.exception == null && asyncResult.result != null) {
                    UiccPkcs15.this.mChannelId = ((int[]) asyncResult.result)[0];
                    UiccPkcs15.log("mChannelId: " + UiccPkcs15.this.mChannelId);
                    AsyncResult.forMessage(this.mCallback, (Object) null, (Throwable) null);
                } else {
                    UiccPkcs15.log("error: " + asyncResult.exception);
                    AsyncResult.forMessage(this.mCallback, (Object) null, asyncResult.exception);
                }
                this.mCallback.sendToTarget();
                return;
            }
            UiccPkcs15.log("Unknown event" + message.what);
        }
    }

    public UiccPkcs15(UiccProfile uiccProfile, Message message) {
        log("Creating UiccPkcs15");
        this.mUiccProfile = uiccProfile;
        this.mLoadedCallback = message;
        this.mPkcs15Selector = new Pkcs15Selector(obtainMessage(1));
    }

    @Override
    public void handleMessage(Message message) {
        log("handleMessage: " + message.what);
        AsyncResult asyncResult = (AsyncResult) message.obj;
        int i = message.what;
        if (i == 1) {
            if (asyncResult.exception == null) {
                this.mFh = new FileHandler((String) asyncResult.result);
                if (!this.mFh.loadFile(ID_ACRF, obtainMessage(5))) {
                    cleanUp();
                    return;
                }
                return;
            }
            log("select pkcs15 failed: " + asyncResult.exception);
            this.mLoadedCallback.sendToTarget();
        }
        switch (i) {
            case 5:
                if (asyncResult.exception == null && asyncResult.result != null) {
                    if (!this.mFh.loadFile(parseAcrf((String) asyncResult.result), obtainMessage(6))) {
                        cleanUp();
                    }
                } else {
                    cleanUp();
                }
                break;
            case 6:
                if (asyncResult.exception == null && asyncResult.result != null) {
                    parseAccf((String) asyncResult.result);
                }
                cleanUp();
                break;
            case 7:
                break;
            default:
                Rlog.e(LOG_TAG, "Unknown event " + message.what);
                break;
        }
    }

    private void cleanUp() {
        log("cleanUp");
        if (this.mChannelId >= 0) {
            this.mUiccProfile.iccCloseLogicalChannel(this.mChannelId, obtainMessage(7));
            this.mChannelId = -1;
        }
        this.mLoadedCallback.sendToTarget();
    }

    private String parseAcrf(String str) {
        String value = null;
        while (!str.isEmpty()) {
            UiccCarrierPrivilegeRules.TLV tlv = new UiccCarrierPrivilegeRules.TLV(TAG_ASN_SEQUENCE);
            try {
                str = tlv.parse(str, false);
                String value2 = tlv.getValue();
                if (value2.startsWith(TAG_TARGET_AID)) {
                    UiccCarrierPrivilegeRules.TLV tlv2 = new UiccCarrierPrivilegeRules.TLV(TAG_TARGET_AID);
                    UiccCarrierPrivilegeRules.TLV tlv3 = new UiccCarrierPrivilegeRules.TLV(TAG_ASN_OCTET_STRING);
                    UiccCarrierPrivilegeRules.TLV tlv4 = new UiccCarrierPrivilegeRules.TLV(TAG_ASN_SEQUENCE);
                    UiccCarrierPrivilegeRules.TLV tlv5 = new UiccCarrierPrivilegeRules.TLV(TAG_ASN_OCTET_STRING);
                    String str2 = tlv2.parse(value2, false);
                    tlv3.parse(tlv2.getValue(), true);
                    if (CARRIER_RULE_AID.equals(tlv3.getValue())) {
                        tlv4.parse(str2, true);
                        tlv5.parse(tlv4.getValue(), true);
                        value = tlv5.getValue();
                    }
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                log("Error: " + e);
            }
        }
        return value;
    }

    private void parseAccf(String str) {
        while (!str.isEmpty()) {
            UiccCarrierPrivilegeRules.TLV tlv = new UiccCarrierPrivilegeRules.TLV(TAG_ASN_SEQUENCE);
            UiccCarrierPrivilegeRules.TLV tlv2 = new UiccCarrierPrivilegeRules.TLV(TAG_ASN_OCTET_STRING);
            try {
                str = tlv.parse(str, false);
                tlv2.parse(tlv.getValue(), true);
                if (!tlv2.getValue().isEmpty()) {
                    this.mRules.add(tlv2.getValue());
                }
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                log("Error: " + e);
                return;
            }
        }
    }

    public List<String> getRules() {
        return this.mRules;
    }

    private static void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        if (this.mRules != null) {
            printWriter.println(" mRules:");
            Iterator<String> it = this.mRules.iterator();
            while (it.hasNext()) {
                printWriter.println("  " + it.next());
            }
        }
    }
}
