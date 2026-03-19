package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.DemoMode;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.StatusBarMobileView;
import com.android.systemui.statusbar.StatusBarWifiView;
import com.android.systemui.statusbar.StatusIconDisplayable;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.util.Utils;
import java.util.List;

public interface StatusBarIconController {
    void addIconGroup(IconManager iconManager);

    void removeAllIconsForSlot(String str);

    void removeIconGroup(IconManager iconManager);

    void setExternalIcon(String str);

    void setIcon(String str, int i, CharSequence charSequence);

    void setIcon(String str, StatusBarIcon statusBarIcon);

    void setIconVisibility(String str, boolean z);

    void setMobileIcons(String str, List<StatusBarSignalPolicy.MobileIconState> list);

    void setSignalIcon(String str, StatusBarSignalPolicy.WifiIconState wifiIconState);

    static ArraySet<String> getIconBlacklist(String str) {
        ArraySet<String> arraySet = new ArraySet<>();
        if (str == null) {
            str = "rotate,";
        }
        for (String str2 : str.split(",")) {
            if (!TextUtils.isEmpty(str2)) {
                arraySet.add(str2);
            }
        }
        return arraySet;
    }

    public static class DarkIconManager extends IconManager {
        private final DarkIconDispatcher mDarkIconDispatcher;
        private int mIconHPadding;

        public DarkIconManager(LinearLayout linearLayout) {
            super(linearLayout);
            this.mIconHPadding = this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_padding);
            this.mDarkIconDispatcher = (DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class);
        }

        @Override
        protected void onIconAdded(int i, String str, boolean z, StatusBarIconHolder statusBarIconHolder) {
            this.mDarkIconDispatcher.addDarkReceiver(addHolder(i, str, z, statusBarIconHolder));
        }

        @Override
        protected LinearLayout.LayoutParams onCreateLayoutParams() {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(-2, this.mIconSize);
            layoutParams.setMargins(this.mIconHPadding, 0, this.mIconHPadding, 0);
            return layoutParams;
        }

        @Override
        protected void destroy() {
            for (int i = 0; i < this.mGroup.getChildCount(); i++) {
                this.mDarkIconDispatcher.removeDarkReceiver((DarkIconDispatcher.DarkReceiver) this.mGroup.getChildAt(i));
            }
            this.mGroup.removeAllViews();
        }

        @Override
        protected void onRemoveIcon(int i) {
            this.mDarkIconDispatcher.removeDarkReceiver((DarkIconDispatcher.DarkReceiver) this.mGroup.getChildAt(i));
            super.onRemoveIcon(i);
        }

        @Override
        public void onSetIcon(int i, StatusBarIcon statusBarIcon) {
            super.onSetIcon(i, statusBarIcon);
            this.mDarkIconDispatcher.applyDark((DarkIconDispatcher.DarkReceiver) this.mGroup.getChildAt(i));
        }

        @Override
        protected DemoStatusIcons createDemoStatusIcons() {
            DemoStatusIcons demoStatusIconsCreateDemoStatusIcons = super.createDemoStatusIcons();
            this.mDarkIconDispatcher.addDarkReceiver(demoStatusIconsCreateDemoStatusIcons);
            return demoStatusIconsCreateDemoStatusIcons;
        }

