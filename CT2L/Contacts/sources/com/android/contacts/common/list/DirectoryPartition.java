package com.android.contacts.common.list;

import com.android.common.widget.CompositeCursorAdapter;

public final class DirectoryPartition extends CompositeCursorAdapter.Partition {
    private String mContentUri;
    private long mDirectoryId;
    private String mDirectoryType;
    private String mDisplayName;
    private boolean mDisplayNumber;
    private String mLabel;
    private boolean mPhotoSupported;
    private boolean mPriorityDirectory;
    private int mResultLimit;
    private int mStatus;

    public DirectoryPartition(boolean showIfEmpty, boolean hasHeader) {
        super(showIfEmpty, hasHeader);
        this.mResultLimit = -1;
        this.mDisplayNumber = true;
    }

    public long getDirectoryId() {
        return this.mDirectoryId;
    }

    public void setDirectoryId(long directoryId) {
        this.mDirectoryId = directoryId;
    }

    public String getDirectoryType() {
        return this.mDirectoryType;
    }

    public void setDirectoryType(String directoryType) {
        this.mDirectoryType = directoryType;
    }

    public String getDisplayName() {
        return this.mDisplayName;
    }

    public void setDisplayName(String displayName) {
        this.mDisplayName = displayName;
    }

    public int getStatus() {
        return this.mStatus;
    }

    public void setStatus(int status) {
        this.mStatus = status;
    }

    public boolean isLoading() {
        return this.mStatus == 0 || this.mStatus == 1;
    }

    public boolean isPriorityDirectory() {
        return this.mPriorityDirectory;
    }

    public void setPriorityDirectory(boolean priorityDirectory) {
        this.mPriorityDirectory = priorityDirectory;
    }

    public boolean isPhotoSupported() {
        return this.mPhotoSupported;
    }

    public void setPhotoSupported(boolean flag) {
        this.mPhotoSupported = flag;
    }

    public int getResultLimit() {
        return this.mResultLimit;
    }

    public String getContentUri() {
        return this.mContentUri;
    }

    public String getLabel() {
        return this.mLabel;
    }

    public void setLabel(String label) {
        this.mLabel = label;
    }

    public String toString() {
        return "DirectoryPartition{mDirectoryId=" + this.mDirectoryId + ", mContentUri='" + this.mContentUri + "', mDirectoryType='" + this.mDirectoryType + "', mDisplayName='" + this.mDisplayName + "', mStatus=" + this.mStatus + ", mPriorityDirectory=" + this.mPriorityDirectory + ", mPhotoSupported=" + this.mPhotoSupported + ", mResultLimit=" + this.mResultLimit + ", mLabel='" + this.mLabel + "'}";
    }

    public boolean isDisplayNumber() {
        return this.mDisplayNumber;
    }
}
