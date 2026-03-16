package com.android.launcher2;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SpinnerAdapter;
import com.android.launcher.R;
import java.io.IOException;
import java.util.ArrayList;

public class WallpaperChooserDialogFragment extends DialogFragment implements AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {
    private boolean mEmbedded;
    private ArrayList<Integer> mImages;
    private WallpaperLoader mLoader;
    private ArrayList<Integer> mThumbs;
    private WallpaperDrawable mWallpaperDrawable = new WallpaperDrawable();

    public static WallpaperChooserDialogFragment newInstance() {
        WallpaperChooserDialogFragment fragment = new WallpaperChooserDialogFragment();
        fragment.setCancelable(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("com.android.launcher2.WallpaperChooserDialogFragment.EMBEDDED_KEY")) {
            this.mEmbedded = savedInstanceState.getBoolean("com.android.launcher2.WallpaperChooserDialogFragment.EMBEDDED_KEY");
        } else {
            this.mEmbedded = isInLayout();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("com.android.launcher2.WallpaperChooserDialogFragment.EMBEDDED_KEY", this.mEmbedded);
    }

    private void cancelLoader() {
        if (this.mLoader != null && this.mLoader.getStatus() != AsyncTask.Status.FINISHED) {
            this.mLoader.cancel(true);
            this.mLoader = null;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        cancelLoader();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cancelLoader();
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        Activity activity = getActivity();
        if (activity != null) {
            activity.finish();
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        findWallpapers();
        return null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        findWallpapers();
        if (!this.mEmbedded) {
            return null;
        }
        View view = inflater.inflate(R.layout.wallpaper_chooser, container, false);
        view.setBackground(this.mWallpaperDrawable);
        final Gallery gallery = (Gallery) view.findViewById(R.id.gallery);
        gallery.setCallbackDuringFling(false);
        gallery.setOnItemSelectedListener(this);
        gallery.setAdapter((SpinnerAdapter) new ImageAdapter(getActivity()));
        View setButton = view.findViewById(R.id.set);
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WallpaperChooserDialogFragment.this.selectWallpaper(gallery.getSelectedItemPosition());
            }
        });
        return view;
    }

