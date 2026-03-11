package com.android.systemui.statusbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.settingslib.Utils;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;

public class StatusBarWifiView extends FrameLayout implements StatusIconDisplayable, DarkIconDispatcher.DarkReceiver {
    private View mAirplaneSpacer;
    private ContextThemeWrapper mDarkContext;
    private float mDarkIntensity;
    private StatusBarIconView mDotView;
    private ImageView mIn;
    private View mInoutContainer;
    private ContextThemeWrapper mLightContext;
    private ImageView mOut;
    private View mSignalSpacer;
    private String mSlot;
    private StatusBarSignalPolicy.WifiIconState mState;
    private int mVisibleState;
    private LinearLayout mWifiGroup;
    private ImageView mWifiIcon;

    public static StatusBarWifiView fromContext(Context context, String str) {
        StatusBarWifiView statusBarWifiView = (StatusBarWifiView) LayoutInflater.from(context).inflate(R.layout.status_bar_wifi_group, (ViewGroup) null);
        statusBarWifiView.setSlot(str);
        statusBarWifiView.init();
        statusBarWifiView.setVisibleState(0);
        return statusBarWifiView;
    }

    public StatusBarWifiView(Context context) {
        super(context);
        this.mDarkIntensity = 0.0f;
        this.mVisibleState = -1;
    }

