package com.android.gallery3d.filtershow.pipeline;

import android.R;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import com.android.gallery3d.filtershow.cache.ImageLoader;
import com.android.gallery3d.filtershow.filters.FiltersManager;
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

    public ImageSavingTask(ProcessingService service) {
        this.mProcessingService = service;
    }

    public void saveImage(Uri sourceUri, Uri selectedUri, File destinationFile, ImagePreset preset, Bitmap previewImage, boolean flatten, int quality, float sizeFactor, boolean exit) {
        SaveRequest request = new SaveRequest();
        request.sourceUri = sourceUri;
        request.selectedUri = selectedUri;
        request.destinationFile = destinationFile;
        request.preset = preset;
        request.flatten = flatten;
        request.quality = quality;
        request.sizeFactor = sizeFactor;
        request.previewImage = previewImage;
        request.exit = exit;
        postRequest(request);
    }

    @Override
    public ProcessingTask.Result doInBackground(ProcessingTask.Request message) {
        SaveRequest request = (SaveRequest) message;
        Uri sourceUri = request.sourceUri;
        Uri selectedUri = request.selectedUri;
        File destinationFile = request.destinationFile;
        Bitmap previewImage = request.previewImage;
        ImagePreset preset = request.preset;
        boolean flatten = request.flatten;
        final boolean exit = request.exit;
        UpdateBitmap updateBitmap = new UpdateBitmap();
        updateBitmap.bitmap = createNotificationBitmap(previewImage, sourceUri, preset);
        postUpdate(updateBitmap);
        SaveImage saveImage = new SaveImage(this.mProcessingService, sourceUri, selectedUri, destinationFile, previewImage, new SaveImage.Callback() {
            @Override
            public void onPreviewSaved(Uri uri) {
                UpdatePreviewSaved previewSaved = new UpdatePreviewSaved();
                previewSaved.uri = uri;
                previewSaved.exit = exit;
                ImageSavingTask.this.postUpdate(previewSaved);
            }

            @Override
            public void onProgress(int max, int current) {
                UpdateProgress updateProgress = new UpdateProgress();
                updateProgress.max = max;
                updateProgress.current = current;
                ImageSavingTask.this.postUpdate(updateProgress);
            }
        });
        Uri uri = saveImage.processAndSaveImage(preset, flatten, request.quality, request.sizeFactor, request.exit);
        URIResult result = new URIResult();
        result.uri = uri;
        result.exit = request.exit;
        return result;
    }

    @Override
    public void onResult(ProcessingTask.Result message) {
        URIResult result = (URIResult) message;
        this.mProcessingService.completeSaveImage(result.uri, result.exit);
    }

    @Override
    public void onUpdate(ProcessingTask.Update message) {
        if (message instanceof UpdatePreviewSaved) {
            Uri uri = ((UpdatePreviewSaved) message).uri;
            boolean exit = ((UpdatePreviewSaved) message).exit;
            this.mProcessingService.completePreviewSaveImage(uri, exit);
        }
        if (message instanceof UpdateBitmap) {
            Bitmap bitmap = ((UpdateBitmap) message).bitmap;
            this.mProcessingService.updateNotificationWithBitmap(bitmap);
        }
        if (message instanceof UpdateProgress) {
            UpdateProgress progress = (UpdateProgress) message;
            this.mProcessingService.updateProgress(progress.max, progress.current);
        }
    }

    private Bitmap createNotificationBitmap(Bitmap preview, Uri sourceUri, ImagePreset preset) {
        int notificationBitmapSize = Resources.getSystem().getDimensionPixelSize(R.dimen.notification_large_icon_width);
        if (preview != null) {
            return Bitmap.createScaledBitmap(preview, notificationBitmapSize, notificationBitmapSize, true);
        }
        Bitmap bitmap = ImageLoader.loadConstrainedBitmap(sourceUri, getContext(), notificationBitmapSize, null, true);
        CachingPipeline pipeline = new CachingPipeline(FiltersManager.getManager(), "Thumb");
        return pipeline.renderFinalImage(bitmap, preset);
    }
}
