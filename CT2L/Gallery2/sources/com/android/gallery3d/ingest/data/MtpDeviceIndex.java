package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import android.mtp.MtpDevice;
import com.android.gallery3d.ingest.data.MtpDeviceIndexRunnable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@TargetApi(12)
public class MtpDeviceIndex {
    public static final Set<Integer> SUPPORTED_IMAGE_FORMATS;
    public static final Set<Integer> SUPPORTED_VIDEO_FORMATS;
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
        Set<Integer> supportedImageFormats = new HashSet<>();
        supportedImageFormats.add(14344);
        supportedImageFormats.add(14337);
        supportedImageFormats.add(14347);
        supportedImageFormats.add(14343);
        supportedImageFormats.add(14340);
        SUPPORTED_IMAGE_FORMATS = Collections.unmodifiableSet(supportedImageFormats);
        Set<Integer> supportedVideoFormats = new HashSet<>();
        supportedVideoFormats.add(47492);
        supportedVideoFormats.add(12298);
        supportedVideoFormats.add(47490);
        supportedVideoFormats.add(12299);
        SUPPORTED_VIDEO_FORMATS = Collections.unmodifiableSet(supportedVideoFormats);
        sInstance = new MtpDeviceIndex(MtpDeviceIndexRunnable.getFactory());
    }

    public static MtpDeviceIndex getInstance() {
        return sInstance;
    }

    protected MtpDeviceIndex(MtpDeviceIndexRunnable.Factory indexRunnableFactory) {
        this.mIndexRunnableFactory = indexRunnableFactory;
    }

    public synchronized MtpDevice getDevice() {
        return this.mDevice;
    }

    public synchronized boolean isDeviceConnected() {
        return this.mDevice != null;
    }

    public boolean isFormatSupported(int format) {
        return SUPPORTED_IMAGE_FORMATS.contains(Integer.valueOf(format)) || SUPPORTED_VIDEO_FORMATS.contains(Integer.valueOf(format));
    }

    public synchronized void setDevice(MtpDevice device) {
        if (device != this.mDevice) {
            this.mDevice = device;
            resetState();
        }
    }

    public synchronized Runnable getIndexRunnable() {
        return (isDeviceConnected() && this.mResults == null) ? this.mIndexRunnableFactory.createMtpDeviceIndexRunnable(this) : null;
    }

    public synchronized boolean isIndexReady() {
        return this.mResults != null;
    }

    public synchronized void setProgressListener(ProgressListener listener) {
        this.mProgressListener = listener;
    }

    public synchronized void unsetProgressListener(ProgressListener listener) {
        if (this.mProgressListener == listener) {
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

    public Object get(int position, SortOrder order) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results == null) {
            return null;
        }
        if (order == SortOrder.ASCENDING) {
            DateBucket bucket = results.buckets[results.unifiedLookupIndex[position]];
            if (bucket.unifiedStartIndex == position) {
                return bucket.date;
            }
            return results.mtpObjects[((bucket.itemsStartIndex + position) - 1) - bucket.unifiedStartIndex];
        }
        int zeroIndex = (results.unifiedLookupIndex.length - 1) - position;
        DateBucket bucket2 = results.buckets[results.unifiedLookupIndex[zeroIndex]];
        if (bucket2.unifiedEndIndex == zeroIndex) {
            return bucket2.date;
        }
        return results.mtpObjects[(bucket2.itemsStartIndex + zeroIndex) - bucket2.unifiedStartIndex];
    }

    public IngestObjectInfo getWithoutLabels(int position, SortOrder order) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results == null) {
            return null;
        }
        if (order == SortOrder.ASCENDING) {
            return results.mtpObjects[position];
        }
        return results.mtpObjects[(results.mtpObjects.length - 1) - position];
    }

    public int getPositionFromPositionWithoutLabels(int position, SortOrder order) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results == null) {
            return -1;
        }
        if (order == SortOrder.DESCENDING) {
            position = (results.mtpObjects.length - 1) - position;
        }
        int bucketNumber = 0;
        int iMin = 0;
        int iMax = results.buckets.length - 1;
        while (true) {
            if (iMax < iMin) {
                break;
            }
            int iMid = (iMax + iMin) / 2;
            if (results.buckets[iMid].itemsStartIndex + results.buckets[iMid].numItems <= position) {
                iMin = iMid + 1;
            } else if (results.buckets[iMid].itemsStartIndex > position) {
                iMax = iMid - 1;
            } else {
                bucketNumber = iMid;
                break;
            }
        }
        int mappedPos = ((results.buckets[bucketNumber].unifiedStartIndex + position) - results.buckets[bucketNumber].itemsStartIndex) + 1;
        if (order == SortOrder.DESCENDING) {
            return results.unifiedLookupIndex.length - mappedPos;
        }
        return mappedPos;
    }

    public int getPositionWithoutLabelsFromPosition(int position, SortOrder order) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results == null) {
            return -1;
        }
        if (order == SortOrder.ASCENDING) {
            DateBucket bucket = results.buckets[results.unifiedLookupIndex[position]];
            if (bucket.unifiedStartIndex == position) {
                position++;
            }
            return ((bucket.itemsStartIndex + position) - 1) - bucket.unifiedStartIndex;
        }
        int zeroIndex = (results.unifiedLookupIndex.length - 1) - position;
        DateBucket bucket2 = results.buckets[results.unifiedLookupIndex[zeroIndex]];
        if (bucket2.unifiedEndIndex == zeroIndex) {
            zeroIndex--;
        }
        return (((results.mtpObjects.length - 1) - bucket2.itemsStartIndex) - zeroIndex) + bucket2.unifiedStartIndex;
    }

    public int sizeWithoutLabels() {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results != null) {
            return results.mtpObjects.length;
        }
        return 0;
    }

    public int getFirstPositionForBucketNumber(int bucketNumber, SortOrder order) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        return order == SortOrder.ASCENDING ? results.buckets[bucketNumber].unifiedStartIndex : (results.unifiedLookupIndex.length - results.buckets[(results.buckets.length - 1) - bucketNumber].unifiedEndIndex) - 1;
    }

    public int getBucketNumberForPosition(int position, SortOrder order) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        return order == SortOrder.ASCENDING ? results.unifiedLookupIndex[position] : (results.buckets.length - 1) - results.unifiedLookupIndex[(results.unifiedLookupIndex.length - 1) - position];
    }

    public DateBucket[] getBuckets(SortOrder order) {
        MtpDeviceIndexRunnable.Results results = this.mResults;
        if (results == null) {
            return null;
        }
        return order == SortOrder.ASCENDING ? results.buckets : results.reversedBuckets;
    }

    protected void resetState() {
        this.mGeneration++;
        this.mResults = null;
    }

    protected boolean isAtGeneration(MtpDevice device, long generation) {
        return this.mGeneration == generation && this.mDevice == device;
    }

    protected synchronized boolean setIndexingResults(MtpDevice device, long generation, MtpDeviceIndexRunnable.Results results) {
        boolean z = true;
        synchronized (this) {
            if (!isAtGeneration(device, generation)) {
                z = false;
            } else {
                this.mResults = results;
                onIndexFinish(true);
            }
        }
        return z;
    }

    protected synchronized void onIndexFinish(boolean successful) {
        if (!successful) {
            resetState();
            if (this.mProgressListener != null) {
                this.mProgressListener.onIndexingFinished();
            }
        } else if (this.mProgressListener != null) {
        }
    }

    protected synchronized void onSorting() {
        if (this.mProgressListener != null) {
            this.mProgressListener.onSortingStarted();
        }
    }

    protected synchronized void onObjectIndexed(IngestObjectInfo object, int numVisited) {
        if (this.mProgressListener != null) {
            this.mProgressListener.onObjectIndexed(object, numVisited);
        }
    }

    protected long getGeneration() {
        return this.mGeneration;
    }
}
