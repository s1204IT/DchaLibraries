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
    private static final String RECENTS_FILENAME = "_task";
    static final String TAG = "TaskPersister";
    private static final String TAG_TASK = "task";
    private static final String TASKS_DIRNAME = "recent_tasks";
    private static final String TASK_EXTENSION = ".xml";
    private final LazyTaskWriterThread mLazyTaskWriterThread;
    private long mNextWriteTime;
    private final RecentTasks mRecentTasks;
    private final ActivityManagerService mService;
    private final ActivityStackSupervisor mStackSupervisor;
    private final File mTaskIdsDir;
    private final SparseArray<SparseBooleanArray> mTaskIdsInFile;
    ArrayList<WriteQueueItem> mWriteQueue;

    private static class WriteQueueItem {
        WriteQueueItem(WriteQueueItem writeQueueItem) {
            this();
        }

        private WriteQueueItem() {
        }
    }

    private static class TaskWriteQueueItem extends WriteQueueItem {
        final TaskRecord mTask;

        TaskWriteQueueItem(TaskRecord task) {
            super(null);
            this.mTask = task;
        }
    }

    private static class ImageWriteQueueItem extends WriteQueueItem {
        final String mFilePath;
        Bitmap mImage;

        ImageWriteQueueItem(String filePath, Bitmap image) {
            super(null);
            this.mFilePath = filePath;
            this.mImage = image;
        }
    }

    TaskPersister(File systemDir, ActivityStackSupervisor stackSupervisor, ActivityManagerService service, RecentTasks recentTasks) {
        this.mTaskIdsInFile = new SparseArray<>();
        this.mNextWriteTime = 0L;
        this.mWriteQueue = new ArrayList<>();
        File legacyImagesDir = new File(systemDir, IMAGES_DIRNAME);
        if (legacyImagesDir.exists() && (!FileUtils.deleteContents(legacyImagesDir) || !legacyImagesDir.delete())) {
            Slog.i(TAG, "Failure deleting legacy images directory: " + legacyImagesDir);
        }
        File legacyTasksDir = new File(systemDir, TASKS_DIRNAME);
        if (legacyTasksDir.exists() && (!FileUtils.deleteContents(legacyTasksDir) || !legacyTasksDir.delete())) {
            Slog.i(TAG, "Failure deleting legacy tasks directory: " + legacyTasksDir);
        }
        this.mTaskIdsDir = new File(Environment.getDataDirectory(), "system_de");
        this.mStackSupervisor = stackSupervisor;
        this.mService = service;
        this.mRecentTasks = recentTasks;
        this.mLazyTaskWriterThread = new LazyTaskWriterThread("LazyTaskWriterThread");
    }

    TaskPersister(File workingDir) {
        this.mTaskIdsInFile = new SparseArray<>();
        this.mNextWriteTime = 0L;
        this.mWriteQueue = new ArrayList<>();
        this.mTaskIdsDir = workingDir;
        this.mStackSupervisor = null;
        this.mService = null;
        this.mRecentTasks = null;
        this.mLazyTaskWriterThread = new LazyTaskWriterThread("LazyTaskWriterThreadTest");
    }

    void startPersisting() {
        if (this.mLazyTaskWriterThread.isAlive()) {
            return;
        }
        this.mLazyTaskWriterThread.start();
    }

    private void removeThumbnails(TaskRecord task) {
        String taskString = Integer.toString(task.taskId);
        for (int queueNdx = this.mWriteQueue.size() - 1; queueNdx >= 0; queueNdx--) {
            WriteQueueItem item = this.mWriteQueue.get(queueNdx);
            if (item instanceof ImageWriteQueueItem) {
                File thumbnailFile = new File(((ImageWriteQueueItem) item).mFilePath);
                if (thumbnailFile.getName().startsWith(taskString)) {
                    this.mWriteQueue.remove(queueNdx);
                }
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
        if (!stall) {
            return;
        }
        Thread.yield();
    }

    SparseBooleanArray loadPersistedTaskIdsForUser(int userId) throws Throwable {
        if (this.mTaskIdsInFile.get(userId) != null) {
            return this.mTaskIdsInFile.get(userId).clone();
        }
        SparseBooleanArray persistedTaskIds = new SparseBooleanArray();
        BufferedReader reader = null;
        try {
            try {
                BufferedReader reader2 = new BufferedReader(new FileReader(getUserPersistedTaskIdsFile(userId)));
                while (true) {
                    try {
                        String line = reader2.readLine();
                        if (line == null) {
                            break;
                        }
                        for (String taskIdString : line.split("\\s+")) {
                            int id = Integer.parseInt(taskIdString);
                            persistedTaskIds.put(id, true);
                        }
                    } catch (FileNotFoundException e) {
                        reader = reader2;
                        IoUtils.closeQuietly(reader);
                        this.mTaskIdsInFile.put(userId, persistedTaskIds);
                        return persistedTaskIds.clone();
                    } catch (Exception e2) {
                        e = e2;
                        reader = reader2;
                        Slog.e(TAG, "Error while reading taskIds file for user " + userId, e);
                        IoUtils.closeQuietly(reader);
                    } catch (Throwable th) {
                        th = th;
                        reader = reader2;
                        IoUtils.closeQuietly(reader);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(reader2);
                reader = reader2;
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (FileNotFoundException e3) {
        } catch (Exception e4) {
            e = e4;
        }
        this.mTaskIdsInFile.put(userId, persistedTaskIds);
        return persistedTaskIds.clone();
    }

    void maybeWritePersistedTaskIdsForUser(SparseBooleanArray taskIds, int userId) throws Throwable {
        if (userId < 0) {
            return;
        }
        SparseBooleanArray persistedIdsInFile = this.mTaskIdsInFile.get(userId);
        if (persistedIdsInFile != null && persistedIdsInFile.equals(taskIds)) {
            return;
        }
        File persistedTaskIdsFile = getUserPersistedTaskIdsFile(userId);
        BufferedWriter writer = null;
        try {
            try {
                BufferedWriter writer2 = new BufferedWriter(new FileWriter(persistedTaskIdsFile));
                for (int i = 0; i < taskIds.size(); i++) {
                    try {
                        if (taskIds.valueAt(i)) {
                            writer2.write(String.valueOf(taskIds.keyAt(i)));
                            writer2.newLine();
                        }
                    } catch (Exception e) {
                        e = e;
                        writer = writer2;
                        Slog.e(TAG, "Error while writing taskIds file for user " + userId, e);
                        IoUtils.closeQuietly(writer);
                    } catch (Throwable th) {
                        th = th;
                        writer = writer2;
                        IoUtils.closeQuietly(writer);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(writer2);
            } catch (Exception e2) {
                e = e2;
            }
            this.mTaskIdsInFile.put(userId, taskIds.clone());
        } catch (Throwable th2) {
            th = th2;
        }
    }

    void unloadUserDataFromMemory(int userId) {
        this.mTaskIdsInFile.delete(userId);
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
                this.mWriteQueue.add(new WriteQueueItem(null));
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

    void saveImage(Bitmap image, String filePath) {
        synchronized (this) {
            int queueNdx = this.mWriteQueue.size() - 1;
            while (true) {
                if (queueNdx < 0) {
                    break;
                }
                WriteQueueItem item = this.mWriteQueue.get(queueNdx);
                if (item instanceof ImageWriteQueueItem) {
                    ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) item;
                    if (imageWriteQueueItem.mFilePath.equals(filePath)) {
                        imageWriteQueueItem.mImage = image;
                        break;
                    }
                }
                queueNdx--;
            }
            if (queueNdx < 0) {
                this.mWriteQueue.add(new ImageWriteQueueItem(filePath, image));
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

    Bitmap getTaskDescriptionIcon(String filePath) {
        Bitmap icon = getImageFromWriteQueue(filePath);
        if (icon != null) {
            return icon;
        }
        return restoreImage(filePath);
    }

    Bitmap getImageFromWriteQueue(String filePath) {
        synchronized (this) {
            for (int queueNdx = this.mWriteQueue.size() - 1; queueNdx >= 0; queueNdx--) {
                WriteQueueItem item = this.mWriteQueue.get(queueNdx);
                if (item instanceof ImageWriteQueueItem) {
                    ImageWriteQueueItem imageWriteQueueItem = (ImageWriteQueueItem) item;
                    if (imageWriteQueueItem.mFilePath.equals(filePath)) {
                        return imageWriteQueueItem.mImage;
                    }
                }
            }
            return null;
        }
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

    List<TaskRecord> restoreTasksForUserLocked(int userId) throws Throwable {
        BufferedReader reader;
        ArrayList<TaskRecord> tasks = new ArrayList<>();
        ArraySet<Integer> recoveredTaskIds = new ArraySet<>();
        File userTasksDir = getUserTasksDir(userId);
        File[] recentFiles = userTasksDir.listFiles();
        if (recentFiles == null) {
            Slog.e(TAG, "restoreTasksForUserLocked: Unable to list files from " + userTasksDir);
            return tasks;
        }
        for (File taskFile : recentFiles) {
            BufferedReader bufferedReader = null;
            try {
                try {
                    reader = new BufferedReader(new FileReader(taskFile));
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
                            TaskRecord task = TaskRecord.restoreFromXml(in, this.mStackSupervisor);
                            if (task != null) {
                                int taskId = task.taskId;
                                if (this.mStackSupervisor.anyTaskForIdLocked(taskId, false, 0) != null) {
                                    Slog.wtf(TAG, "Existing task with taskId " + taskId + "found");
                                } else if (userId != task.userId) {
                                    Slog.wtf(TAG, "Task with userId " + task.userId + " found in " + userTasksDir.getAbsolutePath());
                                } else {
                                    this.mStackSupervisor.setNextTaskIdForUserLocked(taskId, userId);
                                    task.isPersistable = true;
                                    tasks.add(task);
                                    recoveredTaskIds.add(Integer.valueOf(taskId));
                                }
                            } else {
                                Slog.e(TAG, "restoreTasksForUserLocked: Unable to restore taskFile=" + taskFile + ": " + fileToString(taskFile));
                            }
                        } else {
                            Slog.wtf(TAG, "restoreTasksForUserLocked: Unknown xml event=" + event + " name=" + name);
                        }
                    }
                    XmlUtils.skipCurrentTag(in);
                }
                IoUtils.closeQuietly(reader);
                if (0 != 0) {
                    taskFile.delete();
                }
            } catch (Exception e2) {
                e = e2;
                bufferedReader = reader;
                Slog.wtf(TAG, "Unable to parse " + taskFile + ". Error ", e);
                Slog.e(TAG, "Failing file: " + fileToString(taskFile));
                IoUtils.closeQuietly(bufferedReader);
                if (1 != 0) {
                    taskFile.delete();
                }
            } catch (Throwable th2) {
                th = th2;
                bufferedReader = reader;
                IoUtils.closeQuietly(bufferedReader);
                if (0 != 0) {
                    taskFile.delete();
                }
                throw th;
            }
        }
        removeObsoleteFiles(recoveredTaskIds, userTasksDir.listFiles());
        for (int taskNdx = tasks.size() - 1; taskNdx >= 0; taskNdx--) {
            TaskRecord task2 = tasks.get(taskNdx);
            task2.setPrevAffiliate(taskIdToTask(task2.mPrevAffiliateTaskId, tasks));
            task2.setNextAffiliate(taskIdToTask(task2.mNextAffiliateTaskId, tasks));
        }
        Collections.sort(tasks, new Comparator<TaskRecord>() {
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
        return tasks;
    }

    private static void removeObsoleteFiles(ArraySet<Integer> persistentTaskIds, File[] files) {
        if (files == null) {
            Slog.e(TAG, "File error accessing recents directory (directory doesn't exist?).");
            return;
        }
        for (File file : files) {
            String filename = file.getName();
            int taskIdEnd = filename.indexOf(95);
            if (taskIdEnd > 0) {
                try {
                    int taskId = Integer.parseInt(filename.substring(0, taskIdEnd));
                    if (!persistentTaskIds.contains(Integer.valueOf(taskId))) {
                        file.delete();
                    }
                } catch (Exception e) {
                    Slog.wtf(TAG, "removeObsoleteFiles: Can't parse file=" + file.getName());
                    file.delete();
                }
            }
        }
    }

    private void writeTaskIdsFiles() throws Throwable {
        int[] candidateUserIds;
        SparseBooleanArray taskIdsToSave;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                candidateUserIds = this.mRecentTasks.usersWithRecentsLoadedLocked();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        for (int userId : candidateUserIds) {
            synchronized (this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    taskIdsToSave = this.mRecentTasks.mPersistedTaskIds.get(userId).clone();
                } catch (Throwable th2) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th2;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
            maybeWritePersistedTaskIdsForUser(taskIdsToSave, userId);
        }
    }

    private void removeObsoleteFiles(ArraySet<Integer> persistentTaskIds) {
        int[] candidateUserIds;
        synchronized (this.mService) {
            try {
                ActivityManagerService.boostPriorityForLockedSection();
                candidateUserIds = this.mRecentTasks.usersWithRecentsLoadedLocked();
            } catch (Throwable th) {
                ActivityManagerService.resetPriorityAfterLockedSection();
                throw th;
            }
        }
        ActivityManagerService.resetPriorityAfterLockedSection();
        for (int userId : candidateUserIds) {
            removeObsoleteFiles(persistentTaskIds, getUserImagesDir(userId).listFiles());
            removeObsoleteFiles(persistentTaskIds, getUserTasksDir(userId).listFiles());
        }
    }

    static Bitmap restoreImage(String filename) {
        return BitmapFactory.decodeFile(filename);
    }

    private File getUserPersistedTaskIdsFile(int userId) {
        File userTaskIdsDir = new File(this.mTaskIdsDir, String.valueOf(userId));
        if (!userTaskIdsDir.exists() && !userTaskIdsDir.mkdirs()) {
            Slog.e(TAG, "Error while creating user directory: " + userTaskIdsDir);
        }
        return new File(userTaskIdsDir, PERSISTED_TASK_IDS_FILENAME);
    }

    static File getUserTasksDir(int userId) {
        File userTasksDir = new File(Environment.getDataSystemCeDirectory(userId), TASKS_DIRNAME);
        if (!userTasksDir.exists() && !userTasksDir.mkdir()) {
            Slog.e(TAG, "Failure creating tasks directory for user " + userId + ": " + userTasksDir);
        }
        return userTasksDir;
    }

    static File getUserImagesDir(int userId) {
        return new File(Environment.getDataSystemCeDirectory(userId), IMAGES_DIRNAME);
    }

    private static boolean createParentDirectory(String filePath) {
        File parentDir = new File(filePath).getParentFile();
        if (parentDir.exists()) {
            return true;
        }
        return parentDir.mkdirs();
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
            Process.setThreadPriority(10);
            ArraySet<Integer> persistentTaskIds = new ArraySet<>();
            while (true) {
                synchronized (TaskPersister.this) {
                    probablyDone = TaskPersister.this.mWriteQueue.isEmpty();
                }
                if (probablyDone) {
                    persistentTaskIds.clear();
                    synchronized (TaskPersister.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            for (int taskNdx = TaskPersister.this.mRecentTasks.size() - 1; taskNdx >= 0; taskNdx--) {
                                TaskRecord task = TaskPersister.this.mRecentTasks.get(taskNdx);
                                if ((task.isPersistable || task.inRecents) && (task.stack == null || !task.stack.isHomeStack())) {
                                    persistentTaskIds.add(Integer.valueOf(task.taskId));
                                }
                            }
                        } catch (Throwable th) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    TaskPersister.this.removeObsoleteFiles(persistentTaskIds);
                }
                TaskPersister.this.writeTaskIdsFiles();
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
                    String filePath = imageWriteQueueItem.mFilePath;
                    if (!TaskPersister.createParentDirectory(filePath)) {
                        Slog.e(TaskPersister.TAG, "Error while creating images directory for file: " + filePath);
                    } else {
                        Bitmap bitmap = imageWriteQueueItem.mImage;
                        FileOutputStream imageFile2 = null;
                        try {
                            try {
                                imageFile = new FileOutputStream(new File(filePath));
                            } catch (Throwable th2) {
                                th = th2;
                            }
                        } catch (Exception e3) {
                            e = e3;
                        }
                        try {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageFile);
                            IoUtils.closeQuietly(imageFile);
                        } catch (Exception e4) {
                            e = e4;
                            imageFile2 = imageFile;
                            Slog.e(TaskPersister.TAG, "saveImage: unable to save " + filePath, e);
                            IoUtils.closeQuietly(imageFile2);
                        } catch (Throwable th3) {
                            th = th3;
                            imageFile2 = imageFile;
                            IoUtils.closeQuietly(imageFile2);
                            throw th;
                        }
                    }
                } else if (item instanceof TaskWriteQueueItem) {
                    StringWriter stringWriter = null;
                    TaskRecord task2 = ((TaskWriteQueueItem) item).mTask;
                    synchronized (TaskPersister.this.mService) {
                        try {
                            ActivityManagerService.boostPriorityForLockedSection();
                            if (task2.inRecents) {
                                try {
                                    stringWriter = TaskPersister.this.saveToXml(task2);
                                } catch (IOException e5) {
                                } catch (XmlPullParserException e6) {
                                }
                            }
                        } catch (Throwable th4) {
                            ActivityManagerService.resetPriorityAfterLockedSection();
                            throw th4;
                        }
                    }
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    if (stringWriter != null) {
                        FileOutputStream file = null;
                        AtomicFile atomicFile = null;
                        try {
                            AtomicFile atomicFile2 = new AtomicFile(new File(TaskPersister.getUserTasksDir(task2.userId), String.valueOf(task2.taskId) + TaskPersister.RECENTS_FILENAME + TaskPersister.TASK_EXTENSION));
                            try {
                                file = atomicFile2.startWrite();
                                file.write(stringWriter.toString().getBytes());
                                file.write(10);
                                atomicFile2.finishWrite(file);
                            } catch (IOException e7) {
                                e = e7;
                                atomicFile = atomicFile2;
                                if (file != null) {
                                    atomicFile.failWrite(file);
                                }
                                Slog.e(TaskPersister.TAG, "Unable to open " + atomicFile + " for persisting. " + e);
                            }
                        } catch (IOException e8) {
                            e = e8;
                        }
                    }
                } else {
                    continue;
                }
            }
        }
    }
}
