package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.pipeline.ProcessingTask;

public class RenderingRequestTask extends ProcessingTask {
    private boolean mPipelineIsOn = false;
    private CachingPipeline mPreviewPipeline;

    public void setPreviewScaleFactor(float f) {
        this.mPreviewPipeline.setPreviewScaleFactor(f);
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

    public RenderingRequestTask() {
        this.mPreviewPipeline = null;
        this.mPreviewPipeline = new CachingPipeline(FiltersManager.getManager(), "Normal");
    }

    public void setOriginal(Bitmap bitmap) {
        this.mPreviewPipeline.setOriginal(bitmap);
        this.mPipelineIsOn = true;
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
        if (renderingRequest.getType() == 2) {
            this.mPreviewPipeline.renderGeometry(renderingRequest);
        } else if (renderingRequest.getType() == 1) {
            this.mPreviewPipeline.renderFilters(renderingRequest);
        } else {
            this.mPreviewPipeline.render(renderingRequest);
        }
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
}
