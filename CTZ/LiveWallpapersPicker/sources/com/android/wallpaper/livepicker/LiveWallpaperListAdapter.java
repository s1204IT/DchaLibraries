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
        List<ResolveInfo> listQueryIntentServices = this.mPackageManager.queryIntentServices(new Intent("android.service.wallpaper.WallpaperService"), 128);
        this.mWallpapers = generatePlaceholderViews(listQueryIntentServices.size());
        new LiveWallpaperEnumerator(context).execute(listQueryIntentServices);
    }

    private List<LiveWallpaperInfo> generatePlaceholderViews(int i) {
        ArrayList arrayList = new ArrayList(i);
        for (int i2 = 0; i2 < i; i2++) {
            arrayList.add(new LiveWallpaperInfo());
        }
        return arrayList;
    }

    @Override
    public int getCount() {
        if (this.mWallpapers == null) {
            return 0;
        }
        return this.mWallpapers.size();
    }

    @Override
    public Object getItem(int i) {
        return this.mWallpapers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        if (view == null) {
            view = this.mInflater.inflate(R.layout.live_wallpaper_entry, viewGroup, false);
            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) view.findViewById(R.id.title);
            viewHolder.thumbnail = (ImageView) view.findViewById(R.id.thumbnail);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }
        LiveWallpaperInfo liveWallpaperInfo = this.mWallpapers.get(i);
        if (viewHolder.thumbnail != null) {
            viewHolder.thumbnail.setImageDrawable(liveWallpaperInfo.thumbnail);
        }
        if (viewHolder.title != null && liveWallpaperInfo.info != null) {
            viewHolder.title.setText(liveWallpaperInfo.info.loadLabel(this.mPackageManager));
            if (viewHolder.thumbnail == null) {
                viewHolder.title.setCompoundDrawablesWithIntrinsicBounds((Drawable) null, liveWallpaperInfo.thumbnail, (Drawable) null, (Drawable) null);
            }
        }
        return view;
    }

    public class LiveWallpaperInfo {
        public WallpaperInfo info;
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
        protected Void doInBackground(List<ResolveInfo>... listArr) {
            final PackageManager packageManager = this.mContext.getPackageManager();
            List<ResolveInfo> list = listArr[0];
            Resources resources = this.mContext.getResources();
            BitmapDrawable bitmapDrawable = (BitmapDrawable) resources.getDrawable(R.drawable.livewallpaper_placeholder);
            Paint paint = new Paint(5);
            paint.setTextAlign(Paint.Align.CENTER);
            Canvas canvas = new Canvas();
            Collections.sort(list, new Comparator<ResolveInfo>() {
                final Collator mCollator = Collator.getInstance();

                @Override
                public int compare(ResolveInfo resolveInfo, ResolveInfo resolveInfo2) {
                    return this.mCollator.compare(resolveInfo.loadLabel(packageManager), resolveInfo2.loadLabel(packageManager));
                }
            });
            for (ResolveInfo resolveInfo : list) {
                try {
                    WallpaperInfo wallpaperInfo = new WallpaperInfo(this.mContext, resolveInfo);
                    LiveWallpaperInfo liveWallpaperInfo = LiveWallpaperListAdapter.this.new LiveWallpaperInfo();
                    liveWallpaperInfo.info = wallpaperInfo;
                    Drawable drawableLoadThumbnail = wallpaperInfo.loadThumbnail(packageManager);
                    if (drawableLoadThumbnail == null) {
                        int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.live_wallpaper_thumbnail_width);
                        int dimensionPixelSize2 = resources.getDimensionPixelSize(R.dimen.live_wallpaper_thumbnail_height);
                        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(dimensionPixelSize, dimensionPixelSize2, Bitmap.Config.ARGB_8888);
                        paint.setColor(resources.getColor(R.color.live_wallpaper_thumbnail_background));
                        canvas.setBitmap(bitmapCreateBitmap);
                        canvas.drawPaint(paint);
                        bitmapDrawable.setBounds(0, 0, dimensionPixelSize, dimensionPixelSize2);
                        bitmapDrawable.setGravity(17);
                        bitmapDrawable.draw(canvas);
                        String string = wallpaperInfo.loadLabel(packageManager).toString();
                        paint.setColor(resources.getColor(R.color.live_wallpaper_thumbnail_text_color));
                        paint.setTextSize(resources.getDimensionPixelSize(R.dimen.live_wallpaper_thumbnail_text_size));
                        canvas.drawText(string, (int) (((double) dimensionPixelSize) * 0.5d), dimensionPixelSize2 - resources.getDimensionPixelSize(R.dimen.live_wallpaper_thumbnail_text_offset), paint);
                        drawableLoadThumbnail = new BitmapDrawable(resources, bitmapCreateBitmap);
                    }
                    liveWallpaperInfo.thumbnail = drawableLoadThumbnail;
                    publishProgress(liveWallpaperInfo);
                } catch (IOException e) {
                    Log.w("LiveWallpaperListAdapter", "Skipping wallpaper " + resolveInfo.serviceInfo, e);
                } catch (XmlPullParserException e2) {
                    Log.w("LiveWallpaperListAdapter", "Skipping wallpaper " + resolveInfo.serviceInfo, e2);
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(LiveWallpaperInfo... liveWallpaperInfoArr) {
            for (LiveWallpaperInfo liveWallpaperInfo : liveWallpaperInfoArr) {
                liveWallpaperInfo.thumbnail.setDither(true);
                if (this.mWallpaperPosition < LiveWallpaperListAdapter.this.mWallpapers.size()) {
                    LiveWallpaperListAdapter.this.mWallpapers.set(this.mWallpaperPosition, liveWallpaperInfo);
                } else {
                    LiveWallpaperListAdapter.this.mWallpapers.add(liveWallpaperInfo);
                }
                this.mWallpaperPosition++;
                LiveWallpaperListAdapter.this.notifyDataSetChanged();
            }
        }
    }
}
