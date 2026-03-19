package com.mediatek.gallerybasic.base;

public enum ThumbType {
    MICRO,
    MIDDLE,
    FANCY,
    HIGHQUALITY;

    private static int sFancySize = 360;
    private static int sHighQuality;
    private static int sMicroSize;
    private static int sMiddleSize;

    public int getTargetSize() {
        if (this == MICRO) {
            return sMicroSize;
        }
        if (this == MIDDLE) {
            return sMiddleSize;
        }
        if (this == FANCY) {
            return sFancySize;
        }
        if (this == HIGHQUALITY) {
            return sHighQuality;
        }
        return -1;
    }

    public void setTargetSize(int i) {
        if (this == MICRO) {
            sMicroSize = i;
            return;
        }
        if (this == MIDDLE) {
            sMiddleSize = i;
        } else if (this == FANCY) {
            sFancySize = i;
        } else if (this == HIGHQUALITY) {
            sHighQuality = i;
        }
    }
}
