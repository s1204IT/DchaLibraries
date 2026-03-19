package com.android.gallery3d.filtershow.pipeline;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.renderscript.Allocation;
import android.renderscript.RenderScript;

public interface PipelineInterface {
    Allocation getInPixelsAllocation();

    String getName();

    Allocation getOutPixelsAllocation();

    RenderScript getRSContext();

    Resources getResources();

    boolean prepareRenderscriptAllocations(Bitmap bitmap);
}
