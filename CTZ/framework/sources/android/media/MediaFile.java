package android.media;

import android.content.ClipDescription;
import android.media.DecoderCapabilities;
import android.mtp.MtpConstants;
import android.os.SystemProperties;
import com.mediatek.media.MediaFactory;
import com.mediatek.media.mediascanner.MediaFileEx;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MediaFile {
    public static final int FILE_TYPE_3GA = 193;
    public static final int FILE_TYPE_3GPP = 303;
    public static final int FILE_TYPE_3GPP2 = 304;
    public static final int FILE_TYPE_3GPP3 = 199;
    public static final int FILE_TYPE_AAC = 108;
    public static final int FILE_TYPE_AMR = 104;
    public static final int FILE_TYPE_APE = 111;
    public static final int FILE_TYPE_ARW = 804;
    public static final int FILE_TYPE_ASF = 306;
    public static final int FILE_TYPE_AVI = 309;
    public static final int FILE_TYPE_AWB = 105;
    public static final int FILE_TYPE_BMP = 404;
    public static final int FILE_TYPE_CAF = 112;
    public static final int FILE_TYPE_CR2 = 801;
    public static final int FILE_TYPE_DNG = 800;
    public static final int FILE_TYPE_FL = 601;
    public static final int FILE_TYPE_FLA = 196;
    public static final int FILE_TYPE_FLAC = 110;
    public static final int FILE_TYPE_FLV = 398;
    public static final int FILE_TYPE_GIF = 402;
    public static final int FILE_TYPE_HEIF = 407;
    public static final int FILE_TYPE_HTML = 701;
    public static final int FILE_TYPE_HTTPLIVE = 504;
    public static final int FILE_TYPE_IMY = 203;
    public static final int FILE_TYPE_JPEG = 401;
    public static final int FILE_TYPE_M3U = 501;
    public static final int FILE_TYPE_M4A = 102;
    public static final int FILE_TYPE_M4V = 302;
    public static final int FILE_TYPE_MID = 201;
    public static final int FILE_TYPE_MKA = 109;
    public static final int FILE_TYPE_MKV = 307;
    public static final int FILE_TYPE_MP2PS = 393;
    public static final int FILE_TYPE_MP2TS = 308;
    public static final int FILE_TYPE_MP3 = 101;
    public static final int FILE_TYPE_MP4 = 301;
    public static final int FILE_TYPE_MPO = 499;
    public static final int FILE_TYPE_MS_EXCEL = 705;
    public static final int FILE_TYPE_MS_POWERPOINT = 706;
    public static final int FILE_TYPE_MS_WORD = 704;
    public static final int FILE_TYPE_NEF = 802;
    public static final int FILE_TYPE_NRW = 803;
    public static final int FILE_TYPE_OGG = 107;
    public static final int FILE_TYPE_OGM = 394;
    public static final int FILE_TYPE_ORF = 806;
    public static final int FILE_TYPE_PDF = 702;
    public static final int FILE_TYPE_PEF = 808;
    public static final int FILE_TYPE_PLS = 502;
    public static final int FILE_TYPE_PNG = 403;
    public static final int FILE_TYPE_QT = 200;
    public static final int FILE_TYPE_QUICKTIME_AUDIO = 194;
    public static final int FILE_TYPE_QUICKTIME_VIDEO = 397;
    public static final int FILE_TYPE_RA = 198;
    public static final int FILE_TYPE_RAF = 807;
    public static final int FILE_TYPE_RM = 399;
    public static final int FILE_TYPE_RMVB = 396;
    public static final int FILE_TYPE_RV = 395;
    public static final int FILE_TYPE_RW2 = 805;
    public static final int FILE_TYPE_SMF = 202;
    public static final int FILE_TYPE_SRW = 809;
    public static final int FILE_TYPE_TEXT = 700;
    public static final int FILE_TYPE_WAV = 103;
    public static final int FILE_TYPE_WBMP = 405;
    public static final int FILE_TYPE_WEBM = 310;
    public static final int FILE_TYPE_WEBP = 406;
    public static final int FILE_TYPE_WMA = 106;
    public static final int FILE_TYPE_WMV = 305;
    public static final int FILE_TYPE_WPL = 503;
    public static final int FILE_TYPE_XML = 703;
    public static final int FILE_TYPE_ZIP = 707;
    private static final int FIRST_AUDIO_FILE_TYPE = 101;
    private static final int FIRST_DRM_FILE_TYPE = 601;
    private static final int FIRST_IMAGE_FILE_TYPE = 401;
    private static final int FIRST_MIDI_FILE_TYPE = 201;
    private static final int FIRST_PLAYLIST_FILE_TYPE = 501;
    private static final int FIRST_RAW_IMAGE_FILE_TYPE = 800;
    private static final int FIRST_VIDEO_FILE_TYPE = 301;
    private static final int FIRST_VIDEO_FILE_TYPE2 = 200;
    private static final int LAST_AUDIO_FILE_TYPE = 199;
    private static final int LAST_DRM_FILE_TYPE = 601;
    private static final int LAST_IMAGE_FILE_TYPE = 499;
    private static final int LAST_MIDI_FILE_TYPE = 203;
    private static final int LAST_PLAYLIST_FILE_TYPE = 504;
    private static final int LAST_RAW_IMAGE_FILE_TYPE = 809;
    private static final int LAST_VIDEO_FILE_TYPE = 399;
    private static final int LAST_VIDEO_FILE_TYPE2 = 200;
    private static MediaFileEx sMediaFileEx = MediaFactory.getInstance().getMediaFileEx();
    private static final HashMap<String, MediaFileType> sFileTypeMap = new HashMap<>();
    private static final HashMap<String, Integer> sMimeTypeMap = new HashMap<>();
    private static final HashMap<String, Integer> sFileTypeToFormatMap = new HashMap<>();
    private static final HashMap<String, Integer> sMimeTypeToFormatMap = new HashMap<>();
    private static final HashMap<Integer, String> sFormatToMimeTypeMap = new HashMap<>();

    static {
        sMediaFileEx.addMoreAudioFileType();
        addFileType("MP3", 101, MediaFormat.MIMETYPE_AUDIO_MPEG, 12297, true);
        addFileType("MPGA", 101, MediaFormat.MIMETYPE_AUDIO_MPEG, 12297, false);
        addFileType("M4A", 102, "audio/mp4", 12299, false);
        addFileType("WAV", 103, "audio/x-wav", 12296, true);
        addFileType("AMR", 104, "audio/amr");
        addFileType("AWB", 105, MediaFormat.MIMETYPE_AUDIO_AMR_WB);
        addFileType("OGG", 107, "audio/ogg", MtpConstants.FORMAT_OGG, false);
        addFileType("OGG", 107, "application/ogg", MtpConstants.FORMAT_OGG, true);
        addFileType("OGA", 107, "application/ogg", MtpConstants.FORMAT_OGG, false);
        addFileType("AAC", 108, "audio/aac", MtpConstants.FORMAT_AAC, true);
        addFileType("AAC", 108, "audio/aac-adts", MtpConstants.FORMAT_AAC, false);
        addFileType("MKA", 109, "audio/x-matroska");
        addFileType("MID", 201, "audio/midi");
        addFileType("MIDI", 201, "audio/midi");
        addFileType("XMF", 201, "audio/midi");
        addFileType("RTTTL", 201, "audio/midi");
        addFileType("SMF", 202, "audio/sp-midi");
        addFileType("IMY", 203, "audio/imelody");
        addFileType("RTX", 201, "audio/midi");
        addFileType("OTA", 201, "audio/midi");
        addFileType("MXMF", 201, "audio/midi");
        sMediaFileEx.addMoreVideoFileType();
        addFileType("MPEG", 301, "video/mpeg", 12299, true);
        addFileType("MPG", 301, "video/mpeg", 12299, false);
        addFileType("MP4", 301, "video/mp4", 12299, false);
        addFileType("M4V", 302, "video/mp4", 12299, false);
        addFileType("MOV", 200, "video/quicktime", 12299, false);
        addFileType("3GP", 303, MediaFormat.MIMETYPE_VIDEO_H263, MtpConstants.FORMAT_3GP_CONTAINER, true);
        addFileType("3GPP", 303, MediaFormat.MIMETYPE_VIDEO_H263, MtpConstants.FORMAT_3GP_CONTAINER, false);
        addFileType("3G2", 304, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER, false);
        addFileType("3GPP2", 304, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER, false);
        addFileType("MKV", 307, "video/x-matroska");
        addFileType("WEBM", 310, "video/webm");
        addFileType("TS", 308, "video/mp2ts");
        sMediaFileEx.addMoreImageFileType();
        addFileType("JPG", 401, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG, true);
        addFileType("JPEG", 401, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG, false);
        addFileType("GIF", 402, "image/gif", MtpConstants.FORMAT_GIF, true);
        addFileType("PNG", 403, "image/png", MtpConstants.FORMAT_PNG, true);
        addFileType("BMP", 404, "image/x-ms-bmp", MtpConstants.FORMAT_BMP, true);
        addFileType("WBMP", 405, "image/vnd.wap.wbmp", MtpConstants.FORMAT_DEFINED, false);
        addFileType("WEBP", 406, "image/webp", MtpConstants.FORMAT_DEFINED, false);
        addFileType("HEIC", 407, "image/heif", MtpConstants.FORMAT_HEIF, true);
        addFileType("HEIF", 407, "image/heif", MtpConstants.FORMAT_HEIF, false);
        addFileType("DNG", 800, "image/x-adobe-dng", MtpConstants.FORMAT_DNG, true);
        addFileType("CR2", 801, "image/x-canon-cr2", MtpConstants.FORMAT_TIFF, false);
        addFileType("NEF", 802, "image/x-nikon-nef", MtpConstants.FORMAT_TIFF_EP, false);
        addFileType("NRW", 803, "image/x-nikon-nrw", MtpConstants.FORMAT_TIFF, false);
        addFileType("ARW", 804, "image/x-sony-arw", MtpConstants.FORMAT_TIFF, false);
        addFileType("RW2", 805, "image/x-panasonic-rw2", MtpConstants.FORMAT_TIFF, false);
        addFileType("ORF", 806, "image/x-olympus-orf", MtpConstants.FORMAT_TIFF, false);
        addFileType("RAF", 807, "image/x-fuji-raf", MtpConstants.FORMAT_DEFINED, false);
        addFileType("PEF", 808, "image/x-pentax-pef", MtpConstants.FORMAT_TIFF, false);
        addFileType("SRW", 809, "image/x-samsung-srw", MtpConstants.FORMAT_TIFF, false);
        addFileType("M3U", 501, "audio/x-mpegurl", MtpConstants.FORMAT_M3U_PLAYLIST, true);
        addFileType("M3U", 501, "application/x-mpegurl", MtpConstants.FORMAT_M3U_PLAYLIST, false);
        addFileType("PLS", 502, "audio/x-scpls", MtpConstants.FORMAT_PLS_PLAYLIST, true);
        addFileType("WPL", 503, "application/vnd.ms-wpl", MtpConstants.FORMAT_WPL_PLAYLIST, true);
        addFileType("M3U8", 504, "application/vnd.apple.mpegurl");
        addFileType("M3U8", 504, "audio/mpegurl");
        addFileType("M3U8", 504, "audio/x-mpegurl");
        addFileType("FL", 601, "application/x-android-drm-fl");
        addFileType("TXT", 700, ClipDescription.MIMETYPE_TEXT_PLAIN, 12292, true);
        addFileType("HTM", 701, ClipDescription.MIMETYPE_TEXT_HTML, 12293, true);
        addFileType("HTML", 701, ClipDescription.MIMETYPE_TEXT_HTML, 12293, false);
        addFileType("PDF", 702, "application/pdf");
        addFileType("DOC", 704, "application/msword", MtpConstants.FORMAT_MS_WORD_DOCUMENT, true);
        addFileType("XLS", 705, "application/vnd.ms-excel", MtpConstants.FORMAT_MS_EXCEL_SPREADSHEET, true);
        addFileType("PPT", 706, "application/vnd.ms-powerpoint", MtpConstants.FORMAT_MS_POWERPOINT_PRESENTATION, true);
        addFileType("FLAC", 110, MediaFormat.MIMETYPE_AUDIO_FLAC, MtpConstants.FORMAT_FLAC, true);
        addFileType("ZIP", 707, "application/zip");
        addFileType("MPG", 393, "video/mp2p");
        addFileType("MPEG", 393, "video/mp2p");
        sMediaFileEx.addMoreOtherFileType();
    }

    public static class MediaFileType {
        public final int fileType;
        public final String mimeType;

        MediaFileType(int i, String str) {
            this.fileType = i;
            this.mimeType = str;
        }
    }

    static void addFileType(String str, int i, String str2) {
        sFileTypeMap.put(str, new MediaFileType(i, str2));
        sMimeTypeMap.put(str2, Integer.valueOf(i));
    }

    private static void addFileType(String str, int i, String str2, int i2, boolean z) {
        addFileType(str, i, str2);
        sFileTypeToFormatMap.put(str, Integer.valueOf(i2));
        sMimeTypeToFormatMap.put(str2, Integer.valueOf(i2));
        if (z && !sFormatToMimeTypeMap.containsKey(Integer.valueOf(i2))) {
            sFormatToMimeTypeMap.put(Integer.valueOf(i2), str2);
        }
    }

    private static boolean isWMAEnabled() {
        if (!SystemProperties.getBoolean("ro.vendor.mtk_wmv_playback_support", false)) {
            return false;
        }
        List<DecoderCapabilities.AudioDecoder> audioDecoders = DecoderCapabilities.getAudioDecoders();
        int size = audioDecoders.size();
        for (int i = 0; i < size; i++) {
            if (audioDecoders.get(i) == DecoderCapabilities.AudioDecoder.AUDIO_DECODER_WMA) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWMVEnabled() {
        if (!SystemProperties.getBoolean("ro.vendor.mtk_wmv_playback_support", false)) {
            return false;
        }
        List<DecoderCapabilities.VideoDecoder> videoDecoders = DecoderCapabilities.getVideoDecoders();
        int size = videoDecoders.size();
        for (int i = 0; i < size; i++) {
            if (videoDecoders.get(i) == DecoderCapabilities.VideoDecoder.VIDEO_DECODER_WMV) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAudioFileType(int i) {
        return (i >= 101 && i <= 199) || (i >= 201 && i <= 203);
    }

    public static boolean isVideoFileType(int i) {
        return (i >= 301 && i <= 399) || (i >= 200 && i <= 200);
    }

    public static boolean isImageFileType(int i) {
        return (i >= 401 && i <= 499) || (i >= 800 && i <= 809);
    }

    public static boolean isRawImageFileType(int i) {
        return i >= 800 && i <= 809;
    }

    public static boolean isPlayListFileType(int i) {
        return i >= 501 && i <= 504;
    }

    public static boolean isDrmFileType(int i) {
        return i >= 601 && i <= 601;
    }

    public static MediaFileType getFileType(String str) {
        int iLastIndexOf = str.lastIndexOf(46);
        if (iLastIndexOf < 0) {
            return null;
        }
        return sFileTypeMap.get(str.substring(iLastIndexOf + 1).toUpperCase(Locale.ROOT));
    }

    public static boolean isMimeTypeMedia(String str) {
        int fileTypeForMimeType = getFileTypeForMimeType(str);
        return isAudioFileType(fileTypeForMimeType) || isVideoFileType(fileTypeForMimeType) || isImageFileType(fileTypeForMimeType) || isPlayListFileType(fileTypeForMimeType);
    }

    public static String getFileTitle(String str) {
        int i;
        int iLastIndexOf = str.lastIndexOf(47);
        if (iLastIndexOf >= 0 && (i = iLastIndexOf + 1) < str.length()) {
            str = str.substring(i);
        }
        int iLastIndexOf2 = str.lastIndexOf(46);
        if (iLastIndexOf2 > 0) {
            return str.substring(0, iLastIndexOf2);
        }
        return str;
    }

    public static int getFileTypeForMimeType(String str) {
        Integer num = sMimeTypeMap.get(str);
        if (num == null) {
            return 0;
        }
        return num.intValue();
    }

    public static String getMimeTypeForFile(String str) {
        MediaFileType fileType = getFileType(str);
        if (fileType == null) {
            return null;
        }
        return fileType.mimeType;
    }

    public static int getFormatCode(String str, String str2) {
        Integer num;
        if (str2 != null && (num = sMimeTypeToFormatMap.get(str2)) != null) {
            return num.intValue();
        }
        int iLastIndexOf = str.lastIndexOf(46);
        if (iLastIndexOf > 0) {
            Integer num2 = sFileTypeToFormatMap.get(str.substring(iLastIndexOf + 1).toUpperCase(Locale.ROOT));
            if (num2 != null) {
                return num2.intValue();
            }
            return 12288;
        }
        return 12288;
    }

    public static String getMimeTypeForFormatCode(int i) {
        return sFormatToMimeTypeMap.get(Integer.valueOf(i));
    }
}
