package com.android.server.net;

import android.net.NetworkIdentity;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.Binder;
import android.telephony.SubscriptionPlan;
import android.util.ArrayMap;
import android.util.AtomicFile;
import android.util.IntArray;
import android.util.MathUtils;
import android.util.Range;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FileRotator;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.job.controllers.JobStatus;
import com.google.android.collect.Lists;
import com.google.android.collect.Maps;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.ProtocolException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import libcore.io.IoUtils;

public class NetworkStatsCollection implements FileRotator.Reader {
    private static final int FILE_MAGIC = 1095648596;
    private static final int VERSION_NETWORK_INIT = 1;
    private static final int VERSION_UID_INIT = 1;
    private static final int VERSION_UID_WITH_IDENT = 2;
    private static final int VERSION_UID_WITH_SET = 4;
    private static final int VERSION_UID_WITH_TAG = 3;
    private static final int VERSION_UNIFIED_INIT = 16;
    private final long mBucketDuration;
    private boolean mDirty;
    private long mEndMillis;
    private long mStartMillis;
    private ArrayMap<Key, NetworkStatsHistory> mStats = new ArrayMap<>();
    private long mTotalBytes;

    public NetworkStatsCollection(long j) {
        this.mBucketDuration = j;
        reset();
    }

    public void clear() {
        reset();
    }

    public void reset() {
        this.mStats.clear();
        this.mStartMillis = JobStatus.NO_LATEST_RUNTIME;
        this.mEndMillis = Long.MIN_VALUE;
        this.mTotalBytes = 0L;
        this.mDirty = false;
    }

    public long getStartMillis() {
        return this.mStartMillis;
    }

    public long getFirstAtomicBucketMillis() {
        return this.mStartMillis == JobStatus.NO_LATEST_RUNTIME ? JobStatus.NO_LATEST_RUNTIME : this.mStartMillis + this.mBucketDuration;
    }

    public long getEndMillis() {
        return this.mEndMillis;
    }

    public long getTotalBytes() {
        return this.mTotalBytes;
    }

    public boolean isDirty() {
        return this.mDirty;
    }

    public void clearDirty() {
        this.mDirty = false;
    }

    public boolean isEmpty() {
        return this.mStartMillis == JobStatus.NO_LATEST_RUNTIME && this.mEndMillis == Long.MIN_VALUE;
    }

    @VisibleForTesting
    public long roundUp(long j) {
        if (j == Long.MIN_VALUE || j == JobStatus.NO_LATEST_RUNTIME || j == -1) {
            return j;
        }
        long j2 = j % this.mBucketDuration;
        if (j2 > 0) {
            return (j - j2) + this.mBucketDuration;
        }
        return j;
    }

    @VisibleForTesting
    public long roundDown(long j) {
        if (j == Long.MIN_VALUE || j == JobStatus.NO_LATEST_RUNTIME || j == -1) {
            return j;
        }
        long j2 = j % this.mBucketDuration;
        if (j2 > 0) {
            return j - j2;
        }
        return j;
    }

    @VisibleForTesting
    public static long multiplySafe(long j, long j2, long j3) {
        if (j3 == 0) {
            j3 = 1;
        }
        long j4 = j * j2;
        if (((Math.abs(j) | Math.abs(j2)) >>> 31) != 0 && ((j2 != 0 && j4 / j2 != j) || (j == Long.MIN_VALUE && j2 == -1))) {
            return (long) ((j2 / j3) * j);
        }
        return j4 / j3;
    }

    public int[] getRelevantUids(int i) {
        return getRelevantUids(i, Binder.getCallingUid());
    }

    public int[] getRelevantUids(int i, int i2) {
        int iBinarySearch;
        IntArray intArray = new IntArray();
        for (int i3 = 0; i3 < this.mStats.size(); i3++) {
            Key keyKeyAt = this.mStats.keyAt(i3);
            if (NetworkStatsAccess.isAccessibleToUser(keyKeyAt.uid, i2, i) && (iBinarySearch = intArray.binarySearch(keyKeyAt.uid)) < 0) {
                intArray.add(~iBinarySearch, keyKeyAt.uid);
            }
        }
        return intArray.toArray();
    }

