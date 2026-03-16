package android.media;

import android.bluetooth.BluetoothHealth;
import android.mtp.MtpConstants;
import android.opengl.GLES20;
import android.os.UserHandle;
import android.util.Log;
import android.util.Pair;
import android.util.Range;
import android.util.Rational;
import android.util.Size;
import android.view.SurfaceControl;
import android.view.Window;
import com.android.internal.util.Protocol;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class MediaCodecInfo {
    private static final int ERROR_NONE_SUPPORTED = 4;
    private static final int ERROR_UNRECOGNIZED = 1;
    private static final int ERROR_UNSUPPORTED = 2;
    private Map<String, CodecCapabilities> mCaps = new HashMap();
    private boolean mIsEncoder;
    private String mName;
    private static final Range<Integer> POSITIVE_INTEGERS = Range.create(1, Integer.MAX_VALUE);
    private static final Range<Long> POSITIVE_LONGS = Range.create(1L, Long.MAX_VALUE);
    private static final Range<Rational> POSITIVE_RATIONALS = Range.create(new Rational(1, Integer.MAX_VALUE), new Rational(Integer.MAX_VALUE, 1));
    private static final Range<Integer> SIZE_RANGE = Range.create(1, 32768);
    private static final Range<Integer> FRAME_RATE_RANGE = Range.create(0, 960);
    private static final Range<Integer> BITRATE_RANGE = Range.create(0, 500000000);

    public static final class CodecProfileLevel {
        public static final int AACObjectELD = 39;
        public static final int AACObjectERLC = 17;
        public static final int AACObjectHE = 5;
        public static final int AACObjectHE_PS = 29;
        public static final int AACObjectLC = 2;
        public static final int AACObjectLD = 23;
        public static final int AACObjectLTP = 4;
        public static final int AACObjectMain = 1;
        public static final int AACObjectSSR = 3;
        public static final int AACObjectScalable = 6;
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
        public static final int AVCProfileExtended = 4;
        public static final int AVCProfileHigh = 8;
        public static final int AVCProfileHigh10 = 16;
        public static final int AVCProfileHigh422 = 32;
        public static final int AVCProfileHigh444 = 64;
        public static final int AVCProfileMain = 2;
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
        public static final int MPEG4Level0 = 1;
        public static final int MPEG4Level0b = 2;
        public static final int MPEG4Level1 = 4;
        public static final int MPEG4Level2 = 8;
        public static final int MPEG4Level3 = 16;
        public static final int MPEG4Level4 = 32;
        public static final int MPEG4Level4a = 64;
        public static final int MPEG4Level5 = 128;
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
        public int level;
        public int profile;
    }

    MediaCodecInfo(String name, boolean isEncoder, CodecCapabilities[] caps) {
        this.mName = name;
        this.mIsEncoder = isEncoder;
        for (CodecCapabilities c : caps) {
            if (!this.mName.equals("OMX.MARVELL.VIDEO.HW.HANTRODECODER") || !c.getMimeType().equals(MediaFormat.MIMETYPE_VIDEO_VP8)) {
                this.mCaps.put(c.getMimeType(), c);
            }
        }
    }

    public final String getName() {
        return this.mName;
    }

    public final boolean isEncoder() {
        return this.mIsEncoder;
    }

    public final String[] getSupportedTypes() {
        Set<String> typeSet = this.mCaps.keySet();
        String[] types = (String[]) typeSet.toArray(new String[typeSet.size()]);
        Arrays.sort(types);
        return types;
    }

    private static int checkPowerOfTwo(int value, String message) {
        if (((value - 1) & value) != 0) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static class Feature {
        public boolean mDefault;
        public String mName;
        public int mValue;

        public Feature(String name, int value, boolean def) {
            this.mName = name;
            this.mValue = value;
            this.mDefault = def;
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
        public static final int COLOR_FormatYUV422PackedPlanar = 23;
        public static final int COLOR_FormatYUV422PackedSemiPlanar = 40;
        public static final int COLOR_FormatYUV422Planar = 22;
        public static final int COLOR_FormatYUV422SemiPlanar = 24;
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
        private String mMime;
        private VideoCapabilities mVideoCaps;
        public CodecProfileLevel[] profileLevels;
        public static final String FEATURE_AdaptivePlayback = "adaptive-playback";
        public static final String FEATURE_SecurePlayback = "secure-playback";
        public static final String FEATURE_TunneledPlayback = "tunneled-playback";
        private static final Feature[] decoderFeatures = {new Feature(FEATURE_AdaptivePlayback, 1, true), new Feature(FEATURE_SecurePlayback, 2, false), new Feature(FEATURE_TunneledPlayback, 4, false)};

        public CodecCapabilities() {
        }

        public final boolean isFeatureSupported(String name) {
            return checkFeature(name, this.mFlagsSupported);
        }

        public final boolean isFeatureRequired(String name) {
            return checkFeature(name, this.mFlagsRequired);
        }

        public String[] validFeatures() {
            Feature[] features = getValidFeatures();
            String[] res = new String[features.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = features[i].mName;
            }
            return res;
        }

        private Feature[] getValidFeatures() {
            return !isEncoder() ? decoderFeatures : new Feature[0];
        }

        private boolean checkFeature(String name, int flags) {
            Feature[] arr$ = getValidFeatures();
            for (Feature feat : arr$) {
                if (feat.mName.equals(name)) {
                    return (feat.mValue & flags) != 0;
                }
            }
            return false;
        }

        public boolean isRegular() {
            Feature[] arr$ = getValidFeatures();
            for (Feature feat : arr$) {
                if (!feat.mDefault && isFeatureRequired(feat.mName)) {
                    return false;
                }
            }
            return true;
        }

        public final boolean isFormatSupported(MediaFormat format) {
            Map<String, Object> map = format.getMap();
            String mime = (String) map.get(MediaFormat.KEY_MIME);
            if (mime != null && !this.mMime.equalsIgnoreCase(mime)) {
                return false;
            }
            Feature[] arr$ = getValidFeatures();
            for (Feature feat : arr$) {
                Integer yesNo = (Integer) map.get(MediaFormat.KEY_FEATURE_ + feat.mName);
                if (yesNo != null) {
                    if (yesNo.intValue() == 1 && !isFeatureSupported(feat.mName)) {
                        return false;
                    }
                    if (yesNo.intValue() == 0 && isFeatureRequired(feat.mName)) {
                        return false;
                    }
                }
            }
            if (this.mAudioCaps != null && !this.mAudioCaps.supportsFormat(format)) {
                return false;
            }
            if (this.mVideoCaps == null || this.mVideoCaps.supportsFormat(format)) {
                return this.mEncoderCaps == null || this.mEncoderCaps.supportsFormat(format);
            }
            return false;
        }

        public MediaFormat getDefaultFormat() {
            return this.mDefaultFormat;
        }

        public String getMimeType() {
            return this.mMime;
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
            return new CodecCapabilities((CodecProfileLevel[]) Arrays.copyOf(this.profileLevels, this.profileLevels.length), Arrays.copyOf(this.colorFormats, this.colorFormats.length), isEncoder(), this.mFlagsVerified, this.mDefaultFormat, this.mCapabilitiesInfo);
        }

        public static CodecCapabilities createFromProfileLevel(String mime, int profile, int level) {
            CodecProfileLevel pl = new CodecProfileLevel();
            pl.profile = profile;
            pl.level = level;
            MediaFormat defaultFormat = new MediaFormat();
            defaultFormat.setString(MediaFormat.KEY_MIME, mime);
            CodecCapabilities ret = new CodecCapabilities(new CodecProfileLevel[]{pl}, new int[0], true, 0, defaultFormat, new MediaFormat());
            if (ret.mError != 0) {
                return null;
            }
            return ret;
        }

        CodecCapabilities(CodecProfileLevel[] profLevs, int[] colFmts, boolean encoder, int flags, Map<String, Object> defaultFormatMap, Map<String, Object> capabilitiesMap) {
            this(profLevs, colFmts, encoder, flags, new MediaFormat(defaultFormatMap), new MediaFormat(capabilitiesMap));
        }

        CodecCapabilities(CodecProfileLevel[] profLevs, int[] colFmts, boolean encoder, int flags, MediaFormat defaultFormat, MediaFormat info) {
            Map<String, Object> map = info.getMap();
            this.profileLevels = profLevs;
            this.colorFormats = colFmts;
            this.mFlagsVerified = flags;
            this.mDefaultFormat = defaultFormat;
            this.mCapabilitiesInfo = info;
            this.mMime = this.mDefaultFormat.getString(MediaFormat.KEY_MIME);
            if (this.mMime.toLowerCase().startsWith("audio/")) {
                this.mAudioCaps = AudioCapabilities.create(info, this);
                this.mAudioCaps.setDefaultFormat(this.mDefaultFormat);
            } else if (this.mMime.toLowerCase().startsWith("video/")) {
                this.mVideoCaps = VideoCapabilities.create(info, this);
            }
            if (encoder) {
                this.mEncoderCaps = EncoderCapabilities.create(info, this);
                this.mEncoderCaps.setDefaultFormat(this.mDefaultFormat);
            }
            Feature[] arr$ = getValidFeatures();
            for (Feature feat : arr$) {
                String key = MediaFormat.KEY_FEATURE_ + feat.mName;
                Integer yesNo = (Integer) map.get(key);
                if (yesNo != null) {
                    if (yesNo.intValue() > 0) {
                        this.mFlagsRequired |= feat.mValue;
                    }
                    this.mFlagsSupported |= feat.mValue;
                    this.mDefaultFormat.setInteger(key, 1);
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

        public static AudioCapabilities create(MediaFormat info, CodecCapabilities parent) {
            AudioCapabilities caps = new AudioCapabilities();
            caps.init(info, parent);
            return caps;
        }

        public void init(MediaFormat info, CodecCapabilities parent) {
            this.mParent = parent;
            initWithPlatformLimits();
            applyLevelLimits();
            parseFromInfo(info);
        }

        private void initWithPlatformLimits() {
            this.mBitrateRange = Range.create(0, Integer.MAX_VALUE);
            this.mMaxInputChannelCount = 30;
            this.mSampleRateRanges = new Range[]{Range.create(8000, 96000)};
            this.mSampleRates = null;
        }

        private boolean supports(Integer sampleRate, Integer inputChannels) {
            if (inputChannels != null && (inputChannels.intValue() < 1 || inputChannels.intValue() > this.mMaxInputChannelCount)) {
                return false;
            }
            if (sampleRate != null) {
                int ix = Utils.binarySearchDistinctRanges(this.mSampleRateRanges, sampleRate);
                if (ix < 0) {
                    return false;
                }
            }
            return true;
        }

        public boolean isSampleRateSupported(int sampleRate) {
            return supports(Integer.valueOf(sampleRate), null);
        }

        private void limitSampleRates(int[] rates) {
            Arrays.sort(rates);
            ArrayList<Range<Integer>> ranges = new ArrayList<>();
            for (int rate : rates) {
                if (supports(Integer.valueOf(rate), null)) {
                    ranges.add(Range.create(Integer.valueOf(rate), Integer.valueOf(rate)));
                }
            }
            this.mSampleRateRanges = (Range[]) ranges.toArray(new Range[ranges.size()]);
            createDiscreteSampleRates();
        }

        private void createDiscreteSampleRates() {
            this.mSampleRates = new int[this.mSampleRateRanges.length];
            for (int i = 0; i < this.mSampleRateRanges.length; i++) {
                this.mSampleRates[i] = ((Integer) this.mSampleRateRanges[i].getLower()).intValue();
            }
        }

        private void limitSampleRates(Range<Integer>[] rateRanges) {
            Utils.sortDistinctRanges(rateRanges);
            this.mSampleRateRanges = Utils.intersectSortedDistinctRanges(this.mSampleRateRanges, rateRanges);
            for (Range<Integer> range : this.mSampleRateRanges) {
                if (!((Integer) range.getLower()).equals(range.getUpper())) {
                    this.mSampleRates = null;
                    return;
                }
            }
            createDiscreteSampleRates();
        }

        private void applyLevelLimits() {
            int[] sampleRates = null;
            Range<Integer> sampleRateRange = null;
            Range<Integer> bitRates = null;
            int maxChannels = 0;
            String mime = this.mParent.getMimeType();
            if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_MPEG)) {
                sampleRates = new int[]{8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000};
                bitRates = Range.create(8000, 320000);
                maxChannels = 2;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_NB)) {
                sampleRates = new int[]{8000};
                bitRates = Range.create(4750, 12200);
                maxChannels = 1;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_WB)) {
                sampleRates = new int[]{16000};
                bitRates = Range.create(6600, 23850);
                maxChannels = 1;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AAC)) {
                sampleRates = new int[]{7350, 8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000, 64000, 88200, 96000};
                bitRates = Range.create(8000, 510000);
                maxChannels = 48;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_VORBIS)) {
                bitRates = Range.create(32000, 500000);
                sampleRateRange = Range.create(8000, 192000);
                maxChannels = 255;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_OPUS)) {
                bitRates = Range.create(Integer.valueOf(BluetoothHealth.HEALTH_OPERATION_SUCCESS), 510000);
                sampleRates = new int[]{8000, 12000, 16000, 24000, 48000};
                maxChannels = 255;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_RAW)) {
                sampleRateRange = Range.create(1, 96000);
                bitRates = Range.create(1, 10000000);
                maxChannels = 8;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_FLAC)) {
                sampleRateRange = Range.create(1, 655350);
                maxChannels = 255;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_ALAW) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_MLAW)) {
                sampleRates = new int[]{8000};
                bitRates = Range.create(64000, 64000);
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_MSGSM)) {
                sampleRates = new int[]{8000};
                bitRates = Range.create(13000, 13000);
                maxChannels = 1;
            } else {
                Log.w(TAG, "Unsupported mime " + mime);
                this.mParent.mError |= 2;
            }
            if (sampleRates != null) {
                limitSampleRates(sampleRates);
            } else if (sampleRateRange != null) {
                limitSampleRates(new Range[]{sampleRateRange});
            }
            applyLimits(maxChannels, bitRates);
        }

        private void applyLimits(int maxInputChannels, Range<Integer> bitRates) {
            this.mMaxInputChannelCount = ((Integer) Range.create(1, Integer.valueOf(this.mMaxInputChannelCount)).clamp(Integer.valueOf(maxInputChannels))).intValue();
            if (bitRates != null) {
                this.mBitrateRange = this.mBitrateRange.intersect(bitRates);
            }
        }

        private void parseFromInfo(MediaFormat info) {
            Range<Integer> bitRates = MediaCodecInfo.POSITIVE_INTEGERS;
            if (info.containsKey("sample-rate-ranges")) {
                String[] rateStrings = info.getString("sample-rate-ranges").split(",");
                Range<Integer>[] rateRanges = new Range[rateStrings.length];
                for (int i = 0; i < rateStrings.length; i++) {
                    rateRanges[i] = Utils.parseIntRange(rateStrings[i], null);
                }
                limitSampleRates(rateRanges);
            }
            int maxInputChannels = info.containsKey("max-channel-count") ? Utils.parseIntSafely(info.getString("max-channel-count"), 30) : 30;
            if (info.containsKey("bitrate-range")) {
                bitRates = bitRates.intersect(Utils.parseIntRange(info.getString("bitrate-range"), bitRates));
            }
            applyLimits(maxInputChannels, bitRates);
        }

        public void setDefaultFormat(MediaFormat format) {
            if (((Integer) this.mBitrateRange.getLower()).equals(this.mBitrateRange.getUpper())) {
                format.setInteger(MediaFormat.KEY_BIT_RATE, ((Integer) this.mBitrateRange.getLower()).intValue());
            }
            if (this.mMaxInputChannelCount == 1) {
                format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            }
            if (this.mSampleRates != null && this.mSampleRates.length == 1) {
                format.setInteger(MediaFormat.KEY_SAMPLE_RATE, this.mSampleRates[0]);
            }
        }

        public boolean supportsFormat(MediaFormat format) {
            Map<String, Object> map = format.getMap();
            Integer sampleRate = (Integer) map.get(MediaFormat.KEY_SAMPLE_RATE);
            Integer channels = (Integer) map.get(MediaFormat.KEY_CHANNEL_COUNT);
            return supports(sampleRate, channels);
        }
    }

    public static final class VideoCapabilities {
        private static final String TAG = "VideoCapabilities";
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

        public Range<Integer> getSupportedWidthsFor(int height) {
            try {
                Range<Integer> range = this.mWidthRange;
                if (!this.mHeightRange.contains(Integer.valueOf(height)) || height % this.mHeightAlignment != 0) {
                    throw new IllegalArgumentException("unsupported height");
                }
                int heightInBlocks = Utils.divUp(height, this.mBlockHeight);
                int minWidthInBlocks = Math.max(Utils.divUp(((Integer) this.mBlockCountRange.getLower()).intValue(), heightInBlocks), (int) Math.ceil(((Rational) this.mBlockAspectRatioRange.getLower()).doubleValue() * ((double) heightInBlocks)));
                int maxWidthInBlocks = Math.min(((Integer) this.mBlockCountRange.getUpper()).intValue() / heightInBlocks, (int) (((Rational) this.mBlockAspectRatioRange.getUpper()).doubleValue() * ((double) heightInBlocks)));
                Range rangeIntersect = range.intersect(Integer.valueOf(((minWidthInBlocks - 1) * this.mBlockWidth) + this.mWidthAlignment), Integer.valueOf(this.mBlockWidth * maxWidthInBlocks));
                if (height > this.mSmallerDimensionUpperLimit) {
                    rangeIntersect = rangeIntersect.intersect(1, Integer.valueOf(this.mSmallerDimensionUpperLimit));
                }
                Range<Integer> range2 = rangeIntersect.intersect(Integer.valueOf((int) Math.ceil(((Rational) this.mAspectRatioRange.getLower()).doubleValue() * ((double) height))), Integer.valueOf((int) (((Rational) this.mAspectRatioRange.getUpper()).doubleValue() * ((double) height))));
                return range2;
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "could not get supported widths for " + height, e);
                throw new IllegalArgumentException("unsupported height");
            }
        }

        public Range<Integer> getSupportedHeightsFor(int width) {
            try {
                Range<Integer> range = this.mHeightRange;
                if (!this.mWidthRange.contains(Integer.valueOf(width)) || width % this.mWidthAlignment != 0) {
                    throw new IllegalArgumentException("unsupported width");
                }
                int widthInBlocks = Utils.divUp(width, this.mBlockWidth);
                int minHeightInBlocks = Math.max(Utils.divUp(((Integer) this.mBlockCountRange.getLower()).intValue(), widthInBlocks), (int) Math.ceil(((double) widthInBlocks) / ((Rational) this.mBlockAspectRatioRange.getUpper()).doubleValue()));
                int maxHeightInBlocks = Math.min(((Integer) this.mBlockCountRange.getUpper()).intValue() / widthInBlocks, (int) (((double) widthInBlocks) / ((Rational) this.mBlockAspectRatioRange.getLower()).doubleValue()));
                Range rangeIntersect = range.intersect(Integer.valueOf(((minHeightInBlocks - 1) * this.mBlockHeight) + this.mHeightAlignment), Integer.valueOf(this.mBlockHeight * maxHeightInBlocks));
                if (width > this.mSmallerDimensionUpperLimit) {
                    rangeIntersect = rangeIntersect.intersect(1, Integer.valueOf(this.mSmallerDimensionUpperLimit));
                }
                Range<Integer> range2 = rangeIntersect.intersect(Integer.valueOf((int) Math.ceil(((double) width) / ((Rational) this.mAspectRatioRange.getUpper()).doubleValue())), Integer.valueOf((int) (((double) width) / ((Rational) this.mAspectRatioRange.getLower()).doubleValue())));
                return range2;
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "could not get supported heights for " + width, e);
                throw new IllegalArgumentException("unsupported width");
            }
        }

        public Range<Double> getSupportedFrameRatesFor(int width, int height) {
            Range<Integer> range = this.mHeightRange;
            if (!supports(Integer.valueOf(width), Integer.valueOf(height), null)) {
                throw new IllegalArgumentException("unsupported size");
            }
            int blockCount = Utils.divUp(width, this.mBlockWidth) * Utils.divUp(height, this.mBlockHeight);
            return Range.create(Double.valueOf(Math.max(((Long) this.mBlocksPerSecondRange.getLower()).longValue() / ((double) blockCount), ((Integer) this.mFrameRateRange.getLower()).intValue())), Double.valueOf(Math.min(((Long) this.mBlocksPerSecondRange.getUpper()).longValue() / ((double) blockCount), ((Integer) this.mFrameRateRange.getUpper()).intValue())));
        }

        public boolean areSizeAndRateSupported(int width, int height, double frameRate) {
            return supports(Integer.valueOf(width), Integer.valueOf(height), Double.valueOf(frameRate));
        }

        public boolean isSizeSupported(int width, int height) {
            return supports(Integer.valueOf(width), Integer.valueOf(height), null);
        }

        private boolean supports(Integer width, Integer height, Number rate) {
            boolean ok = true;
            if (1 != 0 && width != null) {
                ok = this.mWidthRange.contains(width) && width.intValue() % this.mWidthAlignment == 0;
            }
            if (ok && height != null) {
                ok = this.mHeightRange.contains(height) && height.intValue() % this.mHeightAlignment == 0;
            }
            if (ok && rate != null) {
                ok = this.mFrameRateRange.contains(Utils.intRangeFor(rate.doubleValue()));
            }
            if (ok && height != null && width != null) {
                boolean ok2 = Math.min(height.intValue(), width.intValue()) <= this.mSmallerDimensionUpperLimit;
                int widthInBlocks = Utils.divUp(width.intValue(), this.mBlockWidth);
                int heightInBlocks = Utils.divUp(height.intValue(), this.mBlockHeight);
                int blockCount = widthInBlocks * heightInBlocks;
                boolean ok3 = ok2 && this.mBlockCountRange.contains(Integer.valueOf(blockCount)) && this.mBlockAspectRatioRange.contains(new Rational(widthInBlocks, heightInBlocks)) && this.mAspectRatioRange.contains(new Rational(width.intValue(), height.intValue()));
                if (ok3 && rate != null) {
                    double blocksPerSec = ((double) blockCount) * rate.doubleValue();
                    return this.mBlocksPerSecondRange.contains(Utils.longRangeFor(blocksPerSec));
                }
                return ok3;
            }
            return ok;
        }

        public boolean supportsFormat(MediaFormat format) {
            Map<String, Object> map = format.getMap();
            Integer width = (Integer) map.get("width");
            Integer height = (Integer) map.get("height");
            Number rate = (Number) map.get(MediaFormat.KEY_FRAME_RATE);
            return supports(width, height, rate);
        }

        private VideoCapabilities() {
        }

        public static VideoCapabilities create(MediaFormat info, CodecCapabilities parent) {
            VideoCapabilities caps = new VideoCapabilities();
            caps.init(info, parent);
            return caps;
        }

        public void init(MediaFormat info, CodecCapabilities parent) {
            this.mParent = parent;
            initWithPlatformLimits();
            applyLevelLimits();
            parseFromInfo(info);
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

        public Range<Rational> getAspectRatioRange(boolean blocks) {
            return blocks ? this.mBlockAspectRatioRange : this.mAspectRatioRange;
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

        private void parseFromInfo(MediaFormat info) {
            Map<String, Object> map = info.getMap();
            Size blockSize = new Size(this.mBlockWidth, this.mBlockHeight);
            Size alignment = new Size(this.mWidthAlignment, this.mHeightAlignment);
            Range<Integer> widths = null;
            Range<Integer> heights = null;
            Size blockSize2 = Utils.parseSize(map.get("block-size"), blockSize);
            Size alignment2 = Utils.parseSize(map.get("alignment"), alignment);
            Range<Integer> counts = Utils.parseIntRange(map.get("block-count-range"), null);
            Range<Long> blockRates = Utils.parseLongRange(map.get("blocks-per-second-range"), null);
            Object o = map.get("size-range");
            Pair<Size, Size> sizeRange = Utils.parseSizeRange(o);
            if (sizeRange != null) {
                try {
                    widths = Range.create(Integer.valueOf(sizeRange.first.getWidth()), Integer.valueOf(sizeRange.second.getWidth()));
                    heights = Range.create(Integer.valueOf(sizeRange.first.getHeight()), Integer.valueOf(sizeRange.second.getHeight()));
                } catch (IllegalArgumentException e) {
                    Log.w(TAG, "could not parse size range '" + o + "'");
                    widths = null;
                    heights = null;
                }
            }
            Integer num = 1;
            if (num.equals(map.get("feature-can-swap-width-height"))) {
                if (widths != null) {
                    this.mSmallerDimensionUpperLimit = Math.min(((Integer) widths.getUpper()).intValue(), ((Integer) heights.getUpper()).intValue());
                    heights = widths.extend(heights);
                    widths = heights;
                } else {
                    Log.w(TAG, "feature can-swap-width-height is best used with size-range");
                    this.mSmallerDimensionUpperLimit = Math.min(((Integer) this.mWidthRange.getUpper()).intValue(), ((Integer) this.mHeightRange.getUpper()).intValue());
                    Range rangeExtend = this.mWidthRange.extend(this.mHeightRange);
                    this.mHeightRange = rangeExtend;
                    this.mWidthRange = rangeExtend;
                }
            }
            Range<Rational> ratios = Utils.parseRationalRange(map.get("block-aspect-ratio-range"), null);
            Range<Rational> blockRatios = Utils.parseRationalRange(map.get("pixel-aspect-ratio-range"), null);
            Range<Integer> frameRates = Utils.parseIntRange(map.get("frame-rate-range"), null);
            if (frameRates != null) {
                try {
                    frameRates = frameRates.intersect(MediaCodecInfo.FRAME_RATE_RANGE);
                } catch (IllegalArgumentException e2) {
                    Log.w(TAG, "frame rate range (" + frameRates + ") is out of limits: " + MediaCodecInfo.FRAME_RATE_RANGE);
                    frameRates = null;
                }
            }
            Range<Integer> bitRates = Utils.parseIntRange(map.get("bitrate-range"), null);
            if (bitRates != null) {
                try {
                    bitRates = bitRates.intersect(MediaCodecInfo.BITRATE_RANGE);
                } catch (IllegalArgumentException e3) {
                    Log.w(TAG, "bitrate range (" + bitRates + ") is out of limits: " + MediaCodecInfo.BITRATE_RANGE);
                    bitRates = null;
                }
            }
            MediaCodecInfo.checkPowerOfTwo(blockSize2.getWidth(), "block-size width must be power of two");
            MediaCodecInfo.checkPowerOfTwo(blockSize2.getHeight(), "block-size height must be power of two");
            MediaCodecInfo.checkPowerOfTwo(alignment2.getWidth(), "alignment width must be power of two");
            MediaCodecInfo.checkPowerOfTwo(alignment2.getHeight(), "alignment height must be power of two");
            applyMacroBlockLimits(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE, blockSize2.getWidth(), blockSize2.getHeight(), alignment2.getWidth(), alignment2.getHeight());
            if ((this.mParent.mError & 2) != 0) {
                if (widths != null) {
                    this.mWidthRange = MediaCodecInfo.SIZE_RANGE.intersect(widths);
                }
                if (heights != null) {
                    this.mHeightRange = MediaCodecInfo.SIZE_RANGE.intersect(heights);
                }
                if (counts != null) {
                    this.mBlockCountRange = MediaCodecInfo.POSITIVE_INTEGERS.intersect(Utils.factorRange(counts, ((this.mBlockWidth * this.mBlockHeight) / blockSize2.getWidth()) / blockSize2.getHeight()));
                }
                if (blockRates != null) {
                    this.mBlocksPerSecondRange = MediaCodecInfo.POSITIVE_LONGS.intersect(Utils.factorRange(blockRates, ((this.mBlockWidth * this.mBlockHeight) / blockSize2.getWidth()) / blockSize2.getHeight()));
                }
                if (blockRatios != null) {
                    this.mBlockAspectRatioRange = MediaCodecInfo.POSITIVE_RATIONALS.intersect(Utils.scaleRange(blockRatios, this.mBlockHeight / blockSize2.getHeight(), this.mBlockWidth / blockSize2.getWidth()));
                }
                if (ratios != null) {
                    this.mAspectRatioRange = MediaCodecInfo.POSITIVE_RATIONALS.intersect(ratios);
                }
                if (frameRates != null) {
                    this.mFrameRateRange = MediaCodecInfo.FRAME_RATE_RANGE.intersect(frameRates);
                }
                if (bitRates != null) {
                    this.mBitrateRange = MediaCodecInfo.BITRATE_RANGE.intersect(bitRates);
                }
            } else {
                if (widths != null) {
                    this.mWidthRange = this.mWidthRange.intersect(widths);
                }
                if (heights != null) {
                    this.mHeightRange = this.mHeightRange.intersect(heights);
                }
                if (counts != null) {
                    this.mBlockCountRange = this.mBlockCountRange.intersect(Utils.factorRange(counts, ((this.mBlockWidth * this.mBlockHeight) / blockSize2.getWidth()) / blockSize2.getHeight()));
                }
                if (blockRates != null) {
                    this.mBlocksPerSecondRange = this.mBlocksPerSecondRange.intersect(Utils.factorRange(blockRates, ((this.mBlockWidth * this.mBlockHeight) / blockSize2.getWidth()) / blockSize2.getHeight()));
                }
                if (blockRatios != null) {
                    this.mBlockAspectRatioRange = this.mBlockAspectRatioRange.intersect(Utils.scaleRange(blockRatios, this.mBlockHeight / blockSize2.getHeight(), this.mBlockWidth / blockSize2.getWidth()));
                }
                if (ratios != null) {
                    this.mAspectRatioRange = this.mAspectRatioRange.intersect(ratios);
                }
                if (frameRates != null) {
                    this.mFrameRateRange = this.mFrameRateRange.intersect(frameRates);
                }
                if (bitRates != null) {
                    this.mBitrateRange = this.mBitrateRange.intersect(bitRates);
                }
            }
            updateLimits();
        }

        private void applyBlockLimits(int blockWidth, int blockHeight, Range<Integer> counts, Range<Long> rates, Range<Rational> ratios) {
            MediaCodecInfo.checkPowerOfTwo(blockWidth, "blockWidth must be a power of two");
            MediaCodecInfo.checkPowerOfTwo(blockHeight, "blockHeight must be a power of two");
            int newBlockWidth = Math.max(blockWidth, this.mBlockWidth);
            int newBlockHeight = Math.max(blockHeight, this.mBlockHeight);
            int factor = ((newBlockWidth * newBlockHeight) / this.mBlockWidth) / this.mBlockHeight;
            if (factor != 1) {
                this.mBlockCountRange = Utils.factorRange(this.mBlockCountRange, factor);
                this.mBlocksPerSecondRange = Utils.factorRange(this.mBlocksPerSecondRange, factor);
                this.mBlockAspectRatioRange = Utils.scaleRange(this.mBlockAspectRatioRange, newBlockHeight / this.mBlockHeight, newBlockWidth / this.mBlockWidth);
                this.mHorizontalBlockRange = Utils.factorRange(this.mHorizontalBlockRange, newBlockWidth / this.mBlockWidth);
                this.mVerticalBlockRange = Utils.factorRange(this.mVerticalBlockRange, newBlockHeight / this.mBlockHeight);
            }
            int factor2 = ((newBlockWidth * newBlockHeight) / blockWidth) / blockHeight;
            if (factor2 != 1) {
                counts = Utils.factorRange(counts, factor2);
                rates = Utils.factorRange(rates, factor2);
                ratios = Utils.scaleRange(ratios, newBlockHeight / blockHeight, newBlockWidth / blockWidth);
            }
            this.mBlockCountRange = this.mBlockCountRange.intersect(counts);
            this.mBlocksPerSecondRange = this.mBlocksPerSecondRange.intersect(rates);
            this.mBlockAspectRatioRange = this.mBlockAspectRatioRange.intersect(ratios);
            this.mBlockWidth = newBlockWidth;
            this.mBlockHeight = newBlockHeight;
        }

        private void applyAlignment(int widthAlignment, int heightAlignment) {
            MediaCodecInfo.checkPowerOfTwo(widthAlignment, "widthAlignment must be a power of two");
            MediaCodecInfo.checkPowerOfTwo(heightAlignment, "heightAlignment must be a power of two");
            if (widthAlignment > this.mBlockWidth || heightAlignment > this.mBlockHeight) {
                applyBlockLimits(Math.max(widthAlignment, this.mBlockWidth), Math.max(heightAlignment, this.mBlockHeight), MediaCodecInfo.POSITIVE_INTEGERS, MediaCodecInfo.POSITIVE_LONGS, MediaCodecInfo.POSITIVE_RATIONALS);
            }
            this.mWidthAlignment = Math.max(widthAlignment, this.mWidthAlignment);
            this.mHeightAlignment = Math.max(heightAlignment, this.mHeightAlignment);
            this.mWidthRange = Utils.alignRange(this.mWidthRange, this.mWidthAlignment);
            this.mHeightRange = Utils.alignRange(this.mHeightRange, this.mHeightAlignment);
        }

        private void updateLimits() {
            this.mHorizontalBlockRange = this.mHorizontalBlockRange.intersect(Utils.factorRange(this.mWidthRange, this.mBlockWidth));
            this.mHorizontalBlockRange = this.mHorizontalBlockRange.intersect(Range.create(Integer.valueOf(((Integer) this.mBlockCountRange.getLower()).intValue() / ((Integer) this.mVerticalBlockRange.getUpper()).intValue()), Integer.valueOf(((Integer) this.mBlockCountRange.getUpper()).intValue() / ((Integer) this.mVerticalBlockRange.getLower()).intValue())));
            this.mVerticalBlockRange = this.mVerticalBlockRange.intersect(Utils.factorRange(this.mHeightRange, this.mBlockHeight));
            this.mVerticalBlockRange = this.mVerticalBlockRange.intersect(Range.create(Integer.valueOf(((Integer) this.mBlockCountRange.getLower()).intValue() / ((Integer) this.mHorizontalBlockRange.getUpper()).intValue()), Integer.valueOf(((Integer) this.mBlockCountRange.getUpper()).intValue() / ((Integer) this.mHorizontalBlockRange.getLower()).intValue())));
            this.mBlockCountRange = this.mBlockCountRange.intersect(Range.create(Integer.valueOf(((Integer) this.mVerticalBlockRange.getLower()).intValue() * ((Integer) this.mHorizontalBlockRange.getLower()).intValue()), Integer.valueOf(((Integer) this.mVerticalBlockRange.getUpper()).intValue() * ((Integer) this.mHorizontalBlockRange.getUpper()).intValue())));
            this.mBlockAspectRatioRange = this.mBlockAspectRatioRange.intersect(new Rational(((Integer) this.mHorizontalBlockRange.getLower()).intValue(), ((Integer) this.mVerticalBlockRange.getUpper()).intValue()), new Rational(((Integer) this.mHorizontalBlockRange.getUpper()).intValue(), ((Integer) this.mVerticalBlockRange.getLower()).intValue()));
            this.mWidthRange = this.mWidthRange.intersect(Integer.valueOf(((((Integer) this.mHorizontalBlockRange.getLower()).intValue() - 1) * this.mBlockWidth) + this.mWidthAlignment), Integer.valueOf(((Integer) this.mHorizontalBlockRange.getUpper()).intValue() * this.mBlockWidth));
            this.mHeightRange = this.mHeightRange.intersect(Integer.valueOf(((((Integer) this.mVerticalBlockRange.getLower()).intValue() - 1) * this.mBlockHeight) + this.mHeightAlignment), Integer.valueOf(((Integer) this.mVerticalBlockRange.getUpper()).intValue() * this.mBlockHeight));
            this.mAspectRatioRange = this.mAspectRatioRange.intersect(new Rational(((Integer) this.mWidthRange.getLower()).intValue(), ((Integer) this.mHeightRange.getUpper()).intValue()), new Rational(((Integer) this.mWidthRange.getUpper()).intValue(), ((Integer) this.mHeightRange.getLower()).intValue()));
            this.mSmallerDimensionUpperLimit = Math.min(this.mSmallerDimensionUpperLimit, Math.min(((Integer) this.mWidthRange.getUpper()).intValue(), ((Integer) this.mHeightRange.getUpper()).intValue()));
            this.mBlocksPerSecondRange = this.mBlocksPerSecondRange.intersect(Long.valueOf(((long) ((Integer) this.mBlockCountRange.getLower()).intValue()) * ((long) ((Integer) this.mFrameRateRange.getLower()).intValue())), Long.valueOf(((long) ((Integer) this.mBlockCountRange.getUpper()).intValue()) * ((long) ((Integer) this.mFrameRateRange.getUpper()).intValue())));
            this.mFrameRateRange = this.mFrameRateRange.intersect(Integer.valueOf((int) (((Long) this.mBlocksPerSecondRange.getLower()).longValue() / ((long) ((Integer) this.mBlockCountRange.getUpper()).intValue()))), Integer.valueOf((int) (((Long) this.mBlocksPerSecondRange.getUpper()).longValue() / ((double) ((Integer) this.mBlockCountRange.getLower()).intValue()))));
        }

        private void applyMacroBlockLimits(int maxHorizontalBlocks, int maxVerticalBlocks, int maxBlocks, long maxBlocksPerSecond, int blockWidth, int blockHeight, int widthAlignment, int heightAlignment) {
            applyAlignment(widthAlignment, heightAlignment);
            applyBlockLimits(blockWidth, blockHeight, Range.create(1, Integer.valueOf(maxBlocks)), Range.create(1L, Long.valueOf(maxBlocksPerSecond)), Range.create(new Rational(1, maxVerticalBlocks), new Rational(maxHorizontalBlocks, 1)));
            this.mHorizontalBlockRange = this.mHorizontalBlockRange.intersect(1, Integer.valueOf(maxHorizontalBlocks / (this.mBlockWidth / blockWidth)));
            this.mVerticalBlockRange = this.mVerticalBlockRange.intersect(1, Integer.valueOf(maxVerticalBlocks / (this.mBlockHeight / blockHeight)));
        }

        private void applyLevelLimits() {
            int maxBps;
            int BR;
            int errors = 4;
            CodecProfileLevel[] profileLevels = this.mParent.profileLevels;
            String mime = this.mParent.getMimeType();
            if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_AVC)) {
                int maxBlocks = 99;
                int maxBlocksPerSecond = 1485;
                maxBps = 64000;
                int maxDPBBlocks = 396;
                for (CodecProfileLevel profileLevel : profileLevels) {
                    int MBPS = 0;
                    int FS = 0;
                    int BR2 = 0;
                    int DPB = 0;
                    boolean supported = true;
                    switch (profileLevel.level) {
                        case 1:
                            MBPS = 1485;
                            FS = 99;
                            BR2 = 64;
                            DPB = 396;
                            break;
                        case 2:
                            MBPS = 1485;
                            FS = 99;
                            BR2 = 128;
                            DPB = 396;
                            break;
                        case 4:
                            MBPS = 3000;
                            FS = 396;
                            BR2 = 192;
                            DPB = MediaPlayer.MEDIA_INFO_TIMED_TEXT_ERROR;
                            break;
                        case 8:
                            MBPS = BluetoothHealth.HEALTH_OPERATION_SUCCESS;
                            FS = 396;
                            BR2 = 384;
                            DPB = 2376;
                            break;
                        case 16:
                            MBPS = 11880;
                            FS = 396;
                            BR2 = 768;
                            DPB = 2376;
                            break;
                        case 32:
                            MBPS = 11880;
                            FS = 396;
                            BR2 = 2000;
                            DPB = 2376;
                            break;
                        case 64:
                            MBPS = 19800;
                            FS = 792;
                            BR2 = 4000;
                            DPB = 4752;
                            break;
                        case 128:
                            MBPS = 20250;
                            FS = 1620;
                            BR2 = 4000;
                            DPB = 8100;
                            break;
                        case 256:
                            MBPS = 40500;
                            FS = 1620;
                            BR2 = 10000;
                            DPB = 8100;
                            break;
                        case 512:
                            MBPS = 108000;
                            FS = 3600;
                            BR2 = 14000;
                            DPB = 18000;
                            break;
                        case 1024:
                            MBPS = 216000;
                            FS = 5120;
                            BR2 = Window.PROGRESS_SECONDARY_START;
                            DPB = MtpConstants.DEVICE_PROPERTY_UNDEFINED;
                            break;
                        case 2048:
                            MBPS = 245760;
                            FS = 8192;
                            BR2 = Window.PROGRESS_SECONDARY_START;
                            DPB = 32768;
                            break;
                        case 4096:
                            MBPS = 245760;
                            FS = 8192;
                            BR2 = 50000;
                            DPB = 32768;
                            break;
                        case 8192:
                            MBPS = 522240;
                            FS = 8704;
                            BR2 = 50000;
                            DPB = GLES20.GL_STENCIL_BACK_FUNC;
                            break;
                        case 16384:
                            MBPS = 589824;
                            FS = 22080;
                            BR2 = 135000;
                            DPB = 110400;
                            break;
                        case 32768:
                            MBPS = SurfaceControl.FX_SURFACE_MASK;
                            FS = 36864;
                            BR2 = 240000;
                            DPB = 184320;
                            break;
                        case 65536:
                            MBPS = 2073600;
                            FS = 36864;
                            BR2 = 240000;
                            DPB = 184320;
                            break;
                        default:
                            Log.w(TAG, "Unrecognized level " + profileLevel.level + " for " + mime);
                            errors |= 1;
                            break;
                    }
                    switch (profileLevel.profile) {
                        case 4:
                        case 32:
                        case 64:
                            Log.w(TAG, "Unsupported profile " + profileLevel.profile + " for " + mime);
                            errors |= 2;
                            supported = false;
                        case 1:
                        case 2:
                            BR = BR2 * 1000;
                            break;
                        case 8:
                            BR = BR2 * 1250;
                            break;
                        case 16:
                            BR = BR2 * 3000;
                            break;
                        default:
                            Log.w(TAG, "Unrecognized profile " + profileLevel.profile + " for " + mime);
                            errors |= 1;
                            BR = BR2 * 1000;
                            break;
                    }
                    if (supported) {
                        errors &= -5;
                    }
                    maxBlocksPerSecond = Math.max(MBPS, maxBlocksPerSecond);
                    maxBlocks = Math.max(FS, maxBlocks);
                    maxBps = Math.max(BR, maxBps);
                    maxDPBBlocks = Math.max(maxDPBBlocks, DPB);
                }
                int maxLengthInBlocks = (int) Math.sqrt(maxBlocks * 8);
                applyMacroBlockLimits(maxLengthInBlocks, maxLengthInBlocks, maxBlocks, maxBlocksPerSecond, 16, 16, 1, 1);
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_MPEG4)) {
                int maxWidth = 11;
                int maxHeight = 9;
                int maxRate = 15;
                int maxBlocks2 = 99;
                int maxBlocksPerSecond2 = 1485;
                maxBps = 64000;
                for (CodecProfileLevel profileLevel2 : profileLevels) {
                    int MBPS2 = 0;
                    int FS2 = 0;
                    int BR3 = 0;
                    int FR = 0;
                    int W = 0;
                    int H = 0;
                    boolean supported2 = true;
                    switch (profileLevel2.profile) {
                        case 1:
                            switch (profileLevel2.level) {
                                case 1:
                                    FR = 15;
                                    W = 11;
                                    H = 9;
                                    MBPS2 = 1485;
                                    FS2 = 99;
                                    BR3 = 64;
                                    break;
                                case 2:
                                    FR = 30;
                                    W = 11;
                                    H = 9;
                                    MBPS2 = 1485;
                                    FS2 = 99;
                                    BR3 = 128;
                                    break;
                                case 4:
                                    FR = 30;
                                    W = 11;
                                    H = 9;
                                    MBPS2 = 1485;
                                    FS2 = 99;
                                    BR3 = 64;
                                    break;
                                case 8:
                                    FR = 30;
                                    W = 22;
                                    H = 18;
                                    MBPS2 = 5940;
                                    FS2 = 396;
                                    BR3 = 128;
                                    break;
                                case 16:
                                    FR = 30;
                                    W = 22;
                                    H = 18;
                                    MBPS2 = 11880;
                                    FS2 = 396;
                                    BR3 = 384;
                                    break;
                                case 32:
                                case 64:
                                case 128:
                                    FR = 30;
                                    W = 22;
                                    H = 18;
                                    MBPS2 = 11880;
                                    FS2 = 396;
                                    BR3 = 384;
                                    supported2 = false;
                                    break;
                                default:
                                    Log.w(TAG, "Unrecognized profile/level " + profileLevel2.profile + "/" + profileLevel2.level + " for " + mime);
                                    errors |= 1;
                                    break;
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
                            Log.i(TAG, "Unsupported profile " + profileLevel2.profile + " for " + mime);
                            errors |= 2;
                            supported2 = false;
                            break;
                        case 32768:
                            switch (profileLevel2.level) {
                                case 1:
                                case 4:
                                    FR = 30;
                                    W = 11;
                                    H = 9;
                                    MBPS2 = 2970;
                                    FS2 = 99;
                                    BR3 = 128;
                                    break;
                                case 8:
                                    FR = 30;
                                    W = 22;
                                    H = 18;
                                    MBPS2 = 5940;
                                    FS2 = 396;
                                    BR3 = 384;
                                    break;
                                case 16:
                                    FR = 30;
                                    W = 22;
                                    H = 18;
                                    MBPS2 = 11880;
                                    FS2 = 396;
                                    BR3 = 768;
                                    break;
                                case 32:
                                case 64:
                                    FR = 30;
                                    W = 44;
                                    H = 36;
                                    MBPS2 = 23760;
                                    FS2 = 792;
                                    BR3 = 3000;
                                    break;
                                case 128:
                                    FR = 30;
                                    W = 45;
                                    H = 36;
                                    MBPS2 = 48600;
                                    FS2 = 1620;
                                    BR3 = 8000;
                                    break;
                                default:
                                    Log.w(TAG, "Unrecognized profile/level " + profileLevel2.profile + "/" + profileLevel2.level + " for " + mime);
                                    errors |= 1;
                                    break;
                            }
                            break;
                        default:
                            Log.w(TAG, "Unrecognized profile " + profileLevel2.profile + " for " + mime);
                            errors |= 1;
                            break;
                    }
                    if (supported2) {
                        errors &= -5;
                    }
                    maxBlocksPerSecond2 = Math.max(MBPS2, maxBlocksPerSecond2);
                    maxBlocks2 = Math.max(FS2, maxBlocks2);
                    maxBps = Math.max(BR3 * 1000, maxBps);
                    maxWidth = Math.max(W, maxWidth);
                    maxHeight = Math.max(H, maxHeight);
                    maxRate = Math.max(FR, maxRate);
                }
                applyMacroBlockLimits(maxWidth, maxHeight, maxBlocks2, maxBlocksPerSecond2, 16, 16, 1, 1);
                this.mFrameRateRange = this.mFrameRateRange.intersect(12, Integer.valueOf(maxRate));
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_H263)) {
                int maxWidth2 = 11;
                int maxHeight2 = 9;
                int maxRate2 = 15;
                int maxBlocks3 = 99;
                int maxBlocksPerSecond3 = 1485;
                maxBps = 64000;
                for (CodecProfileLevel profileLevel3 : profileLevels) {
                    int MBPS3 = 0;
                    int BR4 = 0;
                    int FR2 = 0;
                    int W2 = 0;
                    int H2 = 0;
                    switch (profileLevel3.level) {
                        case 1:
                            FR2 = 15;
                            W2 = 11;
                            H2 = 9;
                            BR4 = 1;
                            MBPS3 = 15 * 99;
                            break;
                        case 2:
                            FR2 = 30;
                            W2 = 22;
                            H2 = 18;
                            BR4 = 2;
                            MBPS3 = 30 * 396;
                            break;
                        case 4:
                            FR2 = 30;
                            W2 = 22;
                            H2 = 18;
                            BR4 = 6;
                            MBPS3 = 30 * 396;
                            break;
                        case 8:
                            FR2 = 30;
                            W2 = 22;
                            H2 = 18;
                            BR4 = 32;
                            MBPS3 = 30 * 396;
                            break;
                        case 16:
                            FR2 = 30;
                            W2 = 11;
                            H2 = 9;
                            BR4 = 2;
                            MBPS3 = 30 * 99;
                            break;
                        case 32:
                            FR2 = 60;
                            W2 = 22;
                            H2 = 18;
                            BR4 = 64;
                            MBPS3 = 396 * 50;
                            break;
                        case 64:
                            FR2 = 60;
                            W2 = 45;
                            H2 = 18;
                            BR4 = 128;
                            MBPS3 = 810 * 50;
                            break;
                        case 128:
                            FR2 = 60;
                            W2 = 45;
                            H2 = 36;
                            BR4 = 256;
                            MBPS3 = 1620 * 50;
                            break;
                        default:
                            Log.w(TAG, "Unrecognized profile/level " + profileLevel3.profile + "/" + profileLevel3.level + " for " + mime);
                            errors |= 1;
                            break;
                    }
                    switch (profileLevel3.profile) {
                        case 1:
                        case 2:
                        case 4:
                        case 8:
                        case 16:
                        case 32:
                        case 64:
                        case 128:
                        case 256:
                            break;
                        default:
                            Log.w(TAG, "Unrecognized profile " + profileLevel3.profile + " for " + mime);
                            errors |= 1;
                            break;
                    }
                    errors &= -5;
                    maxBlocksPerSecond3 = Math.max(MBPS3, maxBlocksPerSecond3);
                    maxBlocks3 = Math.max(W2 * H2, maxBlocks3);
                    maxBps = Math.max(64000 * BR4, maxBps);
                    maxWidth2 = Math.max(W2, maxWidth2);
                    maxHeight2 = Math.max(H2, maxHeight2);
                    maxRate2 = Math.max(FR2, maxRate2);
                }
                applyMacroBlockLimits(maxWidth2, maxHeight2, maxBlocks3, maxBlocksPerSecond3, 16, 16, 1, 1);
                this.mFrameRateRange = Range.create(1, Integer.valueOf(maxRate2));
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_VP8) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_VP9)) {
                maxBps = 100000000;
                for (CodecProfileLevel profileLevel4 : profileLevels) {
                    switch (profileLevel4.level) {
                        case 1:
                        case 2:
                        case 4:
                        case 8:
                            break;
                        case 3:
                        case 5:
                        case 6:
                        case 7:
                        default:
                            Log.w(TAG, "Unrecognized level " + profileLevel4.level + " for " + mime);
                            errors |= 1;
                            break;
                    }
                    switch (profileLevel4.profile) {
                        case 1:
                            break;
                        default:
                            Log.w(TAG, "Unrecognized profile " + profileLevel4.profile + " for " + mime);
                            errors |= 1;
                            break;
                    }
                    errors &= -5;
                }
                int blockSize = mime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_VP8) ? 16 : 8;
                applyMacroBlockLimits(32767, 32767, Integer.MAX_VALUE, Integer.MAX_VALUE, blockSize, blockSize, 1, 1);
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_VIDEO_HEVC)) {
                int maxBlocks4 = 36864;
                int maxBlocksPerSecond4 = 36864 * 15;
                maxBps = 128000;
                for (CodecProfileLevel profileLevel5 : profileLevels) {
                    double FR3 = 0.0d;
                    int FS3 = 0;
                    int BR5 = 0;
                    switch (profileLevel5.level) {
                        case 1:
                        case 2:
                            FR3 = 15.0d;
                            FS3 = 36864;
                            BR5 = 128;
                            break;
                        case 4:
                        case 8:
                            FR3 = 30.0d;
                            FS3 = 122880;
                            BR5 = 1500;
                            break;
                        case 16:
                        case 32:
                            FR3 = 30.0d;
                            FS3 = 245760;
                            BR5 = 3000;
                            break;
                        case 64:
                        case 128:
                            FR3 = 30.0d;
                            FS3 = 552960;
                            BR5 = BluetoothHealth.HEALTH_OPERATION_SUCCESS;
                            break;
                        case 256:
                        case 512:
                            FR3 = 33.75d;
                            FS3 = SurfaceControl.FX_SURFACE_MASK;
                            BR5 = 10000;
                            break;
                        case 1024:
                            FR3 = 30.0d;
                            FS3 = 2228224;
                            BR5 = 12000;
                            break;
                        case 2048:
                            FR3 = 30.0d;
                            FS3 = 2228224;
                            BR5 = Window.PROGRESS_SECONDARY_END;
                            break;
                        case 4096:
                            FR3 = 60.0d;
                            FS3 = 2228224;
                            BR5 = Window.PROGRESS_SECONDARY_START;
                            break;
                        case 8192:
                            FR3 = 60.0d;
                            FS3 = 2228224;
                            BR5 = 50000;
                            break;
                        case 16384:
                            FR3 = 30.0d;
                            FS3 = 8912896;
                            BR5 = 25000;
                            break;
                        case 32768:
                            FR3 = 30.0d;
                            FS3 = 8912896;
                            BR5 = UserHandle.PER_USER_RANGE;
                            break;
                        case 65536:
                            FR3 = 60.0d;
                            FS3 = 8912896;
                            BR5 = 40000;
                            break;
                        case 131072:
                            FR3 = 60.0d;
                            FS3 = 8912896;
                            BR5 = Protocol.BASE_WIFI_SCANNER_SERVICE;
                            break;
                        case 262144:
                            FR3 = 120.0d;
                            FS3 = 8912896;
                            BR5 = 60000;
                            break;
                        case 524288:
                            FR3 = 120.0d;
                            FS3 = 8912896;
                            BR5 = 240000;
                            break;
                        case 1048576:
                            FR3 = 30.0d;
                            FS3 = 35651584;
                            BR5 = 60000;
                            break;
                        case 2097152:
                            FR3 = 30.0d;
                            FS3 = 35651584;
                            BR5 = 240000;
                            break;
                        case 4194304:
                            FR3 = 60.0d;
                            FS3 = 35651584;
                            BR5 = 120000;
                            break;
                        case 8388608:
                            FR3 = 60.0d;
                            FS3 = 35651584;
                            BR5 = 480000;
                            break;
                        case 16777216:
                            FR3 = 120.0d;
                            FS3 = 35651584;
                            BR5 = 240000;
                            break;
                        case 33554432:
                            FR3 = 120.0d;
                            FS3 = 35651584;
                            BR5 = 800000;
                            break;
                        default:
                            Log.w(TAG, "Unrecognized level " + profileLevel5.level + " for " + mime);
                            errors |= 1;
                            break;
                    }
                    switch (profileLevel5.profile) {
                        case 1:
                        case 2:
                            break;
                        default:
                            Log.w(TAG, "Unrecognized profile " + profileLevel5.profile + " for " + mime);
                            errors |= 1;
                            break;
                    }
                    errors &= -5;
                    maxBlocksPerSecond4 = Math.max((int) (((double) FS3) * FR3), maxBlocksPerSecond4);
                    maxBlocks4 = Math.max(FS3, maxBlocks4);
                    maxBps = Math.max(BR5 * 1000, maxBps);
                }
                int maxLengthInBlocks2 = (int) Math.sqrt(maxBlocks4 * 8);
                int maxBlocks5 = Utils.divUp(maxBlocks4, 64);
                int maxBlocksPerSecond5 = Utils.divUp(maxBlocksPerSecond4, 64);
                int maxLengthInBlocks3 = Utils.divUp(maxLengthInBlocks2, 8);
                applyMacroBlockLimits(maxLengthInBlocks3, maxLengthInBlocks3, maxBlocks5, maxBlocksPerSecond5, 8, 8, 1, 1);
            } else {
                Log.w(TAG, "Unsupported mime " + mime);
                maxBps = 64000;
                errors = 4 | 2;
            }
            this.mBitrateRange = Range.create(1, Integer.valueOf(maxBps));
            this.mParent.mError |= errors;
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

        private static int parseBitrateMode(String mode) {
            Feature[] arr$ = bitrates;
            for (Feature feat : arr$) {
                if (feat.mName.equalsIgnoreCase(mode)) {
                    return feat.mValue;
                }
            }
            return 0;
        }

        public boolean isBitrateModeSupported(int mode) {
            Feature[] arr$ = bitrates;
            for (Feature feat : arr$) {
                if (mode == feat.mValue) {
                    return (this.mBitControl & (1 << mode)) != 0;
                }
            }
            return false;
        }

        private EncoderCapabilities() {
        }

        public static EncoderCapabilities create(MediaFormat info, CodecCapabilities parent) {
            EncoderCapabilities caps = new EncoderCapabilities();
            caps.init(info, parent);
            return caps;
        }

        public void init(MediaFormat info, CodecCapabilities parent) {
            this.mParent = parent;
            this.mComplexityRange = Range.create(0, 0);
            this.mQualityRange = Range.create(0, 0);
            this.mBitControl = 2;
            applyLevelLimits();
            parseFromInfo(info);
        }

        private void applyLevelLimits() {
            String mime = this.mParent.getMimeType();
            if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_FLAC)) {
                this.mComplexityRange = Range.create(0, 8);
                this.mBitControl = 1;
            } else if (mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_NB) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_AMR_WB) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_ALAW) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_G711_MLAW) || mime.equalsIgnoreCase(MediaFormat.MIMETYPE_AUDIO_MSGSM)) {
                this.mBitControl = 4;
            }
        }

        private void parseFromInfo(MediaFormat info) {
            Map<String, Object> map = info.getMap();
            if (info.containsKey("complexity-range")) {
                this.mComplexityRange = Utils.parseIntRange(info.getString("complexity-range"), this.mComplexityRange);
            }
            if (info.containsKey("quality-range")) {
                this.mQualityRange = Utils.parseIntRange(info.getString("quality-range"), this.mQualityRange);
            }
            if (info.containsKey("feature-bitrate-control")) {
                String[] arr$ = info.getString("feature-bitrate-control").split(",");
                for (String mode : arr$) {
                    this.mBitControl |= parseBitrateMode(mode);
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

        private boolean supports(Integer complexity, Integer quality, Integer profile) {
            boolean ok = true;
            if (1 != 0 && complexity != null) {
                ok = this.mComplexityRange.contains(complexity);
            }
            if (ok && quality != null) {
                ok = this.mQualityRange.contains(quality);
            }
            if (ok && profile != null) {
                CodecProfileLevel[] arr$ = this.mParent.profileLevels;
                int len$ = arr$.length;
                int i$ = 0;
                while (true) {
                    if (i$ >= len$) {
                        break;
                    }
                    CodecProfileLevel pl = arr$[i$];
                    if (pl.profile != profile.intValue()) {
                        i$++;
                    } else {
                        profile = null;
                        break;
                    }
                }
                return profile == null;
            }
            return ok;
        }

        public void setDefaultFormat(MediaFormat format) {
            if (!((Integer) this.mQualityRange.getUpper()).equals(this.mQualityRange.getLower()) && this.mDefaultQuality != null) {
                format.setInteger(MediaFormat.KEY_QUALITY, this.mDefaultQuality.intValue());
            }
            if (!((Integer) this.mComplexityRange.getUpper()).equals(this.mComplexityRange.getLower()) && this.mDefaultComplexity != null) {
                format.setInteger(MediaFormat.KEY_COMPLEXITY, this.mDefaultComplexity.intValue());
            }
            Feature[] arr$ = bitrates;
            for (Feature feat : arr$) {
                if ((this.mBitControl & (1 << feat.mValue)) != 0) {
                    format.setInteger(MediaFormat.KEY_BITRATE_MODE, feat.mValue);
                    return;
                }
            }
        }

        public boolean supportsFormat(MediaFormat format) {
            Map<String, Object> map = format.getMap();
            String mime = this.mParent.getMimeType();
            Integer mode = (Integer) map.get(MediaFormat.KEY_BITRATE_MODE);
            if (mode != null && !isBitrateModeSupported(mode.intValue())) {
                return false;
            }
            Integer complexity = (Integer) map.get(MediaFormat.KEY_COMPLEXITY);
            if (MediaFormat.MIMETYPE_AUDIO_FLAC.equalsIgnoreCase(mime)) {
                Integer flacComplexity = (Integer) map.get(MediaFormat.KEY_FLAC_COMPRESSION_LEVEL);
                if (complexity == null) {
                    complexity = flacComplexity;
                } else if (flacComplexity != null && complexity != flacComplexity) {
                    throw new IllegalArgumentException("conflicting values for complexity and flac-compression-level");
                }
            }
            Integer profile = (Integer) map.get(MediaFormat.KEY_PROFILE);
            if (MediaFormat.MIMETYPE_AUDIO_AAC.equalsIgnoreCase(mime)) {
                Integer aacProfile = (Integer) map.get(MediaFormat.KEY_AAC_PROFILE);
                if (profile == null) {
                    profile = aacProfile;
                } else if (aacProfile != null && aacProfile != profile) {
                    throw new IllegalArgumentException("conflicting values for profile and aac-profile");
                }
            }
            Integer quality = (Integer) map.get(MediaFormat.KEY_QUALITY);
            return supports(complexity, quality, profile);
        }
    }

    public final CodecCapabilities getCapabilitiesForType(String type) {
        CodecCapabilities caps = this.mCaps.get(type);
        if (caps == null) {
            throw new IllegalArgumentException("codec does not support type");
        }
        return caps.dup();
    }

    public MediaCodecInfo makeRegular() {
        ArrayList<CodecCapabilities> caps = new ArrayList<>();
        for (CodecCapabilities c : this.mCaps.values()) {
            if (c.isRegular()) {
                caps.add(c);
            }
        }
        if (caps.size() == 0) {
            return null;
        }
        return caps.size() != this.mCaps.size() ? new MediaCodecInfo(this.mName, this.mIsEncoder, (CodecCapabilities[]) caps.toArray(new CodecCapabilities[caps.size()])) : this;
    }
}
