package com.android.gallery3d.ui;

import android.os.Handler;
import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import com.android.gallery3d.util.Future;
import com.android.gallery3d.util.FutureListener;
import com.android.gallery3d.util.ThreadPool;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import mf.org.apache.xerces.dom3.as.ASContentModel;

public class SelectionManager {
    public static final int DESELECT_ALL_MODE = 4;
    public static final int ENTER_SELECTION_MODE = 1;
    public static final int LEAVE_SELECTION_MODE = 2;
    public static final Object LOCK = new Object();
    public static final int SELECT_ALL_MODE = 3;
    private static final String TAG = "Gallery2/SelectionManager";
    private AbstractGalleryActivity mActivity;
    private DataManager mDataManager;
    private boolean mInSelectionMode;
    private boolean mInverseSelection;
    private boolean mIsAlbumSet;
    private SelectionListener mListener;
    private final Handler mMainHandler;
    ArrayList<Path> mPrepared;
    private Future<?> mRestoreSelectionTask;
    private Future<?> mSaveSelectionTask;
    private MediaSet mSourceMediaSet;
    private boolean mAutoLeave = true;
    private ArrayList<Path> mSelectionPath = null;
    private ArrayList<Long> mSelectionGroupId = null;
    private Set<Path> mClickedSet = new HashSet();
    private int mTotal = -1;

    public interface SelectionListener {
        void onSelectionChange(Path path, boolean z);

        void onSelectionModeChange(int i);

        void onSelectionRestoreDone();
    }

    public SelectionManager(AbstractGalleryActivity abstractGalleryActivity, boolean z) {
        this.mActivity = null;
        this.mActivity = abstractGalleryActivity;
        this.mMainHandler = new Handler(abstractGalleryActivity.getMainLooper());
        this.mDataManager = abstractGalleryActivity.getDataManager();
        this.mIsAlbumSet = z;
    }

    public void setAutoLeaveSelectionMode(boolean z) {
        this.mAutoLeave = z;
    }

    public void setSelectionListener(SelectionListener selectionListener) {
        this.mListener = selectionListener;
    }

    public void selectAll() {
        Log.d(TAG, "<selectAll>");
        this.mInverseSelection = true;
        this.mClickedSet.clear();
        this.mTotal = -1;
        enterSelectionMode();
        if (this.mListener != null) {
            this.mListener.onSelectionModeChange(3);
        }
    }

    public void deSelectAll() {
        Log.d(TAG, "<deSelectAll>");
        this.mInverseSelection = false;
        this.mClickedSet.clear();
        if (this.mListener != null) {
            this.mListener.onSelectionModeChange(4);
        }
    }

    public boolean inSelectAllMode() {
        if (getTotalCount() != 0) {
            return getTotalCount() == getSelectedCount();
        }
        return this.mInverseSelection;
    }

    public boolean inSelectionMode() {
        return this.mInSelectionMode;
    }

    public void enterSelectionMode() {
        if (this.mInSelectionMode) {
            return;
        }
        Log.d(TAG, "<enterSelectionMode>");
        this.mInSelectionMode = true;
        if (this.mListener != null) {
            this.mListener.onSelectionModeChange(1);
        }
    }

    public void leaveSelectionMode() {
        if (this.mInSelectionMode) {
            Log.d(TAG, "<leaveSelectionMode>");
            this.mInSelectionMode = false;
            this.mInverseSelection = false;
            this.mClickedSet.clear();
            this.mTotal = -1;
            if (this.mRestoreSelectionTask != null) {
                this.mRestoreSelectionTask.cancel();
            }
            if (this.mListener != null) {
                this.mListener.onSelectionModeChange(2);
            }
        }
    }

    public boolean isItemSelected(Path path) {
        return this.mClickedSet.contains(path) ^ this.mInverseSelection;
    }

