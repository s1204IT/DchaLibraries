package android.media;

import android.app.IActivityManager;
import android.bluetooth.BluetoothClass;
import android.content.res.AssetManager;
import android.net.ProxyInfo;
import android.net.wifi.AnqpInformationElement;
import android.opengl.GLES30;
import android.security.keymaster.KeymasterArguments;
import android.security.keymaster.KeymasterDefs;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.util.Pair;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
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
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import libcore.io.IoUtils;
import libcore.io.Streams;

public class ExifInterface {
    private static final short BYTE_ALIGN_II = 18761;
    private static final short BYTE_ALIGN_MM = 19789;
    private static final boolean DEBUG = false;
    private static final ExifTag[][] EXIF_TAGS;
    private static final int IFD_EXIF_HINT = 1;
    private static final ExifTag[] IFD_EXIF_TAGS;
    private static final int IFD_FORMAT_BYTE = 1;
    private static final int IFD_FORMAT_DOUBLE = 12;
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
    private static final int IFD_GPS_HINT = 2;
    private static final ExifTag[] IFD_GPS_TAGS;
    private static final int IFD_INTEROPERABILITY_HINT = 3;
    private static final ExifTag[] IFD_INTEROPERABILITY_TAGS;
    private static final ExifTag[] IFD_POINTER_TAGS;
    private static final int IFD_THUMBNAIL_HINT = 4;
    private static final ExifTag[] IFD_THUMBNAIL_TAGS;
    private static final int IFD_TIFF_HINT = 0;
    private static final ExifTag[] IFD_TIFF_TAGS;
    private static final ExifTag JPEG_INTERCHANGE_FORMAT_LENGTH_TAG;
    private static final ExifTag JPEG_INTERCHANGE_FORMAT_TAG;
    private static final int JPEG_SIGNATURE_SIZE = 3;
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
    public static final int ORIENTATION_FLIP_HORIZONTAL = 2;
    public static final int ORIENTATION_FLIP_VERTICAL = 4;
    public static final int ORIENTATION_NORMAL = 1;
    public static final int ORIENTATION_ROTATE_180 = 3;
    public static final int ORIENTATION_ROTATE_270 = 8;
    public static final int ORIENTATION_ROTATE_90 = 6;
    public static final int ORIENTATION_TRANSPOSE = 5;
    public static final int ORIENTATION_TRANSVERSE = 7;
    public static final int ORIENTATION_UNDEFINED = 0;
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
    public static final String TAG_DEVICE_SETTING_DESCRIPTION = "DeviceSettingDescription";
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
    public static final String TAG_MTK_CAMERA_REFOCUS = "MTKCameraRefocus";
    public static final String TAG_MTK_CONSHOT_FOCUS_HIGH = "MTKConshotFocusHigh";
    public static final String TAG_MTK_CONSHOT_FOCUS_LOW = "MTKConshotFocusLow";
    public static final String TAG_MTK_CONSHOT_GROUP_ID = "MTKConshotGroupID";
    public static final String TAG_MTK_CONSHOT_PIC_INDEX = "MTKConshotPicIndex";
    public static final String TAG_OECF = "OECF";
    public static final String TAG_ORIENTATION = "Orientation";
    public static final String TAG_PHOTOMETRIC_INTERPRETATION = "PhotometricInterpretation";
    public static final String TAG_PIXEL_X_DIMENSION = "PixelXDimension";
    public static final String TAG_PIXEL_Y_DIMENSION = "PixelYDimension";
    public static final String TAG_PLANAR_CONFIGURATION = "PlanarConfiguration";
    public static final String TAG_PRIMARY_CHROMATICITIES = "PrimaryChromaticities";
    public static final String TAG_REFERENCE_BLACK_WHITE = "ReferenceBlackWhite";
    public static final String TAG_RELATED_SOUND_FILE = "RelatedSoundFile";
    public static final String TAG_RESOLUTION_UNIT = "ResolutionUnit";
    public static final String TAG_ROWS_PER_STRIP = "RowsPerStrip";
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
    public static final String TAG_SUBJECT_AREA = "SubjectArea";
    public static final String TAG_SUBJECT_DISTANCE_RANGE = "SubjectDistanceRange";
    public static final String TAG_SUBJECT_LOCATION = "SubjectLocation";
    public static final String TAG_SUBSEC_TIME = "SubSecTime";
    public static final String TAG_SUBSEC_TIME_DIG = "SubSecTimeDigitized";
    public static final String TAG_SUBSEC_TIME_DIGITIZED = "SubSecTimeDigitized";
    public static final String TAG_SUBSEC_TIME_ORIG = "SubSecTimeOriginal";
    public static final String TAG_SUBSEC_TIME_ORIGINAL = "SubSecTimeOriginal";
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
    private static SimpleDateFormat sFormatter;
    private static final Pattern sGpsTimestampPattern;
    private static final Object sLock;
    private static final Pattern sNonZeroTimePattern;
    private final AssetManager.AssetInputStream mAssetInputStream;
    private final HashMap[] mAttributes;
    private ByteOrder mExifByteOrder;
    private final String mFilename;
    private boolean mHasThumbnail;
    private InputStream mInputStream;
    private final boolean mIsInputStream;
    private boolean mIsRaw;
    private boolean mIsSupportedFile;
    private final FileDescriptor mSeekableFileDescriptor;
    private byte[] mThumbnailBytes;
    private int mThumbnailLength;
    private int mThumbnailOffset;
    private static final byte MARKER_SOI = -40;
    private static final byte[] JPEG_SIGNATURE = {-1, MARKER_SOI, -1};
    private static final String[] IFD_FORMAT_NAMES = {ProxyInfo.LOCAL_EXCL_LIST, "BYTE", "STRING", "USHORT", "ULONG", "URATIONAL", "SBYTE", "UNDEFINED", "SSHORT", "SLONG", "SRATIONAL", "SINGLE", "DOUBLE"};
    private static final int[] IFD_FORMAT_BYTES_PER_FORMAT = {0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};
    private static final byte[] EXIF_ASCII_PREFIX = {65, 83, 67, 73, 73, 0, 0, 0};
    private static final int[] IFD_POINTER_TAG_HINTS = {1, 2, 3};
    public static final String TAG_DIGITAL_ZOOM_RATIO = "DigitalZoomRatio";
    public static final String TAG_EXPOSURE_TIME = "ExposureTime";
    public static final String TAG_SUBJECT_DISTANCE = "SubjectDistance";
    public static final String TAG_GPS_TIMESTAMP = "GPSTimeStamp";
    private static final HashSet<String> sTagSetForCompatibility = new HashSet<>(Arrays.asList("FNumber", TAG_DIGITAL_ZOOM_RATIO, TAG_EXPOSURE_TIME, TAG_SUBJECT_DISTANCE, TAG_GPS_TIMESTAMP));
    private static final Charset ASCII = Charset.forName("US-ASCII");
    private static final byte[] IDENTIFIER_EXIF_APP1 = "Exif\u0000\u0000".getBytes(ASCII);

    private static native HashMap nativeGetRawAttributesFromAsset(long j);

    private static native HashMap nativeGetRawAttributesFromFileDescriptor(FileDescriptor fileDescriptor);

    private static native HashMap nativeGetRawAttributesFromInputStream(InputStream inputStream);

    private static native byte[] nativeGetThumbnailFromAsset(long j, int i, int i2);

    private static native void nativeInitRaw();

