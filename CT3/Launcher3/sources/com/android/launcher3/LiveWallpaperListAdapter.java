package com.android.launcher3;

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
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import com.android.launcher3.WallpaperPickerActivity;
import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.xmlpull.v1.XmlPullParserException;

public class LiveWallpaperListAdapter extends BaseAdapter implements ListAdapter {
    private final LayoutInflater mInflater;
    private final PackageManager mPackageManager;
    List<LiveWallpaperTile> mWallpapers;

    public LiveWallpaperListAdapter(Context context) {
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mPackageManager = context.getPackageManager();
        List<ResolveInfo> list = this.mPackageManager.queryIntentServices(new Intent("android.service.wallpaper.WallpaperService"), 128);
        this.mWallpapers = new ArrayList();
        new LiveWallpaperEnumerator(context).execute(list);
    }

    @Override
    public int getCount() {
        if (this.mWallpapers == null) {
            return 0;
        }
        return this.mWallpapers.size();
    }

    @Override
    public LiveWallpaperTile getItem(int position) {
        return this.mWallpapers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = this.mInflater.inflate(R.layout.wallpaper_picker_live_wallpaper_item, parent, false);
        } else {
            view = convertView;
        }
        LiveWallpaperTile wallpaperInfo = this.mWallpapers.get(position);
        wallpaperInfo.setView(view);
        ImageView image = (ImageView) view.findViewById(R.id.wallpaper_image);
        ImageView icon = (ImageView) view.findViewById(R.id.wallpaper_icon);
        if (wallpaperInfo.mThumbnail != null) {
            image.setImageDrawable(wallpaperInfo.mThumbnail);
            icon.setVisibility(8);
        } else {
            icon.setImageDrawable(wallpaperInfo.mInfo.loadIcon(this.mPackageManager));
            icon.setVisibility(0);
        }
        TextView label = (TextView) view.findViewById(R.id.wallpaper_item_label);
        label.setText(wallpaperInfo.mInfo.loadLabel(this.mPackageManager));
        return view;
    }

    public static class LiveWallpaperTile extends WallpaperPickerActivity.WallpaperTileInfo {
        WallpaperInfo mInfo;
        Drawable mThumbnail;

        public LiveWallpaperTile(Drawable thumbnail, WallpaperInfo info, Intent intent) {
            this.mThumbnail = thumbnail;
            this.mInfo = info;
        }

        @Override
        public void onClick(WallpaperPickerActivity a) {
            Intent preview = new Intent("android.service.wallpaper.CHANGE_LIVE_WALLPAPER");
            preview.putExtra("android.service.wallpaper.extra.LIVE_WALLPAPER_COMPONENT", this.mInfo.getComponent());
            a.startActivityForResultSafely(preview, 6);
        }
    }

    private class LiveWallpaperEnumerator extends AsyncTask<List<ResolveInfo>, LiveWallpaperTile, Void> {
        private Context mContext;
        private int mWallpaperPosition = 0;

        public LiveWallpaperEnumerator(Context context) {
            this.mContext = context;
        }

        @Override
        public Void doInBackground(List<ResolveInfo>... params) {
            final PackageManager packageManager = this.mContext.getPackageManager();
            List<ResolveInfo> list = params[0];
            Collections.sort(list, new Comparator<ResolveInfo>() {
                final Collator mCollator = Collator.getInstance();

                @Override
                public int compare(ResolveInfo info1, ResolveInfo info2) {
                    return this.mCollator.compare(info1.loadLabel(packageManager), info2.loadLabel(packageManager));
                }
            });
            for (ResolveInfo resolveInfo : list) {
                try {
                    WallpaperInfo info = new WallpaperInfo(this.mContext, resolveInfo);
                    Drawable thumb = info.loadThumbnail(packageManager);
                    Intent launchIntent = new Intent("android.service.wallpaper.WallpaperService");
                    launchIntent.setClassName(info.getPackageName(), info.getServiceName());
                    LiveWallpaperTile wallpaper = new LiveWallpaperTile(thumb, info, launchIntent);
                    publishProgress(wallpaper);
                } catch (IOException e) {
                    Log.w("LiveWallpaperListAdapter", "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                } catch (XmlPullParserException e2) {
                    Log.w("LiveWallpaperListAdapter", "Skipping wallpaper " + resolveInfo.serviceInfo, e2);
                }
            }
            publishProgress((LiveWallpaperTile) null);
            return null;
        }

        @Override
        public void onProgressUpdate(LiveWallpaperTile... infos) {
            for (LiveWallpaperTile info : infos) {
                if (info == null) {
                    LiveWallpaperListAdapter.this.notifyDataSetChanged();
                    return;
                }
                if (info.mThumbnail != null) {
                    info.mThumbnail.setDither(true);
                }
                if (this.mWallpaperPosition < LiveWallpaperListAdapter.this.mWallpapers.size()) {
                    LiveWallpaperListAdapter.this.mWallpapers.set(this.mWallpaperPosition, info);
                } else {
                    LiveWallpaperListAdapter.this.mWallpapers.add(info);
                }
                this.mWallpaperPosition++;
            }
        }
    }
}
