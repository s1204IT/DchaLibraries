package com.android.server.wm;

import android.util.EventLog;
import com.android.server.EventLogTags;

class Task {
    final AppTokenList mAppTokens = new AppTokenList();
    boolean mDeferRemoval = false;
    TaskStack mStack;
    final int mUserId;
    final int taskId;

    Task(AppWindowToken wtoken, TaskStack stack, int userId) {
        this.taskId = wtoken.groupId;
        this.mAppTokens.add(wtoken);
        this.mStack = stack;
        this.mUserId = userId;
    }

    DisplayContent getDisplayContent() {
        return this.mStack.getDisplayContent();
    }

    void addAppToken(int addPos, AppWindowToken wtoken) {
        int lastPos = this.mAppTokens.size();
        for (int pos = 0; pos < lastPos && pos < addPos; pos++) {
            if (this.mAppTokens.get(pos).removed) {
                addPos++;
            }
        }
        this.mAppTokens.add(addPos, wtoken);
        this.mDeferRemoval = false;
    }

    boolean removeAppToken(AppWindowToken wtoken) {
        boolean removed = this.mAppTokens.remove(wtoken);
        if (this.mAppTokens.size() == 0) {
            EventLog.writeEvent(EventLogTags.WM_TASK_REMOVED, Integer.valueOf(this.taskId), "removeAppToken: last token");
        }
        return removed;
    }

    void setSendingToBottom(boolean toBottom) {
        for (int appTokenNdx = 0; appTokenNdx < this.mAppTokens.size(); appTokenNdx++) {
            this.mAppTokens.get(appTokenNdx).sendingToBottom = toBottom;
        }
    }

    public String toString() {
        return "{taskId=" + this.taskId + " appTokens=" + this.mAppTokens + " mdr=" + this.mDeferRemoval + "}";
    }
}
