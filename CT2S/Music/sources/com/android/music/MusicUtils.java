package com.android.music;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;
import com.android.music.IMediaPlaybackService;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;

public class MusicUtils {
    private static String mLastSdStatus;
    static int sActiveTabIndex;
    private static final String sExternalMediaUri;
    private static int sLogPtr;
    private static LogEntry[] sMusicLog;
    private static Time sTime;
    public static IMediaPlaybackService sService = null;
    private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<>();
    private static final long[] sEmptyList = new long[0];
    private static ContentValues[] sContentValuesCache = null;
    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    private static final Object[] sTimeArgs = new Object[5];
    private static int sArtId = -2;
    private static Bitmap mCachedBit = null;
    private static final BitmapFactory.Options sBitmapOptionsCache = new BitmapFactory.Options();
    private static final BitmapFactory.Options sBitmapOptions = new BitmapFactory.Options();
    private static final Uri sArtworkUri = Uri.parse("content://media/external/audio/albumart");
    private static final HashMap<Long, Drawable> sArtCache = new HashMap<>();
    private static int sArtCacheId = -1;

    public static String makeAlbumsLabel(Context context, int numalbums, int numsongs, boolean isUnknown) {
        StringBuilder songs_albums = new StringBuilder();
        Resources r = context.getResources();
        if (isUnknown) {
            if (numsongs == 1) {
                songs_albums.append(context.getString(R.string.onesong));
            } else {
                String f = r.getQuantityText(R.plurals.Nsongs, numsongs).toString();
                sFormatBuilder.setLength(0);
                sFormatter.format(f, Integer.valueOf(numsongs));
                songs_albums.append((CharSequence) sFormatBuilder);
            }
        } else {
            String f2 = r.getQuantityText(R.plurals.Nalbums, numalbums).toString();
            sFormatBuilder.setLength(0);
            sFormatter.format(f2, Integer.valueOf(numalbums));
            songs_albums.append((CharSequence) sFormatBuilder);
            songs_albums.append(context.getString(R.string.albumsongseparator));
        }
        return songs_albums.toString();
    }

    public static String makeAlbumsSongsLabel(Context context, int numalbums, int numsongs, boolean isUnknown) {
        StringBuilder songs_albums = new StringBuilder();
        if (numsongs == 1) {
            songs_albums.append(context.getString(R.string.onesong));
        } else {
            Resources r = context.getResources();
            if (!isUnknown) {
                String f = r.getQuantityText(R.plurals.Nalbums, numalbums).toString();
                sFormatBuilder.setLength(0);
                sFormatter.format(f, Integer.valueOf(numalbums));
                songs_albums.append((CharSequence) sFormatBuilder);
                songs_albums.append(context.getString(R.string.albumsongseparator));
            }
            String f2 = r.getQuantityText(R.plurals.Nsongs, numsongs).toString();
            sFormatBuilder.setLength(0);
            sFormatter.format(f2, Integer.valueOf(numsongs));
            songs_albums.append((CharSequence) sFormatBuilder);
        }
        return songs_albums.toString();
    }

    static {
        sBitmapOptionsCache.inPreferredConfig = Bitmap.Config.RGB_565;
        sBitmapOptionsCache.inDither = false;
        sBitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        sBitmapOptions.inDither = false;
        sExternalMediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        sActiveTabIndex = -1;
        sMusicLog = new LogEntry[100];
        sLogPtr = 0;
        sTime = new Time();
    }

    public static class ServiceToken {
        ContextWrapper mWrappedContext;

        ServiceToken(ContextWrapper context) {
            this.mWrappedContext = context;
        }
    }

    public static ServiceToken bindToService(Activity context) {
        return bindToService(context, null);
    }

    public static ServiceToken bindToService(Activity context, ServiceConnection callback) {
        Activity realActivity = context.getParent();
        if (realActivity == null) {
            realActivity = context;
        }
        ContextWrapper cw = new ContextWrapper(realActivity);
        cw.startService(new Intent(cw, (Class<?>) MediaPlaybackService.class));
        ServiceBinder sb = new ServiceBinder(callback);
        if (cw.bindService(new Intent().setClass(cw, MediaPlaybackService.class), sb, 0)) {
            sConnectionMap.put(cw, sb);
            return new ServiceToken(cw);
        }
        Log.e("Music", "Failed to bind to service");
        return null;
    }

