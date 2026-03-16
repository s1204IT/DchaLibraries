package com.android.contacts.util;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.content.FileProvider;
import android.util.Log;
import com.android.contacts.R;
import com.google.common.io.Closeables;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ContactPhotoUtils {
    public static Uri generateTempImageUri(Context context) {
        String fileProviderAuthority = context.getResources().getString(R.string.photo_file_provider_authority);
        return FileProvider.getUriForFile(context, fileProviderAuthority, new File(pathForTempPhoto(context, generateTempPhotoFileName())));
    }

    public static Uri generateTempCroppedImageUri(Context context) {
        String fileProviderAuthority = context.getResources().getString(R.string.photo_file_provider_authority);
        return FileProvider.getUriForFile(context, fileProviderAuthority, new File(pathForTempPhoto(context, generateTempCroppedPhotoFileName())));
    }

    private static String pathForTempPhoto(Context context, String fileName) {
        File dir = context.getCacheDir();
        dir.mkdirs();
        File f = new File(dir, fileName);
        return f.getAbsolutePath();
    }

    private static String generateTempPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss", Locale.US);
        return "ContactPhoto-" + dateFormat.format(date) + ".jpg";
    }

    private static String generateTempCroppedPhotoFileName() {
        Date date = new Date(System.currentTimeMillis());
        SimpleDateFormat dateFormat = new SimpleDateFormat("'IMG'_yyyyMMdd_HHmmss", Locale.US);
        return "ContactPhoto-" + dateFormat.format(date) + "-cropped.jpg";
    }

    public static Bitmap getBitmapFromUri(Context context, Uri uri) throws FileNotFoundException {
        InputStream imageStream = context.getContentResolver().openInputStream(uri);
        try {
            return BitmapFactory.decodeStream(imageStream);
        } finally {
            Closeables.closeQuietly(imageStream);
        }
    }

    public static byte[] compressBitmap(Bitmap bitmap) {
        int size = bitmap.getWidth() * bitmap.getHeight() * 4;
        ByteArrayOutputStream out = new ByteArrayOutputStream(size);
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
            return out.toByteArray();
        } catch (IOException e) {
            Log.w("ContactPhotoUtils", "Unable to serialize photo: " + e.toString());
            return null;
        }
    }

    public static void addCropExtras(Intent intent, int photoSize) {
        intent.putExtra("crop", "true");
        intent.putExtra("scale", true);
        intent.putExtra("scaleUpIfNeeded", true);
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", photoSize);
        intent.putExtra("outputY", photoSize);
    }

    public static void addPhotoPickerExtras(Intent intent, Uri photoUri) {
        intent.putExtra("output", photoUri);
        intent.addFlags(3);
        intent.setClipData(ClipData.newRawUri("output", photoUri));
    }

    public static boolean savePhotoFromUriToUri(Context context, Uri inputUri, Uri outputUri, boolean deleteAfterSave) {
        FileOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            try {
                outputStream = context.getContentResolver().openAssetFileDescriptor(outputUri, "rw").createOutputStream();
                inputStream = context.getContentResolver().openInputStream(inputUri);
                byte[] buffer = new byte[16384];
                int totalLength = 0;
                while (true) {
                    int length = inputStream.read(buffer);
                    if (length <= 0) {
                        break;
                    }
                    outputStream.write(buffer, 0, length);
                    totalLength += length;
                }
                Log.v("ContactPhotoUtils", "Wrote " + totalLength + " bytes for photo " + inputUri.toString());
                Closeables.closeQuietly(inputStream);
                Closeables.closeQuietly(outputStream);
                if (deleteAfterSave) {
                    context.getContentResolver().delete(inputUri, null, null);
                }
                return true;
            } catch (IOException e) {
                Log.e("ContactPhotoUtils", "Failed to write photo: " + inputUri.toString() + " because: " + e);
                Closeables.closeQuietly(inputStream);
                Closeables.closeQuietly(outputStream);
                if (!deleteAfterSave) {
                    return false;
                }
                context.getContentResolver().delete(inputUri, null, null);
                return false;
            }
        } catch (Throwable th) {
            Closeables.closeQuietly(inputStream);
            Closeables.closeQuietly(outputStream);
            if (deleteAfterSave) {
                context.getContentResolver().delete(inputUri, null, null);
            }
            throw th;
        }
    }
}
