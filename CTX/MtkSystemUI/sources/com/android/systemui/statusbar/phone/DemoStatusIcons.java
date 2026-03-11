package com.android.systemui.statusbar.phone;

import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.DemoMode;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.StatusBarMobileView;
import com.android.systemui.statusbar.StatusBarWifiView;
import com.android.systemui.statusbar.StatusIconDisplayable;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import java.util.ArrayList;
import java.util.Iterator;

public class DemoStatusIcons extends StatusIconContainer implements DemoMode, DarkIconDispatcher.DarkReceiver {
    private int mColor;
    private boolean mDemoMode;
    private final int mIconSize;
    private final ArrayList<StatusBarMobileView> mMobileViews;
    private final LinearLayout mStatusIcons;
    private StatusBarWifiView mWifiView;

    public DemoStatusIcons(LinearLayout linearLayout, int i) {
        super(linearLayout.getContext());
        this.mMobileViews = new ArrayList<>();
        this.mStatusIcons = linearLayout;
        this.mIconSize = i;
        this.mColor = -1;
        if (linearLayout instanceof StatusIconContainer) {
            setShouldRestrictIcons(((StatusIconContainer) linearLayout).isRestrictingIcons());
        } else {
            setShouldRestrictIcons(false);
        }
        setLayoutParams(this.mStatusIcons.getLayoutParams());
        setPadding(this.mStatusIcons.getPaddingLeft(), this.mStatusIcons.getPaddingTop(), this.mStatusIcons.getPaddingRight(), this.mStatusIcons.getPaddingBottom());
        setOrientation(this.mStatusIcons.getOrientation());
        setGravity(16);
        ViewGroup viewGroup = (ViewGroup) this.mStatusIcons.getParent();
        viewGroup.addView(this, viewGroup.indexOfChild(this.mStatusIcons));
    }

    public void remove() {
        this.mMobileViews.clear();
        ((ViewGroup) getParent()).removeView(this);
    }

    public void setColor(int i) {
        this.mColor = i;
        updateColors();
    }

    private void updateColors() {
        for (int i = 0; i < getChildCount(); i++) {
            StatusIconDisplayable statusIconDisplayable = (StatusIconDisplayable) getChildAt(i);
            statusIconDisplayable.setStaticDrawableColor(this.mColor);
            statusIconDisplayable.setDecorColor(this.mColor);
        }
    }

    @Override
    public void dispatchDemoCommand(String str, Bundle bundle) {
        int i;
        int i2;
        if (!this.mDemoMode && str.equals("enter")) {
            this.mDemoMode = true;
            this.mStatusIcons.setVisibility(8);
            setVisibility(0);
            return;
        }
        if (this.mDemoMode && str.equals("exit")) {
            this.mDemoMode = false;
            this.mStatusIcons.setVisibility(0);
            setVisibility(8);
            return;
        }
        if (this.mDemoMode && str.equals("status")) {
            String string = bundle.getString("volume");
            if (string != null) {
                updateSlot("volume", null, string.equals("vibrate") ? R.drawable.stat_sys_ringer_vibrate : 0);
            }
            String string2 = bundle.getString("zen");
            if (string2 != null) {
                if (string2.equals("important")) {
                    i2 = R.drawable.stat_sys_zen_important;
                } else {
                    i2 = string2.equals("none") ? R.drawable.stat_sys_zen_none : 0;
                }
                updateSlot("zen", null, i2);
            }
            String string3 = bundle.getString("bluetooth");
            if (string3 != null) {
                if (string3.equals("disconnected")) {
                    i = R.drawable.stat_sys_data_bluetooth;
                } else {
                    i = string3.equals("connected") ? R.drawable.stat_sys_data_bluetooth_connected : 0;
                }
                updateSlot("bluetooth", null, i);
            }
            String string4 = bundle.getString("location");
            if (string4 != null) {
                updateSlot("location", null, string4.equals("show") ? R.drawable.stat_sys_location : 0);
            }
            String string5 = bundle.getString("alarm");
            if (string5 != null) {
                updateSlot("alarm_clock", null, string5.equals("show") ? R.drawable.stat_sys_alarm : 0);
            }
            String string6 = bundle.getString("tty");
            if (string6 != null) {
                updateSlot("tty", null, string6.equals("show") ? R.drawable.stat_sys_tty_mode : 0);
            }
            String string7 = bundle.getString("mute");
            if (string7 != null) {
                updateSlot("mute", null, string7.equals("show") ? android.R.drawable.stat_notify_call_mute : 0);
            }
            String string8 = bundle.getString("speakerphone");
            if (string8 != null) {
                updateSlot("speakerphone", null, string8.equals("show") ? android.R.drawable.stat_sys_speakerphone : 0);
            }
            String string9 = bundle.getString("cast");
            if (string9 != null) {
                updateSlot("cast", null, string9.equals("show") ? R.drawable.stat_sys_cast : 0);
            }
            String string10 = bundle.getString("hotspot");
            if (string10 != null) {
                updateSlot("hotspot", null, string10.equals("show") ? R.drawable.stat_sys_hotspot : 0);
            }
        }
    }

