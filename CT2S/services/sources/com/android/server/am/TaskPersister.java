package com.android.server.am;

import android.app.ActivityManager;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.pm.IPackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.Xml;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import com.android.server.job.controllers.JobStatus;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class TaskPersister {
    static final boolean DEBUG_PERSISTER = false;
    static final boolean DEBUG_RESTORER = false;
    private static final long FLUSH_QUEUE = -1;
    private static final String IMAGES_DIRNAME = "recent_images";
    static final String IMAGE_EXTENSION = ".png";
    private static final long INTER_WRITE_DELAY_MS = 500;
    private static final long MAX_INSTALL_WAIT_TIME = 86400000;
    private static final int MAX_WRITE_QUEUE_LENGTH = 6;
    private static final long PRE_TASK_DELAY_MS = 3000;
    private static final String RECENTS_FILENAME = "_task";
    private static final String RESTORED_TASKS_DIRNAME = "restored_recent_tasks";
    static final String TAG = "TaskPersister";
    private static final String TAG_TASK = "task";
    private static final String TASKS_DIRNAME = "recent_tasks";
    private static final String TASK_EXTENSION = ".xml";
    static File sImagesDir;
    static File sRestoredTasksDir;
    static File sTasksDir;
    private final LazyTaskWriterThread mLazyTaskWriterThread;
    private ArrayMap<String, Integer> mPackageUidMap;
    private final ActivityManagerService mService;
    private final ActivityStackSupervisor mStackSupervisor;
    private long mNextWriteTime = 0;
    ArrayList<WriteQueueItem> mWriteQueue = new ArrayList<>();
    private ArrayMap<String, List<List<OtherDeviceTask>>> mOtherDeviceTasksMap = new ArrayMap<>(10);
    private long mExpiredTasksCleanupTime = JobStatus.NO_LATEST_RUNTIME;

    private static class WriteQueueItem {
        private WriteQueueItem() {
        }
    }

    private static class TaskWriteQueueItem extends WriteQueueItem {
        final TaskRecord mTask;

        TaskWriteQueueItem(TaskRecord task) {
            super();
            this.mTask = task;
        }
    }

    private static class ImageWriteQueueItem extends WriteQueueItem {
        final String mFilename;
        Bitmap mImage;

        ImageWriteQueueItem(String filename, Bitmap image) {
            super();
            this.mFilename = filename;
            this.mImage = image;
        }
    }

    TaskPersister(File systemDir, ActivityStackSupervisor stackSupervisor) {
        sTasksDir = new File(systemDir, TASKS_DIRNAME);
        if (!sTasksDir.exists() && !sTasksDir.mkdir()) {
            Slog.e(TAG, "Failure creating tasks directory " + sTasksDir);
        }
        sImagesDir = new File(systemDir, IMAGES_DIRNAME);
        if (!sImagesDir.exists() && !sImagesDir.mkdir()) {
            Slog.e(TAG, "Failure creating images directory " + sImagesDir);
        }
        sRestoredTasksDir = new File(systemDir, RESTORED_TASKS_DIRNAME);
        this.mStackSupervisor = stackSupervisor;
        this.mService = stackSupervisor.mService;
        this.mLazyTaskWriterThread = new LazyTaskWriterThread("LazyTaskWriterThread");
    }

    void startPersisting() {
        this.mLazyTaskWriterThread.start();
    }

    private void removeThumbnails(TaskRecord task) {
        String taskString = Integer.toString(task.taskId);
        for (int queueNdx = this.mWriteQueue.size() - 1; queueNdx >= 0; queueNdx--) {
            WriteQueueItem item = this.mWriteQueue.get(queueNdx);
            if ((item instanceof ImageWriteQueueItem) && ((ImageWriteQueueItem) item).mFilename.startsWith(taskString)) {
                this.mWriteQueue.remove(queueNdx);
            }
        }
    }

    private void yieldIfQueueTooDeep() {
        boolean stall = false;
        synchronized (this) {
            if (this.mNextWriteTime == -1) {
                stall = true;
            }
        }
        if (stall) {
            Thread.yield();
        }
    }

    void wakeup(TaskRecord task, boolean flush) {
        synchronized (this) {
            if (task != null) {
                int queueNdx = this.mWriteQueue.size() - 1;
                while (true) {
                    if (queueNdx < 0) {
                        break;
                    }
                    WriteQueueItem item = this.mWriteQueue.get(queueNdx);
                    if (!(item instanceof TaskWriteQueueItem) || ((TaskWriteQueueItem) item).mTask != task) {
                        queueNdx--;
                    } else if (!task.inRecents) {
                        removeThumbnails(task);
                    }
                }
                if (queueNdx < 0 && task.isPersistable) {
                    this.mWriteQueue.add(new TaskWriteQueueItem(task));
                }
            } else {
                this.mWriteQueue.add(new WriteQueueItem());
            }
            if (flush || this.mWriteQueue.size() > 6) {
                this.mNextWriteTime = -1L;
            } else if (this.mNextWriteTime == 0) {
                this.mNextWriteTime = SystemClock.uptimeMillis() + PRE_TASK_DELAY_MS;
            }
            notifyAll();
        }
        yieldIfQueueTooDeep();
    }

    void flush() {
        synchronized (this) {
            this.mNextWriteTime = -1L;
            notifyAll();
            do {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            } while (this.mNextWriteTime == -1);
        }
    }

    void saveImage(Bitmap image, String filename) {
        synchronized (this) {
            int queueNdx = this.mWriteQueue.size() - 1;
            while (true) {
                if (queueNdx < 0) {
                    break;
                }
                WriteQueueItem item = this.mWriteQueue.get(queueNdx);
                if (item instanceof ImageWriteQueueItem) {
                    ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) item;
                    if (imageWriteQueueItem.mFilename.equals(filename)) {
                        imageWriteQueueItem.mImage = image;
                        break;
                    }
                }
                queueNdx--;
            }
            if (queueNdx < 0) {
                this.mWriteQueue.add(new ImageWriteQueueItem(filename, image));
            }
            if (this.mWriteQueue.size() > 6) {
                this.mNextWriteTime = -1L;
            } else if (this.mNextWriteTime == 0) {
                this.mNextWriteTime = SystemClock.uptimeMillis() + PRE_TASK_DELAY_MS;
            }
            notifyAll();
        }
        yieldIfQueueTooDeep();
    }

    Bitmap getTaskDescriptionIcon(String filename) {
        Bitmap icon = getImageFromWriteQueue(filename);
        return icon != null ? icon : restoreImage(filename);
    }

    Bitmap getImageFromWriteQueue(String filename) {
        Bitmap bitmap;
        synchronized (this) {
            int queueNdx = this.mWriteQueue.size() - 1;
            while (true) {
                if (queueNdx >= 0) {
                    WriteQueueItem item = this.mWriteQueue.get(queueNdx);
                    if (item instanceof ImageWriteQueueItem) {
                        ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) item;
                        if (imageWriteQueueItem.mFilename.equals(filename)) {
                            bitmap = imageWriteQueueItem.mImage;
                            break;
                        }
                    }
                    queueNdx--;
                } else {
                    bitmap = null;
                    break;
                }
            }
        }
        return bitmap;
    }

    private StringWriter saveToXml(TaskRecord task) throws XmlPullParserException, IOException {
        XmlSerializer xmlSerializer = new FastXmlSerializer();
        StringWriter stringWriter = new StringWriter();
        xmlSerializer.setOutput(stringWriter);
        xmlSerializer.startDocument(null, true);
        xmlSerializer.startTag(null, TAG_TASK);
        task.saveToXml(xmlSerializer);
        xmlSerializer.endTag(null, TAG_TASK);
        xmlSerializer.endDocument();
        xmlSerializer.flush();
        return stringWriter;
    }

    private String fileToString(File file) {
        String newline = System.lineSeparator();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuffer sb = new StringBuffer(((int) file.length()) * 2);
            while (true) {
                String line = reader.readLine();
                if (line != null) {
                    sb.append(line + newline);
                } else {
                    reader.close();
                    return sb.toString();
                }
            }
        } catch (IOException e) {
            Slog.e(TAG, "Couldn't read file " + file.getName());
            return null;
        }
    }

    private TaskRecord taskIdToTask(int taskId, ArrayList<TaskRecord> tasks) {
        if (taskId < 0) {
            return null;
        }
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task = tasks.get(taskNdx);
            if (task.taskId == taskId) {
                return task;
            }
        }
        Slog.e(TAG, "Restore affiliation error looking for taskId=" + taskId);
        return null;
    }

    ArrayList<TaskRecord> restoreTasksLocked() throws Throwable {
        BufferedReader reader;
        ArrayList<TaskRecord> tasks = new ArrayList<>();
        ArraySet<Integer> recoveredTaskIds = new ArraySet<>();
        File[] recentFiles = sTasksDir.listFiles();
        if (recentFiles == null) {
            Slog.e(TAG, "Unable to list files from " + sTasksDir);
            return tasks;
        }
        for (File taskFile : recentFiles) {
            BufferedReader reader2 = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(taskFile));
                } catch (Throwable th) {
                    th = th;
                }
            } catch (Exception e) {
                e = e;
            }
            try {
                XmlPullParser in = Xml.newPullParser();
                in.setInput(reader);
                while (true) {
                    int event = in.next();
                    if (event == 1 || event == 3) {
                        break;
                    }
                    String name = in.getName();
                    if (event == 2) {
                        if (TAG_TASK.equals(name)) {
                            TaskRecord task = TaskRecord.restoreFromXml(in, this.mStackSupervisor);
                            if (task != null) {
                                task.isPersistable = true;
                                tasks.add(task);
                                int taskId = task.taskId;
                                recoveredTaskIds.add(Integer.valueOf(taskId));
                                this.mStackSupervisor.setNextTaskId(taskId);
                            } else {
                                Slog.e(TAG, "Unable to restore taskFile=" + taskFile + ": " + fileToString(taskFile));
                            }
                        } else {
                            Slog.wtf(TAG, "restoreTasksLocked Unknown xml event=" + event + " name=" + name);
                        }
                    }
                    XmlUtils.skipCurrentTag(in);
                }
                IoUtils.closeQuietly(reader);
                if (0 != 0) {
                    Slog.d(TAG, "Deleting file=" + taskFile.getName());
                    taskFile.delete();
                }
            } catch (Exception e2) {
                e = e2;
                reader2 = reader;
                Slog.wtf(TAG, "Unable to parse " + taskFile + ". Error ", e);
                Slog.e(TAG, "Failing file: " + fileToString(taskFile));
                IoUtils.closeQuietly(reader2);
                if (1 != 0) {
                    Slog.d(TAG, "Deleting file=" + taskFile.getName());
                    taskFile.delete();
                }
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                IoUtils.closeQuietly(reader2);
                if (0 != 0) {
                    Slog.d(TAG, "Deleting file=" + taskFile.getName());
                    taskFile.delete();
                }
                throw th;
            }
        }
        removeObsoleteFiles(recoveredTaskIds);
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task2 = tasks.get(taskNdx);
            task2.setPrevAffiliate(taskIdToTask(task2.mPrevAffiliateTaskId, tasks));
            task2.setNextAffiliate(taskIdToTask(task2.mNextAffiliateTaskId, tasks));
        }
        TaskRecord[] tasksArray = new TaskRecord[tasks.size()];
        tasks.toArray(tasksArray);
        Arrays.sort(tasksArray, new Comparator<TaskRecord>() {
            @Override
            public int compare(TaskRecord lhs, TaskRecord rhs) {
                long diff = rhs.mLastTimeMoved - lhs.mLastTimeMoved;
                if (diff < 0) {
                    return -1;
                }
                if (diff > 0) {
                    return 1;
                }
                return 0;
            }
        });
        return new ArrayList<>(Arrays.asList(tasksArray));
    }

    private static void removeObsoleteFiles(ArraySet<Integer> persistentTaskIds, File[] files) {
        if (files == null) {
            Slog.e(TAG, "File error accessing recents directory (too many files open?).");
            return;
        }
        for (File file : files) {
            String filename = file.getName();
            int taskIdEnd = filename.indexOf(95);
            if (taskIdEnd > 0) {
                try {
                    int taskId = Integer.valueOf(filename.substring(0, taskIdEnd)).intValue();
                    if (!persistentTaskIds.contains(Integer.valueOf(taskId))) {
                        Slog.d(TAG, "removeObsoleteFile: deleting file=" + file.getName());
                        file.delete();
                    }
                } catch (Exception e) {
                    Slog.wtf(TAG, "removeObsoleteFile: Can't parse file=" + file.getName());
                    file.delete();
                }
            }
        }
    }

    private void removeObsoleteFiles(ArraySet<Integer> persistentTaskIds) {
        removeObsoleteFiles(persistentTaskIds, sTasksDir.listFiles());
        removeObsoleteFiles(persistentTaskIds, sImagesDir.listFiles());
    }

    static Bitmap restoreImage(String filename) {
        return BitmapFactory.decodeFile(sImagesDir + File.separator + filename);
    }

    void restoreTasksFromOtherDeviceLocked() {
        readOtherDeviceTasksFromDisk();
        addOtherDeviceTasksToRecentsLocked();
    }

    private void readOtherDeviceTasksFromDisk() {
        File[] taskFiles;
        synchronized (this.mOtherDeviceTasksMap) {
            this.mOtherDeviceTasksMap.clear();
            this.mExpiredTasksCleanupTime = JobStatus.NO_LATEST_RUNTIME;
            if (sRestoredTasksDir.exists() && (taskFiles = sRestoredTasksDir.listFiles()) != null) {
                long earliestMtime = System.currentTimeMillis();
                SparseArray<List<OtherDeviceTask>> tasksByAffiliateIds = new SparseArray<>(taskFiles.length);
                for (File taskFile : taskFiles) {
                    OtherDeviceTask task = OtherDeviceTask.createFromFile(taskFile);
                    if (task == null) {
                        taskFile.delete();
                    } else {
                        List<OtherDeviceTask> tasks = tasksByAffiliateIds.get(task.mAffiliatedTaskId);
                        if (tasks == null) {
                            tasks = new ArrayList<>();
                            tasksByAffiliateIds.put(task.mAffiliatedTaskId, tasks);
                        }
                        tasks.add(task);
                        long taskMtime = taskFile.lastModified();
                        if (earliestMtime > taskMtime) {
                            earliestMtime = taskMtime;
                        }
                    }
                }
                if (tasksByAffiliateIds.size() > 0) {
                    for (int i = 0; i < tasksByAffiliateIds.size(); i++) {
                        List<OtherDeviceTask> chain = tasksByAffiliateIds.valueAt(i);
                        Collections.sort(chain);
                        String packageName = chain.get(chain.size() - 1).mComponentName.getPackageName();
                        List<List<OtherDeviceTask>> chains = this.mOtherDeviceTasksMap.get(packageName);
                        if (chains == null) {
                            chains = new ArrayList<>();
                            this.mOtherDeviceTasksMap.put(packageName, chains);
                        }
                        chains.add(chain);
                    }
                    this.mExpiredTasksCleanupTime = MAX_INSTALL_WAIT_TIME + earliestMtime;
                }
            }
        }
    }

    private void removeExpiredTasksIfNeeded() {
        synchronized (this.mOtherDeviceTasksMap) {
            long now = System.currentTimeMillis();
            boolean noMoreTasks = this.mOtherDeviceTasksMap.isEmpty();
            if (noMoreTasks || now < this.mExpiredTasksCleanupTime) {
                if (noMoreTasks && this.mPackageUidMap != null) {
                    this.mPackageUidMap = null;
                }
                return;
            }
            long earliestNonExpiredMtime = now;
            this.mExpiredTasksCleanupTime = JobStatus.NO_LATEST_RUNTIME;
            for (int i = this.mOtherDeviceTasksMap.size() - 1; i >= 0; i--) {
                List<List<OtherDeviceTask>> chains = this.mOtherDeviceTasksMap.valueAt(i);
                for (int j = chains.size() - 1; j >= 0; j--) {
                    List<OtherDeviceTask> chain = chains.get(j);
                    boolean removeChain = true;
                    for (int k = chain.size() - 1; k >= 0; k--) {
                        OtherDeviceTask task = chain.get(k);
                        long taskLastModified = task.mFile.lastModified();
                        if (MAX_INSTALL_WAIT_TIME + taskLastModified > now) {
                            if (earliestNonExpiredMtime > taskLastModified) {
                                earliestNonExpiredMtime = taskLastModified;
                            }
                            removeChain = false;
                        }
                    }
                    if (removeChain) {
                        for (int k2 = chain.size() - 1; k2 >= 0; k2--) {
                            File file = chain.get(k2).mFile;
                            file.delete();
                        }
                        chains.remove(j);
                    }
                }
                if (chains.isEmpty()) {
                    this.mOtherDeviceTasksMap.keyAt(i);
                    this.mOtherDeviceTasksMap.removeAt(i);
                }
            }
            if (!this.mOtherDeviceTasksMap.isEmpty()) {
                this.mExpiredTasksCleanupTime = MAX_INSTALL_WAIT_TIME + earliestNonExpiredMtime;
            } else {
                this.mPackageUidMap = null;
            }
        }
    }

    void removeFromPackageCache(String packageName) {
        synchronized (this.mOtherDeviceTasksMap) {
            if (this.mPackageUidMap != null) {
                this.mPackageUidMap.remove(packageName);
            }
        }
    }

    private void addOtherDeviceTasksToRecentsLocked() {
        synchronized (this.mOtherDeviceTasksMap) {
            for (int i = this.mOtherDeviceTasksMap.size() - 1; i >= 0; i--) {
                addOtherDeviceTasksToRecentsLocked(this.mOtherDeviceTasksMap.keyAt(i));
            }
        }
    }

    void addOtherDeviceTasksToRecentsLocked(String packageName) {
        synchronized (this.mOtherDeviceTasksMap) {
            List<List<OtherDeviceTask>> chains = this.mOtherDeviceTasksMap.get(packageName);
            if (chains != null) {
                for (int i = chains.size() - 1; i >= 0; i--) {
                    List<OtherDeviceTask> chain = chains.get(i);
                    if (canAddOtherDeviceTaskChain(chain)) {
                        List<TaskRecord> tasks = new ArrayList<>();
                        TaskRecord prev = null;
                        for (int j = chain.size() - 1; j >= 0; j--) {
                            TaskRecord task = createTaskRecordLocked(chain.get(j));
                            if (task == null) {
                                break;
                            }
                            if (prev == null) {
                                task.mPrevAffiliate = null;
                                task.mPrevAffiliateTaskId = -1;
                                task.mAffiliatedTaskId = task.taskId;
                            } else {
                                prev.mNextAffiliate = task;
                                prev.mNextAffiliateTaskId = task.taskId;
                                task.mAffiliatedTaskId = prev.mAffiliatedTaskId;
                                task.mPrevAffiliate = prev;
                                task.mPrevAffiliateTaskId = prev.taskId;
                            }
                            prev = task;
                            tasks.add(0, task);
                        }
                        if (tasks.size() == chain.size()) {
                            int spaceLeft = ActivityManager.getMaxRecentTasksStatic() - this.mService.mRecentTasks.size();
                            if (spaceLeft >= tasks.size()) {
                                this.mService.mRecentTasks.addAll(this.mService.mRecentTasks.size(), tasks);
                                for (int k = tasks.size() - 1; k >= 0; k--) {
                                    wakeup(tasks.get(k), false);
                                }
                            }
                        }
                        for (int j2 = chain.size() - 1; j2 >= 0; j2--) {
                            chain.get(j2).mFile.delete();
                        }
                        chains.remove(i);
                        if (chains.isEmpty()) {
                            this.mOtherDeviceTasksMap.remove(packageName);
                        }
                    }
                }
            }
        }
    }

    private TaskRecord createTaskRecordLocked(OtherDeviceTask other) throws Throwable {
        BufferedReader reader;
        File file = other.mFile;
        BufferedReader reader2 = null;
        TaskRecord task = null;
        try {
            try {
                reader = new BufferedReader(new FileReader(file));
            } catch (Exception e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            XmlPullParser in = Xml.newPullParser();
            in.setInput(reader);
            while (true) {
                int event = in.next();
                if (event == 1 || event == 3) {
                    break;
                }
                String name = in.getName();
                if (event == 2) {
                    if (TAG_TASK.equals(name)) {
                        task = TaskRecord.restoreFromXml(in, this.mStackSupervisor, this.mStackSupervisor.getNextTaskId());
                        if (task != null) {
                            task.isPersistable = true;
                            task.inRecents = true;
                            task.userId = 0;
                            task.mAffiliatedTaskId = -1;
                            task.mPrevAffiliateTaskId = -1;
                            task.mNextAffiliateTaskId = -1;
                            Integer uid = this.mPackageUidMap.get(task.realActivity.getPackageName());
                            if (uid == null) {
                                Slog.wtf(TAG, "Can't find uid for task=" + task + " in mPackageUidMap=" + this.mPackageUidMap);
                                IoUtils.closeQuietly(reader);
                                return null;
                            }
                            int iIntValue = uid.intValue();
                            task.mCallingUid = iIntValue;
                            task.effectiveUid = iIntValue;
                            for (int i = task.mActivities.size() - 1; i >= 0; i--) {
                                ActivityRecord activity = task.mActivities.get(i);
                                Integer uid2 = this.mPackageUidMap.get(activity.launchedFromPackage);
                                if (uid2 == null) {
                                    Slog.wtf(TAG, "Can't find uid for activity=" + activity + " in mPackageUidMap=" + this.mPackageUidMap);
                                    IoUtils.closeQuietly(reader);
                                    return null;
                                }
                                activity.launchedFromUid = uid2.intValue();
                            }
                        } else {
                            Slog.e(TAG, "Unable to create task for backed-up file=" + file + ": " + fileToString(file));
                        }
                    } else {
                        Slog.wtf(TAG, "createTaskRecordLocked Unknown xml event=" + event + " name=" + name);
                    }
                }
                XmlUtils.skipCurrentTag(in);
            }
        } catch (Exception e2) {
            e = e2;
            reader2 = reader;
            Slog.wtf(TAG, "Unable to parse " + file + ". Error ", e);
            Slog.e(TAG, "Failing file: " + fileToString(file));
            IoUtils.closeQuietly(reader2);
            return task;
        } catch (Throwable th2) {
            th = th2;
            reader2 = reader;
            IoUtils.closeQuietly(reader2);
            throw th;
        }
    }

    private boolean canAddOtherDeviceTaskChain(List<OtherDeviceTask> chain) {
        ArraySet<ComponentName> validComponents = new ArraySet<>();
        IPackageManager pm = AppGlobals.getPackageManager();
        for (int i = 0; i < chain.size(); i++) {
            OtherDeviceTask task = chain.get(i);
            if (task.mFile.exists() && isPackageInstalled(task.mComponentName.getPackageName())) {
                if (task.mLaunchPackages != null) {
                    for (int j = task.mLaunchPackages.size() - 1; j >= 0; j--) {
                        if (!isPackageInstalled(task.mLaunchPackages.valueAt(j))) {
                            return false;
                        }
                    }
                }
                if (!validComponents.contains(task.mComponentName)) {
                    try {
                        if (pm.getActivityInfo(task.mComponentName, 0, 0) == null) {
                            return false;
                        }
                        validComponents.add(task.mComponentName);
                    } catch (RemoteException e) {
                        return false;
                    }
                }
            }
            return false;
        }
        return true;
    }

    private boolean isPackageInstalled(String packageName) {
        if (this.mPackageUidMap != null && this.mPackageUidMap.containsKey(packageName)) {
            return true;
        }
        try {
            int uid = AppGlobals.getPackageManager().getPackageUid(packageName, 0);
            if (uid == -1) {
                return false;
            }
            if (this.mPackageUidMap == null) {
                this.mPackageUidMap = new ArrayMap<>();
            }
            this.mPackageUidMap.put(packageName, Integer.valueOf(uid));
            return true;
        } catch (RemoteException e) {
            return false;
        }
    }

    private class LazyTaskWriterThread extends Thread {
        LazyTaskWriterThread(String name) {
            super(name);
        }

        @Override
        public void run() throws Throwable {
            boolean probablyDone;
            WriteQueueItem item;
            FileOutputStream imageFile;
            AtomicFile atomicFile;
            ArraySet<Integer> persistentTaskIds = new ArraySet<>();
            while (true) {
                synchronized (TaskPersister.this) {
                    probablyDone = TaskPersister.this.mWriteQueue.isEmpty();
                }
                if (probablyDone) {
                    persistentTaskIds.clear();
                    synchronized (TaskPersister.this.mService) {
                        ArrayList<TaskRecord> tasks = TaskPersister.this.mService.mRecentTasks;
                        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
                            TaskRecord task = tasks.get(taskNdx);
                            if ((task.isPersistable || task.inRecents) && (task.stack == null || !task.stack.isHomeStack())) {
                                persistentTaskIds.add(Integer.valueOf(task.taskId));
                            }
                        }
                    }
                    TaskPersister.this.removeObsoleteFiles(persistentTaskIds);
                }
                synchronized (TaskPersister.this) {
                    if (TaskPersister.this.mNextWriteTime != -1) {
                        TaskPersister.this.mNextWriteTime = SystemClock.uptimeMillis() + TaskPersister.INTER_WRITE_DELAY_MS;
                    }
                    while (TaskPersister.this.mWriteQueue.isEmpty()) {
                        if (TaskPersister.this.mNextWriteTime != 0) {
                            TaskPersister.this.mNextWriteTime = 0L;
                            TaskPersister.this.notifyAll();
                        }
                        TaskPersister.this.removeExpiredTasksIfNeeded();
                        try {
                            TaskPersister.this.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    item = TaskPersister.this.mWriteQueue.remove(0);
                    for (long now = SystemClock.uptimeMillis(); now < TaskPersister.this.mNextWriteTime; now = SystemClock.uptimeMillis()) {
                        try {
                            TaskPersister.this.wait(TaskPersister.this.mNextWriteTime - now);
                        } catch (InterruptedException e2) {
                        }
                    }
                }
                if (item instanceof ImageWriteQueueItem) {
                    ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) item;
                    String filename = imageWriteQueueItem.mFilename;
                    Bitmap bitmap = imageWriteQueueItem.mImage;
                    FileOutputStream imageFile2 = null;
                    try {
                        try {
                            imageFile = new FileOutputStream(new File(TaskPersister.sImagesDir, filename));
                        } catch (Exception e3) {
                            e = e3;
                        }
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageFile);
                        IoUtils.closeQuietly(imageFile);
                    } catch (Exception e4) {
                        e = e4;
                        imageFile2 = imageFile;
                        Slog.e(TaskPersister.TAG, "saveImage: unable to save " + filename, e);
                        IoUtils.closeQuietly(imageFile2);
                    } catch (Throwable th2) {
                        th = th2;
                        imageFile2 = imageFile;
                        IoUtils.closeQuietly(imageFile2);
                        throw th;
                    }
                } else if (item instanceof TaskWriteQueueItem) {
                    StringWriter stringWriter = null;
                    TaskRecord task2 = ((TaskWriteQueueItem) item).mTask;
                    synchronized (TaskPersister.this.mService) {
                        if (task2.inRecents) {
                            try {
                                stringWriter = TaskPersister.this.saveToXml(task2);
                            } catch (IOException e5) {
                            } catch (XmlPullParserException e6) {
                            }
                        }
                    }
                    if (stringWriter != null) {
                        FileOutputStream file = null;
                        AtomicFile atomicFile2 = null;
                        try {
                            atomicFile = new AtomicFile(new File(TaskPersister.sTasksDir, String.valueOf(task2.taskId) + TaskPersister.RECENTS_FILENAME + TaskPersister.TASK_EXTENSION));
                        } catch (IOException e7) {
                            e = e7;
                        }
                        try {
                            file = atomicFile.startWrite();
                            file.write(stringWriter.toString().getBytes());
                            file.write(10);
                            atomicFile.finishWrite(file);
                        } catch (IOException e8) {
                            e = e8;
                            atomicFile2 = atomicFile;
                            if (file != null) {
                                atomicFile2.failWrite(file);
                            }
                            Slog.e(TaskPersister.TAG, "Unable to open " + atomicFile2 + " for persisting. " + e);
                        }
                    }
                } else {
                    continue;
                }
            }
        }
    }

    private static class OtherDeviceTask implements Comparable<OtherDeviceTask> {
        final int mAffiliatedTaskId;
        final ComponentName mComponentName;
        final File mFile;
        final ArraySet<String> mLaunchPackages;
        final int mTaskId;

        private OtherDeviceTask(File file, ComponentName componentName, int taskId, int affiliatedTaskId, ArraySet<String> launchPackages) {
            this.mFile = file;
            this.mComponentName = componentName;
            this.mTaskId = taskId;
            this.mAffiliatedTaskId = affiliatedTaskId != -1 ? affiliatedTaskId : taskId;
            this.mLaunchPackages = launchPackages;
        }

        @Override
        public int compareTo(OtherDeviceTask another) {
            return this.mTaskId - another.mTaskId;
        }

        static OtherDeviceTask createFromFile(File file) throws Throwable {
            Exception e;
            BufferedReader reader;
            int event;
            if (file == null || !file.exists()) {
                return null;
            }
            BufferedReader reader2 = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(file));
                } catch (Throwable th) {
                    th = th;
                }
            } catch (IOException e2) {
                e = e2;
            } catch (XmlPullParserException e3) {
                e = e3;
            }
            try {
                XmlPullParser in = Xml.newPullParser();
                in.setInput(reader);
                do {
                    event = in.next();
                    if (event == 1) {
                        break;
                    }
                } while (event != 2);
                if (event == 2) {
                    String name = in.getName();
                    if (TaskPersister.TAG_TASK.equals(name)) {
                        int outerDepth = in.getDepth();
                        ComponentName componentName = null;
                        int taskId = -1;
                        int taskAffiliation = -1;
                        for (int j = in.getAttributeCount() - 1; j >= 0; j--) {
                            String attrName = in.getAttributeName(j);
                            String attrValue = in.getAttributeValue(j);
                            if ("real_activity".equals(attrName)) {
                                componentName = ComponentName.unflattenFromString(attrValue);
                            } else if ("task_id".equals(attrName)) {
                                taskId = Integer.valueOf(attrValue).intValue();
                            } else if ("task_affiliation".equals(attrName)) {
                                taskAffiliation = Integer.valueOf(attrValue).intValue();
                            }
                        }
                        if (componentName == null || taskId == -1) {
                            IoUtils.closeQuietly(reader);
                            return null;
                        }
                        ArraySet<String> launchPackages = null;
                        while (true) {
                            int event2 = in.next();
                            if (event2 == 1 || (event2 == 3 && in.getDepth() >= outerDepth)) {
                                break;
                            }
                            if (event2 == 2) {
                                if ("activity".equals(in.getName())) {
                                    for (int j2 = in.getAttributeCount() - 1; j2 >= 0; j2--) {
                                        if ("launched_from_package".equals(in.getAttributeName(j2))) {
                                            if (launchPackages == null) {
                                                launchPackages = new ArraySet<>();
                                            }
                                            launchPackages.add(in.getAttributeValue(j2));
                                        }
                                    }
                                } else {
                                    XmlUtils.skipCurrentTag(in);
                                }
                            }
                        }
                        OtherDeviceTask otherDeviceTask = new OtherDeviceTask(file, componentName, taskId, taskAffiliation, launchPackages);
                        IoUtils.closeQuietly(reader);
                        return otherDeviceTask;
                    }
                    Slog.wtf(TaskPersister.TAG, "createFromFile: Unknown xml event=" + event + " name=" + name);
                } else {
                    Slog.wtf(TaskPersister.TAG, "createFromFile: Unable to find start tag in file=" + file);
                }
                IoUtils.closeQuietly(reader);
                reader2 = reader;
            } catch (IOException e4) {
                e = e4;
                reader2 = reader;
                e = e;
                Slog.wtf(TaskPersister.TAG, "Unable to parse " + file + ". Error ", e);
                IoUtils.closeQuietly(reader2);
            } catch (XmlPullParserException e5) {
                e = e5;
                reader2 = reader;
                e = e;
                Slog.wtf(TaskPersister.TAG, "Unable to parse " + file + ". Error ", e);
                IoUtils.closeQuietly(reader2);
            } catch (Throwable th2) {
                th = th2;
                reader2 = reader;
                IoUtils.closeQuietly(reader2);
                throw th;
            }
            return null;
        }
    }
}
