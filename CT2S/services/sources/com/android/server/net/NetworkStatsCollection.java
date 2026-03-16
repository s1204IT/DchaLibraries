package com.android.server.net;

import android.net.NetworkIdentity;
import android.net.NetworkStats;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.util.ArrayMap;
import android.util.AtomicFile;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    public NetworkStatsCollection(long bucketDuration) {
        this.mBucketDuration = bucketDuration;
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

    public NetworkStatsHistory getHistory(NetworkTemplate template, int uid, int set, int tag, int fields) {
        return getHistory(template, uid, set, tag, fields, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME);
    }

    public NetworkStatsHistory getHistory(NetworkTemplate template, int uid, int set, int tag, int fields, long start, long end) {
        NetworkStatsHistory combined = new NetworkStatsHistory(this.mBucketDuration, estimateBuckets(), fields);
        for (int i = 0; i < this.mStats.size(); i++) {
            Key key = this.mStats.keyAt(i);
            boolean setMatches = set == -1 || key.set == set;
            if (key.uid == uid && setMatches && key.tag == tag && templateMatches(template, key.ident)) {
                NetworkStatsHistory value = this.mStats.valueAt(i);
                combined.recordHistory(value, start, end);
            }
        }
        return combined;
    }

    public NetworkStats getSummary(NetworkTemplate template, long start, long end) {
        long now = System.currentTimeMillis();
        NetworkStats stats = new NetworkStats(end - start, 24);
        NetworkStats.Entry entry = new NetworkStats.Entry();
        NetworkStatsHistory.Entry historyEntry = null;
        if (start != end) {
            for (int i = 0; i < this.mStats.size(); i++) {
                Key key = this.mStats.keyAt(i);
                if (templateMatches(template, key.ident)) {
                    NetworkStatsHistory value = this.mStats.valueAt(i);
                    historyEntry = value.getValues(start, end, now, historyEntry);
                    entry.iface = NetworkStats.IFACE_ALL;
                    entry.uid = key.uid;
                    entry.set = key.set;
                    entry.tag = key.tag;
                    entry.rxBytes = historyEntry.rxBytes;
                    entry.rxPackets = historyEntry.rxPackets;
                    entry.txBytes = historyEntry.txBytes;
                    entry.txPackets = historyEntry.txPackets;
                    entry.operations = historyEntry.operations;
                    if (!entry.isEmpty()) {
                        stats.combineValues(entry);
                    }
                }
            }
        }
        return stats;
    }

    public void recordData(NetworkIdentitySet ident, int uid, int set, int tag, long start, long end, NetworkStats.Entry entry) {
        NetworkStatsHistory history = findOrCreateHistory(ident, uid, set, tag);
        history.recordData(start, end, entry);
        noteRecordedHistory(history.getStart(), history.getEnd(), entry.txBytes + entry.rxBytes);
    }

    private void recordHistory(Key key, NetworkStatsHistory history) {
        if (history.size() != 0) {
            noteRecordedHistory(history.getStart(), history.getEnd(), history.getTotalBytes());
            NetworkStatsHistory target = this.mStats.get(key);
            if (target == null) {
                target = new NetworkStatsHistory(history.getBucketDuration());
                this.mStats.put(key, target);
            }
            target.recordEntireHistory(history);
        }
    }

    public void recordCollection(NetworkStatsCollection another) {
        for (int i = 0; i < another.mStats.size(); i++) {
            Key key = another.mStats.keyAt(i);
            NetworkStatsHistory value = another.mStats.valueAt(i);
            recordHistory(key, value);
        }
    }

    private NetworkStatsHistory findOrCreateHistory(NetworkIdentitySet ident, int uid, int set, int tag) {
        Key key = new Key(ident, uid, set, tag);
        NetworkStatsHistory existing = this.mStats.get(key);
        NetworkStatsHistory updated = null;
        if (existing == null) {
            updated = new NetworkStatsHistory(this.mBucketDuration, 10);
        } else if (existing.getBucketDuration() != this.mBucketDuration) {
            updated = new NetworkStatsHistory(existing, this.mBucketDuration);
        }
        if (updated == null) {
            return existing;
        }
        this.mStats.put(key, updated);
        return updated;
    }

    public void read(InputStream in) throws IOException {
        read(new DataInputStream(in));
    }

    public void read(DataInputStream in) throws IOException {
        int magic = in.readInt();
        if (magic != FILE_MAGIC) {
            throw new ProtocolException("unexpected magic: " + magic);
        }
        int version = in.readInt();
        switch (version) {
            case 16:
                int identSize = in.readInt();
                for (int i = 0; i < identSize; i++) {
                    NetworkIdentitySet ident = new NetworkIdentitySet(in);
                    int size = in.readInt();
                    for (int j = 0; j < size; j++) {
                        int uid = in.readInt();
                        int set = in.readInt();
                        int tag = in.readInt();
                        Key key = new Key(ident, uid, set, tag);
                        NetworkStatsHistory history = new NetworkStatsHistory(in);
                        recordHistory(key, history);
                    }
                }
                return;
            default:
                throw new ProtocolException("unexpected version: " + version);
        }
    }

    public void write(DataOutputStream out) throws IOException {
        HashMap<NetworkIdentitySet, ArrayList<Key>> keysByIdent = Maps.newHashMap();
        for (Key key : this.mStats.keySet()) {
            ArrayList<Key> keys = keysByIdent.get(key.ident);
            if (keys == null) {
                keys = Lists.newArrayList();
                keysByIdent.put(key.ident, keys);
            }
            keys.add(key);
        }
        out.writeInt(FILE_MAGIC);
        out.writeInt(16);
        out.writeInt(keysByIdent.size());
        for (NetworkIdentitySet ident : keysByIdent.keySet()) {
            ArrayList<Key> keys2 = keysByIdent.get(ident);
            ident.writeToStream(out);
            out.writeInt(keys2.size());
            for (Key key2 : keys2) {
                NetworkStatsHistory history = this.mStats.get(key2);
                out.writeInt(key2.uid);
                out.writeInt(key2.set);
                out.writeInt(key2.tag);
                history.writeToStream(out);
            }
        }
        out.flush();
    }

    @Deprecated
    public void readLegacyNetwork(File file) throws Throwable {
        DataInputStream in;
        AtomicFile inputFile = new AtomicFile(file);
        DataInputStream in2 = null;
        try {
            in = new DataInputStream(new BufferedInputStream(inputFile.openRead()));
        } catch (FileNotFoundException e) {
        } catch (Throwable th) {
            th = th;
        }
        try {
            int magic = in.readInt();
            if (magic != FILE_MAGIC) {
                throw new ProtocolException("unexpected magic: " + magic);
            }
            int version = in.readInt();
            switch (version) {
                case 1:
                    int size = in.readInt();
                    for (int i = 0; i < size; i++) {
                        NetworkIdentitySet ident = new NetworkIdentitySet(in);
                        NetworkStatsHistory history = new NetworkStatsHistory(in);
                        Key key = new Key(ident, -1, -1, 0);
                        recordHistory(key, history);
                    }
                    IoUtils.closeQuietly(in);
                    return;
                default:
                    throw new ProtocolException("unexpected version: " + version);
            }
        } catch (FileNotFoundException e2) {
            in2 = in;
            IoUtils.closeQuietly(in2);
        } catch (Throwable th2) {
            th = th2;
            in2 = in;
            IoUtils.closeQuietly(in2);
            throw th;
        }
    }

    @Deprecated
    public void readLegacyUid(File file, boolean onlyTags) throws Throwable {
        DataInputStream in;
        AtomicFile inputFile = new AtomicFile(file);
        DataInputStream in2 = null;
        try {
            in = new DataInputStream(new BufferedInputStream(inputFile.openRead()));
        } catch (FileNotFoundException e) {
        } catch (Throwable th) {
            th = th;
        }
        try {
            int magic = in.readInt();
            if (magic != FILE_MAGIC) {
                throw new ProtocolException("unexpected magic: " + magic);
            }
            int version = in.readInt();
            switch (version) {
                case 1:
                case 2:
                    break;
                case 3:
                case 4:
                    int identSize = in.readInt();
                    for (int i = 0; i < identSize; i++) {
                        NetworkIdentitySet ident = new NetworkIdentitySet(in);
                        int size = in.readInt();
                        for (int j = 0; j < size; j++) {
                            int uid = in.readInt();
                            int set = version >= 4 ? in.readInt() : 0;
                            int tag = in.readInt();
                            Key key = new Key(ident, uid, set, tag);
                            NetworkStatsHistory history = new NetworkStatsHistory(in);
                            if ((tag == 0) != onlyTags) {
                                recordHistory(key, history);
                            }
                        }
                    }
                    break;
                default:
                    throw new ProtocolException("unexpected version: " + version);
            }
            IoUtils.closeQuietly(in);
        } catch (FileNotFoundException e2) {
            in2 = in;
            IoUtils.closeQuietly(in2);
        } catch (Throwable th2) {
            th = th2;
            in2 = in;
            IoUtils.closeQuietly(in2);
            throw th;
        }
    }

    public void removeUids(int[] uids) {
        ArrayList<Key> knownKeys = Lists.newArrayList();
        knownKeys.addAll(this.mStats.keySet());
        for (Key key : knownKeys) {
            if (ArrayUtils.contains(uids, key.uid)) {
                if (key.tag == 0) {
                    NetworkStatsHistory uidHistory = this.mStats.get(key);
                    NetworkStatsHistory removedHistory = findOrCreateHistory(key.ident, -4, 0, 0);
                    removedHistory.recordEntireHistory(uidHistory);
                }
                this.mStats.remove(key);
                this.mDirty = true;
            }
        }
    }

    private void noteRecordedHistory(long startMillis, long endMillis, long totalBytes) {
        if (startMillis < this.mStartMillis) {
            this.mStartMillis = startMillis;
        }
        if (endMillis > this.mEndMillis) {
            this.mEndMillis = endMillis;
        }
        this.mTotalBytes += totalBytes;
        this.mDirty = true;
    }

    private int estimateBuckets() {
        return (int) (Math.min(this.mEndMillis - this.mStartMillis, 3024000000L) / this.mBucketDuration);
    }

    public void dump(IndentingPrintWriter pw) {
        ArrayList<Key> keys = Lists.newArrayList();
        keys.addAll(this.mStats.keySet());
        Collections.sort(keys);
        for (Key key : keys) {
            pw.print("ident=");
            pw.print(key.ident.toString());
            pw.print(" uid=");
            pw.print(key.uid);
            pw.print(" set=");
            pw.print(NetworkStats.setToString(key.set));
            pw.print(" tag=");
            pw.println(NetworkStats.tagToString(key.tag));
            NetworkStatsHistory history = this.mStats.get(key);
            pw.increaseIndent();
            history.dump(pw, true);
            pw.decreaseIndent();
        }
    }

    public void dumpCheckin(PrintWriter pw, long start, long end) {
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateMobileWildcard(), "cell");
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateWifiWildcard(), "wifi");
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateEthernet(), "eth");
        dumpCheckin(pw, start, end, NetworkTemplate.buildTemplateBluetooth(), "bt");
    }

    private void dumpCheckin(PrintWriter pw, long start, long end, NetworkTemplate groupTemplate, String groupPrefix) {
        ArrayMap<Key, NetworkStatsHistory> grouped = new ArrayMap<>();
        for (int i = 0; i < this.mStats.size(); i++) {
            Key key = this.mStats.keyAt(i);
            NetworkStatsHistory value = this.mStats.valueAt(i);
            if (templateMatches(groupTemplate, key.ident)) {
                Key groupKey = new Key(null, key.uid, key.set, key.tag);
                NetworkStatsHistory groupHistory = grouped.get(groupKey);
                if (groupHistory == null) {
                    groupHistory = new NetworkStatsHistory(value.getBucketDuration());
                    grouped.put(groupKey, groupHistory);
                }
                groupHistory.recordHistory(value, start, end);
            }
        }
        for (int i2 = 0; i2 < grouped.size(); i2++) {
            Key key2 = grouped.keyAt(i2);
            NetworkStatsHistory value2 = grouped.valueAt(i2);
            if (value2.size() != 0) {
                pw.print("c,");
                pw.print(groupPrefix);
                pw.print(',');
                pw.print(key2.uid);
                pw.print(',');
                pw.print(NetworkStats.setToCheckinString(key2.set));
                pw.print(',');
                pw.print(key2.tag);
                pw.println();
                value2.dumpCheckin(pw);
            }
        }
    }

    private static boolean templateMatches(NetworkTemplate template, NetworkIdentitySet identSet) {
        for (NetworkIdentity ident : identSet) {
            if (template.matches(ident)) {
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

        public Key(NetworkIdentitySet ident, int uid, int set, int tag) {
            this.ident = ident;
            this.uid = uid;
            this.set = set;
            this.tag = tag;
            this.hashCode = Objects.hash(ident, Integer.valueOf(uid), Integer.valueOf(set), Integer.valueOf(tag));
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
        public int compareTo(Key another) {
            int res = 0;
            if (this.ident != null && another.ident != null) {
                res = this.ident.compareTo(another.ident);
            }
            if (res == 0) {
                res = Integer.compare(this.uid, another.uid);
            }
            if (res == 0) {
                res = Integer.compare(this.set, another.set);
            }
            if (res == 0) {
                return Integer.compare(this.tag, another.tag);
            }
            return res;
        }
    }
}
