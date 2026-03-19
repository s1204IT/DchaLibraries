package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;
import android.os.Build;
import android.webkit.MimeTypeMap;
import com.android.gallery3d.ingest.data.MtpDeviceIndexRunnable;
import com.mediatek.omadrm.OmaDrmStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@TargetApi(12)
public class MtpDeviceIndex {
    public static final Set<Integer> SUPPORTED_IMAGE_FORMATS;
    public static final Set<Integer> SUPPORTED_VIDEO_FORMATS;
    private static final Map<String, Boolean> sCachedSupportedExtenstions;
    private static final MtpDeviceIndex sInstance;
    private MtpDevice mDevice;
    private long mGeneration;
    private final MtpDeviceIndexRunnable.Factory mIndexRunnableFactory;
    private ProgressListener mProgressListener;
    private volatile MtpDeviceIndexRunnable.Results mResults;

    public interface ProgressListener {
        void onIndexingFinished();

        void onObjectIndexed(IngestObjectInfo ingestObjectInfo, int i);

        void onSortingStarted();
    }

    public enum SortOrder {
        ASCENDING,
        DESCENDING
    }

    static {
        HashSet hashSet = new HashSet();
        hashSet.add(14344);
        hashSet.add(14337);
        hashSet.add(14347);
        hashSet.add(14343);
        hashSet.add(14340);
        hashSet.add(14349);
        hashSet.add(14338);
        if (Build.VERSION.SDK_INT >= 24) {
            hashSet.add(14353);
        }
        SUPPORTED_IMAGE_FORMATS = Collections.unmodifiableSet(hashSet);
        HashSet hashSet2 = new HashSet();
        hashSet2.add(47492);
        hashSet2.add(12298);
        hashSet2.add(47490);
        hashSet2.add(47491);
        hashSet2.add(12299);
        SUPPORTED_VIDEO_FORMATS = Collections.unmodifiableSet(hashSet2);
        sInstance = new MtpDeviceIndex(MtpDeviceIndexRunnable.getFactory());
        sCachedSupportedExtenstions = new HashMap();
    }

    public static MtpDeviceIndex getInstance() {
        return sInstance;
    }

    protected MtpDeviceIndex(MtpDeviceIndexRunnable.Factory factory) {
        this.mIndexRunnableFactory = factory;
    }

    public synchronized MtpDevice getDevice() {
        return this.mDevice;
    }

    public synchronized boolean isDeviceConnected() {
        return this.mDevice != null;
    }

    public boolean isFormatSupported(MtpObjectInfo mtpObjectInfo) {
        int iLastIndexOf;
        int format = mtpObjectInfo.getFormat();
        if (SUPPORTED_IMAGE_FORMATS.contains(Integer.valueOf(format)) || SUPPORTED_VIDEO_FORMATS.contains(Integer.valueOf(format))) {
            return true;
        }
        String name = mtpObjectInfo.getName();
        if (name != null && (iLastIndexOf = name.lastIndexOf(46)) >= 0) {
            String strSubstring = name.substring(iLastIndexOf + 1);
            Boolean bool = sCachedSupportedExtenstions.get(strSubstring);
            if (bool != null) {
                return bool.booleanValue();
            }
            String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(strSubstring.toLowerCase(Locale.US));
            if (mimeTypeFromExtension != null) {
                Boolean boolValueOf = Boolean.valueOf(mimeTypeFromExtension.startsWith(OmaDrmStore.MimePrefix.IMAGE) || mimeTypeFromExtension.startsWith(OmaDrmStore.MimePrefix.VIDEO));
                sCachedSupportedExtenstions.put(strSubstring, boolValueOf);
                return boolValueOf.booleanValue();
            }
        }
        return false;
    }

    public synchronized void setDevice(MtpDevice mtpDevice) {
        if (mtpDevice == this.mDevice) {
            return;
        }
        this.mDevice = mtpDevice;
        resetState();
    }

    public synchronized Runnable getIndexRunnable() {
        if (isDeviceConnected() && this.mResults == null) {
            return this.mIndexRunnableFactory.createMtpDeviceIndexRunnable(this);
        }
        return null;
    }

    public synchronized boolean isIndexReady() {
        return this.mResults != null;
    }

    public synchronized void setProgressListener(ProgressListener progressListener) {
        this.mProgressListener = progressListener;
    }

    public synchronized void unsetProgressListener(ProgressListener progressListener) {
        if (this.mProgressListener == progressListener) {
            this.mProgressListener = null;
        }
    }

