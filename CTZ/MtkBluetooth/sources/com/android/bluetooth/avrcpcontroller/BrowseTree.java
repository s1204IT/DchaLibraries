package com.android.bluetooth.avrcpcontroller;

import android.media.MediaDescription;
import android.media.browse.MediaBrowser;
import android.os.Bundle;
import android.service.media.MediaBrowserService;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class BrowseTree {
    private static final boolean DBG = false;
    public static final int DIRECTION_DOWN = 0;
    public static final int DIRECTION_SAME = 2;
    public static final int DIRECTION_UNKNOWN = -1;
    public static final int DIRECTION_UP = 1;
    public static final String NOW_PLAYING_PREFIX = "NOW_PLAYING";
    public static final String PLAYER_PREFIX = "PLAYER";
    public static final String ROOT = "__ROOT__";
    private static final String TAG = "BrowseTree";
    private static final boolean VDBG = false;
    private final HashMap<String, BrowseNode> mBrowseMap = new HashMap<>();
    private BrowseNode mCurrentAddressedPlayer;
    private BrowseNode mCurrentBrowseNode;
    private BrowseNode mCurrentBrowsedPlayer;

    BrowseTree() {
    }

    public void init() {
        MediaDescription.Builder builder = new MediaDescription.Builder();
        builder.setMediaId(ROOT);
        builder.setTitle(ROOT);
        Bundle bundle = new Bundle();
        bundle.putString(AvrcpControllerService.MEDIA_ITEM_UID_KEY, ROOT);
        builder.setExtras(bundle);
        this.mBrowseMap.put(ROOT, new BrowseNode(new MediaBrowser.MediaItem(builder.build(), 1)));
        this.mCurrentBrowseNode = this.mBrowseMap.get(ROOT);
    }

    public void clear() {
        this.mBrowseMap.clear();
    }

    class BrowseNode {
        boolean mCached;
        final List<BrowseNode> mChildren;
        boolean mIsPlayer;
        MediaBrowser.MediaItem mItem;
        MediaBrowserService.Result<List<MediaBrowser.MediaItem>> mResult;

        BrowseNode(MediaBrowser.MediaItem mediaItem) {
            this.mIsPlayer = false;
            this.mCached = false;
            this.mResult = null;
            this.mChildren = new ArrayList();
            this.mItem = mediaItem;
        }

        BrowseNode(AvrcpPlayer avrcpPlayer) {
            this.mIsPlayer = false;
            this.mCached = false;
            this.mResult = null;
            this.mChildren = new ArrayList();
            this.mIsPlayer = true;
            MediaDescription.Builder builder = new MediaDescription.Builder();
            Bundle bundle = new Bundle();
            String str = BrowseTree.PLAYER_PREFIX + avrcpPlayer.getId();
            bundle.putString(AvrcpControllerService.MEDIA_ITEM_UID_KEY, str);
            builder.setExtras(bundle);
            builder.setMediaId(str);
            builder.setTitle(avrcpPlayer.getName());
            this.mItem = new MediaBrowser.MediaItem(builder.build(), 1);
        }

        synchronized List<BrowseNode> getChildren() {
            return this.mChildren;
        }

        synchronized boolean isChild(BrowseNode browseNode) {
            Iterator<BrowseNode> it = this.mChildren.iterator();
            while (it.hasNext()) {
                if (it.next().equals(browseNode)) {
                    return true;
                }
            }
            return false;
        }

        synchronized boolean isCached() {
            return this.mCached;
        }

        synchronized void setCached(boolean z) {
            this.mCached = z;
        }

        synchronized String getID() {
            return this.mItem.getDescription().getMediaId();
        }

        synchronized int getPlayerID() {
            return Integer.parseInt(getID().replace(BrowseTree.PLAYER_PREFIX, ""));
        }

        synchronized String getFolderUID() {
            return this.mItem.getDescription().getExtras().getString(AvrcpControllerService.MEDIA_ITEM_UID_KEY);
        }

        synchronized MediaBrowser.MediaItem getMediaItem() {
            return this.mItem;
        }

        synchronized boolean isPlayer() {
            return this.mIsPlayer;
        }

        synchronized boolean isNowPlaying() {
            return getID().startsWith(BrowseTree.NOW_PLAYING_PREFIX);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof BrowseNode)) {
                return false;
            }
            return getID().equals(obj.getID());
        }

        public String toString() {
            return "ID: " + getID();
        }
    }

    synchronized <E> void refreshChildren(String str, List<E> list) {
        BrowseNode browseNodeFindFolderByIDLocked = findFolderByIDLocked(str);
        if (browseNodeFindFolderByIDLocked == null) {
            Log.w(TAG, "parent not found for parentID " + str);
            return;
        }
        refreshChildren(browseNodeFindFolderByIDLocked, list);
    }

    synchronized <E> void refreshChildren(BrowseNode browseNode, List<E> list) {
        if (list == null) {
            Log.e(TAG, "children cannot be null ");
            return;
        }
        ArrayList arrayList = new ArrayList();
        for (E e : list) {
            if (e instanceof MediaBrowser.MediaItem) {
                arrayList.add(new BrowseNode((MediaBrowser.MediaItem) e));
            } else if (e instanceof AvrcpPlayer) {
                arrayList.add(new BrowseNode((AvrcpPlayer) e));
            }
        }
        browseNode.getID();
        addChildrenLocked(browseNode, arrayList);
        ArrayList arrayList2 = new ArrayList();
        Iterator<BrowseNode> it = browseNode.getChildren().iterator();
        while (it.hasNext()) {
            arrayList2.add(it.next().getMediaItem());
        }
        browseNode.setCached(true);
    }

    synchronized BrowseNode findBrowseNodeByID(String str) {
        BrowseNode browseNode = this.mBrowseMap.get(str);
        if (browseNode != null) {
            return browseNode;
        }
        Log.e(TAG, "folder " + str + " not found!");
        return null;
    }

    BrowseNode findFolderByIDLocked(String str) {
        return this.mBrowseMap.get(str);
    }

    void addChildrenLocked(BrowseNode browseNode, List<BrowseNode> list) {
        Iterator<BrowseNode> it = browseNode.getChildren().iterator();
        while (it.hasNext()) {
            this.mBrowseMap.remove(it.next().getID());
        }
        browseNode.getChildren().clear();
        for (BrowseNode browseNode2 : list) {
            browseNode.getChildren().add(browseNode2);
            this.mBrowseMap.put(browseNode2.getID(), browseNode2);
        }
    }

    synchronized int getDirection(String str) {
        BrowseNode browseNode = this.mCurrentBrowseNode;
        BrowseNode browseNodeFindFolderByIDLocked = findFolderByIDLocked(str);
        if (browseNode == null || browseNodeFindFolderByIDLocked == null) {
            Log.e(TAG, "from folder " + this.mCurrentBrowseNode + " or to folder " + str + " null!");
        }
        if (browseNode.isChild(browseNodeFindFolderByIDLocked)) {
            return 0;
        }
        if (browseNodeFindFolderByIDLocked.isChild(browseNode)) {
            return 1;
        }
        if (browseNode.equals(browseNodeFindFolderByIDLocked)) {
            return 2;
        }
        Log.w(TAG, "from folder " + this.mCurrentBrowseNode + "to folder " + str);
        return -1;
    }

    synchronized boolean setCurrentBrowsedFolder(String str) {
        BrowseNode browseNodeFindFolderByIDLocked = findFolderByIDLocked(str);
        if (browseNodeFindFolderByIDLocked == null) {
            Log.e(TAG, "Setting an unknown browsed folder, ignoring bn " + str);
            return false;
        }
        if (!browseNodeFindFolderByIDLocked.equals(this.mCurrentBrowseNode)) {
            Log.d(TAG, "Set cache false " + browseNodeFindFolderByIDLocked + " curr " + this.mCurrentBrowseNode);
            this.mCurrentBrowseNode.setCached(false);
        }
        this.mCurrentBrowseNode = browseNodeFindFolderByIDLocked;
        return true;
    }

    synchronized BrowseNode getCurrentBrowsedFolder() {
        return this.mCurrentBrowseNode;
    }

    synchronized boolean setCurrentBrowsedPlayer(String str) {
        BrowseNode browseNodeFindFolderByIDLocked = findFolderByIDLocked(str);
        if (browseNodeFindFolderByIDLocked == null) {
            Log.e(TAG, "Setting an unknown browsed player, ignoring bn " + str);
            return false;
        }
        this.mCurrentBrowsedPlayer = browseNodeFindFolderByIDLocked;
        return true;
    }

    synchronized BrowseNode getCurrentBrowsedPlayer() {
        return this.mCurrentBrowsedPlayer;
    }

    synchronized boolean setCurrentAddressedPlayer(String str) {
        BrowseNode browseNodeFindFolderByIDLocked = findFolderByIDLocked(str);
        if (browseNodeFindFolderByIDLocked == null) {
            Log.e(TAG, "Setting an unknown addressed player, ignoring bn " + str);
            return false;
        }
        this.mCurrentAddressedPlayer = browseNodeFindFolderByIDLocked;
        return true;
    }

    synchronized BrowseNode getCurrentAddressedPlayer() {
        return this.mCurrentAddressedPlayer;
    }

    public String toString() {
        return this.mBrowseMap.toString();
    }
}
