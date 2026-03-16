package com.android.gallery3d.data;

import android.content.Context;
import android.net.Uri;
import com.android.gallery3d.util.LightCycleHelper;
import com.android.gallery3d.util.ThreadPool;

public class PanoramaMetadataJob implements ThreadPool.Job<LightCycleHelper.PanoramaMetadata> {
    Context mContext;
    Uri mUri;

    public PanoramaMetadataJob(Context context, Uri uri) {
        this.mContext = context;
        this.mUri = uri;
    }

    @Override
    public LightCycleHelper.PanoramaMetadata run(ThreadPool.JobContext jc) {
        return LightCycleHelper.getPanoramaMetadata(this.mContext, this.mUri);
    }
}
