package com.android.server.net;

import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.os.Binder;
import android.os.DropBoxManager;
import android.util.Log;
import android.util.MathUtils;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.net.VpnInfo;
import com.android.internal.util.FileRotator;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.util.Preconditions;
import com.android.server.job.controllers.JobStatus;
import com.google.android.collect.Sets;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import libcore.io.IoUtils;

public class NetworkStatsRecorder {
    private static final boolean DUMP_BEFORE_DELETE = true;
    private static final boolean LOGD = true;
    private static final boolean LOGV = true;
    private static final String TAG = "NetworkStatsRecorder";
    private static final String TAG_NETSTATS_DUMP = "netstats_dump";
    private final long mBucketDuration;
    private WeakReference<NetworkStatsCollection> mComplete;
    private final String mCookie;
    private final DropBoxManager mDropBox;
    private NetworkStats mLastSnapshot;
    private final NetworkStats.NonMonotonicObserver<String> mObserver;
    private final boolean mOnlyTags;
    private final NetworkStatsCollection mPending;
    private final CombiningRewriter mPendingRewriter;
    private long mPersistThresholdBytes;
    private final FileRotator mRotator;
    private final NetworkStatsCollection mSinceBoot;

    public NetworkStatsRecorder() {
        this.mPersistThresholdBytes = 2097152L;
        this.mRotator = null;
        this.mObserver = null;
        this.mDropBox = null;
        this.mCookie = null;
        this.mBucketDuration = 31449600000L;
        this.mOnlyTags = false;
        this.mPending = null;
        this.mSinceBoot = new NetworkStatsCollection(this.mBucketDuration);
        this.mPendingRewriter = null;
    }

    public NetworkStatsRecorder(FileRotator fileRotator, NetworkStats.NonMonotonicObserver<String> nonMonotonicObserver, DropBoxManager dropBoxManager, String str, long j, boolean z) {
        this.mPersistThresholdBytes = 2097152L;
        this.mRotator = (FileRotator) Preconditions.checkNotNull(fileRotator, "missing FileRotator");
        this.mObserver = (NetworkStats.NonMonotonicObserver) Preconditions.checkNotNull(nonMonotonicObserver, "missing NonMonotonicObserver");
        this.mDropBox = (DropBoxManager) Preconditions.checkNotNull(dropBoxManager, "missing DropBoxManager");
        this.mCookie = str;
        this.mBucketDuration = j;
        this.mOnlyTags = z;
        this.mPending = new NetworkStatsCollection(j);
        this.mSinceBoot = new NetworkStatsCollection(j);
        this.mPendingRewriter = new CombiningRewriter(this.mPending);
    }

    public void setPersistThreshold(long j) {
        Slog.v(TAG, "setPersistThreshold() with " + j);
        this.mPersistThresholdBytes = MathUtils.constrain(j, 1024L, 104857600L);
    }

    public void resetLocked() {
        this.mLastSnapshot = null;
        if (this.mPending != null) {
            this.mPending.reset();
        }
        if (this.mSinceBoot != null) {
            this.mSinceBoot.reset();
        }
        if (this.mComplete != null) {
            this.mComplete.clear();
        }
    }

    public NetworkStats.Entry getTotalSinceBootLocked(NetworkTemplate networkTemplate) {
        return this.mSinceBoot.getSummary(networkTemplate, Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME, 3, Binder.getCallingUid()).getTotal((NetworkStats.Entry) null);
    }

    public NetworkStatsCollection getSinceBoot() {
        return this.mSinceBoot;
    }

    public NetworkStatsCollection getOrLoadCompleteLocked() {
        Preconditions.checkNotNull(this.mRotator, "missing FileRotator");
        NetworkStatsCollection networkStatsCollection = this.mComplete != null ? this.mComplete.get() : null;
        if (networkStatsCollection == null) {
            NetworkStatsCollection networkStatsCollectionLoadLocked = loadLocked(Long.MIN_VALUE, JobStatus.NO_LATEST_RUNTIME);
            this.mComplete = new WeakReference<>(networkStatsCollectionLoadLocked);
            return networkStatsCollectionLoadLocked;
        }
        return networkStatsCollection;
    }

    public NetworkStatsCollection getOrLoadPartialLocked(long j, long j2) {
        Preconditions.checkNotNull(this.mRotator, "missing FileRotator");
        NetworkStatsCollection networkStatsCollection = this.mComplete != null ? this.mComplete.get() : null;
        if (networkStatsCollection == null) {
            return loadLocked(j, j2);
        }
        return networkStatsCollection;
    }

    private NetworkStatsCollection loadLocked(long j, long j2) {
        Slog.d(TAG, "loadLocked() reading from disk for " + this.mCookie);
        NetworkStatsCollection networkStatsCollection = new NetworkStatsCollection(this.mBucketDuration);
        try {
            this.mRotator.readMatching(networkStatsCollection, j, j2);
            networkStatsCollection.recordCollection(this.mPending);
        } catch (IOException e) {
            Log.e(TAG, "problem completely reading network stats", e);
            recoverFromWtf();
        } catch (OutOfMemoryError e2) {
            Log.wtf(TAG, "problem completely reading network stats", e2);
            recoverFromWtf();
        }
        return networkStatsCollection;
    }

