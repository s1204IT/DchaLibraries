package com.android.gallery3d.data;

import android.net.Uri;
import android.provider.MediaStore;
import com.android.gallery3d.common.ApiHelper;
import com.android.gallery3d.common.Utils;
import com.mediatek.plugin.preload.SoOperater;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

public class LocalMergeAlbum extends MediaSet implements ContentListener {
    private int PAGE_SIZE;
    private int mBucketId;
    private final Comparator<MediaItem> mComparator;
    private MediaItem mCoverCache;
    private Object mCoverCacheLock;
    private FetchCache[] mFetcher;
    private TreeMap<Integer, int[]> mIndex;
    private final MediaSet[] mSources;
    private int mSupportedOperation;

    public LocalMergeAlbum(Path path, Comparator<MediaItem> comparator, MediaSet[] mediaSetArr, int i) {
        super(path, -1L);
        this.PAGE_SIZE = 0;
        this.mIndex = new TreeMap<>();
        this.mCoverCacheLock = new Object();
        this.mComparator = comparator;
        this.mSources = mediaSetArr;
        this.mBucketId = i;
        for (MediaSet mediaSet : this.mSources) {
            mediaSet.addContentListener(this);
        }
        reload();
    }

    @Override
    public boolean isCameraRoll() {
        if (this.mSources.length == 0) {
            return false;
        }
        for (MediaSet mediaSet : this.mSources) {
            if (!mediaSet.isCameraRoll()) {
                return false;
            }
        }
        return true;
    }

    private synchronized void updateData() {
        int i;
        new ArrayList();
        if (this.mSources.length != 0) {
            i = -1;
        } else {
            i = 0;
        }
        this.mFetcher = new FetchCache[this.mSources.length];
        int length = this.mSources.length;
        int supportedOperations = i;
        for (int i2 = 0; i2 < length; i2++) {
            this.mFetcher[i2] = new FetchCache(this.mSources[i2]);
            supportedOperations &= this.mSources[i2].getSupportedOperations();
        }
        this.mSupportedOperation = supportedOperations;
        this.mIndex.clear();
        this.mIndex.put(0, new int[this.mSources.length]);
    }

    private synchronized void invalidateCache() {
        int length = this.mSources.length;
        for (int i = 0; i < length; i++) {
            this.mFetcher[i].invalidate();
        }
        this.mIndex.clear();
        this.mIndex.put(0, new int[this.mSources.length]);
        synchronized (this.mCoverCacheLock) {
            this.mCoverCache = null;
        }
    }

    @Override
    public Uri getContentUri() {
        String strValueOf = String.valueOf(this.mBucketId);
        if (ApiHelper.HAS_MEDIA_PROVIDER_FILES_TABLE) {
            return MediaStore.Files.getContentUri("external").buildUpon().appendQueryParameter("bucketId", strValueOf).build();
        }
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendQueryParameter("bucketId", strValueOf).build();
    }

    @Override
    public String getName() {
        return this.mSources.length == 0 ? "" : this.mSources[0].getName();
    }

    @Override
    public int getMediaItemCount() {
        return getTotalMediaItemCount();
    }

    @Override
    public synchronized ArrayList<MediaItem> getMediaItem(int i, int i2) {
        ArrayList<MediaItem> arrayList;
        if (this.PAGE_SIZE == 0) {
            this.PAGE_SIZE = adjustPageSize();
        }
        SortedMap<Integer, int[]> sortedMapHeadMap = this.mIndex.headMap(Integer.valueOf(i + 1));
        int iIntValue = sortedMapHeadMap.lastKey().intValue();
        int[] iArr = (int[]) sortedMapHeadMap.get(Integer.valueOf(iIntValue)).clone();
        MediaItem[] mediaItemArr = new MediaItem[this.mSources.length];
        int length = this.mSources.length;
        for (int i3 = 0; i3 < length; i3++) {
            mediaItemArr[i3] = this.mFetcher[i3].getItem(iArr[i3]);
        }
        arrayList = new ArrayList<>();
        while (iIntValue < i + i2) {
            int i4 = -1;
            for (int i5 = 0; i5 < length; i5++) {
                if (mediaItemArr[i5] != null && (i4 == -1 || this.mComparator.compare(mediaItemArr[i5], mediaItemArr[i4]) < 0)) {
                    i4 = i5;
                }
            }
            if (i4 == -1) {
                break;
            }
            iArr[i4] = iArr[i4] + 1;
            if (iIntValue >= i) {
                arrayList.add(mediaItemArr[i4]);
            }
            mediaItemArr[i4] = this.mFetcher[i4].getItem(iArr[i4]);
            iIntValue++;
            if (iIntValue % this.PAGE_SIZE == 0) {
                this.mIndex.put(Integer.valueOf(iIntValue), (int[]) iArr.clone());
            }
        }
        return arrayList;
    }

