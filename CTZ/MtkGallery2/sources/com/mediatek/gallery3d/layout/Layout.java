package com.mediatek.gallery3d.layout;

import android.graphics.Rect;
import com.android.gallery3d.data.MediaItem;
import com.android.gallery3d.ui.SlotView;
import com.mediatek.gallery3d.util.Log;
import java.util.ArrayList;
import java.util.HashMap;

public class Layout {
    private static final String TAG = "MtkGallery2/Layout";
    protected static int sActionBarHeight;
    protected static int sViewHeightWhenPortrait = -1;
    private int mLayoutType;

    public interface DataChangeListener {
        void onDataChange(int i, MediaItem mediaItem, int i2, boolean z, String str);
    }

    public Layout(int i) {
        this.mLayoutType = -1;
        this.mLayoutType = i;
    }

    public Layout() {
        this.mLayoutType = -1;
    }

    public int getLayoutType() {
        return this.mLayoutType;
    }

    public void setActionBarHeight(int i) {
        sActionBarHeight = i;
        Log.d(TAG, "<setActionBarHeight> <Fancy> sActionBarHeight " + sActionBarHeight);
    }

    public void setViewHeightWhenPortrait(int i) {
        sViewHeightWhenPortrait = i;
    }

    public int getViewHeightWhenPortrait() {
        return sViewHeightWhenPortrait;
    }

    public void setSlotSpec(SlotView.Spec spec) {
    }

    public void setPaddingSpec(int i, int i2) {
    }

    public SlotView.Spec getSlotSpec() {
        return null;
    }

    public boolean setSlotCount(int i) {
        return false;
    }

    public void setViewHeight(int i) {
    }

    public void refreshSlotMap(int i) {
    }

    public void setSlotRenderer(SlotView.SlotRenderer slotRenderer) {
    }

    public int getSlotCount() {
        return -1;
    }

    public void updateSlotCount(int i) {
    }

    public Rect getSlotRect(int i, Rect rect) {
        return null;
    }

    public int getSlotWidth() {
        return -1;
    }

    public int getSlotHeight() {
        return -1;
    }

    public void setSize(int i, int i2) {
    }

    public void setScrollPosition(int i) {
    }

    public int getVisibleStart() {
        return -1;
    }

    public int getVisibleEnd() {
        return -1;
    }

    public int getSlotIndexByPosition(float f, float f2) {
        return -1;
    }

    public int getScrollLimit() {
        return -1;
    }

    public void onDataChange(int i, MediaItem mediaItem, int i2, boolean z) {
    }

    public void clearColumnArray(int i, boolean z) {
    }

    public int getViewWidth() {
        return -1;
    }

    public int getViewHeight() {
        return -1;
    }

    public int getSlotGap() {
        return -1;
    }

    public void setForceRefreshFlag(boolean z) {
    }

    public void setSlotArray(ArrayList<SlotView.SlotEntry> arrayList, HashMap<Integer, ArrayList<SlotView.SlotEntry>> map) {
    }

    public boolean advanceAnimation(long j) {
        return false;
    }
}