    public void recordSnapshotLocked(NetworkStats networkStats, Map<String, NetworkIdentitySet> map, VpnInfo[] vpnInfoArr, long j) {
        NetworkStats networkStats2;
        HashSet hashSetNewHashSet = Sets.newHashSet();
        if (networkStats == null) {
            return;
        }
        if (this.mLastSnapshot == null) {
            this.mLastSnapshot = networkStats;
            return;
        }
        NetworkStats.Entry values = null;
        NetworkStatsCollection networkStatsCollection = this.mComplete != null ? this.mComplete.get() : null;
        NetworkStats networkStatsSubtract = NetworkStats.subtract(networkStats, this.mLastSnapshot, this.mObserver, this.mCookie);
        long elapsedRealtime = j - networkStatsSubtract.getElapsedRealtime();
        if (vpnInfoArr != null) {
            for (VpnInfo vpnInfo : vpnInfoArr) {
                networkStatsSubtract.migrateTun(vpnInfo.ownerUid, vpnInfo.vpnIface, vpnInfo.primaryUnderlyingIface);
            }
        }
        int i = 0;
        while (i < networkStatsSubtract.size()) {
            values = networkStatsSubtract.getValues(i, values);
            if (values.isNegative()) {
                if (this.mObserver != null) {
                    this.mObserver.foundNonMonotonic(networkStatsSubtract, i, this.mCookie);
                }
                values.rxBytes = Math.max(values.rxBytes, 0L);
                values.rxPackets = Math.max(values.rxPackets, 0L);
                values.txBytes = Math.max(values.txBytes, 0L);
                values.txPackets = Math.max(values.txPackets, 0L);
                values.operations = Math.max(values.operations, 0L);
            }
            NetworkIdentitySet networkIdentitySet = map.get(values.iface);
            if (networkIdentitySet == null) {
                hashSetNewHashSet.add(values.iface);
            } else {
                if (!values.isEmpty()) {
                    if (values.isNegative()) {
                        Slog.e(TAG, "tried recording negative data:" + values);
                    } else if ((values.tag == 0) != this.mOnlyTags) {
                        if (this.mPending != null) {
                            this.mPending.recordData(networkIdentitySet, values.uid, values.set, values.tag, elapsedRealtime, j, values);
                        }
                        Slog.i(TAG, "recordSnapshotLocked: ident[" + networkIdentitySet + "]");
                        if (this.mSinceBoot != null) {
                            this.mSinceBoot.recordData(networkIdentitySet, values.uid, values.set, values.tag, elapsedRealtime, j, values);
                        }
                        if (networkStatsCollection != null) {
                            networkStats2 = networkStatsSubtract;
                            networkStatsCollection.recordData(networkIdentitySet, values.uid, values.set, values.tag, elapsedRealtime, j, values);
                        }
                    }
                }
                i++;
                networkStatsSubtract = networkStats2;
            }
            networkStats2 = networkStatsSubtract;
            i++;
            networkStatsSubtract = networkStats2;
        }
        this.mLastSnapshot = networkStats;
        if (hashSetNewHashSet.size() > 0) {
            Slog.w(TAG, "unknown interfaces " + hashSetNewHashSet + ", ignoring those stats");
        }
    }

    public void maybePersistLocked(long j) {
        Preconditions.checkNotNull(this.mRotator, "missing FileRotator");
        if (this.mPending.getTotalBytes() >= this.mPersistThresholdBytes) {
            forcePersistLocked(j);
        } else {
            this.mRotator.maybeRotate(j);
        }
    }

    public void forcePersistLocked(long j) {
        Preconditions.checkNotNull(this.mRotator, "missing FileRotator");
        if (this.mPending.isDirty()) {
            Slog.d(TAG, "forcePersistLocked() writing for " + this.mCookie);
            try {
                this.mRotator.rewriteActive(this.mPendingRewriter, j);
                this.mRotator.maybeRotate(j);
                this.mPending.reset();
            } catch (IOException e) {
                Log.e(TAG, "problem persisting pending stats", e);
                recoverFromWtf();
            } catch (OutOfMemoryError e2) {
                Log.wtf(TAG, "problem persisting pending stats", e2);
                recoverFromWtf();
            }
        }
    }

