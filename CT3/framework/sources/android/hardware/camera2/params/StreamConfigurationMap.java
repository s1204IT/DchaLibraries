package android.hardware.camera2.params;

import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.utils.HashCodeHelpers;
import android.hardware.camera2.utils.SurfaceUtils;
import android.media.ImageReader;
import android.media.MediaCodec;
import android.media.MediaRecorder;
import android.renderscript.Allocation;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceHolder;
import com.android.internal.util.Preconditions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;

public final class StreamConfigurationMap {
    private static final long DURATION_20FPS_NS = 50000000;
    private static final int DURATION_MIN_FRAME = 0;
    private static final int DURATION_STALL = 1;
    private static final int HAL_DATASPACE_DEPTH = 4096;
    private static final int HAL_DATASPACE_RANGE_SHIFT = 27;
    private static final int HAL_DATASPACE_STANDARD_SHIFT = 16;
    private static final int HAL_DATASPACE_TRANSFER_SHIFT = 22;
    private static final int HAL_DATASPACE_UNKNOWN = 0;
    private static final int HAL_DATASPACE_V0_JFIF = 146931712;
    private static final int HAL_PIXEL_FORMAT_BLOB = 33;
    private static final int HAL_PIXEL_FORMAT_IMPLEMENTATION_DEFINED = 34;
    private static final int HAL_PIXEL_FORMAT_RAW10 = 37;
    private static final int HAL_PIXEL_FORMAT_RAW12 = 38;
    private static final int HAL_PIXEL_FORMAT_RAW16 = 32;
    private static final int HAL_PIXEL_FORMAT_RAW_OPAQUE = 36;
    private static final int HAL_PIXEL_FORMAT_Y16 = 540422489;
    private static final int HAL_PIXEL_FORMAT_YCbCr_420_888 = 35;
    private static final String TAG = "StreamConfigurationMap";
    private final StreamConfiguration[] mConfigurations;
    private final StreamConfiguration[] mDepthConfigurations;
    private final StreamConfigurationDuration[] mDepthMinFrameDurations;
    private final StreamConfigurationDuration[] mDepthStallDurations;
    private final HighSpeedVideoConfiguration[] mHighSpeedVideoConfigurations;
    private final ReprocessFormatsMap mInputOutputFormatsMap;
    private final boolean mListHighResolution;
    private final StreamConfigurationDuration[] mMinFrameDurations;
    private final StreamConfigurationDuration[] mStallDurations;
    private final SparseIntArray mOutputFormats = new SparseIntArray();
    private final SparseIntArray mHighResOutputFormats = new SparseIntArray();
    private final SparseIntArray mAllOutputFormats = new SparseIntArray();
    private final SparseIntArray mInputFormats = new SparseIntArray();
    private final SparseIntArray mDepthOutputFormats = new SparseIntArray();
    private final HashMap<Size, Integer> mHighSpeedVideoSizeMap = new HashMap<>();
    private final HashMap<Range<Integer>, Integer> mHighSpeedVideoFpsRangeMap = new HashMap<>();

