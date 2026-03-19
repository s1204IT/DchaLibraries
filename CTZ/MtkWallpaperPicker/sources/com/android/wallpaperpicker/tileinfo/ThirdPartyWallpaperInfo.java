package com.android.wallpaperpicker.tileinfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.wallpaperpicker.R;
import com.android.wallpaperpicker.WallpaperPickerActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class ThirdPartyWallpaperInfo extends WallpaperTileInfo {
    private final int mIconSize;
    private final ResolveInfo mResolveInfo;

    public ThirdPartyWallpaperInfo(ResolveInfo resolveInfo, int i) {
        this.mResolveInfo = resolveInfo;
        this.mIconSize = i;
    }

    @Override
    public void onClick(WallpaperPickerActivity wallpaperPickerActivity) {
        wallpaperPickerActivity.startActivityForResultSafely(new Intent("android.intent.action.SET_WALLPAPER").setComponent(new ComponentName(((PackageItemInfo) this.mResolveInfo.activityInfo).packageName, ((PackageItemInfo) this.mResolveInfo.activityInfo).name)).putExtra("com.android.launcher3.WALLPAPER_OFFSET", wallpaperPickerActivity.getWallpaperParallaxOffset()), 6);
    }

    @Override
    public View createView(Context context, LayoutInflater layoutInflater, ViewGroup viewGroup) {
        this.mView = layoutInflater.inflate(R.layout.wallpaper_picker_third_party_item, viewGroup, false);
        TextView textView = (TextView) this.mView.findViewById(R.id.wallpaper_item_label);
        textView.setText(this.mResolveInfo.loadLabel(context.getPackageManager()));
        Drawable drawableLoadIcon = this.mResolveInfo.loadIcon(context.getPackageManager());
        drawableLoadIcon.setBounds(new Rect(0, 0, this.mIconSize, this.mIconSize));
        textView.setCompoundDrawables(null, drawableLoadIcon, null, null);
        return this.mView;
    }

    public static List<ThirdPartyWallpaperInfo> getAll(Context context) {
        ArrayList arrayList = new ArrayList();
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.wallpaperItemIconSize);
        PackageManager packageManager = context.getPackageManager();
        Intent type = new Intent("android.intent.action.GET_CONTENT").setType("image/*");
        HashSet hashSet = new HashSet();
        Iterator<ResolveInfo> it = packageManager.queryIntentActivities(type, 0).iterator();
        while (it.hasNext()) {
            hashSet.add(((PackageItemInfo) it.next().activityInfo).packageName);
        }
        hashSet.add(context.getPackageName());
        hashSet.add("com.android.wallpaper.livepicker");
        for (ResolveInfo resolveInfo : packageManager.queryIntentActivities(new Intent("android.intent.action.SET_WALLPAPER"), 0)) {
            if (!hashSet.contains(((PackageItemInfo) resolveInfo.activityInfo).packageName)) {
                arrayList.add(new ThirdPartyWallpaperInfo(resolveInfo, dimensionPixelSize));
            }
        }
        return arrayList;
    }
}
