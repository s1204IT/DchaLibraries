package android.media;

import android.content.ClipDescription;
import android.media.DecoderCapabilities;
import android.mtp.MtpConstants;
import android.os.SystemProperties;
import android.provider.ContactsContract;
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
    public static final int FILE_TYPE_APK = 799;
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
    public static final int FILE_TYPE_HTML = 701;
    public static final int FILE_TYPE_HTTPLIVE = 504;
    public static final int FILE_TYPE_ICS = 795;
    public static final int FILE_TYPE_ICZ = 796;
    public static final int FILE_TYPE_IMY = 203;
    public static final int FILE_TYPE_JPEG = 401;
    public static final int FILE_TYPE_M3U = 501;
    public static final int FILE_TYPE_M4A = 102;
    public static final int FILE_TYPE_M4V = 302;
    public static final int FILE_TYPE_MID = 201;
    public static final int FILE_TYPE_MKA = 109;
    public static final int FILE_TYPE_MKV = 307;
    public static final int FILE_TYPE_MP2 = 197;
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
    public static final int FILE_TYPE_VCF = 797;
    public static final int FILE_TYPE_VCS = 798;
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
    private static final int LAST_AUDIO_FILE_TYPE = 199;
    private static final int LAST_DRM_FILE_TYPE = 601;
    private static final int LAST_IMAGE_FILE_TYPE = 499;
    private static final int LAST_MIDI_FILE_TYPE = 203;
    private static final int LAST_PLAYLIST_FILE_TYPE = 504;
    private static final int LAST_RAW_IMAGE_FILE_TYPE = 809;
    private static final int LAST_VIDEO_FILE_TYPE = 399;
    private static final HashMap<String, MediaFileType> sFileTypeMap = new HashMap<>();
    private static final HashMap<String, Integer> sMimeTypeMap = new HashMap<>();
    private static final HashMap<String, Integer> sFileTypeToFormatMap = new HashMap<>();
    private static final HashMap<String, Integer> sMimeTypeToFormatMap = new HashMap<>();
    private static final HashMap<Integer, String> sFormatToMimeTypeMap = new HashMap<>();

    public static class MediaFileType {
        public final int fileType;
        public final String mimeType;

        MediaFileType(int fileType, String mimeType) {
            this.fileType = fileType;
            this.mimeType = mimeType;
        }
    }

    static {
        addFileType("3GP", 199, MediaFormat.MIMETYPE_AUDIO_AMR_NB);
        addFileType("3GA", 193, MediaFormat.MIMETYPE_AUDIO_AMR_NB);
        addFileType("MOV", 194, "audio/quicktime");
        addFileType("QT", 194, "audio/quicktime");
        addFileType("WAV", 103, "audio/wav", 12296);
        addFileType("OGG", 107, MediaFormat.MIMETYPE_AUDIO_VORBIS, MtpConstants.FORMAT_OGG);
        addFileType("OGG", 107, "audio/webm", MtpConstants.FORMAT_OGG);
        addFileType("MP3", 101, MediaFormat.MIMETYPE_AUDIO_MPEG, 12297);
        addFileType("MPGA", 101, MediaFormat.MIMETYPE_AUDIO_MPEG, 12297);
        addFileType("M4A", 102, "audio/mp4", 12299);
        addFileType("WAV", 103, "audio/x-wav", 12296);
        addFileType("AMR", 104, "audio/amr");
        addFileType("AWB", 105, MediaFormat.MIMETYPE_AUDIO_AMR_WB);
        addFileType("OGG", 107, "audio/ogg", MtpConstants.FORMAT_OGG);
        addFileType("OGG", 107, "application/ogg", MtpConstants.FORMAT_OGG);
        addFileType("OGA", 107, "application/ogg", MtpConstants.FORMAT_OGG);
        addFileType("AAC", 108, "audio/aac", MtpConstants.FORMAT_AAC);
        addFileType("AAC", 108, "audio/aac-adts", MtpConstants.FORMAT_AAC);
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
        addFileType("MTS", FILE_TYPE_MP2TS, "video/mp2ts");
        addFileType("M2TS", FILE_TYPE_MP2TS, "video/mp2ts");
        addFileType("MOV", FILE_TYPE_QUICKTIME_VIDEO, "video/quicktime");
        addFileType("QT", FILE_TYPE_QUICKTIME_VIDEO, "video/quicktime");
        addFileType("OGV", FILE_TYPE_OGM, "video/ogm");
        addFileType("OGM", FILE_TYPE_OGM, "video/ogm");
        addFileType("MPEG", 301, "video/mpeg", 12299);
        addFileType("MPG", 301, "video/mpeg", 12299);
        addFileType("MP4", 301, "video/mp4", 12299);
        addFileType("M4V", 302, "video/mp4", 12299);
        addFileType("3GP", FILE_TYPE_3GPP, MediaFormat.MIMETYPE_VIDEO_H263, MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("3GPP", FILE_TYPE_3GPP, MediaFormat.MIMETYPE_VIDEO_H263, MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2", MtpConstants.FORMAT_3GP_CONTAINER);
        addFileType("MKV", FILE_TYPE_MKV, "video/x-matroska");
        addFileType("WEBM", FILE_TYPE_WEBM, "video/webm");
        addFileType("TS", FILE_TYPE_MP2TS, "video/mp2ts");
        if (!SystemProperties.getBoolean("ro.mtk_bsp_package", false)) {
            addFileType("MPO", 499, "image/mpo");
        }
        addFileType("JPG", 401, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG);
        addFileType("JPEG", 401, "image/jpeg", MtpConstants.FORMAT_EXIF_JPEG);
        addFileType("GIF", FILE_TYPE_GIF, "image/gif", MtpConstants.FORMAT_GIF);
        addFileType("PNG", 403, "image/png", MtpConstants.FORMAT_PNG);
        addFileType("BMP", 404, "image/x-ms-bmp", MtpConstants.FORMAT_BMP);
        addFileType("WBMP", 405, "image/vnd.wap.wbmp", MtpConstants.FORMAT_DEFINED);
        addFileType("WEBP", 406, "image/webp", MtpConstants.FORMAT_DEFINED);
        addFileType("DNG", 800, "image/x-adobe-dng", MtpConstants.FORMAT_DNG);
        addFileType("CR2", 801, "image/x-canon-cr2", MtpConstants.FORMAT_TIFF);
        addFileType("NEF", 802, "image/x-nikon-nef", MtpConstants.FORMAT_TIFF_EP);
        addFileType("NRW", 803, "image/x-nikon-nrw", MtpConstants.FORMAT_TIFF);
        addFileType("ARW", FILE_TYPE_ARW, "image/x-sony-arw", MtpConstants.FORMAT_TIFF);
        addFileType("RW2", FILE_TYPE_RW2, "image/x-panasonic-rw2", MtpConstants.FORMAT_TIFF);
        addFileType("ORF", FILE_TYPE_ORF, "image/x-olympus-orf", MtpConstants.FORMAT_TIFF);
        addFileType("RAF", FILE_TYPE_RAF, "image/x-fuji-raf", MtpConstants.FORMAT_DEFINED);
        addFileType("PEF", FILE_TYPE_PEF, "image/x-pentax-pef", MtpConstants.FORMAT_TIFF);
        addFileType("SRW", 809, "image/x-samsung-srw", MtpConstants.FORMAT_TIFF);
        addFileType("M3U", 501, "audio/x-mpegurl", MtpConstants.FORMAT_M3U_PLAYLIST);
        addFileType("M3U", 501, "application/x-mpegurl", MtpConstants.FORMAT_M3U_PLAYLIST);
        addFileType("PLS", 502, "audio/x-scpls", MtpConstants.FORMAT_PLS_PLAYLIST);
        addFileType("WPL", 503, "application/vnd.ms-wpl", MtpConstants.FORMAT_WPL_PLAYLIST);
        addFileType("M3U8", 504, "application/vnd.apple.mpegurl");
        addFileType("M3U8", 504, "audio/mpegurl");
        addFileType("M3U8", 504, "audio/x-mpegurl");
        addFileType("FL", FILE_TYPE_FL, "application/x-android-drm-fl");
        addFileType("TXT", 700, ClipDescription.MIMETYPE_TEXT_PLAIN, 12292);
        addFileType("HTM", 701, ClipDescription.MIMETYPE_TEXT_HTML, 12293);
        addFileType("HTML", 701, ClipDescription.MIMETYPE_TEXT_HTML, 12293);
        addFileType("PDF", 702, "application/pdf");
        addFileType("DOC", FILE_TYPE_MS_WORD, "application/msword", MtpConstants.FORMAT_MS_WORD_DOCUMENT);
        addFileType("XLS", FILE_TYPE_MS_EXCEL, "application/vnd.ms-excel", MtpConstants.FORMAT_MS_EXCEL_SPREADSHEET);
        addFileType("PPT", FILE_TYPE_MS_POWERPOINT, "application/vnd.ms-powerpoint", MtpConstants.FORMAT_MS_POWERPOINT_PRESENTATION);
        addFileType("FLAC", 110, MediaFormat.MIMETYPE_AUDIO_FLAC, MtpConstants.FORMAT_FLAC);
        addFileType("ZIP", FILE_TYPE_ZIP, "application/zip");
        addFileType("MPG", FILE_TYPE_MP2PS, "video/mp2p");
        addFileType("MPEG", FILE_TYPE_MP2PS, "video/mp2p");
        addFileType("ICS", FILE_TYPE_ICS, "text/calendar");
        addFileType("ICZ", FILE_TYPE_ICZ, "text/calendar");
        addFileType("VCF", FILE_TYPE_VCF, ContactsContract.Contacts.CONTENT_VCARD_TYPE);
        addFileType("VCS", FILE_TYPE_VCS, "text/x-vcalendar");
        addFileType("APK", FILE_TYPE_APK, "application/vnd.android.package-archive");
        addFileType("DOCX", FILE_TYPE_MS_WORD, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        addFileType("DOTX", FILE_TYPE_MS_WORD, "application/vnd.openxmlformats-officedocument.wordprocessingml.template");
        addFileType("XLSX", FILE_TYPE_MS_EXCEL, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        addFileType("XLTX", FILE_TYPE_MS_EXCEL, "application/vnd.openxmlformats-officedocument.spreadsheetml.template");
        addFileType("PPTX", FILE_TYPE_MS_POWERPOINT, "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        addFileType("POTX", FILE_TYPE_MS_POWERPOINT, "application/vnd.openxmlformats-officedocument.presentationml.template");
        addFileType("PPSX", FILE_TYPE_MS_POWERPOINT, "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
    }

    static void addFileType(String extension, int fileType, String mimeType) {
        sFileTypeMap.put(extension, new MediaFileType(fileType, mimeType));
        sMimeTypeMap.put(mimeType, Integer.valueOf(fileType));
    }

    static void addFileType(String extension, int fileType, String mimeType, int mtpFormatCode) {
        addFileType(extension, fileType, mimeType);
        sFileTypeToFormatMap.put(extension, Integer.valueOf(mtpFormatCode));
        sMimeTypeToFormatMap.put(mimeType, Integer.valueOf(mtpFormatCode));
        sFormatToMimeTypeMap.put(Integer.valueOf(mtpFormatCode), mimeType);
    }

    private static boolean isWMAEnabled() {
        if (!SystemProperties.getBoolean("ro.mtk_wmv_playback_support", false)) {
            return false;
        }
        List<DecoderCapabilities.AudioDecoder> decoders = DecoderCapabilities.getAudioDecoders();
        int count = decoders.size();
        for (int i = 0; i < count; i++) {
            DecoderCapabilities.AudioDecoder decoder = decoders.get(i);
            if (decoder == DecoderCapabilities.AudioDecoder.AUDIO_DECODER_WMA) {
                return true;
            }
        }
        return false;
    }

    private static boolean isWMVEnabled() {
        if (!SystemProperties.getBoolean("ro.mtk_wmv_playback_support", false)) {
            return false;
        }
        List<DecoderCapabilities.VideoDecoder> decoders = DecoderCapabilities.getVideoDecoders();
        int count = decoders.size();
        for (int i = 0; i < count; i++) {
            DecoderCapabilities.VideoDecoder decoder = decoders.get(i);
            if (decoder == DecoderCapabilities.VideoDecoder.VIDEO_DECODER_WMV) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAudioFileType(int fileType) {
        if (fileType < 101 || fileType > 199) {
            return fileType >= 201 && fileType <= 203;
        }
        return true;
    }

    public static boolean isVideoFileType(int fileType) {
        return fileType >= 301 && fileType <= 399;
    }

    public static boolean isImageFileType(int fileType) {
        if (fileType < 401 || fileType > 499) {
            return fileType >= 800 && fileType <= 809;
        }
        return true;
    }

    public static boolean isRawImageFileType(int fileType) {
        return fileType >= 800 && fileType <= 809;
    }

    public static boolean isPlayListFileType(int fileType) {
        return fileType >= 501 && fileType <= 504;
    }

    public static boolean isDrmFileType(int fileType) {
        return fileType >= 601 && fileType <= 601;
    }

    public static MediaFileType getFileType(String path) {
        int lastDot = path.lastIndexOf(46);
        if (lastDot < 0) {
            return null;
        }
        return sFileTypeMap.get(path.substring(lastDot + 1).toUpperCase(Locale.ROOT));
    }

    public static boolean isMimeTypeMedia(String mimeType) {
        int fileType = getFileTypeForMimeType(mimeType);
        if (isAudioFileType(fileType) || isVideoFileType(fileType) || isImageFileType(fileType)) {
            return true;
        }
        return isPlayListFileType(fileType);
    }

    public static String getFileTitle(String path) {
        int lastSlash;
        int lastSlash2 = path.lastIndexOf(47);
        if (lastSlash2 >= 0 && (lastSlash = lastSlash2 + 1) < path.length()) {
            path = path.substring(lastSlash);
        }
        int lastDot = path.lastIndexOf(46);
        if (lastDot > 0) {
            return path.substring(0, lastDot);
        }
        return path;
    }

    public static int getFileTypeForMimeType(String mimeType) {
        Integer value = sMimeTypeMap.get(mimeType);
        if (value == null) {
            return 0;
        }
        return value.intValue();
    }

    public static String getMimeTypeForFile(String path) {
        MediaFileType mediaFileType = getFileType(path);
        if (mediaFileType == null) {
            return null;
        }
        return mediaFileType.mimeType;
    }

    public static int getFormatCode(String fileName, String mimeType) {
        Integer value;
        if (mimeType != null && (value = sMimeTypeToFormatMap.get(mimeType)) != null) {
            return value.intValue();
        }
        int lastDot = fileName.lastIndexOf(46);
        if (lastDot > 0) {
            String extension = fileName.substring(lastDot + 1).toUpperCase(Locale.ROOT);
            Integer value2 = sFileTypeToFormatMap.get(extension);
            if (value2 != null) {
                return value2.intValue();
            }
            return 12288;
        }
        return 12288;
    }

    public static String getMimeTypeForFormatCode(int formatCode) {
        return sFormatToMimeTypeMap.get(Integer.valueOf(formatCode));
    }
}