    public void removeUidsLocked(int[] iArr) {
        if (this.mRotator != null) {
            try {
                this.mRotator.rewriteAll(new RemoveUidRewriter(this.mBucketDuration, iArr));
            } catch (IOException e) {
                Log.e(TAG, "problem removing UIDs " + Arrays.toString(iArr), e);
                recoverFromWtf();
            } catch (OutOfMemoryError e2) {
                Log.wtf(TAG, "problem removing UIDs " + Arrays.toString(iArr), e2);
                recoverFromWtf();
            }
        }
        if (this.mPending != null) {
            this.mPending.removeUids(iArr);
        }
        if (this.mSinceBoot != null) {
            this.mSinceBoot.removeUids(iArr);
        }
        if (this.mLastSnapshot != null) {
            this.mLastSnapshot = this.mLastSnapshot.withoutUids(iArr);
        }
        NetworkStatsCollection networkStatsCollection = this.mComplete != null ? this.mComplete.get() : null;
        if (networkStatsCollection != null) {
            networkStatsCollection.removeUids(iArr);
        }
    }

    private static class CombiningRewriter implements FileRotator.Rewriter {
        private final NetworkStatsCollection mCollection;

        public CombiningRewriter(NetworkStatsCollection networkStatsCollection) {
            this.mCollection = (NetworkStatsCollection) Preconditions.checkNotNull(networkStatsCollection, "missing NetworkStatsCollection");
        }

        public void reset() {
        }

        public void read(InputStream inputStream) throws IOException {
            this.mCollection.read(inputStream);
        }

        public boolean shouldWrite() {
            return true;
        }

        public void write(OutputStream outputStream) throws IOException {
            this.mCollection.write(new DataOutputStream(outputStream));
            this.mCollection.reset();
        }
    }

    public static class RemoveUidRewriter implements FileRotator.Rewriter {
        private final NetworkStatsCollection mTemp;
        private final int[] mUids;

        public RemoveUidRewriter(long j, int[] iArr) {
            this.mTemp = new NetworkStatsCollection(j);
            this.mUids = iArr;
        }

        public void reset() {
            this.mTemp.reset();
        }

        public void read(InputStream inputStream) throws IOException {
            this.mTemp.read(inputStream);
            this.mTemp.clearDirty();
            this.mTemp.removeUids(this.mUids);
        }

        public boolean shouldWrite() {
            return this.mTemp.isDirty();
        }

        public void write(OutputStream outputStream) throws IOException {
            this.mTemp.write(new DataOutputStream(outputStream));
        }
    }

    public void importLegacyNetworkLocked(File file) throws Throwable {
        Preconditions.checkNotNull(this.mRotator, "missing FileRotator");
        this.mRotator.deleteAll();
        NetworkStatsCollection networkStatsCollection = new NetworkStatsCollection(this.mBucketDuration);
        networkStatsCollection.readLegacyNetwork(file);
        long startMillis = networkStatsCollection.getStartMillis();
        long endMillis = networkStatsCollection.getEndMillis();
        if (!networkStatsCollection.isEmpty()) {
            this.mRotator.rewriteActive(new CombiningRewriter(networkStatsCollection), startMillis);
            this.mRotator.maybeRotate(endMillis);
        }
    }

    public void importLegacyUidLocked(File file) throws Throwable {
        Preconditions.checkNotNull(this.mRotator, "missing FileRotator");
        this.mRotator.deleteAll();
        NetworkStatsCollection networkStatsCollection = new NetworkStatsCollection(this.mBucketDuration);
        networkStatsCollection.readLegacyUid(file, this.mOnlyTags);
        long startMillis = networkStatsCollection.getStartMillis();
        long endMillis = networkStatsCollection.getEndMillis();
        if (!networkStatsCollection.isEmpty()) {
            this.mRotator.rewriteActive(new CombiningRewriter(networkStatsCollection), startMillis);
            this.mRotator.maybeRotate(endMillis);
        }
    }

    public void dumpLocked(IndentingPrintWriter indentingPrintWriter, boolean z) {
        if (this.mPending != null) {
            indentingPrintWriter.print("Pending bytes: ");
            indentingPrintWriter.println(this.mPending.getTotalBytes());
        }
        if (z) {
            indentingPrintWriter.println("Complete history:");
            getOrLoadCompleteLocked().dump(indentingPrintWriter);
        } else {
            indentingPrintWriter.println("History since boot:");
            this.mSinceBoot.dump(indentingPrintWriter);
        }
    }

    public void writeToProtoLocked(ProtoOutputStream protoOutputStream, long j) {
        long jStart = protoOutputStream.start(j);
        if (this.mPending != null) {
            protoOutputStream.write(1112396529665L, this.mPending.getTotalBytes());
        }
        getOrLoadCompleteLocked().writeToProto(protoOutputStream, 1146756268034L);
        protoOutputStream.end(jStart);
    }

    public void dumpCheckin(PrintWriter printWriter, long j, long j2) {
        getOrLoadPartialLocked(j, j2).dumpCheckin(printWriter, j, j2);
    }

    private void recoverFromWtf() {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try {
            try {
                this.mRotator.dumpAll(byteArrayOutputStream);
            } catch (IOException e) {
                byteArrayOutputStream.reset();
            }
            this.mDropBox.addData(TAG_NETSTATS_DUMP, byteArrayOutputStream.toByteArray(), 0);
            this.mRotator.deleteAll();
        } finally {
            IoUtils.closeQuietly(byteArrayOutputStream);
        }
    }
}
