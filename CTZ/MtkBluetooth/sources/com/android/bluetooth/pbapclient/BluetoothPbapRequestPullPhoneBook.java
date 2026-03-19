package com.android.bluetooth.pbapclient;

import android.accounts.Account;
import android.util.Log;
import com.android.vcard.VCardEntry;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.obex.HeaderSet;

final class BluetoothPbapRequestPullPhoneBook extends BluetoothPbapRequest {
    private static final String TAG = "BluetoothPbapRequestPullPhoneBook";
    private static final String TYPE = "x-bt/phonebook";
    private static final boolean VDBG = false;
    private Account mAccount;
    private final byte mFormat;
    private int mNewMissedCalls = -1;
    private BluetoothPbapVcardList mResponse;

    BluetoothPbapRequestPullPhoneBook(String str, Account account, long j, byte b, int i, int i2) {
        this.mAccount = account;
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("maxListCount should be [0..65535]");
        }
        if (i2 < 0 || i2 > 65535) {
            throw new IllegalArgumentException("listStartOffset should be [0..65535]");
        }
        this.mHeaderSet.setHeader(1, str);
        this.mHeaderSet.setHeader(66, TYPE);
        ObexAppParameters obexAppParameters = new ObexAppParameters();
        if (b != 0 && b != 1) {
            b = 0;
        }
        if (j != 0) {
            obexAppParameters.add((byte) 6, j);
        }
        obexAppParameters.add((byte) 7, b);
        if (i > 0) {
            obexAppParameters.add((byte) 4, (short) i);
        } else {
            obexAppParameters.add((byte) 4, (short) -1);
        }
        if (i2 > 0) {
            obexAppParameters.add((byte) 5, (short) i2);
        }
        obexAppParameters.addToHeaderSet(this.mHeaderSet);
        this.mFormat = b;
    }

    @Override
    protected void readResponse(InputStream inputStream) throws IOException {
        Log.v(TAG, "readResponse");
        this.mResponse = new BluetoothPbapVcardList(this.mAccount, inputStream, this.mFormat);
    }

    @Override
    protected void readResponseHeaders(HeaderSet headerSet) {
        Log.v(TAG, "readResponseHeaders");
        ObexAppParameters obexAppParametersFromHeaderSet = ObexAppParameters.fromHeaderSet(headerSet);
        if (obexAppParametersFromHeaderSet.exists((byte) 9)) {
            this.mNewMissedCalls = obexAppParametersFromHeaderSet.getByte((byte) 9);
        }
    }

    public ArrayList<VCardEntry> getList() {
        return this.mResponse.getList();
    }

    public int getNewMissedCalls() {
        return this.mNewMissedCalls;
    }
}
