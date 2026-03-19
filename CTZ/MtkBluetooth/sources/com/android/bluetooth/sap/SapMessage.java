package com.android.bluetooth.sap;

import android.hardware.radio.V1_0.ISap;
import android.os.RemoteException;
import android.util.Log;
import com.google.protobuf.micro.CodedOutputStreamMicro;
import com.google.protobuf.micro.InvalidProtocolBufferMicroException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.android.btsap.SapApi;

public class SapMessage {
    public static final int CON_STATUS_ERROR_CONNECTION = 1;
    public static final int CON_STATUS_ERROR_MAX_MSG_SIZE_TOO_SMALL = 3;
    public static final int CON_STATUS_ERROR_MAX_MSG_SIZE_UNSUPPORTED = 2;
    public static final int CON_STATUS_OK = 0;
    public static final int CON_STATUS_OK_ONGOING_CALL = 4;
    public static final boolean DEBUG = false;
    public static final int DISC_FORCED = 256;
    public static final int DISC_GRACEFULL = 0;
    public static final int DISC_IMMEDIATE = 1;
    public static final int DISC_RFCOMM = 257;
    public static final int ID_CONNECT_REQ = 0;
    public static final int ID_CONNECT_RESP = 1;
    public static final int ID_DISCONNECT_IND = 4;
    public static final int ID_DISCONNECT_REQ = 2;
    public static final int ID_DISCONNECT_RESP = 3;
    public static final int ID_ERROR_RESP = 18;
    public static final int ID_POWER_SIM_OFF_REQ = 9;
    public static final int ID_POWER_SIM_OFF_RESP = 10;
    public static final int ID_POWER_SIM_ON_REQ = 11;
    public static final int ID_POWER_SIM_ON_RESP = 12;
    public static final int ID_RESET_SIM_REQ = 13;
    public static final int ID_RESET_SIM_RESP = 14;
    public static final int ID_RIL_BASE = 256;
    public static final int ID_RIL_GET_SIM_STATUS_REQ = 512;
    public static final int ID_RIL_SIM_ACCESS_TEST_REQ = 513;
    public static final int ID_RIL_SIM_ACCESS_TEST_RESP = 514;
    public static final int ID_RIL_UNKNOWN = 511;
    public static final int ID_RIL_UNSOL_CONNECTED = 256;
    public static final int ID_RIL_UNSOL_DISCONNECT_IND = 258;
    public static final int ID_SET_TRANSPORT_PROTOCOL_REQ = 19;
    public static final int ID_SET_TRANSPORT_PROTOCOL_RESP = 20;
    public static final int ID_STATUS_IND = 17;
    public static final int ID_TRANSFER_APDU_REQ = 5;
    public static final int ID_TRANSFER_APDU_RESP = 6;
    public static final int ID_TRANSFER_ATR_REQ = 7;
    public static final int ID_TRANSFER_ATR_RESP = 8;
    public static final int ID_TRANSFER_CARD_READER_STATUS_REQ = 15;
    public static final int ID_TRANSFER_CARD_READER_STATUS_RESP = 16;
    public static final int INVALID_VALUE = -1;
    public static final int PARAM_ATR_ID = 6;
    public static final int PARAM_CARD_READER_STATUS_ID = 7;
    public static final int PARAM_CARD_READER_STATUS_LENGTH = 1;
    public static final int PARAM_COMMAND_APDU7816_ID = 16;
    public static final int PARAM_COMMAND_APDU_ID = 4;
    public static final int PARAM_CONNECTION_STATUS_ID = 1;
    public static final int PARAM_CONNECTION_STATUS_LENGTH = 1;
    public static final int PARAM_DISCONNECT_TYPE_ID = 3;
    public static final int PARAM_DISCONNECT_TYPE_LENGTH = 1;
    public static final int PARAM_MAX_MSG_SIZE_ID = 0;
    public static final int PARAM_MAX_MSG_SIZE_LENGTH = 2;
    public static final int PARAM_RESPONSE_APDU_ID = 5;
    public static final int PARAM_RESULT_CODE_ID = 2;
    public static final int PARAM_RESULT_CODE_LENGTH = 1;
    public static final int PARAM_STATUS_CHANGE_ID = 8;
    public static final int PARAM_STATUS_CHANGE_LENGTH = 1;
    public static final int PARAM_TRANSPORT_PROTOCOL_ID = 9;
    public static final int PARAM_TRANSPORT_PROTOCOL_LENGTH = 1;
    static final int RESPONSE_SOLICITED = 0;
    static final int RESPONSE_UNSOLICITED = 1;
    public static final int RESULT_ERROR_CARD_NOT_ACCESSIBLE = 2;
    public static final int RESULT_ERROR_CARD_POWERED_OFF = 3;
    public static final int RESULT_ERROR_CARD_POWERED_ON = 5;
    public static final int RESULT_ERROR_CARD_REMOVED = 4;
    public static final int RESULT_ERROR_DATA_NOT_AVAILABLE = 6;
    public static final int RESULT_ERROR_NOT_SUPPORTED = 7;
    public static final int RESULT_ERROR_NO_REASON = 1;
    public static final int RESULT_OK = 0;
    public static final int STATUS_CARD_INSERTED = 4;
    public static final int STATUS_CARD_NOT_ACCESSIBLE = 2;
    public static final int STATUS_CARD_REMOVED = 3;
    public static final int STATUS_CARD_RESET = 1;
    public static final int STATUS_RECOVERED = 5;
    public static final int STATUS_UNKNOWN_ERROR = 0;
    public static final String TAG = "SapMessage";
    public static final boolean TEST = false;
    public static final int TEST_MODE_DISABLE = 0;
    public static final int TEST_MODE_ENABLE = 1;
    public static final int TRANS_PROTO_T0 = 0;
    public static final int TRANS_PROTO_T1 = 1;
    public static final boolean VERBOSE = false;
    static AtomicInteger sNextSerial = new AtomicInteger(1);
    static Map<Integer, Integer> sOngoingRequests = new Hashtable();
    private byte[] mApdu;
    private byte[] mApdu7816;
    private byte[] mApduResp;
    private byte[] mAtr;
    private int mCardReaderStatus;
    private boolean mClearRilQueue;
    private int mConnectionStatus;
    private int mDisconnectionType;
    private int mMaxMsgSize;
    private int mMsgType;
    private int mResultCode;
    private boolean mSendToRil;
    private int mStatusChange;
    private int mTestMode;
    private int mTransportProtocol;

