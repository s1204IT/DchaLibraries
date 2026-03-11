package com.android.browser;

import android.app.ProgressDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MenuItem;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread;
import java.net.URL;

public class WallpaperHandler extends Thread implements DialogInterface.OnCancelListener, MenuItem.OnMenuItemClickListener {
    private boolean mCanceled = false;
    private Context mContext;
    private String mUrl;
    private ProgressDialog mWallpaperProgress;

    public WallpaperHandler(Context context, String url) {
        this.mContext = context;
        this.mUrl = url;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        this.mCanceled = true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        if (this.mUrl != null && getState() == Thread.State.NEW) {
            this.mWallpaperProgress = new ProgressDialog(this.mContext);
            this.mWallpaperProgress.setIndeterminate(true);
            this.mWallpaperProgress.setMessage(this.mContext.getResources().getText(R.string.progress_dialog_setting_wallpaper));
            this.mWallpaperProgress.setCancelable(true);
            this.mWallpaperProgress.setOnCancelListener(this);
            this.mWallpaperProgress.show();
            start();
        }
        return true;
    }

    @Override
    public void run() {
        WallpaperManager wm = WallpaperManager.getInstance(this.mContext);
        Drawable oldWallpaper = wm.getDrawable();
        InputStream inputstream = null;
        try {
            try {
                InputStream inputstream2 = openStream();
                if (inputstream2 != null) {
                    if (!inputstream2.markSupported()) {
                        inputstream2 = new BufferedInputStream(inputstream2, 131072);
                    }
                    inputstream2.mark(131072);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(new BufferedInputStream(inputstream2), null, options);
                    int maxWidth = wm.getDesiredMinimumWidth();
                    int maxHeight = wm.getDesiredMinimumHeight();
                    int maxWidth2 = (int) (((double) maxWidth) * 1.25d);
                    int maxHeight2 = (int) (((double) maxHeight) * 1.25d);
                    int bmWidth = options.outWidth;
                    int bmHeight = options.outHeight;
                    int scale = 1;
                    while (true) {
                        if (bmWidth <= maxWidth2 && bmHeight <= maxHeight2) {
                            break;
                        }
                        scale <<= 1;
                        bmWidth >>= 1;
                        bmHeight >>= 1;
                    }
                    options.inJustDecodeBounds = false;
                    options.inSampleSize = scale;
                    try {
                        inputstream2.reset();
                    } catch (IOException e) {
                        inputstream2.close();
                        inputstream2 = openStream();
                    }
                    Bitmap scaledWallpaper = BitmapFactory.decodeStream(inputstream2, null, options);
                    if (scaledWallpaper != null) {
                        wm.setBitmap(scaledWallpaper);
                    } else {
                        Log.e("WallpaperHandler", "Unable to set new wallpaper, decodeStream returned null.");
                    }
                }
                if (inputstream2 != null) {
                    try {
                        inputstream2.close();
                    } catch (IOException e2) {
                    }
                }
            } catch (IOException e3) {
                Log.e("WallpaperHandler", "Unable to set new wallpaper");
                this.mCanceled = true;
                if (0 != 0) {
                    try {
                        inputstream.close();
                    } catch (IOException e4) {
                    }
                }
            }
            if (this.mCanceled) {
                int width = oldWallpaper.getIntrinsicWidth();
                int height = oldWallpaper.getIntrinsicHeight();
                Bitmap bm = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                Canvas canvas = new Canvas(bm);
                oldWallpaper.setBounds(0, 0, width, height);
                oldWallpaper.draw(canvas);
                canvas.setBitmap(null);
                try {
                    wm.setBitmap(bm);
                } catch (IOException e5) {
                    Log.e("WallpaperHandler", "Unable to restore old wallpaper.");
                }
                this.mCanceled = false;
            }
            if (this.mWallpaperProgress.isShowing()) {
                this.mWallpaperProgress.dismiss();
            }
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputstream.close();
                } catch (IOException e6) {
                }
            }
            throw th;
        }
    }

    private InputStream openStream() throws IOException {
        if (DataUri.isDataUri(this.mUrl)) {
            DataUri dataUri = new DataUri(this.mUrl);
            InputStream inputStream = new ByteArrayInputStream(dataUri.getData());
            return inputStream;
        }
        URL url = new URL(this.mUrl);
        InputStream inputStream2 = url.openStream();
        return inputStream2;
    }
}
