package com.android.music;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.ContextMenu;
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

public class AlbumBrowserActivity extends ListActivity implements ServiceConnection, View.OnCreateContextMenuListener {
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    private AlbumListAdapter mAdapter;
    private boolean mAdapterSent;
    private Cursor mAlbumCursor;
    private String mArtistId;
    private String mCurrentAlbumId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    boolean mIsUnknownAlbum;
    boolean mIsUnknownArtist;
    private MusicUtils.ServiceToken mToken;
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AlbumBrowserActivity.this.getListView().invalidateViews();
            MusicUtils.updateNowPlaying(AlbumBrowserActivity.this);
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.setSpinnerState(AlbumBrowserActivity.this);
            AlbumBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
            if (intent.getAction().equals("android.intent.action.MEDIA_UNMOUNTED")) {
                MusicUtils.clearAlbumArtCache();
            }
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (AlbumBrowserActivity.this.mAdapter != null) {
                AlbumBrowserActivity.this.getAlbumCursor(AlbumBrowserActivity.this.mAdapter.getQueryHandler(), null);
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        if (icicle != null) {
            this.mCurrentAlbumId = icicle.getString("selectedalbum");
            this.mArtistId = icicle.getString("artist");
        } else {
            this.mArtistId = getIntent().getStringExtra("artist");
        }
        super.onCreate(icicle);
        requestWindowFeature(5);
        requestWindowFeature(1);
        setVolumeControlStream(3);
        this.mToken = MusicUtils.bindToService(this, this);
        IntentFilter f = new IntentFilter();
        f.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        f.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        f.addAction("android.intent.action.MEDIA_UNMOUNTED");
        f.addDataScheme("file");
        registerReceiver(this.mScanListener, f);
        setContentView(R.layout.media_picker_activity);
        MusicUtils.updateButtonBar(this, R.id.albumtab);
        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);
        this.mAdapter = (AlbumListAdapter) getLastNonConfigurationInstance();
        if (this.mAdapter == null) {
            this.mAdapter = new AlbumListAdapter(getApplication(), this, R.layout.track_list_item, this.mAlbumCursor, new String[0], new int[0]);
            setListAdapter(this.mAdapter);
            setTitle(R.string.working_albums);
            getAlbumCursor(this.mAdapter.getQueryHandler(), null);
            return;
        }
        this.mAdapter.setActivity(this);
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
    public void onSaveInstanceState(Bundle outcicle) {
        outcicle.putString("selectedalbum", this.mCurrentAlbumId);
        outcicle.putString("artist", this.mArtistId);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        ListView lv = getListView();
        if (lv != null) {
            mLastListPosCourse = lv.getFirstVisiblePosition();
            View cv = lv.getChildAt(0);
            if (cv != null) {
                mLastListPosFine = cv.getTop();
            }
        }
        MusicUtils.unbindFromService(this.mToken);
        if (!this.mAdapterSent && this.mAdapter != null) {
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
        IntentFilter f = new IntentFilter();
        f.addAction("com.android.music.metachanged");
        f.addAction("com.android.music.queuechanged");
        registerReceiver(this.mTrackListListener, f);
        this.mTrackListListener.onReceive(null, null);
        MusicUtils.setSpinnerState(this);
    }

    @Override
    public void onPause() {
        unregisterReceiver(this.mTrackListListener);
        this.mReScanHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    public void init(Cursor c) {
        if (this.mAdapter != null) {
            this.mAdapter.changeCursor(c);
            if (this.mAlbumCursor == null) {
                MusicUtils.displayDatabaseError(this);
                closeContextMenu();
                this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
            } else {
                if (mLastListPosCourse >= 0) {
                    getListView().setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
                    mLastListPosCourse = -1;
                }
                MusicUtils.hideDatabaseError(this);
                MusicUtils.updateButtonBar(this, R.id.albumtab);
                setTitle();
            }
        }
    }

    private void setTitle() {
        CharSequence fancyName = "";
        if (this.mAlbumCursor != null && this.mAlbumCursor.getCount() > 0) {
            this.mAlbumCursor.moveToFirst();
            fancyName = this.mAlbumCursor.getString(this.mAlbumCursor.getColumnIndex("artist"));
            if (fancyName == null || fancyName.equals("<unknown>")) {
                fancyName = getText(R.string.unknown_artist_name);
            }
        }
        if (this.mArtistId != null && fancyName != null) {
            setTitle(fancyName);
        } else {
            setTitle(R.string.albums_title);
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfoIn) {
        boolean z = true;
        menu.add(0, 5, 0, R.string.play_selection);
        SubMenu sub = menu.addSubMenu(0, 1, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, sub);
        menu.add(0, 10, 0, R.string.delete_item);
        AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo) menuInfoIn;
        this.mAlbumCursor.moveToPosition(mi.position);
        this.mCurrentAlbumId = this.mAlbumCursor.getString(this.mAlbumCursor.getColumnIndexOrThrow("_id"));
        this.mCurrentAlbumName = this.mAlbumCursor.getString(this.mAlbumCursor.getColumnIndexOrThrow("album"));
        this.mCurrentArtistNameForAlbum = this.mAlbumCursor.getString(this.mAlbumCursor.getColumnIndexOrThrow("artist"));
        this.mIsUnknownArtist = this.mCurrentArtistNameForAlbum == null || this.mCurrentArtistNameForAlbum.equals("<unknown>");
        if (this.mCurrentAlbumName != null && !this.mCurrentAlbumName.equals("<unknown>")) {
            z = false;
        }
        this.mIsUnknownAlbum = z;
        if (this.mIsUnknownAlbum) {
            menu.setHeaderTitle(getString(R.string.unknown_album_name));
        } else {
            menu.setHeaderTitle(this.mCurrentAlbumName);
        }
        if (!this.mIsUnknownAlbum || !this.mIsUnknownArtist) {
            menu.add(0, 14, 0, R.string.search_title);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        String f;
        switch (item.getItemId()) {
            case 3:
                long[] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                long playlist = item.getIntent().getLongExtra("playlist", 0L);
                MusicUtils.addToPlaylist(this, list, playlist);
                return true;
            case 4:
                Intent intent = new Intent();
                intent.setClass(this, CreatePlaylist.class);
                startActivityForResult(intent, 4);
                return true;
            case 5:
                long[] list2 = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                MusicUtils.playAll(this, list2, 0);
                return true;
            case 6:
            case 7:
            case 8:
            case 9:
            case 11:
            case 13:
            default:
                return super.onContextItemSelected(item);
            case 10:
                long[] list3 = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                if (Environment.isExternalStorageRemovable()) {
                    f = getString(R.string.delete_album_desc);
                } else {
                    f = getString(R.string.delete_album_desc_nosdcard);
                }
                String desc = String.format(f, this.mCurrentAlbumName);
                Bundle b = new Bundle();
                b.putString("description", desc);
                b.putLongArray("items", list3);
                Intent intent2 = new Intent();
                intent2.setClass(this, DeleteItems.class);
                intent2.putExtras(b);
                startActivityForResult(intent2, -1);
                return true;
            case 12:
                long[] list4 = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                MusicUtils.addToCurrentPlaylist(this, list4);
                return true;
            case 14:
                doSearch();
                return true;
        }
    }

    void doSearch() {
        if (BenesseExtension.getDchaState() == 0) {
            String query = "";
            Intent i = new Intent();
            i.setAction("android.intent.action.MEDIA_SEARCH");
            i.setFlags(268435456);
            CharSequence title = "";
            if (!this.mIsUnknownAlbum) {
                query = this.mCurrentAlbumName;
                i.putExtra("android.intent.extra.album", this.mCurrentAlbumName);
                title = this.mCurrentAlbumName;
            }
            if (!this.mIsUnknownArtist) {
                query = query + " " + this.mCurrentArtistNameForAlbum;
                i.putExtra("android.intent.extra.artist", this.mCurrentArtistNameForAlbum);
                title = ((Object) title) + " " + this.mCurrentArtistNameForAlbum;
            }
            i.putExtra("android.intent.extra.focus", "vnd.android.cursor.item/album");
            CharSequence title2 = getString(R.string.mediasearch, new Object[]{title});
            i.putExtra("query", query);
            startActivity(Intent.createChooser(i, title2));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Uri uri;
        switch (requestCode) {
            case 4:
                if (resultCode == -1 && (uri = intent.getData()) != null) {
                    long[] list = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                    MusicUtils.addToPlaylist(this, list, Long.parseLong(uri.getLastPathSegment()));
                    break;
                }
                break;
            case 11:
                if (resultCode == 0) {
                    finish();
                } else {
                    getAlbumCursor(this.mAdapter.getQueryHandler(), null);
                }
                break;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent("android.intent.action.PICK");
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", Long.valueOf(id).toString());
        intent.putExtra("artist", this.mArtistId);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 8, 0, R.string.party_shuffle);
        menu.add(0, 9, 0, R.string.shuffle_all).setIcon(R.drawable.ic_menu_shuffle);
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
        }
        return super.onOptionsItemSelected(item);
    }

    private Cursor getAlbumCursor(AsyncQueryHandler async, String filter) {
        String[] cols = {"_id", "artist", "album", "album_art"};
        if (this.mArtistId != null) {
            Uri uri = MediaStore.Audio.Artists.Albums.getContentUri("external", Long.valueOf(this.mArtistId).longValue());
            if (!TextUtils.isEmpty(filter)) {
                uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
            }
            if (async != null) {
                async.startQuery(0, null, uri, cols, null, null, "album_key");
                return null;
            }
            Cursor ret = MusicUtils.query(this, uri, cols, null, null, "album_key");
            return ret;
        }
        Uri uri2 = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI;
        if (!TextUtils.isEmpty(filter)) {
            uri2 = uri2.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
        }
        if (async != null) {
            async.startQuery(0, null, uri2, cols, null, null, "album_key");
            return null;
        }
        Cursor ret2 = MusicUtils.query(this, uri2, cols, null, null, "album_key");
        return ret2;
    }

    static class AlbumListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        private AlbumBrowserActivity mActivity;
        private int mAlbumArtIndex;
        private int mAlbumIdx;
        private final String mAlbumSongSeparator;
        private int mArtistIdx;
        private String mConstraint;
        private boolean mConstraintIsValid;
        private final BitmapDrawable mDefaultAlbumIcon;
        private final Object[] mFormatArgs;
        private AlphabetIndexer mIndexer;
        private final Drawable mNowPlayingOverlay;
        private AsyncQueryHandler mQueryHandler;
        private final Resources mResources;
        private final StringBuilder mStringBuilder;
        private final String mUnknownAlbum;
        private final String mUnknownArtist;

        static class ViewHolder {
            ImageView icon;
            TextView line1;
            TextView line2;
            ImageView play_indicator;

            ViewHolder() {
            }
        }

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                AlbumListAdapter.this.mActivity.init(cursor);
            }
        }

        AlbumListAdapter(Context context, AlbumBrowserActivity currentactivity, int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);
            this.mStringBuilder = new StringBuilder();
            this.mFormatArgs = new Object[1];
            this.mConstraint = null;
            this.mConstraintIsValid = false;
            this.mActivity = currentactivity;
            this.mQueryHandler = new QueryHandler(context.getContentResolver());
            this.mUnknownAlbum = context.getString(R.string.unknown_album_name);
            this.mUnknownArtist = context.getString(R.string.unknown_artist_name);
            this.mAlbumSongSeparator = context.getString(R.string.albumsongseparator);
            Resources r = context.getResources();
            this.mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);
            Bitmap b = BitmapFactory.decodeResource(r, R.drawable.albumart_mp_unknown_list);
            this.mDefaultAlbumIcon = new BitmapDrawable(context.getResources(), b);
            this.mDefaultAlbumIcon.setFilterBitmap(false);
            this.mDefaultAlbumIcon.setDither(false);
            getColumnIndices(cursor);
            this.mResources = context.getResources();
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