    public StreamConfigurationMap(StreamConfiguration[] configurations, StreamConfigurationDuration[] minFrameDurations, StreamConfigurationDuration[] stallDurations, StreamConfiguration[] depthConfigurations, StreamConfigurationDuration[] depthMinFrameDurations, StreamConfigurationDuration[] depthStallDurations, HighSpeedVideoConfiguration[] highSpeedVideoConfigurations, ReprocessFormatsMap inputOutputFormatsMap, boolean listHighResolution) {
        SparseIntArray map;
        if (configurations == null) {
            Preconditions.checkArrayElementsNotNull(depthConfigurations, "depthConfigurations");
            this.mConfigurations = new StreamConfiguration[0];
            this.mMinFrameDurations = new StreamConfigurationDuration[0];
            this.mStallDurations = new StreamConfigurationDuration[0];
        } else {
            this.mConfigurations = (StreamConfiguration[]) Preconditions.checkArrayElementsNotNull(configurations, "configurations");
            this.mMinFrameDurations = (StreamConfigurationDuration[]) Preconditions.checkArrayElementsNotNull(minFrameDurations, "minFrameDurations");
            this.mStallDurations = (StreamConfigurationDuration[]) Preconditions.checkArrayElementsNotNull(stallDurations, "stallDurations");
        }
        this.mListHighResolution = listHighResolution;
        if (depthConfigurations == null) {
            this.mDepthConfigurations = new StreamConfiguration[0];
            this.mDepthMinFrameDurations = new StreamConfigurationDuration[0];
            this.mDepthStallDurations = new StreamConfigurationDuration[0];
        } else {
            this.mDepthConfigurations = (StreamConfiguration[]) Preconditions.checkArrayElementsNotNull(depthConfigurations, "depthConfigurations");
            this.mDepthMinFrameDurations = (StreamConfigurationDuration[]) Preconditions.checkArrayElementsNotNull(depthMinFrameDurations, "depthMinFrameDurations");
            this.mDepthStallDurations = (StreamConfigurationDuration[]) Preconditions.checkArrayElementsNotNull(depthStallDurations, "depthStallDurations");
        }
        if (highSpeedVideoConfigurations == null) {
            this.mHighSpeedVideoConfigurations = new HighSpeedVideoConfiguration[0];
        } else {
            this.mHighSpeedVideoConfigurations = (HighSpeedVideoConfiguration[]) Preconditions.checkArrayElementsNotNull(highSpeedVideoConfigurations, "highSpeedVideoConfigurations");
        }
        StreamConfiguration[] streamConfigurationArr = this.mConfigurations;
        int i = 0;
        int length = streamConfigurationArr.length;
        while (true) {
            int i2 = i;
            if (i2 >= length) {
                break;
            }
            StreamConfiguration config = streamConfigurationArr[i2];
            int fmt = config.getFormat();
            if (config.isOutput()) {
                this.mAllOutputFormats.put(fmt, this.mAllOutputFormats.get(fmt) + 1);
                long duration = 0;
                if (this.mListHighResolution) {
                    StreamConfigurationDuration[] streamConfigurationDurationArr = this.mMinFrameDurations;
                    int i3 = 0;
                    int length2 = streamConfigurationDurationArr.length;
                    while (true) {
                        if (i3 >= length2) {
                            break;
                        }
                        StreamConfigurationDuration configurationDuration = streamConfigurationDurationArr[i3];
                        if (configurationDuration.getFormat() != fmt || configurationDuration.getWidth() != config.getSize().getWidth() || configurationDuration.getHeight() != config.getSize().getHeight()) {
                            i3++;
                        } else {
                            duration = configurationDuration.getDuration();
                            break;
                        }
                    }
                }
                map = duration <= DURATION_20FPS_NS ? this.mOutputFormats : this.mHighResOutputFormats;
            } else {
                map = this.mInputFormats;
            }
            map.put(fmt, map.get(fmt) + 1);
            i = i2 + 1;
        }
        for (StreamConfiguration config2 : this.mDepthConfigurations) {
            if (config2.isOutput()) {
                this.mDepthOutputFormats.put(config2.getFormat(), this.mDepthOutputFormats.get(config2.getFormat()) + 1);
            }
        }
        if (configurations != null && this.mOutputFormats.indexOfKey(34) < 0) {
            throw new AssertionError("At least one stream configuration for IMPLEMENTATION_DEFINED must exist");
        }
        for (HighSpeedVideoConfiguration config3 : this.mHighSpeedVideoConfigurations) {
            Size size = config3.getSize();
            Range<Integer> fpsRange = config3.getFpsRange();
            Integer fpsRangeCount = this.mHighSpeedVideoSizeMap.get(size);
            this.mHighSpeedVideoSizeMap.put(size, Integer.valueOf((fpsRangeCount == null ? 0 : fpsRangeCount).intValue() + 1));
            Integer sizeCount = this.mHighSpeedVideoFpsRangeMap.get(fpsRange);
            if (sizeCount == null) {
                sizeCount = 0;
            }
            this.mHighSpeedVideoFpsRangeMap.put(fpsRange, Integer.valueOf(sizeCount.intValue() + 1));
        }
        this.mInputOutputFormatsMap = inputOutputFormatsMap;
    }

