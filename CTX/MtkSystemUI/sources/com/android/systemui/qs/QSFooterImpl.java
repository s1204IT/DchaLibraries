package com.android.systemui.qs;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.content.pm.UserInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.CarrierText;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.settingslib.Utils;
import com.android.settingslib.drawable.UserIconDrawable;
import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.Dependency;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.TouchAnimator;
import com.android.systemui.statusbar.phone.MultiUserSwitch;
import com.android.systemui.statusbar.phone.SettingsButton;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.NetworkController;
import com.android.systemui.statusbar.policy.UserInfoController;
import com.android.systemui.tuner.TunerService;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import java.util.Iterator;

public class QSFooterImpl extends FrameLayout implements View.OnClickListener, QSFooter, NetworkController.EmergencyListener, NetworkController.SignalCallback, UserInfoController.OnUserInfoChangedListener {
    private View mActionsContainer;
    private ActivityStarter mActivityStarter;
    private CarrierText mCarrierText;
    private final int mColorForeground;
    private View mDivider;
    private View mDragHandle;
    protected View mEdit;
    private View.OnClickListener mExpandClickListener;
    private boolean mExpanded;
    private float mExpansionAmount;
    protected TouchAnimator mFooterAnimator;
    private final CellSignalState mInfo;
    private boolean mListening;
    private View mMobileGroup;
    private ImageView mMobileRoaming;
    private ImageView mMobileSignal;
    private ImageView mMultiUserAvatar;
    protected MultiUserSwitch mMultiUserSwitch;
    private PageIndicator mPageIndicator;
    private boolean mQsDisabled;
    private QSPanel mQsPanel;
    private SettingsButton mSettingsButton;
    private TouchAnimator mSettingsCogAnimator;
    protected View mSettingsContainer;
    private boolean mShowEmergencyCallsOnly;
    private ISystemUIStatusBarExt mStatusBarExt;
    private UserInfoController mUserInfoController;

