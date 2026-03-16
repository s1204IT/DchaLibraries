package com.android.music;

import android.app.Application;
import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.AbstractCursor;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.android.music.MusicUtils;
import com.android.music.TouchInterceptor;
import java.util.Arrays;

public class TrackBrowserActivity extends ListActivity implements ServiceConnection, View.OnCreateContextMenuListener {
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    private TrackListAdapter mAdapter;
    private String mAlbumId;
    private String mArtistId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    private String mCurrentTrackName;
    private String[] mCursorCols;
    private String mGenre;
    private String mPlaylist;
    private String[] mPlaylistMemberCols;
    private long mSelectedId;
    private int mSelectedPosition;
    private String mSortOrder;
    private MusicUtils.ServiceToken mToken;
    private Cursor mTrackCursor;
    private ListView mTrackList;
    private boolean mDeletedOneRow = false;
    private boolean mEditMode = false;
    private boolean mAdapterSent = false;
    private boolean mUseLastListPos = false;
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.MEDIA_SCANNER_STARTED".equals(action) || "android.intent.action.MEDIA_SCANNER_FINISHED".equals(action)) {
                MusicUtils.setSpinnerState(TrackBrowserActivity.this);
            }
            TrackBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (TrackBrowserActivity.this.mAdapter != null) {
                TrackBrowserActivity.this.getTrackCursor(TrackBrowserActivity.this.mAdapter.getQueryHandler(), null, true);
            }
        }
    };
    private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
        @Override
        public void drop(int from, int to) {
            if (TrackBrowserActivity.this.mTrackCursor instanceof NowPlayingCursor) {
                NowPlayingCursor c = (NowPlayingCursor) TrackBrowserActivity.this.mTrackCursor;
                c.moveItem(from, to);
                ((TrackListAdapter) TrackBrowserActivity.this.getListAdapter()).notifyDataSetChanged();
                TrackBrowserActivity.this.getListView().invalidateViews();
                TrackBrowserActivity.this.mDeletedOneRow = true;
                return;
            }
            MediaStore.Audio.Playlists.Members.moveItem(TrackBrowserActivity.this.getContentResolver(), Long.valueOf(TrackBrowserActivity.this.mPlaylist).longValue(), from, to);
        }
    };
    private TouchInterceptor.RemoveListener mRemoveListener = new TouchInterceptor.RemoveListener() {
        @Override
        public void remove(int which) {
            TrackBrowserActivity.this.removePlaylistItem(which);
        }
    };
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TrackBrowserActivity.this.getListView().invalidateViews();
            if (!TrackBrowserActivity.this.mEditMode) {
                MusicUtils.updateNowPlaying(TrackBrowserActivity.this);
            }
        }
    };
    private BroadcastReceiver mNowPlayingListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.android.music.metachanged")) {
                TrackBrowserActivity.this.getListView().invalidateViews();
                return;
            }
            if (intent.getAction().equals("com.android.music.queuechanged")) {
                if (TrackBrowserActivity.this.mDeletedOneRow) {
                    TrackBrowserActivity.this.mDeletedOneRow = false;
                    return;
                }
                if (MusicUtils.sService != null) {
                    if (TrackBrowserActivity.this.mAdapter != null) {
                        Cursor c = TrackBrowserActivity.this.new NowPlayingCursor(MusicUtils.sService, TrackBrowserActivity.this.mCursorCols);
                        if (c.getCount() != 0) {
                            TrackBrowserActivity.this.mAdapter.changeCursor(c);
                            return;
                        } else {
                            TrackBrowserActivity.this.finish();
                            return;
                        }
                    }
                    return;
                }
                TrackBrowserActivity.this.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(5);
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("withtabs", false)) {
            requestWindowFeature(1);
        }
        setVolumeControlStream(3);
        if (icicle != null) {
            this.mSelectedId = icicle.getLong("selectedtrack");
            this.mAlbumId = icicle.getString("album");
            this.mArtistId = icicle.getString("artist");
            this.mPlaylist = icicle.getString("playlist");
            this.mGenre = icicle.getString("genre");
            this.mEditMode = icicle.getBoolean("editmode", false);
        } else {
            this.mAlbumId = intent.getStringExtra("album");
            this.mArtistId = intent.getStringExtra("artist");
            this.mPlaylist = intent.getStringExtra("playlist");
            this.mGenre = intent.getStringExtra("genre");
            this.mEditMode = intent.getAction().equals("android.intent.action.EDIT");
        }
        this.mCursorCols = new String[]{"_id", "title", "_data", "album", "artist", "artist_id", "duration"};
        this.mPlaylistMemberCols = new String[]{"_id", "title", "_data", "album", "artist", "artist_id", "duration", "play_order", "audio_id", "is_music"};
        setContentView(R.layout.media_picker_activity);
        this.mUseLastListPos = MusicUtils.updateButtonBar(this, R.id.songtab);
        this.mTrackList = getListView();
        this.mTrackList.setOnCreateContextMenuListener(this);
        this.mTrackList.setCacheColorHint(0);
        if (this.mEditMode) {
            ((TouchInterceptor) this.mTrackList).setDropListener(this.mDropListener);
            ((TouchInterceptor) this.mTrackList).setRemoveListener(this.mRemoveListener);
            this.mTrackList.setDivider(null);
            this.mTrackList.setSelector(R.drawable.list_selector_background);
        } else {
            this.mTrackList.setTextFilterEnabled(true);
        }
        this.mAdapter = (TrackListAdapter) getLastNonConfigurationInstance();
        if (this.mAdapter != null) {
            this.mAdapter.setActivity(this);
            setListAdapter(this.mAdapter);
        }
        this.mToken = MusicUtils.bindToService(this, this);
        this.mTrackList.post(new Runnable() {
            @Override
            public void run() {
                TrackBrowserActivity.this.setAlbumArtBackground();
            }
        });
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        boolean z = false;
        IntentFilter f = new IntentFilter();
        f.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        f.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        f.addAction("android.intent.action.MEDIA_UNMOUNTED");
        f.addDataScheme("file");
        registerReceiver(this.mScanListener, f);
        if (this.mAdapter == null) {
            Application application = getApplication();
            int i = this.mEditMode ? R.layout.edit_track_list_item : R.layout.track_list_item;
            String[] strArr = new String[0];
            int[] iArr = new int[0];
            boolean zEquals = "nowplaying".equals(this.mPlaylist);
            if (this.mPlaylist != null && !this.mPlaylist.equals("podcasts") && !this.mPlaylist.equals("recentlyadded")) {
                z = true;
            }
            this.mAdapter = new TrackListAdapter(application, this, i, null, strArr, iArr, zEquals, z);
            setListAdapter(this.mAdapter);
            setTitle(R.string.working_songs);
            getTrackCursor(this.mAdapter.getQueryHandler(), null, true);
        } else {
            this.mTrackCursor = this.mAdapter.getCursor();
            if (this.mTrackCursor != null) {
                init(this.mTrackCursor, false);
            } else {
                setTitle(R.string.working_songs);
                getTrackCursor(this.mAdapter.getQueryHandler(), null, true);
            }
        }
        if (!this.mEditMode) {
            MusicUtils.updateNowPlaying(this);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        finish();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        TrackListAdapter a = this.mAdapter;
        this.mAdapterSent = true;
        return a;
    }

    @Override
    public void onDestroy() {
        ListView lv = getListView();
        if (lv != null) {
            if (this.mUseLastListPos) {
                mLastListPosCourse = lv.getFirstVisiblePosition();
                View cv = lv.getChildAt(0);
                if (cv != null) {
                    mLastListPosFine = cv.getTop();
                }
            }
            if (this.mEditMode) {
                ((TouchInterceptor) lv).setDropListener(null);
                ((TouchInterceptor) lv).setRemoveListener(null);
            }
        }
        MusicUtils.unbindFromService(this.mToken);
        try {
            if ("nowplaying".equals(this.mPlaylist)) {
                unregisterReceiverSafe(this.mNowPlayingListener);
            } else {
                unregisterReceiverSafe(this.mTrackListListener);
            }
        } catch (IllegalArgumentException e) {
        }
        if (!this.mAdapterSent && this.mAdapter != null) {
            this.mAdapter.changeCursor(null);
        }
        setListAdapter(null);
        this.mAdapter = null;
        unregisterReceiverSafe(this.mScanListener);
        super.onDestroy();
    }

    private void unregisterReceiverSafe(BroadcastReceiver receiver) {
        try {
            unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mTrackCursor != null) {
            getListView().invalidateViews();
        }
        MusicUtils.setSpinnerState(this);
    }

    @Override
    public void onPause() {
        this.mReScanHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putLong("selectedtrack", this.mSelectedId);
        outcicle.putString("artist", this.mArtistId);
        outcicle.putString("album", this.mAlbumId);
        outcicle.putString("playlist", this.mPlaylist);
        outcicle.putString("genre", this.mGenre);
        outcicle.putBoolean("editmode", this.mEditMode);
        super.onSaveInstanceState(outcicle);
    }

    public void init(Cursor newCursor, boolean isLimited) {
        if (this.mAdapter != null) {
            this.mAdapter.changeCursor(newCursor);
            if (this.mTrackCursor == null) {
                MusicUtils.displayDatabaseError(this);
                closeContextMenu();
                this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
                return;
            }
            MusicUtils.hideDatabaseError(this);
            this.mUseLastListPos = MusicUtils.updateButtonBar(this, R.id.songtab);
            setTitle();
            if (mLastListPosCourse >= 0 && this.mUseLastListPos) {
                ListView lv = getListView();
                lv.setAdapter(lv.getAdapter());
                lv.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
                if (!isLimited) {
                    mLastListPosCourse = -1;
                }
            }
            IntentFilter f = new IntentFilter();
            f.addAction("com.android.music.metachanged");
            f.addAction("com.android.music.queuechanged");
            if ("nowplaying".equals(this.mPlaylist)) {
                try {
                    int cur = MusicUtils.sService.getQueuePosition();
                    setSelection(cur);
                    registerReceiver(this.mNowPlayingListener, new IntentFilter(f));
                    this.mNowPlayingListener.onReceive(this, new Intent("com.android.music.metachanged"));
                    return;
                } catch (RemoteException e) {
                    return;
                }
            }
            String key = getIntent().getStringExtra("artist");
            if (key != null) {
                int keyidx = this.mTrackCursor.getColumnIndexOrThrow("artist_id");
                this.mTrackCursor.moveToFirst();
                while (true) {
                    if (this.mTrackCursor.isAfterLast()) {
                        break;
                    }
                    String artist = this.mTrackCursor.getString(keyidx);
                    if (artist.equals(key)) {
                        setSelection(this.mTrackCursor.getPosition());
                        break;
                    }
                    this.mTrackCursor.moveToNext();
                }
            }
            registerReceiver(this.mTrackListListener, new IntentFilter(f));
            this.mTrackListListener.onReceive(this, new Intent("com.android.music.metachanged"));
        }
    }

    private void setAlbumArtBackground() {
        if (!this.mEditMode) {
            try {
                long albumid = Long.valueOf(this.mAlbumId).longValue();
                Bitmap bm = MusicUtils.getArtwork(this, -1L, albumid, false);
                if (bm != null) {
                    MusicUtils.setBackground(this.mTrackList, bm);
                    this.mTrackList.setCacheColorHint(0);
                    return;
                }
            } catch (Exception e) {
            }
        }
        this.mTrackList.setBackgroundColor(-16777216);
        this.mTrackList.setCacheColorHint(0);
    }

    private void setTitle() {
        CharSequence fancyName = null;
        if (this.mAlbumId != null) {
            int numresults = this.mTrackCursor != null ? this.mTrackCursor.getCount() : 0;
            if (numresults > 0) {
                this.mTrackCursor.moveToFirst();
                int idx = this.mTrackCursor.getColumnIndexOrThrow("album");
                fancyName = this.mTrackCursor.getString(idx);
                String where = "album_id='" + this.mAlbumId + "' AND artist_id=" + this.mTrackCursor.getLong(this.mTrackCursor.getColumnIndexOrThrow("artist_id"));
                Cursor cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"album"}, where, null, null);
                if (cursor != null) {
                    if (cursor.getCount() != numresults) {
                        fancyName = this.mTrackCursor.getString(idx);
                    }
                    cursor.deactivate();
                }
                if (fancyName == null || fancyName.equals("<unknown>")) {
                    fancyName = getString(R.string.unknown_album_name);
                }
            }
        } else if (this.mPlaylist != null) {
            if (this.mPlaylist.equals("nowplaying")) {
                fancyName = MusicUtils.getCurrentShuffleMode() == 2 ? getText(R.string.partyshuffle_title) : getText(R.string.nowplaying_title);
            } else if (this.mPlaylist.equals("podcasts")) {
                fancyName = getText(R.string.podcasts_title);
            } else if (this.mPlaylist.equals("recentlyadded")) {
                fancyName = getText(R.string.recentlyadded_title);
            } else {
                String[] cols = {"name"};
                Cursor cursor2 = MusicUtils.query(this, ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, Long.valueOf(this.mPlaylist).longValue()), cols, null, null, null);
                if (cursor2 != null) {
                    if (cursor2.getCount() != 0) {
                        cursor2.moveToFirst();
                        fancyName = cursor2.getString(0);
                    }
                    cursor2.deactivate();
                }
            }
        } else if (this.mGenre != null) {
            String[] cols2 = {"name"};
            Cursor cursor3 = MusicUtils.query(this, ContentUris.withAppendedId(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, Long.valueOf(this.mGenre).longValue()), cols2, null, null, null);
            if (cursor3 != null) {
                if (cursor3.getCount() != 0) {
                    cursor3.moveToFirst();
                    fancyName = cursor3.getString(0);
                }
                cursor3.deactivate();
            }
        }
        if (fancyName != null) {
            setTitle(fancyName);
        } else {
            setTitle(R.string.tracks_title);
        }
    }

    private void removePlaylistItem(int which) {
        View v = this.mTrackList.getChildAt(which - this.mTrackList.getFirstVisiblePosition());
        if (v == null) {
            Log.d("TrackBrowser", "No view when removing playlist item " + which);
            return;
        }
        try {
            if (MusicUtils.sService != null && which != MusicUtils.sService.getQueuePosition()) {
                this.mDeletedOneRow = true;
            }
        } catch (RemoteException e) {
            this.mDeletedOneRow = true;
        }
        v.setVisibility(8);
        this.mTrackList.invalidateViews();
        if (this.mTrackCursor instanceof NowPlayingCursor) {
            ((NowPlayingCursor) this.mTrackCursor).removeItem(which);
        } else {
            int colidx = this.mTrackCursor.getColumnIndexOrThrow("_id");
            this.mTrackCursor.moveToPosition(which);
            long id = this.mTrackCursor.getLong(colidx);
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", Long.valueOf(this.mPlaylist).longValue());
            getContentResolver().delete(ContentUris.withAppendedId(uri, id), null, null);
        }
        v.setVisibility(0);
        this.mTrackList.invalidateViews();
    }

    private boolean isMusic(Cursor c) {
        int titleidx = c.getColumnIndex("title");
        int albumidx = c.getColumnIndex("album");
        int artistidx = c.getColumnIndex("artist");
        String title = c.getString(titleidx);
        String album = c.getString(albumidx);
        String artist = c.getString(artistidx);
        if ("<unknown>".equals(album) && "<unknown>".equals(artist) && title != null && title.startsWith("recording")) {
            return false;
        }
        int ismusic_idx = c.getColumnIndex("is_music");
        boolean ismusic = true;
        if (ismusic_idx >= 0) {
            ismusic = this.mTrackCursor.getInt(ismusic_idx) != 0;
        }
        return ismusic;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfoIn) {
        menu.add(0, 5, 0, R.string.play_selection);
        SubMenu sub = menu.addSubMenu(0, 1, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, sub);
        if (this.mEditMode) {
            menu.add(0, 19, 0, R.string.remove_from_playlist);
        }
        menu.add(0, 2, 0, R.string.ringtone_menu);
        menu.add(0, 10, 0, R.string.delete_item);
        AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo) menuInfoIn;
        this.mSelectedPosition = mi.position;
        this.mTrackCursor.moveToPosition(this.mSelectedPosition);
        try {
            int id_idx = this.mTrackCursor.getColumnIndexOrThrow("audio_id");
            this.mSelectedId = this.mTrackCursor.getLong(id_idx);
        } catch (IllegalArgumentException e) {
            this.mSelectedId = mi.id;
        }
        if (isMusic(this.mTrackCursor)) {
            menu.add(0, 20, 0, R.string.search_title);
        }
        this.mCurrentAlbumName = this.mTrackCursor.getString(this.mTrackCursor.getColumnIndexOrThrow("album"));
        this.mCurrentArtistNameForAlbum = this.mTrackCursor.getString(this.mTrackCursor.getColumnIndexOrThrow("artist"));
        this.mCurrentTrackName = this.mTrackCursor.getString(this.mTrackCursor.getColumnIndexOrThrow("title"));
        menu.setHeaderTitle(this.mCurrentTrackName);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String f;
        switch (item.getItemId()) {
            case 2:
                MusicUtils.setRingtone(this, this.mSelectedId);
                return true;
            case 3:
                long[] list = {this.mSelectedId};
                long playlist = item.getIntent().getLongExtra("playlist", 0L);
                MusicUtils.addToPlaylist(this, list, playlist);
                return true;
            case 4:
                Intent intent = new Intent();
                intent.setClass(this, CreatePlaylist.class);
                startActivityForResult(intent, 4);
                return true;
            case 5:
                int position = this.mSelectedPosition;
                MusicUtils.playAll(this, this.mTrackCursor, position);
                return true;
            case 6:
            case 7:
            case 8:
            case 9:
            case 11:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            default:
                return super.onContextItemSelected(item);
            case 10:
                long[] list2 = {(int) this.mSelectedId};
                Bundle b = new Bundle();
                if (Environment.isExternalStorageRemovable()) {
                    f = getString(R.string.delete_song_desc);
                } else {
                    f = getString(R.string.delete_song_desc_nosdcard);
                }
                String desc = String.format(f, this.mCurrentTrackName);
                b.putString("description", desc);
                b.putLongArray("items", list2);
                Intent intent2 = new Intent();
                intent2.setClass(this, DeleteItems.class);
                intent2.putExtras(b);
                startActivityForResult(intent2, -1);
                return true;
            case 12:
                long[] list3 = {this.mSelectedId};
                MusicUtils.addToCurrentPlaylist(this, list3);
                return true;
            case 19:
                removePlaylistItem(this.mSelectedPosition);
                return true;
            case 20:
                doSearch();
                return true;
        }
    }

    void doSearch() {
        String query;
        if (BenesseExtension.getDchaState() == 0) {
            Intent i = new Intent();
            i.setAction("android.intent.action.MEDIA_SEARCH");
            i.setFlags(268435456);
            CharSequence title = this.mCurrentTrackName;
            if ("<unknown>".equals(this.mCurrentArtistNameForAlbum)) {
                query = this.mCurrentTrackName;
            } else {
                query = this.mCurrentArtistNameForAlbum + " " + this.mCurrentTrackName;
                i.putExtra("android.intent.extra.artist", this.mCurrentArtistNameForAlbum);
            }
            if ("<unknown>".equals(this.mCurrentAlbumName)) {
                i.putExtra("android.intent.extra.album", this.mCurrentAlbumName);
            }
            i.putExtra("android.intent.extra.focus", "audio/*");
            CharSequence title2 = getString(R.string.mediasearch, new Object[]{title});
            i.putExtra("query", query);
            startActivity(Intent.createChooser(i, title2));
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int curpos = this.mTrackList.getSelectedItemPosition();
        if (this.mPlaylist != null && !this.mPlaylist.equals("recentlyadded") && curpos >= 0 && event.getMetaState() != 0 && event.getAction() == 0) {
            switch (event.getKeyCode()) {
                case 19:
                    moveItem(true);
                    return true;
                case 20:
                    moveItem(false);
                    return true;
                case 67:
                    removeItem();
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void removeItem() {
        int curcount = this.mTrackCursor.getCount();
        int curpos = this.mTrackList.getSelectedItemPosition();
        if (curcount != 0 && curpos >= 0) {
            if ("nowplaying".equals(this.mPlaylist)) {
                try {
                    if (curpos != MusicUtils.sService.getQueuePosition()) {
                        this.mDeletedOneRow = true;
                    }
                } catch (RemoteException e) {
                }
                View v = this.mTrackList.getSelectedView();
                v.setVisibility(8);
                this.mTrackList.invalidateViews();
                ((NowPlayingCursor) this.mTrackCursor).removeItem(curpos);
                v.setVisibility(0);
                this.mTrackList.invalidateViews();
                return;
            }
            int colidx = this.mTrackCursor.getColumnIndexOrThrow("_id");
            this.mTrackCursor.moveToPosition(curpos);
            long id = this.mTrackCursor.getLong(colidx);
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", Long.valueOf(this.mPlaylist).longValue());
            getContentResolver().delete(ContentUris.withAppendedId(uri, id), null, null);
            int curcount2 = curcount - 1;
            if (curcount2 == 0) {
                finish();
                return;
            }
            ListView listView = this.mTrackList;
            if (curpos >= curcount2) {
                curpos = curcount2;
            }
            listView.setSelection(curpos);
        }
    }

    private void moveItem(boolean up) {
        int curcount = this.mTrackCursor.getCount();
        int curpos = this.mTrackList.getSelectedItemPosition();
        if (!up || curpos >= 1) {
            if (up || curpos < curcount - 1) {
                if (this.mTrackCursor instanceof NowPlayingCursor) {
                    NowPlayingCursor c = (NowPlayingCursor) this.mTrackCursor;
                    c.moveItem(curpos, up ? curpos - 1 : curpos + 1);
                    ((TrackListAdapter) getListAdapter()).notifyDataSetChanged();
                    getListView().invalidateViews();
                    this.mDeletedOneRow = true;
                    if (up) {
                        this.mTrackList.setSelection(curpos - 1);
                        return;
                    } else {
                        this.mTrackList.setSelection(curpos + 1);
                        return;
                    }
                }
                int colidx = this.mTrackCursor.getColumnIndexOrThrow("play_order");
                this.mTrackCursor.moveToPosition(curpos);
                int currentplayidx = this.mTrackCursor.getInt(colidx);
                Uri baseUri = MediaStore.Audio.Playlists.Members.getContentUri("external", Long.valueOf(this.mPlaylist).longValue());
                ContentValues values = new ContentValues();
                String[] wherearg = new String[1];
                ContentResolver res = getContentResolver();
                if (up) {
                    values.put("play_order", Integer.valueOf(currentplayidx - 1));
                    wherearg[0] = this.mTrackCursor.getString(0);
                    res.update(baseUri, values, "_id=?", wherearg);
                    this.mTrackCursor.moveToPrevious();
                } else {
                    values.put("play_order", Integer.valueOf(currentplayidx + 1));
                    wherearg[0] = this.mTrackCursor.getString(0);
                    res.update(baseUri, values, "_id=?", wherearg);
                    this.mTrackCursor.moveToNext();
                }
                values.put("play_order", Integer.valueOf(currentplayidx));
                wherearg[0] = this.mTrackCursor.getString(0);
                res.update(baseUri, values, "_id=?", wherearg);
            }
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (this.mTrackCursor.getCount() != 0) {
            if ((this.mTrackCursor instanceof NowPlayingCursor) && MusicUtils.sService != null) {
                try {
                    MusicUtils.sService.setQueuePosition(position);
                    return;
                } catch (RemoteException e) {
                }
            }
            MusicUtils.playAll(this, this.mTrackCursor, position);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if (this.mPlaylist == null) {
            menu.add(0, 17, 0, R.string.play_all).setIcon(R.drawable.ic_menu_play_clip);
        }
        menu.add(0, 8, 0, R.string.party_shuffle);
        menu.add(0, 9, 0, R.string.shuffle_all).setIcon(R.drawable.ic_menu_shuffle);
        if (this.mPlaylist != null) {
            menu.add(0, 16, 0, R.string.save_as_playlist).setIcon(android.R.drawable.ic_menu_save);
            if (this.mPlaylist.equals("nowplaying")) {
                menu.add(0, 18, 0, R.string.clear_playlist).setIcon(R.drawable.ic_menu_clear_playlist);
                return true;
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MusicUtils.setPartyShuffleMenuIcon(menu);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 8:
                MusicUtils.togglePartyShuffle();
                break;
            case 9:
                Cursor cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "is_music=1", null, "title_key");
                if (cursor != null) {
                    MusicUtils.shuffleAll(this, cursor);
                    cursor.close();
                }
                return true;
            case 16:
                Intent intent = new Intent();
                intent.setClass(this, CreatePlaylist.class);
                startActivityForResult(intent, 16);
                return true;
            case 17:
                MusicUtils.playAll(this, this.mTrackCursor);
                return true;
            case 18:
                MusicUtils.clearQueue();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Uri uri;
        Uri uri2;
        switch (requestCode) {
            case 4:
                if (resultCode == -1 && (uri2 = intent.getData()) != null) {
                    long[] list = {this.mSelectedId};
                    MusicUtils.addToPlaylist(this, list, Integer.valueOf(uri2.getLastPathSegment()).intValue());
                    break;
                }
                break;
            case 11:
                if (resultCode == 0) {
                    finish();
                } else {
                    getTrackCursor(this.mAdapter.getQueryHandler(), null, true);
                }
                break;
            case 16:
                if (resultCode == -1 && (uri = intent.getData()) != null) {
                    long[] list2 = MusicUtils.getSongListForCursor(this.mTrackCursor);
                    int plid = Integer.parseInt(uri.getLastPathSegment());
                    MusicUtils.addToPlaylist(this, list2, plid);
                    break;
                }
                break;
        }
    }

    private Cursor getTrackCursor(TrackListAdapter.TrackQueryHandler queryhandler, String filter, boolean async) {
        if (queryhandler == null) {
            throw new IllegalArgumentException();
        }
        Cursor ret = null;
        this.mSortOrder = "title_key";
        StringBuilder where = new StringBuilder();
        where.append("title != ''");
        if (this.mGenre != null) {
            Uri uri = MediaStore.Audio.Genres.Members.getContentUri("external", Integer.valueOf(this.mGenre).intValue());
            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            this.mSortOrder = "title_key";
            ret = queryhandler.doQuery(uri, this.mCursorCols, where.toString(), null, this.mSortOrder, async);
        } else if (this.mPlaylist != null) {
            if (this.mPlaylist.equals("nowplaying")) {
                if (MusicUtils.sService != null) {
                    ret = new NowPlayingCursor(MusicUtils.sService, this.mCursorCols);
                    if (ret.getCount() == 0) {
                        finish();
                    }
                }
            } else if (this.mPlaylist.equals("podcasts")) {
                where.append(" AND is_podcast=1");
                Uri uri2 = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                if (!TextUtils.isEmpty(filter)) {
                    uri2 = uri2.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
                }
                ret = queryhandler.doQuery(uri2, this.mCursorCols, where.toString(), null, "title_key", async);
            } else if (this.mPlaylist.equals("recentlyadded")) {
                Uri uri3 = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                if (!TextUtils.isEmpty(filter)) {
                    uri3 = uri3.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
                }
                int X = MusicUtils.getIntPref(this, "numweeks", 2) * 604800;
                where.append(" AND date_added>");
                where.append((System.currentTimeMillis() / 1000) - ((long) X));
                ret = queryhandler.doQuery(uri3, this.mCursorCols, where.toString(), null, "title_key", async);
            } else {
                Uri uri4 = MediaStore.Audio.Playlists.Members.getContentUri("external", Long.valueOf(this.mPlaylist).longValue());
                if (!TextUtils.isEmpty(filter)) {
                    uri4 = uri4.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
                }
                this.mSortOrder = "play_order";
                ret = queryhandler.doQuery(uri4, this.mPlaylistMemberCols, where.toString(), null, this.mSortOrder, async);
            }
        } else {
            if (this.mAlbumId != null) {
                where.append(" AND album_id=" + this.mAlbumId);
                this.mSortOrder = "track, " + this.mSortOrder;
            }
            if (this.mArtistId != null) {
                where.append(" AND artist_id=" + this.mArtistId);
            }
            where.append(" AND is_music=1");
            Uri uri5 = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            if (!TextUtils.isEmpty(filter)) {
                uri5 = uri5.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            ret = queryhandler.doQuery(uri5, this.mCursorCols, where.toString(), null, this.mSortOrder, async);
        }
        if (ret != null && async) {
            init(ret, false);
            setTitle();
        }
        return ret;
    }

    private class NowPlayingCursor extends AbstractCursor {
        private String[] mCols;
        private int mCurPos;
        private Cursor mCurrentPlaylistCursor;
        private long[] mCursorIdxs;
        private long[] mNowPlaying;
        private IMediaPlaybackService mService;
        private int mSize;

        public NowPlayingCursor(IMediaPlaybackService service, String[] cols) {
            this.mCols = cols;
            this.mService = service;
            makeNowPlayingCursor();
        }

        private void makeNowPlayingCursor() {
            this.mCurrentPlaylistCursor = null;
            try {
                this.mNowPlaying = this.mService.getQueue();
            } catch (RemoteException e) {
                this.mNowPlaying = new long[0];
            }
            this.mSize = this.mNowPlaying.length;
            if (this.mSize != 0) {
                StringBuilder where = new StringBuilder();
                where.append("_id IN (");
                for (int i = 0; i < this.mSize; i++) {
                    where.append(this.mNowPlaying[i]);
                    if (i < this.mSize - 1) {
                        where.append(",");
                    }
                }
                where.append(")");
                this.mCurrentPlaylistCursor = MusicUtils.query(TrackBrowserActivity.this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this.mCols, where.toString(), null, "_id");
                if (this.mCurrentPlaylistCursor == null) {
                    this.mSize = 0;
                    return;
                }
                int size = this.mCurrentPlaylistCursor.getCount();
                this.mCursorIdxs = new long[size];
                this.mCurrentPlaylistCursor.moveToFirst();
                int colidx = this.mCurrentPlaylistCursor.getColumnIndexOrThrow("_id");
                for (int i2 = 0; i2 < size; i2++) {
                    this.mCursorIdxs[i2] = this.mCurrentPlaylistCursor.getLong(colidx);
                    this.mCurrentPlaylistCursor.moveToNext();
                }
                this.mCurrentPlaylistCursor.moveToFirst();
                this.mCurPos = -1;
                int removed = 0;
                try {
                    for (int i3 = this.mNowPlaying.length - 1; i3 >= 0; i3--) {
                        long trackid = this.mNowPlaying[i3];
                        int crsridx = Arrays.binarySearch(this.mCursorIdxs, trackid);
                        if (crsridx < 0) {
                            removed += this.mService.removeTrack(trackid);
                        }
                    }
                    if (removed > 0) {
                        this.mNowPlaying = this.mService.getQueue();
                        this.mSize = this.mNowPlaying.length;
                        if (this.mSize == 0) {
                            this.mCursorIdxs = null;
                        }
                    }
                } catch (RemoteException e2) {
                    this.mNowPlaying = new long[0];
                }
            }
        }

        @Override
        public int getCount() {
            return this.mSize;
        }

        @Override
        public boolean onMove(int oldPosition, int newPosition) {
            if (oldPosition == newPosition) {
                return true;
            }
            if (this.mNowPlaying == null || this.mCursorIdxs == null || newPosition >= this.mNowPlaying.length) {
                return false;
            }
            long newid = this.mNowPlaying[newPosition];
            int crsridx = Arrays.binarySearch(this.mCursorIdxs, newid);
            this.mCurrentPlaylistCursor.moveToPosition(crsridx);
            this.mCurPos = newPosition;
            return true;
        }

        public boolean removeItem(int which) {
            try {
                if (this.mService.removeTracks(which, which) == 0) {
                    return false;
                }
                this.mSize--;
                for (int i = which; i < this.mSize; i++) {
                    this.mNowPlaying[i] = this.mNowPlaying[i + 1];
                }
                onMove(-1, this.mCurPos);
            } catch (RemoteException e) {
            }
            return true;
        }

        public void moveItem(int from, int to) {
            try {
                this.mService.moveQueueItem(from, to);
                this.mNowPlaying = this.mService.getQueue();
                onMove(-1, this.mCurPos);
            } catch (RemoteException e) {
            }
        }

        @Override
        public String getString(int column) {
            try {
                return this.mCurrentPlaylistCursor.getString(column);
            } catch (Exception e) {
                onChange(true);
                return "";
            }
        }

        @Override
        public short getShort(int column) {
            return this.mCurrentPlaylistCursor.getShort(column);
        }

        @Override
        public int getInt(int column) {
            try {
                return this.mCurrentPlaylistCursor.getInt(column);
            } catch (Exception e) {
                onChange(true);
                return 0;
            }
        }

        @Override
        public long getLong(int column) {
            try {
                return this.mCurrentPlaylistCursor.getLong(column);
            } catch (Exception e) {
                onChange(true);
                return 0L;
            }
        }

        @Override
        public float getFloat(int column) {
            return this.mCurrentPlaylistCursor.getFloat(column);
        }

        @Override
        public double getDouble(int column) {
            return this.mCurrentPlaylistCursor.getDouble(column);
        }

        @Override
        public int getType(int column) {
            return this.mCurrentPlaylistCursor.getType(column);
        }

        @Override
        public boolean isNull(int column) {
            return this.mCurrentPlaylistCursor.isNull(column);
        }

        @Override
        public String[] getColumnNames() {
            return this.mCols;
        }

        @Override
        public void deactivate() {
            if (this.mCurrentPlaylistCursor != null) {
                this.mCurrentPlaylistCursor.deactivate();
            }
        }

        @Override
        public boolean requery() {
            makeNowPlayingCursor();
            return true;
        }
    }

    static class TrackListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        private TrackBrowserActivity mActivity;
        int mArtistIdx;
        int mAudioIdIdx;
        private final StringBuilder mBuilder;
        private String mConstraint;
        private boolean mConstraintIsValid;
        boolean mDisableNowPlayingIndicator;
        int mDurationIdx;
        private AlphabetIndexer mIndexer;
        boolean mIsNowPlaying;
        private TrackQueryHandler mQueryHandler;
        int mTitleIdx;
        private final String mUnknownAlbum;
        private final String mUnknownArtist;

        static class ViewHolder {
            CharArrayBuffer buffer1;
            char[] buffer2;
            TextView duration;
            TextView line1;
            TextView line2;
            ImageView play_indicator;

            ViewHolder() {
            }
        }

        class TrackQueryHandler extends AsyncQueryHandler {

            class QueryArgs {
                public String orderBy;
                public String[] projection;
                public String selection;
                public String[] selectionArgs;
                public Uri uri;

                QueryArgs() {
                }
            }

            TrackQueryHandler(ContentResolver res) {
                super(res);
            }

            public Cursor doQuery(Uri uri, String[] projection, String selection, String[] selectionArgs, String orderBy, boolean async) {
                if (!async) {
                    return MusicUtils.query(TrackListAdapter.this.mActivity, uri, projection, selection, selectionArgs, orderBy);
                }
                Uri limituri = uri.buildUpon().appendQueryParameter("limit", "100").build();
                QueryArgs args = new QueryArgs();
                args.uri = uri;
                args.projection = projection;
                args.selection = selection;
                args.selectionArgs = selectionArgs;
                args.orderBy = orderBy;
                startQuery(0, args, limituri, projection, selection, selectionArgs, orderBy);
                return null;
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                TrackListAdapter.this.mActivity.init(cursor, cookie != null);
                if (token == 0 && cookie != null && cursor != null && !cursor.isClosed() && cursor.getCount() >= 100) {
                    QueryArgs args = (QueryArgs) cookie;
                    startQuery(1, null, args.uri, args.projection, args.selection, args.selectionArgs, args.orderBy);
                }
            }
        }

        TrackListAdapter(Context context, TrackBrowserActivity currentactivity, int layout, Cursor cursor, String[] from, int[] to, boolean isnowplaying, boolean disablenowplayingindicator) {
            super(context, layout, cursor, from, to);
            this.mBuilder = new StringBuilder();
            this.mActivity = null;
            this.mConstraint = null;
            this.mConstraintIsValid = false;
            this.mActivity = currentactivity;
            getColumnIndices(cursor);
            this.mIsNowPlaying = isnowplaying;
            this.mDisableNowPlayingIndicator = disablenowplayingindicator;
            this.mUnknownArtist = context.getString(R.string.unknown_artist_name);
            this.mUnknownAlbum = context.getString(R.string.unknown_album_name);
            this.mQueryHandler = new TrackQueryHandler(context.getContentResolver());
        }

        public void setActivity(TrackBrowserActivity newactivity) {
            this.mActivity = newactivity;
        }

        public TrackQueryHandler getQueryHandler() {
            return this.mQueryHandler;
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                this.mTitleIdx = cursor.getColumnIndexOrThrow("title");
                this.mArtistIdx = cursor.getColumnIndexOrThrow("artist");
                this.mDurationIdx = cursor.getColumnIndexOrThrow("duration");
                try {
                    this.mAudioIdIdx = cursor.getColumnIndexOrThrow("audio_id");
                } catch (IllegalArgumentException e) {
                    this.mAudioIdIdx = cursor.getColumnIndexOrThrow("_id");
                }
                if (this.mIndexer == null) {
                    if (!this.mActivity.mEditMode && this.mActivity.mAlbumId == null) {
                        String alpha = this.mActivity.getString(R.string.fast_scroll_alphabet);
                        this.mIndexer = new MusicAlphabetIndexer(cursor, this.mTitleIdx, alpha);
                        return;
                    }
                    return;
                }
                this.mIndexer.setCursor(cursor);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            ImageView iv = (ImageView) v.findViewById(R.id.icon);
            iv.setVisibility(8);
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.duration = (TextView) v.findViewById(R.id.duration);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.buffer1 = new CharArrayBuffer(100);
            vh.buffer2 = new char[200];
            v.setTag(vh);
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder vh = (ViewHolder) view.getTag();
            cursor.copyStringToBuffer(this.mTitleIdx, vh.buffer1);
            vh.line1.setText(vh.buffer1.data, 0, vh.buffer1.sizeCopied);
            int secs = cursor.getInt(this.mDurationIdx) / 1000;
            if (secs == 0) {
                vh.duration.setText("");
            } else {
                vh.duration.setText(MusicUtils.makeTimeString(context, secs));
            }
            StringBuilder builder = this.mBuilder;
            builder.delete(0, builder.length());
            String name = cursor.getString(this.mArtistIdx);
            if (name == null || name.equals("<unknown>")) {
                builder.append(this.mUnknownArtist);
            } else {
                builder.append(name);
            }
            int len = builder.length();
            if (vh.buffer2.length < len) {
                vh.buffer2 = new char[len];
            }
            builder.getChars(0, len, vh.buffer2, 0);
            vh.line2.setText(vh.buffer2, 0, len);
            ImageView iv = vh.play_indicator;
            long id = -1;
            if (MusicUtils.sService != null) {
                try {
                    if (this.mIsNowPlaying) {
                        id = MusicUtils.sService.getQueuePosition();
                    } else {
                        id = MusicUtils.sService.getAudioId();
                    }
                } catch (RemoteException e) {
                }
            }
            if ((this.mIsNowPlaying && cursor.getPosition() == id) || (!this.mIsNowPlaying && !this.mDisableNowPlayingIndicator && cursor.getLong(this.mAudioIdIdx) == id)) {
                iv.setImageResource(R.drawable.indicator_ic_mp_playing_list);
                iv.setVisibility(0);
            } else {
                iv.setVisibility(8);
            }
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if (this.mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != this.mActivity.mTrackCursor) {
                if (this.mActivity.mTrackCursor instanceof NowPlayingCursor) {
                    this.mActivity.mTrackCursor.deactivate();
                    this.mActivity.mTrackCursor.close();
                    this.mActivity.mTrackCursor = null;
                }
                this.mActivity.mTrackCursor = cursor;
                super.changeCursor(cursor);
                getColumnIndices(cursor);
            }
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (!this.mConstraintIsValid || ((s != null || this.mConstraint != null) && (s == null || !s.equals(this.mConstraint)))) {
                Cursor trackCursor = this.mActivity.getTrackCursor(this.mQueryHandler, s, false);
                this.mConstraint = s;
                this.mConstraintIsValid = true;
                return trackCursor;
            }
            return getCursor();
        }

        @Override
        public Object[] getSections() {
            return this.mIndexer != null ? this.mIndexer.getSections() : new String[]{" "};
        }

        @Override
        public int getPositionForSection(int section) {
            if (this.mIndexer != null) {
                return this.mIndexer.getPositionForSection(section);
            }
            return 0;
        }

        @Override
        public int getSectionForPosition(int position) {
            return 0;
        }
    }
}
