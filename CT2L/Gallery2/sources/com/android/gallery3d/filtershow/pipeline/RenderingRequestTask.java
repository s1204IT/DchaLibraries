package com.android.gallery3d.filtershow.pipeline;

import android.graphics.Bitmap;
import com.android.gallery3d.filtershow.filters.FiltersManager;
import com.android.gallery3d.filtershow.pipeline.ProcessingTask;

public class RenderingRequestTask extends ProcessingTask {
    private boolean mPipelineIsOn = false;
    private CachingPipeline mPreviewPipeline;

    public void setPreviewScaleFactor(float previewScale) {
        this.mPreviewPipeline.setPreviewScaleFactor(previewScale);
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
        if (request.getType() == 2) {
            this.mPreviewPipeline.renderGeometry(request);
        } else if (request.getType() == 1) {
            this.mPreviewPipeline.renderFilters(request);
        } else {
            this.mPreviewPipeline.render(request);
        }
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
}
