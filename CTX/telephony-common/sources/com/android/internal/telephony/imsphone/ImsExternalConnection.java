package com.android.internal.telephony.imsphone;

import android.R;
import android.content.Context;
import android.net.Uri;
import android.telephony.PhoneNumberUtils;
import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.UUSInfo;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ImsExternalConnection extends Connection {
    private static final String CONFERENCE_PREFIX = "conf";
    private ImsExternalCall mCall;
    private int mCallId;
    private final Context mContext;
    private boolean mIsPullable;
    private final Set<Listener> mListeners;
    private Uri mOriginalAddress;

    public interface Listener {
        void onPullExternalCall(ImsExternalConnection imsExternalConnection);
    }

    protected ImsExternalConnection(Phone phone, int i, Uri uri, boolean z) {
        super(phone.getPhoneType());
        this.mListeners = Collections.newSetFromMap(new ConcurrentHashMap(8, 0.9f, 1));
        this.mContext = phone.getContext();
        this.mCall = new ImsExternalCall(phone, this);
        this.mCallId = i;
        setExternalConnectionAddress(uri);
        this.mNumberPresentation = 1;
        this.mIsPullable = z;
        rebuildCapabilities();
        setActive();
    }

    public int getCallId() {
        return this.mCallId;
    }

    @Override
    public Call getCall() {
        return this.mCall;
    }

    @Override
    public long getDisconnectTime() {
        return 0L;
    }

    @Override
    public long getHoldDurationMillis() {
        return 0L;
    }

    @Override
    public String getVendorDisconnectCause() {
        return null;
    }

    @Override
    public void hangup() throws CallStateException {
    }

    @Override
    public void deflect(String str) throws CallStateException {
        throw new CallStateException("Deflect is not supported for external calls");
    }

    @Override
    public void separate() throws CallStateException {
    }

    @Override
    public void proceedAfterWaitChar() {
    }

    @Override
    public void proceedAfterWildChar(String str) {
    }

    @Override
    public void cancelPostDial() {
    }

    @Override
    public int getNumberPresentation() {
        return this.mNumberPresentation;
    }

    @Override
    public UUSInfo getUUSInfo() {
        return null;
    }

    @Override
    public int getPreciseDisconnectCause() {
        return 0;
    }

    @Override
    public boolean isMultiparty() {
        return false;
    }

    @Override
    public void pullExternalCall() {
        Iterator<Listener> it = this.mListeners.iterator();
        while (it.hasNext()) {
            it.next().onPullExternalCall(this);
        }
    }

    public void setActive() {
        if (this.mCall == null) {
            return;
        }
        this.mCall.setActive();
    }

    public void setTerminated() {
        if (this.mCall == null) {
            return;
        }
        this.mCall.setTerminated();
    }

    public void setIsPullable(boolean z) {
        this.mIsPullable = z;
        rebuildCapabilities();
    }

    public void setExternalConnectionAddress(Uri uri) {
        this.mOriginalAddress = uri;
        if ("sip".equals(uri.getScheme()) && uri.getSchemeSpecificPart().startsWith(CONFERENCE_PREFIX)) {
            this.mCnapName = this.mContext.getString(R.string.accessibility_system_action_back_label);
            this.mCnapNamePresentation = 1;
            this.mAddress = "";
            this.mNumberPresentation = 2;
            return;
        }
        this.mAddress = PhoneNumberUtils.convertSipUriToTelUri(uri).getSchemeSpecificPart();
    }

    public void addListener(Listener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(Listener listener) {
        this.mListeners.remove(listener);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("[ImsExternalConnection dialogCallId:");
        sb.append(this.mCallId);
        sb.append(" state:");
        if (this.mCall.getState() == Call.State.ACTIVE) {
            sb.append("Active");
        } else if (this.mCall.getState() == Call.State.DISCONNECTED) {
            sb.append("Disconnected");
        }
        sb.append("]");
        return sb.toString();
    }

    private void rebuildCapabilities() {
        int i;
        if (this.mIsPullable) {
            i = 48;
        } else {
            i = 16;
        }
        setConnectionCapabilities(i);
    }
}
