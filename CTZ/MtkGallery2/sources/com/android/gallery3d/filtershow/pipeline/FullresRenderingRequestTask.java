package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.pipeline.ProcessingTask;

public class FullresRenderingRequestTask extends ProcessingTask {
    private CachingPipeline mFullresPipeline;
    private boolean mPipelineIsOn = false;

    public void setPreviewScaleFactor(float f) {
        this.mFullresPipeline.setPreviewScaleFactor(f);
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

    public FullresRenderingRequestTask() {
        this.mFullresPipeline = null;
        this.mFullresPipeline = new CachingPipeline(FiltersManager.getHighresManager(), "Fullres");
    }

    public void setOriginal(Bitmap bitmap) {
        this.mFullresPipeline.setOriginal(bitmap);
        this.mPipelineIsOn = true;
    }

    public void stop() {
        this.mFullresPipeline.stop();
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
        this.mFullresPipeline.render(renderingRequest);
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
