package com.android.gallery3d.ingest.data;

import android.annotation.TargetApi;
import android.mtp.MtpDevice;
import android.mtp.MtpObjectInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;

@TargetApi(12)
public class MtpDeviceIndexRunnable implements Runnable {
    private static Factory sDefaultFactory = new Factory();
    private SimpleDate mDateInstance = new SimpleDate();
    private final MtpDevice mDevice;
    protected final MtpDeviceIndex mIndex;
    private final long mIndexGeneration;

    public static class Factory {
        public MtpDeviceIndexRunnable createMtpDeviceIndexRunnable(MtpDeviceIndex index) {
            return new MtpDeviceIndexRunnable(index);
        }
    }

    static class Results {
        final DateBucket[] buckets;
        final IngestObjectInfo[] mtpObjects;
        final DateBucket[] reversedBuckets;
        final int[] unifiedLookupIndex;

        public Results(int[] unifiedLookupIndex, IngestObjectInfo[] mtpObjects, DateBucket[] buckets) {
            this.unifiedLookupIndex = unifiedLookupIndex;
            this.mtpObjects = mtpObjects;
            this.buckets = buckets;
            this.reversedBuckets = new DateBucket[buckets.length];
            for (int i = 0; i < buckets.length; i++) {
                this.reversedBuckets[i] = buckets[(buckets.length - 1) - i];
            }
        }
    }

    public static Factory getFactory() {
        return sDefaultFactory;
    }

    public class IndexingException extends RuntimeException {
        public IndexingException() {
        }
    }

    MtpDeviceIndexRunnable(MtpDeviceIndex index) {
        this.mIndex = index;
        this.mDevice = index.getDevice();
        this.mIndexGeneration = index.getGeneration();
    }

    @Override
    public void run() {
        try {
            indexDevice();
        } catch (IndexingException e) {
            this.mIndex.onIndexFinish(false);
        }
    }

    private void indexDevice() throws IndexingException {
        SortedMap<SimpleDate, List<IngestObjectInfo>> bucketsTemp = new TreeMap<>();
        int numObjects = addAllObjects(bucketsTemp);
        this.mIndex.onSorting();
        int numBuckets = bucketsTemp.size();
        DateBucket[] buckets = new DateBucket[numBuckets];
        IngestObjectInfo[] mtpObjects = new IngestObjectInfo[numObjects];
        int[] unifiedLookupIndex = new int[numObjects + numBuckets];
        int currentUnifiedIndexEntry = 0;
        int currentItemsEntry = 0;
        int i = 0;
        for (Map.Entry<SimpleDate, List<IngestObjectInfo>> bucketTemp : bucketsTemp.entrySet()) {
            List<IngestObjectInfo> objects = bucketTemp.getValue();
            Collections.sort(objects);
            int numBucketObjects = objects.size();
            int nextUnifiedEntry = currentUnifiedIndexEntry + numBucketObjects + 1;
            Arrays.fill(unifiedLookupIndex, currentUnifiedIndexEntry, nextUnifiedEntry, i);
            int unifiedStartIndex = currentUnifiedIndexEntry;
            int unifiedEndIndex = nextUnifiedEntry - 1;
            currentUnifiedIndexEntry = nextUnifiedEntry;
            int itemsStartIndex = currentItemsEntry;
            for (int j = 0; j < numBucketObjects; j++) {
                mtpObjects[currentItemsEntry] = objects.get(j);
                currentItemsEntry++;
            }
            buckets[i] = new DateBucket(bucketTemp.getKey(), unifiedStartIndex, unifiedEndIndex, itemsStartIndex, numBucketObjects);
            i++;
        }
        if (!this.mIndex.setIndexingResults(this.mDevice, this.mIndexGeneration, new Results(unifiedLookupIndex, mtpObjects, buckets))) {
            throw new IndexingException();
        }
    }

    protected void addObject(IngestObjectInfo objectInfo, SortedMap<SimpleDate, List<IngestObjectInfo>> bucketsTemp, int numObjects) {
        this.mDateInstance.setTimestamp(objectInfo.getDateCreated());
        List<IngestObjectInfo> bucket = bucketsTemp.get(this.mDateInstance);
        if (bucket == null) {
            bucket = new ArrayList<>();
            bucketsTemp.put(this.mDateInstance, bucket);
            this.mDateInstance = new SimpleDate();
        }
        bucket.add(objectInfo);
        this.mIndex.onObjectIndexed(objectInfo, numObjects);
    }

    protected int addAllObjects(SortedMap<SimpleDate, List<IngestObjectInfo>> bucketsTemp) throws IndexingException {
        int numObjects = 0;
        int[] arr$ = this.mDevice.getStorageIds();
        for (int storageId : arr$) {
            if (!this.mIndex.isAtGeneration(this.mDevice, this.mIndexGeneration)) {
                throw new IndexingException();
            }
            Stack<Integer> pendingDirectories = new Stack<>();
            pendingDirectories.add(-1);
            while (!pendingDirectories.isEmpty()) {
                if (!this.mIndex.isAtGeneration(this.mDevice, this.mIndexGeneration)) {
                    throw new IndexingException();
                }
                int dirHandle = pendingDirectories.pop().intValue();
                int[] arr$2 = this.mDevice.getObjectHandles(storageId, 0, dirHandle);
                for (int objectHandle : arr$2) {
                    MtpObjectInfo mtpObjectInfo = this.mDevice.getObjectInfo(objectHandle);
                    if (mtpObjectInfo == null) {
                        throw new IndexingException();
                    }
                    int format = mtpObjectInfo.getFormat();
                    if (format == 12289) {
                        pendingDirectories.add(Integer.valueOf(objectHandle));
                    } else if (this.mIndex.isFormatSupported(format)) {
                        numObjects++;
                        addObject(new IngestObjectInfo(mtpObjectInfo), bucketsTemp, numObjects);
                    }
                }
            }
        }
        return numObjects;
    }
}