    public NetworkStatsHistory getHistory(NetworkTemplate networkTemplate, SubscriptionPlan subscriptionPlan, int i, int i2, int i3, int i4, long j, long j2, int i5, int i6) {
        long jRoundDown;
        long jRoundUp;
        long j3;
        int i7;
        int i8;
        long jRoundUp2 = j2;
        if (!NetworkStatsAccess.isAccessibleToUser(i, i6, i5)) {
            throw new SecurityException("Network stats history of uid " + i + " is forbidden for caller " + i6);
        }
        int iConstrain = (int) MathUtils.constrain((jRoundUp2 - j) / this.mBucketDuration, 0L, 15552000000L / this.mBucketDuration);
        NetworkStatsHistory networkStatsHistory = new NetworkStatsHistory(this.mBucketDuration, iConstrain, i4);
        if (j == jRoundUp2) {
            return networkStatsHistory;
        }
        long dataUsageTime = subscriptionPlan != null ? subscriptionPlan.getDataUsageTime() : -1L;
        if (dataUsageTime != -1) {
            Iterator<Range<ZonedDateTime>> itCycleIterator = subscriptionPlan.cycleIterator();
            while (itCycleIterator.hasNext()) {
                Range<ZonedDateTime> next = itCycleIterator.next();
                jRoundUp = ((ZonedDateTime) next.getLower()).toInstant().toEpochMilli();
                long epochMilli = ((ZonedDateTime) next.getUpper()).toInstant().toEpochMilli();
                if (jRoundUp <= dataUsageTime && dataUsageTime < epochMilli) {
                    jRoundDown = Long.min(j, jRoundUp);
                    jRoundUp2 = Long.max(jRoundUp2, dataUsageTime);
                    break;
                }
            }
            jRoundDown = j;
            jRoundUp = -1;
        } else {
            jRoundDown = j;
            jRoundUp = -1;
        }
        if (jRoundUp != -1) {
            jRoundUp = roundUp(jRoundUp);
            dataUsageTime = roundDown(dataUsageTime);
            jRoundDown = roundDown(jRoundDown);
            jRoundUp2 = roundUp(jRoundUp2);
        }
        long j4 = jRoundUp2;
        long j5 = dataUsageTime;
        long j6 = jRoundDown;
        long j7 = jRoundUp;
        int i9 = 0;
        while (i9 < this.mStats.size()) {
            Key keyKeyAt = this.mStats.keyAt(i9);
            if (keyKeyAt.uid != i || !NetworkStats.setMatches(i2, keyKeyAt.set) || keyKeyAt.tag != i3 || !templateMatches(networkTemplate, keyKeyAt.ident)) {
                i8 = i9;
            } else {
                i8 = i9;
                networkStatsHistory.recordHistory(this.mStats.valueAt(i9), j6, j4);
            }
            i9 = i8 + 1;
        }
        if (j7 != -1) {
            NetworkStatsHistory.Entry values = networkStatsHistory.getValues(j7, j5, (NetworkStatsHistory.Entry) null);
            if (values.rxBytes == 0 || values.txBytes == 0) {
                networkStatsHistory.recordData(j7, j5, new NetworkStats.Entry(1L, 0L, 1L, 0L, 0L));
                networkStatsHistory.getValues(j7, j5, values);
            }
            long j8 = values.rxBytes + values.txBytes;
            long j9 = values.rxBytes;
            long j10 = values.txBytes;
            long dataUsageBytes = subscriptionPlan.getDataUsageBytes();
            long jMultiplySafe = multiplySafe(dataUsageBytes, j9, j8);
            long jMultiplySafe2 = multiplySafe(dataUsageBytes, j10, j8);
            long totalBytes = networkStatsHistory.getTotalBytes();
            int i10 = 0;
            while (i10 < networkStatsHistory.size()) {
                networkStatsHistory.getValues(i10, values);
                int i11 = i10;
                if (values.bucketStart >= j7) {
                    j3 = j7;
                    if (values.bucketStart + values.bucketDuration <= j5) {
                        values.rxBytes = multiplySafe(jMultiplySafe, values.rxBytes, j9);
                        values.txBytes = multiplySafe(jMultiplySafe2, values.txBytes, j10);
                        values.rxPackets = 0L;
                        values.txPackets = 0L;
                        i7 = i11;
                        networkStatsHistory.setValues(i7, values);
                    }
                    i10 = i7 + 1;
                    j7 = j3;
                } else {
                    j3 = j7;
                }
                i7 = i11;
                i10 = i7 + 1;
                j7 = j3;
            }
            long totalBytes2 = networkStatsHistory.getTotalBytes() - totalBytes;
            if (totalBytes2 != 0) {
                Slog.d("NetworkStats", "Augmented network usage by " + totalBytes2 + " bytes");
            }
            NetworkStatsHistory networkStatsHistory2 = new NetworkStatsHistory(this.mBucketDuration, iConstrain, i4);
            networkStatsHistory2.recordHistory(networkStatsHistory, j, j2);
            return networkStatsHistory2;
        }
        return networkStatsHistory;
    }

