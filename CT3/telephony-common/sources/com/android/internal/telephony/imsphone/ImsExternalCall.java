package com.android.internal.telephony.imsphone;

import com.android.internal.telephony.Call;
import com.android.internal.telephony.CallStateException;
import com.android.internal.telephony.Connection;
import com.android.internal.telephony.Phone;
import java.util.List;

public class ImsExternalCall extends Call {
    private Phone mPhone;

    public ImsExternalCall(Phone phone, ImsExternalConnection connection) {
        this.mPhone = phone;
        this.mConnections.add(connection);
    }

    @Override
    public List<Connection> getConnections() {
        return this.mConnections;
    }

    @Override
    public Phone getPhone() {
        return this.mPhone;
    }

    @Override
    public boolean isMultiparty() {
        return false;
    }

    @Override
    public void hangup() throws CallStateException {
    }

    public void setActive() {
        setState(Call.State.ACTIVE);
    }

    public void setTerminated() {
        setState(Call.State.DISCONNECTED);
    }
}
