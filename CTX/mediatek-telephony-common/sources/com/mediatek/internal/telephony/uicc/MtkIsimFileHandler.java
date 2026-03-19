package com.mediatek.internal.telephony.uicc;

import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.uicc.IsimFileHandler;
import com.android.internal.telephony.uicc.UiccCardApplication;

public final class MtkIsimFileHandler extends IsimFileHandler {
    static final String LOG_TAG_EX = "MtkIsimFH";

    public MtkIsimFileHandler(UiccCardApplication uiccCardApplication, String str, CommandsInterface commandsInterface) {
        super(uiccCardApplication, str, commandsInterface);
    }
}
