package com.android.systemui.statusbar.car;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.view.WindowManager;
import com.android.systemui.BatteryMeterView;
import com.android.systemui.R;
import com.android.systemui.recents.Recents;
import com.android.systemui.recents.misc.SystemServicesProxy;
import com.android.systemui.statusbar.car.CarBatteryController;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.PhoneStatusBarView;
import com.android.systemui.statusbar.policy.BatteryController;

public class CarStatusBar extends PhoneStatusBar implements CarBatteryController.BatteryViewHandler {
    private BatteryMeterView mBatteryMeterView;
    private CarBatteryController mCarBatteryController;
    private CarNavigationBarView mCarNavigationBar;
    private CarNavigationBarController mController;
    private FullscreenUserSwitcher mFullscreenUserSwitcher;
    private BroadcastReceiver mPackageChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getData() == null || CarStatusBar.this.mController == null) {
                return;
            }
            String packageName = intent.getData().getSchemeSpecificPart();
            CarStatusBar.this.mController.onPackageChange(packageName);
        }
    };
    private TaskStackListenerImpl mTaskStackListener;

    @Override
    public void start() {
        super.start();
        this.mTaskStackListener = new TaskStackListenerImpl(this, null);
        SystemServicesProxy.getInstance(this.mContext).registerTaskStackListener(this.mTaskStackListener);
        registerPackageChangeReceivers();
        this.mCarBatteryController.startListening();
    }

    @Override
    public void destroy() {
        this.mCarBatteryController.stopListening();
        super.destroy();
    }

    @Override
    protected PhoneStatusBarView makeStatusBarView() {
        PhoneStatusBarView statusBarView = super.makeStatusBarView();
        this.mBatteryMeterView = (BatteryMeterView) statusBarView.findViewById(R.id.battery);
        this.mBatteryMeterView.setVisibility(8);
        if (Log.isLoggable("CarStatusBar", 3)) {
            Log.d("CarStatusBar", "makeStatusBarView(). mBatteryMeterView: " + this.mBatteryMeterView);
        }
        return statusBarView;
    }

    @Override
    protected BatteryController createBatteryController() {
        this.mCarBatteryController = new CarBatteryController(this.mContext);
        this.mCarBatteryController.addBatteryViewHandler(this);
        return this.mCarBatteryController;
    }

    @Override
    protected void addNavigationBar() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(-1, -1, 2019, 25428072, -3);
        lp.setTitle("CarNavigationBar");
        lp.windowAnimations = 0;
        this.mWindowManager.addView(this.mNavigationBarView, lp);
    }

    @Override
    protected void createNavigationBarView(Context context) {
        if (this.mNavigationBarView != null) {
            return;
        }
        this.mCarNavigationBar = (CarNavigationBarView) View.inflate(context, R.layout.car_navigation_bar, null);
        this.mController = new CarNavigationBarController(context, this.mCarNavigationBar, this);
        this.mNavigationBarView = this.mCarNavigationBar;
    }

    @Override
    public void showBatteryView() {
        if (Log.isLoggable("CarStatusBar", 3)) {
            Log.d("CarStatusBar", "showBatteryView(). mBatteryMeterView: " + this.mBatteryMeterView);
        }
        if (this.mBatteryMeterView == null) {
            return;
        }
        this.mBatteryMeterView.setVisibility(0);
    }

    @Override
    public void hideBatteryView() {
        if (Log.isLoggable("CarStatusBar", 3)) {
            Log.d("CarStatusBar", "hideBatteryView(). mBatteryMeterView: " + this.mBatteryMeterView);
        }
        if (this.mBatteryMeterView == null) {
            return;
        }
        this.mBatteryMeterView.setVisibility(8);
    }

    private void registerPackageChangeReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.PACKAGE_ADDED");
        filter.addAction("android.intent.action.PACKAGE_REMOVED");
        filter.addDataScheme("package");
        this.mContext.registerReceiver(this.mPackageChangeReceiver, filter);
    }

    @Override
    protected void repositionNavigationBar() {
    }

    private class TaskStackListenerImpl extends SystemServicesProxy.TaskStackListener {
        TaskStackListenerImpl(CarStatusBar this$0, TaskStackListenerImpl taskStackListenerImpl) {
            this();
        }

        private TaskStackListenerImpl() {
        }

        @Override
        public void onTaskStackChanged() {
            SystemServicesProxy ssp = Recents.getSystemServices();
            ActivityManager.RunningTaskInfo runningTaskInfo = ssp.getRunningTask();
            CarStatusBar.this.mController.taskChanged(runningTaskInfo.baseActivity.getPackageName());
        }
    }

    @Override
    protected void createUserSwitcher() {
        if (this.mUserSwitcherController.useFullscreenUserSwitcher()) {
            this.mFullscreenUserSwitcher = new FullscreenUserSwitcher(this, this.mUserSwitcherController, (ViewStub) this.mStatusBarWindow.findViewById(R.id.fullscreen_user_switcher_stub));
        } else {
            super.createUserSwitcher();
        }
    }

    @Override
    public void userSwitched(int newUserId) {
        super.userSwitched(newUserId);
        if (this.mFullscreenUserSwitcher == null) {
            return;
        }
        this.mFullscreenUserSwitcher.onUserSwitched(newUserId);
    }

    @Override
    public void updateKeyguardState(boolean goingToFullShade, boolean fromShadeLocked) {
        super.updateKeyguardState(goingToFullShade, fromShadeLocked);
        if (this.mFullscreenUserSwitcher == null) {
            return;
        }
        if (this.mState == 3) {
            this.mFullscreenUserSwitcher.show();
        } else {
            this.mFullscreenUserSwitcher.hide();
        }
    }
}
