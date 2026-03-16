package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.pipeline.ProcessingTask;

public class FullresRenderingRequestTask extends ProcessingTask {
    private CachingPipeline mFullresPipeline;
    private boolean mPipelineIsOn = false;

    public void setPreviewScaleFactor(float previewScale) {
        this.mFullresPipeline.setPreviewScaleFactor(previewScale);
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
        this.mFullresPipeline.render(request);
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