    public final int[] getOutputFormats() {
        return getPublicFormats(true);
    }

    public final int[] getValidOutputFormatsForInput(int inputFormat) {
        if (this.mInputOutputFormatsMap == null) {
            return new int[0];
        }
        return this.mInputOutputFormatsMap.getOutputs(inputFormat);
    }

    public final int[] getInputFormats() {
        return getPublicFormats(false);
    }

    public Size[] getInputSizes(int format) {
        return getPublicFormatSizes(format, false, false);
    }

    public boolean isOutputSupportedFor(int format) {
        checkArgumentFormat(format);
        int internalFormat = imageFormatToInternal(format);
        int dataspace = imageFormatToDataspace(format);
        return dataspace == 4096 ? this.mDepthOutputFormats.indexOfKey(internalFormat) >= 0 : getFormatsMap(true).indexOfKey(internalFormat) >= 0;
    }

    public static <T> boolean isOutputSupportedFor(Class<T> klass) {
        Preconditions.checkNotNull(klass, "klass must not be null");
        return klass == ImageReader.class || klass == MediaRecorder.class || klass == MediaCodec.class || klass == Allocation.class || klass == SurfaceHolder.class || klass == SurfaceTexture.class;
    }

    public boolean isOutputSupportedFor(Surface surface) {
        Preconditions.checkNotNull(surface, "surface must not be null");
        Size surfaceSize = SurfaceUtils.getSurfaceSize(surface);
        int surfaceFormat = SurfaceUtils.getSurfaceFormat(surface);
        int surfaceDataspace = SurfaceUtils.getSurfaceDataspace(surface);
        boolean isFlexible = SurfaceUtils.isFlexibleConsumer(surface);
        if (surfaceFormat >= 1 && surfaceFormat <= 5) {
            surfaceFormat = 34;
        }
        StreamConfiguration[] configs = surfaceDataspace != 4096 ? this.mConfigurations : this.mDepthConfigurations;
        for (StreamConfiguration config : configs) {
            if (config.getFormat() == surfaceFormat && config.isOutput()) {
                if (config.getSize().equals(surfaceSize)) {
                    return true;
                }
                if (isFlexible && config.getSize().getWidth() <= 1920) {
                    return true;
                }
            }
        }
        return false;
    }

    public <T> Size[] getOutputSizes(Class<T> klass) {
        if (!isOutputSupportedFor(klass)) {
            return null;
        }
        return getInternalFormatSizes(34, 0, true, false);
    }

    public Size[] getOutputSizes(int format) {
        return getPublicFormatSizes(format, true, false);
    }

    public Size[] getHighSpeedVideoSizes() {
        Set<Size> keySet = this.mHighSpeedVideoSizeMap.keySet();
        return (Size[]) keySet.toArray(new Size[keySet.size()]);
    }

    public Range<Integer>[] getHighSpeedVideoFpsRangesFor(Size size) {
        int i;
        int i2 = 0;
        Integer fpsRangeCount = this.mHighSpeedVideoSizeMap.get(size);
        if (fpsRangeCount == null || fpsRangeCount.intValue() == 0) {
            throw new IllegalArgumentException(String.format("Size %s does not support high speed video recording", size));
        }
        Range<Integer>[] fpsRanges = new Range[fpsRangeCount.intValue()];
        HighSpeedVideoConfiguration[] highSpeedVideoConfigurationArr = this.mHighSpeedVideoConfigurations;
        int length = highSpeedVideoConfigurationArr.length;
        int i3 = 0;
        while (i2 < length) {
            HighSpeedVideoConfiguration config = highSpeedVideoConfigurationArr[i2];
            if (size.equals(config.getSize())) {
                i = i3 + 1;
                fpsRanges[i3] = config.getFpsRange();
            } else {
                i = i3;
            }
            i2++;
            i3 = i;
        }
        return fpsRanges;
    }

