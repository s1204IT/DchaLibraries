package com.android.systemui.statusbar.car;

import android.app.ActivityManager;
import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.SystemProperties;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.Dependency;
import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.classifier.FalsingLog;
import com.android.systemui.classifier.FalsingManager;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.recents.misc.SysUiTaskStackChangeListener;
import com.android.systemui.shared.system.ActivityManagerWrapper;
import com.android.systemui.statusbar.car.CarBatteryController;
import com.android.systemui.statusbar.car.hvac.HvacController;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.UserSwitcherController;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Map;

public class CarStatusBar extends StatusBar implements CarBatteryController.BatteryViewHandler {
    public static final boolean ENABLE_HVAC_CONNECTION = !SystemProperties.getBoolean("android.car.hvac.demo", true);
    private ActivityManagerWrapper mActivityManagerWrapper;
    private BatteryMeterView mBatteryMeterView;
    private CarBatteryController mCarBatteryController;
    private CarFacetButtonController mCarFacetButtonController;
    private ConnectedDeviceSignalController mConnectedDeviceSignalController;
    private DeviceProvisionedController mDeviceProvisionedController;
    private FullscreenUserSwitcher mFullscreenUserSwitcher;
    private CarNavigationBarView mLeftNavigationBarView;
    private ViewGroup mLeftNavigationBarWindow;
    private CarNavigationBarView mNavigationBarView;
    private ViewGroup mNavigationBarWindow;
    private Drawable mNotificationPanelBackground;
    private CarNavigationBarView mRightNavigationBarView;
    private ViewGroup mRightNavigationBarWindow;
    private boolean mShowBottom;
    private boolean mShowLeft;
    private boolean mShowRight;
    private TaskStackListenerImpl mTaskStackListener;
    private final Object mQueueLock = new Object();
    private boolean mDeviceIsProvisioned = true;

    @Override
    public void start() {
        super.start();
        this.mTaskStackListener = new TaskStackListenerImpl();
        this.mActivityManagerWrapper = ActivityManagerWrapper.getInstance();
        this.mActivityManagerWrapper.registerTaskStackListener(this.mTaskStackListener);
        this.mStackScroller.setScrollingEnabled(true);
        createBatteryController();
        this.mCarBatteryController.startListening();
        if (ENABLE_HVAC_CONNECTION) {
            Log.d("CarStatusBar", "Connecting to HVAC service");
            ((HvacController) Dependency.get(HvacController.class)).connectToCarService();
        }
        this.mCarFacetButtonController = (CarFacetButtonController) Dependency.get(CarFacetButtonController.class);
        this.mDeviceProvisionedController = (DeviceProvisionedController) Dependency.get(DeviceProvisionedController.class);
        this.mDeviceIsProvisioned = this.mDeviceProvisionedController.isDeviceProvisioned();
        if (!this.mDeviceIsProvisioned) {
            this.mDeviceProvisionedController.addCallback(new DeviceProvisionedController.DeviceProvisionedListener() {
                @Override
                public void onDeviceProvisionedChanged() {
                    CarStatusBar.this.mDeviceIsProvisioned = CarStatusBar.this.mDeviceProvisionedController.isDeviceProvisioned();
                    CarStatusBar.this.restartNavBars();
                }
            });
        }
    }

    public void restartNavBars() {
        this.mCarFacetButtonController.removeAll();
        if (ENABLE_HVAC_CONNECTION) {
            ((HvacController) Dependency.get(HvacController.class)).removeAllComponents();
        }
        if (this.mNavigationBarWindow != null) {
            this.mNavigationBarWindow.removeAllViews();
            this.mNavigationBarView = null;
        }
        if (this.mLeftNavigationBarWindow != null) {
            this.mLeftNavigationBarWindow.removeAllViews();
            this.mLeftNavigationBarView = null;
        }
        if (this.mRightNavigationBarWindow != null) {
            this.mRightNavigationBarWindow.removeAllViews();
            this.mRightNavigationBarView = null;
        }
        buildNavBarContent();
    }