    private void updateSlot(String str, String str2, int i) {
        if (this.mDemoMode) {
            if (str2 == null) {
                str2 = this.mContext.getPackageName();
            }
            String str3 = str2;
            int i2 = 0;
            while (true) {
                if (i2 < getChildCount()) {
                    View childAt = getChildAt(i2);
                    if (childAt instanceof StatusBarIconView) {
                        StatusBarIconView statusBarIconView = (StatusBarIconView) childAt;
                        if (str.equals(statusBarIconView.getTag())) {
                            if (i != 0) {
                                StatusBarIcon statusBarIcon = statusBarIconView.getStatusBarIcon();
                                statusBarIcon.visible = true;
                                statusBarIcon.icon = Icon.createWithResource(statusBarIcon.icon.getResPackage(), i);
                                statusBarIconView.set(statusBarIcon);
                                statusBarIconView.updateDrawable();
                                return;
                            }
                        }
                    }
                    i2++;
                } else {
                    i2 = -1;
                    break;
                }
            }
            if (i == 0) {
                if (i2 != -1) {
                    removeViewAt(i2);
                    return;
                }
                return;
            }
            StatusBarIcon statusBarIcon2 = new StatusBarIcon(str3, UserHandle.SYSTEM, i, 0, 0, "Demo");
            statusBarIcon2.visible = true;
            StatusBarIconView statusBarIconView2 = new StatusBarIconView(getContext(), str, null, false);
            statusBarIconView2.setTag(str);
            statusBarIconView2.set(statusBarIcon2);
            statusBarIconView2.setStaticDrawableColor(this.mColor);
            statusBarIconView2.setDecorColor(this.mColor);
            addView(statusBarIconView2, 0, new LinearLayout.LayoutParams(-2, this.mIconSize));
        }
    }

    public void addDemoWifiView(StatusBarSignalPolicy.WifiIconState wifiIconState) {
        Log.d("DemoStatusIcons", "addDemoWifiView: ");
        StatusBarWifiView statusBarWifiViewFromContext = StatusBarWifiView.fromContext(this.mContext, wifiIconState.slot);
        int childCount = getChildCount();
        int i = 0;
        while (true) {
            if (i >= getChildCount()) {
                break;
            }
            if (!(getChildAt(i) instanceof StatusBarMobileView)) {
                i++;
            } else {
                childCount = i;
                break;
            }
        }
        this.mWifiView = statusBarWifiViewFromContext;
        this.mWifiView.applyWifiState(wifiIconState);
        this.mWifiView.setStaticDrawableColor(this.mColor);
        addView(statusBarWifiViewFromContext, childCount);
    }

    public void updateWifiState(StatusBarSignalPolicy.WifiIconState wifiIconState) {
        Log.d("DemoStatusIcons", "updateWifiState: ");
        if (this.mWifiView == null) {
            addDemoWifiView(wifiIconState);
        } else {
            this.mWifiView.applyWifiState(wifiIconState);
        }
    }

    public void addMobileView(StatusBarSignalPolicy.MobileIconState mobileIconState) {
        Log.d("DemoStatusIcons", "addMobileView: ");
        StatusBarMobileView statusBarMobileViewFromContext = StatusBarMobileView.fromContext(this.mContext, mobileIconState.slot);
        statusBarMobileViewFromContext.applyMobileState(mobileIconState);
        statusBarMobileViewFromContext.setStaticDrawableColor(this.mColor);
        this.mMobileViews.add(statusBarMobileViewFromContext);
        addView(statusBarMobileViewFromContext, getChildCount());
    }

    public void updateMobileState(StatusBarSignalPolicy.MobileIconState mobileIconState) {
        Log.d("DemoStatusIcons", "updateMobileState: ");
        for (int i = 0; i < this.mMobileViews.size(); i++) {
            StatusBarMobileView statusBarMobileView = this.mMobileViews.get(i);
            if (statusBarMobileView.getState().subId == mobileIconState.subId) {
                statusBarMobileView.applyMobileState(mobileIconState);
                return;
            }
        }
        addMobileView(mobileIconState);
    }

    public void onRemoveIcon(StatusIconDisplayable statusIconDisplayable) {
        if (statusIconDisplayable.getSlot().equals("wifi")) {
            removeView(this.mWifiView);
            this.mWifiView = null;
            return;
        }
        StatusBarMobileView statusBarMobileViewMatchingMobileView = matchingMobileView(statusIconDisplayable);
        if (statusBarMobileViewMatchingMobileView != null) {
            removeView(statusBarMobileViewMatchingMobileView);
            this.mMobileViews.remove(statusBarMobileViewMatchingMobileView);
        }
    }

    private StatusBarMobileView matchingMobileView(StatusIconDisplayable statusIconDisplayable) {
        if (!(statusIconDisplayable instanceof StatusBarMobileView)) {
            return null;
        }
        StatusBarMobileView statusBarMobileView = (StatusBarMobileView) statusIconDisplayable;
        for (StatusBarMobileView statusBarMobileView2 : this.mMobileViews) {
            if (statusBarMobileView2.getState().subId == statusBarMobileView.getState().subId) {
                return statusBarMobileView2;
            }
        }
        return null;
    }

    @Override
    public void onDarkChanged(Rect rect, float f, int i) {
        setColor(DarkIconDispatcher.getTint(rect, this.mStatusIcons, i));
        if (this.mWifiView != null) {
            this.mWifiView.onDarkChanged(rect, f, i);
        }
        Iterator<StatusBarMobileView> it = this.mMobileViews.iterator();
        while (it.hasNext()) {
            it.next().onDarkChanged(rect, f, i);
        }
    }
}
