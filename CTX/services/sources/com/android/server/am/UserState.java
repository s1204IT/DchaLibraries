package com.android.server.am;

import android.app.IStopUserCallback;
import android.os.Trace;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.util.ProgressReporter;
import java.io.PrintWriter;
import java.util.ArrayList;

public final class UserState {
    public static final int STATE_BOOTING = 0;
    public static final int STATE_RUNNING_LOCKED = 1;
    public static final int STATE_RUNNING_UNLOCKED = 3;
    public static final int STATE_RUNNING_UNLOCKING = 2;
    public static final int STATE_SHUTDOWN = 5;
    public static final int STATE_STOPPING = 4;
    private static final String TAG = "ActivityManager";
    public final UserHandle mHandle;
    public final ProgressReporter mUnlockProgress;
    public boolean switching;
    public boolean tokenProvided;
    public final ArrayList<IStopUserCallback> mStopCallbacks = new ArrayList<>();
    public int state = 0;
    public int lastState = 0;
    final ArrayMap<String, Long> mProviderLastReportedFg = new ArrayMap<>();

    public UserState(UserHandle userHandle) {
        this.mHandle = userHandle;
        this.mUnlockProgress = new ProgressReporter(userHandle.getIdentifier());
    }

    public boolean setState(int i, int i2) {
        if (this.state == i) {
            setState(i2);
            return true;
        }
        Slog.w(TAG, "Expected user " + this.mHandle.getIdentifier() + " in state " + stateToString(i) + " but was in state " + stateToString(this.state));
        return false;
    }

    public void setState(int i) {
        if (i == this.state) {
            return;
        }
        int identifier = this.mHandle.getIdentifier();
        if (this.state != 0) {
            Trace.asyncTraceEnd(64L, stateToString(this.state) + " " + identifier, identifier);
        }
        if (i != 5) {
            Trace.asyncTraceBegin(64L, stateToString(i) + " " + identifier, identifier);
        }
        Slog.i(TAG, "User " + identifier + " state changed from " + stateToString(this.state) + " to " + stateToString(i));
        EventLogTags.writeAmUserStateChanged(identifier, i);
        this.lastState = this.state;
        this.state = i;
    }

    public static String stateToString(int i) {
        switch (i) {
            case 0:
                return "BOOTING";
            case 1:
                return "RUNNING_LOCKED";
            case 2:
                return "RUNNING_UNLOCKING";
            case 3:
                return "RUNNING_UNLOCKED";
            case 4:
                return "STOPPING";
            case 5:
                return "SHUTDOWN";
            default:
                return Integer.toString(i);
        }
    }

    public static int stateToProtoEnum(int i) {
        switch (i) {
            case 0:
                return 0;
            case 1:
                return 1;
            case 2:
                return 2;
            case 3:
                return 3;
            case 4:
                return 4;
            case 5:
                return 5;
            default:
                return i;
        }
    }

    void dump(String str, PrintWriter printWriter) {
        printWriter.print(str);
        printWriter.print("state=");
        printWriter.print(stateToString(this.state));
        if (this.switching) {
            printWriter.print(" SWITCHING");
        }
        printWriter.println();
    }

    void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        protoOutputStream.write(1159641169921L, stateToProtoEnum(this.state));
        protoOutputStream.write(1133871366146L, this.switching);
        protoOutputStream.end(jStart);
    }
}
