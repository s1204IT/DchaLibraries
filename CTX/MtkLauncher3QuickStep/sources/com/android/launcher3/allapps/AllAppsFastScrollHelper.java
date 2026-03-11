package com.android.launcher3.allapps;

import android.support.v7.widget.RecyclerView;
import com.android.launcher3.allapps.AllAppsGridAdapter;
import com.android.launcher3.allapps.AlphabeticalAppsList;
import java.util.HashSet;
import java.util.List;

public class AllAppsFastScrollHelper implements AllAppsGridAdapter.BindViewCallback {
    private static final int INITIAL_TOUCH_SETTLING_DURATION = 100;
    private static final int REPEAT_TOUCH_SETTLING_DURATION = 200;
    private AlphabeticalAppsList mApps;
    String mCurrentFastScrollSection;
    int mFastScrollFrameIndex;
    private boolean mHasFastScrollTouchSettled;
    private boolean mHasFastScrollTouchSettledAtLeastOnce;
    private AllAppsRecyclerView mRv;
    String mTargetFastScrollSection;
    int mTargetFastScrollPosition = -1;
    private HashSet<RecyclerView.ViewHolder> mTrackedFastScrollViews = new HashSet<>();
    final int[] mFastScrollFrames = new int[10];
    Runnable mSmoothSnapNextFrameRunnable = new Runnable() {
        @Override
        public void run() {
            if (AllAppsFastScrollHelper.this.mFastScrollFrameIndex < AllAppsFastScrollHelper.this.mFastScrollFrames.length) {
                AllAppsFastScrollHelper.this.mRv.scrollBy(0, AllAppsFastScrollHelper.this.mFastScrollFrames[AllAppsFastScrollHelper.this.mFastScrollFrameIndex]);
                AllAppsFastScrollHelper.this.mFastScrollFrameIndex++;
                AllAppsFastScrollHelper.this.mRv.postOnAnimation(AllAppsFastScrollHelper.this.mSmoothSnapNextFrameRunnable);
            }
        }
    };
    Runnable mFastScrollToTargetSectionRunnable = new Runnable() {
        @Override
        public void run() {
            AllAppsFastScrollHelper.this.mCurrentFastScrollSection = AllAppsFastScrollHelper.this.mTargetFastScrollSection;
            AllAppsFastScrollHelper.this.mHasFastScrollTouchSettled = true;
            AllAppsFastScrollHelper.this.mHasFastScrollTouchSettledAtLeastOnce = true;
            AllAppsFastScrollHelper.this.updateTrackedViewsFastScrollFocusState();
        }
    };

    public AllAppsFastScrollHelper(AllAppsRecyclerView allAppsRecyclerView, AlphabeticalAppsList alphabeticalAppsList) {
        this.mRv = allAppsRecyclerView;
        this.mApps = alphabeticalAppsList;
    }

    public void onSetAdapter(AllAppsGridAdapter allAppsGridAdapter) {
        allAppsGridAdapter.setBindViewCallback(this);
    }

    public boolean smoothScrollToSection(int i, int i2, AlphabeticalAppsList.FastScrollSectionInfo fastScrollSectionInfo) {
        if (this.mTargetFastScrollPosition != fastScrollSectionInfo.fastScrollToItem.position) {
            this.mTargetFastScrollPosition = fastScrollSectionInfo.fastScrollToItem.position;
            smoothSnapToPosition(i, i2, fastScrollSectionInfo);
            return true;
        }
        return false;
    }

