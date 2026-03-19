package com.android.server.job;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
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
import com.android.internal.util.FastXmlSerializer;
import com.android.server.IoThread;
import com.android.server.audio.AudioService;
import com.android.server.job.controllers.JobStatus;
import com.mediatek.appworkingset.AWSDBHelper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class JobStore {
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
    private static final boolean DEBUG = JobSchedulerService.DEBUG;
    private static final Object sSingletonLock = new Object();
    private final Handler mIoHandler = IoThread.getHandler();
    private int mDirtyOperations = 0;

    public interface JobStatusFunctor {
        void process(JobStatus jobStatus);
    }

    static JobStore initAndGet(JobSchedulerService jobManagerService) {
        JobStore jobStore;
        synchronized (sSingletonLock) {
            if (sSingleton == null) {
                sSingleton = new JobStore(jobManagerService.getContext(), jobManagerService.getLock(), Environment.getDataDirectory());
            }
            jobStore = sSingleton;
        }
        return jobStore;
    }

    public static JobStore initAndGetForTesting(Context context, File dataDir) {
        JobStore jobStoreUnderTest = new JobStore(context, new Object(), dataDir);
        jobStoreUnderTest.clear();
        return jobStoreUnderTest;
    }

    private JobStore(Context context, Object lock, File dataDir) {
        this.mLock = lock;
        this.mContext = context;
        File systemDir = new File(dataDir, "system");
        File jobDir = new File(systemDir, "job");
        jobDir.mkdirs();
        this.mJobsFile = new AtomicFile(new File(jobDir, "jobs.xml"));
        this.mJobSet = new JobSet();
        readJobMapFromDisk(this.mJobSet);
    }

    public boolean add(JobStatus jobStatus) {
        boolean replaced = this.mJobSet.remove(jobStatus);
        this.mJobSet.add(jobStatus);
        if (jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
        }
        if (DEBUG) {
            Slog.d(TAG, "Added job status to store: " + jobStatus);
        }
        return replaced;
    }

    boolean containsJob(JobStatus jobStatus) {
        return this.mJobSet.contains(jobStatus);
    }

    public int size() {
        return this.mJobSet.size();
    }

    public int countJobsForUid(int uid) {
        return this.mJobSet.countJobsForUid(uid);
    }

    public boolean remove(JobStatus jobStatus, boolean writeBack) {
        boolean removed = this.mJobSet.remove(jobStatus);
        if (!removed) {
            if (DEBUG) {
                Slog.d(TAG, "Couldn't remove job: didn't exist: " + jobStatus);
                return false;
            }
            return false;
        }
        if (writeBack && jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
        }
        return removed;
    }

    public void clear() {
        this.mJobSet.clear();
        maybeWriteStatusToDiskAsync();
    }

    public List<JobStatus> getJobsByUser(int userHandle) {
        return this.mJobSet.getJobsByUser(userHandle);
    }

    public List<JobStatus> getJobsByUid(int uid) {
        return this.mJobSet.getJobsByUid(uid);
    }

    public JobStatus getJobByUidAndJobId(int uid, int jobId) {
        return this.mJobSet.get(uid, jobId);
    }

    public void forEachJob(JobStatusFunctor functor) {
        this.mJobSet.forEachJob(functor);
    }

    public void forEachJob(int uid, JobStatusFunctor functor) {
        this.mJobSet.forEachJob(uid, functor);
    }

    private void maybeWriteStatusToDiskAsync() {
        this.mDirtyOperations++;
        if (this.mDirtyOperations < 1) {
            return;
        }
        if (DEBUG) {
            Slog.v(TAG, "Writing jobs to disk.");
        }
        this.mIoHandler.post(new WriteJobsMapToDiskRunnable(this, null));
    }

    public void readJobMapFromDisk(JobSet jobSet) {
        new ReadJobMapFromDiskRunnable(jobSet).run();
    }

    private class WriteJobsMapToDiskRunnable implements Runnable {
        WriteJobsMapToDiskRunnable(JobStore this$0, WriteJobsMapToDiskRunnable writeJobsMapToDiskRunnable) {
            this();
        }

        private WriteJobsMapToDiskRunnable() {
        }

        @Override
        public void run() {
            long startElapsed = SystemClock.elapsedRealtime();
            final List<JobStatus> storeCopy = new ArrayList<>();
            synchronized (JobStore.this.mLock) {
                JobStore.this.mJobSet.forEachJob(new JobStatusFunctor() {
                    @Override
                    public void process(JobStatus job) {
                        if (!job.isPersisted()) {
                            return;
                        }
                        storeCopy.add(new JobStatus(job));
                    }
                });
            }
            writeJobsMapImpl(storeCopy);
            if (!JobSchedulerService.DEBUG) {
                return;
            }
            Slog.v(JobStore.TAG, "Finished writing, took " + (SystemClock.elapsedRealtime() - startElapsed) + "ms");
        }

        private void writeJobsMapImpl(List<JobStatus> jobList) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(baos, StandardCharsets.UTF_8.name());
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                fastXmlSerializer.startTag(null, "job-info");
                fastXmlSerializer.attribute(null, "version", Integer.toString(0));
                for (int i = 0; i < jobList.size(); i++) {
                    JobStatus jobStatus = jobList.get(i);
                    if (JobStore.DEBUG) {
                        Slog.d(JobStore.TAG, "Saving job " + jobStatus.getJobId());
                    }
                    fastXmlSerializer.startTag(null, "job");
                    addAttributesToJobTag(fastXmlSerializer, jobStatus);
                    writeConstraintsToXml(fastXmlSerializer, jobStatus);
                    writeExecutionCriteriaToXml(fastXmlSerializer, jobStatus);
                    writeBundleToXml(jobStatus.getExtras(), fastXmlSerializer);
                    fastXmlSerializer.endTag(null, "job");
                }
                fastXmlSerializer.endTag(null, "job-info");
                fastXmlSerializer.endDocument();
                FileOutputStream fos = JobStore.this.mJobsFile.startWrite();
                fos.write(baos.toByteArray());
                JobStore.this.mJobsFile.finishWrite(fos);
                JobStore.this.mDirtyOperations = 0;
            } catch (IOException e) {
                if (!JobStore.DEBUG) {
                    return;
                }
                Slog.v(JobStore.TAG, "Error writing out job data.", e);
            } catch (XmlPullParserException e2) {
                if (!JobStore.DEBUG) {
                    return;
                }
                Slog.d(JobStore.TAG, "Error persisting bundle.", e2);
            }
        }

        private void addAttributesToJobTag(XmlSerializer out, JobStatus jobStatus) throws IOException {
            out.attribute(null, "jobid", Integer.toString(jobStatus.getJobId()));
            out.attribute(null, "package", jobStatus.getServiceComponent().getPackageName());
            out.attribute(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS, jobStatus.getServiceComponent().getClassName());
            if (jobStatus.getSourcePackageName() != null) {
                out.attribute(null, "sourcePackageName", jobStatus.getSourcePackageName());
            }
            if (jobStatus.getSourceTag() != null) {
                out.attribute(null, "sourceTag", jobStatus.getSourceTag());
            }
            out.attribute(null, "sourceUserId", String.valueOf(jobStatus.getSourceUserId()));
            out.attribute(null, AWSDBHelper.PackageProcessList.KEY_UID, Integer.toString(jobStatus.getUid()));
            out.attribute(null, AWSDBHelper.PackagePriorityList.KEY_PRIORITY, String.valueOf(jobStatus.getPriority()));
            out.attribute(null, "flags", String.valueOf(jobStatus.getFlags()));
        }

        private void writeBundleToXml(PersistableBundle extras, XmlSerializer out) throws XmlPullParserException, IOException {
            out.startTag(null, JobStore.XML_TAG_EXTRAS);
            PersistableBundle extrasCopy = deepCopyBundle(extras, 10);
            extrasCopy.saveToXml(out);
            out.endTag(null, JobStore.XML_TAG_EXTRAS);
        }

        private PersistableBundle deepCopyBundle(PersistableBundle bundle, int maxDepth) {
            if (maxDepth <= 0) {
                return null;
            }
            PersistableBundle copy = (PersistableBundle) bundle.clone();
            Set<String> keySet = bundle.keySet();
            for (String key : keySet) {
                Object o = copy.get(key);
                if (o instanceof PersistableBundle) {
                    PersistableBundle bCopy = deepCopyBundle((PersistableBundle) o, maxDepth - 1);
                    copy.putPersistableBundle(key, bCopy);
                }
            }
            return copy;
        }

        private void writeConstraintsToXml(XmlSerializer out, JobStatus jobStatus) throws IOException {
            out.startTag(null, JobStore.XML_TAG_PARAMS_CONSTRAINTS);
            if (jobStatus.hasConnectivityConstraint()) {
                out.attribute(null, "connectivity", Boolean.toString(true));
            }
            if (jobStatus.hasUnmeteredConstraint()) {
                out.attribute(null, "unmetered", Boolean.toString(true));
            }
            if (jobStatus.hasNotRoamingConstraint()) {
                out.attribute(null, "not-roaming", Boolean.toString(true));
            }
            if (jobStatus.hasIdleConstraint()) {
                out.attribute(null, "idle", Boolean.toString(true));
            }
            if (jobStatus.hasChargingConstraint()) {
                out.attribute(null, "charging", Boolean.toString(true));
            }
            out.endTag(null, JobStore.XML_TAG_PARAMS_CONSTRAINTS);
        }

        private void writeExecutionCriteriaToXml(XmlSerializer out, JobStatus jobStatus) throws IOException {
            JobInfo job = jobStatus.getJob();
            if (jobStatus.getJob().isPeriodic()) {
                out.startTag(null, JobStore.XML_TAG_PERIODIC);
                out.attribute(null, "period", Long.toString(job.getIntervalMillis()));
                out.attribute(null, "flex", Long.toString(job.getFlexMillis()));
            } else {
                out.startTag(null, JobStore.XML_TAG_ONEOFF);
            }
            if (jobStatus.hasDeadlineConstraint()) {
                long deadlineWallclock = System.currentTimeMillis() + (jobStatus.getLatestRunTimeElapsed() - SystemClock.elapsedRealtime());
                out.attribute(null, "deadline", Long.toString(deadlineWallclock));
            }
            if (jobStatus.hasTimingDelayConstraint()) {
                long delayWallclock = System.currentTimeMillis() + (jobStatus.getEarliestRunTime() - SystemClock.elapsedRealtime());
                out.attribute(null, "delay", Long.toString(delayWallclock));
            }
            if (jobStatus.getJob().getInitialBackoffMillis() != 30000 || jobStatus.getJob().getBackoffPolicy() != 1) {
                out.attribute(null, "backoff-policy", Integer.toString(job.getBackoffPolicy()));
                out.attribute(null, "initial-backoff", Long.toString(job.getInitialBackoffMillis()));
            }
            if (job.isPeriodic()) {
                out.endTag(null, JobStore.XML_TAG_PERIODIC);
            } else {
                out.endTag(null, JobStore.XML_TAG_ONEOFF);
            }
        }
    }

    private class ReadJobMapFromDiskRunnable implements Runnable {
        private final JobSet jobSet;

        ReadJobMapFromDiskRunnable(JobSet jobSet) {
            this.jobSet = jobSet;
        }

        @Override
        public void run() {
            try {
                FileInputStream fis = JobStore.this.mJobsFile.openRead();
                synchronized (JobStore.this.mLock) {
                    List<JobStatus> jobs = readJobMapImpl(fis);
                    if (jobs != null) {
                        for (int i = 0; i < jobs.size(); i++) {
                            this.jobSet.add(jobs.get(i));
                        }
                    }
                }
                fis.close();
            } catch (FileNotFoundException e) {
                if (!JobSchedulerService.DEBUG) {
                    return;
                }
                Slog.d(JobStore.TAG, "Could not find jobs file, probably there was nothing to load.");
            } catch (IOException e2) {
                if (!JobSchedulerService.DEBUG) {
                    return;
                }
                Slog.d(JobStore.TAG, "Error parsing xml.", e2);
            } catch (XmlPullParserException e3) {
                if (!JobSchedulerService.DEBUG) {
                    return;
                }
                Slog.d(JobStore.TAG, "Error parsing xml.", e3);
            }
        }

        private List<JobStatus> readJobMapImpl(FileInputStream fis) throws XmlPullParserException, IOException {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, StandardCharsets.UTF_8.name());
            int eventType = parser.getEventType();
            while (eventType != 2 && eventType != 1) {
                eventType = parser.next();
                Slog.d(JobStore.TAG, "Start tag: " + parser.getName());
            }
            if (eventType == 1) {
                if (JobStore.DEBUG) {
                    Slog.d(JobStore.TAG, "No persisted jobs.");
                }
                return null;
            }
            String tagName = parser.getName();
            if (!"job-info".equals(tagName)) {
                return null;
            }
            List<JobStatus> jobs = new ArrayList<>();
            try {
                int version = Integer.parseInt(parser.getAttributeValue(null, "version"));
                if (version != 0) {
                    Slog.d(JobStore.TAG, "Invalid version number, aborting jobs file read.");
                    return null;
                }
                int eventType2 = parser.next();
                do {
                    if (eventType2 == 2) {
                        String tagName2 = parser.getName();
                        if ("job".equals(tagName2)) {
                            JobStatus persistedJob = restoreJobFromXml(parser);
                            if (persistedJob != null) {
                                if (JobStore.DEBUG) {
                                    Slog.d(JobStore.TAG, "Read out " + persistedJob);
                                }
                                jobs.add(persistedJob);
                            } else {
                                Slog.d(JobStore.TAG, "Error reading job from file.");
                            }
                        }
                    }
                    eventType2 = parser.next();
                } while (eventType2 != 1);
                return jobs;
            } catch (NumberFormatException e) {
                Slog.e(JobStore.TAG, "Invalid version number, aborting jobs file read.");
                return null;
            }
        }

        private JobStatus restoreJobFromXml(XmlPullParser parser) throws XmlPullParserException, IOException {
            int eventType;
            boolean zEquals;
            int eventType2;
            int eventType3;
            boolean zEquals2;
            try {
                JobInfo.Builder jobBuilder = buildBuilderFromXml(parser);
                jobBuilder.setPersisted(true);
                int uid = Integer.parseInt(parser.getAttributeValue(null, AWSDBHelper.PackageProcessList.KEY_UID));
                String val = parser.getAttributeValue(null, AWSDBHelper.PackagePriorityList.KEY_PRIORITY);
                if (val != null) {
                    jobBuilder.setPriority(Integer.parseInt(val));
                }
                String val2 = parser.getAttributeValue(null, "flags");
                if (val2 != null) {
                    jobBuilder.setFlags(Integer.parseInt(val2));
                }
                String val3 = parser.getAttributeValue(null, "sourceUserId");
                int sourceUserId = val3 == null ? -1 : Integer.parseInt(val3);
                String sourcePackageName = parser.getAttributeValue(null, "sourcePackageName");
                String sourceTag = parser.getAttributeValue(null, "sourceTag");
                do {
                    eventType = parser.next();
                } while (eventType == 4);
                if (eventType != 2) {
                    zEquals = false;
                } else {
                    zEquals = JobStore.XML_TAG_PARAMS_CONSTRAINTS.equals(parser.getName());
                }
                if (!zEquals) {
                    return null;
                }
                try {
                    buildConstraintsFromXml(jobBuilder, parser);
                    parser.next();
                    do {
                        eventType2 = parser.next();
                    } while (eventType2 == 4);
                    if (eventType2 != 2) {
                        return null;
                    }
                    try {
                        Pair<Long, Long> elapsedRuntimes = buildExecutionTimesFromXml(parser);
                        long elapsedNow = SystemClock.elapsedRealtime();
                        if (JobStore.XML_TAG_PERIODIC.equals(parser.getName())) {
                            try {
                                long periodMillis = Long.valueOf(parser.getAttributeValue(null, "period")).longValue();
                                String val4 = parser.getAttributeValue(null, "flex");
                                long flexMillis = val4 != null ? Long.valueOf(val4).longValue() : periodMillis;
                                jobBuilder.setPeriodic(periodMillis, flexMillis);
                                if (((Long) elapsedRuntimes.second).longValue() > elapsedNow + periodMillis + flexMillis) {
                                    long clampedLateRuntimeElapsed = elapsedNow + flexMillis + periodMillis;
                                    long clampedEarlyRuntimeElapsed = clampedLateRuntimeElapsed - flexMillis;
                                    Slog.w(JobStore.TAG, String.format("Periodic job for uid='%d' persisted run-time is too big [%s, %s]. Clamping to [%s,%s]", Integer.valueOf(uid), DateUtils.formatElapsedTime(((Long) elapsedRuntimes.first).longValue() / 1000), DateUtils.formatElapsedTime(((Long) elapsedRuntimes.second).longValue() / 1000), DateUtils.formatElapsedTime(clampedEarlyRuntimeElapsed / 1000), DateUtils.formatElapsedTime(clampedLateRuntimeElapsed / 1000)));
                                    elapsedRuntimes = Pair.create(Long.valueOf(clampedEarlyRuntimeElapsed), Long.valueOf(clampedLateRuntimeElapsed));
                                }
                            } catch (NumberFormatException e) {
                                Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                return null;
                            }
                        } else if (JobStore.XML_TAG_ONEOFF.equals(parser.getName())) {
                            try {
                                if (((Long) elapsedRuntimes.first).longValue() != 0) {
                                    jobBuilder.setMinimumLatency(((Long) elapsedRuntimes.first).longValue() - elapsedNow);
                                }
                                if (((Long) elapsedRuntimes.second).longValue() != JobStatus.NO_LATEST_RUNTIME) {
                                    jobBuilder.setOverrideDeadline(((Long) elapsedRuntimes.second).longValue() - elapsedNow);
                                }
                            } catch (NumberFormatException e2) {
                                Slog.d(JobStore.TAG, "Error reading job execution criteria, skipping.");
                                return null;
                            }
                        } else {
                            if (JobStore.DEBUG) {
                                Slog.d(JobStore.TAG, "Invalid parameter tag, skipping - " + parser.getName());
                                return null;
                            }
                            return null;
                        }
                        maybeBuildBackoffPolicyFromXml(jobBuilder, parser);
                        parser.nextTag();
                        do {
                            eventType3 = parser.next();
                        } while (eventType3 == 4);
                        if (eventType3 != 2) {
                            zEquals2 = false;
                        } else {
                            zEquals2 = JobStore.XML_TAG_EXTRAS.equals(parser.getName());
                        }
                        if (!zEquals2) {
                            if (JobStore.DEBUG) {
                                Slog.d(JobStore.TAG, "Error reading extras, skipping.");
                                return null;
                            }
                            return null;
                        }
                        PersistableBundle extras = PersistableBundle.restoreFromXml(parser);
                        jobBuilder.setExtras(extras);
                        parser.nextTag();
                        if ("android".equals(sourcePackageName) && extras != null && extras.getBoolean("SyncManagerJob", false)) {
                            sourcePackageName = extras.getString("owningPackage", sourcePackageName);
                            if (JobStore.DEBUG) {
                                Slog.i(JobStore.TAG, "Fixing up sync job source package name from 'android' to '" + sourcePackageName + "'");
                            }
                        }
                        JobStatus js = new JobStatus(jobBuilder.build(), uid, sourcePackageName, sourceUserId, sourceTag, ((Long) elapsedRuntimes.first).longValue(), ((Long) elapsedRuntimes.second).longValue());
                        return js;
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

        private JobInfo.Builder buildBuilderFromXml(XmlPullParser parser) throws NumberFormatException {
            int jobId = Integer.parseInt(parser.getAttributeValue(null, "jobid"));
            String packageName = parser.getAttributeValue(null, "package");
            String className = parser.getAttributeValue(null, AudioService.CONNECT_INTENT_KEY_DEVICE_CLASS);
            ComponentName cname = new ComponentName(packageName, className);
            return new JobInfo.Builder(jobId, cname);
        }

        private void buildConstraintsFromXml(JobInfo.Builder jobBuilder, XmlPullParser parser) {
            String val = parser.getAttributeValue(null, "connectivity");
            if (val != null) {
                jobBuilder.setRequiredNetworkType(1);
            }
            String val2 = parser.getAttributeValue(null, "unmetered");
            if (val2 != null) {
                jobBuilder.setRequiredNetworkType(2);
            }
            String val3 = parser.getAttributeValue(null, "not-roaming");
            if (val3 != null) {
                jobBuilder.setRequiredNetworkType(3);
            }
            String val4 = parser.getAttributeValue(null, "idle");
            if (val4 != null) {
                jobBuilder.setRequiresDeviceIdle(true);
            }
            String val5 = parser.getAttributeValue(null, "charging");
            if (val5 == null) {
                return;
            }
            jobBuilder.setRequiresCharging(true);
        }

        private void maybeBuildBackoffPolicyFromXml(JobInfo.Builder jobBuilder, XmlPullParser parser) {
            String val = parser.getAttributeValue(null, "initial-backoff");
            if (val == null) {
                return;
            }
            long initialBackoff = Long.valueOf(val).longValue();
            int backoffPolicy = Integer.parseInt(parser.getAttributeValue(null, "backoff-policy"));
            jobBuilder.setBackoffCriteria(initialBackoff, backoffPolicy);
        }

        private Pair<Long, Long> buildExecutionTimesFromXml(XmlPullParser parser) throws NumberFormatException {
            long nowWallclock = System.currentTimeMillis();
            long nowElapsed = SystemClock.elapsedRealtime();
            long earliestRunTimeElapsed = 0;
            long latestRunTimeElapsed = JobStatus.NO_LATEST_RUNTIME;
            String val = parser.getAttributeValue(null, "deadline");
            if (val != null) {
                long latestRuntimeWallclock = Long.valueOf(val).longValue();
                long maxDelayElapsed = Math.max(latestRuntimeWallclock - nowWallclock, 0L);
                latestRunTimeElapsed = nowElapsed + maxDelayElapsed;
            }
            String val2 = parser.getAttributeValue(null, "delay");
            if (val2 != null) {
                long earliestRuntimeWallclock = Long.valueOf(val2).longValue();
                long minDelayElapsed = Math.max(earliestRuntimeWallclock - nowWallclock, 0L);
                earliestRunTimeElapsed = nowElapsed + minDelayElapsed;
            }
            return Pair.create(Long.valueOf(earliestRunTimeElapsed), Long.valueOf(latestRunTimeElapsed));
        }
    }

    static class JobSet {
        private SparseArray<ArraySet<JobStatus>> mJobs = new SparseArray<>();

        public List<JobStatus> getJobsByUid(int uid) {
            ArrayList<JobStatus> matchingJobs = new ArrayList<>();
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            if (jobs != null) {
                matchingJobs.addAll(jobs);
            }
            return matchingJobs;
        }

        public List<JobStatus> getJobsByUser(int userId) {
            ArraySet<JobStatus> jobs;
            ArrayList<JobStatus> result = new ArrayList<>();
            for (int i = this.mJobs.size() - 1; i >= 0; i--) {
                if (UserHandle.getUserId(this.mJobs.keyAt(i)) == userId && (jobs = this.mJobs.get(i)) != null) {
                    result.addAll(jobs);
                }
            }
            return result;
        }

        public boolean add(JobStatus job) {
            int uid = job.getUid();
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            if (jobs == null) {
                jobs = new ArraySet<>();
                this.mJobs.put(uid, jobs);
            }
            return jobs.add(job);
        }

        public boolean remove(JobStatus job) {
            int uid = job.getUid();
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            boolean didRemove = jobs != null ? jobs.remove(job) : false;
            if (didRemove && jobs.size() == 0) {
                this.mJobs.remove(uid);
            }
            return didRemove;
        }

        public boolean contains(JobStatus job) {
            int uid = job.getUid();
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            if (jobs != null) {
                return jobs.contains(job);
            }
            return false;
        }

        public JobStatus get(int uid, int jobId) {
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    JobStatus job = jobs.valueAt(i);
                    if (job.getJobId() == jobId) {
                        return job;
                    }
                }
            }
            return null;
        }

        public List<JobStatus> getAllJobs() {
            ArrayList<JobStatus> allJobs = new ArrayList<>(size());
            for (int i = this.mJobs.size() - 1; i >= 0; i--) {
                ArraySet<JobStatus> jobs = this.mJobs.valueAt(i);
                if (jobs != null) {
                    for (int j = jobs.size() - 1; j >= 0; j--) {
                        allJobs.add(jobs.valueAt(j));
                    }
                }
            }
            return allJobs;
        }

        public void clear() {
            this.mJobs.clear();
        }

        public int size() {
            int total = 0;
            for (int i = this.mJobs.size() - 1; i >= 0; i--) {
                total += this.mJobs.valueAt(i).size();
            }
            return total;
        }

        public int countJobsForUid(int uid) {
            int total = 0;
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            if (jobs != null) {
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    JobStatus job = jobs.valueAt(i);
                    if (job.getUid() == job.getSourceUid()) {
                        total++;
                    }
                }
            }
            return total;
        }

        public void forEachJob(JobStatusFunctor functor) {
            for (int uidIndex = this.mJobs.size() - 1; uidIndex >= 0; uidIndex--) {
                ArraySet<JobStatus> jobs = this.mJobs.valueAt(uidIndex);
                for (int i = jobs.size() - 1; i >= 0; i--) {
                    functor.process(jobs.valueAt(i));
                }
            }
        }

        public void forEachJob(int uid, JobStatusFunctor functor) {
            ArraySet<JobStatus> jobs = this.mJobs.get(uid);
            if (jobs == null) {
                return;
            }
            for (int i = jobs.size() - 1; i >= 0; i--) {
                functor.process(jobs.valueAt(i));
            }
        }
    }
}
