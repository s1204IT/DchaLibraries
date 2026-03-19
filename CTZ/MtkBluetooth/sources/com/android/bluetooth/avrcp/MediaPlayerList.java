package com.android.bluetooth.avrcp;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemProperties;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.util.Log;
import android.view.KeyEvent;
import com.android.bluetooth.Utils;
import com.android.bluetooth.avrcp.AvrcpTargetService;
import com.android.bluetooth.avrcp.BrowsablePlayerConnector;
import com.android.bluetooth.avrcp.BrowsedPlayerWrapper;
import com.android.bluetooth.avrcp.MediaPlayerWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaPlayerList {
    private static final int BLUETOOTH_PLAYER_ID = 0;
    private static final String BLUETOOTH_PLAYER_NAME = "Bluetooth Player";
    private static final String BROWSE_ID_PATTERN = "\\d\\d.*";
    private static final int MESSAGE_CHANGE_SONG_KEY_ONE_SECOND = 100;
    private static final String NOW_PLAYING_ID_PATTERN = "NowPlayingId([0-9]*)";
    private static final int NO_ACTIVE_PLAYER = 0;
    private static final String PACKAGE_SCHEME = "package";
    private static final String TAG = "NewAvrcpMediaPlayerList";
    private static final int TIMEOUT_MS = 2000;
    private AudioManager mAudioManager;
    private AvrcpTargetService.ListCallback mCallback;
    private Context mContext;
    private Handler mHandler;
    private int mKeyDownTime;
    private Looper mLooper;
    private MediaSessionManager mMediaSessionManager;
    private boolean mNextPreviousPressed;
    private PackageManager mPackageManager;
    private static final boolean DEBUG = SystemProperties.get("persist.vendor.bluetooth.hostloglevel", "").equals("sqc");
    static boolean sTesting = false;
    private Map<Integer, MediaPlayerWrapper> mMediaPlayers = Collections.synchronizedMap(new HashMap());
    private Map<String, Integer> mMediaPlayerIds = Collections.synchronizedMap(new HashMap());
    private Map<Integer, BrowsedPlayerWrapper> mBrowsablePlayers = Collections.synchronizedMap(new HashMap());
    private int mActivePlayerId = 0;
    private final MediaSessionManager.OnActiveSessionsChangedListener mActiveSessionsChangedListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
        @Override
        public void onActiveSessionsChanged(List<android.media.session.MediaController> list) {
            synchronized (MediaPlayerList.this) {
                Log.v(MediaPlayerList.TAG, "onActiveSessionsChanged: number of controllers: " + list.size());
                if (list.size() == 0) {
                    return;
                }
                HashSet hashSet = new HashSet();
                for (int i = 0; i < list.size(); i++) {
                    Log.d(MediaPlayerList.TAG, "onActiveSessionsChanged: controller: " + list.get(i).getPackageName());
                    if (!hashSet.contains(list.get(i).getPackageName())) {
                        hashSet.add(list.get(i).getPackageName());
                        MediaPlayerList.this.addMediaPlayer(list.get(i));
                    }
                }
            }
        }
    };
    private final BroadcastReceiver mPackageChangedBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String schemeSpecificPart;
            String schemeSpecificPart2;
            String action = intent.getAction();
            Log.v(MediaPlayerList.TAG, "mPackageChangedBroadcastReceiver: action: " + action);
            if (action.equals("android.intent.action.PACKAGE_REMOVED") || action.equals("android.intent.action.PACKAGE_DATA_CLEARED")) {
                if (!intent.getBooleanExtra("android.intent.extra.REPLACING", false) && (schemeSpecificPart = intent.getData().getSchemeSpecificPart()) != null && MediaPlayerList.this.mMediaPlayerIds != null && MediaPlayerList.this.mMediaPlayerIds.containsKey(schemeSpecificPart)) {
                    MediaPlayerList.this.removeMediaPlayer(((Integer) MediaPlayerList.this.mMediaPlayerIds.get(schemeSpecificPart)).intValue());
                    return;
                }
                return;
            }
            if ((action.equals("android.intent.action.PACKAGE_ADDED") || action.equals("android.intent.action.PACKAGE_CHANGED")) && (schemeSpecificPart2 = intent.getData().getSchemeSpecificPart()) != null) {
                if (MediaPlayerList.DEBUG) {
                    Log.d(MediaPlayerList.TAG, "Name of package changed: " + schemeSpecificPart2);
                }
                MediaPlayerList.this.buildMediaPlayerList();
            }
        }
    };
    private final MediaPlayerWrapper.Callback mMediaPlayerCallback = new MediaPlayerWrapper.Callback() {
        @Override
        public void mediaUpdatedCallback(MediaData mediaData) {
            if (mediaData.metadata == null) {
                Log.d(MediaPlayerList.TAG, "mediaUpdatedCallback(): metadata is null");
            } else if (mediaData.state != null) {
                MediaPlayerList.this.sendMediaUpdate(mediaData);
            } else {
                Log.w(MediaPlayerList.TAG, "mediaUpdatedCallback(): Tried to update with null state");
            }
        }
    };
    private final MediaSessionManager.Callback mButtonDispatchCallback = new MediaSessionManager.Callback() {
        public void onMediaKeyEventDispatched(KeyEvent keyEvent, MediaSession.Token token) {
        }

        public void onMediaKeyEventDispatched(KeyEvent keyEvent, ComponentName componentName) {
        }

        public void onAddressedPlayerChanged(MediaSession.Token token) {
            android.media.session.MediaController mediaController = new android.media.session.MediaController(MediaPlayerList.this.mContext, token);
            if (!MediaPlayerList.this.mMediaPlayerIds.containsKey(mediaController.getPackageName())) {
                Log.w(MediaPlayerList.TAG, "onAddressedPlayerChanged(Token): Addressed Player changed to a player we didn't have a session for");
                if (MediaPlayerList.this.addMediaPlayer(mediaController) == -1) {
                    Log.d(MediaPlayerList.TAG, "Fail to add media player: " + mediaController.getPackageName());
                    return;
                }
            }
            Log.i(MediaPlayerList.TAG, "onAddressedPlayerChanged: token=" + mediaController.getPackageName());
            if (MediaPlayerList.this.mMediaPlayerIds == null) {
                return;
            }
            MediaPlayerList.this.setActivePlayer(((Integer) MediaPlayerList.this.mMediaPlayerIds.get(mediaController.getPackageName())).intValue());
        }

        public void onAddressedPlayerChanged(ComponentName componentName) {
            if (componentName != null) {
                if (MediaPlayerList.this.mMediaPlayerIds == null || !MediaPlayerList.this.mMediaPlayerIds.containsKey(componentName.getPackageName())) {
                    MediaPlayerList.e("onAddressedPlayerChanged(Component): Addressed Player changed to a player we don't have a session for");
                    return;
                }
                Log.i(MediaPlayerList.TAG, "onAddressedPlayerChanged: component=" + componentName.getPackageName());
                MediaPlayerList.this.setActivePlayer(((Integer) MediaPlayerList.this.mMediaPlayerIds.get(componentName.getPackageName())).intValue());
            }
        }
    };

    interface FolderUpdateCallback {
        void run(boolean z, boolean z2, boolean z3);
    }

    interface GetFolderItemsCallback {
        void run(String str, List<ListItem> list);
    }

    interface GetPlayerRootCallback {
        void run(int i, boolean z, String str, int i2);
    }

    interface MediaUpdateCallback {
        void run(MediaData mediaData);
    }

    MediaPlayerList(Looper looper, Context context) {
        Log.v(TAG, "Creating MediaPlayerList");
        this.mLooper = looper;
        this.mContext = context;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_DATA_CLEARED");
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme(PACKAGE_SCHEME);
        context.registerReceiver(this.mPackageChangedBroadcastReceiver, intentFilter);
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mKeyDownTime = 0;
        this.mMediaSessionManager = (MediaSessionManager) context.getSystemService("media_session");
        this.mMediaSessionManager.addOnActiveSessionsChangedListener(this.mActiveSessionsChangedListener, null, new Handler(looper));
        this.mMediaSessionManager.setCallback(this.mButtonDispatchCallback, null);
        this.mNextPreviousPressed = false;
        this.mHandler = new MessageHandler(looper);
    }

    void init(AvrcpTargetService.ListCallback listCallback) {
        Log.v(TAG, "Initializing MediaPlayerList");
        this.mCallback = listCallback;
        buildMediaPlayerList();
    }

    void cleanup() {
        this.mContext.unregisterReceiver(this.mPackageChangedBroadcastReceiver);
        this.mMediaSessionManager.removeOnActiveSessionsChangedListener(this.mActiveSessionsChangedListener);
        this.mMediaSessionManager.setCallback(null, null);
        this.mMediaSessionManager = null;
        this.mMediaPlayerIds.clear();
        Iterator<MediaPlayerWrapper> it = this.mMediaPlayers.values().iterator();
        while (it.hasNext()) {
            it.next().cleanup();
        }
        this.mMediaPlayers.clear();
        Iterator<BrowsedPlayerWrapper> it2 = this.mBrowsablePlayers.values().iterator();
        while (it2.hasNext()) {
            it2.next().disconnect();
        }
        this.mBrowsablePlayers.clear();
    }

    private final class MessageHandler extends Handler {
        private static final int MESSAGE_CHANGE_SONG_KEY_ONE_SECOND = 100;

        MessageHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 100) {
                Log.v(MediaPlayerList.TAG, "MESSAGE_CHANGE_SONG_KEY_ONE_SECOND reset mNextPreviousPressed");
                MediaPlayerList.this.mNextPreviousPressed = false;
            } else {
                Log.w(MediaPlayerList.TAG, "Unknown message on timeout handler: " + message.what);
            }
        }
    }

    int getCurrentPlayerId() {
        return 0;
    }

    MediaPlayerWrapper getActivePlayer() {
        return this.mMediaPlayers.get(Integer.valueOf(this.mActivePlayerId));
    }

    void getPlayerRoot(final int i, final GetPlayerRootCallback getPlayerRootCallback) {
        if (Utils.isPtsTestMode()) {
            d("PTS test mode: getPlayerRoot");
            BrowsedPlayerWrapper browsedPlayerWrapper = this.mBrowsablePlayers.get(1);
            browsedPlayerWrapper.getFolderItems(browsedPlayerWrapper.getRootId(), new BrowsedPlayerWrapper.BrowseCallback() {
                @Override
                public final void run(int i2, String str, List list) {
                    MediaPlayerList.lambda$getPlayerRoot$0(getPlayerRootCallback, i, i2, str, list);
                }
            });
            return;
        }
        getPlayerRootCallback.run(i, i == 0, "", this.mBrowsablePlayers.size());
    }

    static void lambda$getPlayerRoot$0(GetPlayerRootCallback getPlayerRootCallback, int i, int i2, String str, List list) {
        if (i2 != 0) {
            getPlayerRootCallback.run(i, i == 0, "", 0);
        } else {
            getPlayerRootCallback.run(i, i == 0, "", list.size());
        }
    }

    List<PlayerInfo> getMediaPlayerList() {
        if (Utils.isPtsTestMode()) {
            d("PTS test mode: getMediaPlayerList");
            ArrayList arrayList = new ArrayList();
            Iterator<BrowsedPlayerWrapper> it = this.mBrowsablePlayers.values().iterator();
            if (it.hasNext()) {
                BrowsedPlayerWrapper next = it.next();
                PlayerInfo playerInfo = new PlayerInfo();
                playerInfo.name = Util.getDisplayName(this.mContext, next.getPackageName());
                playerInfo.id = 0;
                playerInfo.browsable = true;
                arrayList.add(playerInfo);
            }
            return arrayList;
        }
        PlayerInfo playerInfo2 = new PlayerInfo();
        playerInfo2.id = 0;
        playerInfo2.name = BLUETOOTH_PLAYER_NAME;
        playerInfo2.browsable = true;
        ArrayList arrayList2 = new ArrayList();
        arrayList2.add(playerInfo2);
        return arrayList2;
    }

    String getCurrentMediaId() {
        MediaPlayerWrapper activePlayer = getActivePlayer();
        if (activePlayer == null) {
            return "";
        }
        PlaybackState playbackState = activePlayer.getPlaybackState();
        List<Metadata> currentQueue = activePlayer.getCurrentQueue();
        if (playbackState == null || playbackState.getActiveQueueItemId() == -1 || currentQueue.size() == 0) {
            d("getCurrentMediaId: No active queue item Id sending empty mediaId: PlaybackState=" + playbackState);
            return "";
        }
        if (Utils.isPtsTestMode() && playbackState.getState() != 3) {
            d("PTS test mode: Sending empty mediaId: PlaybackState=" + playbackState);
            return "";
        }
        return Util.NOW_PLAYING_PREFIX + playbackState.getActiveQueueItemId();
    }

    Metadata getCurrentSongInfo() {
        MediaPlayerWrapper activePlayer = getActivePlayer();
        if (activePlayer == null) {
            return Util.empty_data();
        }
        Metadata currentMetadata = activePlayer.getCurrentMetadata();
        if (activePlayer.getPackageName().equals("com.kugou.android")) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            while (currentMetadata.duration.equals("0")) {
                long jCurrentTimeMillis2 = System.currentTimeMillis() - jCurrentTimeMillis;
                currentMetadata = activePlayer.getCurrentMetadata();
                if (!currentMetadata.duration.equals("0") || jCurrentTimeMillis2 > 2000) {
                    d("Kugou app will send duration " + currentMetadata.duration);
                    return currentMetadata;
                }
            }
        }
        return currentMetadata;
    }

    PlaybackState getCurrentPlayStatus() {
        MediaPlayerWrapper activePlayer = getActivePlayer();
        if (activePlayer == null) {
            return null;
        }
        return activePlayer.getPlaybackState();
    }

    List<Metadata> getNowPlayingList() {
        if (getCurrentMediaId().equals("")) {
            ArrayList arrayList = new ArrayList();
            Metadata currentSongInfo = getCurrentSongInfo();
            currentSongInfo.mediaId = "";
            arrayList.add(currentSongInfo);
            return arrayList;
        }
        return getActivePlayer().getCurrentQueue();
    }

    void playItem(int i, boolean z, String str) {
        if (z) {
            playNowPlayingItem(str);
        } else {
            playFolderItem(str);
        }
    }

    private void playNowPlayingItem(String str) {
        d("playNowPlayingItem: mediaId=" + str);
        Matcher matcher = Pattern.compile(NOW_PLAYING_ID_PATTERN).matcher(str);
        if (!matcher.find()) {
            Log.wtf(TAG, "playNowPlayingItem: Couldn't match mediaId to pattern: mediaId=" + str);
        }
        long j = Long.parseLong(matcher.group(1));
        if (getActivePlayer() != null) {
            getActivePlayer().playItemFromQueue(j);
        }
    }

    private void playFolderItem(String str) {
        d("playFolderItem: mediaId=" + str);
        if (Utils.isPtsTestMode()) {
            d("PTS test mode: playFolderItem");
            if (!this.mBrowsablePlayers.containsKey(1)) {
                e("PTS Test mode: playFolderItem: Do not have the a browsable player");
                return;
            } else {
                this.mBrowsablePlayers.get(1).playItem(str);
                return;
            }
        }
        if (!str.matches(BROWSE_ID_PATTERN)) {
            Log.wtf(TAG, "playFolderItem: mediaId didn't match pattern: mediaId=" + str);
        }
        int i = Integer.parseInt(str.substring(0, 2));
        String strSubstring = str.substring(2);
        if (!this.mBrowsablePlayers.containsKey(Integer.valueOf(i))) {
            e("playFolderItem: Do not have the a browsable player with ID " + i);
            return;
        }
        this.mBrowsablePlayers.get(Integer.valueOf(i)).playItem(strSubstring);
    }

    void getFolderItemsMediaPlayerList(GetFolderItemsCallback getFolderItemsCallback) {
        d("getFolderItemsMediaPlayerList: Sending Media Player list for root directory");
        ArrayList arrayList = new ArrayList();
        for (BrowsedPlayerWrapper browsedPlayerWrapper : this.mBrowsablePlayers.values()) {
            String displayName = Util.getDisplayName(this.mContext, browsedPlayerWrapper.getPackageName());
            int iIntValue = this.mMediaPlayerIds.get(browsedPlayerWrapper.getPackageName()).intValue();
            d("getFolderItemsMediaPlayerList: Adding player " + displayName);
            arrayList.add(new ListItem(new Folder(String.format("%02d", Integer.valueOf(iIntValue)), false, displayName)));
        }
        getFolderItemsCallback.run("", arrayList);
    }

    void getFolderItems(int i, final String str, final GetFolderItemsCallback getFolderItemsCallback) {
        String rootId;
        d("getFolderItems(): playerId=" + i + ", mediaId=" + str);
        if (Utils.isPtsTestMode()) {
            d("PTS test mode: getFolderItems");
            BrowsedPlayerWrapper browsedPlayerWrapper = this.mBrowsablePlayers.get(1);
            if (str.equals("")) {
                rootId = browsedPlayerWrapper.getRootId();
            } else {
                rootId = str;
            }
            browsedPlayerWrapper.getFolderItems(rootId, new BrowsedPlayerWrapper.BrowseCallback() {
                @Override
                public final void run(int i2, String str2, List list) {
                    MediaPlayerList.lambda$getFolderItems$1(getFolderItemsCallback, str, i2, str2, list);
                }
            });
            return;
        }
        if (str.equals("")) {
            getFolderItemsMediaPlayerList(getFolderItemsCallback);
            return;
        }
        if (!str.matches(BROWSE_ID_PATTERN)) {
            Log.wtf(TAG, "getFolderItems: mediaId didn't match pattern: mediaId=" + str);
        }
        final int i2 = Integer.parseInt(str.substring(0, 2));
        String strSubstring = str.substring(2);
        if (this.mBrowsablePlayers.containsKey(Integer.valueOf(i2))) {
            BrowsedPlayerWrapper browsedPlayerWrapper2 = this.mBrowsablePlayers.get(Integer.valueOf(i2));
            if (strSubstring.equals("")) {
                Log.i(TAG, "Empty media id, getting the root for " + browsedPlayerWrapper2.getPackageName());
                strSubstring = browsedPlayerWrapper2.getRootId();
            }
            browsedPlayerWrapper2.getFolderItems(strSubstring, new BrowsedPlayerWrapper.BrowseCallback() {
                @Override
                public final void run(int i3, String str2, List list) {
                    MediaPlayerList.lambda$getFolderItems$2(getFolderItemsCallback, str, i2, i3, str2, list);
                }
            });
            return;
        }
        getFolderItemsCallback.run(str, new ArrayList());
    }

    static void lambda$getFolderItems$1(GetFolderItemsCallback getFolderItemsCallback, String str, int i, String str2, List list) {
        if (i != 0) {
            getFolderItemsCallback.run(str, new ArrayList());
        } else {
            getFolderItemsCallback.run(str, list);
        }
    }

    static void lambda$getFolderItems$2(GetFolderItemsCallback getFolderItemsCallback, String str, int i, int i2, String str2, List list) {
        if (i2 != 0) {
            getFolderItemsCallback.run(str, new ArrayList());
            return;
        }
        String str3 = String.format("%02d", Integer.valueOf(i));
        Iterator it = list.iterator();
        while (it.hasNext()) {
            ListItem listItem = (ListItem) it.next();
            if (listItem.isFolder) {
                listItem.folder.mediaId = str3.concat(listItem.folder.mediaId);
            } else {
                listItem.song.mediaId = str3.concat(listItem.song.mediaId);
            }
        }
        getFolderItemsCallback.run(str, list);
    }

    int addMediaPlayer(android.media.session.MediaController mediaController) {
        if (mediaController == null) {
            return -1;
        }
        String packageName = mediaController.getPackageName();
        if (packageName.equals("com.android.server.telecom")) {
            Log.d(TAG, "Skip adding telecom to the media player list");
            return -1;
        }
        if (!this.mMediaPlayerIds.containsKey(packageName)) {
            this.mMediaPlayerIds.put(packageName, Integer.valueOf(this.mMediaPlayerIds.size() + 1));
        }
        int iIntValue = this.mMediaPlayerIds.get(packageName).intValue();
        if (this.mMediaPlayers.containsKey(Integer.valueOf(iIntValue))) {
            d("Already have a controller for the player: " + packageName + ", updating instead");
            this.mMediaPlayers.get(Integer.valueOf(iIntValue)).updateMediaController(MediaControllerFactory.wrap(mediaController));
            if (iIntValue == this.mActivePlayerId) {
                sendMediaUpdate(getActivePlayer().getCurrentMediaData());
            }
            return iIntValue;
        }
        MediaPlayerWrapper mediaPlayerWrapperWrap = MediaPlayerWrapper.wrap(MediaControllerFactory.wrap(mediaController), this.mLooper);
        Log.i(TAG, "Adding wrapped media player: " + packageName + " at key: " + this.mMediaPlayerIds.get(mediaController.getPackageName()));
        this.mMediaPlayers.put(Integer.valueOf(iIntValue), mediaPlayerWrapperWrap);
        return iIntValue;
    }

    void removeMediaPlayer(int i) {
        if (!this.mMediaPlayers.containsKey(Integer.valueOf(i))) {
            e("Trying to remove nonexistent media player: " + i);
            return;
        }
        if (i == this.mActivePlayerId && i != 0) {
            getActivePlayer().unregisterCallback();
            this.mActivePlayerId = 0;
        }
        MediaPlayerWrapper mediaPlayerWrapper = this.mMediaPlayers.get(Integer.valueOf(i));
        d("Removing media player " + mediaPlayerWrapper.getPackageName());
        this.mMediaPlayerIds.remove(mediaPlayerWrapper.getPackageName());
        this.mMediaPlayers.remove(Integer.valueOf(i));
        mediaPlayerWrapper.cleanup();
    }

    private void buildMediaPlayerList() {
        BrowsablePlayerConnector.connectToPlayers(this.mContext, this.mLooper, this.mContext.getApplicationContext().getPackageManager().queryIntentServices(new Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE), 131072), new BrowsablePlayerConnector.PlayerListCallback() {
            @Override
            public final void run(List list) {
                MediaPlayerList.lambda$buildMediaPlayerList$4(this.f$0, list);
            }
        });
    }

    public static void lambda$buildMediaPlayerList$4(MediaPlayerList mediaPlayerList, List list) {
        Log.i(TAG, "init: Browsable Player list size is " + list.size());
        if (mediaPlayerList.mMediaSessionManager == null) {
            return;
        }
        Iterator it = list.iterator();
        while (it.hasNext()) {
            BrowsedPlayerWrapper browsedPlayerWrapper = (BrowsedPlayerWrapper) it.next();
            if (!mediaPlayerList.mMediaPlayerIds.containsKey(browsedPlayerWrapper.getPackageName())) {
                mediaPlayerList.mMediaPlayerIds.put(browsedPlayerWrapper.getPackageName(), Integer.valueOf(mediaPlayerList.mMediaPlayerIds.size() + 1));
            }
            d("Adding Browser Wrapper for " + browsedPlayerWrapper.getPackageName() + " with id " + mediaPlayerList.mMediaPlayerIds.get(browsedPlayerWrapper.getPackageName()));
            mediaPlayerList.mBrowsablePlayers.put(mediaPlayerList.mMediaPlayerIds.get(browsedPlayerWrapper.getPackageName()), browsedPlayerWrapper);
            browsedPlayerWrapper.getFolderItems(browsedPlayerWrapper.getRootId(), new BrowsedPlayerWrapper.BrowseCallback() {
                @Override
                public final void run(int i, String str, List list2) {
                    MediaPlayerList.d("Got the contents for: " + str + " : num results=" + list2.size());
                }
            });
        }
        d("Initializing list of current media players");
        Iterator<android.media.session.MediaController> it2 = mediaPlayerList.mMediaSessionManager.getActiveSessions(null).iterator();
        while (it2.hasNext()) {
            mediaPlayerList.addMediaPlayer(it2.next());
        }
        if (mediaPlayerList.mActivePlayerId != 0 || mediaPlayerList.mMediaPlayers.size() <= 0) {
            return;
        }
        mediaPlayerList.setActivePlayer(1);
    }

    void setActivePlayer(int i) {
        synchronized (this.mMediaPlayers) {
            if (!this.mMediaPlayers.containsKey(Integer.valueOf(i))) {
                e("Player doesn't exist in list(): " + i);
                return;
            }
            if (i == this.mActivePlayerId) {
                Log.w(TAG, getActivePlayer().getPackageName() + " is already the active player");
                return;
            }
            if (this.mActivePlayerId != 0 && getActivePlayer() != null) {
                getActivePlayer().unregisterCallback();
            }
            this.mActivePlayerId = i;
            getActivePlayer().registerCallback(this.mMediaPlayerCallback);
            Log.i(TAG, "setActivePlayer(): setting player to " + getActivePlayer().getPackageName());
            if (!getActivePlayer().isMetadataSynced()) {
                Log.w(TAG, "setActivePlayer(): Metadata not synced on new player");
                return;
            }
            if (Utils.isPtsTestMode()) {
                sendFolderUpdate(true, true, false);
            }
            sendMediaUpdate(getActivePlayer().getCurrentMediaData());
        }
    }

    void sendMediaKeyEvent(int i, boolean z) {
        d("sendMediaKeyEvent: key=" + i + " pushed=" + z);
        int i2 = !z ? 1 : 0;
        KeyEvent keyEvent = new KeyEvent(i2, AvrcpPassthrough.toKeyCode(i));
        if (i2 == 0) {
            this.mKeyDownTime = 1;
        } else {
            if (this.mKeyDownTime == 0) {
                Log.w(TAG, "Ignoring unexpected passthrough up key");
                return;
            }
            this.mKeyDownTime = 0;
        }
        boolean zIsMusicActive = this.mAudioManager.isMusicActive();
        boolean zEquals = getActivePlayer().getPackageName().equals("com.tencent.qqmusic");
        if ((!zEquals && zIsMusicActive && keyEvent.getKeyCode() == 126) || this.mNextPreviousPressed) {
            Log.w(TAG, "Ignoring KEYCODE_MEDIA_PLAY when state is PLAYING");
            return;
        }
        if (!zEquals && !zIsMusicActive && keyEvent.getKeyCode() == 127) {
            Log.w(TAG, "Ignoring KEYCODE_MEDIA_PAUSE when state is PAUSE");
            return;
        }
        if ((keyEvent.getKeyCode() == 88 || keyEvent.getKeyCode() == 87) && i2 == 1) {
            Message messageObtainMessage = this.mHandler.obtainMessage();
            this.mNextPreviousPressed = true;
            messageObtainMessage.what = 100;
            this.mHandler.sendMessageDelayed(messageObtainMessage, 1000L);
        }
        this.mMediaSessionManager.dispatchMediaKeyEvent(keyEvent);
    }

    private void sendFolderUpdate(boolean z, boolean z2, boolean z3) {
        d("sendFolderUpdate");
        if (this.mCallback == null) {
            return;
        }
        this.mCallback.run(z, z2, z3);
    }

    private void sendMediaUpdate(MediaData mediaData) {
        d("sendMediaUpdate");
        if (this.mCallback == null) {
            return;
        }
        if (mediaData.queue.size() == 0) {
            Log.i(TAG, "sendMediaUpdate: Creating a one item queue for a player with no queue");
            mediaData.queue.add(mediaData.metadata);
        }
        this.mCallback.run(mediaData);
    }

    void dump(StringBuilder sb) {
        sb.append("List of MediaControllers: size=" + this.mMediaPlayers.size() + "\n");
        Iterator<Integer> it = this.mMediaPlayers.keySet().iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            if (iIntValue == this.mActivePlayerId) {
                sb.append("<Active> ");
            }
            MediaPlayerWrapper mediaPlayerWrapper = this.mMediaPlayers.get(Integer.valueOf(iIntValue));
            sb.append("  Media Player " + iIntValue + ": " + mediaPlayerWrapper.getPackageName() + "\n");
            sb.append(mediaPlayerWrapper.toString().replaceAll("(?m)^", "  "));
            sb.append("\n");
        }
        sb.append("List of Browsers: size=" + this.mBrowsablePlayers.size() + "\n");
        Iterator<BrowsedPlayerWrapper> it2 = this.mBrowsablePlayers.values().iterator();
        while (it2.hasNext()) {
            sb.append(it2.next().toString().replaceAll("(?m)^", "  "));
            sb.append("\n");
        }
    }

    private static void e(String str) {
        if (sTesting) {
            Log.wtfStack(TAG, str);
        } else {
            Log.e(TAG, str);
        }
    }

    private static void d(String str) {
        if (DEBUG) {
            Log.d(TAG, str);
        }
    }
}
