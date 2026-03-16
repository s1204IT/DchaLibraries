package com.android.camera.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.media.CamcorderProfile;
import android.util.SparseArray;
import com.android.camera.debug.Log;
import com.android.camera.util.ApiHelper;
import com.android.camera.util.Callback;
import com.android.camera2.R;
import com.android.ex.camera2.portability.CameraDeviceInfo;
import com.android.ex.camera2.portability.CameraSettings;
import com.android.ex.camera2.portability.Size;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class SettingsUtil {
    private static final boolean DEBUG = false;
    private static final float MEDIUM_RELATIVE_PICTURE_SIZE = 0.5f;
    private static final String SIZE_LARGE = "large";
    private static final String SIZE_MEDIUM = "medium";
    private static final String SIZE_SMALL = "small";
    private static final float SMALL_RELATIVE_PICTURE_SIZE = 0.25f;
    private static final Log.Tag TAG = new Log.Tag("SettingsUtil");
    public static int[] sVideoQualities = {8, 6, 5, 4, 3, 7, 2};
    public static SparseArray<SelectedPictureSizes> sCachedSelectedPictureSizes = new SparseArray<>(2);
    public static SparseArray<SelectedVideoQualities> sCachedSelectedVideoQualities = new SparseArray<>(2);
    public static final CameraDeviceSelector CAMERA_FACING_BACK = new CameraDeviceSelector() {
        @Override
        public boolean useCamera(CameraDeviceInfo.Characteristics info) {
            return info.isFacingBack();
        }
    };
    public static final CameraDeviceSelector CAMERA_FACING_FRONT = new CameraDeviceSelector() {
        @Override
        public boolean useCamera(CameraDeviceInfo.Characteristics info) {
            return info.isFacingFront();
        }
    };

    public interface CameraDeviceSelector {
        boolean useCamera(CameraDeviceInfo.Characteristics characteristics);
    }

    public static int getMaxVideoDuration(Context context) {
        try {
            int duration = context.getResources().getInteger(R.integer.max_video_recording_length);
            return duration;
        } catch (Resources.NotFoundException e) {
            return 0;
        }
    }

    public static class SelectedPictureSizes {
        public Size large;
        public Size medium;
        public Size small;

        public Size getFromSetting(String sizeSetting, List<Size> supportedSizes) {
            if (SettingsUtil.SIZE_LARGE.equals(sizeSetting)) {
                return this.large;
            }
            if (SettingsUtil.SIZE_MEDIUM.equals(sizeSetting)) {
                return this.medium;
            }
            if (SettingsUtil.SIZE_SMALL.equals(sizeSetting)) {
                return this.small;
            }
            if (sizeSetting != null && sizeSetting.split("x").length == 2) {
                Size desiredSize = SettingsUtil.sizeFromString(sizeSetting);
                if (supportedSizes.contains(desiredSize)) {
                    return desiredSize;
                }
            }
            return this.large;
        }

        public String toString() {
            return "SelectedPictureSizes: " + this.large + ", " + this.medium + ", " + this.small;
        }
    }

    public static class SelectedVideoQualities {
        public int large = -1;
        public int medium = -1;
        public int small = -1;

        public int getFromSetting(String sizeSetting) {
            if (!SettingsUtil.SIZE_SMALL.equals(sizeSetting) && !SettingsUtil.SIZE_MEDIUM.equals(sizeSetting)) {
                sizeSetting = SettingsUtil.SIZE_LARGE;
            }
            if (SettingsUtil.SIZE_LARGE.equals(sizeSetting)) {
                return this.large;
            }
            if (SettingsUtil.SIZE_MEDIUM.equals(sizeSetting)) {
                return this.medium;
            }
            return this.small;
        }
    }

    public static void setCameraPictureSize(String sizeSetting, List<Size> supported, CameraSettings settings, int cameraId) {
        Size selectedSize = getCameraPictureSize(sizeSetting, supported, cameraId);
        Log.d(TAG, "Selected " + sizeSetting + " resolution: " + selectedSize.width() + "x" + selectedSize.height());
        settings.setPhotoSize(selectedSize);
    }

    public static Size getPhotoSize(String sizeSetting, List<Size> supported, int cameraId) {
        if (ResolutionUtil.NEXUS_5_LARGE_16_BY_9.equals(sizeSetting)) {
            return ResolutionUtil.NEXUS_5_LARGE_16_BY_9_SIZE;
        }
        return getCameraPictureSize(sizeSetting, supported, cameraId);
    }

    private static Size getCameraPictureSize(String sizeSetting, List<Size> supported, int cameraId) {
        return getSelectedCameraPictureSizes(supported, cameraId).getFromSetting(sizeSetting, supported);
    }

    static SelectedPictureSizes getSelectedCameraPictureSizes(List<Size> supported, int cameraId) {
        List<Size> supportedCopy = new LinkedList<>(supported);
        if (sCachedSelectedPictureSizes.get(cameraId) != null) {
            return sCachedSelectedPictureSizes.get(cameraId);
        }
        if (supportedCopy == null) {
            return null;
        }
        SelectedPictureSizes selectedSizes = new SelectedPictureSizes();
        Collections.sort(supportedCopy, new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                int leftArea = lhs.width() * lhs.height();
                int rightArea = rhs.width() * rhs.height();
                return rightArea - leftArea;
            }
        });
        selectedSizes.large = supportedCopy.remove(0);
        float targetAspectRatio = selectedSizes.large.width() / selectedSizes.large.height();
        ArrayList<Size> aspectRatioMatches = new ArrayList<>();
        for (Size size : supportedCopy) {
            float aspectRatio = size.width() / size.height();
            if (Math.abs(aspectRatio - targetAspectRatio) < 0.01d) {
                aspectRatioMatches.add(size);
            }
        }
        List<Size> searchList = aspectRatioMatches.size() >= 2 ? aspectRatioMatches : supportedCopy;
        if (searchList.isEmpty()) {
            Log.w(TAG, "Only one supported resolution.");
            selectedSizes.medium = selectedSizes.large;
            selectedSizes.small = selectedSizes.large;
        } else if (searchList.size() == 1) {
            Log.w(TAG, "Only two supported resolutions.");
            selectedSizes.medium = searchList.get(0);
            selectedSizes.small = searchList.get(0);
        } else if (searchList.size() == 2) {
            Log.w(TAG, "Exactly three supported resolutions.");
            selectedSizes.medium = searchList.get(0);
            selectedSizes.small = searchList.get(1);
        } else {
            int largePixelCount = selectedSizes.large.width() * selectedSizes.large.height();
            int mediumTargetPixelCount = (int) (largePixelCount * MEDIUM_RELATIVE_PICTURE_SIZE);
            int smallTargetPixelCount = (int) (largePixelCount * SMALL_RELATIVE_PICTURE_SIZE);
            int mediumSizeIndex = findClosestSize(searchList, mediumTargetPixelCount);
            int smallSizeIndex = findClosestSize(searchList, smallTargetPixelCount);
            if (searchList.get(mediumSizeIndex).equals(searchList.get(smallSizeIndex))) {
                if (smallSizeIndex < searchList.size() - 1) {
                    smallSizeIndex++;
                } else {
                    mediumSizeIndex--;
                }
            }
            selectedSizes.medium = searchList.get(mediumSizeIndex);
            selectedSizes.small = searchList.get(smallSizeIndex);
        }
        sCachedSelectedPictureSizes.put(cameraId, selectedSizes);
        return selectedSizes;
    }

    public static int getVideoQuality(String qualitySetting, int cameraId) {
        return getSelectedVideoQualities(cameraId).getFromSetting(qualitySetting);
    }

    static SelectedVideoQualities getSelectedVideoQualities(int cameraId) {
        if (sCachedSelectedVideoQualities.get(cameraId) != null) {
            return sCachedSelectedVideoQualities.get(cameraId);
        }
        int largeIndex = getNextSupportedVideoQualityIndex(cameraId, -1);
        int mediumIndex = getNextSupportedVideoQualityIndex(cameraId, largeIndex);
        int smallIndex = getNextSupportedVideoQualityIndex(cameraId, mediumIndex);
        SelectedVideoQualities selectedQualities = new SelectedVideoQualities();
        selectedQualities.large = sVideoQualities[largeIndex];
        selectedQualities.medium = sVideoQualities[mediumIndex];
        selectedQualities.small = sVideoQualities[smallIndex];
        sCachedSelectedVideoQualities.put(cameraId, selectedQualities);
        return selectedQualities;
    }

    private static int getNextSupportedVideoQualityIndex(int cameraId, int start) {
        for (int i = start + 1; i < sVideoQualities.length; i++) {
            if (isVideoQualitySupported(sVideoQualities[i]) && CamcorderProfile.hasProfile(cameraId, sVideoQualities[i])) {
                return i;
            }
        }
        if (start < 0 || start >= sVideoQualities.length) {
            throw new IllegalArgumentException("Could not find supported video qualities.");
        }
        return start;
    }

    private static boolean isVideoQualitySupported(int videoQuality) {
        if (ApiHelper.isLOrHigher() || videoQuality != 8) {
            return true;
        }
        return DEBUG;
    }

    private static int findClosestSize(List<Size> sortedSizes, int targetPixelCount) {
        int closestMatchIndex = 0;
        int closestMatchPixelCountDiff = Integer.MAX_VALUE;
        for (int i = 0; i < sortedSizes.size(); i++) {
            Size size = sortedSizes.get(i);
            int pixelCountDiff = Math.abs((size.width() * size.height()) - targetPixelCount);
            if (pixelCountDiff < closestMatchPixelCountDiff) {
                closestMatchIndex = i;
                closestMatchPixelCountDiff = pixelCountDiff;
            }
        }
        return closestMatchIndex;
    }

    public static String sizeToSetting(Size size) {
        return Integer.valueOf(size.width()).toString() + "x" + Integer.valueOf(size.height()).toString();
    }

    public static Size sizeFromString(String sizeSetting) {
        String[] parts = sizeSetting.split("x");
        if (parts.length == 2) {
            return new Size(Integer.valueOf(parts[0]).intValue(), Integer.valueOf(parts[1]).intValue());
        }
        return null;
    }

    public static AlertDialog.Builder getFirstTimeLocationAlertBuilder(AlertDialog.Builder builder, Callback<Boolean> callback) {
        if (callback == null) {
            return null;
        }
        getLocationAlertBuilder(builder, callback).setMessage(R.string.remember_location_prompt);
        return builder;
    }

    public static AlertDialog.Builder getLocationAlertBuilder(AlertDialog.Builder builder, final Callback<Boolean> callback) {
        if (callback == null) {
            return null;
        }
        builder.setTitle(R.string.remember_location_title).setPositiveButton(R.string.remember_location_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int arg1) {
                callback.onCallback(true);
            }
        }).setNegativeButton(R.string.remember_location_no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int arg1) {
                callback.onCallback(Boolean.valueOf(SettingsUtil.DEBUG));
            }
        });
        return builder;
    }

    public static int getCameraId(CameraDeviceInfo info, CameraDeviceSelector chooser) {
        int numCameras = info.getNumberOfCameras();
        for (int i = 0; i < numCameras; i++) {
            CameraDeviceInfo.Characteristics props = info.getCharacteristics(i);
            if (props != null && chooser.useCamera(props)) {
                return i;
            }
        }
        return -1;
    }
}
