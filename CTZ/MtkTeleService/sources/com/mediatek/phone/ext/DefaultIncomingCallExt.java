package com.mediatek.phone.ext;

import android.os.Message;
import com.android.internal.telephony.Phone;

public class DefaultIncomingCallExt implements IIncomingCallExt {
    @Override
    public boolean handlePhoneEvent(Message message, Phone phone) {
        return false;
    }

    @Override
    public int changeDisconnectCause(int i) {
        return i;
    }
}
