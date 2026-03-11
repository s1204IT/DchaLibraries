package android.support.v4.media;

import android.os.Bundle;
import android.text.TextUtils;

public final class SessionCommand2 {
    private final int mCommandCode;
    private final String mCustomCommand;
    private final Bundle mExtras;

    public SessionCommand2(int i) {
        if (i == 0) {
            throw new IllegalArgumentException("commandCode shouldn't be COMMAND_CODE_CUSTOM");
        }
        this.mCommandCode = i;
        this.mCustomCommand = null;
        this.mExtras = null;
    }

    public SessionCommand2(String str, Bundle bundle) {
        if (str == null) {
            throw new IllegalArgumentException("action shouldn't be null");
        }
        this.mCommandCode = 0;
        this.mCustomCommand = str;
        this.mExtras = bundle;
    }

    public static SessionCommand2 fromBundle(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        int i = bundle.getInt("android.media.media_session2.command.command_code");
        if (i != 0) {
            return new SessionCommand2(i);
        }
        String string = bundle.getString("android.media.media_session2.command.custom_command");
        if (string == null) {
            return null;
        }
        return new SessionCommand2(string, bundle.getBundle("android.media.media_session2.command.extras"));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SessionCommand2)) {
            return false;
        }
        SessionCommand2 sessionCommand2 = (SessionCommand2) obj;
        return this.mCommandCode == sessionCommand2.mCommandCode && TextUtils.equals(this.mCustomCommand, sessionCommand2.mCustomCommand);
    }

    public int hashCode() {
        return ((this.mCustomCommand != null ? this.mCustomCommand.hashCode() : 0) * 31) + this.mCommandCode;
    }
}
