package com.mediatek.gallery3d.layout;

import android.graphics.Rect;
import com.android.gallery3d.common.Utils;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.AlbumSetSlotRenderer;
import com.android.gallery3d.ui.SlotView;
import com.mediatek.gallery3d.util.FeatureConfig;
import com.mediatek.gallery3d.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class FancyLayout extends Layout {
    private volatile int mContentLength;
    private boolean mForceRefreshFlag;
    private int mHeight;
    private volatile boolean mIsLandCameraFolder;
    private int mPaddingBottom;
    private int mPaddingTop;
    private SlotView.SlotRenderer mRenderer;
    private int mScrollPosition;
    private ArrayList<SlotView.SlotEntry> mSlotArray;
    private int mSlotCount;
    private int mSlotGap;
    private HashMap<Integer, ArrayList<SlotView.SlotEntry>> mSlotMapByColumn;
    private Object mSlotMapByColumnLock;
    private int mSlotWidth;
    private SlotView.Spec mSpec;
    private Rect mTempRect;
    private int mVisibleEnd;
    private int mVisibleStart;
    private int mWidth;

    public FancyLayout(int i) {
        super(i);
        this.mForceRefreshFlag = false;
        this.mIsLandCameraFolder = false;
        this.mSlotMapByColumnLock = new Object();
        this.mTempRect = new Rect();
    }

    public FancyLayout() {
        this.mForceRefreshFlag = false;
        this.mIsLandCameraFolder = false;
        this.mSlotMapByColumnLock = new Object();
        this.mTempRect = new Rect();
    }

    @Override
    public void onDataChange(int i, MediaItem mediaItem, int i2, boolean z) {
        refreshSlotMap(i);
        updateVisibleSlotRange(Math.min(this.mScrollPosition, getScrollLimit()));
    }

    @Override
    public void clearColumnArray(int i, boolean z) {
        synchronized (this.mSlotMapByColumnLock) {
            if (this.mSlotMapByColumn != null && this.mSlotArray != null) {
                for (int i2 = 0; i2 < 2; i2++) {
                    ArrayList<SlotView.SlotEntry> arrayList = this.mSlotMapByColumn.get(Integer.valueOf(i2));
                    if (arrayList == null) {
                        return;
                    }
                    if (z) {
                        arrayList.clear();
                    } else {
                        for (int size = arrayList.size() - 1; size >= 0; size--) {
                            if (arrayList.get(size).slotIndex >= i) {
                                arrayList.remove(size);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setSlotArray(ArrayList<SlotView.SlotEntry> arrayList, HashMap<Integer, ArrayList<SlotView.SlotEntry>> map) {
        synchronized (this.mSlotMapByColumnLock) {
            this.mSlotArray = arrayList;
            this.mSlotMapByColumn = map;
        }
    }

    @Override
    public void setSlotRenderer(SlotView.SlotRenderer slotRenderer) {
        this.mRenderer = slotRenderer;
    }

    @Override
    public void setScrollPosition(int i) {
        if (!this.mForceRefreshFlag && this.mScrollPosition == i) {
            return;
        }
        this.mScrollPosition = i;
        updateVisibleSlotRange(i);
    }

    @Override
    public int getSlotWidth() {
        return this.mSlotWidth;
    }

    @Override
    public int getScrollLimit() {
        return Math.max(0, this.mContentLength - this.mHeight);
    }

    @Override
    public void setSlotSpec(SlotView.Spec spec) {
        this.mSpec = spec;
        this.mSlotGap = spec.slotGap;
    }

    @Override
    public void setPaddingSpec(int i, int i2) {
        this.mPaddingTop = i;
        this.mPaddingBottom = i2;
        Log.d("MtkGallery2/FancyLayout", "<setPaddingSpec> <Fancy> paddingTop " + i + ", paddingBottom " + i2);
    }

    @Override
    public SlotView.Spec getSlotSpec() {
        return this.mSpec;
    }

    @Override
    public int getViewWidth() {
        return this.mWidth;
    }

    @Override
    public int getViewHeight() {
        return this.mHeight;
    }

    @Override
    public int getSlotGap() {
        return this.mSlotGap;
    }

    @Override
    public boolean setSlotCount(int i) {
        if (!this.mForceRefreshFlag && i == this.mSlotCount) {
            return false;
        }
        this.mSlotCount = i;
        setVisibleRange(0, Math.min(this.mSlotCount, AlbumSetSlotRenderer.CACHE_SIZE));
        Log.d("MtkGallery2/FancyLayout", "<setSlotCount> <Fancy> slotCount " + i);
        return true;
    }

    @Override
    public int getSlotCount() {
        return this.mSlotCount;
    }

    @Override
    public void setSize(int i, int i2) {
        this.mWidth = i;
        this.mHeight = i2;
        FancyHelper.doFancyInitialization(this.mWidth, this.mHeight);
        this.mSlotWidth = (this.mWidth - (this.mSlotGap * 1)) / 2;
        Log.d("MtkGallery2/FancyLayout", "<setSize> <Fancy> getScreenWidthAtFancyMode " + FancyHelper.getScreenWidthAtFancyMode());
        if (FeatureConfig.IS_TABLET && this.mSlotArray != null && i < i2) {
            Iterator<SlotView.SlotEntry> it = this.mSlotArray.iterator();
            while (it.hasNext()) {
                it.next().update(this.mSlotWidth, this.mSlotGap);
            }
            clearColumnArray(0, true);
            refreshSlotMap(0);
            updateVisibleSlotRange(Math.min(this.mScrollPosition, getScrollLimit()));
        }
        if (this.mRenderer != null) {
            this.mRenderer.onSlotSizeChanged(getSlotWidth(), getSlotHeight());
        }
    }

    @Override
    public void refreshSlotMap(int i) {
        int i2;
        int i3;
        int i4;
        synchronized (this.mSlotMapByColumnLock) {
            if (this.mSlotMapByColumn != null && this.mSlotArray != null) {
                ArrayList<SlotView.SlotEntry> arrayList = this.mSlotMapByColumn.get(0);
                ArrayList<SlotView.SlotEntry> arrayList2 = this.mSlotMapByColumn.get(1);
                if (arrayList != null && arrayList2 != null) {
                    int i5 = 0;
                    int i6 = 0;
                    while (i < this.mSlotArray.size()) {
                        SlotView.SlotEntry slotEntry = this.mSlotArray.get(i);
                        if (i == 0) {
                            slotEntry.inWhichCol = 0;
                            slotEntry.inWhichRow = 0;
                            int i7 = slotEntry.scaledWidth;
                            int i8 = slotEntry.scaledHeight;
                            if (slotEntry.slotRect == null) {
                                slotEntry.slotRect = new Rect(0, 0, i7, i8);
                            } else {
                                slotEntry.slotRect.set(0, 0, i7, i8);
                            }
                            i4 = slotEntry.scaledHeight;
                            arrayList.add(0, slotEntry);
                            if (slotEntry.isLandCameraFolder) {
                                this.mIsLandCameraFolder = true;
                            } else {
                                this.mIsLandCameraFolder = false;
                            }
                        } else {
                            if (i == 1) {
                                if (this.mIsLandCameraFolder) {
                                    slotEntry.inWhichCol = 0;
                                    slotEntry.inWhichRow = 1;
                                    int i9 = arrayList.get(0).slotRect.bottom + this.mSlotGap;
                                    int i10 = slotEntry.scaledWidth;
                                    int i11 = slotEntry.scaledHeight + i9;
                                    if (slotEntry.slotRect == null) {
                                        slotEntry.slotRect = new Rect(0, i9, i10, i11);
                                    } else {
                                        slotEntry.slotRect.set(0, i9, i10, i11);
                                    }
                                    i4 = arrayList.get(0).scaledHeight + slotEntry.scaledHeight + this.mSlotGap;
                                    arrayList.add(1, slotEntry);
                                } else {
                                    slotEntry.inWhichCol = 1;
                                    slotEntry.inWhichRow = 0;
                                    int i12 = slotEntry.scaledWidth + this.mSlotGap;
                                    int i13 = slotEntry.scaledWidth + i12;
                                    int i14 = slotEntry.scaledHeight;
                                    if (slotEntry.slotRect == null) {
                                        slotEntry.slotRect = new Rect(i12, 0, i13, i14);
                                    } else {
                                        slotEntry.slotRect.set(i12, 0, i13, i14);
                                    }
                                    i2 = arrayList.get(0).scaledHeight;
                                    i3 = slotEntry.scaledHeight;
                                    arrayList2.add(0, slotEntry);
                                }
                            } else if (i == 2 && this.mIsLandCameraFolder) {
                                slotEntry.inWhichCol = 1;
                                slotEntry.inWhichRow = 0;
                                int i15 = slotEntry.scaledWidth + this.mSlotGap;
                                int i16 = arrayList.get(0).scaledHeight + this.mSlotGap;
                                int i17 = slotEntry.scaledWidth + i15;
                                int i18 = slotEntry.scaledHeight + i16;
                                if (slotEntry.slotRect == null) {
                                    slotEntry.slotRect = new Rect(i15, i16, i17, i18);
                                } else {
                                    slotEntry.slotRect.set(i15, i16, i17, i18);
                                }
                                i2 = arrayList.get(0).scaledHeight + this.mSlotGap + arrayList.get(1).scaledHeight;
                                i3 = arrayList.get(0).scaledHeight + this.mSlotGap + slotEntry.scaledHeight;
                                arrayList2.add(0, slotEntry);
                            } else {
                                int i19 = arrayList.get(arrayList.size() - 1).slotRect.bottom;
                                int i20 = arrayList2.get(arrayList2.size() - 1).slotRect.bottom;
                                if (i19 <= i20) {
                                    slotEntry.inWhichCol = 0;
                                    slotEntry.inWhichRow = arrayList.size();
                                    int i21 = i19 + this.mSlotGap;
                                    int i22 = slotEntry.scaledWidth + 0;
                                    int i23 = slotEntry.scaledHeight + i21;
                                    if (slotEntry.slotRect == null) {
                                        slotEntry.slotRect = new Rect(0, i21, i22, i23);
                                    } else {
                                        slotEntry.slotRect.set(0, i21, i22, i23);
                                    }
                                    arrayList.add(arrayList.size(), slotEntry);
                                    i6 = i20;
                                    i5 = i23;
                                } else {
                                    slotEntry.inWhichCol = 1;
                                    slotEntry.inWhichRow = arrayList2.size();
                                    int i24 = slotEntry.scaledWidth + this.mSlotGap;
                                    int i25 = i20 + this.mSlotGap;
                                    int i26 = slotEntry.scaledWidth + i24;
                                    int i27 = slotEntry.scaledHeight + i25;
                                    if (slotEntry.slotRect == null) {
                                        slotEntry.slotRect = new Rect(i24, i25, i26, i27);
                                    } else {
                                        slotEntry.slotRect.set(i24, i25, i26, i27);
                                    }
                                    arrayList2.add(arrayList2.size(), slotEntry);
                                    i5 = i19;
                                    i6 = i27;
                                }
                                i++;
                            }
                            i5 = i2;
                            i6 = i3;
                            i++;
                        }
                        i5 = i4;
                        i++;
                    }
                    this.mContentLength = Math.max(i5, i6);
                }
            }
        }
    }

    @Override
    public Rect getSlotRect(int i, Rect rect) {
        if (this.mSlotArray == null || i < 0 || i >= this.mSlotArray.size() || this.mSlotArray.get(i).slotRect == null) {
            return new Rect(0, 0, 1, 1);
        }
        Rect rect2 = this.mSlotArray.get(i).slotRect;
        if (rect != null) {
            rect.set(rect2.left, rect2.top, rect2.right, rect2.bottom);
            return rect;
        }
        return new Rect(rect2.left, rect2.top, rect2.right, rect2.bottom);
    }

    @Override
    public int getSlotIndexByPosition(float f, float f2) {
        for (int i = this.mVisibleStart; i < this.mVisibleEnd; i++) {
            if (getSlotRect(i, this.mTempRect).contains(Math.round(f), Math.round(this.mScrollPosition + f2))) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getVisibleStart() {
        return this.mVisibleStart;
    }

    @Override
    public int getVisibleEnd() {
        return this.mVisibleEnd;
    }

    @Override
    public void setForceRefreshFlag(boolean z) {
        this.mForceRefreshFlag = z;
    }

    @Override
    public void setViewHeight(int i) {
        this.mHeight = i;
        Log.d("MtkGallery2/FancyLayout", "<setViewHeight> <Fancy> mHeight " + this.mHeight);
    }

    @Override
    public void updateSlotCount(int i) {
        this.mSlotCount = i;
        Log.d("MtkGallery2/FancyLayout", "<updateSlotCount> <Fancy> slotCount " + i);
    }

    private int isVisible(int i, int i2) {
        Rect slotRect = getSlotRect(i, this.mTempRect);
        if (slotRect == null) {
            return -2;
        }
        int iMax = Math.max(0, (i2 - sActionBarHeight) - this.mPaddingTop);
        int i3 = i2 + this.mHeight + this.mPaddingBottom;
        if ((slotRect.top <= iMax && iMax < slotRect.bottom) || ((slotRect.top < i3 && i3 <= slotRect.bottom) || (iMax < slotRect.top && slotRect.bottom < i3))) {
            return 0;
        }
        if (slotRect.top >= i3) {
            return -1;
        }
        if (slotRect.bottom > iMax) {
            return -2;
        }
        return 1;
    }

    private int[] calcVisibleStartAndEnd(int i) {
        int iMin;
        int i2 = 2;
        int[] iArr = new int[2];
        synchronized (this.mSlotMapByColumnLock) {
            int i3 = 0;
            if (this.mSlotArray != null && this.mSlotArray.size() != 0 && this.mSlotMapByColumn != null && this.mSlotMapByColumn.size() != 0 && this.mSlotMapByColumn.get(0).size() != 0) {
                if (this.mSlotArray.size() == 1) {
                    iArr[0] = 0;
                    iArr[1] = 1;
                    return iArr;
                }
                if (this.mSlotArray.size() == 2) {
                    iArr[0] = 0;
                    iArr[1] = 2;
                    return iArr;
                }
                int i4 = 0;
                int i5 = 0;
                int i6 = 0;
                int i7 = 0;
                int i8 = 0;
                while (i4 < i2) {
                    int i9 = i8;
                    int i10 = i7;
                    int i11 = i6;
                    int i12 = i5;
                    int i13 = i3;
                    while (i13 < i2) {
                        int iMax = Math.max(i3, this.mSlotMapByColumn.get(Integer.valueOf(i4)).size() - 1);
                        int i14 = (i3 + iMax) / i2;
                        int i15 = iMax;
                        int i16 = i3;
                        do {
                            int iIsVisible = isVisible(this.mSlotMapByColumn.get(Integer.valueOf(i4)).get(i14).slotIndex, i);
                            if (iIsVisible != -1) {
                                if (iIsVisible != 1) {
                                    if (iIsVisible == 0) {
                                        if (i13 == 0) {
                                        }
                                    }
                                    i14 = (i16 + i15) / 2;
                                    if (i16 == i15) {
                                        break;
                                    }
                                }
                                i16 = i14;
                                i14 = (i16 + i15) / 2;
                                if (i16 == i15) {
                                }
                            }
                            i15 = i14;
                            i14 = (i16 + i15) / 2;
                            if (i16 == i15) {
                            }
                        } while (i15 != i16 + 1);
                        if (i13 != 0) {
                            if (i13 == 1) {
                                int i17 = i16 + 1;
                                iMin = isVisible(this.mSlotMapByColumn.get(Integer.valueOf(i4)).get(Math.min(i17, this.mSlotMapByColumn.get(Integer.valueOf(i4)).size() - 1)).slotIndex, i) == 0 ? Math.min(i17, this.mSlotMapByColumn.get(Integer.valueOf(i4)).size() - 1) : i16;
                            } else {
                                iMin = 0;
                            }
                        } else {
                            int i18 = i15 - 1;
                            iMin = isVisible(this.mSlotMapByColumn.get(Integer.valueOf(i4)).get(Math.max(i18, 0)).slotIndex, i) == 0 ? Math.max(i18, 0) : i15;
                        }
                        if (i4 == 0 && i13 == 0) {
                            i12 = this.mSlotMapByColumn.get(Integer.valueOf(i4)).get(iMin).slotIndex;
                        } else if (i4 == 0 && i13 == 1) {
                            i10 = this.mSlotMapByColumn.get(Integer.valueOf(i4)).get(iMin).slotIndex;
                        } else if (i4 == 1 && i13 == 0) {
                            i11 = this.mSlotMapByColumn.get(Integer.valueOf(i4)).get(iMin).slotIndex;
                        } else if (i4 == 1 && i13 == 1) {
                            i9 = this.mSlotMapByColumn.get(Integer.valueOf(i4)).get(iMin).slotIndex;
                        }
                        i13++;
                        i2 = 2;
                        i3 = 0;
                    }
                    i4++;
                    i5 = i12;
                    i6 = i11;
                    i7 = i10;
                    i8 = i9;
                    i2 = 2;
                    i3 = 0;
                }
                iArr[0] = Utils.clamp(Math.min(i5, i6), 0, this.mSlotArray.size() - 1);
                iArr[1] = Utils.clamp(Math.max(i7, i8) + 1, iArr[0] + 1, this.mSlotArray.size());
                iArr[1] = Math.min(iArr[1], iArr[0] + AlbumSetSlotRenderer.CACHE_SIZE);
                return iArr;
            }
            iArr[1] = 0;
            iArr[0] = 0;
            return iArr;
        }
    }

    private synchronized void updateVisibleSlotRange(int i) {
        int[] iArrCalcVisibleStartAndEnd = calcVisibleStartAndEnd(i);
        if (this.mSlotArray == null || (this.mSlotArray != null && this.mSlotArray.size() == 0)) {
            Log.d("MtkGallery2/FancyLayout", "<updateVisibleSlotRange> <Fancy> set visible as [0, 0], mSlotArray " + this.mSlotArray);
            setVisibleRange(0, 0);
        }
        int i2 = iArrCalcVisibleStartAndEnd[0];
        int i3 = iArrCalcVisibleStartAndEnd[1];
        if (i2 <= i3 && i3 - i2 <= AlbumSetSlotRenderer.CACHE_SIZE && i3 <= this.mSlotArray.size()) {
            setVisibleRange(i2, i3);
        } else {
            Log.d("MtkGallery2/FancyLayout", "<updateVisibleSlotRange> <Fancy> correct visible range: mSlotArray.size() " + this.mSlotArray.size() + ", [" + i2 + ", " + i3 + "] -> [0, " + Math.min(this.mSlotArray.size(), AlbumSetSlotRenderer.CACHE_SIZE) + "]");
            setVisibleRange(0, Math.min(this.mSlotArray.size(), AlbumSetSlotRenderer.CACHE_SIZE));
        }
    }

    private void setVisibleRange(int i, int i2) {
        if (!this.mForceRefreshFlag && i == this.mVisibleStart && i2 == this.mVisibleEnd) {
            return;
        }
        if (i < i2) {
            this.mVisibleStart = i;
            this.mVisibleEnd = i2;
        } else {
            this.mVisibleEnd = 0;
            this.mVisibleStart = 0;
        }
        if (this.mRenderer != null) {
            this.mRenderer.onVisibleRangeChanged(this.mVisibleStart, this.mVisibleEnd);
        }
    }
}
