package com.android.bluetooth.mapclient;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.obex.ClientOperation;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;

abstract class Request {
    protected static final byte ATTACHMENT_OFF = 0;
    protected static final byte ATTACHMENT_ON = 1;
    protected static final byte CHARSET_NATIVE = 0;
    protected static final byte CHARSET_UTF8 = 1;
    protected static final byte[] FILLER_BYTE = {48};
    protected static final byte NOTIFICATION_OFF = 0;
    protected static final byte NOTIFICATION_ON = 1;
    protected static final byte OAP_TAGID_ATTACHMENT = 10;
    protected static final byte OAP_TAGID_CHARSET = 20;
    protected static final byte OAP_TAGID_FILTER_MESSAGE_TYPE = 3;
    protected static final byte OAP_TAGID_FILTER_ORIGINATOR = 8;
    protected static final byte OAP_TAGID_FILTER_PERIOD_BEGIN = 4;
    protected static final byte OAP_TAGID_FILTER_PERIOD_END = 5;
    protected static final byte OAP_TAGID_FILTER_PRIORITY = 9;
    protected static final byte OAP_TAGID_FILTER_READ_STATUS = 6;
    protected static final byte OAP_TAGID_FILTER_RECIPIENT = 7;
    protected static final byte OAP_TAGID_FOLDER_LISTING_SIZE = 17;
    protected static final byte OAP_TAGID_MAS_INSTANCE_ID = 15;
    protected static final byte OAP_TAGID_MAX_LIST_COUNT = 1;
    protected static final byte OAP_TAGID_MESSAGES_LISTING_SIZE = 18;
    protected static final byte OAP_TAGID_MSE_TIME = 25;
    protected static final byte OAP_TAGID_NEW_MESSAGE = 13;
    protected static final byte OAP_TAGID_NOTIFICATION_STATUS = 14;
    protected static final byte OAP_TAGID_PARAMETER_MASK = 16;
    protected static final byte OAP_TAGID_RETRY = 12;
    protected static final byte OAP_TAGID_START_OFFSET = 2;
    protected static final byte OAP_TAGID_STATUS_INDICATOR = 23;
    protected static final byte OAP_TAGID_STATUS_VALUE = 24;
    protected static final byte OAP_TAGID_SUBJECT_LENGTH = 19;
    protected static final byte OAP_TAGID_TRANSPARENT = 11;
    protected static final byte RETRY_OFF = 0;
    protected static final byte RETRY_ON = 1;
    protected static final byte STATUS_INDICATOR_DELETED = 1;
    protected static final byte STATUS_INDICATOR_READ = 0;
    protected static final byte STATUS_NO = 0;
    protected static final byte STATUS_YES = 1;
    protected static final byte TRANSPARENT_OFF = 0;
    protected static final byte TRANSPARENT_ON = 1;
    protected HeaderSet mHeaderSet = new HeaderSet();
    protected int mResponseCode;

    public abstract void execute(ClientSession clientSession) throws IOException;

    Request() {
    }

    protected void executeGet(ClientSession clientSession) throws IOException {
        try {
            ClientOperation clientOperation = clientSession.get(this.mHeaderSet);
            clientOperation.setGetFinalFlag(true);
            clientOperation.continueOperation(true, false);
            readResponseHeaders(clientOperation.getReceivedHeader());
            InputStream inputStreamOpenInputStream = clientOperation.openInputStream();
            readResponse(inputStreamOpenInputStream);
            inputStreamOpenInputStream.close();
            clientOperation.close();
            this.mResponseCode = clientOperation.getResponseCode();
        } catch (IOException e) {
            this.mResponseCode = 208;
            throw e;
        }
    }

    protected void executePut(ClientSession clientSession, byte[] bArr) throws IOException {
        this.mHeaderSet.setHeader(195, Long.valueOf(bArr.length));
        try {
            Operation operationPut = clientSession.put(this.mHeaderSet);
            DataOutputStream dataOutputStreamOpenDataOutputStream = operationPut.openDataOutputStream();
            dataOutputStreamOpenDataOutputStream.write(bArr);
            dataOutputStreamOpenDataOutputStream.close();
            readResponseHeaders(operationPut.getReceivedHeader());
            operationPut.close();
            this.mResponseCode = operationPut.getResponseCode();
        } catch (IOException e) {
            this.mResponseCode = 208;
            throw e;
        }
    }

    public final boolean isSuccess() {
        return this.mResponseCode == 160;
    }

    protected void readResponse(InputStream inputStream) throws IOException {
    }

    protected void readResponseHeaders(HeaderSet headerSet) {
    }
}