    private int getTotalCount() {
        int mediaItemCount;
        if (this.mSourceMediaSet == null) {
            return -1;
        }
        if (this.mTotal < 0) {
            if (this.mIsAlbumSet) {
                mediaItemCount = this.mSourceMediaSet.getSubMediaSetCount();
            } else {
                mediaItemCount = this.mSourceMediaSet.getMediaItemCount();
            }
            this.mTotal = mediaItemCount;
        }
        return this.mTotal;
    }

    public int getSelectedCount() {
        int size = this.mClickedSet.size();
        if (this.mInverseSelection) {
            return getTotalCount() - size;
        }
        return size;
    }

    public void toggle(Path path) {
        Log.d(TAG, "<toggle> path = " + path);
        if (this.mClickedSet.contains(path)) {
            this.mClickedSet.remove(path);
        } else {
            enterSelectionMode();
            this.mClickedSet.add(path);
        }
        int selectedCount = getSelectedCount();
        if (selectedCount == getTotalCount()) {
            selectAll();
        }
        if (this.mListener != null) {
            this.mListener.onSelectionChange(path, isItemSelected(path));
        }
        if (selectedCount == 0 && this.mAutoLeave) {
            leaveSelectionMode();
        }
    }

    private static boolean expandMediaSet(ArrayList<Path> arrayList, MediaSet mediaSet, int i) {
        int i2;
        int subMediaSetCount = mediaSet.getSubMediaSetCount();
        for (int i3 = 0; i3 < subMediaSetCount; i3++) {
            if (!expandMediaSet(arrayList, mediaSet.getSubMediaSet(i3), i)) {
                return false;
            }
        }
        int mediaItemCount = mediaSet.getMediaItemCount();
        int i4 = 0;
        while (i4 < mediaItemCount) {
            int i5 = i4 + 50;
            if (i5 >= mediaItemCount) {
                i2 = mediaItemCount - i4;
            } else {
                i2 = 50;
            }
            ArrayList<MediaItem> mediaItem = mediaSet.getMediaItem(i4, i2);
            if (mediaItem != null && mediaItem.size() > i - arrayList.size()) {
                return false;
            }
            Iterator<MediaItem> it = mediaItem.iterator();
            while (it.hasNext()) {
                arrayList.add(it.next().getPath());
            }
            i4 = i5;
        }
        return true;
    }

    public ArrayList<Path> getSelected(boolean z) {
        return getSelected(z, ASContentModel.AS_UNBOUNDED);
    }

    public ArrayList<Path> getSelected(boolean z, final int i) {
        final ArrayList<Path> arrayList = new ArrayList<>();
        int i2 = 0;
        if (this.mIsAlbumSet) {
            if (this.mInverseSelection) {
                int totalCount = getTotalCount();
                while (i2 < totalCount) {
                    MediaSet subMediaSet = this.mSourceMediaSet.getSubMediaSet(i2);
                    if (subMediaSet != null) {
                        Path path = subMediaSet.getPath();
                        if (this.mClickedSet.contains(path)) {
                            continue;
                        } else if (z) {
                            if (!expandMediaSet(arrayList, subMediaSet, i)) {
                                return null;
                            }
                        } else {
                            arrayList.add(path);
                            if (arrayList.size() > i) {
                                return null;
                            }
                        }
                    }
                    i2++;
                }
            } else {
                for (Path path2 : this.mClickedSet) {
                    if (z) {
                        if (!expandMediaSet(arrayList, this.mDataManager.getMediaSet(path2), i)) {
                            return null;
                        }
                    } else {
                        arrayList.add(path2);
                        if (arrayList.size() > i) {
                            return null;
                        }
                    }
                }
            }
        } else if (this.mInverseSelection) {
            int totalCount2 = getTotalCount();
            while (i2 < totalCount2) {
                int iMin = Math.min(totalCount2 - i2, 500);
                Iterator<MediaItem> it = this.mSourceMediaSet.getMediaItem(i2, iMin).iterator();
                while (it.hasNext()) {
                    Path path3 = it.next().getPath();
                    if (!this.mClickedSet.contains(path3)) {
                        arrayList.add(path3);
                        if (arrayList.size() > i) {
                            return null;
                        }
                    }
                }
                i2 += iMin;
            }
        } else {
            ArrayList<Path> arrayList2 = new ArrayList<>();
            arrayList2.addAll(this.mClickedSet);
            this.mDataManager.mapMediaItems(arrayList2, new MediaSet.ItemConsumer() {
                @Override
                public void consume(int i3, MediaItem mediaItem) {
                    if (arrayList.size() < i && mediaItem != null) {
                        arrayList.add(mediaItem.getPath());
                    }
                }

                @Override
                public boolean stopConsume() {
                    return false;
                }
            }, 0);
        }
        return arrayList;
    }

