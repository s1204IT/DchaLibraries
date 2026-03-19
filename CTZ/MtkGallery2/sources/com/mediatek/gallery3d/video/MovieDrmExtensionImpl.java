package com.mediatek.gallery3d.video;

import android.content.Context;
import android.content.DialogInterface;
import android.drm.DrmManagerClient;
import android.net.Uri;
import com.mediatek.gallery3d.util.Log;
import com.mediatek.gallery3d.video.IMovieDrmExtension;
import com.mediatek.omadrm.OmaDrmUtils;

public class MovieDrmExtensionImpl extends DefaultMovieDrmExtension {
    private static final boolean LOG = true;
    private static final String TAG = "VP_MovieDrmExt";
    private static DrmManagerClient sDrmClient;

    @Override
    public boolean handleDrmFile(Context context, IMovieItem iMovieItem, final IMovieDrmExtension.IMovieDrmCallback iMovieDrmCallback) {
        DrmManagerClient drmManagerClientEnsureDrmClient = ensureDrmClient(context);
        if (!iMovieItem.isDrm()) {
            return false;
        }
        OmaDrmUtils.showConsumerDialog(context, drmManagerClientEnsureDrmClient, iMovieItem.getUri(), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (i == -1) {
                    if (iMovieDrmCallback != null) {
                        iMovieDrmCallback.onContinue();
                    }
                } else if (i == -2 && iMovieDrmCallback != null) {
                    iMovieDrmCallback.onStop();
                }
            }
        });
        return LOG;
    }

    @Override
    public boolean canShare(Context context, IMovieItem iMovieItem) {
        int iCheckRightsStatus;
        Uri uri = iMovieItem.getUri();
        Log.v(TAG, "canShare(" + uri + ")");
        DrmManagerClient drmManagerClientEnsureDrmClient = ensureDrmClient(context);
        boolean zIsDrm = iMovieItem.isDrm();
        boolean z = LOG;
        if (zIsDrm) {
            try {
                iCheckRightsStatus = drmManagerClientEnsureDrmClient.checkRightsStatus(uri, 3);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "canShare() : raise exception, we assume it has no rights to be shared");
                iCheckRightsStatus = 1;
            }
            if (iCheckRightsStatus != 0) {
                z = false;
            }
            Log.v(TAG, "canShare(" + uri + "), rightsStatus=" + iCheckRightsStatus);
        }
        Log.v(TAG, "canShare(" + uri + "), share=" + z);
        return z;
    }

    private static DrmManagerClient ensureDrmClient(Context context) {
        if (sDrmClient == null) {
            synchronized (DrmManagerClient.class) {
                if (sDrmClient == null) {
                    sDrmClient = new DrmManagerClient(context.getApplicationContext());
                }
            }
        }
        return sDrmClient;
    }
}