    public int size() {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results != null) {
            return results.unifiedLookupIndex.length;
        }
        return 0;
    }

    public Object get(int i, SortOrder sortOrder) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results == null) {
            return null;
        }
        if (sortOrder == SortOrder.ASCENDING) {
            DateBucket dateBucket = results.buckets[results.unifiedLookupIndex[i]];
            if (dateBucket.unifiedStartIndex == i) {
                return dateBucket.date;
            }
            return results.mtpObjects[((dateBucket.itemsStartIndex + i) - 1) - dateBucket.unifiedStartIndex];
        }
        int length = (results.unifiedLookupIndex.length - 1) - i;
        DateBucket dateBucket2 = results.buckets[results.unifiedLookupIndex[length]];
        if (dateBucket2.unifiedEndIndex == length) {
            return dateBucket2.date;
        }
        return results.mtpObjects[(dateBucket2.itemsStartIndex + length) - dateBucket2.unifiedStartIndex];
    }

    public IngestObjectInfo getWithoutLabels(int i, SortOrder sortOrder) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results == null) {
            return null;
        }
        if (sortOrder == SortOrder.ASCENDING) {
            return results.mtpObjects[i];
        }
        return results.mtpObjects[(results.mtpObjects.length - 1) - i];
    }

    public int getPositionFromPositionWithoutLabels(int i, SortOrder sortOrder) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results == null || results.buckets == null || results.buckets.length == 0) {
            return -1;
        }
        if (sortOrder == SortOrder.DESCENDING) {
            i = (results.mtpObjects.length - 1) - i;
        }
        int length = results.buckets.length - 1;
        int i2 = 0;
        int i3 = 0;
        while (true) {
            if (length < i3) {
                break;
            }
            int i4 = (length + i3) / 2;
            if (results.buckets[i4].itemsStartIndex + results.buckets[i4].numItems <= i) {
                i3 = i4 + 1;
            } else if (results.buckets[i4].itemsStartIndex > i) {
                length = i4 - 1;
            } else {
                i2 = i4;
                break;
            }
        }
        int i5 = ((results.buckets[i2].unifiedStartIndex + i) - results.buckets[i2].itemsStartIndex) + 1;
        if (sortOrder == SortOrder.DESCENDING) {
            return results.unifiedLookupIndex.length - i5;
        }
        return i5;
    }

    public int getPositionWithoutLabelsFromPosition(int i, SortOrder sortOrder) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results == null || results.buckets == null || results.buckets.length == 0) {
            return -1;
        }
        if (sortOrder == SortOrder.ASCENDING) {
            DateBucket dateBucket = results.buckets[results.unifiedLookupIndex[i]];
            if (dateBucket.unifiedStartIndex == i) {
                i++;
            }
            return ((dateBucket.itemsStartIndex + i) - 1) - dateBucket.unifiedStartIndex;
        }
        int length = (results.unifiedLookupIndex.length - 1) - i;
        DateBucket dateBucket2 = results.buckets[results.unifiedLookupIndex[length]];
        if (dateBucket2.unifiedEndIndex == length) {
            length--;
        }
        return (((results.mtpObjects.length - 1) - dateBucket2.itemsStartIndex) - length) + dateBucket2.unifiedStartIndex;
    }

    public int sizeWithoutLabels() {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results != null) {
            return results.mtpObjects.length;
        }
        return 0;
    }

    public int getFirstPositionForBucketNumber(int i, SortOrder sortOrder) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (sortOrder == SortOrder.ASCENDING) {
            return results.buckets[i].unifiedStartIndex;
        }
        return (results.unifiedLookupIndex.length - results.buckets[(results.buckets.length - 1) - i].unifiedEndIndex) - 1;
    }

    public int getBucketNumberForPosition(int i, SortOrder sortOrder) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (sortOrder == SortOrder.ASCENDING) {
            return results.unifiedLookupIndex[i];
        }
        return (results.buckets.length - 1) - results.unifiedLookupIndex[(results.unifiedLookupIndex.length - 1) - i];
    }

    public DateBucket[] getBuckets(SortOrder sortOrder) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results == null) {
            return null;
        }
        return sortOrder == SortOrder.ASCENDING ? results.buckets : results.reversedBuckets;
    }

    protected void resetState() {
        this.mGeneration++;
        this.mResults = null;
    }

    protected boolean isAtGeneration(MtpDevice mtpDevice, long j) {
        return this.mGeneration == j && this.mDevice == mtpDevice;
    }

    protected synchronized boolean setIndexingResults(MtpDevice mtpDevice, long j, MtpDeviceIndexRunnable.Results results) {
        if (!isAtGeneration(mtpDevice, j)) {
            return false;
        }
        this.mResults = results;
        onIndexFinish(true);
        return true;
    }

    protected synchronized void onIndexFinish(boolean z) {
        if (!z) {
            try {
                resetState();
            } catch (Throwable th) {
                throw th;
            }
        }
        if (this.mProgressListener != null) {
            this.mProgressListener.onIndexingFinished();
        }
    }

    protected synchronized void onSorting() {
        if (this.mProgressListener != null) {
            this.mProgressListener.onSortingStarted();
        }
    }

    protected synchronized void onObjectIndexed(IngestObjectInfo ingestObjectInfo, int i) {
        if (this.mProgressListener != null) {
            this.mProgressListener.onObjectIndexed(ingestObjectInfo, i);
        }
    }

    protected long getGeneration() {
        return this.mGeneration;
    }
}
