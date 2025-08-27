package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalController.IconGroup;
import com.android.systemui.statusbar.policy.SignalController.State;
import com.mediatek.systemui.statusbar.util.FeatureOptions;
import java.io.PrintWriter;
import java.util.BitSet;

/* loaded from: classes.dex */
public abstract class SignalController<T extends State, I extends IconGroup> {
    protected static final boolean CHATTY;
    protected static final boolean DEBUG;
    private final CallbackHandler mCallbackHandler;
    protected final Context mContext;
    private int mHistoryIndex;
    protected final NetworkControllerImpl mNetworkController;
    protected final String mTag;
    protected final int mTransportType;
    protected final T mCurrentState = (T) cleanState();
    protected final T mLastState = (T) cleanState();
    private final State[] mHistory = new State[64];

    protected abstract T cleanState();

    public abstract void notifyListeners(NetworkController.SignalCallback signalCallback);

    static {
        boolean z = true;
        DEBUG = FeatureOptions.LOG_ENABLE || NetworkControllerImpl.DEBUG;
        if (!FeatureOptions.LOG_ENABLE && !NetworkControllerImpl.CHATTY) {
            z = false;
        }
        CHATTY = z;
    }

    public SignalController(String str, Context context, int i, CallbackHandler callbackHandler, NetworkControllerImpl networkControllerImpl) {
        this.mTag = "NetworkController." + str;
        this.mNetworkController = networkControllerImpl;
        this.mTransportType = i;
        this.mContext = context;
        this.mCallbackHandler = callbackHandler;
        for (int i2 = 0; i2 < 64; i2++) {
            this.mHistory[i2] = cleanState();
        }
        Log.d(this.mTag, "constructor");
    }

    public T getState() {
        return this.mCurrentState;
    }

    public void updateConnectivity(BitSet bitSet, BitSet bitSet2) {
        this.mCurrentState.inetCondition = bitSet2.get(this.mTransportType) ? 1 : 0;
        notifyListenersIfNecessary();
    }

    public void resetLastState() {
        this.mCurrentState.copyFrom(this.mLastState);
    }

    public boolean isDirty() {
        if (!this.mLastState.equals(this.mCurrentState)) {
            if (DEBUG) {
                Log.d(this.mTag, "[" + this + "]\nChange in state from: " + this.mLastState + "\n                  to: " + this.mCurrentState);
                return true;
            }
            return true;
        }
        return false;
    }

    public void saveLastState() {
        recordLastState();
        this.mCurrentState.time = System.currentTimeMillis();
        this.mLastState.copyFrom(this.mCurrentState);
    }

    public int getQsCurrentIconId() {
        if (this.mCurrentState.connected) {
            return getIcons().mQsIcons[this.mCurrentState.inetCondition][this.mCurrentState.level];
        }
        if (this.mCurrentState.enabled) {
            return getIcons().mQsDiscState;
        }
        return getIcons().mQsNullState;
    }

    public int getCurrentIconId() {
        if (this.mCurrentState.connected) {
            return getIcons().mSbIcons[this.mCurrentState.inetCondition][this.mCurrentState.level];
        }
        if (this.mCurrentState.enabled) {
            return getIcons().mSbDiscState;
        }
        return getIcons().mSbNullState;
    }

    public int getContentDescription() {
        if (this.mCurrentState.connected) {
            return getIcons().mContentDesc[this.mCurrentState.level];
        }
        return getIcons().mDiscContentDesc;
    }

    public void notifyListenersIfNecessary() {
        if (isDirty()) {
            saveLastState();
            notifyListeners();
        }
    }

    protected String getStringIfExists(int i) {
        return i != 0 ? this.mContext.getString(i) : "";
    }

    protected I getIcons() {
        return (I) this.mCurrentState.iconGroup;
    }

    protected void recordLastState() {
        State[] stateArr = this.mHistory;
        int i = this.mHistoryIndex;
        this.mHistoryIndex = i + 1;
        stateArr[i & 63].copyFrom(this.mLastState);
    }