    private void selectWallpaper(int position) {
        try {
            WallpaperManager wpm = (WallpaperManager) getActivity().getSystemService("wallpaper");
            wpm.setResource(this.mImages.get(position).intValue());
            Activity activity = getActivity();
            activity.setResult(-1);
            activity.finish();
        } catch (IOException e) {
            Log.e("Launcher.WallpaperChooserDialogFragment", "Failed to set wallpaper: " + e);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        selectWallpaper(position);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (this.mLoader != null && this.mLoader.getStatus() != AsyncTask.Status.FINISHED) {
            this.mLoader.cancel();
        }
        this.mLoader = (WallpaperLoader) new WallpaperLoader().execute(Integer.valueOf(position));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void findWallpapers() {
        this.mThumbs = new ArrayList<>(24);
        this.mImages = new ArrayList<>(24);
        Resources resources = getResources();
        String packageName = resources.getResourcePackageName(R.array.wallpapers);
        addWallpapers(resources, packageName, R.array.wallpapers);
        addWallpapers(resources, packageName, R.array.extra_wallpapers);
    }

    private void addWallpapers(Resources resources, String packageName, int list) {
        int thumbRes;
        String[] extras = resources.getStringArray(list);
        for (String extra : extras) {
            int res = resources.getIdentifier(extra, "drawable", packageName);
            if (res != 0 && (thumbRes = resources.getIdentifier(extra + "_small", "drawable", packageName)) != 0) {
                this.mThumbs.add(Integer.valueOf(thumbRes));
                this.mImages.add(Integer.valueOf(res));
            }
        }
    }

    private class ImageAdapter extends BaseAdapter implements ListAdapter, SpinnerAdapter {
        private LayoutInflater mLayoutInflater;

        ImageAdapter(Activity activity) {
            this.mLayoutInflater = activity.getLayoutInflater();
        }

        @Override
        public int getCount() {
            return WallpaperChooserDialogFragment.this.mThumbs.size();
        }

        @Override
        public Object getItem(int position) {
            return Integer.valueOf(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = this.mLayoutInflater.inflate(R.layout.wallpaper_item, parent, false);
            } else {
                view = convertView;
            }
            ImageView image = (ImageView) view.findViewById(R.id.wallpaper_image);
            int thumbRes = ((Integer) WallpaperChooserDialogFragment.this.mThumbs.get(position)).intValue();
            image.setImageResource(thumbRes);
            Drawable thumbDrawable = image.getDrawable();
            if (thumbDrawable != null) {
                thumbDrawable.setDither(true);
            } else {
                Log.e("Launcher.WallpaperChooserDialogFragment", "Error decoding thumbnail resId=" + thumbRes + " for wallpaper #" + position);
            }
            return view;
        }
    }

    class WallpaperLoader extends AsyncTask<Integer, Void, Bitmap> {
        WallpaperLoader() {
        }

        @Override
        protected Bitmap doInBackground(Integer... params) {
            if (isCancelled()) {
                return null;
            }
            try {
                Drawable d = WallpaperChooserDialogFragment.this.getResources().getDrawable(((Integer) WallpaperChooserDialogFragment.this.mImages.get(params[0].intValue())).intValue());
                if (d instanceof BitmapDrawable) {
                    return ((BitmapDrawable) d).getBitmap();
                }
                return null;
            } catch (OutOfMemoryError e) {
                Log.w("Launcher.WallpaperChooserDialogFragment", String.format("Out of memory trying to load wallpaper res=%08x", params[0]), e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap b) {
            if (b != null) {
                if (!isCancelled()) {
                    View v = WallpaperChooserDialogFragment.this.getView();
                    if (v != null) {
                        WallpaperChooserDialogFragment.this.mWallpaperDrawable.setBitmap(b);
                        v.postInvalidate();
                    } else {
                        WallpaperChooserDialogFragment.this.mWallpaperDrawable.setBitmap(null);
                    }
                    WallpaperChooserDialogFragment.this.mLoader = null;
                    return;
                }
                b.recycle();
            }
        }

        void cancel() {
            super.cancel(true);
        }
    }

    static class WallpaperDrawable extends Drawable {
        Bitmap mBitmap;
        int mIntrinsicHeight;
        int mIntrinsicWidth;
        Matrix mMatrix;

        WallpaperDrawable() {
        }

        void setBitmap(Bitmap bitmap) {
            this.mBitmap = bitmap;
            if (this.mBitmap != null) {
                this.mIntrinsicWidth = this.mBitmap.getWidth();
                this.mIntrinsicHeight = this.mBitmap.getHeight();
                this.mMatrix = null;
            }
        }

        @Override
        public void draw(Canvas canvas) {
            if (this.mBitmap != null) {
                if (this.mMatrix == null) {
                    int vwidth = canvas.getWidth();
                    int vheight = canvas.getHeight();
                    int dwidth = this.mIntrinsicWidth;
                    int dheight = this.mIntrinsicHeight;
                    float scale = 1.0f;
                    if (dwidth < vwidth || dheight < vheight) {
                        scale = Math.max(vwidth / dwidth, vheight / dheight);
                    }
                    float dx = ((vwidth - (dwidth * scale)) * 0.5f) + 0.5f;
                    float dy = ((vheight - (dheight * scale)) * 0.5f) + 0.5f;
                    this.mMatrix = new Matrix();
                    this.mMatrix.setScale(scale, scale);
                    this.mMatrix.postTranslate((int) dx, (int) dy);
                }
                canvas.drawBitmap(this.mBitmap, this.mMatrix, null);
            }
        }

        @Override
        public int getOpacity() {
            return -1;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }
    }
}
