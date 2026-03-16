package com.android.gallery3d.exif;

import java.util.HashMap;
import java.util.Map;

class IfdData {
    private static final int[] sIfds = {0, 1, 2, 3, 4};
    private final int mIfdId;
    private final Map<Short, ExifTag> mExifTags = new HashMap();
    private int mOffsetToNextIfd = 0;

    IfdData(int ifdId) {
        this.mIfdId = ifdId;
    }

    protected static int[] getIfds() {
        return sIfds;
    }

    protected ExifTag[] getAllTags() {
        return (ExifTag[]) this.mExifTags.values().toArray(new ExifTag[this.mExifTags.size()]);
    }

    protected int getId() {
        return this.mIfdId;
    }

    protected ExifTag getTag(short tagId) {
        return this.mExifTags.get(Short.valueOf(tagId));
    }

    protected ExifTag setTag(ExifTag tag) {
        tag.setIfd(this.mIfdId);
        return this.mExifTags.put(Short.valueOf(tag.getTagId()), tag);
    }

    protected void removeTag(short tagId) {
        this.mExifTags.remove(Short.valueOf(tagId));
    }

    protected int getTagCount() {
        return this.mExifTags.size();
    }

    protected void setOffsetToNextIfd(int offset) {
        this.mOffsetToNextIfd = offset;
    }

    protected int getOffsetToNextIfd() {
        return this.mOffsetToNextIfd;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj instanceof IfdData) {
            IfdData data = (IfdData) obj;
            if (data.getId() == this.mIfdId && data.getTagCount() == getTagCount()) {
                ExifTag[] tags = data.getAllTags();
                for (ExifTag tag : tags) {
                    if (!ExifInterface.isOffsetTag(tag.getTagId())) {
                        ExifTag tag2 = this.mExifTags.get(Short.valueOf(tag.getTagId()));
                        if (!tag.equals(tag2)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }
}