        public void setActivity(AlbumBrowserActivity newactivity) {
            this.mActivity = newactivity;
        }

        public AsyncQueryHandler getQueryHandler() {
            return this.mQueryHandler;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.icon = (ImageView) v.findViewById(R.id.icon);
            vh.icon.setBackgroundDrawable(this.mDefaultAlbumIcon);
            vh.icon.setPadding(0, 0, 1, 0);
            v.setTag(vh);
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder vh = (ViewHolder) view.getTag();
            String name = cursor.getString(this.mAlbumIdx);
            String displayname = name;
            boolean unknown = name == null || name.equals("<unknown>");
            if (unknown) {
                displayname = this.mUnknownAlbum;
            }
            vh.line1.setText(displayname);
            String name2 = cursor.getString(this.mArtistIdx);
            String displayname2 = name2;
            if (name2 == null || name2.equals("<unknown>")) {
                displayname2 = this.mUnknownArtist;
            }
            vh.line2.setText(displayname2);
            ImageView iv = vh.icon;
            String art = cursor.getString(this.mAlbumArtIndex);
            long aid = cursor.getLong(0);
            if (unknown || art == null || art.length() == 0) {
                iv.setImageDrawable(null);
            } else {
                Drawable d = MusicUtils.getCachedArtwork(context, aid, this.mDefaultAlbumIcon);
                iv.setImageDrawable(d);
            }
            long currentalbumid = MusicUtils.getCurrentAlbumId();
            ImageView iv2 = vh.play_indicator;
            if (currentalbumid == aid) {
                iv2.setImageDrawable(this.mNowPlayingOverlay);
            } else {
                iv2.setImageDrawable(null);
            }
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
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (!this.mConstraintIsValid || ((s != null || this.mConstraint != null) && (s == null || !s.equals(this.mConstraint)))) {
                Cursor albumCursor = this.mActivity.getAlbumCursor(null, s);
                this.mConstraint = s;
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
        public int getPositionForSection(int section) {
            return this.mIndexer.getPositionForSection(section);
        }

        @Override
        public int getSectionForPosition(int position) {
            return 0;
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        MusicUtils.updateNowPlaying(this);
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        finish();
    }
}