    public void setSourceMediaSet(MediaSet mediaSet) {
        this.mSourceMediaSet = mediaSet;
        this.mTotal = -1;
    }

    public ArrayList<Path> getPrepared() {
        return this.mPrepared;
    }

    public void setPrepared(ArrayList<Path> arrayList) {
        this.mPrepared = arrayList;
    }

    public boolean contains(Path path) {
        if (inSelectAllMode()) {
            return true;
        }
        return this.mClickedSet.contains(path);
    }

    public void onSourceContentChanged() {
        this.mTotal = -1;
        int totalCount = getTotalCount();
        Log.d(TAG, "<onSourceContentChanged> New total=" + totalCount);
        if (totalCount == 0) {
            leaveSelectionMode();
        }
    }

    public ArrayList<Path> getSelected(ThreadPool.JobContext jobContext, boolean z, final int i) {
        final ArrayList<Path> arrayList = new ArrayList<>();
        int i2 = 0;
        if (this.mIsAlbumSet) {
            if (this.mInverseSelection) {
                int totalCount = getTotalCount();
                while (i2 < totalCount) {
                    if (jobContext.isCancelled()) {
                        Log.d(TAG, "<getSelected> jc.isCancelled() - 1");
                        return null;
                    }
                    MediaSet subMediaSet = this.mSourceMediaSet.getSubMediaSet(i2);
                    if (subMediaSet != null) {
                        Path path = subMediaSet.getPath();
                        if (this.mClickedSet.contains(path)) {
                            continue;
                        } else if (z) {
                            if (!expandMediaSet(jobContext, arrayList, subMediaSet, i)) {
                                return null;
                            }
                        } else {
                            arrayList.add(path);
                            if (arrayList.size() > i) {
                                return null;
                            }
                        }
                    }
                    i2++;
                }
            } else {
                for (Path path2 : this.mClickedSet) {
                    if (jobContext.isCancelled()) {
                        Log.d(TAG, "<getSelected> jc.isCancelled() - 2");
                        return null;
                    }
                    if (z) {
                        if (!expandMediaSet(jobContext, arrayList, this.mDataManager.getMediaSet(path2), i)) {
                            return null;
                        }
                    } else {
                        arrayList.add(path2);
                        if (arrayList.size() > i) {
                            return null;
                        }
                    }
                }
            }
        } else if (this.mInverseSelection) {
            int totalCount2 = getTotalCount();
            while (i2 < totalCount2) {
                int iMin = Math.min(totalCount2 - i2, 500);
                for (MediaItem mediaItem : this.mSourceMediaSet.getMediaItem(i2, iMin)) {
                    if (jobContext.isCancelled()) {
                        Log.d(TAG, "<getSelected> jc.isCancelled() - 3");
                        return null;
                    }
                    Path path3 = mediaItem.getPath();
                    if (!this.mClickedSet.contains(path3)) {
                        arrayList.add(path3);
                        if (arrayList.size() > i) {
                            return null;
                        }
                    }
                }
                i2 += iMin;
            }
        } else {
            ArrayList<Path> arrayList2 = new ArrayList<>();
            arrayList2.addAll(this.mClickedSet);
            this.mDataManager.mapMediaItems(arrayList2, new MediaSet.ItemConsumer() {
                @Override
                public void consume(int i3, MediaItem mediaItem2) {
                    if (arrayList.size() < i) {
                        arrayList.add(mediaItem2.getPath());
                    }
                }

                @Override
                public boolean stopConsume() {
                    return false;
                }
            }, 0);
        }
        return arrayList;
    }