    public SapMessage(int i) {
        this.mSendToRil = false;
        this.mClearRilQueue = false;
        this.mMsgType = -1;
        this.mMaxMsgSize = -1;
        this.mConnectionStatus = -1;
        this.mResultCode = -1;
        this.mDisconnectionType = -1;
        this.mCardReaderStatus = -1;
        this.mStatusChange = -1;
        this.mTransportProtocol = -1;
        this.mTestMode = -1;
        this.mApdu = null;
        this.mApdu7816 = null;
        this.mApduResp = null;
        this.mAtr = null;
        this.mMsgType = i;
    }

    private static void resetPendingRilMessages() {
        int size = sOngoingRequests.size();
        if (size != 0) {
            Log.w(TAG, "Clearing message queue with size: " + size);
            sOngoingRequests.clear();
        }
    }

    public static int getNumPendingRilMessages() {
        return sOngoingRequests.size();
    }

    public int getMsgType() {
        return this.mMsgType;
    }

    public void setMsgType(int i) {
        this.mMsgType = i;
    }

    public int getMaxMsgSize() {
        return this.mMaxMsgSize;
    }

    public void setMaxMsgSize(int i) {
        this.mMaxMsgSize = i;
    }

    public int getConnectionStatus() {
        return this.mConnectionStatus;
    }

    public void setConnectionStatus(int i) {
        this.mConnectionStatus = i;
    }

    public int getResultCode() {
        return this.mResultCode;
    }