    @Override
    public int getTotalMediaItemCount() {
        int totalMediaItemCount = 0;
        for (MediaSet mediaSet : this.mSources) {
            totalMediaItemCount += mediaSet.getTotalMediaItemCount();
        }
        return totalMediaItemCount;
    }

    @Override
    public long reload() {
        int length = this.mSources.length;
        boolean z = false;
        for (int i = 0; i < length; i++) {
            if (this.mSources[i].reload() > this.mDataVersion) {
                z = true;
            }
        }
        if (z) {
            this.mDataVersion = nextVersionNumber();
            updateData();
            invalidateCache();
        }
        return this.mDataVersion;
    }

    @Override
    public void onContentDirty() {
        notifyContentChanged();
    }

    @Override
    public int getSupportedOperations() {
        return this.mSupportedOperation;
    }

    @Override
    public void delete() {
        for (MediaSet mediaSet : this.mSources) {
            mediaSet.delete();
        }
    }

    @Override
    public void rotate(int i) {
        for (MediaSet mediaSet : this.mSources) {
            mediaSet.rotate(i);
        }
    }

    private class FetchCache {
        private MediaSet mBaseSet;
        private SoftReference<ArrayList<MediaItem>> mCacheRef;
        private int mStartPos;

        public FetchCache(MediaSet mediaSet) {
            this.mBaseSet = mediaSet;
        }

        public void invalidate() {
            this.mCacheRef = null;
        }

        public MediaItem getItem(int i) {
            ArrayList<MediaItem> mediaItem;
            if (LocalMergeAlbum.this.PAGE_SIZE == 0) {
                LocalMergeAlbum.this.PAGE_SIZE = LocalMergeAlbum.this.adjustPageSize();
            }
            boolean z = true;
            if (this.mCacheRef != null && i >= this.mStartPos && i < this.mStartPos + LocalMergeAlbum.this.PAGE_SIZE) {
                mediaItem = this.mCacheRef.get();
                if (mediaItem != null) {
                    z = false;
                }
            } else {
                mediaItem = null;
            }
            if (z) {
                mediaItem = this.mBaseSet.getMediaItem(i, LocalMergeAlbum.this.PAGE_SIZE);
                this.mCacheRef = new SoftReference<>(mediaItem);
                this.mStartPos = i;
            }
            if (i < this.mStartPos || i >= this.mStartPos + mediaItem.size()) {
                return null;
            }
            return mediaItem.get(i - this.mStartPos);
        }
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }

    private int adjustPageSize() {
        int mediaItemCount = getMediaItemCount() / 10;
        if (mediaItemCount <= 0) {
            mediaItemCount = 1;
        }
        int iNextPowerOf2 = Utils.nextPowerOf2(mediaItemCount);
        if (iNextPowerOf2 > 1024) {
            return SoOperater.STEP;
        }
        if (iNextPowerOf2 < 64) {
            return 64;
        }
        return iNextPowerOf2;
    }

    @Override
    public MediaItem getCoverMediaItem() {
        MediaItem mediaItem;
        synchronized (this.mCoverCacheLock) {
            if (this.mCoverCache != null) {
                return this.mCoverCache;
            }
            MediaItem coverMediaItem = super.getCoverMediaItem();
            synchronized (this.mCoverCacheLock) {
                this.mCoverCache = coverMediaItem;
                mediaItem = this.mCoverCache;
            }
            return mediaItem;
        }
    }
}
