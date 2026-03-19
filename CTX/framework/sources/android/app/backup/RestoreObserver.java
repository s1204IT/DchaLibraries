package android.app.backup;

import android.annotation.SystemApi;

public abstract class RestoreObserver {
    @SystemApi
    public void restoreSetsAvailable(RestoreSet[] restoreSetArr) {
    }

    public void restoreStarting(int i) {
    }

    public void onUpdate(int i, String str) {
    }

    public void restoreFinished(int i) {
    }
}
