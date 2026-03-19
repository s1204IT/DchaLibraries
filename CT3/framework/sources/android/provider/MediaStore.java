package android.provider;

import android.app.backup.FullBackup;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.MediaFile;
import android.media.MiniThumbFile;
import android.media.ThumbnailUtils;
import android.net.ProxyInfo;
import android.net.Uri;
import android.net.wifi.WifiEnterpriseConfig;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class MediaStore {
    public static final String ACTION_IMAGE_CAPTURE = "android.media.action.IMAGE_CAPTURE";
    public static final String ACTION_IMAGE_CAPTURE_SECURE = "android.media.action.IMAGE_CAPTURE_SECURE";
    public static final String ACTION_MTP_SESSION_END = "android.provider.action.MTP_SESSION_END";
    public static final String ACTION_VIDEO_CAPTURE = "android.media.action.VIDEO_CAPTURE";
    public static final String AUTHORITY = "media";
    private static final String CONTENT_AUTHORITY_SLASH = "content://media/";
    public static final String EXTRA_DURATION_LIMIT = "android.intent.extra.durationLimit";
    public static final String EXTRA_FINISH_ON_COMPLETION = "android.intent.extra.finishOnCompletion";
    public static final String EXTRA_FULL_SCREEN = "android.intent.extra.fullScreen";
    public static final String EXTRA_LOOP_PLAYBACK = "android.intent.extra.loopPlayback";
    public static final String EXTRA_MEDIA_ALBUM = "android.intent.extra.album";
    public static final String EXTRA_MEDIA_ARTIST = "android.intent.extra.artist";
    public static final String EXTRA_MEDIA_FOCUS = "android.intent.extra.focus";
    public static final String EXTRA_MEDIA_GENRE = "android.intent.extra.genre";
    public static final String EXTRA_MEDIA_PLAYLIST = "android.intent.extra.playlist";
    public static final String EXTRA_MEDIA_RADIO_CHANNEL = "android.intent.extra.radio_channel";
    public static final String EXTRA_MEDIA_TITLE = "android.intent.extra.title";
    public static final String EXTRA_OUTPUT = "output";
    public static final String EXTRA_SCREEN_ORIENTATION = "android.intent.extra.screenOrientation";
    public static final String EXTRA_SHOW_ACTION_ICONS = "android.intent.extra.showActionIcons";
    public static final String EXTRA_SIZE_LIMIT = "android.intent.extra.sizeLimit";
    public static final String EXTRA_URI_LIST = "android.intent.extra.uriList";
    public static final String EXTRA_VIDEO_QUALITY = "android.intent.extra.videoQuality";
    public static final String INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH = "android.media.action.MEDIA_PLAY_FROM_SEARCH";
    public static final String INTENT_ACTION_MEDIA_SEARCH = "android.intent.action.MEDIA_SEARCH";

    @Deprecated
    public static final String INTENT_ACTION_MUSIC_PLAYER = "android.intent.action.MUSIC_PLAYER";
    public static final String INTENT_ACTION_STILL_IMAGE_CAMERA = "android.media.action.STILL_IMAGE_CAMERA";
    public static final String INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE = "android.media.action.STILL_IMAGE_CAMERA_SECURE";
    public static final String INTENT_ACTION_TEXT_OPEN_FROM_SEARCH = "android.media.action.TEXT_OPEN_FROM_SEARCH";
    public static final String INTENT_ACTION_VIDEO_CAMERA = "android.media.action.VIDEO_CAMERA";
    public static final String INTENT_ACTION_VIDEO_PLAY_FROM_SEARCH = "android.media.action.VIDEO_PLAY_FROM_SEARCH";
    public static final String MEDIA_IGNORE_FILENAME = ".nomedia";
    public static final String MEDIA_SCANNER_VOLUME = "volume";
    public static final String META_DATA_STILL_IMAGE_CAMERA_PREWARM_SERVICE = "android.media.still_image_camera_preview_service";
    public static final String MTP_TRANSFER_FILE_PATH = "mtp_transfer_file_path";
    public static final String PARAM_DELETE_DATA = "deletedata";
    private static final String TAG = "MediaStore";
    public static final String UNHIDE_CALL = "unhide";
    public static final String UNKNOWN_STRING = "<unknown>";

    public interface MediaColumns extends BaseColumns {
        public static final String DATA = "_data";
        public static final String DATE_ADDED = "date_added";
        public static final String DATE_MODIFIED = "date_modified";
        public static final String DISPLAY_NAME = "_display_name";
        public static final String DRM_CONTENT_DESCRIPTION = "drm_content_description";
        public static final String DRM_CONTENT_NAME = "drm_content_name";
        public static final String DRM_CONTENT_URI = "drm_content_uri";
        public static final String DRM_CONTENT_VENDOR = "drm_content_vendor";
        public static final String DRM_DATA_LEN = "drm_dataLen";
        public static final String DRM_ICON_URI = "drm_icon_uri";
        public static final String DRM_METHOD = "drm_method";
        public static final String DRM_OFFSET = "drm_offset";
        public static final String DRM_RIGHTS_ISSUER = "drm_rights_issuer";
        public static final String HEIGHT = "height";
        public static final String IS_DRM = "is_drm";
        public static final String MEDIA_SCANNER_NEW_OBJECT_ID = "media_scanner_new_object_id";
        public static final String MIME_TYPE = "mime_type";
        public static final String SIZE = "_size";
        public static final String TITLE = "title";
        public static final String WIDTH = "width";
    }

    public static final class Streaming {

        public static final class OmaRtspSetting implements OmaRtspSettingColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/omartspsetting";
            public static final Uri CONTENT_URI = Uri.parse("content://media/internal/streaming/omartspsetting");
        }

        public interface OmaRtspSettingColumns {
            public static final String MAX_BANDWIDTH = "mtk_rtsp_max_bandwidth";
            public static final String MAX_UDP_PORT = "mtk_rtsp_max_udp_port";
            public static final String MIN_UDP_PORT = "mtk_rtsp_min_udp_port";
            public static final String NAME = "mtk_rtsp_name";
            public static final String NETINFO = "mtk_rtsp_netinfo";
            public static final String PROVIDER_ID = "mtk_rtsp_provider_id";
            public static final String SIM_ID = "mtk_rtsp_sim_id";
            public static final String TO_NAPID = "mtk_rtsp_to_napid";
            public static final String TO_PROXY = "mtk_rtsp_to_proxy";
        }

        public static final class Setting implements OmaRtspSettingColumns, SettingColumns {
        }

        public interface SettingColumns {
            public static final String HTTP_PROXY_ENABLED = "mtk_http_proxy_enabled";
            public static final String HTTP_PROXY_HOST = "mtk_http_proxy_host";
            public static final String HTTP_PROXY_PORT = "mtk_http_proxy_port";
            public static final String RTSP_PROXY_ENABLED = "mtk_rtsp_proxy_enabled";
            public static final String RTSP_PROXY_HOST = "mtk_rtsp_proxy_host";
            public static final String RTSP_PROXY_PORT = "mtk_rtsp_proxy_port";
        }
    }

    public static final class Files {

        public interface FileColumns extends MediaColumns {
            public static final String FILE_NAME = "file_name";
            public static final String FILE_TYPE = "file_type";
            public static final String FORMAT = "format";
            public static final String MEDIA_TYPE = "media_type";
            public static final int MEDIA_TYPE_AUDIO = 2;
            public static final int MEDIA_TYPE_IMAGE = 1;
            public static final int MEDIA_TYPE_NONE = 0;
            public static final int MEDIA_TYPE_PLAYLIST = 4;
            public static final int MEDIA_TYPE_VIDEO = 3;
            public static final String MIME_TYPE = "mime_type";
            public static final String PARENT = "parent";
            public static final String STORAGE_ID = "storage_id";
            public static final String TITLE = "title";
        }

        public static Uri getContentUri(String volumeName) {
            return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/file");
        }

        public static final Uri getContentUri(String volumeName, long rowId) {
            return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/file/" + rowId);
        }

        public static Uri getMtpObjectsUri(String volumeName) {
            return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/object");
        }

        public static final Uri getMtpObjectsUri(String volumeName, long fileId) {
            return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/object/" + fileId);
        }

        public static final Uri getMtpReferencesUri(String volumeName, long fileId) {
            return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/object/" + fileId + "/references");
        }
    }

    private static class InternalThumbnails implements BaseColumns {
        static final int DEFAULT_GROUP_ID = 0;
        private static final int FULL_SCREEN_KIND = 2;
        private static final int MICRO_KIND = 3;
        private static final int MINI_KIND = 1;
        private static byte[] sThumbBuf;
        private static final String[] PROJECTION = {"_id", "_data"};
        private static final String[] SELECTION = {"_id", "_data", "width", "height"};
        private static final Object sThumbBufLock = new Object();

        private InternalThumbnails() {
        }

        private static Bitmap getMiniThumbFromFile(Cursor c, Uri baseUri, ContentResolver cr, BitmapFactory.Options options) {
            Bitmap bitmap = null;
            Uri thumbUri = null;
            try {
                long thumbId = c.getLong(0);
                c.getString(1);
                thumbUri = ContentUris.withAppendedId(baseUri, thumbId);
                ParcelFileDescriptor pfdInput = cr.openFileDescriptor(thumbUri, FullBackup.ROOT_TREE_TOKEN);
                bitmap = BitmapFactory.decodeFileDescriptor(pfdInput.getFileDescriptor(), null, options);
                pfdInput.close();
                return bitmap;
            } catch (FileNotFoundException ex) {
                Log.e(MediaStore.TAG, "couldn't open thumbnail " + thumbUri + "; " + ex);
                return bitmap;
            } catch (IOException ex2) {
                Log.e(MediaStore.TAG, "couldn't open thumbnail " + thumbUri + "; " + ex2);
                return bitmap;
            } catch (OutOfMemoryError ex3) {
                Log.e(MediaStore.TAG, "failed to allocate memory for thumbnail " + thumbUri + "; " + ex3);
                return bitmap;
            }
        }

        static void cancelThumbnailRequest(ContentResolver cr, long origId, Uri baseUri, long groupId) {
            Uri cancelUri = baseUri.buildUpon().appendQueryParameter("cancel", WifiEnterpriseConfig.ENGINE_ENABLE).appendQueryParameter("orig_id", String.valueOf(origId)).appendQueryParameter("group_id", String.valueOf(groupId)).build();
            Cursor c = cr.query(cancelUri, PROJECTION, null, null, null);
            if (c != null) {
                c.close();
            }
        }

        static Bitmap getThumbnail(ContentResolver cr, long origId, long groupId, int kind, BitmapFactory.Options options, Uri baseUri, boolean isVideo) {
            Cursor c;
            Bitmap b;
            Bitmap bitmap = null;
            long thumb_ID = 0;
            Log.v(MediaStore.TAG, "getThumbnail: origId=" + origId + ", kind=" + kind + ", Uri=" + baseUri + ", isVideo=" + isVideo);
            MiniThumbFile thumbFile = MiniThumbFile.instance(isVideo ? Video.Media.EXTERNAL_CONTENT_URI : Images.Media.EXTERNAL_CONTENT_URI);
            Cursor c2 = null;
            MiniThumbFile.ThumbResult result = new MiniThumbFile.ThumbResult();
            try {
                try {
                    long magic = thumbFile.getMagic(origId);
                    if (magic != 0) {
                        if (kind == 3) {
                            thumb_ID = !isVideo ? MediaStore.getImageThumbnailId(cr, baseUri, origId) : MediaStore.getVideoThumbnailId(cr, baseUri, origId);
                            if (magic == thumb_ID) {
                                synchronized (sThumbBufLock) {
                                    if (sThumbBuf == null) {
                                        sThumbBuf = new byte[16384];
                                    }
                                    if (thumbFile.getMiniThumbFromFile(origId, sThumbBuf, result) != null && (bitmap = BitmapFactory.decodeByteArray(sThumbBuf, 0, sThumbBuf.length, options)) == null) {
                                        Log.w(MediaStore.TAG, "couldn't decode byte array.");
                                    }
                                }
                                if (result.getDetail() != 1) {
                                    thumbFile.deactivate();
                                    return bitmap;
                                }
                                ContentValues values = new ContentValues();
                                values.put("mini_thumb_magic", WifiEnterpriseConfig.ENGINE_DISABLE);
                                String[] whereArgs = {String.valueOf(origId)};
                                if (isVideo) {
                                    cr.update(Video.Media.EXTERNAL_CONTENT_URI, values, "_id=? ", whereArgs);
                                } else {
                                    cr.update(Images.Media.EXTERNAL_CONTENT_URI, values, "_id=? ", whereArgs);
                                }
                                thumb_ID = 0;
                            }
                        } else if (kind == 1) {
                            String column = isVideo ? "video_id=" : "image_id=";
                            c2 = cr.query(baseUri, PROJECTION, column + origId, null, null);
                            if (c2 != null && c2.moveToFirst() && (bitmap = getMiniThumbFromFile(c2, baseUri, cr, options)) != null) {
                                return bitmap;
                            }
                        }
                    }
                    Uri blockingUri = baseUri.buildUpon().appendQueryParameter("blocking", WifiEnterpriseConfig.ENGINE_ENABLE).appendQueryParameter("orig_id", String.valueOf(origId)).appendQueryParameter("group_id", String.valueOf(groupId)).build();
                    if (c2 != null) {
                        c2.close();
                    }
                    c = cr.query(blockingUri, SELECTION, null, null, null);
                } catch (SQLiteException ex) {
                    Log.w(MediaStore.TAG, ProxyInfo.LOCAL_EXCL_LIST, ex);
                    if (0 != 0) {
                        c2.close();
                    }
                    thumbFile.deactivate();
                }
                if (c == null) {
                    if (c != null) {
                        c.close();
                    }
                    thumbFile.deactivate();
                    return null;
                }
                if (kind == 3 && thumb_ID == 0) {
                    long thumb_ID2 = !isVideo ? MediaStore.getImageThumbnailId(cr, baseUri, origId) : MediaStore.getVideoThumbnailId(cr, baseUri, origId);
                    long thumb_id = thumbFile.getMagic(origId);
                    if (0 != thumb_ID2 && thumb_id == thumb_ID2) {
                        synchronized (sThumbBufLock) {
                            if (sThumbBuf == null) {
                                sThumbBuf = new byte[16384];
                            }
                            if (thumbFile.getMiniThumbFromFile(origId, sThumbBuf) != null && (bitmap = BitmapFactory.decodeByteArray(sThumbBuf, 0, sThumbBuf.length, options)) == null) {
                                Log.w(MediaStore.TAG, "couldn't decode byte array.");
                            }
                        }
                    }
                } else if (kind == 1) {
                    if (c.moveToFirst()) {
                        bitmap = getMiniThumbFromFile(c, baseUri, cr, options);
                    }
                } else {
                    if (thumb_ID == 0) {
                        throw new IllegalArgumentException("Unsupported kind: " + kind);
                    }
                    Log.w(MediaStore.TAG, "------for thumb_ID !=null------");
                }
                if (bitmap == null) {
                    Log.v(MediaStore.TAG, "Create the thumbnail in memory: origId=" + origId + ", kind=" + kind + ", isVideo=" + isVideo);
                    Uri uri = Uri.parse(baseUri.buildUpon().appendPath(String.valueOf(origId)).toString().replaceFirst("thumbnails", MediaStore.AUTHORITY));
                    if (c != null) {
                        c.close();
                    }
                    c = cr.query(uri, PROJECTION, null, null, null);
                    if (c == null || !c.moveToFirst()) {
                        if (c != null) {
                            c.close();
                        }
                        thumbFile.deactivate();
                        return null;
                    }
                    String filePath = c.getString(1);
                    if (filePath != null) {
                        bitmap = isVideo ? ThumbnailUtils.createVideoThumbnail(filePath, kind) : ThumbnailUtils.createImageThumbnail(filePath, kind);
                        String mimeType = MediaFile.getMimeTypeForFile(filePath);
                        if (bitmap != null && "image/gif".equals(mimeType) && (b = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888)) != null) {
                            Canvas canvas = new Canvas(b);
                            canvas.drawColor(-1);
                            canvas.drawBitmap(bitmap, new Matrix(), null);
                            bitmap.recycle();
                            bitmap = b;
                        }
                    }
                }
                if (c != null) {
                    c.close();
                }
                thumbFile.deactivate();
                return bitmap;
            } finally {
                if (0 != 0) {
                    c2.close();
                }
                thumbFile.deactivate();
            }
        }
    }

    public static final class Images {

        public interface ImageColumns extends MediaColumns {
            public static final String BUCKET_DISPLAY_NAME = "bucket_display_name";
            public static final String BUCKET_ID = "bucket_id";
            public static final String CAMERA_REFOCUS = "camera_refocus";
            public static final String DATE_TAKEN = "datetaken";
            public static final String DESCRIPTION = "description";
            public static final String FOCUS_VALUE_HIGH = "focus_value_high";
            public static final String FOCUS_VALUE_LOW = "focus_value_low";
            public static final String GROUP_COUNT = "group_count";
            public static final String GROUP_ID = "group_id";
            public static final String GROUP_INDEX = "group_index";
            public static final String IS_BEST_SHOT = "is_best_shot";
            public static final String IS_PRIVATE = "isprivate";
            public static final String LATITUDE = "latitude";
            public static final String LONGITUDE = "longitude";
            public static final String MINI_THUMB_MAGIC = "mini_thumb_magic";
            public static final String ORIENTATION = "orientation";
            public static final String PICASA_ID = "picasa_id";
            public static final int STEREO_TYPE_2D = 0;
            public static final int STEREO_TYPE_FRAME_SEQUENCE = 1;
            public static final int STEREO_TYPE_SIDE_BY_SIDE = 2;
            public static final int STEREO_TYPE_SWAP_LEFT_RIGHT = 4;
            public static final int STEREO_TYPE_SWAP_TOP_BOTTOM = 5;
            public static final int STEREO_TYPE_TOP_BOTTOM = 3;
            public static final int STEREO_TYPE_UNKNOWN = -1;
        }

        public static final class Media implements ImageColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/image";
            public static final String DEFAULT_SORT_ORDER = "bucket_display_name";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");

            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection) {
                return cr.query(uri, projection, null, null, "bucket_display_name");
            }

            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection, String where, String orderBy) {
                return cr.query(uri, projection, where, null, orderBy == null ? "bucket_display_name" : orderBy);
            }

            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy) {
                return cr.query(uri, projection, selection, selectionArgs, orderBy == null ? "bucket_display_name" : orderBy);
            }

            public static final Bitmap getBitmap(ContentResolver cr, Uri url) throws IOException {
                InputStream input = cr.openInputStream(url);
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                return bitmap;
            }

            public static final String insertImage(ContentResolver cr, String imagePath, String name, String description) throws FileNotFoundException {
                FileInputStream stream = new FileInputStream(imagePath);
                try {
                    Bitmap bm = BitmapFactory.decodeFile(imagePath);
                    String ret = insertImage(cr, bm, name, description);
                    bm.recycle();
                    return ret;
                } finally {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        Log.e(MediaStore.TAG, "insertImage: IOException! path=" + imagePath, e);
                    }
                }
            }

            private static final Bitmap StoreThumbnail(ContentResolver cr, Bitmap source, long id, float width, float height, int kind) {
                Matrix matrix = new Matrix();
                float scaleX = width / source.getWidth();
                float scaleY = height / source.getHeight();
                matrix.setScale(scaleX, scaleY);
                Bitmap thumb = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
                ContentValues values = new ContentValues(4);
                values.put("kind", Integer.valueOf(kind));
                values.put("image_id", Integer.valueOf((int) id));
                values.put("height", Integer.valueOf(thumb.getHeight()));
                values.put("width", Integer.valueOf(thumb.getWidth()));
                Uri url = cr.insert(Thumbnails.EXTERNAL_CONTENT_URI, values);
                try {
                    OutputStream thumbOut = cr.openOutputStream(url);
                    thumb.compress(Bitmap.CompressFormat.JPEG, 100, thumbOut);
                    thumbOut.close();
                    return thumb;
                } catch (FileNotFoundException ex) {
                    Log.e(MediaStore.TAG, "StoreThumbnail: FileNotFoundException! uri=" + url, ex);
                    return null;
                } catch (IOException ex2) {
                    Log.e(MediaStore.TAG, "StoreThumbnail: IOException! uri=" + url, ex2);
                    return null;
                }
            }

            private static boolean ensureFileExists(String path) {
                File file = new File(path);
                if (file.exists()) {
                    return true;
                }
                int secondSlash = path.indexOf(47, 1);
                if (secondSlash < 1) {
                    return false;
                }
                String directoryPath = path.substring(0, secondSlash);
                File directory = new File(directoryPath);
                if (!directory.exists()) {
                    return false;
                }
                file.getParentFile().mkdirs();
                try {
                    return file.createNewFile();
                } catch (IOException ioe) {
                    Log.e(MediaStore.TAG, "File creation failed", ioe);
                    return false;
                }
            }

            public static final String insertImage(ContentResolver cr, Bitmap source, String title, String description) {
                ContentValues values = new ContentValues();
                values.put("title", title);
                values.put("description", description);
                values.put("mime_type", "image/jpeg");
                Uri url = null;
                try {
                    url = cr.insert(EXTERNAL_CONTENT_URI, values);
                    if (source != null) {
                        OutputStream imageOut = cr.openOutputStream(url);
                        try {
                            source.compress(Bitmap.CompressFormat.JPEG, 50, imageOut);
                            imageOut.close();
                            long id = ContentUris.parseId(url);
                            Bitmap miniThumb = Thumbnails.getThumbnail(cr, id, 1, null);
                            StoreThumbnail(cr, miniThumb, id, 50.0f, 50.0f, 3);
                        } catch (Throwable th) {
                            imageOut.close();
                            throw th;
                        }
                    } else {
                        Log.e(MediaStore.TAG, "Failed to create thumbnail, removing original");
                        cr.delete(url, null, null);
                        url = null;
                    }
                } catch (Exception e) {
                    Log.e(MediaStore.TAG, "Failed to insert image", e);
                    if (url != null) {
                        cr.delete(url, null, null);
                        url = null;
                    }
                }
                if (url == null) {
                    return null;
                }
                String stringUrl = url.toString();
                return stringUrl;
            }

            public static Uri getContentUri(String volumeName) {
                return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/images/media");
            }
        }

        public static class Thumbnails implements BaseColumns {
            public static final String DATA = "_data";
            public static final String DEFAULT_SORT_ORDER = "image_id ASC";
            public static final int FULL_SCREEN_KIND = 2;
            public static final String HEIGHT = "height";
            public static final String IMAGE_ID = "image_id";
            public static final String KIND = "kind";
            public static final int MICRO_KIND = 3;
            public static final int MINI_KIND = 1;
            public static final String THUMB_DATA = "thumb_data";
            public static final String WIDTH = "width";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");

            public static final Cursor query(ContentResolver cr, Uri uri, String[] projection) {
                return cr.query(uri, projection, null, null, DEFAULT_SORT_ORDER);
            }

            public static final Cursor queryMiniThumbnails(ContentResolver cr, Uri uri, int kind, String[] projection) {
                return cr.query(uri, projection, "kind = " + kind, null, DEFAULT_SORT_ORDER);
            }

            public static final Cursor queryMiniThumbnail(ContentResolver cr, long origId, int kind, String[] projection) {
                return cr.query(EXTERNAL_CONTENT_URI, projection, "image_id = " + origId + " AND kind = " + kind, null, null);
            }

            public static void cancelThumbnailRequest(ContentResolver cr, long origId) {
                InternalThumbnails.cancelThumbnailRequest(cr, origId, EXTERNAL_CONTENT_URI, 0L);
            }

            public static Bitmap getThumbnail(ContentResolver cr, long origId, int kind, BitmapFactory.Options options) {
                return InternalThumbnails.getThumbnail(cr, origId, 0L, kind, options, EXTERNAL_CONTENT_URI, false);
            }

            public static void cancelThumbnailRequest(ContentResolver cr, long origId, long groupId) {
                InternalThumbnails.cancelThumbnailRequest(cr, origId, EXTERNAL_CONTENT_URI, groupId);
            }

            public static Bitmap getThumbnail(ContentResolver cr, long origId, long groupId, int kind, BitmapFactory.Options options) {
                return InternalThumbnails.getThumbnail(cr, origId, groupId, kind, options, EXTERNAL_CONTENT_URI, false);
            }

            public static Uri getContentUri(String volumeName) {
                return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/images/thumbnails");
            }
        }
    }

    public static final class Audio {

        public interface AlbumColumns {
            public static final String ALBUM = "album";
            public static final String ALBUM_ART = "album_art";
            public static final String ALBUM_ID = "album_id";
            public static final String ALBUM_KEY = "album_key";
            public static final String ALBUM_PINYIN_KEY = "album_pinyin_key";
            public static final String ARTIST = "artist";
            public static final String FIRST_YEAR = "minyear";
            public static final String LAST_YEAR = "maxyear";
            public static final String NUMBER_OF_SONGS = "numsongs";
            public static final String NUMBER_OF_SONGS_FOR_ARTIST = "numsongs_by_artist";
        }

        public interface ArtistColumns {
            public static final String ARTIST = "artist";
            public static final String ARTIST_KEY = "artist_key";
            public static final String ARTIST_PINYIN_KEY = "artist_pinyin_key";
            public static final String NUMBER_OF_ALBUMS = "number_of_albums";
            public static final String NUMBER_OF_TRACKS = "number_of_tracks";
        }

        public interface AudioColumns extends MediaColumns {
            public static final String ALBUM = "album";
            public static final String ALBUM_ARTIST = "album_artist";
            public static final String ALBUM_ID = "album_id";
            public static final String ALBUM_KEY = "album_key";
            public static final String ARTIST = "artist";
            public static final String ARTIST_ID = "artist_id";
            public static final String ARTIST_KEY = "artist_key";
            public static final String BOOKMARK = "bookmark";
            public static final String COMPILATION = "compilation";
            public static final String COMPOSER = "composer";
            public static final String DURATION = "duration";
            public static final String GENRE = "genre";
            public static final String IS_ALARM = "is_alarm";
            public static final String IS_MUSIC = "is_music";
            public static final String IS_NOTIFICATION = "is_notification";
            public static final String IS_PODCAST = "is_podcast";
            public static final String IS_RECORD = "is_record";
            public static final String IS_RINGTONE = "is_ringtone";
            public static final String TITLE_KEY = "title_key";
            public static final String TITLE_PINYIN_KEY = "title_pinyin_key";
            public static final String TRACK = "track";
            public static final String YEAR = "year";
        }

        public interface GenresColumns {
            public static final String NAME = "name";
        }

        public interface PlaylistsColumns {
            public static final String DATA = "_data";
            public static final String DATE_ADDED = "date_added";
            public static final String DATE_MODIFIED = "date_modified";
            public static final String NAME = "name";
            public static final String NAME_PINYIN_KEY = "name_pinyin_key";
        }

        public static String keyFor(String name) {
            if (name == null) {
                return null;
            }
            if (name.equals(MediaStore.UNKNOWN_STRING)) {
                return "\u0001";
            }
            boolean sortfirst = name.startsWith("\u0001");
            String name2 = name.trim().toLowerCase();
            if (name2.startsWith("the ")) {
                name2 = name2.substring(4);
            }
            if (name2.startsWith("an ")) {
                name2 = name2.substring(3);
            }
            if (name2.startsWith("a ")) {
                name2 = name2.substring(2);
            }
            if (name2.endsWith(", the") || name2.endsWith(",the") || name2.endsWith(", an") || name2.endsWith(",an") || name2.endsWith(", a") || name2.endsWith(",a")) {
                name2 = name2.substring(0, name2.lastIndexOf(44));
            }
            String name3 = name2.replaceAll("[\\[\\]\\(\\)\"'.,?!]", ProxyInfo.LOCAL_EXCL_LIST).trim();
            if (name3.length() <= 0) {
                return ProxyInfo.LOCAL_EXCL_LIST;
            }
            StringBuilder b = new StringBuilder();
            b.append('.');
            int nl = name3.length();
            for (int i = 0; i < nl; i++) {
                b.append(name3.charAt(i));
                b.append('.');
            }
            String key = DatabaseUtils.getCollationKey(b.toString());
            return sortfirst ? "\u0001" + key : key;
        }

        public static final class Media implements AudioColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/audio";
            public static final String DEFAULT_SORT_ORDER = "title_key";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/audio";
            public static final Uri EXTERNAL_CONTENT_URI;
            private static final String[] EXTERNAL_PATHS;
            public static final String EXTRA_MAX_BYTES = "android.provider.MediaStore.extra.MAX_BYTES";
            public static final Uri INTERNAL_CONTENT_URI;
            public static final String RECORD_SOUND_ACTION = "android.provider.MediaStore.RECORD_SOUND";

            static {
                String secondary_storage = System.getenv("SECONDARY_STORAGE");
                if (secondary_storage != null) {
                    EXTERNAL_PATHS = secondary_storage.split(":");
                } else {
                    EXTERNAL_PATHS = new String[0];
                }
                INTERNAL_CONTENT_URI = getContentUri("internal");
                EXTERNAL_CONTENT_URI = getContentUri("external");
            }

            public static Uri getContentUri(String volumeName) {
                return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/audio/media");
            }

            public static Uri getContentUriForPath(String path) {
                for (String ep : EXTERNAL_PATHS) {
                    if (path.startsWith(ep)) {
                        return EXTERNAL_CONTENT_URI;
                    }
                }
                return path.startsWith(Environment.getExternalStorageDirectory().getPath()) ? EXTERNAL_CONTENT_URI : INTERNAL_CONTENT_URI;
            }
        }

        public static final class Genres implements BaseColumns, GenresColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/genre";
            public static final String DEFAULT_SORT_ORDER = "name";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/genre";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");

            public static Uri getContentUri(String volumeName) {
                return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/audio/genres");
            }

            public static Uri getContentUriForAudioId(String volumeName, int audioId) {
                return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/audio/media/" + audioId + "/genres");
            }

            public static final class Members implements AudioColumns {
                public static final String AUDIO_ID = "audio_id";
                public static final String CONTENT_DIRECTORY = "members";
                public static final String DEFAULT_SORT_ORDER = "title_key";
                public static final String GENRE_ID = "genre_id";

                public static final Uri getContentUri(String volumeName, long genreId) {
                    return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/audio/genres/" + genreId + "/members");
                }
            }
        }

        public static final class Playlists implements BaseColumns, PlaylistsColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/playlist";
            public static final String DEFAULT_SORT_ORDER = "name";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/playlist";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");

            public static Uri getContentUri(String volumeName) {
                return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/audio/playlists");
            }

            public static final class Members implements AudioColumns {
                public static final String AUDIO_ID = "audio_id";
                public static final String CONTENT_DIRECTORY = "members";
                public static final String DEFAULT_SORT_ORDER = "play_order";
                public static final String PLAYLIST_ID = "playlist_id";
                public static final String PLAY_ORDER = "play_order";
                public static final String _ID = "_id";

                public static final Uri getContentUri(String volumeName, long playlistId) {
                    return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/audio/playlists/" + playlistId + "/members");
                }

                public static final boolean moveItem(ContentResolver res, long playlistId, int from, int to) {
                    Uri uri = getContentUri("external", playlistId).buildUpon().appendEncodedPath(String.valueOf(from)).appendQueryParameter("move", "true").build();
                    ContentValues values = new ContentValues();
                    values.put("play_order", Integer.valueOf(to));
                    return res.update(uri, values, null, null) != 0;
                }
            }
        }

        public static final class Artists implements BaseColumns, ArtistColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/artists";
            public static final String DEFAULT_SORT_ORDER = "artist_key";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/artist";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");

            public static Uri getContentUri(String volumeName) {
                return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/audio/artists");
            }

            public static final class Albums implements AlbumColumns {
                public static final Uri getContentUri(String volumeName, long artistId) {
                    return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/audio/artists/" + artistId + "/albums");
                }
            }
        }

        public static final class Albums implements BaseColumns, AlbumColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/albums";
            public static final String DEFAULT_SORT_ORDER = "album_key";
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/album";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");

            public static Uri getContentUri(String volumeName) {
                return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/audio/albums");
            }
        }

        public static final class Radio {
            public static final String ENTRY_CONTENT_TYPE = "vnd.android.cursor.item/radio";

            private Radio() {
            }
        }
    }

    public static final class Video {
        public static final String DEFAULT_SORT_ORDER = "_display_name";

        public interface VideoColumns extends MediaColumns {
            public static final String ALBUM = "album";
            public static final String ARTIST = "artist";
            public static final String BOOKMARK = "bookmark";
            public static final String BUCKET_DISPLAY_NAME = "bucket_display_name";
            public static final String BUCKET_ID = "bucket_id";
            public static final String CATEGORY = "category";
            public static final String DATE_TAKEN = "datetaken";
            public static final String DESCRIPTION = "description";
            public static final String DURATION = "duration";
            public static final String IS_LIVE_PHOTO = "is_live_photo";
            public static final String IS_PRIVATE = "isprivate";
            public static final String LANGUAGE = "language";
            public static final String LATITUDE = "latitude";
            public static final String LONGITUDE = "longitude";
            public static final String MINI_THUMB_MAGIC = "mini_thumb_magic";
            public static final String ORIENTATION = "orientation";
            public static final String RESOLUTION = "resolution";
            public static final String SLOW_MOTION_SPEED = "slow_motion_speed";
            public static final int STEREO_TYPE_2D = 0;
            public static final int STEREO_TYPE_FRAME_SEQUENCE = 1;
            public static final int STEREO_TYPE_SIDE_BY_SIDE = 2;
            public static final int STEREO_TYPE_SWAP_LEFT_RIGHT = 4;
            public static final int STEREO_TYPE_SWAP_TOP_BOTTOM = 5;
            public static final int STEREO_TYPE_TOP_BOTTOM = 3;
            public static final int STEREO_TYPE_UNKNOWN = -1;
            public static final String TAGS = "tags";
        }

        public static final Cursor query(ContentResolver cr, Uri uri, String[] projection) {
            return cr.query(uri, projection, null, null, "_display_name");
        }

        public static final class Media implements VideoColumns {
            public static final String CONTENT_TYPE = "vnd.android.cursor.dir/video";
            public static final String DEFAULT_SORT_ORDER = "title";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");

            public static Uri getContentUri(String volumeName) {
                return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/video/media");
            }
        }

        public static class Thumbnails implements BaseColumns {
            public static final String DATA = "_data";
            public static final String DEFAULT_SORT_ORDER = "video_id ASC";
            public static final int FULL_SCREEN_KIND = 2;
            public static final String HEIGHT = "height";
            public static final String KIND = "kind";
            public static final int MICRO_KIND = 3;
            public static final int MINI_KIND = 1;
            public static final String VIDEO_ID = "video_id";
            public static final String WIDTH = "width";
            public static final Uri INTERNAL_CONTENT_URI = getContentUri("internal");
            public static final Uri EXTERNAL_CONTENT_URI = getContentUri("external");

            public static void cancelThumbnailRequest(ContentResolver cr, long origId) {
                InternalThumbnails.cancelThumbnailRequest(cr, origId, EXTERNAL_CONTENT_URI, 0L);
            }

            public static Bitmap getThumbnail(ContentResolver cr, long origId, int kind, BitmapFactory.Options options) {
                return InternalThumbnails.getThumbnail(cr, origId, 0L, kind, options, EXTERNAL_CONTENT_URI, true);
            }

            public static Bitmap getThumbnail(ContentResolver cr, long origId, long groupId, int kind, BitmapFactory.Options options) {
                return InternalThumbnails.getThumbnail(cr, origId, groupId, kind, options, EXTERNAL_CONTENT_URI, true);
            }

            public static void cancelThumbnailRequest(ContentResolver cr, long origId, long groupId) {
                InternalThumbnails.cancelThumbnailRequest(cr, origId, EXTERNAL_CONTENT_URI, groupId);
            }

            public static Uri getContentUri(String volumeName) {
                return Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + volumeName + "/video/thumbnails");
            }
        }
    }

    public static Uri getMediaScannerUri() {
        return Uri.parse("content://media/none/media_scanner");
    }

    public static String getVersion(Context context) {
        Cursor c = context.getContentResolver().query(Uri.parse("content://media/none/version"), null, null, null, null);
        if (c != null) {
            try {
                if (c.moveToFirst()) {
                    return c.getString(0);
                }
            } finally {
                c.close();
            }
        }
        return null;
    }

    public static Uri getMtpTransferFileUri() {
        return Uri.parse("content://media/none/mtp_transfer_file");
    }

    private static long getImageThumbnailId(ContentResolver cr, Uri baseUri, long origId) {
        Cursor c;
        baseUri.toString();
        Uri imagesUri = Uri.parse("content://media/external/images/media/");
        AutoCloseable autoCloseable = null;
        try {
            try {
                c = cr.query(imagesUri, new String[]{"mini_thumb_magic"}, "_id = " + origId, null, null);
            } catch (SQLiteException ex) {
                Log.e(TAG, "getImageThumbnailId: SQLiteException!", ex);
                if (0 != 0) {
                    autoCloseable.close();
                }
            }
            if (c == null) {
                Log.e(TAG, "getImageThumbnailId: Null cursor! id=" + origId);
                if (c != null) {
                    c.close();
                }
                return 0L;
            }
            thumb_Id = c.moveToFirst() ? c.getLong(0) : 0L;
            if (c != null) {
                c.close();
            }
            return thumb_Id;
        } catch (Throwable th) {
            if (0 != 0) {
                autoCloseable.close();
            }
            throw th;
        }
    }

    private static long getVideoThumbnailId(ContentResolver cr, Uri baseUri, long origId) {
        Cursor c;
        baseUri.toString();
        Uri imagesUri = Uri.parse("content://media/external/video/media/");
        AutoCloseable autoCloseable = null;
        try {
            try {
                c = cr.query(imagesUri, new String[]{"mini_thumb_magic"}, "_id = " + origId, null, null);
            } catch (SQLiteException ex) {
                Log.e(TAG, "getVideoThumbnailId: SQLiteException!", ex);
                if (0 != 0) {
                    autoCloseable.close();
                }
            }
            if (c == null) {
                Log.e(TAG, "getVideoThumbnailId: Null cursor! id=" + origId);
                if (c != null) {
                    c.close();
                }
                return 0L;
            }
            thumb_Id = c.moveToFirst() ? c.getLong(0) : 0L;
            if (c != null) {
                c.close();
            }
            return thumb_Id;
        } catch (Throwable th) {
            if (0 != 0) {
                autoCloseable.close();
            }
            throw th;
        }
    }
}
