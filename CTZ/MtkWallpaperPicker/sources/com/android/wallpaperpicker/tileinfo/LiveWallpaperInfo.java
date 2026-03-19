package com.android.wallpaperpicker.tileinfo;

import android.app.WallpaperInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.wallpaperpicker.R;
import com.android.wallpaperpicker.WallpaperPickerActivity;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class LiveWallpaperInfo extends WallpaperTileInfo {
    private WallpaperInfo mInfo;
    private Drawable mThumbnail;

    public LiveWallpaperInfo(Drawable drawable, WallpaperInfo wallpaperInfo, Intent intent) {
        this.mThumbnail = drawable;
        this.mInfo = wallpaperInfo;
    }

    @Override
    public void onClick(WallpaperPickerActivity wallpaperPickerActivity) {
        Intent intent = new Intent("android.service.wallpaper.CHANGE_LIVE_WALLPAPER");
        intent.putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT", this.mInfo.getComponent());
        wallpaperPickerActivity.startActivityForResultSafely(intent, 6);
    }

    @Override
    public View createView(Context context, LayoutInflater layoutInflater, ViewGroup viewGroup) {
        this.mView = layoutInflater.inflate(R.layout.wallpaper_picker_live_wallpaper_item, viewGroup, false);
        ImageView imageView = (ImageView) this.mView.findViewById(R.id.wallpaper_image);
        ImageView imageView2 = (ImageView) this.mView.findViewById(R.id.wallpaper_icon);
        if (this.mThumbnail != null) {
            imageView.setImageDrawable(this.mThumbnail);
            imageView2.setVisibility(8);
        } else {
            imageView2.setImageDrawable(this.mInfo.loadIcon(context.getPackageManager()));
            imageView2.setVisibility(0);
        }
        ((TextView) this.mView.findViewById(R.id.wallpaper_item_label)).setText(this.mInfo.loadLabel(context.getPackageManager()));
        return this.mView;
    }

    public static class LoaderTask extends AsyncTask<Void, Void, List<LiveWallpaperInfo>> {
        private final Context mContext;

        public LoaderTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected List<LiveWallpaperInfo> doInBackground(Void... voidArr) {
            final PackageManager packageManager = this.mContext.getPackageManager();
            List<ResolveInfo> listQueryIntentServices = packageManager.queryIntentServices(new Intent("android.service.wallpaper.WallpaperService"), 128);
            Collections.sort(listQueryIntentServices, new Comparator<ResolveInfo>() {
                final Collator mCollator = Collator.getInstance();

                @Override
                public int compare(ResolveInfo resolveInfo, ResolveInfo resolveInfo2) {
                    return this.mCollator.compare(resolveInfo.loadLabel(packageManager), resolveInfo2.loadLabel(packageManager));
                }
            });
            ArrayList arrayList = new ArrayList();
            for (ResolveInfo resolveInfo : listQueryIntentServices) {
                try {
                    WallpaperInfo wallpaperInfo = new WallpaperInfo(this.mContext, resolveInfo);
                    Drawable drawableLoadThumbnail = wallpaperInfo.loadThumbnail(packageManager);
                    Intent intent = new Intent("android.service.wallpaper.WallpaperService");
                    intent.setClassName(wallpaperInfo.getPackageName(), wallpaperInfo.getServiceName());
                    arrayList.add(new LiveWallpaperInfo(drawableLoadThumbnail, wallpaperInfo, intent));
                } catch (IOException | XmlPullParserException e) {
                    Log.w("LiveWallpaperTile", "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                }
            }
            return arrayList;
        }
    }
}
