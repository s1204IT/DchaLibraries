package android.media;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.backup.FullBackup;
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
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.provider.MediaStore;
import android.provider.Settings;
import android.sax.Element;
import android.sax.ElementListener;
import android.sax.RootElement;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.text.TextUtils;
import android.util.Log;
import android.util.Xml;
import dalvik.system.CloseGuard;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class MediaScanner implements AutoCloseable {
    private static final String ALARMS_DIR = "/alarms/";
    private static final String ALARM_SET = "alarm_set";
    private static final int APP1 = 65505;
    private static final int APPXTAG_PLUS_LENGTHTAG_BYTE_COUNT = 4;
    private static final int DATE_MODIFIED_PLAYLISTS_COLUMN_INDEX = 2;
    private static final boolean DEBUG;
    private static final String DEFAULT_RINGTONE_PROPERTY_PREFIX = "ro.config.";
    private static final boolean ENABLE_BULK_INSERTS = true;
    private static final String EXTERNAL_PRIMARY_STORAGE_PATH_L = "/storage/sdcard0/";
    private static final String EXTERNAL_SECONDARY_STORAGE_PATH_L = "/storage/sdcard1/";
    private static final int FILES_PRESCAN_DATE_MODIFIED_COLUMN_INDEX = 3;
    private static final int FILES_PRESCAN_FORMAT_COLUMN_INDEX = 2;
    private static final int FILES_PRESCAN_ID_COLUMN_INDEX = 0;
    private static final int FILES_PRESCAN_PATH_COLUMN_INDEX = 1;
    private static final String[] FILES_PRESCAN_PROJECTION;
    private static final String[] ID3_GENRES;
    private static final int ID_PLAYLISTS_COLUMN_INDEX = 0;
    private static final String[] ID_PROJECTION;
    private static final boolean LOGD;
    private static final String MIME_APPLICATION_OCTET_STREAM = "application/octet-stream";
    private static final String MTK_REFOCUS_PREFIX = "MRefocus";
    private static final String MUSIC_DIR = "/music/";
    private static final String NOTIFICATIONS_DIR = "/notifications/";
    private static final String NOTIFICATION_SET = "notification_set";
    private static final String NS_GDEPTH = "http://ns.google.com/photos/1.0/depthmap/";
    private static final int PATH_PLAYLISTS_COLUMN_INDEX = 1;
    private static final String[] PLAYLIST_MEMBERS_PROJECTION;
    private static final String PODCAST_DIR = "/podcasts/";
    private static final String RINGTONES_DIR = "/ringtones/";
    private static final String RINGTONE_SET = "ringtone_set";
    private static final int SOI = 65496;
    private static final int SOS = 65498;
    private static final String TAG = "MediaScanner";
    private static final String XMP_EXT_MAIN_HEADER1 = "http://ns.adobe.com/xmp/extension/";
    private static final String XMP_HEADER_START = "http://ns.adobe.com/xap/1.0/\u0000";
    private static HashMap<String, String> mMediaPaths;
    private static HashMap<String, String> mNoMediaPaths;
    private final Uri mAudioUri;
    private final Context mContext;
    private String mDefaultAlarmAlertFilename;
    private boolean mDefaultAlarmSet;
    private String mDefaultNotificationFilename;
    private boolean mDefaultNotificationSet;
    private String mDefaultRingtoneFilename;
    private boolean mDefaultRingtoneSet;
    private final boolean mExternalIsEmulated;
    private final String mExternalStoragePath;
    private final Uri mFilesUri;
    private final Uri mFilesUriNoNotify;
    private final Uri mImagesUri;
    private long mLimitBmpFileSize;
    private long mLimitGifFileSize;
    private MediaInserter mMediaInserter;
    private final ContentProviderClient mMediaProvider;
    private int mMtpObjectHandle;
    private long mNativeContext;
    private final String mPackageName;
    private final Uri mPlaylistsUri;
    private final boolean mProcessGenres;
    private final boolean mProcessPlaylists;
    private final Uri mVideoUri;
    private final String mVolumeName;
    private final AtomicBoolean mClosed = new AtomicBoolean();
    private final CloseGuard mCloseGuard = CloseGuard.get();
    private boolean mWasEmptyPriorToScan = false;
    private final BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
    private final ArrayList<PlaylistEntry> mPlaylistEntries = new ArrayList<>();
    private final ArrayList<FileEntry> mPlayLists = new ArrayList<>();
    private DrmManagerClient mDrmManagerClient = null;
    private final MyMediaScannerClient mClient = new MyMediaScannerClient(this, null);
    private ArrayList<String> mPlaylistFilePathList = new ArrayList<>();

    private final native void native_finalize();

    private static final native void native_init();

    private final native void native_setup();

    private native void processDirectory(String str, MediaScannerClient mediaScannerClient);

    private native void processFile(String str, String str2, MediaScannerClient mediaScannerClient);

    private native void setInterruptedFlag(boolean z);

    private native void setLocale(String str);

    public native byte[] extractAlbumArt(FileDescriptor fileDescriptor);

    static {
        System.loadLibrary("media_jni");
        native_init();
        LOGD = "eng".equals(Build.TYPE);
        DEBUG = !Log.isLoggable(TAG, 3) ? LOGD : true;
        FILES_PRESCAN_PROJECTION = new String[]{"_id", "_data", MediaStore.Files.FileColumns.FORMAT, "date_modified"};
        ID_PROJECTION = new String[]{"_id"};
        PLAYLIST_MEMBERS_PROJECTION = new String[]{MediaStore.Audio.Playlists.Members.PLAYLIST_ID};
        ID3_GENRES = new String[]{"Blues", "Classic Rock", "Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz", "Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap", "Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska", "Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient", "Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical", "Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel", "Noise", "AlternRock", "Bass", "Soul", "Punk", "Space", "Meditative", "Instrumental Pop", "Instrumental Rock", "Ethnic", "Gothic", "Darkwave", "Techno-Industrial", "Electronic", "Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy", "Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle", "Native American", "Cabaret", "New Wave", "Psychadelic", "Rave", "Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk", "Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll", "Hard Rock", "Folk", "Folk-Rock", "National Folk", "Swing", "Fast Fusion", "Bebob", "Latin", "Revival", "Celtic", "Bluegrass", "Avantgarde", "Gothic Rock", "Progressive Rock", "Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band", "Chorus", "Easy Listening", "Acoustic", "Humour", "Speech", "Chanson", "Opera", "Chamber Music", "Sonata", "Symphony", "Booty Bass", "Primus", "Porn Groove", "Satire", "Slow Jam", "Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad", "Rhythmic Soul", "Freestyle", "Duet", "Punk Rock", "Drum Solo", "A capella", "Euro-House", "Dance Hall", "Goa", "Drum & Bass", "Club-House", "Hardcore", "Terror", "Indie", "Britpop", null, "Polsk Punk", "Beat", "Christian Gangsta", "Heavy Metal", "Black Metal", "Crossover", "Contemporary Christian", "Christian Rock", "Merengue", "Salsa", "Thrash Metal", "Anime", "JPop", "Synthpop"};
        mNoMediaPaths = new HashMap<>();
        mMediaPaths = new HashMap<>();
    }

    private static class FileEntry {
        int mFormat;
        long mLastModified;
        boolean mLastModifiedChanged = false;
        String mPath;
        long mRowId;

        FileEntry(long rowId, String path, long lastModified, int format) {
            this.mRowId = rowId;
            this.mPath = path;
            this.mLastModified = lastModified;
            this.mFormat = format;
        }

        public String toString() {
            return this.mPath + " mRowId: " + this.mRowId;
        }
    }

    private static class PlaylistEntry {
        long bestmatchid;
        int bestmatchlevel;
        String path;

        PlaylistEntry(PlaylistEntry playlistEntry) {
            this();
        }

        private PlaylistEntry() {
        }
    }

    public MediaScanner(Context c, String volumeName) {
        this.mLimitBmpFileSize = Long.MAX_VALUE;
        this.mLimitGifFileSize = Long.MAX_VALUE;
        native_setup();
        this.mContext = c;
        this.mPackageName = c.getPackageName();
        this.mVolumeName = volumeName;
        this.mBitmapOptions.inSampleSize = 1;
        this.mBitmapOptions.inJustDecodeBounds = true;
        setDefaultRingtoneFileNames();
        this.mMediaProvider = this.mContext.getContentResolver().acquireContentProviderClient(MediaStore.AUTHORITY);
        this.mAudioUri = MediaStore.Audio.Media.getContentUri(volumeName);
        this.mVideoUri = MediaStore.Video.Media.getContentUri(volumeName);
        this.mImagesUri = MediaStore.Images.Media.getContentUri(volumeName);
        this.mFilesUri = MediaStore.Files.getContentUri(volumeName);
        this.mFilesUriNoNotify = this.mFilesUri.buildUpon().appendQueryParameter("nonotify", WifiEnterpriseConfig.ENGINE_ENABLE).build();
        if (!volumeName.equals("internal")) {
            this.mProcessPlaylists = true;
            this.mProcessGenres = true;
            this.mPlaylistsUri = MediaStore.Audio.Playlists.getContentUri(volumeName);
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
                    setLocale(language + "_" + country);
                } else {
                    setLocale(language);
                }
            }
        }
        this.mCloseGuard.open("close");
        this.mExternalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        this.mExternalIsEmulated = Environment.isExternalStorageEmulated();
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        if (am.isLowRamDevice()) {
            this.mLimitBmpFileSize = 6291456L;
            this.mLimitGifFileSize = 10485760L;
        } else {
            this.mLimitBmpFileSize = 54525952L;
            this.mLimitGifFileSize = 20971520L;
        }
    }

    private void setDefaultRingtoneFileNames() {
        this.mDefaultRingtoneFilename = SystemProperties.get("ro.config.ringtone");
        this.mDefaultNotificationFilename = SystemProperties.get("ro.config.notification_sound");
        this.mDefaultAlarmAlertFilename = SystemProperties.get("ro.config.alarm_alert");
        if (DEBUG) {
            Log.v(TAG, "setDefaultRingtoneFileNames: ringtone=" + this.mDefaultRingtoneFilename + ",notification=" + this.mDefaultNotificationFilename + ",alarm=" + this.mDefaultAlarmAlertFilename);
        }
    }

    private boolean isDrmEnabled() {
        String prop = SystemProperties.get("drm.service.enabled");
        if (prop != null) {
            return prop.equals("true");
        }
        return false;
    }

    private class MyMediaScannerClient implements MediaScannerClient {
        private String mAlbum;
        private String mAlbumArtist;
        private String mArtist;
        private int mCompilation;
        private String mComposer;
        private String mDrmContentDescriptioin;
        private String mDrmContentName;
        private String mDrmContentUr;
        private String mDrmContentVendor;
        private long mDrmDataLen;
        private String mDrmIconUri;
        private long mDrmMethod;
        private long mDrmOffset;
        private String mDrmRightsIssuer;
        private int mDuration;
        private long mFileSize;
        private int mFileType;
        private String mGenre;
        private int mHeight;
        private boolean mIsDrm;
        private long mLastModified;
        private String mMimeType;
        private boolean mNoMedia;
        private int mOrientation;
        private String mPath;
        private String mSlowMotionSpeed;
        private String mTitle;
        private int mTrack;
        private int mWidth;
        private String mWriter;
        private int mYear;

        MyMediaScannerClient(MediaScanner this$0, MyMediaScannerClient myMediaScannerClient) {
            this();
        }

        private MyMediaScannerClient() {
        }

        public FileEntry beginFile(String path, String mimeType, long lastModified, long fileSize, boolean isDirectory, boolean noMedia) {
            MediaFile.MediaFileType mediaFileType;
            int lastDot;
            this.mMimeType = isDirectory ? null : mimeType;
            this.mFileType = 0;
            this.mFileSize = fileSize;
            this.mIsDrm = false;
            if (!isDirectory) {
                if (!noMedia && MediaScanner.isNoMediaFile(path)) {
                    noMedia = true;
                }
                this.mNoMedia = noMedia;
                if (mimeType != null) {
                    this.mFileType = MediaFile.getFileTypeForMimeType(mimeType);
                }
                if (MediaFile.isImageFileType(this.mFileType) && (lastDot = path.lastIndexOf(".")) > 0 && path.substring(lastDot + 1).toUpperCase().equals("DCF")) {
                    if (MediaScanner.DEBUG) {
                        Log.v(MediaScanner.TAG, "detect a *.DCF file with input mime type:" + mimeType);
                    }
                    this.mFileType = 0;
                }
                if (this.mFileType == 0 && (mediaFileType = MediaFile.getFileType(path)) != null) {
                    this.mFileType = mediaFileType.fileType;
                    if (this.mMimeType == null || MediaScanner.this.isValueslessMimeType(this.mMimeType)) {
                        this.mMimeType = mediaFileType.mimeType;
                    }
                }
                if (MediaScanner.this.isDrmEnabled() && MediaFile.isDrmFileType(this.mFileType)) {
                    this.mFileType = getFileTypeFromDrm(path);
                }
                if (MediaScanner.this.isDrmEnabled() && path.endsWith(".mudp")) {
                    if (MediaScanner.this.mDrmManagerClient == null) {
                        MediaScanner.this.mDrmManagerClient = new DrmManagerClient(MediaScanner.this.mContext);
                    }
                    if (MediaScanner.this.mDrmManagerClient.canHandle(path, (String) null)) {
                        this.mMimeType = MediaScanner.this.mDrmManagerClient.getOriginalMimeType(path);
                        this.mIsDrm = true;
                        if (MediaScanner.DEBUG) {
                            Log.d(MediaScanner.TAG, "get cta file " + path + " with original mimetype " + this.mMimeType);
                        }
                    }
                }
            }
            FileEntry entry = MediaScanner.this.makeEntryFor(path);
            long delta = entry != null ? lastModified - entry.mLastModified : 0L;
            boolean wasModified = delta > 1 || delta < -1;
            if (entry == null || wasModified) {
                if (wasModified) {
                    entry.mLastModified = lastModified;
                } else {
                    entry = new FileEntry(0L, path, lastModified, isDirectory ? 12289 : 0);
                }
                entry.mLastModifiedChanged = true;
            }
            if (MediaScanner.this.mProcessPlaylists && MediaFile.isPlayListFileType(this.mFileType)) {
                MediaScanner.this.mPlayLists.add(entry);
                MediaScanner.this.mPlaylistFilePathList.add(path);
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
            this.mPath = path;
            this.mLastModified = lastModified;
            this.mWriter = null;
            this.mCompilation = 0;
            this.mWidth = 0;
            this.mHeight = 0;
            this.mDrmContentDescriptioin = null;
            this.mDrmContentName = null;
            this.mDrmContentUr = null;
            this.mDrmContentVendor = null;
            this.mDrmIconUri = null;
            this.mDrmRightsIssuer = null;
            this.mDrmDataLen = -1L;
            this.mDrmOffset = -1L;
            this.mDrmMethod = -1L;
            this.mSlowMotionSpeed = "(0,0)x0";
            this.mOrientation = 0;
            return entry;
        }

        @Override
        public void scanFile(String path, long lastModified, long fileSize, boolean isDirectory, boolean noMedia) {
            doScanFile(path, null, lastModified, fileSize, isDirectory, false, noMedia);
        }

        public Uri doScanFile(String path, String mimeType, long lastModified, long fileSize, boolean isDirectory, boolean scanAlways, boolean noMedia) {
            boolean music;
            if (Thread.currentThread().isInterrupted()) {
                Log.e(MediaScanner.TAG, "doScanFile isInterrupted!");
                MediaScanner.this.setInterruptedFlag(true);
                return null;
            }
            try {
                FileEntry entry = beginFile(path, mimeType, lastModified, fileSize, isDirectory, noMedia);
                if (entry == null) {
                    return null;
                }
                if (MediaScanner.this.mMtpObjectHandle != 0) {
                    entry.mRowId = 0L;
                }
                if (entry.mPath != null && ((!MediaScanner.this.mDefaultNotificationSet && doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultNotificationFilename)) || ((!MediaScanner.this.mDefaultRingtoneSet && doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultRingtoneFilename)) || (!MediaScanner.this.mDefaultAlarmSet && doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultAlarmAlertFilename))))) {
                    if (MediaScanner.DEBUG) {
                        Log.w(MediaScanner.TAG, "forcing rescan of " + entry.mPath + "since ringtone setting didn't finish");
                    }
                    scanAlways = true;
                }
                if (entry == null) {
                    return null;
                }
                if (!entry.mLastModifiedChanged && !scanAlways) {
                    return null;
                }
                if (noMedia) {
                    Uri result = endFile(entry, false, false, false, false, false);
                    return result;
                }
                String lowpath = path.toLowerCase(Locale.ROOT);
                boolean ringtones = lowpath.indexOf(MediaScanner.RINGTONES_DIR) > 0;
                boolean notifications = lowpath.indexOf(MediaScanner.NOTIFICATIONS_DIR) > 0;
                boolean alarms = lowpath.indexOf(MediaScanner.ALARMS_DIR) > 0;
                boolean podcasts = lowpath.indexOf(MediaScanner.PODCAST_DIR) > 0;
                if (lowpath.indexOf(MediaScanner.MUSIC_DIR) > 0) {
                    music = true;
                } else {
                    music = (ringtones || notifications || alarms || podcasts) ? false : true;
                }
                boolean isaudio = MediaFile.isAudioFileType(this.mFileType);
                boolean isvideo = MediaFile.isVideoFileType(this.mFileType);
                boolean isimage = MediaFile.isImageFileType(this.mFileType);
                if (isaudio || isvideo || isimage) {
                    path = Environment.maybeTranslateEmulatedPathToInternal(new File(path)).getAbsolutePath();
                }
                if (isaudio || isvideo) {
                    MediaScanner.this.processFile(path, mimeType, this);
                }
                if (isimage) {
                    processImageFile(path);
                }
                Uri result2 = endFile(entry, ringtones, notifications, alarms, music, podcasts);
                return result2;
            } catch (RemoteException e) {
                Log.e(MediaScanner.TAG, "RemoteException in MediaScanner.scanFile()", e);
                return null;
            }
        }

        private int parseSubstring(String s, int start, int defaultValue) {
            int length = s.length();
            if (start == length) {
                return defaultValue;
            }
            int start2 = start + 1;
            char ch = s.charAt(start);
            if (ch < '0' || ch > '9') {
                return defaultValue;
            }
            int result = ch - '0';
            while (start2 < length) {
                int start3 = start2 + 1;
                char ch2 = s.charAt(start2);
                if (ch2 < '0' || ch2 > '9') {
                    return result;
                }
                result = (result * 10) + (ch2 - '0');
                start2 = start3;
            }
            return result;
        }

        @Override
        public void handleStringTag(String name, String value) {
            if (MediaScanner.DEBUG) {
                Log.v(MediaScanner.TAG, "handleStringTag: name=" + name + ",value=" + value);
            }
            if (name.equalsIgnoreCase("title") || name.startsWith("title;")) {
                this.mTitle = value;
                return;
            }
            if (name.equalsIgnoreCase("artist") || name.startsWith("artist;")) {
                this.mArtist = value.trim();
                return;
            }
            if (name.equalsIgnoreCase("albumartist") || name.startsWith("albumartist;") || name.equalsIgnoreCase("band") || name.startsWith("band;")) {
                this.mAlbumArtist = value.trim();
                return;
            }
            if (name.equalsIgnoreCase("album") || name.startsWith("album;")) {
                this.mAlbum = value.trim();
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.Audio.AudioColumns.COMPOSER) || name.startsWith("composer;")) {
                this.mComposer = value.trim();
                return;
            }
            if (MediaScanner.this.mProcessGenres && (name.equalsIgnoreCase(MediaStore.Audio.AudioColumns.GENRE) || name.startsWith("genre;"))) {
                this.mGenre = getGenreName(value);
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.Audio.AudioColumns.YEAR) || name.startsWith("year;")) {
                this.mYear = parseSubstring(value, 0, 0);
                return;
            }
            if (name.equalsIgnoreCase("tracknumber") || name.startsWith("tracknumber;")) {
                int num = parseSubstring(value, 0, 0);
                this.mTrack = ((this.mTrack / 1000) * 1000) + num;
                return;
            }
            if (name.equalsIgnoreCase("discnumber") || name.equals("set") || name.startsWith("set;")) {
                int num2 = parseSubstring(value, 0, 0);
                this.mTrack = (num2 * 1000) + (this.mTrack % 1000);
                return;
            }
            if (name.equalsIgnoreCase("duration")) {
                this.mDuration = parseSubstring(value, 0, 0);
                return;
            }
            if (name.equalsIgnoreCase("writer") || name.startsWith("writer;")) {
                this.mWriter = value.trim();
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.Audio.AudioColumns.COMPILATION)) {
                this.mCompilation = parseSubstring(value, 0, 0);
                return;
            }
            if (name.equalsIgnoreCase("isdrm")) {
                this.mIsDrm = parseSubstring(value, 0, 0) == 1;
                return;
            }
            if (name.equalsIgnoreCase("width")) {
                this.mWidth = parseSubstring(value, 0, 0);
                return;
            }
            if (name.equalsIgnoreCase("height")) {
                this.mHeight = parseSubstring(value, 0, 0);
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.MediaColumns.DRM_CONTENT_URI)) {
                this.mDrmContentUr = value.trim();
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.MediaColumns.DRM_OFFSET)) {
                this.mDrmOffset = parseSubstring(value, 0, 0);
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.MediaColumns.DRM_DATA_LEN)) {
                this.mDrmDataLen = parseSubstring(value, 0, 0);
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.MediaColumns.DRM_RIGHTS_ISSUER)) {
                this.mDrmRightsIssuer = value.trim();
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.MediaColumns.DRM_CONTENT_NAME)) {
                this.mDrmContentName = value.trim();
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.MediaColumns.DRM_CONTENT_DESCRIPTION)) {
                this.mDrmContentDescriptioin = value.trim();
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.MediaColumns.DRM_CONTENT_VENDOR)) {
                this.mDrmContentVendor = value.trim();
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.MediaColumns.DRM_ICON_URI)) {
                this.mDrmIconUri = value.trim();
                return;
            }
            if (name.equalsIgnoreCase(MediaStore.MediaColumns.DRM_METHOD)) {
                this.mDrmMethod = parseSubstring(value, 0, 0);
            } else if (name.equalsIgnoreCase("SlowMotion_Speed_Value")) {
                this.mSlowMotionSpeed = "(0,0)x" + value;
            } else {
                if (!name.equalsIgnoreCase("rotation")) {
                    return;
                }
                this.mOrientation = parseSubstring(value, 0, 0);
            }
        }

        private boolean convertGenreCode(String input, String expected) {
            String output = getGenreName(input);
            if (output.equals(expected)) {
                return true;
            }
            if (MediaScanner.DEBUG) {
                Log.d(MediaScanner.TAG, "'" + input + "' -> '" + output + "', expected '" + expected + "'");
                return false;
            }
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

        public String getGenreName(String genreTagValue) {
            if (genreTagValue == null) {
                Log.e(MediaScanner.TAG, "getGenreName: Null genreTag!");
                return null;
            }
            int length = genreTagValue.length();
            if (length > 0) {
                boolean parenthesized = false;
                StringBuffer number = new StringBuffer();
                int i = 0;
                while (i < length) {
                    char c = genreTagValue.charAt(i);
                    if (i == 0 && c == '(') {
                        parenthesized = true;
                    } else {
                        if (!Character.isDigit(c)) {
                            break;
                        }
                        number.append(c);
                    }
                    i++;
                }
                char cCharAt = i < length ? genreTagValue.charAt(i) : ' ';
                if ((parenthesized && cCharAt == ')') || (!parenthesized && Character.isWhitespace(cCharAt))) {
                    try {
                        short genreIndex = Short.parseShort(number.toString());
                        if (genreIndex >= 0) {
                            if (genreIndex < MediaScanner.ID3_GENRES.length && MediaScanner.ID3_GENRES[genreIndex] != null) {
                                return MediaScanner.ID3_GENRES[genreIndex];
                            }
                            if (genreIndex == 255) {
                                Log.e(MediaScanner.TAG, "getGenreName: genreIndex = 0xFF!");
                                return null;
                            }
                            if (genreIndex < 255 && i + 1 < length) {
                                if (parenthesized && cCharAt == ')') {
                                    i++;
                                }
                                String ret = genreTagValue.substring(i).trim();
                                if (ret.length() != 0) {
                                    return ret;
                                }
                            } else {
                                return number.toString();
                            }
                        }
                    } catch (NumberFormatException e) {
                        Log.e(MediaScanner.TAG, "getGenreName: invalidNum=" + number.toString(), e);
                    }
                }
            }
            return genreTagValue;
        }

        private void processImageFile(String path) {
            long limitFileSize = Long.MAX_VALUE;
            if (404 == this.mFileType) {
                limitFileSize = MediaScanner.this.mLimitBmpFileSize;
            } else if (402 == this.mFileType) {
                limitFileSize = MediaScanner.this.mLimitGifFileSize;
            }
            if (this.mFileSize > limitFileSize) {
                if (MediaScanner.DEBUG) {
                    Log.w(MediaScanner.TAG, "processImageFile " + path + " over limit size " + limitFileSize);
                    return;
                }
                return;
            }
            try {
                MediaScanner.this.mBitmapOptions.outWidth = 0;
                MediaScanner.this.mBitmapOptions.outHeight = 0;
                BitmapFactory.decodeFile(path, MediaScanner.this.mBitmapOptions);
                this.mWidth = MediaScanner.this.mBitmapOptions.outWidth;
                this.mHeight = MediaScanner.this.mBitmapOptions.outHeight;
            } catch (Throwable th) {
                Log.e(MediaScanner.TAG, "processImageFile: path=" + path, th);
            }
            if (MediaScanner.DEBUG) {
                Log.v(MediaScanner.TAG, "processImageFile: path = " + path + ", width = " + this.mWidth + ", height = " + this.mHeight + ", limitFileSize = " + limitFileSize);
            }
        }

        @Override
        public void setMimeType(String mimeType) {
            if ("audio/mp4".equals(this.mMimeType) && mimeType.startsWith("video")) {
                return;
            }
            this.mMimeType = mimeType;
            this.mFileType = MediaFile.getFileTypeForMimeType(mimeType);
            if (!MediaScanner.DEBUG) {
                return;
            }
            Log.v(MediaScanner.TAG, "setMimeType: mMimeType = " + this.mMimeType);
        }

        private ContentValues toValues() {
            ContentValues map = new ContentValues();
            map.put("_data", this.mPath);
            map.put("title", this.mTitle);
            map.put("date_modified", Long.valueOf(this.mLastModified));
            map.put("_size", Long.valueOf(this.mFileSize));
            map.put("mime_type", this.mMimeType);
            map.put(MediaStore.MediaColumns.IS_DRM, Boolean.valueOf(this.mIsDrm));
            String resolution = null;
            if (this.mWidth > 0 && this.mHeight > 0) {
                map.put("width", Integer.valueOf(this.mWidth));
                map.put("height", Integer.valueOf(this.mHeight));
                resolution = this.mWidth + "x" + this.mHeight;
            }
            if (!this.mNoMedia) {
                if (MediaFile.isVideoFileType(this.mFileType)) {
                    map.put("artist", (this.mArtist == null || this.mArtist.length() <= 0) ? MediaStore.UNKNOWN_STRING : this.mArtist);
                    map.put("album", (this.mAlbum == null || this.mAlbum.length() <= 0) ? MediaStore.UNKNOWN_STRING : this.mAlbum);
                    map.put("duration", Integer.valueOf(this.mDuration));
                    if (resolution != null) {
                        map.put(MediaStore.Video.VideoColumns.RESOLUTION, resolution);
                    }
                    map.put(MediaStore.Video.VideoColumns.SLOW_MOTION_SPEED, this.mSlowMotionSpeed);
                    map.put("orientation", Integer.valueOf(this.mOrientation));
                } else if (!MediaFile.isImageFileType(this.mFileType) && MediaFile.isAudioFileType(this.mFileType)) {
                    map.put("artist", (this.mArtist == null || this.mArtist.length() <= 0) ? MediaStore.UNKNOWN_STRING : this.mArtist);
                    map.put(MediaStore.Audio.AudioColumns.ALBUM_ARTIST, (this.mAlbumArtist == null || this.mAlbumArtist.length() <= 0) ? null : this.mAlbumArtist);
                    map.put("album", (this.mAlbum == null || this.mAlbum.length() <= 0) ? MediaStore.UNKNOWN_STRING : this.mAlbum);
                    map.put(MediaStore.Audio.AudioColumns.COMPOSER, this.mComposer);
                    map.put(MediaStore.Audio.AudioColumns.GENRE, this.mGenre);
                    if (this.mYear != 0) {
                        map.put(MediaStore.Audio.AudioColumns.YEAR, Integer.valueOf(this.mYear));
                    }
                    map.put(MediaStore.Audio.AudioColumns.TRACK, Integer.valueOf(this.mTrack));
                    map.put("duration", Integer.valueOf(this.mDuration));
                    map.put(MediaStore.Audio.AudioColumns.COMPILATION, Integer.valueOf(this.mCompilation));
                }
            }
            if (this.mIsDrm) {
                map.put(MediaStore.MediaColumns.DRM_CONTENT_DESCRIPTION, this.mDrmContentDescriptioin);
                map.put(MediaStore.MediaColumns.DRM_CONTENT_NAME, this.mDrmContentName);
                map.put(MediaStore.MediaColumns.DRM_CONTENT_URI, this.mDrmContentUr);
                map.put(MediaStore.MediaColumns.DRM_CONTENT_VENDOR, this.mDrmContentVendor);
                map.put(MediaStore.MediaColumns.DRM_DATA_LEN, Long.valueOf(this.mDrmDataLen));
                map.put(MediaStore.MediaColumns.DRM_ICON_URI, this.mDrmIconUri);
                map.put(MediaStore.MediaColumns.DRM_OFFSET, Long.valueOf(this.mDrmOffset));
                map.put(MediaStore.MediaColumns.DRM_RIGHTS_ISSUER, this.mDrmRightsIssuer);
                map.put(MediaStore.MediaColumns.DRM_METHOD, Long.valueOf(this.mDrmMethod));
            }
            return map;
        }

        private Uri endFile(FileEntry entry, boolean ringtones, boolean notifications, boolean alarms, boolean music, boolean podcasts) throws RemoteException {
            int degree;
            String album;
            int lastSlash;
            if (this.mArtist == null || this.mArtist.length() == 0) {
                this.mArtist = this.mAlbumArtist;
            }
            ContentValues values = toValues();
            String title = values.getAsString("title");
            if (title == null || TextUtils.isEmpty(title.trim())) {
                values.put("title", MediaFile.getFileTitle(values.getAsString("_data")));
            }
            if (MediaStore.UNKNOWN_STRING.equals(values.getAsString("album")) && (lastSlash = (album = values.getAsString("_data")).lastIndexOf(47)) >= 0) {
                int previousSlash = 0;
                while (true) {
                    int idx = album.indexOf(47, previousSlash + 1);
                    if (idx < 0 || idx >= lastSlash) {
                        break;
                    }
                    previousSlash = idx;
                }
                if (previousSlash != 0) {
                    values.put("album", album.substring(previousSlash + 1, lastSlash));
                }
            }
            long rowId = entry.mRowId;
            if (MediaScanner.DEBUG) {
                Log.d(MediaScanner.TAG, "endFile() mFileType = " + this.mFileType);
            }
            if (MediaFile.isAudioFileType(this.mFileType) && (rowId == 0 || MediaScanner.this.mMtpObjectHandle != 0)) {
                values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, Boolean.valueOf(ringtones));
                values.put(MediaStore.Audio.AudioColumns.IS_NOTIFICATION, Boolean.valueOf(notifications));
                values.put(MediaStore.Audio.AudioColumns.IS_ALARM, Boolean.valueOf(alarms));
                values.put(MediaStore.Audio.AudioColumns.IS_MUSIC, Boolean.valueOf(music));
                values.put(MediaStore.Audio.AudioColumns.IS_PODCAST, Boolean.valueOf(podcasts));
            } else if ((this.mFileType == 401 || this.mFileType == 499) && !this.mNoMedia) {
                ExifInterface exif = null;
                try {
                    ExifInterface exif2 = new ExifInterface(entry.mPath);
                    exif = exif2;
                } catch (IOException ex) {
                    Log.e(MediaScanner.TAG, "endFile: Null ExifInterface!", ex);
                }
                if (exif != null) {
                    float[] latlng = new float[2];
                    if (exif.getLatLong(latlng)) {
                        values.put("latitude", Float.valueOf(latlng[0]));
                        values.put("longitude", Float.valueOf(latlng[1]));
                    }
                    long time = exif.getGpsDateTime();
                    if (time != -1) {
                        values.put("datetaken", Long.valueOf(time));
                    } else {
                        long time2 = exif.getDateTime();
                        if (time2 != -1 && Math.abs((this.mLastModified * 1000) - time2) >= AlarmManager.INTERVAL_DAY) {
                            values.put("datetaken", Long.valueOf(time2));
                        }
                    }
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
                    if (orientation != -1) {
                        switch (orientation) {
                            case 3:
                                degree = 180;
                                break;
                            case 4:
                            case 5:
                            case 7:
                            default:
                                degree = 0;
                                break;
                            case 6:
                                degree = 90;
                                break;
                            case 8:
                                degree = 270;
                                break;
                        }
                        values.put("orientation", Integer.valueOf(degree));
                    }
                    long groupId = 0;
                    String groupIdStr = exif.getAttribute(ExifInterface.TAG_MTK_CONSHOT_GROUP_ID);
                    if (groupIdStr != null) {
                        try {
                            groupId = Long.parseLong(groupIdStr);
                        } catch (NumberFormatException e) {
                            Log.e(MediaScanner.TAG, "endFile: " + groupIdStr + " cannot be converted to long.");
                        }
                    }
                    int groupIndex = exif.getAttributeInt(ExifInterface.TAG_MTK_CONSHOT_PIC_INDEX, 0);
                    long focusHigh = exif.getAttributeInt(ExifInterface.TAG_MTK_CONSHOT_FOCUS_HIGH, 0);
                    long focusLow = exif.getAttributeInt(ExifInterface.TAG_MTK_CONSHOT_FOCUS_LOW, 0);
                    values.put(MediaStore.Images.ImageColumns.FOCUS_VALUE_HIGH, Long.valueOf(focusHigh));
                    values.put(MediaStore.Images.ImageColumns.FOCUS_VALUE_LOW, Long.valueOf(focusLow));
                    values.put("group_id", Long.valueOf(groupId));
                    values.put(MediaStore.Images.ImageColumns.GROUP_INDEX, Integer.valueOf(groupIndex));
                    int refocus = MediaScanner.isStereoPhoto(entry.mPath) ? 1 : 0;
                    values.put(MediaStore.Images.ImageColumns.CAMERA_REFOCUS, Integer.valueOf(refocus));
                }
            }
            Uri tableUri = MediaScanner.this.mFilesUri;
            MediaInserter inserter = MediaScanner.this.mMediaInserter;
            if (!this.mNoMedia) {
                if (MediaFile.isVideoFileType(this.mFileType)) {
                    tableUri = MediaScanner.this.mVideoUri;
                } else if (MediaFile.isImageFileType(this.mFileType)) {
                    tableUri = MediaScanner.this.mImagesUri;
                } else if (MediaFile.isAudioFileType(this.mFileType)) {
                    tableUri = MediaScanner.this.mAudioUri;
                }
            }
            Uri result = null;
            boolean needToSetSettings = false;
            if (rowId == 0) {
                if (MediaScanner.this.mMtpObjectHandle != 0) {
                    values.put(MediaStore.MediaColumns.MEDIA_SCANNER_NEW_OBJECT_ID, Integer.valueOf(MediaScanner.this.mMtpObjectHandle));
                }
                if (tableUri == MediaScanner.this.mFilesUri) {
                    int format = entry.mFormat;
                    if (format == 0) {
                        format = MediaFile.getFormatCode(entry.mPath, this.mMimeType);
                    }
                    values.put(MediaStore.Files.FileColumns.FORMAT, Integer.valueOf(format));
                }
                if (notifications && ((MediaScanner.this.mWasEmptyPriorToScan && !MediaScanner.this.mDefaultNotificationSet) || doesSettingEmpty(MediaScanner.NOTIFICATION_SET))) {
                    if (TextUtils.isEmpty(MediaScanner.this.mDefaultNotificationFilename) || doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultNotificationFilename)) {
                        needToSetSettings = true;
                        if (MediaScanner.DEBUG) {
                            Log.v(MediaScanner.TAG, "endFile: needToSetNotification=true.");
                        }
                    }
                } else if (ringtones && ((MediaScanner.this.mWasEmptyPriorToScan && !MediaScanner.this.mDefaultRingtoneSet) || doesSettingEmpty(MediaScanner.RINGTONE_SET))) {
                    if (TextUtils.isEmpty(MediaScanner.this.mDefaultRingtoneFilename) || doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultRingtoneFilename)) {
                        needToSetSettings = true;
                        if (MediaScanner.DEBUG) {
                            Log.v(MediaScanner.TAG, "endFile: needToSetRingtone=true.");
                        }
                    }
                } else if (alarms && (((MediaScanner.this.mWasEmptyPriorToScan && !MediaScanner.this.mDefaultAlarmSet) || doesSettingEmpty(MediaScanner.ALARM_SET)) && (TextUtils.isEmpty(MediaScanner.this.mDefaultAlarmAlertFilename) || doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultAlarmAlertFilename)))) {
                    needToSetSettings = true;
                    if (MediaScanner.DEBUG) {
                        Log.v(MediaScanner.TAG, "endFile: needToSetAlarm=true.");
                    }
                }
                if (inserter == null || needToSetSettings) {
                    if (inserter != null) {
                        inserter.flushAll();
                    }
                    result = MediaScanner.this.mMediaProvider.insert(tableUri, values);
                } else if (entry.mFormat == 12289) {
                    inserter.insertwithPriority(tableUri, values);
                } else {
                    inserter.insert(tableUri, values);
                }
                if (result != null) {
                    rowId = ContentUris.parseId(result);
                    entry.mRowId = rowId;
                }
            } else {
                result = ContentUris.withAppendedId(tableUri, rowId);
                values.remove("_data");
                int mediaType = 0;
                if (!MediaScanner.isNoMediaPath(entry.mPath)) {
                    int fileType = MediaFile.getFileTypeForMimeType(this.mMimeType);
                    if (MediaFile.isAudioFileType(fileType)) {
                        mediaType = 2;
                    } else if (MediaFile.isVideoFileType(fileType)) {
                        mediaType = 3;
                    } else if (MediaFile.isImageFileType(fileType)) {
                        mediaType = 1;
                    } else if (MediaFile.isPlayListFileType(fileType)) {
                        mediaType = 4;
                    }
                    values.put("media_type", Integer.valueOf(mediaType));
                }
                MediaScanner.this.mMediaProvider.update(result, values, null, null);
            }
            if (needToSetSettings) {
                if (notifications && doesSettingEmpty(MediaScanner.NOTIFICATION_SET)) {
                    setSettingIfNotSet(Settings.System.NOTIFICATION_SOUND, tableUri, rowId);
                    MediaScanner.this.mDefaultNotificationSet = true;
                    setSettingFlag(MediaScanner.NOTIFICATION_SET);
                    if (MediaScanner.DEBUG) {
                        Log.v(MediaScanner.TAG, "endFile: set notification. uri=" + tableUri + ", rowId=" + rowId);
                    }
                } else if (ringtones && doesSettingEmpty(MediaScanner.RINGTONE_SET)) {
                    setSettingIfNotSet(Settings.System.RINGTONE, tableUri, rowId);
                    MediaScanner.this.mDefaultRingtoneSet = true;
                    setSettingFlag(MediaScanner.RINGTONE_SET);
                    if (MediaScanner.DEBUG) {
                        Log.v(MediaScanner.TAG, "endFile: set ringtone. uri=" + tableUri + ", rowId=" + rowId);
                    }
                } else if (alarms && doesSettingEmpty(MediaScanner.ALARM_SET)) {
                    setSettingIfNotSet(Settings.System.ALARM_ALERT, tableUri, rowId);
                    MediaScanner.this.mDefaultAlarmSet = true;
                    setSettingFlag(MediaScanner.ALARM_SET);
                }
            }
            return result;
        }

        private boolean doesPathHaveFilename(String path, String filename) {
            int pathFilenameStart = path.lastIndexOf(File.separatorChar) + 1;
            int filenameLength = filename.length();
            return path.regionMatches(pathFilenameStart, filename, 0, filenameLength) && pathFilenameStart + filenameLength == path.length();
        }

        private boolean doesSettingEmpty(String settingName) {
            String existingSettingValue = Settings.System.getString(MediaScanner.this.mContext.getContentResolver(), settingName);
            if (TextUtils.isEmpty(existingSettingValue)) {
                return true;
            }
            return false;
        }

        private void setSettingFlag(String settingName) {
            if (MediaScanner.DEBUG) {
                Log.d(MediaScanner.TAG, "setSettingFlag set:" + settingName);
            }
            Settings.System.putString(MediaScanner.this.mContext.getContentResolver(), settingName, "yes");
        }

        private void setSettingIfNotSet(String settingName, Uri uri, long rowId) {
            ContentResolver cr = MediaScanner.this.mContext.getContentResolver();
            String existingSettingValue = Settings.System.getString(cr, settingName);
            if (!TextUtils.isEmpty(existingSettingValue)) {
                return;
            }
            Uri settingUri = Settings.System.getUriFor(settingName);
            Uri ringtoneUri = ContentUris.withAppendedId(uri, rowId);
            RingtoneManager.setActualDefaultRingtoneUri(MediaScanner.this.mContext, RingtoneManager.getDefaultType(settingUri), ringtoneUri);
            if (!MediaScanner.DEBUG) {
                return;
            }
            Log.v(MediaScanner.TAG, "setSettingIfNotSet: name=" + settingName + ",value=" + rowId);
        }

        private int getFileTypeFromDrm(String path) {
            if (!MediaScanner.this.isDrmEnabled()) {
                return 0;
            }
            if (MediaScanner.this.mDrmManagerClient == null) {
                MediaScanner.this.mDrmManagerClient = new DrmManagerClient(MediaScanner.this.mContext);
            }
            if (!MediaScanner.this.mDrmManagerClient.canHandle(path, (String) null)) {
                return 0;
            }
            this.mIsDrm = true;
            String drmMimetype = MediaScanner.this.mDrmManagerClient.getOriginalMimeType(path);
            if (drmMimetype == null) {
                return 0;
            }
            this.mMimeType = drmMimetype;
            int resultFileType = MediaFile.getFileTypeForMimeType(drmMimetype);
            return resultFileType;
        }
    }

    private String settingSetIndicatorName(String base) {
        return base + "_set";
    }

    private boolean wasRingtoneAlreadySet(String name) {
        ContentResolver cr = this.mContext.getContentResolver();
        String indicatorName = settingSetIndicatorName(name);
        try {
            return Settings.System.getInt(cr, indicatorName) != 0;
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
    }

    private void prescan(String filePath, boolean prescanFiles) throws RemoteException {
        String where;
        String[] selectionArgs;
        if (DEBUG) {
            Log.v(TAG, "prescan>>> filePath=" + filePath + ",prescanFiles=" + prescanFiles);
        }
        Cursor c = null;
        this.mPlayLists.clear();
        if (filePath != null) {
            where = "_id>? AND _data=?";
            selectionArgs = new String[]{ProxyInfo.LOCAL_EXCL_LIST, filePath};
        } else {
            where = "_id>?";
            selectionArgs = new String[]{ProxyInfo.LOCAL_EXCL_LIST};
        }
        this.mDefaultRingtoneSet = wasRingtoneAlreadySet(Settings.System.RINGTONE);
        this.mDefaultNotificationSet = wasRingtoneAlreadySet(Settings.System.NOTIFICATION_SOUND);
        this.mDefaultAlarmSet = wasRingtoneAlreadySet(Settings.System.ALARM_ALERT);
        Uri.Builder builder = this.mFilesUri.buildUpon();
        builder.appendQueryParameter(MediaStore.PARAM_DELETE_DATA, "false");
        MediaBulkDeleter deleter = new MediaBulkDeleter(this.mMediaProvider, builder.build());
        long lastId = Long.MIN_VALUE;
        if (prescanFiles) {
            try {
                Uri limitUri = this.mFilesUri.buildUpon().appendQueryParameter("limit", "1000").appendQueryParameter("force", WifiEnterpriseConfig.ENGINE_ENABLE).build();
                this.mWasEmptyPriorToScan = true;
                while (true) {
                    selectionArgs[0] = ProxyInfo.LOCAL_EXCL_LIST + lastId;
                    if (c != null) {
                        c.close();
                    }
                    c = this.mMediaProvider.query(limitUri, FILES_PRESCAN_PROJECTION, where, selectionArgs, "_id", null);
                    if (c == null) {
                        break;
                    }
                    int num = c.getCount();
                    if (num == 0) {
                        break;
                    }
                    this.mWasEmptyPriorToScan = false;
                    String externalPrimaryStoragePathOnM = null;
                    String externalSecondaryStoragePathOnM = null;
                    boolean isSharedSdCardEanbled = SystemProperties.getBoolean("ro.mtk_shared_sdcard", false);
                    StorageManager storageManager = (StorageManager) this.mContext.getSystemService(Context.STORAGE_SERVICE);
                    for (VolumeInfo vol : storageManager.getVolumes()) {
                        if (!VolumeInfo.ID_PRIVATE_INTERNAL.equals(vol.id)) {
                            if (isSharedSdCardEanbled) {
                                if (vol.isPrimary()) {
                                    externalPrimaryStoragePathOnM = vol.getPath().getPath() + "/";
                                } else if (vol.getDisk() != null && vol.getDisk().isSd()) {
                                    externalSecondaryStoragePathOnM = vol.getPath().getPath() + "/";
                                }
                            } else if (vol.isPhoneStorage()) {
                                externalPrimaryStoragePathOnM = vol.getPath().getPath() + "/";
                            } else if (vol.getDisk() != null && vol.getDisk().isSd()) {
                                externalSecondaryStoragePathOnM = vol.getPath().getPath() + "/";
                            }
                        }
                    }
                    if (externalPrimaryStoragePathOnM == null) {
                        if (externalSecondaryStoragePathOnM != null) {
                            externalPrimaryStoragePathOnM = externalSecondaryStoragePathOnM;
                            externalSecondaryStoragePathOnM = null;
                        }
                    } else if (externalPrimaryStoragePathOnM.startsWith("/storage/emulated/")) {
                        externalPrimaryStoragePathOnM = externalPrimaryStoragePathOnM + UserHandle.myUserId() + "/";
                    }
                    if (DEBUG) {
                        Log.v(TAG, "prescan>>> externalPrimaryStoragePathOnM=" + externalPrimaryStoragePathOnM + ", externalSecondaryStoragePathOnM=" + externalSecondaryStoragePathOnM + ", uid = " + UserHandle.myUserId());
                    }
                    while (c.moveToNext()) {
                        long rowId = c.getLong(0);
                        String path = c.getString(1);
                        int format = c.getInt(2);
                        c.getLong(3);
                        lastId = rowId;
                        if (path != null && path.startsWith("/")) {
                            boolean exists = false;
                            String newPath = null;
                            try {
                                if (path.startsWith("/storage/sdcard")) {
                                    newPath = (!path.startsWith(EXTERNAL_PRIMARY_STORAGE_PATH_L) || externalPrimaryStoragePathOnM == null) ? (!path.startsWith(EXTERNAL_SECONDARY_STORAGE_PATH_L) || externalSecondaryStoragePathOnM == null) ? null : path.replace(EXTERNAL_SECONDARY_STORAGE_PATH_L, externalSecondaryStoragePathOnM) : path.replace(EXTERNAL_PRIMARY_STORAGE_PATH_L, externalPrimaryStoragePathOnM);
                                    if (newPath != null) {
                                        if (DEBUG) {
                                            Log.v(TAG, "try to check if newPath exists, " + newPath);
                                        }
                                        if (Os.access(newPath, OsConstants.F_OK)) {
                                            if (DEBUG) {
                                                Log.v(TAG, "update>>> path=" + path + ", newPath=" + newPath);
                                            }
                                            exists = true;
                                            Uri realUri = ContentUris.withAppendedId(this.mFilesUri, rowId);
                                            ContentValues values = new ContentValues();
                                            values.put("_data", newPath);
                                            this.mMediaProvider.update(realUri, values, null, null);
                                        }
                                    }
                                }
                                if (newPath == null) {
                                    exists = Os.access(path, OsConstants.F_OK);
                                }
                            } catch (ErrnoException e) {
                                if (DEBUG) {
                                    Log.e(TAG, "prescan: ErrnoException! path=" + path);
                                }
                            }
                            if (!exists && !MtpConstants.isAbstractObject(format)) {
                                MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
                                int fileType = mediaFileType == null ? 0 : mediaFileType.fileType;
                                if (!MediaFile.isPlayListFileType(fileType)) {
                                    deleter.delete(rowId);
                                    if (path.toLowerCase(Locale.US).endsWith("/.nomedia")) {
                                        deleter.flush();
                                        String parent = new File(path).getParent();
                                        this.mMediaProvider.call(MediaStore.UNHIDE_CALL, parent, null);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                if (c != null) {
                    c.close();
                }
                deleter.flush();
                throw th;
            }
        }
        if (c != null) {
            c.close();
        }
        deleter.flush();
        int originalImageCount = 0;
        int originalVideoCount = 0;
        int originalAudioCount = 0;
        try {
            Cursor c2 = this.mMediaProvider.query(this.mImagesUri.buildUpon().appendQueryParameter("force", WifiEnterpriseConfig.ENGINE_ENABLE).build(), ID_PROJECTION, null, null, null, null);
            if (c2 != null) {
                originalImageCount = c2.getCount();
                c2.close();
            }
            Cursor c3 = this.mMediaProvider.query(this.mVideoUri.buildUpon().appendQueryParameter("force", WifiEnterpriseConfig.ENGINE_ENABLE).build(), ID_PROJECTION, null, null, null, null);
            if (c3 != null) {
                originalVideoCount = c3.getCount();
                c3.close();
            }
            c = this.mMediaProvider.query(this.mAudioUri.buildUpon().appendQueryParameter("force", WifiEnterpriseConfig.ENGINE_ENABLE).build(), ID_PROJECTION, null, null, null, null);
            if (c != null) {
                originalAudioCount = c.getCount();
                c.close();
                c = null;
            }
            if (DEBUG) {
                Log.v(TAG, "prescan<<< imageCount=" + originalImageCount + ",videoCount=" + originalVideoCount + ", audioCount=" + originalAudioCount + ", lastId=" + lastId + ",isEmpty=" + this.mWasEmptyPriorToScan);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }
    }

    static class MediaBulkDeleter {
        final Uri mBaseUri;
        final ContentProviderClient mProvider;
        StringBuilder whereClause = new StringBuilder();
        ArrayList<String> whereArgs = new ArrayList<>(100);

        public MediaBulkDeleter(ContentProviderClient provider, Uri baseUri) {
            this.mProvider = provider;
            this.mBaseUri = baseUri;
        }

        public void delete(long id) throws RemoteException {
            if (this.whereClause.length() != 0) {
                this.whereClause.append(",");
            }
            this.whereClause.append("?");
            this.whereArgs.add(ProxyInfo.LOCAL_EXCL_LIST + id);
            if (this.whereArgs.size() <= 100) {
                return;
            }
            flush();
        }

        public void flush() throws RemoteException {
            int size = this.whereArgs.size();
            if (size <= 0) {
                return;
            }
            String[] foo = new String[size];
            this.mProvider.delete(this.mBaseUri, "_id IN (" + this.whereClause.toString() + ")", (String[]) this.whereArgs.toArray(foo));
            this.whereClause.setLength(0);
            this.whereArgs.clear();
        }
    }

    private void postscan(String[] directories) throws RemoteException {
        if (this.mProcessPlaylists) {
            processPlayLists();
        }
        Cursor c = null;
        try {
            Cursor c2 = this.mMediaProvider.query(this.mImagesUri.buildUpon().appendQueryParameter("force", WifiEnterpriseConfig.ENGINE_ENABLE).build(), ID_PROJECTION, null, null, null, null);
            if (c2 != null) {
                c2.getCount();
                c2.close();
            }
            c = this.mMediaProvider.query(this.mVideoUri.buildUpon().appendQueryParameter("force", WifiEnterpriseConfig.ENGINE_ENABLE).build(), ID_PROJECTION, null, null, null, null);
            if (c != null) {
                c.getCount();
                c.close();
                c = null;
            }
        } finally {
            if (c != null) {
                c.close();
            }
            this.mPlayLists.clear();
        }
    }

    private void releaseResources() {
        if (this.mDrmManagerClient == null) {
            return;
        }
        this.mDrmManagerClient.close();
        this.mDrmManagerClient = null;
    }

    public void scanDirectories(String[] directories) {
        try {
            long start = System.currentTimeMillis();
            prescan(null, true);
            long prescan = System.currentTimeMillis();
            this.mMediaInserter = new MediaInserter(this.mMediaProvider, 500);
            for (String str : directories) {
                processDirectory(str, this.mClient);
            }
            this.mMediaInserter.flushAll();
            this.mMediaInserter = null;
            long scan = System.currentTimeMillis();
            postscan(directories);
            long end = System.currentTimeMillis();
            if (DEBUG) {
                Log.d(TAG, " prescan time: " + (prescan - start) + "ms\n");
                Log.d(TAG, "    scan time: " + (scan - prescan) + "ms\n");
                Log.d(TAG, "postscan time: " + (end - scan) + "ms\n");
                Log.d(TAG, "   total time: " + (end - start) + "ms\n");
            }
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e);
        } catch (SQLException e2) {
            Log.e(TAG, "SQLException in MediaScanner.scan()", e2);
        } catch (RemoteException e3) {
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e3);
        } finally {
            releaseResources();
        }
    }

    public Uri scanSingleFile(String path, String mimeType) {
        try {
            prescan(path, true);
            File file = new File(path);
            if (file.exists()) {
                long lastModifiedSeconds = file.lastModified() / 1000;
                return this.mClient.doScanFile(path, mimeType, lastModifiedSeconds, file.length(), file.isDirectory(), true, isNoMediaPath(path));
            }
            Log.e(TAG, "scanSingleFile: Not exist path=" + path);
            return null;
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
            return null;
        } finally {
            releaseResources();
        }
    }

    private static boolean isNoMediaFile(String path) {
        int lastSlash;
        File file = new File(path);
        if (!file.isDirectory() && (lastSlash = path.lastIndexOf(47)) >= 0 && lastSlash + 2 < path.length()) {
            if (path.regionMatches(lastSlash + 1, "._", 0, 2)) {
                return true;
            }
            if (path.regionMatches(true, path.length() - 4, ".jpg", 0, 4)) {
                if (path.regionMatches(true, lastSlash + 1, "AlbumArt_{", 0, 10) || path.regionMatches(true, lastSlash + 1, "AlbumArt.", 0, 9)) {
                    return true;
                }
                int length = (path.length() - lastSlash) - 1;
                if ((length == 17 && path.regionMatches(true, lastSlash + 1, "AlbumArtSmall", 0, 13)) || (length == 10 && path.regionMatches(true, lastSlash + 1, "Folder", 0, 6))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void clearMediaPathCache(boolean clearMediaPaths, boolean clearNoMediaPaths) {
        synchronized (MediaScanner.class) {
            if (clearMediaPaths) {
                mMediaPaths.clear();
                if (clearNoMediaPaths) {
                    mNoMediaPaths.clear();
                }
            } else if (clearNoMediaPaths) {
            }
        }
    }

    public static boolean isNoMediaPath(String path) {
        if (path == null) {
            return false;
        }
        if (path.indexOf("/.") >= 0) {
            return true;
        }
        int firstSlash = path.lastIndexOf(47);
        if (firstSlash <= 0) {
            return false;
        }
        String parent = path.substring(0, firstSlash);
        synchronized (MediaScanner.class) {
            if (mNoMediaPaths.containsKey(parent)) {
                return true;
            }
            if (!mMediaPaths.containsKey(parent)) {
                int offset = 1;
                while (offset >= 0) {
                    int slashIndex = path.indexOf(47, offset);
                    if (slashIndex > offset) {
                        slashIndex++;
                        File file = new File(path.substring(0, slashIndex) + MediaStore.MEDIA_IGNORE_FILENAME);
                        if (file.exists()) {
                            mNoMediaPaths.put(parent, ProxyInfo.LOCAL_EXCL_LIST);
                            return true;
                        }
                    } else if (slashIndex == offset) {
                        slashIndex++;
                    }
                    offset = slashIndex;
                }
                mMediaPaths.put(parent, ProxyInfo.LOCAL_EXCL_LIST);
            }
            return isNoMediaFile(path);
        }
    }

    public void scanMtpFile(String path, int objectHandle, int format) {
        MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
        int fileType = mediaFileType == null ? 0 : mediaFileType.fileType;
        File file = new File(path);
        long lastModifiedSeconds = file.lastModified() / 1000;
        if (!MediaFile.isAudioFileType(fileType) && !MediaFile.isVideoFileType(fileType) && !MediaFile.isImageFileType(fileType) && !MediaFile.isPlayListFileType(fileType) && !MediaFile.isDrmFileType(fileType)) {
            ContentValues values = new ContentValues();
            values.put("_size", Long.valueOf(format == 12289 ? 0L : file.length()));
            values.put("date_modified", Long.valueOf(lastModifiedSeconds));
            try {
                String[] whereArgs = {Integer.toString(objectHandle)};
                this.mMediaProvider.update(MediaStore.Files.getMtpObjectsUri(this.mVolumeName), values, "_id=?", whereArgs);
                return;
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException in scanMtpFile", e);
                return;
            }
        }
        this.mMtpObjectHandle = objectHandle;
        Cursor fileList = null;
        try {
            try {
                if (MediaFile.isPlayListFileType(fileType)) {
                    prescan(null, true);
                    FileEntry entry = makeEntryFor(path);
                    if (entry != null) {
                        fileList = this.mMediaProvider.query(this.mFilesUri, FILES_PRESCAN_PROJECTION, null, null, null, null);
                        processPlayList(entry, fileList);
                    }
                } else {
                    prescan(path, false);
                    this.mClient.doScanFile(path, mediaFileType.mimeType, lastModifiedSeconds, file.length(), format == 12289, true, isNoMediaPath(path));
                }
                this.mMtpObjectHandle = 0;
                if (fileList != null) {
                    fileList.close();
                }
                releaseResources();
            } catch (RemoteException e2) {
                Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e2);
                this.mMtpObjectHandle = 0;
                if (0 != 0) {
                    fileList.close();
                }
                releaseResources();
            }
        } catch (Throwable th) {
            this.mMtpObjectHandle = 0;
            if (0 != 0) {
                fileList.close();
            }
            releaseResources();
            throw th;
        }
    }

    FileEntry makeEntryFor(String path) {
        Cursor c = null;
        try {
            try {
                String[] selectionArgs = {path};
                c = this.mMediaProvider.query(this.mFilesUriNoNotify, FILES_PRESCAN_PROJECTION, "_data=?", selectionArgs, null, null);
                if (c == null || !c.moveToFirst()) {
                    if (c == null) {
                        return null;
                    }
                    c.close();
                    return null;
                }
                long rowId = c.getLong(0);
                int format = c.getInt(2);
                long lastModified = c.getLong(3);
                FileEntry fileEntry = new FileEntry(rowId, path, lastModified, format);
                if (c != null) {
                    c.close();
                }
                return fileEntry;
            } catch (RemoteException e) {
                Log.e(TAG, "makeEntryFor: RemoteException! path=" + path, e);
                if (c == null) {
                    return null;
                }
                c.close();
                return null;
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    private int matchPaths(String path1, String path2) {
        int result = 0;
        int end1 = path1.length();
        int end2 = path2.length();
        while (end1 > 0 && end2 > 0) {
            int slash1 = path1.lastIndexOf(47, end1 - 1);
            int slash2 = path2.lastIndexOf(47, end2 - 1);
            int backSlash1 = path1.lastIndexOf(92, end1 - 1);
            int backSlash2 = path2.lastIndexOf(92, end2 - 1);
            int start1 = slash1 > backSlash1 ? slash1 : backSlash1;
            int start2 = slash2 > backSlash2 ? slash2 : backSlash2;
            int start12 = start1 < 0 ? 0 : start1 + 1;
            int start22 = start2 < 0 ? 0 : start2 + 1;
            int length = end1 - start12;
            if (end2 - start22 != length || !path1.regionMatches(true, start12, path2, start22, length)) {
                break;
            }
            result++;
            end1 = start12 - 1;
            end2 = start22 - 1;
        }
        return result;
    }

    private boolean matchEntries(long rowId, String data) {
        int len = this.mPlaylistEntries.size();
        boolean done = true;
        for (int i = 0; i < len; i++) {
            PlaylistEntry entry = this.mPlaylistEntries.get(i);
            if (entry.bestmatchlevel != Integer.MAX_VALUE) {
                done = false;
                if (data.equalsIgnoreCase(entry.path)) {
                    entry.bestmatchid = rowId;
                    entry.bestmatchlevel = Integer.MAX_VALUE;
                } else {
                    int matchLength = matchPaths(data, entry.path);
                    if (matchLength > entry.bestmatchlevel) {
                        entry.bestmatchid = rowId;
                        entry.bestmatchlevel = matchLength;
                    }
                }
            }
        }
        return done;
    }

    private void cachePlaylistEntry(String line, String playListDirectory) {
        boolean fullPath;
        PlaylistEntry entry = new PlaylistEntry(null);
        int entryLength = line.length();
        while (entryLength > 0 && Character.isWhitespace(line.charAt(entryLength - 1))) {
            entryLength--;
        }
        if (entryLength < 3) {
            return;
        }
        if (entryLength < line.length()) {
            line = line.substring(0, entryLength);
        }
        char ch1 = line.charAt(0);
        if (ch1 == '/') {
            fullPath = true;
        } else if (Character.isLetter(ch1) && line.charAt(1) == ':') {
            fullPath = line.charAt(2) == '\\';
        } else {
            fullPath = false;
        }
        if (!fullPath) {
            line = playListDirectory + line;
        }
        entry.path = line;
        this.mPlaylistEntries.add(entry);
    }

    private void processCachedPlaylist(Cursor fileList, ContentValues values, Uri playlistUri) {
        fileList.moveToPosition(-1);
        while (fileList.moveToNext()) {
            long rowId = fileList.getLong(0);
            String data = fileList.getString(1);
            if (matchEntries(rowId, data)) {
                break;
            }
        }
        int len = this.mPlaylistEntries.size();
        int index = 0;
        for (int i = 0; i < len; i++) {
            PlaylistEntry entry = this.mPlaylistEntries.get(i);
            if (entry.bestmatchlevel > 0) {
                try {
                    values.clear();
                    values.put("play_order", Integer.valueOf(index));
                    values.put("audio_id", Long.valueOf(entry.bestmatchid));
                    this.mMediaProvider.insert(playlistUri, values);
                    index++;
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException in MediaScanner.processCachedPlaylist()", e);
                    return;
                }
            }
        }
        this.mPlaylistEntries.clear();
    }

    private void processM3uPlayList(String path, String playListDirectory, Uri uri, ContentValues values, Cursor fileList) throws Throwable {
        BufferedReader reader = null;
        try {
            try {
                File f = new File(path);
                if (f.exists()) {
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream(f)), 8192);
                    try {
                        this.mPlaylistEntries.clear();
                        for (String line = reader2.readLine(); line != null; line = reader2.readLine()) {
                            if (line.length() > 0 && line.charAt(0) != '#') {
                                cachePlaylistEntry(line, playListDirectory);
                            }
                        }
                        processCachedPlaylist(fileList, values, uri);
                        reader = reader2;
                    } catch (IOException e) {
                        e = e;
                        reader = reader2;
                        Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e);
                        if (reader == null) {
                            return;
                        }
                        try {
                            reader.close();
                            return;
                        } catch (IOException e2) {
                            Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e2);
                            return;
                        }
                    } catch (Throwable th) {
                        th = th;
                        reader = reader2;
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e3) {
                                Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e3);
                            }
                        }
                        throw th;
                    }
                }
                if (reader == null) {
                    return;
                }
                try {
                    reader.close();
                } catch (IOException e4) {
                    Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e4);
                }
            } catch (IOException e5) {
                e = e5;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private void processPlsPlayList(String path, String playListDirectory, Uri uri, ContentValues values, Cursor fileList) throws Throwable {
        int equals;
        BufferedReader reader = null;
        try {
            try {
                File f = new File(path);
                if (f.exists()) {
                    BufferedReader reader2 = new BufferedReader(new InputStreamReader(new FileInputStream(f)), 8192);
                    try {
                        this.mPlaylistEntries.clear();
                        for (String line = reader2.readLine(); line != null; line = reader2.readLine()) {
                            if (line.startsWith("File") && (equals = line.indexOf(61)) > 0) {
                                cachePlaylistEntry(line.substring(equals + 1), playListDirectory);
                            }
                        }
                        processCachedPlaylist(fileList, values, uri);
                        reader = reader2;
                    } catch (IOException e) {
                        e = e;
                        reader = reader2;
                        Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e);
                        if (reader == null) {
                            return;
                        }
                        try {
                            reader.close();
                            return;
                        } catch (IOException e2) {
                            Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e2);
                            return;
                        }
                    } catch (Throwable th) {
                        th = th;
                        reader = reader2;
                        if (reader != null) {
                            try {
                                reader.close();
                            } catch (IOException e3) {
                                Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e3);
                            }
                        }
                        throw th;
                    }
                }
                if (reader == null) {
                    return;
                }
                try {
                    reader.close();
                } catch (IOException e4) {
                    Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e4);
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e5) {
            e = e5;
        }
    }

    class WplHandler implements ElementListener {
        final ContentHandler handler;
        String playListDirectory;

        public WplHandler(String playListDirectory, Uri uri, Cursor fileList) {
            this.playListDirectory = playListDirectory;
            RootElement root = new RootElement("smil");
            Element body = root.getChild(TtmlUtils.TAG_BODY);
            Element seq = body.getChild("seq");
            Element media = seq.getChild(MediaStore.AUTHORITY);
            media.setElementListener(this);
            this.handler = root.getContentHandler();
        }

        @Override
        public void start(Attributes attributes) {
            String path = attributes.getValue(ProxyInfo.LOCAL_EXCL_LIST, "src");
            if (path == null) {
                return;
            }
            MediaScanner.this.cachePlaylistEntry(path, this.playListDirectory);
        }

        @Override
        public void end() {
        }

        ContentHandler getContentHandler() {
            return this.handler;
        }
    }

    private void processWplPlayList(String path, String playListDirectory, Uri uri, ContentValues values, Cursor fileList) throws Throwable {
        FileInputStream fis = null;
        try {
            try {
                File f = new File(path);
                if (f.exists()) {
                    FileInputStream fis2 = new FileInputStream(f);
                    try {
                        this.mPlaylistEntries.clear();
                        Xml.parse(fis2, Xml.findEncodingByName("UTF-8"), new WplHandler(playListDirectory, uri, fileList).getContentHandler());
                        processCachedPlaylist(fileList, values, uri);
                        fis = fis2;
                    } catch (IOException e) {
                        e = e;
                        fis = fis2;
                        e.printStackTrace();
                        if (fis != null) {
                            try {
                                fis.close();
                                return;
                            } catch (IOException e2) {
                                Log.e(TAG, "IOException in MediaScanner.processWplPlayList()", e2);
                                return;
                            }
                        }
                        return;
                    } catch (SAXException e3) {
                        e = e3;
                        fis = fis2;
                        e.printStackTrace();
                        if (fis != null) {
                            try {
                                fis.close();
                                return;
                            } catch (IOException e4) {
                                Log.e(TAG, "IOException in MediaScanner.processWplPlayList()", e4);
                                return;
                            }
                        }
                        return;
                    } catch (Throwable th) {
                        th = th;
                        fis = fis2;
                        if (fis != null) {
                            try {
                                fis.close();
                            } catch (IOException e5) {
                                Log.e(TAG, "IOException in MediaScanner.processWplPlayList()", e5);
                            }
                        }
                        throw th;
                    }
                }
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e6) {
                        Log.e(TAG, "IOException in MediaScanner.processWplPlayList()", e6);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e7) {
            e = e7;
        } catch (SAXException e8) {
            e = e8;
        }
    }

    private void processPlayList(FileEntry entry, Cursor fileList) throws Throwable {
        Uri membersUri;
        String path = entry.mPath;
        ContentValues values = new ContentValues();
        int lastSlash = path.lastIndexOf(47);
        if (lastSlash < 0) {
            throw new IllegalArgumentException("bad path " + path);
        }
        long rowId = entry.mRowId;
        String name = values.getAsString("name");
        if (name == null && (name = values.getAsString("title")) == null) {
            int lastDot = path.lastIndexOf(46);
            name = lastDot < 0 ? path.substring(lastSlash + 1) : path.substring(lastSlash + 1, lastDot);
        }
        values.put("name", name);
        values.put("date_modified", Long.valueOf(entry.mLastModified));
        if (rowId == 0) {
            values.put("_data", path);
            Uri uri = this.mMediaProvider.insert(this.mPlaylistsUri, values);
            ContentUris.parseId(uri);
            membersUri = Uri.withAppendedPath(uri, "members");
        } else {
            Uri uri2 = ContentUris.withAppendedId(this.mPlaylistsUri, rowId);
            this.mMediaProvider.update(uri2, values, null, null);
            membersUri = Uri.withAppendedPath(uri2, "members");
            this.mMediaProvider.delete(membersUri, null, null);
        }
        String playListDirectory = path.substring(0, lastSlash + 1);
        MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
        int fileType = mediaFileType == null ? 0 : mediaFileType.fileType;
        if (fileType == 501) {
            processM3uPlayList(path, playListDirectory, membersUri, values, fileList);
        } else if (fileType == 502) {
            processPlsPlayList(path, playListDirectory, membersUri, values, fileList);
        } else {
            if (fileType != 503) {
                return;
            }
            processWplPlayList(path, playListDirectory, membersUri, values, fileList);
        }
    }

    private void processPlayLists() throws RemoteException {
        Cursor fileList = null;
        try {
            try {
                fileList = this.mMediaProvider.query(this.mFilesUri, FILES_PRESCAN_PROJECTION, "media_type=2", null, null, null);
                for (FileEntry entry : this.mPlayLists) {
                    if (entry.mLastModifiedChanged) {
                        processPlayList(entry, fileList);
                    }
                }
                if (fileList == null) {
                    return;
                }
                fileList.close();
            } catch (RemoteException e1) {
                Log.e(TAG, "processPlayLists: RemoteException!", e1);
                if (fileList == null) {
                    return;
                }
                fileList.close();
            }
        } catch (Throwable th) {
            if (fileList != null) {
                fileList.close();
            }
            throw th;
        }
    }

    @Override
    public void close() {
        this.mCloseGuard.close();
        if (!this.mClosed.compareAndSet(false, true)) {
            return;
        }
        this.mMediaProvider.close();
        native_finalize();
    }

    protected void finalize() throws Throwable {
        try {
            this.mCloseGuard.warnIfOpen();
            close();
        } finally {
            super.finalize();
        }
    }

    private boolean isValueslessMimeType(String mimetype) {
        boolean valueless = false;
        if (MIME_APPLICATION_OCTET_STREAM.equalsIgnoreCase(mimetype)) {
            valueless = true;
            if (DEBUG) {
                Log.v(TAG, "isValueslessMimeType: mimetype=" + mimetype);
            }
        }
        return valueless;
    }

    public void preScanAll(String volume) {
        try {
            prescan(null, true);
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e);
        }
    }

    public void postScanAll(ArrayList<String> playlistFilePathList) {
        try {
            if (this.mProcessPlaylists) {
                for (String path : playlistFilePathList) {
                    FileEntry entry = makeEntryFor(path);
                    File file = new File(path);
                    long lastModified = file.lastModified();
                    long delta = entry != null ? lastModified - entry.mLastModified : 0L;
                    boolean wasModified = delta > 1 || delta < -1;
                    if (entry == null || wasModified) {
                        if (wasModified) {
                            entry.mLastModified = lastModified;
                        } else {
                            entry = new FileEntry(0L, path, lastModified, 0);
                        }
                        entry.mLastModifiedChanged = true;
                    }
                    this.mPlayLists.add(entry);
                }
                processPlayLists();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.postScanAll()", e);
        }
        Cursor c = null;
        try {
            try {
                Cursor c2 = this.mMediaProvider.query(this.mImagesUri.buildUpon().appendQueryParameter("force", WifiEnterpriseConfig.ENGINE_ENABLE).build(), ID_PROJECTION, null, null, null, null);
                if (c2 != null) {
                    c2.getCount();
                    c2.close();
                }
                c = this.mMediaProvider.query(this.mVideoUri.buildUpon().appendQueryParameter("force", WifiEnterpriseConfig.ENGINE_ENABLE).build(), ID_PROJECTION, null, null, null, null);
                if (c != null) {
                    c.getCount();
                    c.close();
                    c = null;
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in MediaScanner.postScanAll()", e2);
            if (c != null) {
                c.close();
            }
        }
        if (!DEBUG) {
            return;
        }
        Log.v(TAG, "postScanAll");
    }

    public ArrayList<String> scanFolders(Handler insertHanlder, String[] folders, String volume, boolean isSingelFile) {
        try {
            this.mPlayLists.clear();
            this.mMediaInserter = new MediaInserter(insertHanlder, 100);
            for (String path : folders) {
                if (isSingelFile) {
                    File file = new File(path);
                    long lastModifiedSeconds = file.lastModified() / 1000;
                    this.mClient.doScanFile(path, null, lastModifiedSeconds, file.length(), file.isDirectory(), false, isNoMediaPath(path));
                } else {
                    processDirectory(path, this.mClient);
                }
            }
            this.mMediaInserter.flushAll();
            this.mMediaInserter = null;
        } catch (SQLException e) {
            Log.e(TAG, "SQLException in MediaScanner.scan()", e);
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e2);
        } catch (UnsupportedOperationException e3) {
            Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e3);
        }
        return this.mPlaylistFilePathList;
    }

    public ArrayList<String> scanFolders(String[] folders, String volume, boolean isSingelFileOrEmptyFolder) {
        try {
            this.mPlayLists.clear();
            this.mMediaInserter = new MediaInserter(this.mMediaProvider, 500);
            for (String folder : folders) {
                File file = new File(folder);
                if (file.exists()) {
                    long lastModifiedSeconds = file.lastModified() / 1000;
                    this.mClient.doScanFile(folder, null, lastModifiedSeconds, file.length(), file.isDirectory(), false, isNoMediaPath(folder));
                }
                if (!isSingelFileOrEmptyFolder) {
                    processDirectory(folder, this.mClient);
                }
            }
            this.mMediaInserter.flushAll();
            this.mMediaInserter = null;
        } catch (SQLException e) {
            Log.e(TAG, "SQLException in MediaScanner.scan()", e);
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e2);
        } catch (UnsupportedOperationException e3) {
            Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e3);
        }
        return this.mPlaylistFilePathList;
    }

    public static boolean isStereoPhoto(String filePath) throws Throwable {
        if (filePath == null) {
            if (!DEBUG) {
                return false;
            }
            Log.d(TAG, "<isStereoPhoto> filePath is null!!");
            return false;
        }
        File srcFile = new File(filePath);
        if (!srcFile.exists()) {
            if (!DEBUG) {
                return false;
            }
            Log.d(TAG, "<isStereoPhoto> " + filePath + " not exists!!!");
            return false;
        }
        long start = System.currentTimeMillis();
        ArrayList<Section> sections = parseApp1Info(filePath);
        if (sections == null || sections.size() < 0) {
            if (!DEBUG) {
                return false;
            }
            Log.d(TAG, "<isStereoPhoto> " + filePath + ", no app1 sections");
            return false;
        }
        RandomAccessFile rafIn = null;
        try {
            try {
                RandomAccessFile rafIn2 = new RandomAccessFile(filePath, FullBackup.ROOT_TREE_TOKEN);
                for (int i = 0; i < sections.size(); i++) {
                    try {
                        Section section = sections.get(i);
                        if (isStereo(section, rafIn2)) {
                            if (DEBUG) {
                                Log.d(TAG, "<isStereoPhoto> " + filePath + " is stereo photo");
                            }
                            if (rafIn2 != null) {
                                try {
                                    rafIn2.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "<isStereoPhoto> IOException:", e);
                                }
                            }
                            if (DEBUG) {
                                Log.d(TAG, "<isStereoPhoto> <performance> costs(ms): " + (System.currentTimeMillis() - start));
                            }
                            return true;
                        }
                    } catch (FileNotFoundException e2) {
                        e = e2;
                        rafIn = rafIn2;
                        Log.e(TAG, "<isStereoPhoto> FileNotFoundException:", e);
                        if (rafIn != null) {
                            try {
                                rafIn.close();
                            } catch (IOException e3) {
                                Log.e(TAG, "<isStereoPhoto> IOException:", e3);
                            }
                        }
                        if (DEBUG) {
                            Log.d(TAG, "<isStereoPhoto> <performance> costs(ms): " + (System.currentTimeMillis() - start));
                        }
                        return false;
                    } catch (IllegalArgumentException e4) {
                        e = e4;
                        rafIn = rafIn2;
                        Log.e(TAG, "<isStereoPhoto> IllegalArgumentException:", e);
                        if (rafIn != null) {
                            try {
                                rafIn.close();
                            } catch (IOException e5) {
                                Log.e(TAG, "<isStereoPhoto> IOException:", e5);
                            }
                        }
                        if (DEBUG) {
                            Log.d(TAG, "<isStereoPhoto> <performance> costs(ms): " + (System.currentTimeMillis() - start));
                        }
                        return false;
                    } catch (Throwable th) {
                        th = th;
                        rafIn = rafIn2;
                        if (rafIn != null) {
                            try {
                                rafIn.close();
                            } catch (IOException e6) {
                                Log.e(TAG, "<isStereoPhoto> IOException:", e6);
                            }
                        }
                        if (DEBUG) {
                            Log.d(TAG, "<isStereoPhoto> <performance> costs(ms): " + (System.currentTimeMillis() - start));
                        }
                        throw th;
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "<isStereoPhoto> " + filePath + " is not stereo photo");
                }
                if (rafIn2 != null) {
                    try {
                        rafIn2.close();
                    } catch (IOException e7) {
                        Log.e(TAG, "<isStereoPhoto> IOException:", e7);
                    }
                }
                if (DEBUG) {
                    Log.d(TAG, "<isStereoPhoto> <performance> costs(ms): " + (System.currentTimeMillis() - start));
                }
                return false;
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (FileNotFoundException e8) {
            e = e8;
        } catch (IllegalArgumentException e9) {
            e = e9;
        }
    }

    private static boolean isStereo(Section section, RandomAccessFile rafIn) {
        try {
            if (section.mIsXmpMain) {
                rafIn.seek(section.mOffset + 2);
                int len = rafIn.readUnsignedShort() - 2;
                rafIn.skipBytes(XMP_HEADER_START.length());
                byte[] xmpBuffer = new byte[len - XMP_HEADER_START.length()];
                rafIn.read(xmpBuffer, 0, xmpBuffer.length);
                String xmpContent = new String(xmpBuffer);
                if (xmpContent == null) {
                    if (DEBUG) {
                        Log.d(TAG, "<isStereo> xmpContent is null");
                    }
                    return false;
                }
                if (xmpContent.contains(MTK_REFOCUS_PREFIX)) {
                    return true;
                }
            }
            return false;
        } catch (IOException e) {
            Log.e(TAG, "<isStereo> IOException:", e);
            return false;
        }
    }

    private static ArrayList<Section> parseApp1Info(String filePath) throws Throwable {
        RandomAccessFile raf;
        RandomAccessFile randomAccessFile = null;
        try {
            try {
                raf = new RandomAccessFile(filePath, FullBackup.ROOT_TREE_TOKEN);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            if (raf.readUnsignedShort() != SOI) {
                if (DEBUG) {
                    Log.d(TAG, "<parseApp1Info> error, find no SOI");
                }
                ArrayList<Section> arrayList = new ArrayList<>();
                if (raf != null) {
                    try {
                        raf.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "<parseApp1Info> IOException, path " + filePath, e2);
                    }
                }
                return arrayList;
            }
            ArrayList<Section> sections = new ArrayList<>();
            while (true) {
                int value = raf.readUnsignedShort();
                if (value == -1 || value == SOS) {
                    break;
                }
                long offset = raf.getFilePointer() - 2;
                int length = raf.readUnsignedShort();
                if (value == APP1) {
                    Section section = new Section(value, offset, length);
                    long currentPos = raf.getFilePointer();
                    Section section2 = checkIfMainXmpInApp1(raf, section);
                    if (section2 != null && section2.mIsXmpMain) {
                        sections.add(section2);
                        break;
                    }
                    raf.seek(currentPos);
                }
                raf.skipBytes(length - 2);
            }
            if (raf != null) {
                try {
                    raf.close();
                } catch (IOException e3) {
                    Log.e(TAG, "<parseApp1Info> IOException, path " + filePath, e3);
                }
            }
            return sections;
        } catch (IOException e4) {
            e = e4;
            randomAccessFile = raf;
            Log.e(TAG, "<parseApp1Info> IOException, path " + filePath, e);
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e5) {
                    Log.e(TAG, "<parseApp1Info> IOException, path " + filePath, e5);
                }
            }
            return null;
        } catch (Throwable th2) {
            th = th2;
            randomAccessFile = raf;
            if (randomAccessFile != null) {
                try {
                    randomAccessFile.close();
                } catch (IOException e6) {
                    Log.e(TAG, "<parseApp1Info> IOException, path " + filePath, e6);
                }
            }
            throw th;
        }
    }

    private static Section checkIfMainXmpInApp1(RandomAccessFile raf, Section section) {
        if (section == null) {
            if (DEBUG) {
                Log.d(TAG, "<checkIfMainXmpInApp1> section is null!!!");
            }
            return null;
        }
        try {
            if (section.mMarker == APP1) {
                raf.seek(section.mOffset + 4);
                byte[] buffer = new byte[XMP_EXT_MAIN_HEADER1.length()];
                raf.read(buffer, 0, buffer.length);
                String str = new String(buffer, 0, XMP_HEADER_START.length());
                try {
                    if (XMP_HEADER_START.equals(str)) {
                        section.mIsXmpMain = true;
                    }
                } catch (UnsupportedEncodingException e) {
                    e = e;
                    Log.e(TAG, "<checkIfMainXmpInApp1> UnsupportedEncodingException" + e);
                    return null;
                } catch (IOException e2) {
                    e = e2;
                    Log.e(TAG, "<checkIfMainXmpInApp1> IOException" + e);
                    return null;
                }
            }
            return section;
        } catch (UnsupportedEncodingException e3) {
            e = e3;
        } catch (IOException e4) {
            e = e4;
        }
    }

    private static class Section {
        public boolean mIsXmpMain;
        public int mLength;
        public int mMarker;
        public long mOffset;

        public Section(int marker, long offset, int length) {
            this.mMarker = marker;
            this.mOffset = offset;
            this.mLength = length;
        }
    }
}
