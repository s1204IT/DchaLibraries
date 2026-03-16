package com.android.gallery3d.ui;

import com.android.gallery3d.app.AbstractGalleryActivity;
import com.android.gallery3d.data.DataManager;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.data.MediaSet;
import com.android.gallery3d.data.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class SelectionManager {
    private DataManager mDataManager;
    private boolean mInSelectionMode;
    private boolean mInverseSelection;
    private boolean mIsAlbumSet;
    private SelectionListener mListener;
    private MediaSet mSourceMediaSet;
    private boolean mAutoLeave = true;
    private Set<Path> mClickedSet = new HashSet();
    private int mTotal = -1;

    public interface SelectionListener {
        void onSelectionChange(Path path, boolean z);

        void onSelectionModeChange(int i);
    }

    public SelectionManager(AbstractGalleryActivity activity, boolean isAlbumSet) {
        this.mDataManager = activity.getDataManager();
        this.mIsAlbumSet = isAlbumSet;
    }

    public void setAutoLeaveSelectionMode(boolean enable) {
        this.mAutoLeave = enable;
    }

    public void setSelectionListener(SelectionListener listener) {
        this.mListener = listener;
    }

    public void selectAll() {
        this.mInverseSelection = true;
        this.mClickedSet.clear();
        this.mTotal = -1;
        enterSelectionMode();
        if (this.mListener != null) {
            this.mListener.onSelectionModeChange(3);
        }
    }

    public void deSelectAll() {
        leaveSelectionMode();
        this.mInverseSelection = false;
        this.mClickedSet.clear();
    }

    public boolean inSelectAllMode() {
        return this.mInverseSelection;
    }

    public boolean inSelectionMode() {
        return this.mInSelectionMode;
    }

    public void enterSelectionMode() {
        if (!this.mInSelectionMode) {
            this.mInSelectionMode = true;
            if (this.mListener != null) {
                this.mListener.onSelectionModeChange(1);
            }
        }
    }

    public void leaveSelectionMode() {
        if (this.mInSelectionMode) {
            this.mInSelectionMode = false;
            this.mInverseSelection = false;
            this.mClickedSet.clear();
            if (this.mListener != null) {
                this.mListener.onSelectionModeChange(2);
            }
        }
    }

    public boolean isItemSelected(Path itemId) {
        return this.mInverseSelection ^ this.mClickedSet.contains(itemId);
    }

    private int getTotalCount() {
        if (this.mSourceMediaSet == null) {
            return -1;
        }
        if (this.mTotal < 0) {
            this.mTotal = this.mIsAlbumSet ? this.mSourceMediaSet.getSubMediaSetCount() : this.mSourceMediaSet.getMediaItemCount();
        }
        return this.mTotal;
    }

    public int getSelectedCount() {
        int count = this.mClickedSet.size();
        if (this.mInverseSelection) {
            return getTotalCount() - count;
        }
        return count;
    }

    public void toggle(Path path) {
        if (this.mClickedSet.contains(path)) {
            this.mClickedSet.remove(path);
        } else {
            enterSelectionMode();
            this.mClickedSet.add(path);
        }
        int count = getSelectedCount();
        if (count == getTotalCount()) {
            selectAll();
        }
        if (this.mListener != null) {
            this.mListener.onSelectionChange(path, isItemSelected(path));
        }
        if (count == 0 && this.mAutoLeave) {
            leaveSelectionMode();
        }
    }

    private static boolean expandMediaSet(ArrayList<Path> items, MediaSet set, int maxSelection) {
        int count;
        int subCount = set.getSubMediaSetCount();
        for (int i = 0; i < subCount; i++) {
            if (!expandMediaSet(items, set.getSubMediaSet(i), maxSelection)) {
                return false;
            }
        }
        int total = set.getMediaItemCount();
        for (int index = 0; index < total; index += 50) {
            if (index + 50 < total) {
                count = 50;
            } else {
                count = total - index;
            }
            ArrayList<MediaItem> list = set.getMediaItem(index, count);
            if (list != null && list.size() > maxSelection - items.size()) {
                return false;
            }
            for (MediaItem item : list) {
                items.add(item.getPath());
            }
        }
        return true;
    }

    public ArrayList<Path> getSelected(boolean expandSet) {
        return getSelected(expandSet, Integer.MAX_VALUE);
    }

    public ArrayList<Path> getSelected(boolean expandSet, int maxSelection) {
        ArrayList<Path> selected = new ArrayList<>();
        if (this.mIsAlbumSet) {
            if (this.mInverseSelection) {
                int total = getTotalCount();
                for (int i = 0; i < total; i++) {
                    MediaSet set = this.mSourceMediaSet.getSubMediaSet(i);
                    Path id = set.getPath();
                    if (!this.mClickedSet.contains(id)) {
                        if (expandSet) {
                            if (!expandMediaSet(selected, set, maxSelection)) {
                                return null;
                            }
                        } else {
                            selected.add(id);
                            if (selected.size() > maxSelection) {
                                return null;
                            }
                        }
                    }
                }
                return selected;
            }
            for (Path id2 : this.mClickedSet) {
                if (expandSet) {
                    if (!expandMediaSet(selected, this.mDataManager.getMediaSet(id2), maxSelection)) {
                        return null;
                    }
                } else {
                    selected.add(id2);
                    if (selected.size() > maxSelection) {
                        return null;
                    }
                }
            }
            return selected;
        }
        if (this.mInverseSelection) {
            int total2 = getTotalCount();
            int index = 0;
            while (index < total2) {
                int count = Math.min(total2 - index, 500);
                ArrayList<MediaItem> list = this.mSourceMediaSet.getMediaItem(index, count);
                for (MediaItem item : list) {
                    Path id3 = item.getPath();
                    if (!this.mClickedSet.contains(id3)) {
                        selected.add(id3);
                        if (selected.size() > maxSelection) {
                            return null;
                        }
                    }
                }
                index += count;
            }
            return selected;
        }
        Iterator<Path> it = this.mClickedSet.iterator();
        while (it.hasNext()) {
            selected.add(it.next());
            if (selected.size() > maxSelection) {
                return null;
            }
        }
        return selected;
    }

    public void setSourceMediaSet(MediaSet set) {
        this.mSourceMediaSet = set;
        this.mTotal = -1;
    }
}
