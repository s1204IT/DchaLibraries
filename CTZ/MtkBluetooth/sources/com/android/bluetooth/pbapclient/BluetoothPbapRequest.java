package com.android.bluetooth.pbapclient;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import javax.obex.ClientOperation;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;

abstract class BluetoothPbapRequest {
    protected static final byte OAP_TAGID_FILTER = 6;
    protected static final byte OAP_TAGID_FORMAT = 7;
    protected static final byte OAP_TAGID_LIST_START_OFFSET = 5;
    protected static final byte OAP_TAGID_MAX_LIST_COUNT = 4;
    protected static final byte OAP_TAGID_NEW_MISSED_CALLS = 9;
    protected static final byte OAP_TAGID_ORDER = 1;
    protected static final byte OAP_TAGID_PBAP_SUPPORTED_FEATURES = 16;
    protected static final byte OAP_TAGID_PHONEBOOK_SIZE = 8;
    protected static final byte OAP_TAGID_SEARCH_ATTRIBUTE = 3;
    protected static final byte OAP_TAGID_SEARCH_VALUE = 2;
    private static final String TAG = "BluetoothPbapRequest";
    protected int mResponseCode;
    private boolean mAborted = false;
    private ClientOperation mOp = null;
    protected HeaderSet mHeaderSet = new HeaderSet();

    BluetoothPbapRequest() {
    }

    public final boolean isSuccess() {
        return this.mResponseCode == 160;
    }

    public void execute(ClientSession clientSession) throws IOException {
        Log.v(TAG, "execute");
        if (this.mAborted) {
            this.mResponseCode = 208;
            return;
        }
        try {
            this.mOp = clientSession.get(this.mHeaderSet);
            this.mOp.setGetFinalFlag(true);
            this.mOp.continueOperation(true, false);
            readResponseHeaders(this.mOp.getReceivedHeader());
            InputStream inputStreamOpenInputStream = this.mOp.openInputStream();
            readResponse(inputStreamOpenInputStream);
            inputStreamOpenInputStream.close();
            this.mOp.close();
            this.mResponseCode = this.mOp.getResponseCode();
            Log.d(TAG, "mResponseCode=" + this.mResponseCode);
            checkResponseCode(this.mResponseCode);
        } catch (IOException e) {
            Log.e(TAG, "IOException occured when processing request", e);
            this.mResponseCode = 208;
            throw e;
        }
    }

    public void abort() {
        this.mAborted = true;
        if (this.mOp != null) {
            try {
                this.mOp.abort();
            } catch (IOException e) {
                Log.e(TAG, "Exception occured when trying to abort", e);
            }
        }
    }

    protected void readResponse(InputStream inputStream) throws IOException {
        Log.v(TAG, "readResponse");
    }

    protected void readResponseHeaders(HeaderSet headerSet) {
        Log.v(TAG, "readResponseHeaders");
    }

    protected void checkResponseCode(int i) throws IOException {
        Log.v(TAG, "checkResponseCode");
    }
}
