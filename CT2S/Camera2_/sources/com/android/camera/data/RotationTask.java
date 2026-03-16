package com.android.camera.data;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import com.android.camera.data.LocalMediaData;
import com.android.camera.debug.Log;
import com.android.camera.exif.ExifInterface;
import com.android.camera.exif.ExifTag;
import com.android.camera2.R;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class RotationTask extends AsyncTask<LocalData, Void, LocalData> {
    private static final Log.Tag TAG = new Log.Tag("RotationTask");
    private final LocalDataAdapter mAdapter;
    private final boolean mClockwise;
    private final Context mContext;
    private final int mCurrentDataId;
    private ProgressDialog mProgress;

    public RotationTask(Context context, LocalDataAdapter adapter, int currentDataId, boolean clockwise) {
        this.mContext = context;
        this.mAdapter = adapter;
        this.mCurrentDataId = currentDataId;
        this.mClockwise = clockwise;
    }

    @Override
    protected void onPreExecute() {
        this.mProgress = new ProgressDialog(this.mContext);
        int titleStringId = this.mClockwise ? R.string.rotate_right : R.string.rotate_left;
        this.mProgress.setTitle(this.mContext.getString(titleStringId));
        this.mProgress.setMessage(this.mContext.getString(R.string.please_wait));
        this.mProgress.setCancelable(false);
        this.mProgress.show();
    }

    @Override
    protected LocalData doInBackground(LocalData... data) {
        return rotateInJpegExif(data[0]);
    }

    private LocalData rotateInJpegExif(LocalData data) throws Throwable {
        int finalRotationDegrees;
        if (!(data instanceof LocalMediaData.PhotoData)) {
            Log.w(TAG, "Rotation can only happen on PhotoData.");
            return null;
        }
        LocalMediaData.PhotoData imageData = (LocalMediaData.PhotoData) data;
        int originRotation = imageData.getRotation();
        if (this.mClockwise) {
            finalRotationDegrees = (originRotation + 90) % 360;
        } else {
            finalRotationDegrees = (originRotation + 270) % 360;
        }
        String filePath = imageData.getPath();
        ContentValues values = new ContentValues();
        boolean success = false;
        int newOrientation = 0;
        if (imageData.getMimeType().equalsIgnoreCase("image/jpeg")) {
            ExifInterface exifInterface = new ExifInterface();
            ExifTag tag = exifInterface.buildTag(ExifInterface.TAG_ORIENTATION, Short.valueOf(ExifInterface.getOrientationValueForRotation(finalRotationDegrees)));
            if (tag != null) {
                exifInterface.setTag(tag);
                try {
                    exifInterface.forceRewriteExif(filePath);
                    long fileSize = new File(filePath).length();
                    values.put("_size", Long.valueOf(fileSize));
                    newOrientation = finalRotationDegrees;
                    success = true;
                } catch (FileNotFoundException e) {
                    Log.w(TAG, "Cannot find file to set exif: " + filePath);
                } catch (IOException e2) {
                    Log.w(TAG, "Cannot set exif data: " + filePath);
                }
            } else {
                Log.w(TAG, "Cannot build tag: " + ExifInterface.TAG_ORIENTATION);
            }
        }
        if (!success) {
            return null;
        }
        values.put("orientation", Integer.valueOf(finalRotationDegrees));
        this.mContext.getContentResolver().update(imageData.getUri(), values, null, null);
        double[] latLong = data.getLatLong();
        double latitude = 0.0d;
        double longitude = 0.0d;
        if (latLong != null) {
            latitude = latLong[0];
            longitude = latLong[1];
        }
        return new LocalMediaData.PhotoData(data.getContentId(), data.getTitle(), data.getMimeType(), data.getDateTaken(), data.getDateModified(), data.getPath(), newOrientation, imageData.getWidth(), imageData.getHeight(), data.getSizeInBytes(), latitude, longitude);
    }

    @Override
    protected void onPostExecute(LocalData result) {
        this.mProgress.dismiss();
        if (result != null) {
            this.mAdapter.updateData(this.mCurrentDataId, result);
        }
    }
}
