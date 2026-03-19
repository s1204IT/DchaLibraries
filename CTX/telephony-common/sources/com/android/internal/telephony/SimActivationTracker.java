package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.Rlog;
import android.util.LocalLog;
import com.android.internal.util.IndentingPrintWriter;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class SimActivationTracker {
    private static final boolean DBG = true;
    private static final String LOG_TAG = "SAT";
    private static final boolean VDBG = Rlog.isLoggable(LOG_TAG, 2);
    private Phone mPhone;
    private final LocalLog mVoiceActivationStateLog = new LocalLog(10);
    private final LocalLog mDataActivationStateLog = new LocalLog(10);
    private int mVoiceActivationState = 0;
    private int mDataActivationState = 0;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SimActivationTracker.VDBG) {
                SimActivationTracker.this.log("action: " + action);
            }
            if ("android.intent.action.SIM_STATE_CHANGED".equals(action) && "ABSENT".equals(intent.getStringExtra("ss"))) {
                SimActivationTracker.this.log("onSimAbsent, reset activation state to UNKNOWN");
                SimActivationTracker.this.setVoiceActivationState(0);
                SimActivationTracker.this.setDataActivationState(0);
            }
        }
    };

    public SimActivationTracker(Phone phone) {
        this.mPhone = phone;
        this.mPhone.getContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SIM_STATE_CHANGED"));
    }

    public void setVoiceActivationState(int i) {
        if (!isValidActivationState(i) || 4 == i) {
            throw new IllegalArgumentException("invalid voice activation state: " + i);
        }
        log("setVoiceActivationState=" + i);
        this.mVoiceActivationState = i;
        this.mVoiceActivationStateLog.log(toString(i));
        this.mPhone.notifyVoiceActivationStateChanged(i);
    }

    public void setDataActivationState(int i) {
        if (!isValidActivationState(i)) {
            throw new IllegalArgumentException("invalid data activation state: " + i);
        }
        log("setDataActivationState=" + i);
        this.mDataActivationState = i;
        this.mDataActivationStateLog.log(toString(i));
        this.mPhone.notifyDataActivationStateChanged(i);
    }

    public int getVoiceActivationState() {
        return this.mVoiceActivationState;
    }

    public int getDataActivationState() {
        return this.mDataActivationState;
    }

    private static boolean isValidActivationState(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
                return true;
            default:
                return false;
        }
    }

    private static String toString(int i) {
        switch (i) {
            case 0:
                return "unknown";
            case 1:
                return "activating";
            case 2:
                return "activated";
            case 3:
                return "deactivated";
            case 4:
                return "restricted";
            default:
                return "invalid";
        }
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    private void loge(String str) {
        Rlog.e(LOG_TAG, "[" + this.mPhone.getPhoneId() + "]" + str);
    }

    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        IndentingPrintWriter indentingPrintWriter = new IndentingPrintWriter(printWriter, "  ");
        printWriter.println(" mVoiceActivationState Log:");
        indentingPrintWriter.increaseIndent();
        this.mVoiceActivationStateLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
        printWriter.println(" mDataActivationState Log:");
        indentingPrintWriter.increaseIndent();
        this.mDataActivationStateLog.dump(fileDescriptor, indentingPrintWriter, strArr);
        indentingPrintWriter.decreaseIndent();
    }

    public void dispose() {
        this.mPhone.getContext().unregisterReceiver(this.mReceiver);
    }
}
