package android.net.sip;

import android.net.sip.ISipSessionListener;

public class SipSessionAdapter extends ISipSessionListener.Stub {
    @Override
    public void onCalling(ISipSession iSipSession) {
    }

    @Override
    public void onRinging(ISipSession iSipSession, SipProfile sipProfile, String str) {
    }

    @Override
    public void onRingingBack(ISipSession iSipSession) {
    }

    @Override
    public void onCallEstablished(ISipSession iSipSession, String str) {
    }

    @Override
    public void onCallEnded(ISipSession iSipSession) {
    }

    @Override
    public void onCallBusy(ISipSession iSipSession) {
    }

    @Override
    public void onCallTransferring(ISipSession iSipSession, String str) {
    }

    @Override
    public void onCallChangeFailed(ISipSession iSipSession, int i, String str) {
    }

    @Override
    public void onError(ISipSession iSipSession, int i, String str) {
    }

    public void onRegistering(ISipSession iSipSession) {
    }

    public void onRegistrationDone(ISipSession iSipSession, int i) {
    }

    public void onRegistrationFailed(ISipSession iSipSession, int i, String str) {
    }

    public void onRegistrationTimeout(ISipSession iSipSession) {
    }
}
