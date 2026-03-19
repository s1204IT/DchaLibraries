package android.app.backup;

import android.annotation.SystemApi;

@SystemApi
public abstract class SelectBackupTransportCallback {
    public void onSuccess(String str) {
    }

    public void onFailure(int i) {
    }
}
