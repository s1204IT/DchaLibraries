package com.android.systemui.recent;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.RecentsComponent;
import com.android.systemui.SystemUI;
import com.android.systemui.recents.AlternateRecentsComponent;

public class Recents extends SystemUI implements RecentsComponent {
    static AlternateRecentsComponent sAlternateRecents;
    boolean mUseAlternateRecents = true;
    boolean mBootCompleted = false;

    public static AlternateRecentsComponent getRecentsComponent(Context context, boolean forceInitialize) {
        if (sAlternateRecents == null) {
            sAlternateRecents = new AlternateRecentsComponent(context);
            if (forceInitialize) {
                sAlternateRecents.onStart();
                sAlternateRecents.onBootCompleted();
            }
        }
        return sAlternateRecents;
    }

    @Override
    public void start() {
        if (this.mUseAlternateRecents) {
            if (sAlternateRecents == null) {
                sAlternateRecents = getRecentsComponent(this.mContext, false);
            }
            sAlternateRecents.onStart();
        }
        putComponent(RecentsComponent.class, this);
    }

    @Override
    protected void onBootCompleted() {
        if (this.mUseAlternateRecents && sAlternateRecents != null) {
            sAlternateRecents.onBootCompleted();
        }
        this.mBootCompleted = true;
    }

    @Override
    public void showRecents(boolean triggeredFromAltTab, View statusBarView) {
        if (this.mUseAlternateRecents) {
            sAlternateRecents.onShowRecents(triggeredFromAltTab);
        }
    }

    @Override
    public void hideRecents(boolean triggeredFromAltTab, boolean triggeredFromHomeKey) {
        if (this.mUseAlternateRecents) {
            sAlternateRecents.onHideRecents(triggeredFromAltTab, triggeredFromHomeKey);
            return;
        }
        Intent intent = new Intent("com.android.systemui.recent.action.CLOSE");
        intent.setPackage("com.android.systemui");
        sendBroadcastSafely(intent);
        RecentTasksLoader.getInstance(this.mContext).cancelPreloadingFirstTask();
    }

