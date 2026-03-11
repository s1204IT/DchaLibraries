package android.support.v4.media;

import android.annotation.TargetApi;
import android.os.Bundle;

@TargetApi(19)
public class MediaSession2 implements AutoCloseable {
    private final SupportLibraryImpl mImpl;

    public static final class CommandButton {
        private SessionCommand2 mCommand;
        private String mDisplayName;
        private boolean mEnabled;
        private Bundle mExtras;
        private int mIconResId;

        public static final class Builder {
            private SessionCommand2 mCommand;
            private String mDisplayName;
            private boolean mEnabled;
            private Bundle mExtras;
            private int mIconResId;

            public CommandButton build() {
                return new CommandButton(this.mCommand, this.mIconResId, this.mDisplayName, this.mExtras, this.mEnabled);
            }

            public Builder setCommand(SessionCommand2 sessionCommand2) {
                this.mCommand = sessionCommand2;
                return this;
            }

            public Builder setDisplayName(String str) {
                this.mDisplayName = str;
                return this;
            }

            public Builder setEnabled(boolean z) {
                this.mEnabled = z;
                return this;
            }

            public Builder setExtras(Bundle bundle) {
                this.mExtras = bundle;
                return this;
            }

            public Builder setIconResId(int i) {
                this.mIconResId = i;
                return this;
            }
        }

        private CommandButton(SessionCommand2 sessionCommand2, int i, String str, Bundle bundle, boolean z) {
            this.mCommand = sessionCommand2;
            this.mIconResId = i;
            this.mDisplayName = str;
            this.mExtras = bundle;
            this.mEnabled = z;
        }

        public static CommandButton fromBundle(Bundle bundle) {
            if (bundle == null) {
                return null;
            }
            Builder builder = new Builder();
            builder.setCommand(SessionCommand2.fromBundle(bundle.getBundle("android.media.media_session2.command_button.command")));
            builder.setIconResId(bundle.getInt("android.media.media_session2.command_button.icon_res_id", 0));
            builder.setDisplayName(bundle.getString("android.media.media_session2.command_button.display_name"));
            builder.setExtras(bundle.getBundle("android.media.media_session2.command_button.extras"));
            builder.setEnabled(bundle.getBoolean("android.media.media_session2.command_button.enabled"));
            try {
                return builder.build();
            } catch (IllegalStateException e) {
                return null;
            }
        }
    }

    interface SupportLibraryImpl extends AutoCloseable {
    }

    @Override
    public void close() {
        try {
            this.mImpl.close();
        } catch (Exception e) {
        }
    }
}
