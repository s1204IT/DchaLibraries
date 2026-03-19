package com.android.gallery3d.exif;

import java.util.HashMap;
import java.util.Map;

class IfdData {
    private static final int[] sIfds = {0, 1, 2, 3, 4};
    private final int mIfdId;
    private final Map<Short, ExifTag> mExifTags = new HashMap();
    private int mOffsetToNextIfd = 0;

    IfdData(int i) {
        this.mIfdId = i;
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

    protected ExifTag getTag(short s) {
        return this.mExifTags.get(Short.valueOf(s));
    }

    protected ExifTag setTag(ExifTag exifTag) {
        exifTag.setIfd(this.mIfdId);
        return this.mExifTags.put(Short.valueOf(exifTag.getTagId()), exifTag);
    }

    protected void removeTag(short s) {
        this.mExifTags.remove(Short.valueOf(s));
    }

    protected int getTagCount() {
        return this.mExifTags.size();
    }

    protected void setOffsetToNextIfd(int i) {
        this.mOffsetToNextIfd = i;
    }

    protected int getOffsetToNextIfd() {
        return this.mOffsetToNextIfd;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == 0 || !(obj instanceof IfdData) || obj.getId() != this.mIfdId || obj.getTagCount() != getTagCount()) {
            return false;
        }
        for (ExifTag exifTag : obj.getAllTags()) {
            if (!ExifInterface.isOffsetTag(exifTag.getTagId()) && !exifTag.equals(this.mExifTags.get(Short.valueOf(exifTag.getTagId())))) {
                return false;
            }
        }
        return true;
    }
}
