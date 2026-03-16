package com.android.gallery3d.filtershow.pipeline;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.RenderScript;

public interface PipelineInterface {
    Allocation getInPixelsAllocation();

    String getName();

    Allocation getOutPixelsAllocation();

    RenderScript getRSContext();

    Resources getResources();

    boolean prepareRenderscriptAllocations(Bitmap bitmap);
}
