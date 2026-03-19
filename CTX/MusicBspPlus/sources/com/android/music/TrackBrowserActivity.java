package com.android.music;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ListActivity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.AbstractCursor;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.drm.DrmManagerClient;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.provider.MediaStore;
import android.text.TextUtils;
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
import android.widget.SearchView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.music.IMediaPlaybackService;
import com.android.music.MusicUtils;
import com.android.music.TouchInterceptor;
import com.mediatek.omadrm.OmaDrmUtils;
import java.util.Arrays;

public class TrackBrowserActivity extends ListActivity implements DialogInterface.OnClickListener, ServiceConnection, View.OnCreateContextMenuListener {
    private TrackListAdapter mAdapter;
    private String mAlbumId;
    private String mArtistId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    private String mCurrentTrackName;
    private String[] mCursorCols;
    private String mGenre;
    private int mOrientation;
    private String mPlayListName;
    private String mPlaylist;
    private String[] mPlaylistMemberCols;
    private CharSequence mQueryText;
    MenuItem mSearchItem;
    private long mSelectedId;
    private int mSelectedPosition;
    private String mSortOrder;
    private MusicUtils.ServiceToken mToken;
    private Cursor mTrackCursor;
    private ListView mTrackList;
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    public static Long DeletedItemId = -1L;
    private int EMPTYSHOW_SPEND = 200;
    private boolean mDeletedOneRow = false;
    private boolean mEditMode = false;
    private boolean mAdapterSent = false;
    private Bitmap mAlbumArtBitmap = null;
    private AlbumArtFetcher mAsyncAlbumArtFetcher = null;
    private boolean mResetSdStatus = false;
    private IMediaPlaybackService mService = null;
    private DrmManagerClient mDrmClient = null;
    private int mCurTrackPos = -1;
    private boolean mIsMounted = true;
    private boolean mWithtabs = false;
    private boolean mIsInBackgroud = false;
    private SubMenu mSubMenu = null;
    private Menu mOptionMenu = null;
    private boolean mActvityResumAfterCreate = true;
    private Long SelectedDelId = -1L;
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MusicLogUtils.v("TrackBrowser", "mScanListener.onReceive:" + action + ", status = " + Environment.getExternalStorageState());
            if ("android.intent.action.MEDIA_SCANNER_STARTED".equals(action) || "android.intent.action.MEDIA_SCANNER_FINISHED".equals(action)) {
                TrackBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                TrackBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
                return;
            }
            if ("android.intent.action.MEDIA_UNMOUNTED".equals(action)) {
                if (!MusicUtils.hasMountedSDcard(TrackBrowserActivity.this.getApplicationContext()) && TrackBrowserActivity.this.mOptionMenu != null && TrackBrowserActivity.this.mOptionMenu.findItem(18) != null) {
                    TrackBrowserActivity.this.mOptionMenu.findItem(18).setVisible(false);
                }
                TrackBrowserActivity.this.mIsMounted = false;
                TrackBrowserActivity.this.mResetSdStatus = true;
                TrackBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                TrackBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
                TrackBrowserActivity.this.closeContextMenu();
                TrackBrowserActivity.this.closeOptionsMenu();
                if (TrackBrowserActivity.this.mSubMenu != null) {
                    TrackBrowserActivity.this.mSubMenu.close();
                }
                TrackBrowserActivity.this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
                return;
            }
            if ("android.intent.action.MEDIA_MOUNTED".equals(action)) {
                if (TrackBrowserActivity.this.mOptionMenu != null && TrackBrowserActivity.this.mOptionMenu.findItem(18) != null) {
                    TrackBrowserActivity.this.mOptionMenu.findItem(18).setVisible(true);
                }
                TrackBrowserActivity.this.mIsMounted = true;
                TrackBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                TrackBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
            }
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (TrackBrowserActivity.this.mAdapter != null) {
                TrackBrowserActivity.this.getTrackCursor(TrackBrowserActivity.this.mAdapter.getQueryHandler(), null, true);
            }
        }
    };
    public Handler mListHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            TrackBrowserActivity.this.refreshEmptyView();
        }
    };
    private TouchInterceptor.DropListener mDropListener = new TouchInterceptor.DropListener() {
        @Override
        public void drop(int i, int i2) {
            if (!(TrackBrowserActivity.this.mTrackCursor instanceof NowPlayingCursor)) {
                boolean zMoveItem = MediaStore.Audio.Playlists.Members.moveItem(TrackBrowserActivity.this.getContentResolver(), Long.valueOf(TrackBrowserActivity.this.mPlaylist).longValue(), i, i2);
                if (!zMoveItem) {
                    ((TouchInterceptor) TrackBrowserActivity.this.mTrackList).resetPredrawStatus();
                }
                MusicLogUtils.v("TrackBrowser", "drop: from = " + i + ", to = " + i2 + ", isSuccesss = " + zMoveItem);
                return;
            }
            ((NowPlayingCursor) TrackBrowserActivity.this.mTrackCursor).moveItem(i, i2);
            ((TrackListAdapter) TrackBrowserActivity.this.getListAdapter()).notifyDataSetChanged();
            ((TouchInterceptor) TrackBrowserActivity.this.mTrackList).resetPredrawStatus();
            TrackBrowserActivity.this.getListView().invalidateViews();
            TrackBrowserActivity.this.mDeletedOneRow = true;
        }
    };
    private TouchInterceptor.RemoveListener mRemoveListener = new TouchInterceptor.RemoveListener() {
        @Override
        public void remove(int i) {
            TrackBrowserActivity.this.removePlaylistItem(i);
        }
    };
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TrackBrowserActivity.this.getListView().invalidateViews();
            if (TrackBrowserActivity.this.mService != null) {
                MusicUtils.updateNowPlaying(TrackBrowserActivity.this, TrackBrowserActivity.this.mOrientation);
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
                        NowPlayingCursor nowPlayingCursor = TrackBrowserActivity.this.new NowPlayingCursor(MusicUtils.sService, TrackBrowserActivity.this.mCursorCols);
                        if (nowPlayingCursor.getCount() != 0) {
                            TrackBrowserActivity.this.mAdapter.changeCursor(nowPlayingCursor);
                            return;
                        } else {
                            TrackBrowserActivity.this.finish();
                            return;
                        }
                    }
                    return;
                }
                TrackBrowserActivity.this.finish();
                return;
            }
            if (intent.getAction().equals("com.android.music.quitplayback")) {
                TrackBrowserActivity.this.finish();
            }
        }
    };
    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String str) {
            Intent intent = new Intent();
            intent.setClass(TrackBrowserActivity.this, QueryBrowserActivity.class);
            intent.putExtra("query", str);
            TrackBrowserActivity.this.startActivity(intent);
            TrackBrowserActivity.this.mSearchItem.collapseActionView();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String str) {
            return false;
        }
    };
    private TouchInterceptor.UpgradeAlbumArtListener mUpgradeAlbumArtListener = new TouchInterceptor.UpgradeAlbumArtListener() {
        @Override
        public void UpgradeAlbumArt() {
            TrackBrowserActivity.this.setAlbumArtBackground();
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        MusicLogUtils.v("TrackBrowser", "onCreate");
        requestWindowFeature(5);
        Intent intent = getIntent();
        if (intent != null) {
            this.mWithtabs = intent.getBooleanExtra("withtabs", false);
            if (this.mWithtabs) {
                requestWindowFeature(1);
            } else {
                getActionBar().setDisplayHomeAsUpEnabled(true);
            }
        }
        setVolumeControlStream(3);
        if (bundle != null) {
            this.mSelectedId = bundle.getLong("selectedtrack");
            this.mAlbumId = bundle.getString("album");
            this.mArtistId = bundle.getString("artist");
            this.mPlaylist = bundle.getString("playlist");
            this.mGenre = bundle.getString("genre");
            this.mEditMode = bundle.getBoolean("editmode", false);
            this.mCurTrackPos = bundle.getInt("curtrackpos");
        } else {
            this.mSelectedId = intent.getLongExtra("selectedtrack", 0L);
            this.mAlbumId = intent.getStringExtra("album");
            this.mArtistId = intent.getStringExtra("artist");
            this.mPlaylist = intent.getStringExtra("playlist");
            this.mGenre = intent.getStringExtra("genre");
            this.mEditMode = intent.getAction().equals("android.intent.action.EDIT");
        }
        this.mCursorCols = new String[]{"_id", "title", "_data", "album", "artist", "artist_id", "duration", "is_drm"};
        this.mPlaylistMemberCols = new String[]{"_id", "title", "_data", "album", "artist", "artist_id", "duration", "play_order", "audio_id", "is_music", "is_drm"};
        if (!this.mWithtabs) {
            ((NotificationManager) getSystemService("notification")).createNotificationChannel(new NotificationChannel("music_notification_channel", "MUSIC", 2));
        }
        if (this.mWithtabs || this.mEditMode) {
            setContentView(R.layout.media_picker_activity);
        } else {
            setContentView(R.layout.media_picker_activity_nowplaying);
            this.mOrientation = getResources().getConfiguration().orientation;
        }
        if (!this.mWithtabs) {
            this.mToken = MusicUtils.bindToService(this, this);
        }
        this.mTrackList = getListView();
        this.mTrackList.setOnCreateContextMenuListener(this);
        this.mTrackList.setCacheColorHint(0);
        if (!this.mEditMode) {
            this.mTrackList.setTextFilterEnabled(true);
        } else {
            ((TouchInterceptor) this.mTrackList).setDropListener(this.mDropListener);
            ((TouchInterceptor) this.mTrackList).setRemoveListener(this.mRemoveListener);
            ((TouchInterceptor) this.mTrackList).registerContentObserver(getApplicationContext());
        }
        if (getApplicationContext().checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == 0) {
            MusicLogUtils.d("TrackBrowser", "onCreate AlbumArt Permission Granted");
            if (!this.mWithtabs && !this.mEditMode) {
                ((TouchInterceptor) this.mTrackList).setUpgradeAlbumArtListener(this.mUpgradeAlbumArtListener);
                setAlbumArtBackground();
            }
        }
        if (OmaDrmUtils.isOmaDrmEnabled()) {
            this.mDrmClient = new DrmManagerClient(this);
        }
        initAdapter();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addDataScheme("file");
        registerReceiver(this.mScanListener, intentFilter);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        this.mService = IMediaPlaybackService.Stub.asInterface(iBinder);
        MusicUtils.updateNowPlaying(this, this.mOrientation);
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        this.mService = null;
        finish();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        TrackListAdapter trackListAdapter = this.mAdapter;
        this.mAdapterSent = true;
        return trackListAdapter;
    }

    @Override
    public void onDestroy() {
        MusicLogUtils.v("TrackBrowser", "onDestroy");
        if (this.mAsyncAlbumArtFetcher != null && !this.mAsyncAlbumArtFetcher.isCancelled()) {
            this.mAsyncAlbumArtFetcher.cancel(true);
        }
        ListView listView = getListView();
        if (listView != null) {
            if (this.mWithtabs) {
                mLastListPosCourse = listView.getFirstVisiblePosition();
                View childAt = listView.getChildAt(0);
                if (childAt != null) {
                    mLastListPosFine = childAt.getTop();
                }
            }
            if (this.mEditMode) {
                TouchInterceptor touchInterceptor = (TouchInterceptor) listView;
                touchInterceptor.setDropListener(null);
                touchInterceptor.setRemoveListener(null);
                touchInterceptor.unregisterContentObserver(getApplicationContext());
            }
        }
        MusicUtils.unbindFromService(this.mToken);
        this.mService = null;
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
        if (this.mDrmClient != null) {
            this.mDrmClient.release();
            this.mDrmClient = null;
        }
        super.onDestroy();
    }

    private void unregisterReceiverSafe(BroadcastReceiver broadcastReceiver) {
        try {
            unregisterReceiver(broadcastReceiver);
        } catch (IllegalArgumentException e) {
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MusicLogUtils.v("TrackBrowser", "onResume>>>");
        if (this.mTrackCursor != null) {
            if (this.mEditMode || (this.mPlaylist != null && this.mPlaylist.equals("recentlyadded"))) {
                MusicLogUtils.v("TrackBrowser", "need to requery data!");
                getTrackCursor(this.mAdapter.getQueryHandler(), null, true);
            }
            getListView().invalidateViews();
        }
        MusicUtils.setSpinnerState(this);
        this.mIsInBackgroud = false;
        if (this.mService != null) {
            MusicUtils.updateNowPlaying(this, this.mOrientation);
        }
        if (!this.mActvityResumAfterCreate) {
            refreshEmptyView();
        }
        MusicLogUtils.v("TrackBrowser", "onResume<<<");
    }

    public void refreshEmptyView() {
        if (getListView().getAdapter() != null && getListView().getAdapter().getCount() == 0 && getListView().getCount() != 0) {
            MusicLogUtils.v("TrackBrowser", "getExpandableListView().getCount() = " + getListView().getCount());
            this.mListHandler.sendEmptyMessageDelayed(0, (long) this.EMPTYSHOW_SPEND);
            return;
        }
        MusicUtils.emptyShow(getListView(), this);
    }

    @Override
    public void onPause() {
        MusicLogUtils.v("TrackBrowser", "onPause");
        this.mActvityResumAfterCreate = false;
        if (this.mSubMenu != null) {
            this.mSubMenu.close();
        }
        this.mReScanHandler.removeCallbacksAndMessages(null);
        Intent intent = new Intent(getIntent());
        intent.putExtra("selectedtrack", this.mSelectedId);
        intent.putExtra("curtrackpos", this.mCurTrackPos);
        setIntent(intent);
        this.mIsInBackgroud = true;
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putLong("selectedtrack", this.mSelectedId);
        bundle.putString("artist", this.mArtistId);
        bundle.putString("album", this.mAlbumId);
        bundle.putString("playlist", this.mPlaylist);
        bundle.putString("genre", this.mGenre);
        bundle.putBoolean("editmode", this.mEditMode);
        bundle.putInt("curtrackpos", this.mCurTrackPos);
        super.onSaveInstanceState(bundle);
    }

    public void init(Cursor cursor, boolean z) {
        if (this.mAdapter == null) {
            return;
        }
        this.mAdapter.changeCursor(cursor);
        if (this.mTrackCursor == null) {
            if (this.mResetSdStatus) {
                MusicUtils.resetSdStatus();
                this.mResetSdStatus = false;
            }
            MusicUtils.displayDatabaseError(this, this.mIsMounted);
            closeContextMenu();
            this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
            return;
        }
        MusicUtils.emptyShow(getListView(), this);
        MusicUtils.hideDatabaseError(this);
        setTitle();
        if (mLastListPosCourse >= 0 && this.mWithtabs) {
            ListView listView = getListView();
            listView.setAdapter(listView.getAdapter());
            listView.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
            if (!z) {
                mLastListPosCourse = -1;
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.music.metachanged");
        intentFilter.addAction("com.android.music.queuechanged");
        if ("nowplaying".equals(this.mPlaylist)) {
            try {
                intentFilter.addAction("com.android.music.quitplayback");
                setSelection(MusicUtils.sService.getQueuePosition());
                registerReceiver(this.mNowPlayingListener, new IntentFilter(intentFilter));
                this.mNowPlayingListener.onReceive(this, new Intent("com.android.music.metachanged"));
                return;
            } catch (RemoteException e) {
                return;
            }
        }
        String stringExtra = getIntent().getStringExtra("artist");
        if (stringExtra != null) {
            int columnIndexOrThrow = this.mTrackCursor.getColumnIndexOrThrow("artist_id");
            this.mTrackCursor.moveToFirst();
            while (true) {
                if (this.mTrackCursor.isAfterLast()) {
                    break;
                }
                if (this.mTrackCursor.getString(columnIndexOrThrow).equals(stringExtra)) {
                    setSelection(this.mTrackCursor.getPosition());
                    break;
                }
                this.mTrackCursor.moveToNext();
            }
        }
        registerReceiver(this.mTrackListListener, new IntentFilter(intentFilter));
        this.mTrackListListener.onReceive(this, new Intent("com.android.music.metachanged"));
    }

    private void setAlbumArtBackground() {
        MusicLogUtils.v("TrackBrowser", "setAlbumArtBackground: mAlbumId = " + this.mAlbumId + ", mAlbumArtBitmap = " + this.mAlbumArtBitmap);
        if (this.mAlbumId == null) {
            return;
        }
        if (this.mAlbumArtBitmap != null) {
            MusicUtils.setBackground((View) this.mTrackList.getParent().getParent(), this.mAlbumArtBitmap);
            this.mTrackList.setCacheColorHint(0);
            return;
        }
        if (this.mAsyncAlbumArtFetcher != null && !this.mAsyncAlbumArtFetcher.isCancelled()) {
            this.mAsyncAlbumArtFetcher.cancel(true);
        }
        this.mAsyncAlbumArtFetcher = new AlbumArtFetcher();
        try {
            this.mAsyncAlbumArtFetcher.execute(Long.valueOf(this.mAlbumId));
        } catch (IllegalStateException e) {
            MusicLogUtils.v("TrackBrowser", "Exception while fetching album art!!", e);
        }
    }

    private void setTitle() {
        Cursor cursorQuery;
        CharSequence text;
        CharSequence string = null;
        if (this.mAlbumId != null) {
            int count = this.mTrackCursor != null ? this.mTrackCursor.getCount() : 0;
            if (count > 0) {
                this.mTrackCursor.moveToFirst();
                int columnIndexOrThrow = this.mTrackCursor.getColumnIndexOrThrow("album");
                String string2 = this.mTrackCursor.getString(columnIndexOrThrow);
                Cursor cursorQuery2 = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"album"}, "album_id=?  AND artist_id=? ", new String[]{String.valueOf(this.mAlbumId), String.valueOf(this.mTrackCursor.getLong(this.mTrackCursor.getColumnIndexOrThrow("artist_id")))}, null);
                if (cursorQuery2 != null) {
                    if (cursorQuery2.getCount() != count) {
                        string2 = this.mTrackCursor.getString(columnIndexOrThrow);
                    }
                    cursorQuery2.close();
                }
                if (string2 == null || string2.equals("<unknown>")) {
                    string = getString(R.string.unknown_album_name);
                } else {
                    string = string2;
                }
            }
        } else if (this.mPlaylist != null) {
            if (this.mPlaylist.equals("nowplaying")) {
                if (MusicUtils.getCurrentShuffleMode() == 2) {
                    text = getText(R.string.partyshuffle_title);
                } else {
                    text = getText(R.string.nowplaying_title);
                }
            } else if (this.mPlaylist.equals("podcasts")) {
                text = getText(R.string.podcasts_title);
            } else if (this.mPlaylist.equals("recentlyadded")) {
                text = getText(R.string.recentlyadded_title);
            } else {
                Cursor cursorQuery3 = MusicUtils.query(this, ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, Long.valueOf(this.mPlaylist).longValue()), new String[]{"name"}, null, null, null);
                if (cursorQuery3 != null) {
                    if (cursorQuery3.getCount() != 0) {
                        cursorQuery3.moveToFirst();
                        string = cursorQuery3.getString(0);
                    }
                    cursorQuery3.close();
                }
                if (string != null) {
                    this.mPlayListName = string.toString();
                }
            }
            string = text;
            if (string != null) {
            }
        } else if (this.mGenre != null && (cursorQuery = MusicUtils.query(this, ContentUris.withAppendedId(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI, Long.valueOf(this.mGenre).longValue()), new String[]{"name"}, null, null, null)) != null) {
            if (cursorQuery.getCount() != 0) {
                cursorQuery.moveToFirst();
                string = cursorQuery.getString(0);
            }
            cursorQuery.close();
        }
        if (!this.mWithtabs) {
            if (string != null) {
                setTitle(string);
            } else {
                setTitle(R.string.tracks_title);
            }
        }
    }

    private void removePlaylistItem(int i) {
        View childAt = this.mTrackList.getChildAt(i - this.mTrackList.getFirstVisiblePosition());
        if (childAt == null) {
            MusicLogUtils.v("TrackBrowser", "No view when removing playlist item " + i);
            return;
        }
        try {
            if (MusicUtils.sService != null && i != MusicUtils.sService.getQueuePosition()) {
                this.mDeletedOneRow = true;
            }
        } catch (RemoteException e) {
            this.mDeletedOneRow = true;
        }
        childAt.setVisibility(8);
        this.mTrackList.invalidateViews();
        if (this.mTrackCursor instanceof NowPlayingCursor) {
            ((NowPlayingCursor) this.mTrackCursor).removeItem(i);
            ((TrackListAdapter) getListAdapter()).notifyDataSetChanged();
        } else {
            int columnIndexOrThrow = this.mTrackCursor.getColumnIndexOrThrow("_id");
            this.mTrackCursor.moveToPosition(i);
            getContentResolver().delete(ContentUris.withAppendedId(MediaStore.Audio.Playlists.Members.getContentUri("external", Long.valueOf(this.mPlaylist).longValue()), this.mTrackCursor.getLong(columnIndexOrThrow)), null, null);
        }
        childAt.setVisibility(0);
        this.mTrackList.invalidateViews();
    }

    private boolean isMusic(Cursor cursor) {
        int columnIndex = cursor.getColumnIndex("title");
        int columnIndex2 = cursor.getColumnIndex("album");
        int columnIndex3 = cursor.getColumnIndex("artist");
        String string = cursor.getString(columnIndex);
        String string2 = cursor.getString(columnIndex2);
        String string3 = cursor.getString(columnIndex3);
        if ("<unknown>".equals(string2) && "<unknown>".equals(string3) && string != null && string.startsWith("recording")) {
            return false;
        }
        int columnIndex4 = cursor.getColumnIndex("is_music");
        return columnIndex4 < 0 || this.mTrackCursor.getInt(columnIndex4) != 0;
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        contextMenu.add(0, 5, 0, R.string.play_selection);
        this.mSubMenu = contextMenu.addSubMenu(0, 1, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, this.mSubMenu);
        if (this.mEditMode) {
            contextMenu.add(0, 21, 0, R.string.remove_from_playlist);
        }
        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) contextMenuInfo;
        this.mSelectedPosition = adapterContextMenuInfo.position;
        this.mTrackCursor.moveToPosition(this.mSelectedPosition);
        try {
            this.mSelectedId = this.mTrackCursor.getLong(this.mTrackCursor.getColumnIndexOrThrow("audio_id"));
        } catch (IllegalArgumentException e) {
            this.mSelectedId = adapterContextMenuInfo.id;
        }
        int i = this.mTrackCursor.getInt(this.mTrackCursor.getColumnIndexOrThrow("is_drm"));
        boolean zIsVoiceCapable = MusicUtils.isVoiceCapable(this);
        if (UserHandle.myUserId() == 0) {
            if (OmaDrmUtils.isOmaDrmEnabled()) {
                if (zIsVoiceCapable && canDispalyRingtone(i, 0)) {
                    contextMenu.add(0, 2, 0, R.string.ringtone_menu);
                }
            } else if (zIsVoiceCapable) {
                contextMenu.add(0, 2, 0, R.string.ringtone_menu);
            }
        }
        contextMenu.add(0, 10, 0, R.string.delete_item);
        if (isMusic(this.mTrackCursor)) {
            contextMenu.add(0, 22, 0, R.string.search_title);
        }
        this.mCurrentAlbumName = this.mTrackCursor.getString(this.mTrackCursor.getColumnIndexOrThrow("album"));
        this.mCurrentArtistNameForAlbum = this.mTrackCursor.getString(this.mTrackCursor.getColumnIndexOrThrow("artist"));
        this.mCurrentTrackName = this.mTrackCursor.getString(this.mTrackCursor.getColumnIndexOrThrow("title"));
        contextMenu.setHeaderTitle(this.mCurrentTrackName);
        if (OmaDrmUtils.isOmaDrmEnabled() && i == 1) {
            contextMenu.add(0, 15, 0, 134545506);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 10) {
            showDeleteDialog(Long.valueOf(this.mSelectedId), R.string.delete_song_desc, this.mCurrentTrackName);
            return true;
        }
        if (itemId == 12) {
            MusicUtils.addToCurrentPlaylist(this, new long[]{this.mSelectedId});
            return true;
        }
        if (itemId != 15) {
            switch (itemId) {
                case 2:
                    MusicUtils.setRingtone(this, this.mSelectedId);
                    return true;
                case 3:
                    MusicUtils.addToPlaylist(this, new long[]{this.mSelectedId}, menuItem.getIntent().getLongExtra("playlist", 0L));
                    return true;
                case 4:
                    getParent();
                    showCreateDialog(String.valueOf(this.mSelectedId), 2, "new_playlist", null);
                    return true;
                case 5:
                    int i = this.mSelectedPosition;
                    if (checkDrmRightsForPlay(this.mTrackCursor, i, false, true)) {
                        MusicUtils.playAll(this, this.mTrackCursor, i);
                    }
                    return true;
                default:
                    switch (itemId) {
                        case 21:
                            removePlaylistItem(this.mSelectedPosition);
                            this.mReScanHandler.removeCallbacksAndMessages(null);
                            this.mReScanHandler.sendEmptyMessage(0);
                            return true;
                        case 22:
                            doSearch();
                            return true;
                        default:
                            return super.onContextItemSelected(menuItem);
                    }
            }
        }
        if (OmaDrmUtils.isOmaDrmEnabled()) {
            OmaDrmUtils.showProtectionInfoDialog(this, this.mDrmClient, ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this.mSelectedId));
        }
        return true;
    }

    private void showCreateDialog(String str, int i, String str2, long[] jArr) {
        MusicLogUtils.v("TrackBrowser", "showCreateDialog>>");
        removeOldFragmentByTag("Create");
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.v("TrackBrowser", "<showCreateDialog> fragmentManager = " + fragmentManager);
        CreatePlaylist createPlaylistNewInstance = CreatePlaylist.newInstance(str, i, str2, null);
        CreatePlaylist.setCursor(jArr);
        createPlaylistNewInstance.show(fragmentManager, "Create");
        fragmentManager.executePendingTransactions();
    }

    private void showDeleteDialog(Long l, int i, String str) {
        MusicLogUtils.v("TrackBrowser", "showDeleteDialog>>");
        removeOldFragmentByTag("Delete");
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.v("TrackBrowser", "<showDeleteDialog> fragmentManager = " + fragmentManager);
        DeleteDialogFragment.newInstance(true, l.longValue(), i, str).show(fragmentManager, "Delete");
        fragmentManager.executePendingTransactions();
        this.SelectedDelId = l;
    }

    private void removeOldFragmentByTag(String str) {
        MusicLogUtils.v("TrackBrowser", "<removeOldFragmentByTag> tag = " + str);
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.v("TrackBrowser", "<removeOldFragmentByTag> fragmentManager = " + fragmentManager);
        DialogFragment dialogFragment = (DialogFragment) fragmentManager.findFragmentByTag(str);
        MusicLogUtils.v("TrackBrowser", "<removeOldFragmentByTag> oldFragment = " + dialogFragment);
        if (dialogFragment != null) {
            dialogFragment.dismissAllowingStateLoss();
        }
        this.SelectedDelId = -1L;
    }

    void doSearch() {
        String str;
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction("android.intent.action.MEDIA_SEARCH");
        intent.setFlags(268435456);
        String str2 = this.mCurrentTrackName;
        if ("<unknown>".equals(this.mCurrentArtistNameForAlbum)) {
            str = this.mCurrentTrackName;
        } else {
            str = this.mCurrentArtistNameForAlbum + " " + this.mCurrentTrackName;
            intent.putExtra("android.intent.extra.artist", this.mCurrentArtistNameForAlbum);
        }
        if ("<unknown>".equals(this.mCurrentAlbumName)) {
            intent.putExtra("android.intent.extra.album", this.mCurrentAlbumName);
        }
        intent.putExtra("android.intent.extra.focus", "audio/*");
        String string = getString(R.string.mediasearch, new Object[]{str2});
        intent.putExtra("query", str);
        startActivity(Intent.createChooser(intent, string));
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        int selectedItemPosition = this.mTrackList.getSelectedItemPosition();
        if (this.mPlaylist != null && !this.mPlaylist.equals("recentlyadded") && selectedItemPosition >= 0 && keyEvent.getMetaState() != 0 && keyEvent.getAction() == 0) {
            int keyCode = keyEvent.getKeyCode();
            if (keyCode != 67) {
                switch (keyCode) {
                    case 19:
                        moveItem(true);
                        break;
                    case 20:
                        moveItem(false);
                        break;
                }
                return true;
            }
            removeItem();
            return true;
        }
        if (this.mSearchItem != null && keyEvent.getKeyCode() == 82 && this.mSearchItem.isActionViewExpanded()) {
            return true;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    private void removeItem() {
        int count = this.mTrackCursor.getCount();
        int selectedItemPosition = this.mTrackList.getSelectedItemPosition();
        if (count == 0 || selectedItemPosition < 0) {
            return;
        }
        if ("nowplaying".equals(this.mPlaylist)) {
            try {
                if (selectedItemPosition != MusicUtils.sService.getQueuePosition()) {
                    this.mDeletedOneRow = true;
                }
            } catch (RemoteException e) {
            }
            View selectedView = this.mTrackList.getSelectedView();
            selectedView.setVisibility(8);
            this.mTrackList.invalidateViews();
            ((NowPlayingCursor) this.mTrackCursor).removeItem(selectedItemPosition);
            ((TrackListAdapter) getListAdapter()).notifyDataSetChanged();
            selectedView.setVisibility(0);
            this.mTrackList.invalidateViews();
            return;
        }
        int columnIndexOrThrow = this.mTrackCursor.getColumnIndexOrThrow("_id");
        this.mTrackCursor.moveToPosition(selectedItemPosition);
        getContentResolver().delete(ContentUris.withAppendedId(MediaStore.Audio.Playlists.Members.getContentUri("external", Long.valueOf(this.mPlaylist).longValue()), this.mTrackCursor.getLong(columnIndexOrThrow)), null, null);
        int i = count - 1;
        if (i == 0) {
            finish();
            return;
        }
        ListView listView = this.mTrackList;
        if (selectedItemPosition < i) {
            i = selectedItemPosition;
        }
        listView.setSelection(i);
    }

    private void moveItem(boolean z) {
        int count = this.mTrackCursor.getCount();
        int selectedItemPosition = this.mTrackList.getSelectedItemPosition();
        if (!z || selectedItemPosition >= 1) {
            if (!z && selectedItemPosition >= count - 1) {
                return;
            }
            if (this.mTrackCursor instanceof NowPlayingCursor) {
                ((NowPlayingCursor) this.mTrackCursor).moveItem(selectedItemPosition, z ? selectedItemPosition - 1 : selectedItemPosition + 1);
                ((TrackListAdapter) getListAdapter()).notifyDataSetChanged();
                getListView().invalidateViews();
                this.mDeletedOneRow = true;
                if (z) {
                    this.mTrackList.setSelection(selectedItemPosition - 1);
                    return;
                } else {
                    this.mTrackList.setSelection(selectedItemPosition + 1);
                    return;
                }
            }
            int columnIndexOrThrow = this.mTrackCursor.getColumnIndexOrThrow("play_order");
            this.mTrackCursor.moveToPosition(selectedItemPosition);
            int i = this.mTrackCursor.getInt(columnIndexOrThrow);
            Uri contentUri = MediaStore.Audio.Playlists.Members.getContentUri("external", Long.valueOf(this.mPlaylist).longValue());
            ContentValues contentValues = new ContentValues();
            String[] strArr = new String[1];
            ContentResolver contentResolver = getContentResolver();
            if (z) {
                contentValues.put("play_order", Integer.valueOf(i - 1));
                strArr[0] = this.mTrackCursor.getString(0);
                contentResolver.update(contentUri, contentValues, "_id=?", strArr);
                this.mTrackCursor.moveToPrevious();
            } else {
                contentValues.put("play_order", Integer.valueOf(i + 1));
                strArr[0] = this.mTrackCursor.getString(0);
                contentResolver.update(contentUri, contentValues, "_id=?", strArr);
                this.mTrackCursor.moveToNext();
            }
            contentValues.put("play_order", Integer.valueOf(i));
            strArr[0] = this.mTrackCursor.getString(0);
            contentResolver.update(contentUri, contentValues, "_id=?", strArr);
        }
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        MusicLogUtils.v("MusicPerformanceTest", "[Performance test][Music] play song start [" + System.currentTimeMillis() + "]");
        MusicLogUtils.v("MusicPerformanceTest", "[CMCC Performance test][Music] play song start [" + System.currentTimeMillis() + "]");
        if (this.mTrackCursor.getCount() == 0) {
            MusicLogUtils.v("TrackBrowser", "return count 0");
            return;
        }
        if ((this.mTrackCursor instanceof NowPlayingCursor) && MusicUtils.sService != null) {
            try {
                if (checkDrmRightsForPlay(this.mTrackCursor, i, true, true)) {
                    MusicUtils.sService.setQueuePosition(i);
                    return;
                }
                return;
            } catch (RemoteException e) {
            }
        }
        if (checkDrmRightsForPlay(this.mTrackCursor, i, false, true)) {
            MusicUtils.playAll(this, this.mTrackCursor, i);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean zOnCreateOptionsMenu = super.onCreateOptionsMenu(menu);
        if (this.mWithtabs) {
            return zOnCreateOptionsMenu;
        }
        if (this.mPlaylist == null) {
            menu.add(0, 19, 0, R.string.play_all).setIcon(R.drawable.ic_menu_play_clip);
        }
        menu.add(0, 8, 0, R.string.party_shuffle);
        menu.add(0, 9, 0, R.string.shuffle_all).setIcon(R.drawable.ic_menu_shuffle);
        if (this.mPlaylist != null) {
            menu.add(0, 18, 0, R.string.save_as_playlist).setIcon(R.drawable.ic_menu_save);
            if (this.mPlaylist.equals("nowplaying")) {
                menu.add(0, 20, 0, R.string.clear_playlist).setIcon(R.drawable.ic_menu_clear_playlist);
            }
        }
        menu.add(0, 13, 0, R.string.effects_list_title).setIcon(R.drawable.ic_menu_eq);
        this.mSearchItem = MusicUtils.addSearchView(this, menu, this.mQueryTextListener, null);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean zOnPrepareOptionsMenu = super.onPrepareOptionsMenu(menu);
        if (this.mWithtabs) {
            return zOnPrepareOptionsMenu;
        }
        MusicUtils.setPartyShuffleMenuIcon(menu);
        if (this.mPlaylist != null && menu.findItem(20) != null && this.mTrackList != null && this.mTrackList.getCount() <= 0) {
            menu.findItem(20).setVisible(false);
        }
        Bundle bundle = new Bundle();
        bundle.putString("playlistname", this.mPlaylist);
        bundle.putInt("playlistlen", this.mTrackList == null ? 0 : this.mTrackList.getCount());
        if (this.mPlayListName != null && !this.mPlayListName.equals("") && (this.mPlayListName.equals(getText(R.string.soundrecord_playlist_name)) || this.mPlayListName.equals(getText(R.string.fmrecord_playlist_name)) || this.mPlayListName.equals(getText(R.string.phonerecord_playlist_name)))) {
            menu.findItem(8).setVisible(false);
            menu.findItem(9).setVisible(false);
            menu.findItem(18).setVisible(false);
        }
        menu.findItem(13).setVisible(getPackageManager().resolveActivity(new Intent("android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL"), 0) != null);
        this.mOptionMenu = menu;
        if (this.mQueryText != null && !this.mQueryText.toString().equals("")) {
            MusicLogUtils.v("TrackBrowser", "setQueryText:" + ((Object) this.mQueryText));
            ((SearchView) this.mSearchItem.getActionView()).setQuery(this.mQueryText, false);
            this.mQueryText = null;
        }
        if (findViewById(R.id.sd_icon).getVisibility() != 0) {
            return true;
        }
        MusicLogUtils.v("TrackBrowser", "SDcard not ready, disable option menu!");
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int count;
        boolean zOnOptionsItemSelected = super.onOptionsItemSelected(menuItem);
        if (this.mWithtabs) {
            return zOnOptionsItemSelected;
        }
        switch (menuItem.getItemId()) {
            case 8:
                MusicUtils.togglePartyShuffle();
                setTitle();
                return zOnOptionsItemSelected;
            case 9:
                Cursor cursorQuery = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "is_music=1", null, null);
                if (cursorQuery != null) {
                    MusicUtils.shuffleAll(this, cursorQuery);
                    cursorQuery.close();
                }
                return true;
            case 13:
                return MusicUtils.startEffectPanel(this);
            case 18:
                long[] songListForCursor = MusicUtils.getSongListForCursor(this.mTrackCursor);
                showCreateDialog(null, 2, "save_as_playlist", songListForCursor);
                MusicLogUtils.v("TrackBrowser", "SAVE_AS_PLAYLIST list" + Arrays.toString(songListForCursor));
                return true;
            case 19:
                MusicUtils.playAll(this, this.mTrackCursor);
                return true;
            case 20:
                if (this.mPlaylist.equals("nowplaying")) {
                    MusicUtils.clearQueue();
                }
                return true;
            case android.R.id.home:
                if (!this.mIsInBackgroud) {
                    onBackPressed();
                }
                return true;
            case R.id.search:
                onSearchRequested();
                return true;
            default:
                Bundle bundle = new Bundle();
                bundle.putString("playlistname", this.mPlaylist);
                if (this.mTrackList == null) {
                    count = 0;
                } else {
                    count = this.mTrackList.getCount();
                }
                bundle.putInt("playlistlen", count);
                return false;
        }
    }

    @Override
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i != 4) {
            if (i == 11) {
                if (i2 == 0) {
                    finish();
                    return;
                } else {
                    getTrackCursor(this.mAdapter.getQueryHandler(), null, true);
                    return;
                }
            }
            if (i == 18) {
                if (i2 == -1) {
                    Uri data = intent.getData();
                    String stringExtra = intent.getStringExtra("SAVE_PLAYLIST_FLAG");
                    if (stringExtra == null) {
                        stringExtra = "";
                    }
                    long[] songListForCursor = MusicUtils.getSongListForCursor(this.mTrackCursor);
                    int length = songListForCursor.length;
                    if (data != null) {
                        MusicUtils.addToPlaylist(this, songListForCursor, Integer.parseInt(data.getLastPathSegment()));
                        return;
                    } else {
                        if (stringExtra.equals("save_as_playlist")) {
                            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getQuantityString(R.plurals.NNNtrackstoplaylist, length, Integer.valueOf(length)), 0).show();
                            return;
                        }
                        return;
                    }
                }
                return;
            }
            Bundle bundle = new Bundle();
            bundle.putString("playlistname", this.mPlaylist);
            bundle.putInt("playlistlen", this.mTrackList != null ? this.mTrackList.getCount() : 0);
        }
    }

    private Cursor getTrackCursor(TrackListAdapter.TrackQueryHandler trackQueryHandler, String str, boolean z) {
        if (trackQueryHandler == null) {
            throw new IllegalArgumentException();
        }
        Cursor cursorDoQuery = null;
        this.mSortOrder = null;
        StringBuilder sb = new StringBuilder();
        sb.append("title != ''");
        if (this.mGenre != null) {
            Uri contentUri = MediaStore.Audio.Genres.Members.getContentUri("external", Integer.valueOf(this.mGenre).intValue());
            if (!TextUtils.isEmpty(str)) {
                contentUri = contentUri.buildUpon().appendQueryParameter("filter", Uri.encode(str)).build();
            }
            this.mSortOrder = "title_key";
            cursorDoQuery = trackQueryHandler.doQuery(contentUri, this.mCursorCols, sb.toString(), null, null, z);
        } else if (this.mPlaylist != null) {
            if (this.mPlaylist.equals("nowplaying")) {
                if (MusicUtils.sService != null) {
                    cursorDoQuery = new NowPlayingCursor(MusicUtils.sService, this.mCursorCols);
                    if (cursorDoQuery.getCount() == 0) {
                        finish();
                    }
                }
            } else if (this.mPlaylist.equals("podcasts")) {
                sb.append(" AND is_podcast=1");
                Uri uriBuild = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                if (!TextUtils.isEmpty(str)) {
                    uriBuild = uriBuild.buildUpon().appendQueryParameter("filter", Uri.encode(str)).build();
                }
                cursorDoQuery = trackQueryHandler.doQuery(uriBuild, this.mCursorCols, sb.toString(), null, null, z);
            } else if (this.mPlaylist.equals("recentlyadded")) {
                Uri uriBuild2 = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                if (!TextUtils.isEmpty(str)) {
                    uriBuild2 = uriBuild2.buildUpon().appendQueryParameter("filter", Uri.encode(str)).build();
                }
                int intPref = MusicUtils.getIntPref(this, "numweeks", 2) * 604800;
                sb.append(" AND date_added>");
                sb.append((System.currentTimeMillis() / 1000) - ((long) intPref));
                cursorDoQuery = trackQueryHandler.doQuery(uriBuild2, this.mCursorCols, sb.toString(), null, null, z);
            } else {
                Uri contentUri2 = MediaStore.Audio.Playlists.Members.getContentUri("external", Long.valueOf(this.mPlaylist).longValue());
                if (!TextUtils.isEmpty(str)) {
                    contentUri2 = contentUri2.buildUpon().appendQueryParameter("filter", Uri.encode(str)).build();
                }
                this.mSortOrder = "play_order";
                cursorDoQuery = trackQueryHandler.doQuery(contentUri2, this.mPlaylistMemberCols, sb.toString(), null, this.mSortOrder, z);
            }
        } else {
            if (this.mAlbumId != null) {
                sb.append(" AND album_id=" + this.mAlbumId);
                this.mSortOrder = "track, " + this.mSortOrder;
            }
            if (this.mArtistId != null) {
                sb.append(" AND artist_id=" + this.mArtistId);
            }
            sb.append(" AND is_music=1");
            Uri uriBuild3 = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            if (!TextUtils.isEmpty(str)) {
                uriBuild3 = uriBuild3.buildUpon().appendQueryParameter("filter", Uri.encode(str)).build();
            }
            cursorDoQuery = trackQueryHandler.doQuery(uriBuild3, this.mCursorCols, sb.toString(), null, this.mSortOrder, z);
        }
        if (cursorDoQuery != null && z) {
            init(cursorDoQuery, false);
            setTitle();
        }
        return cursorDoQuery;
    }

    private class NowPlayingCursor extends AbstractCursor {
        private String[] mCols;
        private int mCurPos;
        private Cursor mCurrentPlaylistCursor;
        private long[] mCursorIdxs;
        private long[] mNowPlaying;
        private IMediaPlaybackService mService;
        private int mSize;

        public NowPlayingCursor(IMediaPlaybackService iMediaPlaybackService, String[] strArr) {
            this.mCols = strArr;
            this.mService = iMediaPlaybackService;
            makeNowPlayingCursor();
        }

        private void makeNowPlayingCursor() {
            if (this.mCurrentPlaylistCursor != null) {
                this.mCurrentPlaylistCursor.close();
            }
            this.mCurrentPlaylistCursor = null;
            try {
                this.mNowPlaying = this.mService.getQueue();
            } catch (RemoteException e) {
                this.mNowPlaying = new long[0];
            }
            this.mSize = this.mNowPlaying.length;
            if (this.mSize == 0) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            sb.append("_id IN (");
            for (int i = 0; i < this.mSize; i++) {
                sb.append(this.mNowPlaying[i]);
                if (i < this.mSize - 1) {
                    sb.append(",");
                }
            }
            sb.append(")");
            this.mCurrentPlaylistCursor = MusicUtils.query(TrackBrowserActivity.this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, this.mCols, sb.toString(), null, "_id");
            if (this.mCurrentPlaylistCursor == null) {
                this.mSize = 0;
                return;
            }
            int count = this.mCurrentPlaylistCursor.getCount();
            this.mCursorIdxs = new long[count];
            this.mCurrentPlaylistCursor.moveToFirst();
            int columnIndexOrThrow = this.mCurrentPlaylistCursor.getColumnIndexOrThrow("_id");
            for (int i2 = 0; i2 < count; i2++) {
                this.mCursorIdxs[i2] = this.mCurrentPlaylistCursor.getLong(columnIndexOrThrow);
                this.mCurrentPlaylistCursor.moveToNext();
            }
            this.mCurrentPlaylistCursor.moveToFirst();
            this.mCurPos = -1;
            try {
                int iRemoveTrack = 0;
                for (int length = this.mNowPlaying.length - 1; length >= 0; length--) {
                    long j = this.mNowPlaying[length];
                    if (Arrays.binarySearch(this.mCursorIdxs, j) < 0) {
                        iRemoveTrack += this.mService.removeTrack(j);
                    }
                }
                if (iRemoveTrack > 0) {
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

        @Override
        public int getCount() {
            return this.mSize;
        }

        @Override
        public boolean onMove(int i, int i2) {
            if (i == i2) {
                return true;
            }
            if (this.mNowPlaying == null || this.mCursorIdxs == null || i2 >= this.mNowPlaying.length) {
                return false;
            }
            this.mCurrentPlaylistCursor.moveToPosition(Arrays.binarySearch(this.mCursorIdxs, this.mNowPlaying[i2]));
            this.mCurPos = i2;
            return true;
        }

        public boolean removeItem(int i) {
            try {
                if (this.mService.removeTracks(i, i) != 0) {
                    this.mSize--;
                    while (i < this.mSize) {
                        int i2 = i + 1;
                        this.mNowPlaying[i] = this.mNowPlaying[i2];
                        i = i2;
                    }
                    onMove(-1, this.mCurPos);
                } else {
                    return false;
                }
            } catch (RemoteException e) {
            }
            return true;
        }

        public void moveItem(int i, int i2) {
            try {
                if (this.mService.getQueue().length == this.mNowPlaying.length) {
                    this.mService.moveQueueItem(i, i2);
                    this.mNowPlaying = this.mService.getQueue();
                    onMove(-1, this.mCurPos);
                    return;
                }
                if (i >= 0 && i2 >= 0 && i < this.mNowPlaying.length && i2 < this.mNowPlaying.length && i != i2) {
                    long j = this.mNowPlaying[i];
                    if (i < i2) {
                        while (i < i2) {
                            int i3 = i + 1;
                            this.mNowPlaying[i] = this.mNowPlaying[i3];
                            i = i3;
                        }
                    } else if (i2 < i) {
                        while (i > i2) {
                            this.mNowPlaying[i] = this.mNowPlaying[i - 1];
                            i--;
                        }
                    }
                    this.mNowPlaying[i2] = j;
                    onMove(-1, this.mCurPos);
                }
            } catch (RemoteException e) {
            }
        }

        @Override
        public String getString(int i) {
            try {
                return this.mCurrentPlaylistCursor.getString(i);
            } catch (Exception e) {
                onChange(true);
                return "";
            }
        }

        @Override
        public short getShort(int i) {
            return this.mCurrentPlaylistCursor.getShort(i);
        }

        @Override
        public int getInt(int i) {
            try {
                return this.mCurrentPlaylistCursor.getInt(i);
            } catch (Exception e) {
                onChange(true);
                return 0;
            }
        }

        @Override
        public long getLong(int i) {
            try {
                return this.mCurrentPlaylistCursor.getLong(i);
            } catch (Exception e) {
                onChange(true);
                return 0L;
            }
        }

        @Override
        public float getFloat(int i) {
            return this.mCurrentPlaylistCursor.getFloat(i);
        }

        @Override
        public double getDouble(int i) {
            return this.mCurrentPlaylistCursor.getDouble(i);
        }

        @Override
        public int getType(int i) {
            return this.mCurrentPlaylistCursor.getType(i);
        }

        @Override
        public boolean isNull(int i) {
            return this.mCurrentPlaylistCursor.isNull(i);
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

        @Override
        public void close() {
            super.close();
            if (this.mCurrentPlaylistCursor != null) {
                this.mCurrentPlaylistCursor.close();
                this.mCurrentPlaylistCursor = null;
            }
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
        int mDrmMethodIdx;
        int mDurationIdx;
        private AlphabetIndexer mIndexer;
        int mIsDrmIdx;
        boolean mIsNowPlaying;
        private TrackQueryHandler mQueryHandler;
        int mTitleIdx;
        private String mUnknownAlbum;
        private String mUnknownArtist;

        static class ViewHolder {
            CharArrayBuffer buffer1;
            char[] buffer2;
            ImageView drmLock;
            TextView duration;
            ImageView editIcon;
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

            TrackQueryHandler(ContentResolver contentResolver) {
                super(contentResolver);
            }

            public Cursor doQuery(Uri uri, String[] strArr, String str, String[] strArr2, String str2, boolean z) {
                if (!z) {
                    return MusicUtils.query(TrackListAdapter.this.mActivity, uri, strArr, str, strArr2, str2);
                }
                Uri uriBuild = uri.buildUpon().appendQueryParameter("limit", "100").build();
                QueryArgs queryArgs = new QueryArgs();
                queryArgs.uri = uri;
                queryArgs.projection = strArr;
                queryArgs.selection = str;
                queryArgs.selectionArgs = strArr2;
                queryArgs.orderBy = str2;
                startQuery(0, queryArgs, uriBuild, strArr, str, strArr2, str2);
                return null;
            }

            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                StringBuilder sb = new StringBuilder();
                sb.append("query complete: count is ");
                sb.append(cursor == null ? null : Integer.valueOf(cursor.getCount()));
                sb.append("   ");
                sb.append(TrackListAdapter.this.mActivity);
                MusicLogUtils.v("TrackBrowser", sb.toString());
                TrackListAdapter.this.mActivity.init(cursor, obj != null);
                if (i == 0 && obj != null && cursor != null && !cursor.isClosed() && cursor.getCount() >= 100) {
                    QueryArgs queryArgs = (QueryArgs) obj;
                    startQuery(1, null, queryArgs.uri, queryArgs.projection, queryArgs.selection, queryArgs.selectionArgs, queryArgs.orderBy);
                }
            }
        }

        TrackListAdapter(Context context, TrackBrowserActivity trackBrowserActivity, int i, Cursor cursor, String[] strArr, int[] iArr, boolean z, boolean z2) {
            super(context, i, cursor, strArr, iArr);
            this.mBuilder = new StringBuilder();
            this.mActivity = null;
            this.mConstraint = null;
            this.mConstraintIsValid = false;
            this.mIsDrmIdx = -1;
            this.mDrmMethodIdx = -1;
            this.mActivity = trackBrowserActivity;
            getColumnIndices(cursor);
            this.mIsNowPlaying = z;
            this.mDisableNowPlayingIndicator = z2;
            this.mUnknownArtist = context.getString(R.string.unknown_artist_name);
            this.mUnknownAlbum = context.getString(R.string.unknown_album_name);
            this.mQueryHandler = new TrackQueryHandler(context.getContentResolver());
        }

        public void setActivity(TrackBrowserActivity trackBrowserActivity) {
            this.mActivity = trackBrowserActivity;
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
                if (OmaDrmUtils.isOmaDrmEnabled()) {
                    this.mIsDrmIdx = cursor.getColumnIndexOrThrow("is_drm");
                }
                if (this.mIndexer == null) {
                    if (this.mActivity.mEditMode || this.mActivity.mAlbumId != null) {
                        this.mActivity.mTrackList.setFastScrollEnabled(false);
                        return;
                    } else {
                        this.mIndexer = new MusicAlphabetIndexer(cursor, this.mTitleIdx, this.mActivity.getString(R.string.fast_scroll_alphabet));
                        return;
                    }
                }
                this.mIndexer.setCursor(cursor);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View viewNewView = super.newView(context, cursor, viewGroup);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.line1 = (TextView) viewNewView.findViewById(R.id.line1);
            viewHolder.line2 = (TextView) viewNewView.findViewById(R.id.line2);
            viewHolder.duration = (TextView) viewNewView.findViewById(R.id.duration);
            viewHolder.play_indicator = (ImageView) viewNewView.findViewById(R.id.play_indicator);
            viewHolder.buffer1 = new CharArrayBuffer(100);
            viewHolder.buffer2 = new char[200];
            viewHolder.drmLock = (ImageView) viewNewView.findViewById(R.id.drm_lock);
            if (this.mActivity.mEditMode) {
                viewHolder.editIcon = (ImageView) viewNewView.findViewById(R.id.icon);
            }
            viewNewView.setTag(viewHolder);
            return viewNewView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) throws RemoteException {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            cursor.copyStringToBuffer(this.mTitleIdx, viewHolder.buffer1);
            viewHolder.line1.setText(viewHolder.buffer1.data, 0, viewHolder.buffer1.sizeCopied);
            viewHolder.duration.setText(MusicUtils.makeTimeString(context, cursor.getInt(this.mDurationIdx) / 1000));
            StringBuilder sb = this.mBuilder;
            sb.delete(0, sb.length());
            String string = cursor.getString(this.mArtistIdx);
            if (string == null || string.equals("<unknown>")) {
                sb.append(this.mUnknownArtist);
            } else {
                sb.append(string);
            }
            int length = sb.length();
            if (viewHolder.buffer2.length < length) {
                viewHolder.buffer2 = new char[length];
            }
            sb.getChars(0, length, viewHolder.buffer2, 0);
            viewHolder.line2.setText(viewHolder.buffer2, 0, length);
            if (this.mActivity.mEditMode) {
                ImageView imageView = viewHolder.editIcon;
                imageView.setImageResource(R.drawable.ic_playlist_move);
                imageView.setVisibility(0);
            }
            ImageView imageView2 = viewHolder.play_indicator;
            long audioId = -1;
            if (MusicUtils.sService != null) {
                try {
                    if (this.mIsNowPlaying) {
                        audioId = MusicUtils.sService.getQueuePosition();
                    } else {
                        audioId = MusicUtils.sService.getAudioId();
                    }
                } catch (RemoteException e) {
                }
            }
            if ((this.mIsNowPlaying && cursor.getPosition() == audioId) || (!this.mIsNowPlaying && !this.mDisableNowPlayingIndicator && cursor.getLong(this.mAudioIdIdx) == audioId)) {
                imageView2.setVisibility(0);
            } else {
                imageView2.setVisibility(8);
            }
            updateDrmLockIcon(viewHolder.drmLock, cursor);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if (this.mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != this.mActivity.mTrackCursor) {
                this.mActivity.mTrackCursor = cursor;
                super.changeCursor(cursor);
                getColumnIndices(cursor);
            }
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence charSequence) {
            String string = charSequence.toString();
            if (!this.mConstraintIsValid || ((string != null || this.mConstraint != null) && (string == null || !string.equals(this.mConstraint)))) {
                Cursor trackCursor = this.mActivity.getTrackCursor(this.mQueryHandler, string, false);
                this.mConstraint = string;
                this.mConstraintIsValid = true;
                return trackCursor;
            }
            return getCursor();
        }

        @Override
        public Object[] getSections() {
            if (this.mIndexer != null) {
                return this.mIndexer.getSections();
            }
            return new String[]{" "};
        }

        @Override
        public int getPositionForSection(int i) {
            if (this.mIndexer != null) {
                return this.mIndexer.getPositionForSection(i);
            }
            return 0;
        }

        @Override
        public int getSectionForPosition(int i) {
            if (this.mIndexer != null) {
                return this.mIndexer.getSectionForPosition(i);
            }
            return 0;
        }

        private void updateDrmLockIcon(ImageView imageView, Cursor cursor) {
            int drmRightsStatus;
            int i = 0;
            if (OmaDrmUtils.isOmaDrmEnabled() && cursor.getInt(this.mIsDrmIdx) == 1 && (drmRightsStatus = this.mActivity.getDrmRightsStatus(cursor)) >= 0) {
                if (drmRightsStatus == 0) {
                    imageView.setImageResource(134348871);
                } else {
                    imageView.setImageResource(134348872);
                }
            } else {
                i = 8;
            }
            imageView.setVisibility(i);
        }

        public void reloadStringOnLocaleChanges() {
            String string = this.mActivity.getString(R.string.unknown_artist_name);
            String string2 = this.mActivity.getString(R.string.unknown_album_name);
            if (this.mUnknownArtist != null && !this.mUnknownArtist.equals(string)) {
                this.mUnknownArtist = string;
            }
            if (this.mUnknownAlbum != null && !this.mUnknownAlbum.equals(string2)) {
                this.mUnknownAlbum = string2;
            }
        }
    }

    private void initAdapter() {
        this.mAdapter = (TrackListAdapter) getLastNonConfigurationInstance();
        if (this.mAdapter == null) {
            MusicLogUtils.v("TrackBrowser", "starting query");
            this.mAdapter = new TrackListAdapter(getApplication(), this, this.mEditMode ? R.layout.edit_track_list_item : R.layout.track_list_item, null, new String[0], new int[0], "nowplaying".equals(this.mPlaylist), (this.mPlaylist == null || this.mPlaylist.equals("podcasts") || this.mPlaylist.equals("recentlyadded")) ? false : true);
            setListAdapter(this.mAdapter);
            if (!this.mWithtabs) {
                setTitle(R.string.working_songs);
            }
            getTrackCursor(this.mAdapter.getQueryHandler(), null, true);
            return;
        }
        this.mAdapter.setActivity(this);
        this.mAdapter.reloadStringOnLocaleChanges();
        setListAdapter(this.mAdapter);
        this.mTrackCursor = this.mAdapter.getCursor();
        if (this.mTrackCursor != null) {
            init(this.mTrackCursor, false);
            return;
        }
        if (!this.mWithtabs) {
            setTitle(R.string.working_songs);
        }
        getTrackCursor(this.mAdapter.getQueryHandler(), null, true);
    }

    @Override
    public boolean onSearchRequested() {
        if (this.mSearchItem != null) {
            this.mSearchItem.expandActionView();
            return true;
        }
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mOrientation = configuration.orientation;
        if (findViewById(R.id.sd_icon).getVisibility() == 0) {
            MusicLogUtils.v("TrackBrowser", "Configuration Changed at database error, return!");
            return;
        }
        if (this.mOrientation == 1 && this.mIsInBackgroud) {
            MusicUtils.setBackground((View) this.mTrackList.getParent().getParent(), null);
            MusicLogUtils.v("TrackBrowser", "onConfigurationChanged clear background album art when in background.");
        }
        if (this.mService != null) {
            MusicUtils.updateNowPlaying(this, this.mOrientation);
        }
        if (this.mSearchItem != null) {
            this.mQueryText = ((SearchView) this.mSearchItem.getActionView()).getQuery();
            MusicLogUtils.v("TrackBrowser", "searchText:" + ((Object) this.mQueryText));
        }
        invalidateOptionsMenu();
    }

    private class AlbumArtFetcher extends AsyncTask<Long, Void, Bitmap> {
        private AlbumArtFetcher() {
        }

        @Override
        protected Bitmap doInBackground(Long... lArr) {
            if (TrackBrowserActivity.this.mAlbumArtBitmap == null) {
                TrackBrowserActivity.this.mAlbumArtBitmap = MusicUtils.getArtwork(TrackBrowserActivity.this, -1L, lArr[0].longValue(), false);
                MusicLogUtils.v("TrackBrowser", "AlbumArtFetcher: getArtwork returns " + TrackBrowserActivity.this.mAlbumArtBitmap);
            }
            ((TouchInterceptor) TrackBrowserActivity.this.mTrackList).waitMeasureFinished(TrackBrowserActivity.this.getResources().getConfiguration().orientation == 2);
            return TrackBrowserActivity.this.mAlbumArtBitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (bitmap != null) {
                MusicUtils.setBackground((View) TrackBrowserActivity.this.mTrackList.getParent().getParent(), bitmap);
                TrackBrowserActivity.this.mTrackList.setCacheColorHint(0);
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int i) {
        if (i == -1) {
            if (this.mTrackCursor instanceof NowPlayingCursor) {
                if (MusicUtils.sService != null) {
                    try {
                        MusicUtils.sService.setQueuePosition(this.mCurTrackPos);
                        return;
                    } catch (RemoteException e) {
                        MusicLogUtils.v("TrackBrowser", "RemoteException when setQueuePosition: ", e);
                        return;
                    }
                }
                return;
            }
            MusicUtils.playAll(this, this.mTrackCursor, this.mCurTrackPos);
        }
    }

    private Uri getUri(Cursor cursor) {
        int columnIndexOrThrow;
        try {
            columnIndexOrThrow = cursor.getColumnIndexOrThrow("audio_id");
        } catch (IllegalArgumentException e) {
            columnIndexOrThrow = cursor.getColumnIndexOrThrow("_id");
        }
        return ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(columnIndexOrThrow));
    }

    private int getDrmRightsStatus(Cursor cursor) {
        int iCheckRightsStatus;
        try {
            iCheckRightsStatus = this.mDrmClient.checkRightsStatus(getUri(cursor), 1);
        } catch (IllegalArgumentException e) {
            MusicLogUtils.v("TrackBrowser", "getDrmRightsStatus throw IllegalArgumentException " + e);
            iCheckRightsStatus = -1;
        }
        MusicLogUtils.v("TrackBrowser", "getDrmRightsStatus: rightsStatus=" + iCheckRightsStatus);
        return iCheckRightsStatus;
    }

    private boolean checkDrmRightsForRingtone(Cursor cursor, int i) {
        if (!OmaDrmUtils.isOmaDrmEnabled()) {
            return true;
        }
        int position = cursor.getPosition();
        cursor.moveToPosition(i);
        int i2 = cursor.getInt(cursor.getColumnIndexOrThrow("is_drm"));
        MusicLogUtils.d("TrackBrowser", "checkDrmRightsForRingtone: isDrm=" + i2);
        if (i2 == 0) {
            return true;
        }
        try {
            try {
                int iCheckRightsStatus = this.mDrmClient.checkRightsStatus(getUri(cursor), 2);
                boolean z = iCheckRightsStatus == 0;
                MusicLogUtils.v("TrackBrowser", "checkDrmRightsForRingtone: rightsStatus=" + iCheckRightsStatus);
                cursor.moveToPosition(position);
                return z;
            } catch (IllegalArgumentException e) {
                MusicLogUtils.v("TrackBrowser", "checkDrmRightsForRingtone throw IllegalArgumentException " + e);
                MusicLogUtils.v("TrackBrowser", "checkDrmRightsForRingtone: rightsStatus=-1");
                cursor.moveToPosition(position);
                return false;
            }
        } catch (Throwable th) {
            MusicLogUtils.v("TrackBrowser", "checkDrmRightsForRingtone: rightsStatus=-1");
            cursor.moveToPosition(position);
            throw th;
        }
    }

    private boolean checkDrmRightsForPlay(Cursor cursor, int i, boolean z, boolean z2) {
        if (!OmaDrmUtils.isOmaDrmEnabled()) {
            return true;
        }
        this.mCurTrackPos = i;
        cursor.moveToPosition(i);
        int i2 = cursor.getInt(cursor.getColumnIndexOrThrow("is_drm"));
        MusicLogUtils.d("TrackBrowser", "checkDrmRightsForPlay: isDrm=" + i2);
        if (i2 == 0) {
            return true;
        }
        OmaDrmUtils.showConsumerDialog(this, this.mDrmClient, getUri(cursor), this);
        return false;
    }

    private boolean canDispalyRingtone(int i, int i2) {
        if (i != 1) {
            return true;
        }
        return checkDrmRightsForRingtone(this.mTrackCursor, this.mSelectedPosition);
    }
}