    static {
        int i = 4;
        int i2 = 5;
        int i3 = 2;
        int i4 = 3;
        ExifTag exifTag = null;
        IFD_TIFF_TAGS = new ExifTag[]{new ExifTag(TAG_IMAGE_WIDTH, 256, i4, i, exifTag), new ExifTag(TAG_IMAGE_LENGTH, 257, i4, i, exifTag), new ExifTag(TAG_BITS_PER_SAMPLE, 258, i4, exifTag), new ExifTag(TAG_COMPRESSION, 259, i4, exifTag), new ExifTag(TAG_PHOTOMETRIC_INTERPRETATION, 262, i4, exifTag), new ExifTag(TAG_IMAGE_DESCRIPTION, 270, i3, exifTag), new ExifTag(TAG_MAKE, AnqpInformationElement.ANQP_EMERGENCY_NAI, i3, exifTag), new ExifTag(TAG_MODEL, 272, i3, exifTag), new ExifTag(TAG_STRIP_OFFSETS, 273, i4, i, exifTag), new ExifTag(TAG_ORIENTATION, 274, i4, exifTag), new ExifTag(TAG_SAMPLES_PER_PIXEL, 277, i4, exifTag), new ExifTag(TAG_ROWS_PER_STRIP, 278, i4, i, exifTag), new ExifTag(TAG_STRIP_BYTE_COUNTS, 279, i4, i, exifTag), new ExifTag(TAG_X_RESOLUTION, IActivityManager.CREATE_STACK_ON_DISPLAY, i2, exifTag), new ExifTag(TAG_Y_RESOLUTION, IActivityManager.GET_FOCUSED_STACK_ID_TRANSACTION, i2, exifTag), new ExifTag(TAG_PLANAR_CONFIGURATION, IActivityManager.SET_TASK_RESIZEABLE_TRANSACTION, i4, exifTag), new ExifTag(TAG_RESOLUTION_UNIT, IActivityManager.UPDATE_DEVICE_OWNER_TRANSACTION, i4, exifTag), new ExifTag(TAG_TRANSFER_FUNCTION, 301, i4, exifTag), new ExifTag(TAG_SOFTWARE, MediaFile.FILE_TYPE_WMV, i3, exifTag), new ExifTag(TAG_DATETIME, MediaFile.FILE_TYPE_ASF, i3, exifTag), new ExifTag(TAG_ARTIST, 315, i3, exifTag), new ExifTag(TAG_WHITE_POINT, 318, i2, exifTag), new ExifTag(TAG_PRIMARY_CHROMATICITIES, 319, i2, exifTag), new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, i, exifTag), new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, i, exifTag), new ExifTag(TAG_Y_CB_CR_COEFFICIENTS, 529, i2, exifTag), new ExifTag(TAG_Y_CB_CR_SUB_SAMPLING, 530, i4, exifTag), new ExifTag(TAG_Y_CB_CR_POSITIONING, 531, i4, exifTag), new ExifTag(TAG_REFERENCE_BLACK_WHITE, BluetoothClass.Device.PHONE_ISDN, i2, exifTag), new ExifTag(TAG_COPYRIGHT, 33432, i3, exifTag), new ExifTag(TAG_EXIF_IFD_POINTER, 34665, i, exifTag), new ExifTag(TAG_GPS_INFO_IFD_POINTER, GLES30.GL_DRAW_BUFFER0, i, exifTag)};
        IFD_EXIF_TAGS = new ExifTag[]{new ExifTag(TAG_EXPOSURE_TIME, 33434, i2, exifTag), new ExifTag("FNumber", 33437, i2, exifTag), new ExifTag(TAG_EXPOSURE_PROGRAM, 34850, i4, exifTag), new ExifTag(TAG_SPECTRAL_SENSITIVITY, GLES30.GL_MAX_DRAW_BUFFERS, i3, exifTag), new ExifTag("ISOSpeedRatings", GLES30.GL_DRAW_BUFFER2, i4, exifTag), new ExifTag(TAG_OECF, GLES30.GL_DRAW_BUFFER3, 7, exifTag), new ExifTag(TAG_EXIF_VERSION, 36864, i3, exifTag), new ExifTag(TAG_DATETIME_ORIGINAL, 36867, i3, exifTag), new ExifTag(TAG_DATETIME_DIGITIZED, 36868, i3, exifTag), new ExifTag(TAG_COMPONENTS_CONFIGURATION, 37121, 7, exifTag), new ExifTag(TAG_COMPRESSED_BITS_PER_PIXEL, 37122, i2, exifTag), new ExifTag(TAG_SHUTTER_SPEED_VALUE, 37377, 10, exifTag), new ExifTag(TAG_APERTURE_VALUE, 37378, i2, exifTag), new ExifTag(TAG_BRIGHTNESS_VALUE, 37379, 10, exifTag), new ExifTag(TAG_EXPOSURE_BIAS_VALUE, 37380, 10, exifTag), new ExifTag(TAG_MAX_APERTURE_VALUE, 37381, i2, exifTag), new ExifTag(TAG_SUBJECT_DISTANCE, 37382, i2, exifTag), new ExifTag(TAG_METERING_MODE, 37383, i4, exifTag), new ExifTag(TAG_LIGHT_SOURCE, 37384, i4, exifTag), new ExifTag(TAG_FLASH, 37385, i4, exifTag), new ExifTag(TAG_FOCAL_LENGTH, 37386, i2, exifTag), new ExifTag(TAG_SUBJECT_AREA, 37396, i4, exifTag), new ExifTag(TAG_MAKER_NOTE, 37500, 7, exifTag), new ExifTag(TAG_USER_COMMENT, 37510, 7, exifTag), new ExifTag(TAG_SUBSEC_TIME, 37520, i3, exifTag), new ExifTag("SubSecTimeOriginal", 37521, i3, exifTag), new ExifTag("SubSecTimeDigitized", 37522, i3, exifTag), new ExifTag(TAG_FLASHPIX_VERSION, 40960, 7, exifTag), new ExifTag(TAG_COLOR_SPACE, 40961, i4, exifTag), new ExifTag(TAG_PIXEL_X_DIMENSION, 40962, i4, i, exifTag), new ExifTag(TAG_PIXEL_Y_DIMENSION, 40963, i4, i, exifTag), new ExifTag(TAG_RELATED_SOUND_FILE, 40964, i3, exifTag), new ExifTag(TAG_INTEROPERABILITY_IFD_POINTER, 40965, i, exifTag), new ExifTag(TAG_FLASH_ENERGY, 41483, i2, exifTag), new ExifTag(TAG_SPATIAL_FREQUENCY_RESPONSE, 41484, 7, exifTag), new ExifTag(TAG_FOCAL_PLANE_X_RESOLUTION, 41486, i2, exifTag), new ExifTag(TAG_FOCAL_PLANE_Y_RESOLUTION, 41487, i2, exifTag), new ExifTag(TAG_FOCAL_PLANE_RESOLUTION_UNIT, 41488, i4, exifTag), new ExifTag(TAG_SUBJECT_LOCATION, 41492, i4, exifTag), new ExifTag(TAG_EXPOSURE_INDEX, 41493, i2, exifTag), new ExifTag(TAG_SENSING_METHOD, 41495, i4, exifTag), new ExifTag(TAG_FILE_SOURCE, 41728, 7, exifTag), new ExifTag(TAG_SCENE_TYPE, 41729, 7, exifTag), new ExifTag(TAG_CFA_PATTERN, 41730, 7, exifTag), new ExifTag(TAG_CUSTOM_RENDERED, 41985, i4, exifTag), new ExifTag(TAG_EXPOSURE_MODE, 41986, i4, exifTag), new ExifTag(TAG_WHITE_BALANCE, 41987, i4, exifTag), new ExifTag(TAG_DIGITAL_ZOOM_RATIO, 41988, i2, exifTag), new ExifTag(TAG_FOCAL_LENGTH_IN_35MM_FILM, 41989, i4, exifTag), new ExifTag(TAG_SCENE_CAPTURE_TYPE, 41990, i4, exifTag), new ExifTag(TAG_GAIN_CONTROL, 41991, i4, exifTag), new ExifTag(TAG_CONTRAST, 41992, i4, exifTag), new ExifTag(TAG_SATURATION, 41993, i4, exifTag), new ExifTag(TAG_SHARPNESS, 41994, i4, exifTag), new ExifTag(TAG_DEVICE_SETTING_DESCRIPTION, 41995, 7, exifTag), new ExifTag(TAG_SUBJECT_DISTANCE_RANGE, 41996, i4, exifTag), new ExifTag(TAG_IMAGE_UNIQUE_ID, 42016, i3, exifTag)};
        IFD_GPS_TAGS = new ExifTag[]{new ExifTag(TAG_GPS_VERSION_ID, 0, 1, exifTag), new ExifTag(TAG_GPS_LATITUDE_REF, 1, i3, exifTag), new ExifTag(TAG_GPS_LATITUDE, i3, i2, exifTag), new ExifTag(TAG_GPS_LONGITUDE_REF, i4, i3, exifTag), new ExifTag(TAG_GPS_LONGITUDE, i, i2, exifTag), new ExifTag(TAG_GPS_ALTITUDE_REF, i2, 1, exifTag), new ExifTag(TAG_GPS_ALTITUDE, 6, i2, exifTag), new ExifTag(TAG_GPS_TIMESTAMP, 7, i2, exifTag), new ExifTag(TAG_GPS_SATELLITES, 8, i3, exifTag), new ExifTag(TAG_GPS_STATUS, 9, i3, exifTag), new ExifTag(TAG_GPS_MEASURE_MODE, 10, i3, exifTag), new ExifTag(TAG_GPS_DOP, 11, i2, exifTag), new ExifTag(TAG_GPS_SPEED_REF, 12, i3, exifTag), new ExifTag(TAG_GPS_SPEED, 13, i2, exifTag), new ExifTag(TAG_GPS_TRACK_REF, 14, i3, exifTag), new ExifTag(TAG_GPS_TRACK, 15, i2, exifTag), new ExifTag(TAG_GPS_IMG_DIRECTION_REF, 16, i3, exifTag), new ExifTag(TAG_GPS_IMG_DIRECTION, 17, i2, exifTag), new ExifTag(TAG_GPS_MAP_DATUM, 18, i3, exifTag), new ExifTag(TAG_GPS_DEST_LATITUDE_REF, 19, i3, exifTag), new ExifTag(TAG_GPS_DEST_LATITUDE, 20, i2, exifTag), new ExifTag(TAG_GPS_DEST_LONGITUDE_REF, 21, i3, exifTag), new ExifTag(TAG_GPS_DEST_LONGITUDE, 22, i2, exifTag), new ExifTag(TAG_GPS_DEST_BEARING_REF, 23, i3, exifTag), new ExifTag(TAG_GPS_DEST_BEARING, 24, i2, exifTag), new ExifTag(TAG_GPS_DEST_DISTANCE_REF, 25, i3, exifTag), new ExifTag(TAG_GPS_DEST_DISTANCE, 26, i2, exifTag), new ExifTag(TAG_GPS_PROCESSING_METHOD, 27, 7, exifTag), new ExifTag(TAG_GPS_AREA_INFORMATION, 28, 7, exifTag), new ExifTag(TAG_GPS_DATESTAMP, 29, i3, exifTag), new ExifTag(TAG_GPS_DIFFERENTIAL, 30, i4, exifTag)};
        IFD_INTEROPERABILITY_TAGS = new ExifTag[]{new ExifTag(TAG_INTEROPERABILITY_INDEX, 1, i3, exifTag)};
        IFD_THUMBNAIL_TAGS = new ExifTag[]{new ExifTag(TAG_THUMBNAIL_IMAGE_WIDTH, 256, i4, i, exifTag), new ExifTag(TAG_THUMBNAIL_IMAGE_LENGTH, 257, i4, i, exifTag), new ExifTag(TAG_BITS_PER_SAMPLE, 258, i4, exifTag), new ExifTag(TAG_COMPRESSION, 259, i4, exifTag), new ExifTag(TAG_PHOTOMETRIC_INTERPRETATION, 262, i4, exifTag), new ExifTag(TAG_IMAGE_DESCRIPTION, 270, i3, exifTag), new ExifTag(TAG_MAKE, AnqpInformationElement.ANQP_EMERGENCY_NAI, i3, exifTag), new ExifTag(TAG_MODEL, 272, i3, exifTag), new ExifTag(TAG_STRIP_OFFSETS, i4, i, exifTag), new ExifTag(TAG_ORIENTATION, 274, i4, exifTag), new ExifTag(TAG_SAMPLES_PER_PIXEL, 277, i4, exifTag), new ExifTag(TAG_ROWS_PER_STRIP, 278, i4, i, exifTag), new ExifTag(TAG_STRIP_BYTE_COUNTS, 279, i4, i, exifTag), new ExifTag(TAG_X_RESOLUTION, IActivityManager.CREATE_STACK_ON_DISPLAY, i2, exifTag), new ExifTag(TAG_Y_RESOLUTION, IActivityManager.GET_FOCUSED_STACK_ID_TRANSACTION, i2, exifTag), new ExifTag(TAG_PLANAR_CONFIGURATION, IActivityManager.SET_TASK_RESIZEABLE_TRANSACTION, i4, exifTag), new ExifTag(TAG_RESOLUTION_UNIT, IActivityManager.UPDATE_DEVICE_OWNER_TRANSACTION, i4, exifTag), new ExifTag(TAG_TRANSFER_FUNCTION, 301, i4, exifTag), new ExifTag(TAG_SOFTWARE, MediaFile.FILE_TYPE_WMV, i3, exifTag), new ExifTag(TAG_DATETIME, MediaFile.FILE_TYPE_ASF, i3, exifTag), new ExifTag(TAG_ARTIST, 315, i3, exifTag), new ExifTag(TAG_WHITE_POINT, 318, i2, exifTag), new ExifTag(TAG_PRIMARY_CHROMATICITIES, 319, i2, exifTag), new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, i, exifTag), new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, i, exifTag), new ExifTag(TAG_Y_CB_CR_COEFFICIENTS, 529, i2, exifTag), new ExifTag(TAG_Y_CB_CR_SUB_SAMPLING, 530, i4, exifTag), new ExifTag(TAG_Y_CB_CR_POSITIONING, 531, i4, exifTag), new ExifTag(TAG_REFERENCE_BLACK_WHITE, BluetoothClass.Device.PHONE_ISDN, i2, exifTag), new ExifTag(TAG_COPYRIGHT, 33432, i3, exifTag), new ExifTag(TAG_EXIF_IFD_POINTER, 34665, i, exifTag), new ExifTag(TAG_GPS_INFO_IFD_POINTER, GLES30.GL_DRAW_BUFFER0, i, exifTag)};
        EXIF_TAGS = new ExifTag[][]{IFD_TIFF_TAGS, IFD_EXIF_TAGS, IFD_GPS_TAGS, IFD_INTEROPERABILITY_TAGS, IFD_THUMBNAIL_TAGS};
        IFD_POINTER_TAGS = new ExifTag[]{new ExifTag(TAG_EXIF_IFD_POINTER, 34665, i, exifTag), new ExifTag(TAG_GPS_INFO_IFD_POINTER, GLES30.GL_DRAW_BUFFER0, i, exifTag), new ExifTag(TAG_INTEROPERABILITY_IFD_POINTER, 40965, i, exifTag)};
        JPEG_INTERCHANGE_FORMAT_TAG = new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT, 513, i, exifTag);
        JPEG_INTERCHANGE_FORMAT_LENGTH_TAG = new ExifTag(TAG_JPEG_INTERCHANGE_FORMAT_LENGTH, 514, i, exifTag);
        sExifTagMapsForReading = new HashMap[EXIF_TAGS.length];
        sExifTagMapsForWriting = new HashMap[EXIF_TAGS.length];
        System.loadLibrary("media_jni");
        nativeInitRaw();
        sFormatter = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
        sFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        for (int hint = 0; hint < EXIF_TAGS.length; hint++) {
            sExifTagMapsForReading[hint] = new HashMap();
            sExifTagMapsForWriting[hint] = new HashMap();
            for (ExifTag tag : EXIF_TAGS[hint]) {
                sExifTagMapsForReading[hint].put(Integer.valueOf(tag.number), tag);
                sExifTagMapsForWriting[hint].put(tag.name, tag);
            }
        }
        sNonZeroTimePattern = Pattern.compile(".*[1-9].*");
        sGpsTimestampPattern = Pattern.compile("^([0-9][0-9]):([0-9][0-9]):([0-9][0-9])$");
        sLock = new Object();
    }

    private static class Rational {
        public final long denominator;
        public final long numerator;

        Rational(long numerator, long denominator, Rational rational) {
            this(numerator, denominator);
        }

        private Rational(long numerator, long denominator) {
            if (denominator == 0) {
                this.numerator = 0L;
                this.denominator = 1L;
            } else {
                this.numerator = numerator;
                this.denominator = denominator;
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

        ExifAttribute(int format, int numberOfComponents, byte[] bytes, ExifAttribute exifAttribute) {
            this(format, numberOfComponents, bytes);
        }

        private ExifAttribute(int format, int numberOfComponents, byte[] bytes) {
            this.format = format;
            this.numberOfComponents = numberOfComponents;
            this.bytes = bytes;
        }

        public static ExifAttribute createUShort(int[] values, ByteOrder byteOrder) {
            ByteBuffer buffer = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[3] * values.length]);
            buffer.order(byteOrder);
            for (int value : values) {
                buffer.putShort((short) value);
            }
            return new ExifAttribute(3, values.length, buffer.array());
        }

        public static ExifAttribute createUShort(int value, ByteOrder byteOrder) {
            return createUShort(new int[]{value}, byteOrder);
        }

        public static ExifAttribute createULong(long[] values, ByteOrder byteOrder) {
            ByteBuffer buffer = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[4] * values.length]);
            buffer.order(byteOrder);
            for (long value : values) {
                buffer.putInt((int) value);
            }
            return new ExifAttribute(4, values.length, buffer.array());
        }

        public static ExifAttribute createULong(long value, ByteOrder byteOrder) {
            return createULong(new long[]{value}, byteOrder);
        }

        public static ExifAttribute createSLong(int[] values, ByteOrder byteOrder) {
            ByteBuffer buffer = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[9] * values.length]);
            buffer.order(byteOrder);
            for (int value : values) {
                buffer.putInt(value);
            }
            return new ExifAttribute(9, values.length, buffer.array());
        }

        public static ExifAttribute createSLong(int value, ByteOrder byteOrder) {
            return createSLong(new int[]{value}, byteOrder);
        }

        public static ExifAttribute createByte(String value) {
            if (value.length() == 1 && value.charAt(0) >= '0' && value.charAt(0) <= '1') {
                byte[] bytes = {(byte) (value.charAt(0) - '0')};
                return new ExifAttribute(1, bytes.length, bytes);
            }
            byte[] ascii = value.getBytes(ExifInterface.ASCII);
            return new ExifAttribute(1, ascii.length, ascii);
        }

        public static ExifAttribute createString(String value) {
            byte[] ascii = (value + (char) 0).getBytes(ExifInterface.ASCII);
            return new ExifAttribute(2, ascii.length, ascii);
        }

        public static ExifAttribute createURational(Rational[] values, ByteOrder byteOrder) {
            ByteBuffer buffer = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[5] * values.length]);
            buffer.order(byteOrder);
            for (Rational value : values) {
                buffer.putInt((int) value.numerator);
                buffer.putInt((int) value.denominator);
            }
            return new ExifAttribute(5, values.length, buffer.array());
        }

        public static ExifAttribute createURational(Rational value, ByteOrder byteOrder) {
            return createURational(new Rational[]{value}, byteOrder);
        }

        public static ExifAttribute createSRational(Rational[] values, ByteOrder byteOrder) {
            ByteBuffer buffer = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[10] * values.length]);
            buffer.order(byteOrder);
            for (Rational value : values) {
                buffer.putInt((int) value.numerator);
                buffer.putInt((int) value.denominator);
            }
            return new ExifAttribute(10, values.length, buffer.array());
        }

        public static ExifAttribute createSRational(Rational value, ByteOrder byteOrder) {
            return createSRational(new Rational[]{value}, byteOrder);
        }

        public static ExifAttribute createDouble(double[] values, ByteOrder byteOrder) {
            ByteBuffer buffer = ByteBuffer.wrap(new byte[ExifInterface.IFD_FORMAT_BYTES_PER_FORMAT[12] * values.length]);
            buffer.order(byteOrder);
            for (double value : values) {
                buffer.putDouble(value);
            }
            return new ExifAttribute(12, values.length, buffer.array());
        }

        public static ExifAttribute createDouble(double value, ByteOrder byteOrder) {
            return createDouble(new double[]{value}, byteOrder);
        }

        public String toString() {
            return "(" + ExifInterface.IFD_FORMAT_NAMES[this.format] + ", data length:" + this.bytes.length + ")";
        }

        private Object getValue(ByteOrder byteOrder) {
            int ch;
            try {
                ByteOrderAwarenessDataInputStream inputStream = new ByteOrderAwarenessDataInputStream(this.bytes);
                inputStream.setByteOrder(byteOrder);
                switch (this.format) {
                    case 1:
                    case 6:
                        if (this.bytes.length == 1 && this.bytes[0] >= 0 && this.bytes[0] <= 1) {
                            return new String(new char[]{(char) (this.bytes[0] + 48)});
                        }
                        return new String(this.bytes, ExifInterface.ASCII);
                    case 2:
                    case 7:
                        int index = 0;
                        if (this.numberOfComponents >= ExifInterface.EXIF_ASCII_PREFIX.length) {
                            boolean same = true;
                            int i = 0;
                            while (true) {
                                if (i < ExifInterface.EXIF_ASCII_PREFIX.length) {
                                    if (this.bytes[i] == ExifInterface.EXIF_ASCII_PREFIX[i]) {
                                        i++;
                                    } else {
                                        same = false;
                                    }
                                }
                            }
                            if (same) {
                                index = ExifInterface.EXIF_ASCII_PREFIX.length;
                            }
                        }
                        StringBuilder stringBuilder = new StringBuilder();
                        while (index < this.numberOfComponents && (ch = this.bytes[index]) != 0) {
                            if (ch >= 32) {
                                stringBuilder.append((char) ch);
                            } else {
                                stringBuilder.append('?');
                            }
                            index++;
                        }
                        return stringBuilder.toString();
                    case 3:
                        int[] values = new int[this.numberOfComponents];
                        for (int i2 = 0; i2 < this.numberOfComponents; i2++) {
                            values[i2] = inputStream.readUnsignedShort();
                        }
                        return values;
                    case 4:
                        long[] values2 = new long[this.numberOfComponents];
                        for (int i3 = 0; i3 < this.numberOfComponents; i3++) {
                            values2[i3] = inputStream.readUnsignedInt();
                        }
                        return values2;
                    case 5:
                        Rational[] values3 = new Rational[this.numberOfComponents];
                        for (int i4 = 0; i4 < this.numberOfComponents; i4++) {
                            long numerator = inputStream.readUnsignedInt();
                            long denominator = inputStream.readUnsignedInt();
                            values3[i4] = new Rational(numerator, denominator, null);
                        }
                        return values3;
                    case 8:
                        int[] values4 = new int[this.numberOfComponents];
                        for (int i5 = 0; i5 < this.numberOfComponents; i5++) {
                            values4[i5] = inputStream.readShort();
                        }
                        return values4;
                    case 9:
                        int[] values5 = new int[this.numberOfComponents];
                        for (int i6 = 0; i6 < this.numberOfComponents; i6++) {
                            values5[i6] = inputStream.readInt();
                        }
                        return values5;
                    case 10:
                        Rational[] values6 = new Rational[this.numberOfComponents];
                        for (int i7 = 0; i7 < this.numberOfComponents; i7++) {
                            long numerator2 = inputStream.readInt();
                            long denominator2 = inputStream.readInt();
                            values6[i7] = new Rational(numerator2, denominator2, null);
                        }
                        return values6;
                    case 11:
                        double[] values7 = new double[this.numberOfComponents];
                        for (int i8 = 0; i8 < this.numberOfComponents; i8++) {
                            values7[i8] = inputStream.readFloat();
                        }
                        return values7;
                    case 12:
                        double[] values8 = new double[this.numberOfComponents];
                        for (int i9 = 0; i9 < this.numberOfComponents; i9++) {
                            values8[i9] = inputStream.readDouble();
                        }
                        return values8;
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
                long[] array = (long[]) value;
                if (array.length == 1) {
                    return array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof int[]) {
                int[] array2 = (int[]) value;
                if (array2.length == 1) {
                    return array2[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof double[]) {
                double[] array3 = (double[]) value;
                if (array3.length == 1) {
                    return array3[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof Rational[]) {
                Rational[] array4 = (Rational[]) value;
                if (array4.length == 1) {
                    return array4[0].calculate();
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
                long[] array = (long[]) value;
                if (array.length == 1) {
                    return (int) array[0];
                }
                throw new NumberFormatException("There are more than one component");
            }
            if (value instanceof int[]) {
                int[] array2 = (int[]) value;
                if (array2.length == 1) {
                    return array2[0];
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
            StringBuilder stringBuilder = new StringBuilder();
            if (value instanceof long[]) {
                long[] array = (long[]) value;
                for (int i = 0; i < array.length; i++) {
                    stringBuilder.append(array[i]);
                    if (i + 1 != array.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            if (value instanceof int[]) {
                int[] array2 = (int[]) value;
                for (int i2 = 0; i2 < array2.length; i2++) {
                    stringBuilder.append(array2[i2]);
                    if (i2 + 1 != array2.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            if (value instanceof double[]) {
                double[] array3 = (double[]) value;
                for (int i3 = 0; i3 < array3.length; i3++) {
                    stringBuilder.append(array3[i3]);
                    if (i3 + 1 != array3.length) {
                        stringBuilder.append(",");
                    }
                }
                return stringBuilder.toString();
            }
            if (!(value instanceof Rational[])) {
                return null;
            }
            Rational[] array4 = (Rational[]) value;
            for (int i4 = 0; i4 < array4.length; i4++) {
                stringBuilder.append(array4[i4].numerator);
                stringBuilder.append('/');
                stringBuilder.append(array4[i4].denominator);
                if (i4 + 1 != array4.length) {
                    stringBuilder.append(",");
                }
            }
            return stringBuilder.toString();
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

        ExifTag(String name, int number, int primaryFormat, int secondaryFormat, ExifTag exifTag) {
            this(name, number, primaryFormat, secondaryFormat);
        }

        ExifTag(String name, int number, int format, ExifTag exifTag) {
            this(name, number, format);
        }

        private ExifTag(String name, int number, int format) {
            this.name = name;
            this.number = number;
            this.primaryFormat = format;
            this.secondaryFormat = -1;
        }

        private ExifTag(String name, int number, int primaryFormat, int secondaryFormat) {
            this.name = name;
            this.number = number;
            this.primaryFormat = primaryFormat;
            this.secondaryFormat = secondaryFormat;
        }
    }

    public ExifInterface(String filename) throws Throwable {
        this.mAttributes = new HashMap[EXIF_TAGS.length];
        this.mExifByteOrder = ByteOrder.BIG_ENDIAN;
        if (filename == null) {
            throw new IllegalArgumentException("filename cannot be null");
        }
        FileInputStream fileInputStream = null;
        this.mAssetInputStream = null;
        this.mFilename = filename;
        this.mIsInputStream = false;
        try {
            FileInputStream in = new FileInputStream(filename);
            try {
                if (isSeekableFD(in.getFD())) {
                    this.mSeekableFileDescriptor = in.getFD();
                } else {
                    this.mSeekableFileDescriptor = null;
                }
                loadAttributes(in);
                IoUtils.closeQuietly(in);
            } catch (Throwable th) {
                th = th;
                fileInputStream = in;
                IoUtils.closeQuietly(fileInputStream);
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    public ExifInterface(FileDescriptor fileDescriptor) throws Throwable {
        FileInputStream in;
        this.mAttributes = new HashMap[EXIF_TAGS.length];
        this.mExifByteOrder = ByteOrder.BIG_ENDIAN;
        if (fileDescriptor == null) {
            throw new IllegalArgumentException("fileDescriptor cannot be null");
        }
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
        FileInputStream in2 = null;
        try {
            in = new FileInputStream(fileDescriptor);
        } catch (Throwable th) {
            th = th;
        }
        try {
            loadAttributes(in);
            IoUtils.closeQuietly(in);
        } catch (Throwable th2) {
            th = th2;
            in2 = in;
            IoUtils.closeQuietly(in2);
            throw th;
        }
    }

    public ExifInterface(InputStream inputStream) throws Throwable {
        this.mAttributes = new HashMap[EXIF_TAGS.length];
        this.mExifByteOrder = ByteOrder.BIG_ENDIAN;
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream cannot be null");
        }
        this.mFilename = null;
        if (inputStream instanceof AssetManager.AssetInputStream) {
            this.mAssetInputStream = (AssetManager.AssetInputStream) inputStream;
            this.mSeekableFileDescriptor = null;
        } else if ((inputStream instanceof FileInputStream) && isSeekableFD(((FileInputStream) inputStream).getFD())) {
            this.mAssetInputStream = null;
            this.mSeekableFileDescriptor = ((FileInputStream) inputStream).getFD();
        } else {
            this.mAssetInputStream = null;
            this.mSeekableFileDescriptor = null;
        }
        this.mIsInputStream = true;
        loadAttributes(inputStream);
    }

    private ExifAttribute getExifAttribute(String tag) {
        for (int i = 0; i < EXIF_TAGS.length; i++) {
            Object value = this.mAttributes[i].get(tag);
            if (value != null) {
                return (ExifAttribute) value;
            }
        }
        return null;
    }

    public String getAttribute(String tag) {
        ExifAttribute attribute = getExifAttribute(tag);
        if (attribute == null) {
            return null;
        }
        if (!sTagSetForCompatibility.contains(tag)) {
            return attribute.getStringValue(this.mExifByteOrder);
        }
        if (tag.equals(TAG_GPS_TIMESTAMP)) {
            if (attribute.format != 5 && attribute.format != 10) {
                return null;
            }
            Rational[] array = (Rational[]) attribute.getValue(this.mExifByteOrder);
            if (array.length != 3) {
                return null;
            }
            return String.format("%02d:%02d:%02d", Integer.valueOf((int) (array[0].numerator / array[0].denominator)), Integer.valueOf((int) (array[1].numerator / array[1].denominator)), Integer.valueOf((int) (array[2].numerator / array[2].denominator)));
        }
        try {
            return Double.toString(attribute.getDoubleValue(this.mExifByteOrder));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int getAttributeInt(String tag, int defaultValue) {
        ExifAttribute exifAttribute = getExifAttribute(tag);
        if (exifAttribute == null) {
            return defaultValue;
        }
        try {
            return exifAttribute.getIntValue(this.mExifByteOrder);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getAttributeDouble(String tag, double defaultValue) {
        ExifAttribute exifAttribute = getExifAttribute(tag);
        if (exifAttribute == null) {
            return defaultValue;
        }
        try {
            return exifAttribute.getDoubleValue(this.mExifByteOrder);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setAttribute(String tag, String value) {
        Object obj;
        int dataFormat;
        if (value != null && sTagSetForCompatibility.contains(tag)) {
            if (tag.equals(TAG_GPS_TIMESTAMP)) {
                Matcher m = sGpsTimestampPattern.matcher(value);
                if (!m.find()) {
                    Log.w(TAG, "Invalid value for " + tag + " : " + value);
                    return;
                }
                value = Integer.parseInt(m.group(1)) + "/1," + Integer.parseInt(m.group(2)) + "/1," + Integer.parseInt(m.group(3)) + "/1";
            } else {
                try {
                    double doubleValue = Double.parseDouble(value);
                    value = ((long) (10000.0d * doubleValue)) + "/10000";
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid value for " + tag + " : " + value);
                    return;
                }
            }
        }
        for (int i = 0; i < EXIF_TAGS.length; i++) {
            if ((i != 4 || this.mHasThumbnail) && (obj = sExifTagMapsForWriting[i].get(tag)) != null) {
                if (value != null) {
                    ExifTag exifTag = (ExifTag) obj;
                    Pair<Integer, Integer> guess = guessDataFormat(value);
                    if (exifTag.primaryFormat == ((Integer) guess.first).intValue() || exifTag.primaryFormat == ((Integer) guess.second).intValue()) {
                        dataFormat = exifTag.primaryFormat;
                    } else if (exifTag.secondaryFormat != -1 && (exifTag.secondaryFormat == ((Integer) guess.first).intValue() || exifTag.secondaryFormat == ((Integer) guess.second).intValue())) {
                        dataFormat = exifTag.secondaryFormat;
                    } else if (exifTag.primaryFormat == 1 || exifTag.primaryFormat == 7 || exifTag.primaryFormat == 2) {
                        dataFormat = exifTag.primaryFormat;
                    } else {
                        Log.w(TAG, "Given tag (" + tag + ") value didn't match with one of expected formats: " + IFD_FORMAT_NAMES[exifTag.primaryFormat] + (exifTag.secondaryFormat == -1 ? ProxyInfo.LOCAL_EXCL_LIST : ", " + IFD_FORMAT_NAMES[exifTag.secondaryFormat]) + " (guess: " + IFD_FORMAT_NAMES[((Integer) guess.first).intValue()] + (((Integer) guess.second).intValue() == -1 ? ProxyInfo.LOCAL_EXCL_LIST : ", " + IFD_FORMAT_NAMES[((Integer) guess.second).intValue()]) + ")");
                    }
                    switch (dataFormat) {
                        case 1:
                            this.mAttributes[i].put(tag, ExifAttribute.createByte(value));
                            break;
                        case 2:
                        case 7:
                            this.mAttributes[i].put(tag, ExifAttribute.createString(value));
                            break;
                        case 3:
                            String[] values = value.split(",");
                            int[] intArray = new int[values.length];
                            for (int j = 0; j < values.length; j++) {
                                intArray[j] = Integer.parseInt(values[j]);
                            }
                            this.mAttributes[i].put(tag, ExifAttribute.createUShort(intArray, this.mExifByteOrder));
                            break;
                        case 4:
                            String[] values2 = value.split(",");
                            long[] longArray = new long[values2.length];
                            for (int j2 = 0; j2 < values2.length; j2++) {
                                longArray[j2] = Long.parseLong(values2[j2]);
                            }
                            this.mAttributes[i].put(tag, ExifAttribute.createULong(longArray, this.mExifByteOrder));
                            break;
                        case 5:
                            String[] values3 = value.split(",");
                            Rational[] rationalArray = new Rational[values3.length];
                            for (int j3 = 0; j3 < values3.length; j3++) {
                                String[] numbers = values3[j3].split("/");
                                rationalArray[j3] = new Rational(Long.parseLong(numbers[0]), Long.parseLong(numbers[1]), null);
                            }
                            this.mAttributes[i].put(tag, ExifAttribute.createURational(rationalArray, this.mExifByteOrder));
                            break;
                        case 6:
                        case 8:
                        case 11:
                        default:
                            Log.w(TAG, "Data format isn't one of expected formats: " + dataFormat);
                            break;
                        case 9:
                            String[] values4 = value.split(",");
                            int[] intArray2 = new int[values4.length];
                            for (int j4 = 0; j4 < values4.length; j4++) {
                                intArray2[j4] = Integer.parseInt(values4[j4]);
                            }
                            this.mAttributes[i].put(tag, ExifAttribute.createSLong(intArray2, this.mExifByteOrder));
                            break;
                        case 10:
                            String[] values5 = value.split(",");
                            Rational[] rationalArray2 = new Rational[values5.length];
                            for (int j5 = 0; j5 < values5.length; j5++) {
                                String[] numbers2 = values5[j5].split("/");
                                rationalArray2[j5] = new Rational(Long.parseLong(numbers2[0]), Long.parseLong(numbers2[1]), null);
                            }
                            this.mAttributes[i].put(tag, ExifAttribute.createSRational(rationalArray2, this.mExifByteOrder));
                            break;
                        case 12:
                            String[] values6 = value.split(",");
                            double[] doubleArray = new double[values6.length];
                            for (int j6 = 0; j6 < values6.length; j6++) {
                                doubleArray[j6] = Double.parseDouble(values6[j6]);
                            }
                            this.mAttributes[i].put(tag, ExifAttribute.createDouble(doubleArray, this.mExifByteOrder));
                            break;
                    }
                } else {
                    this.mAttributes[i].remove(tag);
                }
            }
        }
    }

    private boolean updateAttribute(String tag, ExifAttribute value) {
        boolean updated = false;
        for (int i = 0; i < EXIF_TAGS.length; i++) {
            if (this.mAttributes[i].containsKey(tag)) {
                this.mAttributes[i].put(tag, value);
                updated = true;
            }
        }
        return updated;
    }

    private void removeAttribute(String tag) {
        for (int i = 0; i < EXIF_TAGS.length; i++) {
            this.mAttributes[i].remove(tag);
        }
    }

    private void loadAttributes(InputStream in) throws Throwable {
        for (int i = 0; i < EXIF_TAGS.length; i++) {
            try {
                try {
                    this.mAttributes[i] = new HashMap();
                } catch (IOException e) {
                    e = e;
                }
            } catch (Throwable th) {
                th = th;
            }
        }
        if (this.mAssetInputStream != null) {
            long asset = this.mAssetInputStream.getNativeAsset();
            if (handleRawResult(nativeGetRawAttributesFromAsset(asset))) {
                addDefaultValuesForCompatibility();
                return;
            }
        } else if (this.mSeekableFileDescriptor == null) {
            InputStream in2 = new BufferedInputStream(in, 3);
            try {
                if (isJpegInputStream((BufferedInputStream) in2)) {
                    in = in2;
                } else {
                    if (handleRawResult(nativeGetRawAttributesFromInputStream(in2))) {
                        addDefaultValuesForCompatibility();
                        return;
                    }
                    in = in2;
                }
            } catch (IOException e2) {
                e = e2;
                Log.w(TAG, "Invalid image: ExifInterface got an unsupported image format file(ExifInterface supports JPEG and some RAW image formats only) or a corrupted JPEG file to ExifInterface.", e);
                this.mIsSupportedFile = false;
                addDefaultValuesForCompatibility();
                return;
            } catch (Throwable th2) {
                th = th2;
                addDefaultValuesForCompatibility();
                throw th;
            }
        } else if (handleRawResult(nativeGetRawAttributesFromFileDescriptor(this.mSeekableFileDescriptor))) {
            addDefaultValuesForCompatibility();
            return;
        }
        getJpegAttributes(in);
        this.mIsSupportedFile = true;
        addDefaultValuesForCompatibility();
    }

    private static boolean isJpegInputStream(BufferedInputStream in) throws IOException {
        in.mark(3);
        byte[] signatureBytes = new byte[3];
        if (in.read(signatureBytes) != 3) {
            throw new EOFException();
        }
        boolean isJpeg = Arrays.equals(JPEG_SIGNATURE, signatureBytes);
        in.reset();
        return isJpeg;
    }

    private boolean handleRawResult(HashMap map) {
        if (map == null) {
            return false;
        }
        this.mIsRaw = true;
        String value = (String) map.remove(TAG_HAS_THUMBNAIL);
        this.mHasThumbnail = value != null ? value.equalsIgnoreCase("true") : false;
        String value2 = (String) map.remove(TAG_THUMBNAIL_OFFSET);
        if (value2 != null) {
            this.mThumbnailOffset = Integer.parseInt(value2);
        }
        String value3 = (String) map.remove(TAG_THUMBNAIL_LENGTH);
        if (value3 != null) {
            this.mThumbnailLength = Integer.parseInt(value3);
        }
        this.mThumbnailBytes = (byte[]) map.remove(TAG_THUMBNAIL_DATA);
        for (Map.Entry entry : map.entrySet()) {
            setAttribute((String) entry.getKey(), (String) entry.getValue());
        }
        return true;
    }

    private static boolean isSeekableFD(FileDescriptor fd) throws IOException {
        try {
            Os.lseek(fd, 0L, OsConstants.SEEK_CUR);
            return true;
        } catch (ErrnoException e) {
            return false;
        }
    }

    private void printAttributes() {
        for (int i = 0; i < this.mAttributes.length; i++) {
            Log.d(TAG, "The size of tag group[" + i + "]: " + this.mAttributes[i].size());
            for (Map.Entry entry : this.mAttributes[i].entrySet()) {
                ExifAttribute tagValue = (ExifAttribute) entry.getValue();
                Log.d(TAG, "tagName: " + entry.getKey() + ", tagType: " + tagValue.toString() + ", tagValue: '" + tagValue.getStringValue(this.mExifByteOrder) + "'");
            }
        }
    }

    public void saveAttributes() throws Throwable {
        FileOutputStream out;
        FileOutputStream out2;
        FileInputStream in;
        if (!this.mIsSupportedFile || this.mIsRaw) {
            throw new IOException("ExifInterface only supports saving attributes on JPEG formats.");
        }
        if (this.mIsInputStream || (this.mSeekableFileDescriptor == null && this.mFilename == null)) {
            throw new IOException("ExifInterface does not support saving attributes for the current input.");
        }
        this.mThumbnailBytes = getThumbnail();
        FileInputStream fileInputStream = null;
        FileOutputStream out3 = null;
        File tempFile = null;
        try {
            try {
                if (this.mFilename != null) {
                    File tempFile2 = new File(this.mFilename + ".tmp");
                    try {
                        File originalFile = new File(this.mFilename);
                        if (!originalFile.renameTo(tempFile2)) {
                            throw new IOException("Could'nt rename to " + tempFile2.getAbsolutePath());
                        }
                        tempFile = tempFile2;
                        IoUtils.closeQuietly(fileInputStream);
                        IoUtils.closeQuietly(out3);
                        FileInputStream in2 = null;
                        out2 = null;
                        try {
                            try {
                                in = new FileInputStream(tempFile);
                            } catch (ErrnoException e) {
                                e = e;
                            }
                        } catch (Throwable th) {
                            th = th;
                        }
                        try {
                            if (this.mFilename == null) {
                                out2 = new FileOutputStream(this.mFilename);
                            } else if (this.mSeekableFileDescriptor != null) {
                                Os.lseek(this.mSeekableFileDescriptor, 0L, OsConstants.SEEK_SET);
                                out2 = new FileOutputStream(this.mSeekableFileDescriptor);
                            }
                            saveJpegAttributes(in, out2);
                            IoUtils.closeQuietly(in);
                            IoUtils.closeQuietly(out2);
                            tempFile.delete();
                            this.mThumbnailBytes = null;
                        } catch (ErrnoException e2) {
                            e = e2;
                            in2 = in;
                            throw e.rethrowAsIOException();
                        } catch (Throwable th2) {
                            th = th2;
                            in2 = in;
                            IoUtils.closeQuietly(in2);
                            IoUtils.closeQuietly(out2);
                            tempFile.delete();
                            throw th;
                        }
                    } catch (ErrnoException e3) {
                        e = e3;
                        throw e.rethrowAsIOException();
                    } catch (Throwable th3) {
                        th = th3;
                        IoUtils.closeQuietly(fileInputStream);
                        IoUtils.closeQuietly(out3);
                        throw th;
                    }
                }
                if (this.mSeekableFileDescriptor != null) {
                    tempFile = File.createTempFile("temp", "jpg");
                    Os.lseek(this.mSeekableFileDescriptor, 0L, OsConstants.SEEK_SET);
                    FileInputStream in3 = new FileInputStream(this.mSeekableFileDescriptor);
                    try {
                        out = new FileOutputStream(tempFile);
                    } catch (ErrnoException e4) {
                        e = e4;
                    } catch (Throwable th4) {
                        th = th4;
                        fileInputStream = in3;
                    }
                    try {
                        Streams.copy(in3, out);
                        out3 = out;
                        fileInputStream = in3;
                    } catch (ErrnoException e5) {
                        e = e5;
                        throw e.rethrowAsIOException();
                    } catch (Throwable th5) {
                        th = th5;
                        out3 = out;
                        fileInputStream = in3;
                        IoUtils.closeQuietly(fileInputStream);
                        IoUtils.closeQuietly(out3);
                        throw th;
                    }
                }
                IoUtils.closeQuietly(fileInputStream);
                IoUtils.closeQuietly(out3);
                FileInputStream in22 = null;
                out2 = null;
                in = new FileInputStream(tempFile);
                if (this.mFilename == null) {
                }
                saveJpegAttributes(in, out2);
                IoUtils.closeQuietly(in);
                IoUtils.closeQuietly(out2);
                tempFile.delete();
                this.mThumbnailBytes = null;
            } catch (Throwable th6) {
                th = th6;
            }
        } catch (ErrnoException e6) {
            e = e6;
        }
    }

    public boolean hasThumbnail() {
        return this.mHasThumbnail;
    }

    public byte[] getThumbnail() {
        if (!this.mHasThumbnail) {
            return null;
        }
        if (this.mThumbnailBytes != null) {
            return this.mThumbnailBytes;
        }
        FileInputStream in = null;
        try {
            try {
                if (this.mAssetInputStream != null) {
                    return nativeGetThumbnailFromAsset(this.mAssetInputStream.getNativeAsset(), this.mThumbnailOffset, this.mThumbnailLength);
                }
                if (this.mFilename != null) {
                    in = new FileInputStream(this.mFilename);
                } else if (this.mSeekableFileDescriptor != null) {
                    FileDescriptor fileDescriptor = Os.dup(this.mSeekableFileDescriptor);
                    Os.lseek(fileDescriptor, 0L, OsConstants.SEEK_SET);
                    in = new FileInputStream(fileDescriptor);
                }
                if (in == null) {
                    throw new FileNotFoundException();
                }
                if (in.skip(this.mThumbnailOffset) != this.mThumbnailOffset) {
                    throw new IOException("Corrupted image");
                }
                byte[] buffer = new byte[this.mThumbnailLength];
                if (in.read(buffer) != this.mThumbnailLength) {
                    throw new IOException("Corrupted image");
                }
                return buffer;
            } catch (ErrnoException | IOException e) {
                return null;
            }
        } finally {
            IoUtils.closeQuietly((AutoCloseable) null);
        }
    }

    public long[] getThumbnailRange() {
        if (!this.mHasThumbnail) {
            return null;
        }
        long[] range = {this.mThumbnailOffset, this.mThumbnailLength};
        return range;
    }

    public boolean getLatLong(float[] output) {
        String latValue = getAttribute(TAG_GPS_LATITUDE);
        String latRef = getAttribute(TAG_GPS_LATITUDE_REF);
        String lngValue = getAttribute(TAG_GPS_LONGITUDE);
        String lngRef = getAttribute(TAG_GPS_LONGITUDE_REF);
        if (latValue != null && latRef != null && lngValue != null && lngRef != null) {
            try {
                output[0] = convertRationalLatLonToFloat(latValue, latRef);
                output[1] = convertRationalLatLonToFloat(lngValue, lngRef);
                return true;
            } catch (IllegalArgumentException e) {
                Log.e(TAG, "getLatLong: IllegalArgumentException!", e);
            }
        }
        return false;
    }

    public double getAltitude(double defaultValue) {
        double altitude = getAttributeDouble(TAG_GPS_ALTITUDE, -1.0d);
        int ref = getAttributeInt(TAG_GPS_ALTITUDE_REF, -1);
        if (altitude < 0.0d || ref < 0) {
            return defaultValue;
        }
        return ((double) (ref != 1 ? 1 : -1)) * altitude;
    }

    public long getDateTime() {
        String dateTimeString = getAttribute(TAG_DATETIME);
        if (dateTimeString == null || !sNonZeroTimePattern.matcher(dateTimeString).matches()) {
            return -1L;
        }
        ParsePosition pos = new ParsePosition(0);
        try {
            Date datetime = sFormatter.parse(dateTimeString, pos);
            if (datetime == null) {
                return -1L;
            }
            long msecs = datetime.getTime();
            String subSecs = getAttribute(TAG_SUBSEC_TIME);
            if (subSecs != null) {
                try {
                    long sub = Long.valueOf(subSecs).longValue();
                    while (sub > 1000) {
                        sub /= 10;
                    }
                    return msecs + sub;
                } catch (NumberFormatException e) {
                    return msecs;
                }
            }
            return msecs;
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "getDateTime: IllegalArgumentException!", ex);
            return -1L;
        }
    }

    public long getGpsDateTime() {
        String date = getAttribute(TAG_GPS_DATESTAMP);
        String time = getAttribute(TAG_GPS_TIMESTAMP);
        if (date == null || time == null || !(sNonZeroTimePattern.matcher(date).matches() || sNonZeroTimePattern.matcher(time).matches())) {
            return -1L;
        }
        String dateTimeString = date + ' ' + time;
        ParsePosition pos = new ParsePosition(0);
        try {
            Date datetime = sFormatter.parse(dateTimeString, pos);
            if (datetime == null) {
                return -1L;
            }
            return datetime.getTime();
        } catch (IllegalArgumentException ex) {
            Log.e(TAG, "getGpsDateTime: IllegalArgumentException!", ex);
            return -1L;
        }
    }

    private static float convertRationalLatLonToFloat(String rationalString, String ref) {
        try {
            String[] parts = rationalString.split(",");
            String[] pair = parts[0].split("/");
            double degrees = Double.parseDouble(pair[0].trim()) / Double.parseDouble(pair[1].trim());
            String[] pair2 = parts[1].split("/");
            double minutes = Double.parseDouble(pair2[0].trim()) / Double.parseDouble(pair2[1].trim());
            String[] pair3 = parts[2].split("/");
            double seconds = Double.parseDouble(pair3[0].trim()) / Double.parseDouble(pair3[1].trim());
            double result = (minutes / 60.0d) + degrees + (seconds / 3600.0d);
            if (!ref.equals("S")) {
                if (!ref.equals("W")) {
                    return (float) result;
                }
            }
            return (float) (-result);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            throw new IllegalArgumentException();
        }
    }

    private void getJpegAttributes(InputStream inputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        byte marker = dataInputStream.readByte();
        if (marker != -1) {
            throw new IOException("Invalid marker: " + Integer.toHexString(marker & 255));
        }
        if (dataInputStream.readByte() != -40) {
            throw new IOException("Invalid marker: " + Integer.toHexString(marker & 255));
        }
        int bytesRead = 1 + 1;
        while (true) {
            byte marker2 = dataInputStream.readByte();
            if (marker2 != -1) {
                throw new IOException("Invalid marker:" + Integer.toHexString(marker2 & 255));
            }
            byte marker3 = dataInputStream.readByte();
            int bytesRead2 = bytesRead + 1 + 1;
            if (marker3 == -39 || marker3 == -38) {
                return;
            }
            int length = dataInputStream.readUnsignedShort() - 2;
            int bytesRead3 = bytesRead2 + 2;
            if (length < 0) {
                throw new IOException("Invalid length");
            }
            switch (marker3) {
                case -64:
                case -63:
                case -62:
                case -61:
                case KeymasterDefs.KM_ERROR_UNSUPPORTED_MIN_MAC_LENGTH:
                case KeymasterDefs.KM_ERROR_MISSING_MIN_MAC_LENGTH:
                case KeymasterDefs.KM_ERROR_INVALID_MAC_LENGTH:
                case KeymasterDefs.KM_ERROR_CALLER_NONCE_PROHIBITED:
                case KeymasterDefs.KM_ERROR_KEY_RATE_LIMIT_EXCEEDED:
                case KeymasterDefs.KM_ERROR_MISSING_MAC_LENGTH:
                case KeymasterDefs.KM_ERROR_MISSING_NONCE:
                case KeymasterDefs.KM_ERROR_UNSUPPORTED_EC_FIELD:
                case KeymasterDefs.KM_ERROR_SECURE_HW_COMMUNICATION_FAILED:
                    if (dataInputStream.skipBytes(1) != 1) {
                        throw new IOException("Invalid SOFx");
                    }
                    this.mAttributes[0].put(TAG_IMAGE_LENGTH, ExifAttribute.createULong(dataInputStream.readUnsignedShort(), this.mExifByteOrder));
                    this.mAttributes[0].put(TAG_IMAGE_WIDTH, ExifAttribute.createULong(dataInputStream.readUnsignedShort(), this.mExifByteOrder));
                    length -= 5;
                    break;
                    break;
                case -31:
                    if (length >= 6) {
                        byte[] identifier = new byte[6];
                        if (inputStream.read(identifier) != 6) {
                            throw new IOException("Invalid exif");
                        }
                        bytesRead3 += 6;
                        length -= 6;
                        if (Arrays.equals(identifier, IDENTIFIER_EXIF_APP1)) {
                            if (length <= 0) {
                                throw new IOException("Invalid exif");
                            }
                            byte[] bytes = new byte[length];
                            if (dataInputStream.read(bytes) != length) {
                                throw new IOException("Invalid exif");
                            }
                            readExifSegment(bytes, bytesRead3);
                            bytesRead3 += length;
                            length = 0;
                        }
                    }
                    break;
                case -2:
                    byte[] bytes2 = new byte[length];
                    if (dataInputStream.read(bytes2) != length) {
                        throw new IOException("Invalid exif");
                    }
                    length = 0;
                    if (getAttribute(TAG_USER_COMMENT) == null) {
                        this.mAttributes[1].put(TAG_USER_COMMENT, ExifAttribute.createString(new String(bytes2, ASCII)));
                    }
                    break;
                    break;
            }
            if (length < 0) {
                throw new IOException("Invalid length");
            }
            if (dataInputStream.skipBytes(length) != length) {
                throw new IOException("Invalid JPEG segment");
            }
            bytesRead = bytesRead3 + length;
        }
    }

    private void saveJpegAttributes(InputStream inputStream, OutputStream outputStream) throws IOException {
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        ByteOrderAwarenessDataOutputStream dataOutputStream = new ByteOrderAwarenessDataOutputStream(outputStream, ByteOrder.BIG_ENDIAN);
        if (dataInputStream.readByte() != -1) {
            throw new IOException("Invalid marker");
        }
        dataOutputStream.writeByte(-1);
        if (dataInputStream.readByte() != -40) {
            throw new IOException("Invalid marker");
        }
        dataOutputStream.writeByte(-40);
        dataOutputStream.writeByte(-1);
        dataOutputStream.writeByte(-31);
        writeExifSegment(dataOutputStream, 6);
        byte[] bytes = new byte[4096];
        while (dataInputStream.readByte() == -1) {
            byte marker = dataInputStream.readByte();
            switch (marker) {
                case KeymasterDefs.KM_ERROR_UNSUPPORTED_TAG:
                case -38:
                    dataOutputStream.writeByte(-1);
                    dataOutputStream.writeByte(marker);
                    Streams.copy(dataInputStream, dataOutputStream);
                    return;
                case -31:
                    int length = dataInputStream.readUnsignedShort() - 2;
                    if (length < 0) {
                        throw new IOException("Invalid length");
                    }
                    byte[] identifier = new byte[6];
                    if (length >= 6) {
                        if (dataInputStream.read(identifier) != 6) {
                            throw new IOException("Invalid exif");
                        }
                        if (Arrays.equals(identifier, IDENTIFIER_EXIF_APP1)) {
                            if (dataInputStream.skip(length - 6) != length - 6) {
                                throw new IOException("Invalid length");
                            }
                            break;
                        }
                    }
                    dataOutputStream.writeByte(-1);
                    dataOutputStream.writeByte(marker);
                    dataOutputStream.writeUnsignedShort(length + 2);
                    if (length >= 6) {
                        length -= 6;
                        dataOutputStream.write(identifier);
                    }
                    while (length > 0) {
                        int read = dataInputStream.read(bytes, 0, Math.min(length, bytes.length));
                        if (read >= 0) {
                            dataOutputStream.write(bytes, 0, read);
                            length -= read;
                        }
                    }
                    break;
                    break;
                default:
                    dataOutputStream.writeByte(-1);
                    dataOutputStream.writeByte(marker);
                    int length2 = dataInputStream.readUnsignedShort();
                    dataOutputStream.writeUnsignedShort(length2);
                    int length3 = length2 - 2;
                    if (length3 < 0) {
                        throw new IOException("Invalid length");
                    }
                    while (length3 > 0) {
                        int read2 = dataInputStream.read(bytes, 0, Math.min(length3, bytes.length));
                        if (read2 >= 0) {
                            dataOutputStream.write(bytes, 0, read2);
                            length3 -= read2;
                        }
                    }
                    break;
                    break;
            }
        }
        throw new IOException("Invalid marker");
    }

    private void readExifSegment(byte[] exifBytes, int exifOffsetFromBeginning) throws IOException {
        ByteOrderAwarenessDataInputStream dataInputStream = new ByteOrderAwarenessDataInputStream(exifBytes);
        short byteOrder = dataInputStream.readShort();
        switch (byteOrder) {
            case 18761:
                this.mExifByteOrder = ByteOrder.LITTLE_ENDIAN;
                break;
            case 19789:
                this.mExifByteOrder = ByteOrder.BIG_ENDIAN;
                break;
            default:
                throw new IOException("Invalid byte order: " + Integer.toHexString(byteOrder));
        }
        dataInputStream.setByteOrder(this.mExifByteOrder);
        int startCode = dataInputStream.readUnsignedShort();
        if (startCode != 42) {
            throw new IOException("Invalid exif start: " + Integer.toHexString(startCode));
        }
        long firstIfdOffset = dataInputStream.readUnsignedInt();
        if (firstIfdOffset < 8 || firstIfdOffset >= exifBytes.length) {
            throw new IOException("Invalid first Ifd offset: " + firstIfdOffset);
        }
        long firstIfdOffset2 = firstIfdOffset - 8;
        if (firstIfdOffset2 > 0 && dataInputStream.skip(firstIfdOffset2) != firstIfdOffset2) {
            throw new IOException("Couldn't jump to first Ifd: " + firstIfdOffset2);
        }
        readImageFileDirectory(dataInputStream, 0);
        String jpegInterchangeFormatString = getAttribute(JPEG_INTERCHANGE_FORMAT_TAG.name);
        String jpegInterchangeFormatLengthString = getAttribute(JPEG_INTERCHANGE_FORMAT_LENGTH_TAG.name);
        if (jpegInterchangeFormatString == null || jpegInterchangeFormatLengthString == null) {
            return;
        }
        try {
            int jpegInterchangeFormat = Integer.parseInt(jpegInterchangeFormatString);
            int jpegInterchangeFormatLength = Math.min(jpegInterchangeFormat + Integer.parseInt(jpegInterchangeFormatLengthString), exifBytes.length) - jpegInterchangeFormat;
            if (jpegInterchangeFormat <= 0 || jpegInterchangeFormatLength <= 0) {
                return;
            }
            this.mHasThumbnail = true;
            this.mThumbnailOffset = exifOffsetFromBeginning + jpegInterchangeFormat;
            this.mThumbnailLength = jpegInterchangeFormatLength;
            if (this.mFilename != null || this.mAssetInputStream != null || this.mSeekableFileDescriptor != null) {
                return;
            }
            byte[] thumbnailBytes = new byte[jpegInterchangeFormatLength];
            dataInputStream.seek(jpegInterchangeFormat);
            dataInputStream.readFully(thumbnailBytes);
            this.mThumbnailBytes = thumbnailBytes;
        } catch (NumberFormatException e) {
        }
    }

    private void addDefaultValuesForCompatibility() {
        String valueOfDateTimeOriginal = getAttribute(TAG_DATETIME_ORIGINAL);
        if (valueOfDateTimeOriginal != null) {
            this.mAttributes[0].put(TAG_DATETIME, ExifAttribute.createString(valueOfDateTimeOriginal));
        }
        if (getAttribute(TAG_IMAGE_WIDTH) == null) {
            this.mAttributes[0].put(TAG_IMAGE_WIDTH, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (getAttribute(TAG_IMAGE_LENGTH) == null) {
            this.mAttributes[0].put(TAG_IMAGE_LENGTH, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (getAttribute(TAG_ORIENTATION) == null) {
            this.mAttributes[0].put(TAG_ORIENTATION, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (getAttribute(TAG_LIGHT_SOURCE) != null) {
            return;
        }
        this.mAttributes[1].put(TAG_LIGHT_SOURCE, ExifAttribute.createULong(0L, this.mExifByteOrder));
    }

    private void readImageFileDirectory(ByteOrderAwarenessDataInputStream dataInputStream, int hint) throws IOException {
        int innerIfdHint;
        if (dataInputStream.peek() + 2 > dataInputStream.mLength) {
            return;
        }
        short numberOfDirectoryEntry = dataInputStream.readShort();
        if (dataInputStream.peek() + ((long) (numberOfDirectoryEntry * 12)) > dataInputStream.mLength) {
            return;
        }
        for (short s = 0; s < numberOfDirectoryEntry; s = (short) (s + 1)) {
            int tagNumber = dataInputStream.readUnsignedShort();
            int dataFormat = dataInputStream.readUnsignedShort();
            int numberOfComponents = dataInputStream.readInt();
            long nextEntryOffset = dataInputStream.peek() + 4;
            ExifTag tag = (ExifTag) sExifTagMapsForReading[hint].get(Integer.valueOf(tagNumber));
            if (tag == null || dataFormat <= 0 || dataFormat >= IFD_FORMAT_BYTES_PER_FORMAT.length) {
                if (tag == null) {
                    Log.w(TAG, "Skip the tag entry since tag number is not defined: " + tagNumber);
                } else {
                    Log.w(TAG, "Skip the tag entry since data format is invalid: " + dataFormat);
                }
                dataInputStream.seek(nextEntryOffset);
            } else {
                int byteCount = numberOfComponents * IFD_FORMAT_BYTES_PER_FORMAT[dataFormat];
                if (byteCount > 4) {
                    long offset = dataInputStream.readUnsignedInt();
                    if (((long) byteCount) + offset <= dataInputStream.mLength) {
                        dataInputStream.seek(offset);
                        innerIfdHint = getIfdHintFromTagNumber(tagNumber);
                        if (innerIfdHint < 0) {
                            long offset2 = -1;
                            switch (dataFormat) {
                                case 3:
                                    offset2 = dataInputStream.readUnsignedShort();
                                    break;
                                case 4:
                                    offset2 = dataInputStream.readUnsignedInt();
                                    break;
                                case 8:
                                    offset2 = dataInputStream.readShort();
                                    break;
                                case 9:
                                    offset2 = dataInputStream.readInt();
                                    break;
                            }
                            if (offset2 > 0 && offset2 < dataInputStream.mLength) {
                                dataInputStream.seek(offset2);
                                readImageFileDirectory(dataInputStream, innerIfdHint);
                            } else {
                                Log.w(TAG, "Skip jump into the IFD since its offset is invalid: " + offset2);
                            }
                            dataInputStream.seek(nextEntryOffset);
                        } else {
                            byte[] bytes = new byte[IFD_FORMAT_BYTES_PER_FORMAT[dataFormat] * numberOfComponents];
                            dataInputStream.readFully(bytes);
                            this.mAttributes[hint].put(tag.name, new ExifAttribute(dataFormat, numberOfComponents, bytes, null));
                            if (dataInputStream.peek() != nextEntryOffset) {
                                dataInputStream.seek(nextEntryOffset);
                            }
                        }
                    } else {
                        Log.w(TAG, "Skip the tag entry since data offset is invalid: " + offset);
                        dataInputStream.seek(nextEntryOffset);
                    }
                } else {
                    innerIfdHint = getIfdHintFromTagNumber(tagNumber);
                    if (innerIfdHint < 0) {
                    }
                }
            }
        }
        if (dataInputStream.peek() + 4 > dataInputStream.mLength) {
            return;
        }
        long nextIfdOffset = dataInputStream.readUnsignedInt();
        if (nextIfdOffset <= 8 || nextIfdOffset >= dataInputStream.mLength) {
            return;
        }
        dataInputStream.seek(nextIfdOffset);
        readImageFileDirectory(dataInputStream, 4);
    }

    private static int getIfdHintFromTagNumber(int tagNumber) {
        for (int i = 0; i < IFD_POINTER_TAG_HINTS.length; i++) {
            if (IFD_POINTER_TAGS[i].number == tagNumber) {
                return IFD_POINTER_TAG_HINTS[i];
            }
        }
        return -1;
    }

    private int writeExifSegment(ByteOrderAwarenessDataOutputStream dataOutputStream, int exifOffsetFromBeginning) throws IOException {
        int[] ifdOffsets = new int[EXIF_TAGS.length];
        int[] ifdDataSizes = new int[EXIF_TAGS.length];
        for (ExifTag tag : IFD_POINTER_TAGS) {
            removeAttribute(tag.name);
        }
        removeAttribute(JPEG_INTERCHANGE_FORMAT_TAG.name);
        removeAttribute(JPEG_INTERCHANGE_FORMAT_LENGTH_TAG.name);
        for (int hint = 0; hint < EXIF_TAGS.length; hint++) {
            for (Object obj : this.mAttributes[hint].entrySet().toArray()) {
                Map.Entry entry = (Map.Entry) obj;
                if (entry.getValue() == null) {
                    this.mAttributes[hint].remove(entry.getKey());
                }
            }
        }
        if (!this.mAttributes[3].isEmpty()) {
            this.mAttributes[1].put(IFD_POINTER_TAGS[2].name, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (!this.mAttributes[1].isEmpty()) {
            this.mAttributes[0].put(IFD_POINTER_TAGS[0].name, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (!this.mAttributes[2].isEmpty()) {
            this.mAttributes[0].put(IFD_POINTER_TAGS[1].name, ExifAttribute.createULong(0L, this.mExifByteOrder));
        }
        if (this.mHasThumbnail) {
            this.mAttributes[0].put(JPEG_INTERCHANGE_FORMAT_TAG.name, ExifAttribute.createULong(0L, this.mExifByteOrder));
            this.mAttributes[0].put(JPEG_INTERCHANGE_FORMAT_LENGTH_TAG.name, ExifAttribute.createULong(this.mThumbnailLength, this.mExifByteOrder));
        }
        for (int i = 0; i < EXIF_TAGS.length; i++) {
            int sum = 0;
            Iterator entry$iterator = this.mAttributes[i].entrySet().iterator();
            while (entry$iterator.hasNext()) {
                ExifAttribute exifAttribute = (ExifAttribute) ((Map.Entry) entry$iterator.next()).getValue();
                int size = exifAttribute.size();
                if (size > 4) {
                    sum += size;
                }
            }
            ifdDataSizes[i] = ifdDataSizes[i] + sum;
        }
        int position = 8;
        for (int hint2 = 0; hint2 < EXIF_TAGS.length; hint2++) {
            if (!this.mAttributes[hint2].isEmpty()) {
                ifdOffsets[hint2] = position;
                position += (this.mAttributes[hint2].size() * 12) + 2 + 4 + ifdDataSizes[hint2];
            }
        }
        if (this.mHasThumbnail) {
            int thumbnailOffset = position;
            this.mAttributes[0].put(JPEG_INTERCHANGE_FORMAT_TAG.name, ExifAttribute.createULong(thumbnailOffset, this.mExifByteOrder));
            this.mThumbnailOffset = exifOffsetFromBeginning + thumbnailOffset;
            position += this.mThumbnailLength;
        }
        int totalSize = position + 8;
        if (!this.mAttributes[1].isEmpty()) {
            this.mAttributes[0].put(IFD_POINTER_TAGS[0].name, ExifAttribute.createULong(ifdOffsets[1], this.mExifByteOrder));
        }
        if (!this.mAttributes[2].isEmpty()) {
            this.mAttributes[0].put(IFD_POINTER_TAGS[1].name, ExifAttribute.createULong(ifdOffsets[2], this.mExifByteOrder));
        }
        if (!this.mAttributes[3].isEmpty()) {
            this.mAttributes[1].put(IFD_POINTER_TAGS[2].name, ExifAttribute.createULong(ifdOffsets[3], this.mExifByteOrder));
        }
        dataOutputStream.writeUnsignedShort(totalSize);
        dataOutputStream.write(IDENTIFIER_EXIF_APP1);
        dataOutputStream.writeShort(this.mExifByteOrder == ByteOrder.BIG_ENDIAN ? BYTE_ALIGN_MM : BYTE_ALIGN_II);
        dataOutputStream.setByteOrder(this.mExifByteOrder);
        dataOutputStream.writeUnsignedShort(42);
        dataOutputStream.writeUnsignedInt(8L);
        for (int hint3 = 0; hint3 < EXIF_TAGS.length; hint3++) {
            if (!this.mAttributes[hint3].isEmpty()) {
                dataOutputStream.writeUnsignedShort(this.mAttributes[hint3].size());
                int dataOffset = ifdOffsets[hint3] + 2 + (this.mAttributes[hint3].size() * 12) + 4;
                for (Map.Entry entry2 : this.mAttributes[hint3].entrySet()) {
                    ExifTag tag2 = (ExifTag) sExifTagMapsForWriting[hint3].get(entry2.getKey());
                    int tagNumber = tag2.number;
                    ExifAttribute attribute = (ExifAttribute) entry2.getValue();
                    int size2 = attribute.size();
                    dataOutputStream.writeUnsignedShort(tagNumber);
                    dataOutputStream.writeUnsignedShort(attribute.format);
                    dataOutputStream.writeInt(attribute.numberOfComponents);
                    if (size2 > 4) {
                        dataOutputStream.writeUnsignedInt(dataOffset);
                        dataOffset += size2;
                    } else {
                        dataOutputStream.write(attribute.bytes);
                        if (size2 < 4) {
                            for (int i2 = size2; i2 < 4; i2++) {
                                dataOutputStream.writeByte(0);
                            }
                        }
                    }
                }
                if (hint3 == 0 && !this.mAttributes[4].isEmpty()) {
                    dataOutputStream.writeUnsignedInt(ifdOffsets[4]);
                } else {
                    dataOutputStream.writeUnsignedInt(0L);
                }
                Iterator entry$iterator2 = this.mAttributes[hint3].entrySet().iterator();
                while (entry$iterator2.hasNext()) {
                    ExifAttribute attribute2 = (ExifAttribute) ((Map.Entry) entry$iterator2.next()).getValue();
                    if (attribute2.bytes.length > 4) {
                        dataOutputStream.write(attribute2.bytes, 0, attribute2.bytes.length);
                    }
                }
            }
        }
        if (this.mHasThumbnail) {
            dataOutputStream.write(getThumbnail());
        }
        dataOutputStream.setByteOrder(ByteOrder.BIG_ENDIAN);
        return totalSize;
    }

    private static Pair<Integer, Integer> guessDataFormat(String entryValue) {
        if (entryValue.contains(",")) {
            String[] entryValues = entryValue.split(",");
            Pair<Integer, Integer> dataFormat = guessDataFormat(entryValues[0]);
            if (((Integer) dataFormat.first).intValue() == 2) {
                return dataFormat;
            }
            for (int i = 1; i < entryValues.length; i++) {
                Pair<Integer, Integer> guessDataFormat = guessDataFormat(entryValues[i]);
                int first = -1;
                int second = -1;
                if (guessDataFormat.first == dataFormat.first || guessDataFormat.second == dataFormat.first) {
                    first = ((Integer) dataFormat.first).intValue();
                }
                if (((Integer) dataFormat.second).intValue() != -1 && (guessDataFormat.first == dataFormat.second || guessDataFormat.second == dataFormat.second)) {
                    second = ((Integer) dataFormat.second).intValue();
                }
                if (first == -1 && second == -1) {
                    return new Pair<>(2, -1);
                }
                if (first == -1) {
                    dataFormat = new Pair<>(Integer.valueOf(second), -1);
                } else if (second == -1) {
                    dataFormat = new Pair<>(Integer.valueOf(first), -1);
                }
            }
            return dataFormat;
        }
        if (entryValue.contains("/")) {
            String[] rationalNumber = entryValue.split("/");
            if (rationalNumber.length == 2) {
                try {
                    long numerator = Long.parseLong(rationalNumber[0]);
                    long denominator = Long.parseLong(rationalNumber[1]);
                    if (numerator < 0 || denominator < 0) {
                        return new Pair<>(10, -1);
                    }
                    if (numerator > 2147483647L || denominator > 2147483647L) {
                        return new Pair<>(5, -1);
                    }
                    return new Pair<>(10, 5);
                } catch (NumberFormatException e) {
                }
            }
            return new Pair<>(2, -1);
        }
        try {
            Long longValue = Long.valueOf(Long.parseLong(entryValue));
            if (longValue.longValue() >= 0 && longValue.longValue() <= 65535) {
                return new Pair<>(3, 4);
            }
            if (longValue.longValue() < 0) {
                return new Pair<>(9, -1);
            }
            return new Pair<>(4, -1);
        } catch (NumberFormatException e2) {
            try {
                Double.parseDouble(entryValue);
                return new Pair<>(12, -1);
            } catch (NumberFormatException e3) {
                return new Pair<>(2, -1);
            }
        }
    }

    private static class ByteOrderAwarenessDataInputStream extends ByteArrayInputStream {
        private ByteOrder mByteOrder;
        private final long mLength;
        private long mPosition;
        private static final ByteOrder LITTLE_ENDIAN = ByteOrder.LITTLE_ENDIAN;
        private static final ByteOrder BIG_ENDIAN = ByteOrder.BIG_ENDIAN;

        public ByteOrderAwarenessDataInputStream(byte[] bytes) {
            super(bytes);
            this.mByteOrder = ByteOrder.BIG_ENDIAN;
            this.mLength = bytes.length;
            this.mPosition = 0L;
        }

        public void setByteOrder(ByteOrder byteOrder) {
            this.mByteOrder = byteOrder;
        }

        public void seek(long byteCount) throws IOException {
            this.mPosition = 0L;
            reset();
            if (skip(byteCount) == byteCount) {
            } else {
                throw new IOException("Couldn't seek up to the byteCount");
            }
        }

        public long peek() {
            return this.mPosition;
        }

        public void readFully(byte[] buffer) throws IOException {
            this.mPosition += (long) buffer.length;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            if (super.read(buffer, 0, buffer.length) == buffer.length) {
            } else {
                throw new IOException("Couldn't read up to the length of buffer");
            }
        }

        public byte readByte() throws IOException {
            this.mPosition++;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            int ch = super.read();
            if (ch < 0) {
                throw new EOFException();
            }
            return (byte) ch;
        }

        public short readShort() throws IOException {
            this.mPosition += 2;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            int ch1 = super.read();
            int ch2 = super.read();
            if ((ch1 | ch2) < 0) {
                throw new EOFException();
            }
            if (this.mByteOrder == LITTLE_ENDIAN) {
                return (short) ((ch2 << 8) + ch1);
            }
            if (this.mByteOrder == BIG_ENDIAN) {
                return (short) ((ch1 << 8) + ch2);
            }
            throw new IOException("Invalid byte order: " + this.mByteOrder);
        }

        public int readInt() throws IOException {
            this.mPosition += 4;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            int ch1 = super.read();
            int ch2 = super.read();
            int ch3 = super.read();
            int ch4 = super.read();
            if ((ch1 | ch2 | ch3 | ch4) < 0) {
                throw new EOFException();
            }
            if (this.mByteOrder == LITTLE_ENDIAN) {
                return (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + ch1;
            }
            if (this.mByteOrder == BIG_ENDIAN) {
                return (ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4;
            }
            throw new IOException("Invalid byte order: " + this.mByteOrder);
        }

        @Override
        public long skip(long byteCount) {
            long skipped = super.skip(Math.min(byteCount, this.mLength - this.mPosition));
            this.mPosition += skipped;
            return skipped;
        }

        public int readUnsignedShort() throws IOException {
            this.mPosition += 2;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            int ch1 = super.read();
            int ch2 = super.read();
            if ((ch1 | ch2) < 0) {
                throw new EOFException();
            }
            if (this.mByteOrder == LITTLE_ENDIAN) {
                return (ch2 << 8) + ch1;
            }
            if (this.mByteOrder == BIG_ENDIAN) {
                return (ch1 << 8) + ch2;
            }
            throw new IOException("Invalid byte order: " + this.mByteOrder);
        }

        public long readUnsignedInt() throws IOException {
            return ((long) readInt()) & KeymasterArguments.UINT32_MAX_VALUE;
        }

        public long readLong() throws IOException {
            this.mPosition += 8;
            if (this.mPosition > this.mLength) {
                throw new EOFException();
            }
            int ch1 = super.read();
            int ch2 = super.read();
            int ch3 = super.read();
            int ch4 = super.read();
            int ch5 = super.read();
            int ch6 = super.read();
            int ch7 = super.read();
            int ch8 = super.read();
            if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0) {
                throw new EOFException();
            }
            if (this.mByteOrder == LITTLE_ENDIAN) {
                return (((long) ch8) << 56) + (((long) ch7) << 48) + (((long) ch6) << 40) + (((long) ch5) << 32) + (((long) ch4) << 24) + (((long) ch3) << 16) + (((long) ch2) << 8) + ((long) ch1);
            }
            if (this.mByteOrder == BIG_ENDIAN) {
                return (((long) ch1) << 56) + (((long) ch2) << 48) + (((long) ch3) << 40) + (((long) ch4) << 32) + (((long) ch5) << 24) + (((long) ch6) << 16) + (((long) ch7) << 8) + ((long) ch8);
            }
            throw new IOException("Invalid byte order: " + this.mByteOrder);
        }

        public float readFloat() throws IOException {
            return Float.intBitsToFloat(readInt());
        }

        public double readDouble() throws IOException {
            return Double.longBitsToDouble(readLong());
        }
    }

    private static class ByteOrderAwarenessDataOutputStream extends FilterOutputStream {
        private ByteOrder mByteOrder;
        private final OutputStream mOutputStream;

        public ByteOrderAwarenessDataOutputStream(OutputStream out, ByteOrder byteOrder) {
            super(out);
            this.mOutputStream = out;
            this.mByteOrder = byteOrder;
        }

        public void setByteOrder(ByteOrder byteOrder) {
            this.mByteOrder = byteOrder;
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            this.mOutputStream.write(bytes);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            this.mOutputStream.write(bytes, offset, length);
        }

        public void writeByte(int val) throws IOException {
            this.mOutputStream.write(val);
        }

        public void writeShort(short val) throws IOException {
            if (this.mByteOrder == ByteOrder.LITTLE_ENDIAN) {
                this.mOutputStream.write((val >>> 0) & 255);
                this.mOutputStream.write((val >>> 8) & 255);
            } else {
                if (this.mByteOrder != ByteOrder.BIG_ENDIAN) {
                    return;
                }
                this.mOutputStream.write((val >>> 8) & 255);
                this.mOutputStream.write((val >>> 0) & 255);
            }
        }

        public void writeInt(int val) throws IOException {
            if (this.mByteOrder == ByteOrder.LITTLE_ENDIAN) {
                this.mOutputStream.write((val >>> 0) & 255);
                this.mOutputStream.write((val >>> 8) & 255);
                this.mOutputStream.write((val >>> 16) & 255);
                this.mOutputStream.write((val >>> 24) & 255);
                return;
            }
            if (this.mByteOrder != ByteOrder.BIG_ENDIAN) {
                return;
            }
            this.mOutputStream.write((val >>> 24) & 255);
            this.mOutputStream.write((val >>> 16) & 255);
            this.mOutputStream.write((val >>> 8) & 255);
            this.mOutputStream.write((val >>> 0) & 255);
        }

        public void writeUnsignedShort(int val) throws IOException {
            writeShort((short) val);
        }

        public void writeUnsignedInt(long val) throws IOException {
            writeInt((int) val);
        }
    }
}