    public void setResultCode(int i) {
        this.mResultCode = i;
    }

    public int getDisconnectionType() {
        return this.mDisconnectionType;
    }

    public void setDisconnectionType(int i) {
        this.mDisconnectionType = i;
    }

    public int getCardReaderStatus() {
        return this.mCardReaderStatus;
    }

    public void setCardReaderStatus(int i) {
        this.mCardReaderStatus = i;
    }

    public int getStatusChange() {
        return this.mStatusChange;
    }

    public void setStatusChange(int i) {
        this.mStatusChange = i;
    }

    public int getTransportProtocol() {
        return this.mTransportProtocol;
    }

    public void setTransportProtocol(int i) {
        this.mTransportProtocol = i;
    }

    public byte[] getApdu() {
        return this.mApdu;
    }

    public void setApdu(byte[] bArr) {
        this.mApdu = bArr;
    }

    public byte[] getApdu7816() {
        return this.mApdu7816;
    }

    public void setApdu7816(byte[] bArr) {
        this.mApdu7816 = bArr;
    }

    public byte[] getApduResp() {
        return this.mApduResp;
    }

    public void setApduResp(byte[] bArr) {
        this.mApduResp = bArr;
    }

    public byte[] getAtr() {
        return this.mAtr;
    }

    public void setAtr(byte[] bArr) {
        this.mAtr = bArr;
    }

    public boolean getSendToRil() {
        return this.mSendToRil;
    }

    public void setSendToRil(boolean z) {
        this.mSendToRil = z;
    }

    public boolean getClearRilQueue() {
        return this.mClearRilQueue;
    }

    public void setClearRilQueue(boolean z) {
        this.mClearRilQueue = z;
    }

    public int getTestMode() {
        return this.mTestMode;
    }

    public void setTestMode(int i) {
        this.mTestMode = i;
    }

    private int getParamCount() {
        int i = this.mMaxMsgSize != -1 ? 1 : 0;
        if (this.mConnectionStatus != -1) {
            i++;
        }
        if (this.mResultCode != -1) {
            i++;
        }
        if (this.mDisconnectionType != -1) {
            i++;
        }
        if (this.mCardReaderStatus != -1) {
            i++;
        }
        if (this.mStatusChange != -1) {
            i++;
        }
        if (this.mTransportProtocol != -1) {
            i++;
        }
        if (this.mApdu != null) {
            i++;
        }
        if (this.mApdu7816 != null) {
            i++;
        }
        if (this.mApduResp != null) {
            i++;
        }
        if (this.mAtr != null) {
            return i + 1;
        }
        return i;
    }

    public static SapMessage readMessage(int i, InputStream inputStream) {
        SapMessage sapMessage = new SapMessage(i);
        try {
            int i2 = inputStream.read();
            skip(inputStream, 2);
            if (i2 > 0) {
                if (!sapMessage.parseParameters(i2, inputStream)) {
                    return null;
                }
            }
            if (i != 0) {
                if (i != 2) {
                    if (i != 5) {
                        if (i == 7 || i == 9 || i == 11 || i == 13 || i == 15) {
                            sapMessage.setSendToRil(true);
                        } else if (i == 19) {
                            if (sapMessage.getTransportProtocol() == -1) {
                                Log.e(TAG, "Missing TransportProtocol parameter in SET_TRANSPORT_PROTOCOL_REQ");
                                return null;
                            }
                            sapMessage.setSendToRil(true);
                        } else {
                            Log.e(TAG, "Unknown request type");
                            return null;
                        }
                    } else {
                        if (sapMessage.getApdu() == null && sapMessage.getApdu7816() == null) {
                            Log.e(TAG, "Missing Apdu parameter in TRANSFER_APDU_REQ");
                            return null;
                        }
                        sapMessage.setSendToRil(true);
                    }
                }
            } else if (sapMessage.getMaxMsgSize() == -1) {
                Log.e(TAG, "Missing MaxMsgSize parameter in CONNECT_REQ");
                return null;
            }
            return sapMessage;
        } catch (IOException e) {
            Log.w(TAG, e);
            return null;
        }
    }

