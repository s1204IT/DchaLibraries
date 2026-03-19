package android.media;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.mtp.MtpConstants;
import android.net.wifi.WifiScanner;
import android.opengl.GLES30;
import android.security.keymaster.KeymasterDefs;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.format.Time;
import android.util.Log;
import android.util.Pair;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.telephony.gsm.SmsCbConstants;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class ExifInterface {
    private static final short BYTE_ALIGN_II = 18761;
    private static final short BYTE_ALIGN_MM = 19789;
    private static final int DATA_DEFLATE_ZIP = 8;
    private static final int DATA_HUFFMAN_COMPRESSED = 2;
    private static final int DATA_JPEG = 6;
    private static final int DATA_JPEG_COMPRESSED = 7;
    private static final int DATA_LOSSY_JPEG = 34892;
    private static final int DATA_PACK_BITS_COMPRESSED = 32773;
    private static final int DATA_UNCOMPRESSED = 1;
    private static final boolean DEBUG = false;
    private static final ExifTag[] EXIF_POINTER_TAGS;
    private static final ExifTag[][] EXIF_TAGS;
    private static final ExifTag[] IFD_EXIF_TAGS;
    private static final int IFD_FORMAT_BYTE = 1;
    private static final int IFD_FORMAT_DOUBLE = 12;
    private static final int IFD_FORMAT_IFD = 13;
    private static final int IFD_FORMAT_SBYTE = 6;
    private static final int IFD_FORMAT_SINGLE = 11;
    private static final int IFD_FORMAT_SLONG = 9;
    private static final int IFD_FORMAT_SRATIONAL = 10;
    private static final int IFD_FORMAT_SSHORT = 8;
    private static final int IFD_FORMAT_STRING = 2;
    private static final int IFD_FORMAT_ULONG = 4;
    private static final int IFD_FORMAT_UNDEFINED = 7;
    private static final int IFD_FORMAT_URATIONAL = 5;
    private static final int IFD_FORMAT_USHORT = 3;
    private static final ExifTag[] IFD_GPS_TAGS;
    private static final ExifTag[] IFD_INTEROPERABILITY_TAGS;
    private static final int IFD_OFFSET = 8;
    private static final ExifTag[] IFD_THUMBNAIL_TAGS;
    private static final ExifTag[] IFD_TIFF_TAGS;
    private static final int IFD_TYPE_EXIF = 1;
    private static final int IFD_TYPE_GPS = 2;
    private static final int IFD_TYPE_INTEROPERABILITY = 3;
    private static final int IFD_TYPE_ORF_CAMERA_SETTINGS = 7;
    private static final int IFD_TYPE_ORF_IMAGE_PROCESSING = 8;
    private static final int IFD_TYPE_ORF_MAKER_NOTE = 6;
    private static final int IFD_TYPE_PEF = 9;
    private static final int IFD_TYPE_PREVIEW = 5;
    private static final int IFD_TYPE_PRIMARY = 0;
    private static final int IFD_TYPE_THUMBNAIL = 4;
    private static final int IMAGE_TYPE_ARW = 1;
    private static final int IMAGE_TYPE_CR2 = 2;
    private static final int IMAGE_TYPE_DNG = 3;
    private static final int IMAGE_TYPE_HEIF = 12;
    private static final int IMAGE_TYPE_JPEG = 4;
    private static final int IMAGE_TYPE_NEF = 5;
    private static final int IMAGE_TYPE_NRW = 6;
    private static final int IMAGE_TYPE_ORF = 7;
    private static final int IMAGE_TYPE_PEF = 8;
    private static final int IMAGE_TYPE_RAF = 9;
    private static final int IMAGE_TYPE_RW2 = 10;
    private static final int IMAGE_TYPE_SRW = 11;
    private static final int IMAGE_TYPE_UNKNOWN = 0;
    private static final ExifTag JPEG_INTERCHANGE_FORMAT_LENGTH_TAG;
    private static final ExifTag JPEG_INTERCHANGE_FORMAT_TAG;
    private static final byte MARKER = -1;
    private static final byte MARKER_APP1 = -31;
    private static final byte MARKER_COM = -2;
    private static final byte MARKER_EOI = -39;
    private static final byte MARKER_SOF0 = -64;
    private static final byte MARKER_SOF1 = -63;
    private static final byte MARKER_SOF10 = -54;
    private static final byte MARKER_SOF11 = -53;
    private static final byte MARKER_SOF13 = -51;
    private static final byte MARKER_SOF14 = -50;
    private static final byte MARKER_SOF15 = -49;
    private static final byte MARKER_SOF2 = -62;
    private static final byte MARKER_SOF3 = -61;
    private static final byte MARKER_SOF5 = -59;
    private static final byte MARKER_SOF6 = -58;
    private static final byte MARKER_SOF7 = -57;
    private static final byte MARKER_SOF9 = -55;
    private static final byte MARKER_SOS = -38;
    private static final int MAX_THUMBNAIL_SIZE = 512;
    private static final ExifTag[] ORF_CAMERA_SETTINGS_TAGS;
    private static final ExifTag[] ORF_IMAGE_PROCESSING_TAGS;
    private static final int ORF_MAKER_NOTE_HEADER_1_SIZE = 8;
    private static final int ORF_MAKER_NOTE_HEADER_2_SIZE = 12;
    private static final ExifTag[] ORF_MAKER_NOTE_TAGS;
    private static final short ORF_SIGNATURE_1 = 20306;
    private static final short ORF_SIGNATURE_2 = 21330;
    public static final int ORIENTATION_FLIP_HORIZONTAL = 2;
    public static final int ORIENTATION_FLIP_VERTICAL = 4;
    public static final int ORIENTATION_NORMAL = 1;
    public static final int ORIENTATION_ROTATE_180 = 3;
    public static final int ORIENTATION_ROTATE_270 = 8;
    public static final int ORIENTATION_ROTATE_90 = 6;
    public static final int ORIENTATION_TRANSPOSE = 5;
    public static final int ORIENTATION_TRANSVERSE = 7;
    public static final int ORIENTATION_UNDEFINED = 0;
    private static final int ORIGINAL_RESOLUTION_IMAGE = 0;
    private static final int PEF_MAKER_NOTE_SKIP_SIZE = 6;
    private static final String PEF_SIGNATURE = "PENTAX";
    private static final ExifTag[] PEF_TAGS;
    private static final int PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO = 1;
    private static final int PHOTOMETRIC_INTERPRETATION_RGB = 2;
    private static final int PHOTOMETRIC_INTERPRETATION_WHITE_IS_ZERO = 0;
    private static final int PHOTOMETRIC_INTERPRETATION_YCBCR = 6;
    private static final int RAF_INFO_SIZE = 160;
    private static final int RAF_JPEG_LENGTH_VALUE_SIZE = 4;
    private static final int RAF_OFFSET_TO_JPEG_IMAGE_OFFSET = 84;
    private static final String RAF_SIGNATURE = "FUJIFILMCCD-RAW";
    private static final int REDUCED_RESOLUTION_IMAGE = 1;
    private static final short RW2_SIGNATURE = 85;
    private static final int SIGNATURE_CHECK_SIZE = 5000;
    private static final byte START_CODE = 42;
    private static final String TAG = "ExifInterface";

    @Deprecated
    public static final String TAG_APERTURE = "FNumber";
    public static final String TAG_APERTURE_VALUE = "ApertureValue";
    public static final String TAG_ARTIST = "Artist";
    public static final String TAG_BITS_PER_SAMPLE = "BitsPerSample";
    public static final String TAG_BRIGHTNESS_VALUE = "BrightnessValue";
    public static final String TAG_CFA_PATTERN = "CFAPattern";
    public static final String TAG_COLOR_SPACE = "ColorSpace";
    public static final String TAG_COMPONENTS_CONFIGURATION = "ComponentsConfiguration";
    public static final String TAG_COMPRESSED_BITS_PER_PIXEL = "CompressedBitsPerPixel";
    public static final String TAG_COMPRESSION = "Compression";
    public static final String TAG_CONTRAST = "Contrast";
    public static final String TAG_COPYRIGHT = "Copyright";
    public static final String TAG_CUSTOM_RENDERED = "CustomRendered";
    public static final String TAG_DATETIME = "DateTime";
    public static final String TAG_DATETIME_DIGITIZED = "DateTimeDigitized";
    public static final String TAG_DATETIME_ORIGINAL = "DateTimeOriginal";
    public static final String TAG_DEFAULT_CROP_SIZE = "DefaultCropSize";
    public static final String TAG_DEVICE_SETTING_DESCRIPTION = "DeviceSettingDescription";
    public static final String TAG_DNG_VERSION = "DNGVersion";
    private static final String TAG_EXIF_IFD_POINTER = "ExifIFDPointer";
    public static final String TAG_EXIF_VERSION = "ExifVersion";
    public static final String TAG_EXPOSURE_BIAS_VALUE = "ExposureBiasValue";
    public static final String TAG_EXPOSURE_INDEX = "ExposureIndex";
    public static final String TAG_EXPOSURE_MODE = "ExposureMode";
    public static final String TAG_EXPOSURE_PROGRAM = "ExposureProgram";
    public static final String TAG_FILE_SOURCE = "FileSource";
    public static final String TAG_FLASH = "Flash";
    public static final String TAG_FLASHPIX_VERSION = "FlashpixVersion";
    public static final String TAG_FLASH_ENERGY = "FlashEnergy";
    public static final String TAG_FOCAL_LENGTH = "FocalLength";
    public static final String TAG_FOCAL_LENGTH_IN_35MM_FILM = "FocalLengthIn35mmFilm";
    public static final String TAG_FOCAL_PLANE_RESOLUTION_UNIT = "FocalPlaneResolutionUnit";
    public static final String TAG_FOCAL_PLANE_X_RESOLUTION = "FocalPlaneXResolution";
    public static final String TAG_FOCAL_PLANE_Y_RESOLUTION = "FocalPlaneYResolution";
    public static final String TAG_F_NUMBER = "FNumber";
    public static final String TAG_GAIN_CONTROL = "GainControl";
    public static final String TAG_GPS_ALTITUDE = "GPSAltitude";
    public static final String TAG_GPS_ALTITUDE_REF = "GPSAltitudeRef";
    public static final String TAG_GPS_AREA_INFORMATION = "GPSAreaInformation";
    public static final String TAG_GPS_DATESTAMP = "GPSDateStamp";
    public static final String TAG_GPS_DEST_BEARING = "GPSDestBearing";
    public static final String TAG_GPS_DEST_BEARING_REF = "GPSDestBearingRef";
    public static final String TAG_GPS_DEST_DISTANCE = "GPSDestDistance";
    public static final String TAG_GPS_DEST_DISTANCE_REF = "GPSDestDistanceRef";
    public static final String TAG_GPS_DEST_LATITUDE = "GPSDestLatitude";
    public static final String TAG_GPS_DEST_LATITUDE_REF = "GPSDestLatitudeRef";
    public static final String TAG_GPS_DEST_LONGITUDE = "GPSDestLongitude";
    public static final String TAG_GPS_DEST_LONGITUDE_REF = "GPSDestLongitudeRef";
    public static final String TAG_GPS_DIFFERENTIAL = "GPSDifferential";
    public static final String TAG_GPS_DOP = "GPSDOP";
    public static final String TAG_GPS_IMG_DIRECTION = "GPSImgDirection";
    public static final String TAG_GPS_IMG_DIRECTION_REF = "GPSImgDirectionRef";
    private static final String TAG_GPS_INFO_IFD_POINTER = "GPSInfoIFDPointer";
    public static final String TAG_GPS_LATITUDE = "GPSLatitude";
    public static final String TAG_GPS_LATITUDE_REF = "GPSLatitudeRef";
    public static final String TAG_GPS_LONGITUDE = "GPSLongitude";
    public static final String TAG_GPS_LONGITUDE_REF = "GPSLongitudeRef";
    public static final String TAG_GPS_MAP_DATUM = "GPSMapDatum";
    public static final String TAG_GPS_MEASURE_MODE = "GPSMeasureMode";
    public static final String TAG_GPS_PROCESSING_METHOD = "GPSProcessingMethod";
    public static final String TAG_GPS_SATELLITES = "GPSSatellites";
    public static final String TAG_GPS_SPEED = "GPSSpeed";
    public static final String TAG_GPS_SPEED_REF = "GPSSpeedRef";
    public static final String TAG_GPS_STATUS = "GPSStatus";
    public static final String TAG_GPS_TRACK = "GPSTrack";
    public static final String TAG_GPS_TRACK_REF = "GPSTrackRef";
    public static final String TAG_GPS_VERSION_ID = "GPSVersionID";
    private static final String TAG_HAS_THUMBNAIL = "HasThumbnail";
    public static final String TAG_IMAGE_DESCRIPTION = "ImageDescription";
    public static final String TAG_IMAGE_LENGTH = "ImageLength";
    public static final String TAG_IMAGE_UNIQUE_ID = "ImageUniqueID";
    public static final String TAG_IMAGE_WIDTH = "ImageWidth";
    private static final String TAG_INTEROPERABILITY_IFD_POINTER = "InteroperabilityIFDPointer";
    public static final String TAG_INTEROPERABILITY_INDEX = "InteroperabilityIndex";

    @Deprecated
    public static final String TAG_ISO = "ISOSpeedRatings";
    public static final String TAG_ISO_SPEED_RATINGS = "ISOSpeedRatings";
    public static final String TAG_JPEG_INTERCHANGE_FORMAT = "JPEGInterchangeFormat";
    public static final String TAG_JPEG_INTERCHANGE_FORMAT_LENGTH = "JPEGInterchangeFormatLength";
    public static final String TAG_LIGHT_SOURCE = "LightSource";
    public static final String TAG_MAKE = "Make";
    public static final String TAG_MAKER_NOTE = "MakerNote";
    public static final String TAG_MAX_APERTURE_VALUE = "MaxApertureValue";
    public static final String TAG_METERING_MODE = "MeteringMode";
    public static final String TAG_MODEL = "Model";
    public static final String TAG_NEW_SUBFILE_TYPE = "NewSubfileType";
    public static final String TAG_OECF = "OECF";
    public static final String TAG_ORF_ASPECT_FRAME = "AspectFrame";
    private static final String TAG_ORF_CAMERA_SETTINGS_IFD_POINTER = "CameraSettingsIFDPointer";
    private static final String TAG_ORF_IMAGE_PROCESSING_IFD_POINTER = "ImageProcessingIFDPointer";
    public static final String TAG_ORF_PREVIEW_IMAGE_LENGTH = "PreviewImageLength";
    public static final String TAG_ORF_PREVIEW_IMAGE_START = "PreviewImageStart";
    public static final String TAG_ORF_THUMBNAIL_IMAGE = "ThumbnailImage";
    public static final String TAG_ORIENTATION = "Orientation";
    public static final String TAG_PHOTOMETRIC_INTERPRETATION = "PhotometricInterpretation";
    public static final String TAG_PIXEL_X_DIMENSION = "PixelXDimension";
    public static final String TAG_PIXEL_Y_DIMENSION = "PixelYDimension";
    public static final String TAG_PLANAR_CONFIGURATION = "PlanarConfiguration";
    public static final String TAG_PRIMARY_CHROMATICITIES = "PrimaryChromaticities";
    private static final ExifTag TAG_RAF_IMAGE_SIZE;
    public static final String TAG_REFERENCE_BLACK_WHITE = "ReferenceBlackWhite";
    public static final String TAG_RELATED_SOUND_FILE = "RelatedSoundFile";
    public static final String TAG_RESOLUTION_UNIT = "ResolutionUnit";
    public static final String TAG_ROWS_PER_STRIP = "RowsPerStrip";
    public static final String TAG_RW2_ISO = "ISO";
    public static final String TAG_RW2_JPG_FROM_RAW = "JpgFromRaw";
    public static final String TAG_RW2_SENSOR_BOTTOM_BORDER = "SensorBottomBorder";
    public static final String TAG_RW2_SENSOR_LEFT_BORDER = "SensorLeftBorder";
    public static final String TAG_RW2_SENSOR_RIGHT_BORDER = "SensorRightBorder";
    public static final String TAG_RW2_SENSOR_TOP_BORDER = "SensorTopBorder";
    public static final String TAG_SAMPLES_PER_PIXEL = "SamplesPerPixel";
    public static final String TAG_SATURATION = "Saturation";
    public static final String TAG_SCENE_CAPTURE_TYPE = "SceneCaptureType";
    public static final String TAG_SCENE_TYPE = "SceneType";
    public static final String TAG_SENSING_METHOD = "SensingMethod";
    public static final String TAG_SHARPNESS = "Sharpness";
    public static final String TAG_SHUTTER_SPEED_VALUE = "ShutterSpeedValue";
    public static final String TAG_SOFTWARE = "Software";
    public static final String TAG_SPATIAL_FREQUENCY_RESPONSE = "SpatialFrequencyResponse";
    public static final String TAG_SPECTRAL_SENSITIVITY = "SpectralSensitivity";
    public static final String TAG_STRIP_BYTE_COUNTS = "StripByteCounts";
    public static final String TAG_STRIP_OFFSETS = "StripOffsets";
    public static final String TAG_SUBFILE_TYPE = "SubfileType";
    public static final String TAG_SUBJECT_AREA = "SubjectArea";
    public static final String TAG_SUBJECT_DISTANCE_RANGE = "SubjectDistanceRange";
    public static final String TAG_SUBJECT_LOCATION = "SubjectLocation";
    public static final String TAG_SUBSEC_TIME = "SubSecTime";
    public static final String TAG_SUBSEC_TIME_DIG = "SubSecTimeDigitized";
    public static final String TAG_SUBSEC_TIME_DIGITIZED = "SubSecTimeDigitized";
    public static final String TAG_SUBSEC_TIME_ORIG = "SubSecTimeOriginal";
    public static final String TAG_SUBSEC_TIME_ORIGINAL = "SubSecTimeOriginal";
    private static final String TAG_SUB_IFD_POINTER = "SubIFDPointer";
    private static final String TAG_THUMBNAIL_DATA = "ThumbnailData";
    public static final String TAG_THUMBNAIL_IMAGE_LENGTH = "ThumbnailImageLength";
    public static final String TAG_THUMBNAIL_IMAGE_WIDTH = "ThumbnailImageWidth";
    private static final String TAG_THUMBNAIL_LENGTH = "ThumbnailLength";
    private static final String TAG_THUMBNAIL_OFFSET = "ThumbnailOffset";
    public static final String TAG_TRANSFER_FUNCTION = "TransferFunction";
    public static final String TAG_USER_COMMENT = "UserComment";
    public static final String TAG_WHITE_BALANCE = "WhiteBalance";
    public static final String TAG_WHITE_POINT = "WhitePoint";
    public static final String TAG_X_RESOLUTION = "XResolution";
    public static final String TAG_Y_CB_CR_COEFFICIENTS = "YCbCrCoefficients";
    public static final String TAG_Y_CB_CR_POSITIONING = "YCbCrPositioning";
    public static final String TAG_Y_CB_CR_SUB_SAMPLING = "YCbCrSubSampling";
    public static final String TAG_Y_RESOLUTION = "YResolution";
    public static final int WHITEBALANCE_AUTO = 0;
    public static final int WHITEBALANCE_MANUAL = 1;
    private static final HashMap[] sExifTagMapsForReading;
    private static final HashMap[] sExifTagMapsForWriting;
    private static final Pattern sGpsTimestampPattern;
    private static final Pattern sNonZeroTimePattern;
    private final AssetManager.AssetInputStream mAssetInputStream;
    private final HashMap[] mAttributes;
    private Set<Integer> mAttributesOffsets;
    private ByteOrder mExifByteOrder;
    private int mExifOffset;
    private final String mFilename;
    private boolean mHasThumbnail;
    private final boolean mIsInputStream;
    private boolean mIsSupportedFile;
    private int mMimeType;
    private int mOrfMakerNoteOffset;
    private int mOrfThumbnailLength;
    private int mOrfThumbnailOffset;
    private int mRw2JpgFromRawOffset;
    private final FileDescriptor mSeekableFileDescriptor;
    private byte[] mThumbnailBytes;
    private int mThumbnailCompression;
    private int mThumbnailLength;
    private int mThumbnailOffset;
    private static final byte MARKER_SOI = -40;
    private static final byte[] JPEG_SIGNATURE = {-1, MARKER_SOI, -1};
    private static final byte[] HEIF_TYPE_FTYP = {102, 116, 121, 112};
    private static final byte[] HEIF_BRAND_MIF1 = {109, 105, 102, 49};
    private static final byte[] HEIF_BRAND_HEIC = {104, 101, 105, 99};
    private static final byte[] ORF_MAKER_NOTE_HEADER_1 = {79, 76, 89, 77, 80, 0};
    private static final byte[] ORF_MAKER_NOTE_HEADER_2 = {79, 76, 89, 77, 80, 85, 83, 0, 73, 73};
    private static final String[] IFD_FORMAT_NAMES = {"", "BYTE", "STRING", "USHORT", "ULONG", "URATIONAL", "SBYTE", "UNDEFINED", "SSHORT", "SLONG", "SRATIONAL", "SINGLE", "DOUBLE"};
    private static final int[] IFD_FORMAT_BYTES_PER_FORMAT = {0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8, 1};
    private static final byte[] EXIF_ASCII_PREFIX = {65, 83, 67, 73, 73, 0, 0, 0};
    private static final int[] BITS_PER_SAMPLE_RGB = {8, 8, 8};
    private static final int[] BITS_PER_SAMPLE_GREYSCALE_1 = {4};
    private static final int[] BITS_PER_SAMPLE_GREYSCALE_2 = {8};
    public static final String TAG_DIGITAL_ZOOM_RATIO = "DigitalZoomRatio";
    public static final String TAG_EXPOSURE_TIME = "ExposureTime";
    public static final String TAG_SUBJECT_DISTANCE = "SubjectDistance";
    public static final String TAG_GPS_TIMESTAMP = "GPSTimeStamp";
    private static final HashSet<String> sTagSetForCompatibility = new HashSet<>(Arrays.asList("FNumber", TAG_DIGITAL_ZOOM_RATIO, TAG_EXPOSURE_TIME, TAG_SUBJECT_DISTANCE, TAG_GPS_TIMESTAMP));
    private static final HashMap<Integer, Integer> sExifPointerTagMap = new HashMap<>();
    private static final Charset ASCII = Charset.forName("US-ASCII");
    private static final byte[] IDENTIFIER_EXIF_APP1 = "Exif\u0000\u0000".getBytes(ASCII);
    private static SimpleDateFormat sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");

    @Retention(RetentionPolicy.SOURCE)
    public @interface IfdType {
    }

    static {
        int i = 3;
        int i2 = 4;
        int i3 = 6;
        int i4 = 1;
        int i5 = 2;
        int i6 = 5;
        int i7 = 7;
        int i8 = 3;
        int i9 = 4;
        int i10 = 23;
        IFD_TIFF_TAGS = new ExifTag[]{new ExifTag(TAG_NEW_SUBFILE_TYPE, 254, i2), new ExifTag(TAG_SUBFILE_TYPE, 255, i2), new ExifTag(TAG_IMAGE_WIDTH, 256, 3, 4), new ExifTag(TAG_IMAGE_LENGTH, 257, 3, 4), new ExifTag(TAG_BITS_PER_SAMPLE, 258, i), new ExifTag(TAG_COMPRESSION, 259, i), new ExifTag(TAG_PHOTOMETRIC_INTERPRETATION, 262, i), new ExifTag(TAG_IMAGE_DESCRIPTION, 270, i5), new ExifTag(TAG_MAKE, 271, i5), new ExifTag(TAG_MODEL, 272, i5), new ExifTag(TAG_STRIP_OFFSETS, 273, i8, i9), new ExifTag(TAG_ORIENTATION, 274, i), new ExifTag(TAG_SAMPLES_PER_PIXEL, 277, i), new ExifTag(TAG_ROWS_PER_STRIP, 278, i8, i9), new ExifTag(TAG_STRIP_BYTE_COUNTS, 279, i8, i9), new ExifTag(TAG_X_RESOLUTION, 282, i6), new ExifTag(TAG_Y_RESOLUTION, 283, i6), new ExifTag(TAG_PLANAR_CONFIGURATION, 284, i), new ExifTag(TAG_RESOLUTION_UNIT, 296, i), new ExifTag(TAG_TRANSFER_FUNCTION, 301, i), new ExifTag(TAG_SOFTWARE, 305, i5), new ExifTag(TAG_DATETIME, 306, i5), new ExifTag(TAG_ARTIST, 315, i5), new ExifTag(TAG_WHITE_POINT, 318, i6), new ExifTag(TAG_PRIMARY_CHROMATICITIES, 319, i6), new ExifTag(TAG_SUB_IFD_POINTER, 330, i2), new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, i2), new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, i2), new ExifTag(TAG_Y_CB_CR_COEFFICIENTS, MetricsProto.MetricsEvent.DIALOG_CUSTOM_LIST_CONFIRMATION, i6), new ExifTag(TAG_Y_CB_CR_SUB_SAMPLING, MetricsProto.MetricsEvent.DIALOG_APN_EDITOR_ERROR, i), new ExifTag(TAG_Y_CB_CR_POSITIONING, MetricsProto.MetricsEvent.DIALOG_OWNER_INFO_SETTINGS, i), new ExifTag(TAG_REFERENCE_BLACK_WHITE, 532, i6), new ExifTag(TAG_COPYRIGHT, 33432, i5), new ExifTag(TAG_EXIF_IFD_POINTER, 34665, i2), new ExifTag(TAG_GPS_INFO_IFD_POINTER, GLES30.GL_DRAW_BUFFER0, i2), new ExifTag(TAG_RW2_SENSOR_TOP_BORDER, i2, i2), new ExifTag(TAG_RW2_SENSOR_LEFT_BORDER, i6, i2), new ExifTag(TAG_RW2_SENSOR_BOTTOM_BORDER, i3, i2), new ExifTag(TAG_RW2_SENSOR_RIGHT_BORDER, i7, i2), new ExifTag(TAG_RW2_ISO, i10, i), new ExifTag(TAG_RW2_JPG_FROM_RAW, 46, i7)};
        int i11 = 10;
        int i12 = 3;
        int i13 = 4;
        IFD_EXIF_TAGS = new ExifTag[]{new ExifTag(TAG_EXPOSURE_TIME, 33434, i6), new ExifTag("FNumber", 33437, i6), new ExifTag(TAG_EXPOSURE_PROGRAM, 34850, i), new ExifTag(TAG_SPECTRAL_SENSITIVITY, GLES30.GL_MAX_DRAW_BUFFERS, i5), new ExifTag("ISOSpeedRatings", GLES30.GL_DRAW_BUFFER2, i), new ExifTag(TAG_OECF, GLES30.GL_DRAW_BUFFER3, i7), new ExifTag(TAG_EXIF_VERSION, 36864, i5), new ExifTag(TAG_DATETIME_ORIGINAL, 36867, i5), new ExifTag(TAG_DATETIME_DIGITIZED, 36868, i5), new ExifTag(TAG_COMPONENTS_CONFIGURATION, 37121, i7), new ExifTag(TAG_COMPRESSED_BITS_PER_PIXEL, 37122, i6), new ExifTag(TAG_SHUTTER_SPEED_VALUE, 37377, i11), new ExifTag(TAG_APERTURE_VALUE, 37378, i6), new ExifTag(TAG_BRIGHTNESS_VALUE, 37379, i11), new ExifTag(TAG_EXPOSURE_BIAS_VALUE, 37380, i11), new ExifTag(TAG_MAX_APERTURE_VALUE, 37381, i6), new ExifTag(TAG_SUBJECT_DISTANCE, 37382, i6), new ExifTag(TAG_METERING_MODE, 37383, i), new ExifTag(TAG_LIGHT_SOURCE, 37384, i), new ExifTag(TAG_FLASH, 37385, i), new ExifTag(TAG_FOCAL_LENGTH, 37386, i6), new ExifTag(TAG_SUBJECT_AREA, 37396, i), new ExifTag(TAG_MAKER_NOTE, 37500, i7), new ExifTag(TAG_USER_COMMENT, 37510, i7), new ExifTag(TAG_SUBSEC_TIME, 37520, i5), new ExifTag("SubSecTimeOriginal", 37521, i5), new ExifTag("SubSecTimeDigitized", 37522, i5), new ExifTag(TAG_FLASHPIX_VERSION, 40960, i7), new ExifTag(TAG_COLOR_SPACE, 40961, i), new ExifTag(TAG_PIXEL_X_DIMENSION, 40962, i12, i13), new ExifTag(TAG_PIXEL_Y_DIMENSION, 40963, i12, i13), new ExifTag(TAG_RELATED_SOUND_FILE, 40964, i5), new ExifTag(TAG_INTEROPERABILITY_IFD_POINTER, 40965, i2), new ExifTag(TAG_FLASH_ENERGY, 41483, i6), new ExifTag(TAG_SPATIAL_FREQUENCY_RESPONSE, 41484, i7), new ExifTag(TAG_FOCAL_PLANE_X_RESOLUTION, 41486, i6), new ExifTag(TAG_FOCAL_PLANE_Y_RESOLUTION, 41487, i6), new ExifTag(TAG_FOCAL_PLANE_RESOLUTION_UNIT, 41488, i), new ExifTag(TAG_SUBJECT_LOCATION, 41492, i), new ExifTag(TAG_EXPOSURE_INDEX, 41493, i6), new ExifTag(TAG_SENSING_METHOD, 41495, i), new ExifTag(TAG_FILE_SOURCE, 41728, i7), new ExifTag(TAG_SCENE_TYPE, 41729, i7), new ExifTag(TAG_CFA_PATTERN, 41730, i7), new ExifTag(TAG_CUSTOM_RENDERED, 41985, i), new ExifTag(TAG_EXPOSURE_MODE, 41986, i), new ExifTag(TAG_WHITE_BALANCE, 41987, i), new ExifTag(TAG_DIGITAL_ZOOM_RATIO, 41988, i6), new ExifTag(TAG_FOCAL_LENGTH_IN_35MM_FILM, 41989, i), new ExifTag(TAG_SCENE_CAPTURE_TYPE, 41990, i), new ExifTag(TAG_GAIN_CONTROL, 41991, i), new ExifTag(TAG_CONTRAST, 41992, i), new ExifTag(TAG_SATURATION, 41993, i), new ExifTag(TAG_SHARPNESS, 41994, i), new ExifTag(TAG_DEVICE_SETTING_DESCRIPTION, 41995, i7), new ExifTag(TAG_SUBJECT_DISTANCE_RANGE, 41996, i), new ExifTag(TAG_IMAGE_UNIQUE_ID, 42016, i5), new ExifTag(TAG_DNG_VERSION, 50706, i4), new ExifTag(TAG_DEFAULT_CROP_SIZE, 50720, i12, i13)};
        IFD_GPS_TAGS = new ExifTag[]{new ExifTag(TAG_GPS_VERSION_ID, 0, i4), new ExifTag(TAG_GPS_LATITUDE_REF, i4, i5), new ExifTag(TAG_GPS_LATITUDE, i5, i6), new ExifTag(TAG_GPS_LONGITUDE_REF, i, i5), new ExifTag(TAG_GPS_LONGITUDE, i2, i6), new ExifTag(TAG_GPS_ALTITUDE_REF, i6, i4), new ExifTag(TAG_GPS_ALTITUDE, i3, i6), new ExifTag(TAG_GPS_TIMESTAMP, i7, i6), new ExifTag(TAG_GPS_SATELLITES, 8, i5), new ExifTag(TAG_GPS_STATUS, 9, i5), new ExifTag(TAG_GPS_MEASURE_MODE, 10, i5), new ExifTag(TAG_GPS_DOP, 11, i6), new ExifTag(TAG_GPS_SPEED_REF, 12, i5), new ExifTag(TAG_GPS_SPEED, 13, i6), new ExifTag(TAG_GPS_TRACK_REF, 14, i5), new ExifTag(TAG_GPS_TRACK, 15, i6), new ExifTag(TAG_GPS_IMG_DIRECTION_REF, 16, i5), new ExifTag(TAG_GPS_IMG_DIRECTION, 17, i6), new ExifTag(TAG_GPS_MAP_DATUM, 18, i5), new ExifTag(TAG_GPS_DEST_LATITUDE_REF, 19, i5), new ExifTag(TAG_GPS_DEST_LATITUDE, 20, i6), new ExifTag(TAG_GPS_DEST_LONGITUDE_REF, 21, i5), new ExifTag(TAG_GPS_DEST_LONGITUDE, 22, i6), new ExifTag(TAG_GPS_DEST_BEARING_REF, i10, i5), new ExifTag(TAG_GPS_DEST_BEARING, 24, i6), new ExifTag(TAG_GPS_DEST_DISTANCE_REF, 25, i5), new ExifTag(TAG_GPS_DEST_DISTANCE, 26, i6), new ExifTag(TAG_GPS_PROCESSING_METHOD, 27, i7), new ExifTag(TAG_GPS_AREA_INFORMATION, 28, i7), new ExifTag(TAG_GPS_DATESTAMP, 29, i5), new ExifTag(TAG_GPS_DIFFERENTIAL, 30, i)};
        IFD_INTEROPERABILITY_TAGS = new ExifTag[]{new ExifTag(TAG_INTEROPERABILITY_INDEX, i4, i5)};
        int i14 = 3;
        int i15 = 4;
        IFD_THUMBNAIL_TAGS = new ExifTag[]{new ExifTag(TAG_NEW_SUBFILE_TYPE, 254, i2), new ExifTag(TAG_SUBFILE_TYPE, 255, i2), new ExifTag(TAG_THUMBNAIL_IMAGE_WIDTH, 256, 3, 4), new ExifTag(TAG_THUMBNAIL_IMAGE_LENGTH, 257, 3, 4), new ExifTag(TAG_BITS_PER_SAMPLE, 258, i), new ExifTag(TAG_COMPRESSION, 259, i), new ExifTag(TAG_PHOTOMETRIC_INTERPRETATION, 262, i), new ExifTag(TAG_IMAGE_DESCRIPTION, 270, i5), new ExifTag(TAG_MAKE, 271, i5), new ExifTag(TAG_MODEL, 272, i5), new ExifTag(TAG_STRIP_OFFSETS, 273, i14, i15), new ExifTag(TAG_ORIENTATION, 274, i), new ExifTag(TAG_SAMPLES_PER_PIXEL, 277, i), new ExifTag(TAG_ROWS_PER_STRIP, 278, i14, i15), new ExifTag(TAG_STRIP_BYTE_COUNTS, 279, i14, i15), new ExifTag(TAG_X_RESOLUTION, 282, i6), new ExifTag(TAG_Y_RESOLUTION, 283, i6), new ExifTag(TAG_PLANAR_CONFIGURATION, 284, i), new ExifTag(TAG_RESOLUTION_UNIT, 296, i), new ExifTag(TAG_TRANSFER_FUNCTION, 301, i), new ExifTag(TAG_SOFTWARE, 305, i5), new ExifTag(TAG_DATETIME, 306, i5), new ExifTag(TAG_ARTIST, 315, i5), new ExifTag(TAG_WHITE_POINT, 318, i6), new ExifTag(TAG_PRIMARY_CHROMATICITIES, 319, i6), new ExifTag(TAG_SUB_IFD_POINTER, 330, i2), new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, i2), new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, i2), new ExifTag(TAG_Y_CB_CR_COEFFICIENTS, MetricsProto.MetricsEvent.DIALOG_CUSTOM_LIST_CONFIRMATION, i6), new ExifTag(TAG_Y_CB_CR_SUB_SAMPLING, MetricsProto.MetricsEvent.DIALOG_APN_EDITOR_ERROR, i), new ExifTag(TAG_Y_CB_CR_POSITIONING, MetricsProto.MetricsEvent.DIALOG_OWNER_INFO_SETTINGS, i), new ExifTag(TAG_REFERENCE_BLACK_WHITE, 532, i6), new ExifTag(TAG_COPYRIGHT, 33432, i5), new ExifTag(TAG_EXIF_IFD_POINTER, 34665, i2), new ExifTag(TAG_GPS_INFO_IFD_POINTER, GLES30.GL_DRAW_BUFFER0, i2), new ExifTag(TAG_DNG_VERSION, 50706, i4), new ExifTag(TAG_DEFAULT_CROP_SIZE, 50720, i14, i15)};
        TAG_RAF_IMAGE_SIZE = new ExifTag(TAG_STRIP_OFFSETS, 273, i);
        ORF_MAKER_NOTE_TAGS = new ExifTag[]{new ExifTag(TAG_ORF_THUMBNAIL_IMAGE, 256, i7), new ExifTag(TAG_ORF_CAMERA_SETTINGS_IFD_POINTER, MtpConstants.RESPONSE_SPECIFICATION_OF_DESTINATION_UNSUPPORTED, i2), new ExifTag(TAG_ORF_IMAGE_PROCESSING_IFD_POINTER, 8256, i2)};
        ORF_CAMERA_SETTINGS_TAGS = new ExifTag[]{new ExifTag(TAG_ORF_PREVIEW_IMAGE_START, 257, i2), new ExifTag(TAG_ORF_PREVIEW_IMAGE_LENGTH, 258, i2)};
        ORF_IMAGE_PROCESSING_TAGS = new ExifTag[]{new ExifTag(TAG_ORF_ASPECT_FRAME, SmsCbConstants.MESSAGE_ID_CMAS_ALERT_EXTREME_IMMEDIATE_OBSERVED, i)};
        PEF_TAGS = new ExifTag[]{new ExifTag(TAG_COLOR_SPACE, 55, i)};
        EXIF_TAGS = new ExifTag[][]{IFD_TIFF_TAGS, IFD_EXIF_TAGS, IFD_GPS_TAGS, IFD_INTEROPERABILITY_TAGS, IFD_THUMBNAIL_TAGS, IFD_TIFF_TAGS, ORF_MAKER_NOTE_TAGS, ORF_CAMERA_SETTINGS_TAGS, ORF_IMAGE_PROCESSING_TAGS, PEF_TAGS};
        EXIF_POINTER_TAGS = new ExifTag[]{new ExifTag(TAG_SUB_IFD_POINTER, 330, i2), new ExifTag(TAG_EXIF_IFD_POINTER, 34665, i2), new ExifTag(TAG_GPS_INFO_IFD_POINTER, GLES30.GL_DRAW_BUFFER0, i2), new ExifTag(TAG_INTEROPERABILITY_IFD_POINTER, 40965, i2), new ExifTag(TAG_ORF_CAMERA_SETTINGS_IFD_POINTER, MtpConstants.RESPONSE_SPECIFICATION_OF_DESTINATION_UNSUPPORTED, i4), new ExifTag(TAG_ORF_IMAGE_PROCESSING_IFD_POINTER, 8256, i4)};
        JPEG_INTERCHANGE_FORMAT_TAG = new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, i2);
        JPEG_INTERCHANGE_FORMAT_LENGTH_TAG = new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, i2);
        sExifTagMapsForReading = new HashMap[EXIF_TAGS.length];
        sExifTagMapsForWriting = new HashMap[EXIF_TAGS.length];
        sFormatter.setTimeZone(TimeZone.getTimeZone(Time.TIMEZONE_UTC));
        for (int i16 = 0; i16 < EXIF_TAGS.length; i16++) {
            sExifTagMapsForReading[i16] = new HashMap();
            sExifTagMapsForWriting[i16] = new HashMap();
            for (ExifTag exifTag : EXIF_TAGS[i16]) {
                sExifTagMapsForReading[i16].put(Integer.valueOf(exifTag.number), exifTag);
                sExifTagMapsForWriting[i16].put(exifTag.name, exifTag);
            }
        }
        sExifPointerTagMap.put(Integer.valueOf(EXIF_POINTER_TAGS[0].number), 5);
        sExifPointerTagMap.put(Integer.valueOf(EXIF_POINTER_TAGS[1].number), 1);
        sExifPointerTagMap.put(Integer.valueOf(EXIF_POINTER_TAGS[2].number), 2);
        sExifPointerTagMap.put(Integer.valueOf(EXIF_POINTER_TAGS[3].number), 3);
        sExifPointerTagMap.put(Integer.valueOf(EXIF_POINTER_TAGS[4].number), 7);
        sExifPointerTagMap.put(Integer.valueOf(EXIF_POINTER_TAGS[5].number), 8);
        sNonZeroTimePattern = Pattern.compile(".*[1-9].*");
        sGpsTimestampPattern = Pattern.compile("^([0-9][0-9]):([0-9][0-9]):([0-9][0-9])$");
    }

    private static class Rational {
        public final long denominator;
        public final long numerator;

        private Rational(long j, long j2) {
            if (j2 == 0) {
                this.numerator = 0L;
                this.denominator = 1L;
            } else {
                this.numerator = j;
                this.denominator = j2;
            }
        }

        public String toString() {
            return this.numerator + "/" + this.denominator;
        }

        public double calculate() {
            return this.numerator / this.denominator;
        }
    }

    private static class ExifAttribute {
        public final byte[] bytes;
        public final int format;
        public final int numberOfComponents;

        private ExifAttribute(int i, int i2, byte[] bArr) {
            this.format = i;
            this.numberOfComponents = i2;
            this.bytes = bArr;
        }

        public static ExifAttribute createUShort(int[] iArr, ByteOrder byteOrder) {
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[3] * iArr.length]);
            byteBufferWrap.order(byteOrder);
            for (int i : iArr) {
                byteBufferWrap.putShort((short) i);
            }
            return new ExifAttribute(3, iArr.length, byteBufferWrap.array());
        }

        public static ExifAttribute createUShort(int i, ByteOrder byteOrder) {
            return createUShort(new int[]{i}, byteOrder);
        }

        public static ExifAttribute createULong(long[] jArr, ByteOrder byteOrder) {
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[4] * jArr.length]);
            byteBufferWrap.order(byteOrder);
            for (long j : jArr) {
                byteBufferWrap.putInt((int) j);
            }
            return new ExifAttribute(4, jArr.length, byteBufferWrap.array());
        }

        public static ExifAttribute createULong(long j, ByteOrder byteOrder) {
            return createULong(new long[]{j}, byteOrder);
        }

        public static ExifAttribute createSLong(int[] iArr, ByteOrder byteOrder) {
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[9] * iArr.length]);
            byteBufferWrap.order(byteOrder);
            for (int i : iArr) {
                byteBufferWrap.putInt(i);
            }
            return new ExifAttribute(9, iArr.length, byteBufferWrap.array());
        }

        public static ExifAttribute createSLong(int i, ByteOrder byteOrder) {
            return createSLong(new int[]{i}, byteOrder);
        }

        public static ExifAttribute createByte(String str) {
            if (str.length() != 1 || str.charAt(0) < '0' || str.charAt(0) > '1') {
                byte[] bytes = str.getBytes(ExifInterface.ASCII);
                return new ExifAttribute(1, bytes.length, bytes);
            }
            byte[] bArr = {(byte) (str.charAt(0) - '0')};
            return new ExifAttribute(1, bArr.length, bArr);
        }

        public static ExifAttribute createString(String str) {
            byte[] bytes = (str + (char) 0).getBytes(ExifInterface.ASCII);
            return new ExifAttribute(2, bytes.length, bytes);
        }

        public static ExifAttribute createURational(Rational[] rationalArr, ByteOrder byteOrder) {
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[5] * rationalArr.length]);
            byteBufferWrap.order(byteOrder);
            for (Rational rational : rationalArr) {
                byteBufferWrap.putInt((int) rational.numerator);
                byteBufferWrap.putInt((int) rational.denominator);
            }
            return new ExifAttribute(5, rationalArr.length, byteBufferWrap.array());
        }

        public static ExifAttribute createURational(Rational rational, ByteOrder byteOrder) {
            return createURational(new Rational[]{rational}, byteOrder);
        }

        public static ExifAttribute createSRational(Rational[] rationalArr, ByteOrder byteOrder) {
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[10] * rationalArr.length]);
            byteBufferWrap.order(byteOrder);
            for (Rational rational : rationalArr) {
                byteBufferWrap.putInt((int) rational.numerator);
                byteBufferWrap.putInt((int) rational.denominator);
            }
            return new ExifAttribute(10, rationalArr.length, byteBufferWrap.array());
        }

        public static ExifAttribute createSRational(Rational rational, ByteOrder byteOrder) {
            return createSRational(new Rational[]{rational}, byteOrder);
        }

        public static ExifAttribute createDouble(double[] dArr, ByteOrder byteOrder) {
            ByteBuffer byteBufferWrap = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[12] * dArr.length]);
            byteBufferWrap.order(byteOrder);
            for (double d : dArr) {
                byteBufferWrap.putDouble(d);
            }
            return new ExifAttribute(12, dArr.length, byteBufferWrap.array());
        }

        public static ExifAttribute createDouble(double d, ByteOrder byteOrder) {
            return createDouble(new double[]{d}, byteOrder);
        }

        public String toString() {
            return "(" + ExifInterface.IFD_FORMAT_NAMES[this.format] + ", data length:" + this.bytes.length + ")";
        }

        private Object getValue(ByteOrder byteOrder) {
            byte b;
            try {
                ByteOrderedDataInputStream byteOrderedDataInputStream = new ByteOrderedDataInputStream(this.bytes);
                byteOrderedDataInputStream.setByteOrder(byteOrder);
                boolean z = true;
                int length = 0;
                switch (this.format) {
                    case 1:
                    case 6:
                        if (this.bytes.length == 1 && this.bytes[0] >= 0 && this.bytes[0] <= 1) {
                            return new String(new char[]{(char) (this.bytes[0] + 48)});
                        }
                        return new String(this.bytes, ExifInterface.ASCII);
                    case 2:
                    case 7:
                        if (this.numberOfComponents >= ExifInterface.EXIF_ASCII_PREFIX.length) {
                            int i = 0;
                            while (true) {
                                if (i < ExifInterface.EXIF_ASCII_PREFIX.length) {
                                    if (this.bytes[i] == ExifInterface.EXIF_ASCII_PREFIX[i]) {
                                        i++;
                                    } else {
                                        z = false;
                                    }
                                }
                            }
                            if (z) {
                                length = ExifInterface.EXIF_ASCII_PREFIX.length;
                            }
                        }
                        StringBuilder sb = new StringBuilder();
                        while (length < this.numberOfComponents && (b = this.bytes[length]) != 0) {
                            if (b >= 32) {
                                sb.append((char) b);
                            } else {
                                sb.append('?');
                            }
                            length++;
                        }
                        return sb.toString();
                    case 3:
                        int[] iArr = new int[this.numberOfComponents];
                        while (length < this.numberOfComponents) {
                            iArr[length] = byteOrderedDataInputStream.readUnsignedShort();
                            length++;
                        }
                        return iArr;
                    case 4:
                        long[] jArr = new long[this.numberOfComponents];
                        while (length < this.numberOfComponents) {
                            jArr[length] = byteOrderedDataInputStream.readUnsignedInt();
                            length++;
                        }
                        return jArr;
                    case 5:
                        Rational[] rationalArr = new Rational[this.numberOfComponents];
                        while (length < this.numberOfComponents) {
                            rationalArr[length] = new Rational(byteOrderedDataInputStream.readUnsignedInt(), byteOrderedDataInputStream.readUnsignedInt());
                            length++;
                        }
                        return rationalArr;
                    case 8:
                        int[] iArr2 = new int[this.numberOfComponents];
                        while (length < this.numberOfComponents) {
                            iArr2[length] = byteOrderedDataInputStream.readShort();
                            length++;
                        }
                        return iArr2;
                    case 9:
                        int[] iArr3 = new int[this.numberOfComponents];
                        while (length < this.numberOfComponents) {
                            iArr3[length] = byteOrderedDataInputStream.readInt();
                            length++;
                        }
                        return iArr3;
                    case 10:
                        Rational[] rationalArr2 = new Rational[this.numberOfComponents];
                        while (length < this.numberOfComponents) {
                            rationalArr2[length] = new Rational(byteOrderedDataInputStream.readInt(), byteOrderedDataInputStream.readInt());
                            length++;
                        }
                        return rationalArr2;
                    case 11:
                        double[] dArr = new double[this.numberOfComponents];
                        while (length < this.numberOfComponents) {
                            dArr[length] = byteOrderedDataInputStream.readFloat();
                            length++;
                        }
                        return dArr;
                    case 12:
                        double[] dArr2 = new double[this.numberOfComponents];
                        while (length < this.numberOfComponents) {
                            dArr2[length] = byteOrderedDataInputStream.readDouble();
                            length++;
                        }
                        return dArr2;
                    default:
                        return null;
                }
            } catch (IOException e) {
                Log.w(ExifInterface.TAG, "IOException occurred during reading a value", e);
                return null;
            }
        }

        public double getDoubleValue(ByteOrder byteOrder) {
            Object value = getValue(byteOrder);
            if (value == null) {
                throw new NumberFormatException("NULL can't be converted to a double value");
            }
            if (value instanceof String) {
                return Double.parseDouble((String) value);
            }
            if (value instanceof long[]) {
                if (((long[]) value).length == 1) {
                    return r4[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof int[]) {
                if (((int[]) value).length == 1) {
                    return r4[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof double[]) {
                double[] dArr = (double[]) value;
                if (dArr.length == 1) {
                    return dArr[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof Rational[]) {
                Rational[] rationalArr = (Rational[]) value;
                if (rationalArr.length == 1) {
                    return rationalArr[0].calculate();
                }
                throw new NumberFormatException("There are more than one component");
            }
            throw new NumberFormatException("Couldn't find a double value");
        }

        public int getIntValue(ByteOrder byteOrder) {
            Object value = getValue(byteOrder);
            if (value == null) {
                throw new NumberFormatException("NULL can't be converted to a integer value");
            }
            if (value instanceof String) {
                return Integer.parseInt((String) value);
            }
            if (value instanceof long[]) {
                long[] jArr = (long[]) value;
                if (jArr.length == 1) {
                    return (int) jArr[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof int[]) {
                int[] iArr = (int[]) value;
                if (iArr.length == 1) {
                    return iArr[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            throw new NumberFormatException("Couldn't find a integer value");
        }

        public String getStringValue(ByteOrder byteOrder) {
            Object value = getValue(byteOrder);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                return (String) value;
            }
            StringBuilder sb = new StringBuilder();
            int i = 0;
            if (value instanceof long[]) {
                long[] jArr = (long[]) value;
                while (i < jArr.length) {
                    sb.append(jArr[i]);
                    i++;
                    if (i != jArr.length) {
                        sb.append(",");
                    }
                }
                return sb.toString();
            }
            if (value instanceof int[]) {
                int[] iArr = (int[]) value;
                while (i < iArr.length) {
                    sb.append(iArr[i]);
                    i++;
                    if (i != iArr.length) {
                        sb.append(",");
                    }
                }
                return sb.toString();
            }
            if (value instanceof double[]) {
                double[] dArr = (double[]) value;
                while (i < dArr.length) {
                    sb.append(dArr[i]);
                    i++;
                    if (i != dArr.length) {
                        sb.append(",");
                    }
                }
                return sb.toString();
            }
            if (!(value instanceof Rational[])) {
                return null;
            }
            Rational[] rationalArr = (Rational[]) value;
            while (i < rationalArr.length) {
                sb.append(rationalArr[i].numerator);
                sb.append('/');
                sb.append(rationalArr[i].denominator);
                i++;
                if (i != rationalArr.length) {
                    sb.append(",");
                }
            }
            return sb.toString();
        }

        public int size() {
            return ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[this.format] * this.numberOfComponents;
        }
    }

    private static class ExifTag {
        public final String name;
        public final int number;
        public final int primaryFormat;
        public final int secondaryFormat;

        private ExifTag(String str, int i, int i2) {
            this.name = str;
            this.number = i;
            this.primaryFormat = i2;
            this.secondaryFormat = -1;
        }

        private ExifTag(String str, int i, int i2, int i3) {
            this.name = str;
            this.number = i;
            this.primaryFormat = i2;
            this.secondaryFormat = i3;
        }
    }

    public ExifInterface(String str) throws Throwable {
        FileInputStream fileInputStream;
        this.mAttributes = new HashMap[EXIF_TAGS.length];
        this.mAttributesOffsets = new HashSet(EXIF_TAGS.length);
        this.mExifByteOrder = ByteOrder.BIG_ENDIAN;
        if (str == null) {
            throw new IllegalArgumentException("filename cannot be null");
        }
        this.mAssetInputStream = null;
        this.mFilename = str;
        this.mIsInputStream = false;
        try {
            fileInputStream = new FileInputStream(str);
            try {
                if (isSeekableFD(fileInputStream.getFD())) {
                    this.mSeekableFileDescriptor = fileInputStream.getFD();
                } else {
                    this.mSeekableFileDescriptor = null;
                }
                loadAttributes(fileInputStream);
                IoUtils.closeQuietly(fileInputStream);
            } catch (Throwable th) {
                th = th;
                IoUtils.closeQuietly(fileInputStream);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
            fileInputStream = null;
        }
    }

    public ExifInterface(FileDescriptor fileDescriptor) throws Throwable {
        this.mAttributes = new HashMap[EXIF_TAGS.length];
        this.mAttributesOffsets = new HashSet(EXIF_TAGS.length);
        this.mExifByteOrder = ByteOrder.BIG_ENDIAN;
        if (fileDescriptor == null) {
            throw new IllegalArgumentException("fileDescriptor cannot be null");
        }
        FileInputStream fileInputStream = null;
        this.mAssetInputStream = null;
        this.mFilename = null;
        if (isSeekableFD(fileDescriptor)) {
            this.mSeekableFileDescriptor = fileDescriptor;
            try {
                fileDescriptor = Os.dup(fileDescriptor);
            } catch (ErrnoException e) {
                throw e.rethrowAsIOException();
            }
        } else {
            this.mSeekableFileDescriptor = null;
        }
        this.mIsInputStream = false;
        try {
            FileInputStream fileInputStream2 = new FileInputStream(fileDescriptor);
            try {
                loadAttributes(fileInputStream2);
                IoUtils.closeQuietly(fileInputStream2);
            } catch (Throwable th) {
                th = th;
                fileInputStream = fileInputStream2;
                IoUtils.closeQuietly(fileInputStream);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public ExifInterface(InputStream inputStream) throws IOException {
        this.mAttributes = new HashMap[EXIF_TAGS.length];
        this.mAttributesOffsets = new HashSet(EXIF_TAGS.length);
        this.mExifByteOrder = ByteOrder.BIG_ENDIAN;
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream cannot be null");
        }
        this.mFilename = null;
        if (inputStream instanceof AssetManager.AssetInputStream) {
            this.mAssetInputStream = (AssetManager.AssetInputStream) inputStream;
            this.mSeekableFileDescriptor = null;
        } else if (inputStream instanceof FileInputStream) {
            FileInputStream fileInputStream = (FileInputStream) inputStream;
            if (isSeekableFD(fileInputStream.getFD())) {
                this.mAssetInputStream = null;
                this.mSeekableFileDescriptor = fileInputStream.getFD();
            } else {
                this.mAssetInputStream = null;
                this.mSeekableFileDescriptor = null;
            }
        }
        this.mIsInputStream = true;
        loadAttributes(inputStream);
    }

    private ExifAttribute getExifAttribute(String str) {
        for (int i = 0; i < EXIF_TAGS.length; i++) {
            Object obj = this.mAttributes[i].get(str);
            if (obj != null) {
                return (ExifAttribute) obj;
            }
        }
        return null;
    }

    public String getAttribute(String str) {
        ExifAttribute exifAttribute = getExifAttribute(str);
        if (exifAttribute == null) {
            return null;
        }
        if (!sTagSetForCompatibility.contains(str)) {
            return exifAttribute.getStringValue(this.mExifByteOrder);
        }
        if (str.equals(TAG_GPS_TIMESTAMP)) {
            if (exifAttribute.format == 5 || exifAttribute.format == 10) {
                if (((Rational[]) exifAttribute.getValue(this.mExifByteOrder)).length != 3) {
                    return null;
                }
                return String.format("%02d:%02d:%02d", Integer.valueOf((int) (r7[0].numerator / r7[0].denominator)), Integer.valueOf((int) (r7[1].numerator / r7[1].denominator)), Integer.valueOf((int) (r7[2].numerator / r7[2].denominator)));
            }
            return null;
        }
        try {
            return Double.toString(exifAttribute.getDoubleValue(this.mExifByteOrder));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int getAttributeInt(String str, int i) {
        ExifAttribute exifAttribute = getExifAttribute(str);
        if (exifAttribute == null) {
            return i;
        }
        try {
            return exifAttribute.getIntValue(this.mExifByteOrder);
        } catch (NumberFormatException e) {
            return i;
        }
    }

    public double getAttributeDouble(String str, double d) {
        ExifAttribute exifAttribute = getExifAttribute(str);
        if (exifAttribute == null) {
            return d;
        }
        try {
            return exifAttribute.getDoubleValue(this.mExifByteOrder);
        } catch (NumberFormatException e) {
            return d;
        }
    }

    public void setAttribute(String str, String str2) {
        Object obj;
        int i;
        String str3 = str2;
        if (str3 != null && sTagSetForCompatibility.contains(str)) {
            if (str.equals(TAG_GPS_TIMESTAMP)) {
                Matcher matcher = sGpsTimestampPattern.matcher(str3);
                if (!matcher.find()) {
                    Log.w(TAG, "Invalid value for " + str + " : " + str3);
                    return;
                }
                str3 = Integer.parseInt(matcher.group(1)) + "/1," + Integer.parseInt(matcher.group(2)) + "/1," + Integer.parseInt(matcher.group(3)) + "/1";
            } else {
                try {
                    str3 = ((long) (Double.parseDouble(str2) * 10000.0d)) + "/10000";
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid value for " + str + " : " + str3);
                    return;
                }
            }
        }
        for (int i2 = 0; i2 < EXIF_TAGS.length; i2++) {
            if ((i2 != 4 || this.mHasThumbnail) && (obj = sExifTagMapsForWriting[i2].get(str)) != null) {
                if (str3 == null) {
                    this.mAttributes[i2].remove(str);
                } else {
                    ExifTag exifTag = (ExifTag) obj;
                    Pair<Integer, Integer> pairGuessDataFormat = guessDataFormat(str3);
                    if (exifTag.primaryFormat == pairGuessDataFormat.first.intValue() || exifTag.primaryFormat == pairGuessDataFormat.second.intValue()) {
                        i = exifTag.primaryFormat;
                    } else if (exifTag.secondaryFormat != -1 && (exifTag.secondaryFormat == pairGuessDataFormat.first.intValue() || exifTag.secondaryFormat == pairGuessDataFormat.second.intValue())) {
                        i = exifTag.secondaryFormat;
                    } else if (exifTag.primaryFormat == 1 || exifTag.primaryFormat == 7 || exifTag.primaryFormat == 2) {
                        i = exifTag.primaryFormat;
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Given tag (");
                        sb.append(str);
                        sb.append(") value didn't match with one of expected formats: ");
                        sb.append(IFD_FORMAT_NAMES[exifTag.primaryFormat]);
                        sb.append(exifTag.secondaryFormat == -1 ? "" : ", " + IFD_FORMAT_NAMES[exifTag.secondaryFormat]);
                        sb.append(" (guess: ");
                        sb.append(IFD_FORMAT_NAMES[pairGuessDataFormat.first.intValue()]);
                        sb.append(pairGuessDataFormat.second.intValue() == -1 ? "" : ", " + IFD_FORMAT_NAMES[pairGuessDataFormat.second.intValue()]);
                        sb.append(")");
                        Log.w(TAG, sb.toString());
                    }
                    switch (i) {
                        case 1:
                            this.mAttributes[i2].put(str, ExifAttribute.createByte(str3));
                            break;
                        case 2:
                        case 7:
                            this.mAttributes[i2].put(str, ExifAttribute.createString(str3));
                            break;
                        case 3:
                            String[] strArrSplit = str3.split(",");
                            int[] iArr = new int[strArrSplit.length];
                            for (int i3 = 0; i3 < strArrSplit.length; i3++) {
                                iArr[i3] = Integer.parseInt(strArrSplit[i3]);
                            }
                            this.mAttributes[i2].put(str, ExifAttribute.createUShort(iArr, this.mExifByteOrder));
                            break;
                        case 4:
                            String[] strArrSplit2 = str3.split(",");
                            long[] jArr = new long[strArrSplit2.length];
                            for (int i4 = 0; i4 < strArrSplit2.length; i4++) {
                                jArr[i4] = Long.parseLong(strArrSplit2[i4]);
                            }
                            this.mAttributes[i2].put(str, ExifAttribute.createULong(jArr, this.mExifByteOrder));
                            break;
                        case 5:
                            String[] strArrSplit3 = str3.split(",");
                            Rational[] rationalArr = new Rational[strArrSplit3.length];
                            for (int i5 = 0; i5 < strArrSplit3.length; i5++) {
                                String[] strArrSplit4 = strArrSplit3[i5].split("/");
                                rationalArr[i5] = new Rational((long) Double.parseDouble(strArrSplit4[0]), (long) Double.parseDouble(strArrSplit4[1]));
                            }
                            this.mAttributes[i2].put(str, ExifAttribute.createURational(rationalArr, this.mExifByteOrder));
                            break;
                        case 6:
                        case 8:
                        case 11:
                        default:
                            Log.w(TAG, "Data format isn't one of expected formats: " + i);
                            break;
                        case 9:
                            String[] strArrSplit5 = str3.split(",");
                            int[] iArr2 = new int[strArrSplit5.length];
                            for (int i6 = 0; i6 < strArrSplit5.length; i6++) {
                                iArr2[i6] = Integer.parseInt(strArrSplit5[i6]);
                            }
                            this.mAttributes[i2].put(str, ExifAttribute.createSLong(iArr2, this.mExifByteOrder));
                            break;
                        case 10:
                            String[] strArrSplit6 = str3.split(",");
                            Rational[] rationalArr2 = new Rational[strArrSplit6.length];
                            for (int i7 = 0; i7 < strArrSplit6.length; i7++) {
                                String[] strArrSplit7 = strArrSplit6[i7].split("/");
                                rationalArr2[i7] = new Rational((long) Double.parseDouble(strArrSplit7[0]), (long) Double.parseDouble(strArrSplit7[1]));
                            }
                            this.mAttributes[i2].put(str, ExifAttribute.createSRational(rationalArr2, this.mExifByteOrder));
                            break;
                        case 12:
                            String[] strArrSplit8 = str3.split(",");
                            double[] dArr = new double[strArrSplit8.length];
                            for (int i8 = 0; i8 < strArrSplit8.length; i8++) {
                                dArr[i8] = Double.parseDouble(strArrSplit8[i8]);
                            }
                            this.mAttributes[i2].put(str, ExifAttribute.createDouble(dArr, this.mExifByteOrder));
                            break;
                    }
                }
            }
        }
    }

    private boolean updateAttribute(String str, ExifAttribute exifAttribute) {
        boolean z = false;
        for (int i = 0; i < EXIF_TAGS.length; i++) {
            if (this.mAttributes[i].containsKey(str)) {
                this.mAttributes[i].put(str, exifAttribute);
                z = true;
            }
        }
        return z;
    }

    private void removeAttribute(String str) {
        for (int i = 0; i < EXIF_TAGS.length; i++) {
            this.mAttributes[i].remove(str);
        }
    }

    private void loadAttributes(InputStream inputStream) throws IOException {
        for (int i = 0; i < EXIF_TAGS.length; i++) {
            try {
                try {
                    this.mAttributes[i] = new HashMap();
                } catch (IOException e) {
                    this.mIsSupportedFile = false;
                }
            } finally {
                addDefaultValuesForCompatibility();
            }
        }
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream, 5000);
        this.mMimeType = getMimeType(bufferedInputStream);
        ByteOrderedDataInputStream byteOrderedDataInputStream = new ByteOrderedDataInputStream(bufferedInputStream);
        switch (this.mMimeType) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 5:
            case 6:
            case 8:
            case 11:
                getRawAttributes(byteOrderedDataInputStream);
                break;
            case 4:
                getJpegAttributes(byteOrderedDataInputStream, 0, 0);
                break;
            case 7:
                getOrfAttributes(byteOrderedDataInputStream);
                break;
            case 9:
                getRafAttributes(byteOrderedDataInputStream);
                break;
            case 10:
                getRw2Attributes(byteOrderedDataInputStream);
                break;
            case 12:
                getHeifAttributes(byteOrderedDataInputStream);
                break;
        }
        setThumbnailData(byteOrderedDataInputStream);
        this.mIsSupportedFile = true;
    }

    private static boolean isSeekableFD(FileDescriptor fileDescriptor) throws IOException {
        try {
            Os.lseek(fileDescriptor, 0L, OsConstants.SEEK_CUR);
            return true;
        } catch (ErrnoException e) {
            return false;
        }
    }

    private void printAttributes() {
        for (int i = 0; i < this.mAttributes.length; i++) {
            Log.d(TAG, "The size of tag group[" + i + "]: " + this.mAttributes[i].size());
            for (Map.Entry entry : this.mAttributes[i].entrySet()) {
                ExifAttribute exifAttribute = (ExifAttribute) entry.getValue();
                Log.d(TAG, "tagName: " + entry.getKey() + ", tagType: " + exifAttribute.toString() + ", tagValue: '" + exifAttribute.getStringValue(this.mExifByteOrder) + "'");
            }
        }
    }

    public void saveAttributes() throws Throwable {
        FileOutputStream fileOutputStream;
        File fileCreateTempFile;
        FileInputStream fileInputStream;
        AutoCloseable autoCloseable;
        FileInputStream fileInputStream2;
        FileOutputStream fileOutputStream2;
        if (!this.mIsSupportedFile || this.mMimeType != 4) {
            throw new IOException("ExifInterface only supports saving attributes on JPEG formats.");
        }
        if (this.mIsInputStream || (this.mSeekableFileDescriptor == null && this.mFilename == null)) {
            throw new IOException("ExifInterface does not support saving attributes for the current input.");
        }
        this.mThumbnailBytes = getThumbnail();
        FileInputStream fileInputStream3 = null;
        fileOutputStream = null;
        FileInputStream fileInputStream4 = null;
        FileOutputStream fileOutputStream3 = null;
        fileInputStream3 = null;
        try {
            try {
                if (this.mFilename != null) {
                    fileCreateTempFile = new File(this.mFilename + ".tmp");
                    if (!new File(this.mFilename).renameTo(fileCreateTempFile)) {
                        throw new IOException("Could'nt rename to " + fileCreateTempFile.getAbsolutePath());
                    }
                    fileInputStream = null;
                } else {
                    if (this.mSeekableFileDescriptor != null) {
                        fileCreateTempFile = File.createTempFile("temp", "jpg");
                        Os.lseek(this.mSeekableFileDescriptor, 0L, OsConstants.SEEK_SET);
                        fileInputStream = new FileInputStream(this.mSeekableFileDescriptor);
                        try {
                            fileOutputStream = new FileOutputStream(fileCreateTempFile);
                        } catch (ErrnoException e) {
                            e = e;
                            fileOutputStream = null;
                        } catch (Throwable th) {
                            th = th;
                            fileOutputStream = null;
                        }
                        try {
                            Streams.copy(fileInputStream, fileOutputStream);
                            autoCloseable = fileOutputStream;
                            IoUtils.closeQuietly(fileInputStream);
                            IoUtils.closeQuietly(autoCloseable);
                            fileInputStream2 = new FileInputStream(fileCreateTempFile);
                            if (this.mFilename == null) {
                                fileOutputStream2 = new FileOutputStream(this.mFilename);
                            } else if (this.mSeekableFileDescriptor != null) {
                                Os.lseek(this.mSeekableFileDescriptor, 0L, OsConstants.SEEK_SET);
                                fileOutputStream2 = new FileOutputStream(this.mSeekableFileDescriptor);
                            } else {
                                fileOutputStream2 = null;
                            }
                            saveJpegAttributes(fileInputStream2, fileOutputStream2);
                            IoUtils.closeQuietly(fileInputStream2);
                            IoUtils.closeQuietly(fileOutputStream2);
                            fileCreateTempFile.delete();
                            this.mThumbnailBytes = null;
                            return;
                        } catch (ErrnoException e2) {
                            e = e2;
                            fileInputStream3 = fileInputStream;
                            fileOutputStream = fileOutputStream;
                            try {
                                throw e.rethrowAsIOException();
                            } catch (Throwable th2) {
                                th = th2;
                                IoUtils.closeQuietly(fileInputStream3);
                                IoUtils.closeQuietly(fileOutputStream);
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            fileInputStream3 = fileInputStream;
                            IoUtils.closeQuietly(fileInputStream3);
                            IoUtils.closeQuietly(fileOutputStream);
                            throw th;
                        }
                    }
                    fileCreateTempFile = null;
                    fileInputStream = null;
                }
                saveJpegAttributes(fileInputStream2, fileOutputStream2);
                IoUtils.closeQuietly(fileInputStream2);
                IoUtils.closeQuietly(fileOutputStream2);
                fileCreateTempFile.delete();
                this.mThumbnailBytes = null;
                return;
            } catch (ErrnoException e3) {
                FileOutputStream fileOutputStream4 = fileOutputStream2;
                e = e3;
                fileInputStream4 = fileInputStream2;
                try {
                    throw e.rethrowAsIOException();
                } catch (Throwable th4) {
                    th = th4;
                    fileInputStream2 = fileInputStream4;
                    fileOutputStream3 = fileOutputStream4;
                    IoUtils.closeQuietly(fileInputStream2);
                    IoUtils.closeQuietly(fileOutputStream3);
                    fileCreateTempFile.delete();
                    throw th;
                }
            } catch (Throwable th5) {
                FileOutputStream fileOutputStream5 = fileOutputStream2;
                th = th5;
                fileOutputStream3 = fileOutputStream5;
                IoUtils.closeQuietly(fileInputStream2);
                IoUtils.closeQuietly(fileOutputStream3);
                fileCreateTempFile.delete();
                throw th;
            }
            autoCloseable = fileInputStream;
            IoUtils.closeQuietly(fileInputStream);
            IoUtils.closeQuietly(autoCloseable);
            fileInputStream2 = new FileInputStream(fileCreateTempFile);
            if (this.mFilename == null) {
            }
        } catch (ErrnoException e4) {
            e = e4;
            fileOutputStream = null;
        } catch (Throwable th6) {
            th = th6;
            fileOutputStream = null;
        }
    }

    public boolean hasThumbnail() {
        return this.mHasThumbnail;
    }

    public byte[] getThumbnail() {
        if (this.mThumbnailCompression == 6 || this.mThumbnailCompression == 7) {
            return getThumbnailBytes();
        }
        return null;
    }

    public byte[] getThumbnailBytes() throws Throwable {
        InputStream fileInputStream;
        if (!this.mHasThumbnail) {
            return null;
        }
        ?? r0 = this.mThumbnailBytes;
        try {
            if (r0 != 0) {
                return this.mThumbnailBytes;
            }
            try {
                try {
                    if (this.mAssetInputStream != null) {
                        fileInputStream = this.mAssetInputStream;
                        try {
                            if (!fileInputStream.markSupported()) {
                                Log.d(TAG, "Cannot read thumbnail from inputstream without mark/reset support");
                                IoUtils.closeQuietly(fileInputStream);
                                return null;
                            }
                            fileInputStream.reset();
                        } catch (ErrnoException | IOException e) {
                            e = e;
                            Log.d(TAG, "Encountered exception while getting thumbnail", e);
                            IoUtils.closeQuietly(fileInputStream);
                            return null;
                        }
                    } else if (this.mFilename != null) {
                        fileInputStream = new FileInputStream(this.mFilename);
                    } else if (this.mSeekableFileDescriptor != null) {
                        FileDescriptor fileDescriptorDup = Os.dup(this.mSeekableFileDescriptor);
                        Os.lseek(fileDescriptorDup, 0L, OsConstants.SEEK_SET);
                        fileInputStream = new FileInputStream(fileDescriptorDup);
                    } else {
                        fileInputStream = null;
                    }
                    if (fileInputStream == null) {
                        throw new FileNotFoundException();
                    }
                    if (fileInputStream.skip(this.mThumbnailOffset) != this.mThumbnailOffset) {
                        throw new IOException("Corrupted image");
                    }
                    byte[] bArr = new byte[this.mThumbnailLength];
                    if (fileInputStream.read(bArr) != this.mThumbnailLength) {
                        throw new IOException("Corrupted image");
                    }
                    this.mThumbnailBytes = bArr;
                    IoUtils.closeQuietly(fileInputStream);
                    return bArr;
                } catch (Throwable th) {
                    th = th;
                    r0 = 0;
                    IoUtils.closeQuietly((AutoCloseable) r0);
                    throw th;
                }
            } catch (ErrnoException | IOException e2) {
                e = e2;
                fileInputStream = null;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public Bitmap getThumbnailBitmap() {
        if (!this.mHasThumbnail) {
            return null;
        }
        if (this.mThumbnailBytes == null) {
            this.mThumbnailBytes = getThumbnailBytes();
        }
        if (this.mThumbnailCompression == 6 || this.mThumbnailCompression == 7) {
            return BitmapFactory.decodeByteArray(this.mThumbnailBytes, 0, this.mThumbnailLength);
        }
        if (this.mThumbnailCompression == 1) {
            int[] iArr = new int[this.mThumbnailBytes.length / 3];
            for (int i = 0; i < iArr.length; i++) {
                int i2 = 3 * i;
                iArr[i] = (this.mThumbnailBytes[i2] << WifiScanner.PnoSettings.PnoNetwork.FLAG_SAME_NETWORK) + 0 + (this.mThumbnailBytes[i2 + 1] << 8) + this.mThumbnailBytes[i2 + 2];
            }
            ExifAttribute exifAttribute = (ExifAttribute) this.mAttributes[4].get(TAG_IMAGE_LENGTH);
            ExifAttribute exifAttribute2 = (ExifAttribute) this.mAttributes[4].get(TAG_IMAGE_WIDTH);
            if (exifAttribute != null && exifAttribute2 != null) {
                return Bitmap.createBitmap(iArr, exifAttribute2.getIntValue(this.mExifByteOrder), exifAttribute.getIntValue(this.mExifByteOrder), Bitmap.Config.ARGB_8888);
            }
        }
        return null;
    }

    public boolean isThumbnailCompressed() {
        if (this.mHasThumbnail) {
            return this.mThumbnailCompression == 6 || this.mThumbnailCompression == 7;
        }
        return false;
    }

    public long[] getThumbnailRange() {
        if (!this.mHasThumbnail) {
            return null;
        }
        return new long[]{this.mThumbnailOffset, this.mThumbnailLength};
    }

    public boolean getLatLong(float[] fArr) {
        String attribute = getAttribute(TAG_GPS_LATITUDE);
        String attribute2 = getAttribute(TAG_GPS_LATITUDE_REF);
        String attribute3 = getAttribute(TAG_GPS_LONGITUDE);
        String attribute4 = getAttribute(TAG_GPS_LONGITUDE_REF);
        if (attribute != null && attribute2 != null && attribute3 != null && attribute4 != null) {
            try {
                fArr[0] = convertRationalLatLonToFloat(attribute, attribute2);
                fArr[1] = convertRationalLatLonToFloat(attribute3, attribute4);
                return true;
            } catch (IllegalArgumentException e) {
            }
        }
        return false;
    }

    public double getAltitude(double d) {
        double attributeDouble = getAttributeDouble(TAG_GPS_ALTITUDE, -1.0d);
        int attributeInt = getAttributeInt(TAG_GPS_ALTITUDE_REF, -1);
        if (attributeDouble < 0.0d || attributeInt < 0) {
            return d;
        }
        return attributeDouble * ((double) (attributeInt == 1 ? -1 : 1));
    }

    public long getDateTime() {
        String attribute = getAttribute(TAG_DATETIME);
        if (attribute == null || !sNonZeroTimePattern.matcher(attribute).matches()) {
            return -1L;
        }
        try {
            Date date = sFormatter.parse(attribute, new ParsePosition(0));
            if (date == null) {
                return -1L;
            }
            long time = date.getTime();
            String attribute2 = getAttribute(TAG_SUBSEC_TIME);
            if (attribute2 != null) {
                try {
                    long j = Long.parseLong(attribute2);
                    while (j > 1000) {
                        j /= 10;
                    }
                    return time + j;
                } catch (NumberFormatException e) {
                    return time;
                }
            }
            return time;
        } catch (IllegalArgumentException e2) {
            Log.e(TAG, "getDateTime: IllegalArgumentException!", e2);
            return -1L;
        }
    }

    public long getGpsDateTime() {
        String attribute = getAttribute(TAG_GPS_DATESTAMP);
        String attribute2 = getAttribute(TAG_GPS_TIMESTAMP);
        if (attribute == null || attribute2 == null || (!sNonZeroTimePattern.matcher(attribute).matches() && !sNonZeroTimePattern.matcher(attribute2).matches())) {
            return -1L;
        }
        try {
            Date date = sFormatter.parse(attribute + ' ' + attribute2, new ParsePosition(0));
            if (date == null) {
                return -1L;
            }
            return date.getTime();
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "getGpsDateTime: IllegalArgumentException!", e);
            return -1L;
        }
    }

    public static float convertRationalLatLonToFloat(String str, String str2) {
        try {
            String[] strArrSplit = str.split(",");
            String[] strArrSplit2 = strArrSplit[0].split("/");
            double d = Double.parseDouble(strArrSplit2[0].trim()) / Double.parseDouble(strArrSplit2[1].trim());
            String[] strArrSplit3 = strArrSplit[1].split("/");
            double d2 = Double.parseDouble(strArrSplit3[0].trim()) / Double.parseDouble(strArrSplit3[1].trim());
            String[] strArrSplit4 = strArrSplit[2].split("/");
            double d3 = d + (d2 / 60.0d) + ((Double.parseDouble(strArrSplit4[0].trim()) / Double.parseDouble(strArrSplit4[1].trim())) / 3600.0d);
            if (!str2.equals("S")) {
                if (!str2.equals("W")) {
                    return (float) d3;
                }
            }
            return (float) (-d3);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException();
        }
    }

    private int getMimeType(BufferedInputStream bufferedInputStream) throws IOException {
        bufferedInputStream.mark(5000);
        byte[] bArr = new byte[5000];
        bufferedInputStream.read(bArr);
        bufferedInputStream.reset();
        if (isJpegFormat(bArr)) {
            return 4;
        }
        if (isRafFormat(bArr)) {
            return 9;
        }
        if (isHeifFormat(bArr)) {
            return 12;
        }
        if (isOrfFormat(bArr)) {
            return 7;
        }
        if (isRw2Format(bArr)) {
            return 10;
        }
        return 0;
    }

    private static boolean isJpegFormat(byte[] bArr) throws IOException {
        for (int i = 0; i < JPEG_SIGNATURE.length; i++) {
            if (bArr[i] != JPEG_SIGNATURE[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isRafFormat(byte[] bArr) throws IOException {
        byte[] bytes = RAF_SIGNATURE.getBytes();
        for (int i = 0; i < bytes.length; i++) {
            if (bArr[i] != bytes[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean isHeifFormat(byte[] bArr) throws Throwable {
        ByteOrderedDataInputStream byteOrderedDataInputStream;
        try {
            byteOrderedDataInputStream = new ByteOrderedDataInputStream(bArr);
            try {
                byteOrderedDataInputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
                long length = byteOrderedDataInputStream.readInt();
                byte[] bArr2 = new byte[4];
                byteOrderedDataInputStream.read(bArr2);
                if (!Arrays.equals(bArr2, HEIF_TYPE_FTYP)) {
                    byteOrderedDataInputStream.close();
                    return false;
                }
                long j = 16;
                if (length == 1) {
                    length = byteOrderedDataInputStream.readLong();
                    if (length < 16) {
                        byteOrderedDataInputStream.close();
                        return false;
                    }
                } else {
                    j = 8;
                }
                if (length > bArr.length) {
                    length = bArr.length;
                }
                long j2 = length - j;
                if (j2 < 8) {
                    byteOrderedDataInputStream.close();
                    return false;
                }
                byte[] bArr3 = new byte[4];
                boolean z = false;
                boolean z2 = false;
                for (long j3 = 0; j3 < j2 / 4; j3++) {
                    if (byteOrderedDataInputStream.read(bArr3) != bArr3.length) {
                        byteOrderedDataInputStream.close();
                        return false;
                    }
                    if (j3 != 1) {
                        if (Arrays.equals(bArr3, HEIF_BRAND_MIF1)) {
                            z = true;
                        } else if (Arrays.equals(bArr3, HEIF_BRAND_HEIC)) {
                            z2 = true;
                        }
                        if (z && z2) {
                            byteOrderedDataInputStream.close();
                            return true;
                        }
                    }
                }
            } catch (Exception e) {
                if (byteOrderedDataInputStream != null) {
                }
                return false;
            } catch (Throwable th) {
                th = th;
                if (byteOrderedDataInputStream != null) {
                    byteOrderedDataInputStream.close();
                }
                throw th;
            }
        } catch (Exception e2) {
            byteOrderedDataInputStream = null;
        } catch (Throwable th2) {
            th = th2;
            byteOrderedDataInputStream = null;
        }
        byteOrderedDataInputStream.close();
        return false;
    }

    private boolean isOrfFormat(byte[] bArr) throws IOException {
        ByteOrderedDataInputStream byteOrderedDataInputStream = new ByteOrderedDataInputStream(bArr);
        this.mExifByteOrder = readByteOrder(byteOrderedDataInputStream);
        byteOrderedDataInputStream.setByteOrder(this.mExifByteOrder);
        short s = byteOrderedDataInputStream.readShort();
        if (s == 20306 || s == 21330) {
            return true;
        }
        return false;
    }

    private boolean isRw2Format(byte[] bArr) throws IOException {
        ByteOrderedDataInputStream byteOrderedDataInputStream = new ByteOrderedDataInputStream(bArr);
        this.mExifByteOrder = readByteOrder(byteOrderedDataInputStream);
        byteOrderedDataInputStream.setByteOrder(this.mExifByteOrder);
        if (byteOrderedDataInputStream.readShort() == 85) {
            return true;
        }
        return false;
    }

    private void getJpegAttributes(android.media.ExifInterface.ByteOrderedDataInputStream r9, int r10, int r11) throws java.io.IOException {
        r9.setByteOrder(java.nio.ByteOrder.BIG_ENDIAN);
        r9.seek((long) r10);
        r0 = r9.readByte();
        if (r0 == -1) {
            r10 = r10 + 1;
            if (r9.readByte() == -40) {
                r10 = r10 + 1;
                while (true) {
                    r0 = r9.readByte();
                    if (r0 == -1) {
                        r0 = r9.readByte();
                        r10 = (r10 + 1) + 1;
                        if (r0 == -39 || r0 == -38) {
                        } else {
                            r3 = r9.readUnsignedShort() + (-2);
                            r10 = r10 + 2;
                            if (r3 >= 0) {
                                if (r0 != -31) {
                                    if (r0 != -2) {
                                        switch (r0) {
                                            default:
                                                switch (r0) {
                                                    default:
                                                        switch (r0) {
                                                            default:
                                                                switch (r0) {
                                                                }
                                                            case android.security.keymaster.KeymasterDefs.KM_ERROR_CALLER_NONCE_PROHIBITED:
                                                            case android.security.keymaster.KeymasterDefs.KM_ERROR_KEY_RATE_LIMIT_EXCEEDED:
                                                            case android.security.keymaster.KeymasterDefs.KM_ERROR_MISSING_MAC_LENGTH:
                                                                if (r9.skipBytes(1) == 1) {
                                                                    r8.mAttributes[r11].put(android.media.ExifInterface.TAG_IMAGE_LENGTH, android.media.ExifInterface.ExifAttribute.createULong((long) r9.readUnsignedShort(), r8.mExifByteOrder));
                                                                    r8.mAttributes[r11].put(android.media.ExifInterface.TAG_IMAGE_WIDTH, android.media.ExifInterface.ExifAttribute.createULong((long) r9.readUnsignedShort(), r8.mExifByteOrder));
                                                                    r3 = r3 + (-5);
                                                                } else {
                                                                    throw new java.io.IOException("Invalid SOFx");
                                                                }
                                                        }
                                                    case android.security.keymaster.KeymasterDefs.KM_ERROR_UNSUPPORTED_MIN_MAC_LENGTH:
                                                    case android.security.keymaster.KeymasterDefs.KM_ERROR_MISSING_MIN_MAC_LENGTH:
                                                    case android.security.keymaster.KeymasterDefs.KM_ERROR_INVALID_MAC_LENGTH:
                                                }
                                            case -64:
                                            case -63:
                                            case -62:
                                            case -61:
                                        }
                                    } else {
                                        r0 = new byte[r3];
                                        if (r9.read(r0) == r3) {
                                            if (getAttribute(android.media.ExifInterface.TAG_USER_COMMENT) == null) {
                                                r8.mAttributes[1].put(android.media.ExifInterface.TAG_USER_COMMENT, android.media.ExifInterface.ExifAttribute.createString(new java.lang.String(r0, android.media.ExifInterface.ASCII)));
                                            }
                                            r3 = 0;
                                        } else {
                                            throw new java.io.IOException("Invalid exif");
                                        }
                                    }
                                } else {
                                    if (r3 >= 6) {
                                        r4 = new byte[6];
                                        if (r9.read(r4) == 6) {
                                            r10 = r10 + 6;
                                            r3 = r3 + (-6);
                                            if (java.util.Arrays.equals(r4, android.media.ExifInterface.IDENTIFIER_EXIF_APP1)) {
                                                if (r3 > 0) {
                                                    r8.mExifOffset = r10;
                                                    r0 = new byte[r3];
                                                    if (r9.read(r0) == r3) {
                                                        r10 = r10 + r3;
                                                        readExifSegment(r0, r11);
                                                        r3 = 0;
                                                    } else {
                                                        throw new java.io.IOException("Invalid exif");
                                                    }
                                                } else {
                                                    throw new java.io.IOException("Invalid exif");
                                                }
                                            }
                                        } else {
                                            throw new java.io.IOException("Invalid exif");
                                        }
                                    }
                                }
                                if (r3 >= 0) {
                                    if (r9.skipBytes(r3) == r3) {
                                        r10 = r10 + r3;
                                    } else {
                                        throw new java.io.IOException("Invalid JPEG segment");
                                    }
                                } else {
                                    throw new java.io.IOException("Invalid length");
                                }
                            } else {
                                throw new java.io.IOException("Invalid length");
                            }
                        }
                    } else {
                        r10 = new java.lang.StringBuilder();
                        r10.append("Invalid marker:");
                        r10.append(java.lang.Integer.toHexString(r0 & 255));
                        throw new java.io.IOException(r10.toString());
                    }
                }
            } else {
                r10 = new java.lang.StringBuilder();
                r10.append("Invalid marker: ");
                r10.append(java.lang.Integer.toHexString(r0 & 255));
                throw new java.io.IOException(r10.toString());
            }
        } else {
            r10 = new java.lang.StringBuilder();
            r10.append("Invalid marker: ");
            r10.append(java.lang.Integer.toHexString(r0 & 255));
            throw new java.io.IOException(r10.toString());
        }
    }

    private void getRawAttributes(ByteOrderedDataInputStream byteOrderedDataInputStream) throws IOException {
        ExifAttribute exifAttribute;
        parseTiffHeaders(byteOrderedDataInputStream, byteOrderedDataInputStream.available());
        readImageFileDirectory(byteOrderedDataInputStream, 0);
        updateImageSizeValues(byteOrderedDataInputStream, 0);
        updateImageSizeValues(byteOrderedDataInputStream, 5);
        updateImageSizeValues(byteOrderedDataInputStream, 4);
        validateImages(byteOrderedDataInputStream);
        if (this.mMimeType == 8 && (exifAttribute = (ExifAttribute) this.mAttributes[1].get(TAG_MAKER_NOTE)) != null) {
            ByteOrderedDataInputStream byteOrderedDataInputStream2 = new ByteOrderedDataInputStream(exifAttribute.bytes);
            byteOrderedDataInputStream2.setByteOrder(this.mExifByteOrder);
            byteOrderedDataInputStream2.seek(6L);
            readImageFileDirectory(byteOrderedDataInputStream2, 9);
            ExifAttribute exifAttribute2 = (ExifAttribute) this.mAttributes[9].get(TAG_COLOR_SPACE);
            if (exifAttribute2 != null) {
                this.mAttributes[1].put(TAG_COLOR_SPACE, exifAttribute2);
            }
        }
    }

    private void getRafAttributes(ByteOrderedDataInputStream byteOrderedDataInputStream) throws IOException {
        byteOrderedDataInputStream.skipBytes(84);
        byte[] bArr = new byte[4];
        byte[] bArr2 = new byte[4];
        byteOrderedDataInputStream.read(bArr);
        byteOrderedDataInputStream.skipBytes(4);
        byteOrderedDataInputStream.read(bArr2);
        int i = ByteBuffer.wrap(bArr).getInt();
        int i2 = ByteBuffer.wrap(bArr2).getInt();
        getJpegAttributes(byteOrderedDataInputStream, i, 5);
        byteOrderedDataInputStream.seek(i2);
        byteOrderedDataInputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
        int i3 = byteOrderedDataInputStream.readInt();
        for (int i4 = 0; i4 < i3; i4++) {
            int unsignedShort = byteOrderedDataInputStream.readUnsignedShort();
            int unsignedShort2 = byteOrderedDataInputStream.readUnsignedShort();
            if (unsignedShort == TAG_RAF_IMAGE_SIZE.number) {
                short s = byteOrderedDataInputStream.readShort();
                short s2 = byteOrderedDataInputStream.readShort();
                ExifAttribute exifAttributeCreateUShort = ExifAttribute.createUShort(s, this.mExifByteOrder);
                ExifAttribute exifAttributeCreateUShort2 = ExifAttribute.createUShort(s2, this.mExifByteOrder);
                this.mAttributes[0].put(TAG_IMAGE_LENGTH, exifAttributeCreateUShort);
                this.mAttributes[0].put(TAG_IMAGE_WIDTH, exifAttributeCreateUShort2);
                return;
            }
            byteOrderedDataInputStream.skipBytes(unsignedShort2);
        }
    }

    private void getHeifAttributes(final ByteOrderedDataInputStream byteOrderedDataInputStream) throws IOException {
        String strExtractMetadata;
        String strExtractMetadata2;
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        try {
            mediaMetadataRetriever.setDataSource(new MediaDataSource() {
                long mPosition;

                @Override
                public void close() throws IOException {
                }

                @Override
                public int readAt(long j, byte[] bArr, int i, int i2) throws IOException {
                    if (i2 == 0) {
                        return 0;
                    }
                    if (j < 0) {
                        return -1;
                    }
                    try {
                        if (this.mPosition != j) {
                            byteOrderedDataInputStream.seek(j);
                            this.mPosition = j;
                        }
                        int i3 = byteOrderedDataInputStream.read(bArr, i, i2);
                        if (i3 >= 0) {
                            this.mPosition += (long) i3;
                            return i3;
                        }
                    } catch (IOException e) {
                    }
                    this.mPosition = -1L;
                    return -1;
                }

                @Override
                public long getSize() throws IOException {
                    return -1L;
                }
            });
            String strExtractMetadata3 = mediaMetadataRetriever.extractMetadata(33);
            String strExtractMetadata4 = mediaMetadataRetriever.extractMetadata(34);
            String strExtractMetadata5 = mediaMetadataRetriever.extractMetadata(26);
            String strExtractMetadata6 = mediaMetadataRetriever.extractMetadata(17);
            String strExtractMetadata7 = null;
            if ("yes".equals(strExtractMetadata5)) {
                strExtractMetadata7 = mediaMetadataRetriever.extractMetadata(29);
                strExtractMetadata = mediaMetadataRetriever.extractMetadata(30);
                strExtractMetadata2 = mediaMetadataRetriever.extractMetadata(31);
            } else if ("yes".equals(strExtractMetadata6)) {
                strExtractMetadata7 = mediaMetadataRetriever.extractMetadata(18);
                strExtractMetadata = mediaMetadataRetriever.extractMetadata(19);
                strExtractMetadata2 = mediaMetadataRetriever.extractMetadata(24);
            } else {
                strExtractMetadata = null;
                strExtractMetadata2 = null;
            }
            if (strExtractMetadata7 != null) {
                this.mAttributes[0].put(TAG_IMAGE_WIDTH, ExifAttribute.createUShort(Integer.parseInt(strExtractMetadata7), this.mExifByteOrder));
            }
            if (strExtractMetadata != null) {
                this.mAttributes[0].put(TAG_IMAGE_LENGTH, ExifAttribute.createUShort(Integer.parseInt(strExtractMetadata), this.mExifByteOrder));
            }
            if (strExtractMetadata2 != null) {
                int i = Integer.parseInt(strExtractMetadata2);
                this.mAttributes[0].put(TAG_ORIENTATION, ExifAttribute.createUShort(i != 90 ? i != 180 ? i != 270 ? 1 : 8 : 3 : 6, this.mExifByteOrder));
            }
            if (strExtractMetadata3 != null && strExtractMetadata4 != null) {
                int i2 = Integer.parseInt(strExtractMetadata3);
                int i3 = Integer.parseInt(strExtractMetadata4);
                if (i3 <= 6) {
                    throw new IOException("Invalid exif length");
                }
                byteOrderedDataInputStream.seek(i2);
                byte[] bArr = new byte[6];
                if (byteOrderedDataInputStream.read(bArr) != 6) {
                    throw new IOException("Can't read identifier");
                }
                int i4 = i3 - 6;
                if (!Arrays.equals(bArr, IDENTIFIER_EXIF_APP1)) {
                    throw new IOException("Invalid identifier");
                }
                byte[] bArr2 = new byte[i4];
                if (byteOrderedDataInputStream.read(bArr2) != i4) {
                    throw new IOException("Can't read exif");
                }
                readExifSegment(bArr2, 0);
            }
        } finally {
            mediaMetadataRetriever.release();
        }
    }

    private void getOrfAttributes(ByteOrderedDataInputStream byteOrderedDataInputStream) throws IOException {
        getRawAttributes(byteOrderedDataInputStream);
        ExifAttribute exifAttribute = (ExifAttribute) this.mAttributes[1].get(TAG_MAKER_NOTE);
        if (exifAttribute != null) {
            ByteOrderedDataInputStream byteOrderedDataInputStream2 = new ByteOrderedDataInputStream(exifAttribute.bytes);
            byteOrderedDataInputStream2.setByteOrder(this.mExifByteOrder);
            byte[] bArr = new byte[ORF_MAKER_NOTE_HEADER_1.length];
            byteOrderedDataInputStream2.readFully(bArr);
            byteOrderedDataInputStream2.seek(0L);
            byte[] bArr2 = new byte[ORF_MAKER_NOTE_HEADER_2.length];
            byteOrderedDataInputStream2.readFully(bArr2);
            if (Arrays.equals(bArr, ORF_MAKER_NOTE_HEADER_1)) {
                byteOrderedDataInputStream2.seek(8L);
            } else if (Arrays.equals(bArr2, ORF_MAKER_NOTE_HEADER_2)) {
                byteOrderedDataInputStream2.seek(12L);
            }
            readImageFileDirectory(byteOrderedDataInputStream2, 6);
            ExifAttribute exifAttribute2 = (ExifAttribute) this.mAttributes[7].get(TAG_ORF_PREVIEW_IMAGE_START);
            ExifAttribute exifAttribute3 = (ExifAttribute) this.mAttributes[7].get(TAG_ORF_PREVIEW_IMAGE_LENGTH);
            if (exifAttribute2 != null && exifAttribute3 != null) {
                this.mAttributes[5].put(TAG_JPEG_INTERCHANGE_FORMAT, exifAttribute2);
                this.mAttributes[5].put(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, exifAttribute3);
            }
            ExifAttribute exifAttribute4 = (ExifAttribute) this.mAttributes[8].get(TAG_ORF_ASPECT_FRAME);
            if (exifAttribute4 != null) {
                int[] iArr = new int[4];
                int[] iArr2 = (int[]) exifAttribute4.getValue(this.mExifByteOrder);
                if (iArr2[2] > iArr2[0] && iArr2[3] > iArr2[1]) {
                    int i = (iArr2[2] - iArr2[0]) + 1;
                    int i2 = (iArr2[3] - iArr2[1]) + 1;
                    if (i < i2) {
                        int i3 = i + i2;
                        i2 = i3 - i2;
                        i = i3 - i2;
                    }
                    ExifAttribute exifAttributeCreateUShort = ExifAttribute.createUShort(i, this.mExifByteOrder);
                    ExifAttribute exifAttributeCreateUShort2 = ExifAttribute.createUShort(i2, this.mExifByteOrder);
                    this.mAttributes[0].put(TAG_IMAGE_WIDTH, exifAttributeCreateUShort);
                    this.mAttributes[0].put(TAG_IMAGE_LENGTH, exifAttributeCreateUShort2);
                }
            }
        }
    }

    private void getRw2Attributes(ByteOrderedDataInputStream byteOrderedDataInputStream) throws IOException {
        getRawAttributes(byteOrderedDataInputStream);
        if (((ExifAttribute) this.mAttributes[0].get(TAG_RW2_JPG_FROM_RAW)) != null) {
            getJpegAttributes(byteOrderedDataInputStream, this.mRw2JpgFromRawOffset, 5);
        }
        ExifAttribute exifAttribute = (ExifAttribute) this.mAttributes[0].get(TAG_RW2_ISO);
        ExifAttribute exifAttribute2 = (ExifAttribute) this.mAttributes[1].get("ISOSpeedRatings");
        if (exifAttribute != null && exifAttribute2 == null) {
            this.mAttributes[1].put("ISOSpeedRatings", exifAttribute);
        }
    }

    private void saveJpegAttributes(InputStream inputStream, OutputStream outputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        ByteOrderedDataOutputStream byteOrderedDataOutputStream = new ByteOrderedDataOutputStream(outputStream, ByteOrder.BIG_ENDIAN);
        if (dataInputStream.readByte() != -1) {
            throw new IOException("Invalid marker");
        }
        byteOrderedDataOutputStream.writeByte(-1);
        if (dataInputStream.readByte() != -40) {
            throw new IOException("Invalid marker");
        }
        byteOrderedDataOutputStream.writeByte(-40);
        byteOrderedDataOutputStream.writeByte(-1);
        byteOrderedDataOutputStream.writeByte(-31);
        writeExifSegment(byteOrderedDataOutputStream, 6);
        byte[] bArr = new byte[4096];
        while (dataInputStream.readByte() == -1) {
            byte b = dataInputStream.readByte();
            if (b == -31) {
                int unsignedShort = dataInputStream.readUnsignedShort() - 2;
                if (unsignedShort < 0) {
                    throw new IOException("Invalid length");
                }
                byte[] bArr2 = new byte[6];
                if (unsignedShort >= 6) {
                    if (dataInputStream.read(bArr2) != 6) {
                        throw new IOException("Invalid exif");
                    }
                    if (Arrays.equals(bArr2, IDENTIFIER_EXIF_APP1)) {
                        int i = unsignedShort - 6;
                        if (dataInputStream.skipBytes(i) != i) {
                            throw new IOException("Invalid length");
                        }
                    }
                }
                byteOrderedDataOutputStream.writeByte(-1);
                byteOrderedDataOutputStream.writeByte(b);
                byteOrderedDataOutputStream.writeUnsignedShort(unsignedShort + 2);
                if (unsignedShort >= 6) {
                    unsignedShort -= 6;
                    byteOrderedDataOutputStream.write(bArr2);
                }
                while (unsignedShort > 0) {
                    int i2 = dataInputStream.read(bArr, 0, Math.min(unsignedShort, bArr.length));
                    if (i2 >= 0) {
                        byteOrderedDataOutputStream.write(bArr, 0, i2);
                        unsignedShort -= i2;
                    }
                }
            } else {
                switch (b) {
                    case KeymasterDefs.KM_ERROR_UNSUPPORTED_TAG:
                    case -38:
                        byteOrderedDataOutputStream.writeByte(-1);
                        byteOrderedDataOutputStream.writeByte(b);
                        Streams.copy(dataInputStream, byteOrderedDataOutputStream);
                        return;
                    default:
                        byteOrderedDataOutputStream.writeByte(-1);
                        byteOrderedDataOutputStream.writeByte(b);
                        int unsignedShort2 = dataInputStream.readUnsignedShort();
                        byteOrderedDataOutputStream.writeUnsignedShort(unsignedShort2);
                        int i3 = unsignedShort2 - 2;
                        if (i3 < 0) {
                            throw new IOException("Invalid length");
                        }
                        while (i3 > 0) {
                            int i4 = dataInputStream.read(bArr, 0, Math.min(i3, bArr.length));
                            if (i4 >= 0) {
                                byteOrderedDataOutputStream.write(bArr, 0, i4);
                                i3 -= i4;
                            }
                        }
                        break;
                        break;
                }
            }
        }
        throw new IOException("Invalid marker");
    }

    private void readExifSegment(byte[] bArr, int i) throws IOException {
        ByteOrderedDataInputStream byteOrderedDataInputStream = new ByteOrderedDataInputStream(bArr);
        parseTiffHeaders(byteOrderedDataInputStream, bArr.length);
        readImageFileDirectory(byteOrderedDataInputStream, i);
    }

    private void addDefaultValuesForCompatibility() {
        String attribute = getAttribute(TAG_DATETIME_ORIGINAL);
        if (attribute != null && getAttribute(TAG_DATETIME) == null) {
            this.mAttributes[0].put(TAG_DATETIME, ExifAttribute.createString(attribute));
        }
        if (getAttribute(TAG_IMAGE_WIDTH) == null) {
            this.mAttributes[0].put(TAG_IMAGE_WIDTH, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (getAttribute(TAG_IMAGE_LENGTH) == null) {
            this.mAttributes[0].put(TAG_IMAGE_LENGTH, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (getAttribute(TAG_ORIENTATION) == null) {
            this.mAttributes[0].put(TAG_ORIENTATION, ExifAttribute.createUShort(0, this.mExifByteOrder));
        }
        if (getAttribute(TAG_LIGHT_SOURCE) == null) {
            this.mAttributes[1].put(TAG_LIGHT_SOURCE, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
    }

    private ByteOrder readByteOrder(ByteOrderedDataInputStream byteOrderedDataInputStream) throws IOException {
        short s = byteOrderedDataInputStream.readShort();
        if (s == 18761) {
            return ByteOrder.LITTLE_ENDIAN;
        }
        if (s == 19789) {
            return ByteOrder.BIG_ENDIAN;
        }
        throw new IOException("Invalid byte order: " + Integer.toHexString(s));
    }

    private void parseTiffHeaders(ByteOrderedDataInputStream byteOrderedDataInputStream, int i) throws IOException {
        this.mExifByteOrder = readByteOrder(byteOrderedDataInputStream);
        byteOrderedDataInputStream.setByteOrder(this.mExifByteOrder);
        int unsignedShort = byteOrderedDataInputStream.readUnsignedShort();
        if (this.mMimeType != 7 && this.mMimeType != 10 && unsignedShort != 42) {
            throw new IOException("Invalid start code: " + Integer.toHexString(unsignedShort));
        }
        int i2 = byteOrderedDataInputStream.readInt();
        if (i2 < 8 || i2 >= i) {
            throw new IOException("Invalid first Ifd offset: " + i2);
        }
        int i3 = i2 - 8;
        if (i3 > 0 && byteOrderedDataInputStream.skipBytes(i3) != i3) {
            throw new IOException("Couldn't jump to first Ifd: " + i3);
        }
    }

    private void readImageFileDirectory(ByteOrderedDataInputStream byteOrderedDataInputStream, int i) throws IOException {
        short s;
        long j;
        boolean z;
        short s2;
        int i2;
        int i3;
        if (byteOrderedDataInputStream.mPosition + 2 > byteOrderedDataInputStream.mLength) {
            return;
        }
        short s3 = byteOrderedDataInputStream.readShort();
        if (byteOrderedDataInputStream.mPosition + (12 * s3) > byteOrderedDataInputStream.mLength || s3 <= 0) {
            return;
        }
        short s4 = 0;
        while (s4 < s3) {
            int unsignedShort = byteOrderedDataInputStream.readUnsignedShort();
            int unsignedShort2 = byteOrderedDataInputStream.readUnsignedShort();
            int i4 = byteOrderedDataInputStream.readInt();
            long jPeek = byteOrderedDataInputStream.peek() + 4;
            ExifTag exifTag = (ExifTag) sExifTagMapsForReading[i].get(Integer.valueOf(unsignedShort));
            if (exifTag == null) {
                Log.w(TAG, "Skip the tag entry since tag number is not defined: " + unsignedShort);
                s = s3;
            } else if (unsignedShort2 <= 0 || unsignedShort2 >= IFD_FORMAT_BYTES_PER_FORMAT.length) {
                s = s3;
                Log.w(TAG, "Skip the tag entry since data format is invalid: " + unsignedShort2);
            } else {
                s = s3;
                j = ((long) i4) * ((long) IFD_FORMAT_BYTES_PER_FORMAT[unsignedShort2]);
                if (j < 0 || j > 2147483647L) {
                    Log.w(TAG, "Skip the tag entry since the number of components is invalid: " + i4);
                    z = false;
                } else {
                    z = true;
                }
                if (z) {
                    byteOrderedDataInputStream.seek(jPeek);
                    s2 = s4;
                } else {
                    if (j > 4) {
                        int i5 = byteOrderedDataInputStream.readInt();
                        if (this.mMimeType == 7) {
                            if (exifTag.name == TAG_MAKER_NOTE) {
                                this.mOrfMakerNoteOffset = i5;
                            } else if (i == 6 && exifTag.name == TAG_ORF_THUMBNAIL_IMAGE) {
                                this.mOrfThumbnailOffset = i5;
                                this.mOrfThumbnailLength = i4;
                                ExifAttribute exifAttributeCreateUShort = ExifAttribute.createUShort(6, this.mExifByteOrder);
                                i2 = unsignedShort2;
                                i3 = i4;
                                ExifAttribute exifAttributeCreateULong = ExifAttribute.createULong(this.mOrfThumbnailOffset, this.mExifByteOrder);
                                ExifAttribute exifAttributeCreateULong2 = ExifAttribute.createULong(this.mOrfThumbnailLength, this.mExifByteOrder);
                                this.mAttributes[4].put(TAG_COMPRESSION, exifAttributeCreateUShort);
                                this.mAttributes[4].put(TAG_JPEG_INTERCHANGE_FORMAT, exifAttributeCreateULong);
                                this.mAttributes[4].put(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, exifAttributeCreateULong2);
                            }
                            i2 = unsignedShort2;
                            i3 = i4;
                        } else {
                            i2 = unsignedShort2;
                            i3 = i4;
                            if (this.mMimeType == 10 && exifTag.name == TAG_RW2_JPG_FROM_RAW) {
                                this.mRw2JpgFromRawOffset = i5;
                            }
                        }
                        long j2 = i5;
                        s2 = s4;
                        if (j2 + j <= byteOrderedDataInputStream.mLength) {
                            byteOrderedDataInputStream.seek(j2);
                        } else {
                            Log.w(TAG, "Skip the tag entry since data offset is invalid: " + i5);
                            byteOrderedDataInputStream.seek(jPeek);
                        }
                    } else {
                        s2 = s4;
                        i2 = unsignedShort2;
                        i3 = i4;
                    }
                    Integer num = sExifPointerTagMap.get(Integer.valueOf(unsignedShort));
                    if (num != null) {
                        long unsignedShort3 = -1;
                        switch (i2) {
                            case 3:
                                unsignedShort3 = byteOrderedDataInputStream.readUnsignedShort();
                                break;
                            case 4:
                                unsignedShort3 = byteOrderedDataInputStream.readUnsignedInt();
                                break;
                            case 8:
                                unsignedShort3 = byteOrderedDataInputStream.readShort();
                                break;
                            case 9:
                            case 13:
                                unsignedShort3 = byteOrderedDataInputStream.readInt();
                                break;
                        }
                        if (unsignedShort3 <= 0 || unsignedShort3 >= byteOrderedDataInputStream.mLength) {
                            Log.w(TAG, "Skip jump into the IFD since its offset is invalid: " + unsignedShort3);
                        } else if (this.mAttributesOffsets.contains(Integer.valueOf((int) unsignedShort3))) {
                            Log.w(TAG, "Skip jump into the IFD since it has already been read: IfdType " + num + " (at " + unsignedShort3 + ")");
                        } else {
                            this.mAttributesOffsets.add(Integer.valueOf(byteOrderedDataInputStream.mPosition));
                            byteOrderedDataInputStream.seek(unsignedShort3);
                            readImageFileDirectory(byteOrderedDataInputStream, num.intValue());
                        }
                        byteOrderedDataInputStream.seek(jPeek);
                    } else {
                        byte[] bArr = new byte[(int) j];
                        byteOrderedDataInputStream.readFully(bArr);
                        ExifAttribute exifAttribute = new ExifAttribute(i2, i3, bArr);
                        this.mAttributes[i].put(exifTag.name, exifAttribute);
                        if (exifTag.name == TAG_DNG_VERSION) {
                            this.mMimeType = 3;
                        }
                        if (((exifTag.name == TAG_MAKE || exifTag.name == TAG_MODEL) && exifAttribute.getStringValue(this.mExifByteOrder).contains(PEF_SIGNATURE)) || (exifTag.name == TAG_COMPRESSION && exifAttribute.getIntValue(this.mExifByteOrder) == 65535)) {
                            this.mMimeType = 8;
                        }
                        if (byteOrderedDataInputStream.peek() != jPeek) {
                            byteOrderedDataInputStream.seek(jPeek);
                        }
                    }
                }
                s4 = (short) (s2 + 1);
                s3 = s;
            }
            z = false;
            j = 0;
            if (z) {
            }
            s4 = (short) (s2 + 1);
            s3 = s;
        }
        if (byteOrderedDataInputStream.peek() + 4 <= byteOrderedDataInputStream.mLength) {
            int i6 = byteOrderedDataInputStream.readInt();
            long j3 = i6;
            if (j3 <= 0 || i6 >= byteOrderedDataInputStream.mLength) {
                Log.w(TAG, "Stop reading file since a wrong offset may cause an infinite loop: " + i6);
                return;
            }
            if (this.mAttributesOffsets.contains(Integer.valueOf(i6))) {
                Log.w(TAG, "Stop reading file since re-reading an IFD may cause an infinite loop: " + i6);
                return;
            }
            this.mAttributesOffsets.add(Integer.valueOf(byteOrderedDataInputStream.mPosition));
            byteOrderedDataInputStream.seek(j3);
            if (this.mAttributes[4].isEmpty()) {
                readImageFileDirectory(byteOrderedDataInputStream, 4);
            } else if (this.mAttributes[5].isEmpty()) {
                readImageFileDirectory(byteOrderedDataInputStream, 5);
            }
        }
    }

    private void retrieveJpegImageSize(ByteOrderedDataInputStream byteOrderedDataInputStream, int i) throws IOException {
        ExifAttribute exifAttribute;
        ExifAttribute exifAttribute2 = (ExifAttribute) this.mAttributes[i].get(TAG_IMAGE_LENGTH);
        ExifAttribute exifAttribute3 = (ExifAttribute) this.mAttributes[i].get(TAG_IMAGE_WIDTH);
        if ((exifAttribute2 == null || exifAttribute3 == null) && (exifAttribute = (ExifAttribute) this.mAttributes[i].get(TAG_JPEG_INTERCHANGE_FORMAT)) != null) {
            getJpegAttributes(byteOrderedDataInputStream, exifAttribute.getIntValue(this.mExifByteOrder), i);
        }
    }

    private void setThumbnailData(ByteOrderedDataInputStream byteOrderedDataInputStream) throws IOException {
        HashMap map = this.mAttributes[4];
        ExifAttribute exifAttribute = (ExifAttribute) map.get(TAG_COMPRESSION);
        if (exifAttribute != null) {
            this.mThumbnailCompression = exifAttribute.getIntValue(this.mExifByteOrder);
            int i = this.mThumbnailCompression;
            if (i != 1) {
                switch (i) {
                    case 6:
                        handleThumbnailFromJfif(byteOrderedDataInputStream, map);
                        break;
                }
            }
            if (isSupportedDataType(map)) {
                handleThumbnailFromStrips(byteOrderedDataInputStream, map);
                return;
            }
            return;
        }
        handleThumbnailFromJfif(byteOrderedDataInputStream, map);
    }

    private void handleThumbnailFromJfif(ByteOrderedDataInputStream byteOrderedDataInputStream, HashMap map) throws IOException {
        ExifAttribute exifAttribute = (ExifAttribute) map.get(TAG_JPEG_INTERCHANGE_FORMAT);
        ExifAttribute exifAttribute2 = (ExifAttribute) map.get(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH);
        if (exifAttribute != null && exifAttribute2 != null) {
            int intValue = exifAttribute.getIntValue(this.mExifByteOrder);
            int iMin = Math.min(exifAttribute2.getIntValue(this.mExifByteOrder), byteOrderedDataInputStream.available() - intValue);
            if (this.mMimeType == 4 || this.mMimeType == 9 || this.mMimeType == 10) {
                intValue += this.mExifOffset;
            } else if (this.mMimeType == 7) {
                intValue += this.mOrfMakerNoteOffset;
            }
            if (intValue > 0 && iMin > 0) {
                this.mHasThumbnail = true;
                this.mThumbnailOffset = intValue;
                this.mThumbnailLength = iMin;
                this.mThumbnailCompression = 6;
                if (this.mFilename == null && this.mAssetInputStream == null && this.mSeekableFileDescriptor == null) {
                    byte[] bArr = new byte[iMin];
                    byteOrderedDataInputStream.seek(intValue);
                    byteOrderedDataInputStream.readFully(bArr);
                    this.mThumbnailBytes = bArr;
                }
            }
        }
    }

    private void handleThumbnailFromStrips(ByteOrderedDataInputStream byteOrderedDataInputStream, HashMap map) throws IOException {
        ExifAttribute exifAttribute = (ExifAttribute) map.get(TAG_STRIP_OFFSETS);
        ExifAttribute exifAttribute2 = (ExifAttribute) map.get(TAG_STRIP_BYTE_COUNTS);
        if (exifAttribute != null && exifAttribute2 != null) {
            long[] jArrConvertToLongArray = convertToLongArray(exifAttribute.getValue(this.mExifByteOrder));
            long[] jArrConvertToLongArray2 = convertToLongArray(exifAttribute2.getValue(this.mExifByteOrder));
            if (jArrConvertToLongArray == null) {
                Log.w(TAG, "stripOffsets should not be null.");
                return;
            }
            if (jArrConvertToLongArray2 == null) {
                Log.w(TAG, "stripByteCounts should not be null.");
                return;
            }
            byte[] bArr = new byte[(int) Arrays.stream(jArrConvertToLongArray2).sum()];
            int i = 0;
            int length = 0;
            for (int i2 = 0; i2 < jArrConvertToLongArray.length; i2++) {
                int i3 = (int) jArrConvertToLongArray[i2];
                int i4 = (int) jArrConvertToLongArray2[i2];
                int i5 = i3 - i;
                if (i5 < 0) {
                    Log.d(TAG, "Invalid strip offset value");
                }
                byteOrderedDataInputStream.seek(i5);
                int i6 = i + i5;
                byte[] bArr2 = new byte[i4];
                byteOrderedDataInputStream.read(bArr2);
                i = i6 + i4;
                System.arraycopy(bArr2, 0, bArr, length, bArr2.length);
                length += bArr2.length;
            }
            this.mHasThumbnail = true;
            this.mThumbnailBytes = bArr;
            this.mThumbnailLength = bArr.length;
        }
    }

    private boolean isSupportedDataType(HashMap map) throws IOException {
        ExifAttribute exifAttribute;
        ExifAttribute exifAttribute2 = (ExifAttribute) map.get(TAG_BITS_PER_SAMPLE);
        if (exifAttribute2 != null) {
            int[] iArr = (int[]) exifAttribute2.getValue(this.mExifByteOrder);
            if (Arrays.equals(BITS_PER_SAMPLE_RGB, iArr)) {
                return true;
            }
            if (this.mMimeType == 3 && (exifAttribute = (ExifAttribute) map.get(TAG_PHOTOMETRIC_INTERPRETATION)) != null) {
                int intValue = exifAttribute.getIntValue(this.mExifByteOrder);
                return (intValue == 1 && Arrays.equals(iArr, BITS_PER_SAMPLE_GREYSCALE_2)) || (intValue == 6 && Arrays.equals(iArr, BITS_PER_SAMPLE_RGB));
            }
            return false;
        }
        return false;
    }

    private boolean isThumbnail(HashMap map) throws IOException {
        ExifAttribute exifAttribute = (ExifAttribute) map.get(TAG_IMAGE_LENGTH);
        ExifAttribute exifAttribute2 = (ExifAttribute) map.get(TAG_IMAGE_WIDTH);
        if (exifAttribute != null && exifAttribute2 != null) {
            int intValue = exifAttribute.getIntValue(this.mExifByteOrder);
            int intValue2 = exifAttribute2.getIntValue(this.mExifByteOrder);
            if (intValue <= 512 && intValue2 <= 512) {
                return true;
            }
            return false;
        }
        return false;
    }

    private void validateImages(InputStream inputStream) throws IOException {
        swapBasedOnImageSize(0, 5);
        swapBasedOnImageSize(0, 4);
        swapBasedOnImageSize(5, 4);
        ExifAttribute exifAttribute = (ExifAttribute) this.mAttributes[1].get(TAG_PIXEL_X_DIMENSION);
        ExifAttribute exifAttribute2 = (ExifAttribute) this.mAttributes[1].get(TAG_PIXEL_Y_DIMENSION);
        if (exifAttribute != null && exifAttribute2 != null) {
            this.mAttributes[0].put(TAG_IMAGE_WIDTH, exifAttribute);
            this.mAttributes[0].put(TAG_IMAGE_LENGTH, exifAttribute2);
        }
        if (this.mAttributes[4].isEmpty() && isThumbnail(this.mAttributes[5])) {
            this.mAttributes[4] = this.mAttributes[5];
            this.mAttributes[5] = new HashMap();
        }
        if (!isThumbnail(this.mAttributes[4])) {
            Log.d(TAG, "No image meets the size requirements of a thumbnail image.");
        }
    }

    private void updateImageSizeValues(ByteOrderedDataInputStream byteOrderedDataInputStream, int i) throws IOException {
        ExifAttribute exifAttributeCreateUShort;
        ExifAttribute exifAttributeCreateUShort2;
        ExifAttribute exifAttribute = (ExifAttribute) this.mAttributes[i].get(TAG_DEFAULT_CROP_SIZE);
        ExifAttribute exifAttribute2 = (ExifAttribute) this.mAttributes[i].get(TAG_RW2_SENSOR_TOP_BORDER);
        ExifAttribute exifAttribute3 = (ExifAttribute) this.mAttributes[i].get(TAG_RW2_SENSOR_LEFT_BORDER);
        ExifAttribute exifAttribute4 = (ExifAttribute) this.mAttributes[i].get(TAG_RW2_SENSOR_BOTTOM_BORDER);
        ExifAttribute exifAttribute5 = (ExifAttribute) this.mAttributes[i].get(TAG_RW2_SENSOR_RIGHT_BORDER);
        if (exifAttribute != null) {
            if (exifAttribute.format == 5) {
                Rational[] rationalArr = (Rational[]) exifAttribute.getValue(this.mExifByteOrder);
                exifAttributeCreateUShort = ExifAttribute.createURational(rationalArr[0], this.mExifByteOrder);
                exifAttributeCreateUShort2 = ExifAttribute.createURational(rationalArr[1], this.mExifByteOrder);
            } else {
                int[] iArr = (int[]) exifAttribute.getValue(this.mExifByteOrder);
                exifAttributeCreateUShort = ExifAttribute.createUShort(iArr[0], this.mExifByteOrder);
                exifAttributeCreateUShort2 = ExifAttribute.createUShort(iArr[1], this.mExifByteOrder);
            }
            this.mAttributes[i].put(TAG_IMAGE_WIDTH, exifAttributeCreateUShort);
            this.mAttributes[i].put(TAG_IMAGE_LENGTH, exifAttributeCreateUShort2);
            return;
        }
        if (exifAttribute2 != null && exifAttribute3 != null && exifAttribute4 != null && exifAttribute5 != null) {
            int intValue = exifAttribute2.getIntValue(this.mExifByteOrder);
            int intValue2 = exifAttribute4.getIntValue(this.mExifByteOrder);
            int intValue3 = exifAttribute5.getIntValue(this.mExifByteOrder);
            int intValue4 = exifAttribute3.getIntValue(this.mExifByteOrder);
            if (intValue2 > intValue && intValue3 > intValue4) {
                ExifAttribute exifAttributeCreateUShort3 = ExifAttribute.createUShort(intValue2 - intValue, this.mExifByteOrder);
                ExifAttribute exifAttributeCreateUShort4 = ExifAttribute.createUShort(intValue3 - intValue4, this.mExifByteOrder);
                this.mAttributes[i].put(TAG_IMAGE_LENGTH, exifAttributeCreateUShort3);
                this.mAttributes[i].put(TAG_IMAGE_WIDTH, exifAttributeCreateUShort4);
                return;
            }
            return;
        }
        retrieveJpegImageSize(byteOrderedDataInputStream, i);
    }

    private int writeExifSegment(ByteOrderedDataOutputStream byteOrderedDataOutputStream, int i) throws IOException {
        int[] iArr = new int[EXIF_TAGS.length];
        int[] iArr2 = new int[EXIF_TAGS.length];
        for (ExifTag exifTag : EXIF_POINTER_TAGS) {
            removeAttribute(exifTag.name);
        }
        removeAttribute(JPEG_INTERCHANGE_FORMAT_TAG.name);
        removeAttribute(JPEG_INTERCHANGE_FORMAT_LENGTH_TAG.name);
        for (int i2 = 0; i2 < EXIF_TAGS.length; i2++) {
            for (Object obj : this.mAttributes[i2].entrySet().toArray()) {
                Map.Entry entry = (Map.Entry) obj;
                if (entry.getValue() == null) {
                    this.mAttributes[i2].remove(entry.getKey());
                }
            }
        }
        if (!this.mAttributes[1].isEmpty()) {
            this.mAttributes[0].put(EXIF_POINTER_TAGS[1].name, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (!this.mAttributes[2].isEmpty()) {
            this.mAttributes[0].put(EXIF_POINTER_TAGS[2].name, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (!this.mAttributes[3].isEmpty()) {
            this.mAttributes[1].put(EXIF_POINTER_TAGS[3].name, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (this.mHasThumbnail) {
            this.mAttributes[4].put(JPEG_INTERCHANGE_FORMAT_TAG.name, ExifAttribute.createULong(0L, this.mExifByteOrder));
            this.mAttributes[4].put(JPEG_INTERCHANGE_FORMAT_LENGTH_TAG.name, ExifAttribute.createULong(this.mThumbnailLength, this.mExifByteOrder));
        }
        for (int i3 = 0; i3 < EXIF_TAGS.length; i3++) {
            Iterator it = this.mAttributes[i3].entrySet().iterator();
            int i4 = 0;
            while (it.hasNext()) {
                int size = ((ExifAttribute) ((Map.Entry) it.next()).getValue()).size();
                if (size > 4) {
                    i4 += size;
                }
            }
            iArr2[i3] = iArr2[i3] + i4;
        }
        int size2 = 8;
        for (int i5 = 0; i5 < EXIF_TAGS.length; i5++) {
            if (!this.mAttributes[i5].isEmpty()) {
                iArr[i5] = size2;
                size2 += (this.mAttributes[i5].size() * 12) + 2 + 4 + iArr2[i5];
            }
        }
        if (this.mHasThumbnail) {
            this.mAttributes[4].put(JPEG_INTERCHANGE_FORMAT_TAG.name, ExifAttribute.createULong(size2, this.mExifByteOrder));
            this.mThumbnailOffset = i + size2;
            size2 += this.mThumbnailLength;
        }
        int i6 = size2 + 8;
        if (!this.mAttributes[1].isEmpty()) {
            this.mAttributes[0].put(EXIF_POINTER_TAGS[1].name, ExifAttribute.createULong(iArr[1], this.mExifByteOrder));
        }
        if (!this.mAttributes[2].isEmpty()) {
            this.mAttributes[0].put(EXIF_POINTER_TAGS[2].name, ExifAttribute.createULong(iArr[2], this.mExifByteOrder));
        }
        if (!this.mAttributes[3].isEmpty()) {
            this.mAttributes[1].put(EXIF_POINTER_TAGS[3].name, ExifAttribute.createULong(iArr[3], this.mExifByteOrder));
        }
        byteOrderedDataOutputStream.writeUnsignedShort(i6);
        byteOrderedDataOutputStream.write(IDENTIFIER_EXIF_APP1);
        byteOrderedDataOutputStream.writeShort(this.mExifByteOrder == ByteOrder.BIG_ENDIAN ? BYTE_ALIGN_MM : BYTE_ALIGN_II);
        byteOrderedDataOutputStream.setByteOrder(this.mExifByteOrder);
        byteOrderedDataOutputStream.writeUnsignedShort(42);
        byteOrderedDataOutputStream.writeUnsignedInt(8L);
        for (int i7 = 0; i7 < EXIF_TAGS.length; i7++) {
            if (!this.mAttributes[i7].isEmpty()) {
                byteOrderedDataOutputStream.writeUnsignedShort(this.mAttributes[i7].size());
                int size3 = iArr[i7] + 2 + (this.mAttributes[i7].size() * 12) + 4;
                for (Map.Entry entry2 : this.mAttributes[i7].entrySet()) {
                    int i8 = ((ExifTag) sExifTagMapsForWriting[i7].get(entry2.getKey())).number;
                    ExifAttribute exifAttribute = (ExifAttribute) entry2.getValue();
                    int size4 = exifAttribute.size();
                    byteOrderedDataOutputStream.writeUnsignedShort(i8);
                    byteOrderedDataOutputStream.writeUnsignedShort(exifAttribute.format);
                    byteOrderedDataOutputStream.writeInt(exifAttribute.numberOfComponents);
                    if (size4 > 4) {
                        byteOrderedDataOutputStream.writeUnsignedInt(size3);
                        size3 += size4;
                    } else {
                        byteOrderedDataOutputStream.write(exifAttribute.bytes);
                        if (size4 < 4) {
                            while (size4 < 4) {
                                byteOrderedDataOutputStream.writeByte(0);
                                size4++;
                            }
                        }
                    }
                }
                if (i7 == 0 && !this.mAttributes[4].isEmpty()) {
                    byteOrderedDataOutputStream.writeUnsignedInt(iArr[4]);
                } else {
                    byteOrderedDataOutputStream.writeUnsignedInt(0L);
                }
                Iterator it2 = this.mAttributes[i7].entrySet().iterator();
                while (it2.hasNext()) {
                    ExifAttribute exifAttribute2 = (ExifAttribute) ((Map.Entry) it2.next()).getValue();
                    if (exifAttribute2.bytes.length > 4) {
                        byteOrderedDataOutputStream.write(exifAttribute2.bytes, 0, exifAttribute2.bytes.length);
                    }
                }
            }
        }
        if (this.mHasThumbnail) {
            byteOrderedDataOutputStream.write(getThumbnailBytes());
        }
        byteOrderedDataOutputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
        return i6;
    }

    private static Pair<Integer, Integer> guessDataFormat(String str) {
        int iIntValue;
        int iIntValue2;
        if (str.contains(",")) {
            String[] strArrSplit = str.split(",");
            Pair<Integer, Integer> pairGuessDataFormat = guessDataFormat(strArrSplit[0]);
            if (pairGuessDataFormat.first.intValue() == 2) {
                return pairGuessDataFormat;
            }
            for (int i = 1; i < strArrSplit.length; i++) {
                Pair<Integer, Integer> pairGuessDataFormat2 = guessDataFormat(strArrSplit[i]);
                if (pairGuessDataFormat2.first == pairGuessDataFormat.first || pairGuessDataFormat2.second == pairGuessDataFormat.first) {
                    iIntValue = pairGuessDataFormat.first.intValue();
                } else {
                    iIntValue = -1;
                }
                if (pairGuessDataFormat.second.intValue() != -1 && (pairGuessDataFormat2.first == pairGuessDataFormat.second || pairGuessDataFormat2.second == pairGuessDataFormat.second)) {
                    iIntValue2 = pairGuessDataFormat.second.intValue();
                } else {
                    iIntValue2 = -1;
                }
                if (iIntValue == -1 && iIntValue2 == -1) {
                    return new Pair<>(2, -1);
                }
                if (iIntValue == -1) {
                    pairGuessDataFormat = new Pair<>(Integer.valueOf(iIntValue2), -1);
                } else if (iIntValue2 == -1) {
                    pairGuessDataFormat = new Pair<>(Integer.valueOf(iIntValue), -1);
                }
            }
            return pairGuessDataFormat;
        }
        if (str.contains("/")) {
            String[] strArrSplit2 = str.split("/");
            if (strArrSplit2.length == 2) {
                try {
                    long j = (long) Double.parseDouble(strArrSplit2[0]);
                    long j2 = (long) Double.parseDouble(strArrSplit2[1]);
                    if (j >= 0 && j2 >= 0) {
                        if (j <= 2147483647L && j2 <= 2147483647L) {
                            return new Pair<>(10, 5);
                        }
                        return new Pair<>(5, -1);
                    }
                    return new Pair<>(10, -1);
                } catch (NumberFormatException e) {
                }
            }
            return new Pair<>(2, -1);
        }
        try {
            Long lValueOf = Long.valueOf(Long.parseLong(str));
            if (lValueOf.longValue() >= 0 && lValueOf.longValue() <= 65535) {
                return new Pair<>(3, 4);
            }
            if (lValueOf.longValue() < 0) {
                return new Pair<>(9, -1);
            }
            return new Pair<>(4, -1);
        } catch (NumberFormatException e2) {
            try {
                Double.parseDouble(str);
                return new Pair<>(12, -1);
            } catch (NumberFormatException e3) {
                return new Pair<>(2, -1);
            }
        }
    }

    private static class ByteOrderedDataInputStream extends InputStream implements DataInput {
        private ByteOrder mByteOrder;
        private DataInputStream mDataInputStream;
        private InputStream mInputStream;
        private final int mLength;
        private int mPosition;
        private static final ByteOrder LITTLE_ENDIAN = ByteOrder.LITTLE_ENDIAN;
        private static final ByteOrder BIG_ENDIAN = ByteOrder.BIG_ENDIAN;

        public ByteOrderedDataInputStream(InputStream inputStream) throws IOException {
            this.mByteOrder = ByteOrder.BIG_ENDIAN;
            this.mInputStream = inputStream;
            this.mDataInputStream = new DataInputStream(inputStream);
            this.mLength = this.mDataInputStream.available();
            this.mPosition = 0;
            this.mDataInputStream.mark(this.mLength);
        }

        public ByteOrderedDataInputStream(byte[] bArr) throws IOException {
            this(new ByteArrayInputStream(bArr));
        }

        public void setByteOrder(ByteOrder byteOrder) {
            this.mByteOrder = byteOrder;
        }

        public void seek(long j) throws IOException {
            if (this.mPosition > j) {
                this.mPosition = 0;
                this.mDataInputStream.reset();
                this.mDataInputStream.mark(this.mLength);
            } else {
                j -= (long) this.mPosition;
            }
            int i = (int) j;
            if (skipBytes(i) != i) {
                throw new IOException("Couldn't seek up to the byteCount");
            }
        }

        public int peek() {
            return this.mPosition;
        }

        @Override
        public int available() throws IOException {
            return this.mDataInputStream.available();
        }

        @Override
        public int read() throws IOException {
            this.mPosition++;
            return this.mDataInputStream.read();
        }

        @Override
        public int readUnsignedByte() throws IOException {
            this.mPosition++;
            return this.mDataInputStream.readUnsignedByte();
        }

        @Override
        public String readLine() throws IOException {
            Log.d(ExifInterface.TAG, "Currently unsupported");
            return null;
        }

        @Override
        public boolean readBoolean() throws IOException {
            this.mPosition++;
            return this.mDataInputStream.readBoolean();
        }

        @Override
        public char readChar() throws IOException {
            this.mPosition += 2;
            return this.mDataInputStream.readChar();
        }

        @Override
        public String readUTF() throws IOException {
            this.mPosition += 2;
            return this.mDataInputStream.readUTF();
        }

        @Override
        public void readFully(byte[] bArr, int i, int i2) throws IOException {
            this.mPosition += i2;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            if (this.mDataInputStream.read(bArr, i, i2) != i2) {
                throw new IOException("Couldn't read up to the length of buffer");
            }
        }

        @Override
        public void readFully(byte[] bArr) throws IOException {
            this.mPosition += bArr.length;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            if (this.mDataInputStream.read(bArr, 0, bArr.length) != bArr.length) {
                throw new IOException("Couldn't read up to the length of buffer");
            }
        }

        @Override
        public byte readByte() throws IOException {
            this.mPosition++;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            int i = this.mDataInputStream.read();
            if (i < 0) {
                throw new EOFException();
            }
            return (byte) i;
        }

        @Override
        public short readShort() throws IOException {
            this.mPosition += 2;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            int i = this.mDataInputStream.read();
            int i2 = this.mDataInputStream.read();
            if ((i | i2) < 0) {
                throw new EOFException();
            }
            if (this.mByteOrder == LITTLE_ENDIAN) {
                return (short) ((i2 << 8) + i);
            }
            if (this.mByteOrder == BIG_ENDIAN) {
                return (short) ((i << 8) + i2);
            }
            throw new IOException("Invalid byte order: " + this.mByteOrder);
        }

        @Override
        public int readInt() throws IOException {
            this.mPosition += 4;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            int i = this.mDataInputStream.read();
            int i2 = this.mDataInputStream.read();
            int i3 = this.mDataInputStream.read();
            int i4 = this.mDataInputStream.read();
            if ((i | i2 | i3 | i4) < 0) {
                throw new EOFException();
            }
            if (this.mByteOrder == LITTLE_ENDIAN) {
                return (i4 << 24) + (i3 << 16) + (i2 << 8) + i;
            }
            if (this.mByteOrder == BIG_ENDIAN) {
                return (i << 24) + (i2 << 16) + (i3 << 8) + i4;
            }
            throw new IOException("Invalid byte order: " + this.mByteOrder);
        }

        @Override
        public int skipBytes(int i) throws IOException {
            int iMin = Math.min(i, this.mLength - this.mPosition);
            int iSkipBytes = 0;
            while (iSkipBytes < iMin) {
                iSkipBytes += this.mDataInputStream.skipBytes(iMin - iSkipBytes);
            }
            this.mPosition += iSkipBytes;
            return iSkipBytes;
        }

        @Override
        public int readUnsignedShort() throws IOException {
            this.mPosition += 2;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            int i = this.mDataInputStream.read();
            int i2 = this.mDataInputStream.read();
            if ((i | i2) < 0) {
                throw new EOFException();
            }
            if (this.mByteOrder == LITTLE_ENDIAN) {
                return (i2 << 8) + i;
            }
            if (this.mByteOrder == BIG_ENDIAN) {
                return (i << 8) + i2;
            }
            throw new IOException("Invalid byte order: " + this.mByteOrder);
        }

        public long readUnsignedInt() throws IOException {
            return ((long) readInt()) & 4294967295L;
        }

        @Override
        public long readLong() throws IOException {
            this.mPosition += 8;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            int i = this.mDataInputStream.read();
            int i2 = this.mDataInputStream.read();
            int i3 = this.mDataInputStream.read();
            int i4 = this.mDataInputStream.read();
            int i5 = this.mDataInputStream.read();
            int i6 = this.mDataInputStream.read();
            int i7 = this.mDataInputStream.read();
            int i8 = this.mDataInputStream.read();
            if ((i | i2 | i3 | i4 | i5 | i6 | i7 | i8) < 0) {
                throw new EOFException();
            }
            if (this.mByteOrder != LITTLE_ENDIAN) {
                if (this.mByteOrder != BIG_ENDIAN) {
                    throw new IOException("Invalid byte order: " + this.mByteOrder);
                }
                return (((long) i) << 56) + (((long) i2) << 48) + (((long) i3) << 40) + (((long) i4) << 32) + (((long) i5) << 24) + (((long) i6) << 16) + (((long) i7) << 8) + ((long) i8);
            }
            return (((long) i8) << 56) + (((long) i7) << 48) + (((long) i6) << 40) + (((long) i5) << 32) + (((long) i4) << 24) + (((long) i3) << 16) + (((long) i2) << 8) + ((long) i);
        }

        @Override
        public float readFloat() throws IOException {
            return Float.intBitsToFloat(readInt());
        }

        @Override
        public double readDouble() throws IOException {
            return Double.longBitsToDouble(readLong());
        }
    }

    private static class ByteOrderedDataOutputStream extends FilterOutputStream {
        private ByteOrder mByteOrder;
        private final OutputStream mOutputStream;

        public ByteOrderedDataOutputStream(OutputStream outputStream, ByteOrder byteOrder) {
            super(outputStream);
            this.mOutputStream = outputStream;
            this.mByteOrder = byteOrder;
        }

        public void setByteOrder(ByteOrder byteOrder) {
            this.mByteOrder = byteOrder;
        }

        @Override
        public void write(byte[] bArr) throws IOException {
            this.mOutputStream.write(bArr);
        }

        @Override
        public void write(byte[] bArr, int i, int i2) throws IOException {
            this.mOutputStream.write(bArr, i, i2);
        }

        public void writeByte(int i) throws IOException {
            this.mOutputStream.write(i);
        }

        public void writeShort(short s) throws IOException {
            if (this.mByteOrder == ByteOrder.LITTLE_ENDIAN) {
                this.mOutputStream.write((s >>> 0) & 255);
                this.mOutputStream.write((s >>> 8) & 255);
            } else if (this.mByteOrder == ByteOrder.BIG_ENDIAN) {
                this.mOutputStream.write((s >>> 8) & 255);
                this.mOutputStream.write((s >>> 0) & 255);
            }
        }

        public void writeInt(int i) throws IOException {
            if (this.mByteOrder == ByteOrder.LITTLE_ENDIAN) {
                this.mOutputStream.write((i >>> 0) & 255);
                this.mOutputStream.write((i >>> 8) & 255);
                this.mOutputStream.write((i >>> 16) & 255);
                this.mOutputStream.write((i >>> 24) & 255);
                return;
            }
            if (this.mByteOrder == ByteOrder.BIG_ENDIAN) {
                this.mOutputStream.write((i >>> 24) & 255);
                this.mOutputStream.write((i >>> 16) & 255);
                this.mOutputStream.write((i >>> 8) & 255);
                this.mOutputStream.write((i >>> 0) & 255);
            }
        }

        public void writeUnsignedShort(int i) throws IOException {
            writeShort((short) i);
        }

        public void writeUnsignedInt(long j) throws IOException {
            writeInt((int) j);
        }
    }

    private void swapBasedOnImageSize(int i, int i2) throws IOException {
        if (this.mAttributes[i].isEmpty() || this.mAttributes[i2].isEmpty()) {
            return;
        }
        ExifAttribute exifAttribute = (ExifAttribute) this.mAttributes[i].get(TAG_IMAGE_LENGTH);
        ExifAttribute exifAttribute2 = (ExifAttribute) this.mAttributes[i].get(TAG_IMAGE_WIDTH);
        ExifAttribute exifAttribute3 = (ExifAttribute) this.mAttributes[i2].get(TAG_IMAGE_LENGTH);
        ExifAttribute exifAttribute4 = (ExifAttribute) this.mAttributes[i2].get(TAG_IMAGE_WIDTH);
        if (exifAttribute != null && exifAttribute2 != null && exifAttribute3 != null && exifAttribute4 != null) {
            int intValue = exifAttribute.getIntValue(this.mExifByteOrder);
            int intValue2 = exifAttribute2.getIntValue(this.mExifByteOrder);
            int intValue3 = exifAttribute3.getIntValue(this.mExifByteOrder);
            int intValue4 = exifAttribute4.getIntValue(this.mExifByteOrder);
            if (intValue < intValue3 && intValue2 < intValue4) {
                HashMap map = this.mAttributes[i];
                this.mAttributes[i] = this.mAttributes[i2];
                this.mAttributes[i2] = map;
            }
        }
    }

    private boolean containsMatch(byte[] bArr, byte[] bArr2) {
        for (int i = 0; i < bArr.length - bArr2.length; i++) {
            for (int i2 = 0; i2 < bArr2.length && bArr[i + i2] == bArr2[i2]; i2++) {
                if (i2 == bArr2.length - 1) {
                    return true;
                }
            }
        }
        return false;
    }

    private static long[] convertToLongArray(Object obj) {
        if (obj instanceof int[]) {
            int[] iArr = (int[]) obj;
            long[] jArr = new long[iArr.length];
            for (int i = 0; i < iArr.length; i++) {
                jArr[i] = iArr[i];
            }
            return jArr;
        }
        if (obj instanceof long[]) {
            return (long[]) obj;
        }
        return null;
    }
}
