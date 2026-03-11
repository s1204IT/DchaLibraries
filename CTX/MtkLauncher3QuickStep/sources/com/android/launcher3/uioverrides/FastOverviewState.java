package com.android.launcher3.uioverrides;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.quickstep.views.RecentsView;

public class FastOverviewState extends OverviewState {
    private static final float MAX_PREVIEW_SCALE_UP = 1.3f;
    public static final float OVERVIEW_TRANSLATION_FACTOR = 0.4f;
    private static final int STATE_FLAGS = 454;

    public FastOverviewState(int i) {
        super(i, 200, STATE_FLAGS);
    }

    @Override
    public void onStateTransitionEnd(Launcher launcher) {
        super.onStateTransitionEnd(launcher);
        ((RecentsView) launcher.getOverviewPanel()).getQuickScrubController().onFinishedTransitionToQuickScrub();
    }

    @Override
    public int getVisibleElements(Launcher launcher) {
        return 0;
    }

    @Override
    public float[] getOverviewScaleAndTranslationYFactor(Launcher launcher) {
        ((RecentsView) launcher.getOverviewPanel()).getTaskSize(sTempRect);
        return new float[]{getOverviewScale(launcher.getDeviceProfile(), sTempRect, launcher), 0.4f};
    }

    public static float getOverviewScale(DeviceProfile deviceProfile, Rect rect, Context context) {
        if (deviceProfile.isVerticalBarLayout()) {
            return 1.0f;
        }
        Resources resources = context.getResources();
        return Math.min(Math.min(deviceProfile.availableHeightPx / (rect.height() + resources.getDimension(R.dimen.task_thumbnail_top_margin)), deviceProfile.availableWidthPx / (rect.width() + (2.0f * (resources.getDimension(R.dimen.recents_page_spacing) + resources.getDimension(R.dimen.quickscrub_adjacent_visible_width))))), MAX_PREVIEW_SCALE_UP);
    }

    @Override
    public void onStateDisabled(Launcher launcher) {
        super.onStateDisabled(launcher);
        ((RecentsView) launcher.getOverviewPanel()).getQuickScrubController().cancelActiveQuickscrub();
    }
}
