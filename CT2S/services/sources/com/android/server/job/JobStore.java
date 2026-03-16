package com.android.server.job;

import android.app.job.JobInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.PersistableBundle;
import android.os.SystemClock;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Pair;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.server.IoThread;
import com.android.server.job.controllers.JobStatus;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class JobStore {
    private static final boolean DEBUG = false;
    private static final int JOBS_FILE_VERSION = 0;
    private static final int MAX_OPS_BEFORE_WRITE = 1;
    private static final String TAG = "JobStore";
    private static final String XML_TAG_EXTRAS = "extras";
    private static final String XML_TAG_ONEOFF = "one-off";
    private static final String XML_TAG_PARAMS_CONSTRAINTS = "constraints";
    private static final String XML_TAG_PERIODIC = "periodic";
    private static JobStore sSingleton;
    private static final Object sSingletonLock = new Object();
    final Context mContext;
    final ArraySet<JobStatus> mJobSet;
    private final AtomicFile mJobsFile;
    private final Handler mIoHandler = IoThread.getHandler();
    private int mDirtyOperations = 0;

    static JobStore initAndGet(JobSchedulerService jobManagerService) {
        JobStore jobStore;
        synchronized (sSingletonLock) {
            if (sSingleton == null) {
                sSingleton = new JobStore(jobManagerService.getContext(), Environment.getDataDirectory());
            }
            jobStore = sSingleton;
        }
        return jobStore;
    }

    public static JobStore initAndGetForTesting(Context context, File dataDir) {
        JobStore jobStoreUnderTest = new JobStore(context, dataDir);
        jobStoreUnderTest.clear();
        return jobStoreUnderTest;
    }

    private JobStore(Context context, File dataDir) {
        this.mContext = context;
        File systemDir = new File(dataDir, "system");
        File jobDir = new File(systemDir, "job");
        jobDir.mkdirs();
        this.mJobsFile = new AtomicFile(new File(jobDir, "jobs.xml"));
        this.mJobSet = new ArraySet<>();
        readJobMapFromDisk(this.mJobSet);
    }

    public boolean add(JobStatus jobStatus) {
        boolean replaced = this.mJobSet.remove(jobStatus);
        this.mJobSet.add(jobStatus);
        if (jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
        }
        return replaced;
    }

    public boolean containsJobIdForUid(int jobId, int uId) {
        for (int i = this.mJobSet.size() - 1; i >= 0; i--) {
            JobStatus ts = this.mJobSet.valueAt(i);
            if (ts.getUid() == uId && ts.getJobId() == jobId) {
                return true;
            }
        }
        return DEBUG;
    }

    boolean containsJob(JobStatus jobStatus) {
        return this.mJobSet.contains(jobStatus);
    }

    public int size() {
        return this.mJobSet.size();
    }

    public boolean remove(JobStatus jobStatus) {
        boolean removed = this.mJobSet.remove(jobStatus);
        if (!removed) {
            return DEBUG;
        }
        if (jobStatus.isPersisted()) {
            maybeWriteStatusToDiskAsync();
            return removed;
        }
        return removed;
    }

    public void clear() {
        this.mJobSet.clear();
        maybeWriteStatusToDiskAsync();
    }

    public List<JobStatus> getJobsByUser(int userHandle) {
        List<JobStatus> matchingJobs = new ArrayList<>();
        for (JobStatus ts : this.mJobSet) {
            if (UserHandle.getUserId(ts.getUid()) == userHandle) {
                matchingJobs.add(ts);
            }
        }
        return matchingJobs;
    }

    public List<JobStatus> getJobsByUid(int uid) {
        List<JobStatus> matchingJobs = new ArrayList<>();
        for (JobStatus ts : this.mJobSet) {
            if (ts.getUid() == uid) {
                matchingJobs.add(ts);
            }
        }
        return matchingJobs;
    }

    public JobStatus getJobByUidAndJobId(int uid, int jobId) {
        for (JobStatus ts : this.mJobSet) {
            if (ts.getUid() == uid && ts.getJobId() == jobId) {
                return ts;
            }
        }
        return null;
    }

    public ArraySet<JobStatus> getJobs() {
        return this.mJobSet;
    }

    private void maybeWriteStatusToDiskAsync() {
        this.mDirtyOperations++;
        if (this.mDirtyOperations >= 1) {
            this.mIoHandler.post(new WriteJobsMapToDiskRunnable());
        }
    }

    public void readJobMapFromDisk(ArraySet<JobStatus> jobSet) {
        new ReadJobMapFromDiskRunnable(jobSet).run();
    }

    private class WriteJobsMapToDiskRunnable implements Runnable {
        private WriteJobsMapToDiskRunnable() {
        }

        @Override
        public void run() {
            SystemClock.elapsedRealtime();
            List<JobStatus> mStoreCopy = new ArrayList<>();
            synchronized (JobStore.this) {
                for (int i = 0; i < JobStore.this.mJobSet.size(); i++) {
                    JobStatus jobStatus = JobStore.this.mJobSet.valueAt(i);
                    JobStatus copy = new JobStatus(jobStatus.getJob(), jobStatus.getUid(), jobStatus.getEarliestRunTime(), jobStatus.getLatestRunTimeElapsed());
                    mStoreCopy.add(copy);
                }
            }
            writeJobsMapImpl(mStoreCopy);
        }

        private void writeJobsMapImpl(List<JobStatus> jobList) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XmlSerializer fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(baos, "utf-8");
                fastXmlSerializer.startDocument(null, true);
                fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                fastXmlSerializer.startTag(null, "job-info");
                fastXmlSerializer.attribute(null, "version", Integer.toString(0));
                for (int i = 0; i < jobList.size(); i++) {
                    JobStatus jobStatus = jobList.get(i);
                    fastXmlSerializer.startTag(null, "job");
                    addIdentifierAttributesToJobTag(fastXmlSerializer, jobStatus);
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
            } catch (XmlPullParserException e2) {
            }
        }

        private void addIdentifierAttributesToJobTag(XmlSerializer out, JobStatus jobStatus) throws IOException {
            out.attribute(null, "jobid", Integer.toString(jobStatus.getJobId()));
            out.attribute(null, "package", jobStatus.getServiceComponent().getPackageName());
            out.attribute(null, "class", jobStatus.getServiceComponent().getClassName());
            out.attribute(null, "uid", Integer.toString(jobStatus.getUid()));
        }

        private void writeBundleToXml(PersistableBundle extras, XmlSerializer out) throws XmlPullParserException, IOException {
            out.startTag(null, JobStore.XML_TAG_EXTRAS);
            extras.saveToXml(out);
            out.endTag(null, JobStore.XML_TAG_EXTRAS);
        }

        private void writeConstraintsToXml(XmlSerializer out, JobStatus jobStatus) throws IOException {
            out.startTag(null, JobStore.XML_TAG_PARAMS_CONSTRAINTS);
            if (jobStatus.hasUnmeteredConstraint()) {
                out.attribute(null, "unmetered", Boolean.toString(true));
            }
            if (jobStatus.hasConnectivityConstraint()) {
                out.attribute(null, "connectivity", Boolean.toString(true));
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
        private final ArraySet<JobStatus> jobSet;

        ReadJobMapFromDiskRunnable(ArraySet<JobStatus> jobSet) {
            this.jobSet = jobSet;
        }

        @Override
        public void run() {
            try {
                FileInputStream fis = JobStore.this.mJobsFile.openRead();
                synchronized (JobStore.this) {
                    List<JobStatus> jobs = readJobMapImpl(fis);
                    if (jobs != null) {
                        for (int i = 0; i < jobs.size(); i++) {
                            this.jobSet.add(jobs.get(i));
                        }
                    }
                }
                fis.close();
            } catch (FileNotFoundException e) {
            } catch (IOException e2) {
            } catch (XmlPullParserException e3) {
            }
        }

        private List<JobStatus> readJobMapImpl(FileInputStream fis) throws XmlPullParserException, IOException {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(fis, null);
            int eventType = parser.getEventType();
            while (eventType != 2 && eventType != 1) {
                eventType = parser.next();
                Slog.d(JobStore.TAG, parser.getName());
            }
            if (eventType == 1) {
                return null;
            }
            String tagName = parser.getName();
            if (!"job-info".equals(tagName)) {
                return null;
            }
            List<JobStatus> jobs = new ArrayList<>();
            try {
                int version = Integer.valueOf(parser.getAttributeValue(null, "version")).intValue();
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
            int eventType2;
            int eventType3;
            try {
                JobInfo.Builder jobBuilder = buildBuilderFromXml(parser);
                jobBuilder.setPersisted(true);
                int uid = Integer.valueOf(parser.getAttributeValue(null, "uid")).intValue();
                do {
                    eventType = parser.next();
                } while (eventType == 4);
                if (eventType != 2 || !JobStore.XML_TAG_PARAMS_CONSTRAINTS.equals(parser.getName())) {
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
                        Pair<Long, Long> runtimes = buildExecutionTimesFromXml(parser);
                        if (JobStore.XML_TAG_PERIODIC.equals(parser.getName())) {
                            try {
                                String val = parser.getAttributeValue(null, "period");
                                jobBuilder.setPeriodic(Long.valueOf(val).longValue());
                            } catch (NumberFormatException e) {
                                Slog.d(JobStore.TAG, "Error reading periodic execution criteria, skipping.");
                                return null;
                            }
                        } else if (JobStore.XML_TAG_ONEOFF.equals(parser.getName())) {
                            try {
                                if (((Long) runtimes.first).longValue() != 0) {
                                    jobBuilder.setMinimumLatency(((Long) runtimes.first).longValue() - SystemClock.elapsedRealtime());
                                }
                                if (((Long) runtimes.second).longValue() != JobStatus.NO_LATEST_RUNTIME) {
                                    jobBuilder.setOverrideDeadline(((Long) runtimes.second).longValue() - SystemClock.elapsedRealtime());
                                }
                            } catch (NumberFormatException e2) {
                                Slog.d(JobStore.TAG, "Error reading job execution criteria, skipping.");
                                return null;
                            }
                        } else {
                            return null;
                        }
                        maybeBuildBackoffPolicyFromXml(jobBuilder, parser);
                        parser.nextTag();
                        do {
                            eventType3 = parser.next();
                        } while (eventType3 == 4);
                        if (eventType3 != 2 || !JobStore.XML_TAG_EXTRAS.equals(parser.getName())) {
                            return null;
                        }
                        PersistableBundle extras = PersistableBundle.restoreFromXml(parser);
                        jobBuilder.setExtras(extras);
                        parser.nextTag();
                        return new JobStatus(jobBuilder.build(), uid, ((Long) runtimes.first).longValue(), ((Long) runtimes.second).longValue());
                    } catch (NumberFormatException e3) {
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
            int jobId = Integer.valueOf(parser.getAttributeValue(null, "jobid")).intValue();
            String packageName = parser.getAttributeValue(null, "package");
            String className = parser.getAttributeValue(null, "class");
            ComponentName cname = new ComponentName(packageName, className);
            return new JobInfo.Builder(jobId, cname);
        }

        private void buildConstraintsFromXml(JobInfo.Builder jobBuilder, XmlPullParser parser) {
            String val = parser.getAttributeValue(null, "unmetered");
            if (val != null) {
                jobBuilder.setRequiredNetworkType(2);
            }
            String val2 = parser.getAttributeValue(null, "connectivity");
            if (val2 != null) {
                jobBuilder.setRequiredNetworkType(1);
            }
            String val3 = parser.getAttributeValue(null, "idle");
            if (val3 != null) {
                jobBuilder.setRequiresDeviceIdle(true);
            }
            String val4 = parser.getAttributeValue(null, "charging");
            if (val4 != null) {
                jobBuilder.setRequiresCharging(true);
            }
        }

        private void maybeBuildBackoffPolicyFromXml(JobInfo.Builder jobBuilder, XmlPullParser parser) {
            String val = parser.getAttributeValue(null, "initial-backoff");
            if (val != null) {
                long initialBackoff = Long.valueOf(val).longValue();
                int backoffPolicy = Integer.valueOf(parser.getAttributeValue(null, "backoff-policy")).intValue();
                jobBuilder.setBackoffCriteria(initialBackoff, backoffPolicy);
            }
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
}
