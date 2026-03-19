package com.mediatek.gallerybasic.dynamic;

import com.mediatek.gallerybasic.base.MediaData;
import com.mediatek.gallerybasic.base.Player;
import com.mediatek.gallerybasic.util.DebugUtils;
import com.mediatek.gallerybasic.util.Log;

class PlayList {
    static final boolean $assertionsDisabled = false;
    public static final int INVALIDE = -1;
    private static final String TAG = "MtkGallery2/PlayList";
    private EntryFilling mFilling;
    private Entry[] mList;
    private Entry[] mReleaseList;

    public interface EntryFilling {
        void fillEntry(int i, Entry entry);

        void updateEntry(int i, Entry entry);
    }

    public static class Entry {
        public MediaData data;
        public Player player;
        public int threadIndex = -1;

        public String toString() {
            return "[ data = " + this.data + ", player = " + this.player + ", threadIndex = " + this.threadIndex + "]";
        }
    }

    public PlayList(int i, EntryFilling entryFilling) {
        this.mList = new Entry[i];
        this.mFilling = entryFilling;
    }

    public Entry get(int i) {
        return this.mList[i];
    }

    public void update(MediaData[] mediaDataArr) {
        this.mReleaseList = this.mList;
        this.mList = new Entry[this.mReleaseList.length];
        for (int i = 0; i < mediaDataArr.length; i++) {
            if (mediaDataArr[i] == null) {
                this.mList[i] = new Entry();
                this.mFilling.updateEntry(i, this.mList[i]);
            } else {
                int iFindEntryByMediaData = findEntryByMediaData(this.mReleaseList, mediaDataArr[i]);
                if (iFindEntryByMediaData == -1) {
                    this.mList[i] = new Entry();
                    this.mList[i].data = mediaDataArr[i];
                    this.mFilling.fillEntry(i, this.mList[i]);
                } else {
                    this.mList[i] = this.mReleaseList[iFindEntryByMediaData];
                    this.mFilling.updateEntry(i, this.mList[i]);
                    this.mReleaseList[iFindEntryByMediaData] = null;
                }
            }
        }
        logEntrys("<After update, mList>", this.mList);
        logEntrys("<After update, mReleaseList>", this.mReleaseList);
    }

    public final Entry[] getReleaseList() {
        return this.mReleaseList;
    }

    public final Entry[] getList() {
        return this.mList;
    }

    private static int findEntryByMediaData(Entry[] entryArr, MediaData mediaData) {
        int length = entryArr.length;
        for (int i = 0; i < length; i++) {
            if (entryArr[i] != null && entryArr[i].data != null && entryArr[i].data.equals(mediaData)) {
                return i;
            }
        }
        return -1;
    }

    private void logEntrys(String str, Entry[] entryArr) {
        if (!DebugUtils.DEBUG_PLAY_ENGINE) {
            return;
        }
        Log.d(TAG, str + " begin ----------------------------------------");
        for (int i = 0; i < entryArr.length; i++) {
            Log.d(TAG, str + " [" + i + "] = " + entryArr[i]);
        }
        Log.d(TAG, str + " end ----------------------------------------");
    }
}