    public NetworkStats getSummary(NetworkTemplate networkTemplate, long j, long j2, int i, int i2) {
        int i3;
        NetworkStats.Entry entry;
        int i4;
        NetworkStatsCollection networkStatsCollection = this;
        long jCurrentTimeMillis = System.currentTimeMillis();
        NetworkStats networkStats = new NetworkStats(j2 - j, 24);
        if (j == j2) {
            return networkStats;
        }
        NetworkStats.Entry entry2 = new NetworkStats.Entry();
        NetworkStatsHistory.Entry entry3 = null;
        int i5 = 0;
        while (i5 < networkStatsCollection.mStats.size()) {
            Key keyKeyAt = networkStatsCollection.mStats.keyAt(i5);
            if (templateMatches(networkTemplate, keyKeyAt.ident) && NetworkStatsAccess.isAccessibleToUser(keyKeyAt.uid, i2, i) && keyKeyAt.set < 1000) {
                i3 = i5;
                entry = entry2;
                NetworkStatsHistory.Entry values = networkStatsCollection.mStats.valueAt(i5).getValues(j, j2, jCurrentTimeMillis, entry3);
                entry.iface = NetworkStats.IFACE_ALL;
                entry.uid = keyKeyAt.uid;
                entry.set = keyKeyAt.set;
                entry.tag = keyKeyAt.tag;
                if (!keyKeyAt.ident.areAllMembersOnDefaultNetwork()) {
                    i4 = 0;
                } else {
                    i4 = 1;
                }
                entry.defaultNetwork = i4;
                entry.metered = keyKeyAt.ident.isAnyMemberMetered() ? 1 : 0;
                entry.roaming = keyKeyAt.ident.isAnyMemberRoaming() ? 1 : 0;
                entry.rxBytes = values.rxBytes;
                entry.rxPackets = values.rxPackets;
                entry.txBytes = values.txBytes;
                entry.txPackets = values.txPackets;
                entry.operations = values.operations;
                if (!entry.isEmpty()) {
                    networkStats.combineValues(entry);
                }
                entry3 = values;
            } else {
                i3 = i5;
                entry = entry2;
            }
            i5 = i3 + 1;
            entry2 = entry;
            networkStatsCollection = this;
        }
        return networkStats;
    }

    public void recordData(NetworkIdentitySet networkIdentitySet, int i, int i2, int i3, long j, long j2, NetworkStats.Entry entry) {
        NetworkStatsHistory networkStatsHistoryFindOrCreateHistory = findOrCreateHistory(networkIdentitySet, i, i2, i3);
        networkStatsHistoryFindOrCreateHistory.recordData(j, j2, entry);
        noteRecordedHistory(networkStatsHistoryFindOrCreateHistory.getStart(), networkStatsHistoryFindOrCreateHistory.getEnd(), entry.txBytes + entry.rxBytes);
    }

    private void recordHistory(Key key, NetworkStatsHistory networkStatsHistory) {
        if (networkStatsHistory.size() == 0) {
            return;
        }
        noteRecordedHistory(networkStatsHistory.getStart(), networkStatsHistory.getEnd(), networkStatsHistory.getTotalBytes());
        NetworkStatsHistory networkStatsHistory2 = this.mStats.get(key);
        if (networkStatsHistory2 == null) {
            networkStatsHistory2 = new NetworkStatsHistory(networkStatsHistory.getBucketDuration());
            this.mStats.put(key, networkStatsHistory2);
        }
        networkStatsHistory2.recordEntireHistory(networkStatsHistory);
    }

    public void recordCollection(NetworkStatsCollection networkStatsCollection) {
        for (int i = 0; i < networkStatsCollection.mStats.size(); i++) {
            recordHistory(networkStatsCollection.mStats.keyAt(i), networkStatsCollection.mStats.valueAt(i));
        }
    }

