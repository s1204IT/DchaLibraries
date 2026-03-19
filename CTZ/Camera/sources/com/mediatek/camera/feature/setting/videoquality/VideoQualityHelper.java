package com.mediatek.camera.feature.setting.videoquality;

import android.app.Activity;
import android.media.CamcorderProfile;
import com.mediatek.camera.R;
import com.mediatek.camera.portability.CamcorderProfileEx;

public class VideoQualityHelper {
    static int[] sVideoQualities = {8, 6, 5, 4, 3, 7, 2};
    static int[] sMtkVideoQualities = {123, 111, 110, 109, 108};

    static String getCurrentResolution(int i, String str) {
        CamcorderProfile profile = CamcorderProfileEx.getProfile(i, Integer.parseInt(str));
        return profile.videoFrameWidth + "x" + profile.videoFrameHeight;
    }

    static String getQualityTitle(Activity activity, String str, int i) {
        CamcorderProfile profile = CamcorderProfileEx.getProfile(i, Integer.parseInt(str));
        switch (profile.videoFrameHeight * profile.videoFrameWidth) {
            case 25344:
                return activity.getResources().getString(R.string.quality_qcif);
            case 76800:
                return activity.getResources().getString(R.string.quality_qvga);
            case 101376:
                return activity.getResources().getString(R.string.quality_cif);
            case 307200:
            case 345600:
                return activity.getResources().getString(R.string.quality_vga);
            case 921600:
                return activity.getResources().getString(R.string.quality_hd);
            case 2073600:
            case 2088960:
                return activity.getResources().getString(R.string.quality_fhd);
            case 3686400:
                return activity.getResources().getString(R.string.quality_2k);
            case 8294400:
            case 8355840:
                return activity.getResources().getString(R.string.quality_4k);
            default:
                return "";
        }
    }
}
