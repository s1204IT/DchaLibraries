package com.android.quicksearchbox;

import android.app.Application;

/* loaded from: classes.dex */
public class QsbApplicationWrapper extends Application {
    private QsbApplication mApp;

    @Override // android.app.Application
    public void onTerminate() {
        synchronized (this) {
            if (this.mApp != null) {
                this.mApp.close();
            }
        }
        super.onTerminate();
    }

    public synchronized QsbApplication getApp() {
        if (this.mApp == null) {
            this.mApp = createQsbApplication();
        }
        return this.mApp;
    }

    protected QsbApplication createQsbApplication() {
        return new QsbApplication(this);
    }
}
