package com.android.systemui.statusbar.phone;

import android.os.Bundle;
import android.os.UserHandle;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.DemoMode;
import com.android.systemui.R;
import com.android.systemui.statusbar.StatusBarIconView;

public class DemoStatusIcons extends LinearLayout implements DemoMode {
    private boolean mDemoMode;
    private final int mIconSize;
    private final LinearLayout mStatusIcons;

    public DemoStatusIcons(LinearLayout statusIcons, int iconSize) {
        super(statusIcons.getContext());
        this.mStatusIcons = statusIcons;
        this.mIconSize = iconSize;
        setLayoutParams(this.mStatusIcons.getLayoutParams());
        setOrientation(this.mStatusIcons.getOrientation());
        setGravity(16);
        ViewGroup p = (ViewGroup) this.mStatusIcons.getParent();
        p.addView(this, p.indexOfChild(this.mStatusIcons));
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        int iconId;
        int iconId2;
        if (!this.mDemoMode && command.equals("enter")) {
            this.mDemoMode = true;
            this.mStatusIcons.setVisibility(8);
            setVisibility(0);
            return;
        }
        if (this.mDemoMode && command.equals("exit")) {
            this.mDemoMode = false;
            this.mStatusIcons.setVisibility(0);
            setVisibility(8);
            return;
        }
        if (this.mDemoMode && command.equals("status")) {
            String volume = args.getString("volume");
            if (volume != null) {
                int iconId3 = volume.equals("vibrate") ? R.drawable.stat_sys_ringer_vibrate : 0;
                updateSlot("volume", null, iconId3);
            }
            String zen = args.getString("zen");
            if (zen != null) {
                if (zen.equals("important")) {
                    iconId2 = R.drawable.stat_sys_zen_important;
                } else {
                    iconId2 = zen.equals("none") ? R.drawable.stat_sys_zen_none : 0;
                }
                updateSlot("zen", null, iconId2);
            }
            String bt = args.getString("bluetooth");
            if (bt != null) {
                if (bt.equals("disconnected")) {
                    iconId = R.drawable.stat_sys_data_bluetooth;
                } else {
                    iconId = bt.equals("connected") ? R.drawable.stat_sys_data_bluetooth_connected : 0;
                }
                updateSlot("bluetooth", null, iconId);
            }
            String location = args.getString("location");
            if (location != null) {
                int iconId4 = location.equals("show") ? R.drawable.stat_sys_location : 0;
                updateSlot("location", null, iconId4);
            }
            String alarm = args.getString("alarm");
            if (alarm != null) {
                int iconId5 = alarm.equals("show") ? R.drawable.stat_sys_alarm : 0;
                updateSlot("alarm_clock", null, iconId5);
            }
            String sync = args.getString("sync");
            if (sync != null) {
                int iconId6 = sync.equals("show") ? R.drawable.stat_sys_sync : 0;
                updateSlot("sync_active", null, iconId6);
            }
            String tty = args.getString("tty");
            if (tty != null) {
                int iconId7 = tty.equals("show") ? R.drawable.stat_sys_tty_mode : 0;
                updateSlot("tty", null, iconId7);
            }
            String eri = args.getString("eri");
            if (eri != null) {
                int iconId8 = eri.equals("show") ? R.drawable.stat_sys_roaming_cdma_0 : 0;
                updateSlot("cdma_eri", null, iconId8);
            }
            String mute = args.getString("mute");
            if (mute != null) {
                int iconId9 = mute.equals("show") ? android.R.drawable.stat_notify_call_mute : 0;
                updateSlot("mute", null, iconId9);
            }
            String speakerphone = args.getString("speakerphone");
            if (speakerphone != null) {
                int iconId10 = speakerphone.equals("show") ? android.R.drawable.stat_sys_speakerphone : 0;
                updateSlot("speakerphone", null, iconId10);
            }
            String cast = args.getString("cast");
            if (cast != null) {
                int iconId11 = cast.equals("show") ? R.drawable.stat_sys_cast : 0;
                updateSlot("cast", null, iconId11);
            }
            String hotspot = args.getString("hotspot");
            if (hotspot != null) {
                int iconId12 = hotspot.equals("show") ? R.drawable.stat_sys_hotspot : 0;
                updateSlot("hotspot", null, iconId12);
            }
        }
    }

    private void updateSlot(String slot, String iconPkg, int iconId) {
        if (this.mDemoMode) {
            int removeIndex = -1;
            int i = 0;
            while (true) {
                if (i >= getChildCount()) {
                    break;
                }
                StatusBarIconView v = (StatusBarIconView) getChildAt(i);
                if (!slot.equals(v.getTag())) {
                    i++;
                } else if (iconId == 0) {
                    removeIndex = i;
                } else {
                    StatusBarIcon icon = v.getStatusBarIcon();
                    icon.iconPackage = iconPkg;
                    icon.iconId = iconId;
                    v.set(icon);
                    v.updateDrawable();
                    return;
                }
            }
            if (iconId == 0 && removeIndex != -1) {
                removeViewAt(removeIndex);
                return;
            }
            StatusBarIcon icon2 = new StatusBarIcon(iconPkg, UserHandle.CURRENT, iconId, 0, 0, "Demo");
            StatusBarIconView v2 = new StatusBarIconView(getContext(), null, null);
            v2.setTag(slot);
            v2.set(icon2);
            addView(v2, 0, new LinearLayout.LayoutParams(this.mIconSize, this.mIconSize));
        }
    }
}