    public Range<Integer>[] getHighSpeedVideoFpsRanges() {
        Set<Range<Integer>> keySet = this.mHighSpeedVideoFpsRangeMap.keySet();
        return (Range[]) keySet.toArray(new Range[keySet.size()]);
    }

    public Size[] getHighSpeedVideoSizesFor(Range<Integer> fpsRange) {
        int i;
        int i2 = 0;
        Integer sizeCount = this.mHighSpeedVideoFpsRangeMap.get(fpsRange);
        if (sizeCount == null || sizeCount.intValue() == 0) {
            throw new IllegalArgumentException(String.format("FpsRange %s does not support high speed video recording", fpsRange));
        }
        Size[] sizes = new Size[sizeCount.intValue()];
        HighSpeedVideoConfiguration[] highSpeedVideoConfigurationArr = this.mHighSpeedVideoConfigurations;
        int length = highSpeedVideoConfigurationArr.length;
        int i3 = 0;
        while (i2 < length) {
            HighSpeedVideoConfiguration config = highSpeedVideoConfigurationArr[i2];
            if (fpsRange.equals(config.getFpsRange())) {
                i = i3 + 1;
                sizes[i3] = config.getSize();
            } else {
                i = i3;
            }
            i2++;
            i3 = i;
        }
        return sizes;
    }

    public Size[] getHighResolutionOutputSizes(int format) {
        if (this.mListHighResolution) {
            return getPublicFormatSizes(format, true, true);
        }
        return null;
    }

    public long getOutputMinFrameDuration(int format, Size size) {
        Preconditions.checkNotNull(size, "size must not be null");
        checkArgumentFormatSupported(format, true);
        return getInternalFormatDuration(imageFormatToInternal(format), imageFormatToDataspace(format), size, 0);
    }

    public <T> long getOutputMinFrameDuration(Class<T> klass, Size size) {
        if (!isOutputSupportedFor(klass)) {
            throw new IllegalArgumentException("klass was not supported");
        }
        return getInternalFormatDuration(34, 0, size, 0);
    }

    public long getOutputStallDuration(int format, Size size) {
        checkArgumentFormatSupported(format, true);
        return getInternalFormatDuration(imageFormatToInternal(format), imageFormatToDataspace(format), size, 1);
    }

    public <T> long getOutputStallDuration(Class<T> klass, Size size) {
        if (!isOutputSupportedFor(klass)) {
            throw new IllegalArgumentException("klass was not supported");
        }
        return getInternalFormatDuration(34, 0, size, 1);
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
        if (Arrays.equals(this.mConfigurations, other.mConfigurations) && Arrays.equals(this.mMinFrameDurations, other.mMinFrameDurations) && Arrays.equals(this.mStallDurations, other.mStallDurations) && Arrays.equals(this.mDepthConfigurations, other.mDepthConfigurations)) {
            return Arrays.equals(this.mHighSpeedVideoConfigurations, other.mHighSpeedVideoConfigurations);
        }
        return false;
    }

    public int hashCode() {
        return HashCodeHelpers.hashCodeGeneric(this.mConfigurations, this.mMinFrameDurations, this.mStallDurations, this.mDepthConfigurations, this.mHighSpeedVideoConfigurations);
    }

