package com.android.music;

import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.music.IMediaPlaybackService;
import com.android.music.MusicUtils;
import java.text.Collator;
import java.util.ArrayList;

public class PlaylistBrowserActivity extends ListActivity implements View.OnCreateContextMenuListener {
    private static boolean isMusicPermissionGrant = true;
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    private PlaylistListAdapter mAdapter;
    boolean mAdapterSent;
    private boolean mCreateShortcut;
    private int mOrientaiton;
    private Cursor mPlaylistCursor;
    private MenuItem mSearchItem;
    private SearchView mSearchView;
    private MusicUtils.ServiceToken mToken;
    private boolean mIsMounted = true;
    private boolean mWithtabs = false;
    private Toast mToast = null;
    private IMediaPlaybackService mService = null;
    private final DialogInterface.OnClickListener mDeleteDialogListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            MusicLogUtils.v("PlaylistBrowser", "<mDeleteDialogListener onClick>");
            dialogInterface.dismiss();
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            if (PlaylistBrowserActivity.this.mAdapter != null) {
                PlaylistBrowserActivity.this.getPlaylistCursor(PlaylistBrowserActivity.this.mAdapter.getQueryHandler(), null);
            }
        }
    };
    String[] mCols = {"_id", "name"};
    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            PlaylistBrowserActivity.this.getListView().invalidateViews();
            if (PlaylistBrowserActivity.this.mService != null) {
                MusicUtils.updateNowPlaying(PlaylistBrowserActivity.this, PlaylistBrowserActivity.this.mOrientaiton);
            }
        }
    };
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MusicLogUtils.v("PlaylistBrowser", "mScanListener.onReceive:" + action + ", status = " + Environment.getExternalStorageState());
            if ("android.intent.action.MEDIA_SCANNER_STARTED".equals(action) || "android.intent.action.MEDIA_SCANNER_FINISHED".equals(action)) {
                MusicUtils.setSpinnerState(PlaylistBrowserActivity.this);
                PlaylistBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                PlaylistBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
            } else {
                if ("android.intent.action.MEDIA_UNMOUNTED".equals(action)) {
                    PlaylistBrowserActivity.this.mIsMounted = false;
                    PlaylistBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                    PlaylistBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
                    PlaylistBrowserActivity.this.closeContextMenu();
                    PlaylistBrowserActivity.this.closeOptionsMenu();
                    PlaylistBrowserActivity.this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
                    return;
                }
                if ("android.intent.action.MEDIA_MOUNTED".equals(action)) {
                    PlaylistBrowserActivity.this.mIsMounted = true;
                    PlaylistBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                    PlaylistBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
                }
            }
        }
    };
    SearchView.OnQueryTextListener mQueryTextListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String str) {
            Intent intent = new Intent();
            intent.setClass(PlaylistBrowserActivity.this, QueryBrowserActivity.class);
            intent.putExtra("query", str);
            PlaylistBrowserActivity.this.startActivity(intent);
            return true;
        }

        @Override
        public boolean onQueryTextChange(String str) {
            return false;
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        MusicLogUtils.v("PlaylistBrowser", "onCreate");
        if (getApplicationContext().checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != 0) {
            MusicLogUtils.v("PlaylistBrowser", "onCreate Permissions not granted");
            isMusicPermissionGrant = false;
            Toast.makeText(this, R.string.music_storage_permission_deny, 0).show();
            return;
        }
        MusicLogUtils.v("PlaylistBrowser", "onCreate Permissions granted");
        isMusicPermissionGrant = true;
        final Intent intent = getIntent();
        final String action = intent.getAction();
        if ("android.intent.action.CREATE_SHORTCUT".equals(action)) {
            this.mCreateShortcut = true;
            MusicUtils.resetSdStatus();
        }
        this.mWithtabs = intent.getBooleanExtra("withtabs", false);
        setVolumeControlStream(3);
        if (this.mWithtabs) {
            requestWindowFeature(1);
            requestWindowFeature(5);
            setContentView(R.layout.media_picker_activity);
        } else {
            setContentView(R.layout.media_picker_activity_nowplaying);
            this.mOrientaiton = getResources().getConfiguration().orientation;
            this.mToken = MusicUtils.bindToService(this, new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                    PlaylistBrowserActivity.this.mService = IMediaPlaybackService.Stub.asInterface(iBinder);
                    if ("android.intent.action.VIEW".equals(action)) {
                        Bundle extras = intent.getExtras();
                        if (extras == null) {
                            MusicLogUtils.v("PlaylistBrowser", "Unexpected:getExtras() returns null.");
                        } else {
                            try {
                                long j = Long.parseLong(extras.getString("playlist"));
                                if (j == -1) {
                                    PlaylistBrowserActivity.this.playRecentlyAdded();
                                } else if (j == -3) {
                                    PlaylistBrowserActivity.this.playPodcasts();
                                } else if (j == -2) {
                                    long[] allSongs = MusicUtils.getAllSongs(PlaylistBrowserActivity.this);
                                    if (allSongs == null) {
                                        PlaylistBrowserActivity.this.showUSBInUsingMsg();
                                    } else {
                                        MusicUtils.playAll(PlaylistBrowserActivity.this, allSongs, 0);
                                    }
                                } else {
                                    MusicUtils.playPlaylist(PlaylistBrowserActivity.this, j);
                                }
                            } catch (NumberFormatException e) {
                                MusicLogUtils.v("PlaylistBrowser", "Playlist id missing or broken");
                            }
                        }
                        PlaylistBrowserActivity.this.finish();
                    }
                }

                @Override
                public void onServiceDisconnected(ComponentName componentName) {
                    PlaylistBrowserActivity.this.mService = null;
                    PlaylistBrowserActivity.this.finish();
                }
            });
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addDataScheme("file");
        registerReceiver(this.mScanListener, intentFilter);
        ListView listView = getListView();
        listView.setOnCreateContextMenuListener(this);
        listView.setTextFilterEnabled(true);
        this.mAdapter = (PlaylistListAdapter) getLastNonConfigurationInstance();
        if (this.mAdapter == null) {
            MusicLogUtils.v("PlaylistBrowser", "starting query");
            this.mAdapter = new PlaylistListAdapter(getApplication(), this, R.layout.track_list_item, this.mPlaylistCursor, new String[]{"name"}, new int[]{android.R.id.text1});
            setListAdapter(this.mAdapter);
            setTitle(R.string.working_playlists);
            getPlaylistCursor(this.mAdapter.getQueryHandler(), null);
        } else {
            this.mAdapter.setActivity(this);
            setListAdapter(this.mAdapter);
            this.mPlaylistCursor = this.mAdapter.getCursor();
            String strRetrieveRecentString = this.mAdapter.retrieveRecentString();
            if (this.mPlaylistCursor != null && strRetrieveRecentString != null && !strRetrieveRecentString.equals(getString(R.string.recentlyadded))) {
                MusicLogUtils.v("PlaylistBrowser", "old playlist cursor needs to be changed!");
                this.mPlaylistCursor.close();
                this.mPlaylistCursor = null;
            }
            if (this.mPlaylistCursor != null) {
                init(this.mPlaylistCursor);
            } else {
                setTitle(R.string.working_playlists);
                getPlaylistCursor(this.mAdapter.getQueryHandler(), null);
            }
        }
        this.mAdapter.storeRecentString();
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        PlaylistListAdapter playlistListAdapter = this.mAdapter;
        this.mAdapterSent = true;
        return playlistListAdapter;
    }

    @Override
    public void onDestroy() {
        MusicLogUtils.v("PlaylistBrowser", "onDestroy");
        if (isMusicPermissionGrant) {
            ListView listView = getListView();
            if (listView != null) {
                mLastListPosCourse = listView.getFirstVisiblePosition();
                View childAt = listView.getChildAt(0);
                if (childAt != null) {
                    mLastListPosFine = childAt.getTop();
                }
            }
            if (this.mToken != null) {
                MusicUtils.unbindFromService(this.mToken);
                this.mService = null;
            }
            if (!this.mAdapterSent && this.mAdapter != null) {
                this.mAdapter.changeCursor(null);
            }
            setListAdapter(null);
            this.mAdapter = null;
            unregisterReceiver(this.mScanListener);
        }
        super.onDestroy();
    }

    private void showRenameDialog(long j) {
        MusicLogUtils.v("PlaylistBrowser", "showEditDialog>>");
        removeOldFragmentByTag("Rename");
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.v("PlaylistBrowser", "<showRenameDialog> fragmentManager = " + fragmentManager);
        RenamePlaylist.newInstance(true).show(fragmentManager, "Rename");
        fragmentManager.executePendingTransactions();
    }

    private void showEditDialog() {
        MusicLogUtils.v("PlaylistBrowser", "showEditDialog>>");
        removeOldFragmentByTag("Edit");
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.v("PlaylistBrowser", "<showEditDialog> fragmentManager = " + fragmentManager);
        WeekSelector.newInstance(true).show(fragmentManager, "Edit");
        fragmentManager.executePendingTransactions();
    }

    private void removeOldFragmentByTag(String str) {
        MusicLogUtils.v("PlaylistBrowser", "<removeOldFragmentByTag> tag = " + str);
        FragmentManager fragmentManager = getFragmentManager();
        MusicLogUtils.v("PlaylistBrowser", "<removeOldFragmentByTag> fragmentManager = " + fragmentManager);
        DialogFragment dialogFragment = (DialogFragment) fragmentManager.findFragmentByTag(str);
        MusicLogUtils.v("PlaylistBrowser", "<removeOldFragmentByTag> oldFragment = " + dialogFragment);
        if (dialogFragment != null) {
            dialogFragment.dismissAllowingStateLoss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isMusicPermissionGrant) {
            MusicLogUtils.v("PlaylistBrowser", "onResume>>");
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction("com.android.music.metachanged");
            intentFilter.addAction("com.android.music.queuechanged");
            registerReceiver(this.mTrackListListener, intentFilter);
            this.mTrackListListener.onReceive(null, null);
            MusicUtils.setSpinnerState(this);
            MusicLogUtils.v("PlaylistBrowser", "onResume>>>");
            return;
        }
        finish();
    }

    @Override
    public void onPause() {
        MusicLogUtils.v("PlaylistBrowser", "onPause");
        if (isMusicPermissionGrant) {
            unregisterReceiver(this.mTrackListListener);
            this.mReScanHandler.removeCallbacksAndMessages(null);
        }
        super.onPause();
    }

    public void init(Cursor cursor) {
        if (this.mAdapter == null) {
            return;
        }
        this.mAdapter.changeCursor(cursor);
        if (this.mPlaylistCursor == null) {
            MusicUtils.displayDatabaseError(this, this.mIsMounted);
            closeContextMenu();
            this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
        } else {
            if (mLastListPosCourse >= 0) {
                getListView().setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
                mLastListPosCourse = -1;
            }
            MusicUtils.hideDatabaseError(this);
            setTitle();
        }
    }

    private void setTitle() {
        setTitle(R.string.playlists_title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (this.mWithtabs) {
            return super.onCreateOptionsMenu(menu);
        }
        if (!this.mCreateShortcut) {
            menu.add(0, 8, 0, R.string.party_shuffle);
            getMenuInflater().inflate(R.menu.music_search_menu, menu);
            this.mSearchItem = menu.findItem(R.id.search);
            this.mSearchView = (SearchView) this.mSearchItem.getActionView();
            this.mSearchView.setOnQueryTextListener(this.mQueryTextListener);
            this.mSearchView.setQueryHint(getString(R.string.search_hint));
            this.mSearchView.setIconifiedByDefault(true);
            SearchManager searchManager = (SearchManager) getSystemService("search");
            if (searchManager != null) {
                this.mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            }
        }
        menu.add(0, 13, 0, R.string.effects_list_title).setIcon(R.drawable.ic_menu_eq);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!isMusicPermissionGrant) {
            return true;
        }
        if (this.mWithtabs) {
            return super.onPrepareOptionsMenu(menu);
        }
        if (this.mPlaylistCursor == null) {
            this.mPlaylistCursor = this.mAdapter.getCursor();
            MusicLogUtils.v("PlaylistBrowser", "Playlist cursor is null, need not show option menu!");
        }
        MusicUtils.setEffectPanelMenu(getApplicationContext(), menu);
        MusicUtils.setPartyShuffleMenuIcon(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (this.mWithtabs) {
            return super.onOptionsItemSelected(menuItem);
        }
        int itemId = menuItem.getItemId();
        if (itemId == 8) {
            MusicUtils.togglePartyShuffle();
        } else {
            if (itemId == 13) {
                return MusicUtils.startEffectPanel(this);
            }
            if (itemId == R.id.search) {
                if (!this.mCreateShortcut) {
                    onSearchRequested();
                    return true;
                }
                return true;
            }
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        if (!isMusicPermissionGrant || this.mCreateShortcut) {
            return;
        }
        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) contextMenuInfo;
        contextMenu.add(0, 5, 0, R.string.play_selection);
        if (adapterContextMenuInfo.id >= 0) {
            contextMenu.add(0, 17, 0, R.string.delete_playlist_menu);
        }
        if (adapterContextMenuInfo.id == -1) {
            contextMenu.add(0, 18, 0, R.string.edit_playlist_menu);
        }
        if (adapterContextMenuInfo.id >= 0) {
            contextMenu.add(0, 19, 0, R.string.rename_playlist_menu);
        }
        this.mPlaylistCursor.moveToPosition(adapterContextMenuInfo.position);
        contextMenu.setHeaderTitle(this.mPlaylistCursor.getString(this.mPlaylistCursor.getColumnIndexOrThrow("name")));
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        int itemId = menuItem.getItemId();
        if (itemId == 5) {
            if (adapterContextMenuInfo.id == -1) {
                playRecentlyAdded();
                return true;
            }
            if (adapterContextMenuInfo.id == -3) {
                playPodcasts();
                return true;
            }
            MusicUtils.playPlaylist(this, adapterContextMenuInfo.id);
            return true;
        }
        switch (itemId) {
            case 17:
                getContentResolver().delete(ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, adapterContextMenuInfo.id), null, null);
                Toast.makeText(this, R.string.playlist_deleted_message, 0).show();
                if (this.mPlaylistCursor.getCount() == 0) {
                    setTitle(R.string.no_playlists_title);
                }
                break;
            case 18:
                if (adapterContextMenuInfo.id == -1) {
                    showEditDialog();
                } else {
                    MusicLogUtils.v("PlaylistBrowser", "should not be here");
                }
                break;
            case 19:
                MusicUtils.setLongPref(getApplicationContext(), "rename", adapterContextMenuInfo.id);
                showRenameDialog(adapterContextMenuInfo.id);
                break;
        }
        return true;
    }

    @Override
    public void onActivityResult(int i, int i2, Intent intent) {
        if (i == 11) {
            if (i2 == 0) {
                finish();
                return;
            } else {
                if (this.mAdapter != null) {
                    getPlaylistCursor(this.mAdapter.getQueryHandler(), null);
                    return;
                }
                return;
            }
        }
        new Bundle();
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        if (this.mCreateShortcut) {
            Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/playlist");
            intent.putExtra("playlist", String.valueOf(j));
            intent.setFlags(67108864);
            Intent intent2 = new Intent();
            intent2.putExtra("android.intent.extra.shortcut.INTENT", intent);
            intent2.putExtra("android.intent.extra.shortcut.NAME", ((TextView) view.findViewById(R.id.line1)).getText());
            intent2.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_shortcut_music_playlist));
            setResult(-1, intent2);
            finish();
            return;
        }
        if (j == -1) {
            Intent intent3 = new Intent("android.intent.action.PICK");
            intent3.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
            intent3.putExtra("playlist", "recentlyadded");
            startActivity(intent3);
            return;
        }
        if (j == -3) {
            Intent intent4 = new Intent("android.intent.action.PICK");
            intent4.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
            intent4.putExtra("playlist", "podcasts");
            startActivity(intent4);
            return;
        }
        Intent intent5 = new Intent("android.intent.action.EDIT");
        intent5.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent5.putExtra("playlist", Long.valueOf(j).toString());
        startActivity(intent5);
    }

    private void playRecentlyAdded() {
        Cursor cursorQuery = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "date_added>" + ((System.currentTimeMillis() / 1000) - ((long) (MusicUtils.getIntPref(this, "numweeks", 2) * 604800))), null, "title_key");
        if (cursorQuery == null) {
            showUSBInUsingMsg();
            return;
        }
        try {
            int count = cursorQuery.getCount();
            long[] jArr = new long[count];
            for (int i = 0; i < count; i++) {
                cursorQuery.moveToNext();
                jArr[i] = cursorQuery.getLong(0);
            }
            MusicUtils.playAll(this, jArr, 0);
        } catch (SQLiteException e) {
        } catch (Throwable th) {
            cursorQuery.close();
            throw th;
        }
        cursorQuery.close();
    }

    private void playPodcasts() {
        Cursor cursorQuery = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"_id"}, "is_podcast=1", null, "title_key");
        if (cursorQuery == null) {
            showUSBInUsingMsg();
            return;
        }
        try {
            int count = cursorQuery.getCount();
            long[] jArr = new long[count];
            for (int i = 0; i < count; i++) {
                cursorQuery.moveToNext();
                jArr[i] = cursorQuery.getLong(0);
            }
            MusicUtils.playAll(this, jArr, 0);
        } catch (SQLiteException e) {
        } catch (Throwable th) {
            cursorQuery.close();
            throw th;
        }
        cursorQuery.close();
    }

    private Cursor getPlaylistCursor(AsyncQueryHandler asyncQueryHandler, String str) {
        String[] strArr;
        StringBuilder sb = new StringBuilder();
        sb.append("name != ''");
        if (str == null) {
            strArr = null;
        } else {
            String[] strArrSplit = str.split(" ");
            String[] strArr2 = new String[strArrSplit.length];
            Collator.getInstance().setStrength(0);
            for (int i = 0; i < strArrSplit.length; i++) {
                strArr2[i] = '%' + strArrSplit[i] + '%';
            }
            for (int i2 = 0; i2 < strArrSplit.length; i2++) {
                sb.append(" AND ");
                sb.append("name LIKE ?");
            }
            strArr = strArr2;
        }
        String string = sb.toString();
        if (asyncQueryHandler != null) {
            asyncQueryHandler.startQuery(0, null, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, this.mCols, string, strArr, "name");
            return null;
        }
        return mergedCursor(MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, this.mCols, string, strArr, "name"));
    }

    private Cursor mergedCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        if (cursor instanceof MergeCursor) {
            MusicLogUtils.v("PlaylistBrowser", "Already wrapped");
            return cursor;
        }
        MatrixCursor matrixCursor = new MatrixCursor(this.mCols);
        if (this.mCreateShortcut) {
            ArrayList arrayList = new ArrayList(2);
            arrayList.add(-2L);
            arrayList.add(getString(R.string.play_all));
            matrixCursor.addRow(arrayList);
        }
        ArrayList arrayList2 = new ArrayList(2);
        arrayList2.add(-1L);
        arrayList2.add(getString(R.string.recentlyadded));
        matrixCursor.addRow(arrayList2);
        Cursor cursorQuery = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"count(*)"}, "is_podcast=1", null, null);
        if (cursorQuery != null) {
            cursorQuery.moveToFirst();
            int i = cursorQuery.getInt(0);
            cursorQuery.close();
            if (i > 0) {
                ArrayList arrayList3 = new ArrayList(2);
                arrayList3.add(-3L);
                arrayList3.add(getString(R.string.podcasts_listitem));
                matrixCursor.addRow(arrayList3);
            }
        }
        return new MergeCursor(new Cursor[]{matrixCursor, cursor});
    }

    static class PlaylistListAdapter extends SimpleCursorAdapter {
        private PlaylistBrowserActivity mActivity;
        private String mConstraint;
        private boolean mConstraintIsValid;
        int mIdIdx;
        private AsyncQueryHandler mQueryHandler;
        private String mRecentString;
        int mTitleIdx;

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver contentResolver) {
                super(contentResolver);
            }

            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                if (cursor != null) {
                    MusicLogUtils.v("PlaylistBrowser", "query complete: " + cursor.getCount());
                } else {
                    MusicLogUtils.v("PlaylistBrowser", "query complete: cursor is null");
                }
                boolean z = false;
                if (cursor != null) {
                    try {
                        cursor = PlaylistListAdapter.this.mActivity.mergedCursor(cursor);
                    } catch (SQLiteException e) {
                        MusicLogUtils.v("PlaylistBrowser", "---------Exception: " + e.toString());
                        z = true;
                    }
                }
                if (z) {
                    PlaylistListAdapter.this.mActivity.finish();
                } else {
                    PlaylistListAdapter.this.mActivity.init(cursor);
                }
            }
        }

        PlaylistListAdapter(Context context, PlaylistBrowserActivity playlistBrowserActivity, int i, Cursor cursor, String[] strArr, int[] iArr) {
            super(context, i, cursor, strArr, iArr);
            this.mActivity = null;
            this.mConstraint = null;
            this.mConstraintIsValid = false;
            this.mRecentString = null;
            this.mActivity = playlistBrowserActivity;
            getColumnIndices(cursor);
            this.mQueryHandler = new QueryHandler(context.getContentResolver());
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                this.mTitleIdx = cursor.getColumnIndexOrThrow("name");
                this.mIdIdx = cursor.getColumnIndexOrThrow("_id");
            }
        }

        public void setActivity(PlaylistBrowserActivity playlistBrowserActivity) {
            this.mActivity = playlistBrowserActivity;
        }

        public AsyncQueryHandler getQueryHandler() {
            return this.mQueryHandler;
        }

        public void storeRecentString() {
            if (this.mActivity != null) {
                this.mRecentString = this.mActivity.getString(R.string.recentlyadded);
            }
        }

        public String retrieveRecentString() {
            return this.mRecentString;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.line1)).setText(cursor.getString(this.mTitleIdx));
            long j = cursor.getLong(this.mIdIdx);
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            if (j == -1) {
                imageView.setImageResource(R.drawable.ic_mp_playlist_recently_added_list);
            } else {
                imageView.setImageResource(R.drawable.ic_mp_playlist_list);
            }
            ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
            layoutParams.width = -2;
            layoutParams.height = -2;
            ((ImageView) view.findViewById(R.id.play_indicator)).setVisibility(8);
            view.findViewById(R.id.line2).setVisibility(8);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if (this.mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != this.mActivity.mPlaylistCursor) {
                this.mActivity.mPlaylistCursor = cursor;
                super.changeCursor(cursor);
                getColumnIndices(cursor);
            }
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence charSequence) {
            String string = charSequence.toString();
            if (!this.mConstraintIsValid || ((string != null || this.mConstraint != null) && (string == null || !string.equals(this.mConstraint)))) {
                Cursor playlistCursor = this.mActivity.getPlaylistCursor(null, string);
                this.mConstraint = string;
                this.mConstraintIsValid = true;
                return playlistCursor;
            }
            return getCursor();
        }
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
        this.mOrientaiton = configuration.orientation;
        if (findViewById(R.id.sd_icon).getVisibility() == 0) {
            MusicLogUtils.v("PlaylistBrowser", "Configuration Changed at database error, return!");
        } else if (this.mService != null) {
            MusicUtils.updateNowPlaying(this, this.mOrientaiton);
        }
    }

    private void showUSBInUsingMsg() {
        if (this.mToast == null) {
            this.mToast = Toast.makeText(getApplicationContext(), getString(R.string.usb_in_using), 0);
        }
        this.mToast.show();
    }
}
