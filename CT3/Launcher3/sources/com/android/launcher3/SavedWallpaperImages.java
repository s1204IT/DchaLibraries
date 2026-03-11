package com.android.launcher3;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import com.android.gallery3d.common.BitmapCropTask;
import com.android.launcher3.WallpaperCropActivity;
import com.android.launcher3.WallpaperPickerActivity;
import com.android.photos.views.TiledImageRenderer;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class SavedWallpaperImages extends BaseAdapter implements ListAdapter {
    private static String TAG = "Launcher3.SavedWallpaperImages";
    Context mContext;
    private ImageDb mDb;
    ArrayList<SavedWallpaperTile> mImages;
    LayoutInflater mLayoutInflater;

    public static class SavedWallpaperTile extends WallpaperPickerActivity.FileWallpaperInfo {
        private int mDbId;
        private Float[] mExtras;

        public SavedWallpaperTile(int dbId, File target, Drawable thumb, Float[] extras) {
            super(target, thumb);
            this.mDbId = dbId;
            this.mExtras = (extras == null || extras.length != 3) ? null : extras;
        }

        @Override
        public void onDelete(WallpaperPickerActivity a) {
            a.getSavedImages().deleteImage(this.mDbId);
        }

        @Override
        protected WallpaperCropActivity.CropViewScaleAndOffsetProvider getCropViewScaleAndOffsetProvider() {
            if (this.mExtras != null) {
                return new WallpaperCropActivity.CropViewScaleAndOffsetProvider() {
                    @Override
                    public void updateCropView(WallpaperCropActivity a, TiledImageRenderer.TileSource src) {
                        a.mCropView.setScaleAndCenter(SavedWallpaperTile.this.mExtras[0].floatValue(), SavedWallpaperTile.this.mExtras[1].floatValue(), SavedWallpaperTile.this.mExtras[2].floatValue());
                    }
                };
            }
            return null;
        }

        @Override
        public void onSave(WallpaperPickerActivity a) {
            if (this.mExtras == null) {
                super.onSave(a);
            } else {
                boolean shouldFadeOutOnFinish = a.getWallpaperParallaxOffset() == 0.0f;
                a.cropImageAndSetWallpaper(Uri.fromFile(this.mFile), (BitmapCropTask.OnBitmapCroppedHandler) null, true, shouldFadeOutOnFinish);
            }
        }
    }

    public SavedWallpaperImages(Context context) {
        ImageDb.moveFromCacheDirectoryIfNecessary(context);
        this.mDb = new ImageDb(context);
        this.mContext = context;
        this.mLayoutInflater = LayoutInflater.from(context);
    }

    public void loadThumbnailsAndImageIdList() {
        this.mImages = new ArrayList<>();
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor result = db.query("saved_wallpaper_images", new String[]{"id", "image_thumbnail", "image", "extras"}, null, null, null, null, "id DESC", null);
        while (result.moveToNext()) {
            String filename = result.getString(1);
            File file = new File(this.mContext.getFilesDir(), filename);
            Bitmap thumb = BitmapFactory.decodeFile(file.getAbsolutePath());
            if (thumb != null) {
                Float[] extras = null;
                String extraStr = result.getString(3);
                if (extraStr != null) {
                    String[] parts = extraStr.split(",");
                    extras = new Float[parts.length];
                    for (int i = 0; i < parts.length; i++) {
                        try {
                            extras[i] = Float.valueOf(Float.parseFloat(parts[i]));
                        } catch (Exception e) {
                            extras = null;
                        }
                    }
                }
                this.mImages.add(new SavedWallpaperTile(result.getInt(0), new File(this.mContext.getFilesDir(), result.getString(2)), new BitmapDrawable(thumb), extras));
            }
        }
        result.close();
    }

    @Override
    public int getCount() {
        return this.mImages.size();
    }

    @Override
    public SavedWallpaperTile getItem(int position) {
        return this.mImages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Drawable thumbDrawable = this.mImages.get(position).mThumb;
        if (thumbDrawable == null) {
            Log.e(TAG, "Error decoding thumbnail for wallpaper #" + position);
        }
        return WallpaperPickerActivity.createImageTileView(this.mLayoutInflater, convertView, parent, thumbDrawable);
    }

    private Pair<String, String> getImageFilenames(int id) {
        SQLiteDatabase db = this.mDb.getReadableDatabase();
        Cursor result = db.query("saved_wallpaper_images", new String[]{"image_thumbnail", "image"}, "id = ?", new String[]{Integer.toString(id)}, null, null, null, null);
        if (result.getCount() <= 0) {
            return null;
        }
        result.moveToFirst();
        String thumbFilename = result.getString(0);
        String imageFilename = result.getString(1);
        result.close();
        return new Pair<>(thumbFilename, imageFilename);
    }

    public void deleteImage(int id) {
        Pair<String, String> filenames = getImageFilenames(id);
        File imageFile = new File(this.mContext.getFilesDir(), (String) filenames.first);
        imageFile.delete();
        File thumbFile = new File(this.mContext.getFilesDir(), (String) filenames.second);
        thumbFile.delete();
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        db.delete("saved_wallpaper_images", "id = ?", new String[]{Integer.toString(id)});
    }

    public void writeImage(Bitmap thumbnail, byte[] imageBytes) {
        try {
            writeImage(thumbnail, new ByteArrayInputStream(imageBytes), (Float[]) null);
        } catch (IOException e) {
            Log.e(TAG, "Failed writing images to storage " + e);
        }
    }

    public void writeImage(Bitmap thumbnail, Uri uri, Float[] extras) {
        try {
            writeImage(thumbnail, this.mContext.getContentResolver().openInputStream(uri), extras);
        } catch (IOException e) {
            Log.e(TAG, "Failed writing images to storage " + e);
        }
    }

    private void writeImage(Bitmap thumbnail, InputStream in, Float[] extras) throws IOException {
        File imageFile = File.createTempFile("wallpaper", "", this.mContext.getFilesDir());
        FileOutputStream imageFileStream = this.mContext.openFileOutput(imageFile.getName(), 0);
        byte[] buf = new byte[4096];
        while (true) {
            int len = in.read(buf);
            if (len <= 0) {
                break;
            } else {
                imageFileStream.write(buf, 0, len);
            }
        }
        imageFileStream.close();
        in.close();
        File thumbFile = File.createTempFile("wallpaperthumb", "", this.mContext.getFilesDir());
        FileOutputStream thumbFileStream = this.mContext.openFileOutput(thumbFile.getName(), 0);
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 95, thumbFileStream);
        thumbFileStream.close();
        SQLiteDatabase db = this.mDb.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("image_thumbnail", thumbFile.getName());
        values.put("image", imageFile.getName());
        if (extras != null) {
            values.put("extras", TextUtils.join(",", extras));
        }
        db.insert("saved_wallpaper_images", null, values);
    }

    static class ImageDb extends SQLiteOpenHelper {
        Context mContext;

        public ImageDb(Context context) {
            super(context, context.getDatabasePath("saved_wallpaper_images.db").getPath(), (SQLiteDatabase.CursorFactory) null, 2);
            this.mContext = context;
        }

        public static void moveFromCacheDirectoryIfNecessary(Context context) {
            File oldSavedImagesFile = new File(context.getCacheDir(), "saved_wallpaper_images.db");
            File savedImagesFile = context.getDatabasePath("saved_wallpaper_images.db");
            if (!oldSavedImagesFile.exists()) {
                return;
            }
            oldSavedImagesFile.renameTo(savedImagesFile);
        }

        @Override
        public void onCreate(SQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS saved_wallpaper_images (id INTEGER NOT NULL, image_thumbnail TEXT NOT NULL, image TEXT NOT NULL, extras TEXT, PRIMARY KEY (id ASC) );");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion == 1) {
                db.execSQL("ALTER TABLE saved_wallpaper_images ADD COLUMN extras TEXT;");
            } else {
                if (oldVersion == newVersion) {
                    return;
                }
                db.execSQL("DELETE FROM saved_wallpaper_images");
                onCreate(db);
            }
        }
    }
}
