package com.android.music;

import android.app.ActionBar;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
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
import com.android.music.IMediaPlaybackService;
import com.android.music.MusicUtils;

public class AlbumBrowserActivity extends ListActivity implements ServiceConnection, View.OnCreateContextMenuListener {
    private static int sLastListPosCourse = -1;
    private static int sLastListPosFine = -1;
    private AlbumListAdapter mAdapter;
    private boolean mAdapterSent;
    private Cursor mAlbumCursor;
    private String mArtistId;
    private Context mContext;
    private String mCurrentAlbumId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    boolean mIsUnknownAlbum;
    boolean mIsUnknownArtist;
    private ListView mListView;
    private int mOrientation;
    private CharSequence mQueryText;
    MenuItem mSearchItem;
    private MusicUtils.ServiceToken mToken;
    private boolean mIsMounted = true;
    private IMediaPlaybackService mService = null;
    private boolean mWithtabs = false;
    private boolean mIsInBackgroud = false;
    private SubMenu mSubMenu = null;
    private int mVisibleItemsCount = 20;
    private final String[] mCursorCols = {"_id", "artist", "album", "album_art"};
    private boolean mActvityResumAfterCreate = true;
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AlbumBrowserActivity.this.getListView().invalidateViews();
            if (AlbumBrowserActivity.this.mService != null) {
                MusicUtils.updateNowPlaying(AlbumBrowserActivity.this, AlbumBrowserActivity.this.mOrientation);
            }
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MusicLogUtils.d("Album", "mScanListener.onReceive:" + action + ", status = " + Environment.getExternalStorageState());
            if ("android.intent.action.MEDIA_SCANNER_STARTED".equals(action) || "android.intent.action.MEDIA_SCANNER_FINISHED".equals(action)) {
                MusicUtils.setSpinnerState(AlbumBrowserActivity.this);
                AlbumBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                AlbumBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
                return;
            }
            if ("android.intent.action.MEDIA_UNMOUNTED".equals(action)) {
                AlbumBrowserActivity.this.mIsMounted = false;
                AlbumBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                AlbumBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
                AlbumBrowserActivity.this.closeContextMenu();
                AlbumBrowserActivity.this.closeOptionsMenu();
                if (AlbumBrowserActivity.this.mSubMenu != null) {
                    AlbumBrowserActivity.this.mSubMenu.close();
                }
                AlbumBrowserActivity.this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
                return;
            }
            if ("android.intent.action.MEDIA_MOUNTED".equals(action)) {
                AlbumBrowserActivity.this.mIsMounted = true;
                AlbumBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                AlbumBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
            }
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (AlbumBrowserActivity.this.mAdapter != null) {
                AlbumBrowserActivity.this.getAlbumCursor(AlbumBrowserActivity.this.mAdapter.getQueryHandler(), null);
            }
        }
    };
    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String str) {
            Intent intent = new Intent();
            intent.setClass(AlbumBrowserActivity.this, QueryBrowserActivity.class);
            intent.putExtra("query", str);
            AlbumBrowserActivity.this.startActivity(intent);
            AlbumBrowserActivity.this.mSearchItem.collapseActionView();
            return true;
        }

        @Override
        public boolean onQueryTextChange(String str) {
            return false;
        }
    };
    private final Handler mRefreshAlbumArtHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            View childAt;
            int i = message.arg1;
            int i2 = message.arg2;
            int firstVisiblePosition = AlbumBrowserActivity.this.mListView.getFirstVisiblePosition();
            int i3 = i2 - firstVisiblePosition;
            int lastVisiblePosition = (AlbumBrowserActivity.this.mListView.getLastVisiblePosition() - firstVisiblePosition) + 1;
            AlbumBrowserActivity albumBrowserActivity = AlbumBrowserActivity.this;
            if (AlbumBrowserActivity.this.mVisibleItemsCount > lastVisiblePosition) {
                lastVisiblePosition = AlbumBrowserActivity.this.mVisibleItemsCount;
            }
            albumBrowserActivity.mVisibleItemsCount = lastVisiblePosition;
            if (i3 >= 0 && i3 <= AlbumBrowserActivity.this.mVisibleItemsCount && (childAt = AlbumBrowserActivity.this.mListView.getChildAt(i3)) != null) {
                AlbumListAdapter.ViewHolder viewHolder = (AlbumListAdapter.ViewHolder) childAt.getTag();
                if (i < 0) {
                    viewHolder.mIcon.setImageDrawable(AlbumBrowserActivity.this.mAdapter.mDefaultAlbumIcon);
                } else {
                    viewHolder.mIcon.setImageDrawable(MusicUtils.getCachedArtwork(AlbumBrowserActivity.this.mContext, i, AlbumBrowserActivity.this.mAdapter.mDefaultAlbumIcon));
                }
            }
        }
    };
    private final BroadcastReceiver mHdmiChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.clearAlbumArtCache();
            AlbumBrowserActivity.this.getListView().invalidateViews();
        }
    };
    public Handler mListHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            AlbumBrowserActivity.this.refreshEmptyView();
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        MusicLogUtils.d("Album", "onCreate");
        Intent intent = getIntent();
        this.mContext = getApplicationContext();
        if (bundle != null) {
            this.mCurrentAlbumId = bundle.getString("selectedalbum");
            this.mArtistId = bundle.getString("artist");
        } else {
            this.mCurrentAlbumId = intent.getStringExtra("selectedalbum");
            this.mArtistId = intent.getStringExtra("artist");
        }
        this.mWithtabs = intent.getBooleanExtra("withtabs", false);
        requestWindowFeature(5);
        if (this.mWithtabs) {
            requestWindowFeature(1);
        } else {
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        setVolumeControlStream(3);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addDataScheme("file");
        registerReceiver(this.mScanListener, intentFilter);
        if (this.mWithtabs) {
            setContentView(R.layout.media_picker_activity);
        } else {
            setContentView(R.layout.media_picker_activity_nowplaying);
            this.mToken = MusicUtils.bindToService(this, this);
            this.mOrientation = getResources().getConfiguration().orientation;
        }
        this.mListView = getListView();
        this.mListView.setOnCreateContextMenuListener(this);
        this.mListView.setTextFilterEnabled(true);
        this.mAdapter = (AlbumListAdapter) getLastNonConfigurationInstance();
        if (this.mAdapter == null) {
            MusicLogUtils.d("Album", "starting query");
            this.mAdapter = new AlbumListAdapter(getApplication(), this, R.layout.track_list_item, this.mAlbumCursor, new String[0], new int[0]);
            setListAdapter(this.mAdapter);
            setTitle(R.string.working_albums);
            getAlbumCursor(this.mAdapter.getQueryHandler(), null);
            return;
        }
        this.mAdapter.setActivity(this);
        this.mAdapter.reloadStringOnLocaleChanges();
        setListAdapter(this.mAdapter);
        this.mAlbumCursor = this.mAdapter.getCursor();
        if (this.mAlbumCursor != null) {
            init(this.mAlbumCursor);
        } else {
            getAlbumCursor(this.mAdapter.getQueryHandler(), null);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        this.mAdapterSent = true;
        return this.mAdapter;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString("selectedalbum", this.mCurrentAlbumId);
        bundle.putString("artist", this.mArtistId);
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onDestroy() {
        MusicLogUtils.d("Album", "onDestroy");
        ListView listView = getListView();
        if (listView != null) {
            sLastListPosCourse = listView.getFirstVisiblePosition();
            View childAt = listView.getChildAt(0);
            if (childAt != null) {
                sLastListPosFine = childAt.getTop();
            }
        }
        if (this.mToken != null) {
            MusicUtils.unbindFromService(this.mToken);
            this.mService = null;
        }
        if (!this.mAdapterSent && this.mAdapter != null) {
            this.mAdapter.quitLazyLoadingThread();
            this.mAdapter.changeCursor(null);
        }
        setListAdapter(null);
        this.mAdapter = null;
        unregisterReceiver(this.mScanListener);
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        MusicLogUtils.d("Album", "onResume:");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.music.metachanged");
        intentFilter.addAction("com.android.music.queuechanged");
        registerReceiver(this.mTrackListListener, intentFilter);
        this.mTrackListListener.onReceive(null, null);
        MusicUtils.setSpinnerState(this);
        this.mIsInBackgroud = false;
        if (this.mService != null) {
            MusicUtils.updateNowPlaying(this, this.mOrientation);
        }
        if (!this.mActvityResumAfterCreate) {
            refreshEmptyView();
        }
        MusicLogUtils.d("Album", "onResume<<<");
    }

    @Override
    public void onPause() {
        MusicLogUtils.d("Album", "onPause");
        this.mActvityResumAfterCreate = false;
        if (this.mSubMenu != null) {
            this.mSubMenu.close();
        }
        unregisterReceiver(this.mTrackListListener);
        this.mReScanHandler.removeCallbacksAndMessages(null);
        this.mIsInBackgroud = true;
        super.onPause();
    }

    public void init(Cursor cursor) {
        if (this.mAdapter == null) {
            return;
        }
        this.mAdapter.changeCursor(cursor);
        if (this.mAlbumCursor == null) {
            MusicLogUtils.d("Album", "mAlbumCursor is null");
            MusicUtils.displayDatabaseError(this, this.mIsMounted);
            closeContextMenu();
            this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
            return;
        }
        MusicUtils.emptyShow(getListView(), this);
        if (sLastListPosCourse >= 0) {
            getListView().setSelectionFromTop(sLastListPosCourse, sLastListPosFine);
            sLastListPosCourse = -1;
        }
        MusicUtils.hideDatabaseError(this);
        setTitle();
    }

    private void setTitle() {
        CharSequence string = "";
        if (this.mAlbumCursor != null && this.mAlbumCursor.getCount() > 0) {
            this.mAlbumCursor.moveToFirst();
            string = this.mAlbumCursor.getString(this.mAlbumCursor.getColumnIndex("artist"));
            if (string == null || string.equals("<unknown>")) {
                string = getText(R.string.unknown_artist_name);
            }
        }
        if (this.mArtistId != null && string != null) {
            setTitle(string);
        } else {
            setTitle(R.string.albums_title);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        boolean z;
        contextMenu.add(0, 5, 0, R.string.play_selection);
        boolean z2 = true;
        this.mSubMenu = contextMenu.addSubMenu(0, 1, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, this.mSubMenu);
        contextMenu.add(0, 10, 0, R.string.delete_item);
        this.mAlbumCursor.moveToPosition(((AdapterView.AdapterContextMenuInfo) contextMenuInfo).position);
        this.mCurrentAlbumId = this.mAlbumCursor.getString(this.mAlbumCursor.getColumnIndexOrThrow("_id"));
        this.mCurrentAlbumName = this.mAlbumCursor.getString(this.mAlbumCursor.getColumnIndexOrThrow("album"));
        this.mCurrentArtistNameForAlbum = this.mAlbumCursor.getString(this.mAlbumCursor.getColumnIndexOrThrow("artist"));
        if (this.mCurrentArtistNameForAlbum != null && !this.mCurrentArtistNameForAlbum.equals("<unknown>")) {
            z = false;
        } else {
            z = true;
        }
        this.mIsUnknownArtist = z;
        if (this.mCurrentAlbumName != null && !this.mCurrentAlbumName.equals("<unknown>")) {
            z2 = false;
        }
        this.mIsUnknownAlbum = z2;
        if (this.mIsUnknownAlbum) {
            contextMenu.setHeaderTitle(getString(R.string.unknown_album_name));
        } else {
            contextMenu.setHeaderTitle(this.mCurrentAlbumName);
        }
        if (!this.mIsUnknownAlbum || !this.mIsUnknownArtist) {
            contextMenu.add(0, 16, 0, R.string.search_title);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId == 10) {
            long[] songListForAlbum = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
            Bundle bundle = new Bundle();
            bundle.putInt("delete_desc_string_id", R.string.delete_album_desc);
            bundle.putString("delete_desc_track_info", this.mCurrentAlbumName);
            bundle.putLongArray("items", songListForAlbum);
            Intent intent = new Intent();
            intent.setClass(this, DeleteItems.class);
            intent.putExtras(bundle);
            startActivityForResult(intent, -1);
            return true;
        }
        if (itemId == 12) {
            MusicUtils.addToCurrentPlaylist(this, MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId)));
            return true;
        }
        if (itemId != 16) {
            switch (itemId) {
                case 3:
                    MusicUtils.addToPlaylist(this, MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId)), menuItem.getIntent().getLongExtra("playlist", 0L));
                    return true;
                case 4:
                    getParent();
                    showCreateDialog(String.valueOf(this.mCurrentAlbumId), 1, "new_playlist");
                    return true;
                case 5:
                    MusicUtils.playAll(this, MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId)), 0);
                    return true;
                default:
                    return super.onContextItemSelected(menuItem);
            }
        }
        doSearch();
        return true;
    }

    private void showCreateDialog(String str, int i, String str2) {
        MusicLogUtils.d("Album", "showEditDialog>>");
        removeOldFragmentByTag("Create");
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.d("Album", "<showDeleteDialog> fragmentManager = " + fragmentManager);
        CreatePlaylist.newInstance(str, i, str2, null).show(fragmentManager, "Create");
        fragmentManager.executePendingTransactions();
    }

    private void removeOldFragmentByTag(String str) {
        MusicLogUtils.d("Album", "<removeOldFragmentByTag> tag = " + str);
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.d("Album", "<removeOldFragmentByTag> fragmentManager = " + fragmentManager);
        DialogFragment dialogFragment = (DialogFragment) fragmentManager.findFragmentByTag(str);
        MusicLogUtils.d("Album", "<removeOldFragmentByTag> oldFragment = " + dialogFragment);
        if (dialogFragment != null) {
            dialogFragment.dismissAllowingStateLoss();
        }
    }

    void doSearch() {
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        String str = "";
        Intent intent = new Intent();
        intent.setAction("android.intent.action.MEDIA_SEARCH");
        intent.setFlags(268435456);
        String str2 = "";
        if (!this.mIsUnknownAlbum) {
            str = this.mCurrentAlbumName;
            intent.putExtra("android.intent.extra.album", this.mCurrentAlbumName);
            str2 = this.mCurrentAlbumName;
        }
        if (!this.mIsUnknownArtist) {
            str = str + " " + this.mCurrentArtistNameForAlbum;
            intent.putExtra("android.intent.extra.artist", this.mCurrentArtistNameForAlbum);
            str2 = ((Object) str2) + " " + this.mCurrentArtistNameForAlbum;
        }
        intent.putExtra("android.intent.extra.focus", "vnd.android.cursor.item/album");
        String string = getString(R.string.mediasearch, new Object[]{str2});
        intent.putExtra("query", str);
        startActivity(Intent.createChooser(intent, string));
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i != 4) {
            if (i == 11) {
                if (i2 == 0) {
                    finish();
                    return;
                } else {
                    getAlbumCursor(this.mAdapter.getQueryHandler(), null);
                    return;
                }
            }
            return;
        }
        if (i2 == -1) {
            Uri data = intent.getData();
            String stringExtra = intent.getStringExtra("add_to_playlist_item_id");
            MusicLogUtils.d("Album", "onActivityResult: selectAlbumId = " + stringExtra);
            if (data != null && stringExtra != null) {
                MusicUtils.addToPlaylist(this, MusicUtils.getSongListForAlbum(this, Long.parseLong(stringExtra)), Long.parseLong(data.getLastPathSegment()));
            }
        }
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        Intent intent = new Intent("android.intent.action.PICK");
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", Long.valueOf(j).toString());
        intent.putExtra("artist", this.mArtistId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.mWithtabs) {
            return super.onCreateOptionsMenu(menu);
        }
        menu.add(0, 8, 0, R.string.party_shuffle);
        menu.add(0, 9, 0, R.string.shuffle_all).setIcon(R.drawable.ic_menu_shuffle);
        menu.add(0, 13, 0, R.string.effects_list_title).setIcon(R.drawable.ic_menu_eq);
        this.mSearchItem = MusicUtils.addSearchView(this, menu, this.mQueryTextListener, null);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (this.mWithtabs) {
            return super.onPrepareOptionsMenu(menu);
        }
        if (this.mAlbumCursor == null) {
            MusicLogUtils.d("Album", "Album cursor is null, need not show option menu!");
            return false;
        }
        MusicUtils.setPartyShuffleMenuIcon(menu);
        MusicUtils.setEffectPanelMenu(this.mContext, menu);
        if (this.mQueryText != null && !this.mQueryText.toString().equals("")) {
            MusicLogUtils.d("Album", "setQueryText:" + ((Object) this.mQueryText));
            ((SearchView) this.mSearchItem.getActionView()).setQuery(this.mQueryText, false);
            this.mQueryText = null;
            return true;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (this.mWithtabs) {
            return super.onOptionsItemSelected(menuItem);
        }
        switch (menuItem.getItemId()) {
            case 8:
                MusicUtils.togglePartyShuffle();
                break;
            case 9:
                Cursor cursorQuery = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "is_music=1", null, "title_key");
                if (cursorQuery != null) {
                    MusicUtils.shuffleAll(this, cursorQuery);
                    cursorQuery.close();
                }
                return true;
            case 13:
                return MusicUtils.startEffectPanel(this);
            case android.R.id.home:
                if (!this.mIsInBackgroud) {
                    onBackPressed();
                }
                break;
            case R.id.search:
                onSearchRequested();
                return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private Cursor getAlbumCursor(AsyncQueryHandler asyncQueryHandler, String str) {
        Uri uriBuild;
        Uri uriBuild2;
        if (this.mArtistId != null) {
            Uri contentUri = MediaStore.Audio.Artists.Albums.getContentUri("external", Long.valueOf(this.mArtistId).longValue());
            if (!TextUtils.isEmpty(str)) {
                uriBuild2 = contentUri.buildUpon().appendQueryParameter("filter", Uri.encode(str)).build();
            } else {
                uriBuild2 = contentUri;
            }
            if (asyncQueryHandler != null) {
                asyncQueryHandler.startQuery(0, null, uriBuild2, this.mCursorCols, null, null, "album_key");
                return null;
            }
            return MusicUtils.query(this, uriBuild2, this.mCursorCols, null, null, "album_key");
        }
        Uri uri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        if (!TextUtils.isEmpty(str)) {
            uriBuild = uri.buildUpon().appendQueryParameter("filter", Uri.encode(str)).build();
        } else {
            uriBuild = uri;
        }
        if (asyncQueryHandler != null) {
            asyncQueryHandler.startQuery(0, null, uriBuild, this.mCursorCols, null, null, "album_key");
            return null;
        }
        return MusicUtils.query(this, uriBuild, this.mCursorCols, null, null, "album_key");
    }

    static class AlbumListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        private AlbumBrowserActivity mActivity;
        private int mAlbumArtIndex;
        private int mAlbumIdx;
        private final String mAlbumSongSeparator;
        private int mArtistIdx;
        private final BitmapDrawable mBackgroundAlbumIcon;
        private String mConstraint;
        private boolean mConstraintIsValid;
        private final BitmapDrawable mDefaultAlbumIcon;
        private final Object[] mFormatArgs;
        private AlphabetIndexer mIndexer;
        private HandlerThread mLazyLoadingThread;
        private Handler mLazyLoaingHandler;
        private AsyncQueryHandler mQueryHandler;
        private final Resources mResources;
        private final StringBuilder mStringBuilder;
        private String mUnknownAlbum;
        private String mUnknownArtist;
        private int mWhat;

        static class ViewHolder {
            ImageView mIcon;
            TextView mLine1;
            TextView mLine2;
            ImageView mPlayIndicator;

            ViewHolder() {
            }
        }

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver contentResolver) {
                super(contentResolver);
            }

            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                StringBuilder sb = new StringBuilder();
                sb.append("query complete: ");
                sb.append(cursor == null ? null : Integer.valueOf(cursor.getCount()));
                MusicLogUtils.d("Album", sb.toString());
                AlbumListAdapter.this.mActivity.init(cursor);
            }
        }

        AlbumListAdapter(Context context, AlbumBrowserActivity albumBrowserActivity, int i, Cursor cursor, String[] strArr, int[] iArr) {
            super(context, i, cursor, strArr, iArr);
            this.mStringBuilder = new StringBuilder();
            this.mFormatArgs = new Object[1];
            this.mConstraint = null;
            this.mConstraintIsValid = false;
            this.mWhat = 0;
            this.mActivity = albumBrowserActivity;
            this.mQueryHandler = new QueryHandler(context.getContentResolver());
            this.mUnknownAlbum = context.getString(R.string.unknown_album_name);
            this.mUnknownArtist = context.getString(R.string.unknown_artist_name);
            this.mAlbumSongSeparator = context.getString(R.string.albumsongseparator);
            Bitmap bitmapDecodeResource = BitmapFactory.decodeResource(context.getResources(), R.drawable.albumart_mp_unknown_list);
            this.mDefaultAlbumIcon = new BitmapDrawable(context.getResources(), bitmapDecodeResource);
            this.mDefaultAlbumIcon.setFilterBitmap(false);
            this.mDefaultAlbumIcon.setDither(false);
            getColumnIndices(cursor);
            this.mResources = context.getResources();
            this.mBackgroundAlbumIcon = new BitmapDrawable(context.getResources(), Bitmap.createBitmap(bitmapDecodeResource.getWidth(), bitmapDecodeResource.getHeight(), Bitmap.Config.ARGB_8888));
            this.mBackgroundAlbumIcon.setFilterBitmap(false);
            this.mBackgroundAlbumIcon.setDither(false);
            this.mLazyLoadingThread = new HandlerThread("LazyLoading");
            this.mLazyLoadingThread.start();
            this.mLazyLoaingHandler = new Handler(this.mLazyLoadingThread.getLooper()) {
                @Override
                public void handleMessage(Message message) {
                    int i2 = message.arg1;
                    if (i2 >= 0) {
                        MusicUtils.getCachedArtwork(AlbumListAdapter.this.mActivity, message.arg1, AlbumListAdapter.this.mDefaultAlbumIcon);
                    }
                    AlbumListAdapter.this.mActivity.mRefreshAlbumArtHandler.sendMessageDelayed(AlbumListAdapter.this.mActivity.mRefreshAlbumArtHandler.obtainMessage(0, i2, message.arg2), 100L);
                }
            };
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                this.mAlbumIdx = cursor.getColumnIndexOrThrow("album");
                this.mArtistIdx = cursor.getColumnIndexOrThrow("artist");
                this.mAlbumArtIndex = cursor.getColumnIndexOrThrow("album_art");
                if (this.mIndexer != null) {
                    this.mIndexer.setCursor(cursor);
                } else {
                    this.mIndexer = new MusicAlphabetIndexer(cursor, this.mAlbumIdx, this.mResources.getString(R.string.fast_scroll_alphabet));
                }
            }
        }

        public void setActivity(AlbumBrowserActivity albumBrowserActivity) {
            this.mActivity = albumBrowserActivity;
        }

        public AsyncQueryHandler getQueryHandler() {
            return this.mQueryHandler;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View viewNewView = super.newView(context, cursor, viewGroup);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.mLine1 = (TextView) viewNewView.findViewById(R.id.line1);
            viewHolder.mLine2 = (TextView) viewNewView.findViewById(R.id.line2);
            viewHolder.mPlayIndicator = (ImageView) viewNewView.findViewById(R.id.play_indicator);
            viewHolder.mIcon = (ImageView) viewNewView.findViewById(R.id.icon);
            viewHolder.mIcon.setBackgroundDrawable(this.mBackgroundAlbumIcon);
            viewHolder.mIcon.setPadding(0, 0, 1, 0);
            viewNewView.setTag(viewHolder);
            return viewNewView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            String string = cursor.getString(this.mAlbumIdx);
            boolean z = string == null || string.equals("<unknown>");
            if (z) {
                string = this.mUnknownAlbum;
            }
            viewHolder.mLine1.setText(string);
            String string2 = cursor.getString(this.mArtistIdx);
            if (string2 == null || string2.equals("<unknown>")) {
                string2 = this.mUnknownArtist;
            }
            viewHolder.mLine2.setText(string2);
            String string3 = cursor.getString(this.mAlbumArtIndex);
            long j = cursor.getLong(0);
            long currentAlbumId = MusicUtils.getCurrentAlbumId();
            ImageView imageView = viewHolder.mPlayIndicator;
            if (currentAlbumId == j) {
                imageView.setVisibility(0);
            } else {
                imageView.setVisibility(8);
            }
            ImageView imageView2 = viewHolder.mIcon;
            if (z || string3 == null || string3.length() == 0) {
                j = -1;
            }
            imageView2.setImageDrawable(null);
            if (this.mLazyLoaingHandler.hasMessages(this.mWhat)) {
                this.mLazyLoaingHandler.removeMessages(this.mWhat);
            }
            this.mLazyLoaingHandler.sendMessage(this.mLazyLoaingHandler.obtainMessage(this.mWhat, (int) j, cursor.getPosition()));
            this.mWhat++;
            this.mWhat %= this.mActivity.mVisibleItemsCount;
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if (this.mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != this.mActivity.mAlbumCursor) {
                this.mActivity.mAlbumCursor = cursor;
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence charSequence) {
            String string = charSequence.toString();
            if (!this.mConstraintIsValid || ((string != null || this.mConstraint != null) && (string == null || !string.equals(this.mConstraint)))) {
                Cursor albumCursor = this.mActivity.getAlbumCursor(null, string);
                this.mConstraint = string;
                this.mConstraintIsValid = true;
                return albumCursor;
            }
            return getCursor();
        }

        @Override
        public Object[] getSections() {
            return this.mIndexer.getSections();
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

        void quitLazyLoadingThread() {
            boolean zQuit;
            if (this.mLazyLoadingThread != null) {
                zQuit = this.mLazyLoadingThread.quit();
            } else {
                zQuit = false;
            }
            MusicLogUtils.d("Album", "Quit lazy loading thread when activity ondestroy: isQuitSuccuss = " + zQuit);
        }
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
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        this.mOrientation = configuration.orientation;
        if (findViewById(R.id.sd_icon).getVisibility() == 0) {
            MusicLogUtils.d("Album", "Configuration Changed at database error, return!");
            return;
        }
        if (this.mService != null) {
            MusicUtils.updateNowPlaying(this, this.mOrientation);
        }
        if (this.mSearchItem != null) {
            this.mQueryText = ((SearchView) this.mSearchItem.getActionView()).getQuery();
            MusicLogUtils.d("Album", "searchText:" + ((Object) this.mQueryText));
        }
        invalidateOptionsMenu();
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
    public boolean dispatchKeyEvent(KeyEvent keyEvent) {
        if (this.mSearchItem != null && keyEvent.getKeyCode() == 82 && this.mSearchItem.isActionViewExpanded()) {
            return true;
        }
        return super.dispatchKeyEvent(keyEvent);
    }

    public void refreshEmptyView() {
        if (getListView().getAdapter() != null && getListView().getAdapter().getCount() == 0 && getListView().getCount() != 0) {
            MusicLogUtils.d("Album", "getExpandableListView().getCount() = " + getListView().getCount());
            this.mListHandler.sendEmptyMessageDelayed(0, 200L);
            return;
        }
        MusicUtils.emptyShow(getListView(), this);
    }
}
