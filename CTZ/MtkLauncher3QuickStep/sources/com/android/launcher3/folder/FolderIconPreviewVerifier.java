package com.android.launcher3.folder;

import com.android.launcher3.FolderInfo;
import com.android.launcher3.InvariantDeviceProfile;

public class FolderIconPreviewVerifier {
    private int mGridCountX;
    private final int mMaxGridCountX;
    private final int mMaxGridCountY;
    private final int mMaxItemsPerPage;
    private final int[] mGridSize = new int[2];
    private boolean mDisplayingUpperLeftQuadrant = false;

    public FolderIconPreviewVerifier(InvariantDeviceProfile invariantDeviceProfile) {
        this.mMaxGridCountX = invariantDeviceProfile.numFolderColumns;
        this.mMaxGridCountY = invariantDeviceProfile.numFolderRows;
        this.mMaxItemsPerPage = this.mMaxGridCountX * this.mMaxGridCountY;
    }

    public void setFolderInfo(FolderInfo folderInfo) {
        int size = folderInfo.contents.size();
        FolderPagedView.calculateGridSize(size, 0, 0, this.mMaxGridCountX, this.mMaxGridCountY, this.mMaxItemsPerPage, this.mGridSize);
        this.mGridCountX = this.mGridSize[0];
        this.mDisplayingUpperLeftQuadrant = size > 4;
    }

    public boolean isItemInPreview(int i) {
        return isItemInPreview(0, i);
    }

    public boolean isItemInPreview(int i, int i2) {
        if (i > 0 || this.mDisplayingUpperLeftQuadrant) {
            return i2 % this.mGridCountX < 2 && i2 / this.mGridCountX < 2;
        }
        return i2 < 4;
    }
}
