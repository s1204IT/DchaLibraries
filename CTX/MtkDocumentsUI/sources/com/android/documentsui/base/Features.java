package com.android.documentsui.base;

import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.os.UserManager;
import android.util.SparseBooleanArray;
import com.android.documentsui.R;

public interface Features {
    public static final boolean OMC_RUNTIME;

    void forceFeature(int i, boolean z);

    boolean isArchiveCreationEnabled();

    boolean isCommandInterceptorEnabled();

    boolean isContentPagingEnabled();

    boolean isContentRefreshEnabled();

    boolean isDebugSupportEnabled();

    boolean isFoldersInSearchResultsEnabled();

    boolean isGestureScaleEnabled();

    boolean isInspectorEnabled();

    boolean isJobProgressDialogEnabled();

    boolean isLaunchToDocumentEnabled();

    boolean isLauncherEnabled();

    boolean isNotificationChannelEnabled();

    boolean isOverwriteConfirmationEnabled();

    boolean isRemoteActionsEnabled();

    boolean isSystemKeyboardNavigationEnabled();

    boolean isVirtualFilesSharingEnabled();

    static {
        OMC_RUNTIME = Build.VERSION.SDK_INT > 25;
    }

    static Features create(Context context) {
        return new RuntimeFeatures(context.getResources(), UserManager.get(context));
    }

    public static final class RuntimeFeatures implements Features {
        static final boolean $assertionsDisabled = false;
        private final SparseBooleanArray mDebugEnabled = new SparseBooleanArray();
        private final Resources mRes;
        private final UserManager mUserMgr;

        public RuntimeFeatures(Resources resources, UserManager userManager) {
            this.mRes = resources;
            this.mUserMgr = userManager;
        }

        @Override
        public void forceFeature(int i, boolean z) {
            this.mDebugEnabled.put(i, z);
        }

        private boolean isEnabled(int i) {
            return this.mDebugEnabled.get(i, this.mRes.getBoolean(i));
        }

        @Override
        public boolean isArchiveCreationEnabled() {
            return isEnabled(R.bool.feature_archive_creation);
        }

        @Override
        public boolean isCommandInterceptorEnabled() {
            return isEnabled(R.bool.feature_command_interceptor);
        }

        @Override
        public boolean isContentPagingEnabled() {
            return isEnabled(R.bool.feature_content_paging);
        }

        @Override
        public boolean isContentRefreshEnabled() {
            return isEnabled(R.bool.feature_content_refresh);
        }

        private boolean isFunPolicyEnabled() {
            return !this.mUserMgr.hasUserRestriction("no_fun");
        }

        private boolean isDebugPolicyEnabled() {
            return !this.mUserMgr.hasUserRestriction("no_debugging_features");
        }

        @Override
        public boolean isDebugSupportEnabled() {
            return isDebugPolicyEnabled() && isFunPolicyEnabled();
        }

        @Override
        public boolean isFoldersInSearchResultsEnabled() {
            return isEnabled(R.bool.feature_folders_in_search_results);
        }

        @Override
        public boolean isGestureScaleEnabled() {
            return isEnabled(R.bool.feature_gesture_scale);
        }

        @Override
        public boolean isInspectorEnabled() {
            return isEnabled(R.bool.feature_inspector);
        }

        @Override
        public boolean isJobProgressDialogEnabled() {
            return isEnabled(R.bool.feature_job_progress_dialog);
        }

        @Override
        public boolean isLaunchToDocumentEnabled() {
            return isEnabled(R.bool.feature_launch_to_document);
        }

        @Override
        public boolean isNotificationChannelEnabled() {
            return isEnabled(R.bool.feature_notification_channel);
        }

        @Override
        public boolean isOverwriteConfirmationEnabled() {
            return isEnabled(R.bool.feature_overwrite_confirmation);
        }

        @Override
        public boolean isRemoteActionsEnabled() {
            return isEnabled(R.bool.feature_remote_actions);
        }

        @Override
        public boolean isSystemKeyboardNavigationEnabled() {
            return isEnabled(R.bool.feature_system_keyboard_navigation);
        }

        @Override
        public boolean isVirtualFilesSharingEnabled() {
            return isEnabled(R.bool.feature_virtual_files_sharing);
        }

        @Override
        public boolean isLauncherEnabled() {
            return isEnabled(R.bool.is_launcher_enabled);
        }
    }
}