    public QSFooterImpl(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mInfo = new CellSignalState();
        this.mColorForeground = Utils.getColorAttr(context, R.attr.colorForeground);
        this.mStatusBarExt = OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext).makeSystemUIStatusBar(this.mContext);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDivider = findViewById(com.android.systemui.R.id.qs_footer_divider);
        this.mEdit = findViewById(R.id.edit);
        this.mEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public final void onClick(View view) {
                ((ActivityStarter) Dependency.get(ActivityStarter.class)).postQSRunnableDismissingKeyguard(new Runnable() {
                    @Override
                    public final void run() {
                        qSFooterImpl.mQsPanel.showEdit(view);
                    }
                });
            }
        });
        this.mPageIndicator = (PageIndicator) findViewById(com.android.systemui.R.id.footer_page_indicator);
        this.mSettingsButton = (SettingsButton) findViewById(com.android.systemui.R.id.settings_button);
        this.mSettingsContainer = findViewById(com.android.systemui.R.id.settings_button_container);
        this.mSettingsButton.setOnClickListener(this);
        this.mMobileGroup = findViewById(com.android.systemui.R.id.mobile_combo);
        this.mMobileSignal = (ImageView) findViewById(com.android.systemui.R.id.mobile_signal);
        this.mMobileRoaming = (ImageView) findViewById(com.android.systemui.R.id.mobile_roaming);
        this.mCarrierText = (CarrierText) findViewById(com.android.systemui.R.id.qs_carrier_text);
        this.mCarrierText.setDisplayFlags(3);
        this.mMultiUserSwitch = (MultiUserSwitch) findViewById(com.android.systemui.R.id.multi_user_switch);
        this.mMultiUserAvatar = (ImageView) this.mMultiUserSwitch.findViewById(com.android.systemui.R.id.multi_user_avatar);
        this.mDragHandle = findViewById(com.android.systemui.R.id.qs_drag_handle_view);
        this.mActionsContainer = findViewById(com.android.systemui.R.id.qs_footer_actions_container);
        ((RippleDrawable) this.mSettingsButton.getBackground()).setForceSoftware(true);
        updateResources();
        this.mUserInfoController = (UserInfoController) Dependency.get(UserInfoController.class);
        this.mActivityStarter = (ActivityStarter) Dependency.get(ActivityStarter.class);
        addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public final void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                this.f$0.updateAnimator(i3 - i);
            }
        });
        setImportantForAccessibility(1);
    }

    private void updateAnimator(int i) {
        int numQuickTiles = QuickQSPanel.getNumQuickTiles(this.mContext);
        int dimensionPixelSize = (i - ((this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.qs_quick_tile_size) - this.mContext.getResources().getDimensionPixelSize(com.android.systemui.R.dimen.qs_quick_tile_padding)) * numQuickTiles)) / (numQuickTiles - 1);
        int dimensionPixelOffset = this.mContext.getResources().getDimensionPixelOffset(com.android.systemui.R.dimen.default_gear_space);
        TouchAnimator.Builder builder = new TouchAnimator.Builder();
        View view = this.mSettingsContainer;
        float[] fArr = new float[2];
        fArr[0] = isLayoutRtl() ? dimensionPixelSize - dimensionPixelOffset : -(dimensionPixelSize - dimensionPixelOffset);
        fArr[1] = 0.0f;
        this.mSettingsCogAnimator = builder.addFloat(view, "translationX", fArr).addFloat(this.mSettingsButton, "rotation", -120.0f, 0.0f).build();
        setExpansion(this.mExpansionAmount);
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        updateResources();
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        updateResources();
    }

    private void updateResources() {
        updateFooterAnimator();
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.mActionsContainer.getLayoutParams();
        layoutParams.width = this.mContext.getResources().getInteger(com.android.systemui.R.integer.qs_footer_actions_width);
        layoutParams.weight = this.mContext.getResources().getInteger(com.android.systemui.R.integer.qs_footer_actions_weight);
        this.mActionsContainer.setLayoutParams(layoutParams);
    }

    private void updateFooterAnimator() {
        this.mFooterAnimator = createFooterAnimator();
    }

    private TouchAnimator createFooterAnimator() {
        return new TouchAnimator.Builder().addFloat(this.mDivider, "alpha", 0.0f, 1.0f).addFloat(this.mCarrierText, "alpha", 0.0f, 0.0f, 1.0f).addFloat(this.mMobileGroup, "alpha", 0.0f, 1.0f).addFloat(this.mActionsContainer, "alpha", 0.0f, 1.0f).addFloat(this.mDragHandle, "alpha", 1.0f, 0.0f, 0.0f).addFloat(this.mPageIndicator, "alpha", 0.0f, 1.0f).setStartDelay(0.15f).build();
    }

    @Override
    public void setKeyguardShowing(boolean z) {
        setExpansion(this.mExpansionAmount);
    }

    @Override
    public void setExpandClickListener(View.OnClickListener onClickListener) {
        this.mExpandClickListener = onClickListener;
    }

    @Override
    public void setExpanded(boolean z) {
        if (this.mExpanded == z) {
            return;
        }
        this.mExpanded = z;
        updateEverything();
    }

    @Override
    public void setExpansion(float f) {
        this.mExpansionAmount = f;
        if (this.mSettingsCogAnimator != null) {
            this.mSettingsCogAnimator.setPosition(f);
        }
        if (this.mFooterAnimator != null) {
            this.mFooterAnimator.setPosition(f);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        setListening(false);
        super.onDetachedFromWindow();
    }

    @Override
    public void setListening(boolean z) {
        if (z == this.mListening) {
            return;
        }
        this.mListening = z;
        updateListeners();
    }

    @Override
    public boolean performAccessibilityAction(int i, Bundle bundle) {
        if (i == 262144 && this.mExpandClickListener != null) {
            this.mExpandClickListener.onClick(null);
            return true;
        }
        return super.performAccessibilityAction(i, bundle);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo accessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(accessibilityNodeInfo);
        accessibilityNodeInfo.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_EXPAND);
    }

    @Override
    public void disable(int i, int i2, boolean z) {
        boolean z2 = (i2 & 1) != 0;
        if (z2 == this.mQsDisabled) {
            return;
        }
        this.mQsDisabled = z2;
        updateEverything();
    }

    public void updateEverything() {
        post(new Runnable() {
            @Override
            public final void run() {
                QSFooterImpl.lambda$updateEverything$3(this.f$0);
            }
        });
    }

    public static void lambda$updateEverything$3(QSFooterImpl qSFooterImpl) {
        qSFooterImpl.updateVisibilities();
        qSFooterImpl.setClickable(false);
    }

    private void updateVisibilities() {
        this.mSettingsContainer.setVisibility(this.mQsDisabled ? 8 : 0);
        this.mSettingsContainer.findViewById(com.android.systemui.R.id.tuner_icon).setVisibility(TunerService.isTunerEnabled(this.mContext) ? 0 : 4);
        boolean zIsDeviceInDemoMode = UserManager.isDeviceInDemoMode(this.mContext);
        this.mMultiUserSwitch.setVisibility(showUserSwitcher(zIsDeviceInDemoMode) ? 0 : 4);
        this.mEdit.setVisibility((zIsDeviceInDemoMode || !this.mExpanded) ? 4 : 0);
        this.mSettingsButton.setVisibility((zIsDeviceInDemoMode || !this.mExpanded) ? 4 : 0);
    }

    private boolean showUserSwitcher(boolean z) {
        int i = 0;
        if (!this.mExpanded || z || !UserManager.supportsMultipleUsers()) {
            return false;
        }
        UserManager userManager = UserManager.get(this.mContext);
        if (userManager.hasUserRestriction("no_user_switch")) {
            return false;
        }
        Iterator it = userManager.getUsers(true).iterator();
        while (it.hasNext()) {
            if (((UserInfo) it.next()).supportsSwitchToByUser() && (i = i + 1) > 1) {
                return true;
            }
        }
        return getResources().getBoolean(com.android.systemui.R.bool.qs_show_user_switcher_for_single_user);
    }

    private void updateListeners() {
        if (this.mListening) {
            this.mUserInfoController.addCallback(this);
            if (((NetworkController) Dependency.get(NetworkController.class)).hasVoiceCallingFeature()) {
                ((NetworkController) Dependency.get(NetworkController.class)).addEmergencyListener(this);
                ((NetworkController) Dependency.get(NetworkController.class)).addCallback((NetworkController.SignalCallback) this);
                return;
            }
            return;
        }
        this.mUserInfoController.removeCallback(this);
        ((NetworkController) Dependency.get(NetworkController.class)).removeEmergencyListener(this);
        ((NetworkController) Dependency.get(NetworkController.class)).removeCallback((NetworkController.SignalCallback) this);
    }

    @Override
    public void setQSPanel(QSPanel qSPanel) {
        this.mQsPanel = qSPanel;
        if (this.mQsPanel != null) {
            this.mMultiUserSwitch.setQsPanel(qSPanel);
            this.mQsPanel.setFooterPageIndicator(this.mPageIndicator);
        }
    }

    @Override
    public void onClick(View view) {
        if (this.mExpanded && view == this.mSettingsButton) {
            if (!((DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class)).isCurrentUserSetup()) {
                this.mActivityStarter.postQSRunnableDismissingKeyguard(new Runnable() {
                    @Override
                    public final void run() {
                        QSFooterImpl.lambda$onClick$4();
                    }
                });
                return;
            }
            MetricsLogger.action(this.mContext, this.mExpanded ? 406 : 490);
            if (this.mSettingsButton.isTunerClick()) {
                ((ActivityStarter) Dependency.get(ActivityStarter.class)).postQSRunnableDismissingKeyguard(new Runnable() {
                    @Override
                    public final void run() {
                        QSFooterImpl.lambda$onClick$6(this.f$0);
                    }
                });
            } else {
                startSettingsActivity();
            }
        }
    }

    static void lambda$onClick$4() {
    }

    public static void lambda$onClick$6(final QSFooterImpl qSFooterImpl) {
        if (TunerService.isTunerEnabled(qSFooterImpl.mContext)) {
            TunerService.showResetRequest(qSFooterImpl.mContext, new Runnable() {
                @Override
                public final void run() {
                    this.f$0.startSettingsActivity();
                }
            });
        } else {
            Toast.makeText(qSFooterImpl.getContext(), com.android.systemui.R.string.tuner_toast, 1).show();
            TunerService.setTunerEnabled(qSFooterImpl.mContext, true);
        }
        qSFooterImpl.startSettingsActivity();
    }

    private void startSettingsActivity() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        this.mActivityStarter.startActivity(new Intent("android.settings.SETTINGS"), true);
    }

    @Override
    public void setEmergencyCallsOnly(boolean z) {
        if (z != this.mShowEmergencyCallsOnly) {
            this.mShowEmergencyCallsOnly = z;
            if (this.mExpanded) {
                updateEverything();
            }
        }
    }

    @Override
    public void onUserInfoChanged(String str, Drawable drawable, String str2) {
        if (drawable != null && UserManager.get(this.mContext).isGuestUser(KeyguardUpdateMonitor.getCurrentUser()) && !(drawable instanceof UserIconDrawable)) {
            drawable = drawable.getConstantState().newDrawable(this.mContext.getResources()).mutate();
            drawable.setColorFilter(Utils.getColorAttr(this.mContext, R.attr.colorForeground), PorterDuff.Mode.SRC_IN);
        }
        this.mMultiUserAvatar.setImageDrawable(drawable);
    }

    private void handleUpdateState() {
        this.mMobileGroup.setVisibility(this.mInfo.visible ? 0 : 8);
        if (this.mInfo.visible) {
            this.mMobileRoaming.setVisibility(this.mInfo.roaming ? 0 : 8);
            this.mMobileRoaming.setImageTintList(ColorStateList.valueOf(this.mColorForeground));
            SignalDrawable signalDrawable = new SignalDrawable(this.mContext);
            signalDrawable.setDarkIntensity(QuickStatusBarHeader.getColorIntensity(this.mColorForeground));
            this.mMobileSignal.setImageDrawable(signalDrawable);
            this.mMobileSignal.setImageLevel(this.mStatusBarExt.getCommonSignalIconId(this.mInfo.mobileSignalIconId));
            StringBuilder sb = new StringBuilder();
            if (this.mInfo.contentDescription != null) {
                sb.append(this.mInfo.contentDescription);
                sb.append(", ");
            }
            if (this.mInfo.roaming) {
                sb.append(this.mContext.getString(com.android.systemui.R.string.data_connection_roaming));
                sb.append(", ");
            }
            if (TextUtils.equals(this.mInfo.typeContentDescription, this.mContext.getString(com.android.systemui.R.string.data_connection_no_internet)) || TextUtils.equals(this.mInfo.typeContentDescription, this.mContext.getString(com.android.systemui.R.string.cell_data_off_content_description))) {
                sb.append(this.mInfo.typeContentDescription);
            }
            this.mMobileSignal.setContentDescription(sb);
        }
    }

    @Override
    public void setMobileDataIndicators(NetworkController.IconState iconState, NetworkController.IconState iconState2, int i, int i2, int i3, int i4, boolean z, boolean z2, String str, String str2, boolean z3, int i5, boolean z4, boolean z5) {
        this.mInfo.visible = iconState.visible;
        this.mInfo.mobileSignalIconId = iconState.icon;
        this.mInfo.contentDescription = iconState.contentDescription;
        this.mInfo.typeContentDescription = str;
        this.mInfo.roaming = z4;
        handleUpdateState();
    }

    @Override
    public void setNoSims(boolean z, boolean z2) {
        if (z) {
            this.mInfo.visible = false;
        }
        handleUpdateState();
    }

    private final class CellSignalState {
        public String contentDescription;
        int mobileSignalIconId;
        boolean roaming;
        String typeContentDescription;
        boolean visible;

        private CellSignalState() {
        }
    }
}
