package com.android.systemui.statusbar.phone;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.LinearLayout;
import com.android.systemui.Dependency;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.SysUiServiceProvider;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.EncryptionHelper;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.NetworkController;

public class CollapsedStatusBarFragment extends Fragment implements CommandQueue.Callbacks {
    private View mClockView;
    private StatusBarIconController.DarkIconManager mDarkIconManager;
    private int mDisabled1;
    private KeyguardMonitor mKeyguardMonitor;
    private NetworkController mNetworkController;
    private View mNotificationIconAreaInner;
    private View mOperatorNameFrame;
    private NetworkController.SignalCallback mSignalCallback = new NetworkController.SignalCallback() {
        @Override
        public void setIsAirplaneMode(NetworkController.IconState iconState) {
            CollapsedStatusBarFragment.this.mStatusBarComponent.recomputeDisableFlags(true);
        }
    };
    private PhoneStatusBarView mStatusBar;
    private StatusBar mStatusBarComponent;
    private LinearLayout mSystemIconArea;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mKeyguardMonitor = (KeyguardMonitor) Dependency.get(KeyguardMonitor.class);
        this.mNetworkController = (NetworkController) Dependency.get(NetworkController.class);
        this.mStatusBarComponent = (StatusBar) SysUiServiceProvider.getComponent(getContext(), StatusBar.class);
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return layoutInflater.inflate(R.layout.status_bar, viewGroup, false);
    }

    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        this.mStatusBar = (PhoneStatusBarView) view;
        if (bundle != null && bundle.containsKey("panel_state")) {
            this.mStatusBar.go(bundle.getInt("panel_state"));
        }
        this.mDarkIconManager = new StatusBarIconController.DarkIconManager((LinearLayout) view.findViewById(R.id.statusIcons));
        this.mDarkIconManager.setShouldLog(true);
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).addIconGroup(this.mDarkIconManager);
        this.mSystemIconArea = (LinearLayout) this.mStatusBar.findViewById(R.id.system_icon_area);
        this.mClockView = this.mStatusBar.findViewById(R.id.clock);
        showSystemIconArea(false);
        showClock(false);
        initEmergencyCryptkeeperText();
        initOperatorName();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putInt("panel_state", this.mStatusBar.getState());
    }

    @Override
    public void onResume() {
        super.onResume();
        ((CommandQueue) SysUiServiceProvider.getComponent(getContext(), CommandQueue.class)).addCallbacks(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        ((CommandQueue) SysUiServiceProvider.getComponent(getContext(), CommandQueue.class)).removeCallbacks(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ((StatusBarIconController) Dependency.get(StatusBarIconController.class)).removeIconGroup(this.mDarkIconManager);
        if (this.mNetworkController.hasEmergencyCryptKeeperText()) {
            this.mNetworkController.removeCallback(this.mSignalCallback);
        }
    }

    public void initNotificationIconArea(NotificationIconAreaController notificationIconAreaController) {
        ViewGroup viewGroup = (ViewGroup) this.mStatusBar.findViewById(R.id.notification_icon_area);
        this.mNotificationIconAreaInner = notificationIconAreaController.getNotificationInnerAreaView();
        if (this.mNotificationIconAreaInner.getParent() != null) {
            ((ViewGroup) this.mNotificationIconAreaInner.getParent()).removeView(this.mNotificationIconAreaInner);
        }
        viewGroup.addView(this.mNotificationIconAreaInner);
        showNotificationIconArea(false);
    }

    @Override
    public void disable(int i, int i2, boolean z) {
        int iAdjustDisableFlags = adjustDisableFlags(i);
        int i3 = this.mDisabled1 ^ iAdjustDisableFlags;
        this.mDisabled1 = iAdjustDisableFlags;
        if ((i3 & 1048576) != 0) {
            if ((1048576 & iAdjustDisableFlags) != 0) {
                hideSystemIconArea(z);
                hideOperatorName(z);
            } else {
                showSystemIconArea(z);
                showOperatorName(z);
            }
        }
        if ((i3 & 131072) != 0) {
            if ((131072 & iAdjustDisableFlags) != 0) {
                hideNotificationIconArea(z);
            } else {
                showNotificationIconArea(z);
            }
        }
        if ((i3 & 8388608) != 0 || this.mClockView.getVisibility() != clockHiddenMode()) {
            if ((iAdjustDisableFlags & 8388608) != 0) {
                hideClock(z);
            } else {
                showClock(z);
            }
        }
    }

    protected int adjustDisableFlags(int i) {
        if (!this.mStatusBarComponent.isLaunchTransitionFadingAway() && !this.mKeyguardMonitor.isKeyguardFadingAway() && shouldHideNotificationIcons()) {
            i = i | 131072 | 1048576 | 8388608;
        }
        if (this.mNetworkController != null && EncryptionHelper.IS_DATA_ENCRYPTED) {
            if (this.mNetworkController.hasEmergencyCryptKeeperText()) {
                i |= 131072;
            }
            if (!this.mNetworkController.isRadioOn()) {
                return i | 1048576;
            }
            return i;
        }
        return i;
    }

    private boolean shouldHideNotificationIcons() {
        return (!this.mStatusBar.isClosed() && this.mStatusBarComponent.hideStatusBarIconsWhenExpanded()) || this.mStatusBarComponent.hideStatusBarIconsForBouncer();
    }

    public void hideSystemIconArea(boolean z) {
        animateHide(this.mSystemIconArea, z);
    }

    public void showSystemIconArea(boolean z) {
        animateShow(this.mSystemIconArea, z);
    }

    public void hideClock(boolean z) {
        animateHiddenState(this.mClockView, clockHiddenMode(), z);
    }

    public void showClock(boolean z) {
        animateShow(this.mClockView, z);
    }

    private int clockHiddenMode() {
        if (!this.mStatusBar.isClosed() && !this.mKeyguardMonitor.isShowing()) {
            return 4;
        }
        return 8;
    }

    public void hideNotificationIconArea(boolean z) {
        animateHide(this.mNotificationIconAreaInner, z);
    }

    public void showNotificationIconArea(boolean z) {
        animateShow(this.mNotificationIconAreaInner, z);
    }

    public void hideOperatorName(boolean z) {
        if (this.mOperatorNameFrame != null) {
            animateHide(this.mOperatorNameFrame, z);
        }
    }

    public void showOperatorName(boolean z) {
        if (this.mOperatorNameFrame != null) {
            animateShow(this.mOperatorNameFrame, z);
        }
    }

    private void animateHiddenState(final View view, final int i, boolean z) {
        view.animate().cancel();
        if (!z) {
            view.setAlpha(0.0f);
            view.setVisibility(i);
        } else {
            view.animate().alpha(0.0f).setDuration(160L).setStartDelay(0L).setInterpolator(Interpolators.ALPHA_OUT).withEndAction(new Runnable() {
                @Override
                public final void run() {
                    view.setVisibility(i);
                }
            });
        }
    }

    private void animateHide(View view, boolean z) {
        animateHiddenState(view, 4, z);
    }

    private void animateShow(View view, boolean z) {
        view.animate().cancel();
        view.setVisibility(0);
        if (!z) {
            view.setAlpha(1.0f);
            return;
        }
        view.animate().alpha(1.0f).setDuration(320L).setInterpolator(Interpolators.ALPHA_IN).setStartDelay(50L).withEndAction(null);
        if (this.mKeyguardMonitor.isKeyguardFadingAway()) {
            view.animate().setDuration(this.mKeyguardMonitor.getKeyguardFadingAwayDuration()).setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN).setStartDelay(this.mKeyguardMonitor.getKeyguardFadingAwayDelay()).start();
        }
    }

    private void initEmergencyCryptkeeperText() {
        View viewFindViewById = this.mStatusBar.findViewById(R.id.emergency_cryptkeeper_text);
        if (this.mNetworkController.hasEmergencyCryptKeeperText()) {
            if (viewFindViewById != null) {
                ((ViewStub) viewFindViewById).inflate();
            }
            this.mNetworkController.addCallback(this.mSignalCallback);
        } else if (viewFindViewById != null) {
            ((ViewGroup) viewFindViewById.getParent()).removeView(viewFindViewById);
        }
    }

    private void initOperatorName() {
        if (getResources().getBoolean(R.bool.config_showOperatorNameInStatusBar)) {
            this.mOperatorNameFrame = ((ViewStub) this.mStatusBar.findViewById(R.id.operator_name)).inflate();
        }
    }
}
