package com.android.music;

import android.app.Activity;
import android.app.SearchManager;
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
import android.os.storage.StorageManager;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;
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
    private static final String sExternalMediaUri;
    private static int sLogPtr;
    private static LogEntry[] sMusicLog;
    private static Time sTime;
    public static IMediaPlaybackService sService = null;
    private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<>();
    private static final long[] sEmptyList = new long[0];
    private static ContentValues[] sContentValuesCache = null;
    private static String sLastSdStatus = null;
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

    public static String makeAlbumsLabel(Context context, int i, int i2, boolean z) {
        StringBuilder sb = new StringBuilder();
        Resources resources = context.getResources();
        if (z) {
            if (i2 == 1) {
                sb.append(context.getString(R.string.onesong));
            } else {
                String string = resources.getQuantityText(R.plurals.Nsongs, i2).toString();
                sFormatBuilder.setLength(0);
                sFormatter.format(Locale.getDefault(), string, Integer.valueOf(i2));
                sb.append((CharSequence) sFormatBuilder);
            }
        } else {
            String string2 = resources.getQuantityText(R.plurals.Nalbums, i).toString();
            sFormatBuilder.setLength(0);
            sFormatter.format(Locale.getDefault(), string2, Integer.valueOf(i));
            sb.append((CharSequence) sFormatBuilder);
            sb.append(context.getString(R.string.albumsongseparator));
        }
        return sb.toString();
    }

    public static String makeAlbumsSongsLabel(Context context, int i, int i2, boolean z) {
        StringBuilder sb = new StringBuilder();
        if (i2 == 1) {
            sb.append(context.getString(R.string.onesong));
        } else {
            Resources resources = context.getResources();
            if (!z) {
                String string = resources.getQuantityText(R.plurals.Nalbums, i).toString();
                sFormatBuilder.setLength(0);
                sFormatter.format(Locale.getDefault(), string, Integer.valueOf(i));
                sb.append((CharSequence) sFormatBuilder);
                sb.append(context.getString(R.string.albumsongseparator));
            }
            String string2 = resources.getQuantityText(R.plurals.Nsongs, i2).toString();
            sFormatBuilder.setLength(0);
            sFormatter.format(Locale.getDefault(), string2, Integer.valueOf(i2));
            sb.append((CharSequence) sFormatBuilder);
        }
        return sb.toString();
    }

    static {
        sBitmapOptionsCache.inPreferredConfig = Bitmap.Config.RGB_565;
        sBitmapOptionsCache.inDither = false;
        sBitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;
        sBitmapOptions.inDither = false;
        sExternalMediaUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString();
        sMusicLog = new LogEntry[100];
        sLogPtr = 0;
        sTime = new Time();
    }

    public static class ServiceToken {
        ContextWrapper mWrappedContext;

        ServiceToken(ContextWrapper contextWrapper) {
            this.mWrappedContext = contextWrapper;
        }
    }

    public static ServiceToken bindToService(Activity activity) {
        return bindToService(activity, null);
    }

    public static ServiceToken bindToService(Activity activity, ServiceConnection serviceConnection) {
        Activity parent = activity.getParent();
        if (parent == null) {
            parent = activity;
        }
        MusicLogUtils.v("MusicUtils", "bindToService: activity=" + activity.toString());
        ContextWrapper contextWrapper = new ContextWrapper(parent);
        contextWrapper.startService(new Intent(contextWrapper, (Class<?>) MediaPlaybackService.class));
        ServiceBinder serviceBinder = new ServiceBinder(serviceConnection);
        if (contextWrapper.bindService(new Intent().setClass(contextWrapper, MediaPlaybackService.class), serviceBinder, 0)) {
            sConnectionMap.put(contextWrapper, serviceBinder);
            return new ServiceToken(contextWrapper);
        }
        MusicLogUtils.v("MusicUtils", "Failed to bind to service");
        return null;
    }

    public static void unbindFromService(ServiceToken serviceToken) {
        sLastSdStatus = null;
        MusicLogUtils.v("MusicUtils", "Reset mLastSdStatus to be null");
        if (serviceToken == null) {
            MusicLogUtils.v("MusicUtils", "Trying to unbind with null token");
            return;
        }
        ContextWrapper contextWrapper = serviceToken.mWrappedContext;
        ServiceBinder serviceBinderRemove = sConnectionMap.remove(contextWrapper);
        if (serviceBinderRemove == null) {
            MusicLogUtils.v("MusicUtils", "Trying to unbind for unknown Context");
            return;
        }
        contextWrapper.unbindService(serviceBinderRemove);
        if (sConnectionMap.isEmpty()) {
            sService = null;
        }
    }

    private static class ServiceBinder implements ServiceConnection {
        ServiceConnection mCallback;

        ServiceBinder(ServiceConnection serviceConnection) {
            this.mCallback = serviceConnection;
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MusicUtils.sService = IMediaPlaybackService.Stub.asInterface(iBinder);
            MusicUtils.initAlbumArtCache();
            if (this.mCallback != null) {
                this.mCallback.onServiceConnected(componentName, iBinder);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (this.mCallback != null) {
                this.mCallback.onServiceDisconnected(componentName);
            }
            MusicUtils.sService = null;
        }
    }

    public static long getCurrentAlbumId() {
        if (sService != null) {
            try {
                return sService.getAlbumId();
            } catch (RemoteException e) {
                return -1L;
            }
        }
        return -1L;
    }

    public static long getCurrentArtistId() {
        if (sService != null) {
            try {
                return sService.getArtistId();
            } catch (RemoteException e) {
                return -1L;
            }
        }
        return -1L;
    }

    public static long getCurrentAudioId() {
        if (sService != null) {
            try {
                return sService.getAudioId();
            } catch (RemoteException e) {
                return -1L;
            }
        }
        return -1L;
    }

    public static int getCurrentShuffleMode() {
        if (sService != null) {
            try {
                return sService.getShuffleMode();
            } catch (RemoteException e) {
            }
        }
        return 0;
    }

    public static void togglePartyShuffle() {
        if (sService != null) {
            try {
                if (getCurrentShuffleMode() != 2) {
                    sService.setShuffleMode(2);
                } else {
                    sService.setShuffleMode(0);
                }
            } catch (RemoteException e) {
            }
        }
    }

    public static void setPartyShuffleMenuIcon(Menu menu) {
        MenuItem menuItemFindItem = menu.findItem(8);
        if (menuItemFindItem != null) {
            if (getCurrentShuffleMode() == 2) {
                menuItemFindItem.setIcon(R.drawable.ic_menu_party_shuffle);
                menuItemFindItem.setTitle(R.string.party_shuffle_off);
            } else {
                menuItemFindItem.setIcon(R.drawable.ic_menu_party_shuffle);
                menuItemFindItem.setTitle(R.string.party_shuffle);
            }
        }
    }

    public static long[] getSongListForCursor(Cursor cursor) {
        int columnIndexOrThrow;
        if (cursor == null) {
            return sEmptyList;
        }
        int count = cursor.getCount();
        long[] jArr = new long[count];
        cursor.moveToFirst();
        try {
            columnIndexOrThrow = cursor.getColumnIndexOrThrow("audio_id");
        } catch (IllegalArgumentException e) {
            columnIndexOrThrow = cursor.getColumnIndexOrThrow("_id");
        }
        for (int i = 0; i < count; i++) {
            jArr[i] = cursor.getLong(columnIndexOrThrow);
            cursor.moveToNext();
        }
        return jArr;
    }

    public static long[] getSongListForArtist(Context context, long j) {
        Cursor cursorQuery = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "artist_id=?  AND is_music=1", new String[]{String.valueOf(j)}, "album_key,track");
        if (cursorQuery != null) {
            long[] songListForCursor = getSongListForCursor(cursorQuery);
            cursorQuery.close();
            return songListForCursor;
        }
        return sEmptyList;
    }

    public static long[] getSongListForAlbum(Context context, long j) {
        Cursor cursorQuery = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "album_id=? AND is_music=1", new String[]{String.valueOf(j)}, "track");
        if (cursorQuery != null) {
            long[] songListForCursor = getSongListForCursor(cursorQuery);
            cursorQuery.close();
            return songListForCursor;
        }
        return sEmptyList;
    }

    public static long[] getSongListForPlaylist(Context context, long j) {
        Cursor cursorQuery = query(context, MediaStore.Audio.Playlists.Members.getContentUri("external", j), new String[]{"audio_id"}, null, null, "play_order");
        if (cursorQuery != null) {
            long[] songListForCursor = getSongListForCursor(cursorQuery);
            cursorQuery.close();
            return songListForCursor;
        }
        return sEmptyList;
    }

    public static void playPlaylist(Context context, long j) {
        long[] songListForPlaylist = getSongListForPlaylist(context, j);
        if (songListForPlaylist != null) {
            playAll(context, songListForPlaylist, -1, false);
        }
    }

    public static long[] getAllSongs(Context context) {
        Cursor cursorQuery = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "is_music=1", null, "title_key");
        if (cursorQuery == null) {
            return null;
        }
        try {
            int count = cursorQuery.getCount();
            long[] jArr = new long[count];
            for (int i = 0; i < count; i++) {
                cursorQuery.moveToNext();
                jArr[i] = cursorQuery.getLong(0);
            }
            if (cursorQuery != null) {
                cursorQuery.close();
            }
            return jArr;
        } finally {
            if (cursorQuery != null) {
                cursorQuery.close();
            }
        }
    }

    public static void makePlaylistMenu(Context context, SubMenu subMenu) {
        String[] strArr = {"_id", "name"};
        ContentResolver contentResolver = context.getContentResolver();
        if (contentResolver == null) {
            System.out.println("resolver = null");
            return;
        }
        Cursor cursorQuery = contentResolver.query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, strArr, "name != ''", null, "name");
        subMenu.clear();
        subMenu.add(1, 12, 0, R.string.queue);
        subMenu.add(1, 4, 0, R.string.new_playlist);
        if (cursorQuery != null && cursorQuery.moveToFirst()) {
            while (!cursorQuery.isAfterLast()) {
                Intent intent = new Intent();
                intent.putExtra("playlist", cursorQuery.getLong(0));
                subMenu.add(1, 3, 0, cursorQuery.getString(1)).setIntent(intent);
                cursorQuery.moveToNext();
            }
        }
        if (cursorQuery != null) {
            cursorQuery.close();
        }
    }

    public static int clearPlaylist(Context context, int i) {
        try {
            try {
                return context.getContentResolver().delete(MediaStore.Audio.Playlists.Members.getContentUri("external", i), null, null);
            } catch (UnsupportedOperationException e) {
                MusicLogUtils.v("MusicUtils", "clearPlaylist() with UnsupportedOperationException:" + e);
                return -1;
            }
        } catch (Throwable th) {
            return 0;
        }
    }

    public static void removeTracks(Context context, long[] jArr) {
        if (sService == null) {
            MusicLogUtils.v("MusicUtils", "removeTracks(),sService is null");
            return;
        }
        if (!hasMountedSDcard(context)) {
            return;
        }
        try {
            for (long j : jArr) {
                sService.removeTrack(j);
            }
        } catch (RemoteException e) {
            MusicLogUtils.v("MusicUtils", "removeTracks with RemoteException", e);
        }
    }

    public static int deleteTracks(Context context, long[] jArr) {
        int iDelete;
        MusicLogUtils.v("MusicUtils", ">> deleteTracks");
        if (context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0) {
            Toast.makeText(context, R.string.music_storage_permission_deny, 0).show();
            return -1;
        }
        if (sService == null) {
            MusicLogUtils.v("MusicUtils", "deleteTracks(),sService is null");
            return 0;
        }
        String[] strArr = {"_id", "_data", "album_id"};
        StringBuilder sb = new StringBuilder();
        sb.append("_id IN (");
        for (int i = 0; i < jArr.length; i++) {
            sb.append(jArr[i]);
            if (i < jArr.length - 1) {
                sb.append(",");
            }
        }
        sb.append(")");
        if (!hasMountedSDcard(context)) {
            return 0;
        }
        Cursor cursorQuery = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, strArr, sb.toString(), null, null);
        if (cursorQuery != null) {
            if (cursorQuery.getCount() == 0) {
                MusicLogUtils.v("MusicUtils", "c.getCount()==0");
                cursorQuery.close();
                return -1;
            }
            cursorQuery.moveToFirst();
            while (!cursorQuery.isAfterLast()) {
                long j = cursorQuery.getLong(2);
                synchronized (sArtCache) {
                    sArtCache.remove(Long.valueOf(j));
                }
                cursorQuery.moveToNext();
            }
            if (hasMountedSDcard(context)) {
                try {
                    iDelete = context.getContentResolver().delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, sb.toString(), null);
                    cursorQuery.moveToFirst();
                    while (!cursorQuery.isAfterLast() && hasMountedSDcard(context)) {
                        String string = cursorQuery.getString(1);
                        try {
                            if (!new File(string).delete()) {
                                MusicLogUtils.v("MusicUtils", "Failed to delete file " + string);
                            }
                            cursorQuery.moveToNext();
                        } catch (SecurityException e) {
                            cursorQuery.moveToNext();
                        }
                    }
                    cursorQuery.close();
                } catch (UnsupportedOperationException e2) {
                    cursorQuery.close();
                    return 0;
                }
            } else {
                cursorQuery.close();
                return 0;
            }
        } else {
            iDelete = 0;
        }
        context.getContentResolver().notifyChange(Uri.parse("content://media"), null);
        MusicLogUtils.v("MusicUtils", "<< deleteTracks: num = " + iDelete);
        return iDelete;
    }

    public static void addToCurrentPlaylist(Context context, long[] jArr) {
        if (sService == null) {
            return;
        }
        try {
            sService.enqueue(jArr, 3);
            Toast.makeText(context, context.getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, jArr.length, Integer.valueOf(jArr.length)), 0).show();
        } catch (RemoteException e) {
        }
    }

    private static void makeInsertItems(long[] jArr, int i, int i2, int i3) {
        if (i + i2 > jArr.length) {
            i2 = jArr.length - i;
        }
        if (sContentValuesCache == null || sContentValuesCache.length != i2) {
            sContentValuesCache = new ContentValues[i2];
        }
        for (int i4 = 0; i4 < i2; i4++) {
            if (sContentValuesCache[i4] == null) {
                sContentValuesCache[i4] = new ContentValues();
            }
            sContentValuesCache[i4].put("play_order", Integer.valueOf(i3 + i + i4));
            sContentValuesCache[i4].put("audio_id", Long.valueOf(jArr[i + i4]));
        }
    }

    public static void addToPlaylist(Context context, long[] jArr, long j) {
        Cursor cursorQuery;
        int i;
        if (jArr == null) {
            MusicLogUtils.v("MusicUtils", "ListSelection null");
            return;
        }
        int length = jArr.length;
        ContentResolver contentResolver = context.getContentResolver();
        String[] strArr = {"play_order"};
        Cursor cursor = null;
        try {
            try {
                Uri contentUri = MediaStore.Audio.Playlists.Members.getContentUri("external", j);
                cursorQuery = contentResolver.query(contentUri, strArr, null, null, "play_order desc");
                if (cursorQuery != null) {
                    try {
                        i = cursorQuery.moveToFirst() ? cursorQuery.getInt(0) + 1 : 0;
                    } catch (UnsupportedOperationException e) {
                        e = e;
                        cursor = cursorQuery;
                        MusicLogUtils.v("MusicUtils", "addToPlaylist() with UnsupportedOperationException:" + e);
                        if (cursor != null) {
                            cursor.close();
                            return;
                        }
                        return;
                    } catch (Throwable th) {
                        th = th;
                        if (cursorQuery != null) {
                            cursorQuery.close();
                        }
                        throw th;
                    }
                }
                MusicLogUtils.v("MusicUtils", "addToPlaylist: base = " + i);
                int iBulkInsert = 0;
                for (int i2 = 0; i2 < length; i2 += 1000) {
                    makeInsertItems(jArr, i2, 1000, i);
                    iBulkInsert += contentResolver.bulkInsert(contentUri, sContentValuesCache);
                }
                Toast.makeText(context, context.getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, iBulkInsert, Integer.valueOf(iBulkInsert)), 0).show();
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
            } catch (UnsupportedOperationException e2) {
                e = e2;
            }
        } catch (Throwable th2) {
            th = th2;
            cursorQuery = cursor;
        }
    }

    public static Cursor query(Context context, Uri uri, String[] strArr, String str, String[] strArr2, String str2, int i) {
        try {
            ContentResolver contentResolver = context.getContentResolver();
            if (contentResolver == null) {
                return null;
            }
            if (i > 0) {
                uri = uri.buildUpon().appendQueryParameter("limit", "" + i).build();
            }
            return contentResolver.query(uri, strArr, str, strArr2, str2);
        } catch (UnsupportedOperationException e) {
            return null;
        }
    }

    public static Cursor query(Context context, Uri uri, String[] strArr, String str, String[] strArr2, String str2) {
        return query(context, uri, strArr, str, strArr2, str2, 0);
    }

    public static boolean isMediaScannerScanning(Context context) {
        Cursor cursorQuery = query(context, MediaStore.getMediaScannerUri(), new String[]{"volume"}, null, null, null);
        if (cursorQuery != null) {
            z = cursorQuery.getCount() > 0;
            cursorQuery.close();
        }
        return z;
    }

    public static void setSpinnerState(Activity activity) {
        if (isMediaScannerScanning(activity)) {
            activity.setProgressBarIndeterminateVisibility(true);
        } else {
            activity.setProgressBarIndeterminateVisibility(false);
        }
    }

    public static void displayDatabaseError(Activity activity, boolean z) {
        if (activity.isFinishing()) {
            return;
        }
        String externalStorageState = Environment.getExternalStorageState();
        if (sLastSdStatus != null && sLastSdStatus.equals(externalStorageState)) {
            MusicLogUtils.v("MusicUtils", "displayDatabaseError: SD status is not change");
            return;
        }
        MusicLogUtils.v("MusicUtils", "displayDatabaseError: SD status=" + externalStorageState);
        sLastSdStatus = externalStorageState;
        boolean zEquals = externalStorageState.equals("shared");
        int i = R.string.sdcard_busy_message;
        int i2 = R.string.sdcard_busy_title;
        if (!zEquals && !externalStorageState.equals("unmounted")) {
            if (externalStorageState.equals("removed")) {
                i2 = R.string.sdcard_missing_title;
                i = R.string.sdcard_missing_message;
            } else if (externalStorageState.equals("mounted") && z) {
                activity.setTitle("");
                Intent intent = new Intent();
                intent.setClass(activity, ScanningProgress.class);
                Activity parent = activity.getParent();
                if (parent != null) {
                    parent.startActivityForResult(intent, 11);
                } else {
                    activity.startActivityForResult(intent, 11);
                }
            }
        }
        activity.setTitle(i2);
        View viewFindViewById = activity.findViewById(R.id.sd_error);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(0);
        }
        View viewFindViewById2 = activity.findViewById(R.id.sd_message);
        if (viewFindViewById2 != null) {
            viewFindViewById2.setVisibility(0);
        }
        View viewFindViewById3 = activity.findViewById(R.id.sd_icon);
        if (viewFindViewById3 != null) {
            viewFindViewById3.setVisibility(0);
        }
        View viewFindViewById4 = activity.findViewById(android.R.id.list);
        if (viewFindViewById4 != null) {
            viewFindViewById4.setVisibility(8);
        }
        View viewFindViewById5 = activity.findViewById(R.id.nowplaying);
        if (viewFindViewById5 != null) {
            ((View) viewFindViewById5.getParent()).setVisibility(8);
        }
        View viewFindViewById6 = activity.findViewById(R.id.scan);
        if (viewFindViewById6 != null) {
            viewFindViewById6.setVisibility(8);
        }
        ((TextView) activity.findViewById(R.id.sd_message)).setText(i);
        Intent intent2 = new Intent("com.android.music.sdcardstatusupdate");
        intent2.putExtra("message", i);
        intent2.putExtra("onoff", false);
        activity.sendBroadcast(intent2);
    }

    public static void hideDatabaseError(Activity activity) {
        View viewFindViewById = activity.findViewById(R.id.sd_error);
        if (viewFindViewById != null) {
            viewFindViewById.setVisibility(8);
        }
        View viewFindViewById2 = activity.findViewById(R.id.sd_message);
        if (viewFindViewById2 != null) {
            viewFindViewById2.setVisibility(8);
        }
        View viewFindViewById3 = activity.findViewById(R.id.sd_icon);
        if (viewFindViewById3 != null) {
            viewFindViewById3.setVisibility(8);
        }
        View viewFindViewById4 = activity.findViewById(android.R.id.list);
        if (viewFindViewById4 != null) {
            viewFindViewById4.setVisibility(0);
        }
        Intent intent = new Intent("com.android.music.sdcardstatusupdate");
        intent.putExtra("onoff", true);
        activity.sendBroadcast(intent);
        MusicLogUtils.v("MusicUtils", "hideDatabaseError when sdcard mounted!");
    }

    public static String makeTimeString(Context context, long j) {
        String string = context.getString(j < 3600 ? R.string.durationformatshort : R.string.durationformatlong);
        sFormatBuilder.setLength(0);
        Object[] objArr = sTimeArgs;
        objArr[0] = Long.valueOf(j / 3600);
        long j2 = j / 60;
        objArr[1] = Long.valueOf(j2);
        objArr[2] = Long.valueOf(j2 % 60);
        objArr[3] = Long.valueOf(j);
        objArr[4] = Long.valueOf(j % 60);
        return sFormatter.format(Locale.getDefault(), string, objArr).toString();
    }

    public static void shuffleAll(Context context, Cursor cursor) {
        playAll(context, cursor, -1, true);
    }

    public static void playAll(Context context, Cursor cursor) {
        playAll(context, cursor, 0, false);
    }

    public static void playAll(Context context, Cursor cursor, int i) {
        playAll(context, cursor, i, false);
    }

    public static void playAll(Context context, long[] jArr, int i) {
        playAll(context, jArr, i, false);
    }

    private static void playAll(Context context, Cursor cursor, int i, boolean z) {
        playAll(context, getSongListForCursor(cursor), i, z);
    }

    private static void playAll(Context context, long[] jArr, int i, boolean z) {
        long audioId;
        int queuePosition;
        MusicLogUtils.v("MusicUtils", "Play<<" + jArr.length + ", service = " + sService);
        if (jArr.length == 0 || sService == null) {
            MusicLogUtils.v("MusicUtils", "attempt to play empty song list");
            Toast.makeText(context, context.getString(R.string.emptyplaylist, Integer.valueOf(jArr.length)), 0).show();
            return;
        }
        try {
            MusicLogUtils.v("MusicUtils", "Play 1");
            context.startActivity(new Intent().setClass(context, MediaPlaybackActivity.class));
            if (z) {
                sService.setShuffleMode(1);
            }
            audioId = sService.getAudioId();
            queuePosition = sService.getQueuePosition();
            MusicLogUtils.v("MusicUtils", "position = " + i + ", currid = " + audioId + ", curpos = " + queuePosition);
        } catch (RemoteException e) {
        }
        if (i != -1 && queuePosition == i && audioId == jArr[i] && Arrays.equals(jArr, sService.getQueue())) {
            MusicLogUtils.v("MusicUtils", "playAll: same playlist!");
            sService.play();
            return;
        }
        if (i < 0) {
            i = 0;
        }
        IMediaPlaybackService iMediaPlaybackService = sService;
        if (z) {
            i = -1;
        }
        iMediaPlaybackService.open(jArr, i);
        MusicLogUtils.v("MusicUtils", "Play>>");
    }

    public static void clearQueue() {
        try {
            sService.removeTracks(0, Integer.MAX_VALUE);
            if (sService.getShuffleMode() == 2) {
                sService.setShuffleMode(0);
            }
        } catch (RemoteException e) {
        }
    }

    private static class FastBitmapDrawable extends Drawable {
        private Bitmap mBitmap;

        public FastBitmapDrawable(Bitmap bitmap) {
            this.mBitmap = bitmap;
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
        public void setAlpha(int i) {
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
        }
    }

    public static void initAlbumArtCache() {
        try {
            int mediaMountedCount = sService.getMediaMountedCount();
            if (mediaMountedCount != sArtCacheId) {
                clearAlbumArtCache();
                sArtCacheId = mediaMountedCount;
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

    public static Drawable getCachedArtwork(Context context, long j, BitmapDrawable bitmapDrawable) {
        Drawable drawable;
        synchronized (sArtCache) {
            drawable = sArtCache.get(Long.valueOf(j));
        }
        if (drawable == null) {
            Bitmap bitmap = bitmapDrawable.getBitmap();
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Bitmap artwork = getArtwork(context, -1L, j, false);
            if (artwork == null) {
                return bitmapDrawable;
            }
            Drawable fastBitmapDrawable = new FastBitmapDrawable(Bitmap.createScaledBitmap(artwork, width, height, true));
            synchronized (sArtCache) {
                Drawable drawable2 = sArtCache.get(Long.valueOf(j));
                if (drawable2 == null) {
                    sArtCache.put(Long.valueOf(j), fastBitmapDrawable);
                } else {
                    fastBitmapDrawable = drawable2;
                }
            }
            return fastBitmapDrawable;
        }
        return drawable;
    }

    public static Bitmap getArtwork(Context context, long j, long j2, boolean z) {
        InputStream inputStreamOpenInputStream;
        Bitmap artworkFromFile;
        MusicLogUtils.v("MusicUtils", ">> getArtWork, song_id=" + j + ", album_id=" + j2);
        InputStream inputStream = null;
        if (j2 < 0) {
            if (j >= 0 && (artworkFromFile = getArtworkFromFile(context, j, -1L)) != null) {
                return artworkFromFile;
            }
            if (z) {
                return getDefaultArtwork(context);
            }
            return null;
        }
        ContentResolver contentResolver = context.getContentResolver();
        Uri uriWithAppendedId = ContentUris.withAppendedId(sArtworkUri, j2);
        try {
            if (uriWithAppendedId == null) {
                return null;
            }
            try {
                inputStreamOpenInputStream = contentResolver.openInputStream(uriWithAppendedId);
            } catch (FileNotFoundException e) {
            }
            try {
                Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(inputStreamOpenInputStream, null, sBitmapOptions);
                if (inputStreamOpenInputStream != null) {
                    try {
                        inputStreamOpenInputStream.close();
                    } catch (IOException e2) {
                    }
                }
                return bitmapDecodeStream;
            } catch (FileNotFoundException e3) {
                inputStream = inputStreamOpenInputStream;
                MusicLogUtils.d("MusicUtils", "getArtWork: open " + uriWithAppendedId.toString() + " failed, try getArtworkFromFile");
                Bitmap artworkFromFile2 = getArtworkFromFile(context, j, j2);
                if (artworkFromFile2 != null) {
                    if (artworkFromFile2.getConfig() == null && (artworkFromFile2 = artworkFromFile2.copy(Bitmap.Config.RGB_565, false)) == null && z) {
                        Bitmap defaultArtwork = getDefaultArtwork(context);
                        if (inputStream != null) {
                            try {
                                inputStream.close();
                            } catch (IOException e4) {
                            }
                        }
                        return defaultArtwork;
                    }
                } else if (z) {
                    artworkFromFile2 = getDefaultArtwork(context);
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e5) {
                    }
                }
                return artworkFromFile2;
            } catch (Throwable th) {
                th = th;
                inputStream = inputStreamOpenInputStream;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e6) {
                    }
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }

    private static Bitmap getArtworkFromFile(Context context, long j, long j2) throws Throwable {
        MusicLogUtils.v("MusicUtils", ">> getArtworkFromFile, songid=" + j + ", albumid=" + j2);
        if (j2 < 0 && j < 0) {
            throw new IllegalArgumentException("Must specify an album or a song id");
        }
        Bitmap bitmapDecodeFileDescriptor = null;
        try {
            try {
                try {
                    try {
                        if (j2 < 0) {
                            ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor = context.getContentResolver().openFileDescriptor(Uri.parse("content://media/external/audio/media/" + j + "/albumart"), "r");
                            MusicLogUtils.v("MusicUtils", "getArtworkFromFile: pFD=" + parcelFileDescriptorOpenFileDescriptor);
                            context = parcelFileDescriptorOpenFileDescriptor;
                            if (parcelFileDescriptorOpenFileDescriptor != null) {
                                FileDescriptor fileDescriptor = parcelFileDescriptorOpenFileDescriptor.getFileDescriptor();
                                if (fileDescriptor != null) {
                                    bitmapDecodeFileDescriptor = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                                    context = parcelFileDescriptorOpenFileDescriptor;
                                } else {
                                    MusicLogUtils.v("MusicUtils", "getArtworkFromFile: fd is null");
                                    context = parcelFileDescriptorOpenFileDescriptor;
                                }
                            }
                        } else {
                            ParcelFileDescriptor parcelFileDescriptorOpenFileDescriptor2 = context.getContentResolver().openFileDescriptor(ContentUris.withAppendedId(sArtworkUri, j2), "r");
                            MusicLogUtils.v("MusicUtils", "getArtworkFromFile: pFD=" + parcelFileDescriptorOpenFileDescriptor2);
                            context = parcelFileDescriptorOpenFileDescriptor2;
                            if (parcelFileDescriptorOpenFileDescriptor2 != null) {
                                FileDescriptor fileDescriptor2 = parcelFileDescriptorOpenFileDescriptor2.getFileDescriptor();
                                if (fileDescriptor2 != null) {
                                    bitmapDecodeFileDescriptor = BitmapFactory.decodeFileDescriptor(fileDescriptor2);
                                    context = parcelFileDescriptorOpenFileDescriptor2;
                                } else {
                                    MusicLogUtils.v("MusicUtils", "getArtworkFromFile: fd is null");
                                    context = parcelFileDescriptorOpenFileDescriptor2;
                                }
                            }
                        }
                    } catch (Throwable th) {
                        th = th;
                        if (context != 0) {
                            try {
                                context.close();
                            } catch (IOException e) {
                                MusicLogUtils.v("MusicUtils", "fd.close: IOException!");
                            }
                        }
                        throw th;
                    }
                } catch (IOException e2) {
                    MusicLogUtils.v("MusicUtils", "fd.close: IOException!");
                }
            } catch (FileNotFoundException e3) {
                MusicLogUtils.v("MusicUtils", "getArtworkFromFile: FileNotFoundException!");
                if (context != 0) {
                    context.close();
                }
                if (bitmapDecodeFileDescriptor != null) {
                }
                MusicLogUtils.v("MusicUtils", "<< getArtworkFromFile: " + bitmapDecodeFileDescriptor);
                return bitmapDecodeFileDescriptor;
            } catch (IllegalStateException e4) {
                if (context != 0) {
                    context.close();
                }
                if (bitmapDecodeFileDescriptor != null) {
                }
                MusicLogUtils.v("MusicUtils", "<< getArtworkFromFile: " + bitmapDecodeFileDescriptor);
                return bitmapDecodeFileDescriptor;
            }
        } catch (FileNotFoundException e5) {
            context = bitmapDecodeFileDescriptor;
            MusicLogUtils.v("MusicUtils", "getArtworkFromFile: FileNotFoundException!");
            if (context != 0) {
            }
            if (bitmapDecodeFileDescriptor != null) {
            }
            MusicLogUtils.v("MusicUtils", "<< getArtworkFromFile: " + bitmapDecodeFileDescriptor);
            return bitmapDecodeFileDescriptor;
        } catch (IllegalStateException e6) {
            context = bitmapDecodeFileDescriptor;
            if (context != 0) {
            }
            if (bitmapDecodeFileDescriptor != null) {
            }
            MusicLogUtils.v("MusicUtils", "<< getArtworkFromFile: " + bitmapDecodeFileDescriptor);
            return bitmapDecodeFileDescriptor;
        } catch (Throwable th2) {
            th = th2;
            context = bitmapDecodeFileDescriptor;
            if (context != 0) {
            }
            throw th;
        }
        if (context != 0) {
            context.close();
        }
        if (bitmapDecodeFileDescriptor != null) {
            mCachedBit = bitmapDecodeFileDescriptor;
        }
        MusicLogUtils.v("MusicUtils", "<< getArtworkFromFile: " + bitmapDecodeFileDescriptor);
        return bitmapDecodeFileDescriptor;
    }

    static Bitmap getDefaultArtwork(Context context) {
        MusicLogUtils.v("MusicUtils", "getDefaultArtwork");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeStream(context.getResources().openRawResource(R.drawable.albumart_mp_unknown), null, options);
    }

    static long getLongPref(Context context, String str, long j) {
        return context.getSharedPreferences(context.getPackageName(), 0).getLong(str, j);
    }

    static void setLongPref(Context context, String str, long j) {
        SharedPreferences.Editor editorEdit = context.getSharedPreferences(context.getPackageName(), 0).edit();
        editorEdit.putLong(str, j);
        SharedPreferencesCompat.apply(editorEdit);
    }

    static int getIntPref(Context context, String str, int i) {
        return context.getSharedPreferences(context.getPackageName(), 0).getInt(str, i);
    }

    static void setIntPref(Context context, String str, int i) {
        SharedPreferences.Editor editorEdit = context.getSharedPreferences(context.getPackageName(), 0).edit();
        editorEdit.putInt(str, i);
        SharedPreferencesCompat.apply(editorEdit);
    }

    static void showCreatePlaylistToast(int i, Context context) {
        Toast.makeText(context, context.getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, i, Integer.valueOf(i)), 0).show();
    }

    static void showDeleteToast(int i, Context context) {
        Toast.makeText(context, context.getResources().getQuantityString(R.plurals.NNNtracksdeleted, i, Integer.valueOf(i)), 0).show();
    }

    static void setRingtone(Context context, long j) {
        ContentResolver contentResolver = context.getContentResolver();
        if (context.checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") != 0 || context.checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
            Toast.makeText(context, R.string.music_storage_permission_deny, 0).show();
            return;
        }
        Uri uriWithAppendedId = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, j);
        try {
            ContentValues contentValues = new ContentValues(2);
            contentValues.put("is_ringtone", "1");
            contentValues.put("is_alarm", "1");
            contentResolver.update(uriWithAppendedId, contentValues, null, null);
            Cursor cursorQuery = query(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id", "_data", "title"}, "_id=?", new String[]{String.valueOf(j)}, null);
            if (cursorQuery != null) {
                try {
                    if (cursorQuery.getCount() == 1) {
                        cursorQuery.moveToFirst();
                        Settings.System.putString(contentResolver, "ringtone", uriWithAppendedId.toString());
                        Toast.makeText(context, context.getString(R.string.ringtone_set, cursorQuery.getString(2)), 0).show();
                    }
                } finally {
                    if (cursorQuery != null) {
                        cursorQuery.close();
                    }
                }
            }
        } catch (UnsupportedOperationException e) {
            MusicLogUtils.v("MusicUtils", "couldn't set ringtone flag for id " + j);
        }
    }

    static void updateNowPlaying(Activity activity, int i) {
        View viewFindViewById = activity.findViewById(R.id.nowplaying);
        if (viewFindViewById == null) {
            return;
        }
        MusicLogUtils.v("MusicUtils", "updateNowPlaying: activity = " + activity + ", orientaiton = " + i);
        View view = (View) viewFindViewById.getParent();
        if (i == 2 || activity.findViewById(R.id.sd_icon).getVisibility() == 0) {
            view.setVisibility(8);
            return;
        }
        View viewFindViewById2 = activity.findViewById(R.id.blank_between_search_and_overflow);
        try {
            if (sService != null && sService.getAudioId() != -1) {
                TextView textView = (TextView) viewFindViewById.findViewById(R.id.title);
                TextView textView2 = (TextView) viewFindViewById.findViewById(R.id.artist);
                textView.setText(sService.getTrackName());
                textView.setSelected(true);
                String artistName = sService.getArtistName();
                if ("<unknown>".equals(artistName)) {
                    artistName = activity.getString(R.string.unknown_artist_name);
                }
                textView2.setText(artistName);
                viewFindViewById.setVisibility(0);
                viewFindViewById.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
                        Context context = view2.getContext();
                        context.startActivity(new Intent(context, (Class<?>) MediaPlaybackActivity.class));
                    }
                });
                view.setVisibility(0);
                viewFindViewById2.setVisibility(8);
                MusicLogUtils.v("MusicUtils", "updateNowPlaying with id = " + sService.getAudioId() + ", track name = " + sService.getTrackName());
            } else {
                view.setVisibility(8);
                viewFindViewById.setVisibility(8);
            }
        } catch (RemoteException e) {
            viewFindViewById.setVisibility(8);
            MusicLogUtils.v("MusicUtils", "updateNowPlaying with RemoteException: " + e);
        }
        if (MusicBrowserActivity.class.equals(activity.getClass())) {
            View viewFindViewById3 = activity.findViewById(R.id.overflow_menu_nowplaying);
            view.setVisibility(0);
            View viewFindViewById4 = activity.findViewById(R.id.search_menu_nowplaying);
            if (viewFindViewById.getVisibility() != 0 && viewFindViewById3.getVisibility() == 0 && viewFindViewById4.getVisibility() == 0) {
                viewFindViewById2.setVisibility(0);
                return;
            }
            return;
        }
        activity.findViewById(R.id.search_menu_nowplaying).setVisibility(8);
    }

    static void setBackground(View view, Bitmap bitmap) {
        if (bitmap == null) {
            view.setBackgroundResource(0);
            return;
        }
        int width = view.getWidth();
        int height = view.getHeight();
        float fMax = Math.max(width / bitmap.getWidth(), height / bitmap.getHeight()) * 1.3f;
        if (width == 0 || height == 0) {
            view.setBackgroundResource(0);
            return;
        }
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0.0f);
        ColorMatrix colorMatrix2 = new ColorMatrix();
        colorMatrix2.setScale(0.3f, 0.3f, 0.3f, 1.0f);
        colorMatrix.postConcat(colorMatrix2);
        paint.setColorFilter(new ColorMatrixColorFilter(colorMatrix));
        Matrix matrix = new Matrix();
        matrix.setTranslate((-r3) / 2, (-r4) / 2);
        matrix.postRotate(10.0f);
        matrix.postScale(fMax, fMax);
        matrix.postTranslate(width / 2, height / 2);
        canvas.drawBitmap(bitmap, matrix, paint);
        view.setBackground(new BitmapDrawable(view.getResources(), bitmapCreateBitmap));
    }

    static class LogEntry {
        Object item;
        long time = System.currentTimeMillis();

        LogEntry(Object obj) {
            this.item = obj;
        }

        void dump(PrintWriter printWriter) {
            MusicUtils.sTime.set(this.time);
            printWriter.print(MusicUtils.sTime.toString() + " : ");
            if (this.item instanceof Exception) {
                ((Exception) this.item).printStackTrace(printWriter);
            } else {
                printWriter.println(this.item);
            }
        }
    }

    static void debugLog(Object obj) {
        sMusicLog[sLogPtr] = new LogEntry(obj);
        sLogPtr++;
        if (sLogPtr >= sMusicLog.length) {
            sLogPtr = 0;
        }
    }

    static void debugDump(PrintWriter printWriter) {
        for (int i = 0; i < sMusicLog.length; i++) {
            int length = sLogPtr + i;
            if (length >= sMusicLog.length) {
                length -= sMusicLog.length;
            }
            LogEntry logEntry = sMusicLog[length];
            if (logEntry != null) {
                logEntry.dump(printWriter);
            }
        }
    }

    public static void resetSdStatus() {
        sLastSdStatus = null;
        MusicLogUtils.v("MusicUtils", "Reset mLastSdStatus to be null to refresh database error UI!");
    }

    static boolean hasMountedSDcard(Context context) {
        String[] volumePaths;
        StorageManager storageManager = (StorageManager) context.getSystemService("storage");
        boolean z = false;
        z = false;
        if (storageManager != null && (volumePaths = storageManager.getVolumePaths()) != null) {
            int length = volumePaths.length;
            boolean z2 = false;
            for (int i = 0; i < length; i++) {
                String volumeState = storageManager.getVolumeState(volumePaths[i]);
                MusicLogUtils.v("MusicUtils", "hasMountedSDcard: path = " + volumePaths[i] + ",status = " + volumeState);
                if ("mounted".equals(volumeState)) {
                    z2 = true;
                }
            }
            z = z2;
        }
        MusicLogUtils.v("MusicUtils", "hasMountedSDcard = " + z);
        return z;
    }

    static MenuItem addSearchView(Activity activity, Menu menu, SearchView.OnQueryTextListener onQueryTextListener, SearchView.OnSuggestionListener onSuggestionListener) {
        activity.getMenuInflater().inflate(R.menu.music_search_menu, menu);
        MenuItem menuItemFindItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) menuItemFindItem.getActionView();
        if (onQueryTextListener != null) {
            searchView.setOnQueryTextListener(onQueryTextListener);
        }
        if (onSuggestionListener != null) {
            searchView.setOnSuggestionListener(onSuggestionListener);
        }
        searchView.setQueryHint(activity.getString(R.string.search_hint));
        searchView.setIconifiedByDefault(true);
        SearchManager searchManager = (SearchManager) activity.getSystemService("search");
        if (searchManager != null) {
            searchView.setSearchableInfo(searchManager.getSearchableInfo(activity.getComponentName()));
        }
        return menuItemFindItem;
    }

    static void emptyShow(ListView listView, Activity activity) {
        if (listView.getCount() == 0) {
            View viewFindViewById = activity.findViewById(R.id.scan);
            if (viewFindViewById != null) {
                viewFindViewById.setVisibility(0);
                TextView textView = (TextView) activity.findViewById(R.id.message);
                MusicLogUtils.v("MusicUtils", "empty show");
                View viewFindViewById2 = viewFindViewById.findViewById(R.id.spinner);
                View viewFindViewById3 = viewFindViewById.findViewById(R.id.message);
                if (viewFindViewById2 != null && viewFindViewById3 != null) {
                    if (isMediaScannerScanning(activity)) {
                        activity.setProgressBarIndeterminateVisibility(false);
                        textView.setText(R.string.scanning);
                        viewFindViewById2.setVisibility(0);
                        viewFindViewById3.setVisibility(0);
                    } else if (hasMountedSDcard(activity.getApplicationContext())) {
                        textView.setText(R.string.no_music_title);
                        viewFindViewById2.setVisibility(8);
                        viewFindViewById3.setVisibility(0);
                    } else {
                        MusicLogUtils.v("MusicUtils", "empty show gone");
                        textView.setText(R.string.no_music_title);
                        viewFindViewById2.setVisibility(8);
                        viewFindViewById3.setVisibility(8);
                    }
                }
            }
            listView.setEmptyView(viewFindViewById);
            return;
        }
        if (isMediaScannerScanning(activity)) {
            activity.setProgressBarIndeterminateVisibility(true);
        } else {
            activity.setProgressBarIndeterminateVisibility(false);
        }
    }

    static boolean startEffectPanel(Activity activity) {
        if (sService == null) {
            return false;
        }
        try {
            int audioSessionId = sService.getAudioSessionId();
            Intent intent = new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL");
            intent.putExtra("android.media.extra.AUDIO_SESSION", audioSessionId);
            activity.startActivityForResult(intent, 13);
            return true;
        } catch (RemoteException e) {
            MusicLogUtils.v("MusicUtils", "RemoteException in start effect  " + e);
            return false;
        }
    }

    static void setEffectPanelMenu(Context context, Menu menu) {
        menu.findItem(13).setVisible(context.getPackageManager().resolveActivity(new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL"), 0) != null);
    }

    static void resetStaticService() {
        sConnectionMap.clear();
        sService = null;
        MusicLogUtils.v("MusicUtils", "resetStaticService when service onDestroy!");
    }

    static boolean hasBoundClient() {
        if (sConnectionMap == null || sConnectionMap.isEmpty()) {
            return false;
        }
        return true;
    }

    static int idForplaylist(Context context, String str) {
        Cursor cursorQuery = query(context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{"_id", "name"}, null, null, "name");
        int i = -1;
        if (cursorQuery == null) {
            return -1;
        }
        cursorQuery.moveToFirst();
        while (true) {
            if (!cursorQuery.isAfterLast()) {
                String string = cursorQuery.getString(1);
                if (string != null && string.compareToIgnoreCase(str) == 0) {
                    i = cursorQuery.getInt(0);
                    break;
                }
                cursorQuery.moveToNext();
            } else {
                break;
            }
        }
        cursorQuery.close();
        return i;
    }

    static String makePlaylistName(Context context, String str) {
        Cursor cursorQuery = context.getContentResolver().query(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, new String[]{"name"}, "name != ''", null, "name");
        if (cursorQuery == null) {
            return null;
        }
        int i = 2;
        String str2 = String.format(str, 1);
        boolean z = false;
        while (!z) {
            cursorQuery.moveToFirst();
            z = true;
            while (!cursorQuery.isAfterLast()) {
                if (cursorQuery.getString(0).compareToIgnoreCase(str2) == 0) {
                    Object[] objArr = {Integer.valueOf(i)};
                    i++;
                    str2 = String.format(str, objArr);
                    z = false;
                }
                cursorQuery.moveToNext();
            }
        }
        cursorQuery.close();
        return str2;
    }

    public static boolean isVoiceCapable(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService("phone");
        return telephonyManager != null && telephonyManager.isVoiceCapable();
    }
}
