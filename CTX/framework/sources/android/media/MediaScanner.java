package android.media;

import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.drm.DrmManagerClient;
import android.graphics.BitmapFactory;
import android.media.MediaFile;
import android.mtp.MtpConstants;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.MediaStore;
import android.provider.Settings;
import android.sax.ElementListener;
import android.sax.RootElement;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.telecom.Logging.Session;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.util.Xml;
import com.mediatek.media.MediaFactory;
import com.mediatek.media.mediascanner.MediaScannerClientEx;
import dalvik.system.CloseGuard;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class MediaScanner implements AutoCloseable {
    private static final String ALARMS_DIR = "/alarms/";
    private static final int DATE_MODIFIED_PLAYLISTS_COLUMN_INDEX = 2;
    private static final String DEFAULT_RINGTONE_PROPERTY_PREFIX = "ro.config.";
    protected static final boolean ENABLE_BULK_INSERTS = true;
    private static final int FILES_PRESCAN_DATE_MODIFIED_COLUMN_INDEX = 3;
    private static final int FILES_PRESCAN_FORMAT_COLUMN_INDEX = 2;
    private static final int FILES_PRESCAN_ID_COLUMN_INDEX = 0;
    private static final int FILES_PRESCAN_PATH_COLUMN_INDEX = 1;
    private static final String[] FILES_PRESCAN_PROJECTION;
    private static final String[] ID3_GENRES;
    private static final int ID_PLAYLISTS_COLUMN_INDEX = 0;
    protected static final String[] ID_PROJECTION;
    public static final String LAST_INTERNAL_SCAN_FINGERPRINT = "lastScanFingerprint";
    private static final String MUSIC_DIR = "/music/";
    private static final String NOTIFICATIONS_DIR = "/notifications/";
    private static final int PATH_PLAYLISTS_COLUMN_INDEX = 1;
    private static final String[] PLAYLIST_MEMBERS_PROJECTION;
    private static final String PODCAST_DIR = "/podcasts/";
    private static final String PRODUCT_SOUNDS_DIR = "/product/media/audio";
    private static final String RINGTONES_DIR = "/ringtones/";
    public static final String SCANNED_BUILD_PREFS_NAME = "MediaScanBuild";
    private static final String SYSTEM_SOUNDS_DIR = "/system/media/audio";
    private static final String TAG = "MediaScanner";
    private static HashMap<String, String> mMediaPaths;
    private static HashMap<String, String> mNoMediaPaths;
    private static String sLastInternalScanFingerprint;
    private final Uri mAudioUri;
    private final Context mContext;
    private String mDefaultAlarmAlertFilename;
    private boolean mDefaultAlarmSet;
    private String mDefaultNotificationFilename;
    private boolean mDefaultNotificationSet;
    private String mDefaultRingtoneFilename;
    private boolean mDefaultRingtoneSet;
    private final Uri mFilesUri;
    private final Uri mFilesUriNoNotify;
    protected final Uri mImagesUri;
    protected MediaInserter mMediaInserter;
    protected final ContentProviderClient mMediaProvider;
    private int mMtpObjectHandle;
    private long mNativeContext;
    private int mOriginalCount;
    private final String mPackageName;
    private final Uri mPlaylistsUri;
    private final boolean mProcessGenres;
    protected final boolean mProcessPlaylists;
    protected final Uri mVideoUri;
    private final String mVolumeName;
    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private boolean mWasEmptyPriorToScan = false;
    private final BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
    private final ArrayList<PlaylistEntry> mPlaylistEntries = new ArrayList<>();
    protected final ArrayList<FileEntry> mPlayLists = new ArrayList<>();
    protected ArrayList<String> mPlaylistFilePathList = new ArrayList<>();
    private DrmManagerClient mDrmManagerClient = null;
    protected final MyMediaScannerClient mClient = new MyMediaScannerClient();

    private final native void native_finalize();

    private static final native void native_init();

    private final native void native_setup();

    private native boolean processFile(String str, String str2, MediaScannerClient mediaScannerClient);

    private native void setLocale(String str);

    public native byte[] extractAlbumArt(FileDescriptor fileDescriptor);

    protected native void processDirectory(String str, MediaScannerClient mediaScannerClient);

    static {
        System.loadLibrary("media_jni");
        native_init();
        FILES_PRESCAN_PROJECTION = new String[]{"_id", "_data", "format", "date_modified"};
        ID_PROJECTION = new String[]{"_id"};
        PLAYLIST_MEMBERS_PROJECTION = new String[]{MediaStore.Audio.Playlists.Members.PLAYLIST_ID};
        ID3_GENRES = new String[]{"Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise", "AlternRock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop", "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret", "New Wave", "Psychadelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock", "Folk", "Folk-Rock", "National Folk", "Swing", "Fast Fusion", "Bebob", "Latin", "Revival", "Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock", "Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band", "Chorus", "Easy Listening", "Acoustic", "Humour", "Speech", "Chanson", "Opera", "Chamber Music", "Sonata", "Symphony", "Booty Bass", "Primus", "Porn Groove", "Satire", "Slow Jam", "Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad", "Rhythmic Soul", "Freestyle", "Duet", "Punk Rock", "Drum Solo", "A capella", "Euro-House", "Dance Hall", "Goa", "Drum & Bass", "Club-House", "Hardcore", "Terror", "Indie", "Britpop", null, "Polsk Punk", "Beat", "Christian Gangsta", "Heavy Metal", "Black Metal", "Crossover", "Contemporary Christian", "Christian Rock", "Merengue", "Salsa", "Thrash Metal", "Anime", "JPop", "Synthpop"};
        mNoMediaPaths = new HashMap<>();
        mMediaPaths = new HashMap<>();
    }

    protected static class FileEntry {
        public int mFormat;
        public long mLastModified;
        public boolean mLastModifiedChanged = false;
        public String mPath;
        public long mRowId;

        public FileEntry(long j, String str, long j2, int i) {
            this.mRowId = j;
            this.mPath = str;
            this.mLastModified = j2;
            this.mFormat = i;
        }

        public String toString() {
            return this.mPath + " mRowId: " + this.mRowId;
        }
    }

    private static class PlaylistEntry {
        long bestmatchid;
        int bestmatchlevel;
        String path;

        private PlaylistEntry() {
        }
    }

    public MediaScanner(Context context, String str) {
        native_setup();
        this.mContext = context;
        this.mPackageName = context.getPackageName();
        this.mVolumeName = str;
        this.mBitmapOptions.inSampleSize = 1;
        this.mBitmapOptions.inJustDecodeBounds = true;
        setDefaultRingtoneFileNames();
        this.mMediaProvider = this.mContext.getContentResolver().acquireContentProviderClient(MediaStore.AUTHORITY);
        if (sLastInternalScanFingerprint == null) {
            sLastInternalScanFingerprint = this.mContext.getSharedPreferences(SCANNED_BUILD_PREFS_NAME, 0).getString(LAST_INTERNAL_SCAN_FINGERPRINT, new String());
        }
        this.mAudioUri = MediaStore.Audio.Media.getContentUri(str);
        this.mVideoUri = MediaStore.Video.Media.getContentUri(str);
        this.mImagesUri = MediaStore.Images.Media.getContentUri(str);
        this.mFilesUri = MediaStore.Files.getContentUri(str);
        this.mFilesUriNoNotify = this.mFilesUri.buildUpon().appendQueryParameter("nonotify", WifiEnterpriseConfig.ENGINE_ENABLE).build();
        if (!str.equals("internal")) {
            this.mProcessPlaylists = true;
            this.mProcessGenres = true;
            this.mPlaylistsUri = MediaStore.Audio.Playlists.getContentUri(str);
        } else {
            this.mProcessPlaylists = false;
            this.mProcessGenres = false;
            this.mPlaylistsUri = null;
        }
        Locale locale = this.mContext.getResources().getConfiguration().locale;
        if (locale != null) {
            String language = locale.getLanguage();
            String country = locale.getCountry();
            if (language != null) {
                if (country != null) {
                    setLocale(language + Session.SESSION_SEPARATION_CHAR_CHILD + country);
                } else {
                    setLocale(language);
                }
            }
        }
        this.mCloseGuard.open("close");
    }

    private void setDefaultRingtoneFileNames() {
        this.mDefaultRingtoneFilename = SystemProperties.get("ro.config.ringtone");
        this.mDefaultNotificationFilename = SystemProperties.get("ro.config.notification_sound");
        this.mDefaultAlarmAlertFilename = SystemProperties.get("ro.config.alarm_alert");
    }

    private boolean isDrmEnabled() {
        String str = SystemProperties.get("drm.service.enabled");
        return str != null && str.equals("true");
    }

    protected class MyMediaScannerClient implements MediaScannerClient {
        private String mAlbum;
        private String mAlbumArtist;
        private String mArtist;
        private int mCompilation;
        private String mComposer;
        private long mDate;
        private int mDuration;
        private long mFileSize;
        private int mFileType;
        private String mGenre;
        private int mHeight;
        private boolean mIsDrm;
        private long mLastModified;
        private String mMimeType;
        private boolean mNoMedia;
        private String mPath;
        private boolean mScanSuccess;
        private String mTitle;
        private int mTrack;
        private int mWidth;
        private String mWriter;
        private int mYear;
        private MediaScannerClientEx mMediaScannerClientEx = MediaFactory.getInstance().getMediaScannerClientEx();
        private final SimpleDateFormat mDateFormatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss");

        public MyMediaScannerClient() {
            this.mDateFormatter.setTimeZone(TimeZone.getTimeZone(Time.TIMEZONE_UTC));
        }

        public FileEntry beginFile(String str, String str2, long j, long j2, boolean z, boolean z2) throws Throwable {
            MediaFile.MediaFileType fileType;
            this.mMimeType = str2;
            this.mFileType = 0;
            this.mFileSize = j2;
            this.mIsDrm = false;
            this.mScanSuccess = true;
            if (!z) {
                this.mNoMedia = (z2 || !MediaScanner.isNoMediaFile(str)) ? z2 : true;
                if (str2 != null) {
                    this.mFileType = MediaFile.getFileTypeForMimeType(str2);
                }
                this.mMediaScannerClientEx.correctFileType(str, str2, MediaScanner.this);
                if (this.mFileType == 0 && (fileType = MediaFile.getFileType(str)) != null) {
                    this.mFileType = fileType.fileType;
                    if (this.mMimeType == null || this.mMediaScannerClientEx.isValueslessMimeType(this.mMimeType)) {
                        this.mMimeType = fileType.mimeType;
                    }
                }
                if (MediaScanner.this.isDrmEnabled() && MediaFile.isDrmFileType(this.mFileType)) {
                    this.mFileType = getFileTypeFromDrm(str);
                }
                this.mMediaScannerClientEx.correctMetaData(str, MediaScanner.this);
            }
            FileEntry fileEntryMakeEntryFor = MediaScanner.this.makeEntryFor(str);
            long j3 = fileEntryMakeEntryFor != null ? j - fileEntryMakeEntryFor.mLastModified : 0L;
            boolean z3 = j3 > 1 || j3 < -1;
            if (fileEntryMakeEntryFor == null || z3) {
                if (z3) {
                    fileEntryMakeEntryFor.mLastModified = j;
                } else {
                    fileEntryMakeEntryFor = new FileEntry(0L, str, j, z ? 12289 : 0);
                }
                fileEntryMakeEntryFor.mLastModifiedChanged = true;
            }
            if (MediaScanner.this.mProcessPlaylists && MediaFile.isPlayListFileType(this.mFileType)) {
                MediaScanner.this.mPlayLists.add(fileEntryMakeEntryFor);
                MediaScanner.this.mPlaylistFilePathList.add(str);
                return null;
            }
            this.mArtist = null;
            this.mAlbumArtist = null;
            this.mAlbum = null;
            this.mTitle = null;
            this.mComposer = null;
            this.mGenre = null;
            this.mTrack = 0;
            this.mYear = 0;
            this.mDuration = 0;
            this.mPath = str;
            this.mDate = 0L;
            this.mLastModified = j;
            this.mWriter = null;
            this.mCompilation = 0;
            this.mWidth = 0;
            this.mHeight = 0;
            this.mMediaScannerClientEx.init();
            return fileEntryMakeEntryFor;
        }

        @Override
        public void scanFile(String str, long j, long j2, boolean z, boolean z2) throws Throwable {
            doScanFile(str, null, j, j2, z, false, z2);
        }

        public Uri doScanFile(String str, String str2, long j, long j2, boolean z, boolean z2, boolean z3) throws Throwable {
            boolean z4;
            String absolutePath;
            Uri uriEndFile;
            try {
                FileEntry fileEntryBeginFile = beginFile(str, str2, j, j2, z, z3);
                if (fileEntryBeginFile != null) {
                    if (MediaScanner.this.mMtpObjectHandle != 0) {
                        fileEntryBeginFile.mRowId = 0L;
                    }
                    if (fileEntryBeginFile.mPath != null) {
                        if ((MediaScanner.this.mDefaultNotificationSet || !doesPathHaveFilename(fileEntryBeginFile.mPath, MediaScanner.this.mDefaultNotificationFilename)) && ((MediaScanner.this.mDefaultRingtoneSet || !doesPathHaveFilename(fileEntryBeginFile.mPath, MediaScanner.this.mDefaultRingtoneFilename)) && (MediaScanner.this.mDefaultAlarmSet || !doesPathHaveFilename(fileEntryBeginFile.mPath, MediaScanner.this.mDefaultAlarmAlertFilename)))) {
                            if (MediaScanner.isSystemSoundWithMetadata(fileEntryBeginFile.mPath) && !Build.FINGERPRINT.equals(MediaScanner.sLastInternalScanFingerprint)) {
                                Log.i(MediaScanner.TAG, "forcing rescan of " + fileEntryBeginFile.mPath + " since build fingerprint changed");
                            }
                            z4 = z2;
                        } else {
                            Log.w(MediaScanner.TAG, "forcing rescan of " + fileEntryBeginFile.mPath + "since ringtone setting didn't finish");
                        }
                        z4 = true;
                    } else {
                        z4 = z2;
                    }
                    if (fileEntryBeginFile == null) {
                        return null;
                    }
                    if (!fileEntryBeginFile.mLastModifiedChanged && !z4) {
                        return null;
                    }
                    if (z3) {
                        uriEndFile = endFile(fileEntryBeginFile, false, false, false, false, false);
                    } else {
                        boolean zIsAudioFileType = MediaFile.isAudioFileType(this.mFileType);
                        boolean zIsVideoFileType = MediaFile.isVideoFileType(this.mFileType);
                        boolean zIsImageFileType = MediaFile.isImageFileType(this.mFileType);
                        if (zIsAudioFileType || zIsVideoFileType || zIsImageFileType) {
                            absolutePath = Environment.maybeTranslateEmulatedPathToInternal(new File(str)).getAbsolutePath();
                        } else {
                            absolutePath = str;
                        }
                        if (zIsAudioFileType || zIsVideoFileType) {
                            this.mScanSuccess = MediaScanner.this.processFile(absolutePath, str2, this);
                        }
                        if (zIsImageFileType) {
                            this.mScanSuccess = processImageFile(absolutePath);
                        }
                        String lowerCase = absolutePath.toLowerCase(Locale.ROOT);
                        boolean z5 = this.mScanSuccess && lowerCase.indexOf(MediaScanner.RINGTONES_DIR) > 0;
                        boolean z6 = this.mScanSuccess && lowerCase.indexOf(MediaScanner.NOTIFICATIONS_DIR) > 0;
                        boolean z7 = this.mScanSuccess && lowerCase.indexOf(MediaScanner.ALARMS_DIR) > 0;
                        boolean z8 = this.mScanSuccess && lowerCase.indexOf(MediaScanner.PODCAST_DIR) > 0;
                        uriEndFile = endFile(fileEntryBeginFile, z5, z6, z7, this.mScanSuccess && (lowerCase.indexOf(MediaScanner.MUSIC_DIR) > 0 || !(z5 || z6 || z7 || z8)), z8);
                    }
                    return uriEndFile;
                }
                return null;
            } catch (RemoteException e) {
                Log.e(MediaScanner.TAG, "RemoteException in MediaScanner.scanFile()", e);
                return null;
            }
        }

        private long parseDate(String str) {
            try {
                return this.mDateFormatter.parse(str).getTime();
            } catch (ParseException e) {
                return 0L;
            }
        }

        private int parseSubstring(String str, int i, int i2) {
            int length = str.length();
            if (i == length) {
                return i2;
            }
            int i3 = i + 1;
            char cCharAt = str.charAt(i);
            if (cCharAt < '0' || cCharAt > '9') {
                return i2;
            }
            int i4 = cCharAt - '0';
            while (i3 < length) {
                int i5 = i3 + 1;
                char cCharAt2 = str.charAt(i3);
                if (cCharAt2 < '0' || cCharAt2 > '9') {
                    return i4;
                }
                i4 = (i4 * 10) + (cCharAt2 - '0');
                i3 = i5;
            }
            return i4;
        }

        @Override
        public void handleStringTag(String str, String str2) {
            if (str.equalsIgnoreCase("title") || str.startsWith("title;")) {
                this.mTitle = str2;
                return;
            }
            if (str.equalsIgnoreCase("artist") || str.startsWith("artist;")) {
                this.mArtist = str2.trim();
                return;
            }
            if (str.equalsIgnoreCase("albumartist") || str.startsWith("albumartist;") || str.equalsIgnoreCase("band") || str.startsWith("band;")) {
                this.mAlbumArtist = str2.trim();
                return;
            }
            if (str.equalsIgnoreCase("album") || str.startsWith("album;")) {
                this.mAlbum = str2.trim();
                return;
            }
            if (!str.equalsIgnoreCase(MediaStore.Audio.AudioColumns.COMPOSER) && !str.startsWith("composer;")) {
                if (MediaScanner.this.mProcessGenres && (str.equalsIgnoreCase(MediaStore.Audio.AudioColumns.GENRE) || str.startsWith("genre;"))) {
                    this.mGenre = getGenreName(str2);
                    return;
                }
                if (str.equalsIgnoreCase(MediaStore.Audio.AudioColumns.YEAR) || str.startsWith("year;")) {
                    this.mYear = parseSubstring(str2, 0, 0);
                    return;
                }
                if (str.equalsIgnoreCase("tracknumber") || str.startsWith("tracknumber;")) {
                    this.mTrack = ((this.mTrack / 1000) * 1000) + parseSubstring(str2, 0, 0);
                    return;
                }
                if (str.equalsIgnoreCase("discnumber") || str.equals("set") || str.startsWith("set;")) {
                    this.mTrack = (parseSubstring(str2, 0, 0) * 1000) + (this.mTrack % 1000);
                    return;
                }
                if (str.equalsIgnoreCase("duration")) {
                    this.mDuration = parseSubstring(str2, 0, 0);
                    return;
                }
                if (str.equalsIgnoreCase("writer") || str.startsWith("writer;")) {
                    this.mWriter = str2.trim();
                    return;
                }
                if (str.equalsIgnoreCase(MediaStore.Audio.AudioColumns.COMPILATION)) {
                    this.mCompilation = parseSubstring(str2, 0, 0);
                    return;
                }
                if (!str.equalsIgnoreCase("isdrm")) {
                    if (str.equalsIgnoreCase("date")) {
                        this.mDate = parseDate(str2);
                        return;
                    }
                    if (str.equalsIgnoreCase("width")) {
                        this.mWidth = parseSubstring(str2, 0, 0);
                        return;
                    } else if (str.equalsIgnoreCase("height")) {
                        this.mHeight = parseSubstring(str2, 0, 0);
                        return;
                    } else {
                        this.mMediaScannerClientEx.parseExMetaDataFromStringTag(str, str2, MediaScanner.this);
                        return;
                    }
                }
                this.mIsDrm = parseSubstring(str2, 0, 0) == 1;
                return;
            }
            this.mComposer = str2.trim();
        }

        private boolean convertGenreCode(String str, String str2) {
            String genreName = getGenreName(str);
            if (genreName.equals(str2)) {
                return true;
            }
            Log.d(MediaScanner.TAG, "'" + str + "' -> '" + genreName + "', expected '" + str2 + "'");
            return false;
        }

        private void testGenreNameConverter() {
            convertGenreCode("2", "Country");
            convertGenreCode("(2)", "Country");
            convertGenreCode("(2", "(2");
            convertGenreCode("2 Foo", "Country");
            convertGenreCode("(2) Foo", "Country");
            convertGenreCode("(2 Foo", "(2 Foo");
            convertGenreCode("2Foo", "2Foo");
            convertGenreCode("(2)Foo", "Country");
            convertGenreCode("200 Foo", "Foo");
            convertGenreCode("(200) Foo", "Foo");
            convertGenreCode("200Foo", "200Foo");
            convertGenreCode("(200)Foo", "Foo");
            convertGenreCode("200)Foo", "200)Foo");
            convertGenreCode("200) Foo", "200) Foo");
        }

        public String getGenreName(String str) {
            int i;
            if (str == null) {
                return null;
            }
            int length = str.length();
            if (length > 0) {
                StringBuffer stringBuffer = new StringBuffer();
                int i2 = 0;
                boolean z = false;
                while (i2 < length) {
                    char cCharAt = str.charAt(i2);
                    if (i2 == 0 && cCharAt == '(') {
                        z = true;
                    } else {
                        if (!Character.isDigit(cCharAt)) {
                            break;
                        }
                        stringBuffer.append(cCharAt);
                    }
                    i2++;
                }
                char cCharAt2 = i2 < length ? str.charAt(i2) : ' ';
                if ((z && cCharAt2 == ')') || (!z && Character.isWhitespace(cCharAt2))) {
                    try {
                        short s = Short.parseShort(stringBuffer.toString());
                        if (s >= 0) {
                            if (s < MediaScanner.ID3_GENRES.length && MediaScanner.ID3_GENRES[s] != null) {
                                return MediaScanner.ID3_GENRES[s];
                            }
                            if (s == 255) {
                                return null;
                            }
                            if (s < 255 && (i = i2 + 1) < length) {
                                if (!z || cCharAt2 != ')') {
                                    i = i2;
                                }
                                String strTrim = str.substring(i).trim();
                                if (strTrim.length() != 0) {
                                    return strTrim;
                                }
                            } else {
                                return stringBuffer.toString();
                            }
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }
            return str;
        }

        private boolean processImageFile(String str) {
            try {
                MediaScanner.this.mBitmapOptions.outWidth = 0;
                MediaScanner.this.mBitmapOptions.outHeight = 0;
                BitmapFactory.decodeFile(str, MediaScanner.this.mBitmapOptions);
                this.mWidth = MediaScanner.this.mBitmapOptions.outWidth;
                this.mHeight = MediaScanner.this.mBitmapOptions.outHeight;
                if (this.mWidth > 0) {
                    return this.mHeight > 0;
                }
                return false;
            } catch (Throwable th) {
                return false;
            }
        }

        @Override
        public void setMimeType(String str) {
            if ("audio/mp4".equals(this.mMimeType) && str.startsWith("video")) {
                return;
            }
            this.mMimeType = str;
            this.mFileType = MediaFile.getFileTypeForMimeType(str);
        }

        private ContentValues toValues() {
            String str;
            ContentValues contentValues = new ContentValues();
            contentValues.put("_data", this.mPath);
            contentValues.put("title", this.mTitle);
            contentValues.put("date_modified", Long.valueOf(this.mLastModified));
            contentValues.put("_size", Long.valueOf(this.mFileSize));
            contentValues.put("mime_type", this.mMimeType);
            contentValues.put(MediaStore.MediaColumns.IS_DRM, Boolean.valueOf(this.mIsDrm));
            String str2 = null;
            if (this.mWidth > 0 && this.mHeight > 0) {
                contentValues.put("width", Integer.valueOf(this.mWidth));
                contentValues.put("height", Integer.valueOf(this.mHeight));
                str = this.mWidth + "x" + this.mHeight;
            } else {
                str = null;
            }
            if (!this.mNoMedia) {
                if (MediaFile.isVideoFileType(this.mFileType)) {
                    contentValues.put("artist", (this.mArtist == null || this.mArtist.length() <= 0) ? MediaStore.UNKNOWN_STRING : this.mArtist);
                    contentValues.put("album", (this.mAlbum == null || this.mAlbum.length() <= 0) ? MediaStore.UNKNOWN_STRING : this.mAlbum);
                    contentValues.put("duration", Integer.valueOf(this.mDuration));
                    if (str != null) {
                        contentValues.put(MediaStore.Video.VideoColumns.RESOLUTION, str);
                    }
                    if (this.mDate > 0) {
                        contentValues.put("datetaken", Long.valueOf(this.mDate));
                    }
                } else if (!MediaFile.isImageFileType(this.mFileType) && this.mScanSuccess && MediaFile.isAudioFileType(this.mFileType)) {
                    contentValues.put("artist", (this.mArtist == null || this.mArtist.length() <= 0) ? MediaStore.UNKNOWN_STRING : this.mArtist);
                    if (this.mAlbumArtist != null && this.mAlbumArtist.length() > 0) {
                        str2 = this.mAlbumArtist;
                    }
                    contentValues.put(MediaStore.Audio.AudioColumns.ALBUM_ARTIST, str2);
                    contentValues.put("album", (this.mAlbum == null || this.mAlbum.length() <= 0) ? MediaStore.UNKNOWN_STRING : this.mAlbum);
                    contentValues.put(MediaStore.Audio.AudioColumns.COMPOSER, this.mComposer);
                    contentValues.put(MediaStore.Audio.AudioColumns.GENRE, this.mGenre);
                    if (this.mYear != 0) {
                        contentValues.put(MediaStore.Audio.AudioColumns.YEAR, Integer.valueOf(this.mYear));
                    }
                    contentValues.put(MediaStore.Audio.AudioColumns.TRACK, Integer.valueOf(this.mTrack));
                    contentValues.put("duration", Integer.valueOf(this.mDuration));
                    contentValues.put(MediaStore.Audio.AudioColumns.COMPILATION, Integer.valueOf(this.mCompilation));
                }
                if (!this.mScanSuccess) {
                    contentValues.put("media_type", (Integer) 0);
                }
            }
            this.mMediaScannerClientEx.addExMetaDataToContentValues(contentValues, MediaScanner.this);
            return contentValues;
        }

        private Uri endFile(FileEntry fileEntry, boolean z, boolean z2, boolean z3, boolean z4, boolean z5) throws RemoteException {
            ExifInterface exifInterface;
            long j;
            int attributeInt;
            boolean z6;
            long j2;
            Uri uriWithAppendedId;
            String asString;
            int iLastIndexOf;
            int i;
            if (this.mArtist == null || this.mArtist.length() == 0) {
                this.mArtist = this.mAlbumArtist;
            }
            ContentValues values = toValues();
            String asString2 = values.getAsString("title");
            if (asString2 == null || TextUtils.isEmpty(asString2.trim())) {
                values.put("title", MediaFile.getFileTitle(values.getAsString("_data")));
            }
            if (MediaStore.UNKNOWN_STRING.equals(values.getAsString("album")) && (iLastIndexOf = (asString = values.getAsString("_data")).lastIndexOf(47)) >= 0) {
                int i2 = 0;
                while (true) {
                    i = i2 + 1;
                    int iIndexOf = asString.indexOf(47, i);
                    if (iIndexOf < 0 || iIndexOf >= iLastIndexOf) {
                        break;
                    }
                    i2 = iIndexOf;
                }
                if (i2 != 0) {
                    values.put("album", asString.substring(i, iLastIndexOf));
                }
            }
            long j3 = fileEntry.mRowId;
            if (!MediaFile.isAudioFileType(this.mFileType) || (j3 != 0 && MediaScanner.this.mMtpObjectHandle == 0)) {
                if ((this.mFileType == 401 || this.mFileType == 407 || MediaFile.isRawImageFileType(this.mFileType)) && !this.mNoMedia) {
                    try {
                        exifInterface = new ExifInterface(fileEntry.mPath);
                    } catch (IOException e) {
                        exifInterface = null;
                    }
                    if (exifInterface != null) {
                        float[] fArr = new float[2];
                        if (exifInterface.getLatLong(fArr)) {
                            values.put("latitude", Float.valueOf(fArr[0]));
                            values.put("longitude", Float.valueOf(fArr[1]));
                        }
                        long gpsDateTime = exifInterface.getGpsDateTime();
                        if (gpsDateTime != -1) {
                            values.put("datetaken", Long.valueOf(gpsDateTime));
                        } else {
                            long dateTime = exifInterface.getDateTime();
                            if (dateTime != -1) {
                                j = j3;
                                if (Math.abs((this.mLastModified * 1000) - dateTime) >= 86400000) {
                                    values.put("datetaken", Long.valueOf(dateTime));
                                }
                            }
                            attributeInt = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                            if (attributeInt != -1) {
                                values.put(MediaStore.Images.ImageColumns.ORIENTATION, Integer.valueOf(attributeInt != 3 ? attributeInt != 6 ? attributeInt != 8 ? 0 : 270 : 90 : 180));
                            }
                            this.mMediaScannerClientEx.putExtensionContentValuesForImage(values, fileEntry.mPath);
                        }
                        j = j3;
                        attributeInt = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                        if (attributeInt != -1) {
                        }
                        this.mMediaScannerClientEx.putExtensionContentValuesForImage(values, fileEntry.mPath);
                    }
                }
                Uri uri = MediaScanner.this.mFilesUri;
                MediaInserter mediaInserter = MediaScanner.this.mMediaInserter;
                if (this.mScanSuccess && !this.mNoMedia) {
                    if (!MediaFile.isVideoFileType(this.mFileType)) {
                        uri = MediaScanner.this.mVideoUri;
                    } else if (MediaFile.isImageFileType(this.mFileType)) {
                        uri = MediaScanner.this.mImagesUri;
                    } else if (MediaFile.isAudioFileType(this.mFileType)) {
                        uri = MediaScanner.this.mAudioUri;
                    }
                }
                z6 = z2 || ((!MediaScanner.this.mWasEmptyPriorToScan || MediaScanner.this.mDefaultNotificationSet) && !this.mMediaScannerClientEx.doesSettingEmpty("notification_set", MediaScanner.this.mContext)) ? !(!z || ((!MediaScanner.this.mWasEmptyPriorToScan || MediaScanner.this.mDefaultRingtoneSet) && !this.mMediaScannerClientEx.doesSettingEmpty("ringtone_set", MediaScanner.this.mContext)) ? !z3 || (((!MediaScanner.this.mWasEmptyPriorToScan || MediaScanner.this.mDefaultAlarmSet) && !this.mMediaScannerClientEx.doesSettingEmpty("alarm_set", MediaScanner.this.mContext)) || !(TextUtils.isEmpty(MediaScanner.this.mDefaultAlarmAlertFilename) || doesPathHaveFilename(fileEntry.mPath, MediaScanner.this.mDefaultAlarmAlertFilename))) : !(TextUtils.isEmpty(MediaScanner.this.mDefaultRingtoneFilename) || doesPathHaveFilename(fileEntry.mPath, MediaScanner.this.mDefaultRingtoneFilename))) : TextUtils.isEmpty(MediaScanner.this.mDefaultNotificationFilename) || doesPathHaveFilename(fileEntry.mPath, MediaScanner.this.mDefaultNotificationFilename);
                if (j != 0) {
                    if (MediaScanner.this.mMtpObjectHandle != 0) {
                        values.put(MediaStore.MediaColumns.MEDIA_SCANNER_NEW_OBJECT_ID, Integer.valueOf(MediaScanner.this.mMtpObjectHandle));
                    }
                    if (uri == MediaScanner.this.mFilesUri) {
                        int formatCode = fileEntry.mFormat;
                        if (formatCode == 0) {
                            formatCode = MediaFile.getFormatCode(fileEntry.mPath, this.mMimeType);
                        }
                        values.put("format", Integer.valueOf(formatCode));
                    }
                    if (mediaInserter == null || z6) {
                        if (mediaInserter != null) {
                            mediaInserter.flushAll();
                        }
                        uriWithAppendedId = MediaScanner.this.mMediaProvider.insert(uri, values);
                    } else {
                        if (fileEntry.mFormat == 12289) {
                            mediaInserter.insertwithPriority(uri, values);
                        } else {
                            mediaInserter.insert(uri, values);
                        }
                        uriWithAppendedId = null;
                    }
                    if (uriWithAppendedId != null) {
                        long id = ContentUris.parseId(uriWithAppendedId);
                        fileEntry.mRowId = id;
                        j2 = id;
                    } else {
                        j2 = j;
                    }
                } else {
                    j2 = j;
                    uriWithAppendedId = ContentUris.withAppendedId(uri, j2);
                    values.remove("_data");
                    if (this.mScanSuccess && !MediaScanner.isNoMediaPath(fileEntry.mPath)) {
                        int fileTypeForMimeType = MediaFile.getFileTypeForMimeType(this.mMimeType);
                        values.put("media_type", Integer.valueOf(MediaFile.isAudioFileType(fileTypeForMimeType) ? 2 : MediaFile.isVideoFileType(fileTypeForMimeType) ? 3 : MediaFile.isImageFileType(fileTypeForMimeType) ? 1 : MediaFile.isPlayListFileType(fileTypeForMimeType) ? 4 : 0));
                    }
                    MediaScanner.this.mMediaProvider.update(uriWithAppendedId, values, null, null);
                }
                if (z6) {
                    if (z2 && this.mMediaScannerClientEx.doesSettingEmpty("notification_set", MediaScanner.this.mContext)) {
                        setRingtoneIfNotSet(Settings.System.NOTIFICATION_SOUND, uri, j2);
                        MediaScanner.this.mDefaultNotificationSet = true;
                        this.mMediaScannerClientEx.setSettingFlag("notification_set", MediaScanner.this.mContext);
                    } else if (z && this.mMediaScannerClientEx.doesSettingEmpty("ringtone_set", MediaScanner.this.mContext)) {
                        setRingtoneIfNotSet(Settings.System.RINGTONE, uri, j2);
                        MediaScanner.this.mDefaultRingtoneSet = true;
                        this.mMediaScannerClientEx.setSettingFlag("ringtone_set", MediaScanner.this.mContext);
                    } else if (z3 && this.mMediaScannerClientEx.doesSettingEmpty("alarm_set", MediaScanner.this.mContext)) {
                        setRingtoneIfNotSet(Settings.System.ALARM_ALERT, uri, j2);
                        MediaScanner.this.mDefaultAlarmSet = true;
                        this.mMediaScannerClientEx.setSettingFlag("alarm_set", MediaScanner.this.mContext);
                    }
                }
                return uriWithAppendedId;
            }
            values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, Boolean.valueOf(z));
            values.put(MediaStore.Audio.AudioColumns.IS_NOTIFICATION, Boolean.valueOf(z2));
            values.put(MediaStore.Audio.AudioColumns.IS_ALARM, Boolean.valueOf(z3));
            values.put(MediaStore.Audio.AudioColumns.IS_MUSIC, Boolean.valueOf(z4));
            values.put(MediaStore.Audio.AudioColumns.IS_PODCAST, Boolean.valueOf(z5));
            j = j3;
            Uri uri2 = MediaScanner.this.mFilesUri;
            MediaInserter mediaInserter2 = MediaScanner.this.mMediaInserter;
            if (this.mScanSuccess) {
                if (!MediaFile.isVideoFileType(this.mFileType)) {
                }
            }
            if (z2) {
            }
            if (j != 0) {
            }
            if (z6) {
            }
            return uriWithAppendedId;
        }

        private boolean doesPathHaveFilename(String str, String str2) {
            int iLastIndexOf = str.lastIndexOf(File.separatorChar) + 1;
            int length = str2.length();
            return str.regionMatches(iLastIndexOf, str2, 0, length) && iLastIndexOf + length == str.length();
        }

        private void setRingtoneIfNotSet(String str, Uri uri, long j) {
            if (!MediaScanner.this.wasRingtoneAlreadySet(str)) {
                ContentResolver contentResolver = MediaScanner.this.mContext.getContentResolver();
                if (TextUtils.isEmpty(Settings.System.getString(contentResolver, str))) {
                    Uri uriFor = Settings.System.getUriFor(str);
                    RingtoneManager.setActualDefaultRingtoneUri(MediaScanner.this.mContext, RingtoneManager.getDefaultType(uriFor), ContentUris.withAppendedId(uri, j));
                }
                Settings.System.putInt(contentResolver, MediaScanner.this.settingSetIndicatorName(str), 1);
            }
        }

        private int getFileTypeFromDrm(String str) {
            if (!MediaScanner.this.isDrmEnabled()) {
                return 0;
            }
            if (MediaScanner.this.mDrmManagerClient == null) {
                MediaScanner.this.mDrmManagerClient = new DrmManagerClient(MediaScanner.this.mContext);
            }
            if (!MediaScanner.this.mDrmManagerClient.canHandle(str, (String) null)) {
                return 0;
            }
            this.mIsDrm = true;
            String originalMimeType = MediaScanner.this.mDrmManagerClient.getOriginalMimeType(str);
            if (originalMimeType == null) {
                return 0;
            }
            this.mMimeType = originalMimeType;
            return MediaFile.getFileTypeForMimeType(originalMimeType);
        }
    }

    private static boolean isSystemSoundWithMetadata(String str) {
        if (str.startsWith("/system/media/audio/alarms/") || str.startsWith("/system/media/audio/ringtones/") || str.startsWith("/system/media/audio/notifications/") || str.startsWith("/product/media/audio/alarms/") || str.startsWith("/product/media/audio/ringtones/") || str.startsWith("/product/media/audio/notifications/")) {
            return true;
        }
        return false;
    }

    private String settingSetIndicatorName(String str) {
        return str + "_set";
    }

    private boolean wasRingtoneAlreadySet(String str) {
        try {
            return Settings.System.getInt(this.mContext.getContentResolver(), settingSetIndicatorName(str)) != 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    protected void prescan(String str, boolean z) throws Throwable {
        String str2;
        String[] strArr;
        Cursor cursorQuery;
        Cursor cursor;
        boolean zAccess;
        int i;
        this.mPlayLists.clear();
        if (str != null) {
            str2 = "_id>? AND _data=?";
            strArr = new String[]{"", str};
        } else {
            str2 = "_id>?";
            strArr = new String[]{""};
        }
        this.mDefaultRingtoneSet = wasRingtoneAlreadySet(Settings.System.RINGTONE);
        this.mDefaultNotificationSet = wasRingtoneAlreadySet(Settings.System.NOTIFICATION_SOUND);
        this.mDefaultAlarmSet = wasRingtoneAlreadySet(Settings.System.ALARM_ALERT);
        Uri.Builder builderBuildUpon = this.mFilesUri.buildUpon();
        builderBuildUpon.appendQueryParameter(MediaStore.PARAM_DELETE_DATA, "false");
        MediaBulkDeleter mediaBulkDeleter = new MediaBulkDeleter(this.mMediaProvider, builderBuildUpon.build());
        Cursor cursor2 = null;
        if (z) {
            try {
                Uri uriBuild = this.mFilesUri.buildUpon().appendQueryParameter("limit", "1000").build();
                this.mWasEmptyPriorToScan = true;
                long j = Long.MIN_VALUE;
                cursorQuery = null;
                while (true) {
                    try {
                        strArr[0] = "" + j;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                            cursor = null;
                        } else {
                            cursor = cursorQuery;
                        }
                    } catch (Throwable th) {
                        th = th;
                    }
                    try {
                        long j2 = j;
                        cursorQuery = this.mMediaProvider.query(uriBuild, FILES_PRESCAN_PROJECTION, str2, strArr, "_id", null);
                        if (cursorQuery == null || cursorQuery.getCount() == 0) {
                            break;
                        }
                        this.mWasEmptyPriorToScan = false;
                        j = j2;
                        while (cursorQuery.moveToNext()) {
                            j = cursorQuery.getLong(0);
                            String string = cursorQuery.getString(1);
                            int i2 = cursorQuery.getInt(2);
                            cursorQuery.getLong(3);
                            if (string != null && string.startsWith("/")) {
                                try {
                                    zAccess = Os.access(string, OsConstants.F_OK);
                                } catch (ErrnoException e) {
                                    zAccess = false;
                                }
                                if (!zAccess && !MtpConstants.isAbstractObject(i2)) {
                                    MediaFile.MediaFileType fileType = MediaFile.getFileType(string);
                                    if (fileType != null) {
                                        i = fileType.fileType;
                                    } else {
                                        i = 0;
                                    }
                                    if (!MediaFile.isPlayListFileType(i)) {
                                        mediaBulkDeleter.delete(j);
                                        if (string.toLowerCase(Locale.US).endsWith("/.nomedia")) {
                                            mediaBulkDeleter.flush();
                                            this.mMediaProvider.call(MediaStore.UNHIDE_CALL, new File(string).getParent(), null);
                                        }
                                    }
                                }
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        cursorQuery = cursor;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        mediaBulkDeleter.flush();
                        throw th;
                    }
                }
                cursor2 = cursorQuery;
            } catch (Throwable th3) {
                th = th3;
                cursorQuery = null;
            }
        }
        if (cursor2 != null) {
            cursor2.close();
        }
        mediaBulkDeleter.flush();
        this.mOriginalCount = 0;
        Cursor cursorQuery2 = this.mMediaProvider.query(this.mImagesUri, ID_PROJECTION, null, null, null, null);
        if (cursorQuery2 != null) {
            this.mOriginalCount = cursorQuery2.getCount();
            cursorQuery2.close();
        }
    }

    static class MediaBulkDeleter {
        final Uri mBaseUri;
        final ContentProviderClient mProvider;
        StringBuilder whereClause = new StringBuilder();
        ArrayList<String> whereArgs = new ArrayList<>(100);

        public MediaBulkDeleter(ContentProviderClient contentProviderClient, Uri uri) {
            this.mProvider = contentProviderClient;
            this.mBaseUri = uri;
        }

        public void delete(long j) throws RemoteException {
            if (this.whereClause.length() != 0) {
                this.whereClause.append(",");
            }
            this.whereClause.append("?");
            this.whereArgs.add("" + j);
            if (this.whereArgs.size() > 100) {
                flush();
            }
        }

        public void flush() throws RemoteException {
            int size = this.whereArgs.size();
            if (size > 0) {
                String[] strArr = (String[]) this.whereArgs.toArray(new String[size]);
                this.mProvider.delete(this.mBaseUri, "_id IN (" + this.whereClause.toString() + ")", strArr);
                this.whereClause.setLength(0);
                this.whereArgs.clear();
            }
        }
    }

    private void postscan(String[] strArr) throws Throwable {
        if (this.mProcessPlaylists) {
            processPlayLists();
        }
        this.mPlayLists.clear();
    }

    private void releaseResources() {
        if (this.mDrmManagerClient != null) {
            this.mDrmManagerClient.close();
            this.mDrmManagerClient = null;
        }
    }

    public void scanDirectories(String[] strArr) {
        try {
            try {
                try {
                    System.currentTimeMillis();
                    prescan(null, true);
                    System.currentTimeMillis();
                    this.mMediaInserter = new MediaInserter(this.mMediaProvider, 500);
                    for (String str : strArr) {
                        processDirectory(str, this.mClient);
                    }
                    this.mMediaInserter.flushAll();
                    this.mMediaInserter = null;
                    System.currentTimeMillis();
                    postscan(strArr);
                    System.currentTimeMillis();
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in MediaScanner.scan()", e);
                }
            } catch (SQLException e2) {
                Log.e(TAG, "SQLException in MediaScanner.scan()", e2);
            } catch (UnsupportedOperationException e3) {
                Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e3);
            }
        } finally {
            releaseResources();
        }
    }

    public Uri scanSingleFile(String str, String str2) {
        try {
            prescan(str, true);
            File file = new File(str);
            if (file.exists() && file.canRead()) {
                return this.mClient.doScanFile(str, str2, file.lastModified() / 1000, file.length(), file.isDirectory(), true, isNoMediaPath(str));
            }
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
            return null;
        } finally {
            releaseResources();
        }
    }

    private static boolean isNoMediaFile(String str) {
        int iLastIndexOf;
        if (!new File(str).isDirectory() && (iLastIndexOf = str.lastIndexOf(47)) >= 0 && iLastIndexOf + 2 < str.length()) {
            int i = iLastIndexOf + 1;
            if (str.regionMatches(i, "._", 0, 2)) {
                return true;
            }
            if (str.regionMatches(true, str.length() - 4, ".jpg", 0, 4)) {
                if (str.regionMatches(true, i, "AlbumArt_{", 0, 10) || str.regionMatches(true, i, "AlbumArt.", 0, 9)) {
                    return true;
                }
                int length = (str.length() - iLastIndexOf) - 1;
                if ((length == 17 && str.regionMatches(true, i, "AlbumArtSmall", 0, 13)) || (length == 10 && str.regionMatches(true, i, "Folder", 0, 6))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void clearMediaPathCache(boolean z, boolean z2) {
        synchronized (MediaScanner.class) {
            if (z) {
                try {
                    mMediaPaths.clear();
                } catch (Throwable th) {
                    throw th;
                }
            }
            if (z2) {
                mNoMediaPaths.clear();
            }
        }
    }

    public static boolean isNoMediaPath(String str) {
        if (str == null) {
            return false;
        }
        if (str.indexOf("/.") >= 0) {
            return true;
        }
        int iLastIndexOf = str.lastIndexOf(47);
        if (iLastIndexOf <= 0) {
            return false;
        }
        String strSubstring = str.substring(0, iLastIndexOf);
        synchronized (MediaScanner.class) {
            if (mNoMediaPaths.containsKey(strSubstring)) {
                return true;
            }
            if (!mMediaPaths.containsKey(strSubstring)) {
                int i = 1;
                while (i >= 0) {
                    int iIndexOf = str.indexOf(47, i);
                    if (iIndexOf > i) {
                        iIndexOf++;
                        if (new File(str.substring(0, iIndexOf) + MediaStore.MEDIA_IGNORE_FILENAME).exists()) {
                            mNoMediaPaths.put(strSubstring, "");
                            return true;
                        }
                    }
                    i = iIndexOf;
                }
                mMediaPaths.put(strSubstring, "");
            }
            return isNoMediaFile(str);
        }
    }

    public void scanMtpFile(String str, int i, int i2) throws Throwable {
        MediaFile.MediaFileType fileType = MediaFile.getFileType(str);
        int i3 = fileType == null ? 0 : fileType.fileType;
        File file = new File(str);
        long jLastModified = file.lastModified() / 1000;
        if (!MediaFile.isAudioFileType(i3) && !MediaFile.isVideoFileType(i3) && !MediaFile.isImageFileType(i3) && !MediaFile.isPlayListFileType(i3) && !MediaFile.isDrmFileType(i3)) {
            ContentValues contentValues = new ContentValues();
            contentValues.put("_size", Long.valueOf(file.length()));
            contentValues.put("date_modified", Long.valueOf(jLastModified));
            try {
                this.mMediaProvider.update(MediaStore.Files.getMtpObjectsUri(this.mVolumeName), contentValues, "_id=?", new String[]{Integer.toString(i)});
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in scanMtpFile", e);
                return;
            }
        }
        this.mMtpObjectHandle = i;
        Cursor cursor = null;
        try {
            try {
                if (MediaFile.isPlayListFileType(i3)) {
                    prescan(null, true);
                    FileEntry fileEntryMakeEntryFor = makeEntryFor(str);
                    if (fileEntryMakeEntryFor != null) {
                        Cursor cursorQuery = this.mMediaProvider.query(this.mFilesUri, FILES_PRESCAN_PROJECTION, null, null, null, null);
                        try {
                            processPlayList(fileEntryMakeEntryFor, cursorQuery);
                            cursor = cursorQuery;
                        } catch (RemoteException e2) {
                            e = e2;
                            cursor = cursorQuery;
                            Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
                            this.mMtpObjectHandle = 0;
                            if (cursor != null) {
                            }
                            releaseResources();
                        } catch (Throwable th) {
                            th = th;
                            cursor = cursorQuery;
                            this.mMtpObjectHandle = 0;
                            if (cursor != null) {
                                cursor.close();
                            }
                            releaseResources();
                            throw th;
                        }
                    }
                } else {
                    prescan(str, false);
                    this.mClient.doScanFile(str, fileType.mimeType, jLastModified, file.length(), i2 == 12289, true, isNoMediaPath(str));
                }
                this.mMtpObjectHandle = 0;
            } catch (RemoteException e3) {
                e = e3;
            }
            if (cursor != null) {
                cursor.close();
            }
            releaseResources();
        } catch (Throwable th2) {
            th = th2;
        }
    }

    protected FileEntry makeEntryFor(String str) throws Throwable {
        Cursor cursorQuery;
        Cursor cursor = null;
        try {
            cursorQuery = this.mMediaProvider.query(this.mFilesUriNoNotify, FILES_PRESCAN_PROJECTION, "_data=?", new String[]{str}, null, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.moveToFirst()) {
                        FileEntry fileEntry = new FileEntry(cursorQuery.getLong(0), str, cursorQuery.getLong(3), cursorQuery.getInt(2));
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        return fileEntry;
                    }
                } catch (RemoteException e) {
                    if (cursorQuery != null) {
                    }
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
        } catch (RemoteException e2) {
            cursorQuery = null;
        } catch (Throwable th2) {
            th = th2;
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
        return null;
    }

    private int matchPaths(String str, String str2) {
        int i;
        int i2;
        int length = str.length();
        int length2 = str2.length();
        int i3 = 0;
        while (length > 0 && length2 > 0) {
            int i4 = length - 1;
            int iLastIndexOf = str.lastIndexOf(47, i4);
            int i5 = length2 - 1;
            int iLastIndexOf2 = str2.lastIndexOf(47, i5);
            int iLastIndexOf3 = str.lastIndexOf(92, i4);
            int iLastIndexOf4 = str2.lastIndexOf(92, i5);
            if (iLastIndexOf > iLastIndexOf3) {
                iLastIndexOf3 = iLastIndexOf;
            }
            if (iLastIndexOf2 <= iLastIndexOf4) {
                iLastIndexOf2 = iLastIndexOf4;
            }
            if (iLastIndexOf3 >= 0) {
                i = iLastIndexOf3 + 1;
            } else {
                i = 0;
            }
            if (iLastIndexOf2 >= 0) {
                i2 = iLastIndexOf2 + 1;
            } else {
                i2 = 0;
            }
            int i6 = length - i;
            if (length2 - i2 != i6 || !str.regionMatches(true, i, str2, i2, i6)) {
                break;
            }
            i3++;
            length = i - 1;
            length2 = i2 - 1;
        }
        return i3;
    }

    private boolean matchEntries(long j, String str) {
        int size = this.mPlaylistEntries.size();
        boolean z = true;
        for (int i = 0; i < size; i++) {
            PlaylistEntry playlistEntry = this.mPlaylistEntries.get(i);
            if (playlistEntry.bestmatchlevel != Integer.MAX_VALUE) {
                if (str.equalsIgnoreCase(playlistEntry.path)) {
                    playlistEntry.bestmatchid = j;
                    playlistEntry.bestmatchlevel = Integer.MAX_VALUE;
                } else {
                    int iMatchPaths = matchPaths(str, playlistEntry.path);
                    if (iMatchPaths > playlistEntry.bestmatchlevel) {
                        playlistEntry.bestmatchid = j;
                        playlistEntry.bestmatchlevel = iMatchPaths;
                    }
                }
                z = false;
            }
        }
        return z;
    }

    private void cachePlaylistEntry(String str, String str2) {
        PlaylistEntry playlistEntry = new PlaylistEntry();
        int length = str.length();
        while (length > 0 && Character.isWhitespace(str.charAt(length - 1))) {
            length--;
        }
        if (length < 3) {
            return;
        }
        boolean z = false;
        if (length < str.length()) {
            str = str.substring(0, length);
        }
        char cCharAt = str.charAt(0);
        if (cCharAt == '/' || (Character.isLetter(cCharAt) && str.charAt(1) == ':' && str.charAt(2) == '\\')) {
            z = true;
        }
        if (!z) {
            str = str2 + str;
        }
        playlistEntry.path = str;
        this.mPlaylistEntries.add(playlistEntry);
    }

    private void processCachedPlaylist(Cursor cursor, ContentValues contentValues, Uri uri) {
        int i;
        cursor.moveToPosition(-1);
        do {
            if (!cursor.moveToNext()) {
                break;
            }
        } while (!matchEntries(cursor.getLong(0), cursor.getString(1)));
        int size = this.mPlaylistEntries.size();
        int i2 = 0;
        for (i = 0; i < size; i++) {
            PlaylistEntry playlistEntry = this.mPlaylistEntries.get(i);
            if (playlistEntry.bestmatchlevel > 0) {
                try {
                    contentValues.clear();
                    contentValues.put("play_order", Integer.valueOf(i2));
                    contentValues.put("audio_id", Long.valueOf(playlistEntry.bestmatchid));
                    this.mMediaProvider.insert(uri, contentValues);
                    i2++;
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in MediaScanner.processCachedPlaylist()", e);
                    return;
                }
            }
        }
        this.mPlaylistEntries.clear();
    }

    private void processM3uPlayList(String str, String str2, Uri uri, ContentValues contentValues, Cursor cursor) throws Throwable {
        BufferedReader bufferedReader;
        BufferedReader bufferedReader2 = null;
        try {
            try {
                try {
                    File file = new File(str);
                    if (file.exists()) {
                        bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)), 8192);
                        try {
                            this.mPlaylistEntries.clear();
                            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                                if (line.length() > 0 && line.charAt(0) != '#') {
                                    cachePlaylistEntry(line, str2);
                                }
                            }
                            processCachedPlaylist(cursor, contentValues, uri);
                        } catch (IOException e) {
                            e = e;
                            bufferedReader2 = bufferedReader;
                            Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e);
                            if (bufferedReader2 != null) {
                                bufferedReader2.close();
                            } else {
                                return;
                            }
                        } catch (Throwable th) {
                            th = th;
                            bufferedReader2 = bufferedReader;
                            if (bufferedReader2 != null) {
                                try {
                                    bufferedReader2.close();
                                } catch (IOException e2) {
                                    Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e2);
                                }
                            }
                            throw th;
                        }
                    } else {
                        bufferedReader = null;
                    }
                } catch (IOException e3) {
                    Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e3);
                    return;
                }
            } catch (IOException e4) {
                e = e4;
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void processPlsPlayList(String str, String str2, Uri uri, ContentValues contentValues, Cursor cursor) throws Throwable {
        BufferedReader bufferedReader;
        int iIndexOf;
        BufferedReader bufferedReader2 = null;
        try {
            try {
                try {
                    File file = new File(str);
                    if (file.exists()) {
                        bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)), 8192);
                        try {
                            this.mPlaylistEntries.clear();
                            for (String line = bufferedReader.readLine(); line != null; line = bufferedReader.readLine()) {
                                if (line.startsWith("File") && (iIndexOf = line.indexOf(61)) > 0) {
                                    cachePlaylistEntry(line.substring(iIndexOf + 1), str2);
                                }
                            }
                            processCachedPlaylist(cursor, contentValues, uri);
                        } catch (IOException e) {
                            e = e;
                            bufferedReader2 = bufferedReader;
                            Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e);
                            if (bufferedReader2 != null) {
                                bufferedReader2.close();
                            } else {
                                return;
                            }
                        } catch (Throwable th) {
                            th = th;
                            bufferedReader2 = bufferedReader;
                            if (bufferedReader2 != null) {
                                try {
                                    bufferedReader2.close();
                                } catch (IOException e2) {
                                    Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e2);
                                }
                            }
                            throw th;
                        }
                    } else {
                        bufferedReader = null;
                    }
                } catch (Throwable th2) {
                    th = th2;
                }
            } catch (IOException e3) {
                e = e3;
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        } catch (IOException e4) {
            Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e4);
        }
    }

    class WplHandler implements ElementListener {
        final ContentHandler handler;
        String playListDirectory;

        public WplHandler(String str, Uri uri, Cursor cursor) {
            this.playListDirectory = str;
            RootElement rootElement = new RootElement("smil");
            rootElement.getChild("body").getChild("seq").getChild(MediaStore.AUTHORITY).setElementListener(this);
            this.handler = rootElement.getContentHandler();
        }

        @Override
        public void start(Attributes attributes) {
            String value = attributes.getValue("", "src");
            if (value != null) {
                MediaScanner.this.cachePlaylistEntry(value, this.playListDirectory);
            }
        }

        @Override
        public void end() {
        }

        ContentHandler getContentHandler() {
            return this.handler;
        }
    }

    private void processWplPlayList(String str, String str2, Uri uri, ContentValues contentValues, Cursor cursor) throws Throwable {
        FileInputStream fileInputStream;
        FileInputStream fileInputStream2 = null;
        try {
            try {
                try {
                    File file = new File(str);
                    if (file.exists()) {
                        fileInputStream = new FileInputStream(file);
                        try {
                            this.mPlaylistEntries.clear();
                            Xml.parse(fileInputStream, Xml.findEncodingByName("UTF-8"), new WplHandler(str2, uri, cursor).getContentHandler());
                            processCachedPlaylist(cursor, contentValues, uri);
                        } catch (IOException e) {
                            e = e;
                            fileInputStream2 = fileInputStream;
                            e.printStackTrace();
                            if (fileInputStream2 == null) {
                                return;
                            } else {
                                fileInputStream2.close();
                            }
                        } catch (SAXException e2) {
                            e = e2;
                            fileInputStream2 = fileInputStream;
                            e.printStackTrace();
                            if (fileInputStream2 == null) {
                                return;
                            } else {
                                fileInputStream2.close();
                            }
                        } catch (Throwable th) {
                            th = th;
                            if (fileInputStream != null) {
                                try {
                                    fileInputStream.close();
                                } catch (IOException e3) {
                                    Log.e(TAG, "IOException in MediaScanner.processWplPlayList()", e3);
                                }
                            }
                            throw th;
                        }
                    } else {
                        fileInputStream = null;
                    }
                } catch (IOException e4) {
                    Log.e(TAG, "IOException in MediaScanner.processWplPlayList()", e4);
                    return;
                }
            } catch (IOException e5) {
                e = e5;
            } catch (SAXException e6) {
                e = e6;
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        } catch (Throwable th2) {
            th = th2;
            fileInputStream = null;
        }
    }

    private void processPlayList(FileEntry fileEntry, Cursor cursor) throws Throwable {
        Uri uriWithAppendedPath;
        String str = fileEntry.mPath;
        ContentValues contentValues = new ContentValues();
        int iLastIndexOf = str.lastIndexOf(47);
        if (iLastIndexOf < 0) {
            throw new IllegalArgumentException("bad path " + str);
        }
        long j = fileEntry.mRowId;
        String asString = contentValues.getAsString("name");
        if (asString == null && (asString = contentValues.getAsString("title")) == null) {
            int iLastIndexOf2 = str.lastIndexOf(46);
            asString = iLastIndexOf2 < 0 ? str.substring(iLastIndexOf + 1) : str.substring(iLastIndexOf + 1, iLastIndexOf2);
        }
        contentValues.put("name", asString);
        contentValues.put("date_modified", Long.valueOf(fileEntry.mLastModified));
        if (j == 0) {
            contentValues.put("_data", str);
            Uri uriInsert = this.mMediaProvider.insert(this.mPlaylistsUri, contentValues);
            ContentUris.parseId(uriInsert);
            uriWithAppendedPath = Uri.withAppendedPath(uriInsert, "members");
        } else {
            Uri uriWithAppendedId = ContentUris.withAppendedId(this.mPlaylistsUri, j);
            this.mMediaProvider.update(uriWithAppendedId, contentValues, null, null);
            uriWithAppendedPath = Uri.withAppendedPath(uriWithAppendedId, "members");
            this.mMediaProvider.delete(uriWithAppendedPath, null, null);
        }
        Uri uri = uriWithAppendedPath;
        String strSubstring = str.substring(0, iLastIndexOf + 1);
        MediaFile.MediaFileType fileType = MediaFile.getFileType(str);
        int i = fileType != null ? fileType.fileType : 0;
        if (i == 501) {
            processM3uPlayList(str, strSubstring, uri, contentValues, cursor);
        } else if (i == 502) {
            processPlsPlayList(str, strSubstring, uri, contentValues, cursor);
        } else if (i == 503) {
            processWplPlayList(str, strSubstring, uri, contentValues, cursor);
        }
    }

    protected void processPlayLists() throws Throwable {
        Cursor cursor = null;
        try {
            Cursor cursorQuery = this.mMediaProvider.query(this.mFilesUri, FILES_PRESCAN_PROJECTION, "media_type=2", null, null, null);
            for (FileEntry fileEntry : this.mPlayLists) {
                try {
                    if (fileEntry.mLastModifiedChanged) {
                        processPlayList(fileEntry, cursorQuery);
                    }
                } catch (RemoteException e) {
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                        return;
                    }
                    return;
                } catch (Throwable th) {
                    th = th;
                    cursor = cursorQuery;
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        } catch (RemoteException e2) {
        } catch (Throwable th2) {
            th = th2;
        }
    }

    @Override
    public void close() {
        this.mCloseGuard.close();
        if (this.mClosed.compareAndSet(false, true)) {
            this.mMediaProvider.close();
            native_finalize();
        }
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            close();
        } finally {
            super.finalize();
        }
    }
}