    private void smoothSnapToPosition(int i, int i2, AlphabeticalAppsList.FastScrollSectionInfo fastScrollSectionInfo) {
        long j;
        int iMin;
        this.mRv.removeCallbacks(this.mSmoothSnapNextFrameRunnable);
        this.mRv.removeCallbacks(this.mFastScrollToTargetSectionRunnable);
        trackAllChildViews();
        if (this.mHasFastScrollTouchSettled) {
            this.mCurrentFastScrollSection = fastScrollSectionInfo.sectionName;
            this.mTargetFastScrollSection = null;
            updateTrackedViewsFastScrollFocusState();
        } else {
            this.mCurrentFastScrollSection = null;
            this.mTargetFastScrollSection = fastScrollSectionInfo.sectionName;
            this.mHasFastScrollTouchSettled = false;
            updateTrackedViewsFastScrollFocusState();
            AllAppsRecyclerView allAppsRecyclerView = this.mRv;
            Runnable runnable = this.mFastScrollToTargetSectionRunnable;
            if (this.mHasFastScrollTouchSettledAtLeastOnce) {
                j = 200;
            } else {
                j = 100;
            }
            allAppsRecyclerView.postDelayed(runnable, j);
        }
        List<AlphabeticalAppsList.FastScrollSectionInfo> fastScrollerSections = this.mApps.getFastScrollerSections();
        int i3 = fastScrollSectionInfo.fastScrollToItem.position;
        if (fastScrollerSections.size() <= 0 || fastScrollerSections.get(0) != fastScrollSectionInfo) {
            iMin = Math.min(i2, this.mRv.getCurrentScrollY(i3, 0));
        } else {
            iMin = 0;
        }
        int length = this.mFastScrollFrames.length;
        int i4 = iMin - i;
        float fSignum = Math.signum(i4);
        int iCeil = (int) (((double) fSignum) * Math.ceil(Math.abs(i4) / length));
        int i5 = i4;
        for (int i6 = 0; i6 < length; i6++) {
            this.mFastScrollFrames[i6] = (int) (Math.min(Math.abs(iCeil), Math.abs(i5)) * fSignum);
            i5 -= iCeil;
        }
        this.mFastScrollFrameIndex = 0;
        this.mRv.postOnAnimation(this.mSmoothSnapNextFrameRunnable);
    }

    public void onFastScrollCompleted() {
        this.mRv.removeCallbacks(this.mSmoothSnapNextFrameRunnable);
        this.mRv.removeCallbacks(this.mFastScrollToTargetSectionRunnable);
        this.mHasFastScrollTouchSettled = false;
        this.mHasFastScrollTouchSettledAtLeastOnce = false;
        this.mCurrentFastScrollSection = null;
        this.mTargetFastScrollSection = null;
        this.mTargetFastScrollPosition = -1;
        updateTrackedViewsFastScrollFocusState();
        this.mTrackedFastScrollViews.clear();
    }

    @Override
    public void onBindView(AllAppsGridAdapter.ViewHolder viewHolder) {
        if (this.mCurrentFastScrollSection != null || this.mTargetFastScrollSection != null) {
            this.mTrackedFastScrollViews.add(viewHolder);
        }
    }

    private void trackAllChildViews() {
        int childCount = this.mRv.getChildCount();
        for (int i = 0; i < childCount; i++) {
            RecyclerView.ViewHolder childViewHolder = this.mRv.getChildViewHolder(this.mRv.getChildAt(i));
            if (childViewHolder != null) {
                this.mTrackedFastScrollViews.add(childViewHolder);
            }
        }
    }

    public void updateTrackedViewsFastScrollFocusState() {
        AlphabeticalAppsList.AdapterItem adapterItem;
        for (RecyclerView.ViewHolder viewHolder : this.mTrackedFastScrollViews) {
            int adapterPosition = viewHolder.getAdapterPosition();
            boolean z = false;
            if (this.mCurrentFastScrollSection != null && adapterPosition > -1 && adapterPosition < this.mApps.getAdapterItems().size() && (adapterItem = this.mApps.getAdapterItems().get(adapterPosition)) != null && this.mCurrentFastScrollSection.equals(adapterItem.sectionName) && adapterItem.position == this.mTargetFastScrollPosition) {
                z = true;
            }
            viewHolder.itemView.setActivated(z);
        }
    }
}
