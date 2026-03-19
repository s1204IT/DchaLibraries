package com.mediatek.phone.ext;

import android.os.Message;
import android.widget.EditText;
import com.android.internal.telephony.MmiCode;
import com.android.internal.telephony.Phone;

public interface IMmiCodeExt {
    void configBeforeMmiDialogShow(MmiCode mmiCode);

    void onMmiDailogShow(Message message);

    boolean showUssdInteractionDialog(Phone phone, EditText editText);

    boolean skipPlayingUssdTone();
}
