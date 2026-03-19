package android.hardware.location;

import android.annotation.SystemApi;

@SystemApi
public class ContextHubClientCallback {
    public void onMessageFromNanoApp(ContextHubClient contextHubClient, NanoAppMessage nanoAppMessage) {
    }

    public void onHubReset(ContextHubClient contextHubClient) {
    }

    public void onNanoAppAborted(ContextHubClient contextHubClient, long j, int i) {
    }

    public void onNanoAppLoaded(ContextHubClient contextHubClient, long j) {
    }

    public void onNanoAppUnloaded(ContextHubClient contextHubClient, long j) {
    }

    public void onNanoAppEnabled(ContextHubClient contextHubClient, long j) {
    }

    public void onNanoAppDisabled(ContextHubClient contextHubClient, long j) {
    }
}
