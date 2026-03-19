package com.mediatek.perfservice;

import android.content.Context;
import java.util.HashSet;

public class PerfServiceStateNotifier {

    private static final int[] f11x78558e6c = null;
    static final String TAG = "PerfServiceStateNotifier";
    IPerfServiceWrapper mPerfService = new PerfServiceWrapper((Context) null);

    private static int[] m3456xfd169010() {
        if (f11x78558e6c != null) {
            return f11x78558e6c;
        }
        int[] iArr = new int[ActivityState.valuesCustom().length];
        try {
            iArr[ActivityState.Destroyed.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ActivityState.Paused.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[ActivityState.Resumed.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[ActivityState.Stopped.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f11x78558e6c = iArr;
        return iArr;
    }

    public enum ActivityState {
        Paused,
        Resumed,
        Destroyed,
        Stopped;

        public static ActivityState[] valuesCustom() {
            return values();
        }
    }

    public void notifyActivityState(String packageName, int pid, String className, ActivityState actState) {
        int state;
        switch (m3456xfd169010()[actState.ordinal()]) {
            case 1:
                state = 2;
                break;
            case 2:
                state = 0;
                break;
            case 3:
                state = 1;
                break;
            case 4:
                state = 4;
                break;
            default:
                return;
        }
        this.mPerfService.notifyAppState(packageName, className, state, pid);
    }

    public void notifyAppDied(int pid, HashSet<String> packageList) {
        for (String packageName : packageList) {
            this.mPerfService.notifyAppState(packageName, (String) null, 3, pid);
        }
    }
}
