package android.media;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.IContentProvider;
import android.database.Cursor;
import android.database.SQLException;
import android.drm.DrmManagerClient;
import android.graphics.BitmapFactory;
import android.media.MediaFile;
import android.mtp.MtpConstants;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemProperties;
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
import com.android.internal.R;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class MediaScanner {
    private static final String ALARMS_DIR = "/alarms/";
    private static final int DATE_MODIFIED_PLAYLISTS_COLUMN_INDEX = 2;
    private static final String DEFAULT_RINGTONE_PROPERTY_PREFIX = "ro.config.";
    private static final boolean ENABLE_BULK_INSERTS = true;
    private static final int FILES_PRESCAN_DATE_MODIFIED_COLUMN_INDEX = 3;
    private static final int FILES_PRESCAN_FORMAT_COLUMN_INDEX = 2;
    private static final int FILES_PRESCAN_ID_COLUMN_INDEX = 0;
    private static final int FILES_PRESCAN_PATH_COLUMN_INDEX = 1;
    private static final String[] FILES_PRESCAN_PROJECTION;
    private static final String[] ID3_GENRES;
    private static final int ID_PLAYLISTS_COLUMN_INDEX = 0;
    private static final String[] ID_PROJECTION;
    private static final String MUSIC_DIR = "/music/";
    private static final String NOTIFICATIONS_DIR = "/notifications/";
    private static final int PATH_PLAYLISTS_COLUMN_INDEX = 1;
    private static final String[] PLAYLIST_MEMBERS_PROJECTION;
    private static final String PODCAST_DIR = "/podcasts/";
    private static final String RINGTONES_DIR = "/ringtones/";
    private static final String TAG = "MediaScanner";
    private static HashMap<String, String> mMediaPaths;
    private static HashMap<String, String> mNoMediaPaths;
    private Uri mAudioUri;
    private boolean mCaseInsensitivePaths;
    private Context mContext;
    private String mDefaultAlarmAlertFilename;
    private boolean mDefaultAlarmSet;
    private String mDefaultNotificationFilename;
    private boolean mDefaultNotificationSet;
    private String mDefaultRingtoneFilename;
    private boolean mDefaultRingtoneSet;
    private final boolean mExternalIsEmulated;
    private final String mExternalStoragePath;
    private Uri mFilesUri;
    private Uri mFilesUriNoNotify;
    private Uri mImagesUri;
    private MediaInserter mMediaInserter;
    private IContentProvider mMediaProvider;
    private int mMtpObjectHandle;
    private long mNativeContext;
    private int mOriginalCount;
    private String mPackageName;
    private ArrayList<FileEntry> mPlayLists;
    private Uri mPlaylistsUri;
    private boolean mProcessGenres;
    private boolean mProcessPlaylists;
    private Uri mThumbsUri;
    private Uri mVideoUri;
    private boolean mWasEmptyPriorToScan = false;
    private final BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
    private ArrayList<PlaylistEntry> mPlaylistEntries = new ArrayList<>();
    private DrmManagerClient mDrmManagerClient = null;
    private final MyMediaScannerClient mClient = new MyMediaScannerClient();

    private final native void native_finalize();

    private static final native void native_init();

    private final native void native_setup();

    private native void processDirectory(String str, MediaScannerClient mediaScannerClient);

    private native void processFile(String str, String str2, MediaScannerClient mediaScannerClient);

    public native byte[] extractAlbumArt(FileDescriptor fileDescriptor);

    public native void setLocale(String str);

    static {
        System.loadLibrary("media_jni");
        native_init();
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

        private PlaylistEntry() {
        }
    }

    public MediaScanner(Context c) {
        native_setup();
        this.mContext = c;
        this.mPackageName = c.getPackageName();
        this.mBitmapOptions.inSampleSize = 1;
        this.mBitmapOptions.inJustDecodeBounds = true;
        setDefaultRingtoneFileNames();
        this.mExternalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
        this.mExternalIsEmulated = Environment.isExternalStorageEmulated();
    }

    private void setDefaultRingtoneFileNames() {
        this.mDefaultRingtoneFilename = SystemProperties.get("ro.config.ringtone");
        this.mDefaultNotificationFilename = SystemProperties.get("ro.config.notification_sound");
        this.mDefaultAlarmAlertFilename = SystemProperties.get("ro.config.alarm_alert");
    }

    private boolean isDrmEnabled() {
        String prop = SystemProperties.get("drm.service.enabled");
        return prop != null && prop.equals("true");
    }

    private class MyMediaScannerClient implements MediaScannerClient {
        private String mAlbum;
        private String mAlbumArtist;
        private String mArtist;
        private int mCompilation;
        private String mComposer;
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
        private String mTitle;
        private int mTrack;
        private int mWidth;
        private String mWriter;
        private int mYear;

        private MyMediaScannerClient() {
        }

        public FileEntry beginFile(String path, String mimeType, long lastModified, long fileSize, boolean isDirectory, boolean noMedia) {
            MediaFile.MediaFileType mediaFileType;
            this.mMimeType = mimeType;
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
                if (this.mFileType == 0 && (mediaFileType = MediaFile.getFileType(path)) != null) {
                    this.mFileType = mediaFileType.fileType;
                    if (this.mMimeType == null) {
                        this.mMimeType = mediaFileType.mimeType;
                    }
                }
                if (MediaScanner.this.isDrmEnabled() && MediaFile.isDrmFileType(this.mFileType)) {
                    this.mFileType = getFileTypeFromDrm(path);
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
            return entry;
        }

        @Override
        public void scanFile(String path, long lastModified, long fileSize, boolean isDirectory, boolean noMedia) {
            doScanFile(path, null, lastModified, fileSize, isDirectory, false, noMedia);
        }

        public Uri doScanFile(String path, String mimeType, long lastModified, long fileSize, boolean isDirectory, boolean scanAlways, boolean noMedia) {
            try {
                FileEntry entry = beginFile(path, mimeType, lastModified, fileSize, isDirectory, noMedia);
                if (MediaScanner.this.mMtpObjectHandle != 0) {
                    entry.mRowId = 0L;
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
                boolean music = lowpath.indexOf(MediaScanner.MUSIC_DIR) > 0 || !(ringtones || notifications || alarms || podcasts);
                boolean isaudio = MediaFile.isAudioFileType(this.mFileType);
                boolean isvideo = MediaFile.isVideoFileType(this.mFileType);
                boolean isimage = MediaFile.isImageFileType(this.mFileType);
                if ((isaudio || isvideo || isimage) && MediaScanner.this.mExternalIsEmulated && path.startsWith(MediaScanner.this.mExternalStoragePath)) {
                    String directPath = Environment.getMediaStorageDirectory() + path.substring(MediaScanner.this.mExternalStoragePath.length());
                    File f = new File(directPath);
                    if (f.exists()) {
                        path = directPath;
                    }
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
            if (start != length) {
                int start2 = start + 1;
                char ch = s.charAt(start);
                if (ch >= '0' && ch <= '9') {
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
                return defaultValue;
            }
            return defaultValue;
        }

        @Override
        public void handleStringTag(String name, String value) {
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
            if (!name.equalsIgnoreCase(MediaStore.Audio.AudioColumns.COMPOSER) && !name.startsWith("composer;")) {
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
                } else if (name.equalsIgnoreCase("width")) {
                    this.mWidth = parseSubstring(value, 0, 0);
                    return;
                } else {
                    if (name.equalsIgnoreCase("height")) {
                        this.mHeight = parseSubstring(value, 0, 0);
                        return;
                    }
                    return;
                }
            }
            this.mComposer = value.trim();
        }

        private boolean convertGenreCode(String input, String expected) {
            String output = getGenreName(input);
            if (output.equals(expected)) {
                return true;
            }
            Log.d(MediaScanner.TAG, "'" + input + "' -> '" + output + "', expected '" + expected + "'");
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
                char charAfterNumber = i < length ? genreTagValue.charAt(i) : ' ';
                if ((parenthesized && charAfterNumber == ')') || (!parenthesized && Character.isWhitespace(charAfterNumber))) {
                    try {
                        short genreIndex = Short.parseShort(number.toString());
                        if (genreIndex >= 0) {
                            if (genreIndex < MediaScanner.ID3_GENRES.length && MediaScanner.ID3_GENRES[genreIndex] != null) {
                                return MediaScanner.ID3_GENRES[genreIndex];
                            }
                            if (genreIndex == 255) {
                                return null;
                            }
                            if (genreIndex < 255 && i + 1 < length) {
                                if (parenthesized && charAfterNumber == ')') {
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
                    }
                }
            }
            return genreTagValue;
        }

        private void processImageFile(String path) {
            try {
                MediaScanner.this.mBitmapOptions.outWidth = 0;
                MediaScanner.this.mBitmapOptions.outHeight = 0;
                BitmapFactory.decodeFile(path, MediaScanner.this.mBitmapOptions);
                this.mWidth = MediaScanner.this.mBitmapOptions.outWidth;
                this.mHeight = MediaScanner.this.mBitmapOptions.outHeight;
            } catch (Throwable th) {
            }
        }

        @Override
        public void setMimeType(String mimeType) {
            if (!"audio/mp4".equals(this.mMimeType) || !mimeType.startsWith("video")) {
                this.mMimeType = mimeType;
                this.mFileType = MediaFile.getFileTypeForMimeType(mimeType);
            }
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
            if (MediaFile.isAudioFileType(this.mFileType) && (rowId == 0 || MediaScanner.this.mMtpObjectHandle != 0)) {
                values.put(MediaStore.Audio.AudioColumns.IS_RINGTONE, Boolean.valueOf(ringtones));
                values.put(MediaStore.Audio.AudioColumns.IS_NOTIFICATION, Boolean.valueOf(notifications));
                values.put(MediaStore.Audio.AudioColumns.IS_ALARM, Boolean.valueOf(alarms));
                values.put(MediaStore.Audio.AudioColumns.IS_MUSIC, Boolean.valueOf(music));
                values.put(MediaStore.Audio.AudioColumns.IS_PODCAST, Boolean.valueOf(podcasts));
            } else if (this.mFileType == 31 && !this.mNoMedia) {
                ExifInterface exif = null;
                try {
                    ExifInterface exif2 = new ExifInterface(entry.mPath);
                    exif = exif2;
                } catch (IOException e) {
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
                        if (time2 != -1 && Math.abs((this.mLastModified * 1000) - time2) >= 86400000) {
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
                                degree = R.styleable.Theme_textUnderlineColor;
                                break;
                        }
                        values.put(MediaStore.Images.ImageColumns.ORIENTATION, Integer.valueOf(degree));
                    }
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
                if (MediaScanner.this.mWasEmptyPriorToScan) {
                    if (notifications && !MediaScanner.this.mDefaultNotificationSet) {
                        if (TextUtils.isEmpty(MediaScanner.this.mDefaultNotificationFilename) || doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultNotificationFilename)) {
                            needToSetSettings = true;
                        }
                    } else if (ringtones && !MediaScanner.this.mDefaultRingtoneSet) {
                        if (TextUtils.isEmpty(MediaScanner.this.mDefaultRingtoneFilename) || doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultRingtoneFilename)) {
                            needToSetSettings = true;
                        }
                    } else if (alarms && !MediaScanner.this.mDefaultAlarmSet && (TextUtils.isEmpty(MediaScanner.this.mDefaultAlarmAlertFilename) || doesPathHaveFilename(entry.mPath, MediaScanner.this.mDefaultAlarmAlertFilename))) {
                        needToSetSettings = true;
                    }
                }
                if (inserter == null || needToSetSettings) {
                    if (inserter != null) {
                        inserter.flushAll();
                    }
                    result = MediaScanner.this.mMediaProvider.insert(MediaScanner.this.mPackageName, tableUri, values);
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
                MediaScanner.this.mMediaProvider.update(MediaScanner.this.mPackageName, result, values, null, null);
            }
            if (needToSetSettings) {
                if (notifications) {
                    setSettingIfNotSet(Settings.System.NOTIFICATION_SOUND, tableUri, rowId);
                    MediaScanner.this.mDefaultNotificationSet = true;
                } else if (ringtones) {
                    setSettingIfNotSet(Settings.System.RINGTONE, tableUri, rowId);
                    MediaScanner.this.mDefaultRingtoneSet = true;
                } else if (alarms) {
                    setSettingIfNotSet(Settings.System.ALARM_ALERT, tableUri, rowId);
                    MediaScanner.this.mDefaultAlarmSet = true;
                }
            }
            return result;
        }

        private boolean doesPathHaveFilename(String path, String filename) {
            int pathFilenameStart = path.lastIndexOf(File.separatorChar) + 1;
            int filenameLength = filename.length();
            return path.regionMatches(pathFilenameStart, filename, 0, filenameLength) && pathFilenameStart + filenameLength == path.length();
        }

        private void setSettingIfNotSet(String settingName, Uri uri, long rowId) {
            String existingSettingValue = Settings.System.getString(MediaScanner.this.mContext.getContentResolver(), settingName);
            if (TextUtils.isEmpty(existingSettingValue)) {
                Settings.System.putString(MediaScanner.this.mContext.getContentResolver(), settingName, ContentUris.withAppendedId(uri, rowId).toString());
            }
        }

        private int getFileTypeFromDrm(String path) {
            if (MediaScanner.this.isDrmEnabled()) {
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
            return 0;
        }
    }

    private void prescan(String filePath, boolean prescanFiles) throws RemoteException {
        String where;
        String[] selectionArgs;
        Cursor c = null;
        if (this.mPlayLists == null) {
            this.mPlayLists = new ArrayList<>();
        } else {
            this.mPlayLists.clear();
        }
        if (filePath != null) {
            where = "_id>? AND _data=?";
            selectionArgs = new String[]{ProxyInfo.LOCAL_EXCL_LIST, filePath};
        } else {
            where = "_id>?";
            selectionArgs = new String[]{ProxyInfo.LOCAL_EXCL_LIST};
        }
        Uri.Builder builder = this.mFilesUri.buildUpon();
        builder.appendQueryParameter(MediaStore.PARAM_DELETE_DATA, "false");
        MediaBulkDeleter deleter = new MediaBulkDeleter(this.mMediaProvider, this.mPackageName, builder.build());
        if (prescanFiles) {
            long lastId = Long.MIN_VALUE;
            try {
                Uri limitUri = this.mFilesUri.buildUpon().appendQueryParameter("limit", "1000").build();
                this.mWasEmptyPriorToScan = true;
                while (true) {
                    selectionArgs[0] = ProxyInfo.LOCAL_EXCL_LIST + lastId;
                    if (c != null) {
                        c.close();
                    }
                    c = this.mMediaProvider.query(this.mPackageName, limitUri, FILES_PRESCAN_PROJECTION, where, selectionArgs, "_id", null);
                    if (c != null) {
                        int num = c.getCount();
                        if (num == 0) {
                            break;
                        }
                        this.mWasEmptyPriorToScan = false;
                        while (c.moveToNext()) {
                            long rowId = c.getLong(0);
                            String path = c.getString(1);
                            int format = c.getInt(2);
                            c.getLong(3);
                            lastId = rowId;
                            if (path != null && path.startsWith("/")) {
                                boolean exists = false;
                                try {
                                    exists = Os.access(path, OsConstants.F_OK);
                                } catch (ErrnoException e) {
                                }
                                if (!exists && !MtpConstants.isAbstractObject(format)) {
                                    MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
                                    int fileType = mediaFileType == null ? 0 : mediaFileType.fileType;
                                    if (!MediaFile.isPlayListFileType(fileType)) {
                                        deleter.delete(rowId);
                                        if (path.toLowerCase(Locale.US).endsWith("/.nomedia")) {
                                            deleter.flush();
                                            String parent = new File(path).getParent();
                                            this.mMediaProvider.call(this.mPackageName, MediaStore.UNHIDE_CALL, parent, null);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        break;
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
        this.mOriginalCount = 0;
        Cursor c2 = this.mMediaProvider.query(this.mPackageName, this.mImagesUri, ID_PROJECTION, null, null, null, null);
        if (c2 != null) {
            this.mOriginalCount = c2.getCount();
            c2.close();
        }
    }

    private boolean inScanDirectory(String path, String[] directories) {
        for (String directory : directories) {
            if (path.startsWith(directory)) {
                return true;
            }
        }
        return false;
    }

    private void pruneDeadThumbnailFiles() {
        HashSet<String> existingFiles = new HashSet<>();
        String[] files = new File("/sdcard/DCIM/.thumbnails").list();
        Cursor c = null;
        if (files == null) {
            files = new String[0];
        }
        for (String str : files) {
            String fullPathString = "/sdcard/DCIM/.thumbnails/" + str;
            existingFiles.add(fullPathString);
        }
        try {
            c = this.mMediaProvider.query(this.mPackageName, this.mThumbsUri, new String[]{"_data"}, null, null, null, null);
            Log.v(TAG, "pruneDeadThumbnailFiles... " + c);
            if (c != null && c.moveToFirst()) {
                do {
                    String fullPathString2 = c.getString(0);
                    existingFiles.remove(fullPathString2);
                } while (c.moveToNext());
            }
            for (String fileToDelete : existingFiles) {
                try {
                    new File(fileToDelete).delete();
                } catch (SecurityException e) {
                }
            }
            Log.v(TAG, "/pruneDeadThumbnailFiles... " + c);
            if (c != null) {
                c.close();
            }
        } catch (RemoteException e2) {
            if (c != null) {
                c.close();
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
    }

    static class MediaBulkDeleter {
        final Uri mBaseUri;
        final String mPackageName;
        final IContentProvider mProvider;
        StringBuilder whereClause = new StringBuilder();
        ArrayList<String> whereArgs = new ArrayList<>(100);

        public MediaBulkDeleter(IContentProvider provider, String packageName, Uri baseUri) {
            this.mProvider = provider;
            this.mPackageName = packageName;
            this.mBaseUri = baseUri;
        }

        public void delete(long id) throws RemoteException {
            if (this.whereClause.length() != 0) {
                this.whereClause.append(",");
            }
            this.whereClause.append("?");
            this.whereArgs.add(ProxyInfo.LOCAL_EXCL_LIST + id);
            if (this.whereArgs.size() > 100) {
                flush();
            }
        }

        public void flush() throws RemoteException {
            int size = this.whereArgs.size();
            if (size > 0) {
                String[] foo = new String[size];
                this.mProvider.delete(this.mPackageName, this.mBaseUri, "_id IN (" + this.whereClause.toString() + ")", (String[]) this.whereArgs.toArray(foo));
                this.whereClause.setLength(0);
                this.whereArgs.clear();
            }
        }
    }

    private void postscan(String[] directories) throws RemoteException {
        if (this.mProcessPlaylists) {
            processPlayLists();
        }
        if (this.mOriginalCount == 0 && this.mImagesUri.equals(MediaStore.Images.Media.getContentUri("external"))) {
            pruneDeadThumbnailFiles();
        }
        this.mPlayLists = null;
        this.mMediaProvider = null;
    }

    private void releaseResources() {
        if (this.mDrmManagerClient != null) {
            this.mDrmManagerClient.release();
            this.mDrmManagerClient = null;
        }
    }

    private void initialize(String volumeName) {
        this.mMediaProvider = this.mContext.getContentResolver().acquireProvider("media");
        this.mAudioUri = MediaStore.Audio.Media.getContentUri(volumeName);
        this.mVideoUri = MediaStore.Video.Media.getContentUri(volumeName);
        this.mImagesUri = MediaStore.Images.Media.getContentUri(volumeName);
        this.mThumbsUri = MediaStore.Images.Thumbnails.getContentUri(volumeName);
        this.mFilesUri = MediaStore.Files.getContentUri(volumeName);
        this.mFilesUriNoNotify = this.mFilesUri.buildUpon().appendQueryParameter("nonotify", WifiEnterpriseConfig.ENGINE_ENABLE).build();
        if (!volumeName.equals("internal")) {
            this.mProcessPlaylists = true;
            this.mProcessGenres = true;
            this.mPlaylistsUri = MediaStore.Audio.Playlists.getContentUri(volumeName);
            this.mCaseInsensitivePaths = true;
        }
    }

    public void scanDirectories(String[] directories, String volumeName) {
        try {
            System.currentTimeMillis();
            initialize(volumeName);
            prescan(null, true);
            System.currentTimeMillis();
            this.mMediaInserter = new MediaInserter(this.mMediaProvider, this.mPackageName, 500);
            for (String str : directories) {
                processDirectory(str, this.mClient);
            }
            this.mMediaInserter.flushAll();
            this.mMediaInserter = null;
            System.currentTimeMillis();
            postscan(directories);
            System.currentTimeMillis();
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.scan()", e);
        } catch (UnsupportedOperationException e2) {
            Log.e(TAG, "UnsupportedOperationException in MediaScanner.scan()", e2);
        } catch (SQLException e3) {
            Log.e(TAG, "SQLException in MediaScanner.scan()", e3);
        } finally {
            releaseResources();
        }
    }

    public Uri scanSingleFile(String path, String volumeName, String mimeType) {
        Uri uriDoScanFile;
        try {
            initialize(volumeName);
            prescan(path, true);
            File file = new File(path);
            if (!file.exists()) {
                uriDoScanFile = null;
            } else {
                long lastModifiedSeconds = file.lastModified() / 1000;
                uriDoScanFile = this.mClient.doScanFile(path, mimeType, lastModifiedSeconds, file.length(), false, true, isNoMediaPath(path));
                releaseResources();
            }
        } catch (RemoteException e) {
            Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e);
            uriDoScanFile = null;
        } finally {
            releaseResources();
        }
        return uriDoScanFile;
    }

    private static boolean isNoMediaFile(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            return false;
        }
        int lastSlash = path.lastIndexOf(47);
        if (lastSlash >= 0 && lastSlash + 2 < path.length()) {
            if (path.regionMatches(lastSlash + 1, "._", 0, 2)) {
                return true;
            }
            if (path.regionMatches(true, path.length() - 4, ".jpg", 0, 4)) {
                if (path.regionMatches(true, lastSlash + 1, "AlbumArt_{", 0, 10) || path.regionMatches(true, lastSlash + 1, "AlbumArt.", 0, 9)) {
                    return true;
                }
                int length = (path.length() - lastSlash) - 1;
                if (length == 17 && path.regionMatches(true, lastSlash + 1, "AlbumArtSmall", 0, 13)) {
                    return true;
                }
                if (length == 10 && path.regionMatches(true, lastSlash + 1, "Folder", 0, 6)) {
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
                    }
                    offset = slashIndex;
                }
                mMediaPaths.put(parent, ProxyInfo.LOCAL_EXCL_LIST);
            }
            return isNoMediaFile(path);
        }
    }

    public void scanMtpFile(String path, String volumeName, int objectHandle, int format) {
        initialize(volumeName);
        MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
        int fileType = mediaFileType == null ? 0 : mediaFileType.fileType;
        File file = new File(path);
        long lastModifiedSeconds = file.lastModified() / 1000;
        if (!MediaFile.isAudioFileType(fileType) && !MediaFile.isVideoFileType(fileType) && !MediaFile.isImageFileType(fileType) && !MediaFile.isPlayListFileType(fileType) && !MediaFile.isDrmFileType(fileType)) {
            ContentValues values = new ContentValues();
            values.put("_size", Long.valueOf(file.length()));
            values.put("date_modified", Long.valueOf(lastModifiedSeconds));
            try {
                String[] whereArgs = {Integer.toString(objectHandle)};
                this.mMediaProvider.update(this.mPackageName, MediaStore.Files.getMtpObjectsUri(volumeName), values, "_id=?", whereArgs);
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
                        fileList = this.mMediaProvider.query(this.mPackageName, this.mFilesUri, FILES_PRESCAN_PROJECTION, null, null, null, null);
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
            } catch (Throwable th) {
                this.mMtpObjectHandle = 0;
                if (0 != 0) {
                    fileList.close();
                }
                releaseResources();
                throw th;
            }
        } catch (RemoteException e2) {
            Log.e(TAG, "RemoteException in MediaScanner.scanFile()", e2);
            this.mMtpObjectHandle = 0;
            if (0 != 0) {
                fileList.close();
            }
            releaseResources();
        }
    }

    FileEntry makeEntryFor(String path) {
        Cursor c = null;
        try {
            String[] selectionArgs = {path};
            c = this.mMediaProvider.query(this.mPackageName, this.mFilesUriNoNotify, FILES_PRESCAN_PROJECTION, "_data=?", selectionArgs, null, null);
            if (c.moveToFirst()) {
                long rowId = c.getLong(0);
                int format = c.getInt(2);
                long lastModified = c.getLong(3);
                FileEntry fileEntry = new FileEntry(rowId, path, lastModified, format);
                if (c == null) {
                    return fileEntry;
                }
                c.close();
                return fileEntry;
            }
            if (c != null) {
                c.close();
            }
        } catch (RemoteException e) {
            if (c != null) {
                c.close();
            }
        } catch (Throwable th) {
            if (c != null) {
                c.close();
            }
            throw th;
        }
        return null;
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
        boolean fullPath = false;
        PlaylistEntry entry = new PlaylistEntry();
        int entryLength = line.length();
        while (entryLength > 0 && Character.isWhitespace(line.charAt(entryLength - 1))) {
            entryLength--;
        }
        if (entryLength >= 3) {
            if (entryLength < line.length()) {
                line = line.substring(0, entryLength);
            }
            char ch1 = line.charAt(0);
            if (ch1 == '/' || (Character.isLetter(ch1) && line.charAt(1) == ':' && line.charAt(2) == '\\')) {
                fullPath = true;
            }
            if (!fullPath) {
                line = playListDirectory + line;
            }
            entry.path = line;
            this.mPlaylistEntries.add(entry);
        }
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
                    this.mMediaProvider.insert(this.mPackageName, playlistUri, values);
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
                        if (reader != null) {
                            try {
                                reader.close();
                                return;
                            } catch (IOException e2) {
                                Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e2);
                                return;
                            }
                        }
                        return;
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
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                        Log.e(TAG, "IOException in MediaScanner.processM3uPlayList()", e4);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
            }
        } catch (IOException e5) {
            e = e5;
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
                        if (reader != null) {
                            try {
                                reader.close();
                                return;
                            } catch (IOException e2) {
                                Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e2);
                                return;
                            }
                        }
                        return;
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
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e4) {
                        Log.e(TAG, "IOException in MediaScanner.processPlsPlayList()", e4);
                    }
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
            Element media = seq.getChild("media");
            media.setElementListener(this);
            this.handler = root.getContentHandler();
        }

        @Override
        public void start(Attributes attributes) {
            String path = attributes.getValue(ProxyInfo.LOCAL_EXCL_LIST, "src");
            if (path != null) {
                MediaScanner.this.cachePlaylistEntry(path, this.playListDirectory);
            }
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
            Uri uri = this.mMediaProvider.insert(this.mPackageName, this.mPlaylistsUri, values);
            ContentUris.parseId(uri);
            membersUri = Uri.withAppendedPath(uri, "members");
        } else {
            Uri uri2 = ContentUris.withAppendedId(this.mPlaylistsUri, rowId);
            this.mMediaProvider.update(this.mPackageName, uri2, values, null, null);
            membersUri = Uri.withAppendedPath(uri2, "members");
            this.mMediaProvider.delete(this.mPackageName, membersUri, null, null);
        }
        String playListDirectory = path.substring(0, lastSlash + 1);
        MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
        int fileType = mediaFileType == null ? 0 : mediaFileType.fileType;
        if (fileType == 41) {
            processM3uPlayList(path, playListDirectory, membersUri, values, fileList);
        } else if (fileType == 42) {
            processPlsPlayList(path, playListDirectory, membersUri, values, fileList);
        } else if (fileType == 43) {
            processWplPlayList(path, playListDirectory, membersUri, values, fileList);
        }
    }

    private void processPlayLists() throws RemoteException {
        Cursor fileList = null;
        try {
            fileList = this.mMediaProvider.query(this.mPackageName, this.mFilesUri, FILES_PRESCAN_PROJECTION, "media_type=2", null, null, null);
            for (FileEntry entry : this.mPlayLists) {
                if (entry.mLastModifiedChanged) {
                    processPlayList(entry, fileList);
                }
            }
            if (fileList != null) {
                fileList.close();
            }
        } catch (RemoteException e) {
            if (fileList != null) {
                fileList.close();
            }
        } catch (Throwable th) {
            if (fileList != null) {
                fileList.close();
            }
            throw th;
        }
    }

    public void release() {
        native_finalize();
    }

    protected void finalize() {
        this.mContext.getContentResolver().releaseProvider(this.mMediaProvider);
        native_finalize();
    }
}
