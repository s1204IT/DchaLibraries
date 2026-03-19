package com.android.documentsui.base;

import android.util.Log;
import android.util.Pair;
import com.android.documentsui.Injector;
import com.android.documentsui.R;

public class DebugHelper {
    static final boolean $assertionsDisabled = false;
    private static final int[][] sCode = {new int[]{19, 19, 20, 20, 21, 22, 21, 22, 30, 29}, new int[]{51, 51, 47, 47, 29, 32, 29, 32, 30, 29}};
    private static final int[][] sColors = {new int[]{-2411978, -4776932}, new int[]{-12797356, -14983648}, new int[]{-736755, -415707}, new int[]{-12024339, -15906911}};
    private static final Pair<String, Integer>[] sMessages = {new Pair<>("Woof Woof", Integer.valueOf(R.drawable.debug_msg_1)), new Pair<>("ワンワン", Integer.valueOf(R.drawable.debug_msg_2))};
    private int mCodeIndex;
    private int mColorIndex;
    private boolean mDebugEnabled;
    private final Injector<?> mInjector;
    private long mLastTime;
    private int mMessageIndex;
    private int mPosition;

    public DebugHelper(Injector<?> injector) {
        this.mInjector = injector;
    }

    public int[] getNextColors() {
        if (this.mColorIndex == sColors.length) {
            this.mColorIndex = 0;
        }
        int[][] iArr = sColors;
        int i = this.mColorIndex;
        this.mColorIndex = i + 1;
        return iArr[i];
    }

    public Pair<String, Integer> getNextMessage() {
        if (this.mMessageIndex == sMessages.length) {
            this.mMessageIndex = 0;
        }
        Pair<String, Integer>[] pairArr = sMessages;
        int i = this.mMessageIndex;
        this.mMessageIndex = i + 1;
        return pairArr[i];
    }

    public void debugCheck(long j, int i) {
        if (j == this.mLastTime) {
            return;
        }
        this.mLastTime = j;
        if (this.mPosition == 0) {
            int i2 = 0;
            while (true) {
                if (i2 >= sCode.length) {
                    break;
                }
                if (i != sCode[i2][0]) {
                    i2++;
                } else {
                    this.mCodeIndex = i2;
                    break;
                }
            }
        }
        if (i == sCode[this.mCodeIndex][this.mPosition]) {
            this.mPosition++;
        } else if (this.mPosition > 2 || (this.mPosition == 2 && i != sCode[this.mCodeIndex][0])) {
            this.mPosition = 0;
        }
        if (this.mPosition == sCode[this.mCodeIndex].length) {
            this.mPosition = 0;
            toggleDebugMode();
        }
    }

    public void toggleDebugMode() {
        this.mDebugEnabled = !this.mDebugEnabled;
        if (this.mInjector.actions != 0) {
            this.mInjector.actions.setDebugMode(this.mDebugEnabled);
        }
        if (SharedMinimal.VERBOSE) {
            StringBuilder sb = new StringBuilder();
            sb.append("Debug mode ");
            sb.append(this.mDebugEnabled ? "on" : "off");
            Log.v("DebugHelper", sb.toString());
        }
    }
}
