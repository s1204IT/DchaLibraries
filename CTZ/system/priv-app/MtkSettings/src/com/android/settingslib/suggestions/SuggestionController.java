package com.android.settingslib.suggestions;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;
import android.service.settings.suggestions.ISuggestionService;
import android.service.settings.suggestions.Suggestion;
import android.util.Log;
import java.util.List;
/* loaded from: classes.dex */
public class SuggestionController {
    private ServiceConnectionListener mConnectionListener;
    private final Context mContext;
    private ISuggestionService mRemoteService;
    private ServiceConnection mServiceConnection = createServiceConnection();
    private final Intent mServiceIntent;

    /* loaded from: classes.dex */
    public interface ServiceConnectionListener {
        void onServiceConnected();

        void onServiceDisconnected();
    }

    public SuggestionController(Context context, ComponentName componentName, ServiceConnectionListener serviceConnectionListener) {
        this.mContext = context.getApplicationContext();
        this.mConnectionListener = serviceConnectionListener;
        this.mServiceIntent = new Intent().setComponent(componentName);
    }

    public void start() {
        this.mContext.bindServiceAsUser(this.mServiceIntent, this.mServiceConnection, 1, Process.myUserHandle());
    }

    public void stop() {
        if (this.mRemoteService != null) {
            this.mRemoteService = null;
            this.mContext.unbindService(this.mServiceConnection);
        }
    }

    public List<Suggestion> getSuggestions() {
        if (isReady()) {
            try {
                return this.mRemoteService.getSuggestions();
            } catch (RemoteException | RuntimeException e) {
                Log.w("SuggestionController", "Error when calling getSuggestion()", e);
                return null;
            } catch (NullPointerException e2) {
                Log.w("SuggestionController", "mRemote service detached before able to query", e2);
                return null;
            }
        }
        return null;
    }

    public void dismissSuggestions(Suggestion suggestion) {
        if (!isReady()) {
            Log.w("SuggestionController", "SuggestionController not ready, cannot dismiss " + suggestion.getId());
            return;
        }
        try {
            this.mRemoteService.dismissSuggestion(suggestion);
        } catch (RemoteException | RuntimeException e) {
            Log.w("SuggestionController", "Error when calling dismissSuggestion()", e);
        }
    }

    public void launchSuggestion(Suggestion suggestion) {
        if (!isReady()) {
            Log.w("SuggestionController", "SuggestionController not ready, cannot launch " + suggestion.getId());
            return;
        }
        try {
            this.mRemoteService.launchSuggestion(suggestion);
        } catch (RemoteException | RuntimeException e) {
            Log.w("SuggestionController", "Error when calling launchSuggestion()", e);
        }
    }

    private boolean isReady() {
        return this.mRemoteService != null;
    }

    private ServiceConnection createServiceConnection() {
        return new ServiceConnection() { // from class: com.android.settingslib.suggestions.SuggestionController.1
            @Override // android.content.ServiceConnection
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                SuggestionController.this.mRemoteService = ISuggestionService.Stub.asInterface(iBinder);
                if (SuggestionController.this.mConnectionListener != null) {
                    SuggestionController.this.mConnectionListener.onServiceConnected();
                }
            }

            @Override // android.content.ServiceConnection
            public void onServiceDisconnected(ComponentName componentName) {
                if (SuggestionController.this.mConnectionListener != null) {
                    SuggestionController.this.mRemoteService = null;
                    SuggestionController.this.mConnectionListener.onServiceDisconnected();
                }
            }
        };
    }
}