    void setNavBarVisibility(int i) {
        if (this.mNavigationBarWindow != null) {
            this.mNavigationBarWindow.setVisibility(i);
        }
        if (this.mLeftNavigationBarWindow != null) {
            this.mLeftNavigationBarWindow.setVisibility(i);
        }
        if (this.mRightNavigationBarWindow != null) {
            this.mRightNavigationBarWindow.setVisibility(i);
        }
    }

    @Override
    public boolean hideKeyguard() {
        boolean zHideKeyguard = super.hideKeyguard();
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.hideKeyguardButtons();
        }
        if (this.mLeftNavigationBarView != null) {
            this.mLeftNavigationBarView.hideKeyguardButtons();
        }
        if (this.mRightNavigationBarView != null) {
            this.mRightNavigationBarView.hideKeyguardButtons();
        }
        return zHideKeyguard;
    }

    @Override
    public void showKeyguard() {
        super.showKeyguard();
        if (this.mNavigationBarView != null) {
            this.mNavigationBarView.showKeyguardButtons();
        }
        if (this.mLeftNavigationBarView != null) {
            this.mLeftNavigationBarView.showKeyguardButtons();
        }
        if (this.mRightNavigationBarView != null) {
            this.mRightNavigationBarView.showKeyguardButtons();
        }
    }

    @Override
    protected void makeStatusBarView() {
        super.makeStatusBarView();
        this.mNotificationPanelBackground = getDefaultWallpaper();
        this.mScrimController.setScrimBehindDrawable(this.mNotificationPanelBackground);
        FragmentHostManager.get(this.mStatusBarWindow).addTagListener("CollapsedStatusBarFragment", new FragmentHostManager.FragmentListener() {
            @Override
            public final void onFragmentViewCreated(String str, Fragment fragment) {
                CarStatusBar.lambda$makeStatusBarView$0(this.f$0, str, fragment);
            }
        });
    }

    public static void lambda$makeStatusBarView$0(CarStatusBar carStatusBar, String str, Fragment fragment) {
        carStatusBar.mBatteryMeterView = (BatteryMeterView) fragment.getView().findViewById(R.id.battery);
        carStatusBar.mBatteryMeterView.setVisibility(8);
    }

    private BatteryController createBatteryController() {
        this.mCarBatteryController = new CarBatteryController(this.mContext);
        this.mCarBatteryController.addBatteryViewHandler(this);
        return this.mCarBatteryController;
    }

    @Override
    protected void createNavigationBar() {
        this.mShowBottom = this.mContext.getResources().getBoolean(R.bool.config_enableBottomNavigationBar);
        this.mShowLeft = this.mContext.getResources().getBoolean(R.bool.config_enableLeftNavigationBar);
        this.mShowRight = this.mContext.getResources().getBoolean(R.bool.config_enableRightNavigationBar);
        buildNavBarWindows();
        buildNavBarContent();
        attachNavBarWindows();
    }

    private void buildNavBarContent() {
        if (this.mShowBottom) {
            buildBottomBar(this.mDeviceIsProvisioned ? R.layout.car_navigation_bar : R.layout.car_navigation_bar_unprovisioned);
        }
        if (this.mShowLeft) {
            buildLeft(this.mDeviceIsProvisioned ? R.layout.car_left_navigation_bar : R.layout.car_left_navigation_bar_unprovisioned);
        }
        if (this.mShowRight) {
            buildRight(this.mDeviceIsProvisioned ? R.layout.car_right_navigation_bar : R.layout.car_right_navigation_bar_unprovisioned);
        }
    }

    private void buildNavBarWindows() {
        if (this.mShowBottom) {
            this.mNavigationBarWindow = (ViewGroup) View.inflate(this.mContext, R.layout.navigation_bar_window, null);
        }
        if (this.mShowLeft) {
            this.mLeftNavigationBarWindow = (ViewGroup) View.inflate(this.mContext, R.layout.navigation_bar_window, null);
        }
        if (this.mShowRight) {
            this.mRightNavigationBarWindow = (ViewGroup) View.inflate(this.mContext, R.layout.navigation_bar_window, null);
        }
    }

    private void attachNavBarWindows() {
        if (this.mShowBottom) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams(-1, -1, 2019, 8650792, -3);
            layoutParams.setTitle("CarNavigationBar");
            layoutParams.windowAnimations = 0;
            this.mWindowManager.addView(this.mNavigationBarWindow, layoutParams);
        }
        if (this.mShowLeft) {
            WindowManager.LayoutParams layoutParams2 = new WindowManager.LayoutParams(this.mContext.getResources().getDimensionPixelSize(R.dimen.car_left_navigation_bar_width), -1, 2024, 8650792, -3);
            layoutParams2.setTitle("LeftCarNavigationBar");
            layoutParams2.windowAnimations = 0;
            layoutParams2.privateFlags |= 4194304;
            layoutParams2.gravity = 3;
            this.mWindowManager.addView(this.mLeftNavigationBarWindow, layoutParams2);
        }
        if (this.mShowRight) {
            WindowManager.LayoutParams layoutParams3 = new WindowManager.LayoutParams(this.mContext.getResources().getDimensionPixelSize(R.dimen.car_right_navigation_bar_width), -1, 2024, 8650792, -3);
            layoutParams3.setTitle("RightCarNavigationBar");
            layoutParams3.windowAnimations = 0;
            layoutParams3.privateFlags |= 4194304;
            layoutParams3.gravity = 5;
            this.mWindowManager.addView(this.mRightNavigationBarWindow, layoutParams3);
        }
    }

    private void buildBottomBar(int i) {
        View.inflate(this.mContext, i, this.mNavigationBarWindow);
        this.mNavigationBarView = (CarNavigationBarView) this.mNavigationBarWindow.getChildAt(0);
        if (this.mNavigationBarView == null) {
            Log.e("CarStatusBar", "CarStatusBar failed inflate for R.layout.car_navigation_bar");
            throw new RuntimeException("Unable to build botom nav bar due to missing layout");
        }
        this.mNavigationBarView.setStatusBar(this);
    }

    private void buildLeft(int i) {
        View.inflate(this.mContext, i, this.mLeftNavigationBarWindow);
        this.mLeftNavigationBarView = (CarNavigationBarView) this.mLeftNavigationBarWindow.getChildAt(0);
        if (this.mLeftNavigationBarView == null) {
            Log.e("CarStatusBar", "CarStatusBar failed inflate for R.layout.car_navigation_bar");
            throw new RuntimeException("Unable to build left nav bar due to missing layout");
        }
        this.mLeftNavigationBarView.setStatusBar(this);
    }

    private void buildRight(int i) {
        View.inflate(this.mContext, i, this.mRightNavigationBarWindow);
        this.mRightNavigationBarView = (CarNavigationBarView) this.mRightNavigationBarWindow.getChildAt(0);
        if (this.mRightNavigationBarView == null) {
            Log.e("CarStatusBar", "CarStatusBar failed inflate for R.layout.car_navigation_bar");
            throw new RuntimeException("Unable to build right nav bar due to missing layout");
        }
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        synchronized (this.mQueueLock) {
            printWriter.println("  mStackScroller: " + viewInfo(this.mStackScroller));
            printWriter.println("  mStackScroller: " + viewInfo(this.mStackScroller) + " scroll " + this.mStackScroller.getScrollX() + "," + this.mStackScroller.getScrollY());
        }
        printWriter.print("  mTaskStackListener=");
        printWriter.println(this.mTaskStackListener);
        printWriter.print("  mCarFacetButtonController=");
        printWriter.println(this.mCarFacetButtonController);
        printWriter.print("  mFullscreenUserSwitcher=");
        printWriter.println(this.mFullscreenUserSwitcher);
        printWriter.print("  mCarBatteryController=");
        printWriter.println(this.mCarBatteryController);
        printWriter.print("  mBatteryMeterView=");
        printWriter.println(this.mBatteryMeterView);
        printWriter.print("  mConnectedDeviceSignalController=");
        printWriter.println(this.mConnectedDeviceSignalController);
        printWriter.print("  mNavigationBarView=");
        printWriter.println(this.mNavigationBarView);
        if (KeyguardUpdateMonitor.getInstance(this.mContext) != null) {
            KeyguardUpdateMonitor.getInstance(this.mContext).dump(fileDescriptor, printWriter, strArr);
        }
        FalsingManager.getInstance(this.mContext).dump(printWriter);
        FalsingLog.dump(printWriter);
        printWriter.println("SharedPreferences:");
        for (Map.Entry<String, ?> entry : Prefs.getAll(this.mContext).entrySet()) {
            printWriter.print("  ");
            printWriter.print(entry.getKey());
            printWriter.print("=");
            printWriter.println(entry.getValue());
        }
    }

    @Override
    protected View.OnTouchListener getStatusBarWindowTouchListener() {
        return null;
    }

    @Override
    public void showBatteryView() {
        if (Log.isLoggable("CarStatusBar", 3)) {
            Log.d("CarStatusBar", "showBatteryView(). mBatteryMeterView: " + this.mBatteryMeterView);
        }
        if (this.mBatteryMeterView != null) {
            this.mBatteryMeterView.setVisibility(0);
        }
    }

    @Override
    public void hideBatteryView() {
        if (Log.isLoggable("CarStatusBar", 3)) {
            Log.d("CarStatusBar", "hideBatteryView(). mBatteryMeterView: " + this.mBatteryMeterView);
        }
        if (this.mBatteryMeterView != null) {
            this.mBatteryMeterView.setVisibility(8);
        }
    }

    private class TaskStackListenerImpl extends SysUiTaskStackChangeListener {
        private TaskStackListenerImpl() {
        }

        @Override
        public void onTaskStackChanged() {
            try {
                CarStatusBar.this.mCarFacetButtonController.taskChanged(ActivityManager.getService().getAllStackInfos());
            } catch (Exception e) {
                Log.e("CarStatusBar", "Getting StackInfo from activity manager failed", e);
            }
        }
    }

    @Override
    protected void createUserSwitcher() {
        if (((UserSwitcherController) Dependency.get(UserSwitcherController.class)).useFullscreenUserSwitcher()) {
            this.mFullscreenUserSwitcher = new FullscreenUserSwitcher(this, (ViewStub) this.mStatusBarWindow.findViewById(R.id.fullscreen_user_switcher_stub), this.mContext);
        } else {
            super.createUserSwitcher();
        }
    }

    @Override
    public void onUserSwitched(int i) {
        super.onUserSwitched(i);
        if (this.mFullscreenUserSwitcher != null) {
            this.mFullscreenUserSwitcher.onUserSwitched(i);
        }
    }

    @Override
    public void updateKeyguardState(boolean z, boolean z2) {
        super.updateKeyguardState(z, z2);
        if (this.mFullscreenUserSwitcher != null) {
            if (this.mState == 3) {
                this.mFullscreenUserSwitcher.show();
            } else {
                this.mFullscreenUserSwitcher.hide();
            }
        }
    }

    @Override
    public void updateMediaMetaData(boolean z, boolean z2) {
    }

    @Override
    public void animateExpandNotificationsPanel() {
        this.mHeadsUpManager.releaseAllImmediately();
        super.animateExpandNotificationsPanel();
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        super.onDensityOrFontScaleChanged();
        this.mNotificationPanelBackground = getDefaultWallpaper();
        this.mScrimController.setScrimBehindDrawable(this.mNotificationPanelBackground);
    }

    private Drawable getDefaultWallpaper() {
        return this.mContext.getDrawable(android.R.drawable.btn_star_big_on_disable_focused);
    }
}