    /* JADX DEBUG: Move duplicate insns, count: 1 to block B:28:0x004e */
    public void dump(PrintWriter printWriter) {
        printWriter.println("  - " + this.mTag + " -----");
        StringBuilder sb = new StringBuilder();
        sb.append("  Current State: ");
        sb.append(this.mCurrentState);
        printWriter.println(sb.toString());
        int i = 0;
        for (int i2 = 0; i2 < 64; i2++) {
            if (this.mHistory[i2].time != 0) {
                i++;
            }
        }
        int i3 = this.mHistoryIndex + 64;
        while (true) {
            i3--;
            if (i3 < (this.mHistoryIndex + 64) - i) {
                return;
            } else {
                printWriter.println("  Previous State(" + ((this.mHistoryIndex + 64) - i3) + "): " + this.mHistory[i3 & 63]);
            }
        }
    }

    public final void notifyListeners() {
        notifyListeners(this.mCallbackHandler);
    }

    static class IconGroup {
        final int[] mContentDesc;
        final int mDiscContentDesc;
        final String mName;
        final int mQsDiscState;
        final int[][] mQsIcons;
        final int mQsNullState;
        final int mSbDiscState;
        final int[][] mSbIcons;
        final int mSbNullState;

        public IconGroup(String str, int[][] iArr, int[][] iArr2, int[] iArr3, int i, int i2, int i3, int i4, int i5) {
            this.mName = str;
            this.mSbIcons = iArr;
            this.mQsIcons = iArr2;
            this.mContentDesc = iArr3;
            this.mSbNullState = i;
            this.mQsNullState = i2;
            this.mSbDiscState = i3;
            this.mQsDiscState = i4;
            this.mDiscContentDesc = i5;
        }

        public String toString() {
            return "IconGroup(" + this.mName + ")";
        }
    }

    static class State {
        boolean activityIn;
        boolean activityOut;
        boolean connected;
        boolean enabled;
        IconGroup iconGroup;
        int inetCondition;
        int level;
        int rssi;
        long time;

        State() {
        }

        public void copyFrom(State state) {
            this.connected = state.connected;
            this.enabled = state.enabled;
            this.level = state.level;
            this.iconGroup = state.iconGroup;
            this.inetCondition = state.inetCondition;
            this.activityIn = state.activityIn;
            this.activityOut = state.activityOut;
            this.rssi = state.rssi;
            this.time = state.time;
        }

        public String toString() {
            if (this.time != 0) {
                StringBuilder sb = new StringBuilder();
                toString(sb);
                return sb.toString();
            }
            return "Empty " + getClass().getSimpleName();
        }

        protected void toString(StringBuilder sb) {
            sb.append("this=");
            sb.append(hashCode());
            sb.append(',');
            sb.append("connected=");
            sb.append(this.connected);
            sb.append(',');
            sb.append("enabled=");
            sb.append(this.enabled);
            sb.append(',');
            sb.append("level=");
            sb.append(this.level);
            sb.append(',');
            sb.append("inetCondition=");
            sb.append(this.inetCondition);
            sb.append(',');
            sb.append("iconGroup=");
            sb.append(this.iconGroup);
            sb.append(',');
            sb.append("activityIn=");
            sb.append(this.activityIn);
            sb.append(',');
            sb.append("activityOut=");
            sb.append(this.activityOut);
            sb.append(',');
            sb.append("rssi=");
            sb.append(this.rssi);
            sb.append(',');
            sb.append("lastModified=");
            sb.append(DateFormat.format("MM-dd HH:mm:ss", this.time));
        }

        public boolean equals(Object obj) {
            if (!obj.getClass().equals(getClass())) {
                return false;
            }
            State state = (State) obj;
            return state.connected == this.connected && state.enabled == this.enabled && state.level == this.level && state.inetCondition == this.inetCondition && state.iconGroup == this.iconGroup && state.activityIn == this.activityIn && state.activityOut == this.activityOut && state.rssi == this.rssi;
        }
    }
}
