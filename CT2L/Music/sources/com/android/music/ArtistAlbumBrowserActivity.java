package com.android.music;

import android.app.ExpandableListActivity;
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
import android.database.CursorWrapper;
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
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;
import com.android.music.MusicUtils;

public class ArtistAlbumBrowserActivity extends ExpandableListActivity implements ServiceConnection, View.OnCreateContextMenuListener {
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    private ArtistAlbumListAdapter mAdapter;
    private boolean mAdapterSent;
    private Cursor mArtistCursor;
    private String mCurrentAlbumId;
    private String mCurrentAlbumName;
    private String mCurrentArtistId;
    private String mCurrentArtistName;
    private String mCurrentArtistNameForAlbum;
    boolean mIsUnknownAlbum;
    boolean mIsUnknownArtist;
    private MusicUtils.ServiceToken mToken;
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArtistAlbumBrowserActivity.this.getExpandableListView().invalidateViews();
            MusicUtils.updateNowPlaying(ArtistAlbumBrowserActivity.this);
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.setSpinnerState(ArtistAlbumBrowserActivity.this);
            ArtistAlbumBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
            if (intent.getAction().equals("android.intent.action.MEDIA_UNMOUNTED")) {
                MusicUtils.clearAlbumArtCache();
            }
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (ArtistAlbumBrowserActivity.this.mAdapter != null) {
                ArtistAlbumBrowserActivity.this.getArtistCursor(ArtistAlbumBrowserActivity.this.mAdapter.getQueryHandler(), null);
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(5);
        requestWindowFeature(1);
        setVolumeControlStream(3);
        if (icicle != null) {
            this.mCurrentAlbumId = icicle.getString("selectedalbum");
            this.mCurrentAlbumName = icicle.getString("selectedalbumname");
            this.mCurrentArtistId = icicle.getString("selectedartist");
            this.mCurrentArtistName = icicle.getString("selectedartistname");
        }
        this.mToken = MusicUtils.bindToService(this, this);
        IntentFilter f = new IntentFilter();
        f.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        f.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        f.addAction("android.intent.action.MEDIA_UNMOUNTED");
        f.addDataScheme("file");
        registerReceiver(this.mScanListener, f);
        setContentView(R.layout.media_picker_activity_expanding);
        MusicUtils.updateButtonBar(this, R.id.artisttab);
        ExpandableListView lv = getExpandableListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);
        this.mAdapter = (ArtistAlbumListAdapter) getLastNonConfigurationInstance();
        if (this.mAdapter == null) {
            this.mAdapter = new ArtistAlbumListAdapter(getApplication(), this, null, R.layout.track_list_item_group, new String[0], new int[0], R.layout.track_list_item_child, new String[0], new int[0]);
            setListAdapter(this.mAdapter);
            setTitle(R.string.working_artists);
            getArtistCursor(this.mAdapter.getQueryHandler(), null);
            return;
        }
        this.mAdapter.setActivity(this);
        setListAdapter(this.mAdapter);
        this.mArtistCursor = this.mAdapter.getCursor();
        if (this.mArtistCursor != null) {
            init(this.mArtistCursor);
        } else {
            getArtistCursor(this.mAdapter.getQueryHandler(), null);
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
        outcicle.putString("selectedalbumname", this.mCurrentAlbumName);
        outcicle.putString("selectedartist", this.mCurrentArtistId);
        outcicle.putString("selectedartistname", this.mCurrentArtistName);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        ExpandableListView lv = getExpandableListView();
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
        setListAdapter(null);
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
            if (this.mArtistCursor == null) {
                MusicUtils.displayDatabaseError(this);
                closeContextMenu();
                this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
                return;
            }
            if (mLastListPosCourse >= 0) {
                ExpandableListView elv = getExpandableListView();
                elv.setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
                mLastListPosCourse = -1;
            }
            MusicUtils.hideDatabaseError(this);
            MusicUtils.updateButtonBar(this, R.id.artisttab);
            setTitle();
        }
    }

    private void setTitle() {
        setTitle(R.string.artists_title);
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        this.mCurrentAlbumId = Long.valueOf(id).toString();
        Intent intent = new Intent("android.intent.action.PICK");
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", this.mCurrentAlbumId);
        Cursor c = (Cursor) getExpandableListAdapter().getChild(groupPosition, childPosition);
        String album = c.getString(c.getColumnIndex("album"));
        if (album == null || album.equals("<unknown>")) {
            this.mArtistCursor.moveToPosition(groupPosition);
            this.mCurrentArtistId = this.mArtistCursor.getString(this.mArtistCursor.getColumnIndex("_id"));
            intent.putExtra("artist", this.mCurrentArtistId);
        }
        startActivity(intent);
        return true;
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

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfoIn) {
        menu.add(0, 5, 0, R.string.play_selection);
        SubMenu sub = menu.addSubMenu(0, 1, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, sub);
        menu.add(0, 10, 0, R.string.delete_item);
        ExpandableListView.ExpandableListContextMenuInfo mi = (ExpandableListView.ExpandableListContextMenuInfo) menuInfoIn;
        int itemtype = ExpandableListView.getPackedPositionType(mi.packedPosition);
        int gpos = ExpandableListView.getPackedPositionGroup(mi.packedPosition);
        int cpos = ExpandableListView.getPackedPositionChild(mi.packedPosition);
        if (itemtype == 0) {
            if (gpos == -1) {
                Log.d("Artist/Album", "no group");
                return;
            }
            this.mArtistCursor.moveToPosition(gpos - getExpandableListView().getHeaderViewsCount());
            this.mCurrentArtistId = this.mArtistCursor.getString(this.mArtistCursor.getColumnIndexOrThrow("_id"));
            this.mCurrentArtistName = this.mArtistCursor.getString(this.mArtistCursor.getColumnIndexOrThrow("artist"));
            this.mCurrentAlbumId = null;
            this.mIsUnknownArtist = this.mCurrentArtistName == null || this.mCurrentArtistName.equals("<unknown>");
            this.mIsUnknownAlbum = true;
            if (this.mIsUnknownArtist) {
                menu.setHeaderTitle(getString(R.string.unknown_artist_name));
                return;
            } else {
                menu.setHeaderTitle(this.mCurrentArtistName);
                menu.add(0, 14, 0, R.string.search_title);
                return;
            }
        }
        if (itemtype == 1) {
            if (cpos == -1) {
                Log.d("Artist/Album", "no child");
                return;
            }
            Cursor c = (Cursor) getExpandableListAdapter().getChild(gpos, cpos);
            c.moveToPosition(cpos);
            this.mCurrentArtistId = null;
            this.mCurrentAlbumId = Long.valueOf(mi.id).toString();
            this.mCurrentAlbumName = c.getString(c.getColumnIndexOrThrow("album"));
            this.mArtistCursor.moveToPosition(gpos - getExpandableListView().getHeaderViewsCount());
            this.mCurrentArtistNameForAlbum = this.mArtistCursor.getString(this.mArtistCursor.getColumnIndexOrThrow("artist"));
            this.mIsUnknownArtist = this.mCurrentArtistNameForAlbum == null || this.mCurrentArtistNameForAlbum.equals("<unknown>");
            this.mIsUnknownAlbum = this.mCurrentAlbumName == null || this.mCurrentAlbumName.equals("<unknown>");
            if (this.mIsUnknownAlbum) {
                menu.setHeaderTitle(getString(R.string.unknown_album_name));
            } else {
                menu.setHeaderTitle(this.mCurrentAlbumName);
            }
            if (!this.mIsUnknownAlbum || !this.mIsUnknownArtist) {
                menu.add(0, 14, 0, R.string.search_title);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        long[] list;
        String f;
        String desc;
        String f2;
        switch (item.getItemId()) {
            case 3:
                long[] list2 = this.mCurrentArtistId != null ? MusicUtils.getSongListForArtist(this, Long.parseLong(this.mCurrentArtistId)) : MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                long playlist = item.getIntent().getLongExtra("playlist", 0L);
                MusicUtils.addToPlaylist(this, list2, playlist);
                return true;
            case 4:
                Intent intent = new Intent();
                intent.setClass(this, CreatePlaylist.class);
                startActivityForResult(intent, 4);
                return true;
            case 5:
                long[] list3 = this.mCurrentArtistId != null ? MusicUtils.getSongListForArtist(this, Long.parseLong(this.mCurrentArtistId)) : MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                MusicUtils.playAll(this, list3, 0);
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
                if (this.mCurrentArtistId != null) {
                    list = MusicUtils.getSongListForArtist(this, Long.parseLong(this.mCurrentArtistId));
                    if (Environment.isExternalStorageRemovable()) {
                        f2 = getString(R.string.delete_artist_desc);
                    } else {
                        f2 = getString(R.string.delete_artist_desc_nosdcard);
                    }
                    desc = String.format(f2, this.mCurrentArtistName);
                } else {
                    list = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                    if (Environment.isExternalStorageRemovable()) {
                        f = getString(R.string.delete_album_desc);
                    } else {
                        f = getString(R.string.delete_album_desc_nosdcard);
                    }
                    desc = String.format(f, this.mCurrentAlbumName);
                }
                Bundle b = new Bundle();
                b.putString("description", desc);
                b.putLongArray("items", list);
                Intent intent2 = new Intent();
                intent2.setClass(this, DeleteItems.class);
                intent2.putExtras(b);
                startActivityForResult(intent2, -1);
                return true;
            case 12:
                long[] list4 = this.mCurrentArtistId != null ? MusicUtils.getSongListForArtist(this, Long.parseLong(this.mCurrentArtistId)) : MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                MusicUtils.addToCurrentPlaylist(this, list4);
                return true;
            case 14:
                doSearch();
                return true;
        }
    }

    void doSearch() {
        String query;
        Object title;
        if (BenesseExtension.getDchaState() == 0) {
            Intent i = new Intent();
            i.setAction("android.intent.action.MEDIA_SEARCH");
            i.setFlags(268435456);
            if (this.mCurrentArtistId != null) {
                title = this.mCurrentArtistName;
                query = this.mCurrentArtistName;
                i.putExtra("android.intent.extra.artist", this.mCurrentArtistName);
                i.putExtra("android.intent.extra.focus", "vnd.android.cursor.item/artist");
            } else {
                if (this.mIsUnknownAlbum) {
                    query = this.mCurrentArtistNameForAlbum;
                    title = query;
                } else {
                    query = this.mCurrentAlbumName;
                    title = query;
                    if (!this.mIsUnknownArtist) {
                        query = query + " " + this.mCurrentArtistNameForAlbum;
                    }
                }
                i.putExtra("android.intent.extra.artist", this.mCurrentArtistNameForAlbum);
                i.putExtra("android.intent.extra.album", this.mCurrentAlbumName);
                i.putExtra("android.intent.extra.focus", "vnd.android.cursor.item/album");
            }
            String title2 = getString(R.string.mediasearch, new Object[]{title});
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
                    long[] list = null;
                    if (this.mCurrentArtistId != null) {
                        list = MusicUtils.getSongListForArtist(this, Long.parseLong(this.mCurrentArtistId));
                    } else if (this.mCurrentAlbumId != null) {
                        list = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                    }
                    MusicUtils.addToPlaylist(this, list, Long.parseLong(uri.getLastPathSegment()));
                    break;
                }
                break;
            case 11:
                if (resultCode == 0) {
                    finish();
                } else {
                    getArtistCursor(this.mAdapter.getQueryHandler(), null);
                }
                break;
        }
    }

    private Cursor getArtistCursor(AsyncQueryHandler async, String filter) {
        String[] cols = {"_id", "artist", "number_of_albums", "number_of_tracks"};
        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        if (!TextUtils.isEmpty(filter)) {
            uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filter)).build();
        }
        if (async != null) {
            async.startQuery(0, null, uri, cols, null, null, "artist_key");
            return null;
        }
        Cursor ret = MusicUtils.query(this, uri, cols, null, null, "artist_key");
        return ret;
    }

    static class ArtistAlbumListAdapter extends SimpleCursorTreeAdapter implements SectionIndexer {
        private ArtistAlbumBrowserActivity mActivity;
        private final String mAlbumSongSeparator;
        private final StringBuilder mBuffer;
        private String mConstraint;
        private boolean mConstraintIsValid;
        private final Context mContext;
        private final BitmapDrawable mDefaultAlbumIcon;
        private final Object[] mFormatArgs;
        private final Object[] mFormatArgs3;
        private int mGroupAlbumIdx;
        private int mGroupArtistIdIdx;
        private int mGroupArtistIdx;
        private int mGroupSongIdx;
        private MusicAlphabetIndexer mIndexer;
        private final Drawable mNowPlayingOverlay;
        private AsyncQueryHandler mQueryHandler;
        private final Resources mResources;
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
                ArtistAlbumListAdapter.this.mActivity.init(cursor);
            }
        }

        ArtistAlbumListAdapter(Context context, ArtistAlbumBrowserActivity currentactivity, Cursor cursor, int glayout, String[] gfrom, int[] gto, int clayout, String[] cfrom, int[] cto) {
            super(context, cursor, glayout, gfrom, gto, clayout, cfrom, cto);
            this.mBuffer = new StringBuilder();
            this.mFormatArgs = new Object[1];
            this.mFormatArgs3 = new Object[3];
            this.mConstraint = null;
            this.mConstraintIsValid = false;
            this.mActivity = currentactivity;
            this.mQueryHandler = new QueryHandler(context.getContentResolver());
            Resources r = context.getResources();
            this.mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);
            this.mDefaultAlbumIcon = (BitmapDrawable) r.getDrawable(R.drawable.albumart_mp_unknown_list);
            this.mDefaultAlbumIcon.setFilterBitmap(false);
            this.mDefaultAlbumIcon.setDither(false);
            this.mContext = context;
            getColumnIndices(cursor);
            this.mResources = context.getResources();
            this.mAlbumSongSeparator = context.getString(R.string.albumsongseparator);
            this.mUnknownAlbum = context.getString(R.string.unknown_album_name);
            this.mUnknownArtist = context.getString(R.string.unknown_artist_name);
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                this.mGroupArtistIdIdx = cursor.getColumnIndexOrThrow("_id");
                this.mGroupArtistIdx = cursor.getColumnIndexOrThrow("artist");
                this.mGroupAlbumIdx = cursor.getColumnIndexOrThrow("number_of_albums");
                this.mGroupSongIdx = cursor.getColumnIndexOrThrow("number_of_tracks");
                if (this.mIndexer != null) {
                    this.mIndexer.setCursor(cursor);
                } else {
                    this.mIndexer = new MusicAlphabetIndexer(cursor, this.mGroupArtistIdx, this.mResources.getString(R.string.fast_scroll_alphabet));
                }
            }
        }

        public void setActivity(ArtistAlbumBrowserActivity newactivity) {
            this.mActivity = newactivity;
        }

        public AsyncQueryHandler getQueryHandler() {
            return this.mQueryHandler;
        }

        @Override
        public View newGroupView(Context context, Cursor cursor, boolean isExpanded, ViewGroup parent) {
            View v = super.newGroupView(context, cursor, isExpanded, parent);
            ImageView iv = (ImageView) v.findViewById(R.id.icon);
            ViewGroup.LayoutParams p = iv.getLayoutParams();
            p.width = -2;
            p.height = -2;
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.icon = (ImageView) v.findViewById(R.id.icon);
            vh.icon.setPadding(0, 0, 1, 0);
            v.setTag(vh);
            return v;
        }

        @Override
        public View newChildView(Context context, Cursor cursor, boolean isLastChild, ViewGroup parent) {
            View v = super.newChildView(context, cursor, isLastChild, parent);
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
        public void bindGroupView(View view, Context context, Cursor cursor, boolean isexpanded) {
            ViewHolder vh = (ViewHolder) view.getTag();
            String artist = cursor.getString(this.mGroupArtistIdx);
            String displayartist = artist;
            boolean unknown = artist == null || artist.equals("<unknown>");
            if (unknown) {
                displayartist = this.mUnknownArtist;
            }
            vh.line1.setText(displayartist);
            int numalbums = cursor.getInt(this.mGroupAlbumIdx);
            int numsongs = cursor.getInt(this.mGroupSongIdx);
            String songs_albums = MusicUtils.makeAlbumsLabel(context, numalbums, numsongs, unknown);
            vh.line2.setText(songs_albums);
            long currentartistid = MusicUtils.getCurrentArtistId();
            long artistid = cursor.getLong(this.mGroupArtistIdIdx);
            if (currentartistid == artistid && !isexpanded) {
                vh.play_indicator.setImageDrawable(this.mNowPlayingOverlay);
            } else {
                vh.play_indicator.setImageDrawable(null);
            }
        }

        @Override
        public void bindChildView(View view, Context context, Cursor cursor, boolean islast) {
            ViewHolder vh = (ViewHolder) view.getTag();
            String name = cursor.getString(cursor.getColumnIndexOrThrow("album"));
            String displayname = name;
            boolean unknown = name == null || name.equals("<unknown>");
            if (unknown) {
                displayname = this.mUnknownAlbum;
            }
            vh.line1.setText(displayname);
            int numsongs = cursor.getInt(cursor.getColumnIndexOrThrow("numsongs"));
            int numartistsongs = cursor.getInt(cursor.getColumnIndexOrThrow("numsongs_by_artist"));
            StringBuilder builder = this.mBuffer;
            builder.delete(0, builder.length());
            if (unknown) {
                numsongs = numartistsongs;
            }
            if (numsongs == 1) {
                builder.append(context.getString(R.string.onesong));
            } else if (numsongs == numartistsongs) {
                Object[] args = this.mFormatArgs;
                args[0] = Integer.valueOf(numsongs);
                builder.append(this.mResources.getQuantityString(R.plurals.Nsongs, numsongs, args));
            } else {
                Object[] args2 = this.mFormatArgs3;
                args2[0] = Integer.valueOf(numsongs);
                args2[1] = Integer.valueOf(numartistsongs);
                args2[2] = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                builder.append(this.mResources.getQuantityString(R.plurals.Nsongscomp, numsongs, args2));
            }
            vh.line2.setText(builder.toString());
            ImageView iv = vh.icon;
            String art = cursor.getString(cursor.getColumnIndexOrThrow("album_art"));
            if (unknown || art == null || art.length() == 0) {
                iv.setBackgroundDrawable(this.mDefaultAlbumIcon);
                iv.setImageDrawable(null);
            } else {
                long artIndex = cursor.getLong(0);
                Drawable d = MusicUtils.getCachedArtwork(context, artIndex, this.mDefaultAlbumIcon);
                iv.setImageDrawable(d);
            }
            long currentalbumid = MusicUtils.getCurrentAlbumId();
            long aid = cursor.getLong(0);
            ImageView iv2 = vh.play_indicator;
            if (currentalbumid == aid) {
                iv2.setImageDrawable(this.mNowPlayingOverlay);
            } else {
                iv2.setImageDrawable(null);
            }
        }

        @Override
        protected Cursor getChildrenCursor(Cursor groupCursor) {
            long id = groupCursor.getLong(groupCursor.getColumnIndexOrThrow("_id"));
            String[] cols = {"_id", "album", "numsongs", "numsongs_by_artist", "album_art"};
            Cursor c = MusicUtils.query(this.mActivity, MediaStore.Audio.Artists.Albums.getContentUri("external", id), cols, null, null, "album_key");
            return new CursorWrapper(c, groupCursor.getString(this.mGroupArtistIdx)) {
                String mArtistName;
                int mMagicColumnIdx;

                {
                    super(c);
                    this.mArtistName = artist;
                    if (this.mArtistName == null || this.mArtistName.equals("<unknown>")) {
                        this.mArtistName = ArtistAlbumListAdapter.this.mUnknownArtist;
                    }
                    this.mMagicColumnIdx = c.getColumnCount();
                }

                @Override
                public String getString(int columnIndex) {
                    return columnIndex != this.mMagicColumnIdx ? super.getString(columnIndex) : this.mArtistName;
                }

                @Override
                public int getColumnIndexOrThrow(String name) {
                    return "artist".equals(name) ? this.mMagicColumnIdx : super.getColumnIndexOrThrow(name);
                }

                @Override
                public String getColumnName(int idx) {
                    return idx != this.mMagicColumnIdx ? super.getColumnName(idx) : "artist";
                }

                @Override
                public int getColumnCount() {
                    return super.getColumnCount() + 1;
                }
            };
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if (this.mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != this.mActivity.mArtistCursor) {
                this.mActivity.mArtistCursor = cursor;
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (!this.mConstraintIsValid || ((s != null || this.mConstraint != null) && (s == null || !s.equals(this.mConstraint)))) {
                Cursor artistCursor = this.mActivity.getArtistCursor(null, s);
                this.mConstraint = s;
                this.mConstraintIsValid = true;
                return artistCursor;
            }
            return getCursor();
        }

        @Override
        public Object[] getSections() {
            return this.mIndexer.getSections();
        }

        @Override
        public int getPositionForSection(int sectionIndex) {
            return this.mIndexer.getPositionForSection(sectionIndex);
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