    private static boolean expandMediaSet(ThreadPool.JobContext jobContext, ArrayList<Path> arrayList, MediaSet mediaSet, int i) {
        int i2;
        if (jobContext.isCancelled()) {
            Log.d(TAG, "<expandMediaSet> jc.isCancelled() - 1");
            return false;
        }
        if (mediaSet == null) {
            Log.d(TAG, "<expandMediaSet> set == null, return false");
            return false;
        }
        int subMediaSetCount = mediaSet.getSubMediaSetCount();
        for (int i3 = 0; i3 < subMediaSetCount; i3++) {
            if (jobContext.isCancelled()) {
                Log.d(TAG, "<expandMediaSet> jc.isCancelled() - 2");
                return false;
            }
            if (!expandMediaSet(arrayList, mediaSet.getSubMediaSet(i3), i)) {
                return false;
            }
        }
        int mediaItemCount = mediaSet.getMediaItemCount();
        int i4 = 0;
        while (i4 < mediaItemCount) {
            if (jobContext.isCancelled()) {
                Log.d(TAG, "<expandMediaSet> jc.isCancelled() - 3");
                return false;
            }
            int i5 = i4 + 50;
            if (i5 >= mediaItemCount) {
                i2 = mediaItemCount - i4;
            } else {
                i2 = 50;
            }
            ArrayList<MediaItem> mediaItem = mediaSet.getMediaItem(i4, i2);
            if (mediaItem == null) {
                Log.d(TAG, "<expandMediaSet> list == null, return false");
                return false;
            }
            if (mediaItem.size() > i - arrayList.size()) {
                return false;
            }
            for (MediaItem mediaItem2 : mediaItem) {
                if (jobContext.isCancelled()) {
                    Log.d(TAG, "<expandMediaSet> jc.isCancelled() - 4");
                    return false;
                }
                arrayList.add(mediaItem2.getPath());
            }
            i4 = i5;
        }
        return true;
    }

    public void saveSelection() {
        if (this.mSaveSelectionTask != null) {
            this.mSaveSelectionTask.cancel();
        }
        Log.d(TAG, "<saveSelection> submit task");
        this.mSaveSelectionTask = this.mActivity.getThreadPool().submit(new ThreadPool.Job<Void>() {
            @Override
            public Void run(ThreadPool.JobContext jobContext) {
                synchronized (SelectionManager.LOCK) {
                    Log.d(SelectionManager.TAG, "<saveSelection> task begin");
                    if (!jobContext.isCancelled()) {
                        if (SelectionManager.this.mSelectionPath != null) {
                            SelectionManager.this.mSelectionPath.clear();
                        }
                        if (SelectionManager.this.mSelectionGroupId != null) {
                            SelectionManager.this.mSelectionGroupId.clear();
                        }
                        try {
                            SelectionManager.this.mSelectionPath = SelectionManager.this.getSelected(false);
                            SelectionManager.this.exitInverseSelectionAfterSave();
                        } catch (Exception e) {
                            SelectionManager.this.mSelectionPath = null;
                            SelectionManager.this.mSelectionGroupId = null;
                        }
                        Log.d(SelectionManager.TAG, "<saveSelection> task end");
                        return null;
                    }
                    Log.d(SelectionManager.TAG, "<saveSelection> task cancelled");
                    return null;
                }
            }
        });
    }

