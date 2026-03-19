package com.android.server.wifi.anqp;

import com.android.server.wifi.anqp.Constants;

public abstract class ANQPElement {
    private final Constants.ANQPElementType mID;

    protected ANQPElement(Constants.ANQPElementType id) {
        this.mID = id;
    }

    public Constants.ANQPElementType getID() {
        return this.mID;
    }
}