    private NetworkStatsHistory findOrCreateHistory(NetworkIdentitySet networkIdentitySet, int i, int i2, int i3) {
        NetworkStatsHistory networkStatsHistory;
        Key key = new Key(networkIdentitySet, i, i2, i3);
        NetworkStatsHistory networkStatsHistory2 = this.mStats.get(key);
        if (networkStatsHistory2 == null) {
            networkStatsHistory = new NetworkStatsHistory(this.mBucketDuration, 10);
        } else if (networkStatsHistory2.getBucketDuration() != this.mBucketDuration) {
            networkStatsHistory = new NetworkStatsHistory(networkStatsHistory2, this.mBucketDuration);
        } else {
            networkStatsHistory = null;
        }
        if (networkStatsHistory != null) {
            this.mStats.put(key, networkStatsHistory);
            return networkStatsHistory;
        }
        return networkStatsHistory2;
    }

    public void read(InputStream inputStream) throws IOException {
        read(new DataInputStream(inputStream));
    }

    public void read(DataInputStream dataInputStream) throws IOException {
        int i = dataInputStream.readInt();
        if (i != FILE_MAGIC) {
            throw new ProtocolException("unexpected magic: " + i);
        }
        int i2 = dataInputStream.readInt();
        if (i2 == 16) {
            int i3 = dataInputStream.readInt();
            for (int i4 = 0; i4 < i3; i4++) {
                NetworkIdentitySet networkIdentitySet = new NetworkIdentitySet(dataInputStream);
                int i5 = dataInputStream.readInt();
                for (int i6 = 0; i6 < i5; i6++) {
                    recordHistory(new Key(networkIdentitySet, dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt()), new NetworkStatsHistory(dataInputStream));
                }
            }
            return;
        }
        throw new ProtocolException("unexpected version: " + i2);
    }

    public void write(DataOutputStream dataOutputStream) throws IOException {
        HashMap mapNewHashMap = Maps.newHashMap();
        for (Key key : this.mStats.keySet()) {
            ArrayList arrayListNewArrayList = (ArrayList) mapNewHashMap.get(key.ident);
            if (arrayListNewArrayList == null) {
                arrayListNewArrayList = Lists.newArrayList();
                mapNewHashMap.put(key.ident, arrayListNewArrayList);
            }
            arrayListNewArrayList.add(key);
        }
        dataOutputStream.writeInt(FILE_MAGIC);
        dataOutputStream.writeInt(16);
        dataOutputStream.writeInt(mapNewHashMap.size());
        for (NetworkIdentitySet networkIdentitySet : mapNewHashMap.keySet()) {
            ArrayList<Key> arrayList = (ArrayList) mapNewHashMap.get(networkIdentitySet);
            networkIdentitySet.writeToStream(dataOutputStream);
            dataOutputStream.writeInt(arrayList.size());
            for (Key key2 : arrayList) {
                NetworkStatsHistory networkStatsHistory = this.mStats.get(key2);
                dataOutputStream.writeInt(key2.uid);
                dataOutputStream.writeInt(key2.set);
                dataOutputStream.writeInt(key2.tag);
                networkStatsHistory.writeToStream(dataOutputStream);
            }
        }
        dataOutputStream.flush();
    }

    @Deprecated
    public void readLegacyNetwork(File file) throws Throwable {
        DataInputStream dataInputStream;
        Throwable th;
        int i;
        try {
            dataInputStream = new DataInputStream(new BufferedInputStream(new AtomicFile(file).openRead()));
        } catch (FileNotFoundException e) {
            dataInputStream = null;
        } catch (Throwable th2) {
            dataInputStream = null;
            th = th2;
        }
        try {
            i = dataInputStream.readInt();
        } catch (FileNotFoundException e2) {
        } catch (Throwable th3) {
            th = th3;
            IoUtils.closeQuietly(dataInputStream);
            throw th;
        }
        if (i != FILE_MAGIC) {
            throw new ProtocolException("unexpected magic: " + i);
        }
        int i2 = dataInputStream.readInt();
        if (i2 == 1) {
            int i3 = dataInputStream.readInt();
            for (int i4 = 0; i4 < i3; i4++) {
                recordHistory(new Key(new NetworkIdentitySet(dataInputStream), -1, -1, 0), new NetworkStatsHistory(dataInputStream));
            }
            IoUtils.closeQuietly(dataInputStream);
            return;
        }
        throw new ProtocolException("unexpected version: " + i2);
    }

