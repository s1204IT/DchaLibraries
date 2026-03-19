package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.imageshow.MasterImage;
import com.android.gallery3d.filtershow.pipeline.ProcessingTask;

public class UpdatePreviewTask extends ProcessingTask {
    private boolean mHasUnhandledPreviewRequest = false;
    private boolean mPipelineIsOn = false;
    private CachingPipeline mPreviewPipeline;

    public UpdatePreviewTask() {
        this.mPreviewPipeline = null;
        this.mPreviewPipeline = new CachingPipeline(FiltersManager.getPreviewManager(), "Preview");
    }

    public void setOriginal(Bitmap bitmap) {
        this.mPreviewPipeline.setOriginal(bitmap);
        this.mPipelineIsOn = true;
    }

    public void updatePreview() {
        if (!this.mPipelineIsOn) {
            return;
        }
        this.mHasUnhandledPreviewRequest = true;
        if (postRequest(null)) {
            this.mHasUnhandledPreviewRequest = false;
        }
    }

    @Override
    public boolean isPriorityTask() {
        return true;
    }

    @Override
    public ProcessingTask.Result doInBackground(ProcessingTask.Request request) {
        SharedBuffer previewBuffer = MasterImage.getImage().getPreviewBuffer();
        ImagePreset imagePresetDequeuePreset = MasterImage.getImage().getPreviewPreset().dequeuePreset();
        if (imagePresetDequeuePreset != null) {
            this.mPreviewPipeline.compute(previewBuffer, imagePresetDequeuePreset, 0);
            if (previewBuffer.getProducer() == null) {
                return null;
            }
            previewBuffer.getProducer().setPreset(imagePresetDequeuePreset);
            previewBuffer.getProducer().sync();
            previewBuffer.swapProducer();
        }
        return null;
    }

    @Override
    public void onResult(ProcessingTask.Result result) {
        MasterImage.getImage().notifyObservers();
        if (this.mHasUnhandledPreviewRequest) {
            updatePreview();
        }
    }
}
