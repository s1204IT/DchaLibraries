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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread;
import java.net.URL;
import java.net.URLConnection;

public class WallpaperHandler extends Thread implements MenuItem.OnMenuItemClickListener, DialogInterface.OnCancelListener {
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
        InputStream inputStream = null;
        try {
            try {
                InputStream inputstream = openStream();
                if (inputstream != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    while (true) {
                        int seg = inputstream.read(buf);
                        if (seg == -1) {
                            break;
                        } else {
                            baos.write(buf, 0, seg);
                        }
                    }
                    byte[] imageData = baos.toByteArray();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
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
                    Bitmap scaledWallpaper = BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
                    if (scaledWallpaper != null) {
                        wm.setBitmap(scaledWallpaper);
                    } else {
                        Log.e("WallpaperHandler", "Unable to set new wallpaper, decodeStream returned null.");
                    }
                }
                if (inputstream != null) {
                    try {
                        inputstream.close();
                    } catch (IOException e) {
                    }
                }
            } catch (IOException e2) {
                e2.printStackTrace();
                Log.e("WallpaperHandler", "Unable to set new wallpaper");
                this.mCanceled = true;
                if (0 != 0) {
                    try {
                        inputStream.close();
                    } catch (IOException e3) {
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
                } catch (IOException e4) {
                    Log.e("WallpaperHandler", "Unable to restore old wallpaper.");
                }
                this.mCanceled = false;
            }
            destroyDialog();
        } catch (Throwable th) {
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e5) {
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
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Connection", "close");
        InputStream inputStream2 = conn.getInputStream();
        return inputStream2;
    }

    public void destroyDialog() {
        if (this.mWallpaperProgress == null || !this.mWallpaperProgress.isShowing()) {
            return;
        }
        this.mWallpaperProgress.dismiss();
    }
}
