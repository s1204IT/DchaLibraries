package com.android.systemui.statusbar;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationGuts;

public class AppOpsInfo extends LinearLayout implements NotificationGuts.GutsContent {
    private String mAppName;
    private ArraySet<Integer> mAppOps;
    private int mAppUid;
    private NotificationGuts mGutsContainer;
    private MetricsLogger mMetricsLogger;
    private View.OnClickListener mOnOk;
    private OnSettingsClickListener mOnSettingsClickListener;
    private String mPkg;
    private PackageManager mPm;
    private StatusBarNotification mSbn;

    public interface OnSettingsClickListener {
        void onClick(View view, String str, int i, ArraySet<Integer> arraySet);
    }

    public AppOpsInfo(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mOnOk = new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                this.f$0.closeControls(view);
            }
        };
    }

    public void bindGuts(PackageManager packageManager, OnSettingsClickListener onSettingsClickListener, StatusBarNotification statusBarNotification, ArraySet<Integer> arraySet) {
        this.mPkg = statusBarNotification.getPackageName();
        this.mSbn = statusBarNotification;
        this.mPm = packageManager;
        this.mAppName = this.mPkg;
        this.mOnSettingsClickListener = onSettingsClickListener;
        this.mAppOps = arraySet;
        bindHeader();
        bindPrompt();
        bindButtons();
        this.mMetricsLogger = new MetricsLogger();
        this.mMetricsLogger.visibility(1345, true);
    }

    private void bindHeader() {
        Drawable defaultActivityIcon;
        try {
            ApplicationInfo applicationInfo = this.mPm.getApplicationInfo(this.mPkg, 795136);
            if (applicationInfo != null) {
                this.mAppUid = this.mSbn.getUid();
                this.mAppName = String.valueOf(this.mPm.getApplicationLabel(applicationInfo));
                defaultActivityIcon = this.mPm.getApplicationIcon(applicationInfo);
            } else {
                defaultActivityIcon = null;
            }
        } catch (PackageManager.NameNotFoundException e) {
            defaultActivityIcon = this.mPm.getDefaultActivityIcon();
        }
        ((ImageView) findViewById(R.id.pkgicon)).setImageDrawable(defaultActivityIcon);
        ((TextView) findViewById(R.id.pkgname)).setText(this.mAppName);
    }

    private void bindPrompt() {
        ((TextView) findViewById(R.id.prompt)).setText(getPrompt());
    }

    private void bindButtons() {
        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                AppOpsInfo appOpsInfo = this.f$0;
                appOpsInfo.mOnSettingsClickListener.onClick(view, appOpsInfo.mPkg, appOpsInfo.mAppUid, appOpsInfo.mAppOps);
            }
        });
        ((TextView) findViewById(R.id.ok)).setOnClickListener(this.mOnOk);
    }

    private String getPrompt() {
        if (this.mAppOps == null || this.mAppOps.size() == 0) {
            return "";
        }
        if (this.mAppOps.size() == 1) {
            if (this.mAppOps.contains(26)) {
                return this.mContext.getString(R.string.appops_camera);
            }
            if (this.mAppOps.contains(27)) {
                return this.mContext.getString(R.string.appops_microphone);
            }
            return this.mContext.getString(R.string.appops_overlay);
        }
        if (this.mAppOps.size() == 2) {
            if (this.mAppOps.contains(26)) {
                if (this.mAppOps.contains(27)) {
                    return this.mContext.getString(R.string.appops_camera_mic);
                }
                return this.mContext.getString(R.string.appops_camera_overlay);
            }
            return this.mContext.getString(R.string.appops_mic_overlay);
        }
        return this.mContext.getString(R.string.appops_camera_mic_overlay);
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        if (this.mGutsContainer != null && accessibilityEvent.getEventType() == 32) {
            if (this.mGutsContainer.isExposed()) {
                accessibilityEvent.getText().add(this.mContext.getString(R.string.notification_channel_controls_opened_accessibility, this.mAppName));
            } else {
                accessibilityEvent.getText().add(this.mContext.getString(R.string.notification_channel_controls_closed_accessibility, this.mAppName));
            }
        }
    }

    public void closeControls(View view) {
        this.mMetricsLogger.visibility(1345, false);
        int[] iArr = new int[2];
        int[] iArr2 = new int[2];
        this.mGutsContainer.getLocationOnScreen(iArr);
        view.getLocationOnScreen(iArr2);
        int width = view.getWidth() / 2;
        int height = view.getHeight() / 2;
        this.mGutsContainer.closeControls((iArr2[0] - iArr[0]) + width, (iArr2[1] - iArr[1]) + height, false, false);
    }

    @Override
    public void setGutsParent(NotificationGuts notificationGuts) {
        this.mGutsContainer = notificationGuts;
    }

    @Override
    public boolean willBeRemoved() {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public View getContentView() {
        return this;
    }

    @Override
    public boolean handleCloseControls(boolean z, boolean z2) {
        return false;
    }

    @Override
    public int getActualHeight() {
        return getHeight();
    }
}
