package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.systemui.Dependency;
import com.android.systemui.Dumpable;
import com.android.systemui.R;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import java.io.FileDescriptor;
import java.io.PrintWriter;

public class LightBarController implements Dumpable, BatteryController.BatteryStateChangeCallback {
    private final Color mDarkModeColor;
    private boolean mDockedLight;
    private int mDockedStackVisibility;
    private FingerprintUnlockController mFingerprintUnlockController;
    private boolean mForceDarkForScrim;
    private boolean mFullscreenLight;
    private int mFullscreenStackVisibility;
    private boolean mHasLightNavigationBar;
    private int mLastNavigationBarMode;
    private int mLastStatusBarMode;
    private LightBarTransitionsController mNavigationBarController;
    private boolean mNavigationLight;
    private boolean mQsCustomizing;
    private int mSystemUiVisibility;
    private final Rect mLastFullscreenBounds = new Rect();
    private final Rect mLastDockedBounds = new Rect();
    private final DarkIconDispatcher mStatusBarIconController = (DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class);
    private final BatteryController mBatteryController = (BatteryController) Dependency.get(BatteryController.class);

    public LightBarController(Context context) {
        this.mDarkModeColor = Color.valueOf(context.getColor(R.color.dark_mode_icon_color_single_tone));
        this.mBatteryController.addCallback(this);
    }

    public void setNavigationBar(LightBarTransitionsController lightBarTransitionsController) {
        this.mNavigationBarController = lightBarTransitionsController;
        updateNavigation();
    }

    public void setFingerprintUnlockController(FingerprintUnlockController fingerprintUnlockController) {
        this.mFingerprintUnlockController = fingerprintUnlockController;
    }

    public void onSystemUiVisibilityChanged(int i, int i2, int i3, Rect rect, Rect rect2, boolean z, int i4) {
        int i5 = this.mFullscreenStackVisibility;
        int i6 = ~i3;
        int i7 = (i & i3) | (i5 & i6);
        int i8 = this.mDockedStackVisibility;
        int i9 = (i2 & i3) | (i6 & i8);
        int i10 = i9 ^ i8;
        if (((i5 ^ i7) & 8192) != 0 || (i10 & 8192) != 0 || z || !this.mLastFullscreenBounds.equals(rect) || !this.mLastDockedBounds.equals(rect2)) {
            this.mFullscreenLight = isLight(i7, i4, 8192);
            this.mDockedLight = isLight(i9, i4, 8192);
            updateStatus(rect, rect2);
        }
        this.mFullscreenStackVisibility = i7;
        this.mDockedStackVisibility = i9;
        this.mLastStatusBarMode = i4;
        this.mLastFullscreenBounds.set(rect);
        this.mLastDockedBounds.set(rect2);
    }

    public void onNavigationVisibilityChanged(int i, int i2, boolean z, int i3) {
        int i4 = this.mSystemUiVisibility;
        int i5 = (i2 & i) | ((~i2) & i4);
        if (((i4 ^ i5) & 16) != 0 || z) {
            boolean z2 = this.mNavigationLight;
            this.mHasLightNavigationBar = isLight(i, i3, 16);
            this.mNavigationLight = (!this.mHasLightNavigationBar || this.mForceDarkForScrim || this.mQsCustomizing) ? false : true;
            if (this.mNavigationLight != z2) {
                updateNavigation();
            }
        }
        this.mSystemUiVisibility = i5;
        this.mLastNavigationBarMode = i3;
    }

    private void reevaluate() {
        onSystemUiVisibilityChanged(this.mFullscreenStackVisibility, this.mDockedStackVisibility, 0, this.mLastFullscreenBounds, this.mLastDockedBounds, true, this.mLastStatusBarMode);
        onNavigationVisibilityChanged(this.mSystemUiVisibility, 0, true, this.mLastNavigationBarMode);
    }

    public void setQsCustomizing(boolean z) {
        if (this.mQsCustomizing == z) {
            return;
        }
        this.mQsCustomizing = z;
        reevaluate();
    }