    @Override
    public void toggleRecents(Display display, int layoutDirection, View statusBarView) {
        Bitmap first;
        int x;
        int y;
        if (this.mUseAlternateRecents) {
            sAlternateRecents.onToggleRecents();
            return;
        }
        Log.d("Recents", "toggle recents panel");
        try {
            TaskDescription firstTask = RecentTasksLoader.getInstance(this.mContext).getFirstTask();
            Intent intent = new Intent("com.android.systemui.recent.action.TOGGLE_RECENTS");
            intent.setClassName("com.android.systemui", "com.android.systemui.recent.RecentsActivity");
            intent.setFlags(276824064);
            if (firstTask == null) {
                if (RecentsActivity.forceOpaqueBackground(this.mContext)) {
                    ActivityOptions opts = ActivityOptions.makeCustomAnimation(this.mContext, R.anim.recents_launch_from_launcher_enter, R.anim.recents_launch_from_launcher_exit);
                    this.mContext.startActivityAsUser(intent, opts.toBundle(), new UserHandle(-2));
                    return;
                } else {
                    this.mContext.startActivityAsUser(intent, new UserHandle(-2));
                    return;
                }
            }
            if (firstTask.getThumbnail() instanceof BitmapDrawable) {
                first = ((BitmapDrawable) firstTask.getThumbnail()).getBitmap();
            } else {
                first = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
                Drawable d = RecentTasksLoader.getInstance(this.mContext).getDefaultThumbnail();
                d.draw(new Canvas(first));
            }
            Resources res = this.mContext.getResources();
            float thumbWidth = res.getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_width);
            float thumbHeight = res.getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_height);
            if (first == null) {
                throw new RuntimeException("Recents thumbnail is null");
            }
            if ((first.getWidth() != thumbWidth || first.getHeight() != thumbHeight) && (first = Bitmap.createScaledBitmap(first, (int) thumbWidth, (int) thumbHeight, true)) == null) {
                throw new RuntimeException("Recents thumbnail is null");
            }
            DisplayMetrics dm = new DisplayMetrics();
            display.getMetrics(dm);
            Configuration config = res.getConfiguration();
            if (config.orientation == 1) {
                float appLabelLeftMargin = res.getDimensionPixelSize(R.dimen.status_bar_recents_app_label_left_margin);
                float appLabelWidth = res.getDimensionPixelSize(R.dimen.status_bar_recents_app_label_width);
                float thumbLeftMargin = res.getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_left_margin);
                float thumbBgPadding = res.getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_bg_padding);
                float width = appLabelLeftMargin + appLabelWidth + thumbLeftMargin + thumbWidth + (2.0f * thumbBgPadding);
                x = (int) (((dm.widthPixels - width) / 2.0f) + appLabelLeftMargin + appLabelWidth + thumbBgPadding + thumbLeftMargin);
                y = (int) ((dm.heightPixels - res.getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_height)) - thumbBgPadding);
                if (layoutDirection == 1) {
                    x = (dm.widthPixels - x) - res.getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_width);
                }
            } else {
                float thumbTopMargin = res.getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_top_margin);
                float thumbBgPadding2 = res.getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_bg_padding);
                float textPadding = res.getDimensionPixelSize(R.dimen.status_bar_recents_text_description_padding);
                float labelTextSize = res.getDimensionPixelSize(R.dimen.status_bar_recents_app_label_text_size);
                Paint p = new Paint();
                p.setTextSize(labelTextSize);
                float labelTextHeight = p.getFontMetricsInt().bottom - p.getFontMetricsInt().top;
                float descriptionTextSize = res.getDimensionPixelSize(R.dimen.status_bar_recents_app_description_text_size);
                p.setTextSize(descriptionTextSize);
                float descriptionTextHeight = p.getFontMetricsInt().bottom - p.getFontMetricsInt().top;
                float statusBarHeight = res.getDimensionPixelSize(android.R.dimen.accessibility_focus_highlight_stroke_width);
                float height = thumbTopMargin + thumbHeight + (2.0f * thumbBgPadding2) + textPadding + labelTextHeight + statusBarHeight + textPadding + descriptionTextHeight;
                float recentsItemRightPadding = res.getDimensionPixelSize(R.dimen.status_bar_recents_item_padding);
                float recentsScrollViewRightPadding = res.getDimensionPixelSize(R.dimen.status_bar_recents_right_glow_margin);
                x = (int) ((((dm.widthPixels - res.getDimensionPixelSize(R.dimen.status_bar_recents_thumbnail_width)) - thumbBgPadding2) - recentsItemRightPadding) - recentsScrollViewRightPadding);
                y = (int) ((((dm.heightPixels - statusBarHeight) - height) / 2.0f) + thumbTopMargin + statusBarHeight + thumbBgPadding2 + statusBarHeight);
            }
            ActivityOptions opts2 = ActivityOptions.makeThumbnailScaleDownAnimation(statusBarView, first, x, y, new ActivityOptions.OnAnimationStartedListener() {
                public void onAnimationStarted() {
                    Intent intent2 = new Intent("com.android.systemui.recent.action.WINDOW_ANIMATION_START");
                    intent2.setPackage("com.android.systemui");
                    Recents.this.sendBroadcastSafely(intent2);
                }
            });
            intent.putExtra("com.android.systemui.recent.WAITING_FOR_WINDOW_ANIMATION", true);
            startActivitySafely(intent, opts2.toBundle());
        } catch (ActivityNotFoundException e) {
            Log.e("Recents", "Failed to launch RecentAppsIntent", e);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if (this.mUseAlternateRecents) {
            sAlternateRecents.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void preloadRecents() {
        if (this.mUseAlternateRecents) {
            sAlternateRecents.onPreloadRecents();
            return;
        }
        Intent intent = new Intent("com.android.systemui.recent.action.PRELOAD");
        intent.setClassName("com.android.systemui", "com.android.systemui.recent.RecentsPreloadReceiver");
        sendBroadcastSafely(intent);
        RecentTasksLoader.getInstance(this.mContext).preloadFirstTask();
    }

    @Override
    public void cancelPreloadingRecents() {
        if (this.mUseAlternateRecents) {
            sAlternateRecents.onCancelPreloadingRecents();
            return;
        }
        Intent intent = new Intent("com.android.systemui.recent.CANCEL_PRELOAD");
        intent.setClassName("com.android.systemui", "com.android.systemui.recent.RecentsPreloadReceiver");
        sendBroadcastSafely(intent);
        RecentTasksLoader.getInstance(this.mContext).cancelPreloadingFirstTask();
    }

    @Override
    public void showNextAffiliatedTask() {
        if (this.mUseAlternateRecents) {
            sAlternateRecents.onShowNextAffiliatedTask();
        }
    }

    @Override
    public void showPrevAffiliatedTask() {
        if (this.mUseAlternateRecents) {
            sAlternateRecents.onShowPrevAffiliatedTask();
        }
    }

    @Override
    public void setCallback(RecentsComponent.Callbacks cb) {
        if (this.mUseAlternateRecents) {
            sAlternateRecents.setRecentsComponentCallback(cb);
        }
    }

    public void sendBroadcastSafely(Intent intent) {
        if (this.mBootCompleted) {
            this.mContext.sendBroadcastAsUser(intent, new UserHandle(-2));
        }
    }

    private void startActivitySafely(Intent intent, Bundle opts) {
        if (this.mBootCompleted) {
            this.mContext.startActivityAsUser(intent, opts, new UserHandle(-2));
        }
    }
}
