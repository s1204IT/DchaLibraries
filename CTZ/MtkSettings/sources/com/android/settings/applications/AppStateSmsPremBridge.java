package com.android.settings.applications;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.telephony.ISms;
import com.android.settings.applications.AppStateBaseBridge;
import com.android.settingslib.applications.ApplicationsState;
import java.util.ArrayList;

public class AppStateSmsPremBridge extends AppStateBaseBridge {
    public static final ApplicationsState.AppFilter FILTER_APP_PREMIUM_SMS = new ApplicationsState.AppFilter() {
        @Override
        public void init() {
        }

        @Override
        public boolean filterApp(ApplicationsState.AppEntry appEntry) {
            return (appEntry.extraInfo instanceof SmsState) && ((SmsState) appEntry.extraInfo).smsState != 0;
        }
    };
    private final Context mContext;
    private final ISms mSmsManager;

    public static class SmsState {
        public int smsState;
    }

    public AppStateSmsPremBridge(Context context, ApplicationsState applicationsState, AppStateBaseBridge.Callback callback) {
        super(applicationsState, callback);
        this.mContext = context;
        this.mSmsManager = ISms.Stub.asInterface(ServiceManager.getService("isms"));
    }

    @Override
    protected void loadAllExtraInfo() {
        ArrayList<ApplicationsState.AppEntry> allApps = this.mAppSession.getAllApps();
        int size = allApps.size();
        for (int i = 0; i < size; i++) {
            ApplicationsState.AppEntry appEntry = allApps.get(i);
            updateExtraInfo(appEntry, appEntry.info.packageName, appEntry.info.uid);
        }
    }

    @Override
    protected void updateExtraInfo(ApplicationsState.AppEntry appEntry, String str, int i) {
        appEntry.extraInfo = getState(str);
    }

    public SmsState getState(String str) {
        SmsState smsState = new SmsState();
        smsState.smsState = getSmsState(str);
        return smsState;
    }

    private int getSmsState(String str) {
        try {
            return this.mSmsManager.getPremiumSmsPermission(str);
        } catch (RemoteException e) {
            return 0;
        }
    }

    public void setSmsState(String str, int i) {
        try {
            this.mSmsManager.setPremiumSmsPermission(str, i);
        } catch (RemoteException e) {
        }
    }
}
