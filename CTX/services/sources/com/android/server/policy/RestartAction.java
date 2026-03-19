package com.android.server.policy;

import android.R;
import android.content.Context;
import android.os.UserManager;
import com.android.internal.globalactions.LongPressAction;
import com.android.internal.globalactions.SinglePressAction;
import com.android.server.policy.WindowManagerPolicy;

public final class RestartAction extends SinglePressAction implements LongPressAction {
    private final Context mContext;
    private final WindowManagerPolicy.WindowManagerFuncs mWindowManagerFuncs;

    public RestartAction(Context context, WindowManagerPolicy.WindowManagerFuncs windowManagerFuncs) {
        super(R.drawable.ic_media_route_connected_light_08_mtrl, R.string.config_defaultQrCodeComponent);
        this.mContext = context;
        this.mWindowManagerFuncs = windowManagerFuncs;
    }

    public boolean onLongPress() {
        if (!((UserManager) this.mContext.getSystemService(UserManager.class)).hasUserRestriction("no_safe_boot")) {
            this.mWindowManagerFuncs.rebootSafeMode(true);
            return true;
        }
        return false;
    }

    public boolean showDuringKeyguard() {
        return true;
    }

    public boolean showBeforeProvisioning() {
        return true;
    }

    public void onPress() {
        this.mWindowManagerFuncs.reboot(false);
    }
}
