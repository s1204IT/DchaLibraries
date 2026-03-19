package com.android.documentsui.dirlist;

import android.database.Cursor;
import android.util.Log;
import com.android.documentsui.MenuManager;
import com.android.documentsui.base.DocumentInfo;
import com.android.documentsui.base.MimeTypes;
import com.android.documentsui.selection.SelectionHelper;
import java.util.function.Function;

public class SelectionMetadata extends SelectionHelper.SelectionObserver implements MenuManager.SelectionDetails {
    private final Function<String, Cursor> mDocFinder;
    private int mDirectoryCount = 0;
    private int mFileCount = 0;
    private int mPartialCount = 0;
    private int mWritableDirectoryCount = 0;
    private int mNoDeleteCount = 0;
    private int mNoRenameCount = 0;
    private int mInArchiveCount = 0;
    private boolean mSupportsSettings = false;

    public SelectionMetadata(Function<String, Cursor> function) {
        this.mDocFinder = function;
    }

    @Override
    public void onItemStateChanged(String str, boolean z) {
        int i;
        Cursor cursorApply = this.mDocFinder.apply(str);
        if (cursorApply == null) {
            Log.w("SelectionMetadata", "Model returned null cursor for document: " + str + ". Ignoring state changed event.");
            return;
        }
        if (!z) {
            i = -1;
        } else {
            i = 1;
        }
        if (MimeTypes.isDirectoryType(DocumentInfo.getCursorString(cursorApply, "mime_type"))) {
            this.mDirectoryCount += i;
        } else {
            this.mFileCount += i;
        }
        int cursorInt = DocumentInfo.getCursorInt(cursorApply, "flags");
        int i2 = 65536 & cursorInt;
        if (i2 != 0) {
            this.mPartialCount += i;
        }
        if ((cursorInt & 8) != 0) {
            this.mWritableDirectoryCount += i;
        }
        if ((cursorInt & 1028) == 0) {
            this.mNoDeleteCount += i;
        }
        if ((cursorInt & 64) == 0) {
            this.mNoRenameCount += i;
        }
        if (i2 != 0) {
            this.mPartialCount += i;
        }
        this.mSupportsSettings = (cursorInt & 2048) != 0 && this.mFileCount + this.mDirectoryCount == 1;
        if ("com.android.documentsui.archives".equals(DocumentInfo.getCursorString(cursorApply, "android:authority"))) {
            this.mInArchiveCount += i;
        }
    }

    @Override
    public void onSelectionReset() {
        this.mFileCount = 0;
        this.mDirectoryCount = 0;
        this.mPartialCount = 0;
        this.mWritableDirectoryCount = 0;
        this.mNoDeleteCount = 0;
        this.mNoRenameCount = 0;
    }

    @Override
    public boolean containsDirectories() {
        return this.mDirectoryCount > 0;
    }

    @Override
    public boolean containsFiles() {
        return this.mFileCount > 0;
    }

    @Override
    public int size() {
        return this.mDirectoryCount + this.mFileCount;
    }

    @Override
    public boolean containsPartialFiles() {
        return this.mPartialCount > 0;
    }

    @Override
    public boolean containsFilesInArchive() {
        return this.mInArchiveCount > 0;
    }

    @Override
    public boolean canDelete() {
        return size() > 0 && this.mNoDeleteCount == 0;
    }

    @Override
    public boolean canExtract() {
        return size() > 0 && this.mInArchiveCount == size();
    }

    @Override
    public boolean canRename() {
        return this.mNoRenameCount == 0 && size() == 1;
    }

    @Override
    public boolean canViewInOwner() {
        return this.mSupportsSettings;
    }

    @Override
    public boolean canPasteInto() {
        return this.mDirectoryCount == 1 && this.mWritableDirectoryCount == 1 && size() == 1;
    }

    @Override
    public boolean canOpenWith() {
        return size() == 1 && this.mDirectoryCount == 0 && this.mInArchiveCount == 0 && this.mPartialCount == 0;
    }
}
