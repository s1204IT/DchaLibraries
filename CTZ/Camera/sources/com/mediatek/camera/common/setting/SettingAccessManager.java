package com.mediatek.camera.common.setting;

import android.os.ConditionVariable;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SettingAccessManager {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(SettingAccessManager.class.getSimpleName());
    private boolean mInControlling;
    private List<Access> mAccessList = new ArrayList();
    private volatile int mActiveAccessCount = 0;
    private Object mAccessCountSyncObject = new Object();
    private ConditionVariable mConditionVariable = new ConditionVariable();

    public Access getAccess(String str) {
        return new Access(str);
    }

    public synchronized boolean activeAccess(Access access) {
        return activeAccess(access, true);
    }

    public synchronized boolean activeAccess(Access access, boolean z) {
        if (z) {
            try {
                LogHelper.d(TAG, "[activeAccess], access:" + access.mName + ", mInControlling:" + this.mInControlling);
            } catch (Throwable th) {
                throw th;
            }
        }
        if (this.mInControlling) {
            return false;
        }
        access.validate();
        this.mAccessList.add(access);
        synchronized (this.mAccessCountSyncObject) {
            this.mActiveAccessCount++;
        }
        return true;
    }

    public void recycleAccess(Access access) {
        recycleAccess(access, true);
    }

    public void recycleAccess(Access access, boolean z) {
        if (z) {
            LogHelper.d(TAG, "[recycleAccess], access:" + access.mName);
        }
        synchronized (this.mAccessCountSyncObject) {
            this.mActiveAccessCount--;
        }
        access.invalidate();
        this.mAccessList.remove(access);
        if (this.mActiveAccessCount <= 0) {
            this.mConditionVariable.open();
        }
    }

    public synchronized void startControl() {
        LogHelper.d(TAG, "[startControl]");
        this.mInControlling = true;
        Iterator<Access> it = this.mAccessList.iterator();
        while (it.hasNext()) {
            it.next().invalidate();
        }
        this.mConditionVariable.close();
        LogHelper.d(TAG, "[startControl], mActiveAccessCount:" + this.mActiveAccessCount);
        if (this.mActiveAccessCount > 0) {
            this.mConditionVariable.block();
        }
        this.mAccessList.clear();
        this.mActiveAccessCount = 0;
    }

    public synchronized void stopControl() {
        LogHelper.d(TAG, "[stopControl]");
        this.mInControlling = false;
    }

    public class Access {
        private String mName;
        private boolean mValid;

        public Access(String str) {
            this.mName = str;
        }

        public boolean isValid() {
            return this.mValid;
        }

        private void validate() {
            this.mValid = true;
        }

        private void invalidate() {
            this.mValid = false;
        }
    }
}
