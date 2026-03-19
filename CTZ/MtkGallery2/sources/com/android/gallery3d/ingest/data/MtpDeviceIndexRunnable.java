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
        public MtpDeviceIndexRunnable createMtpDeviceIndexRunnable(MtpDeviceIndex mtpDeviceIndex) {
            return new MtpDeviceIndexRunnable(mtpDeviceIndex);
        }
    }

    static class Results {
        final DateBucket[] buckets;
        final IngestObjectInfo[] mtpObjects;
        final DateBucket[] reversedBuckets;
        final int[] unifiedLookupIndex;

        public Results(int[] iArr, IngestObjectInfo[] ingestObjectInfoArr, DateBucket[] dateBucketArr) {
            this.unifiedLookupIndex = iArr;
            this.mtpObjects = ingestObjectInfoArr;
            this.buckets = dateBucketArr;
            this.reversedBuckets = new DateBucket[dateBucketArr.length];
            for (int i = 0; i < dateBucketArr.length; i++) {
                this.reversedBuckets[i] = dateBucketArr[(dateBucketArr.length - 1) - i];
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

    MtpDeviceIndexRunnable(MtpDeviceIndex mtpDeviceIndex) {
        this.mIndex = mtpDeviceIndex;
        this.mDevice = mtpDeviceIndex.getDevice();
        this.mIndexGeneration = mtpDeviceIndex.getGeneration();
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
        TreeMap treeMap = new TreeMap();
        int iAddAllObjects = addAllObjects(treeMap);
        this.mIndex.onSorting();
        int size = treeMap.size();
        DateBucket[] dateBucketArr = new DateBucket[size];
        IngestObjectInfo[] ingestObjectInfoArr = new IngestObjectInfo[iAddAllObjects];
        int[] iArr = new int[iAddAllObjects + size];
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        for (Map.Entry<SimpleDate, List<IngestObjectInfo>> entry : treeMap.entrySet()) {
            List<IngestObjectInfo> value = entry.getValue();
            Collections.sort(value);
            int size2 = value.size();
            int i4 = i + size2 + 1;
            Arrays.fill(iArr, i, i4, i3);
            int i5 = i4 - 1;
            int i6 = i2;
            for (int i7 = 0; i7 < size2; i7++) {
                ingestObjectInfoArr[i6] = value.get(i7);
                i6++;
            }
            dateBucketArr[i3] = new DateBucket(entry.getKey(), i, i5, i2, size2);
            i3++;
            i = i4;
            i2 = i6;
        }
        if (!this.mIndex.setIndexingResults(this.mDevice, this.mIndexGeneration, new Results(iArr, ingestObjectInfoArr, dateBucketArr))) {
            throw new IndexingException();
        }
    }

    protected void addObject(IngestObjectInfo ingestObjectInfo, SortedMap<SimpleDate, List<IngestObjectInfo>> sortedMap, int i) {
        this.mDateInstance.setTimestamp(ingestObjectInfo.getDateCreated());
        List<IngestObjectInfo> arrayList = sortedMap.get(this.mDateInstance);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            sortedMap.put(this.mDateInstance, arrayList);
            this.mDateInstance = new SimpleDate();
        }
        arrayList.add(ingestObjectInfo);
        this.mIndex.onObjectIndexed(ingestObjectInfo, i);
    }

    protected int addAllObjects(SortedMap<SimpleDate, List<IngestObjectInfo>> sortedMap) throws IndexingException {
        int i = 0;
        for (int i2 : this.mDevice.getStorageIds()) {
            if (!this.mIndex.isAtGeneration(this.mDevice, this.mIndexGeneration)) {
                throw new IndexingException();
            }
            Stack stack = new Stack();
            stack.add(-1);
            while (!stack.isEmpty()) {
                if (!this.mIndex.isAtGeneration(this.mDevice, this.mIndexGeneration)) {
                    throw new IndexingException();
                }
                int i3 = i;
                for (int i4 : this.mDevice.getObjectHandles(i2, 0, ((Integer) stack.pop()).intValue())) {
                    MtpObjectInfo objectInfo = this.mDevice.getObjectInfo(i4);
                    if (objectInfo == null) {
                        throw new IndexingException();
                    }
                    if (objectInfo.getFormat() == 12289) {
                        stack.add(Integer.valueOf(i4));
                    } else if (this.mIndex.isFormatSupported(objectInfo)) {
                        i3++;
                        addObject(new IngestObjectInfo(objectInfo), sortedMap, i3);
                    }
                }
                i = i3;
            }
        }
        return i;
    }
}
