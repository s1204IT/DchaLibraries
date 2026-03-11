package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.text.format.DateFormat;
import android.util.Log;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.SignalController.IconGroup;
import com.android.systemui.statusbar.policy.SignalController.State;
import java.io.PrintWriter;
import java.util.BitSet;

public abstract class SignalController<T extends State, I extends IconGroup> {
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

    public SignalController(String str, Context context, int i, CallbackHandler callbackHandler, NetworkControllerImpl networkControllerImpl) {
        this.mTag = "NetworkController." + str;
        this.mNetworkController = networkControllerImpl;
        this.mTransportType = i;
        this.mContext = context;
        this.mCallbackHandler = callbackHandler;
        for (int i2 = 0; i2 < 64; i2++) {
            this.mHistory[i2] = cleanState();
        }
    }

    public T getState() {
        return this.mCurrentState;
    }

    public void updateConnectivity(BitSet connectedTransports, BitSet validatedTransports) {
        this.mCurrentState.inetCondition = validatedTransports.get(this.mTransportType) ? 1 : 0;
        notifyListenersIfNecessary();
    }

    public void resetLastState() {
        this.mCurrentState.copyFrom(this.mLastState);
    }

    public boolean isDirty() {
        if (!this.mLastState.equals(this.mCurrentState)) {
            Log.d(this.mTag, "Change in state from: " + this.mLastState + "\n\tto: " + this.mCurrentState);
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
        if (!isDirty()) {
            return;
        }
        saveLastState();
        notifyListeners();
    }

    protected String getStringIfExists(int resId) {
        return resId != 0 ? this.mContext.getString(resId) : "";
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

    public void dump(PrintWriter pw) {
        pw.println("  - " + this.mTag + " -----");
        pw.println("  Current State: " + this.mCurrentState);
        int size = 0;
        for (int i = 0; i < 64; i++) {
            if (this.mHistory[i].time != 0) {
                size++;
            }
        }
        for (int i2 = (this.mHistoryIndex + 64) - 1; i2 >= (this.mHistoryIndex + 64) - size; i2--) {
            pw.println("  Previous State(" + ((this.mHistoryIndex + 64) - i2) + "): " + this.mHistory[i2 & 63]);
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

        public IconGroup(String name, int[][] sbIcons, int[][] qsIcons, int[] contentDesc, int sbNullState, int qsNullState, int sbDiscState, int qsDiscState, int discContentDesc) {
            this.mName = name;
            this.mSbIcons = sbIcons;
            this.mQsIcons = qsIcons;
            this.mContentDesc = contentDesc;
            this.mSbNullState = sbNullState;
            this.mQsNullState = qsNullState;
            this.mSbDiscState = sbDiscState;
            this.mQsDiscState = qsDiscState;
            this.mDiscContentDesc = discContentDesc;
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
                StringBuilder builder = new StringBuilder();
                toString(builder);
                return builder.toString();
            }
            return "Empty " + getClass().getSimpleName();
        }

        protected void toString(StringBuilder builder) {
            builder.append("connected=").append(this.connected).append(',').append("enabled=").append(this.enabled).append(',').append("level=").append(this.level).append(',').append("inetCondition=").append(this.inetCondition).append(',').append("iconGroup=").append(this.iconGroup).append(',').append("activityIn=").append(this.activityIn).append(',').append("activityOut=").append(this.activityOut).append(',').append("rssi=").append(this.rssi).append(',').append("lastModified=").append(DateFormat.format("MM-dd hh:mm:ss", this.time));
        }

        public boolean equals(Object o) {
            if (!o.getClass().equals(getClass())) {
                return false;
            }
            State other = (State) o;
            return other.connected == this.connected && other.enabled == this.enabled && other.level == this.level && other.inetCondition == this.inetCondition && other.iconGroup == this.iconGroup && other.activityIn == this.activityIn && other.activityOut == this.activityOut && other.rssi == this.rssi;
        }
    }
}
