package com.android.wallpaperpicker;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import com.android.wallpaperpicker.tileinfo.FileWallpaperInfo;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SavedWallpaperImages {
    private static String TAG = "SavedWallpaperImages";
    private final Context mContext;
    private final ImageDb mDb;

    public static class SavedWallpaperInfo extends FileWallpaperInfo {
        private int mDbId;

        public SavedWallpaperInfo(int i, File file, Drawable drawable) {
            super(file, drawable);
            this.mDbId = i;
        }

        @Override
        public void onDelete(WallpaperPickerActivity wallpaperPickerActivity) {
            wallpaperPickerActivity.getSavedImages().deleteImage(this.mDbId);
        }
    }

    public SavedWallpaperImages(Context context) {
        ImageDb.moveFromCacheDirectoryIfNecessary(context);
        this.mDb = new ImageDb(context);
        this.mContext = context;
    }

    public List<SavedWallpaperInfo> loadThumbnailsAndImageIdList() {
        ArrayList arrayList = new ArrayList();
        Cursor cursorQuery = this.mDb.getReadableDatabase().query("saved_wallpaper_images", new String[]{"id", "image_thumbnail", "image"}, null, null, null, null, "id DESC", null);
        while (cursorQuery.moveToNext()) {
            Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(new File(this.mContext.getFilesDir(), cursorQuery.getString(1)).getAbsolutePath());
            if (bitmapDecodeFile != null) {
                arrayList.add(new SavedWallpaperInfo(cursorQuery.getInt(0), new File(this.mContext.getFilesDir(), cursorQuery.getString(2)), new BitmapDrawable(this.mContext.getResources(), bitmapDecodeFile)));
            }
        }
        cursorQuery.close();
        return arrayList;
    }

    public void deleteImage(int i) {
        SQLiteDatabase writableDatabase = this.mDb.getWritableDatabase();
        Cursor cursorQuery = writableDatabase.query("saved_wallpaper_images", new String[]{"image_thumbnail", "image"}, "id = ?", new String[]{Integer.toString(i)}, null, null, null, null);
        if (cursorQuery.moveToFirst()) {
            new File(this.mContext.getFilesDir(), cursorQuery.getString(0)).delete();
            new File(this.mContext.getFilesDir(), cursorQuery.getString(1)).delete();
        }
        cursorQuery.close();
        writableDatabase.delete("saved_wallpaper_images", "id = ?", new String[]{Integer.toString(i)});
    }

    public void writeImage(Bitmap bitmap, byte[] bArr) {
        try {
            File fileCreateTempFile = File.createTempFile("wallpaper", "", this.mContext.getFilesDir());
            FileOutputStream fileOutputStreamOpenFileOutput = this.mContext.openFileOutput(fileCreateTempFile.getName(), 0);
            fileOutputStreamOpenFileOutput.write(bArr);
            fileOutputStreamOpenFileOutput.close();
            File fileCreateTempFile2 = File.createTempFile("wallpaperthumb", "", this.mContext.getFilesDir());
            FileOutputStream fileOutputStreamOpenFileOutput2 = this.mContext.openFileOutput(fileCreateTempFile2.getName(), 0);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fileOutputStreamOpenFileOutput2);
            fileOutputStreamOpenFileOutput2.close();
            SQLiteDatabase writableDatabase = this.mDb.getWritableDatabase();
            ContentValues contentValues = new ContentValues();
            contentValues.put("image_thumbnail", fileCreateTempFile2.getName());
            contentValues.put("image", fileCreateTempFile.getName());
            writableDatabase.insert("saved_wallpaper_images", null, contentValues);
        } catch (IOException e) {
            Log.e(TAG, "Failed writing images to storage " + e);
        }
    }

    private static class ImageDb extends SQLiteOpenHelper {
        public ImageDb(Context context) {
            super(context, context.getDatabasePath("saved_wallpaper_images.db").getPath(), (SQLiteDatabase.CursorFactory) null, 1);
        }

        public static void moveFromCacheDirectoryIfNecessary(Context context) {
            File file = new File(context.getCacheDir(), "saved_wallpaper_images.db");
            File databasePath = context.getDatabasePath("saved_wallpaper_images.db");
            if (file.exists()) {
                file.renameTo(databasePath);
            }
        }

        @Override
        public void onCreate(SQLiteDatabase sQLiteDatabase) {
            sQLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS saved_wallpaper_images (id INTEGER NOT NULL, image_thumbnail TEXT NOT NULL, image TEXT NOT NULL, PRIMARY KEY (id ASC) );");
        }

        @Override
        public void onUpgrade(SQLiteDatabase sQLiteDatabase, int i, int i2) {
            if (i != i2) {
                sQLiteDatabase.execSQL("DELETE FROM saved_wallpaper_images");
            }
        }
    }
}