    private static void read(InputStream inputStream, byte[] bArr) throws IOException {
        int length = bArr.length;
        int i = 0;
        while (i < length) {
            int i2 = inputStream.read(bArr, i, length - i);
            if (i2 == -1) {
                throw new IOException("EOS reached while reading a byte array.");
            }
            i += i2;
        }
    }

    private static void skip(InputStream inputStream, int i) throws IOException {
        for (int i2 = 0; i2 < i; i2++) {
            inputStream.read();
        }
    }

    private boolean parseParameters(int i, InputStream inputStream) throws IOException {
        boolean z = true;
        int i2 = 0;
        for (int i3 = 0; i3 < i; i3++) {
            int i4 = inputStream.read();
            inputStream.read();
            int i5 = (inputStream.read() << 8) | inputStream.read();
            int i6 = i5 % 4;
            if (i6 != 0) {
                i2 = 4 - i6;
            }
            if (i4 != 16) {
                switch (i4) {
                    case 0:
                        if (i5 != 2) {
                            Log.e(TAG, "Received PARAM_MAX_MSG_SIZE with wrong length: " + i5 + " skipping this parameter.");
                            skip(inputStream, i5 + i2);
                        } else {
                            this.mMaxMsgSize = inputStream.read();
                            this.mMaxMsgSize = (this.mMaxMsgSize << 8) | inputStream.read();
                            skip(inputStream, 2);
                        }
                        break;
                    case 1:
                        if (i5 != 1) {
                            Log.e(TAG, "Received PARAM_CONNECTION_STATUS with wrong length: " + i5 + " skipping this parameter.");
                            skip(inputStream, i5 + i2);
                        } else {
                            this.mConnectionStatus = inputStream.read();
                            skip(inputStream, 3);
                        }
                        break;
                    case 2:
                        if (i5 != 1) {
                            Log.e(TAG, "Received PARAM_RESULT_CODE with wrong length: " + i5 + " skipping this parameter.");
                            skip(inputStream, i5 + i2);
                        } else {
                            this.mResultCode = inputStream.read();
                            skip(inputStream, 3);
                        }
                        break;
                    case 3:
                        if (i5 != 1) {
                            Log.e(TAG, "Received PARAM_DISCONNECT_TYPE_ID with wrong length: " + i5 + " skipping this parameter.");
                            skip(inputStream, i5 + i2);
                        } else {
                            this.mDisconnectionType = inputStream.read();
                            skip(inputStream, 3);
                        }
                        break;
                    case 4:
                        this.mApdu = new byte[i5];
                        read(inputStream, this.mApdu);
                        skip(inputStream, i2);
                        continue;
                    case 5:
                        this.mApduResp = new byte[i5];
                        read(inputStream, this.mApduResp);
                        skip(inputStream, i2);
                        continue;
                    case 6:
                        this.mAtr = new byte[i5];
                        read(inputStream, this.mAtr);
                        skip(inputStream, i2);
                        continue;
                    case 7:
                        if (i5 != 1) {
                            Log.e(TAG, "Received PARAM_CARD_READER_STATUS with wrong length: " + i5 + " skipping this parameter.");
                            skip(inputStream, i5 + i2);
                        } else {
                            this.mCardReaderStatus = inputStream.read();
                            skip(inputStream, 3);
                        }
                        break;
                    case 8:
                        if (i5 != 1) {
                            Log.e(TAG, "Received PARAM_STATUS_CHANGE with wrong length: " + i5 + " skipping this parameter.");
                            skip(inputStream, i5 + i2);
                        } else {
                            this.mStatusChange = inputStream.read();
                            skip(inputStream, 3);
                        }
                        break;
                    case 9:
                        if (i5 != 1) {
                            Log.e(TAG, "Received PARAM_TRANSPORT_PROTOCOL with wrong length: " + i5 + " skipping this parameter.");
                            skip(inputStream, i5 + i2);
                        } else {
                            this.mTransportProtocol = inputStream.read();
                            skip(inputStream, 3);
                        }
                        break;
                    default:
                        Log.e(TAG, "Received unknown parameter ID: " + i4 + " length: " + i5 + " skipping this parameter.");
                        skip(inputStream, i5 + i2);
                        continue;
                }
                z = false;
            } else {
                this.mApdu7816 = new byte[i5];
                read(inputStream, this.mApdu7816);
                skip(inputStream, i2);
            }
        }
        return z;
    }