        @Override
        protected void exitDemoMode() {
            this.mDarkIconDispatcher.removeDarkReceiver(this.mDemoStatusIcons);
            super.exitDemoMode();
        }
    }

    public static class TintedIconManager extends IconManager {
        private int mColor;

        public TintedIconManager(ViewGroup viewGroup) {
            super(viewGroup);
        }

        @Override
        protected void onIconAdded(int i, String str, boolean z, StatusBarIconHolder statusBarIconHolder) {
            StatusIconDisplayable statusIconDisplayableAddHolder = addHolder(i, str, z, statusBarIconHolder);
            statusIconDisplayableAddHolder.setStaticDrawableColor(this.mColor);
            statusIconDisplayableAddHolder.setDecorColor(this.mColor);
        }

        public void setTint(int i) {
            this.mColor = i;
            for (int i2 = 0; i2 < this.mGroup.getChildCount(); i2++) {
                KeyEvent.Callback childAt = this.mGroup.getChildAt(i2);
                if (childAt instanceof StatusIconDisplayable) {
                    StatusIconDisplayable statusIconDisplayable = (StatusIconDisplayable) childAt;
                    statusIconDisplayable.setStaticDrawableColor(this.mColor);
                    statusIconDisplayable.setDecorColor(this.mColor);
                }
            }
        }

        @Override
        protected DemoStatusIcons createDemoStatusIcons() {
            DemoStatusIcons demoStatusIconsCreateDemoStatusIcons = super.createDemoStatusIcons();
            demoStatusIconsCreateDemoStatusIcons.setColor(this.mColor);
            return demoStatusIconsCreateDemoStatusIcons;
        }
    }

    public static class IconManager implements DemoMode {
        protected final Context mContext;
        protected DemoStatusIcons mDemoStatusIcons;
        protected final ViewGroup mGroup;
        protected final int mIconSize;
        private boolean mIsInDemoMode;
        protected boolean mShouldLog = false;
        protected boolean mDemoable = true;

        public IconManager(ViewGroup viewGroup) {
            this.mGroup = viewGroup;
            this.mContext = viewGroup.getContext();
            this.mIconSize = this.mContext.getResources().getDimensionPixelSize(android.R.dimen.handwriting_bounds_offset_left);
            Utils.DisableStateTracker disableStateTracker = new Utils.DisableStateTracker(0, 2);
            this.mGroup.addOnAttachStateChangeListener(disableStateTracker);
            if (this.mGroup.isAttachedToWindow()) {
                disableStateTracker.onViewAttachedToWindow(this.mGroup);
            }
        }

        public boolean isDemoable() {
            return this.mDemoable;
        }

        public void setShouldLog(boolean z) {
            this.mShouldLog = z;
        }

        public boolean shouldLog() {
            return this.mShouldLog;
        }

        protected void onIconAdded(int i, String str, boolean z, StatusBarIconHolder statusBarIconHolder) {
            addHolder(i, str, z, statusBarIconHolder);
        }

        protected StatusIconDisplayable addHolder(int i, String str, boolean z, StatusBarIconHolder statusBarIconHolder) {
            switch (statusBarIconHolder.getType()) {
                case 0:
                    return addIcon(i, str, z, statusBarIconHolder.getIcon());
                case 1:
                    return addSignalIcon(i, str, statusBarIconHolder.getWifiState());
                case 2:
                    return addMobileIcon(i, str, statusBarIconHolder.getMobileState());
                default:
                    return null;
            }
        }

        protected StatusBarIconView addIcon(int i, String str, boolean z, StatusBarIcon statusBarIcon) {
            StatusBarIconView statusBarIconViewOnCreateStatusBarIconView = onCreateStatusBarIconView(str, z);
            statusBarIconViewOnCreateStatusBarIconView.set(statusBarIcon);
            this.mGroup.addView(statusBarIconViewOnCreateStatusBarIconView, i, onCreateLayoutParams());
            return statusBarIconViewOnCreateStatusBarIconView;
        }

        protected StatusBarWifiView addSignalIcon(int i, String str, StatusBarSignalPolicy.WifiIconState wifiIconState) {
            StatusBarWifiView statusBarWifiViewOnCreateStatusBarWifiView = onCreateStatusBarWifiView(str);
            statusBarWifiViewOnCreateStatusBarWifiView.applyWifiState(wifiIconState);
            this.mGroup.addView(statusBarWifiViewOnCreateStatusBarWifiView, i, onCreateLayoutParams());
            if (this.mIsInDemoMode) {
                this.mDemoStatusIcons.addDemoWifiView(wifiIconState);
            }
            return statusBarWifiViewOnCreateStatusBarWifiView;
        }

        protected StatusBarMobileView addMobileIcon(int i, String str, StatusBarSignalPolicy.MobileIconState mobileIconState) {
            StatusBarMobileView statusBarMobileViewOnCreateStatusBarMobileView = onCreateStatusBarMobileView(str);
            statusBarMobileViewOnCreateStatusBarMobileView.applyMobileState(mobileIconState);
            this.mGroup.addView(statusBarMobileViewOnCreateStatusBarMobileView, i, onCreateLayoutParams());
            if (this.mIsInDemoMode) {
                this.mDemoStatusIcons.addMobileView(mobileIconState);
            }
            return statusBarMobileViewOnCreateStatusBarMobileView;
        }

        private StatusBarIconView onCreateStatusBarIconView(String str, boolean z) {
            return new StatusBarIconView(this.mContext, str, null, z);
        }

        private StatusBarWifiView onCreateStatusBarWifiView(String str) {
            return StatusBarWifiView.fromContext(this.mContext, str);
        }

        private StatusBarMobileView onCreateStatusBarMobileView(String str) {
            return StatusBarMobileView.fromContext(this.mContext, str);
        }

        protected LinearLayout.LayoutParams onCreateLayoutParams() {
            return new LinearLayout.LayoutParams(-2, this.mIconSize);
        }

        protected void destroy() {
            this.mGroup.removeAllViews();
        }

        protected void onIconExternal(int i, int i2) {
            ImageView imageView = (ImageView) this.mGroup.getChildAt(i);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setAdjustViewBounds(true);
            setHeightAndCenter(imageView, i2);
        }

        private void setHeightAndCenter(ImageView imageView, int i) {
            ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
            layoutParams.height = i;
            if (layoutParams instanceof LinearLayout.LayoutParams) {
                ((LinearLayout.LayoutParams) layoutParams).gravity = 16;
            }
            imageView.setLayoutParams(layoutParams);
        }

        protected void onRemoveIcon(int i) {
            if (this.mIsInDemoMode) {
                this.mDemoStatusIcons.onRemoveIcon((StatusIconDisplayable) this.mGroup.getChildAt(i));
            }
            this.mGroup.removeViewAt(i);
        }

        public void onSetIcon(int i, StatusBarIcon statusBarIcon) {
            ((StatusBarIconView) this.mGroup.getChildAt(i)).set(statusBarIcon);
        }

        public void onSetIconHolder(int i, StatusBarIconHolder statusBarIconHolder) {
            switch (statusBarIconHolder.getType()) {
                case 0:
                    onSetIcon(i, statusBarIconHolder.getIcon());
                    break;
                case 1:
                    onSetSignalIcon(i, statusBarIconHolder.getWifiState());
                    break;
                case 2:
                    onSetMobileIcon(i, statusBarIconHolder.getMobileState());
                    break;
            }
        }

        public void onSetSignalIcon(int i, StatusBarSignalPolicy.WifiIconState wifiIconState) {
            StatusBarWifiView statusBarWifiView = (StatusBarWifiView) this.mGroup.getChildAt(i);
            if (statusBarWifiView != null) {
                statusBarWifiView.applyWifiState(wifiIconState);
            }
            if (this.mIsInDemoMode) {
                this.mDemoStatusIcons.updateWifiState(wifiIconState);
            }
        }

        public void onSetMobileIcon(int i, StatusBarSignalPolicy.MobileIconState mobileIconState) {
            StatusBarMobileView statusBarMobileView = (StatusBarMobileView) this.mGroup.getChildAt(i);
            if (statusBarMobileView != null) {
                statusBarMobileView.applyMobileState(mobileIconState);
            }
            if (this.mIsInDemoMode) {
                this.mDemoStatusIcons.updateMobileState(mobileIconState);
            }
        }

        @Override
        public void dispatchDemoCommand(String str, Bundle bundle) {
            if (!this.mDemoable) {
                return;
            }
            if (str.equals("exit")) {
                if (this.mDemoStatusIcons != null) {
                    this.mDemoStatusIcons.dispatchDemoCommand(str, bundle);
                    exitDemoMode();
                }
                this.mIsInDemoMode = false;
                return;
            }
            if (this.mDemoStatusIcons == null) {
                this.mIsInDemoMode = true;
                this.mDemoStatusIcons = createDemoStatusIcons();
            }
            this.mDemoStatusIcons.dispatchDemoCommand(str, bundle);
        }

        protected void exitDemoMode() {
            this.mDemoStatusIcons.remove();
            this.mDemoStatusIcons = null;
        }

        protected DemoStatusIcons createDemoStatusIcons() {
            return new DemoStatusIcons((LinearLayout) this.mGroup, this.mIconSize);
        }
    }
}
