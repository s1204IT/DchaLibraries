package com.android.gallery3d.data;

import android.net.Uri;
import android.provider.MediaStore;
import com.android.gallery3d.common.ApiHelper;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.SortedMap;
import java.util.TreeMap;

public class LocalMergeAlbum extends MediaSet implements ContentListener {
    private int mBucketId;
    private final Comparator<MediaItem> mComparator;
    private FetchCache[] mFetcher;
    private TreeMap<Integer, int[]> mIndex;
    private final MediaSet[] mSources;
    private int mSupportedOperation;

    public LocalMergeAlbum(Path path, Comparator<MediaItem> comparator, MediaSet[] sources, int bucketId) {
        super(path, -1L);
        this.mIndex = new TreeMap<>();
        this.mComparator = comparator;
        this.mSources = sources;
        this.mBucketId = bucketId;
        MediaSet[] arr$ = this.mSources;
        for (MediaSet set : arr$) {
            set.addContentListener(this);
        }
        reload();
    }

    @Override
    public boolean isCameraRoll() {
        if (this.mSources.length == 0) {
            return false;
        }
        MediaSet[] arr$ = this.mSources;
        for (MediaSet set : arr$) {
            if (!set.isCameraRoll()) {
                return false;
            }
        }
        return true;
    }

    private void updateData() {
        new ArrayList();
        int supported = this.mSources.length == 0 ? 0 : -1;
        this.mFetcher = new FetchCache[this.mSources.length];
        int n = this.mSources.length;
        for (int i = 0; i < n; i++) {
            this.mFetcher[i] = new FetchCache(this.mSources[i]);
            supported &= this.mSources[i].getSupportedOperations();
        }
        this.mSupportedOperation = supported;
        this.mIndex.clear();
        this.mIndex.put(0, new int[this.mSources.length]);
    }

    private void invalidateCache() {
        int n = this.mSources.length;
        for (int i = 0; i < n; i++) {
            this.mFetcher[i].invalidate();
        }
        this.mIndex.clear();
        this.mIndex.put(0, new int[this.mSources.length]);
    }

    @Override
    public Uri getContentUri() {
        String bucketId = String.valueOf(this.mBucketId);
        return ApiHelper.HAS_MEDIA_PROVIDER_FILES_TABLE ? MediaStore.Files.getContentUri("external").buildUpon().appendQueryParameter("bucketId", bucketId).build() : MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon().appendQueryParameter("bucketId", bucketId).build();
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
    public ArrayList<MediaItem> getMediaItem(int i, int i2) {
        SortedMap<Integer, int[]> sortedMapHeadMap = this.mIndex.headMap(Integer.valueOf(i + 1));
        int iIntValue = sortedMapHeadMap.lastKey().intValue();
        int[] iArr = (int[]) sortedMapHeadMap.get(Integer.valueOf(iIntValue)).clone();
        MediaItem[] mediaItemArr = new MediaItem[this.mSources.length];
        int length = this.mSources.length;
        for (int i3 = 0; i3 < length; i3++) {
            mediaItemArr[i3] = this.mFetcher[i3].getItem(iArr[i3]);
        }
        ArrayList<MediaItem> arrayList = new ArrayList<>();
        for (int i4 = iIntValue; i4 < i + i2; i4++) {
            int i5 = -1;
            for (int i6 = 0; i6 < length; i6++) {
                if (mediaItemArr[i6] != null && (i5 == -1 || this.mComparator.compare(mediaItemArr[i6], mediaItemArr[i5]) < 0)) {
                    i5 = i6;
                }
            }
            if (i5 == -1) {
                break;
            }
            iArr[i5] = iArr[i5] + 1;
            if (i4 >= i) {
                arrayList.add(mediaItemArr[i5]);
            }
            mediaItemArr[i5] = this.mFetcher[i5].getItem(iArr[i5]);
            if ((i4 + 1) % 64 == 0) {
                this.mIndex.put(Integer.valueOf(i4 + 1), (int[]) iArr.clone());
            }
        }
        return arrayList;
    }

    @Override
    public int getTotalMediaItemCount() {
        int count = 0;
        MediaSet[] arr$ = this.mSources;
        for (MediaSet set : arr$) {
            count += set.getTotalMediaItemCount();
        }
        return count;
    }

    @Override
    public long reload() {
        boolean changed = false;
        int n = this.mSources.length;
        for (int i = 0; i < n; i++) {
            if (this.mSources[i].reload() > this.mDataVersion) {
                changed = true;
            }
        }
        if (changed) {
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
        MediaSet[] arr$ = this.mSources;
        for (MediaSet set : arr$) {
            set.delete();
        }
    }

    @Override
    public void rotate(int degrees) {
        MediaSet[] arr$ = this.mSources;
        for (MediaSet set : arr$) {
            set.rotate(degrees);
        }
    }

    private static class FetchCache {
        private MediaSet mBaseSet;
        private SoftReference<ArrayList<MediaItem>> mCacheRef;
        private int mStartPos;

        public FetchCache(MediaSet baseSet) {
            this.mBaseSet = baseSet;
        }

        public void invalidate() {
            this.mCacheRef = null;
        }

        public MediaItem getItem(int index) {
            boolean needLoading = false;
            ArrayList<MediaItem> cache = null;
            if (this.mCacheRef == null || index < this.mStartPos || index >= this.mStartPos + 64) {
                needLoading = true;
            } else {
                ArrayList<MediaItem> cache2 = this.mCacheRef.get();
                cache = cache2;
                if (cache == null) {
                    needLoading = true;
                }
            }
            if (needLoading) {
                cache = this.mBaseSet.getMediaItem(index, 64);
                this.mCacheRef = new SoftReference<>(cache);
                this.mStartPos = index;
            }
            if (index < this.mStartPos || index >= this.mStartPos + cache.size()) {
                return null;
            }
            return cache.get(index - this.mStartPos);
        }
    }

    @Override
    public boolean isLeafAlbum() {
        return true;
    }
}