    private static void writeParameter(OutputStream outputStream, int i, int i2, int i3) throws IOException {
        outputStream.write(i);
        outputStream.write(0);
        outputStream.write(0);
        outputStream.write(i3);
        switch (i3) {
            case 1:
                outputStream.write(i2 & 255);
                outputStream.write(0);
                outputStream.write(0);
                outputStream.write(0);
                return;
            case 2:
                outputStream.write((i2 >> 8) & 255);
                outputStream.write(i2 & 255);
                outputStream.write(0);
                outputStream.write(0);
                return;
            default:
                throw new IOException("Unable to write value of length: " + i3);
        }
    }

    private static void writeParameter(OutputStream outputStream, int i, byte[] bArr) throws IOException {
        outputStream.write(i);
        outputStream.write(0);
        outputStream.write((bArr.length >> 8) & 255);
        outputStream.write(bArr.length & 255);
        outputStream.write(bArr);
        if (bArr.length % 4 != 0) {
            for (int i2 = 0; i2 < 4 - (bArr.length % 4); i2++) {
                outputStream.write(0);
            }
        }
    }

    public void write(OutputStream outputStream) throws IOException {
        outputStream.write(this.mMsgType);
        outputStream.write(getParamCount());
        outputStream.write(0);
        outputStream.write(0);
        if (this.mConnectionStatus != -1) {
            writeParameter(outputStream, 1, this.mConnectionStatus, 1);
        }
        if (this.mMaxMsgSize != -1) {
            writeParameter(outputStream, 0, this.mMaxMsgSize, 2);
        }
        if (this.mResultCode != -1) {
            writeParameter(outputStream, 2, this.mResultCode, 1);
        }
        if (this.mDisconnectionType != -1) {
            writeParameter(outputStream, 3, this.mDisconnectionType, 1);
        }
        if (this.mCardReaderStatus != -1) {
            writeParameter(outputStream, 7, this.mCardReaderStatus, 1);
        }
        if (this.mStatusChange != -1) {
            writeParameter(outputStream, 8, this.mStatusChange, 1);
        }
        if (this.mTransportProtocol != -1) {
            writeParameter(outputStream, 9, this.mTransportProtocol, 1);
        }
        if (this.mApdu != null) {
            writeParameter(outputStream, 4, this.mApdu);
        }
        if (this.mApdu7816 != null) {
            writeParameter(outputStream, 16, this.mApdu7816);
        }
        if (this.mApduResp != null) {
            writeParameter(outputStream, 5, this.mApduResp);
        }
        if (this.mAtr != null) {
            writeParameter(outputStream, 6, this.mAtr);
        }
    }

    private void writeLength(int i, CodedOutputStreamMicro codedOutputStreamMicro) throws IOException {
        codedOutputStreamMicro.writeRawBytes(new byte[]{0, 0, (byte) ((i >> 8) & 255), (byte) (i & 255)});
    }

    private ArrayList<Byte> primitiveArrayToContainerArrayList(byte[] bArr) {
        ArrayList<Byte> arrayList = new ArrayList<>(bArr.length);
        for (byte b : bArr) {
            arrayList.add(Byte.valueOf(b));
        }
        return arrayList;
    }

