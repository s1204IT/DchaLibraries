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

public class WallpaperHandler extends Thread implements DialogInterface.OnCancelListener, MenuItem.OnMenuItemClickListener {
    private boolean mCanceled = false;
    private Context mContext;
    private String mUrl;
    private ProgressDialog mWallpaperProgress;

    public WallpaperHandler(Context context, String str) {
        this.mContext = context;
        this.mUrl = str;
    }

    @Override
    public void onCancel(DialogInterface dialogInterface) {
        this.mCanceled = true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
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
    public void run() throws Throwable {
        InputStream inputStreamOpenStream;
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(this.mContext);
        Drawable drawable = wallpaperManager.getDrawable();
        InputStream inputStream = null;
        try {
        } catch (Throwable th) {
            th = th;
        }
        try {
            try {
                inputStreamOpenStream = openStream();
                if (inputStreamOpenStream != null) {
                    try {
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        byte[] bArr = new byte[8192];
                        while (true) {
                            int i = inputStreamOpenStream.read(bArr);
                            if (i == -1) {
                                break;
                            } else {
                                byteArrayOutputStream.write(bArr, 0, i);
                            }
                        }
                        byte[] byteArray = byteArrayOutputStream.toByteArray();
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
                        int desiredMinimumWidth = (int) (((double) wallpaperManager.getDesiredMinimumWidth()) * 1.25d);
                        int desiredMinimumHeight = (int) (((double) wallpaperManager.getDesiredMinimumHeight()) * 1.25d);
                        int i2 = options.outWidth;
                        int i3 = options.outHeight;
                        int i4 = 1;
                        while (true) {
                            if (i2 <= desiredMinimumWidth && i3 <= desiredMinimumHeight) {
                                break;
                            }
                            i4 <<= 1;
                            i2 >>= 1;
                            i3 >>= 1;
                        }
                        options.inJustDecodeBounds = false;
                        options.inSampleSize = i4;
                        Bitmap bitmapDecodeByteArray = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length, options);
                        if (bitmapDecodeByteArray != null) {
                            wallpaperManager.setBitmap(bitmapDecodeByteArray);
                        } else {
                            Log.e("WallpaperHandler", "Unable to set new wallpaper, decodeStream returned null.");
                        }
                    } catch (IOException e) {
                        e = e;
                        e.printStackTrace();
                        Log.e("WallpaperHandler", "Unable to set new wallpaper");
                        this.mCanceled = true;
                        if (inputStreamOpenStream != null) {
                            inputStreamOpenStream.close();
                        }
                        if (this.mCanceled) {
                        }
                        destroyDialog();
                    }
                }
            } catch (IOException e2) {
            }
        } catch (IOException e3) {
            e = e3;
            inputStreamOpenStream = null;
        } catch (Throwable th2) {
            th = th2;
            if (0 != 0) {
                try {
                    inputStream.close();
                } catch (IOException e4) {
                }
            }
            throw th;
        }
        if (inputStreamOpenStream != null) {
            inputStreamOpenStream.close();
        }
        if (this.mCanceled) {
            int intrinsicWidth = drawable.getIntrinsicWidth();
            int intrinsicHeight = drawable.getIntrinsicHeight();
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmapCreateBitmap);
            drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
            drawable.draw(canvas);
            canvas.setBitmap(null);
            try {
                wallpaperManager.setBitmap(bitmapCreateBitmap);
            } catch (IOException e5) {
                Log.e("WallpaperHandler", "Unable to restore old wallpaper.");
            }
            this.mCanceled = false;
        }
        destroyDialog();
    }

    private InputStream openStream() throws IOException {
        if (DataUri.isDataUri(this.mUrl)) {
            return new ByteArrayInputStream(new DataUri(this.mUrl).getData());
        }
        URLConnection uRLConnectionOpenConnection = new URL(this.mUrl).openConnection();
        uRLConnectionOpenConnection.setRequestProperty("Connection", "close");
        return uRLConnectionOpenConnection.getInputStream();
    }

    public void destroyDialog() {
        if (this.mWallpaperProgress != null && this.mWallpaperProgress.isShowing()) {
            this.mWallpaperProgress.dismiss();
        }
    }
}
