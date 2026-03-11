package android.support.v4.media;

import android.os.Bundle;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class SessionCommandGroup2 {
    private Set<SessionCommand2> mCommands = new HashSet();

    public static SessionCommandGroup2 fromBundle(Bundle bundle) {
        ArrayList parcelableArrayList;
        SessionCommand2 sessionCommand2FromBundle;
        if (bundle == null || (parcelableArrayList = bundle.getParcelableArrayList("android.media.mediasession2.commandgroup.commands")) == null) {
            return null;
        }
        SessionCommandGroup2 sessionCommandGroup2 = new SessionCommandGroup2();
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 >= parcelableArrayList.size()) {
                return sessionCommandGroup2;
            }
            Parcelable parcelable = (Parcelable) parcelableArrayList.get(i2);
            if ((parcelable instanceof Bundle) && (sessionCommand2FromBundle = SessionCommand2.fromBundle((Bundle) parcelable)) != null) {
                sessionCommandGroup2.addCommand(sessionCommand2FromBundle);
            }
            i = i2 + 1;
        }
    }

    public void addCommand(SessionCommand2 sessionCommand2) {
        if (sessionCommand2 == null) {
            throw new IllegalArgumentException("command shouldn't be null");
        }
        this.mCommands.add(sessionCommand2);
    }
}
