package com.android.wallpaperpicker.tileinfo;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.os.Process;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.wallpaperpicker.R;
import com.android.wallpaperpicker.WallpaperPickerActivity;

public class PickImageInfo extends WallpaperTileInfo {
    @Override
    public void onClick(WallpaperPickerActivity wallpaperPickerActivity) {
        Intent type = new Intent("android.intent.action.GET_CONTENT").setType("image/*");
        type.putExtra("android.intent.extra.drm_level", 1);
        wallpaperPickerActivity.startActivityForResultSafely(type, 5);
    }

    @Override
    public View createView(Context context, LayoutInflater layoutInflater, ViewGroup viewGroup) {
        this.mView = layoutInflater.inflate(R.layout.wallpaper_picker_image_picker_item, viewGroup, false);
        Bitmap thumbnailOfLastPhoto = getThumbnailOfLastPhoto(context);
        if (thumbnailOfLastPhoto != null) {
            ImageView imageView = (ImageView) this.mView.findViewById(R.id.wallpaper_image);
            imageView.setImageBitmap(thumbnailOfLastPhoto);
            imageView.setColorFilter(context.getResources().getColor(R.color.wallpaper_picker_translucent_gray), PorterDuff.Mode.SRC_ATOP);
        }
        this.mView.setTag(this);
        return this.mView;
    }

    private Bitmap getThumbnailOfLastPhoto(Context context) {
        if (!(context.checkPermission("android.permission.READ_EXTERNAL_STORAGE", Process.myPid(), Process.myUid()) == 0)) {
            return null;
        }
        Cursor cursorQuery = MediaStore.Images.Media.query(context.getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, new String[]{"_id", "datetaken"}, null, null, "datetaken DESC LIMIT 1");
        if (cursorQuery != null) {
            thumbnail = cursorQuery.moveToNext() ? MediaStore.Images.Thumbnails.getThumbnail(context.getContentResolver(), cursorQuery.getInt(0), 1, null) : null;
            cursorQuery.close();
        }
        return thumbnail;
    }
}