    public static void unbindFromService(ServiceToken token) {
        if (token == null) {
            Log.e("MusicUtils", "Trying to unbind with null token");
            return;
        }
        ContextWrapper cw = token.mWrappedContext;
        ServiceBinder sb = sConnectionMap.remove(cw);
        if (sb == null) {
            Log.e("MusicUtils", "Trying to unbind for unknown Context");
            return;
        }
        cw.unbindService(sb);
        if (sConnectionMap.isEmpty()) {
            sService = null;
        }
    }

    private static class ServiceBinder implements ServiceConnection {
        ServiceConnection mCallback;

        ServiceBinder(ServiceConnection callback) {
            this.mCallback = callback;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MusicUtils.sService = IMediaPlaybackService.Stub.asInterface(service);
            MusicUtils.initAlbumArtCache();
            if (this.mCallback != null) {
                this.mCallback.onServiceConnected(className, service);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            if (this.mCallback != null) {
                this.mCallback.onServiceDisconnected(className);
            }
            MusicUtils.sService = null;
        }
    }

    public static long getCurrentAlbumId() {
        if (sService != null) {
            try {
                return sService.getAlbumId();
            } catch (RemoteException e) {
            }
        }
        return -1L;
    }

    public static long getCurrentArtistId() {
        if (sService != null) {
            try {
                return sService.getArtistId();
            } catch (RemoteException e) {
            }
        }
        return -1L;
    }

    public static long getCurrentAudioId() {
        if (sService != null) {
            try {
                return sService.getAudioId();
            } catch (RemoteException e) {
            }
        }
        return -1L;
    }

    public static int getCurrentShuffleMode() {
        if (sService == null) {
            return 0;
        }
        try {
            int mode = sService.getShuffleMode();
            return mode;
        } catch (RemoteException e) {
            return 0;
        }
    }

    public static void togglePartyShuffle() {
        if (sService != null) {
            int shuffle = getCurrentShuffleMode();
            try {
                if (shuffle == 2) {
                    sService.setShuffleMode(0);
                } else {
                    sService.setShuffleMode(2);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public static void setPartyShuffleMenuIcon(Menu menu) {
        MenuItem item = menu.findItem(8);
        if (item != null) {
            int shuffle = getCurrentShuffleMode();
            if (shuffle == 2) {
                item.setIcon(R.drawable.ic_menu_party_shuffle);
                item.setTitle(R.string.party_shuffle_off);
            } else {
                item.setIcon(R.drawable.ic_menu_party_shuffle);
                item.setTitle(R.string.party_shuffle);
            }
        }
    }

    public static long[] getSongListForCursor(Cursor cursor) {
        int colidx;
        if (cursor == null) {
            return sEmptyList;
        }
        int len = cursor.getCount();
        long[] list = new long[len];
        cursor.moveToFirst();
        try {
            colidx = cursor.getColumnIndexOrThrow("audio_id");
        } catch (IllegalArgumentException e) {
            colidx = cursor.getColumnIndexOrThrow("_id");
        }
        for (int i = 0; i < len; i++) {
            list[i] = cursor.getLong(colidx);
            cursor.moveToNext();
        }
        return list;
    }

    public static long[] getSongListForArtist(Context context, long id) {
        String[] ccols = {"_id"};
        String where = "artist_id=" + id + " AND is_music=1";
        Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, where, null, "album_key,track");
        if (cursor != null) {
            long[] list = getSongListForCursor(cursor);
            cursor.close();
            return list;
        }
        long[] list2 = sEmptyList;
        return list2;
    }

    public static long[] getSongListForAlbum(Context context, long id) {
        String[] ccols = {"_id"};
        String where = "album_id=" + id + " AND is_music=1";
        Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, where, null, "track");
        if (cursor != null) {
            long[] list = getSongListForCursor(cursor);
            cursor.close();
            return list;
        }
        long[] list2 = sEmptyList;
        return list2;
    }

    public static long[] getSongListForPlaylist(Context context, long plid) {
        String[] ccols = {"audio_id"};
        Cursor cursor = query(context, MediaStore.Audio.Playlists.Members.getContentUri("external", plid), ccols, null, null, "play_order");
        if (cursor != null) {
            long[] list = getSongListForCursor(cursor);
            cursor.close();
            return list;
        }
        long[] list2 = sEmptyList;
        return list2;
    }

    public static void playPlaylist(Context context, long plid) {
        long[] list = getSongListForPlaylist(context, plid);
        if (list != null) {
            playAll(context, list, -1, false);
        }
    }

    public static long[] getAllSongs(Context context) {
        Cursor c = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "is_music=1", null, null);
        if (c != null) {
            try {
                if (c.getCount() != 0) {
                    int len = c.getCount();
                    long[] list = new long[len];
                    for (int i = 0; i < len; i++) {
                        c.moveToNext();
                        list[i] = c.getLong(0);
                    }
                    if (c == null) {
                        return list;
                    }
                    c.close();
                    return list;
                }
            } finally {
                if (c != null) {
                    c.close();
                }
            }
        }
        return null;
    }

    public static void makePlaylistMenu(Context context, SubMenu sub) {
        String[] cols = {"_id", "name"};
        ContentResolver resolver = context.getContentResolver();
        if (resolver == null) {
            System.out.println("resolver = null");
            return;
        }
        Cursor cur = resolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, cols, "name != ''", null, "name");
        sub.clear();
        sub.add(1, 12, 0, R.string.queue);
        sub.add(1, 4, 0, R.string.new_playlist);
        if (cur != null && cur.getCount() > 0) {
            cur.moveToFirst();
            while (!cur.isAfterLast()) {
                Intent intent = new Intent();
                intent.putExtra("playlist", cur.getLong(0));
                sub.add(1, 3, 0, cur.getString(1)).setIntent(intent);
                cur.moveToNext();
            }
        }
        if (cur != null) {
            cur.close();
        }
    }

