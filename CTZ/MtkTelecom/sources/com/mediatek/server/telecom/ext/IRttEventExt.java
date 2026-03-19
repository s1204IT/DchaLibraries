package com.mediatek.server.telecom.ext;

import android.os.Bundle;

public interface IRttEventExt {
    int getRttProperties();

    boolean handleRttEvent(String str, Bundle bundle, Object obj, Object obj2);
}
