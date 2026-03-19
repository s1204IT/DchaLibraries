package com.android.uiautomator.core;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.IActivityController;
import android.app.IActivityManager;
import android.app.UiAutomation;
import android.app.UiAutomationConnection;
import android.content.Intent;
import android.os.HandlerThread;
import android.os.RemoteException;

public class UiAutomationShellWrapper {
    private static final String HANDLER_THREAD_NAME = "UiAutomatorHandlerThread";
    private final HandlerThread mHandlerThread = new HandlerThread(HANDLER_THREAD_NAME);
    private UiAutomation mUiAutomation;

    public void connect() {
        if (this.mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already connected!");
        }
        this.mHandlerThread.start();
        this.mUiAutomation = new UiAutomation(this.mHandlerThread.getLooper(), new UiAutomationConnection());
        this.mUiAutomation.connect();
    }

    public void setRunAsMonkey(boolean z) {
        IActivityManager service = ActivityManager.getService();
        if (service == null) {
            throw new RuntimeException("Can't manage monkey status; is the system running?");
        }
        try {
            if (z) {
                service.setActivityController(new DummyActivityController(), true);
            } else {
                service.setActivityController((IActivityController) null, true);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    public void disconnect() {
        if (!this.mHandlerThread.isAlive()) {
            throw new IllegalStateException("Already disconnected!");
        }
        this.mUiAutomation.disconnect();
        this.mHandlerThread.quit();
    }

    public UiAutomation getUiAutomation() {
        return this.mUiAutomation;
    }

    public void setCompressedLayoutHierarchy(boolean z) {
        AccessibilityServiceInfo serviceInfo = this.mUiAutomation.getServiceInfo();
        if (z) {
            serviceInfo.flags &= -3;
        } else {
            serviceInfo.flags |= 2;
        }
        this.mUiAutomation.setServiceInfo(serviceInfo);
    }

    private class DummyActivityController extends IActivityController.Stub {
        private DummyActivityController() {
        }

        public boolean activityStarting(Intent intent, String str) throws RemoteException {
            return true;
        }

        public boolean activityResuming(String str) throws RemoteException {
            return true;
        }

        public boolean appCrashed(String str, int i, String str2, String str3, long j, String str4) throws RemoteException {
            return true;
        }

        public int appEarlyNotResponding(String str, int i, String str2) throws RemoteException {
            return 0;
        }

        public int appNotResponding(String str, int i, String str2) throws RemoteException {
            return 0;
        }

        public int systemNotResponding(String str) throws RemoteException {
            return 0;
        }
    }
}
