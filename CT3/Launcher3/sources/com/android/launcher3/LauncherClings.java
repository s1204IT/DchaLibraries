package com.android.launcher3;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.annotation.TargetApi;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityManager;

class LauncherClings implements View.OnClickListener {
    private LayoutInflater mInflater;
    boolean mIsVisible;
    Launcher mLauncher;

    public LauncherClings(Launcher launcher) {
        this.mLauncher = launcher;
        this.mInflater = LayoutInflater.from(this.mLauncher);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.cling_dismiss_migration_use_default) {
            dismissMigrationCling();
            return;
        }
        if (id == R.id.cling_dismiss_migration_copy_apps) {
            LauncherModel model = this.mLauncher.getModel();
            model.resetLoadedState(false, true);
            model.startLoader(-1001, 3);
            SharedPreferences.Editor editor = Utilities.getPrefs(this.mLauncher).edit();
            editor.putBoolean("launcher.user_migrated_from_old_data", true);
            editor.apply();
            dismissMigrationCling();
            return;
        }
        if (id != R.id.cling_dismiss_longpress_info) {
            return;
        }
        dismissLongPressCling();
    }

    public void showMigrationCling() {
        this.mIsVisible = true;
        this.mLauncher.hideWorkspaceSearchAndHotseat();
        ViewGroup root = (ViewGroup) this.mLauncher.findViewById(R.id.launcher);
        View inflated = this.mInflater.inflate(R.layout.migration_cling, root);
        inflated.findViewById(R.id.cling_dismiss_migration_copy_apps).setOnClickListener(this);
        inflated.findViewById(R.id.cling_dismiss_migration_use_default).setOnClickListener(this);
    }

    private void dismissMigrationCling() {
        this.mLauncher.showWorkspaceSearchAndHotseat();
        Runnable dismissCb = new Runnable() {
            @Override
            public void run() {
                Runnable cb = new Runnable() {
                    @Override
                    public void run() {
                        LauncherClings.this.showLongPressCling(false);
                    }
                };
                LauncherClings.this.dismissCling(LauncherClings.this.mLauncher.findViewById(R.id.migration_cling), cb, "cling_gel.migration.dismissed", 200);
            }
        };
        this.mLauncher.getWorkspace().post(dismissCb);
    }

    public void showLongPressCling(boolean showWelcome) {
        this.mIsVisible = true;
        ViewGroup root = (ViewGroup) this.mLauncher.findViewById(R.id.launcher);
        View cling = this.mInflater.inflate(R.layout.longpress_cling, root, false);
        cling.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                LauncherClings.this.mLauncher.showOverviewMode(true);
                LauncherClings.this.dismissLongPressCling();
                return true;
            }
        });
        final ViewGroup content = (ViewGroup) cling.findViewById(R.id.cling_content);
        this.mInflater.inflate(showWelcome ? R.layout.longpress_cling_welcome_content : R.layout.longpress_cling_content, content);
        content.findViewById(R.id.cling_dismiss_longpress_info).setOnClickListener(this);
        if ("crop_bg_top_and_sides".equals(content.getTag())) {
            Drawable bg = new BorderCropDrawable(this.mLauncher.getResources().getDrawable(R.drawable.cling_bg), true, true, true, false);
            content.setBackground(bg);
        }
        root.addView(cling);
        if (showWelcome) {
            return;
        }
        content.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                ObjectAnimator anim;
                content.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                if ("crop_bg_top_and_sides".equals(content.getTag())) {
                    content.setTranslationY(-content.getMeasuredHeight());
                    anim = LauncherAnimUtils.ofFloat(content, "translationY", 0.0f);
                } else {
                    content.setScaleX(0.0f);
                    content.setScaleY(0.0f);
                    PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat("scaleX", 1.0f);
                    PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat("scaleY", 1.0f);
                    anim = LauncherAnimUtils.ofPropertyValuesHolder(content, scaleX, scaleY);
                }
                anim.setDuration(250L);
                anim.setInterpolator(new LogDecelerateInterpolator(100, 0));
                anim.start();
            }
        });
    }

    void dismissLongPressCling() {
        Runnable dismissCb = new Runnable() {
            @Override
            public void run() {
                LauncherClings.this.dismissCling(LauncherClings.this.mLauncher.findViewById(R.id.longpress_cling), null, "cling_gel.workspace.dismissed", 200);
            }
        };
        this.mLauncher.getWorkspace().post(dismissCb);
    }

    void dismissCling(final View cling, final Runnable postAnimationCb, final String flag, int duration) {
        if (cling == null || cling.getVisibility() == 8) {
            return;
        }
        Runnable cleanUpClingCb = new Runnable() {
            @Override
            public void run() {
                cling.setVisibility(8);
                LauncherClings.this.mLauncher.getSharedPrefs().edit().putBoolean(flag, true).apply();
                LauncherClings.this.mIsVisible = false;
                if (postAnimationCb == null) {
                    return;
                }
                postAnimationCb.run();
            }
        };
        if (duration <= 0) {
            cleanUpClingCb.run();
        } else {
            cling.animate().alpha(0.0f).setDuration(duration).withEndAction(cleanUpClingCb);
        }
    }

    public boolean isVisible() {
        return this.mIsVisible;
    }

    @TargetApi(18)
    private boolean areClingsEnabled() {
        if (ActivityManager.isRunningInTestHarness()) {
            return false;
        }
        AccessibilityManager a11yManager = (AccessibilityManager) this.mLauncher.getSystemService("accessibility");
        if (a11yManager.isTouchExplorationEnabled()) {
            return false;
        }
        if (Utilities.ATLEAST_JB_MR2) {
            UserManager um = (UserManager) this.mLauncher.getSystemService("user");
            Bundle restrictions = um.getUserRestrictions();
            if (restrictions.getBoolean("no_modify_accounts", false)) {
                return false;
            }
        }
        return Settings.Secure.getInt(this.mLauncher.getContentResolver(), "skip_first_use_hints", 0) != 1;
    }

    public boolean shouldShowFirstRunOrMigrationClings() {
        SharedPreferences sharedPrefs = this.mLauncher.getSharedPrefs();
        return (!areClingsEnabled() || sharedPrefs.getBoolean("cling_gel.workspace.dismissed", false) || sharedPrefs.getBoolean("cling_gel.migration.dismissed", false)) ? false : true;
    }

    public static void markFirstRunClingDismissed(Context ctx) {
        Utilities.getPrefs(ctx).edit().putBoolean("cling_gel.workspace.dismissed", true).apply();
    }
}