    public void setScrimState(ScrimState scrimState, float f, ColorExtractor.GradientColors gradientColors) {
        boolean z = this.mForceDarkForScrim;
        this.mForceDarkForScrim = (scrimState == ScrimState.BOUNCER || scrimState == ScrimState.BOUNCER_SCRIMMED || f < 0.1f || gradientColors.supportsDarkText()) ? false : true;
        if (this.mHasLightNavigationBar && this.mForceDarkForScrim != z) {
            reevaluate();
        }
    }

    private boolean isLight(int i, int i2, int i3) {
        return (i2 == 4 || i2 == 6) && ((i & i3) != 0);
    }

    private boolean animateChange() {
        int mode;
        return (this.mFingerprintUnlockController == null || (mode = this.mFingerprintUnlockController.getMode()) == 2 || mode == 1) ? false : true;
    }

    private void updateStatus(Rect rect, Rect rect2) {
        boolean z = !rect2.isEmpty();
        if ((this.mFullscreenLight && this.mDockedLight) || (this.mFullscreenLight && !z)) {
            this.mStatusBarIconController.setIconsDarkArea(null);
            this.mStatusBarIconController.getTransitionsController().setIconsDark(true, animateChange());
            return;
        }
        if ((!this.mFullscreenLight && !this.mDockedLight) || (!this.mFullscreenLight && !z)) {
            this.mStatusBarIconController.getTransitionsController().setIconsDark(false, animateChange());
            return;
        }
        if (!this.mFullscreenLight) {
            rect = rect2;
        }
        if (rect.isEmpty()) {
            this.mStatusBarIconController.setIconsDarkArea(null);
        } else {
            this.mStatusBarIconController.setIconsDarkArea(rect);
        }
        this.mStatusBarIconController.getTransitionsController().setIconsDark(true, animateChange());
    }

    private void updateNavigation() {
        if (this.mNavigationBarController != null) {
            this.mNavigationBarController.setIconsDark(this.mNavigationLight, animateChange());
        }
    }

    @Override
    public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
    }

    @Override
    public void onPowerSaveChanged(boolean z) {
        reevaluate();
    }

    @Override
    public void dump(FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr) {
        printWriter.println("LightBarController: ");
        printWriter.print(" mSystemUiVisibility=0x");
        printWriter.print(Integer.toHexString(this.mSystemUiVisibility));
        printWriter.print(" mFullscreenStackVisibility=0x");
        printWriter.print(Integer.toHexString(this.mFullscreenStackVisibility));
        printWriter.print(" mDockedStackVisibility=0x");
        printWriter.println(Integer.toHexString(this.mDockedStackVisibility));
        printWriter.print(" mFullscreenLight=");
        printWriter.print(this.mFullscreenLight);
        printWriter.print(" mDockedLight=");
        printWriter.println(this.mDockedLight);
        printWriter.print(" mLastFullscreenBounds=");
        printWriter.print(this.mLastFullscreenBounds);
        printWriter.print(" mLastDockedBounds=");
        printWriter.println(this.mLastDockedBounds);
        printWriter.print(" mNavigationLight=");
        printWriter.print(this.mNavigationLight);
        printWriter.print(" mHasLightNavigationBar=");
        printWriter.println(this.mHasLightNavigationBar);
        printWriter.print(" mLastStatusBarMode=");
        printWriter.print(this.mLastStatusBarMode);
        printWriter.print(" mLastNavigationBarMode=");
        printWriter.println(this.mLastNavigationBarMode);
        printWriter.print(" mForceDarkForScrim=");
        printWriter.print(this.mForceDarkForScrim);
        printWriter.print(" mQsCustomizing=");
        printWriter.println(this.mQsCustomizing);
        printWriter.println();
        LightBarTransitionsController transitionsController = this.mStatusBarIconController.getTransitionsController();
        if (transitionsController != null) {
            printWriter.println(" StatusBarTransitionsController:");
            transitionsController.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
        }
        if (this.mNavigationBarController != null) {
            printWriter.println(" NavigationBarTransitionsController:");
            this.mNavigationBarController.dump(fileDescriptor, printWriter, strArr);
            printWriter.println();
        }
    }
}
