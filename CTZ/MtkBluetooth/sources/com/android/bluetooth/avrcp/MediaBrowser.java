package com.android.bluetooth.avrcp;

import android.content.ComponentName;
import android.content.Context;
import android.media.browse.MediaBrowser;
import android.media.session.MediaSession;
import android.os.Bundle;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;

public class MediaBrowser {
    android.media.browse.MediaBrowser mDelegate;

    public static abstract class ConnectionCallback extends MediaBrowser.ConnectionCallback {
    }

    public static abstract class ItemCallback extends MediaBrowser.ItemCallback {
    }

    public static abstract class SubscriptionCallback extends MediaBrowser.SubscriptionCallback {
    }

    public MediaBrowser(android.media.browse.MediaBrowser mediaBrowser) {
        this.mDelegate = mediaBrowser;
    }

    public MediaBrowser(Context context, ComponentName componentName, ConnectionCallback connectionCallback, Bundle bundle) {
        this.mDelegate = new android.media.browse.MediaBrowser(context, componentName, connectionCallback, bundle);
    }

    public void connect() {
        this.mDelegate.connect();
    }

    public void disconnect() {
        this.mDelegate.disconnect();
    }

    public Bundle getExtras() {
        return this.mDelegate.getExtras();
    }

    public void getItem(String str, ItemCallback itemCallback) {
        this.mDelegate.getItem(str, itemCallback);
    }

    public String getRoot() {
        return this.mDelegate.getRoot();
    }

    public ComponentName getServiceComponent() {
        return this.mDelegate.getServiceComponent();
    }

    public MediaSession.Token getSessionToken() {
        return this.mDelegate.getSessionToken();
    }

    public boolean isConnected() {
        return this.mDelegate.isConnected();
    }

    public void subscribe(String str, Bundle bundle, SubscriptionCallback subscriptionCallback) {
        this.mDelegate.subscribe(str, bundle, subscriptionCallback);
    }

    public void subscribe(String str, SubscriptionCallback subscriptionCallback) {
        this.mDelegate.subscribe(str, subscriptionCallback);
    }

    public void unsubscribe(String str) {
        this.mDelegate.unsubscribe(str);
    }

    public void unsubscribe(String str, SubscriptionCallback subscriptionCallback) {
        this.mDelegate.unsubscribe(str, subscriptionCallback);
    }

    @VisibleForTesting
    public void testInit(Context context, ComponentName componentName, ConnectionCallback connectionCallback, Bundle bundle) {
        Log.wtfStack("AvrcpMockMediaBrowser", "This function should never be called");
    }
}
