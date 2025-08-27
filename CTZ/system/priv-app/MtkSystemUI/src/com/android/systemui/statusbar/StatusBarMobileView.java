package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.SystemProperties;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.annotations.VisibleForTesting;
import com.android.settingslib.graph.SignalDrawable;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.mediatek.systemui.ext.ISystemUIStatusBarExt;
import com.mediatek.systemui.ext.OpSystemUICustomizationFactoryBase;
import com.mediatek.systemui.statusbar.util.FeatureOptions;

/* loaded from: classes.dex */
public class StatusBarMobileView extends FrameLayout implements StatusIconDisplayable, DarkIconDispatcher.DarkReceiver {
    private static final boolean DEBUG;
    private StatusBarIconView mDotView;
    private ImageView mIn;
    private View mInoutContainer;
    private boolean mIsWfcCase;
    private boolean mIsWfcEnable;
    private ImageView mMobile;
    private SignalDrawable mMobileDrawable;
    private LinearLayout mMobileGroup;
    private ImageView mMobileRoaming;
    private ImageView mMobileType;
    private ImageView mNetworkType;
    private ImageView mOut;
    private String mSlot;
    private StatusBarSignalPolicy.MobileIconState mState;
    private ISystemUIStatusBarExt mStatusBarExt;
    private int mVisibleState;
    private ImageView mVolteType;

    static {
        DEBUG = Log.isLoggable("StatusBarMobileView", 3) || FeatureOptions.LOG_ENABLE;
    }

    public static StatusBarMobileView fromContext(Context context, String str) throws Resources.NotFoundException {
        StatusBarMobileView statusBarMobileView = (StatusBarMobileView) LayoutInflater.from(context).inflate(R.layout.status_bar_mobile_signal_group, (ViewGroup) null);
        statusBarMobileView.setSlot(str);
        statusBarMobileView.init();
        statusBarMobileView.setVisibleState(0);
        return statusBarMobileView;
    }

    public StatusBarMobileView(Context context) {
        super(context);
        this.mVisibleState = -1;
    }

