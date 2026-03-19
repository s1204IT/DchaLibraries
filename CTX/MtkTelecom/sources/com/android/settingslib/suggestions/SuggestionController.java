package com.android.settingslib.suggestions;

import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Process;
import android.os.RemoteException;
import android.service.settings.suggestions.ISuggestionService;
import android.service.settings.suggestions.Suggestion;
import android.util.Log;
import java.util.List;

public class SuggestionController {
    private final Context mContext;
    private ISuggestionService mRemoteService;
    private ServiceConnection mServiceConnection;
    private final Intent mServiceIntent;

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
        if (!isReady()) {
            return null;
        }
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

    private boolean isReady() {
        return this.mRemoteService != null;
    }
}