    public StatusBarWifiView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mDarkIntensity = 0.0f;
        this.mVisibleState = -1;
    }

    public StatusBarWifiView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mDarkIntensity = 0.0f;
        this.mVisibleState = -1;
    }

    public StatusBarWifiView(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mDarkIntensity = 0.0f;
        this.mVisibleState = -1;
    }

    public void setSlot(String str) {
        this.mSlot = str;
    }

    @Override
    public void setStaticDrawableColor(int i) {
        ColorStateList colorStateListValueOf = ColorStateList.valueOf(i);
        this.mWifiIcon.setImageTintList(colorStateListValueOf);
        this.mIn.setImageTintList(colorStateListValueOf);
        this.mOut.setImageTintList(colorStateListValueOf);
        this.mDotView.setDecorColor(i);
    }

    @Override
    public void setDecorColor(int i) {
        this.mDotView.setDecorColor(i);
    }

    @Override
    public String getSlot() {
        return this.mSlot;
    }

    @Override
    public boolean isIconVisible() {
        return this.mState != null && this.mState.visible;
    }

    @Override
    public void setVisibleState(int i) {
        if (i == this.mVisibleState) {
        }
        this.mVisibleState = i;
        switch (i) {
            case 0:
                this.mWifiGroup.setVisibility(0);
                this.mDotView.setVisibility(8);
                break;
            case 1:
                this.mWifiGroup.setVisibility(8);
                this.mDotView.setVisibility(0);
                break;
            default:
                this.mWifiGroup.setVisibility(8);
                this.mDotView.setVisibility(8);
                break;
        }
    }

    @Override
    public int getVisibleState() {
        return this.mVisibleState;
    }

    @Override
    public void getDrawingRect(Rect rect) {
        super.getDrawingRect(rect);
        float translationX = getTranslationX();
        float translationY = getTranslationY();
        rect.left = (int) (rect.left + translationX);
        rect.right = (int) (rect.right + translationX);
        rect.top = (int) (rect.top + translationY);
        rect.bottom = (int) (rect.bottom + translationY);
    }

    private void init() {
        int themeAttr = Utils.getThemeAttr(this.mContext, R.attr.lightIconTheme);
        int themeAttr2 = Utils.getThemeAttr(this.mContext, R.attr.darkIconTheme);
        this.mLightContext = new ContextThemeWrapper(this.mContext, themeAttr);
        this.mDarkContext = new ContextThemeWrapper(this.mContext, themeAttr2);
        this.mWifiGroup = (LinearLayout) findViewById(R.id.wifi_group);
        this.mWifiIcon = (ImageView) findViewById(R.id.wifi_signal);
        this.mIn = (ImageView) findViewById(R.id.wifi_in);
        this.mOut = (ImageView) findViewById(R.id.wifi_out);
        this.mSignalSpacer = findViewById(R.id.wifi_signal_spacer);
        this.mAirplaneSpacer = findViewById(R.id.wifi_airplane_spacer);
        this.mInoutContainer = findViewById(R.id.inout_container);
        initDotView();
    }

    private void initDotView() {
        this.mDotView = new StatusBarIconView(this.mContext, this.mSlot, null);
        this.mDotView.setVisibleState(1);
        int dimensionPixelSize = this.mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_icon_size);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(dimensionPixelSize, dimensionPixelSize);
        layoutParams.gravity = 8388627;
        addView(this.mDotView, layoutParams);
    }

    public void applyWifiState(StatusBarSignalPolicy.WifiIconState wifiIconState) {
        if (wifiIconState == null) {
            setVisibility(8);
            this.mState = null;
            return;
        }
        if (this.mState == null) {
            this.mState = wifiIconState.copy();
            initViewState();
        }
        if (!this.mState.equals(wifiIconState)) {
            updateState(wifiIconState.copy());
        }
    }

    private void updateState(StatusBarSignalPolicy.WifiIconState wifiIconState) {
        setContentDescription(wifiIconState.contentDescription);
        if (this.mState.resId != wifiIconState.resId && wifiIconState.resId >= 0) {
            NeutralGoodDrawable neutralGoodDrawableCreate = NeutralGoodDrawable.create(this.mLightContext, this.mDarkContext, wifiIconState.resId);
            neutralGoodDrawableCreate.setDarkIntensity(this.mDarkIntensity);
            this.mWifiIcon.setImageDrawable(neutralGoodDrawableCreate);
        }
        this.mIn.setVisibility(wifiIconState.activityIn ? 0 : 8);
        this.mOut.setVisibility(wifiIconState.activityOut ? 0 : 8);
        this.mInoutContainer.setVisibility((wifiIconState.activityIn || wifiIconState.activityOut) ? 0 : 8);
        this.mAirplaneSpacer.setVisibility(wifiIconState.airplaneSpacerVisible ? 0 : 8);
        this.mSignalSpacer.setVisibility(wifiIconState.signalSpacerVisible ? 0 : 8);
        if (this.mState.visible != wifiIconState.visible) {
            setVisibility(wifiIconState.visible ? 0 : 8);
        }
        this.mState = wifiIconState;
    }

    private void initViewState() {
        setContentDescription(this.mState.contentDescription);
        if (this.mState.resId >= 0) {
            NeutralGoodDrawable neutralGoodDrawableCreate = NeutralGoodDrawable.create(this.mLightContext, this.mDarkContext, this.mState.resId);
            neutralGoodDrawableCreate.setDarkIntensity(this.mDarkIntensity);
            this.mWifiIcon.setImageDrawable(neutralGoodDrawableCreate);
        }
        this.mIn.setVisibility(this.mState.activityIn ? 0 : 8);
        this.mOut.setVisibility(this.mState.activityOut ? 0 : 8);
        this.mInoutContainer.setVisibility((this.mState.activityIn || this.mState.activityOut) ? 0 : 8);
        this.mAirplaneSpacer.setVisibility(this.mState.airplaneSpacerVisible ? 0 : 8);
        this.mSignalSpacer.setVisibility(this.mState.signalSpacerVisible ? 0 : 8);
        setVisibility(this.mState.visible ? 0 : 8);
    }

    @Override
    public void onDarkChanged(Rect rect, float f, int i) {
        if (!DarkIconDispatcher.isInArea(rect, this)) {
            return;
        }
        this.mDarkIntensity = f;
        Drawable drawable = this.mWifiIcon.getDrawable();
        if (drawable instanceof NeutralGoodDrawable) {
            ((NeutralGoodDrawable) drawable).setDarkIntensity(f);
        }
        this.mIn.setImageTintList(ColorStateList.valueOf(DarkIconDispatcher.getTint(rect, this, i)));
        this.mOut.setImageTintList(ColorStateList.valueOf(DarkIconDispatcher.getTint(rect, this, i)));
        this.mDotView.setDecorColor(i);
        this.mDotView.setIconColor(i, false);
    }

    @Override
    public String toString() {
        return "StatusBarWifiView(slot=" + this.mSlot + " state=" + this.mState + ")";
    }
}
