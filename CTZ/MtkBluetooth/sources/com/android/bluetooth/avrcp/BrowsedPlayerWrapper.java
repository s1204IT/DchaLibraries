package com.android.bluetooth.avrcp;

import android.content.ComponentName;
import android.content.Context;
import android.media.browse.MediaBrowser;
import android.os.SystemProperties;
import android.util.Log;
import com.android.bluetooth.avrcp.MediaBrowser;
import com.android.bluetooth.avrcp.MediaController;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class BrowsedPlayerWrapper {
    private static final boolean DEBUG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    static final int NUM_CACHED_FOLDERS = 5;
    public static final int STATUS_CONN_ERROR = 1;
    public static final int STATUS_LOOKUP_ERROR = 2;
    public static final int STATUS_SUCCESS = 0;
    private static final String TAG = "NewAvrcpBrowsedPlayerWrapper";
    private ConnectionCallback mCallback;
    private Context mContext;
    private String mPackageName;
    private MediaBrowser mWrappedBrowser;
    private String mRoot = "";
    LinkedHashMap<String, List<ListItem>> mCachedFolders = new LinkedHashMap<String, List<ListItem>>(5) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, List<ListItem>> entry) {
            return size() > 5;
        }
    };

    interface BrowseCallback {
        void run(int i, String str, List<ListItem> list);
    }

    interface ConnectionCallback {
        void run(int i, BrowsedPlayerWrapper browsedPlayerWrapper);
    }

    enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }

    private BrowsedPlayerWrapper(Context context, String str, String str2) {
        this.mContext = context;
        this.mPackageName = str;
        this.mWrappedBrowser = MediaBrowserFactory.make(context, new ComponentName(str, str2), new MediaConnectionCallback(), null);
    }

    static BrowsedPlayerWrapper wrap(Context context, String str, String str2) {
        Log.i(TAG, "Wrapping Media Browser " + str);
        return new BrowsedPlayerWrapper(context, str, str2);
    }

    void connect(final ConnectionCallback connectionCallback) {
        if (connectionCallback == null) {
            Log.wtfStack(TAG, "connect: Trying to connect to " + this.mPackageName + "with null callback");
        }
        if (this.mCallback != null) {
            Log.w(TAG, "connect: Already trying to connect to " + this.mPackageName);
            return;
        }
        if (DEBUG) {
            Log.d(TAG, "connect: Connecting to browsable player: " + this.mPackageName);
        }
        this.mCallback = new ConnectionCallback() {
            @Override
            public final void run(int i, BrowsedPlayerWrapper browsedPlayerWrapper) {
                BrowsedPlayerWrapper.lambda$connect$0(connectionCallback, i, browsedPlayerWrapper);
            }
        };
        this.mWrappedBrowser.connect();
    }

    static void lambda$connect$0(ConnectionCallback connectionCallback, int i, BrowsedPlayerWrapper browsedPlayerWrapper) {
        connectionCallback.run(i, browsedPlayerWrapper);
        browsedPlayerWrapper.disconnect();
    }

    void disconnect() {
        if (DEBUG) {
            Log.d(TAG, "disconnect: Disconnecting from " + this.mPackageName);
        }
        this.mWrappedBrowser.disconnect();
        this.mCallback = null;
    }

    public String getPackageName() {
        return this.mPackageName;
    }

    public String getRootId() {
        return this.mRoot;
    }

    public void playItem(final String str) {
        if (DEBUG) {
            Log.d(TAG, "playItem: Play Item from media ID: " + str);
        }
        connect(new ConnectionCallback() {
            @Override
            public final void run(int i, BrowsedPlayerWrapper browsedPlayerWrapper) {
                BrowsedPlayerWrapper.lambda$playItem$1(this.f$0, str, i, browsedPlayerWrapper);
            }
        });
    }

    public static void lambda$playItem$1(BrowsedPlayerWrapper browsedPlayerWrapper, String str, int i, BrowsedPlayerWrapper browsedPlayerWrapper2) {
        if (DEBUG) {
            Log.d(TAG, "playItem: Connected to browsable player " + browsedPlayerWrapper.mPackageName);
        }
        MediaController.TransportControls transportControls = MediaControllerFactory.make(browsedPlayerWrapper.mContext, browsedPlayerWrapper2.mWrappedBrowser.getSessionToken()).getTransportControls();
        Log.i(TAG, "playItem: Playing " + str);
        transportControls.playFromMediaId(str, null);
    }

    public boolean getFolderItems(final String str, final BrowseCallback browseCallback) {
        if (this.mCachedFolders.containsKey(str)) {
            Log.i(TAG, "getFolderItems: Grabbing cached data for mediaId: " + str);
            browseCallback.run(0, str, Util.cloneList(this.mCachedFolders.get(str)));
            return true;
        }
        if (browseCallback == null) {
            Log.wtfStack(TAG, "connect: Trying to connect to " + this.mPackageName + "with null callback");
        }
        if (this.mCallback != null) {
            Log.w(TAG, "connect: Already trying to connect to " + this.mPackageName);
            return false;
        }
        if (DEBUG) {
            Log.d(TAG, "connect: Connecting to browsable player: " + this.mPackageName);
        }
        this.mCallback = new ConnectionCallback() {
            @Override
            public final void run(int i, BrowsedPlayerWrapper browsedPlayerWrapper) {
                BrowsedPlayerWrapper.lambda$getFolderItems$2(this.f$0, browseCallback, str, i, browsedPlayerWrapper);
            }
        };
        this.mWrappedBrowser.connect();
        return true;
    }

    public static void lambda$getFolderItems$2(BrowsedPlayerWrapper browsedPlayerWrapper, BrowseCallback browseCallback, String str, int i, BrowsedPlayerWrapper browsedPlayerWrapper2) {
        Log.i(TAG, "getFolderItems: Connected to browsable player: " + browsedPlayerWrapper.mPackageName);
        if (i != 0) {
            browseCallback.run(i, "", new ArrayList());
        }
        browsedPlayerWrapper.getFolderItemsInternal(str, browseCallback);
    }

    private boolean getFolderItemsInternal(String str, BrowseCallback browseCallback) {
        if (str.equals("")) {
            Log.i(TAG, "getFolderItemsInternal: mediaID = " + str);
            return false;
        }
        this.mWrappedBrowser.subscribe(str, new BrowserSubscriptionCallback(browseCallback));
        return true;
    }

    class MediaConnectionCallback extends MediaBrowser.ConnectionCallback {
        MediaConnectionCallback() {
        }

        @Override
        public void onConnected() {
            Log.i(BrowsedPlayerWrapper.TAG, "onConnected: " + BrowsedPlayerWrapper.this.mPackageName + " is connected");
            BrowsedPlayerWrapper.this.mRoot = BrowsedPlayerWrapper.this.mWrappedBrowser.getRoot();
            if (BrowsedPlayerWrapper.this.mCallback == null) {
                return;
            }
            if (BrowsedPlayerWrapper.this.mRoot == null || BrowsedPlayerWrapper.this.mRoot.isEmpty()) {
                BrowsedPlayerWrapper.this.mCallback.run(1, BrowsedPlayerWrapper.this);
            } else {
                BrowsedPlayerWrapper.this.mCallback.run(0, BrowsedPlayerWrapper.this);
                BrowsedPlayerWrapper.this.mCallback = null;
            }
        }

        @Override
        public void onConnectionFailed() {
            Log.w(BrowsedPlayerWrapper.TAG, "onConnectionFailed: Connection Failed with " + BrowsedPlayerWrapper.this.mPackageName);
            if (BrowsedPlayerWrapper.this.mCallback != null) {
                BrowsedPlayerWrapper.this.mCallback.run(1, BrowsedPlayerWrapper.this);
            }
            BrowsedPlayerWrapper.this.mCallback = null;
        }

        @Override
        public void onConnectionSuspended() {
            BrowsedPlayerWrapper.this.mWrappedBrowser.disconnect();
            Log.i(BrowsedPlayerWrapper.TAG, "onConnectionSuspended: Connection Suspended with " + BrowsedPlayerWrapper.this.mPackageName);
        }
    }

    private class BrowserSubscriptionCallback extends MediaBrowser.SubscriptionCallback {
        BrowseCallback mCallback;

        BrowserSubscriptionCallback(BrowseCallback browseCallback) {
            this.mCallback = null;
            this.mCallback = browseCallback;
        }

        @Override
        public void onChildrenLoaded(String str, List<MediaBrowser.MediaItem> list) {
            if (BrowsedPlayerWrapper.DEBUG) {
                Log.d(BrowsedPlayerWrapper.TAG, "onChildrenLoaded: mediaId=" + str + " size= " + list.size());
            }
            if (this.mCallback == null) {
                Log.w(BrowsedPlayerWrapper.TAG, "onChildrenLoaded: " + BrowsedPlayerWrapper.this.mPackageName + " children loaded while callback is null");
            }
            BrowsedPlayerWrapper.this.mWrappedBrowser.unsubscribe(str);
            ArrayList arrayList = new ArrayList();
            for (MediaBrowser.MediaItem mediaItem : list) {
                if (BrowsedPlayerWrapper.DEBUG) {
                    Log.d(BrowsedPlayerWrapper.TAG, "onChildrenLoaded: Child=\"" + mediaItem.toString() + "\",  ID=\"" + mediaItem.getMediaId() + "\"");
                }
                if (mediaItem.isBrowsable()) {
                    String str2 = "Not Provided";
                    if (mediaItem.getDescription().getTitle() != null) {
                        str2 = Util.toMetadata(mediaItem, true).title;
                    }
                    arrayList.add(new ListItem(new Folder(mediaItem.getMediaId(), false, str2)));
                } else {
                    arrayList.add(new ListItem(Util.toMetadata(mediaItem, false)));
                }
            }
            BrowsedPlayerWrapper.this.mCachedFolders.put(str, arrayList);
            this.mCallback.run(0, str, Util.cloneList(arrayList));
            this.mCallback = null;
            BrowsedPlayerWrapper.this.disconnect();
        }

        @Override
        public void onError(String str) {
            Log.e(BrowsedPlayerWrapper.TAG, "BrowserSubscriptionCallback: Could not get folder items");
            this.mCallback.run(2, str, new ArrayList());
            BrowsedPlayerWrapper.this.disconnect();
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Browsable Package Name: " + this.mPackageName + "\n");
        sb.append("   Cached Media ID's: ");
        Iterator<String> it = this.mCachedFolders.keySet().iterator();
        while (it.hasNext()) {
            sb.append("\"" + it.next() + "\", ");
        }
        sb.append("\n");
        return sb.toString();
    }
}
