package com.android.server.backup;

import com.android.server.backup.transport.OnTransportRegisteredListener;

public final class $$Lambda$TransportManager$Z9ckpFUW2V4jkdHnyXIEiLuAoBc implements OnTransportRegisteredListener {
    public static final $$Lambda$TransportManager$Z9ckpFUW2V4jkdHnyXIEiLuAoBc INSTANCE = new $$Lambda$TransportManager$Z9ckpFUW2V4jkdHnyXIEiLuAoBc();

    private $$Lambda$TransportManager$Z9ckpFUW2V4jkdHnyXIEiLuAoBc() {
    }

    @Override
    public final void onTransportRegistered(String str, String str2) {
        TransportManager.lambda$new$0(str, str2);
    }
}
