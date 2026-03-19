package android.media;

import android.bluetooth.BluetoothHealth;
import android.mtp.MtpConstants;
import android.opengl.GLES20;
import android.os.UserHandle;
import android.os.health.HealthKeys;
import android.telephony.NetworkScanRequest;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.view.SurfaceControl;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.Protocol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MediaCodecInfo {
    private static final int DEFAULT_MAX_SUPPORTED_INSTANCES = 32;
    private static final int ERROR_NONE_SUPPORTED = 4;
    private static final int ERROR_UNRECOGNIZED = 1;
    private static final int ERROR_UNSUPPORTED = 2;
    private static final int MAX_SUPPORTED_INSTANCES_LIMIT = 256;
    private Map<String, CodecCapabilities> mCaps = new HashMap();
    private boolean mIsEncoder;
    private String mName;
    private static final Range<Integer> POSITIVE_INTEGERS = Range.create(1, Integer.MAX_VALUE);
    private static final Range<Long> POSITIVE_LONGS = Range.create(1L, Long.MAX_VALUE);
    private static final Range<Rational> POSITIVE_RATIONALS = Range.create(new Rational(1, Integer.MAX_VALUE), new Rational(Integer.MAX_VALUE, 1));
    private static final Range<Integer> SIZE_RANGE = Range.create(1, 32768);
    private static final Range<Integer> FRAME_RATE_RANGE = Range.create(0, 960);
    private static final Range<Integer> BITRATE_RANGE = Range.create(0, 500000000);

    MediaCodecInfo(String str, boolean z, CodecCapabilities[] codecCapabilitiesArr) {
        this.mName = str;
        this.mIsEncoder = z;
        for (CodecCapabilities codecCapabilities : codecCapabilitiesArr) {
            this.mCaps.put(codecCapabilities.getMimeType(), codecCapabilities);
        }
    }

    public final String getName() {
        return this.mName;
    }

    public final boolean isEncoder() {
        return this.mIsEncoder;
    }

    public final String[] getSupportedTypes() {
        Set<String> setKeySet = this.mCaps.keySet();
        String[] strArr = (String[]) setKeySet.toArray(new String[setKeySet.size()]);
        Arrays.sort(strArr);
        return strArr;
    }

    private static int checkPowerOfTwo(int i, String str) {
        if (((i - 1) & i) != 0) {
            throw new IllegalArgumentException(str);
        }
        return i;
    }

    private static class Feature {
        public boolean mDefault;
        public String mName;
        public int mValue;

        public Feature(String str, int i, boolean z) {
            this.mName = str;
            this.mValue = i;
            this.mDefault = z;
        }
    }

    public static final class CodecCapabilities {
        public static final int COLOR_Format12bitRGB444 = 3;
        public static final int COLOR_Format16bitARGB1555 = 5;
        public static final int COLOR_Format16bitARGB4444 = 4;
        public static final int COLOR_Format16bitBGR565 = 7;
        public static final int COLOR_Format16bitRGB565 = 6;
        public static final int COLOR_Format18BitBGR666 = 41;
        public static final int COLOR_Format18bitARGB1665 = 9;
        public static final int COLOR_Format18bitRGB666 = 8;
        public static final int COLOR_Format19bitARGB1666 = 10;
        public static final int COLOR_Format24BitABGR6666 = 43;
        public static final int COLOR_Format24BitARGB6666 = 42;
        public static final int COLOR_Format24bitARGB1887 = 13;
        public static final int COLOR_Format24bitBGR888 = 12;
        public static final int COLOR_Format24bitRGB888 = 11;
        public static final int COLOR_Format25bitARGB1888 = 14;
        public static final int COLOR_Format32bitABGR8888 = 2130747392;
        public static final int COLOR_Format32bitARGB8888 = 16;
        public static final int COLOR_Format32bitBGRA8888 = 15;
        public static final int COLOR_Format8bitRGB332 = 2;
        public static final int COLOR_FormatCbYCrY = 27;
        public static final int COLOR_FormatCrYCbY = 28;
        public static final int COLOR_FormatL16 = 36;
        public static final int COLOR_FormatL2 = 33;
        public static final int COLOR_FormatL24 = 37;
        public static final int COLOR_FormatL32 = 38;
        public static final int COLOR_FormatL4 = 34;
        public static final int COLOR_FormatL8 = 35;
        public static final int COLOR_FormatMonochrome = 1;
        public static final int COLOR_FormatRGBAFlexible = 2134288520;
        public static final int COLOR_FormatRGBFlexible = 2134292616;
        public static final int COLOR_FormatRawBayer10bit = 31;
        public static final int COLOR_FormatRawBayer8bit = 30;
        public static final int COLOR_FormatRawBayer8bitcompressed = 32;
        public static final int COLOR_FormatSurface = 2130708361;
        public static final int COLOR_FormatYCbYCr = 25;
        public static final int COLOR_FormatYCrYCb = 26;
        public static final int COLOR_FormatYUV411PackedPlanar = 18;
        public static final int COLOR_FormatYUV411Planar = 17;
        public static final int COLOR_FormatYUV420Flexible = 2135033992;
        public static final int COLOR_FormatYUV420PackedPlanar = 20;
        public static final int COLOR_FormatYUV420PackedSemiPlanar = 39;
        public static final int COLOR_FormatYUV420Planar = 19;
        public static final int COLOR_FormatYUV420SemiPlanar = 21;
        public static final int COLOR_FormatYUV422Flexible = 2135042184;
        public static final int COLOR_FormatYUV422PackedPlanar = 23;
        public static final int COLOR_FormatYUV422PackedSemiPlanar = 40;
        public static final int COLOR_FormatYUV422Planar = 22;
        public static final int COLOR_FormatYUV422SemiPlanar = 24;
        public static final int COLOR_FormatYUV444Flexible = 2135181448;
        public static final int COLOR_FormatYUV444Interleaved = 29;
        public static final int COLOR_QCOM_FormatYUV420SemiPlanar = 2141391872;
        public static final int COLOR_TI_FormatYUV420PackedSemiPlanar = 2130706688;
        private static final String TAG = "CodecCapabilities";
        public int[] colorFormats;
        private AudioCapabilities mAudioCaps;
        private MediaFormat mCapabilitiesInfo;
        private MediaFormat mDefaultFormat;
        private EncoderCapabilities mEncoderCaps;
        int mError;
        private int mFlagsRequired;
        private int mFlagsSupported;
        private int mFlagsVerified;
        private int mMaxSupportedInstances;
        private String mMime;
        private VideoCapabilities mVideoCaps;
        public CodecProfileLevel[] profileLevels;
        public static final String FEATURE_AdaptivePlayback = "adaptive-playback";
        public static final String FEATURE_SecurePlayback = "secure-playback";
        public static final String FEATURE_TunneledPlayback = "tunneled-playback";
        public static final String FEATURE_PartialFrame = "partial-frame";
        private static final Feature[] decoderFeatures = {new Feature(FEATURE_AdaptivePlayback, 1, true), new Feature(FEATURE_SecurePlayback, 2, false), new Feature(FEATURE_TunneledPlayback, 4, false), new Feature(FEATURE_PartialFrame, 8, false)};
        public static final String FEATURE_IntraRefresh = "intra-refresh";
        private static final Feature[] encoderFeatures = {new Feature(FEATURE_IntraRefresh, 1, false)};

        public CodecCapabilities() {
        }

        public final boolean isFeatureSupported(String str) {
            return checkFeature(str, this.mFlagsSupported);
        }

        public final boolean isFeatureRequired(String str) {
            return checkFeature(str, this.mFlagsRequired);
        }

        public String[] validFeatures() {
            Feature[] validFeatures = getValidFeatures();
            String[] strArr = new String[validFeatures.length];
            for (int i = 0; i < strArr.length; i++) {
                strArr[i] = validFeatures[i].mName;
            }
            return strArr;
        }

        private Feature[] getValidFeatures() {
            if (!isEncoder()) {
                return decoderFeatures;
            }
            return encoderFeatures;
        }

        private boolean checkFeature(String str, int i) {
            for (Feature feature : getValidFeatures()) {
                if (feature.mName.equals(str)) {
                    return (feature.mValue & i) != 0;
                }
            }
            return false;
        }

        public boolean isRegular() {
            for (Feature feature : getValidFeatures()) {
                if (!feature.mDefault && isFeatureRequired(feature.mName)) {
                    return false;
                }
            }
            return true;
        }

        public final boolean isFormatSupported(MediaFormat mediaFormat) {
            Map<String, Object> map = mediaFormat.getMap();
            String str = (String) map.get(MediaFormat.KEY_MIME);
            if (str != null && !this.mMime.equalsIgnoreCase(str)) {
                return false;
            }
            for (Feature feature : getValidFeatures()) {
                Integer num = (Integer) map.get(MediaFormat.KEY_FEATURE_ + feature.mName);
                if (num != null && ((num.intValue() == 1 && !isFeatureSupported(feature.mName)) || (num.intValue() == 0 && isFeatureRequired(feature.mName)))) {
                    return false;
                }
            }
            Integer num2 = (Integer) map.get(MediaFormat.KEY_PROFILE);
            Integer num3 = (Integer) map.get("level");
            if (num2 != null) {
                if (!supportsProfileLevel(num2.intValue(), num3)) {
                    return false;
                }
                int i = 0;
                for (CodecProfileLevel codecProfileLevel : this.profileLevels) {
                    if (codecProfileLevel.profile == num2.intValue() && codecProfileLevel.level > i) {
                        i = codecProfileLevel.level;
                    }
                }
                CodecCapabilities codecCapabilitiesCreateFromProfileLevel = createFromProfileLevel(this.mMime, num2.intValue(), i);
                HashMap map2 = new HashMap(map);
                map2.remove(MediaFormat.KEY_PROFILE);
                MediaFormat mediaFormat2 = new MediaFormat(map2);
                if (codecCapabilitiesCreateFromProfileLevel != null && !codecCapabilitiesCreateFromProfileLevel.isFormatSupported(mediaFormat2)) {
                    return false;
                }
            }
            if (this.mAudioCaps != null && !this.mAudioCaps.supportsFormat(mediaFormat)) {
                return false;
            }
            if (this.mVideoCaps == null || this.mVideoCaps.supportsFormat(mediaFormat)) {
                return this.mEncoderCaps == null || this.mEncoderCaps.supportsFormat(mediaFormat);
            }
            return false;
        }

        private static boolean supportsBitrate(Range<Integer> range, MediaFormat mediaFormat) {
            Map<String, Object> map = mediaFormat.getMap();
            Integer numValueOf = (Integer) map.get(MediaFormat.KEY_MAX_BIT_RATE);
            Integer num = (Integer) map.get(MediaFormat.KEY_BIT_RATE);
            if (num != null) {
                if (numValueOf != null) {
                    numValueOf = Integer.valueOf(Math.max(num.intValue(), numValueOf.intValue()));
                } else {
                    numValueOf = num;
                }
            }
            if (numValueOf != null && numValueOf.intValue() > 0) {
                return range.contains(numValueOf);
            }
            return true;
        }

        private boolean supportsProfileLevel(int i, Integer num) {
            for (CodecProfileLevel codecProfileLevel : this.profileLevels) {
                if (codecProfileLevel.profile == i) {
                    if (num == null || this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                        return true;
                    }
                    if ((!this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_H263) || codecProfileLevel.level == num.intValue() || codecProfileLevel.level != 16 || num.intValue() <= 1) && (!this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_MPEG4) || codecProfileLevel.level == num.intValue() || codecProfileLevel.level != 4 || num.intValue() <= 1)) {
                        if (this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                            boolean z = (codecProfileLevel.level & 44739242) != 0;
                            if (!((44739242 & num.intValue()) != 0) || z) {
                            }
                        } else if (codecProfileLevel.level >= num.intValue()) {
                            return createFromProfileLevel(this.mMime, i, codecProfileLevel.level) == null || createFromProfileLevel(this.mMime, i, num.intValue()) != null;
                        }
                    }
                }
            }
            return false;
        }

        public MediaFormat getDefaultFormat() {
            return this.mDefaultFormat;
        }

        public String getMimeType() {
            return this.mMime;
        }

        public int getMaxSupportedInstances() {
            return this.mMaxSupportedInstances;
        }

        private boolean isAudio() {
            return this.mAudioCaps != null;
        }

        public AudioCapabilities getAudioCapabilities() {
            return this.mAudioCaps;
        }

        private boolean isEncoder() {
            return this.mEncoderCaps != null;
        }

        public EncoderCapabilities getEncoderCapabilities() {
            return this.mEncoderCaps;
        }

        private boolean isVideo() {
            return this.mVideoCaps != null;
        }

        public VideoCapabilities getVideoCapabilities() {
            return this.mVideoCaps;
        }

        public CodecCapabilities dup() {
            CodecCapabilities codecCapabilities = new CodecCapabilities();
            codecCapabilities.profileLevels = (CodecProfileLevel[]) Arrays.copyOf(this.profileLevels, this.profileLevels.length);
            codecCapabilities.colorFormats = Arrays.copyOf(this.colorFormats, this.colorFormats.length);
            codecCapabilities.mMime = this.mMime;
            codecCapabilities.mMaxSupportedInstances = this.mMaxSupportedInstances;
            codecCapabilities.mFlagsRequired = this.mFlagsRequired;
            codecCapabilities.mFlagsSupported = this.mFlagsSupported;
            codecCapabilities.mFlagsVerified = this.mFlagsVerified;
            codecCapabilities.mAudioCaps = this.mAudioCaps;
            codecCapabilities.mVideoCaps = this.mVideoCaps;
            codecCapabilities.mEncoderCaps = this.mEncoderCaps;
            codecCapabilities.mDefaultFormat = this.mDefaultFormat;
            codecCapabilities.mCapabilitiesInfo = this.mCapabilitiesInfo;
            return codecCapabilities;
        }

        public static CodecCapabilities createFromProfileLevel(String str, int i, int i2) {
            CodecProfileLevel codecProfileLevel = new CodecProfileLevel();
            codecProfileLevel.profile = i;
            codecProfileLevel.level = i2;
            MediaFormat mediaFormat = new MediaFormat();
            mediaFormat.setString(MediaFormat.KEY_MIME, str);
            CodecCapabilities codecCapabilities = new CodecCapabilities(new CodecProfileLevel[]{codecProfileLevel}, new int[0], true, 0, mediaFormat, new MediaFormat());
            if (codecCapabilities.mError != 0) {
                return null;
            }
            return codecCapabilities;
        }

        CodecCapabilities(CodecProfileLevel[] codecProfileLevelArr, int[] iArr, boolean z, int i, Map<String, Object> map, Map<String, Object> map2) {
            this(codecProfileLevelArr, iArr, z, i, new MediaFormat(map), new MediaFormat(map2));
        }

        CodecCapabilities(CodecProfileLevel[] codecProfileLevelArr, int[] iArr, boolean z, int i, MediaFormat mediaFormat, MediaFormat mediaFormat2) {
            Map<String, Object> map = mediaFormat2.getMap();
            this.colorFormats = iArr;
            this.mFlagsVerified = i;
            this.mDefaultFormat = mediaFormat;
            this.mCapabilitiesInfo = mediaFormat2;
            this.mMime = this.mDefaultFormat.getString(MediaFormat.KEY_MIME);
            if (codecProfileLevelArr.length == 0 && this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_VP9)) {
                CodecProfileLevel codecProfileLevel = new CodecProfileLevel();
                codecProfileLevel.profile = 1;
                codecProfileLevel.level = VideoCapabilities.equivalentVP9Level(mediaFormat2);
                codecProfileLevelArr = new CodecProfileLevel[]{codecProfileLevel};
            }
            this.profileLevels = codecProfileLevelArr;
            if (this.mMime.toLowerCase().startsWith("audio/")) {
                this.mAudioCaps = AudioCapabilities.create(mediaFormat2, this);
                this.mAudioCaps.getDefaultFormat(this.mDefaultFormat);
            } else if (this.mMime.toLowerCase().startsWith("video/") || this.mMime.equalsIgnoreCase(MediaFormat.MIMETYPE_IMAGE_ANDROID_HEIC)) {
                this.mVideoCaps = VideoCapabilities.create(mediaFormat2, this);
            }
            if (z) {
                this.mEncoderCaps = EncoderCapabilities.create(mediaFormat2, this);
                this.mEncoderCaps.getDefaultFormat(this.mDefaultFormat);
            }
            this.mMaxSupportedInstances = Utils.parseIntSafely(MediaCodecList.getGlobalSettings().get("max-concurrent-instances"), 32);
            this.mMaxSupportedInstances = ((Integer) Range.create(1, 256).clamp(Integer.valueOf(Utils.parseIntSafely(map.get("max-concurrent-instances"), this.mMaxSupportedInstances)))).intValue();
            for (Feature feature : getValidFeatures()) {
                String str = MediaFormat.KEY_FEATURE_ + feature.mName;
                Integer num = (Integer) map.get(str);
                if (num != null) {
                    if (num.intValue() > 0) {
                        this.mFlagsRequired |= feature.mValue;
                    }
                    this.mFlagsSupported = feature.mValue | this.mFlagsSupported;
                    this.mDefaultFormat.setInteger(str, 1);
                }
            }
        }
    }

    public static final class AudioCapabilities {
        private static final int MAX_INPUT_CHANNEL_COUNT = 30;
        private static final String TAG = "AudioCapabilities";
        private Range<Integer> mBitrateRange;
        private int mMaxInputChannelCount;
        private CodecCapabilities mParent;
        private Range<Integer>[] mSampleRateRanges;
        private int[] mSampleRates;

        public Range<Integer> getBitrateRange() {
            return this.mBitrateRange;
        }

        public int[] getSupportedSampleRates() {
            return Arrays.copyOf(this.mSampleRates, this.mSampleRates.length);
        }

        public Range<Integer>[] getSupportedSampleRateRanges() {
            return (Range[]) Arrays.copyOf(this.mSampleRateRanges, this.mSampleRateRanges.length);
        }

        public int getMaxInputChannelCount() {
            return this.mMaxInputChannelCount;
        }

        private AudioCapabilities() {
        }

        public static AudioCapabilities create(MediaFormat mediaFormat, CodecCapabilities codecCapabilities) {
            AudioCapabilities audioCapabilities = new AudioCapabilities();
            audioCapabilities.init(mediaFormat, codecCapabilities);
            return audioCapabilities;
        }

        private void init(MediaFormat mediaFormat, CodecCapabilities codecCapabilities) {
            this.mParent = codecCapabilities;
            initWithPlatformLimits();
            applyLevelLimits();
            parseFromInfo(mediaFormat);
        }

        private void initWithPlatformLimits() {
            this.mBitrateRange = Range.create(0, Integer.MAX_VALUE);
            this.mMaxInputChannelCount = 30;
            this.mSampleRateRanges = new Range[]{Range.create(8000, 96000)};
            this.mSampleRates = null;
        }

        private boolean supports(Integer num, Integer num2) {
            if (num2 == null || (num2.intValue() >= 1 && num2.intValue() <= this.mMaxInputChannelCount)) {
                return num == null || Utils.binarySearchDistinctRanges(this.mSampleRateRanges, num) >= 0;
            }
            return false;
        }

        public boolean isSampleRateSupported(int i) {
            return supports(Integer.valueOf(i), null);
        }

        private void limitSampleRates(int[] iArr) {
            Arrays.sort(iArr);
            ArrayList arrayList = new ArrayList();
            for (int i : iArr) {
                if (supports(Integer.valueOf(i), null)) {
                    arrayList.add(Range.create(Integer.valueOf(i), Integer.valueOf(i)));
                }
            }
            this.mSampleRateRanges = (Range[]) arrayList.toArray(new Range[arrayList.size()]);
            createDiscreteSampleRates();
        }

        private void createDiscreteSampleRates() {
            this.mSampleRates = new int[this.mSampleRateRanges.length];
            for (int i = 0; i < this.mSampleRateRanges.length; i++) {
                this.mSampleRates[i] = ((Integer) this.mSampleRateRanges[i].getLower()).intValue();
            }
        }

        private void limitSampleRates(Range<Integer>[] rangeArr) {
            Utils.sortDistinctRanges(rangeArr);
            this.mSampleRateRanges = Utils.intersectSortedDistinctRanges(this.mSampleRateRanges, rangeArr);
            for (Range<Integer> range : this.mSampleRateRanges) {
                if (!((Integer) range.getLower()).equals(range.getUpper())) {
                    this.mSampleRates = null;
                    return;
                }
            }
            createDiscreteSampleRates();
        }

        private void applyLevelLimits() {
            int[] iArr;
            Range<Integer> rangeCreate;
            int[] iArr2;
            String mimeType = this.mParent.getMimeType();
            int i = 2;
            Range<Integer> rangeCreate2 = null;
            if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_MPEG)) {
                iArr = new int[]{8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000};
                rangeCreate = Range.create(8000, 320000);
            } else {
                if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_NB)) {
                    iArr = new int[]{8000};
                    rangeCreate = Range.create(4750, 12200);
                } else if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_WB)) {
                    iArr = new int[]{16000};
                    rangeCreate = Range.create(6600, 23850);
                } else if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                    iArr = new int[]{7350, 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000, 64000, 88200, 96000};
                    rangeCreate = Range.create(8000, 510000);
                    i = 48;
                } else if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_VORBIS)) {
                    i = 255;
                    rangeCreate = Range.create(32000, 500000);
                    iArr = null;
                    rangeCreate2 = Range.create(8000, Integer.valueOf(AudioFormat.SAMPLE_RATE_HZ_MAX));
                } else {
                    if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_OPUS)) {
                        i = 255;
                        iArr2 = new int[]{8000, 12000, 16000, 24000, 48000};
                        rangeCreate = Range.create(Integer.valueOf(BluetoothHealth.HEALTH_OPERATION_SUCCESS), 510000);
                    } else if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_RAW)) {
                        Range<Integer> rangeCreate3 = Range.create(1, 96000);
                        rangeCreate = Range.create(1, 10000000);
                        i = AudioTrack.CHANNEL_COUNT_MAX;
                        iArr2 = null;
                        rangeCreate2 = rangeCreate3;
                    } else if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_FLAC)) {
                        i = 255;
                        rangeCreate = null;
                        rangeCreate2 = Range.create(1, 655350);
                        iArr = null;
                    } else {
                        if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_ALAW) || mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_MLAW)) {
                            iArr = new int[]{8000};
                            rangeCreate = Range.create(64000, 64000);
                        } else if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_MSGSM)) {
                            iArr = new int[]{8000};
                            rangeCreate = Range.create(13000, 13000);
                        } else {
                            if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AC3)) {
                                i = 6;
                            } else if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_EAC3)) {
                                i = 16;
                            } else {
                                Log.w(TAG, "Unsupported mime " + mimeType);
                                CodecCapabilities codecCapabilities = this.mParent;
                                codecCapabilities.mError = codecCapabilities.mError | 2;
                                iArr = null;
                                rangeCreate = null;
                            }
                            iArr = null;
                            rangeCreate = null;
                        }
                        i = 30;
                    }
                    iArr = iArr2;
                }
                i = 1;
            }
            if (iArr != null) {
                limitSampleRates(iArr);
            } else if (rangeCreate2 != null) {
                limitSampleRates(new Range[]{rangeCreate2});
            }
            applyLimits(i, rangeCreate);
        }

        private void applyLimits(int i, Range<Integer> range) {
            this.mMaxInputChannelCount = ((Integer) Range.create(1, Integer.valueOf(this.mMaxInputChannelCount)).clamp(Integer.valueOf(i))).intValue();
            if (range != null) {
                this.mBitrateRange = this.mBitrateRange.intersect(range);
            }
        }

        private void parseFromInfo(MediaFormat mediaFormat) {
            Range rangeIntersect = MediaCodecInfo.POSITIVE_INTEGERS;
            int intSafely = 0;
            if (mediaFormat.containsKey("sample-rate-ranges")) {
                String[] strArrSplit = mediaFormat.getString("sample-rate-ranges").split(",");
                Range[] rangeArr = new Range[strArrSplit.length];
                for (int i = 0; i < strArrSplit.length; i++) {
                    rangeArr[i] = Utils.parseIntRange(strArrSplit[i], null);
                }
                limitSampleRates((Range<Integer>[]) rangeArr);
            }
            if (mediaFormat.containsKey("max-channel-count")) {
                intSafely = Utils.parseIntSafely(mediaFormat.getString("max-channel-count"), 30);
            } else if ((this.mParent.mError & 2) == 0) {
                intSafely = 30;
            }
            if (mediaFormat.containsKey("bitrate-range")) {
                rangeIntersect = rangeIntersect.intersect(Utils.parseIntRange(mediaFormat.getString("bitrate-range"), rangeIntersect));
            }
            applyLimits(intSafely, rangeIntersect);
        }

        public void getDefaultFormat(MediaFormat mediaFormat) {
            if (((Integer) this.mBitrateRange.getLower()).equals(this.mBitrateRange.getUpper())) {
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, ((Integer) this.mBitrateRange.getLower()).intValue());
            }
            if (this.mMaxInputChannelCount == 1) {
                mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            }
            if (this.mSampleRates != null && this.mSampleRates.length == 1) {
                mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, this.mSampleRates[0]);
            }
        }

        public boolean supportsFormat(MediaFormat mediaFormat) {
            Map<String, Object> map = mediaFormat.getMap();
            return supports((Integer) map.get(MediaFormat.KEY_SAMPLE_RATE), (Integer) map.get(MediaFormat.KEY_CHANNEL_COUNT)) && CodecCapabilities.supportsBitrate(this.mBitrateRange, mediaFormat);
        }
    }

    public static final class VideoCapabilities {
        private static final String TAG = "VideoCapabilities";
        private boolean mAllowMbOverride;
        private Range<Rational> mAspectRatioRange;
        private Range<Integer> mBitrateRange;
        private Range<Rational> mBlockAspectRatioRange;
        private Range<Integer> mBlockCountRange;
        private int mBlockHeight;
        private int mBlockWidth;
        private Range<Long> mBlocksPerSecondRange;
        private Range<Integer> mFrameRateRange;
        private int mHeightAlignment;
        private Range<Integer> mHeightRange;
        private Range<Integer> mHorizontalBlockRange;
        private Map<Size, Range<Long>> mMeasuredFrameRates;
        private CodecCapabilities mParent;
        private int mSmallerDimensionUpperLimit;
        private Range<Integer> mVerticalBlockRange;
        private int mWidthAlignment;
        private Range<Integer> mWidthRange;

        public Range<Integer> getBitrateRange() {
            return this.mBitrateRange;
        }

        public Range<Integer> getSupportedWidths() {
            return this.mWidthRange;
        }

        public Range<Integer> getSupportedHeights() {
            return this.mHeightRange;
        }

        public int getWidthAlignment() {
            return this.mWidthAlignment;
        }

        public int getHeightAlignment() {
            return this.mHeightAlignment;
        }

        public int getSmallerDimensionUpperLimit() {
            return this.mSmallerDimensionUpperLimit;
        }

        public Range<Integer> getSupportedFrameRates() {
            return this.mFrameRateRange;
        }

        public Range<Integer> getSupportedWidthsFor(int i) {
            try {
                Range<Integer> range = this.mWidthRange;
                if (!this.mHeightRange.contains(Integer.valueOf(i)) || i % this.mHeightAlignment != 0) {
                    throw new IllegalArgumentException("unsupported height");
                }
                int iDivUp = Utils.divUp(i, this.mBlockHeight);
                double d = iDivUp;
                Range rangeIntersect = range.intersect(Integer.valueOf(((Math.max(Utils.divUp(((Integer) this.mBlockCountRange.getLower()).intValue(), iDivUp), (int) Math.ceil(((Rational) this.mBlockAspectRatioRange.getLower()).doubleValue() * d)) - 1) * this.mBlockWidth) + this.mWidthAlignment), Integer.valueOf(Math.min(((Integer) this.mBlockCountRange.getUpper()).intValue() / iDivUp, (int) (((Rational) this.mBlockAspectRatioRange.getUpper()).doubleValue() * d)) * this.mBlockWidth));
                if (i > this.mSmallerDimensionUpperLimit) {
                    rangeIntersect = rangeIntersect.intersect(1, Integer.valueOf(this.mSmallerDimensionUpperLimit));
                }
                double d2 = i;
                return rangeIntersect.intersect(Integer.valueOf((int) Math.ceil(((Rational) this.mAspectRatioRange.getLower()).doubleValue() * d2)), Integer.valueOf((int) (((Rational) this.mAspectRatioRange.getUpper()).doubleValue() * d2)));
            } catch (IllegalArgumentException e) {
                Log.v(TAG, "could not get supported widths for " + i);
                throw new IllegalArgumentException("unsupported height");
            }
        }

        public Range<Integer> getSupportedHeightsFor(int i) {
            try {
                Range<Integer> range = this.mHeightRange;
                if (!this.mWidthRange.contains(Integer.valueOf(i)) || i % this.mWidthAlignment != 0) {
                    throw new IllegalArgumentException("unsupported width");
                }
                int iDivUp = Utils.divUp(i, this.mBlockWidth);
                double d = iDivUp;
                Range rangeIntersect = range.intersect(Integer.valueOf(((Math.max(Utils.divUp(((Integer) this.mBlockCountRange.getLower()).intValue(), iDivUp), (int) Math.ceil(d / ((Rational) this.mBlockAspectRatioRange.getUpper()).doubleValue())) - 1) * this.mBlockHeight) + this.mHeightAlignment), Integer.valueOf(Math.min(((Integer) this.mBlockCountRange.getUpper()).intValue() / iDivUp, (int) (d / ((Rational) this.mBlockAspectRatioRange.getLower()).doubleValue())) * this.mBlockHeight));
                if (i > this.mSmallerDimensionUpperLimit) {
                    rangeIntersect = rangeIntersect.intersect(1, Integer.valueOf(this.mSmallerDimensionUpperLimit));
                }
                double d2 = i;
                return rangeIntersect.intersect(Integer.valueOf((int) Math.ceil(d2 / ((Rational) this.mAspectRatioRange.getUpper()).doubleValue())), Integer.valueOf((int) (d2 / ((Rational) this.mAspectRatioRange.getLower()).doubleValue())));
            } catch (IllegalArgumentException e) {
                Log.v(TAG, "could not get supported heights for " + i);
                throw new IllegalArgumentException("unsupported width");
            }
        }

        public Range<Double> getSupportedFrameRatesFor(int i, int i2) {
            Range<Integer> range = this.mHeightRange;
            if (!supports(Integer.valueOf(i), Integer.valueOf(i2), null)) {
                throw new IllegalArgumentException("unsupported size");
            }
            double dDivUp = Utils.divUp(i, this.mBlockWidth) * Utils.divUp(i2, this.mBlockHeight);
            return Range.create(Double.valueOf(Math.max(((Long) this.mBlocksPerSecondRange.getLower()).longValue() / dDivUp, ((Integer) this.mFrameRateRange.getLower()).intValue())), Double.valueOf(Math.min(((Long) this.mBlocksPerSecondRange.getUpper()).longValue() / dDivUp, ((Integer) this.mFrameRateRange.getUpper()).intValue())));
        }

        private int getBlockCount(int i, int i2) {
            return Utils.divUp(i, this.mBlockWidth) * Utils.divUp(i2, this.mBlockHeight);
        }

        private Size findClosestSize(int i, int i2) {
            int blockCount = getBlockCount(i, i2);
            Size size = null;
            int i3 = Integer.MAX_VALUE;
            for (Size size2 : this.mMeasuredFrameRates.keySet()) {
                int iAbs = Math.abs(blockCount - getBlockCount(size2.getWidth(), size2.getHeight()));
                if (iAbs < i3) {
                    size = size2;
                    i3 = iAbs;
                }
            }
            return size;
        }

        private Range<Double> estimateFrameRatesFor(int i, int i2) {
            Size sizeFindClosestSize = findClosestSize(i, i2);
            Range<Long> range = this.mMeasuredFrameRates.get(sizeFindClosestSize);
            Double dValueOf = Double.valueOf(((double) getBlockCount(sizeFindClosestSize.getWidth(), sizeFindClosestSize.getHeight())) / ((double) Math.max(getBlockCount(i, i2), 1)));
            return Range.create(Double.valueOf(((Long) range.getLower()).longValue() * dValueOf.doubleValue()), Double.valueOf(((Long) range.getUpper()).longValue() * dValueOf.doubleValue()));
        }

        public Range<Double> getAchievableFrameRatesFor(int i, int i2) {
            if (!supports(Integer.valueOf(i), Integer.valueOf(i2), null)) {
                throw new IllegalArgumentException("unsupported size");
            }
            if (this.mMeasuredFrameRates == null || this.mMeasuredFrameRates.size() <= 0) {
                Log.w(TAG, "Codec did not publish any measurement data.");
                return null;
            }
            return estimateFrameRatesFor(i, i2);
        }

        public boolean areSizeAndRateSupported(int i, int i2, double d) {
            return supports(Integer.valueOf(i), Integer.valueOf(i2), Double.valueOf(d));
        }

        public boolean isSizeSupported(int i, int i2) {
            return supports(Integer.valueOf(i), Integer.valueOf(i2), null);
        }

        private boolean supports(Integer num, Integer num2, Number number) {
            boolean zContains = num == null || (this.mWidthRange.contains(num) && num.intValue() % this.mWidthAlignment == 0);
            if (zContains && num2 != null) {
                zContains = this.mHeightRange.contains(num2) && num2.intValue() % this.mHeightAlignment == 0;
            }
            if (zContains && number != null) {
                zContains = this.mFrameRateRange.contains(Utils.intRangeFor(number.doubleValue()));
            }
            if (zContains && num2 != null && num != null) {
                boolean z = Math.min(num2.intValue(), num.intValue()) <= this.mSmallerDimensionUpperLimit;
                int iDivUp = Utils.divUp(num.intValue(), this.mBlockWidth);
                int iDivUp2 = Utils.divUp(num2.intValue(), this.mBlockHeight);
                int i = iDivUp * iDivUp2;
                boolean z2 = z && this.mBlockCountRange.contains(Integer.valueOf(i)) && this.mBlockAspectRatioRange.contains(new Rational(iDivUp, iDivUp2)) && this.mAspectRatioRange.contains(new Rational(num.intValue(), num2.intValue()));
                if (z2 && number != null) {
                    return this.mBlocksPerSecondRange.contains(Utils.longRangeFor(((double) i) * number.doubleValue()));
                }
                return z2;
            }
            return zContains;
        }

        public boolean supportsFormat(MediaFormat mediaFormat) {
            Map<String, Object> map = mediaFormat.getMap();
            return supports((Integer) map.get("width"), (Integer) map.get("height"), (Number) map.get(MediaFormat.KEY_FRAME_RATE)) && CodecCapabilities.supportsBitrate(this.mBitrateRange, mediaFormat);
        }

        private VideoCapabilities() {
        }

        public static VideoCapabilities create(MediaFormat mediaFormat, CodecCapabilities codecCapabilities) {
            VideoCapabilities videoCapabilities = new VideoCapabilities();
            videoCapabilities.init(mediaFormat, codecCapabilities);
            return videoCapabilities;
        }

        private void init(MediaFormat mediaFormat, CodecCapabilities codecCapabilities) {
            this.mParent = codecCapabilities;
            initWithPlatformLimits();
            applyLevelLimits();
            parseFromInfo(mediaFormat);
            updateLimits();
        }

        public Size getBlockSize() {
            return new Size(this.mBlockWidth, this.mBlockHeight);
        }

        public Range<Integer> getBlockCountRange() {
            return this.mBlockCountRange;
        }

        public Range<Long> getBlocksPerSecondRange() {
            return this.mBlocksPerSecondRange;
        }

        public Range<Rational> getAspectRatioRange(boolean z) {
            return z ? this.mBlockAspectRatioRange : this.mAspectRatioRange;
        }

        private void initWithPlatformLimits() {
            this.mBitrateRange = MediaCodecInfo.BITRATE_RANGE;
            this.mWidthRange = MediaCodecInfo.SIZE_RANGE;
            this.mHeightRange = MediaCodecInfo.SIZE_RANGE;
            this.mFrameRateRange = MediaCodecInfo.FRAME_RATE_RANGE;
            this.mHorizontalBlockRange = MediaCodecInfo.SIZE_RANGE;
            this.mVerticalBlockRange = MediaCodecInfo.SIZE_RANGE;
            this.mBlockCountRange = MediaCodecInfo.POSITIVE_INTEGERS;
            this.mBlocksPerSecondRange = MediaCodecInfo.POSITIVE_LONGS;
            this.mBlockAspectRatioRange = MediaCodecInfo.POSITIVE_RATIONALS;
            this.mAspectRatioRange = MediaCodecInfo.POSITIVE_RATIONALS;
            this.mWidthAlignment = 2;
            this.mHeightAlignment = 2;
            this.mBlockWidth = 2;
            this.mBlockHeight = 2;
            this.mSmallerDimensionUpperLimit = ((Integer) MediaCodecInfo.SIZE_RANGE.getUpper()).intValue();
        }

        private Map<Size, Range<Long>> getMeasuredFrameRates(Map<String, Object> map) {
            Size size;
            Range<Long> longRange;
            HashMap map2 = new HashMap();
            for (String str : map.keySet()) {
                if (str.startsWith("measured-frame-rate-")) {
                    str.substring("measured-frame-rate-".length());
                    String[] strArrSplit = str.split(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                    if (strArrSplit.length == 5 && (size = Utils.parseSize(strArrSplit[3], null)) != null && size.getWidth() * size.getHeight() > 0 && (longRange = Utils.parseLongRange(map.get(str), null)) != null && ((Long) longRange.getLower()).longValue() >= 0 && ((Long) longRange.getUpper()).longValue() >= 0) {
                        map2.put(size, longRange);
                    }
                }
            }
            return map2;
        }

        private static Pair<Range<Integer>, Range<Integer>> parseWidthHeightRanges(Object obj) {
            Pair<Size, Size> sizeRange = Utils.parseSizeRange(obj);
            if (sizeRange != null) {
                try {
                    return Pair.create(Range.create(Integer.valueOf(sizeRange.first.getWidth()), Integer.valueOf(sizeRange.second.getWidth())), Range.create(Integer.valueOf(sizeRange.first.getHeight()), Integer.valueOf(sizeRange.second.getHeight())));
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "could not parse size range '" + obj + "'");
                    return null;
                }
            }
            return null;
        }

        public static int equivalentVP9Level(MediaFormat mediaFormat) {
            int iIntValue;
            int iMax;
            Map<String, Object> map = mediaFormat.getMap();
            Size size = Utils.parseSize(map.get("block-size"), new Size(8, 8));
            int width = size.getWidth() * size.getHeight();
            Range<Integer> intRange = Utils.parseIntRange(map.get("block-count-range"), null);
            if (intRange != null) {
                iIntValue = ((Integer) intRange.getUpper()).intValue() * width;
            } else {
                iIntValue = 0;
            }
            Range<Long> longRange = Utils.parseLongRange(map.get("blocks-per-second-range"), null);
            long jLongValue = longRange == null ? 0L : ((long) width) * ((Long) longRange.getUpper()).longValue();
            Pair<Range<Integer>, Range<Integer>> widthHeightRanges = parseWidthHeightRanges(map.get("size-range"));
            if (widthHeightRanges != null) {
                iMax = Math.max(((Integer) widthHeightRanges.first.getUpper()).intValue(), ((Integer) widthHeightRanges.second.getUpper()).intValue());
            } else {
                iMax = 0;
            }
            Range<Integer> intRange2 = Utils.parseIntRange(map.get("bitrate-range"), null);
            int iDivUp = intRange2 != null ? Utils.divUp(((Integer) intRange2.getUpper()).intValue(), 1000) : 0;
            if (jLongValue <= 829440 && iIntValue <= 36864 && iDivUp <= 200 && iMax <= 512) {
                return 1;
            }
            if (jLongValue <= 2764800 && iIntValue <= 73728 && iDivUp <= 800 && iMax <= 768) {
                return 2;
            }
            if (jLongValue <= 4608000 && iIntValue <= 122880 && iDivUp <= 1800 && iMax <= 960) {
                return 4;
            }
            if (jLongValue <= 9216000 && iIntValue <= 245760 && iDivUp <= 3600 && iMax <= 1344) {
                return 8;
            }
            if (jLongValue <= 20736000 && iIntValue <= 552960 && iDivUp <= 7200 && iMax <= 2048) {
                return 16;
            }
            if (jLongValue <= 36864000 && iIntValue <= 983040 && iDivUp <= 12000 && iMax <= 2752) {
                return 32;
            }
            if (jLongValue <= 83558400 && iIntValue <= 2228224 && iDivUp <= 18000 && iMax <= 4160) {
                return 64;
            }
            if (jLongValue <= 160432128 && iIntValue <= 2228224 && iDivUp <= 30000 && iMax <= 4160) {
                return 128;
            }
            if (jLongValue <= 311951360 && iIntValue <= 8912896 && iDivUp <= 60000 && iMax <= 8384) {
                return 256;
            }
            if (jLongValue <= 588251136 && iIntValue <= 8912896 && iDivUp <= 120000 && iMax <= 8384) {
                return 512;
            }
            if (jLongValue <= 1176502272 && iIntValue <= 8912896 && iDivUp <= 180000 && iMax <= 8384) {
                return 1024;
            }
            if (jLongValue <= 1176502272 && iIntValue <= 35651584 && iDivUp <= 180000 && iMax <= 16832) {
                return 2048;
            }
            if (jLongValue > 2353004544L || iIntValue > 35651584 || iDivUp > 240000 || iMax > 16832) {
                return (jLongValue > 4706009088L || iIntValue > 35651584 || iDivUp > 480000 || iMax <= 16832) ? 8192 : 8192;
            }
            return 4096;
        }

        private void parseFromInfo(MediaFormat mediaFormat) {
            Range<Integer> range;
            Range<Integer> range2;
            Range<Integer> rangeExtend;
            Range<Integer> range3;
            Object objIntersect;
            Range<T> rangeIntersect;
            Map<String, Object> map = mediaFormat.getMap();
            Size size = new Size(this.mBlockWidth, this.mBlockHeight);
            Size size2 = new Size(this.mWidthAlignment, this.mHeightAlignment);
            Size size3 = Utils.parseSize(map.get("block-size"), size);
            Size size4 = Utils.parseSize(map.get("alignment"), size2);
            Range<Integer> intRange = Utils.parseIntRange(map.get("block-count-range"), null);
            Range<Long> longRange = Utils.parseLongRange(map.get("blocks-per-second-range"), null);
            this.mMeasuredFrameRates = getMeasuredFrameRates(map);
            Pair<Range<Integer>, Range<Integer>> widthHeightRanges = parseWidthHeightRanges(map.get("size-range"));
            if (widthHeightRanges != null) {
                range2 = widthHeightRanges.first;
                range = widthHeightRanges.second;
            } else {
                range = null;
                range2 = null;
            }
            if (map.containsKey("feature-can-swap-width-height")) {
                if (range2 != null) {
                    this.mSmallerDimensionUpperLimit = Math.min(((Integer) range2.getUpper()).intValue(), ((Integer) range.getUpper()).intValue());
                    rangeExtend = range2.extend(range);
                    range3 = rangeExtend;
                } else {
                    Log.w(TAG, "feature can-swap-width-height is best used with size-range");
                    this.mSmallerDimensionUpperLimit = Math.min(((Integer) this.mWidthRange.getUpper()).intValue(), ((Integer) this.mHeightRange.getUpper()).intValue());
                    Range rangeExtend2 = this.mWidthRange.extend(this.mHeightRange);
                    this.mHeightRange = rangeExtend2;
                    this.mWidthRange = rangeExtend2;
                    rangeExtend = range;
                    range3 = range2;
                }
            } else {
                rangeExtend = range;
                range3 = range2;
            }
            Range<Rational> rationalRange = Utils.parseRationalRange(map.get("block-aspect-ratio-range"), null);
            Range<Rational> rationalRange2 = Utils.parseRationalRange(map.get("pixel-aspect-ratio-range"), null);
            Range<Integer> intRange2 = Utils.parseIntRange(map.get("frame-rate-range"), null);
            if (intRange2 != null) {
                try {
                    objIntersect = intRange2.intersect(MediaCodecInfo.FRAME_RATE_RANGE);
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "frame rate range (" + intRange2 + ") is out of limits: " + MediaCodecInfo.FRAME_RATE_RANGE);
                    objIntersect = null;
                }
            } else {
                objIntersect = intRange2;
            }
            Range<Integer> intRange3 = Utils.parseIntRange(map.get("bitrate-range"), null);
            if (intRange3 != null) {
                try {
                    rangeIntersect = intRange3.intersect(MediaCodecInfo.BITRATE_RANGE);
                } catch (IllegalArgumentException e2) {
                    Log.w(TAG, "bitrate range (" + intRange3 + ") is out of limits: " + MediaCodecInfo.BITRATE_RANGE);
                    rangeIntersect = 0;
                }
            } else {
                rangeIntersect = intRange3;
            }
            MediaCodecInfo.checkPowerOfTwo(size3.getWidth(), "block-size width must be power of two");
            MediaCodecInfo.checkPowerOfTwo(size3.getHeight(), "block-size height must be power of two");
            MediaCodecInfo.checkPowerOfTwo(size4.getWidth(), "alignment width must be power of two");
            MediaCodecInfo.checkPowerOfTwo(size4.getHeight(), "alignment height must be power of two");
            Range<T> range4 = objIntersect;
            Range<Integer> range5 = rangeExtend;
            applyMacroBlockLimits(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, size3.getWidth(), size3.getHeight(), size4.getWidth(), size4.getHeight());
            if ((this.mParent.mError & 2) != 0 || this.mAllowMbOverride) {
                if (range3 != null) {
                    this.mWidthRange = MediaCodecInfo.SIZE_RANGE.intersect(range3);
                }
                if (range5 != null) {
                    this.mHeightRange = MediaCodecInfo.SIZE_RANGE.intersect(range5);
                }
                if (intRange != null) {
                    this.mBlockCountRange = MediaCodecInfo.POSITIVE_INTEGERS.intersect(Utils.factorRange(intRange, ((this.mBlockWidth * this.mBlockHeight) / size3.getWidth()) / size3.getHeight()));
                }
                if (longRange != null) {
                    this.mBlocksPerSecondRange = MediaCodecInfo.POSITIVE_LONGS.intersect(Utils.factorRange(longRange, ((this.mBlockWidth * this.mBlockHeight) / size3.getWidth()) / size3.getHeight()));
                }
                if (rationalRange2 != null) {
                    this.mBlockAspectRatioRange = MediaCodecInfo.POSITIVE_RATIONALS.intersect(Utils.scaleRange(rationalRange2, this.mBlockHeight / size3.getHeight(), this.mBlockWidth / size3.getWidth()));
                }
                if (rationalRange != null) {
                    this.mAspectRatioRange = MediaCodecInfo.POSITIVE_RATIONALS.intersect(rationalRange);
                }
                if (range4 != 0) {
                    this.mFrameRateRange = MediaCodecInfo.FRAME_RATE_RANGE.intersect(range4);
                }
                if (rangeIntersect != 0) {
                    if ((this.mParent.mError & 2) != 0) {
                        this.mBitrateRange = MediaCodecInfo.BITRATE_RANGE.intersect(rangeIntersect);
                    } else {
                        this.mBitrateRange = this.mBitrateRange.intersect(rangeIntersect);
                    }
                }
            } else {
                if (range3 != null) {
                    this.mWidthRange = this.mWidthRange.intersect(range3);
                }
                if (range5 != null) {
                    this.mHeightRange = this.mHeightRange.intersect(range5);
                }
                if (intRange != null) {
                    this.mBlockCountRange = this.mBlockCountRange.intersect(Utils.factorRange(intRange, ((this.mBlockWidth * this.mBlockHeight) / size3.getWidth()) / size3.getHeight()));
                }
                if (longRange != null) {
                    this.mBlocksPerSecondRange = this.mBlocksPerSecondRange.intersect(Utils.factorRange(longRange, ((this.mBlockWidth * this.mBlockHeight) / size3.getWidth()) / size3.getHeight()));
                }
                if (rationalRange2 != null) {
                    this.mBlockAspectRatioRange = this.mBlockAspectRatioRange.intersect(Utils.scaleRange(rationalRange2, this.mBlockHeight / size3.getHeight(), this.mBlockWidth / size3.getWidth()));
                }
                if (rationalRange != null) {
                    this.mAspectRatioRange = this.mAspectRatioRange.intersect(rationalRange);
                }
                if (range4 != 0) {
                    this.mFrameRateRange = this.mFrameRateRange.intersect(range4);
                }
                if (rangeIntersect != 0) {
                    this.mBitrateRange = this.mBitrateRange.intersect(rangeIntersect);
                }
            }
            updateLimits();
        }

        private void applyBlockLimits(int i, int i2, Range<Integer> range, Range<Long> range2, Range<Rational> range3) {
            MediaCodecInfo.checkPowerOfTwo(i, "blockWidth must be a power of two");
            MediaCodecInfo.checkPowerOfTwo(i2, "blockHeight must be a power of two");
            int iMax = Math.max(i, this.mBlockWidth);
            int iMax2 = Math.max(i2, this.mBlockHeight);
            int i3 = iMax * iMax2;
            int i4 = (i3 / this.mBlockWidth) / this.mBlockHeight;
            if (i4 != 1) {
                this.mBlockCountRange = Utils.factorRange(this.mBlockCountRange, i4);
                this.mBlocksPerSecondRange = Utils.factorRange(this.mBlocksPerSecondRange, i4);
                this.mBlockAspectRatioRange = Utils.scaleRange(this.mBlockAspectRatioRange, iMax2 / this.mBlockHeight, iMax / this.mBlockWidth);
                this.mHorizontalBlockRange = Utils.factorRange(this.mHorizontalBlockRange, iMax / this.mBlockWidth);
                this.mVerticalBlockRange = Utils.factorRange(this.mVerticalBlockRange, iMax2 / this.mBlockHeight);
            }
            int i5 = (i3 / i) / i2;
            if (i5 != 1) {
                range = Utils.factorRange(range, i5);
                range2 = Utils.factorRange(range2, i5);
                range3 = Utils.scaleRange(range3, iMax2 / i2, iMax / i);
            }
            this.mBlockCountRange = this.mBlockCountRange.intersect(range);
            this.mBlocksPerSecondRange = this.mBlocksPerSecondRange.intersect(range2);
            this.mBlockAspectRatioRange = this.mBlockAspectRatioRange.intersect(range3);
            this.mBlockWidth = iMax;
            this.mBlockHeight = iMax2;
        }

        private void applyAlignment(int i, int i2) {
            MediaCodecInfo.checkPowerOfTwo(i, "widthAlignment must be a power of two");
            MediaCodecInfo.checkPowerOfTwo(i2, "heightAlignment must be a power of two");
            if (i > this.mBlockWidth || i2 > this.mBlockHeight) {
                applyBlockLimits(Math.max(i, this.mBlockWidth), Math.max(i2, this.mBlockHeight), MediaCodecInfo.POSITIVE_INTEGERS, MediaCodecInfo.POSITIVE_LONGS, MediaCodecInfo.POSITIVE_RATIONALS);
            }
            this.mWidthAlignment = Math.max(i, this.mWidthAlignment);
            this.mHeightAlignment = Math.max(i2, this.mHeightAlignment);
            this.mWidthRange = Utils.alignRange(this.mWidthRange, this.mWidthAlignment);
            this.mHeightRange = Utils.alignRange(this.mHeightRange, this.mHeightAlignment);
        }

        private void updateLimits() {
            this.mHorizontalBlockRange = this.mHorizontalBlockRange.intersect(Utils.factorRange(this.mWidthRange, this.mBlockWidth));
            this.mHorizontalBlockRange = this.mHorizontalBlockRange.intersect(Range.create(Integer.valueOf(((Integer) this.mBlockCountRange.getLower()).intValue() / ((Integer) this.mVerticalBlockRange.getUpper()).intValue()), Integer.valueOf(((Integer) this.mBlockCountRange.getUpper()).intValue() / ((Integer) this.mVerticalBlockRange.getLower()).intValue())));
            this.mVerticalBlockRange = this.mVerticalBlockRange.intersect(Utils.factorRange(this.mHeightRange, this.mBlockHeight));
            this.mVerticalBlockRange = this.mVerticalBlockRange.intersect(Range.create(Integer.valueOf(((Integer) this.mBlockCountRange.getLower()).intValue() / ((Integer) this.mHorizontalBlockRange.getUpper()).intValue()), Integer.valueOf(((Integer) this.mBlockCountRange.getUpper()).intValue() / ((Integer) this.mHorizontalBlockRange.getLower()).intValue())));
            this.mBlockCountRange = this.mBlockCountRange.intersect(Range.create(Integer.valueOf(((Integer) this.mHorizontalBlockRange.getLower()).intValue() * ((Integer) this.mVerticalBlockRange.getLower()).intValue()), Integer.valueOf(((Integer) this.mHorizontalBlockRange.getUpper()).intValue() * ((Integer) this.mVerticalBlockRange.getUpper()).intValue())));
            this.mBlockAspectRatioRange = this.mBlockAspectRatioRange.intersect(new Rational(((Integer) this.mHorizontalBlockRange.getLower()).intValue(), ((Integer) this.mVerticalBlockRange.getUpper()).intValue()), new Rational(((Integer) this.mHorizontalBlockRange.getUpper()).intValue(), ((Integer) this.mVerticalBlockRange.getLower()).intValue()));
            this.mWidthRange = this.mWidthRange.intersect(Integer.valueOf(((((Integer) this.mHorizontalBlockRange.getLower()).intValue() - 1) * this.mBlockWidth) + this.mWidthAlignment), Integer.valueOf(((Integer) this.mHorizontalBlockRange.getUpper()).intValue() * this.mBlockWidth));
            this.mHeightRange = this.mHeightRange.intersect(Integer.valueOf(((((Integer) this.mVerticalBlockRange.getLower()).intValue() - 1) * this.mBlockHeight) + this.mHeightAlignment), Integer.valueOf(((Integer) this.mVerticalBlockRange.getUpper()).intValue() * this.mBlockHeight));
            this.mAspectRatioRange = this.mAspectRatioRange.intersect(new Rational(((Integer) this.mWidthRange.getLower()).intValue(), ((Integer) this.mHeightRange.getUpper()).intValue()), new Rational(((Integer) this.mWidthRange.getUpper()).intValue(), ((Integer) this.mHeightRange.getLower()).intValue()));
            this.mSmallerDimensionUpperLimit = Math.min(this.mSmallerDimensionUpperLimit, Math.min(((Integer) this.mWidthRange.getUpper()).intValue(), ((Integer) this.mHeightRange.getUpper()).intValue()));
            this.mBlocksPerSecondRange = this.mBlocksPerSecondRange.intersect(Long.valueOf(((long) ((Integer) this.mBlockCountRange.getLower()).intValue()) * ((long) ((Integer) this.mFrameRateRange.getLower()).intValue())), Long.valueOf(((long) ((Integer) this.mBlockCountRange.getUpper()).intValue()) * ((long) ((Integer) this.mFrameRateRange.getUpper()).intValue())));
            this.mFrameRateRange = this.mFrameRateRange.intersect(Integer.valueOf((int) (((Long) this.mBlocksPerSecondRange.getLower()).longValue() / ((long) ((Integer) this.mBlockCountRange.getUpper()).intValue()))), Integer.valueOf((int) (((Long) this.mBlocksPerSecondRange.getUpper()).longValue() / ((double) ((Integer) this.mBlockCountRange.getLower()).intValue()))));
        }

        private void applyMacroBlockLimits(int i, int i2, int i3, long j, int i4, int i5, int i6, int i7) {
            applyMacroBlockLimits(1, 1, i, i2, i3, j, i4, i5, i6, i7);
        }

        private void applyMacroBlockLimits(int i, int i2, int i3, int i4, int i5, long j, int i6, int i7, int i8, int i9) {
            applyAlignment(i8, i9);
            applyBlockLimits(i6, i7, Range.create(1, Integer.valueOf(i5)), Range.create(1L, Long.valueOf(j)), Range.create(new Rational(1, i4), new Rational(i3, 1)));
            this.mHorizontalBlockRange = this.mHorizontalBlockRange.intersect(Integer.valueOf(Utils.divUp(i, this.mBlockWidth / i6)), Integer.valueOf(i3 / (this.mBlockWidth / i6)));
            this.mVerticalBlockRange = this.mVerticalBlockRange.intersect(Integer.valueOf(Utils.divUp(i2, this.mBlockHeight / i7)), Integer.valueOf(i4 / (this.mBlockHeight / i7)));
        }

        private void applyLevelLimits() {
            VideoCapabilities videoCapabilities;
            VideoCapabilities videoCapabilities2;
            int iMax;
            int i;
            int i2;
            int i3;
            int i4;
            int i5;
            int i6;
            long j;
            int i7;
            int i8;
            int i9;
            int i10;
            int i11;
            int i12;
            int i13;
            int i14;
            int i15;
            int i16;
            int i17;
            int i18;
            int i19;
            int i20;
            int i21;
            int i22;
            int i23;
            int i24;
            int i25;
            boolean z;
            int i26;
            int i27;
            int i28;
            int i29;
            int i30;
            int i31;
            int i32;
            int i33;
            int i34;
            int i35;
            String str;
            int i36;
            CodecProfileLevel[] codecProfileLevelArr;
            int i37;
            boolean z2;
            int i38;
            int i39;
            int i40;
            boolean z3;
            int i41;
            int i42;
            int i43;
            int i44;
            int i45;
            int iMax2;
            int iMax3;
            int i46;
            int i47;
            int i48;
            int i49;
            int i50;
            String str2;
            CodecProfileLevel[] codecProfileLevelArr2;
            int i51;
            int i52;
            int i53;
            int i54;
            int i55;
            boolean z4;
            int i56;
            int i57;
            int i58;
            int i59;
            int i60;
            int i61;
            int i62;
            int i63;
            int i64;
            int i65;
            int i66;
            int i67;
            int i68;
            int i69;
            int i70;
            int i71;
            CodecProfileLevel[] codecProfileLevelArr3;
            boolean z5;
            int i72;
            CodecProfileLevel[] codecProfileLevelArr4 = this.mParent.profileLevels;
            String mimeType = this.mParent.getMimeType();
            int i73 = 4;
            if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                int length = codecProfileLevelArr4.length;
                iMax = 64000;
                long jMax = 1485;
                int i74 = 0;
                int iMax4 = 396;
                int iMax5 = 99;
                i = 4;
                while (i74 < length) {
                    CodecProfileLevel codecProfileLevel = codecProfileLevelArr4[i74];
                    switch (codecProfileLevel.level) {
                        case 1:
                            i63 = 1485;
                            i64 = 64;
                            i65 = 99;
                            i66 = 396;
                            break;
                        case 2:
                            i63 = 1485;
                            i64 = 128;
                            i65 = 99;
                            i66 = 396;
                            break;
                        case 4:
                            i63 = 3000;
                            i67 = 192;
                            i68 = 900;
                            i64 = i67;
                            i66 = i68;
                            i65 = 396;
                            break;
                        case 8:
                            i63 = BluetoothHealth.HEALTH_OPERATION_SUCCESS;
                            i67 = MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION;
                            i68 = 2376;
                            i64 = i67;
                            i66 = i68;
                            i65 = 396;
                            break;
                        case 16:
                            i63 = 11880;
                            i67 = 768;
                            i68 = 2376;
                            i64 = i67;
                            i66 = i68;
                            i65 = 396;
                            break;
                        case 32:
                            i63 = 11880;
                            i67 = 2000;
                            i68 = 2376;
                            i64 = i67;
                            i66 = i68;
                            i65 = 396;
                            break;
                        case 64:
                            i63 = 19800;
                            i69 = MetricsProto.MetricsEvent.DEFAULT_AUTOFILL_PICKER;
                            i70 = 4000;
                            i71 = 4752;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        case 128:
                            i63 = 20250;
                            i69 = 1620;
                            i70 = 4000;
                            i71 = 8100;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        case 256:
                            i63 = 40500;
                            i69 = 1620;
                            i70 = 10000;
                            i71 = 8100;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        case 512:
                            i63 = 108000;
                            i69 = NetworkScanRequest.MAX_SEARCH_MAX_SEC;
                            i70 = 14000;
                            i71 = 18000;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        case 1024:
                            i63 = 216000;
                            i69 = 5120;
                            i70 = 20000;
                            i71 = MtpConstants.DEVICE_PROPERTY_UNDEFINED;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        case 2048:
                            i63 = 245760;
                            i69 = 8192;
                            i70 = 20000;
                            i71 = 32768;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        case 4096:
                            i63 = 245760;
                            i69 = 8192;
                            i70 = 50000;
                            i71 = 32768;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        case 8192:
                            i63 = 522240;
                            i69 = 8704;
                            i70 = 50000;
                            i71 = GLES20.GL_STENCIL_BACK_FUNC;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        case 16384:
                            i63 = 589824;
                            i69 = 22080;
                            i70 = 135000;
                            i71 = 110400;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        case 32768:
                            i63 = SurfaceControl.FX_SURFACE_MASK;
                            i69 = 36864;
                            i70 = 240000;
                            i71 = 184320;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        case 65536:
                            i63 = 2073600;
                            i69 = 36864;
                            i70 = 240000;
                            i71 = 184320;
                            i65 = i69;
                            i64 = i70;
                            i66 = i71;
                            break;
                        default:
                            Log.w(TAG, "Unrecognized level " + codecProfileLevel.level + " for " + mimeType);
                            i |= 1;
                            i64 = 0;
                            i65 = 0;
                            i63 = 0;
                            i66 = 0;
                            break;
                    }
                    int i75 = length;
                    int i76 = codecProfileLevel.profile;
                    if (i76 == i73) {
                        codecProfileLevelArr3 = codecProfileLevelArr4;
                        Log.w(TAG, "Unsupported profile " + codecProfileLevel.profile + " for " + mimeType);
                        i |= 2;
                        z5 = false;
                        i72 = i64 * 1000;
                    } else if (i76 == 8) {
                        codecProfileLevelArr3 = codecProfileLevelArr4;
                        i72 = i64 * MetricsProto.MetricsEvent.FIELD_SELECTION_RANGE_START;
                        z5 = true;
                    } else {
                        if (i76 != 16) {
                            if (i76 != 32 && i76 != 64) {
                                if (i76 != 65536) {
                                    if (i76 != 524288) {
                                        switch (i76) {
                                            case 1:
                                            case 2:
                                                break;
                                            default:
                                                StringBuilder sb = new StringBuilder();
                                                codecProfileLevelArr3 = codecProfileLevelArr4;
                                                sb.append("Unrecognized profile ");
                                                sb.append(codecProfileLevel.profile);
                                                sb.append(" for ");
                                                sb.append(mimeType);
                                                Log.w(TAG, sb.toString());
                                                i |= 1;
                                                i72 = i64 * 1000;
                                                break;
                                        }
                                    }
                                }
                                codecProfileLevelArr3 = codecProfileLevelArr4;
                                z5 = true;
                            }
                            i72 = i64 * 1000;
                        } else {
                            codecProfileLevelArr3 = codecProfileLevelArr4;
                            i72 = i64 * 3000;
                        }
                        z5 = true;
                    }
                    if (z5) {
                        i &= -5;
                    }
                    jMax = Math.max(i63, jMax);
                    iMax5 = Math.max(i65, iMax5);
                    iMax = Math.max(i72, iMax);
                    iMax4 = Math.max(iMax4, i66);
                    i74++;
                    length = i75;
                    codecProfileLevelArr4 = codecProfileLevelArr3;
                    i73 = 4;
                }
                int iSqrt = (int) Math.sqrt(iMax5 * 8);
                applyMacroBlockLimits(iSqrt, iSqrt, iMax5, jMax, 16, 16, 1, 1);
                videoCapabilities2 = this;
            } else if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_MPEG2)) {
                CodecProfileLevel[] codecProfileLevelArr5 = codecProfileLevelArr4;
                int length2 = codecProfileLevelArr5.length;
                int iMax6 = 15;
                iMax = 64000;
                int i77 = 0;
                int iMax7 = 9;
                int iMax8 = 11;
                int i78 = 4;
                long jMax2 = 1485;
                int iMax9 = 99;
                while (i77 < length2) {
                    CodecProfileLevel codecProfileLevel2 = codecProfileLevelArr5[i77];
                    switch (codecProfileLevel2.profile) {
                        case 0:
                            if (codecProfileLevel2.level != 1) {
                                Log.w(TAG, "Unrecognized profile/level " + codecProfileLevel2.profile + "/" + codecProfileLevel2.level + " for " + mimeType);
                                i78 |= 1;
                                str2 = mimeType;
                                codecProfileLevelArr2 = codecProfileLevelArr5;
                                i51 = length2;
                                i52 = 0;
                                z4 = true;
                                i57 = 0;
                                i55 = 0;
                                i53 = 0;
                                i54 = 0;
                                i56 = 0;
                            } else {
                                i46 = 45;
                                i47 = 36;
                                i48 = 40500;
                                i49 = 1620;
                                i50 = 15000;
                                str2 = mimeType;
                                codecProfileLevelArr2 = codecProfileLevelArr5;
                                i51 = length2;
                                i52 = 30;
                                i53 = i47;
                                i54 = i46;
                                i55 = i50;
                                z4 = true;
                                int i79 = i49;
                                i56 = i48;
                                i57 = i79;
                            }
                            break;
                        case 1:
                            switch (codecProfileLevel2.level) {
                                case 0:
                                    str2 = mimeType;
                                    i56 = 11880;
                                    codecProfileLevelArr2 = codecProfileLevelArr5;
                                    i51 = length2;
                                    i52 = 30;
                                    i57 = 396;
                                    i53 = 18;
                                    i54 = 22;
                                    i55 = 4000;
                                    z4 = true;
                                    break;
                                case 1:
                                    i46 = 45;
                                    i47 = 36;
                                    i48 = 40500;
                                    i49 = 1620;
                                    i50 = 15000;
                                    str2 = mimeType;
                                    codecProfileLevelArr2 = codecProfileLevelArr5;
                                    i51 = length2;
                                    i52 = 30;
                                    i53 = i47;
                                    i54 = i46;
                                    i55 = i50;
                                    z4 = true;
                                    int i792 = i49;
                                    i56 = i48;
                                    i57 = i792;
                                    break;
                                case 2:
                                    i58 = 60;
                                    i59 = 90;
                                    i60 = 68;
                                    i56 = 183600;
                                    i61 = 6120;
                                    i62 = 60000;
                                    str2 = mimeType;
                                    i52 = i58;
                                    codecProfileLevelArr2 = codecProfileLevelArr5;
                                    i51 = length2;
                                    i55 = i62;
                                    i54 = i59;
                                    i53 = i60;
                                    i57 = i61;
                                    z4 = true;
                                    break;
                                case 3:
                                    i58 = 60;
                                    i59 = 120;
                                    i60 = 68;
                                    i56 = 244800;
                                    i61 = 8160;
                                    i62 = 80000;
                                    str2 = mimeType;
                                    i52 = i58;
                                    codecProfileLevelArr2 = codecProfileLevelArr5;
                                    i51 = length2;
                                    i55 = i62;
                                    i54 = i59;
                                    i53 = i60;
                                    i57 = i61;
                                    z4 = true;
                                    break;
                                case 4:
                                    i58 = 60;
                                    i59 = 120;
                                    i60 = 68;
                                    i56 = 489600;
                                    i61 = 8160;
                                    i62 = 80000;
                                    str2 = mimeType;
                                    i52 = i58;
                                    codecProfileLevelArr2 = codecProfileLevelArr5;
                                    i51 = length2;
                                    i55 = i62;
                                    i54 = i59;
                                    i53 = i60;
                                    i57 = i61;
                                    z4 = true;
                                    break;
                                default:
                                    Log.w(TAG, "Unrecognized profile/level " + codecProfileLevel2.profile + "/" + codecProfileLevel2.level + " for " + mimeType);
                                    i78 |= 1;
                                    str2 = mimeType;
                                    codecProfileLevelArr2 = codecProfileLevelArr5;
                                    i51 = length2;
                                    i52 = 0;
                                    z4 = true;
                                    i57 = 0;
                                    i55 = 0;
                                    i53 = 0;
                                    i54 = 0;
                                    i56 = 0;
                                    break;
                            }
                            break;
                        case 2:
                        case 3:
                        case 4:
                        case 5:
                            Log.i(TAG, "Unsupported profile " + codecProfileLevel2.profile + " for " + mimeType);
                            i78 |= 2;
                            str2 = mimeType;
                            codecProfileLevelArr2 = codecProfileLevelArr5;
                            i51 = length2;
                            i52 = 0;
                            z4 = false;
                            i57 = 0;
                            i55 = 0;
                            i53 = 0;
                            i54 = 0;
                            i56 = 0;
                            break;
                        default:
                            Log.w(TAG, "Unrecognized profile " + codecProfileLevel2.profile + " for " + mimeType);
                            i78 |= 1;
                            str2 = mimeType;
                            codecProfileLevelArr2 = codecProfileLevelArr5;
                            i51 = length2;
                            i52 = 0;
                            z4 = true;
                            i57 = 0;
                            i55 = 0;
                            i53 = 0;
                            i54 = 0;
                            i56 = 0;
                            break;
                    }
                    if (z4) {
                        i78 &= -5;
                    }
                    jMax2 = Math.max(i56, jMax2);
                    iMax9 = Math.max(i57, iMax9);
                    iMax = Math.max(i55 * 1000, iMax);
                    iMax8 = Math.max(i54, iMax8);
                    iMax7 = Math.max(i53, iMax7);
                    iMax6 = Math.max(i52, iMax6);
                    i77++;
                    length2 = i51;
                    codecProfileLevelArr5 = codecProfileLevelArr2;
                    mimeType = str2;
                }
                applyMacroBlockLimits(iMax8, iMax7, iMax9, jMax2, 16, 16, 1, 1);
                this.mFrameRateRange = this.mFrameRateRange.intersect(12, Integer.valueOf(iMax6));
                videoCapabilities2 = this;
                i = i78;
            } else if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_MPEG4)) {
                CodecProfileLevel[] codecProfileLevelArr6 = codecProfileLevelArr4;
                int length3 = codecProfileLevelArr6.length;
                int i80 = 15;
                iMax = 64000;
                long jMax3 = 1485;
                int i81 = 0;
                int iMax10 = 11;
                int i82 = 9;
                int iMax11 = 99;
                int i83 = 4;
                while (i81 < length3) {
                    CodecProfileLevel codecProfileLevel3 = codecProfileLevelArr6[i81];
                    switch (codecProfileLevel3.profile) {
                        case 1:
                            i35 = length3;
                            int i84 = codecProfileLevel3.level;
                            if (i84 == 4) {
                                str = mimeType;
                                i36 = 1485;
                                codecProfileLevelArr = codecProfileLevelArr6;
                                i37 = 9;
                                z2 = true;
                                i38 = 64;
                                i41 = 11;
                                i40 = 30;
                                i42 = 99;
                                z3 = false;
                            } else if (i84 == 8) {
                                str = mimeType;
                                i36 = 5940;
                                codecProfileLevelArr = codecProfileLevelArr6;
                                i37 = 18;
                                z2 = true;
                                i38 = 128;
                                i41 = 22;
                                i40 = 30;
                                i42 = 396;
                                z3 = false;
                            } else if (i84 == 16) {
                                i39 = 11880;
                                i38 = MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION;
                                str = mimeType;
                                i36 = i39;
                                codecProfileLevelArr = codecProfileLevelArr6;
                                i37 = 18;
                                z2 = true;
                                i41 = 22;
                                i40 = 30;
                                i42 = 396;
                                z3 = false;
                            } else if (i84 != 64) {
                                if (i84 == 128) {
                                    i43 = 45;
                                    i44 = 36;
                                    i36 = 40500;
                                    i42 = 1620;
                                    i45 = 8000;
                                } else if (i84 != 256) {
                                    switch (i84) {
                                        case 1:
                                            str = mimeType;
                                            i40 = 15;
                                            i36 = 1485;
                                            codecProfileLevelArr = codecProfileLevelArr6;
                                            i37 = 9;
                                            z2 = true;
                                            i38 = 64;
                                            i41 = 11;
                                            i42 = 99;
                                            z3 = true;
                                            break;
                                        case 2:
                                            str = mimeType;
                                            i40 = 15;
                                            i36 = 1485;
                                            codecProfileLevelArr = codecProfileLevelArr6;
                                            i37 = 9;
                                            z2 = true;
                                            i38 = 128;
                                            i41 = 11;
                                            i42 = 99;
                                            z3 = true;
                                            break;
                                        default:
                                            Log.w(TAG, "Unrecognized profile/level " + codecProfileLevel3.profile + "/" + codecProfileLevel3.level + " for " + mimeType);
                                            i83 |= 1;
                                            str = mimeType;
                                            codecProfileLevelArr = codecProfileLevelArr6;
                                            i37 = 0;
                                            z2 = true;
                                            i38 = 0;
                                            i41 = 0;
                                            i36 = 0;
                                            i40 = 0;
                                            i42 = 0;
                                            z3 = false;
                                            break;
                                    }
                                } else {
                                    i43 = 80;
                                    i44 = 45;
                                    i36 = 108000;
                                    i42 = NetworkScanRequest.MAX_SEARCH_MAX_SEC;
                                    i45 = 12000;
                                }
                                str = mimeType;
                                i37 = i44;
                                codecProfileLevelArr = codecProfileLevelArr6;
                                i38 = i45;
                                i40 = 30;
                                z3 = false;
                                i41 = i43;
                                z2 = true;
                            } else {
                                str = mimeType;
                                codecProfileLevelArr = codecProfileLevelArr6;
                                i37 = 30;
                                i40 = 30;
                                z3 = false;
                                i41 = 40;
                                z2 = true;
                                i36 = 36000;
                                i38 = 4000;
                                i42 = 1200;
                            }
                            break;
                        case 2:
                        case 4:
                        case 8:
                        case 16:
                        case 32:
                        case 64:
                        case 128:
                        case 256:
                        case 512:
                        case 1024:
                        case 2048:
                        case 4096:
                        case 8192:
                        case 16384:
                            i35 = length3;
                            Log.i(TAG, "Unsupported profile " + codecProfileLevel3.profile + " for " + mimeType);
                            i83 |= 2;
                            str = mimeType;
                            codecProfileLevelArr = codecProfileLevelArr6;
                            i37 = 0;
                            z2 = false;
                            i38 = 0;
                            i41 = 0;
                            i36 = 0;
                            i40 = 0;
                            i42 = 0;
                            z3 = false;
                            break;
                        case 32768:
                            int i85 = codecProfileLevel3.level;
                            if (i85 == 1 || i85 == 4) {
                                i35 = length3;
                                str = mimeType;
                                i36 = 2970;
                                codecProfileLevelArr = codecProfileLevelArr6;
                                i37 = 9;
                                z2 = true;
                                i38 = 128;
                                i41 = 11;
                                i40 = 30;
                                i42 = 99;
                                z3 = false;
                            } else {
                                if (i85 == 8) {
                                    i35 = length3;
                                    i39 = 5940;
                                    i38 = MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION;
                                } else if (i85 == 16) {
                                    i35 = length3;
                                    i39 = 11880;
                                    i38 = 768;
                                } else if (i85 != 24) {
                                    if (i85 == 32) {
                                        i35 = length3;
                                        i43 = 44;
                                        i44 = 36;
                                        i36 = 23760;
                                        i42 = MetricsProto.MetricsEvent.DEFAULT_AUTOFILL_PICKER;
                                        i45 = 3000;
                                    } else if (i85 != 128) {
                                        StringBuilder sb2 = new StringBuilder();
                                        i35 = length3;
                                        sb2.append("Unrecognized profile/level ");
                                        sb2.append(codecProfileLevel3.profile);
                                        sb2.append("/");
                                        sb2.append(codecProfileLevel3.level);
                                        sb2.append(" for ");
                                        sb2.append(mimeType);
                                        Log.w(TAG, sb2.toString());
                                        i83 |= 1;
                                        str = mimeType;
                                        codecProfileLevelArr = codecProfileLevelArr6;
                                        i37 = 0;
                                        z2 = true;
                                        i38 = 0;
                                        i41 = 0;
                                        i36 = 0;
                                        i40 = 0;
                                        i42 = 0;
                                        z3 = false;
                                    } else {
                                        i35 = length3;
                                        i43 = 45;
                                        i44 = 36;
                                        i36 = 48600;
                                        i42 = 1620;
                                        i45 = 8000;
                                    }
                                    str = mimeType;
                                    i37 = i44;
                                    codecProfileLevelArr = codecProfileLevelArr6;
                                    i38 = i45;
                                    i40 = 30;
                                    z3 = false;
                                    i41 = i43;
                                    z2 = true;
                                } else {
                                    i35 = length3;
                                    i39 = 11880;
                                    i38 = 1500;
                                }
                                str = mimeType;
                                i36 = i39;
                                codecProfileLevelArr = codecProfileLevelArr6;
                                i37 = 18;
                                z2 = true;
                                i41 = 22;
                                i40 = 30;
                                i42 = 396;
                                z3 = false;
                            }
                            break;
                        default:
                            i35 = length3;
                            Log.w(TAG, "Unrecognized profile " + codecProfileLevel3.profile + " for " + mimeType);
                            i83 |= 1;
                            str = mimeType;
                            codecProfileLevelArr = codecProfileLevelArr6;
                            i37 = 0;
                            z2 = true;
                            i38 = 0;
                            i41 = 0;
                            i36 = 0;
                            i40 = 0;
                            i42 = 0;
                            z3 = false;
                            break;
                    }
                    if (z2) {
                        i83 &= -5;
                    }
                    int i86 = i80;
                    int i87 = i40;
                    jMax3 = Math.max(i36, jMax3);
                    iMax11 = Math.max(i42, iMax11);
                    iMax = Math.max(i38 * 1000, iMax);
                    if (z3) {
                        iMax10 = Math.max(i41, iMax10);
                        iMax2 = Math.max(i37, i82);
                        iMax3 = Math.max(i87, i86);
                    } else {
                        int iSqrt2 = (int) Math.sqrt(i42 * 2);
                        iMax10 = Math.max(iSqrt2, iMax10);
                        iMax2 = Math.max(iSqrt2, i82);
                        iMax3 = Math.max(Math.max(i87, 60), i86);
                    }
                    i82 = iMax2;
                    i80 = iMax3;
                    i81++;
                    length3 = i35;
                    codecProfileLevelArr6 = codecProfileLevelArr;
                    mimeType = str;
                }
                videoCapabilities2 = this;
                applyMacroBlockLimits(iMax10, i82, iMax11, jMax3, 16, 16, 1, 1);
                videoCapabilities2.mFrameRateRange = videoCapabilities2.mFrameRateRange.intersect(12, Integer.valueOf(i80));
                i = i83;
            } else {
                if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_H263)) {
                    CodecProfileLevel[] codecProfileLevelArr7 = codecProfileLevelArr4;
                    int length4 = codecProfileLevelArr7.length;
                    int iMax12 = 15;
                    int i88 = 16;
                    long jMax4 = 1485;
                    int iMax13 = 64000;
                    int i89 = 0;
                    int i90 = 11;
                    int iMin = 9;
                    int iMax14 = 11;
                    int iMax15 = 9;
                    int iMax16 = 99;
                    int i91 = 4;
                    while (i89 < length4) {
                        int i92 = length4;
                        CodecProfileLevel codecProfileLevel4 = codecProfileLevelArr7[i89];
                        CodecProfileLevel[] codecProfileLevelArr8 = codecProfileLevelArr7;
                        int i93 = codecProfileLevel4.level;
                        int i94 = i88;
                        if (i93 == 4) {
                            i18 = i89;
                            i19 = 6;
                            i20 = 11880;
                        } else if (i93 != 8) {
                            if (i93 == 16) {
                                i18 = i89;
                                z = codecProfileLevel4.profile == 1 || codecProfileLevel4.profile == 4;
                                if (z) {
                                    i33 = i90;
                                    i34 = iMin;
                                } else {
                                    i33 = 1;
                                    i34 = 1;
                                    i94 = 4;
                                }
                                i23 = i90;
                                i25 = iMin;
                                i26 = 15;
                                i21 = 2;
                                i27 = 9;
                                i22 = i33;
                                i20 = 1485;
                                i24 = i34;
                                i28 = 11;
                            } else if (i93 == 32) {
                                i18 = i89;
                                i20 = 19800;
                                i23 = i90;
                                i25 = iMin;
                                i27 = 18;
                                i28 = 22;
                                i21 = 64;
                                i22 = 1;
                                i24 = 1;
                                i94 = 4;
                                i26 = 60;
                                z = false;
                            } else if (i93 == 64) {
                                i18 = i89;
                                i23 = i90;
                                i25 = iMin;
                                i27 = 18;
                                i21 = 128;
                                i22 = 1;
                                i24 = 1;
                                i94 = 4;
                                i26 = 60;
                                z = false;
                                i28 = 45;
                                i20 = 40500;
                            } else if (i93 != 128) {
                                switch (i93) {
                                    case 1:
                                        i18 = i89;
                                        i20 = 1485;
                                        i22 = i90;
                                        i23 = i22;
                                        i24 = iMin;
                                        i25 = i24;
                                        i27 = 9;
                                        i28 = 11;
                                        i21 = 1;
                                        i26 = 15;
                                        z = true;
                                        break;
                                    case 2:
                                        i18 = i89;
                                        i19 = 2;
                                        i20 = 5940;
                                        break;
                                    default:
                                        StringBuilder sb3 = new StringBuilder();
                                        i18 = i89;
                                        sb3.append("Unrecognized profile/level ");
                                        sb3.append(codecProfileLevel4.profile);
                                        sb3.append("/");
                                        sb3.append(codecProfileLevel4.level);
                                        sb3.append(" for ");
                                        sb3.append(mimeType);
                                        Log.w(TAG, sb3.toString());
                                        i91 |= 1;
                                        i22 = i90;
                                        i23 = i22;
                                        i24 = iMin;
                                        i25 = i24;
                                        z = false;
                                        i26 = 0;
                                        i27 = 0;
                                        i20 = 0;
                                        i28 = 0;
                                        i21 = 0;
                                        break;
                                }
                            } else {
                                i18 = i89;
                                i21 = 256;
                                i23 = i90;
                                i25 = iMin;
                                i27 = 36;
                                i24 = 1;
                                i94 = 4;
                                i26 = 60;
                                i28 = 45;
                                i20 = 81000;
                                z = false;
                                i22 = 1;
                            }
                            int i95 = i26;
                            i29 = codecProfileLevel4.profile;
                            int i96 = iMax12;
                            if (i29 == 4 && i29 != 8 && i29 != 16 && i29 != 32 && i29 != 64 && i29 != 128 && i29 != 256) {
                                switch (i29) {
                                    case 1:
                                    case 2:
                                        i30 = iMax15;
                                        break;
                                    default:
                                        StringBuilder sb4 = new StringBuilder();
                                        i30 = iMax15;
                                        sb4.append("Unrecognized profile ");
                                        sb4.append(codecProfileLevel4.profile);
                                        sb4.append(" for ");
                                        sb4.append(mimeType);
                                        Log.w(TAG, sb4.toString());
                                        i91 |= 1;
                                        break;
                                }
                            } else {
                                i30 = iMax15;
                            }
                            if (z) {
                                this.mAllowMbOverride = true;
                                i31 = i22;
                                i32 = i24;
                            } else {
                                i31 = 11;
                                i32 = 9;
                            }
                            i91 &= -5;
                            jMax4 = Math.max(i20, jMax4);
                            iMax16 = Math.max(i28 * i27, iMax16);
                            iMax13 = Math.max(64000 * i21, iMax13);
                            iMax14 = Math.max(i28, iMax14);
                            iMax15 = Math.max(i27, i30);
                            iMax12 = Math.max(i95, i96);
                            int iMin2 = Math.min(i31, i23);
                            iMin = Math.min(i32, i25);
                            length4 = i92;
                            codecProfileLevelArr7 = codecProfileLevelArr8;
                            i88 = i94;
                            i90 = iMin2;
                            i89 = i18 + 1;
                        } else {
                            i18 = i89;
                            i19 = 32;
                            i20 = 11880;
                        }
                        i21 = i19;
                        i22 = i90;
                        i23 = i22;
                        i24 = iMin;
                        i25 = i24;
                        z = true;
                        i26 = 30;
                        i27 = 18;
                        i28 = 22;
                        int i952 = i26;
                        i29 = codecProfileLevel4.profile;
                        int i962 = iMax12;
                        i30 = i29 == 4 ? iMax15 : iMax15;
                        if (z) {
                        }
                        i91 &= -5;
                        jMax4 = Math.max(i20, jMax4);
                        iMax16 = Math.max(i28 * i27, iMax16);
                        iMax13 = Math.max(64000 * i21, iMax13);
                        iMax14 = Math.max(i28, iMax14);
                        iMax15 = Math.max(i27, i30);
                        iMax12 = Math.max(i952, i962);
                        int iMin22 = Math.min(i31, i23);
                        iMin = Math.min(i32, i25);
                        length4 = i92;
                        codecProfileLevelArr7 = codecProfileLevelArr8;
                        i88 = i94;
                        i90 = iMin22;
                        i89 = i18 + 1;
                    }
                    int i97 = iMin;
                    int i98 = i88;
                    int i99 = i90;
                    if (!this.mAllowMbOverride) {
                        this.mBlockAspectRatioRange = Range.create(new Rational(11, 9), new Rational(11, 9));
                    }
                    videoCapabilities = this;
                    applyMacroBlockLimits(i99, i97, iMax14, iMax15, iMax16, jMax4, 16, 16, i98, i98);
                    videoCapabilities.mFrameRateRange = Range.create(1, Integer.valueOf(iMax12));
                    i = i91;
                    iMax = iMax13;
                } else {
                    videoCapabilities = this;
                    if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_VP8)) {
                        iMax = 100000000;
                        i = 4;
                        for (CodecProfileLevel codecProfileLevel5 : codecProfileLevelArr4) {
                            int i100 = codecProfileLevel5.level;
                            if (i100 != 4 && i100 != 8) {
                                switch (i100) {
                                    case 1:
                                    case 2:
                                        break;
                                    default:
                                        Log.w(TAG, "Unrecognized level " + codecProfileLevel5.level + " for " + mimeType);
                                        i |= 1;
                                        break;
                                }
                            }
                            if (codecProfileLevel5.profile != 1) {
                                Log.w(TAG, "Unrecognized profile " + codecProfileLevel5.profile + " for " + mimeType);
                                i |= 1;
                            }
                            i &= -5;
                        }
                        videoCapabilities.applyMacroBlockLimits(32767, 32767, Integer.MAX_VALUE, 2147483647L, 16, 16, 1, 1);
                    } else {
                        CodecProfileLevel[] codecProfileLevelArr9 = codecProfileLevelArr4;
                        if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_VP9)) {
                            int iMax17 = 512;
                            int length5 = codecProfileLevelArr9.length;
                            iMax = 200000;
                            i = 4;
                            long jMax5 = 829440;
                            int iMax18 = 36864;
                            int i101 = 0;
                            while (i101 < length5) {
                                CodecProfileLevel codecProfileLevel6 = codecProfileLevelArr9[i101];
                                switch (codecProfileLevel6.level) {
                                    case 1:
                                        j = 829440;
                                        i7 = 36864;
                                        i8 = 200;
                                        i9 = 512;
                                        i10 = length5;
                                        i11 = i8;
                                        i12 = i9;
                                        break;
                                    case 2:
                                        j = 2764800;
                                        i7 = 73728;
                                        i8 = 800;
                                        i9 = 768;
                                        i10 = length5;
                                        i11 = i8;
                                        i12 = i9;
                                        break;
                                    case 4:
                                        j = 4608000;
                                        i7 = 122880;
                                        i8 = 1800;
                                        i9 = 960;
                                        i10 = length5;
                                        i11 = i8;
                                        i12 = i9;
                                        break;
                                    case 8:
                                        j = 9216000;
                                        i7 = 245760;
                                        i8 = NetworkScanRequest.MAX_SEARCH_MAX_SEC;
                                        i9 = 1344;
                                        i10 = length5;
                                        i11 = i8;
                                        i12 = i9;
                                        break;
                                    case 16:
                                        j = 20736000;
                                        i7 = 552960;
                                        i8 = 7200;
                                        i9 = 2048;
                                        i10 = length5;
                                        i11 = i8;
                                        i12 = i9;
                                        break;
                                    case 32:
                                        j = 36864000;
                                        i7 = SurfaceControl.FX_SURFACE_MASK;
                                        i8 = 12000;
                                        i9 = 2752;
                                        i10 = length5;
                                        i11 = i8;
                                        i12 = i9;
                                        break;
                                    case 64:
                                        j = 83558400;
                                        i7 = 2228224;
                                        i8 = 18000;
                                        i9 = 4160;
                                        i10 = length5;
                                        i11 = i8;
                                        i12 = i9;
                                        break;
                                    case 128:
                                        j = 160432128;
                                        i7 = 2228224;
                                        i8 = 30000;
                                        i9 = 4160;
                                        i10 = length5;
                                        i11 = i8;
                                        i12 = i9;
                                        break;
                                    case 256:
                                        j = 311951360;
                                        i13 = 60000;
                                        i14 = 8384;
                                        i11 = i13;
                                        i10 = length5;
                                        i12 = i14;
                                        i7 = 8912896;
                                        break;
                                    case 512:
                                        j = 588251136;
                                        i13 = 120000;
                                        i14 = 8384;
                                        i11 = i13;
                                        i10 = length5;
                                        i12 = i14;
                                        i7 = 8912896;
                                        break;
                                    case 1024:
                                        j = 1176502272;
                                        i13 = 180000;
                                        i14 = 8384;
                                        i11 = i13;
                                        i10 = length5;
                                        i12 = i14;
                                        i7 = 8912896;
                                        break;
                                    case 2048:
                                        j = 1176502272;
                                        i15 = 180000;
                                        i16 = 16832;
                                        i11 = i15;
                                        i10 = length5;
                                        i12 = i16;
                                        i7 = 35651584;
                                        break;
                                    case 4096:
                                        j = 2353004544L;
                                        i15 = 240000;
                                        i16 = 16832;
                                        i11 = i15;
                                        i10 = length5;
                                        i12 = i16;
                                        i7 = 35651584;
                                        break;
                                    case 8192:
                                        j = 4706009088L;
                                        i15 = 480000;
                                        i16 = 16832;
                                        i11 = i15;
                                        i10 = length5;
                                        i12 = i16;
                                        i7 = 35651584;
                                        break;
                                    default:
                                        Log.w(TAG, "Unrecognized level " + codecProfileLevel6.level + " for " + mimeType);
                                        i |= 1;
                                        i10 = length5;
                                        i7 = 0;
                                        i12 = 0;
                                        i11 = 0;
                                        j = 0;
                                        break;
                                }
                                CodecProfileLevel[] codecProfileLevelArr10 = codecProfileLevelArr9;
                                int i102 = codecProfileLevel6.profile;
                                if (i102 != 4 && i102 != 8 && i102 != 4096 && i102 != 8192) {
                                    switch (i102) {
                                        case 1:
                                        case 2:
                                            i17 = i101;
                                            break;
                                        default:
                                            StringBuilder sb5 = new StringBuilder();
                                            i17 = i101;
                                            sb5.append("Unrecognized profile ");
                                            sb5.append(codecProfileLevel6.profile);
                                            sb5.append(" for ");
                                            sb5.append(mimeType);
                                            Log.w(TAG, sb5.toString());
                                            i |= 1;
                                            continue;
                                            i &= -5;
                                            jMax5 = Math.max(j, jMax5);
                                            iMax18 = Math.max(i7, iMax18);
                                            iMax = Math.max(i11 * 1000, iMax);
                                            iMax17 = Math.max(i12, iMax17);
                                            i101 = i17 + 1;
                                            length5 = i10;
                                            codecProfileLevelArr9 = codecProfileLevelArr10;
                                            break;
                                    }
                                } else {
                                    i17 = i101;
                                }
                                i &= -5;
                                jMax5 = Math.max(j, jMax5);
                                iMax18 = Math.max(i7, iMax18);
                                iMax = Math.max(i11 * 1000, iMax);
                                iMax17 = Math.max(i12, iMax17);
                                i101 = i17 + 1;
                                length5 = i10;
                                codecProfileLevelArr9 = codecProfileLevelArr10;
                            }
                            int iDivUp = Utils.divUp(iMax17, 8);
                            videoCapabilities2 = this;
                            videoCapabilities2.applyMacroBlockLimits(iDivUp, iDivUp, Utils.divUp(iMax18, 64), Utils.divUp(jMax5, 64L), 8, 8, 1, 1);
                        } else {
                            videoCapabilities2 = videoCapabilities;
                            if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                                CodecProfileLevel[] codecProfileLevelArr11 = codecProfileLevelArr9;
                                int length6 = codecProfileLevelArr11.length;
                                i = 4;
                                iMax = 128000;
                                int i103 = 0;
                                long jMax6 = 8640;
                                int iMax19 = 576;
                                while (i103 < length6) {
                                    CodecProfileLevel codecProfileLevel7 = codecProfileLevelArr11[i103];
                                    double d = 0.0d;
                                    switch (codecProfileLevel7.level) {
                                        case 1:
                                        case 2:
                                            d = 15.0d;
                                            i2 = 36864;
                                            i3 = 128;
                                            break;
                                        case 4:
                                        case 8:
                                            d = 30.0d;
                                            i2 = 122880;
                                            i3 = 1500;
                                            break;
                                        case 16:
                                        case 32:
                                            d = 30.0d;
                                            i2 = 245760;
                                            i3 = 3000;
                                            break;
                                        case 64:
                                        case 128:
                                            d = 30.0d;
                                            i2 = 552960;
                                            i3 = BluetoothHealth.HEALTH_OPERATION_SUCCESS;
                                            break;
                                        case 256:
                                        case 512:
                                            d = 33.75d;
                                            i2 = SurfaceControl.FX_SURFACE_MASK;
                                            i3 = 10000;
                                            break;
                                        case 1024:
                                            d = 30.0d;
                                            i2 = 2228224;
                                            i3 = 12000;
                                            break;
                                        case 2048:
                                            d = 30.0d;
                                            i2 = 2228224;
                                            i3 = 30000;
                                            break;
                                        case 4096:
                                            d = 60.0d;
                                            i2 = 2228224;
                                            i3 = 20000;
                                            break;
                                        case 8192:
                                            d = 60.0d;
                                            i2 = 2228224;
                                            i3 = 50000;
                                            break;
                                        case 16384:
                                            d = 30.0d;
                                            i4 = 25000;
                                            i3 = i4;
                                            i2 = 8912896;
                                            break;
                                        case 32768:
                                            d = 30.0d;
                                            i4 = UserHandle.PER_USER_RANGE;
                                            i3 = i4;
                                            i2 = 8912896;
                                            break;
                                        case 65536:
                                            d = 60.0d;
                                            i4 = HealthKeys.BASE_PACKAGE;
                                            i3 = i4;
                                            i2 = 8912896;
                                            break;
                                        case 131072:
                                            d = 60.0d;
                                            i4 = Protocol.BASE_WIFI_SCANNER_SERVICE;
                                            i3 = i4;
                                            i2 = 8912896;
                                            break;
                                        case 262144:
                                            d = 120.0d;
                                            i4 = 60000;
                                            i3 = i4;
                                            i2 = 8912896;
                                            break;
                                        case 524288:
                                            d = 120.0d;
                                            i4 = 240000;
                                            i3 = i4;
                                            i2 = 8912896;
                                            break;
                                        case 1048576:
                                            d = 30.0d;
                                            i5 = 60000;
                                            i3 = i5;
                                            i2 = 35651584;
                                            break;
                                        case 2097152:
                                            d = 30.0d;
                                            i5 = 240000;
                                            i3 = i5;
                                            i2 = 35651584;
                                            break;
                                        case 4194304:
                                            d = 60.0d;
                                            i5 = 120000;
                                            i3 = i5;
                                            i2 = 35651584;
                                            break;
                                        case 8388608:
                                            d = 60.0d;
                                            i5 = 480000;
                                            i3 = i5;
                                            i2 = 35651584;
                                            break;
                                        case 16777216:
                                            d = 120.0d;
                                            i5 = 240000;
                                            i3 = i5;
                                            i2 = 35651584;
                                            break;
                                        case 33554432:
                                            d = 120.0d;
                                            i5 = 800000;
                                            i3 = i5;
                                            i2 = 35651584;
                                            break;
                                        default:
                                            Log.w(TAG, "Unrecognized level " + codecProfileLevel7.level + " for " + mimeType);
                                            i |= 1;
                                            i2 = 0;
                                            i3 = 0;
                                            break;
                                    }
                                    int i104 = codecProfileLevel7.profile;
                                    CodecProfileLevel[] codecProfileLevelArr12 = codecProfileLevelArr11;
                                    if (i104 != 4096) {
                                        switch (i104) {
                                            case 1:
                                            case 2:
                                                i6 = length6;
                                                break;
                                            default:
                                                StringBuilder sb6 = new StringBuilder();
                                                i6 = length6;
                                                sb6.append("Unrecognized profile ");
                                                sb6.append(codecProfileLevel7.profile);
                                                sb6.append(" for ");
                                                sb6.append(mimeType);
                                                Log.w(TAG, sb6.toString());
                                                i |= 1;
                                                break;
                                        }
                                    }
                                    i &= -5;
                                    jMax6 = Math.max((int) (d * ((double) r2)), jMax6);
                                    iMax19 = Math.max(i2 >> 6, iMax19);
                                    iMax = Math.max(i3 * 1000, iMax);
                                    i103++;
                                    codecProfileLevelArr11 = codecProfileLevelArr12;
                                    length6 = i6;
                                }
                                int iSqrt3 = (int) Math.sqrt(iMax19 * 8);
                                videoCapabilities2.applyMacroBlockLimits(iSqrt3, iSqrt3, iMax19, jMax6, 8, 8, 1, 1);
                            } else {
                                Log.w(TAG, "Unsupported mime " + mimeType);
                                iMax = 64000;
                                i = 6;
                            }
                        }
                    }
                }
                videoCapabilities2 = videoCapabilities;
            }
            videoCapabilities2.mBitrateRange = Range.create(1, Integer.valueOf(iMax));
            videoCapabilities2.mParent.mError |= i;
        }
    }

    public static final class EncoderCapabilities {
        public static final int BITRATE_MODE_CBR = 2;
        public static final int BITRATE_MODE_CQ = 0;
        public static final int BITRATE_MODE_VBR = 1;
        private static final Feature[] bitrates = {new Feature("VBR", 1, true), new Feature("CBR", 2, false), new Feature("CQ", 0, false)};
        private int mBitControl;
        private Range<Integer> mComplexityRange;
        private Integer mDefaultComplexity;
        private Integer mDefaultQuality;
        private CodecCapabilities mParent;
        private Range<Integer> mQualityRange;
        private String mQualityScale;

        public Range<Integer> getQualityRange() {
            return this.mQualityRange;
        }

        public Range<Integer> getComplexityRange() {
            return this.mComplexityRange;
        }

        private static int parseBitrateMode(String str) {
            for (Feature feature : bitrates) {
                if (feature.mName.equalsIgnoreCase(str)) {
                    return feature.mValue;
                }
            }
            return 0;
        }

        public boolean isBitrateModeSupported(int i) {
            for (Feature feature : bitrates) {
                if (i == feature.mValue) {
                    return ((1 << i) & this.mBitControl) != 0;
                }
            }
            return false;
        }

        private EncoderCapabilities() {
        }

        public static EncoderCapabilities create(MediaFormat mediaFormat, CodecCapabilities codecCapabilities) {
            EncoderCapabilities encoderCapabilities = new EncoderCapabilities();
            encoderCapabilities.init(mediaFormat, codecCapabilities);
            return encoderCapabilities;
        }

        private void init(MediaFormat mediaFormat, CodecCapabilities codecCapabilities) {
            this.mParent = codecCapabilities;
            this.mComplexityRange = Range.create(0, 0);
            this.mQualityRange = Range.create(0, 0);
            this.mBitControl = 2;
            applyLevelLimits();
            parseFromInfo(mediaFormat);
        }

        private void applyLevelLimits() {
            String mimeType = this.mParent.getMimeType();
            if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_FLAC)) {
                this.mComplexityRange = Range.create(0, 8);
                this.mBitControl = 1;
            } else if (mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_NB) || mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_WB) || mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_ALAW) || mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_MLAW) || mimeType.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_MSGSM)) {
                this.mBitControl = 4;
            }
        }

        private void parseFromInfo(MediaFormat mediaFormat) {
            Map<String, Object> map = mediaFormat.getMap();
            if (mediaFormat.containsKey("complexity-range")) {
                this.mComplexityRange = Utils.parseIntRange(mediaFormat.getString("complexity-range"), this.mComplexityRange);
            }
            if (mediaFormat.containsKey("quality-range")) {
                this.mQualityRange = Utils.parseIntRange(mediaFormat.getString("quality-range"), this.mQualityRange);
            }
            if (mediaFormat.containsKey("feature-bitrate-modes")) {
                for (String str : mediaFormat.getString("feature-bitrate-modes").split(",")) {
                    this.mBitControl = (1 << parseBitrateMode(str)) | this.mBitControl;
                }
            }
            try {
                this.mDefaultComplexity = Integer.valueOf(Integer.parseInt((String) map.get("complexity-default")));
            } catch (NumberFormatException e) {
            }
            try {
                this.mDefaultQuality = Integer.valueOf(Integer.parseInt((String) map.get("quality-default")));
            } catch (NumberFormatException e2) {
            }
            this.mQualityScale = (String) map.get("quality-scale");
        }

        private boolean supports(Integer num, Integer num2, Integer num3) {
            boolean zContains;
            if (num != null) {
                zContains = this.mComplexityRange.contains(num);
            } else {
                zContains = true;
            }
            if (zContains && num2 != null) {
                zContains = this.mQualityRange.contains(num2);
            }
            if (!zContains || num3 == null) {
                return zContains;
            }
            CodecProfileLevel[] codecProfileLevelArr = this.mParent.profileLevels;
            int length = codecProfileLevelArr.length;
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (codecProfileLevelArr[i].profile != num3.intValue()) {
                    i++;
                } else {
                    num3 = null;
                    break;
                }
            }
            return num3 == null;
        }

        public void getDefaultFormat(MediaFormat mediaFormat) {
            if (!((Integer) this.mQualityRange.getUpper()).equals(this.mQualityRange.getLower()) && this.mDefaultQuality != null) {
                mediaFormat.setInteger(MediaFormat.KEY_QUALITY, this.mDefaultQuality.intValue());
            }
            if (!((Integer) this.mComplexityRange.getUpper()).equals(this.mComplexityRange.getLower()) && this.mDefaultComplexity != null) {
                mediaFormat.setInteger(MediaFormat.KEY_COMPLEXITY, this.mDefaultComplexity.intValue());
            }
            for (Feature feature : bitrates) {
                if ((this.mBitControl & (1 << feature.mValue)) != 0) {
                    mediaFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, feature.mValue);
                    return;
                }
            }
        }

        public boolean supportsFormat(MediaFormat mediaFormat) {
            Integer num;
            Map<String, Object> map = mediaFormat.getMap();
            String mimeType = this.mParent.getMimeType();
            Integer num2 = (Integer) map.get(MediaFormat.KEY_BITRATE_MODE);
            if (num2 != null && !isBitrateModeSupported(num2.intValue())) {
                return false;
            }
            Integer num3 = (Integer) map.get(MediaFormat.KEY_COMPLEXITY);
            if (MediaFormat.MIMETYPE_AUDIO_FLAC.equalsIgnoreCase(mimeType)) {
                Integer num4 = (Integer) map.get(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL);
                if (num3 != null) {
                    if (num4 != null && !num3.equals(num4)) {
                        throw new IllegalArgumentException("conflicting values for complexity and flac-compression-level");
                    }
                } else {
                    num3 = num4;
                }
            }
            Integer num5 = (Integer) map.get(MediaFormat.KEY_PROFILE);
            if (MediaFormat.MIMETYPE_AUDIO_AAC.equalsIgnoreCase(mimeType)) {
                num = (Integer) map.get(MediaFormat.KEY_AAC_PROFILE);
                if (num5 != null) {
                    if (num != null && !num.equals(num5)) {
                        throw new IllegalArgumentException("conflicting values for profile and aac-profile");
                    }
                    num = num5;
                }
            } else {
                num = num5;
            }
            return supports(num3, (Integer) map.get(MediaFormat.KEY_QUALITY), num);
        }
    }

    public static final class CodecProfileLevel {
        public static final int AACObjectELD = 39;
        public static final int AACObjectERLC = 17;
        public static final int AACObjectERScalable = 20;
        public static final int AACObjectHE = 5;
        public static final int AACObjectHE_PS = 29;
        public static final int AACObjectLC = 2;
        public static final int AACObjectLD = 23;
        public static final int AACObjectLTP = 4;
        public static final int AACObjectMain = 1;
        public static final int AACObjectSSR = 3;
        public static final int AACObjectScalable = 6;
        public static final int AACObjectXHE = 42;
        public static final int AVCLevel1 = 1;
        public static final int AVCLevel11 = 4;
        public static final int AVCLevel12 = 8;
        public static final int AVCLevel13 = 16;
        public static final int AVCLevel1b = 2;
        public static final int AVCLevel2 = 32;
        public static final int AVCLevel21 = 64;
        public static final int AVCLevel22 = 128;
        public static final int AVCLevel3 = 256;
        public static final int AVCLevel31 = 512;
        public static final int AVCLevel32 = 1024;
        public static final int AVCLevel4 = 2048;
        public static final int AVCLevel41 = 4096;
        public static final int AVCLevel42 = 8192;
        public static final int AVCLevel5 = 16384;
        public static final int AVCLevel51 = 32768;
        public static final int AVCLevel52 = 65536;
        public static final int AVCProfileBaseline = 1;
        public static final int AVCProfileConstrainedBaseline = 65536;
        public static final int AVCProfileConstrainedHigh = 524288;
        public static final int AVCProfileExtended = 4;
        public static final int AVCProfileHigh = 8;
        public static final int AVCProfileHigh10 = 16;
        public static final int AVCProfileHigh422 = 32;
        public static final int AVCProfileHigh444 = 64;
        public static final int AVCProfileMain = 2;
        public static final int DolbyVisionLevelFhd24 = 4;
        public static final int DolbyVisionLevelFhd30 = 8;
        public static final int DolbyVisionLevelFhd60 = 16;
        public static final int DolbyVisionLevelHd24 = 1;
        public static final int DolbyVisionLevelHd30 = 2;
        public static final int DolbyVisionLevelUhd24 = 32;
        public static final int DolbyVisionLevelUhd30 = 64;
        public static final int DolbyVisionLevelUhd48 = 128;
        public static final int DolbyVisionLevelUhd60 = 256;
        public static final int DolbyVisionProfileDvavPen = 2;
        public static final int DolbyVisionProfileDvavPer = 1;
        public static final int DolbyVisionProfileDvavSe = 512;
        public static final int DolbyVisionProfileDvheDen = 8;
        public static final int DolbyVisionProfileDvheDer = 4;
        public static final int DolbyVisionProfileDvheDtb = 128;
        public static final int DolbyVisionProfileDvheDth = 64;
        public static final int DolbyVisionProfileDvheDtr = 16;
        public static final int DolbyVisionProfileDvheSt = 256;
        public static final int DolbyVisionProfileDvheStn = 32;
        public static final int H263Level10 = 1;
        public static final int H263Level20 = 2;
        public static final int H263Level30 = 4;
        public static final int H263Level40 = 8;
        public static final int H263Level45 = 16;
        public static final int H263Level50 = 32;
        public static final int H263Level60 = 64;
        public static final int H263Level70 = 128;
        public static final int H263ProfileBackwardCompatible = 4;
        public static final int H263ProfileBaseline = 1;
        public static final int H263ProfileH320Coding = 2;
        public static final int H263ProfileHighCompression = 32;
        public static final int H263ProfileHighLatency = 256;
        public static final int H263ProfileISWV2 = 8;
        public static final int H263ProfileISWV3 = 16;
        public static final int H263ProfileInterlace = 128;
        public static final int H263ProfileInternet = 64;
        public static final int HEVCHighTierLevel1 = 2;
        public static final int HEVCHighTierLevel2 = 8;
        public static final int HEVCHighTierLevel21 = 32;
        public static final int HEVCHighTierLevel3 = 128;
        public static final int HEVCHighTierLevel31 = 512;
        public static final int HEVCHighTierLevel4 = 2048;
        public static final int HEVCHighTierLevel41 = 8192;
        public static final int HEVCHighTierLevel5 = 32768;
        public static final int HEVCHighTierLevel51 = 131072;
        public static final int HEVCHighTierLevel52 = 524288;
        public static final int HEVCHighTierLevel6 = 2097152;
        public static final int HEVCHighTierLevel61 = 8388608;
        public static final int HEVCHighTierLevel62 = 33554432;
        private static final int HEVCHighTierLevels = 44739242;
        public static final int HEVCMainTierLevel1 = 1;
        public static final int HEVCMainTierLevel2 = 4;
        public static final int HEVCMainTierLevel21 = 16;
        public static final int HEVCMainTierLevel3 = 64;
        public static final int HEVCMainTierLevel31 = 256;
        public static final int HEVCMainTierLevel4 = 1024;
        public static final int HEVCMainTierLevel41 = 4096;
        public static final int HEVCMainTierLevel5 = 16384;
        public static final int HEVCMainTierLevel51 = 65536;
        public static final int HEVCMainTierLevel52 = 262144;
        public static final int HEVCMainTierLevel6 = 1048576;
        public static final int HEVCMainTierLevel61 = 4194304;
        public static final int HEVCMainTierLevel62 = 16777216;
        public static final int HEVCProfileMain = 1;
        public static final int HEVCProfileMain10 = 2;
        public static final int HEVCProfileMain10HDR10 = 4096;
        public static final int HEVCProfileMainStill = 4;
        public static final int MPEG2LevelH14 = 2;
        public static final int MPEG2LevelHL = 3;
        public static final int MPEG2LevelHP = 4;
        public static final int MPEG2LevelLL = 0;
        public static final int MPEG2LevelML = 1;
        public static final int MPEG2Profile422 = 2;
        public static final int MPEG2ProfileHigh = 5;
        public static final int MPEG2ProfileMain = 1;
        public static final int MPEG2ProfileSNR = 3;
        public static final int MPEG2ProfileSimple = 0;
        public static final int MPEG2ProfileSpatial = 4;
        public static final int MPEG4Level0 = 1;
        public static final int MPEG4Level0b = 2;
        public static final int MPEG4Level1 = 4;
        public static final int MPEG4Level2 = 8;
        public static final int MPEG4Level3 = 16;
        public static final int MPEG4Level3b = 24;
        public static final int MPEG4Level4 = 32;
        public static final int MPEG4Level4a = 64;
        public static final int MPEG4Level5 = 128;
        public static final int MPEG4Level6 = 256;
        public static final int MPEG4ProfileAdvancedCoding = 4096;
        public static final int MPEG4ProfileAdvancedCore = 8192;
        public static final int MPEG4ProfileAdvancedRealTime = 1024;
        public static final int MPEG4ProfileAdvancedScalable = 16384;
        public static final int MPEG4ProfileAdvancedSimple = 32768;
        public static final int MPEG4ProfileBasicAnimated = 256;
        public static final int MPEG4ProfileCore = 4;
        public static final int MPEG4ProfileCoreScalable = 2048;
        public static final int MPEG4ProfileHybrid = 512;
        public static final int MPEG4ProfileMain = 8;
        public static final int MPEG4ProfileNbit = 16;
        public static final int MPEG4ProfileScalableTexture = 32;
        public static final int MPEG4ProfileSimple = 1;
        public static final int MPEG4ProfileSimpleFBA = 128;
        public static final int MPEG4ProfileSimpleFace = 64;
        public static final int MPEG4ProfileSimpleScalable = 2;
        public static final int VP8Level_Version0 = 1;
        public static final int VP8Level_Version1 = 2;
        public static final int VP8Level_Version2 = 4;
        public static final int VP8Level_Version3 = 8;
        public static final int VP8ProfileMain = 1;
        public static final int VP9Level1 = 1;
        public static final int VP9Level11 = 2;
        public static final int VP9Level2 = 4;
        public static final int VP9Level21 = 8;
        public static final int VP9Level3 = 16;
        public static final int VP9Level31 = 32;
        public static final int VP9Level4 = 64;
        public static final int VP9Level41 = 128;
        public static final int VP9Level5 = 256;
        public static final int VP9Level51 = 512;
        public static final int VP9Level52 = 1024;
        public static final int VP9Level6 = 2048;
        public static final int VP9Level61 = 4096;
        public static final int VP9Level62 = 8192;
        public static final int VP9Profile0 = 1;
        public static final int VP9Profile1 = 2;
        public static final int VP9Profile2 = 4;
        public static final int VP9Profile2HDR = 4096;
        public static final int VP9Profile3 = 8;
        public static final int VP9Profile3HDR = 8192;
        public int level;
        public int profile;

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof CodecProfileLevel)) {
                return false;
            }
            CodecProfileLevel codecProfileLevel = (CodecProfileLevel) obj;
            return codecProfileLevel.profile == this.profile && codecProfileLevel.level == this.level;
        }

        public int hashCode() {
            return Long.hashCode((((long) this.profile) << 32) | ((long) this.level));
        }
    }

    public final CodecCapabilities getCapabilitiesForType(String str) {
        CodecCapabilities codecCapabilities = this.mCaps.get(str);
        if (codecCapabilities == null) {
            throw new IllegalArgumentException("codec does not support type");
        }
        return codecCapabilities.dup();
    }

    public MediaCodecInfo makeRegular() {
        ArrayList arrayList = new ArrayList();
        for (CodecCapabilities codecCapabilities : this.mCaps.values()) {
            if (codecCapabilities.isRegular()) {
                arrayList.add(codecCapabilities);
            }
        }
        if (arrayList.size() == 0) {
            return null;
        }
        if (arrayList.size() == this.mCaps.size()) {
            return this;
        }
        return new MediaCodecInfo(this.mName, this.mIsEncoder, (CodecCapabilities[]) arrayList.toArray(new CodecCapabilities[arrayList.size()]));
    }
}
