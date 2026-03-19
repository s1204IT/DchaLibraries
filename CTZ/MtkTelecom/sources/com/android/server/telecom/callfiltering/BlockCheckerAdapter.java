package com.android.server.telecom.callfiltering;

import android.content.Context;
import android.os.Bundle;
import com.android.internal.telephony.BlockChecker;

public class BlockCheckerAdapter {
    public boolean isBlocked(Context context, String str, Bundle bundle) {
        return BlockChecker.isBlocked(context, str, bundle);
    }
}
