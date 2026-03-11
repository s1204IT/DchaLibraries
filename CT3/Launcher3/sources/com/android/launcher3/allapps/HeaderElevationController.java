package com.android.launcher3.allapps;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import com.android.launcher3.R;

public abstract class HeaderElevationController extends RecyclerView.OnScrollListener {
    private int mCurrentY = 0;

    abstract void onScroll(int i);

    public void reset() {
        this.mCurrentY = 0;
        onScroll(this.mCurrentY);
    }

    @Override
    public final void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        this.mCurrentY += dy;
        onScroll(this.mCurrentY);
    }

    public void updateBackgroundPadding(Rect bgPadding) {
    }

    public static class ControllerV16 extends HeaderElevationController {
        private final float mScrollToElevation;
        private final View mShadow;

        public ControllerV16(View header) {
            Resources res = header.getContext().getResources();
            this.mScrollToElevation = res.getDimension(R.dimen.all_apps_header_scroll_to_elevation);
            this.mShadow = new View(header.getContext());
            this.mShadow.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{503316480, 0}));
            this.mShadow.setAlpha(0.0f);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, res.getDimensionPixelSize(R.dimen.all_apps_header_shadow_height));
            lp.topMargin = ((FrameLayout.LayoutParams) header.getLayoutParams()).height;
            ((ViewGroup) header.getParent()).addView(this.mShadow, lp);
        }

        @Override
        public void onScroll(int scrollY) {
            float elevationPct = Math.min(scrollY, this.mScrollToElevation) / this.mScrollToElevation;
            this.mShadow.setAlpha(elevationPct);
        }

        @Override
        public void updateBackgroundPadding(Rect bgPadding) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.mShadow.getLayoutParams();
            lp.leftMargin = bgPadding.left;
            lp.rightMargin = bgPadding.right;
            this.mShadow.requestLayout();
        }
    }

    @TargetApi(21)
    public static class ControllerVL extends HeaderElevationController {
        private final View mHeader;
        private final float mMaxElevation;
        private final float mScrollToElevation;

        public ControllerVL(View header) {
            this.mHeader = header;
            this.mHeader.setOutlineProvider(ViewOutlineProvider.BOUNDS);
            Resources res = header.getContext().getResources();
            this.mMaxElevation = res.getDimension(R.dimen.all_apps_header_max_elevation);
            this.mScrollToElevation = res.getDimension(R.dimen.all_apps_header_scroll_to_elevation);
        }

        @Override
        public void onScroll(int scrollY) {
            float elevationPct = Math.min(scrollY, this.mScrollToElevation) / this.mScrollToElevation;
            float newElevation = this.mMaxElevation * elevationPct;
            if (Float.compare(this.mHeader.getElevation(), newElevation) == 0) {
                return;
            }
            this.mHeader.setElevation(newElevation);
        }
    }
}
