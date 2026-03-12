package com.android.systemui.recents;

import android.R;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import com.android.systemui.recents.Constants;
import com.android.systemui.recents.misc.Console;
import com.android.systemui.recents.misc.SystemServicesProxy;

public class RecentsConfiguration {
    static RecentsConfiguration sInstance;
    static int sPrevConfigurationHashCode;
    public int altTabKeyDelay;
    public float animationPxMovementPerSecond;
    public boolean debugModeEnabled;
    public boolean developerOptionsEnabled;
    public boolean fakeShadows;
    public Interpolator fastOutLinearInInterpolator;
    public Interpolator fastOutSlowInInterpolator;
    public int filteringCurrentViewsAnimDuration;
    public int filteringNewViewsAnimDuration;
    boolean hasTransposedNavBar;
    boolean hasTransposedSearchBar;
    boolean isLandscape;
    public boolean launchedFromAppWithThumbnail;
    public boolean launchedFromHome;
    public boolean launchedFromSearchHome;
    public boolean launchedHasConfigurationChanged;
    public int launchedNumVisibleTasks;
    public int launchedNumVisibleThumbnails;
    public boolean launchedReuseTaskStackViews;
    public int launchedToTaskId;
    public boolean launchedWithAltTab;
    public boolean launchedWithNoRecentTasks;
    public Interpolator linearOutSlowInInterpolator;
    public boolean lockToAppEnabled;
    public int maxNumTasksToLoad;
    public int navBarScrimEnterDuration;
    public Interpolator quintOutInterpolator;
    public int searchBarSpaceHeightPx;
    public int svelteLevel;
    public int taskBarDismissDozeDelaySeconds;
    public int taskBarHeight;
    public float taskBarViewAffiliationColorMinAlpha;
    public int taskBarViewDarkTextColor;
    public int taskBarViewDefaultBackgroundColor;
    public int taskBarViewHighlightColor;
    public int taskBarViewLightTextColor;
    public int taskStackMaxDim;
    public float taskStackOverscrollPct;
    public int taskStackScrollDuration;
    public int taskStackTopPaddingPx;
    public float taskStackWidthPaddingPct;
    public int taskViewAffiliateGroupEnterOffsetPx;
    public int taskViewEnterFromAppDuration;
    public int taskViewEnterFromHomeDuration;
    public int taskViewEnterFromHomeStaggerDelay;
    public int taskViewExitToAppDuration;
    public int taskViewExitToHomeDuration;
    public int taskViewHighlightPx;
    public int taskViewRemoveAnimDuration;
    public int taskViewRemoveAnimTranslationXPx;
    public int taskViewRoundedCornerRadiusPx;
    public float taskViewThumbnailAlpha;
    public int taskViewTranslationZMaxPx;
    public int taskViewTranslationZMinPx;
    public int transitionEnterFromAppDelay;
    public int transitionEnterFromHomeDelay;
    public boolean useHardwareLayers;
    public Rect systemInsets = new Rect();
    public Rect displayRect = new Rect();
    int searchBarAppWidgetId = -1;

