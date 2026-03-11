package com.android.launcher3.dragndrop;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.drawable.Drawable;
import android.os.Process;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.IconCache;
import com.android.launcher3.LauncherAppState;
import com.android.launcher3.R;
import com.android.launcher3.compat.LauncherAppsCompatVO;
import com.android.launcher3.compat.ShortcutConfigActivityInfo;

@TargetApi(26)
class PinShortcutRequestActivityInfo extends ShortcutConfigActivityInfo {
    private static final String DUMMY_COMPONENT_CLASS = "pinned-shortcut";
    private final Context mContext;
    private final ShortcutInfo mInfo;
    private final LauncherApps.PinItemRequest mRequest;

    public PinShortcutRequestActivityInfo(LauncherApps.PinItemRequest pinItemRequest, Context context) {
        super(new ComponentName(pinItemRequest.getShortcutInfo().getPackage(), DUMMY_COMPONENT_CLASS), pinItemRequest.getShortcutInfo().getUserHandle());
        this.mRequest = pinItemRequest;
        this.mInfo = pinItemRequest.getShortcutInfo();
        this.mContext = context;
    }

    @Override
    public int getItemType() {
        return 6;
    }

    @Override
    public CharSequence getLabel() {
        return this.mInfo.getShortLabel();
    }

    @Override
    public Drawable getFullResIcon(IconCache iconCache) {
        Drawable shortcutIconDrawable = ((LauncherApps) this.mContext.getSystemService(LauncherApps.class)).getShortcutIconDrawable(this.mInfo, LauncherAppState.getIDP(this.mContext).fillResIconDpi);
        if (shortcutIconDrawable == null) {
            return new FastBitmapDrawable(iconCache.getDefaultIcon(Process.myUserHandle()));
        }
        return shortcutIconDrawable;
    }

    @Override
    public com.android.launcher3.ShortcutInfo createShortcutInfo() {
        return LauncherAppsCompatVO.createShortcutInfoFromPinItemRequest(this.mContext, this.mRequest, this.mContext.getResources().getInteger(R.integer.config_dropAnimMaxDuration) + 500 + 150);
    }

    @Override
    public boolean startConfigActivity(Activity activity, int i) {
        return false;
    }

    @Override
    public boolean isPersistable() {
        return false;
    }
}
