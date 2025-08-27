package com.android.systemui.statusbar;

import android.R;
import android.content.Context;
import android.util.Log;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/* loaded from: classes.dex */
public class NotificationBlockingHelperManager {
    private ExpandableNotificationRow mBlockingHelperRow;
    private final Context mContext;
    private boolean mIsShadeExpanded;
    private Set<String> mNonBlockablePkgs = new HashSet();

    public NotificationBlockingHelperManager(Context context) {
        this.mContext = context;
        Collections.addAll(this.mNonBlockablePkgs, this.mContext.getResources().getStringArray(R.array.config_defaultCloudSearchServices));
    }

    boolean perhapsShowBlockingHelper(ExpandableNotificationRow expandableNotificationRow, NotificationMenuRowPlugin notificationMenuRowPlugin) {
        if (expandableNotificationRow.getEntry().userSentiment != -1 || !this.mIsShadeExpanded || expandableNotificationRow.getIsNonblockable() || (expandableNotificationRow.isChildInGroup() && !expandableNotificationRow.isOnlyChildInGroup())) {
            return false;
        }
        dismissCurrentBlockingHelper();
        NotificationGutsManager notificationGutsManager = (NotificationGutsManager) Dependency.get(NotificationGutsManager.class);
        this.mBlockingHelperRow = expandableNotificationRow;
        this.mBlockingHelperRow.setBlockingHelperShowing(true);
        notificationGutsManager.openGuts(this.mBlockingHelperRow, 0, 0, notificationMenuRowPlugin.getLongpressMenuItem(this.mContext));
        ((MetricsLogger) Dependency.get(MetricsLogger.class)).count("blocking_helper_shown", 1);
        return true;
    }

    boolean dismissCurrentBlockingHelper() {
        if (isBlockingHelperRowNull()) {
            return false;
        }
        if (!this.mBlockingHelperRow.isBlockingHelperShowing()) {
            Log.e("BlockingHelper", "Manager.dismissCurrentBlockingHelper: Non-null row is not showing a blocking helper");
        }
        this.mBlockingHelperRow.setBlockingHelperShowing(false);
        if (this.mBlockingHelperRow.isAttachedToWindow()) {
            ((NotificationEntryManager) Dependency.get(NotificationEntryManager.class)).updateNotifications();
        }
        this.mBlockingHelperRow = null;
        return true;
    }

    public void setNotificationShadeExpanded(float f) {
        this.mIsShadeExpanded = f > 0.0f;
    }

    public boolean isNonblockablePackage(String str) {
        return this.mNonBlockablePkgs.contains(str);
    }

    boolean isBlockingHelperRowNull() {
        return this.mBlockingHelperRow == null;
    }

    void setBlockingHelperRowForTest(ExpandableNotificationRow expandableNotificationRow) {
        this.mBlockingHelperRow = expandableNotificationRow;
    }
}