    public static void deleteTracks(Context context, long[] list) {
        String[] cols = {"_id", "_data", "album_id"};
        StringBuilder where = new StringBuilder();
        where.append("_id IN (");
        for (int i = 0; i < list.length; i++) {
            where.append(list[i]);
            if (i < list.length - 1) {
                where.append(",");
            }
        }
        where.append(")");
        Cursor c = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, where.toString(), null, null);
        if (c != null) {
            try {
                c.moveToFirst();
                while (!c.isAfterLast()) {
                    long id = c.getLong(0);
                    sService.removeTrack(id);
                    long artIndex = c.getLong(2);
                    synchronized (sArtCache) {
                        sArtCache.remove(Long.valueOf(artIndex));
                    }
                    c.moveToNext();
                }
            } catch (RemoteException e) {
            }
            context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, where.toString(), null);
            c.moveToFirst();
            while (!c.isAfterLast()) {
                String name = c.getString(1);
                File f = new File(name);
                try {
                    if (!f.delete()) {
                        Log.e("MusicUtils", "Failed to delete file " + name);
                    }
                    c.moveToNext();
                } catch (SecurityException e2) {
                    c.moveToNext();
                }
            }
            c.close();
        }
        String message = context.getResources().getQuantityString(R.plurals.NNNtracksdeleted, list.length, Integer.valueOf(list.length));
        Toast.makeText(context, message, 0).show();
        context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
    }

    public static void addToCurrentPlaylist(Context context, long[] list) {
        if (sService != null) {
            try {
                sService.enqueue(list, 3);
                String message = context.getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, list.length, Integer.valueOf(list.length));
                Toast.makeText(context, message, 0).show();
            } catch (RemoteException e) {
            }
        }
    }

    private static void makeInsertItems(long[] ids, int offset, int len, int base) {
        if (offset + len > ids.length) {
            len = ids.length - offset;
        }
        if (sContentValuesCache == null || sContentValuesCache.length != len) {
            sContentValuesCache = new ContentValues[len];
        }
        for (int i = 0; i < len; i++) {
            if (sContentValuesCache[i] == null) {
                sContentValuesCache[i] = new ContentValues();
            }
            sContentValuesCache[i].put("play_order", Integer.valueOf(base + offset + i));
            sContentValuesCache[i].put("audio_id", Long.valueOf(ids[offset + i]));
        }
    }

    public static void addToPlaylist(Context context, long[] ids, long playlistid) {
        if (ids == null) {
            Log.e("MusicBase", "ListSelection null");
            return;
        }
        int size = ids.length;
        ContentResolver resolver = context.getContentResolver();
        String[] cols = {"count(*)"};
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", playlistid);
        Cursor cur = resolver.query(uri, cols, null, null, null);
        cur.moveToFirst();
        int base = cur.getInt(0);
        cur.close();
        int numinserted = 0;
        for (int i = 0; i < size; i += 1000) {
            makeInsertItems(ids, i, 1000, base);
            numinserted += resolver.bulkInsert(uri, sContentValuesCache);
        }
        String message = context.getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, numinserted, Integer.valueOf(numinserted));
        Toast.makeText(context, message, 0).show();
    }

    public static Cursor query(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder, int limit) {
        try {
            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return null;
            }
            if (limit > 0) {
                uri = uri.buildUpon().appendQueryParameter("limit", "" + limit).build();
            }
            return resolver.query(uri, projection, selection, selectionArgs, sortOrder);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    public static Cursor query(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return query(context, uri, projection, selection, selectionArgs, sortOrder, 0);
    }

    public static boolean isMediaScannerScanning(Context context) {
        boolean result = false;
        Cursor cursor = query(context, MediaStore.getMediaScannerUri(), new String[]{"volume"}, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                result = "external".equals(cursor.getString(0));
            }
            cursor.close();
        }
        return result;
    }

    public static void setSpinnerState(Activity a) {
        if (isMediaScannerScanning(a)) {
            a.getWindow().setFeatureInt(5, -3);
            a.getWindow().setFeatureInt(5, -1);
        } else {
            a.getWindow().setFeatureInt(5, -2);
        }
    }

    public static void displayDatabaseError(Activity a) {
        int title;
        int message;
        if (!a.isFinishing()) {
            String status = Environment.getExternalStorageState();
            if (Environment.isExternalStorageRemovable()) {
                title = R.string.sdcard_error_title;
                message = R.string.sdcard_error_message;
            } else {
                title = R.string.sdcard_error_title_nosdcard;
                message = R.string.sdcard_error_message_nosdcard;
            }
            if (status.equals("shared") || status.equals("unmounted")) {
                if (Environment.isExternalStorageRemovable()) {
                    title = R.string.sdcard_busy_title;
                    message = R.string.sdcard_busy_message;
                } else {
                    title = R.string.sdcard_busy_title_nosdcard;
                    message = R.string.sdcard_busy_message_nosdcard;
                }
            } else if (status.equals("removed")) {
                if (Environment.isExternalStorageRemovable()) {
                    title = R.string.sdcard_missing_title;
                    message = R.string.sdcard_missing_message;
                } else {
                    title = R.string.sdcard_missing_title_nosdcard;
                    message = R.string.sdcard_missing_message_nosdcard;
                }
            } else if (status.equals("mounted")) {
                a.setTitle("");
                Intent intent = new Intent();
                intent.setClass(a, ScanningProgress.class);
                a.startActivityForResult(intent, 11);
            } else if (!TextUtils.equals(mLastSdStatus, status)) {
                mLastSdStatus = status;
                Log.d("MusicUtils", "sd card: " + status);
            }
            a.setTitle(title);
            View v = a.findViewById(R.id.sd_message);
            if (v != null) {
                v.setVisibility(0);
            }
            View v2 = a.findViewById(R.id.sd_icon);
            if (v2 != null) {
                v2.setVisibility(0);
            }
            View v3 = a.findViewById(android.R.id.list);
            if (v3 != null) {
                v3.setVisibility(8);
            }
            View v4 = a.findViewById(R.id.buttonbar);
            if (v4 != null) {
                v4.setVisibility(8);
            }
            TextView tv = (TextView) a.findViewById(R.id.sd_message);
            tv.setText(message);
        }
    }

    public static void hideDatabaseError(Activity a) {
        View v = a.findViewById(R.id.sd_message);
        if (v != null) {
            v.setVisibility(8);
        }
        View v2 = a.findViewById(R.id.sd_icon);
        if (v2 != null) {
            v2.setVisibility(8);
        }
        View v3 = a.findViewById(android.R.id.list);
        if (v3 != null) {
            v3.setVisibility(0);
        }
    }

    public static String makeTimeString(Context context, long secs) {
        String durationformat = context.getString(secs < 3600 ? R.string.durationformatshort : R.string.durationformatlong);
        sFormatBuilder.setLength(0);
        Object[] timeArgs = sTimeArgs;
        timeArgs[0] = Long.valueOf(secs / 3600);
        timeArgs[1] = Long.valueOf(secs / 60);
        timeArgs[2] = Long.valueOf((secs / 60) % 60);
        timeArgs[3] = Long.valueOf(secs);
        timeArgs[4] = Long.valueOf(secs % 60);
        return sFormatter.format(durationformat, timeArgs).toString();
    }

    public static void shuffleAll(Context context, Cursor cursor) {
        playAll(context, cursor, 0, true);
    }

    public static void playAll(Context context, Cursor cursor) {
        playAll(context, cursor, 0, false);
    }

    public static void playAll(Context context, Cursor cursor, int position) {
        playAll(context, cursor, position, false);
    }

    public static void playAll(Context context, long[] list, int position) {
        playAll(context, list, position, false);
    }

    private static void playAll(Context context, Cursor cursor, int position, boolean force_shuffle) {
        long[] list = getSongListForCursor(cursor);
        playAll(context, list, position, force_shuffle);
    }

    private static void playAll(Context context, long[] list, int position, boolean force_shuffle) {
        if (list.length == 0 || sService == null) {
            Log.d("MusicUtils", "attempt to play empty song list");
            String message = context.getString(R.string.emptyplaylist, Integer.valueOf(list.length));
            Toast.makeText(context, message, 0).show();
            return;
        }
        if (force_shuffle) {
            try {
                sService.setShuffleMode(1);
            } catch (RemoteException e) {
                return;
            } finally {
                Intent intent = new Intent("com.android.music.PLAYBACK_VIEWER").setFlags(67108864);
                context.startActivity(intent);
            }
        }
        long curid = sService.getAudioId();
        int curpos = sService.getQueuePosition();
        if (position != -1 && curpos == position && curid == list[position]) {
            long[] playlist = sService.getQueue();
            if (Arrays.equals(list, playlist)) {
                sService.play();
                return;
            }
        }
        if (position < 0) {
            position = 0;
        }
        sService.open(list, force_shuffle ? -1 : position);
        sService.play();
    }

    public static void clearQueue() {
        try {
            sService.removeTracks(0, Integer.MAX_VALUE);
        } catch (RemoteException e) {
        }
    }

    private static class FastBitmapDrawable extends Drawable {
        private Bitmap mBitmap;

        public FastBitmapDrawable(Bitmap b) {
            this.mBitmap = b;
        }

        @Override
        public void draw(Canvas canvas) {
            canvas.drawBitmap(this.mBitmap, 0.0f, 0.0f, (Paint) null);
        }

        @Override
        public int getOpacity() {
            return -1;
        }

        @Override
        public void setAlpha(int alpha) {
        }

        @Override
        public void setColorFilter(ColorFilter cf) {
        }
    }

    public static void initAlbumArtCache() {
        try {
            int id = sService.getMediaMountedCount();
            if (id != sArtCacheId) {
                clearAlbumArtCache();
                sArtCacheId = id;
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public static void clearAlbumArtCache() {
        synchronized (sArtCache) {
            sArtCache.clear();
        }
    }

    public static Drawable getCachedArtwork(Context context, long artIndex, BitmapDrawable defaultArtwork) {
        Drawable d;
        synchronized (sArtCache) {
            d = sArtCache.get(Long.valueOf(artIndex));
        }
        if (d == null) {
            d = defaultArtwork;
            Bitmap icon = defaultArtwork.getBitmap();
            int w = icon.getWidth();
            int h = icon.getHeight();
            Bitmap b = getArtworkQuick(context, artIndex, w, h);
            if (b != null) {
                d = new FastBitmapDrawable(b);
                synchronized (sArtCache) {
                    Drawable value = sArtCache.get(Long.valueOf(artIndex));
                    if (value == null) {
                        sArtCache.put(Long.valueOf(artIndex), d);
                    } else {
                        d = value;
                    }
                }
            }
        }
        return d;
    }

    private static Bitmap getArtworkQuick(Context context, long album_id, int w, int h) {
        int w2 = w - 1;
        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
        if (uri != null) {
            ParcelFileDescriptor fd = null;
            try {
                fd = res.openFileDescriptor(uri, "r");
                int sampleSize = 1;
                sBitmapOptionsCache.inJustDecodeBounds = true;
                BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, sBitmapOptionsCache);
                int nextWidth = sBitmapOptionsCache.outWidth >> 1;
                for (int nextHeight = sBitmapOptionsCache.outHeight >> 1; nextWidth > w2 && nextHeight > h; nextHeight >>= 1) {
                    sampleSize <<= 1;
                    nextWidth >>= 1;
                }
                sBitmapOptionsCache.inSampleSize = sampleSize;
                sBitmapOptionsCache.inJustDecodeBounds = false;
                Bitmap b = BitmapFactory.decodeFileDescriptor(fd.getFileDescriptor(), null, sBitmapOptionsCache);
                if (b != null && (sBitmapOptionsCache.outWidth != w2 || sBitmapOptionsCache.outHeight != h)) {
                    Bitmap tmp = Bitmap.createScaledBitmap(b, w2, h, true);
                    if (tmp != b) {
                        b.recycle();
                    }
                    b = tmp;
                }
                if (fd != null) {
                    try {
                        fd.close();
                        return b;
                    } catch (IOException e) {
                        return b;
                    }
                }
                return b;
            } catch (FileNotFoundException e2) {
                if (fd != null) {
                    try {
                        fd.close();
                    } catch (IOException e3) {
                    }
                }
            } catch (Throwable th) {
                if (fd != null) {
                    try {
                        fd.close();
                    } catch (IOException e4) {
                    }
                }
                throw th;
            }
        }
        return null;
    }

    public static Bitmap getArtwork(Context context, long song_id, long album_id) {
        return getArtwork(context, song_id, album_id, true);
    }

    public static Bitmap getArtwork(Context context, long song_id, long album_id, boolean allowdefault) {
        Bitmap bm;
        if (album_id < 0) {
            if (song_id >= 0 && (bm = getArtworkFromFile(context, song_id, -1L)) != null) {
                return bm;
            }
            if (allowdefault) {
                return getDefaultArtwork(context);
            }
            return null;
        }
        ContentResolver res = context.getContentResolver();
        Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);
        if (uri == null) {
            return null;
        }
        InputStream in = null;
        try {
            try {
                in = res.openInputStream(uri);
                Bitmap bm2 = BitmapFactory.decodeStream(in, null, sBitmapOptions);
                if (in == null) {
                    return bm2;
                }
                try {
                    in.close();
                    return bm2;
                } catch (IOException e) {
                    return bm2;
                }
            } catch (FileNotFoundException e2) {
                Bitmap bm3 = getArtworkFromFile(context, song_id, album_id);
                if (bm3 != null) {
                    if (bm3.getConfig() == null && (bm3 = bm3.copy(Bitmap.Config.RGB_565, false)) == null && allowdefault) {
                        Bitmap bm4 = getDefaultArtwork(context);
                        if (in == null) {
                            return bm4;
                        }
                        try {
                            in.close();
                            return bm4;
                        } catch (IOException e3) {
                            return bm4;
                        }
                    }
                } else if (allowdefault) {
                    bm3 = getDefaultArtwork(context);
                }
                if (in == null) {
                    return bm3;
                }
                try {
                    in.close();
                    return bm3;
                } catch (IOException e4) {
                    return bm3;
                }
            }
        } catch (Throwable th) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e5) {
                }
            }
            throw th;
        }
    }

    private static Bitmap getArtworkFromFile(Context context, long songid, long albumid) {
        Bitmap bm = null;
        if (albumid < 0 && songid < 0) {
            throw new IllegalArgumentException("Must specify an album or a song id");
        }
        try {
            if (albumid < 0) {
                Uri uri = Uri.parse("content://media/external/audio/media/" + songid + "/albumart");
                ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
                if (pfd != null) {
                    FileDescriptor fd = pfd.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd);
                }
            } else {
                Uri uri2 = ContentUris.withAppendedId(sArtworkUri, albumid);
                ParcelFileDescriptor pfd2 = context.getContentResolver().openFileDescriptor(uri2, "r");
                if (pfd2 != null) {
                    FileDescriptor fd2 = pfd2.getFileDescriptor();
                    bm = BitmapFactory.decodeFileDescriptor(fd2);
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IllegalStateException e2) {
        }
        if (bm != null) {
            mCachedBit = bm;
        }
        return bm;
    }

    private static Bitmap getDefaultArtwork(Context context) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(context.getResources().openRawResource(R.drawable.albumart_mp_unknown), null, opts);
    }

    static int getIntPref(Context context, String name, int def) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), 0);
        return prefs.getInt(name, def);
    }

    static void setIntPref(Context context, String name, int value) {
        SharedPreferences prefs = context.getSharedPreferences(context.getPackageName(), 0);
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt(name, value);
        SharedPreferencesCompat.apply(ed);
    }

    static void setRingtone(Context context, long id) {
        ContentResolver resolver = context.getContentResolver();
        Uri ringUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        try {
            ContentValues values = new ContentValues(2);
            values.put("is_ringtone", "1");
            values.put("is_alarm", "1");
            resolver.update(ringUri, values, null, null);
            String[] cols = {"_id", "_data", "title"};
            String where = "_id=" + id;
            Cursor cursor = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cols, where, null, null);
            if (cursor != null) {
                try {
                    if (cursor.getCount() == 1) {
                        cursor.moveToFirst();
                        Settings.System.putString(resolver, "ringtone", ringUri.toString());
                        String message = context.getString(R.string.ringtone_set, cursor.getString(2));
                        Toast.makeText(context, message, 0).show();
                    }
                } finally {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } catch (UnsupportedOperationException e) {
            Log.e("MusicUtils", "couldn't set ringtone flag for id " + id);
        }
    }

    static boolean updateButtonBar(Activity a, int highlight) {
        final TabWidget ll = (TabWidget) a.findViewById(R.id.buttonbar);
        boolean withtabs = false;
        Intent intent = a.getIntent();
        if (intent != null) {
            withtabs = intent.getBooleanExtra("withtabs", false);
        }
        if (highlight == 0 || !withtabs) {
            ll.setVisibility(8);
        } else {
            if (withtabs) {
                ll.setVisibility(0);
            }
            for (int i = ll.getChildCount() - 1; i >= 0; i--) {
                View v = ll.getChildAt(i);
                boolean isActive = v.getId() == highlight;
                if (isActive) {
                    ll.setCurrentTab(i);
                    sActiveTabIndex = i;
                }
                v.setTag(Integer.valueOf(i));
                v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v2, boolean hasFocus) {
                        if (hasFocus) {
                            for (int i2 = 0; i2 < ll.getTabCount(); i2++) {
                                if (ll.getChildTabViewAt(i2) == v2) {
                                    ll.setCurrentTab(i2);
                                    MusicUtils.processTabClick((Activity) ll.getContext(), v2, ll.getChildAt(MusicUtils.sActiveTabIndex).getId());
                                    return;
                                }
                            }
                        }
                    }
                });
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v2) {
                        MusicUtils.processTabClick((Activity) ll.getContext(), v2, ll.getChildAt(MusicUtils.sActiveTabIndex).getId());
                    }
                });
            }
        }
        return withtabs;
    }

    static void processTabClick(Activity a, View v, int current) {
        int id = v.getId();
        if (id != current) {
            TabWidget ll = (TabWidget) a.findViewById(R.id.buttonbar);
            activateTab(a, id);
            if (id != R.id.nowplayingtab) {
                ll.setCurrentTab(((Integer) v.getTag()).intValue());
                setIntPref(a, "activetab", id);
            }
        }
    }

    static void activateTab(Activity a, int id) {
        Intent intent = new Intent("android.intent.action.PICK");
        switch (id) {
            case R.id.artisttab:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/artistalbum");
                break;
            case R.id.albumtab:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
                break;
            case R.id.songtab:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
                break;
            case R.id.playlisttab:
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/playlist");
                break;
            case R.id.nowplayingtab:
                a.startActivity(new Intent(a, (Class<?>) MediaPlaybackActivity.class));
                return;
            default:
                return;
        }
        intent.putExtra("withtabs", true);
        intent.addFlags(67108864);
        a.startActivity(intent);
        a.finish();
        a.overridePendingTransition(0, 0);
    }

    static void updateNowPlaying(Activity a) {
        View nowPlayingView = a.findViewById(R.id.nowplaying);
        if (nowPlayingView != null) {
            try {
                Intent intent = a.getIntent();
                if (intent != null) {
                    intent.getBooleanExtra("withtabs", false);
                }
                if (sService != null && sService.getAudioId() != -1) {
                    TextView title = (TextView) nowPlayingView.findViewById(R.id.title);
                    TextView artist = (TextView) nowPlayingView.findViewById(R.id.artist);
                    title.setText(sService.getTrackName());
                    String artistName = sService.getArtistName();
                    if ("<unknown>".equals(artistName)) {
                        artistName = a.getString(R.string.unknown_artist_name);
                    }
                    artist.setText(artistName);
                    nowPlayingView.setVisibility(0);
                    nowPlayingView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Context c = v.getContext();
                            c.startActivity(new Intent(c, (Class<?>) MediaPlaybackActivity.class));
                        }
                    });
                    return;
                }
            } catch (RemoteException e) {
            }
            nowPlayingView.setVisibility(8);
        }
    }

    static void setBackground(View v, Bitmap bm) {
        if (bm == null) {
            v.setBackgroundResource(0);
            return;
        }
        int vwidth = v.getWidth();
        int vheight = v.getHeight();
        int bwidth = bm.getWidth();
        int bheight = bm.getHeight();
        float scalex = vwidth / bwidth;
        float scaley = vheight / bheight;
        float scale = Math.max(scalex, scaley) * 1.3f;
        Bitmap.Config config = Bitmap.Config.ARGB_8888;
        Bitmap bg = Bitmap.createBitmap(vwidth, vheight, config);
        Canvas c = new Canvas(bg);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        ColorMatrix greymatrix = new ColorMatrix();
        greymatrix.setSaturation(0.0f);
        ColorMatrix darkmatrix = new ColorMatrix();
        darkmatrix.setScale(0.3f, 0.3f, 0.3f, 1.0f);
        greymatrix.postConcat(darkmatrix);
        ColorFilter filter = new ColorMatrixColorFilter(greymatrix);
        paint.setColorFilter(filter);
        Matrix matrix = new Matrix();
        matrix.setTranslate((-bwidth) / 2, (-bheight) / 2);
        matrix.postRotate(10.0f);
        matrix.postScale(scale, scale);
        matrix.postTranslate(vwidth / 2, vheight / 2);
        c.drawBitmap(bm, matrix, paint);
        v.setBackgroundDrawable(new BitmapDrawable(bg));
    }

    static int getCardId(Context context) {
        ContentResolver res = context.getContentResolver();
        Cursor c = res.query(Uri.parse("content://media/external/fs_id"), null, null, null, null);
        if (c == null) {
            return -1;
        }
        c.moveToFirst();
        int id = c.getInt(0);
        c.close();
        return id;
    }

    static class LogEntry {
        Object item;
        long time = System.currentTimeMillis();

        LogEntry(Object o) {
            this.item = o;
        }

        void dump(PrintWriter out) {
            MusicUtils.sTime.set(this.time);
            out.print(MusicUtils.sTime.toString() + " : ");
            if (this.item instanceof Exception) {
                ((Exception) this.item).printStackTrace(out);
            } else {
                out.println(this.item);
            }
        }
    }

    static void debugLog(Object o) {
        sMusicLog[sLogPtr] = new LogEntry(o);
        sLogPtr++;
        if (sLogPtr >= sMusicLog.length) {
            sLogPtr = 0;
        }
    }

    static void debugDump(PrintWriter out) {
        for (int i = 0; i < sMusicLog.length; i++) {
            int idx = sLogPtr + i;
            if (idx >= sMusicLog.length) {
                idx -= sMusicLog.length;
            }
            LogEntry entry = sMusicLog[idx];
            if (entry != null) {
                entry.dump(out);
            }
        }
    }
}
