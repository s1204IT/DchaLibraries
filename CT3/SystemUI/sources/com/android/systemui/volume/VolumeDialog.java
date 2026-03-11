package com.android.systemui.volume;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.os.Debug;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.tuner.TunerZenModePanel;
import com.android.systemui.volume.VolumeDialogController;
import com.android.systemui.volume.VolumeDialogMotion;
import com.android.systemui.volume.ZenModePanel;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class VolumeDialog implements TunerService.Tunable {
    private static final String TAG = Util.logTag(VolumeDialog.class);
    private final AccessibilityManager mAccessibilityMgr;
    private int mActiveStream;
    private final AudioManager mAudioManager;
    private Callback mCallback;
    private long mCollapseTime;
    private final Context mContext;
    private final VolumeDialogController mController;
    private int mDensity;
    private CustomDialog mDialog;
    private ViewGroup mDialogContentView;
    private ViewGroup mDialogView;
    private ImageButton mExpandButton;
    private int mExpandButtonAnimationDuration;
    private boolean mExpandButtonAnimationRunning;
    private boolean mExpanded;
    private final KeyguardManager mKeyguard;
    private LayoutTransition mLayoutTransition;
    private VolumeDialogMotion mMotion;
    private boolean mPendingRecheckAll;
    private boolean mPendingStateChanged;
    private SafetyWarningDialog mSafetyWarning;
    private boolean mShowFullZen;
    private boolean mShowing;
    private SpTexts mSpTexts;
    private VolumeDialogController.State mState;
    private final int mWindowType;
    private ZenFooter mZenFooter;
    private final ZenModeController mZenModeController;
    private TunerZenModePanel mZenPanel;
    private final H mHandler = new H();
    private final List<VolumeRow> mRows = new ArrayList();
    private final SparseBooleanArray mDynamic = new SparseBooleanArray();
    private final Object mSafetyWarningLock = new Object();
    private final Accessibility mAccessibility = new Accessibility(this, null);
    private boolean mShowHeaders = false;
    private boolean mAutomute = true;
    private boolean mSilentMode = true;
    private boolean mHovering = false;
    private final VolumeDialogController.Callbacks mControllerCallbackH = new VolumeDialogController.Callbacks() {
        @Override
        public void onShowRequested(int reason) {
            VolumeDialog.this.showH(reason);
        }

        @Override
        public void onDismissRequested(int reason) {
            VolumeDialog.this.dismissH(reason);
        }

        @Override
        public void onScreenOff() {
            VolumeDialog.this.dismissH(4);
        }

        @Override
        public void onStateChanged(VolumeDialogController.State state) {
            VolumeDialog.this.onStateChangedH(state);
        }

        @Override
        public void onLayoutDirectionChanged(int layoutDirection) {
            VolumeDialog.this.mDialogView.setLayoutDirection(layoutDirection);
        }

        @Override
        public void onConfigurationChanged() {
            Configuration newConfig = VolumeDialog.this.mContext.getResources().getConfiguration();
            int density = newConfig.densityDpi;
            if (density != VolumeDialog.this.mDensity) {
                VolumeDialog.this.mDialog.dismiss();
                VolumeDialog.this.mZenFooter.cleanup();
                VolumeDialog.this.initDialog();
            }
            VolumeDialog.this.updateWindowWidthH();
            VolumeDialog.this.mSpTexts.update();
            VolumeDialog.this.mZenFooter.onConfigurationChanged();
        }

        @Override
        public void onShowVibrateHint() {
            if (!VolumeDialog.this.mSilentMode) {
                return;
            }
            VolumeDialog.this.mController.setRingerMode(0, false);
        }

        @Override
        public void onShowSilentHint() {
            if (!VolumeDialog.this.mSilentMode) {
                return;
            }
            VolumeDialog.this.mController.setRingerMode(2, false);
        }

        @Override
        public void onShowSafetyWarning(int flags) {
            VolumeDialog.this.showSafetyWarningH(flags);
        }
    };
    private final ZenModePanel.Callback mZenPanelCallback = new ZenModePanel.Callback() {
        @Override
        public void onPrioritySettings() {
            VolumeDialog.this.mCallback.onZenPrioritySettingsClicked();
        }

        @Override
        public void onInteraction() {
            VolumeDialog.this.mHandler.sendEmptyMessage(6);
        }

        @Override
        public void onExpanded(boolean expanded) {
        }
    };
    private final View.OnClickListener mClickExpand = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (VolumeDialog.this.mExpandButtonAnimationRunning) {
                return;
            }
            boolean newExpand = !VolumeDialog.this.mExpanded;
            Events.writeEvent(VolumeDialog.this.mContext, 3, Boolean.valueOf(newExpand));
            VolumeDialog.this.setExpandedH(newExpand);
        }
    };
    private final ColorStateList mActiveSliderTint = loadColorStateList(R.color.system_accent_color);
    private final ColorStateList mInactiveSliderTint = loadColorStateList(R.color.volume_slider_inactive);

    public interface Callback {
        void onZenPrioritySettingsClicked();
    }

    public VolumeDialog(Context context, int windowType, VolumeDialogController controller, ZenModeController zenModeController, Callback callback) {
        this.mContext = context;
        this.mController = controller;
        this.mCallback = callback;
        this.mWindowType = windowType;
        this.mZenModeController = zenModeController;
        this.mKeyguard = (KeyguardManager) context.getSystemService("keyguard");
        this.mAudioManager = (AudioManager) context.getSystemService("audio");
        this.mAccessibilityMgr = (AccessibilityManager) this.mContext.getSystemService("accessibility");
        initDialog();
        this.mAccessibility.init();
        controller.addCallback(this.mControllerCallbackH, this.mHandler);
        controller.getState();
        TunerService.get(this.mContext).addTunable(this, "sysui_show_full_zen");
        Configuration currentConfig = this.mContext.getResources().getConfiguration();
        this.mDensity = currentConfig.densityDpi;
    }

    public void initDialog() {
        this.mDialog = new CustomDialog(this.mContext);
        this.mSpTexts = new SpTexts(this.mContext);
        this.mLayoutTransition = new LayoutTransition();
        this.mLayoutTransition.setDuration(new ValueAnimator().getDuration() / 2);
        this.mHovering = false;
        this.mShowing = false;
        Window window = this.mDialog.getWindow();
        window.requestFeature(1);
        window.setBackgroundDrawable(new ColorDrawable(0));
        window.clearFlags(2);
        window.addFlags(17563944);
        this.mDialog.setCanceledOnTouchOutside(true);
        Resources res = this.mContext.getResources();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.type = this.mWindowType;
        lp.format = -3;
        lp.setTitle(VolumeDialog.class.getSimpleName());
        lp.gravity = 49;
        lp.y = res.getDimensionPixelSize(R.dimen.volume_offset_top);
        lp.gravity = 48;
        lp.windowAnimations = -1;
        window.setAttributes(lp);
        window.setSoftInputMode(48);
        this.mDialog.setContentView(R.layout.volume_dialog);
        this.mDialogView = (ViewGroup) this.mDialog.findViewById(R.id.volume_dialog);
        this.mDialogView.setOnHoverListener(new View.OnHoverListener() {
            @Override
            public boolean onHover(View v, MotionEvent event) {
                int action = event.getActionMasked();
                VolumeDialog volumeDialog = VolumeDialog.this;
                boolean z = action == 9 || action == 7;
                volumeDialog.mHovering = z;
                VolumeDialog.this.rescheduleTimeoutH();
                return true;
            }
        });
        this.mDialogContentView = (ViewGroup) this.mDialog.findViewById(R.id.volume_dialog_content);
        this.mExpanded = false;
        this.mExpandButton = (ImageButton) this.mDialogView.findViewById(R.id.volume_expand_button);
        this.mExpandButton.setOnClickListener(this.mClickExpand);
        updateWindowWidthH();
        updateExpandButtonH();
        this.mDialogContentView.setLayoutTransition(this.mLayoutTransition);
        this.mMotion = new VolumeDialogMotion(this.mDialog, this.mDialogView, this.mDialogContentView, this.mExpandButton, new VolumeDialogMotion.Callback() {
            @Override
            public void onAnimatingChanged(boolean animating) {
                if (animating) {
                    return;
                }
                if (VolumeDialog.this.mPendingStateChanged) {
                    VolumeDialog.this.mHandler.sendEmptyMessage(7);
                    VolumeDialog.this.mPendingStateChanged = false;
                }
                if (!VolumeDialog.this.mPendingRecheckAll) {
                    return;
                }
                VolumeDialog.this.mHandler.sendEmptyMessage(4);
                VolumeDialog.this.mPendingRecheckAll = false;
            }
        });
        if (this.mRows.isEmpty()) {
            addRow(2, R.drawable.ic_volume_ringer, R.drawable.ic_volume_ringer_mute, true);
            addRow(3, R.drawable.ic_volume_media, R.drawable.ic_volume_media_mute, true);
            addRow(4, R.drawable.ic_volume_alarm, R.drawable.ic_volume_alarm_mute, false);
            addRow(0, R.drawable.ic_volume_voice, R.drawable.ic_volume_voice, false);
            addRow(6, R.drawable.ic_volume_bt_sco, R.drawable.ic_volume_bt_sco, false);
            addRow(1, R.drawable.ic_volume_system, R.drawable.ic_volume_system_mute, false);
        } else {
            addExistingRows();
        }
        this.mExpandButtonAnimationDuration = res.getInteger(R.integer.volume_expand_animation_duration);
        this.mZenFooter = (ZenFooter) this.mDialog.findViewById(R.id.volume_zen_footer);
        this.mZenFooter.init(this.mZenModeController);
        this.mZenPanel = (TunerZenModePanel) this.mDialog.findViewById(R.id.tuner_zen_mode_panel);
        this.mZenPanel.init(this.mZenModeController);
        this.mZenPanel.setCallback(this.mZenPanelCallback);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        boolean z = false;
        if (!"sysui_show_full_zen".equals(key)) {
            return;
        }
        if (newValue != null && Integer.parseInt(newValue) != 0) {
            z = true;
        }
        this.mShowFullZen = z;
    }

    private ColorStateList loadColorStateList(int colorResId) {
        return ColorStateList.valueOf(this.mContext.getColor(colorResId));
    }

    public void updateWindowWidthH() {
        ViewGroup.LayoutParams lp = this.mDialogView.getLayoutParams();
        DisplayMetrics dm = this.mContext.getResources().getDisplayMetrics();
        if (D.BUG) {
            Log.d(TAG, "updateWindowWidth dm.w=" + dm.widthPixels);
        }
        int w = dm.widthPixels;
        int max = this.mContext.getResources().getDimensionPixelSize(R.dimen.volume_dialog_panel_width);
        if (w > max) {
            w = max;
        }
        lp.width = w;
        this.mDialogView.setLayoutParams(lp);
    }

    public void setStreamImportant(int stream, boolean important) {
        this.mHandler.obtainMessage(5, stream, important ? 1 : 0).sendToTarget();
    }

    public void setShowHeaders(boolean showHeaders) {
        if (showHeaders == this.mShowHeaders) {
            return;
        }
        this.mShowHeaders = showHeaders;
        this.mHandler.sendEmptyMessage(4);
    }

    public void setAutomute(boolean automute) {
        if (this.mAutomute == automute) {
            return;
        }
        this.mAutomute = automute;
        this.mHandler.sendEmptyMessage(4);
    }

    public void setSilentMode(boolean silentMode) {
        if (this.mSilentMode == silentMode) {
            return;
        }
        this.mSilentMode = silentMode;
        this.mHandler.sendEmptyMessage(4);
    }

    private void addRow(int stream, int iconRes, int iconMuteRes, boolean important) {
        VolumeRow row = new VolumeRow(null);
        initRow(row, stream, iconRes, iconMuteRes, important);
        if (!this.mRows.isEmpty()) {
            addSpacer(row);
        }
        this.mDialogContentView.addView(row.view, this.mDialogContentView.getChildCount() - 2);
        this.mRows.add(row);
    }

    private void addExistingRows() {
        int N = this.mRows.size();
        for (int i = 0; i < N; i++) {
            VolumeRow row = this.mRows.get(i);
            initRow(row, row.stream, row.iconRes, row.iconMuteRes, row.important);
            if (i > 0) {
                addSpacer(row);
            }
            this.mDialogContentView.addView(row.view, this.mDialogContentView.getChildCount() - 2);
        }
    }

    private void addSpacer(VolumeRow row) {
        View v = new View(this.mContext);
        v.setId(android.R.id.background);
        int h = this.mContext.getResources().getDimensionPixelSize(R.dimen.volume_slider_interspacing);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, h);
        this.mDialogContentView.addView(v, this.mDialogContentView.getChildCount() - 2, lp);
        row.space = v;
    }

    private boolean isAttached() {
        if (this.mDialogContentView != null) {
            return this.mDialogContentView.isAttachedToWindow();
        }
        return false;
    }

    public VolumeRow getActiveRow() {
        for (VolumeRow row : this.mRows) {
            if (row.stream == this.mActiveStream) {
                return row;
            }
        }
        return this.mRows.get(0);
    }

    private VolumeRow findRow(int stream) {
        for (VolumeRow row : this.mRows) {
            if (row.stream == stream) {
                return row;
            }
        }
        return null;
    }

    public void dump(PrintWriter writer) {
        writer.println(VolumeDialog.class.getSimpleName() + " state:");
        writer.print("  mShowing: ");
        writer.println(this.mShowing);
        writer.print("  mExpanded: ");
        writer.println(this.mExpanded);
        writer.print("  mExpandButtonAnimationRunning: ");
        writer.println(this.mExpandButtonAnimationRunning);
        writer.print("  mActiveStream: ");
        writer.println(this.mActiveStream);
        writer.print("  mDynamic: ");
        writer.println(this.mDynamic);
        writer.print("  mShowHeaders: ");
        writer.println(this.mShowHeaders);
        writer.print("  mAutomute: ");
        writer.println(this.mAutomute);
        writer.print("  mSilentMode: ");
        writer.println(this.mSilentMode);
        writer.print("  mCollapseTime: ");
        writer.println(this.mCollapseTime);
        writer.print("  mAccessibility.mFeedbackEnabled: ");
        writer.println(this.mAccessibility.mFeedbackEnabled);
    }

    public static int getImpliedLevel(SeekBar seekBar, int progress) {
        int m = seekBar.getMax();
        int n = (m / 100) - 1;
        if (progress == 0) {
            return 0;
        }
        if (progress == m) {
            int level = m / 100;
            return level;
        }
        int level2 = ((int) ((progress / m) * n)) + 1;
        return level2;
    }

    @SuppressLint({"InflateParams"})
    private void initRow(final VolumeRow row, final int stream, int iconRes, int iconMuteRes, boolean important) {
        row.stream = stream;
        row.iconRes = iconRes;
        row.iconMuteRes = iconMuteRes;
        row.important = important;
        row.view = this.mDialog.getLayoutInflater().inflate(R.layout.volume_dialog_row, (ViewGroup) null);
        row.view.setTag(row);
        row.header = (TextView) row.view.findViewById(R.id.volume_row_header);
        this.mSpTexts.add(row.header);
        row.slider = (SeekBar) row.view.findViewById(R.id.volume_row_slider);
        row.slider.setOnSeekBarChangeListener(new VolumeSeekBarChangeListener(this, row, null));
        row.anim = null;
        row.view.setOnTouchListener(new View.OnTouchListener() {
            private boolean mDragging;
            private final Rect mSliderHitRect = new Rect();

            @Override
            @SuppressLint({"ClickableViewAccessibility"})
            public boolean onTouch(View v, MotionEvent event) {
                row.slider.getHitRect(this.mSliderHitRect);
                if (!this.mDragging && event.getActionMasked() == 0 && event.getY() < this.mSliderHitRect.top) {
                    this.mDragging = true;
                }
                if (!this.mDragging) {
                    return false;
                }
                event.offsetLocation(-this.mSliderHitRect.left, -this.mSliderHitRect.top);
                row.slider.dispatchTouchEvent(event);
                if (event.getActionMasked() == 1 || event.getActionMasked() == 3) {
                    this.mDragging = false;
                }
                return true;
            }
        });
        row.icon = (ImageButton) row.view.findViewById(R.id.volume_row_icon);
        row.icon.setImageResource(iconRes);
        row.icon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Events.writeEvent(VolumeDialog.this.mContext, 7, Integer.valueOf(row.stream), Integer.valueOf(row.iconState));
                VolumeDialog.this.mController.setActiveStream(row.stream);
                if (row.stream == 2) {
                    boolean hasVibrator = VolumeDialog.this.mController.hasVibrator();
                    if (VolumeDialog.this.mState.ringerModeInternal == 2) {
                        if (hasVibrator) {
                            VolumeDialog.this.mController.setRingerMode(1, false);
                        } else {
                            boolean wasZero = row.ss.level == 0;
                            VolumeDialog.this.mController.setStreamVolume(stream, wasZero ? row.lastAudibleLevel : 0);
                        }
                    } else {
                        VolumeDialog.this.mController.setRingerMode(2, false);
                        if (row.ss.level == 0) {
                            VolumeDialog.this.mController.setStreamVolume(stream, 1);
                        }
                    }
                } else {
                    boolean vmute = row.ss.level == row.ss.levelMin;
                    VolumeDialog.this.mController.setStreamVolume(stream, vmute ? row.lastAudibleLevel : row.ss.levelMin);
                }
                row.userAttempt = 0L;
            }
        });
    }

    public void showH(int reason) {
        if (D.BUG) {
            Log.d(TAG, "showH r=" + Events.DISMISS_REASONS[reason]);
        }
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        rescheduleTimeoutH();
        if (this.mShowing) {
            return;
        }
        this.mShowing = true;
        this.mMotion.startShow();
        Events.writeEvent(this.mContext, 0, Integer.valueOf(reason), Boolean.valueOf(this.mKeyguard.isKeyguardLocked()));
        this.mController.notifyVisible(true);
    }

    protected void rescheduleTimeoutH() {
        this.mHandler.removeMessages(2);
        int timeout = computeTimeoutH();
        this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(2, 3, 0), timeout);
        if (D.BUG) {
            Log.d(TAG, "rescheduleTimeout " + timeout + " " + Debug.getCaller());
        }
        this.mController.userActivity();
    }

    private int computeTimeoutH() {
        if (this.mAccessibility.mFeedbackEnabled) {
            return 20000;
        }
        if (this.mHovering) {
            return 16000;
        }
        if (this.mSafetyWarning != null || this.mExpanded || this.mExpandButtonAnimationRunning) {
            return 5000;
        }
        return this.mActiveStream == 3 ? 1500 : 3000;
    }

    protected void dismissH(int reason) {
        if (this.mMotion.isAnimating()) {
            return;
        }
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(1);
        if (this.mShowing) {
            this.mShowing = false;
            this.mMotion.startDismiss(new Runnable() {
                @Override
                public void run() {
                    VolumeDialog.this.setExpandedH(false);
                }
            });
            if (this.mAccessibilityMgr.isEnabled()) {
                AccessibilityEvent event = AccessibilityEvent.obtain(32);
                event.setPackageName(this.mContext.getPackageName());
                event.setClassName(CustomDialog.class.getSuperclass().getName());
                event.getText().add(this.mContext.getString(R.string.volume_dialog_accessibility_dismissed_message));
                this.mAccessibilityMgr.sendAccessibilityEvent(event);
            }
            Events.writeEvent(this.mContext, 1, Integer.valueOf(reason));
            this.mController.notifyVisible(false);
            synchronized (this.mSafetyWarningLock) {
                if (this.mSafetyWarning != null) {
                    if (D.BUG) {
                        Log.d(TAG, "SafetyWarning dismissed");
                    }
                    this.mSafetyWarning.dismiss();
                }
            }
        }
    }

    public void updateDialogBottomMarginH() {
        long diff = System.currentTimeMillis() - this.mCollapseTime;
        boolean collapsing = this.mCollapseTime != 0 && diff < getConservativeCollapseDuration();
        ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) this.mDialogView.getLayoutParams();
        int bottomMargin = collapsing ? this.mDialogContentView.getHeight() : this.mContext.getResources().getDimensionPixelSize(R.dimen.volume_dialog_margin_bottom);
        if (bottomMargin == mlp.bottomMargin) {
            return;
        }
        if (D.BUG) {
            Log.d(TAG, "bottomMargin " + mlp.bottomMargin + " -> " + bottomMargin);
        }
        mlp.bottomMargin = bottomMargin;
        this.mDialogView.setLayoutParams(mlp);
    }

    private long getConservativeCollapseDuration() {
        return this.mExpandButtonAnimationDuration * 3;
    }

    public void prepareForCollapse() {
        this.mHandler.removeMessages(8);
        this.mCollapseTime = System.currentTimeMillis();
        updateDialogBottomMarginH();
        this.mHandler.sendEmptyMessageDelayed(8, getConservativeCollapseDuration());
    }

    public void setExpandedH(boolean expanded) {
        if (this.mExpanded == expanded) {
            return;
        }
        this.mExpanded = expanded;
        this.mExpandButtonAnimationRunning = isAttached();
        if (D.BUG) {
            Log.d(TAG, "setExpandedH " + expanded);
        }
        if (!this.mExpanded && this.mExpandButtonAnimationRunning) {
            prepareForCollapse();
        }
        updateRowsH();
        if (this.mExpandButtonAnimationRunning) {
            Drawable d = this.mExpandButton.getDrawable();
            if (d instanceof AnimatedVectorDrawable) {
                AnimatedVectorDrawable avd = (AnimatedVectorDrawable) d.getConstantState().newDrawable();
                this.mExpandButton.setImageDrawable(avd);
                avd.start();
                this.mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        VolumeDialog.this.mExpandButtonAnimationRunning = false;
                        VolumeDialog.this.updateExpandButtonH();
                        VolumeDialog.this.rescheduleTimeoutH();
                    }
                }, this.mExpandButtonAnimationDuration);
            }
        }
        rescheduleTimeoutH();
    }

    public void updateExpandButtonH() {
        if (D.BUG) {
            Log.d(TAG, "updateExpandButtonH");
        }
        this.mExpandButton.setClickable(!this.mExpandButtonAnimationRunning);
        if (this.mExpandButtonAnimationRunning && isAttached()) {
            return;
        }
        int res = this.mExpanded ? R.drawable.ic_volume_collapse_animation : R.drawable.ic_volume_expand_animation;
        if (hasTouchFeature()) {
            this.mExpandButton.setImageResource(res);
        } else {
            this.mExpandButton.setImageResource(R.drawable.ic_volume_ringer);
            this.mExpandButton.setBackgroundResource(0);
        }
        this.mExpandButton.setContentDescription(this.mContext.getString(this.mExpanded ? R.string.accessibility_volume_collapse : R.string.accessibility_volume_expand));
    }

    private boolean isVisibleH(VolumeRow row, boolean isActive) {
        if ((this.mExpanded && row.view.getVisibility() == 0) || (this.mExpanded && (row.important || isActive))) {
            return true;
        }
        if (this.mExpanded) {
            return false;
        }
        return isActive;
    }

    private void updateRowsH() {
        if (D.BUG) {
            Log.d(TAG, "updateRowsH");
        }
        VolumeRow activeRow = getActiveRow();
        updateFooterH();
        updateExpandButtonH();
        if (!this.mShowing) {
            trimObsoleteH();
        }
        for (VolumeRow row : this.mRows) {
            boolean isActive = row == activeRow;
            boolean visible = isVisibleH(row, isActive);
            Util.setVisOrGone(row.view, visible);
            Util.setVisOrGone(row.space, visible ? this.mExpanded : false);
            updateVolumeRowHeaderVisibleH(row);
            row.header.setAlpha((this.mExpanded && isActive) ? 1.0f : 0.5f);
            updateVolumeRowSliderTintH(row, isActive);
        }
    }

    private void trimObsoleteH() {
        if (D.BUG) {
            Log.d(TAG, "trimObsoleteH");
        }
        for (int i = this.mRows.size() - 1; i >= 0; i--) {
            VolumeRow row = this.mRows.get(i);
            if (row.ss != null && row.ss.dynamic && !this.mDynamic.get(row.stream)) {
                this.mRows.remove(i);
                this.mDialogContentView.removeView(row.view);
                this.mDialogContentView.removeView(row.space);
            }
        }
    }

    public void onStateChangedH(VolumeDialogController.State state) {
        boolean animating = this.mMotion.isAnimating();
        if (D.BUG) {
            Log.d(TAG, "onStateChangedH animating=" + animating);
        }
        this.mState = state;
        if (animating) {
            this.mPendingStateChanged = true;
            return;
        }
        this.mDynamic.clear();
        for (int i = 0; i < state.states.size(); i++) {
            int stream = state.states.keyAt(i);
            VolumeDialogController.StreamState ss = state.states.valueAt(i);
            if (ss.dynamic) {
                this.mDynamic.put(stream, true);
                if (findRow(stream) == null) {
                    addRow(stream, R.drawable.ic_volume_remote, R.drawable.ic_volume_remote_mute, true);
                }
            }
        }
        if (this.mActiveStream != state.activeStream) {
            this.mActiveStream = state.activeStream;
            updateRowsH();
            rescheduleTimeoutH();
        }
        for (VolumeRow row : this.mRows) {
            updateVolumeRowH(row);
        }
        updateFooterH();
    }

    public void updateFooterH() {
        boolean fullVisible = false;
        if (D.BUG) {
            Log.d(TAG, "updateFooterH");
        }
        boolean wasVisible = this.mZenFooter.getVisibility() == 0;
        boolean visible = this.mState.zenMode != 0 && (this.mAudioManager.isStreamAffectedByRingerMode(this.mActiveStream) || this.mExpanded) && !this.mZenPanel.isEditing();
        if (wasVisible != visible && !visible) {
            prepareForCollapse();
        }
        Util.setVisOrGone(this.mZenFooter, visible);
        this.mZenFooter.update();
        boolean fullWasVisible = this.mZenPanel.getVisibility() == 0;
        if (this.mShowFullZen && !visible) {
            fullVisible = true;
        }
        if (fullWasVisible != fullVisible && !fullVisible) {
            prepareForCollapse();
        }
        Util.setVisOrGone(this.mZenPanel, fullVisible);
        if (!fullVisible) {
            return;
        }
        this.mZenPanel.setZenState(this.mState.zenMode);
        this.mZenPanel.setDoneListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VolumeDialog.this.prepareForCollapse();
                VolumeDialog.this.mHandler.sendEmptyMessage(9);
            }
        });
    }

    private void updateVolumeRowH(VolumeRow row) {
        VolumeDialogController.StreamState ss;
        boolean zenMuted;
        int iconRes;
        int i;
        int vlevel;
        if (D.BUG) {
            Log.d(TAG, "updateVolumeRowH s=" + row.stream);
        }
        if (this.mState == null || (ss = this.mState.states.get(row.stream)) == null) {
            return;
        }
        row.ss = ss;
        if (ss.level > 0) {
            row.lastAudibleLevel = ss.level;
        }
        if (ss.level == row.requestedLevel) {
            row.requestedLevel = -1;
        }
        boolean isRingStream = row.stream == 2;
        boolean isSystemStream = row.stream == 1;
        boolean isAlarmStream = row.stream == 4;
        boolean isMusicStream = row.stream == 3;
        boolean isRingVibrate = isRingStream && this.mState.ringerModeInternal == 1;
        boolean isRingSilent = isRingStream && this.mState.ringerModeInternal == 0;
        boolean isZenAlarms = this.mState.zenMode == 3;
        boolean isZenNone = this.mState.zenMode == 2;
        boolean isZenPriority = this.mState.zenMode == 1;
        boolean z = (isRingStream || isSystemStream) ? isZenNone : false;
        boolean z2 = isRingStream ? isZenPriority : false;
        if (isZenAlarms) {
            zenMuted = !isRingStream ? isSystemStream : true;
        } else if (isZenNone) {
            zenMuted = (isRingStream || isSystemStream || isAlarmStream) ? true : isMusicStream;
        } else {
            zenMuted = false;
        }
        int max = ss.levelMax * 100;
        if (max != row.slider.getMax()) {
            row.slider.setMax(max);
        }
        updateVolumeRowHeaderVisibleH(row);
        String text = ss.name;
        if (this.mShowHeaders) {
            if (z) {
                text = this.mContext.getString(R.string.volume_stream_muted_dnd, ss.name);
            } else if (isRingVibrate && z2) {
                text = this.mContext.getString(R.string.volume_stream_vibrate_dnd, ss.name);
            } else if (isRingVibrate) {
                text = this.mContext.getString(R.string.volume_stream_vibrate, ss.name);
            } else if (ss.muted || (this.mAutomute && ss.level == 0)) {
                text = this.mContext.getString(R.string.volume_stream_muted, ss.name);
            } else if (z2) {
                text = this.mContext.getString(R.string.volume_stream_limited_dnd, ss.name);
            }
        }
        Util.setText(row.header, text);
        boolean iconEnabled = (this.mAutomute || ss.muteSupported) && !zenMuted;
        row.icon.setEnabled(iconEnabled);
        row.icon.setAlpha(iconEnabled ? 1.0f : 0.5f);
        if (isRingVibrate) {
            iconRes = R.drawable.ic_volume_ringer_vibrate;
        } else if (isRingSilent || zenMuted) {
            iconRes = row.cachedIconRes;
        } else if (ss.routedToBluetooth) {
            iconRes = ss.muted ? R.drawable.ic_volume_media_bt_mute : R.drawable.ic_volume_media_bt;
        } else {
            iconRes = ((this.mAutomute && ss.level == 0) || ss.muted) ? row.iconMuteRes : row.iconRes;
        }
        if (iconRes != row.cachedIconRes) {
            if (row.cachedIconRes != 0 && isRingVibrate) {
                this.mController.vibrate();
            }
            row.cachedIconRes = iconRes;
            row.icon.setImageResource(iconRes);
        }
        if (iconRes == R.drawable.ic_volume_ringer_vibrate) {
            i = 3;
        } else if (iconRes == R.drawable.ic_volume_media_bt_mute || iconRes == row.iconMuteRes) {
            i = 2;
        } else if (iconRes == R.drawable.ic_volume_media_bt || iconRes == row.iconRes) {
            i = 1;
        } else {
            i = 0;
        }
        row.iconState = i;
        if (iconEnabled) {
            if (isRingStream) {
                if (isRingVibrate) {
                    row.icon.setContentDescription(this.mContext.getString(R.string.volume_stream_content_description_unmute, ss.name));
                } else if (this.mController.hasVibrator()) {
                    row.icon.setContentDescription(this.mContext.getString(R.string.volume_stream_content_description_vibrate, ss.name));
                } else {
                    row.icon.setContentDescription(this.mContext.getString(R.string.volume_stream_content_description_mute, ss.name));
                }
            } else if (ss.muted || (this.mAutomute && ss.level == 0)) {
                row.icon.setContentDescription(this.mContext.getString(R.string.volume_stream_content_description_unmute, ss.name));
            } else {
                row.icon.setContentDescription(this.mContext.getString(R.string.volume_stream_content_description_mute, ss.name));
            }
        } else {
            row.icon.setContentDescription(ss.name);
        }
        boolean enableSlider = !zenMuted;
        if (row.ss.muted && (isRingVibrate || (!isRingStream && !zenMuted))) {
            vlevel = 0;
        } else {
            vlevel = row.ss.level;
        }
        updateVolumeRowSliderH(row, enableSlider, vlevel);
    }

    private void updateVolumeRowHeaderVisibleH(VolumeRow row) {
        boolean showHeaders;
        boolean z = row.ss != null ? row.ss.dynamic : false;
        if (this.mShowHeaders) {
            showHeaders = true;
        } else {
            showHeaders = this.mExpanded ? z : false;
        }
        if (row.cachedShowHeaders == showHeaders) {
            return;
        }
        row.cachedShowHeaders = showHeaders;
        Util.setVisOrGone(row.header, showHeaders);
    }

    private void updateVolumeRowSliderTintH(VolumeRow row, boolean isActive) {
        if (isActive && this.mExpanded) {
            row.slider.requestFocus();
        }
        ColorStateList tint = (isActive && row.slider.isEnabled()) ? this.mActiveSliderTint : this.mInactiveSliderTint;
        if (tint == row.cachedSliderTint) {
            return;
        }
        row.cachedSliderTint = tint;
        row.slider.setProgressTintList(tint);
        row.slider.setThumbTintList(tint);
    }

    private void updateVolumeRowSliderH(VolumeRow row, boolean enable, int vlevel) {
        int newProgress;
        row.slider.setEnabled(enable);
        updateVolumeRowSliderTintH(row, row.stream == this.mActiveStream);
        if (row.tracking) {
            return;
        }
        int progress = row.slider.getProgress();
        int level = getImpliedLevel(row.slider, progress);
        boolean rowVisible = row.view.getVisibility() == 0;
        boolean inGracePeriod = SystemClock.uptimeMillis() - row.userAttempt < 1000;
        this.mHandler.removeMessages(3, row);
        if (this.mShowing && rowVisible && inGracePeriod) {
            if (D.BUG) {
                Log.d(TAG, "inGracePeriod");
            }
            this.mHandler.sendMessageAtTime(this.mHandler.obtainMessage(3, row), row.userAttempt + 1000);
            return;
        }
        if ((vlevel == level && this.mShowing && rowVisible) || progress == (newProgress = vlevel * 100)) {
            return;
        }
        if (this.mShowing && rowVisible) {
            if (row.anim != null && row.anim.isRunning() && row.animTargetProgress == newProgress) {
                return;
            }
            if (row.anim == null) {
                row.anim = ObjectAnimator.ofInt(row.slider, "progress", progress, newProgress);
                row.anim.setInterpolator(new DecelerateInterpolator());
            } else {
                row.anim.cancel();
                row.anim.setIntValues(progress, newProgress);
            }
            row.animTargetProgress = newProgress;
            row.anim.setDuration(80L);
            row.anim.start();
            return;
        }
        if (row.anim != null) {
            row.anim.cancel();
        }
        row.slider.setProgress(newProgress);
    }

    public void recheckH(VolumeRow row) {
        if (row == null) {
            if (D.BUG) {
                Log.d(TAG, "recheckH ALL");
            }
            trimObsoleteH();
            for (VolumeRow r : this.mRows) {
                updateVolumeRowH(r);
            }
            return;
        }
        if (D.BUG) {
            Log.d(TAG, "recheckH " + row.stream);
        }
        updateVolumeRowH(row);
    }

    public void setStreamImportantH(int stream, boolean important) {
        for (VolumeRow row : this.mRows) {
            if (row.stream == stream) {
                row.important = important;
                return;
            }
        }
    }

    public void showSafetyWarningH(int flags) {
        if ((flags & 1025) != 0 || this.mShowing) {
            synchronized (this.mSafetyWarningLock) {
                if (this.mSafetyWarning != null) {
                    return;
                }
                this.mSafetyWarning = new SafetyWarningDialog(this.mContext, this.mController.getAudioManager()) {
                    @Override
                    protected void cleanUp() {
                        synchronized (VolumeDialog.this.mSafetyWarningLock) {
                            VolumeDialog.this.mSafetyWarning = null;
                        }
                        VolumeDialog.this.recheckH(null);
                    }
                };
                this.mSafetyWarning.show();
                recheckH(null);
            }
        }
        rescheduleTimeoutH();
    }

    private boolean hasTouchFeature() {
        PackageManager pm = this.mContext.getPackageManager();
        return pm.hasSystemFeature("android.hardware.touchscreen");
    }

    private final class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    VolumeDialog.this.showH(msg.arg1);
                    break;
                case 2:
                    VolumeDialog.this.dismissH(msg.arg1);
                    break;
                case 3:
                    VolumeDialog.this.recheckH((VolumeRow) msg.obj);
                    break;
                case 4:
                    VolumeDialog.this.recheckH(null);
                    break;
                case 5:
                    VolumeDialog.this.setStreamImportantH(msg.arg1, msg.arg2 != 0);
                    break;
                case 6:
                    VolumeDialog.this.rescheduleTimeoutH();
                    break;
                case 7:
                    VolumeDialog.this.onStateChangedH(VolumeDialog.this.mState);
                    break;
                case 8:
                    VolumeDialog.this.updateDialogBottomMarginH();
                    break;
                case 9:
                    VolumeDialog.this.updateFooterH();
                    break;
            }
        }
    }

    private final class CustomDialog extends Dialog {
        public CustomDialog(Context context) {
            super(context);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            VolumeDialog.this.rescheduleTimeoutH();
            return super.dispatchTouchEvent(ev);
        }

        @Override
        protected void onStop() {
            super.onStop();
            boolean animating = VolumeDialog.this.mMotion.isAnimating();
            if (D.BUG) {
                Log.d(VolumeDialog.TAG, "onStop animating=" + animating);
            }
            if (animating) {
                VolumeDialog.this.mPendingRecheckAll = true;
            } else {
                VolumeDialog.this.mHandler.sendEmptyMessage(4);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (isShowing() && event.getAction() == 4) {
                VolumeDialog.this.dismissH(1);
                return true;
            }
            return false;
        }

        @Override
        public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
            event.setClassName(getClass().getSuperclass().getName());
            event.setPackageName(VolumeDialog.this.mContext.getPackageName());
            ViewGroup.LayoutParams params = getWindow().getAttributes();
            boolean isFullScreen = params.width == -1 && params.height == -1;
            event.setFullScreen(isFullScreen);
            if (event.getEventType() != 32 || !VolumeDialog.this.mShowing) {
                return false;
            }
            event.getText().add(VolumeDialog.this.mContext.getString(R.string.volume_dialog_accessibility_shown_message, VolumeDialog.this.getActiveRow().ss.name));
            return true;
        }
    }

    private final class VolumeSeekBarChangeListener implements SeekBar.OnSeekBarChangeListener {
        private final VolumeRow mRow;

        VolumeSeekBarChangeListener(VolumeDialog this$0, VolumeRow row, VolumeSeekBarChangeListener volumeSeekBarChangeListener) {
            this(row);
        }

        private VolumeSeekBarChangeListener(VolumeRow row) {
            this.mRow = row;
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int minProgress;
            if (this.mRow.ss == null) {
                return;
            }
            if (D.BUG) {
                Log.d(VolumeDialog.TAG, AudioSystem.streamToString(this.mRow.stream) + " onProgressChanged " + progress + " fromUser=" + fromUser);
            }
            if (fromUser) {
                if (this.mRow.ss.levelMin > 0 && progress < (minProgress = this.mRow.ss.levelMin * 100)) {
                    seekBar.setProgress(minProgress);
                    progress = minProgress;
                }
                int userLevel = VolumeDialog.getImpliedLevel(seekBar, progress);
                if (this.mRow.ss.level == userLevel && (!this.mRow.ss.muted || userLevel <= 0)) {
                    return;
                }
                this.mRow.userAttempt = SystemClock.uptimeMillis();
                if (this.mRow.requestedLevel == userLevel) {
                    return;
                }
                VolumeDialog.this.mController.setStreamVolume(this.mRow.stream, userLevel);
                this.mRow.requestedLevel = userLevel;
                Events.writeEvent(VolumeDialog.this.mContext, 9, Integer.valueOf(this.mRow.stream), Integer.valueOf(userLevel));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (D.BUG) {
                Log.d(VolumeDialog.TAG, "onStartTrackingTouch " + this.mRow.stream);
            }
            VolumeDialog.this.mController.setActiveStream(this.mRow.stream);
            this.mRow.tracking = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (D.BUG) {
                Log.d(VolumeDialog.TAG, "onStopTrackingTouch " + this.mRow.stream);
            }
            this.mRow.tracking = false;
            this.mRow.userAttempt = SystemClock.uptimeMillis();
            int userLevel = VolumeDialog.getImpliedLevel(seekBar, seekBar.getProgress());
            Events.writeEvent(VolumeDialog.this.mContext, 16, Integer.valueOf(this.mRow.stream), Integer.valueOf(userLevel));
            if (this.mRow.ss.level == userLevel) {
                return;
            }
            VolumeDialog.this.mHandler.sendMessageDelayed(VolumeDialog.this.mHandler.obtainMessage(3, this.mRow), 1000L);
        }
    }

    private final class Accessibility extends View.AccessibilityDelegate {
        private boolean mFeedbackEnabled;

        Accessibility(VolumeDialog this$0, Accessibility accessibility) {
            this();
        }

        private Accessibility() {
        }

        public void init() {
            VolumeDialog.this.mDialogView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewDetachedFromWindow(View v) {
                    if (D.BUG) {
                        Log.d(VolumeDialog.TAG, "onViewDetachedFromWindow");
                    }
                }

                @Override
                public void onViewAttachedToWindow(View v) {
                    if (D.BUG) {
                        Log.d(VolumeDialog.TAG, "onViewAttachedToWindow");
                    }
                    Accessibility.this.updateFeedbackEnabled();
                }
            });
            VolumeDialog.this.mDialogView.setAccessibilityDelegate(this);
            VolumeDialog.this.mAccessibilityMgr.addAccessibilityStateChangeListener(new AccessibilityManager.AccessibilityStateChangeListener() {
                @Override
                public void onAccessibilityStateChanged(boolean enabled) {
                    Accessibility.this.updateFeedbackEnabled();
                }
            });
            updateFeedbackEnabled();
        }

        @Override
        public boolean onRequestSendAccessibilityEvent(ViewGroup host, View child, AccessibilityEvent event) {
            VolumeDialog.this.rescheduleTimeoutH();
            return super.onRequestSendAccessibilityEvent(host, child, event);
        }

        public void updateFeedbackEnabled() {
            this.mFeedbackEnabled = computeFeedbackEnabled();
        }

        private boolean computeFeedbackEnabled() {
            List<AccessibilityServiceInfo> services = VolumeDialog.this.mAccessibilityMgr.getEnabledAccessibilityServiceList(-1);
            for (AccessibilityServiceInfo asi : services) {
                if (asi.feedbackType != 0 && asi.feedbackType != 16) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class VolumeRow {
        private ObjectAnimator anim;
        private int animTargetProgress;
        private int cachedIconRes;
        private boolean cachedShowHeaders;
        private ColorStateList cachedSliderTint;
        private TextView header;
        private ImageButton icon;
        private int iconMuteRes;
        private int iconRes;
        private int iconState;
        private boolean important;
        private int lastAudibleLevel;
        private int requestedLevel;
        private SeekBar slider;
        private View space;
        private VolumeDialogController.StreamState ss;
        private int stream;
        private boolean tracking;
        private long userAttempt;
        private View view;

        VolumeRow(VolumeRow volumeRow) {
            this();
        }

        private VolumeRow() {
            this.requestedLevel = -1;
            this.cachedShowHeaders = false;
            this.lastAudibleLevel = 1;
        }
    }
}
