package com.android.music;

import android.app.DialogFragment;
import android.app.ExpandableListActivity;
import android.app.FragmentManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.BenesseExtension;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

public class ArtistAlbumBrowserActivity extends ExpandableListActivity implements View.OnCreateContextMenuListener {
    private static int sLastListPosCourse = -1;
    private static int sLastListPosFine = -1;
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
    private boolean mIsMounted = true;
    private SubMenu mSubMenu = null;
    private final String[] mCursorCols = {"_id", "artist", "number_of_albums", "number_of_tracks"};
    private boolean mActvityResumAfterCreate = true;
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ArtistAlbumBrowserActivity.this.getExpandableListView().invalidateViews();
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MusicLogUtils.d("Artist/Album", "mScanListener.onReceive:" + action + ", status = " + Environment.getExternalStorageState());
            if ("android.intent.action.MEDIA_SCANNER_STARTED".equals(action) || "android.intent.action.MEDIA_SCANNER_FINISHED".equals(action)) {
                MusicUtils.setSpinnerState(ArtistAlbumBrowserActivity.this);
                ArtistAlbumBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                ArtistAlbumBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
                return;
            }
            if ("android.intent.action.MEDIA_UNMOUNTED".equals(action)) {
                ArtistAlbumBrowserActivity.this.mIsMounted = false;
                ArtistAlbumBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                ArtistAlbumBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
                ArtistAlbumBrowserActivity.this.closeContextMenu();
                ArtistAlbumBrowserActivity.this.closeOptionsMenu();
                if (ArtistAlbumBrowserActivity.this.mSubMenu != null) {
                    ArtistAlbumBrowserActivity.this.mSubMenu.close();
                }
                ArtistAlbumBrowserActivity.this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
                return;
            }
            if ("android.intent.action.MEDIA_MOUNTED".equals(action)) {
                ArtistAlbumBrowserActivity.this.mIsMounted = true;
                ArtistAlbumBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                ArtistAlbumBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
            }
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (ArtistAlbumBrowserActivity.this.mAdapter != null) {
                ArtistAlbumBrowserActivity.this.getArtistCursor(ArtistAlbumBrowserActivity.this.mAdapter.getQueryHandler(), null);
            }
        }
    };
    public Handler mListHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            ArtistAlbumBrowserActivity.this.reFreshEmptyView();
        }
    };
    private final BroadcastReceiver mHdmiChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.clearAlbumArtCache();
            ArtistAlbumBrowserActivity.this.getExpandableListView().invalidateViews();
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        PDebug.Start("ArtistAlbumBrowserActivity.onCreate");
        super.onCreate(bundle);
        MusicLogUtils.d("Artist/Album", "onCreate");
        requestWindowFeature(5);
        requestWindowFeature(1);
        setVolumeControlStream(3);
        if (bundle != null) {
            this.mCurrentAlbumId = bundle.getString("selectedalbum");
            this.mCurrentAlbumName = bundle.getString("selectedalbumname");
            this.mCurrentArtistId = bundle.getString("selectedartist");
            this.mCurrentArtistName = bundle.getString("selectedartistname");
        } else {
            Intent intent = getIntent();
            this.mCurrentAlbumId = intent.getStringExtra("selectedalbum");
            this.mCurrentArtistId = intent.getStringExtra("selectedartist");
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addDataScheme("file");
        registerReceiver(this.mScanListener, intentFilter);
        setContentView(R.layout.media_picker_activity_expanding);
        ExpandableListView expandableListView = getExpandableListView();
        expandableListView.setOnCreateContextMenuListener(this);
        expandableListView.setTextFilterEnabled(true);
        this.mAdapter = (ArtistAlbumListAdapter) getLastNonConfigurationInstance();
        if (this.mAdapter == null) {
            PDebug.Start("ArtistAlbumBrowserActivity.setListAdapter()");
            MusicLogUtils.d("Artist/Album", "starting query");
            this.mAdapter = new ArtistAlbumListAdapter(getApplication(), this, null, R.layout.track_list_item_group, new String[0], new int[0], R.layout.track_list_item_child, new String[0], new int[0]);
            setListAdapter(this.mAdapter);
            setTitle(R.string.working_artists);
            PDebug.End("ArtistAlbumBrowserActivity.setListAdapter()");
            getArtistCursor(this.mAdapter.getQueryHandler(), null);
        } else {
            this.mAdapter.setActivity(this);
            this.mAdapter.reloadStringOnLocaleChanges();
            setListAdapter(this.mAdapter);
            this.mArtistCursor = this.mAdapter.getCursor();
            if (this.mArtistCursor != null) {
                init(this.mArtistCursor);
            } else {
                getArtistCursor(this.mAdapter.getQueryHandler(), null);
            }
        }
        PDebug.End("ArtistAlbumBrowserActivity.onCreate");
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        this.mAdapterSent = true;
        return this.mAdapter;
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString("selectedalbum", this.mCurrentAlbumId);
        bundle.putString("selectedalbumname", this.mCurrentAlbumName);
        bundle.putString("selectedartist", this.mCurrentArtistId);
        bundle.putString("selectedartistname", this.mCurrentArtistName);
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onDestroy() {
        MusicLogUtils.d("Artist/Album", "onDestroy");
        ExpandableListView expandableListView = getExpandableListView();
        if (expandableListView != null) {
            sLastListPosCourse = expandableListView.getFirstVisiblePosition();
            View childAt = expandableListView.getChildAt(0);
            if (childAt != null) {
                sLastListPosFine = childAt.getTop();
            }
        }
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
        PDebug.Start("ArtistAlbumBrowserActivity.onResume");
        super.onResume();
        MusicLogUtils.d("Artist/Album", "onResume>>>");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.music.metachanged");
        intentFilter.addAction("com.android.music.queuechanged");
        registerReceiver(this.mTrackListListener, intentFilter);
        this.mTrackListListener.onReceive(null, null);
        if (!this.mActvityResumAfterCreate) {
            reFreshEmptyView();
        }
        MusicUtils.setSpinnerState(this);
        MusicLogUtils.d("Artist/Album", "onResume<<<");
        PDebug.End("ArtistAlbumBrowserActivity.onResume");
    }

    public void reFreshEmptyView() {
        if (getExpandableListView().getAdapter() != null && getExpandableListView().getAdapter().getCount() == 0 && getExpandableListView().getCount() != 0) {
            this.mListHandler.sendEmptyMessageDelayed(0, 200L);
        } else {
            MusicUtils.emptyShow(getExpandableListView(), this);
        }
    }

    @Override
    public void onPause() {
        MusicLogUtils.d("Artist/Album", "onPause");
        this.mActvityResumAfterCreate = false;
        if (this.mSubMenu != null) {
            this.mSubMenu.close();
        }
        unregisterReceiver(this.mTrackListListener);
        this.mReScanHandler.removeCallbacksAndMessages(null);
        Intent intent = new Intent(getIntent());
        intent.putExtra("selectedalbum", this.mCurrentAlbumId);
        intent.putExtra("selectedartist", this.mCurrentArtistId);
        setIntent(intent);
        super.onPause();
    }

    public void init(Cursor cursor) {
        PDebug.Start("ArtistAlbumBrowserActivity.init");
        if (this.mAdapter == null) {
            return;
        }
        PDebug.Start("ArtistAlbumBrowserActivity.changeCursor");
        this.mAdapter.changeCursor(cursor);
        PDebug.End("ArtistAlbumBrowserActivity.changeCursor");
        if (this.mArtistCursor == null) {
            MusicLogUtils.d("Artist/Album", "mArtistCursor is null");
            MusicUtils.displayDatabaseError(this, this.mIsMounted);
            closeContextMenu();
            this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
            return;
        }
        MusicUtils.emptyShow(getExpandableListView(), this);
        if (sLastListPosCourse >= 0) {
            getExpandableListView().setSelectionFromTop(sLastListPosCourse, sLastListPosFine);
            sLastListPosCourse = -1;
        }
        MusicUtils.hideDatabaseError(this);
        setTitle();
        PDebug.End("ArtistAlbumBrowserActivity.init");
    }

    private void setTitle() {
        setTitle(R.string.artists_title);
    }

    @Override
    public boolean onChildClick(ExpandableListView expandableListView, View view, int i, int i2, long j) {
        this.mCurrentAlbumId = Long.valueOf(j).toString();
        Intent intent = new Intent("android.intent.action.PICK");
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", this.mCurrentAlbumId);
        Cursor cursor = (Cursor) getExpandableListAdapter().getChild(i, i2);
        String string = cursor.getString(cursor.getColumnIndex("album"));
        if (string == null || string.equals("<unknown>")) {
            this.mArtistCursor.moveToPosition(i);
            this.mCurrentArtistId = this.mArtistCursor.getString(this.mArtistCursor.getColumnIndex("_id"));
            intent.putExtra("artist", this.mCurrentArtistId);
        }
        startActivity(intent);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        boolean z;
        boolean z2;
        contextMenu.add(0, 5, 0, R.string.play_selection);
        boolean z3 = true;
        this.mSubMenu = contextMenu.addSubMenu(0, 1, 0, R.string.add_to_playlist);
        MusicUtils.makePlaylistMenu(this, this.mSubMenu);
        contextMenu.add(0, 10, 0, R.string.delete_item);
        ExpandableListView.ExpandableListContextMenuInfo expandableListContextMenuInfo = (ExpandableListView.ExpandableListContextMenuInfo) contextMenuInfo;
        int packedPositionType = ExpandableListView.getPackedPositionType(expandableListContextMenuInfo.packedPosition);
        int packedPositionGroup = ExpandableListView.getPackedPositionGroup(expandableListContextMenuInfo.packedPosition);
        int packedPositionChild = ExpandableListView.getPackedPositionChild(expandableListContextMenuInfo.packedPosition);
        if (packedPositionType == 0) {
            if (packedPositionGroup == -1) {
                MusicLogUtils.d("Artist/Album", "no group");
                return;
            }
            this.mArtistCursor.moveToPosition(packedPositionGroup - getExpandableListView().getHeaderViewsCount());
            this.mCurrentArtistId = this.mArtistCursor.getString(this.mArtistCursor.getColumnIndexOrThrow("_id"));
            this.mCurrentArtistName = this.mArtistCursor.getString(this.mArtistCursor.getColumnIndexOrThrow("artist"));
            this.mCurrentAlbumId = null;
            if (this.mCurrentArtistName != null && !this.mCurrentArtistName.equals("<unknown>")) {
                z2 = false;
            } else {
                z2 = true;
            }
            this.mIsUnknownArtist = z2;
            this.mIsUnknownAlbum = true;
            if (this.mIsUnknownArtist) {
                contextMenu.setHeaderTitle(getString(R.string.unknown_artist_name));
                return;
            } else {
                contextMenu.setHeaderTitle(this.mCurrentArtistName);
                contextMenu.add(0, 16, 0, R.string.search_title);
                return;
            }
        }
        if (packedPositionType == 1) {
            if (packedPositionChild == -1) {
                MusicLogUtils.d("Artist/Album", "no child");
                return;
            }
            Cursor cursor = (Cursor) getExpandableListAdapter().getChild(packedPositionGroup, packedPositionChild);
            cursor.moveToPosition(packedPositionChild);
            this.mCurrentArtistId = null;
            this.mCurrentAlbumId = Long.valueOf(expandableListContextMenuInfo.id).toString();
            this.mCurrentAlbumName = cursor.getString(cursor.getColumnIndexOrThrow("album"));
            this.mArtistCursor.moveToPosition(packedPositionGroup - getExpandableListView().getHeaderViewsCount());
            this.mCurrentArtistNameForAlbum = this.mArtistCursor.getString(this.mArtistCursor.getColumnIndexOrThrow("artist"));
            if (this.mCurrentArtistNameForAlbum != null && !this.mCurrentArtistNameForAlbum.equals("<unknown>")) {
                z = false;
            } else {
                z = true;
            }
            this.mIsUnknownArtist = z;
            if (this.mCurrentAlbumName != null && !this.mCurrentAlbumName.equals("<unknown>")) {
                z3 = false;
            }
            this.mIsUnknownAlbum = z3;
            if (this.mIsUnknownAlbum) {
                contextMenu.setHeaderTitle(getString(R.string.unknown_album_name));
            } else {
                contextMenu.setHeaderTitle(this.mCurrentAlbumName);
            }
            if (!this.mIsUnknownAlbum || !this.mIsUnknownArtist) {
                contextMenu.add(0, 16, 0, R.string.search_title);
            }
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        long[] songListForAlbum;
        long[] songListForAlbum2;
        long[] songListForAlbum3;
        long[] songListForAlbum4;
        int itemId = menuItem.getItemId();
        if (itemId == 10) {
            Bundle bundle = new Bundle();
            if (this.mCurrentArtistId != null) {
                songListForAlbum = MusicUtils.getSongListForArtist(this, Long.parseLong(this.mCurrentArtistId));
                bundle.putInt("delete_desc_string_id", R.string.delete_artist_desc);
                bundle.putString("delete_desc_track_info", this.mCurrentArtistName);
            } else {
                songListForAlbum = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                bundle.putInt("delete_desc_string_id", R.string.delete_album_desc);
                bundle.putString("delete_desc_track_info", this.mCurrentAlbumName);
            }
            bundle.putLongArray("items", songListForAlbum);
            Intent intent = new Intent();
            intent.setClass(this, DeleteItems.class);
            intent.putExtras(bundle);
            startActivityForResult(intent, -1);
            return true;
        }
        if (itemId == 12) {
            if (this.mCurrentArtistId != null) {
                songListForAlbum2 = MusicUtils.getSongListForArtist(this, Long.parseLong(this.mCurrentArtistId));
            } else {
                songListForAlbum2 = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
            }
            MusicUtils.addToCurrentPlaylist(this, songListForAlbum2);
            return true;
        }
        if (itemId != 16) {
            switch (itemId) {
                case 3:
                    if (this.mCurrentArtistId != null) {
                        songListForAlbum3 = MusicUtils.getSongListForArtist(this, Long.parseLong(this.mCurrentArtistId));
                    } else {
                        songListForAlbum3 = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                    }
                    MusicUtils.addToPlaylist(this, songListForAlbum3, menuItem.getIntent().getLongExtra("playlist", 0L));
                    return true;
                case 4:
                    if (this.mCurrentArtistId != null) {
                        showCreateDialog("selectedartist_" + this.mCurrentArtistId, 0, "new_playlist");
                    } else if (this.mCurrentAlbumId != null) {
                        showCreateDialog("selectedalbum_" + this.mCurrentAlbumId, 0, "new_playlist");
                    }
                    return true;
                case 5:
                    if (this.mCurrentArtistId != null) {
                        songListForAlbum4 = MusicUtils.getSongListForArtist(this, Long.parseLong(this.mCurrentArtistId));
                    } else {
                        songListForAlbum4 = MusicUtils.getSongListForAlbum(this, Long.parseLong(this.mCurrentAlbumId));
                    }
                    MusicUtils.playAll(this, songListForAlbum4, 0);
                    return true;
                default:
                    return super.onContextItemSelected(menuItem);
            }
        }
        doSearch();
        return true;
    }

    private void showCreateDialog(String str, int i, String str2) {
        MusicLogUtils.d("Artist/Album", "showEditDialog>>");
        removeOldFragmentByTag("Create");
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.d("Artist/Album", "<showDeleteDialog> fragmentManager = " + fragmentManager);
        CreatePlaylist.newInstance(str, i, str2, null).show(fragmentManager, "Create");
        fragmentManager.executePendingTransactions();
    }

    private void removeOldFragmentByTag(String str) {
        MusicLogUtils.d("Artist/Album", "<removeOldFragmentByTag> tag = " + str);
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.d("Artist/Album", "<removeOldFragmentByTag> fragmentManager = " + fragmentManager);
        DialogFragment dialogFragment = (DialogFragment) fragmentManager.findFragmentByTag(str);
        MusicLogUtils.d("Artist/Album", "<removeOldFragmentByTag> oldFragment = " + dialogFragment);
        if (dialogFragment != null) {
            dialogFragment.dismissAllowingStateLoss();
        }
    }

    void doSearch() {
        String str;
        String str2;
        if (BenesseExtension.getDchaState() != 0) {
            return;
        }
        Intent intent = new Intent();
        intent.setAction("android.intent.action.MEDIA_SEARCH");
        intent.setFlags(268435456);
        if (this.mCurrentArtistId != null) {
            str = this.mCurrentArtistName;
            str2 = this.mCurrentArtistName;
            intent.putExtra("android.intent.extra.artist", this.mCurrentArtistName);
            intent.putExtra("android.intent.extra.focus", "vnd.android.cursor.item/artist");
        } else {
            if (this.mIsUnknownAlbum) {
                str = this.mCurrentArtistNameForAlbum;
            } else {
                str = this.mCurrentAlbumName;
                if (!this.mIsUnknownArtist) {
                    str2 = str + " " + this.mCurrentArtistNameForAlbum;
                }
                intent.putExtra("android.intent.extra.artist", this.mCurrentArtistNameForAlbum);
                intent.putExtra("android.intent.extra.album", this.mCurrentAlbumName);
                intent.putExtra("android.intent.extra.focus", "vnd.android.cursor.item/album");
            }
            str2 = str;
            intent.putExtra("android.intent.extra.artist", this.mCurrentArtistNameForAlbum);
            intent.putExtra("android.intent.extra.album", this.mCurrentAlbumName);
            intent.putExtra("android.intent.extra.focus", "vnd.android.cursor.item/album");
        }
        String string = getString(R.string.mediasearch, new Object[]{str});
        intent.putExtra("query", str2);
        startActivity(Intent.createChooser(intent, string));
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        long[] songListForAlbum = null;
        if (i != 4) {
            if (i == 11) {
                if (i2 == 0) {
                    finish();
                    return;
                } else {
                    getArtistCursor(this.mAdapter.getQueryHandler(), null);
                    return;
                }
            }
            return;
        }
        if (i2 == -1) {
            Uri data = intent.getData();
            String stringExtra = intent.getStringExtra("add_to_playlist_item_id");
            if (data != null && stringExtra != null) {
                String strSubstring = stringExtra.substring(stringExtra.lastIndexOf("_") + 1);
                MusicLogUtils.d("Artist/Album", "onActivityResult: selectItemId = " + strSubstring);
                if (stringExtra.startsWith("selectedartist")) {
                    songListForAlbum = MusicUtils.getSongListForArtist(this, Long.parseLong(strSubstring));
                } else if (stringExtra.startsWith("selectedalbum")) {
                    songListForAlbum = MusicUtils.getSongListForAlbum(this, Long.parseLong(strSubstring));
                }
                MusicUtils.addToPlaylist(this, songListForAlbum, Long.parseLong(data.getLastPathSegment()));
            }
        }
    }

    private Cursor getArtistCursor(AsyncQueryHandler asyncQueryHandler, String str) {
        Uri uriBuild;
        Uri uri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI;
        if (!TextUtils.isEmpty(str)) {
            uriBuild = uri.buildUpon().appendQueryParameter("filter", Uri.encode(str)).build();
        } else {
            uriBuild = uri;
        }
        if (asyncQueryHandler == null) {
            return MusicUtils.query(this, uriBuild, this.mCursorCols, null, null, "artist_key");
        }
        PDebug.Start("ArtistAlbumBrowserActivity.startAsyncQuery");
        asyncQueryHandler.startQuery(0, null, uriBuild, this.mCursorCols, null, null, "artist_key");
        PDebug.End("ArtistAlbumBrowserActivity.startAsyncQuery");
        return null;
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
        private AsyncQueryHandler mQueryHandler;
        private final Resources mResources;
        private String mUnknownAlbum;
        private String mUnknownArtist;

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
                PDebug.Start("ArtistAlbumBrowserActivity.onQueryComplete");
                StringBuilder sb = new StringBuilder();
                sb.append("query complete: ");
                sb.append(cursor == null ? null : Integer.valueOf(cursor.getCount()));
                MusicLogUtils.d("Artist/Album", sb.toString());
                ArtistAlbumListAdapter.this.mActivity.init(cursor);
                PDebug.End("ArtistAlbumBrowserActivity.onQueryComplete");
            }
        }

        ArtistAlbumListAdapter(Context context, ArtistAlbumBrowserActivity artistAlbumBrowserActivity, Cursor cursor, int i, String[] strArr, int[] iArr, int i2, String[] strArr2, int[] iArr2) {
            super(context, cursor, i, strArr, iArr, i2, strArr2, iArr2);
            this.mBuffer = new StringBuilder();
            this.mFormatArgs = new Object[1];
            this.mFormatArgs3 = new Object[3];
            this.mConstraint = null;
            this.mConstraintIsValid = false;
            this.mActivity = artistAlbumBrowserActivity;
            this.mQueryHandler = new QueryHandler(context.getContentResolver());
            this.mDefaultAlbumIcon = (BitmapDrawable) context.getResources().getDrawable(R.drawable.albumart_mp_unknown_list);
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

        public void setActivity(ArtistAlbumBrowserActivity artistAlbumBrowserActivity) {
            this.mActivity = artistAlbumBrowserActivity;
        }

        public AsyncQueryHandler getQueryHandler() {
            return this.mQueryHandler;
        }

        @Override
        public View newGroupView(Context context, Cursor cursor, boolean z, ViewGroup viewGroup) {
            View viewNewGroupView = super.newGroupView(context, cursor, z, viewGroup);
            ViewGroup.LayoutParams layoutParams = ((ImageView) viewNewGroupView.findViewById(R.id.icon)).getLayoutParams();
            layoutParams.width = -2;
            layoutParams.height = -2;
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.mLine1 = (TextView) viewNewGroupView.findViewById(R.id.line1);
            viewHolder.mLine2 = (TextView) viewNewGroupView.findViewById(R.id.line2);
            viewHolder.mPlayIndicator = (ImageView) viewNewGroupView.findViewById(R.id.play_indicator);
            viewHolder.mIcon = (ImageView) viewNewGroupView.findViewById(R.id.icon);
            viewHolder.mIcon.setPadding(0, 0, 1, 0);
            viewNewGroupView.setTag(viewHolder);
            return viewNewGroupView;
        }

        @Override
        public View newChildView(Context context, Cursor cursor, boolean z, ViewGroup viewGroup) {
            View viewNewChildView = super.newChildView(context, cursor, z, viewGroup);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.mLine1 = (TextView) viewNewChildView.findViewById(R.id.line1);
            viewHolder.mLine2 = (TextView) viewNewChildView.findViewById(R.id.line2);
            viewHolder.mPlayIndicator = (ImageView) viewNewChildView.findViewById(R.id.play_indicator);
            viewHolder.mIcon = (ImageView) viewNewChildView.findViewById(R.id.icon);
            viewHolder.mIcon.setBackgroundDrawable(this.mDefaultAlbumIcon);
            viewHolder.mIcon.setPadding(0, 0, 1, 0);
            viewNewChildView.setTag(viewHolder);
            return viewNewChildView;
        }

        @Override
        public void bindGroupView(View view, Context context, Cursor cursor, boolean z) {
            boolean z2;
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            String string = cursor.getString(this.mGroupArtistIdx);
            if (string == null || string.equals("<unknown>")) {
                z2 = true;
            } else {
                z2 = false;
            }
            if (z2) {
                string = this.mUnknownArtist;
            }
            viewHolder.mLine1.setText(string);
            viewHolder.mLine2.setText(MusicUtils.makeAlbumsLabel(context, cursor.getInt(this.mGroupAlbumIdx), cursor.getInt(this.mGroupSongIdx), z2));
            if (MusicUtils.getCurrentArtistId() == cursor.getLong(this.mGroupArtistIdIdx) && !z) {
                viewHolder.mPlayIndicator.setVisibility(0);
            } else {
                viewHolder.mPlayIndicator.setVisibility(8);
            }
        }

        @Override
        public void bindChildView(View view, Context context, Cursor cursor, boolean z) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            String string = cursor.getString(cursor.getColumnIndexOrThrow("album"));
            boolean z2 = string == null || string.equals("<unknown>");
            if (z2) {
                string = this.mUnknownAlbum;
            }
            viewHolder.mLine1.setText(string);
            int i = cursor.getInt(cursor.getColumnIndexOrThrow("numsongs"));
            int i2 = cursor.getInt(cursor.getColumnIndexOrThrow("numsongs_by_artist"));
            StringBuilder sb = this.mBuffer;
            sb.delete(0, sb.length());
            if (z2) {
                i = i2;
            }
            if (i == 1) {
                sb.append(context.getString(R.string.onesong));
            } else if (i == i2) {
                Object[] objArr = this.mFormatArgs;
                objArr[0] = Integer.valueOf(i);
                sb.append(this.mResources.getQuantityString(R.plurals.Nsongs, i, objArr));
            } else {
                Object[] objArr2 = this.mFormatArgs3;
                objArr2[0] = Integer.valueOf(i);
                objArr2[1] = Integer.valueOf(i2);
                objArr2[2] = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                sb.append(this.mResources.getQuantityString(R.plurals.Nsongscomp, i, objArr2));
            }
            viewHolder.mLine2.setText(sb.toString());
            ImageView imageView = viewHolder.mIcon;
            String string2 = cursor.getString(cursor.getColumnIndexOrThrow("album_art"));
            if (z2 || string2 == null || string2.length() == 0) {
                imageView.setBackgroundDrawable(this.mDefaultAlbumIcon);
                imageView.setImageDrawable(null);
            } else {
                imageView.setImageDrawable(MusicUtils.getCachedArtwork(context, cursor.getLong(0), this.mDefaultAlbumIcon));
            }
            long currentAlbumId = MusicUtils.getCurrentAlbumId();
            long j = cursor.getLong(0);
            ImageView imageView2 = viewHolder.mPlayIndicator;
            if (currentAlbumId == j) {
                imageView2.setVisibility(0);
            } else {
                imageView2.setVisibility(8);
            }
        }

        @Override
        protected Cursor getChildrenCursor(Cursor cursor) {
            long j = cursor.getLong(cursor.getColumnIndexOrThrow("_id"));
            if (j < 0) {
                return null;
            }
            Cursor cursorQuery = MusicUtils.query(this.mActivity, MediaStore.Audio.Artists.Albums.getContentUri("external", j), new String[]{"_id", "album", "numsongs", "numsongs_by_artist", "album_art"}, null, null, "album_key");
            if (cursorQuery == null) {
                return null;
            }
            return new CursorWrapper(cursorQuery, cursor.getString(this.mGroupArtistIdx)) {
                String mArtistName;
                int mMagicColumnIdx;

                {
                    super(cursorQuery);
                    this.mArtistName = str;
                    if (this.mArtistName == null || this.mArtistName.equals("<unknown>")) {
                        this.mArtistName = ArtistAlbumListAdapter.this.mUnknownArtist;
                    }
                    this.mMagicColumnIdx = cursorQuery.getColumnCount();
                }

                @Override
                public String getString(int i) {
                    if (i != this.mMagicColumnIdx) {
                        return super.getString(i);
                    }
                    return this.mArtistName;
                }

                @Override
                public int getColumnIndexOrThrow(String str) {
                    if ("artist".equals(str)) {
                        return this.mMagicColumnIdx;
                    }
                    return super.getColumnIndexOrThrow(str);
                }

                @Override
                public String getColumnName(int i) {
                    if (i != this.mMagicColumnIdx) {
                        return super.getColumnName(i);
                    }
                    return "artist";
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
        public Cursor runQueryOnBackgroundThread(CharSequence charSequence) {
            String string = charSequence.toString();
            if (!this.mConstraintIsValid || ((string != null || this.mConstraint != null) && (string == null || !string.equals(this.mConstraint)))) {
                Cursor artistCursor = this.mActivity.getArtistCursor(null, string);
                this.mConstraint = string;
                this.mConstraintIsValid = true;
                return artistCursor;
            }
            return getCursor();
        }

        private int getGroupPositon(int i) {
            int groupCount = getGroupCount();
            ExpandableListView expandableListView = this.mActivity.getExpandableListView();
            if (expandableListView == null) {
                MusicLogUtils.d("Artist/Album", "getGroupPositon with ExpandableListView is null");
                return 0;
            }
            int childrenCount = 0;
            for (int i2 = 0; i2 < groupCount; i2++) {
                if (expandableListView.isGroupExpanded(i2)) {
                    childrenCount += getChildrenCount(i2);
                }
                if (i - childrenCount <= i2) {
                    return i2;
                }
            }
            return groupCount - 1;
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
                return this.mIndexer.getSectionForPosition(getGroupPositon(i));
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
    }
}