    public StatusBarMobileView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mVisibleState = -1;
    }

    public StatusBarMobileView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mVisibleState = -1;
    }

    public StatusBarMobileView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mVisibleState = -1;
    }

    @Override // android.view.View
    public void getDrawingRect(Rect rect) {
        super.getDrawingRect(rect);
        float translationX = getTranslationX();
        float translationY = getTranslationY();
        rect.left = (int) (rect.left + translationX);
        rect.right = (int) (rect.right + translationX);
        rect.top = (int) (rect.top + translationY);
        rect.bottom = (int) (rect.bottom + translationY);
    }

    private void init() throws Resources.NotFoundException {
        this.mMobileGroup = (LinearLayout) findViewById(R.id.mobile_group);
        this.mMobile = (ImageView) findViewById(R.id.mobile_signal);
        this.mMobileType = (ImageView) findViewById(R.id.mobile_type);
        this.mMobileRoaming = (ImageView) findViewById(R.id.mobile_roaming);
        this.mIn = (ImageView) findViewById(R.id.mobile_in);
        this.mOut = (ImageView) findViewById(R.id.mobile_out);
        this.mInoutContainer = findViewById(R.id.inout_container);
        this.mNetworkType = (ImageView) findViewById(R.id.network_type);
        this.mVolteType = (ImageView) findViewById(R.id.volte_indicator_ext);
        this.mMobileDrawable = new SignalDrawable(getContext());
        this.mMobile.setImageDrawable(this.mMobileDrawable);
        initDotView();
        this.mIsWfcEnable = SystemProperties.get("persist.vendor.mtk_wfc_support").equals("1");
        this.mStatusBarExt = OpSystemUICustomizationFactoryBase.getOpFactory(this.mContext).makeSystemUIStatusBar(this.mContext);
    }

    private void initDotView() throws Resources.NotFoundException {
        this.mDotView = new StatusBarIconView(this.mContext, this.mSlot, null);
        this.mDotView.setVisibleState(1);
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_size);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dimensionPixelSize, dimensionPixelSize);
        layoutParams.gravity = 8388627;
        addView(this.mDotView, layoutParams);
    }

    public void applyMobileState(StatusBarSignalPolicy.MobileIconState mobileIconState) {
        if (DEBUG) {
            Log.d(getMobileTag(), "[" + hashCode() + "][visibility=" + getVisibility() + "] applyMobileState: state = " + mobileIconState);
        }
        if (mobileIconState == null) {
            setVisibility(8);
            this.mState = null;
        } else if (this.mState == null) {
            this.mState = mobileIconState.copy();
            initViewState();
        } else if (!this.mState.equals(mobileIconState)) {
            updateState(mobileIconState.copy());
        }
    }

    private void initViewState() {
        setContentDescription(this.mState.contentDescription);
        int i = 0;
        if (!this.mState.visible) {
            this.mMobileGroup.setVisibility(8);
        } else {
            this.mMobileGroup.setVisibility(0);
        }
        this.mMobileDrawable.setLevel(this.mState.strengthId);
        if (this.mState.typeId > 0) {
            if (!this.mStatusBarExt.disableHostFunction()) {
                this.mMobileType.setContentDescription(this.mState.typeContentDescription);
                this.mMobileType.setImageResource(this.mState.typeId);
            }
            this.mMobileType.setVisibility(0);
        } else {
            this.mMobileType.setVisibility(8);
        }
        this.mMobileRoaming.setVisibility(this.mState.roaming ? 0 : 8);
        this.mIn.setVisibility(this.mState.activityIn ? 0 : 8);
        this.mOut.setVisibility(this.mState.activityIn ? 0 : 8);
        View view = this.mInoutContainer;
        if (!this.mState.activityIn && !this.mState.activityOut) {
            i = 8;
        }
        view.setVisibility(i);
        setCustomizeViewProperty();
        showWfcIfAirplaneMode();
        this.mStatusBarExt.addCustomizedView(this.mState.subId, this.mContext, this.mMobileGroup);
        setCustomizedOpViews();
    }

    private void updateState(StatusBarSignalPolicy.MobileIconState mobileIconState) {
        setContentDescription(mobileIconState.contentDescription);
        if (this.mState.visible != mobileIconState.visible) {
            this.mMobileGroup.setVisibility(mobileIconState.visible ? 0 : 8);
            requestLayout();
        }
        if (this.mState.strengthId != mobileIconState.strengthId) {
            this.mMobileDrawable.setLevel(mobileIconState.strengthId);
        }
        if (this.mState.typeId != mobileIconState.typeId) {
            if (mobileIconState.typeId != 0) {
                if (!this.mStatusBarExt.disableHostFunction()) {
                    this.mMobileType.setContentDescription(mobileIconState.typeContentDescription);
                    this.mMobileType.setImageResource(mobileIconState.typeId);
                }
                this.mMobileType.setVisibility(0);
            } else {
                this.mMobileType.setVisibility(8);
            }
        }
        this.mMobileRoaming.setVisibility(mobileIconState.roaming ? 0 : 8);
        this.mIn.setVisibility(mobileIconState.activityIn ? 0 : 8);
        this.mOut.setVisibility(mobileIconState.activityIn ? 0 : 8);
        this.mInoutContainer.setVisibility((mobileIconState.activityIn || mobileIconState.activityOut) ? 0 : 8);
        if (this.mState.networkIcon != mobileIconState.networkIcon) {
            setNetworkIcon(mobileIconState.networkIcon);
            this.mStatusBarExt.setDisVolteView(this.mState.subId, mobileIconState.volteIcon, this.mVolteType);
        }
        if (this.mState.volteIcon != mobileIconState.volteIcon) {
            setVolteIcon(mobileIconState.volteIcon);
        }
        this.mState = mobileIconState;
        showWfcIfAirplaneMode();
        setCustomizedOpViews();
    }

    @Override // com.android.systemui.statusbar.policy.DarkIconDispatcher.DarkReceiver
    public void onDarkChanged(Rect rect, float f, int i) {
        if (!DarkIconDispatcher.isInArea(rect, this)) {
            return;
        }
        this.mMobileDrawable.setDarkIntensity(f);
        ColorStateList colorStateListValueOf = ColorStateList.valueOf(DarkIconDispatcher.getTint(rect, this, i));
        this.mIn.setImageTintList(colorStateListValueOf);
        this.mOut.setImageTintList(colorStateListValueOf);
        this.mMobileType.setImageTintList(colorStateListValueOf);
        this.mMobileRoaming.setImageTintList(colorStateListValueOf);
        this.mNetworkType.setImageTintList(colorStateListValueOf);
        this.mVolteType.setImageTintList(colorStateListValueOf);
        this.mDotView.setDecorColor(i);
        this.mDotView.setIconColor(i, false);
        this.mMobile.setImageTintList(colorStateListValueOf);
        this.mStatusBarExt.setCustomizedPlmnTextTint(i);
        this.mStatusBarExt.setIconTint(colorStateListValueOf);
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public String getSlot() {
        return this.mSlot;
    }

    public void setSlot(String str) {
        this.mSlot = str;
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public void setStaticDrawableColor(int i) {
        ColorStateList colorStateListValueOf = ColorStateList.valueOf(i);
        this.mMobileDrawable.setDarkIntensity(i == -1 ? 0.0f : 1.0f);
        this.mIn.setImageTintList(colorStateListValueOf);
        this.mOut.setImageTintList(colorStateListValueOf);
        this.mMobileType.setImageTintList(colorStateListValueOf);
        this.mMobileRoaming.setImageTintList(colorStateListValueOf);
        this.mNetworkType.setImageTintList(colorStateListValueOf);
        this.mVolteType.setImageTintList(colorStateListValueOf);
        this.mDotView.setDecorColor(i);
        this.mMobile.setImageTintList(colorStateListValueOf);
        this.mStatusBarExt.setCustomizedPlmnTextTint(i);
        this.mStatusBarExt.setIconTint(colorStateListValueOf);
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public void setDecorColor(int i) {
        this.mDotView.setDecorColor(i);
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public boolean isIconVisible() {
        return this.mState.visible || needShowWfcInAirplaneMode();
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public void setVisibleState(int i) {
        if (i == this.mVisibleState) {
        }
        this.mVisibleState = i;
        switch (i) {
            case 0:
                this.mMobileGroup.setVisibility(0);
                this.mDotView.setVisibility(8);
                break;
            case 1:
                this.mMobileGroup.setVisibility(4);
                this.mDotView.setVisibility(0);
                break;
            default:
                this.mMobileGroup.setVisibility(4);
                this.mDotView.setVisibility(4);
                break;
        }
    }

    @Override // com.android.systemui.statusbar.StatusIconDisplayable
    public int getVisibleState() {
        return this.mVisibleState;
    }

    @VisibleForTesting
    public StatusBarSignalPolicy.MobileIconState getState() {
        return this.mState;
    }

    @Override // android.view.View
    public String toString() {
        return "StatusBarMobileView(slot=" + this.mSlot + ", hash=" + hashCode() + ", state=" + this.mState + ")";
    }

    private void setCustomizeViewProperty() {
        setNetworkIcon(this.mState.networkIcon);
        setVolteIcon(this.mState.volteIcon);
    }

    private void setVolteIcon(int i) {
        if (i > 0) {
            this.mVolteType.setImageResource(i);
            this.mVolteType.setVisibility(0);
        } else {
            this.mVolteType.setVisibility(8);
        }
        this.mStatusBarExt.setCustomizedVolteView(i, this.mVolteType);
        this.mStatusBarExt.setDisVolteView(this.mState.subId, i, this.mVolteType);
    }

    private void setNetworkIcon(int i) {
        if (!FeatureOptions.MTK_CTA_SET) {
            return;
        }
        if (i > 0) {
            if (!this.mStatusBarExt.disableHostFunction()) {
                this.mNetworkType.setImageResource(i);
            }
            this.mNetworkType.setVisibility(0);
            return;
        }
        this.mNetworkType.setVisibility(8);
    }

    private void setCustomizedOpViews() {
        this.mStatusBarExt.SetHostViewInvisible(this.mMobileRoaming);
        this.mStatusBarExt.SetHostViewInvisible(this.mIn);
        this.mStatusBarExt.SetHostViewInvisible(this.mOut);
        if (this.mState.visible) {
            this.mStatusBarExt.getServiceStateForCustomizedView(this.mState.subId);
            this.mStatusBarExt.setCustomizedNetworkTypeView(this.mState.subId, this.mState.networkIcon, this.mNetworkType);
            this.mStatusBarExt.setCustomizedDataTypeView(this.mState.subId, this.mState.typeId, this.mState.mDataActivityIn, this.mState.mDataActivityOut);
            this.mStatusBarExt.setCustomizedSignalStrengthView(this.mState.subId, this.mState.strengthId, this.mMobile);
            this.mStatusBarExt.setCustomizedMobileTypeView(this.mState.subId, this.mMobileType);
            this.mStatusBarExt.setCustomizedView(this.mState.subId);
        }
    }

    private void showWfcIfAirplaneMode() {
        if (needShowWfcInAirplaneMode()) {
            if (DEBUG) {
                Log.d(getMobileTag(), "showWfcIfAirplaneMode: show wfc in airplane mode");
            }
            this.mMobileGroup.setVisibility(0);
            this.mMobile.setVisibility(8);
            this.mMobileType.setVisibility(8);
            this.mNetworkType.setVisibility(8);
            this.mMobileRoaming.setVisibility(8);
            this.mIsWfcCase = true;
            requestLayout();
            return;
        }
        if (this.mIsWfcCase) {
            if (DEBUG) {
                Log.d(getMobileTag(), "showWfcIfAirplaneMode: recover to show mobile view");
            }
            this.mMobile.setVisibility(0);
            this.mIsWfcCase = false;
            requestLayout();
        }
    }

    private boolean needShowWfcInAirplaneMode() {
        return (!this.mIsWfcEnable || this.mState.visible || this.mState.volteIcon == 0) ? false : true;
    }

    private String getMobileTag() {
        Object[] objArr = new Object[1];
        objArr[0] = Integer.valueOf(this.mState != null ? this.mState.subId : -1);
        return String.format("StatusBarMobileView(%d)", objArr);
    }
}
