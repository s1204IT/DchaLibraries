package com.android.bluetooth.mapclient;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;

final class RequestGetMessagesListing extends Request {
    private static final String TYPE = "x-bt/MAP-msg-listing";
    private MessagesListing mResponse = null;
    private boolean mNewMessage = false;
    private Date mServerTime = null;

    RequestGetMessagesListing(String str, int i, MessagesFilter messagesFilter, int i2, int i3, int i4) {
        if (i2 < 0 || i2 > 255) {
            throw new IllegalArgumentException("subjectLength should be [0..255]");
        }
        if (i3 < 0 || i3 > 65535) {
            throw new IllegalArgumentException("maxListCount should be [0..65535]");
        }
        if (i4 < 0 || i4 > 65535) {
            throw new IllegalArgumentException("listStartOffset should be [0..65535]");
        }
        this.mHeaderSet.setHeader(66, TYPE);
        if (str == null) {
            this.mHeaderSet.setHeader(1, "");
        } else {
            this.mHeaderSet.setHeader(1, str);
        }
        ObexAppParameters obexAppParameters = new ObexAppParameters();
        if (messagesFilter != null) {
            if (messagesFilter.messageType != 0) {
                obexAppParameters.add((byte) 3, messagesFilter.messageType);
            }
            if (messagesFilter.periodBegin != null) {
                obexAppParameters.add((byte) 4, messagesFilter.periodBegin);
            }
            if (messagesFilter.periodEnd != null) {
                obexAppParameters.add((byte) 5, messagesFilter.periodEnd);
            }
            if (messagesFilter.readStatus != 0) {
                obexAppParameters.add((byte) 6, messagesFilter.readStatus);
            }
            if (messagesFilter.recipient != null) {
                obexAppParameters.add((byte) 7, messagesFilter.recipient);
            }
            if (messagesFilter.originator != null) {
                obexAppParameters.add((byte) 8, messagesFilter.originator);
            }
            if (messagesFilter.priority != 0) {
                obexAppParameters.add((byte) 9, messagesFilter.priority);
            }
        }
        if (i2 != 0) {
            obexAppParameters.add((byte) 19, (byte) i2);
        }
        if (i > 0) {
            obexAppParameters.add((byte) 16, i);
        }
        if (i3 >= 0) {
            obexAppParameters.add((byte) 1, (short) i3);
        }
        if (i4 != 0) {
            obexAppParameters.add((byte) 2, (short) i4);
        }
        obexAppParameters.addToHeaderSet(this.mHeaderSet);
    }

    @Override
    protected void readResponse(InputStream inputStream) {
        this.mResponse = new MessagesListing(inputStream);
    }

    @Override
    protected void readResponseHeaders(HeaderSet headerSet) {
        String string;
        ObexAppParameters obexAppParametersFromHeaderSet = ObexAppParameters.fromHeaderSet(headerSet);
        this.mNewMessage = (obexAppParametersFromHeaderSet.getByte((byte) 13) & 1) == 1;
        if (obexAppParametersFromHeaderSet.exists((byte) 25) && (string = obexAppParametersFromHeaderSet.getString((byte) 25)) != null) {
            this.mServerTime = new ObexTime(string).getTime();
        }
    }

    public ArrayList<Message> getList() {
        if (this.mResponse == null) {
            return null;
        }
        return this.mResponse.getList();
    }

    public boolean getNewMessageStatus() {
        return this.mNewMessage;
    }

    public Date getMseTime() {
        return this.mServerTime;
    }

    @Override
    public void execute(ClientSession clientSession) throws IOException {
        executeGet(clientSession);
    }
}