    @Deprecated
    public void readLegacyUid(File file, boolean z) throws Throwable {
        DataInputStream dataInputStream;
        Throwable th;
        int i;
        try {
            dataInputStream = new DataInputStream(new BufferedInputStream(new AtomicFile(file).openRead()));
        } catch (FileNotFoundException e) {
            dataInputStream = null;
        } catch (Throwable th2) {
            dataInputStream = null;
            th = th2;
        }
        try {
            i = dataInputStream.readInt();
        } catch (FileNotFoundException e2) {
        } catch (Throwable th3) {
            th = th3;
            IoUtils.closeQuietly(dataInputStream);
            throw th;
        }
        if (i != FILE_MAGIC) {
            throw new ProtocolException("unexpected magic: " + i);
        }
        int i2 = dataInputStream.readInt();
        switch (i2) {
            case 1:
                break;
            case 2:
                break;
            case 3:
            case 4:
                int i3 = dataInputStream.readInt();
                for (int i4 = 0; i4 < i3; i4++) {
                    NetworkIdentitySet networkIdentitySet = new NetworkIdentitySet(dataInputStream);
                    int i5 = dataInputStream.readInt();
                    for (int i6 = 0; i6 < i5; i6++) {
                        int i7 = dataInputStream.readInt();
                        int i8 = i2 >= 4 ? dataInputStream.readInt() : 0;
                        int i9 = dataInputStream.readInt();
                        Key key = new Key(networkIdentitySet, i7, i8, i9);
                        NetworkStatsHistory networkStatsHistory = new NetworkStatsHistory(dataInputStream);
                        if ((i9 == 0) != z) {
                            recordHistory(key, networkStatsHistory);
                        }
                    }
                }
                break;
            default:
                throw new ProtocolException("unexpected version: " + i2);
        }
        IoUtils.closeQuietly(dataInputStream);
    }

    public void removeUids(int[] iArr) {
        ArrayList<Key> arrayListNewArrayList = Lists.newArrayList();
        arrayListNewArrayList.addAll(this.mStats.keySet());
        for (Key key : arrayListNewArrayList) {
            if (ArrayUtils.contains(iArr, key.uid)) {
                if (key.tag == 0) {
                    findOrCreateHistory(key.ident, -4, 0, 0).recordEntireHistory(this.mStats.get(key));
                }
                this.mStats.remove(key);
                this.mDirty = true;
            }
        }
    }

    private void noteRecordedHistory(long j, long j2, long j3) {
        if (j < this.mStartMillis) {
            this.mStartMillis = j;
        }
        if (j2 > this.mEndMillis) {
            this.mEndMillis = j2;
        }
        this.mTotalBytes += j3;
        this.mDirty = true;
    }

    private int estimateBuckets() {
        return (int) (Math.min(this.mEndMillis - this.mStartMillis, 3024000000L) / this.mBucketDuration);
    }

    private ArrayList<Key> getSortedKeys() {
        ArrayList<Key> arrayListNewArrayList = Lists.newArrayList();
        arrayListNewArrayList.addAll(this.mStats.keySet());
        Collections.sort(arrayListNewArrayList);
        return arrayListNewArrayList;
    }

    public void dump(IndentingPrintWriter indentingPrintWriter) {
        for (Key key : getSortedKeys()) {
            indentingPrintWriter.print("ident=");
            indentingPrintWriter.print(key.ident.toString());
            indentingPrintWriter.print(" uid=");
            indentingPrintWriter.print(key.uid);
            indentingPrintWriter.print(" set=");
            indentingPrintWriter.print(NetworkStats.setToString(key.set));
            indentingPrintWriter.print(" tag=");
            indentingPrintWriter.println(NetworkStats.tagToString(key.tag));
            NetworkStatsHistory networkStatsHistory = this.mStats.get(key);
            indentingPrintWriter.increaseIndent();
            networkStatsHistory.dump(indentingPrintWriter, true);
            indentingPrintWriter.decreaseIndent();
        }
    }

