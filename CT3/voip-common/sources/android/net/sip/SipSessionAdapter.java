package android.net.sip;

import android.net.sip.ISipSessionListener;

public class SipSessionAdapter extends ISipSessionListener.Stub {
    @Override
    public void onCalling(ISipSession session) {
    }

    @Override
    public void onRinging(ISipSession session, SipProfile caller, String sessionDescription) {
    }

    @Override
    public void onRingingBack(ISipSession session) {
    }

    @Override
    public void onCallEstablished(ISipSession session, String sessionDescription) {
    }

    @Override
    public void onCallEnded(ISipSession session) {
    }

    @Override
    public void onCallBusy(ISipSession session) {
    }

    @Override
    public void onCallTransferring(ISipSession session, String sessionDescription) {
    }

    @Override
    public void onCallChangeFailed(ISipSession session, int errorCode, String message) {
    }

    @Override
    public void onError(ISipSession session, int errorCode, String message) {
    }

    public void onRegistering(ISipSession session) {
    }

    public void onRegistrationDone(ISipSession session, int duration) {
    }

    public void onRegistrationFailed(ISipSession session, int errorCode, String message) {
    }

    public void onRegistrationTimeout(ISipSession session) {
    }
}
