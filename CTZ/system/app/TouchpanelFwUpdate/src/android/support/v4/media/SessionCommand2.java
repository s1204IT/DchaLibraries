package android.support.v4.media;

import android.os.Bundle;
import android.text.TextUtils;

/* loaded from: classes.dex */
public final class SessionCommand2 {
    private final int mCommandCode;
    private final String mCustomCommand;
    private final Bundle mExtras;

    public SessionCommand2(int commandCode) {
        if (commandCode == 0) {
            throw new IllegalArgumentException("commandCode shouldn't be COMMAND_CODE_CUSTOM");
        }
        this.mCommandCode = commandCode;
        this.mCustomCommand = null;
        this.mExtras = null;
    }

    public SessionCommand2(String action, Bundle extras) {
        if (action == null) {
            throw new IllegalArgumentException("action shouldn't be null");
        }
        this.mCommandCode = 0;
        this.mCustomCommand = action;
        this.mExtras = extras;
    }

    public static SessionCommand2 fromBundle(Bundle command) {
        if (command == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        int code = command.getInt("android.media.media_session2.command.command_code");
        if (code != 0) {
            return new SessionCommand2(code);
        }
        String customCommand = command.getString("android.media.media_session2.command.custom_command");
        if (customCommand == null) {
            return null;
        }
        return new SessionCommand2(customCommand, command.getBundle("android.media.media_session2.command.extras"));
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SessionCommand2)) {
            return false;
        }
        SessionCommand2 other = (SessionCommand2) obj;
        return this.mCommandCode == other.mCommandCode && TextUtils.equals(this.mCustomCommand, other.mCustomCommand);
    }

    public int hashCode() {
        return ((this.mCustomCommand != null ? this.mCustomCommand.hashCode() : 0) * 31) + this.mCommandCode;
    }
}
