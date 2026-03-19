package com.android.server.job;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
import android.net.NetworkRequest;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.text.format.DateUtils;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.BitUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.IoThread;
import com.android.server.LocalServices;
import com.android.server.audio.AudioService;
import com.android.server.content.SyncJobService;
import com.android.server.job.JobSchedulerInternal;
import com.android.server.job.JobStore;
import com.android.server.job.controllers.JobStatus;
import com.android.server.net.watchlist.WatchlistLoggingHandler;
import com.android.server.pm.PackageManagerService;
import com.android.server.pm.Settings;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public final class JobStore {
    private static final int JOBS_FILE_VERSION = 0;
    private static final int MAX_OPS_BEFORE_WRITE = 1;
    private static final String TAG = "JobStore";
    private static final String XML_TAG_EXTRAS = "extras";
    private static final String XML_TAG_ONEOFF = "one-off";
    private static final String XML_TAG_PARAMS_CONSTRAINTS = "constraints";
    private static final String XML_TAG_PERIODIC = "periodic";
    private static JobStore sSingleton;
    final Context mContext;
    final JobSet mJobSet;
    private final AtomicFile mJobsFile;
    final Object mLock;
    private boolean mRtcGood;
    private final long mXmlTimestamp;
    private static final boolean DEBUG = JobSchedulerService.DEBUG;
    private static final Object sSingletonLock = new Object();
    private final Handler mIoHandler = IoThread.getHandler();
    private JobSchedulerInternal.JobStorePersistStats mPersistInfo = new JobSchedulerInternal.JobStorePersistStats();
    private final Runnable mWriteRunnable = new AnonymousClass1();
    private int mDirtyOperations = 0;

    static JobStore initAndGet(JobSchedulerService jobSchedulerService) {
        JobStore jobStore;
        synchronized (sSingletonLock) {
            if (sSingleton == null) {
                sSingleton = new JobStore(jobSchedulerService.getContext(), jobSchedulerService.getLock(), Environment.getDataDirectory());
            }
            jobStore = sSingleton;
        }
        return jobStore;
    }

    @VisibleForTesting
    public static JobStore initAndGetForTesting(Context context, File file) {
        JobStore jobStore = new JobStore(context, new Object(), file);
        jobStore.clear();
        return jobStore;
    }

    private JobStore(Context context, Object obj, File file) throws Throwable {
        this.mLock = obj;
        this.mContext = context;
        File file2 = new File(new File(file, "system"), "job");
        file2.mkdirs();
        this.mJobsFile = new AtomicFile(new File(file2, "jobs.xml"), "jobs");
        this.mJobSet = new JobSet();
        this.mXmlTimestamp = this.mJobsFile.getLastModifiedTime();
        this.mRtcGood = JobSchedulerService.sSystemClock.millis() > this.mXmlTimestamp;
        readJobMapFromDisk(this.mJobSet, this.mRtcGood);
    }

    public boolean jobTimesInflatedValid() {
        return this.mRtcGood;
    }

    public boolean clockNowValidToInflate(long j) {
        return j >= this.mXmlTimestamp;
    }

    public void getRtcCorrectedJobsLocked(final ArrayList<JobStatus> arrayList, final ArrayList<JobStatus> arrayList2) {
        final long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
        forEachJob(new Consumer() {
            @Override
            public final void accept(Object obj) {
                JobStore.lambda$getRtcCorrectedJobsLocked$0(jMillis, arrayList, arrayList2, (JobStatus) obj);
            }
        });
    }

    static void lambda$getRtcCorrectedJobsLocked$0(long j, ArrayList arrayList, ArrayList arrayList2, JobStatus jobStatus) {
        Pair<Long, Long> persistedUtcTimes = jobStatus.getPersistedUtcTimes();
        if (persistedUtcTimes != null) {
            Pair<Long, Long> pairConvertRtcBoundsToElapsed = convertRtcBoundsToElapsed(persistedUtcTimes, j);
            arrayList.add(new JobStatus(jobStatus, jobStatus.getBaseHeartbeat(), ((Long) pairConvertRtcBoundsToElapsed.first).longValue(), ((Long) pairConvertRtcBoundsToElapsed.second).longValue(), 0, jobStatus.getLastSuccessfulRunTime(), jobStatus.getLastFailedRunTime()));
            arrayList2.add(jobStatus);
        }
    }

    public boolean add(JobStatus jobStatus) {
        boolean zRemove = this.mJobSet.remove(jobStatus);
        this.mJobSet.add(jobStatus);
        if (jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
        }
        if (DEBUG) {
            Slog.d(TAG, "Added job status to store: " + jobStatus);
        }
        return zRemove;
    }

    boolean containsJob(JobStatus jobStatus) {
        return this.mJobSet.contains(jobStatus);
    }

    public int size() {
        return this.mJobSet.size();
    }

    public JobSchedulerInternal.JobStorePersistStats getPersistStats() {
        return this.mPersistInfo;
    }

    public int countJobsForUid(int i) {
        return this.mJobSet.countJobsForUid(i);
    }

    public boolean remove(JobStatus jobStatus, boolean z) {
        boolean zRemove = this.mJobSet.remove(jobStatus);
        if (!zRemove) {
            if (DEBUG) {
                Slog.d(TAG, "Couldn't remove job: didn't exist: " + jobStatus);
                return false;
            }
            return false;
        }
        if (z && jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
        }
        return zRemove;
    }

    public void removeJobsOfNonUsers(int[] iArr) {
        this.mJobSet.removeJobsOfNonUsers(iArr);
    }

    @VisibleForTesting
    public void clear() {
        this.mJobSet.clear();
        maybeWriteStatusToDiskAsync();
    }

    public List<JobStatus> getJobsByUser(int i) {
        return this.mJobSet.getJobsByUser(i);
    }

    public List<JobStatus> getJobsByUid(int i) {
        return this.mJobSet.getJobsByUid(i);
    }

    public JobStatus getJobByUidAndJobId(int i, int i2) {
        return this.mJobSet.get(i, i2);
    }

    public void forEachJob(Consumer<JobStatus> consumer) {
        this.mJobSet.forEachJob((Predicate<JobStatus>) null, consumer);
    }

    public void forEachJob(Predicate<JobStatus> predicate, Consumer<JobStatus> consumer) {
        this.mJobSet.forEachJob(predicate, consumer);
    }

    public void forEachJob(int i, Consumer<JobStatus> consumer) {
        this.mJobSet.forEachJob(i, consumer);
    }

    public void forEachJobForSourceUid(int i, Consumer<JobStatus> consumer) {
        this.mJobSet.forEachJobForSourceUid(i, consumer);
    }

    private void maybeWriteStatusToDiskAsync() {
        this.mDirtyOperations++;
        if (this.mDirtyOperations >= 1) {
            if (DEBUG) {
                Slog.v(TAG, "Writing jobs to disk.");
            }
            this.mIoHandler.removeCallbacks(this.mWriteRunnable);
            this.mIoHandler.post(this.mWriteRunnable);
        }
    }

    @VisibleForTesting
    public void readJobMapFromDisk(JobSet jobSet, boolean z) throws Throwable {
        new ReadJobMapFromDiskRunnable(jobSet, z).run();
    }

    class AnonymousClass1 implements Runnable {
        AnonymousClass1() {
        }

        @Override
        public void run() throws Throwable {
            long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
            final ArrayList arrayList = new ArrayList();
            synchronized (JobStore.this.mLock) {
                JobStore.this.mJobSet.forEachJob((Predicate<JobStatus>) null, new Consumer() {
                    @Override
                    public final void accept(Object obj) {
                        JobStore.AnonymousClass1.lambda$run$0(arrayList, (JobStatus) obj);
                    }
                });
            }
            writeJobsMapImpl(arrayList);
            if (JobStore.DEBUG) {
                Slog.v(JobStore.TAG, "Finished writing, took " + (JobSchedulerService.sElapsedRealtimeClock.millis() - jMillis) + "ms");
            }
        }

        static void lambda$run$0(List list, JobStatus jobStatus) {
            if (jobStatus.isPersisted()) {
                list.add(new JobStatus(jobStatus));
            }
        }

        private void writeJobsMapImpl(List<JobStatus> list) throws Throwable {
            int i;
            int i2;
            int i3;
            int i4 = 0;
            try {
                try {
                    long jUptimeMillis = SystemClock.uptimeMillis();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                    fastXmlSerializer.setOutput(byteArrayOutputStream, StandardCharsets.UTF_8.name());
                    fastXmlSerializer.startDocument(null, true);
                    fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                    fastXmlSerializer.startTag(null, "job-info");
                    fastXmlSerializer.attribute(null, "version", Integer.toString(0));
                    i = 0;
                    i2 = 0;
                    i3 = 0;
                    for (int i5 = 0; i5 < list.size(); i5++) {
                        try {
                            JobStatus jobStatus = list.get(i5);
                            if (JobStore.DEBUG) {
                                Slog.d(JobStore.TAG, "Saving job " + jobStatus.getJobId());
                            }
                            fastXmlSerializer.startTag(null, "job");
                            addAttributesToJobTag(fastXmlSerializer, jobStatus);
                            writeConstraintsToXml(fastXmlSerializer, jobStatus);
                            writeExecutionCriteriaToXml(fastXmlSerializer, jobStatus);
                            writeBundleToXml(jobStatus.getJob().getExtras(), fastXmlSerializer);
                            fastXmlSerializer.endTag(null, "job");
                            i++;
                            if (jobStatus.getUid() == 1000) {
                                i2++;
                                if (JobStore.isSyncJob(jobStatus)) {
                                    i3++;
                                }
                            }
                        } catch (IOException e) {
                            e = e;
                            i4 = i3;
                            if (JobStore.DEBUG) {
                                Slog.v(JobStore.TAG, "Error writing out job data.", e);
                            }
                            JobStore.this.mPersistInfo.countAllJobsSaved = i;
                            JobStore.this.mPersistInfo.countSystemServerJobsSaved = i2;
                            JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = i4;
                            return;
                        } catch (XmlPullParserException e2) {
                            e = e2;
                            i4 = i3;
                            if (JobStore.DEBUG) {
                                Slog.d(JobStore.TAG, "Error persisting bundle.", e);
                            }
                            JobStore.this.mPersistInfo.countAllJobsSaved = i;
                            JobStore.this.mPersistInfo.countSystemServerJobsSaved = i2;
                            JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = i4;
                            return;
                        } catch (Throwable th) {
                            th = th;
                            JobStore.this.mPersistInfo.countAllJobsSaved = i;
                            JobStore.this.mPersistInfo.countSystemServerJobsSaved = i2;
                            JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = i3;
                            throw th;
                        }
                    }
                    fastXmlSerializer.endTag(null, "job-info");
                    fastXmlSerializer.endDocument();
                    FileOutputStream fileOutputStreamStartWrite = JobStore.this.mJobsFile.startWrite(jUptimeMillis);
                    fileOutputStreamStartWrite.write(byteArrayOutputStream.toByteArray());
                    JobStore.this.mJobsFile.finishWrite(fileOutputStreamStartWrite);
                    JobStore.this.mDirtyOperations = 0;
                    JobStore.this.mPersistInfo.countAllJobsSaved = i;
                    JobStore.this.mPersistInfo.countSystemServerJobsSaved = i2;
                    JobStore.this.mPersistInfo.countSystemSyncManagerJobsSaved = i3;
                } catch (Throwable th2) {
                    th = th2;
                    i3 = i4;
                }
            } catch (IOException e3) {
                e = e3;
                i = 0;
                i2 = 0;
            } catch (XmlPullParserException e4) {
                e = e4;
                i = 0;
                i2 = 0;
            } catch (Throwable th3) {
                th = th3;
                i = 0;
                i2 = 0;
                i3 = 0;
            }
        }

        private void addAttributesToJobTag(XmlSerializer xmlSerializer, JobStatus jobStatus) throws IOException {
            xmlSerializer.attribute(null, "jobid", Integer.toString(jobStatus.getJobId()));
            xmlSerializer.attribute(null, Settings.ATTR_PACKAGE, jobStatus.getServiceComponent().getPackageName());
            xmlSerializer.attribute(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS, jobStatus.getServiceComponent().getClassName());
            if (jobStatus.getSourcePackageName() != null) {
                xmlSerializer.attribute(null, "sourcePackageName", jobStatus.getSourcePackageName());
            }
            if (jobStatus.getSourceTag() != null) {
                xmlSerializer.attribute(null, "sourceTag", jobStatus.getSourceTag());
            }
            xmlSerializer.attribute(null, "sourceUserId", String.valueOf(jobStatus.getSourceUserId()));
            xmlSerializer.attribute(null, WatchlistLoggingHandler.WatchlistEventKeys.UID, Integer.toString(jobStatus.getUid()));
            xmlSerializer.attribute(null, "priority", String.valueOf(jobStatus.getPriority()));
            xmlSerializer.attribute(null, "flags", String.valueOf(jobStatus.getFlags()));
            if (jobStatus.getInternalFlags() != 0) {
                xmlSerializer.attribute(null, "internalFlags", String.valueOf(jobStatus.getInternalFlags()));
            }
            xmlSerializer.attribute(null, "lastSuccessfulRunTime", String.valueOf(jobStatus.getLastSuccessfulRunTime()));
            xmlSerializer.attribute(null, "lastFailedRunTime", String.valueOf(jobStatus.getLastFailedRunTime()));
        }

        private void writeBundleToXml(PersistableBundle persistableBundle, XmlSerializer xmlSerializer) throws XmlPullParserException, IOException {
            xmlSerializer.startTag(null, JobStore.XML_TAG_EXTRAS);
            deepCopyBundle(persistableBundle, 10).saveToXml(xmlSerializer);
            xmlSerializer.endTag(null, JobStore.XML_TAG_EXTRAS);
        }

        private PersistableBundle deepCopyBundle(PersistableBundle persistableBundle, int i) {
            if (i <= 0) {
                return null;
            }
            PersistableBundle persistableBundle2 = (PersistableBundle) persistableBundle.clone();
            for (String str : persistableBundle.keySet()) {
                Object obj = persistableBundle2.get(str);
                if (obj instanceof PersistableBundle) {
                    persistableBundle2.putPersistableBundle(str, deepCopyBundle((PersistableBundle) obj, i - 1));
                }
            }
            return persistableBundle2;
        }

        private void writeConstraintsToXml(XmlSerializer xmlSerializer, JobStatus jobStatus) throws IOException {
            xmlSerializer.startTag(null, JobStore.XML_TAG_PARAMS_CONSTRAINTS);
            if (jobStatus.hasConnectivityConstraint()) {
                NetworkRequest requiredNetwork = jobStatus.getJob().getRequiredNetwork();
                xmlSerializer.attribute(null, "net-capabilities", Long.toString(BitUtils.packBits(requiredNetwork.networkCapabilities.getCapabilities())));
                xmlSerializer.attribute(null, "net-unwanted-capabilities", Long.toString(BitUtils.packBits(requiredNetwork.networkCapabilities.getUnwantedCapabilities())));
                xmlSerializer.attribute(null, "net-transport-types", Long.toString(BitUtils.packBits(requiredNetwork.networkCapabilities.getTransportTypes())));
            }
            if (jobStatus.hasIdleConstraint()) {
                xmlSerializer.attribute(null, "idle", Boolean.toString(true));
            }
            if (jobStatus.hasChargingConstraint()) {
                xmlSerializer.attribute(null, "charging", Boolean.toString(true));
            }
            if (jobStatus.hasBatteryNotLowConstraint()) {
                xmlSerializer.attribute(null, "battery-not-low", Boolean.toString(true));
            }
            xmlSerializer.endTag(null, JobStore.XML_TAG_PARAMS_CONSTRAINTS);
        }

        private void writeExecutionCriteriaToXml(XmlSerializer xmlSerializer, JobStatus jobStatus) throws IOException {
            long jLongValue;
            long jLongValue2;
            JobInfo job = jobStatus.getJob();
            if (jobStatus.getJob().isPeriodic()) {
                xmlSerializer.startTag(null, JobStore.XML_TAG_PERIODIC);
                xmlSerializer.attribute(null, "period", Long.toString(job.getIntervalMillis()));
                xmlSerializer.attribute(null, "flex", Long.toString(job.getFlexMillis()));
            } else {
                xmlSerializer.startTag(null, JobStore.XML_TAG_ONEOFF);
            }
            Pair<Long, Long> persistedUtcTimes = jobStatus.getPersistedUtcTimes();
            if (JobStore.DEBUG && persistedUtcTimes != null) {
                Slog.i(JobStore.TAG, "storing original UTC timestamps for " + jobStatus);
            }
            long jMillis = JobSchedulerService.sSystemClock.millis();
            long jMillis2 = JobSchedulerService.sElapsedRealtimeClock.millis();
            if (jobStatus.hasDeadlineConstraint()) {
                if (persistedUtcTimes == null) {
                    jLongValue2 = (jobStatus.getLatestRunTimeElapsed() - jMillis2) + jMillis;
                } else {
                    jLongValue2 = ((Long) persistedUtcTimes.second).longValue();
                }
                xmlSerializer.attribute(null, "deadline", Long.toString(jLongValue2));
            }
            if (jobStatus.hasTimingDelayConstraint()) {
                if (persistedUtcTimes == null) {
                    jLongValue = jMillis + (jobStatus.getEarliestRunTime() - jMillis2);
                } else {
                    jLongValue = ((Long) persistedUtcTimes.first).longValue();
                }
                xmlSerializer.attribute(null, "delay", Long.toString(jLongValue));
            }
            if (jobStatus.getJob().getInitialBackoffMillis() != 30000 || jobStatus.getJob().getBackoffPolicy() != 1) {
                xmlSerializer.attribute(null, "backoff-policy", Integer.toString(job.getBackoffPolicy()));
                xmlSerializer.attribute(null, "initial-backoff", Long.toString(job.getInitialBackoffMillis()));
            }
            if (job.isPeriodic()) {
                xmlSerializer.endTag(null, JobStore.XML_TAG_PERIODIC);
            } else {
                xmlSerializer.endTag(null, JobStore.XML_TAG_ONEOFF);
            }
        }
    }

    private static Pair<Long, Long> convertRtcBoundsToElapsed(Pair<Long, Long> pair, long j) {
        long jMax;
        long jMillis = JobSchedulerService.sSystemClock.millis();
        if (((Long) pair.first).longValue() > 0) {
            jMax = Math.max(((Long) pair.first).longValue() - jMillis, 0L) + j;
        } else {
            jMax = 0;
        }
        long jLongValue = ((Long) pair.second).longValue();
        long jMax2 = JobStatus.NO_LATEST_RUNTIME;
        if (jLongValue < JobStatus.NO_LATEST_RUNTIME) {
            jMax2 = j + Math.max(((Long) pair.second).longValue() - jMillis, 0L);
        }
        return Pair.create(Long.valueOf(jMax), Long.valueOf(jMax2));
    }

    private static boolean isSyncJob(JobStatus jobStatus) {
        return SyncJobService.class.getName().equals(jobStatus.getServiceComponent().getClassName());
    }

    private final class ReadJobMapFromDiskRunnable implements Runnable {
        private final JobSet jobSet;
        private final boolean rtcGood;

        ReadJobMapFromDiskRunnable(JobSet jobSet, boolean z) {
            this.jobSet = jobSet;
            this.rtcGood = z;
        }

        @Override
        public void run() throws java.lang.Throwable {
            r0 = 0;
            r1 = com.android.server.job.JobStore.this.mJobsFile.openRead();
            r2 = com.android.server.job.JobStore.this.mLock;
            synchronized (r2) {
                ;
                r3 = readJobMapImpl(r1, r13.rtcGood);
                if (r3 != null) {
                    r4 = com.android.server.job.JobSchedulerService.sElapsedRealtimeClock.millis();
                    r6 = android.app.ActivityManager.getService();
                    r7 = 0;
                    r8 = 0;
                    r9 = 0;
                    while (r0 < r3.size()) {
                        r10 = r3.get(r0);
                        r10.prepareLocked(r6);
                        r10.enqueueTime = r4;
                        r13.jobSet.add(r10);
                        r9 = r9 + 1;
                        if (r10.getUid() == 1000) {
                            r7 = r7 + 1;
                            if (com.android.server.job.JobStore.isSyncJob(r10)) {
                                r8 = r8 + 1;
                            }
                        }
                        r0 = r0 + 1;
                    }
                    r0 = r7;
                } else {
                    r8 = 0;
                    r9 = 0;
                }
                r1.close();
                if (com.android.server.job.JobStore.this.mPersistInfo.countAllJobsLoaded < 0) {
                    com.android.server.job.JobStore.this.mPersistInfo.countAllJobsLoaded = r9;
                    com.android.server.job.JobStore.this.mPersistInfo.countSystemServerJobsLoaded = r0;
                    com.android.server.job.JobStore.this.mPersistInfo.countSystemSyncManagerJobsLoaded = r8;
                }
                r1 = new java.lang.StringBuilder();
                r1.append("Read ");
                r1.append(r9);
                r1.append(" jobs");
                android.util.Slog.i(com.android.server.job.JobStore.TAG, r1.toString());
                return;
            }
        }

        private List<JobStatus> readJobMapImpl(FileInputStream fileInputStream, boolean z) throws XmlPullParserException, IOException {
            XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
            xmlPullParserNewPullParser.setInput(fileInputStream, StandardCharsets.UTF_8.name());
            int eventType = xmlPullParserNewPullParser.getEventType();
            while (eventType != 2 && eventType != 1) {
                eventType = xmlPullParserNewPullParser.next();
                Slog.d(JobStore.TAG, "Start tag: " + xmlPullParserNewPullParser.getName());
            }
            if (eventType == 1) {
                if (JobStore.DEBUG) {
                    Slog.d(JobStore.TAG, "No persisted jobs.");
                }
                return null;
            }
            if (!"job-info".equals(xmlPullParserNewPullParser.getName())) {
                return null;
            }
            ArrayList arrayList = new ArrayList();
            try {
                if (Integer.parseInt(xmlPullParserNewPullParser.getAttributeValue(null, "version")) != 0) {
                    Slog.d(JobStore.TAG, "Invalid version number, aborting jobs file read.");
                    return null;
                }
                int next = xmlPullParserNewPullParser.next();
                do {
                    if (next == 2 && "job".equals(xmlPullParserNewPullParser.getName())) {
                        JobStatus jobStatusRestoreJobFromXml = restoreJobFromXml(z, xmlPullParserNewPullParser);
                        if (jobStatusRestoreJobFromXml != null) {
                            if (JobStore.DEBUG) {
                                Slog.d(JobStore.TAG, "Read out " + jobStatusRestoreJobFromXml);
                            }
                            arrayList.add(jobStatusRestoreJobFromXml);
                        } else {
                            Slog.d(JobStore.TAG, "Error reading job from file.");
                        }
                    }
                    next = xmlPullParserNewPullParser.next();
                } while (next != 1);
                return arrayList;
            } catch (NumberFormatException e) {
                Slog.e(JobStore.TAG, "Invalid version number, aborting jobs file read.");
                return null;
            }
        }

        private JobStatus restoreJobFromXml(boolean z, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            long j;
            long j2;
            int next;
            int i;
            int next2;
            String str;
            Pair<Long, Long> pair;
            long jLongValue;
            int i2;
            long j3;
            String str2;
            long j4;
            int next3;
            String str3;
            ?? r2 = 0;
            try {
                JobInfo.Builder builderBuildBuilderFromXml = buildBuilderFromXml(xmlPullParser);
                builderBuildBuilderFromXml.setPersisted(true);
                int i3 = Integer.parseInt(xmlPullParser.getAttributeValue(null, WatchlistLoggingHandler.WatchlistEventKeys.UID));
                String attributeValue = xmlPullParser.getAttributeValue(null, "priority");
                if (attributeValue != null) {
                    builderBuildBuilderFromXml.setPriority(Integer.parseInt(attributeValue));
                }
                String attributeValue2 = xmlPullParser.getAttributeValue(null, "flags");
                if (attributeValue2 != null) {
                    builderBuildBuilderFromXml.setFlags(Integer.parseInt(attributeValue2));
                }
                String attributeValue3 = xmlPullParser.getAttributeValue(null, "internalFlags");
                int i4 = attributeValue3 != null ? Integer.parseInt(attributeValue3) : 0;
                String attributeValue4 = xmlPullParser.getAttributeValue(null, "sourceUserId");
                int i5 = attributeValue4 == null ? -1 : Integer.parseInt(attributeValue4);
                String attributeValue5 = xmlPullParser.getAttributeValue(null, "lastSuccessfulRunTime");
                if (attributeValue5 != null) {
                    j = Long.parseLong(attributeValue5);
                } else {
                    j = 0;
                }
                String attributeValue6 = xmlPullParser.getAttributeValue(null, "lastFailedRunTime");
                if (attributeValue6 != null) {
                    j2 = Long.parseLong(attributeValue6);
                } else {
                    j2 = 0;
                }
                String attributeValue7 = xmlPullParser.getAttributeValue(null, "sourcePackageName");
                String attributeValue8 = xmlPullParser.getAttributeValue(null, "sourceTag");
                do {
                    next = xmlPullParser.next();
                    i = 4;
                } while (next == 4);
                if (next != 2 || !JobStore.XML_TAG_PARAMS_CONSTRAINTS.equals(xmlPullParser.getName())) {
                    return null;
                }
                try {
                    buildConstraintsFromXml(builderBuildBuilderFromXml, xmlPullParser);
                    xmlPullParser.next();
                    while (true) {
                        next2 = xmlPullParser.next();
                        if (next2 != i) {
                            break;
                        }
                        i = i;
                        r2 = r2;
                    }
                    if (next2 != 2) {
                        return r2;
                    }
                    try {
                        Pair<Long, Long> pairBuildRtcExecutionTimesFromXml = buildRtcExecutionTimesFromXml(xmlPullParser);
                        long jMillis = JobSchedulerService.sElapsedRealtimeClock.millis();
                        Pair pairConvertRtcBoundsToElapsed = JobStore.convertRtcBoundsToElapsed(pairBuildRtcExecutionTimesFromXml, jMillis);
                        if (JobStore.XML_TAG_PERIODIC.equals(xmlPullParser.getName())) {
                            try {
                                str = attributeValue7;
                                long j5 = Long.parseLong(xmlPullParser.getAttributeValue(r2, "period"));
                                String attributeValue9 = xmlPullParser.getAttributeValue(r2, "flex");
                                if (attributeValue9 != null) {
                                    pair = pairBuildRtcExecutionTimesFromXml;
                                    jLongValue = Long.valueOf(attributeValue9).longValue();
                                } else {
                                    pair = pairBuildRtcExecutionTimesFromXml;
                                    jLongValue = j5;
                                }
                                builderBuildBuilderFromXml.setPeriodic(j5, jLongValue);
                                if (((Long) pairConvertRtcBoundsToElapsed.second).longValue() > jMillis + j5 + jLongValue) {
                                    long j6 = jMillis + jLongValue + j5;
                                    long j7 = j6 - jLongValue;
                                    str2 = attributeValue8;
                                    i2 = i5;
                                    j3 = jMillis;
                                    Slog.w(JobStore.TAG, String.format("Periodic job for uid='%d' persisted run-time is too big [%s, %s]. Clamping to [%s,%s]", Integer.valueOf(i3), DateUtils.formatElapsedTime(((Long) pairConvertRtcBoundsToElapsed.first).longValue() / 1000), DateUtils.formatElapsedTime(((Long) pairConvertRtcBoundsToElapsed.second).longValue() / 1000), DateUtils.formatElapsedTime(j7 / 1000), DateUtils.formatElapsedTime(j6 / 1000)));
                                    pairConvertRtcBoundsToElapsed = Pair.create(Long.valueOf(j7), Long.valueOf(j6));
                                } else {
                                    i2 = i5;
                                    j3 = jMillis;
                                    str2 = attributeValue8;
                                }
                                j4 = 0;
                            } catch (NumberFormatException e) {
                                Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                return null;
                            }
                        } else {
                            str = attributeValue7;
                            pair = pairBuildRtcExecutionTimesFromXml;
                            i2 = i5;
                            j3 = jMillis;
                            str2 = attributeValue8;
                            if (!JobStore.XML_TAG_ONEOFF.equals(xmlPullParser.getName())) {
                                if (JobStore.DEBUG) {
                                    Slog.d(JobStore.TAG, "Invalid parameter tag, skipping - " + xmlPullParser.getName());
                                    return null;
                                }
                                return null;
                            }
                            try {
                                j4 = 0;
                                if (((Long) pairConvertRtcBoundsToElapsed.first).longValue() != 0) {
                                    builderBuildBuilderFromXml.setMinimumLatency(((Long) pairConvertRtcBoundsToElapsed.first).longValue() - j3);
                                }
                                if (((Long) pairConvertRtcBoundsToElapsed.second).longValue() != JobStatus.NO_LATEST_RUNTIME) {
                                    builderBuildBuilderFromXml.setOverrideDeadline(((Long) pairConvertRtcBoundsToElapsed.second).longValue() - j3);
                                }
                            } catch (NumberFormatException e2) {
                                Slog.d(JobStore.TAG, "Error reading job execution criteria, skipping.");
                                return null;
                            }
                        }
                        maybeBuildBackoffPolicyFromXml(builderBuildBuilderFromXml, xmlPullParser);
                        xmlPullParser.nextTag();
                        do {
                            next3 = xmlPullParser.next();
                        } while (next3 == 4);
                        if (next3 != 2 || !JobStore.XML_TAG_EXTRAS.equals(xmlPullParser.getName())) {
                            if (JobStore.DEBUG) {
                                Slog.d(JobStore.TAG, "Error reading extras, skipping.");
                                return null;
                            }
                            return null;
                        }
                        PersistableBundle persistableBundleRestoreFromXml = PersistableBundle.restoreFromXml(xmlPullParser);
                        builderBuildBuilderFromXml.setExtras(persistableBundleRestoreFromXml);
                        xmlPullParser.nextTag();
                        String str4 = str;
                        if (PackageManagerService.PLATFORM_PACKAGE_NAME.equals(str4) && persistableBundleRestoreFromXml != null && persistableBundleRestoreFromXml.getBoolean("SyncManagerJob", false)) {
                            String string = persistableBundleRestoreFromXml.getString("owningPackage", str4);
                            if (JobStore.DEBUG) {
                                Slog.i(JobStore.TAG, "Fixing up sync job source package name from 'android' to '" + string + "'");
                            }
                            str3 = string;
                        } else {
                            str3 = str4;
                        }
                        JobSchedulerInternal jobSchedulerInternal = (JobSchedulerInternal) LocalServices.getService(JobSchedulerInternal.class);
                        int i6 = i2;
                        return new JobStatus(builderBuildBuilderFromXml.build(), i3, str3, i6, JobSchedulerService.standbyBucketForPackage(str3, i6, j3), jobSchedulerInternal != null ? jobSchedulerInternal.currentHeartbeat() : j4, str2, ((Long) pairConvertRtcBoundsToElapsed.first).longValue(), ((Long) pairConvertRtcBoundsToElapsed.second).longValue(), j, j2, z ? null : pair, i4);
                    } catch (NumberFormatException e3) {
                        if (JobStore.DEBUG) {
                            Slog.d(JobStore.TAG, "Error parsing execution time parameters, skipping.");
                            return null;
                        }
                        return null;
                    }
                } catch (NumberFormatException e4) {
                    Slog.d(JobStore.TAG, "Error reading constraints, skipping.");
                    return null;
                }
            } catch (NumberFormatException e5) {
                Slog.e(JobStore.TAG, "Error parsing job's required fields, skipping");
                return null;
            }
        }

        private JobInfo.Builder buildBuilderFromXml(XmlPullParser xmlPullParser) throws NumberFormatException {
            return new JobInfo.Builder(Integer.parseInt(xmlPullParser.getAttributeValue(null, "jobid")), new ComponentName(xmlPullParser.getAttributeValue(null, Settings.ATTR_PACKAGE), xmlPullParser.getAttributeValue(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS)));
        }

        private void buildConstraintsFromXml(JobInfo.Builder builder, XmlPullParser xmlPullParser) {
            long jPackBits;
            String attributeValue = xmlPullParser.getAttributeValue(null, "net-capabilities");
            String attributeValue2 = xmlPullParser.getAttributeValue(null, "net-unwanted-capabilities");
            String attributeValue3 = xmlPullParser.getAttributeValue(null, "net-transport-types");
            if (attributeValue != null && attributeValue3 != null) {
                NetworkRequest networkRequestBuild = new NetworkRequest.Builder().build();
                if (attributeValue2 != null) {
                    jPackBits = Long.parseLong(attributeValue2);
                } else {
                    jPackBits = BitUtils.packBits(networkRequestBuild.networkCapabilities.getUnwantedCapabilities());
                }
                networkRequestBuild.networkCapabilities.setCapabilities(BitUtils.unpackBits(Long.parseLong(attributeValue)), BitUtils.unpackBits(jPackBits));
                networkRequestBuild.networkCapabilities.setTransportTypes(BitUtils.unpackBits(Long.parseLong(attributeValue3)));
                builder.setRequiredNetwork(networkRequestBuild);
            } else {
                if (xmlPullParser.getAttributeValue(null, "connectivity") != null) {
                    builder.setRequiredNetworkType(1);
                }
                if (xmlPullParser.getAttributeValue(null, "metered") != null) {
                    builder.setRequiredNetworkType(4);
                }
                if (xmlPullParser.getAttributeValue(null, "unmetered") != null) {
                    builder.setRequiredNetworkType(2);
                }
                if (xmlPullParser.getAttributeValue(null, "not-roaming") != null) {
                    builder.setRequiredNetworkType(3);
                }
            }
            if (xmlPullParser.getAttributeValue(null, "idle") != null) {
                builder.setRequiresDeviceIdle(true);
            }
            if (xmlPullParser.getAttributeValue(null, "charging") != null) {
                builder.setRequiresCharging(true);
            }
        }

        private void maybeBuildBackoffPolicyFromXml(JobInfo.Builder builder, XmlPullParser xmlPullParser) {
            String attributeValue = xmlPullParser.getAttributeValue(null, "initial-backoff");
            if (attributeValue != null) {
                builder.setBackoffCriteria(Long.parseLong(attributeValue), Integer.parseInt(xmlPullParser.getAttributeValue(null, "backoff-policy")));
            }
        }

        private Pair<Long, Long> buildRtcExecutionTimesFromXml(XmlPullParser xmlPullParser) throws NumberFormatException {
            long j;
            long j2;
            String attributeValue = xmlPullParser.getAttributeValue(null, "delay");
            if (attributeValue != null) {
                j = Long.parseLong(attributeValue);
            } else {
                j = 0;
            }
            String attributeValue2 = xmlPullParser.getAttributeValue(null, "deadline");
            if (attributeValue2 != null) {
                j2 = Long.parseLong(attributeValue2);
            } else {
                j2 = JobStatus.NO_LATEST_RUNTIME;
            }
            return Pair.create(Long.valueOf(j), Long.valueOf(j2));
        }

        private Pair<Long, Long> buildExecutionTimesFromXml(XmlPullParser xmlPullParser) throws NumberFormatException {
            long jMax;
            long jMillis = JobSchedulerService.sSystemClock.millis();
            long jMillis2 = JobSchedulerService.sElapsedRealtimeClock.millis();
            String attributeValue = xmlPullParser.getAttributeValue(null, "deadline");
            long jMax2 = 0;
            if (attributeValue != null) {
                jMax = Math.max(Long.parseLong(attributeValue) - jMillis, 0L) + jMillis2;
            } else {
                jMax = JobStatus.NO_LATEST_RUNTIME;
            }
            String attributeValue2 = xmlPullParser.getAttributeValue(null, "delay");
            if (attributeValue2 != null) {
                jMax2 = jMillis2 + Math.max(Long.parseLong(attributeValue2) - jMillis, 0L);
            }
            return Pair.create(Long.valueOf(jMax2), Long.valueOf(jMax));
        }
    }

    static final class JobSet {

        @VisibleForTesting
        final SparseArray<ArraySet<JobStatus>> mJobs = new SparseArray<>();

        @VisibleForTesting
        final SparseArray<ArraySet<JobStatus>> mJobsPerSourceUid = new SparseArray<>();

        public List<JobStatus> getJobsByUid(int i) {
            ArrayList arrayList = new ArrayList();
            ArraySet<JobStatus> arraySet = this.mJobs.get(i);
            if (arraySet != null) {
                arrayList.addAll(arraySet);
            }
            return arrayList;
        }

        public List<JobStatus> getJobsByUser(int i) {
            ArraySet<JobStatus> arraySetValueAt;
            ArrayList arrayList = new ArrayList();
            for (int size = this.mJobsPerSourceUid.size() - 1; size >= 0; size--) {
                if (UserHandle.getUserId(this.mJobsPerSourceUid.keyAt(size)) == i && (arraySetValueAt = this.mJobsPerSourceUid.valueAt(size)) != null) {
                    arrayList.addAll(arraySetValueAt);
                }
            }
            return arrayList;
        }

        public boolean add(JobStatus jobStatus) {
            int uid = jobStatus.getUid();
            int sourceUid = jobStatus.getSourceUid();
            ArraySet<JobStatus> arraySet = this.mJobs.get(uid);
            if (arraySet == null) {
                arraySet = new ArraySet<>();
                this.mJobs.put(uid, arraySet);
            }
            ArraySet<JobStatus> arraySet2 = this.mJobsPerSourceUid.get(sourceUid);
            if (arraySet2 == null) {
                arraySet2 = new ArraySet<>();
                this.mJobsPerSourceUid.put(sourceUid, arraySet2);
            }
            boolean zAdd = arraySet.add(jobStatus);
            boolean zAdd2 = arraySet2.add(jobStatus);
            if (zAdd != zAdd2) {
                Slog.wtf(JobStore.TAG, "mJobs and mJobsPerSourceUid mismatch; caller= " + zAdd + " source= " + zAdd2);
            }
            return zAdd || zAdd2;
        }

        public boolean remove(JobStatus jobStatus) {
            int uid = jobStatus.getUid();
            ArraySet<JobStatus> arraySet = this.mJobs.get(uid);
            int sourceUid = jobStatus.getSourceUid();
            ArraySet<JobStatus> arraySet2 = this.mJobsPerSourceUid.get(sourceUid);
            boolean z = arraySet != null && arraySet.remove(jobStatus);
            boolean z2 = arraySet2 != null && arraySet2.remove(jobStatus);
            if (z != z2) {
                Slog.wtf(JobStore.TAG, "Job presence mismatch; caller=" + z + " source=" + z2);
            }
            if (!z && !z2) {
                return false;
            }
            if (arraySet != null && arraySet.size() == 0) {
                this.mJobs.remove(uid);
            }
            if (arraySet2 != null && arraySet2.size() == 0) {
                this.mJobsPerSourceUid.remove(sourceUid);
            }
            return true;
        }

        public void removeJobsOfNonUsers(final int[] iArr) {
            removeAll(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return JobStore.JobSet.lambda$removeJobsOfNonUsers$0(iArr, (JobStatus) obj);
                }
            }.or(new Predicate() {
                @Override
                public final boolean test(Object obj) {
                    return JobStore.JobSet.lambda$removeJobsOfNonUsers$1(iArr, (JobStatus) obj);
                }
            }));
        }

        static boolean lambda$removeJobsOfNonUsers$0(int[] iArr, JobStatus jobStatus) {
            return !ArrayUtils.contains(iArr, jobStatus.getSourceUserId());
        }

        static boolean lambda$removeJobsOfNonUsers$1(int[] iArr, JobStatus jobStatus) {
            return !ArrayUtils.contains(iArr, jobStatus.getUserId());
        }

        private void removeAll(Predicate<JobStatus> predicate) {
            for (int size = this.mJobs.size() - 1; size >= 0; size--) {
                ArraySet<JobStatus> arraySetValueAt = this.mJobs.valueAt(size);
                for (int size2 = arraySetValueAt.size() - 1; size2 >= 0; size2--) {
                    if (predicate.test(arraySetValueAt.valueAt(size2))) {
                        arraySetValueAt.removeAt(size2);
                    }
                }
                if (arraySetValueAt.size() == 0) {
                    this.mJobs.removeAt(size);
                }
            }
            for (int size3 = this.mJobsPerSourceUid.size() - 1; size3 >= 0; size3--) {
                ArraySet<JobStatus> arraySetValueAt2 = this.mJobsPerSourceUid.valueAt(size3);
                for (int size4 = arraySetValueAt2.size() - 1; size4 >= 0; size4--) {
                    if (predicate.test(arraySetValueAt2.valueAt(size4))) {
                        arraySetValueAt2.removeAt(size4);
                    }
                }
                if (arraySetValueAt2.size() == 0) {
                    this.mJobsPerSourceUid.removeAt(size3);
                }
            }
        }

        public boolean contains(JobStatus jobStatus) {
            ArraySet<JobStatus> arraySet = this.mJobs.get(jobStatus.getUid());
            return arraySet != null && arraySet.contains(jobStatus);
        }

        public JobStatus get(int i, int i2) {
            ArraySet<JobStatus> arraySet = this.mJobs.get(i);
            if (arraySet != null) {
                for (int size = arraySet.size() - 1; size >= 0; size--) {
                    JobStatus jobStatusValueAt = arraySet.valueAt(size);
                    if (jobStatusValueAt.getJobId() == i2) {
                        return jobStatusValueAt;
                    }
                }
                return null;
            }
            return null;
        }

        public List<JobStatus> getAllJobs() {
            ArrayList arrayList = new ArrayList(size());
            for (int size = this.mJobs.size() - 1; size >= 0; size--) {
                ArraySet<JobStatus> arraySetValueAt = this.mJobs.valueAt(size);
                if (arraySetValueAt != null) {
                    for (int size2 = arraySetValueAt.size() - 1; size2 >= 0; size2--) {
                        arrayList.add(arraySetValueAt.valueAt(size2));
                    }
                }
            }
            return arrayList;
        }

        public void clear() {
            this.mJobs.clear();
            this.mJobsPerSourceUid.clear();
        }

        public int size() {
            int size = 0;
            for (int size2 = this.mJobs.size() - 1; size2 >= 0; size2--) {
                size += this.mJobs.valueAt(size2).size();
            }
            return size;
        }

        public int countJobsForUid(int i) {
            ArraySet<JobStatus> arraySet = this.mJobs.get(i);
            int i2 = 0;
            if (arraySet != null) {
                for (int size = arraySet.size() - 1; size >= 0; size--) {
                    JobStatus jobStatusValueAt = arraySet.valueAt(size);
                    if (jobStatusValueAt.getUid() == jobStatusValueAt.getSourceUid()) {
                        i2++;
                    }
                }
            }
            return i2;
        }

        public void forEachJob(Predicate<JobStatus> predicate, Consumer<JobStatus> consumer) {
            for (int size = this.mJobs.size() - 1; size >= 0; size--) {
                ArraySet<JobStatus> arraySetValueAt = this.mJobs.valueAt(size);
                if (arraySetValueAt != null) {
                    for (int size2 = arraySetValueAt.size() - 1; size2 >= 0; size2--) {
                        JobStatus jobStatusValueAt = arraySetValueAt.valueAt(size2);
                        if (predicate == null || predicate.test(jobStatusValueAt)) {
                            consumer.accept(jobStatusValueAt);
                        }
                    }
                }
            }
        }

        public void forEachJob(int i, Consumer<JobStatus> consumer) {
            ArraySet<JobStatus> arraySet = this.mJobs.get(i);
            if (arraySet != null) {
                for (int size = arraySet.size() - 1; size >= 0; size--) {
                    consumer.accept(arraySet.valueAt(size));
                }
            }
        }

        public void forEachJobForSourceUid(int i, Consumer<JobStatus> consumer) {
            ArraySet<JobStatus> arraySet = this.mJobsPerSourceUid.get(i);
            if (arraySet != null) {
                for (int size = arraySet.size() - 1; size >= 0; size--) {
                    consumer.accept(arraySet.valueAt(size));
                }
            }
        }
    }
}
