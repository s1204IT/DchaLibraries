package com.android.bluetooth.mapclient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import javax.obex.ClientSession;

final class RequestGetFolderListing extends Request {
    private static final String TYPE = "x-obex/folder-listing";
    private FolderListing mResponse = null;

    RequestGetFolderListing(int i, int i2) {
        if (i < 0 || i > 65535) {
            throw new IllegalArgumentException("maxListCount should be [0..65535]");
        }
        if (i2 < 0 || i2 > 65535) {
            throw new IllegalArgumentException("listStartOffset should be [0..65535]");
        }
        this.mHeaderSet.setHeader(66, TYPE);
        ObexAppParameters obexAppParameters = new ObexAppParameters();
        if (i >= 0) {
            obexAppParameters.add((byte) 1, (short) i);
        }
        if (i2 > 0) {
            obexAppParameters.add((byte) 2, (short) i2);
        }
        obexAppParameters.addToHeaderSet(this.mHeaderSet);
    }

    @Override
    protected void readResponse(InputStream inputStream) {
        this.mResponse = new FolderListing(inputStream);
    }

    public ArrayList<String> getList() {
        if (this.mResponse == null) {
            return null;
        }
        return this.mResponse.getList();
    }

    @Override
    public void execute(ClientSession clientSession) throws IOException {
        executeGet(clientSession);
    }
}
