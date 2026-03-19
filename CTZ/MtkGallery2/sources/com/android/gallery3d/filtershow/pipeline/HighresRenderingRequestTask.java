package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.pipeline.ProcessingTask;

public class HighresRenderingRequestTask extends ProcessingTask {
    private CachingPipeline mHighresPreviewPipeline;
    private boolean mPipelineIsOn = false;

    public void setHighresPreviewScaleFactor(float f) {
        this.mHighresPreviewPipeline.setHighResPreviewScaleFactor(f);
    }

    public void setPreviewScaleFactor(float f) {
        this.mHighresPreviewPipeline.setPreviewScaleFactor(f);
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

    public void setOriginalBitmapHighres(Bitmap bitmap) {
        this.mPipelineIsOn = true;
    }

    public void stop() {
        this.mHighresPreviewPipeline.stop();
    }

    public void postRenderingRequest(RenderingRequest renderingRequest) {
        if (!this.mPipelineIsOn) {
            return;
        }
        Render render = new Render();
        render.request = renderingRequest;
        postRequest(render);
    }

    @Override
    public ProcessingTask.Result doInBackground(ProcessingTask.Request request) {
        RenderingRequest renderingRequest = ((Render) request).request;
        this.mHighresPreviewPipeline.renderHighres(renderingRequest);
        RenderResult renderResult = new RenderResult();
        renderResult.request = renderingRequest;
        return renderResult;
    }

    @Override
    public void onResult(ProcessingTask.Result result) {
        if (result == null) {
            return;
        }
        ((RenderResult) result).request.markAvailable();
    }

    @Override
    public boolean isDelayedTask() {
        return true;
    }
}
