package android.hardware.camera2.utils;

import android.hardware.camera2.legacy.LegacyCameraDevice;
import android.hardware.camera2.legacy.LegacyExceptionUtils;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class SurfaceUtils {
    public static boolean isSurfaceForPreview(Surface surface) {
        return LegacyCameraDevice.isPreviewConsumer(surface);
    }

    public static boolean isSurfaceForHwVideoEncoder(Surface surface) {
        return LegacyCameraDevice.isVideoEncoderConsumer(surface);
    }

    public static long getSurfaceId(Surface surface) {
        try {
            return LegacyCameraDevice.getSurfaceId(surface);
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            return 0L;
        }
    }

    public static Size getSurfaceSize(Surface surface) {
        try {
            return LegacyCameraDevice.getSurfaceSize(surface);
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            throw new IllegalArgumentException("Surface was abandoned", e);
        }
    }

    public static int getSurfaceFormat(Surface surface) {
        try {
            return LegacyCameraDevice.detectSurfaceType(surface);
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            throw new IllegalArgumentException("Surface was abandoned", e);
        }
    }

    public static int getSurfaceDataspace(Surface surface) {
        try {
            return LegacyCameraDevice.detectSurfaceDataspace(surface);
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            throw new IllegalArgumentException("Surface was abandoned", e);
        }
    }

    public static boolean isFlexibleConsumer(Surface surface) {
        return LegacyCameraDevice.isFlexibleConsumer(surface);
    }

    private static void checkHighSpeedSurfaceFormat(Surface surface) {
        int surfaceFormat = getSurfaceFormat(surface);
        if (surfaceFormat != 34) {
            throw new IllegalArgumentException("Surface format(" + surfaceFormat + ") is not for preview or hardware video encoding!");
        }
    }

    public static void checkConstrainedHighSpeedSurfaces(Collection<Surface> collection, Range<Integer> range, StreamConfigurationMap streamConfigurationMap) {
        List listAsList;
        if (collection == null || collection.size() == 0 || collection.size() > 2) {
            throw new IllegalArgumentException("Output target surface list must not be null and the size must be 1 or 2");
        }
        if (range == null) {
            listAsList = Arrays.asList(streamConfigurationMap.getHighSpeedVideoSizes());
        } else {
            Range<Integer>[] highSpeedVideoFpsRanges = streamConfigurationMap.getHighSpeedVideoFpsRanges();
            if (!Arrays.asList(highSpeedVideoFpsRanges).contains(range)) {
                throw new IllegalArgumentException("Fps range " + range.toString() + " in the request is not a supported high speed fps range " + Arrays.toString(highSpeedVideoFpsRanges));
            }
            listAsList = Arrays.asList(streamConfigurationMap.getHighSpeedVideoSizesFor(range));
        }
        for (Surface surface : collection) {
            checkHighSpeedSurfaceFormat(surface);
            Size surfaceSize = getSurfaceSize(surface);
            if (!listAsList.contains(surfaceSize)) {
                throw new IllegalArgumentException("Surface size " + surfaceSize.toString() + " is not part of the high speed supported size list " + Arrays.toString(listAsList.toArray()));
            }
            if (!isSurfaceForPreview(surface) && !isSurfaceForHwVideoEncoder(surface)) {
                throw new IllegalArgumentException("This output surface is neither preview nor hardware video encoding surface");
            }
            if (isSurfaceForPreview(surface) && isSurfaceForHwVideoEncoder(surface)) {
                throw new IllegalArgumentException("This output surface can not be both preview and hardware video encoding surface");
            }
        }
        if (collection.size() == 2) {
            Iterator<Surface> it = collection.iterator();
            if (isSurfaceForPreview(it.next()) == isSurfaceForPreview(it.next())) {
                throw new IllegalArgumentException("The 2 output surfaces must have different type");
            }
        }
    }
}
