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
        if (this.mPipelineIsOn) {
            this.mHasUnhandledPreviewRequest = true;
            if (postRequest(null)) {
                this.mHasUnhandledPreviewRequest = false;
            }
        }
    }

    @Override
    public boolean isPriorityTask() {
        return true;
    }

    @Override
    public ProcessingTask.Result doInBackground(ProcessingTask.Request message) {
        SharedBuffer buffer = MasterImage.getImage().getPreviewBuffer();
        SharedPreset preset = MasterImage.getImage().getPreviewPreset();
        ImagePreset renderingPreset = preset.dequeuePreset();
        if (renderingPreset != null) {
            this.mPreviewPipeline.compute(buffer, renderingPreset, 0);
            buffer.getProducer().setPreset(renderingPreset);
            buffer.getProducer().sync();
            buffer.swapProducer();
            return null;
        }
        return null;
    }

    @Override
    public void onResult(ProcessingTask.Result message) {
        MasterImage.getImage().notifyObservers();
        if (this.mHasUnhandledPreviewRequest) {
            updatePreview();
        }
    }
}
