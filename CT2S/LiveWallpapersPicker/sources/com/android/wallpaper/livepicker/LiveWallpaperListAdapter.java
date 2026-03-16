package com.android.wallpaper.livepicker;

import android.app.WallpaperInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
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
    private List<LiveWallpaperInfo> mWallpapers;

    public LiveWallpaperListAdapter(Context context) {
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mPackageManager = context.getPackageManager();
        List<ResolveInfo> list = this.mPackageManager.queryIntentServices(new Intent("android.service.wallpaper.WallpaperService"), 128);
        this.mWallpapers = generatePlaceholderViews(list.size());
        new LiveWallpaperEnumerator(context).execute(list);
    }

    private List<LiveWallpaperInfo> generatePlaceholderViews(int amount) {
        ArrayList<LiveWallpaperInfo> list = new ArrayList<>(amount);
        for (int i = 0; i < amount; i++) {
            LiveWallpaperInfo info = new LiveWallpaperInfo();
            list.add(info);
        }
        return list;
    }

    @Override
    public int getCount() {
        if (this.mWallpapers == null) {
            return 0;
        }
        return this.mWallpapers.size();
    }

    @Override
    public Object getItem(int position) {
        return this.mWallpapers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = this.mInflater.inflate(R.layout.live_wallpaper_entry, parent, false);
            holder = new ViewHolder();
            holder.title = (TextView) convertView.findViewById(R.id.title);
            holder.thumbnail = (ImageView) convertView.findViewById(R.id.thumbnail);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        LiveWallpaperInfo wallpaperInfo = this.mWallpapers.get(position);
        if (holder.thumbnail != null) {
            holder.thumbnail.setImageDrawable(wallpaperInfo.thumbnail);
        }
        if (holder.title != null && wallpaperInfo.info != null) {
            holder.title.setText(wallpaperInfo.info.loadLabel(this.mPackageManager));
            if (holder.thumbnail == null) {
                holder.title.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, wallpaperInfo.thumbnail, (Drawable) null, (Drawable) null);
            }
        }
        return convertView;
    }

    public class LiveWallpaperInfo {
        public WallpaperInfo info;
        public Intent intent;
        public Drawable thumbnail;

        public LiveWallpaperInfo() {
        }
    }

    private class ViewHolder {
        ImageView thumbnail;
        TextView title;

        private ViewHolder() {
        }
    }

    private class LiveWallpaperEnumerator extends AsyncTask<List<ResolveInfo>, LiveWallpaperInfo, Void> {
        private Context mContext;
        private int mWallpaperPosition = 0;

        public LiveWallpaperEnumerator(Context context) {
            this.mContext = context;
        }

        @Override
        protected Void doInBackground(List<ResolveInfo>... params) {
            final PackageManager packageManager = this.mContext.getPackageManager();
            List<ResolveInfo> list = params[0];
            Resources res = this.mContext.getResources();
            BitmapDrawable galleryIcon = (BitmapDrawable) res.getDrawable(R.drawable.livewallpaper_placeholder);
            Paint paint = new Paint(5);
            paint.setTextAlign(Paint.Align.CENTER);
            Canvas canvas = new Canvas();
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
                    LiveWallpaperInfo wallpaper = LiveWallpaperListAdapter.this.new LiveWallpaperInfo();
                    wallpaper.intent = new Intent("android.service.wallpaper.WallpaperService");
                    wallpaper.intent.setClassName(info.getPackageName(), info.getServiceName());
                    wallpaper.info = info;
                    Drawable thumb = info.loadThumbnail(packageManager);
                    if (thumb == null) {
                        int thumbWidth = res.getDimensionPixelSize(R.dimen.live_wallpaper_thumbnail_width);
                        int thumbHeight = res.getDimensionPixelSize(R.dimen.live_wallpaper_thumbnail_height);
                        Bitmap thumbnail = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888);
                        paint.setColor(res.getColor(R.color.live_wallpaper_thumbnail_background));
                        canvas.setBitmap(thumbnail);
                        canvas.drawPaint(paint);
                        galleryIcon.setBounds(0, 0, thumbWidth, thumbHeight);
                        galleryIcon.setGravity(17);
                        galleryIcon.draw(canvas);
                        String title = info.loadLabel(packageManager).toString();
                        paint.setColor(res.getColor(R.color.live_wallpaper_thumbnail_text_color));
                        paint.setTextSize(res.getDimensionPixelSize(R.dimen.live_wallpaper_thumbnail_text_size));
                        canvas.drawText(title, (int) (((double) thumbWidth) * 0.5d), thumbHeight - res.getDimensionPixelSize(R.dimen.live_wallpaper_thumbnail_text_offset), paint);
                        thumb = new BitmapDrawable(res, thumbnail);
                    }
                    wallpaper.thumbnail = thumb;
                    publishProgress(wallpaper);
                } catch (IOException e) {
                    Log.w("LiveWallpaperListAdapter", "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                } catch (XmlPullParserException e2) {
                    Log.w("LiveWallpaperListAdapter", "Skipping wallpaper " + resolveInfo.serviceInfo, e2);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(LiveWallpaperInfo... infos) {
            for (LiveWallpaperInfo info : infos) {
                info.thumbnail.setDither(true);
                if (this.mWallpaperPosition < LiveWallpaperListAdapter.this.mWallpapers.size()) {
                    LiveWallpaperListAdapter.this.mWallpapers.set(this.mWallpaperPosition, info);
                } else {
                    LiveWallpaperListAdapter.this.mWallpapers.add(info);
                }
                this.mWallpaperPosition++;
                LiveWallpaperListAdapter.this.notifyDataSetChanged();
            }
        }
    }
}