    private void exitInverseSelectionAfterSave() {
        if (this.mInverseSelection && this.mSelectionPath != null) {
            this.mClickedSet.clear();
            int size = this.mSelectionPath.size();
            for (int i = 0; i < size; i++) {
                this.mClickedSet.add(this.mSelectionPath.get(i));
            }
            this.mInverseSelection = false;
        }
    }

    private class RestoreSelectionJobListener implements FutureListener<Void> {
        private RestoreSelectionJobListener() {
        }

        @Override
        public void onFutureDone(Future<Void> future) {
            SelectionManager.this.mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    SelectionManager.this.mListener.onSelectionRestoreDone();
                }
            });
        }
    }

    private class RestoreSelectionJob implements ThreadPool.Job<Void> {
        private RestoreSelectionJob() {
        }

        @Override
        public Void run(ThreadPool.JobContext jobContext) {
            int mediaItemCount;
            synchronized (SelectionManager.LOCK) {
                Log.d(SelectionManager.TAG, "<restoreSelection> task begin");
                if (!jobContext.isCancelled()) {
                    if (SelectionManager.this.mSourceMediaSet != null && SelectionManager.this.mSelectionPath != null) {
                        SelectionManager selectionManager = SelectionManager.this;
                        if (SelectionManager.this.mIsAlbumSet) {
                            mediaItemCount = SelectionManager.this.mSourceMediaSet.getSubMediaSetCount();
                        } else {
                            mediaItemCount = SelectionManager.this.mSourceMediaSet.getMediaItemCount();
                        }
                        selectionManager.mTotal = mediaItemCount;
                        HashSet hashSet = new HashSet();
                        if (!SelectionManager.this.mIsAlbumSet) {
                            ArrayList<MediaItem> mediaItem = SelectionManager.this.mSourceMediaSet.getMediaItem(0, SelectionManager.this.mTotal);
                            if (mediaItem != null && mediaItem.size() > 0) {
                                for (MediaItem mediaItem2 : mediaItem) {
                                    if (jobContext.isCancelled()) {
                                        Log.d(SelectionManager.TAG, "<restoreSelection> task cancelledin job run 3");
                                        return null;
                                    }
                                    Path path = mediaItem2.getPath();
                                    if (SelectionManager.this.mSelectionPath.contains(path)) {
                                        hashSet.add(path);
                                    }
                                }
                            }
                        } else {
                            for (int i = 0; i < SelectionManager.this.mTotal; i++) {
                                MediaSet subMediaSet = SelectionManager.this.mSourceMediaSet.getSubMediaSet(i);
                                if (jobContext.isCancelled()) {
                                    Log.d(SelectionManager.TAG, "<restoreSelection> task cancelled, in job run 2");
                                    return null;
                                }
                                if (subMediaSet != null) {
                                    Path path2 = subMediaSet.getPath();
                                    if (SelectionManager.this.mSelectionPath.contains(path2)) {
                                        hashSet.add(path2);
                                    }
                                }
                            }
                        }
                        SelectionManager.this.mInverseSelection = false;
                        SelectionManager.this.mClickedSet.clear();
                        SelectionManager.this.mClickedSet = hashSet;
                        SelectionManager.this.mSelectionPath.clear();
                        SelectionManager.this.mSelectionPath = null;
                        if (SelectionManager.this.mSelectionGroupId != null) {
                            SelectionManager.this.mSelectionGroupId.clear();
                            SelectionManager.this.mSelectionGroupId = null;
                        }
                        Log.d(SelectionManager.TAG, "<restoreSelection> task end");
                        return null;
                    }
                    return null;
                }
                Log.d(SelectionManager.TAG, "<restoreSelection> task cancelledin job run 1");
                return null;
            }
        }
    }

    public void restoreSelection() {
        if (this.mRestoreSelectionTask != null) {
            this.mRestoreSelectionTask.cancel();
        }
        Log.d(TAG, "<restoreSelection> submit task");
        this.mRestoreSelectionTask = this.mActivity.getThreadPool().submit(new RestoreSelectionJob(), new RestoreSelectionJobListener());
    }
}