    private int checkArgumentFormatSupported(int format, boolean output) {
        checkArgumentFormat(format);
        int internalFormat = imageFormatToInternal(format);
        int internalDataspace = imageFormatToDataspace(format);
        if (output) {
            if (internalDataspace == 4096) {
                if (this.mDepthOutputFormats.indexOfKey(internalFormat) >= 0) {
                    return format;
                }
            } else if (this.mAllOutputFormats.indexOfKey(internalFormat) >= 0) {
                return format;
            }
        } else if (this.mInputFormats.indexOfKey(internalFormat) >= 0) {
            return format;
        }
        throw new IllegalArgumentException(String.format("format %x is not supported by this stream configuration map", Integer.valueOf(format)));
    }

    static int checkArgumentFormatInternal(int format) {
        switch (format) {
            case 33:
            case 34:
            case 36:
            case 540422489:
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
            case 256:
                throw new IllegalArgumentException("ImageFormat.JPEG is an unknown internal format");
            default:
                return format;
        }
    }

    static int depthFormatToPublic(int format) {
        switch (format) {
            case 33:
                return 257;
            case 34:
                throw new IllegalArgumentException("IMPLEMENTATION_DEFINED must not leak to public API");
            case 256:
                throw new IllegalArgumentException("ImageFormat.JPEG is an unknown internal format");
            case 540422489:
                return ImageFormat.DEPTH16;
            default:
                throw new IllegalArgumentException("Unknown DATASPACE_DEPTH format " + format);
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
            case 256:
            case 257:
                return 33;
            case ImageFormat.DEPTH16:
                return 540422489;
            default:
                return format;
        }
    }

