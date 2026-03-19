package com.android.gallery3d.filtershow.pipeline;

import android.R;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ProcessingTask;
import com.android.gallery3d.filtershow.tools.SaveImage;
import java.io.File;

public class ImageSavingTask extends ProcessingTask {
    private ProcessingService mProcessingService;

    static class SaveRequest implements ProcessingTask.Request {
        File destinationFile;
        boolean exit;
        boolean flatten;
        ImagePreset preset;
        Bitmap previewImage;
        int quality;
        Uri selectedUri;
        float sizeFactor;
        Uri sourceUri;

        SaveRequest() {
        }
    }

    static class UpdateBitmap implements ProcessingTask.Update {
        Bitmap bitmap;

        UpdateBitmap() {
        }
    }

    static class UpdateProgress implements ProcessingTask.Update {
        int current;
        int max;

        UpdateProgress() {
        }
    }

    static class UpdatePreviewSaved implements ProcessingTask.Update {
        boolean exit;
        Uri uri;

        UpdatePreviewSaved() {
        }
    }

    static class URIResult implements ProcessingTask.Result {
        boolean exit;
        Uri uri;

        URIResult() {
        }
    }

    public ImageSavingTask(ProcessingService processingService) {
        this.mProcessingService = processingService;
    }

    public void saveImage(Uri uri, Uri uri2, File file, ImagePreset imagePreset, Bitmap bitmap, boolean z, int i, float f, boolean z2) {
        SaveRequest saveRequest = new SaveRequest();
        saveRequest.sourceUri = uri;
        saveRequest.selectedUri = uri2;
        saveRequest.destinationFile = file;
        saveRequest.preset = imagePreset;
        saveRequest.flatten = z;
        saveRequest.quality = i;
        saveRequest.sizeFactor = f;
        saveRequest.previewImage = bitmap;
        saveRequest.exit = z2;
        postRequest(saveRequest);
    }

    @Override
    public ProcessingTask.Result doInBackground(ProcessingTask.Request request) throws Throwable {
        SaveRequest saveRequest = (SaveRequest) request;
        Uri uri = saveRequest.sourceUri;
        Uri uri2 = saveRequest.selectedUri;
        File file = saveRequest.destinationFile;
        Bitmap bitmap = saveRequest.previewImage;
        ImagePreset imagePreset = saveRequest.preset;
        boolean z = saveRequest.flatten;
        final boolean z2 = saveRequest.exit;
        UpdateBitmap updateBitmap = new UpdateBitmap();
        if (MasterImage.getImage().getOriginalBounds() == null) {
            URIResult uRIResult = new URIResult();
            uRIResult.uri = null;
            uRIResult.exit = false;
            return uRIResult;
        }
        MasterImage.getImage().backupBounds();
        updateBitmap.bitmap = createNotificationBitmap(bitmap, uri, imagePreset);
        postUpdate(updateBitmap);
        Uri uriProcessAndSaveImage = new SaveImage(this.mProcessingService, uri, uri2, file, bitmap, new SaveImage.Callback() {
            @Override
            public void onProgress(int i, int i2) {
                UpdateProgress updateProgress = new UpdateProgress();
                updateProgress.max = i;
                updateProgress.current = i2;
                ImageSavingTask.this.postUpdate(updateProgress);
            }
        }).processAndSaveImage(imagePreset, z, saveRequest.quality, saveRequest.sizeFactor, saveRequest.exit);
        URIResult uRIResult2 = new URIResult();
        uRIResult2.uri = uriProcessAndSaveImage;
        uRIResult2.exit = saveRequest.exit;
        return uRIResult2;
    }

    @Override
    public void onResult(ProcessingTask.Result result) {
        URIResult uRIResult = (URIResult) result;
        this.mProcessingService.completeSaveImage(uRIResult.uri, uRIResult.exit);
    }

    @Override
    public void onUpdate(ProcessingTask.Update update) {
        if (update instanceof UpdatePreviewSaved) {
            this.mProcessingService.completePreviewSaveImage(update.uri, update.exit);
        }
        if (update instanceof UpdateBitmap) {
            this.mProcessingService.updateNotificationWithBitmap(update.bitmap);
        }
        if (update instanceof UpdateProgress) {
            this.mProcessingService.updateProgress(update.max, update.current);
        }
    }

    private Bitmap createNotificationBitmap(Bitmap bitmap, Uri uri, ImagePreset imagePreset) throws Throwable {
        int dimensionPixelSize = Resources.getSystem().getDimensionPixelSize(R.dimen.notification_large_icon_width);
        if (bitmap == null) {
            return new CachingPipeline(FiltersManager.getManager(), "Thumb").renderFinalImage(ImageLoader.loadConstrainedBitmap(uri, getContext(), dimensionPixelSize, null, true), imagePreset);
        }
        return Bitmap.createScaledBitmap(bitmap, dimensionPixelSize, dimensionPixelSize, true);
    }
}