    private RecentsConfiguration(Context context) {
        this.fastOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_slow_in);
        this.fastOutLinearInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.fast_out_linear_in);
        this.linearOutSlowInInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.linear_out_slow_in);
        this.quintOutInterpolator = AnimationUtils.loadInterpolator(context, R.interpolator.decelerate_quint);
    }

    public static RecentsConfiguration reinitialize(Context context, SystemServicesProxy ssp) {
        if (sInstance == null) {
            sInstance = new RecentsConfiguration(context);
        }
        int configHashCode = context.getResources().getConfiguration().hashCode();
        if (sPrevConfigurationHashCode != configHashCode) {
            sInstance.update(context);
            sPrevConfigurationHashCode = configHashCode;
        }
        sInstance.updateOnReinitialize(context, ssp);
        return sInstance;
    }

    public static RecentsConfiguration getInstance() {
        return sInstance;
    }

    void update(Context context) {
        SharedPreferences settings = context.getSharedPreferences(context.getPackageName(), 0);
        Resources res = context.getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        this.debugModeEnabled = settings.getBoolean(Constants.Values.App.Key_DebugModeEnabled, false);
        if (this.debugModeEnabled) {
            Console.Enabled = true;
        }
        this.isLandscape = res.getConfiguration().orientation == 2;
        this.hasTransposedSearchBar = res.getBoolean(com.android.systemui.R.bool.recents_has_transposed_search_bar);
        this.hasTransposedNavBar = res.getBoolean(com.android.systemui.R.bool.recents_has_transposed_nav_bar);
        this.displayRect.set(0, 0, dm.widthPixels, dm.heightPixels);
        this.animationPxMovementPerSecond = res.getDimensionPixelSize(com.android.systemui.R.dimen.recents_animation_movement_in_dps_per_second);
        this.filteringCurrentViewsAnimDuration = res.getInteger(com.android.systemui.R.integer.recents_filter_animate_current_views_duration);
        this.filteringNewViewsAnimDuration = res.getInteger(com.android.systemui.R.integer.recents_filter_animate_new_views_duration);
        this.maxNumTasksToLoad = ActivityManager.getMaxRecentTasksStatic();
        this.searchBarSpaceHeightPx = res.getDimensionPixelSize(com.android.systemui.R.dimen.recents_search_bar_space_height);
        this.searchBarAppWidgetId = settings.getInt(Constants.Values.App.Key_SearchAppWidgetId, -1);
        this.taskStackScrollDuration = res.getInteger(com.android.systemui.R.integer.recents_animate_task_stack_scroll_duration);
        TypedValue widthPaddingPctValue = new TypedValue();
        res.getValue(com.android.systemui.R.dimen.recents_stack_width_padding_percentage, widthPaddingPctValue, true);
        this.taskStackWidthPaddingPct = widthPaddingPctValue.getFloat();
        TypedValue stackOverscrollPctValue = new TypedValue();
        res.getValue(com.android.systemui.R.dimen.recents_stack_overscroll_percentage, stackOverscrollPctValue, true);
        this.taskStackOverscrollPct = stackOverscrollPctValue.getFloat();
        this.taskStackMaxDim = res.getInteger(com.android.systemui.R.integer.recents_max_task_stack_view_dim);
        this.taskStackTopPaddingPx = res.getDimensionPixelSize(com.android.systemui.R.dimen.recents_stack_top_padding);
        this.transitionEnterFromAppDelay = res.getInteger(com.android.systemui.R.integer.recents_enter_from_app_transition_duration);
        this.transitionEnterFromHomeDelay = res.getInteger(com.android.systemui.R.integer.recents_enter_from_home_transition_duration);
        this.taskViewEnterFromAppDuration = res.getInteger(com.android.systemui.R.integer.recents_task_enter_from_app_duration);
        this.taskViewEnterFromHomeDuration = res.getInteger(com.android.systemui.R.integer.recents_task_enter_from_home_duration);
        this.taskViewEnterFromHomeStaggerDelay = res.getInteger(com.android.systemui.R.integer.recents_task_enter_from_home_stagger_delay);
        this.taskViewExitToAppDuration = res.getInteger(com.android.systemui.R.integer.recents_task_exit_to_app_duration);
        this.taskViewExitToHomeDuration = res.getInteger(com.android.systemui.R.integer.recents_task_exit_to_home_duration);
        this.taskViewRemoveAnimDuration = res.getInteger(com.android.systemui.R.integer.recents_animate_task_view_remove_duration);
        this.taskViewRemoveAnimTranslationXPx = res.getDimensionPixelSize(com.android.systemui.R.dimen.recents_task_view_remove_anim_translation_x);
        this.taskViewRoundedCornerRadiusPx = res.getDimensionPixelSize(com.android.systemui.R.dimen.recents_task_view_rounded_corners_radius);
        this.taskViewHighlightPx = res.getDimensionPixelSize(com.android.systemui.R.dimen.recents_task_view_highlight);
        this.taskViewTranslationZMinPx = res.getDimensionPixelSize(com.android.systemui.R.dimen.recents_task_view_z_min);
        this.taskViewTranslationZMaxPx = res.getDimensionPixelSize(com.android.systemui.R.dimen.recents_task_view_z_max);
        this.taskViewAffiliateGroupEnterOffsetPx = res.getDimensionPixelSize(com.android.systemui.R.dimen.recents_task_view_affiliate_group_enter_offset);
        TypedValue thumbnailAlphaValue = new TypedValue();
        res.getValue(com.android.systemui.R.dimen.recents_task_view_thumbnail_alpha, thumbnailAlphaValue, true);
        this.taskViewThumbnailAlpha = thumbnailAlphaValue.getFloat();
        this.taskBarViewDefaultBackgroundColor = res.getColor(com.android.systemui.R.color.recents_task_bar_default_background_color);
        this.taskBarViewLightTextColor = res.getColor(com.android.systemui.R.color.recents_task_bar_light_text_color);
        this.taskBarViewDarkTextColor = res.getColor(com.android.systemui.R.color.recents_task_bar_dark_text_color);
        this.taskBarViewHighlightColor = res.getColor(com.android.systemui.R.color.recents_task_bar_highlight_color);
        TypedValue affMinAlphaPctValue = new TypedValue();
        res.getValue(com.android.systemui.R.dimen.recents_task_affiliation_color_min_alpha_percentage, affMinAlphaPctValue, true);
        this.taskBarViewAffiliationColorMinAlpha = affMinAlphaPctValue.getFloat();
        this.taskBarHeight = res.getDimensionPixelSize(com.android.systemui.R.dimen.recents_task_bar_height);
        this.taskBarDismissDozeDelaySeconds = res.getInteger(com.android.systemui.R.integer.recents_task_bar_dismiss_delay_seconds);
        this.navBarScrimEnterDuration = res.getInteger(com.android.systemui.R.integer.recents_nav_bar_scrim_enter_duration);
        this.useHardwareLayers = res.getBoolean(com.android.systemui.R.bool.config_recents_use_hardware_layers);
        this.altTabKeyDelay = res.getInteger(com.android.systemui.R.integer.recents_alt_tab_key_delay);
        this.fakeShadows = res.getBoolean(com.android.systemui.R.bool.config_recents_fake_shadows);
        this.svelteLevel = res.getInteger(com.android.systemui.R.integer.recents_svelte_level);
    }

    public void updateSystemInsets(Rect insets) {
        this.systemInsets.set(insets);
    }

    public void updateSearchBarAppWidgetId(Context context, int appWidgetId) {
        this.searchBarAppWidgetId = appWidgetId;
        SharedPreferences settings = context.getSharedPreferences(context.getPackageName(), 0);
        settings.edit().putInt(Constants.Values.App.Key_SearchAppWidgetId, appWidgetId).apply();
    }

    void updateOnReinitialize(Context context, SystemServicesProxy ssp) {
        this.developerOptionsEnabled = ssp.getGlobalSetting(context, "development_settings_enabled") != 0;
        this.lockToAppEnabled = ssp.getSystemSetting(context, "lock_to_app_enabled") != 0;
    }

    public void updateOnConfigurationChange() {
        this.launchedReuseTaskStackViews = false;
        this.launchedHasConfigurationChanged = true;
    }

    public boolean hasSearchBarAppWidget() {
        return this.searchBarAppWidgetId >= 0;
    }

    public boolean shouldAnimateStatusBarScrim() {
        return this.launchedFromHome;
    }

    public boolean hasStatusBarScrim() {
        return !this.launchedWithNoRecentTasks;
    }

    public boolean shouldAnimateNavBarScrim() {
        return true;
    }

    public boolean hasNavBarScrim() {
        return (this.launchedWithNoRecentTasks || (this.hasTransposedNavBar && this.isLandscape)) ? false : true;
    }

    public void getTaskStackBounds(int windowWidth, int windowHeight, int topInset, int rightInset, Rect taskStackBounds) {
        Rect searchBarBounds = new Rect();
        getSearchBarBounds(windowWidth, windowHeight, topInset, searchBarBounds);
        if (this.isLandscape && this.hasTransposedSearchBar) {
            taskStackBounds.set(0, topInset, windowWidth - rightInset, windowHeight);
        } else {
            taskStackBounds.set(0, searchBarBounds.bottom, windowWidth, windowHeight);
        }
    }

    public void getSearchBarBounds(int windowWidth, int windowHeight, int topInset, Rect searchBarSpaceBounds) {
        int searchBarSize = this.searchBarSpaceHeightPx;
        if (!hasSearchBarAppWidget()) {
            searchBarSize = 0;
        }
        if (this.isLandscape && this.hasTransposedSearchBar) {
            searchBarSpaceBounds.set(0, topInset, searchBarSize, windowHeight);
        } else {
            searchBarSpaceBounds.set(0, topInset, windowWidth, topInset + searchBarSize);
        }
    }
}
