package com.android.ims.internal;

public class ImsStreamMediaSession {
    private static final String TAG = "ImsStreamMediaSession";
    private Listener mListener;

    public static class Listener {
    }

    ImsStreamMediaSession(IImsStreamMediaSession iImsStreamMediaSession) {
    }

    ImsStreamMediaSession(IImsStreamMediaSession iImsStreamMediaSession, Listener listener) {
        this(iImsStreamMediaSession);
        setListener(listener);
    }

    public void setListener(Listener listener) {
        this.mListener = listener;
    }
}
