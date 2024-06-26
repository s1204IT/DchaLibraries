package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.os.UserHandle;
import android.os.UserManager;
import com.android.systemui.statusbar.phone.ManagedProfileController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
/* loaded from: classes.dex */
public class ManagedProfileControllerImpl implements ManagedProfileController {
    private final Context mContext;
    private int mCurrentUser;
    private boolean mListening;
    private final UserManager mUserManager;
    private final List<ManagedProfileController.Callback> mCallbacks = new ArrayList();
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() { // from class: com.android.systemui.statusbar.phone.ManagedProfileControllerImpl.1
        @Override // android.content.BroadcastReceiver
        public void onReceive(Context context, Intent intent) {
            ManagedProfileControllerImpl.this.reloadManagedProfiles();
            for (ManagedProfileController.Callback callback : ManagedProfileControllerImpl.this.mCallbacks) {
                callback.onManagedProfileChanged();
            }
        }
    };
    private final LinkedList<UserInfo> mProfiles = new LinkedList<>();

    public ManagedProfileControllerImpl(Context context) {
        this.mContext = context;
        this.mUserManager = UserManager.get(this.mContext);
    }

    @Override // com.android.systemui.statusbar.policy.CallbackController
    public void addCallback(ManagedProfileController.Callback callback) {
        this.mCallbacks.add(callback);
        if (this.mCallbacks.size() == 1) {
            setListening(true);
        }
        callback.onManagedProfileChanged();
    }

    @Override // com.android.systemui.statusbar.policy.CallbackController
    public void removeCallback(ManagedProfileController.Callback callback) {
        if (this.mCallbacks.remove(callback) && this.mCallbacks.size() == 0) {
            setListening(false);
        }
    }

    @Override // com.android.systemui.statusbar.phone.ManagedProfileController
    public void setWorkModeEnabled(boolean z) {
        synchronized (this.mProfiles) {
            Iterator<UserInfo> it = this.mProfiles.iterator();
            while (it.hasNext()) {
                if (!this.mUserManager.requestQuietModeEnabled(!z, UserHandle.of(it.next().id))) {
                    ((StatusBarManager) this.mContext.getSystemService("statusbar")).collapsePanels();
                }
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void reloadManagedProfiles() {
        synchronized (this.mProfiles) {
            boolean z = this.mProfiles.size() > 0;
            int currentUser = ActivityManager.getCurrentUser();
            this.mProfiles.clear();
            for (UserInfo userInfo : this.mUserManager.getEnabledProfiles(currentUser)) {
                if (userInfo.isManagedProfile()) {
                    this.mProfiles.add(userInfo);
                }
            }
            if (this.mProfiles.size() == 0 && z && currentUser == this.mCurrentUser) {
                for (ManagedProfileController.Callback callback : this.mCallbacks) {
                    callback.onManagedProfileRemoved();
                }
            }
            this.mCurrentUser = currentUser;
        }
    }

    @Override // com.android.systemui.statusbar.phone.ManagedProfileController
    public boolean hasActiveProfile() {
        boolean z;
        if (!this.mListening) {
            reloadManagedProfiles();
        }
        synchronized (this.mProfiles) {
            z = this.mProfiles.size() > 0;
        }
        return z;
    }

    @Override // com.android.systemui.statusbar.phone.ManagedProfileController
    public boolean isWorkModeEnabled() {
        if (!this.mListening) {
            reloadManagedProfiles();
        }
        synchronized (this.mProfiles) {
            Iterator<UserInfo> it = this.mProfiles.iterator();
            while (it.hasNext()) {
                if (it.next().isQuietModeEnabled()) {
                    return false;
                }
            }
            return true;
        }
    }

    private void setListening(boolean z) {
        this.mListening = z;
        if (z) {
            reloadManagedProfiles();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("android.intent.action.USER_SWITCHED");
            intentFilter.addAction("android.intent.action.MANAGED_PROFILE_ADDED");
            intentFilter.addAction("android.intent.action.MANAGED_PROFILE_REMOVED");
            intentFilter.addAction("android.intent.action.MANAGED_PROFILE_AVAILABLE");
            intentFilter.addAction("android.intent.action.MANAGED_PROFILE_UNAVAILABLE");
            this.mContext.registerReceiverAsUser(this.mReceiver, UserHandle.ALL, intentFilter, null, null);
            return;
        }
        this.mContext.unregisterReceiver(this.mReceiver);
    }
}