    static int imageFormatToDataspace(int format) {
        switch (format) {
            case 256:
                return HAL_DATASPACE_V0_JFIF;
            case 257:
            case ImageFormat.DEPTH16:
                return 4096;
            default:
                return 0;
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

    private Size[] getPublicFormatSizes(int format, boolean output, boolean highRes) {
        try {
            checkArgumentFormatSupported(format, output);
            int internalFormat = imageFormatToInternal(format);
            int dataspace = imageFormatToDataspace(format);
            return getInternalFormatSizes(internalFormat, dataspace, output, highRes);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Size[] getInternalFormatSizes(int format, int dataspace, boolean output, boolean highRes) {
        SparseIntArray formatsMap;
        int sizeIndex;
        if (dataspace == 4096 && highRes) {
            return new Size[0];
        }
        if (!output) {
            formatsMap = this.mInputFormats;
        } else if (dataspace == 4096) {
            formatsMap = this.mDepthOutputFormats;
        } else {
            formatsMap = highRes ? this.mHighResOutputFormats : this.mOutputFormats;
        }
        int sizesCount = formatsMap.get(format);
        if (((!output || dataspace == 4096) && sizesCount == 0) || (output && dataspace != 4096 && this.mAllOutputFormats.get(format) == 0)) {
            throw new IllegalArgumentException("format not available");
        }
        Size[] sizes = new Size[sizesCount];
        int sizeIndex2 = 0;
        StreamConfiguration[] configurations = dataspace == 4096 ? this.mDepthConfigurations : this.mConfigurations;
        StreamConfigurationDuration[] minFrameDurations = dataspace == 4096 ? this.mDepthMinFrameDurations : this.mMinFrameDurations;
        int i = 0;
        int length = configurations.length;
        while (true) {
            int i2 = i;
            sizeIndex = sizeIndex2;
            if (i2 >= length) {
                break;
            }
            StreamConfiguration config = configurations[i2];
            int fmt = config.getFormat();
            if (fmt != format || config.isOutput() != output) {
                sizeIndex2 = sizeIndex;
            } else if (output && this.mListHighResolution) {
                long duration = 0;
                int i3 = 0;
                while (true) {
                    if (i3 >= minFrameDurations.length) {
                        break;
                    }
                    StreamConfigurationDuration d = minFrameDurations[i3];
                    if (d.getFormat() != fmt || d.getWidth() != config.getSize().getWidth() || d.getHeight() != config.getSize().getHeight()) {
                        i3++;
                    } else {
                        duration = d.getDuration();
                        break;
                    }
                }
                if (dataspace != 4096) {
                    if (highRes != (duration > DURATION_20FPS_NS)) {
                        sizeIndex2 = sizeIndex;
                    }
                }
            } else {
                sizeIndex2 = sizeIndex + 1;
                sizes[sizeIndex] = config.getSize();
            }
            i = i2 + 1;
        }
        if (sizeIndex != sizesCount) {
            throw new AssertionError("Too few sizes (expected " + sizesCount + ", actual " + sizeIndex + ")");
        }
        return sizes;
    }

    private int[] getPublicFormats(boolean output) {
        int[] formats = new int[getPublicFormatCount(output)];
        int i = 0;
        SparseIntArray map = getFormatsMap(output);
        int j = 0;
        while (j < map.size()) {
            int format = map.keyAt(j);
            formats[i] = imageFormatToPublic(format);
            j++;
            i++;
        }
        if (output) {
            int j2 = 0;
            while (j2 < this.mDepthOutputFormats.size()) {
                formats[i] = depthFormatToPublic(this.mDepthOutputFormats.keyAt(j2));
                j2++;
                i++;
            }
        }
        if (formats.length != i) {
            throw new AssertionError("Too few formats " + i + ", expected " + formats.length);
        }
        return formats;
    }

    private SparseIntArray getFormatsMap(boolean output) {
        return output ? this.mAllOutputFormats : this.mInputFormats;
    }

    private long getInternalFormatDuration(int format, int dataspace, Size size, int duration) {
        if (!isSupportedInternalConfiguration(format, dataspace, size)) {
            throw new IllegalArgumentException("size was not supported");
        }
        StreamConfigurationDuration[] durations = getDurations(duration, dataspace);
        for (StreamConfigurationDuration configurationDuration : durations) {
            if (configurationDuration.getFormat() == format && configurationDuration.getWidth() == size.getWidth() && configurationDuration.getHeight() == size.getHeight()) {
                return configurationDuration.getDuration();
            }
        }
        return 0L;
    }

    private StreamConfigurationDuration[] getDurations(int duration, int dataspace) {
        switch (duration) {
            case 0:
                return dataspace == 4096 ? this.mDepthMinFrameDurations : this.mMinFrameDurations;
            case 1:
                return dataspace == 4096 ? this.mDepthStallDurations : this.mStallDurations;
            default:
                throw new IllegalArgumentException("duration was invalid");
        }
    }

    private int getPublicFormatCount(boolean output) {
        SparseIntArray formatsMap = getFormatsMap(output);
        int size = formatsMap.size();
        if (output) {
            return size + this.mDepthOutputFormats.size();
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

    private boolean isSupportedInternalConfiguration(int format, int dataspace, Size size) {
        StreamConfiguration[] configurations = dataspace == 4096 ? this.mDepthConfigurations : this.mConfigurations;
        for (int i = 0; i < configurations.length; i++) {
            if (configurations[i].getFormat() == format && configurations[i].getSize().equals(size)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("StreamConfiguration(");
        appendOutputsString(sb);
        sb.append(", ");
        appendHighResOutputsString(sb);
        sb.append(", ");
        appendInputsString(sb);
        sb.append(", ");
        appendValidOutputFormatsForInputString(sb);
        sb.append(", ");
        appendHighSpeedVideoConfigurationsString(sb);
        sb.append(")");
        return sb.toString();
    }

    private void appendOutputsString(StringBuilder sb) {
        sb.append("Outputs(");
        int[] formats = getOutputFormats();
        for (int format : formats) {
            Size[] sizes = getOutputSizes(format);
            for (Size size : sizes) {
                long minFrameDuration = getOutputMinFrameDuration(format, size);
                long stallDuration = getOutputStallDuration(format, size);
                sb.append(String.format("[w:%d, h:%d, format:%s(%d), min_duration:%d, stall:%d], ", Integer.valueOf(size.getWidth()), Integer.valueOf(size.getHeight()), formatToString(format), Integer.valueOf(format), Long.valueOf(minFrameDuration), Long.valueOf(stallDuration)));
            }
        }
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
    }

    private void appendHighResOutputsString(StringBuilder sb) {
        sb.append("HighResolutionOutputs(");
        int[] formats = getOutputFormats();
        for (int format : formats) {
            Size[] sizes = getHighResolutionOutputSizes(format);
            if (sizes != null) {
                for (Size size : sizes) {
                    long minFrameDuration = getOutputMinFrameDuration(format, size);
                    long stallDuration = getOutputStallDuration(format, size);
                    sb.append(String.format("[w:%d, h:%d, format:%s(%d), min_duration:%d, stall:%d], ", Integer.valueOf(size.getWidth()), Integer.valueOf(size.getHeight()), formatToString(format), Integer.valueOf(format), Long.valueOf(minFrameDuration), Long.valueOf(stallDuration)));
                }
            }
        }
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
    }

    private void appendInputsString(StringBuilder sb) {
        sb.append("Inputs(");
        int[] formats = getInputFormats();
        for (int format : formats) {
            Size[] sizes = getInputSizes(format);
            for (Size size : sizes) {
                sb.append(String.format("[w:%d, h:%d, format:%s(%d)], ", Integer.valueOf(size.getWidth()), Integer.valueOf(size.getHeight()), formatToString(format), Integer.valueOf(format)));
            }
        }
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
    }

    private void appendValidOutputFormatsForInputString(StringBuilder sb) {
        sb.append("ValidOutputFormatsForInput(");
        int[] inputFormats = getInputFormats();
        for (int inputFormat : inputFormats) {
            sb.append(String.format("[in:%s(%d), out:", formatToString(inputFormat), Integer.valueOf(inputFormat)));
            int[] outputFormats = getValidOutputFormatsForInput(inputFormat);
            for (int i = 0; i < outputFormats.length; i++) {
                sb.append(String.format("%s(%d)", formatToString(outputFormats[i]), Integer.valueOf(outputFormats[i])));
                if (i < outputFormats.length - 1) {
                    sb.append(", ");
                }
            }
            sb.append("], ");
        }
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
    }

    private void appendHighSpeedVideoConfigurationsString(StringBuilder sb) {
        sb.append("HighSpeedVideoConfigurations(");
        Size[] sizes = getHighSpeedVideoSizes();
        for (Size size : sizes) {
            Range<Integer>[] ranges = getHighSpeedVideoFpsRangesFor(size);
            for (Range<Integer> range : ranges) {
                sb.append(String.format("[w:%d, h:%d, min_fps:%d, max_fps:%d], ", Integer.valueOf(size.getWidth()), Integer.valueOf(size.getHeight()), range.getLower(), range.getUpper()));
            }
        }
        if (sb.charAt(sb.length() - 1) == ' ') {
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append(")");
    }

    private String formatToString(int format) {
        switch (format) {
            case 1:
                return "RGBA_8888";
            case 2:
                return "RGBX_8888";
            case 3:
                return "RGB_888";
            case 4:
                return "RGB_565";
            case 16:
                return "NV16";
            case 17:
                return "NV21";
            case 20:
                return "YUY2";
            case 32:
                return "RAW_SENSOR";
            case 34:
                return "PRIVATE";
            case 35:
                return "YUV_420_888";
            case 36:
                return "RAW_PRIVATE";
            case 37:
                return "RAW10";
            case 256:
                return "JPEG";
            case 257:
                return "DEPTH_POINT_CLOUD";
            case ImageFormat.Y8:
                return "Y8";
            case 540422489:
                return "Y16";
            case ImageFormat.YV12:
                return "YV12";
            case ImageFormat.DEPTH16:
                return "DEPTH16";
            default:
                return "UNKNOWN";
        }
    }
}
