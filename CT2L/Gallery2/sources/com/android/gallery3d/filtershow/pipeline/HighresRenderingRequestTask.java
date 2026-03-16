package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.pipeline.ProcessingTask;

public class HighresRenderingRequestTask extends ProcessingTask {
    private CachingPipeline mHighresPreviewPipeline;
    private boolean mPipelineIsOn = false;

    public void setHighresPreviewScaleFactor(float highResPreviewScale) {
        this.mHighresPreviewPipeline.setHighResPreviewScaleFactor(highResPreviewScale);
    }

    public void setPreviewScaleFactor(float previewScale) {
        this.mHighresPreviewPipeline.setPreviewScaleFactor(previewScale);
    }

    static class Render implements ProcessingTask.Request {
        RenderingRequest request;

        Render() {
        }
    }

    static class RenderResult implements ProcessingTask.Result {
        RenderingRequest request;

        RenderResult() {
        }
    }

    public HighresRenderingRequestTask() {
        this.mHighresPreviewPipeline = null;
        this.mHighresPreviewPipeline = new CachingPipeline(FiltersManager.getHighresManager(), "Highres");
    }

    public void setOriginal(Bitmap bitmap) {
        this.mHighresPreviewPipeline.setOriginal(bitmap);
    }

    public void setOriginalBitmapHighres(Bitmap originalHires) {
        this.mPipelineIsOn = true;
    }

    public void stop() {
        this.mHighresPreviewPipeline.stop();
    }

    public void postRenderingRequest(RenderingRequest request) {
        if (this.mPipelineIsOn) {
            Render render = new Render();
            render.request = request;
            postRequest(render);
        }
    }

    @Override
    public ProcessingTask.Result doInBackground(ProcessingTask.Request message) {
        RenderingRequest request = ((Render) message).request;
        this.mHighresPreviewPipeline.renderHighres(request);
        RenderResult result = new RenderResult();
        result.request = request;
        return result;
    }

    @Override
    public void onResult(ProcessingTask.Result message) {
        if (message != null) {
            RenderingRequest request = ((RenderResult) message).request;
            request.markAvailable();
        }
    }

    @Override
    public boolean isDelayedTask() {
        return true;
    }
}