    public void writeToProto(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        for (Key key : getSortedKeys()) {
            long jStart2 = protoOutputStream.start(2246267895809L);
            long jStart3 = protoOutputStream.start(1146756268033L);
            key.ident.writeToProto(protoOutputStream, 1146756268033L);
            protoOutputStream.write(1120986464258L, key.uid);
            protoOutputStream.write(1120986464259L, key.set);
            protoOutputStream.write(1120986464260L, key.tag);
            protoOutputStream.end(jStart3);
            this.mStats.get(key).writeToProto(protoOutputStream, 1146756268034L);
            protoOutputStream.end(jStart2);
        }
        protoOutputStream.end(jStart);
    }

    public void dumpCheckin(PrintWriter printWriter, long j, long j2) {
        dumpCheckin(printWriter, j, j2, NetworkTemplate.buildTemplateMobileWildcard(), "cell");
        dumpCheckin(printWriter, j, j2, NetworkTemplate.buildTemplateWifiWildcard(), "wifi");
        dumpCheckin(printWriter, j, j2, NetworkTemplate.buildTemplateEthernet(), "eth");
        dumpCheckin(printWriter, j, j2, NetworkTemplate.buildTemplateBluetooth(), "bt");
    }

    private void dumpCheckin(PrintWriter printWriter, long j, long j2, NetworkTemplate networkTemplate, String str) {
        ArrayMap arrayMap = new ArrayMap();
        for (int i = 0; i < this.mStats.size(); i++) {
            Key keyKeyAt = this.mStats.keyAt(i);
            NetworkStatsHistory networkStatsHistoryValueAt = this.mStats.valueAt(i);
            if (templateMatches(networkTemplate, keyKeyAt.ident) && keyKeyAt.set < 1000) {
                Key key = new Key(null, keyKeyAt.uid, keyKeyAt.set, keyKeyAt.tag);
                NetworkStatsHistory networkStatsHistory = (NetworkStatsHistory) arrayMap.get(key);
                if (networkStatsHistory == null) {
                    networkStatsHistory = new NetworkStatsHistory(networkStatsHistoryValueAt.getBucketDuration());
                    arrayMap.put(key, networkStatsHistory);
                }
                networkStatsHistory.recordHistory(networkStatsHistoryValueAt, j, j2);
            }
        }
        for (int i2 = 0; i2 < arrayMap.size(); i2++) {
            Key key2 = (Key) arrayMap.keyAt(i2);
            NetworkStatsHistory networkStatsHistory2 = (NetworkStatsHistory) arrayMap.valueAt(i2);
            if (networkStatsHistory2.size() != 0) {
                printWriter.print("c,");
                printWriter.print(str);
                printWriter.print(',');
                printWriter.print(key2.uid);
                printWriter.print(',');
                printWriter.print(NetworkStats.setToCheckinString(key2.set));
                printWriter.print(',');
                printWriter.print(key2.tag);
                printWriter.println();
                networkStatsHistory2.dumpCheckin(printWriter);
            }
        }
    }

    private static boolean templateMatches(NetworkTemplate networkTemplate, NetworkIdentitySet networkIdentitySet) {
        Iterator<NetworkIdentity> it = networkIdentitySet.iterator();
        while (it.hasNext()) {
            if (networkTemplate.matches(it.next())) {
                return true;
            }
        }
        return false;
    }

    private static class Key implements Comparable<Key> {
        private final int hashCode;
        public final NetworkIdentitySet ident;
        public final int set;
        public final int tag;
        public final int uid;

        public Key(NetworkIdentitySet networkIdentitySet, int i, int i2, int i3) {
            this.ident = networkIdentitySet;
            this.uid = i;
            this.set = i2;
            this.tag = i3;
            this.hashCode = Objects.hash(networkIdentitySet, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
        }

        public int hashCode() {
            return this.hashCode;
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof Key)) {
                return false;
            }
            Key key = (Key) obj;
            return this.uid == key.uid && this.set == key.set && this.tag == key.tag && Objects.equals(this.ident, key.ident);
        }

        @Override
        public int compareTo(Key key) {
            int iCompare;
            if (this.ident != null && key.ident != null) {
                iCompare = this.ident.compareTo(key.ident);
            } else {
                iCompare = 0;
            }
            if (iCompare == 0) {
                iCompare = Integer.compare(this.uid, key.uid);
            }
            if (iCompare == 0) {
                iCompare = Integer.compare(this.set, key.set);
            }
            if (iCompare == 0) {
                return Integer.compare(this.tag, key.tag);
            }
            return iCompare;
        }
    }
}