    public void send(ISap iSap) throws RemoteException, RuntimeException {
        ArrayList<Byte> arrayListPrimitiveArrayToContainerArrayList;
        int andIncrement = sNextSerial.getAndIncrement();
        Log.e(TAG, "callISapReq: called for mMsgType " + this.mMsgType + " rilSerial " + andIncrement);
        if (this.mClearRilQueue) {
            resetPendingRilMessages();
        }
        sOngoingRequests.put(Integer.valueOf(andIncrement), Integer.valueOf(this.mMsgType));
        int i = this.mMsgType;
        if (i == 0) {
            iSap.connectReq(andIncrement, this.mMaxMsgSize);
            return;
        }
        if (i == 2) {
            iSap.disconnectReq(andIncrement);
            return;
        }
        int i2 = 0;
        if (i == 5) {
            if (this.mApdu != null) {
                arrayListPrimitiveArrayToContainerArrayList = primitiveArrayToContainerArrayList(this.mApdu);
            } else if (this.mApdu7816 != null) {
                arrayListPrimitiveArrayToContainerArrayList = primitiveArrayToContainerArrayList(this.mApdu7816);
                i2 = 1;
            } else {
                Log.e(TAG, "Missing Apdu parameter in TRANSFER_APDU_REQ");
                throw new IllegalArgumentException();
            }
            iSap.apduReq(andIncrement, i2, arrayListPrimitiveArrayToContainerArrayList);
            return;
        }
        if (i == 7) {
            iSap.transferAtrReq(andIncrement);
            return;
        }
        if (i == 9) {
            iSap.powerReq(andIncrement, false);
            return;
        }
        if (i == 11) {
            iSap.powerReq(andIncrement, true);
            return;
        }
        if (i == 13) {
            iSap.resetSimReq(andIncrement);
            return;
        }
        if (i == 15) {
            iSap.transferCardReaderStatusReq(andIncrement);
            return;
        }
        if (i == 19) {
            if (this.mTransportProtocol != 0) {
                if (this.mTransportProtocol != 1) {
                    Log.e(TAG, "Missing or invalid TransportProtocol parameter in SET_TRANSPORT_PROTOCOL_REQ: " + this.mTransportProtocol);
                    throw new IllegalArgumentException();
                }
                i2 = 1;
            }
            iSap.setTransferProtocolReq(andIncrement, i2);
            return;
        }
        Log.e(TAG, "Unknown request type");
        throw new IllegalArgumentException();
    }

    public static SapMessage newInstance(SapApi.MsgHeader msgHeader) throws IOException {
        return new SapMessage(msgHeader);
    }

    private SapMessage(SapApi.MsgHeader msgHeader) throws IOException {
        this.mSendToRil = false;
        this.mClearRilQueue = false;
        this.mMsgType = -1;
        this.mMaxMsgSize = -1;
        this.mConnectionStatus = -1;
        this.mResultCode = -1;
        this.mDisconnectionType = -1;
        this.mCardReaderStatus = -1;
        this.mStatusChange = -1;
        this.mTransportProtocol = -1;
        this.mTestMode = -1;
        this.mApdu = null;
        this.mApdu7816 = null;
        this.mApduResp = null;
        this.mAtr = null;
        try {
            switch (msgHeader.getType()) {
                case 2:
                    createSolicited(msgHeader);
                    return;
                case 3:
                    createUnsolicited(msgHeader);
                    return;
                default:
                    throw new IOException("Wrong msg header received: Type: " + msgHeader.getType());
            }
        } catch (InvalidProtocolBufferMicroException e) {
            Log.w(TAG, "Error occured parsing a RIL message", e);
            throw new IOException("Error occured parsing a RIL message");
        }
    }

    private void createUnsolicited(SapApi.MsgHeader msgHeader) throws IOException {
        int id = msgHeader.getId();
        if (id == 2) {
            SapApi.RIL_SIM_SAP_DISCONNECT_IND from = SapApi.RIL_SIM_SAP_DISCONNECT_IND.parseFrom(msgHeader.getPayload().toByteArray());
            this.mMsgType = ID_RIL_UNSOL_DISCONNECT_IND;
            if (from.hasDisconnectType()) {
                setDisconnectionType(from.getDisconnectType());
                return;
            } else {
                this.mMsgType = 511;
                return;
            }
        }
        if (id == 7) {
            SapApi.RIL_SIM_SAP_STATUS_IND from2 = SapApi.RIL_SIM_SAP_STATUS_IND.parseFrom(msgHeader.getPayload().toByteArray());
            this.mMsgType = 17;
            if (from2.hasStatusChange()) {
                setStatusChange(from2.getStatusChange());
                return;
            } else {
                this.mMsgType = 511;
                return;
            }
        }
        this.mMsgType = 511;
    }

