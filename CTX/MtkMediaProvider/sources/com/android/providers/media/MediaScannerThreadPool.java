package com.android.providers.media;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaFile;
import android.media.MediaScanner;
import android.net.Uri;
import android.os.Handler;
import android.os.Process;
import android.util.Log;
import com.mediatek.media.mediascanner.MediaScannerExImpl;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MediaScannerThreadPool extends ThreadPoolExecutor {
    private final PriorityBlockingQueue<String> mAudioVideoQueue;
    private final Context mContext;
    private final String[] mDirectories;
    private boolean mHasExecutedAllTask;
    private final PriorityBlockingQueue<String> mImageQueue;
    private final Handler mInsertHanlder;
    private final PriorityBlockingQueue<String> mNormalFileQueue;
    private CountDownLatch mParseLatch;
    private final ArrayList<String> mPlaylistFilePathList;
    private final Handler mServiceHandler;
    private final Vector<String> mSingleFileList;
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;
    private static final PriorityBlockingQueue<Runnable> sWorkQueue = new PriorityBlockingQueue<>(64, getTaskComparator());
    private static final AtomicInteger sCount = new AtomicInteger(1);
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "Scan-thread#" + MediaScannerThreadPool.sCount.getAndIncrement());
        }
    };
    private static final ConcurrentHashMap<String, FolderStructure> mFolderMap = new ConcurrentHashMap<>(128);

    public MediaScannerThreadPool(Context context, String[] strArr, Handler handler, Handler handler2) {
        super(3, 3, 10L, KEEP_ALIVE_TIME_UNIT, sWorkQueue, sThreadFactory);
        this.mPlaylistFilePathList = new ArrayList<>();
        this.mAudioVideoQueue = new PriorityBlockingQueue<>(64, getQueueComparator());
        this.mImageQueue = new PriorityBlockingQueue<>(64, getQueueComparator());
        this.mNormalFileQueue = new PriorityBlockingQueue<>(64, getQueueComparator());
        this.mSingleFileList = new Vector<>(64);
        this.mHasExecutedAllTask = false;
        this.mContext = context;
        this.mDirectories = strArr;
        this.mServiceHandler = handler;
        this.mInsertHanlder = handler2;
        sCount.set(1);
    }

    @Override
    protected void afterExecute(Runnable runnable, Throwable th) {
        super.afterExecute(runnable, th);
        if ((runnable instanceof SingleTypeScanTask) && !this.mHasExecutedAllTask) {
            if (this.mAudioVideoQueue.isEmpty() || this.mImageQueue.isEmpty()) {
                if (MediaUtils.LOG_SCAN) {
                    Log.v("MediaScannerThreadPool", "Audio/video or image singleScanTask has finish, execute all tasks to threadpool");
                }
                executeAllTask();
            }
        }
    }

    @Override
    protected void terminated() {
        if (MediaUtils.LOG_SCAN) {
            Log.v("MediaScannerThreadPool", "All task(" + getTaskCount() + ") scan finish, send message to insert all to database.");
        }
        this.mInsertHanlder.sendEmptyMessage(2);
        super.terminated();
    }

    private static Comparator<Runnable> getTaskComparator() {
        return new Comparator<Runnable>() {
            @Override
            public int compare(Runnable runnable, Runnable runnable2) {
                if (((Task) runnable2).mPriority > ((Task) runnable).mPriority) {
                    return 1;
                }
                return -1;
            }
        };
    }

    private Comparator<String> getQueueComparator() {
        return new Comparator<String>() {
            @Override
            public int compare(String str, String str2) {
                if (str != null && str2 != null) {
                    FolderStructure folderStructure = (FolderStructure) MediaScannerThreadPool.mFolderMap.get(str);
                    FolderStructure folderStructure2 = (FolderStructure) MediaScannerThreadPool.mFolderMap.get(str2);
                    int totalSize = MediaScannerThreadPool.this.getTotalSize(folderStructure);
                    int totalSize2 = MediaScannerThreadPool.this.getTotalSize(folderStructure2);
                    int maxScanTaskSizeByType = MediaScannerThreadPool.this.getMaxScanTaskSizeByType(MediaScannerThreadPool.this.getFolderFileType(folderStructure));
                    if (totalSize2 < maxScanTaskSizeByType && totalSize < maxScanTaskSizeByType) {
                        return MediaScannerThreadPool.this.getFolderSerialNum(folderStructure2) > MediaScannerThreadPool.this.getFolderSerialNum(folderStructure) ? 1 : -1;
                    }
                    if (totalSize2 == totalSize) {
                        return -1;
                    }
                    return totalSize2 - totalSize;
                }
                return -1;
            }
        };
    }

    public ArrayList<String> getPlaylistFilePaths() {
        return this.mPlaylistFilePathList;
    }

    public synchronized void stopScan() {
        if (MediaUtils.LOG_SCAN) {
            Log.w("MediaScannerThreadPool", "stopScan in threadpool, clear work queue, stop insert and mark all task executed");
        }
        this.mHasExecutedAllTask = true;
        this.mInsertHanlder.sendEmptyMessage(3);
        sWorkQueue.clear();
        this.mAudioVideoQueue.clear();
        this.mImageQueue.clear();
        this.mNormalFileQueue.clear();
        this.mSingleFileList.clear();
        shutdownNow();
        if (this.mParseLatch != null) {
            while (this.mParseLatch.getCount() > 0) {
                this.mParseLatch.countDown();
            }
        }
    }

    public static void updateFolderMap() {
        if (MediaUtils.LOG_SCAN) {
            Log.v("MediaScannerThreadPool", "updateFolderMap: clear folder map");
        }
        mFolderMap.clear();
    }

    public void parseScanTask() {
        int i;
        String[] strArr;
        ArrayList arrayList = new ArrayList();
        ArrayList arrayList2 = new ArrayList();
        if (this.mDirectories != null && this.mDirectories.length > 0) {
            String[] strArr2 = this.mDirectories;
            int length = strArr2.length;
            int i2 = 0;
            while (i2 < length) {
                File[] fileArrListFiles = new File(strArr2[i2]).listFiles();
                if (fileArrListFiles != null) {
                    int length2 = fileArrListFiles.length;
                    int i3 = 0;
                    while (i3 < length2) {
                        File file = fileArrListFiles[i3];
                        if (file.isDirectory()) {
                            String[] list = file.list();
                            if (list == null || list.length <= 0) {
                                strArr = strArr2;
                                i = i3;
                                mFolderMap.put(file.getPath(), new FolderStructure(new int[]{0, 0, 0, 0, 2, 0}, file.lastModified() / 1000, false, file.getPath()));
                            } else {
                                arrayList2.add(file.getPath());
                                i = i3;
                                strArr = strArr2;
                            }
                        } else {
                            i = i3;
                            strArr = strArr2;
                            arrayList.add(file.getPath());
                        }
                        i3 = i + 1;
                        strArr2 = strArr;
                    }
                }
                i2++;
                strArr2 = strArr2;
            }
        }
        executeParseTask(arrayList2, arrayList);
        try {
            synchronized (this) {
                this.mParseLatch.await(3L, TimeUnit.MINUTES);
            }
        } catch (InterruptedException e) {
            Log.w("MediaScannerThreadPool", "parseScanTask with InterruptedException", e);
        }
    }

    private synchronized void executeParseTask(List<String> list, List<String> list2) {
        if (this.mHasExecutedAllTask) {
            this.mParseLatch = new CountDownLatch(0);
            Log.w("MediaScannerThreadPool", "executeParseTask with all task have been executed, it may happen when stopScan");
            return;
        }
        this.mParseLatch = new CountDownLatch(list.size());
        if (!list.isEmpty()) {
            Iterator<String> it = list.iterator();
            int i = 1;
            while (it.hasNext()) {
                execute(new ParseTask(it.next(), "external", Integer.MAX_VALUE, i));
                i++;
            }
        }
        execute(new InsertFolderTask());
        if (!list2.isEmpty()) {
            String string = list2.toString();
            execute(new ScanTask("singlefile_" + string.substring(1, string.length() - 1), "external", 2147482647, list2.size()));
        }
    }

    private abstract class Task implements Runnable {
        final int mPriority;
        final int mSize;

        public Task(int i, int i2) {
            this.mPriority = i;
            this.mSize = i2;
        }
    }

    private class ParseTask extends Task {
        private int mCheckInterval;
        private final String mPath;
        private int mQuitParseLayerSize;
        private final int mSerialNum;
        private final String mVolume;

        public ParseTask(String str, String str2, int i, int i2) {
            super(i, 1);
            this.mCheckInterval = 100;
            this.mQuitParseLayerSize = 10;
            this.mPath = str;
            this.mVolume = str2;
            this.mSerialNum = i2;
        }

        @Override
        public void run() {
            Process.setThreadPriority(11);
            long jCurrentTimeMillis = System.currentTimeMillis();
            ArrayList arrayList = new ArrayList();
            parseFolder(this.mPath, arrayList);
            MediaScannerThreadPool.this.mSingleFileList.addAll(arrayList);
            MediaScannerThreadPool.this.mParseLatch.countDown();
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            if (MediaUtils.LOG_SCAN) {
                Log.v("MediaScannerThreadPool", "parse finsih in " + Thread.currentThread().getName() + ": folder = " + this.mPath + ", cost = " + (jCurrentTimeMillis2 - jCurrentTimeMillis) + "ms");
            }
        }

        private void parseFolder(String str, List<String> list) {
            if (MediaScannerThreadPool.this.mHasExecutedAllTask) {
                Log.w("MediaScannerThreadPool", "parseFolder with all task have been executed, it may happen stop scan");
                return;
            }
            FolderStructure folderStructure = getFolderStructure(str);
            if (MediaScannerThreadPool.this.isNeedParse(folderStructure)) {
                for (String str2 : MediaScannerThreadPool.this.getSubFileList(folderStructure, str)) {
                    File file = new File(str, str2);
                    if (file.isDirectory()) {
                        parseFolder(file.getPath(), list);
                    } else {
                        list.add(file.getPath());
                    }
                }
                return;
            }
            if (!MediaScannerThreadPool.this.isEmptyFolder(folderStructure)) {
                MediaScannerThreadPool.this.addToQueueByType(str, MediaScannerThreadPool.this.getFolderFileType(folderStructure));
            } else {
                list.add(str);
            }
        }

        private FolderStructure getFolderStructure(String str) {
            boolean z;
            int i;
            int length;
            int i2;
            int folderSize;
            int i3;
            int i4;
            if (str == null) {
                return null;
            }
            FolderStructure folderStructure = (FolderStructure) MediaScannerThreadPool.mFolderMap.get(str);
            if (folderStructure != null) {
                return folderStructure;
            }
            boolean zIsNoMediaPath = MediaScanner.isNoMediaPath(str + "/");
            int fileTypeByName = zIsNoMediaPath ? 1 : 2;
            int layerSizeByPath = MediaScannerThreadPool.this.getLayerSizeByPath(str, this.mPath);
            File file = new File(str);
            String[] list = file.list();
            if (list != null) {
                length = list.length;
                int i5 = 0 + length;
                if (length < 100 && layerSizeByPath < this.mQuitParseLayerSize) {
                    int length2 = list.length;
                    int i6 = 0;
                    int i7 = 0;
                    folderSize = 0;
                    int i8 = i5;
                    int fileTypeByName2 = fileTypeByName;
                    int i9 = 0;
                    i2 = 0;
                    while (true) {
                        if (i9 < length2) {
                            String str2 = list[i9];
                            File file2 = new File(str, str2);
                            if (file2.isDirectory()) {
                                i2++;
                                int i10 = folderSize + 1;
                                FolderStructure folderStructure2 = getFolderStructure(file2.getPath());
                                int totalSize = MediaScannerThreadPool.this.getTotalSize(folderStructure2);
                                i8 += totalSize;
                                if (totalSize <= i6) {
                                    if (totalSize <= i7) {
                                        totalSize = i7;
                                    }
                                } else {
                                    totalSize = i6;
                                    i6 = totalSize;
                                }
                                int folderFileType = MediaScannerThreadPool.this.getFolderFileType(folderStructure2) | fileTypeByName2;
                                folderSize = i10 + MediaScannerThreadPool.this.getFolderSize(folderStructure2);
                                fileTypeByName2 = folderFileType;
                                i7 = totalSize;
                            } else if (!zIsNoMediaPath && !MediaScannerThreadPool.this.isMultiMediaPath(fileTypeByName2)) {
                                fileTypeByName2 = MediaScannerThreadPool.this.getFileTypeByName(str2) | fileTypeByName2;
                            }
                            if (folderSize > this.mCheckInterval) {
                                this.mQuitParseLayerSize = 7 - ((this.mCheckInterval * 2) / 100);
                                StringBuilder sb = new StringBuilder();
                                i4 = i6;
                                sb.append("Parse folder num over limit(");
                                sb.append(this.mCheckInterval);
                                sb.append("), so set mQuitParseLayerSize to ");
                                sb.append(this.mQuitParseLayerSize);
                                sb.append(" in ");
                                sb.append(str);
                                Log.d("MediaScannerThreadPool", sb.toString());
                                this.mCheckInterval += 100;
                            } else {
                                i4 = i6;
                            }
                            if (layerSizeByPath <= this.mQuitParseLayerSize) {
                                i9++;
                                i6 = i4;
                            } else {
                                fileTypeByName = fileTypeByName2;
                                i = i8;
                                i6 = i4;
                                break;
                            }
                        } else {
                            fileTypeByName = fileTypeByName2;
                            i = i8;
                            break;
                        }
                    }
                    if (!MediaScannerThreadPool.this.isMultiMediaPath(fileTypeByName)) {
                        i3 = 2000;
                    } else {
                        i3 = 100;
                    }
                    z = i > i3 && length - i2 < 100 && (i7 > (i * 10) / 100 || i6 < (i * 80) / 100);
                    FolderStructure folderStructure3 = MediaScannerThreadPool.this.new FolderStructure(new int[]{i, folderSize, length, i2, fileTypeByName, this.mSerialNum}, file.lastModified() / 1000, z, this.mPath);
                    if (!z) {
                        list = null;
                    }
                    folderStructure3.setSubFileList(list);
                    MediaScannerThreadPool.mFolderMap.put(str, folderStructure3);
                    return folderStructure3;
                }
                for (String str3 : list) {
                    if (zIsNoMediaPath || MediaScannerThreadPool.this.isMultiMediaPath(fileTypeByName)) {
                        break;
                    }
                    fileTypeByName = MediaScannerThreadPool.this.getFileTypeByName(str3);
                }
                i = i5;
                z = false;
            } else {
                z = false;
                i = 0;
                length = 0;
            }
            i2 = 0;
            folderSize = 0;
            FolderStructure folderStructure32 = MediaScannerThreadPool.this.new FolderStructure(new int[]{i, folderSize, length, i2, fileTypeByName, this.mSerialNum}, file.lastModified() / 1000, z, this.mPath);
            if (!z) {
            }
            folderStructure32.setSubFileList(list);
            MediaScannerThreadPool.mFolderMap.put(str, folderStructure32);
            return folderStructure32;
        }
    }

    private int getLayerSizeByPath(String str, String str2) {
        if (str2 != null) {
            str = str.substring(str2.lastIndexOf("/"));
        }
        String[] strArrSplit = null;
        if (str != null) {
            strArrSplit = str.split("/");
        }
        if (strArrSplit != null) {
            return strArrSplit.length;
        }
        return 0;
    }

    private void addToQueueByType(String str, int i) {
        if ((i & 20) > 0) {
            this.mAudioVideoQueue.add(str);
        } else if ((i & 8) > 0) {
            this.mImageQueue.add(str);
        } else {
            this.mNormalFileQueue.add(str);
        }
    }

    private synchronized void executeSingleTypeScanTask() {
        if (this.mHasExecutedAllTask) {
            Log.w("MediaScannerThreadPool", "executeSingleTypeScanTask with alltask have been executed, it may happen when stopScan");
            return;
        }
        execute(new SingleTypeScanTask(this.mAudioVideoQueue, 41000));
        execute(new SingleTypeScanTask(this.mImageQueue, 40000));
        execute(new SingleTypeScanTask(this.mNormalFileQueue, 39000));
    }

    private synchronized void executeAllTask() {
        if (this.mHasExecutedAllTask) {
            Log.w("MediaScannerThreadPool", "executeAllTask with all task have been executed, it may happen when stopScan");
            return;
        }
        this.mHasExecutedAllTask = true;
        int i = 1;
        while (!this.mAudioVideoQueue.isEmpty()) {
            String strPoll = this.mAudioVideoQueue.poll();
            if (strPoll != null) {
                execute(new ScanTask(strPoll, "external", 30000 - i, getTotalSize(mFolderMap.get(strPoll))));
                i++;
            }
        }
        int i2 = 1;
        while (!this.mImageQueue.isEmpty()) {
            String strPoll2 = this.mImageQueue.poll();
            if (strPoll2 != null) {
                execute(new ScanTask(strPoll2, "external", 30000 - i2, getTotalSize(mFolderMap.get(strPoll2))));
                i2++;
            }
        }
        ArrayList arrayList = new ArrayList();
        int i3 = 1;
        int i4 = 0;
        while (!this.mNormalFileQueue.isEmpty()) {
            String strPoll3 = this.mNormalFileQueue.poll();
            if (strPoll3 != null) {
                int totalSize = getTotalSize(mFolderMap.get(strPoll3));
                int i5 = i4 + totalSize;
                arrayList.add(strPoll3);
                if (totalSize < 100 && i5 < 100 && !this.mNormalFileQueue.isEmpty()) {
                    i4 = i5;
                } else {
                    String string = arrayList.toString();
                    execute(new ScanTask(string.substring(1, string.length() - 1), "external", 10000 - i3, i5));
                    arrayList.clear();
                    i4 = 0;
                    i3++;
                }
            }
        }
        int size = this.mSingleFileList.size();
        if (size > 0) {
            String string2 = this.mSingleFileList.toString();
            execute(new ScanTask("singlefile_" + string2.substring(1, string2.length() - 1), "external", 20000, size));
            this.mSingleFileList.clear();
        }
        if (this.mParseLatch.getCount() == 0) {
            this.mServiceHandler.removeMessages(12);
            this.mServiceHandler.sendEmptyMessage(12);
        }
    }

    private int getTotalSize(FolderStructure folderStructure) {
        if (folderStructure != null) {
            return folderStructure.mTotalSize;
        }
        return 0;
    }

    private int getFolderSize(FolderStructure folderStructure) {
        if (folderStructure != null) {
            return folderStructure.mFolderSize;
        }
        return 0;
    }

    private int getSubTotalSize(FolderStructure folderStructure) {
        if (folderStructure != null) {
            return folderStructure.mSubTotalSize;
        }
        return 0;
    }

    private int getFolderFileType(FolderStructure folderStructure) {
        if (folderStructure != null) {
            return folderStructure.mFileType;
        }
        return 0;
    }

    private int getFolderSerialNum(FolderStructure folderStructure) {
        if (folderStructure != null) {
            return folderStructure.mSerialNum;
        }
        return 0;
    }

    private String[] getSubFileList(FolderStructure folderStructure, String str) {
        String[] strArr = folderStructure != null ? folderStructure.mSubFileList : null;
        if (strArr == null) {
            return new File(str).list();
        }
        return strArr;
    }

    private boolean isNeedParse(FolderStructure folderStructure) {
        if (folderStructure != null) {
            return folderStructure.mNeedParse;
        }
        return false;
    }

    private boolean isEmptyFolder(FolderStructure folderStructure) {
        return getSubTotalSize(folderStructure) == 0;
    }

    private boolean isMultiMediaPath(int i) {
        return (i & 28) > 0;
    }

    private int getMaxScanTaskSizeByType(int i) {
        return isMultiMediaPath(i) ? 500 : 2000;
    }

    private int getFileTypeByName(String str) {
        MediaFile.MediaFileType fileType = MediaFile.getFileType(str);
        int i = fileType == null ? 0 : fileType.fileType;
        if (MediaFile.isAudioFileType(i) || MediaFile.isVideoFileType(i)) {
            return 20;
        }
        if (MediaFile.isImageFileType(i)) {
            return 8;
        }
        return 2;
    }

    private class FolderStructure {
        private int mFileType;
        private int mFolderSize;
        private long mLastModified;
        private boolean mNeedParse;
        private String mRootFolderPath;
        private int mSerialNum;
        private String[] mSubFileList = null;
        private int mSubFolderSize;
        private int mSubTotalSize;
        private int mTotalSize;

        public FolderStructure(int[] iArr, long j, boolean z, String str) {
            this.mTotalSize = iArr[0];
            this.mFolderSize = iArr[1];
            this.mSubTotalSize = iArr[2];
            this.mSubFolderSize = iArr[3];
            this.mFileType = iArr[4];
            this.mSerialNum = iArr[5];
            this.mLastModified = j;
            this.mNeedParse = z;
            this.mRootFolderPath = str;
        }

        public void setSubFileList(String[] strArr) {
            this.mSubFileList = strArr;
        }
    }

    private class ScanTask extends Task {
        private final String mPath;
        private final String mVolume;

        public ScanTask(String str, String str2, int i, int i2) {
            super(i, i2);
            this.mPath = str;
            this.mVolume = str2;
        }

        @Override
        public void run() {
            Process.setThreadPriority(11);
            MediaScannerThreadPool.this.scan(this.mPath, this.mVolume, this.mPriority, this.mSize);
        }
    }

    private class SingleTypeScanTask extends Task {
        private PriorityBlockingQueue<String> mQueue;

        public SingleTypeScanTask(PriorityBlockingQueue<String> priorityBlockingQueue, int i) {
            super(i, 0);
            this.mQueue = priorityBlockingQueue;
        }

        @Override
        public void run() {
            long jCurrentTimeMillis = System.currentTimeMillis();
            Process.setThreadPriority(11);
            long count = MediaScannerThreadPool.this.mParseLatch.getCount();
            while (true) {
                if (this.mQueue.isEmpty() && count <= 0) {
                    break;
                }
                if (count > 0 && this.mQueue.isEmpty()) {
                    try {
                        MediaScannerThreadPool.this.mParseLatch.await(1L, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Log.w("MediaScannerThreadPool", "Wait parse finish in SingleTypeScanTask with InterruptedException", e);
                    }
                }
                if (!this.mQueue.isEmpty()) {
                    String strPoll = this.mQueue.poll();
                    if (strPoll != null) {
                        FolderStructure folderStructure = (FolderStructure) MediaScannerThreadPool.mFolderMap.get(strPoll);
                        MediaScannerThreadPool.this.scan(strPoll, "external", this.mPriority - MediaScannerThreadPool.this.getFolderSerialNum(folderStructure), MediaScannerThreadPool.this.getTotalSize(folderStructure));
                    }
                }
                count = MediaScannerThreadPool.this.mParseLatch.getCount();
            }
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            if (MediaUtils.LOG_SCAN) {
                Log.v("MediaScannerThreadPool", "SingleTypeScanTask() finished in " + Thread.currentThread().getName() + " cost " + (jCurrentTimeMillis2 - jCurrentTimeMillis) + "ms");
            }
        }
    }

    private void scan(String str, String str2, int i, int i2) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        MediaScannerExImpl mediaScannerExImpl = new MediaScannerExImpl(this.mContext, str2);
        boolean z = false;
        Throwable th = null;
        try {
            if (str.startsWith("singlefile_")) {
                str = str.substring("singlefile_".length());
                z = true;
            }
            ArrayList arrayListScanFolders = mediaScannerExImpl.scanFolders(this.mInsertHanlder, str.split(", "), str2, z);
            if (!arrayListScanFolders.isEmpty()) {
                synchronized (this.mPlaylistFilePathList) {
                    this.mPlaylistFilePathList.addAll(arrayListScanFolders);
                }
            }
            mediaScannerExImpl.close();
            Log.v("MediaScannerThreadPool", "scan finsih in " + Thread.currentThread().getName() + ": size = " + i2 + ", priority = " + i + ", cost = " + (System.currentTimeMillis() - jCurrentTimeMillis) + "ms (" + str + ")");
        } catch (Throwable th2) {
            if (0 != 0) {
                try {
                    mediaScannerExImpl.close();
                } catch (Throwable th3) {
                    th.addSuppressed(th3);
                }
            } else {
                mediaScannerExImpl.close();
            }
            throw th2;
        }
    }

    private class InsertFolderTask extends Task {
        private ContentResolver mContentResolver;

        public InsertFolderTask() {
            super(2147482647, 0);
            this.mContentResolver = MediaScannerThreadPool.this.mContext.getContentResolver();
        }

        @Override
        public void run() {
            long jCurrentTimeMillis = System.currentTimeMillis();
            ArrayList arrayList = new ArrayList(MediaScannerThreadPool.mFolderMap.size());
            Iterator it = MediaScannerThreadPool.mFolderMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String str = (String) entry.getKey();
                FolderStructure folderStructure = (FolderStructure) entry.getValue();
                if (!isExistInDatabase(str)) {
                    if (!isExistInFileSystem(str)) {
                        it.remove();
                    } else {
                        ContentValues contentValues = new ContentValues();
                        contentValues.put("title", MediaFile.getFileTitle(str));
                        contentValues.put("format", (Integer) 12289);
                        contentValues.put("_data", str);
                        contentValues.put("date_modified", Long.valueOf(folderStructure.mLastModified));
                        contentValues.put("_size", (Integer) 0);
                        contentValues.put("is_drm", (Integer) 0);
                        arrayList.add(contentValues);
                        if (arrayList.size() >= 500) {
                            sortByPath(arrayList);
                            flush(MediaScannerInserter.FILE_URI, arrayList);
                            arrayList.clear();
                        }
                    }
                }
            }
            if (!arrayList.isEmpty()) {
                sortByPath(arrayList);
                flush(MediaScannerInserter.FILE_URI, arrayList);
            }
            MediaScannerThreadPool.this.executeSingleTypeScanTask();
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            if (MediaUtils.LOG_SCAN) {
                Log.v("MediaScannerThreadPool", "Insert all folder entries finsih in " + Thread.currentThread().getName() + ": folder size = " + MediaScannerThreadPool.mFolderMap.size() + ", insert num = " + arrayList.size() + ", cost = " + (jCurrentTimeMillis2 - jCurrentTimeMillis) + "ms");
            }
        }

        private void sortByPath(List<ContentValues> list) {
            Collections.sort(list, new Comparator<ContentValues>() {
                @Override
                public int compare(ContentValues contentValues, ContentValues contentValues2) {
                    String asString = contentValues.getAsString("_data");
                    String asString2 = contentValues2.getAsString("_data");
                    if (asString2 != null && asString != null) {
                        return asString.compareTo(asString2);
                    }
                    return 0;
                }
            });
        }

        private boolean isExistInDatabase(String str) throws Throwable {
            boolean z;
            Cursor cursor = null;
            try {
                try {
                    Cursor cursorQuery = this.mContentResolver.query(MediaScannerInserter.FILE_URI, null, "_data=?", new String[]{str}, null, null);
                    if (cursorQuery != null) {
                        try {
                            z = cursorQuery.moveToFirst();
                        } catch (Exception e) {
                            e = e;
                            cursor = cursorQuery;
                            Log.e("MediaScannerThreadPool", "Check isExistInDatabase with Exception for " + str, e);
                            if (cursor != null) {
                                cursor.close();
                            }
                            return true;
                        } catch (Throwable th) {
                            th = th;
                            cursor = cursorQuery;
                            if (cursor != null) {
                                cursor.close();
                            }
                            throw th;
                        }
                    }
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                    return z;
                } catch (Exception e2) {
                    e = e2;
                }
            } catch (Throwable th2) {
                th = th2;
            }
        }

        private boolean isExistInFileSystem(String str) {
            return new File(str).exists();
        }

        private void flush(Uri uri, List<ContentValues> list) {
            long jCurrentTimeMillis = System.currentTimeMillis();
            int size = list.size();
            try {
                this.mContentResolver.bulkInsert(uri, (ContentValues[]) list.toArray(new ContentValues[size]));
            } catch (Exception e) {
                Log.e("MediaScannerThreadPool", "bulkInsert with Exception for " + uri, e);
            }
            long jCurrentTimeMillis2 = System.currentTimeMillis();
            if (MediaUtils.LOG_SCAN) {
                Log.d("MediaScannerThreadPool", "flush() " + uri + " with size " + size + " which cost " + (jCurrentTimeMillis2 - jCurrentTimeMillis) + "ms");
            }
        }
    }
}
