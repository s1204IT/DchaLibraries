package com.mediatek.phone.ext;

import android.os.Message;
import com.android.internal.telephony.Phone;

public interface IIncomingCallExt {
    int changeDisconnectCause(int i);

    boolean handlePhoneEvent(Message message, Phone phone);
}