    private void createSolicited(SapApi.MsgHeader msgHeader) throws IOException {
        if (!msgHeader.hasToken()) {
            throw new IOException("Token is missing");
        }
        if (!msgHeader.hasError()) {
            throw new IOException("Error code is missing");
        }
        int token = msgHeader.getToken();
        int error = msgHeader.getError();
        Integer numRemove = sOngoingRequests.remove(Integer.valueOf(token));
        if (numRemove == null) {
            Log.w(TAG, "Solicited response received on a command not initiated - ignoring.");
            return;
        }
        this.mResultCode = mapRilErrorCode(error);
        int iIntValue = numRemove.intValue();
        if (iIntValue == 0) {
            SapApi.RIL_SIM_SAP_CONNECT_RSP from = SapApi.RIL_SIM_SAP_CONNECT_RSP.parseFrom(msgHeader.getPayload().toByteArray());
            this.mMsgType = 1;
            if (from.hasMaxMessageSize()) {
                this.mMaxMsgSize = from.getMaxMessageSize();
            }
            switch (from.getResponse()) {
                case 0:
                    this.mConnectionStatus = 0;
                    break;
                case 1:
                    this.mConnectionStatus = 1;
                    break;
                case 2:
                    this.mConnectionStatus = 2;
                    break;
                case 3:
                    this.mConnectionStatus = 3;
                    break;
                case 4:
                    this.mConnectionStatus = 4;
                    break;
                default:
                    this.mConnectionStatus = 1;
                    break;
            }
            this.mResultCode = -1;
            return;
        }
        if (iIntValue == 2) {
            this.mMsgType = 3;
            this.mResultCode = -1;
            return;
        }
        if (iIntValue == 5) {
            SapApi.RIL_SIM_SAP_APDU_RSP from2 = SapApi.RIL_SIM_SAP_APDU_RSP.parseFrom(msgHeader.getPayload().toByteArray());
            this.mMsgType = 6;
            switch (from2.getResponse()) {
                case 0:
                    this.mResultCode = 0;
                    if (from2.hasApduResponse()) {
                        this.mApduResp = from2.getApduResponse().toByteArray();
                        return;
                    }
                    return;
                case 1:
                    this.mResultCode = 1;
                    return;
                case 2:
                    this.mResultCode = 4;
                    return;
                case 3:
                    this.mResultCode = 3;
                    return;
                case 4:
                    this.mResultCode = 2;
                    return;
                default:
                    this.mResultCode = 1;
                    return;
            }
        }
        if (iIntValue == 7) {
            SapApi.RIL_SIM_SAP_TRANSFER_ATR_RSP from3 = SapApi.RIL_SIM_SAP_TRANSFER_ATR_RSP.parseFrom(msgHeader.getPayload().toByteArray());
            this.mMsgType = 8;
            if (from3.hasAtr()) {
                this.mAtr = from3.getAtr().toByteArray();
            }
            switch (from3.getResponse()) {
                case 0:
                    this.mResultCode = 0;
                    return;
                case 1:
                    this.mResultCode = 1;
                    return;
                case 3:
                    this.mResultCode = 3;
                    return;
                case 4:
                    this.mResultCode = 2;
                    return;
                case 6:
                    this.mResultCode = 6;
                    return;
                case 18:
                    this.mResultCode = 5;
                    return;
                default:
                    this.mResultCode = 1;
                    return;
            }
        }
        if (iIntValue == 9) {
            SapApi.RIL_SIM_SAP_POWER_RSP from4 = SapApi.RIL_SIM_SAP_POWER_RSP.parseFrom(msgHeader.getPayload().toByteArray());
            this.mMsgType = 10;
            int response = from4.getResponse();
            if (response == 0) {
                this.mResultCode = 0;
                return;
            }
            if (response == 2) {
                this.mResultCode = 1;
                return;
            }
            if (response == 11) {
                this.mResultCode = 2;
                return;
            }
            switch (response) {
                case 17:
                    this.mResultCode = 3;
                    return;
                case 18:
                    this.mResultCode = 5;
                    return;
                default:
                    this.mResultCode = 1;
                    return;
            }
        }
        if (iIntValue == 11) {
            SapApi.RIL_SIM_SAP_POWER_RSP from5 = SapApi.RIL_SIM_SAP_POWER_RSP.parseFrom(msgHeader.getPayload().toByteArray());
            this.mMsgType = 12;
            int response2 = from5.getResponse();
            if (response2 == 0) {
                this.mResultCode = 0;
                return;
            }
            if (response2 == 2) {
                this.mResultCode = 1;
                return;
            }
            if (response2 == 11) {
                this.mResultCode = 2;
                return;
            }
            switch (response2) {
                case 17:
                    this.mResultCode = 3;
                    return;
                case 18:
                    this.mResultCode = 5;
                    return;
                default:
                    this.mResultCode = 1;
                    return;
            }
        }
        if (iIntValue == 13) {
            SapApi.RIL_SIM_SAP_RESET_SIM_RSP from6 = SapApi.RIL_SIM_SAP_RESET_SIM_RSP.parseFrom(msgHeader.getPayload().toByteArray());
            this.mMsgType = 14;
            int response3 = from6.getResponse();
            if (response3 == 0) {
                this.mResultCode = 0;
                return;
            }
            if (response3 == 2) {
                this.mResultCode = 1;
                return;
            }
            if (response3 == 11) {
                this.mResultCode = 2;
                return;
            } else if (response3 == 17) {
                this.mResultCode = 3;
                return;
            } else {
                this.mResultCode = 1;
                return;
            }
        }
        if (iIntValue == 15) {
            SapApi.RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP from7 = SapApi.RIL_SIM_SAP_TRANSFER_CARD_READER_STATUS_RSP.parseFrom(msgHeader.getPayload().toByteArray());
            this.mMsgType = 16;
            int response4 = from7.getResponse();
            if (response4 == 0) {
                this.mResultCode = 0;
                if (from7.hasCardReaderStatus()) {
                    this.mCardReaderStatus = from7.getCardReaderStatus();
                    return;
                } else {
                    this.mResultCode = 6;
                    return;
                }
            }
            if (response4 == 2) {
                this.mResultCode = 1;
                return;
            } else if (response4 == 6) {
                this.mResultCode = 6;
                return;
            } else {
                this.mResultCode = 1;
                return;
            }
        }
        if (iIntValue != 19) {
            if (iIntValue == 513) {
                this.mMsgType = ID_RIL_SIM_ACCESS_TEST_RESP;
                return;
            }
            Log.e(TAG, "Unknown request type: " + numRemove);
            return;
        }
        SapApi.RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP from8 = SapApi.RIL_SIM_SAP_SET_TRANSFER_PROTOCOL_RSP.parseFrom(msgHeader.getPayload().toByteArray());
        this.mMsgType = 20;
        int response5 = from8.getResponse();
        if (response5 == 0) {
            this.mResultCode = 0;
            return;
        }
        if (response5 == 2) {
            this.mResultCode = 7;
            return;
        }
        if (response5 == 11) {
            this.mResultCode = 2;
            return;
        }
        switch (response5) {
            case 16:
                this.mResultCode = 4;
                return;
            case 17:
                this.mResultCode = 3;
                return;
            default:
                this.mResultCode = 7;
                return;
        }
    }

    private static int mapRilErrorCode(int i) {
        switch (i) {
        }
        return 1;
    }

    public static String getMsgTypeName(int i) {
        return null;
    }
}
