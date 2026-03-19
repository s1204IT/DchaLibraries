package com.android.server.wm;

import android.util.ArrayMap;
import android.util.ArraySet;
import java.io.PrintWriter;
import java.util.ArrayList;

class AnimatingAppWindowTokenRegistry {
    private boolean mEndingDeferredFinish;
    private ArraySet<AppWindowToken> mAnimatingTokens = new ArraySet<>();
    private ArrayMap<AppWindowToken, Runnable> mFinishedTokens = new ArrayMap<>();
    private ArrayList<Runnable> mTmpRunnableList = new ArrayList<>();

    AnimatingAppWindowTokenRegistry() {
    }

    void notifyStarting(AppWindowToken appWindowToken) {
        this.mAnimatingTokens.add(appWindowToken);
    }

    void notifyFinished(AppWindowToken appWindowToken) {
        this.mAnimatingTokens.remove(appWindowToken);
        this.mFinishedTokens.remove(appWindowToken);
        if (this.mAnimatingTokens.isEmpty()) {
            endDeferringFinished();
        }
    }

    boolean notifyAboutToFinish(AppWindowToken appWindowToken, Runnable runnable) {
        if (!this.mAnimatingTokens.remove(appWindowToken)) {
            return false;
        }
        if (this.mAnimatingTokens.isEmpty()) {
            endDeferringFinished();
            return false;
        }
        this.mFinishedTokens.put(appWindowToken, runnable);
        return true;
    }

    private void endDeferringFinished() {
        if (this.mEndingDeferredFinish) {
            return;
        }
        try {
            this.mEndingDeferredFinish = true;
            for (int size = this.mFinishedTokens.size() - 1; size >= 0; size--) {
                this.mTmpRunnableList.add(this.mFinishedTokens.valueAt(size));
            }
            this.mFinishedTokens.clear();
            for (int size2 = this.mTmpRunnableList.size() - 1; size2 >= 0; size2--) {
                this.mTmpRunnableList.get(size2).run();
            }
            this.mTmpRunnableList.clear();
        } finally {
            this.mEndingDeferredFinish = false;
        }
    }

    void dump(PrintWriter printWriter, String str, String str2) {
        if (!this.mAnimatingTokens.isEmpty() || !this.mFinishedTokens.isEmpty()) {
            printWriter.print(str2);
            printWriter.println(str);
            String str3 = str2 + "  ";
            printWriter.print(str3);
            printWriter.print("mAnimatingTokens=");
            printWriter.println(this.mAnimatingTokens);
            printWriter.print(str3);
            printWriter.print("mFinishedTokens=");
            printWriter.println(this.mFinishedTokens);
        }
    }
}
