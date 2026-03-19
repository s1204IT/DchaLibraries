package com.android.server.am;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.FileUtils;
import android.os.Process;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.util.Xml;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class TaskPersister {
    static final boolean DEBUG = false;
    private static final long FLUSH_QUEUE = -1;
    private static final String IMAGES_DIRNAME = "recent_images";
    static final String IMAGE_EXTENSION = ".png";
    private static final long INTER_WRITE_DELAY_MS = 500;
    private static final int MAX_WRITE_QUEUE_LENGTH = 6;
    private static final String PERSISTED_TASK_IDS_FILENAME = "persisted_taskIds.txt";
    private static final long PRE_TASK_DELAY_MS = 3000;
    static final String TAG = "TaskPersister";
    private static final String TAG_TASK = "task";
    private static final String TASKS_DIRNAME = "recent_tasks";
    private static final String TASK_FILENAME_SUFFIX = "_task.xml";
    private final Object mIoLock;
    private final LazyTaskWriterThread mLazyTaskWriterThread;
    private long mNextWriteTime;
    private final RecentTasks mRecentTasks;
    private final ActivityManagerService mService;
    private final ActivityStackSupervisor mStackSupervisor;
    private final File mTaskIdsDir;
    private final SparseArray<SparseBooleanArray> mTaskIdsInFile;
    ArrayList<WriteQueueItem> mWriteQueue;

    private static class WriteQueueItem {
        private WriteQueueItem() {
        }
    }

    private static class TaskWriteQueueItem extends WriteQueueItem {
        final TaskRecord mTask;

        TaskWriteQueueItem(TaskRecord taskRecord) {
            super();
            this.mTask = taskRecord;
        }
    }

    private static class ImageWriteQueueItem extends WriteQueueItem {
        final String mFilePath;
        Bitmap mImage;

        ImageWriteQueueItem(String str, Bitmap bitmap) {
            super();
            this.mFilePath = str;
            this.mImage = bitmap;
        }
    }

    TaskPersister(File file, ActivityStackSupervisor activityStackSupervisor, ActivityManagerService activityManagerService, RecentTasks recentTasks) {
        this.mTaskIdsInFile = new SparseArray<>();
        this.mIoLock = new Object();
        this.mNextWriteTime = 0L;
        this.mWriteQueue = new ArrayList<>();
        File file2 = new File(file, IMAGES_DIRNAME);
        if (file2.exists() && (!FileUtils.deleteContents(file2) || !file2.delete())) {
            Slog.i(TAG, "Failure deleting legacy images directory: " + file2);
        }
        File file3 = new File(file, TASKS_DIRNAME);
        if (file3.exists() && (!FileUtils.deleteContents(file3) || !file3.delete())) {
            Slog.i(TAG, "Failure deleting legacy tasks directory: " + file3);
        }
        this.mTaskIdsDir = new File(Environment.getDataDirectory(), "system_de");
        this.mStackSupervisor = activityStackSupervisor;
        this.mService = activityManagerService;
        this.mRecentTasks = recentTasks;
        this.mLazyTaskWriterThread = new LazyTaskWriterThread("LazyTaskWriterThread");
    }

    @VisibleForTesting
    TaskPersister(File file) {
        this.mTaskIdsInFile = new SparseArray<>();
        this.mIoLock = new Object();
        this.mNextWriteTime = 0L;
        this.mWriteQueue = new ArrayList<>();
        this.mTaskIdsDir = file;
        this.mStackSupervisor = null;
        this.mService = null;
        this.mRecentTasks = null;
        this.mLazyTaskWriterThread = new LazyTaskWriterThread("LazyTaskWriterThreadTest");
    }

    void startPersisting() {
        if (!this.mLazyTaskWriterThread.isAlive()) {
            this.mLazyTaskWriterThread.start();
        }
    }

    private void removeThumbnails(TaskRecord taskRecord) {
        String string = Integer.toString(taskRecord.taskId);
        for (int size = this.mWriteQueue.size() - 1; size >= 0; size--) {
            WriteQueueItem writeQueueItem = this.mWriteQueue.get(size);
            if ((writeQueueItem instanceof ImageWriteQueueItem) && new File(((ImageWriteQueueItem) writeQueueItem).mFilePath).getName().startsWith(string)) {
                this.mWriteQueue.remove(size);
            }
        }
    }

    private void yieldIfQueueTooDeep() {
        boolean z;
        synchronized (this) {
            if (this.mNextWriteTime == -1) {
                z = true;
            } else {
                z = false;
            }
        }
        if (z) {
            Thread.yield();
        }
    }

    SparseBooleanArray loadPersistedTaskIdsForUser(int i) {
        BufferedReader bufferedReader;
        Exception e;
        if (this.mTaskIdsInFile.get(i) != null) {
            return this.mTaskIdsInFile.get(i).clone();
        }
        SparseBooleanArray sparseBooleanArray = new SparseBooleanArray();
        synchronized (this.mIoLock) {
            try {
            } catch (Throwable th) {
                th = th;
            }
            try {
                bufferedReader = new BufferedReader(new FileReader(getUserPersistedTaskIdsFile(i)));
                while (true) {
                    try {
                        String line = bufferedReader.readLine();
                        if (line == null) {
                            break;
                        }
                        for (String str : line.split("\\s+")) {
                            sparseBooleanArray.put(Integer.parseInt(str), true);
                        }
                    } catch (FileNotFoundException e2) {
                        IoUtils.closeQuietly(bufferedReader);
                        this.mTaskIdsInFile.put(i, sparseBooleanArray);
                        return sparseBooleanArray.clone();
                    } catch (Exception e3) {
                        e = e3;
                        Slog.e(TAG, "Error while reading taskIds file for user " + i, e);
                    }
                }
            } catch (FileNotFoundException e4) {
                bufferedReader = null;
            } catch (Exception e5) {
                bufferedReader = null;
                e = e5;
            } catch (Throwable th2) {
                th = th2;
                IoUtils.closeQuietly((AutoCloseable) null);
                throw th;
            }
            IoUtils.closeQuietly(bufferedReader);
        }
        this.mTaskIdsInFile.put(i, sparseBooleanArray);
        return sparseBooleanArray.clone();
    }

    @VisibleForTesting
    void writePersistedTaskIdsForUser(SparseBooleanArray sparseBooleanArray, int i) {
        if (i < 0) {
            return;
        }
        File userPersistedTaskIdsFile = getUserPersistedTaskIdsFile(i);
        synchronized (this.mIoLock) {
            BufferedWriter bufferedWriter = null;
            try {
                try {
                    BufferedWriter bufferedWriter2 = new BufferedWriter(new FileWriter(userPersistedTaskIdsFile));
                    for (int i2 = 0; i2 < sparseBooleanArray.size(); i2++) {
                        try {
                            if (sparseBooleanArray.valueAt(i2)) {
                                bufferedWriter2.write(String.valueOf(sparseBooleanArray.keyAt(i2)));
                                bufferedWriter2.newLine();
                            }
                        } catch (Exception e) {
                            e = e;
                            bufferedWriter = bufferedWriter2;
                            Slog.e(TAG, "Error while writing taskIds file for user " + i, e);
                            IoUtils.closeQuietly(bufferedWriter);
                        } catch (Throwable th) {
                            th = th;
                            bufferedWriter = bufferedWriter2;
                            IoUtils.closeQuietly(bufferedWriter);
                            throw th;
                        }
                    }
                    IoUtils.closeQuietly(bufferedWriter2);
                } catch (Exception e2) {
                    e = e2;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }
    }

    void unloadUserDataFromMemory(int i) {
        this.mTaskIdsInFile.delete(i);
    }

    void wakeup(TaskRecord taskRecord, boolean z) {
        synchronized (this) {
            try {
                if (taskRecord != null) {
                    int size = this.mWriteQueue.size() - 1;
                    while (true) {
                        if (size < 0) {
                            break;
                        }
                        WriteQueueItem writeQueueItem = this.mWriteQueue.get(size);
                        if (!(writeQueueItem instanceof TaskWriteQueueItem) || ((TaskWriteQueueItem) writeQueueItem).mTask != taskRecord) {
                            size--;
                        } else if (!taskRecord.inRecents) {
                            removeThumbnails(taskRecord);
                        }
                    }
                    if (size < 0 && taskRecord.isPersistable) {
                        this.mWriteQueue.add(new TaskWriteQueueItem(taskRecord));
                    }
                } else {
                    this.mWriteQueue.add(new WriteQueueItem());
                }
                if (z || this.mWriteQueue.size() > 6) {
                    this.mNextWriteTime = -1L;
                } else if (this.mNextWriteTime == 0) {
                    this.mNextWriteTime = SystemClock.uptimeMillis() + PRE_TASK_DELAY_MS;
                }
                notifyAll();
            } catch (Throwable th) {
                throw th;
            }
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

    void saveImage(Bitmap bitmap, String str) {
        synchronized (this) {
            int size = this.mWriteQueue.size() - 1;
            while (true) {
                if (size < 0) {
                    break;
                }
                WriteQueueItem writeQueueItem = this.mWriteQueue.get(size);
                if (writeQueueItem instanceof ImageWriteQueueItem) {
                    ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) writeQueueItem;
                    if (imageWriteQueueItem.mFilePath.equals(str)) {
                        imageWriteQueueItem.mImage = bitmap;
                        break;
                    }
                }
                size--;
            }
            if (size < 0) {
                this.mWriteQueue.add(new ImageWriteQueueItem(str, bitmap));
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

    Bitmap getTaskDescriptionIcon(String str) {
        Bitmap imageFromWriteQueue = getImageFromWriteQueue(str);
        if (imageFromWriteQueue != null) {
            return imageFromWriteQueue;
        }
        return restoreImage(str);
    }

    Bitmap getImageFromWriteQueue(String str) {
        synchronized (this) {
            for (int size = this.mWriteQueue.size() - 1; size >= 0; size--) {
                WriteQueueItem writeQueueItem = this.mWriteQueue.get(size);
                if (writeQueueItem instanceof ImageWriteQueueItem) {
                    ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) writeQueueItem;
                    if (imageWriteQueueItem.mFilePath.equals(str)) {
                        return imageWriteQueueItem.mImage;
                    }
                }
            }
            return null;
        }
    }

    private StringWriter saveToXml(TaskRecord taskRecord) throws XmlPullParserException, IOException {
        XmlSerializer fastXmlSerializer = new FastXmlSerializer();
        StringWriter stringWriter = new StringWriter();
        fastXmlSerializer.setOutput(stringWriter);
        fastXmlSerializer.startDocument(null, true);
        fastXmlSerializer.startTag(null, TAG_TASK);
        taskRecord.saveToXml(fastXmlSerializer);
        fastXmlSerializer.endTag(null, TAG_TASK);
        fastXmlSerializer.endDocument();
        fastXmlSerializer.flush();
        return stringWriter;
    }

    private String fileToString(File file) {
        String strLineSeparator = System.lineSeparator();
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            StringBuffer stringBuffer = new StringBuffer(((int) file.length()) * 2);
            while (true) {
                String line = bufferedReader.readLine();
                if (line != null) {
                    stringBuffer.append(line + strLineSeparator);
                } else {
                    bufferedReader.close();
                    return stringBuffer.toString();
                }
            }
        } catch (IOException e) {
            Slog.e(TAG, "Couldn't read file " + file.getName());
            return null;
        }
    }

    private TaskRecord taskIdToTask(int i, ArrayList<TaskRecord> arrayList) {
        if (i < 0) {
            return null;
        }
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = arrayList.get(size);
            if (taskRecord.taskId == i) {
                return taskRecord;
            }
        }
        Slog.e(TAG, "Restore affiliation error looking for taskId=" + i);
        return null;
    }

    List<TaskRecord> restoreTasksForUserLocked(int i, SparseBooleanArray sparseBooleanArray) throws Throwable {
        BufferedReader bufferedReader;
        ArrayList<TaskRecord> arrayList = new ArrayList<>();
        ArraySet arraySet = new ArraySet();
        File userTasksDir = getUserTasksDir(i);
        File[] fileArrListFiles = userTasksDir.listFiles();
        if (fileArrListFiles == null) {
            Slog.e(TAG, "restoreTasksForUserLocked: Unable to list files from " + userTasksDir);
            return arrayList;
        }
        ?? r7 = 0;
        int i2 = 0;
        while (true) {
            ?? r9 = 1;
            if (i2 >= fileArrListFiles.length) {
                break;
            }
            File file = fileArrListFiles[i2];
            if (file.getName().endsWith(TASK_FILENAME_SUFFIX)) {
                try {
                    int i3 = Integer.parseInt(file.getName().substring(r7, file.getName().length() - TASK_FILENAME_SUFFIX.length()));
                    try {
                        if (sparseBooleanArray.get(i3, r7)) {
                            Slog.w(TAG, "Task #" + i3 + " has already been created so we don't restore again");
                        } else {
                            BufferedReader bufferedReader2 = null;
                            try {
                                try {
                                    bufferedReader = new BufferedReader(new FileReader(file));
                                } catch (Throwable th) {
                                    th = th;
                                    bufferedReader = null;
                                }
                            } catch (Exception e) {
                                e = e;
                            }
                            try {
                                XmlPullParser xmlPullParserNewPullParser = Xml.newPullParser();
                                xmlPullParserNewPullParser.setInput(bufferedReader);
                                while (true) {
                                    int next = xmlPullParserNewPullParser.next();
                                    if (next == r9 || next == 3) {
                                        break;
                                    }
                                    String name = xmlPullParserNewPullParser.getName();
                                    if (next == 2) {
                                        if (TAG_TASK.equals(name)) {
                                            TaskRecord taskRecordRestoreFromXml = TaskRecord.restoreFromXml(xmlPullParserNewPullParser, this.mStackSupervisor);
                                            if (taskRecordRestoreFromXml != null) {
                                                int i4 = taskRecordRestoreFromXml.taskId;
                                                if (this.mStackSupervisor.anyTaskForIdLocked(i4, r9) != null) {
                                                    Slog.wtf(TAG, "Existing task with taskId " + i4 + "found");
                                                } else if (i != taskRecordRestoreFromXml.userId) {
                                                    Slog.wtf(TAG, "Task with userId " + taskRecordRestoreFromXml.userId + " found in " + userTasksDir.getAbsolutePath());
                                                } else {
                                                    this.mStackSupervisor.setNextTaskIdForUserLocked(i4, i);
                                                    taskRecordRestoreFromXml.isPersistable = r9;
                                                    arrayList.add(taskRecordRestoreFromXml);
                                                    arraySet.add(Integer.valueOf(i4));
                                                }
                                            } else {
                                                Slog.e(TAG, "restoreTasksForUserLocked: Unable to restore taskFile=" + file + ": " + fileToString(file));
                                            }
                                        } else {
                                            Slog.wtf(TAG, "restoreTasksForUserLocked: Unknown xml event=" + next + " name=" + name);
                                        }
                                    }
                                    XmlUtils.skipCurrentTag(xmlPullParserNewPullParser);
                                    r9 = 1;
                                }
                                IoUtils.closeQuietly(bufferedReader);
                            } catch (Exception e2) {
                                e = e2;
                                bufferedReader2 = bufferedReader;
                                Slog.wtf(TAG, "Unable to parse " + file + ". Error ", e);
                                StringBuilder sb = new StringBuilder();
                                sb.append("Failing file: ");
                                sb.append(fileToString(file));
                                Slog.e(TAG, sb.toString());
                                IoUtils.closeQuietly(bufferedReader2);
                                file.delete();
                            } catch (Throwable th2) {
                                th = th2;
                                IoUtils.closeQuietly(bufferedReader);
                                throw th;
                            }
                        }
                    } catch (NumberFormatException e3) {
                        e = e3;
                        Slog.w(TAG, "Unexpected task file name", e);
                    }
                } catch (NumberFormatException e4) {
                    e = e4;
                }
            }
            i2++;
            r7 = 0;
        }
        removeObsoleteFiles(arraySet, userTasksDir.listFiles());
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            TaskRecord taskRecord = arrayList.get(size);
            taskRecord.setPrevAffiliate(taskIdToTask(taskRecord.mPrevAffiliateTaskId, arrayList));
            taskRecord.setNextAffiliate(taskIdToTask(taskRecord.mNextAffiliateTaskId, arrayList));
        }
        Collections.sort(arrayList, new Comparator<TaskRecord>() {
            @Override
            public int compare(TaskRecord taskRecord2, TaskRecord taskRecord3) {
                long j = taskRecord3.mLastTimeMoved - taskRecord2.mLastTimeMoved;
                if (j < 0) {
                    return -1;
                }
                if (j > 0) {
                    return 1;
                }
                return 0;
            }
        });
        return arrayList;
    }

    private static void removeObsoleteFiles(ArraySet<Integer> arraySet, File[] fileArr) {
        if (fileArr == null) {
            Slog.e(TAG, "File error accessing recents directory (directory doesn't exist?).");
            return;
        }
        for (File file : fileArr) {
            String name = file.getName();
            int iIndexOf = name.indexOf(95);
            if (iIndexOf > 0) {
                try {
                    if (!arraySet.contains(Integer.valueOf(Integer.parseInt(name.substring(0, iIndexOf))))) {
                        file.delete();
                    }
                } catch (Exception e) {
                    Slog.wtf(TAG, "removeObsoleteFiles: Can't parse file=" + file.getName());
                    file.delete();
                }
            }
        }
    }

    private void writeTaskIdsFiles() {
        int i;
        SparseArray sparseArray = new SparseArray();
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                for (int i2 : this.mRecentTasks.usersWithRecentsLoadedLocked()) {
                    SparseBooleanArray taskIdsForUser = this.mRecentTasks.getTaskIdsForUser(i2);
                    SparseBooleanArray sparseBooleanArray = this.mTaskIdsInFile.get(i2);
                    if (sparseBooleanArray == null || !sparseBooleanArray.equals(taskIdsForUser)) {
                        SparseBooleanArray sparseBooleanArrayClone = taskIdsForUser.clone();
                        this.mTaskIdsInFile.put(i2, sparseBooleanArrayClone);
                        sparseArray.put(i2, sparseBooleanArrayClone);
                    }
                }
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        for (i = 0; i < sparseArray.size(); i++) {
            writePersistedTaskIdsForUser((SparseBooleanArray) sparseArray.valueAt(i), sparseArray.keyAt(i));
        }
    }

    private void removeObsoleteFiles(ArraySet<Integer> arraySet) {
        int[] iArrUsersWithRecentsLoadedLocked;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                iArrUsersWithRecentsLoadedLocked = this.mRecentTasks.usersWithRecentsLoadedLocked();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        for (int i : iArrUsersWithRecentsLoadedLocked) {
            removeObsoleteFiles(arraySet, getUserImagesDir(i).listFiles());
            removeObsoleteFiles(arraySet, getUserTasksDir(i).listFiles());
        }
    }

    static Bitmap restoreImage(String str) {
        return BitmapFactory.decodeFile(str);
    }

    private File getUserPersistedTaskIdsFile(int i) {
        File file = new File(this.mTaskIdsDir, String.valueOf(i));
        if (!file.exists() && !file.mkdirs()) {
            Slog.e(TAG, "Error while creating user directory: " + file);
        }
        return new File(file, PERSISTED_TASK_IDS_FILENAME);
    }

    static File getUserTasksDir(int i) {
        File file = new File(Environment.getDataSystemCeDirectory(i), TASKS_DIRNAME);
        if (!file.exists() && !file.mkdir()) {
            Slog.e(TAG, "Failure creating tasks directory for user " + i + ": " + file);
        }
        return file;
    }

    static File getUserImagesDir(int i) {
        return new File(Environment.getDataSystemCeDirectory(i), IMAGES_DIRNAME);
    }

    private static boolean createParentDirectory(String str) {
        File parentFile = new File(str).getParentFile();
        return parentFile.exists() || parentFile.mkdirs();
    }

    private class LazyTaskWriterThread extends Thread {
        LazyTaskWriterThread(String str) {
            super(str);
        }

        @Override
        public void run() throws Throwable {
            boolean zIsEmpty;
            Process.setThreadPriority(10);
            ArraySet<Integer> arraySet = new ArraySet<>();
            while (true) {
                synchronized (TaskPersister.this) {
                    zIsEmpty = TaskPersister.this.mWriteQueue.isEmpty();
                }
                if (zIsEmpty) {
                    arraySet.clear();
                    synchronized (TaskPersister.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            TaskPersister.this.mRecentTasks.getPersistableTaskIds(arraySet);
                            TaskPersister.this.mService.mWindowManager.removeObsoleteTaskFiles(arraySet, TaskPersister.this.mRecentTasks.usersWithRecentsLoadedLocked());
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    TaskPersister.this.removeObsoleteFiles(arraySet);
                }
                TaskPersister.this.writeTaskIdsFiles();
                processNextItem();
            }
        }

        private void processNextItem() throws Throwable {
            WriteQueueItem writeQueueItemRemove;
            StringWriter stringWriterSaveToXml;
            AtomicFile atomicFile;
            FileOutputStream fileOutputStreamStartWrite;
            FileOutputStream fileOutputStream;
            synchronized (TaskPersister.this) {
                if (TaskPersister.this.mNextWriteTime != -1) {
                    TaskPersister.this.mNextWriteTime = SystemClock.uptimeMillis() + 500;
                }
                while (TaskPersister.this.mWriteQueue.isEmpty()) {
                    if (TaskPersister.this.mNextWriteTime != 0) {
                        TaskPersister.this.mNextWriteTime = 0L;
                        TaskPersister.this.notifyAll();
                    }
                    try {
                        TaskPersister.this.wait();
                    } catch (InterruptedException e) {
                    }
                }
                writeQueueItemRemove = TaskPersister.this.mWriteQueue.remove(0);
                for (long jUptimeMillis = SystemClock.uptimeMillis(); jUptimeMillis < TaskPersister.this.mNextWriteTime; jUptimeMillis = SystemClock.uptimeMillis()) {
                    try {
                        TaskPersister.this.wait(TaskPersister.this.mNextWriteTime - jUptimeMillis);
                    } catch (InterruptedException e2) {
                    }
                }
            }
            FileOutputStream fileOutputStream2 = null;
            if (writeQueueItemRemove instanceof ImageWriteQueueItem) {
                ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) writeQueueItemRemove;
                String str = imageWriteQueueItem.mFilePath;
                if (!TaskPersister.createParentDirectory(str)) {
                    Slog.e(TaskPersister.TAG, "Error while creating images directory for file: " + str);
                    return;
                }
                Bitmap bitmap = imageWriteQueueItem.mImage;
                try {
                    try {
                        fileOutputStream = new FileOutputStream(new File(str));
                    } catch (Exception e3) {
                        e = e3;
                    }
                } catch (Throwable th) {
                    th = th;
                }
                try {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                    IoUtils.closeQuietly(fileOutputStream);
                    return;
                } catch (Exception e4) {
                    e = e4;
                    fileOutputStream2 = fileOutputStream;
                    Slog.e(TaskPersister.TAG, "saveImage: unable to save " + str, e);
                    IoUtils.closeQuietly(fileOutputStream2);
                    return;
                } catch (Throwable th2) {
                    th = th2;
                    fileOutputStream2 = fileOutputStream;
                    IoUtils.closeQuietly(fileOutputStream2);
                    throw th;
                }
            }
            if (writeQueueItemRemove instanceof TaskWriteQueueItem) {
                TaskRecord taskRecord = ((TaskWriteQueueItem) writeQueueItemRemove).mTask;
                synchronized (TaskPersister.this.mService) {
                    try {
                        ActivityManagerService.boostPriorityForLockedSection();
                        if (taskRecord.inRecents) {
                            try {
                                stringWriterSaveToXml = TaskPersister.this.saveToXml(taskRecord);
                            } catch (IOException e5) {
                                stringWriterSaveToXml = null;
                            } catch (XmlPullParserException e6) {
                                stringWriterSaveToXml = null;
                            }
                        } else {
                            stringWriterSaveToXml = null;
                        }
                    } catch (Throwable th3) {
                        ActivityManagerService.resetPriorityAfterLockedSection();
                        throw th3;
                    }
                }
                ActivityManagerService.resetPriorityAfterLockedSection();
                if (stringWriterSaveToXml != null) {
                    try {
                        atomicFile = new AtomicFile(new File(TaskPersister.getUserTasksDir(taskRecord.userId), String.valueOf(taskRecord.taskId) + TaskPersister.TASK_FILENAME_SUFFIX));
                        try {
                            fileOutputStreamStartWrite = atomicFile.startWrite();
                        } catch (IOException e7) {
                            e = e7;
                        }
                    } catch (IOException e8) {
                        e = e8;
                        atomicFile = null;
                    }
                    try {
                        fileOutputStreamStartWrite.write(stringWriterSaveToXml.toString().getBytes());
                        fileOutputStreamStartWrite.write(10);
                        atomicFile.finishWrite(fileOutputStreamStartWrite);
                    } catch (IOException e9) {
                        fileOutputStream2 = fileOutputStreamStartWrite;
                        e = e9;
                        if (fileOutputStream2 != null) {
                            atomicFile.failWrite(fileOutputStream2);
                        }
                        Slog.e(TaskPersister.TAG, "Unable to open " + atomicFile + " for persisting. " + e);
                    }
                }
            }
        }
    }
}
