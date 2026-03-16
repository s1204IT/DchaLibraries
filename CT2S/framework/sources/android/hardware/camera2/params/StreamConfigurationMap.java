package android.hardware.camera2.params;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.legacy.LegacyCameraDevice;
import android.hardware.camera2.legacy.LegacyExceptionUtils;
import android.hardware.camera2.utils.HashCodeHelpers;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.renderscript.Allocation;
import android.util.Range;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.android.internal.util.Preconditions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public final class StreamConfigurationMap {
    private static final int DURATION_MIN_FRAME = 0;
    private static final int DURATION_STALL = 1;
    private static final int HAL_PIXEL_FORMAT_BLOB = 33;
    private static final int HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED = 34;
    private static final int HAL_PIXEL_FORMAT_RAW_OPAQUE = 36;
    private static final String TAG = "StreamConfigurationMap";
    private final StreamConfiguration[] mConfigurations;
    private final HighSpeedVideoConfiguration[] mHighSpeedVideoConfigurations;
    private final StreamConfigurationDuration[] mMinFrameDurations;
    private final StreamConfigurationDuration[] mStallDurations;
    private final HashMap<Integer, Integer> mOutputFormats = new HashMap<>();
    private final HashMap<Integer, Integer> mInputFormats = new HashMap<>();
    private final HashMap<Size, Integer> mHighSpeedVideoSizeMap = new HashMap<>();
    private final HashMap<Range<Integer>, Integer> mHighSpeedVideoFpsRangeMap = new HashMap<>();

    public StreamConfigurationMap(StreamConfiguration[] configurations, StreamConfigurationDuration[] minFrameDurations, StreamConfigurationDuration[] stallDurations, HighSpeedVideoConfiguration[] highSpeedVideoConfigurations) {
        this.mConfigurations = (StreamConfiguration[]) Preconditions.checkArrayElementsNotNull(configurations, "configurations");
        this.mMinFrameDurations = (StreamConfigurationDuration[]) Preconditions.checkArrayElementsNotNull(minFrameDurations, "minFrameDurations");
        this.mStallDurations = (StreamConfigurationDuration[]) Preconditions.checkArrayElementsNotNull(stallDurations, "stallDurations");
        if (highSpeedVideoConfigurations == null) {
            this.mHighSpeedVideoConfigurations = new HighSpeedVideoConfiguration[0];
        } else {
            this.mHighSpeedVideoConfigurations = (HighSpeedVideoConfiguration[]) Preconditions.checkArrayElementsNotNull(highSpeedVideoConfigurations, "highSpeedVideoConfigurations");
        }
        for (StreamConfiguration config : configurations) {
            HashMap<Integer, Integer> map = config.isOutput() ? this.mOutputFormats : this.mInputFormats;
            Integer count = map.get(Integer.valueOf(config.getFormat()));
            if (count == null) {
                count = 0;
            }
            map.put(Integer.valueOf(config.getFormat()), Integer.valueOf(count.intValue() + 1));
        }
        if (!this.mOutputFormats.containsKey(34)) {
            throw new AssertionError("At least one stream configuration for IMPLEMENTATION_DEFINED must exist");
        }
        HighSpeedVideoConfiguration[] arr$ = this.mHighSpeedVideoConfigurations;
        for (HighSpeedVideoConfiguration config2 : arr$) {
            Size size = config2.getSize();
            Range<Integer> fpsRange = config2.getFpsRange();
            Integer fpsRangeCount = this.mHighSpeedVideoSizeMap.get(size);
            this.mHighSpeedVideoSizeMap.put(size, Integer.valueOf((fpsRangeCount == null ? 0 : fpsRangeCount).intValue() + 1));
            Integer sizeCount = this.mHighSpeedVideoFpsRangeMap.get(fpsRange);
            if (sizeCount == null) {
                sizeCount = 0;
            }
            this.mHighSpeedVideoFpsRangeMap.put(fpsRange, Integer.valueOf(sizeCount.intValue() + 1));
        }
    }

    public final int[] getOutputFormats() {
        return getPublicFormats(true);
    }

    public final int[] getInputFormats() {
        return getPublicFormats(false);
    }

    public Size[] getInputSizes(int format) {
        return getPublicFormatSizes(format, false);
    }

    public boolean isOutputSupportedFor(int format) {
        checkArgumentFormat(format);
        return getFormatsMap(true).containsKey(Integer.valueOf(imageFormatToInternal(format)));
    }

    public static <T> boolean isOutputSupportedFor(Class<T> klass) {
        Preconditions.checkNotNull(klass, "klass must not be null");
        return klass == ImageReader.class || klass == MediaRecorder.class || klass == MediaCodec.class || klass == Allocation.class || klass == SurfaceHolder.class || klass == SurfaceTexture.class;
    }

    public boolean isOutputSupportedFor(Surface surface) {
        Preconditions.checkNotNull(surface, "surface must not be null");
        try {
            Size surfaceSize = LegacyCameraDevice.getSurfaceSize(surface);
            int surfaceFormat = LegacyCameraDevice.detectSurfaceType(surface);
            boolean isFlexible = LegacyCameraDevice.isFlexibleConsumer(surface);
            if (surfaceFormat >= 1 && surfaceFormat <= 5) {
                surfaceFormat = 34;
            }
            StreamConfiguration[] arr$ = this.mConfigurations;
            for (StreamConfiguration config : arr$) {
                if (config.getFormat() == surfaceFormat && config.isOutput()) {
                    if (config.getSize().equals(surfaceSize)) {
                        return true;
                    }
                    if (isFlexible && config.getSize().getWidth() <= 1080) {
                        return true;
                    }
                }
            }
            return false;
        } catch (LegacyExceptionUtils.BufferQueueAbandonedException e) {
            throw new IllegalArgumentException("Abandoned surface", e);
        }
    }

    public <T> Size[] getOutputSizes(Class<T> klass) {
        if (ImageReader.class.isAssignableFrom(klass)) {
            return new Size[0];
        }
        if (!isOutputSupportedFor(klass)) {
            return null;
        }
        return getInternalFormatSizes(34, true);
    }

    public Size[] getOutputSizes(int format) {
        return getPublicFormatSizes(format, true);
    }

    public Size[] getHighSpeedVideoSizes() {
        Set<Size> keySet = this.mHighSpeedVideoSizeMap.keySet();
        return (Size[]) keySet.toArray(new Size[keySet.size()]);
    }

    public Range<Integer>[] getHighSpeedVideoFpsRangesFor(Size size) {
        int i;
        Integer fpsRangeCount = this.mHighSpeedVideoSizeMap.get(size);
        if (fpsRangeCount == null || fpsRangeCount.intValue() == 0) {
            throw new IllegalArgumentException(String.format("Size %s does not support high speed video recording", size));
        }
        Range<Integer>[] fpsRanges = new Range[fpsRangeCount.intValue()];
        HighSpeedVideoConfiguration[] arr$ = this.mHighSpeedVideoConfigurations;
        int len$ = arr$.length;
        int i$ = 0;
        int i2 = 0;
        while (i$ < len$) {
            HighSpeedVideoConfiguration config = arr$[i$];
            if (size.equals(config.getSize())) {
                i = i2 + 1;
                fpsRanges[i2] = config.getFpsRange();
            } else {
                i = i2;
            }
            i$++;
            i2 = i;
        }
        return fpsRanges;
    }

    public Range<Integer>[] getHighSpeedVideoFpsRanges() {
        Set<Range<Integer>> keySet = this.mHighSpeedVideoFpsRangeMap.keySet();
        return (Range[]) keySet.toArray(new Range[keySet.size()]);
    }

    public Size[] getHighSpeedVideoSizesFor(Range<Integer> fpsRange) {
        int i;
        Integer sizeCount = this.mHighSpeedVideoFpsRangeMap.get(fpsRange);
        if (sizeCount == null || sizeCount.intValue() == 0) {
            throw new IllegalArgumentException(String.format("FpsRange %s does not support high speed video recording", fpsRange));
        }
        Size[] sizes = new Size[sizeCount.intValue()];
        HighSpeedVideoConfiguration[] arr$ = this.mHighSpeedVideoConfigurations;
        int len$ = arr$.length;
        int i$ = 0;
        int i2 = 0;
        while (i$ < len$) {
            HighSpeedVideoConfiguration config = arr$[i$];
            if (fpsRange.equals(config.getFpsRange())) {
                i = i2 + 1;
                sizes[i2] = config.getSize();
            } else {
                i = i2;
            }
            i$++;
            i2 = i;
        }
        return sizes;
    }

    public long getOutputMinFrameDuration(int format, Size size) {
        Preconditions.checkNotNull(size, "size must not be null");
        checkArgumentFormatSupported(format, true);
        return getInternalFormatDuration(imageFormatToInternal(format), size, 0);
    }

    public <T> long getOutputMinFrameDuration(Class<T> klass, Size size) {
        if (!isOutputSupportedFor(klass)) {
            throw new IllegalArgumentException("klass was not supported");
        }
        return getInternalFormatDuration(34, size, 0);
    }

    public long getOutputStallDuration(int format, Size size) {
        checkArgumentFormatSupported(format, true);
        return getInternalFormatDuration(imageFormatToInternal(format), size, 1);
    }

    public <T> long getOutputStallDuration(Class<T> klass, Size size) {
        if (!isOutputSupportedFor(klass)) {
            throw new IllegalArgumentException("klass was not supported");
        }
        return getInternalFormatDuration(34, size, 1);
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StreamConfigurationMap)) {
            return false;
        }
        StreamConfigurationMap other = (StreamConfigurationMap) obj;
        return Arrays.equals(this.mConfigurations, other.mConfigurations) && Arrays.equals(this.mMinFrameDurations, other.mMinFrameDurations) && Arrays.equals(this.mStallDurations, other.mStallDurations) && Arrays.equals(this.mHighSpeedVideoConfigurations, other.mHighSpeedVideoConfigurations);
    }

    public int hashCode() {
        return HashCodeHelpers.hashCode((HighSpeedVideoConfiguration[]) this.mConfigurations, (HighSpeedVideoConfiguration[]) this.mMinFrameDurations, (HighSpeedVideoConfiguration[]) this.mStallDurations, this.mHighSpeedVideoConfigurations);
    }

    private int checkArgumentFormatSupported(int format, boolean output) {
        checkArgumentFormat(format);
        int[] formats = output ? getOutputFormats() : getInputFormats();
        for (int i : formats) {
            if (format == i) {
                return format;
            }
        }
        throw new IllegalArgumentException(String.format("format %x is not supported by this stream configuration map", Integer.valueOf(format)));
    }

    static int checkArgumentFormatInternal(int format) {
        switch (format) {
            case 33:
            case 34:
            case 36:
                return format;
            case 256:
                throw new IllegalArgumentException("ImageFormat.JPEG is an unknown internal format");
            default:
                return checkArgumentFormat(format);
        }
    }

    static int checkArgumentFormat(int format) {
        if (!ImageFormat.isPublicFormat(format) && !PixelFormat.isPublicFormat(format)) {
            throw new IllegalArgumentException(String.format("format 0x%x was not defined in either ImageFormat or PixelFormat", Integer.valueOf(format)));
        }
        return format;
    }

    static int imageFormatToPublic(int format) {
        switch (format) {
            case 33:
                return 256;
            case 34:
                throw new IllegalArgumentException("IMPLEMENTATION_DEFINED must not leak to public API");
            case 256:
                throw new IllegalArgumentException("ImageFormat.JPEG is an unknown internal format");
            default:
                return format;
        }
    }

    static int[] imageFormatToPublic(int[] formats) {
        if (formats == null) {
            return null;
        }
        for (int i = 0; i < formats.length; i++) {
            formats[i] = imageFormatToPublic(formats[i]);
        }
        return formats;
    }

    static int imageFormatToInternal(int format) {
        switch (format) {
            case 34:
                throw new IllegalArgumentException("IMPLEMENTATION_DEFINED is not allowed via public API");
            case 256:
                return 33;
            default:
                return format;
        }
    }

    public static int[] imageFormatToInternal(int[] formats) {
        if (formats == null) {
            return null;
        }
        for (int i = 0; i < formats.length; i++) {
            formats[i] = imageFormatToInternal(formats[i]);
        }
        return formats;
    }

    private Size[] getPublicFormatSizes(int format, boolean output) {
        try {
            checkArgumentFormatSupported(format, output);
            return getInternalFormatSizes(imageFormatToInternal(format), output);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Size[] getInternalFormatSizes(int format, boolean output) {
        int sizeIndex;
        HashMap<Integer, Integer> formatsMap = getFormatsMap(output);
        Integer sizesCount = formatsMap.get(Integer.valueOf(format));
        if (sizesCount == null) {
            throw new IllegalArgumentException("format not available");
        }
        int len = sizesCount.intValue();
        Size[] sizes = new Size[len];
        StreamConfiguration[] arr$ = this.mConfigurations;
        int len$ = arr$.length;
        int i$ = 0;
        int sizeIndex2 = 0;
        while (i$ < len$) {
            StreamConfiguration config = arr$[i$];
            if (config.getFormat() == format && config.isOutput() == output) {
                sizeIndex = sizeIndex2 + 1;
                sizes[sizeIndex2] = config.getSize();
            } else {
                sizeIndex = sizeIndex2;
            }
            i$++;
            sizeIndex2 = sizeIndex;
        }
        if (sizeIndex2 != len) {
            throw new AssertionError("Too few sizes (expected " + len + ", actual " + sizeIndex2 + ")");
        }
        return sizes;
    }

    private int[] getPublicFormats(boolean output) {
        int[] formats = new int[getPublicFormatCount(output)];
        int i = 0;
        Iterator<Integer> it = getFormatsMap(output).keySet().iterator();
        while (it.hasNext()) {
            int format = it.next().intValue();
            if (format != 34 && format != 36) {
                formats[i] = format;
                i++;
            }
        }
        if (formats.length != i) {
            throw new AssertionError("Too few formats " + i + ", expected " + formats.length);
        }
        return imageFormatToPublic(formats);
    }

    private HashMap<Integer, Integer> getFormatsMap(boolean output) {
        return output ? this.mOutputFormats : this.mInputFormats;
    }

    private long getInternalFormatDuration(int format, Size size, int duration) {
        if (!arrayContains(getInternalFormatSizes(format, true), size)) {
            throw new IllegalArgumentException("size was not supported");
        }
        StreamConfigurationDuration[] durations = getDurations(duration);
        for (StreamConfigurationDuration configurationDuration : durations) {
            if (configurationDuration.getFormat() == format && configurationDuration.getWidth() == size.getWidth() && configurationDuration.getHeight() == size.getHeight()) {
                return configurationDuration.getDuration();
            }
        }
        return 0L;
    }

    private StreamConfigurationDuration[] getDurations(int duration) {
        switch (duration) {
            case 0:
                return this.mMinFrameDurations;
            case 1:
                return this.mStallDurations;
            default:
                throw new IllegalArgumentException("duration was invalid");
        }
    }

    private int getPublicFormatCount(boolean output) {
        HashMap<Integer, Integer> formatsMap = getFormatsMap(output);
        int size = formatsMap.size();
        if (formatsMap.containsKey(34)) {
            size--;
        }
        if (formatsMap.containsKey(36)) {
            return size - 1;
        }
        return size;
    }

    private static <T> boolean arrayContains(T[] array, T element) {
        if (array == null) {
            return false;
        }
        for (T el : array) {
            if (Objects.equals(el, element)) {
                return true;
            }
        }
        return false;
    }
}
