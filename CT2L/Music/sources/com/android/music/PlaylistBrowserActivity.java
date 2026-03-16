package com.android.music;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import com.android.music.MusicUtils;
import java.text.Collator;
import java.util.ArrayList;

public class PlaylistBrowserActivity extends ListActivity implements View.OnCreateContextMenuListener {
    private static int mLastListPosCourse = -1;
    private static int mLastListPosFine = -1;
    private PlaylistListAdapter mAdapter;
    boolean mAdapterSent;
    private boolean mCreateShortcut;
    private Cursor mPlaylistCursor;
    private MusicUtils.ServiceToken mToken;
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.setSpinnerState(PlaylistBrowserActivity.this);
            PlaylistBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (PlaylistBrowserActivity.this.mAdapter != null) {
                PlaylistBrowserActivity.this.getPlaylistCursor(PlaylistBrowserActivity.this.mAdapter.getQueryHandler(), null);
            }
        }
    };
    String[] mCols = {"_id", "name"};

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Intent intent = getIntent();
        final String action = intent.getAction();
        if ("android.intent.action.CREATE_SHORTCUT".equals(action)) {
            this.mCreateShortcut = true;
        }
        requestWindowFeature(5);
        requestWindowFeature(1);
        setVolumeControlStream(3);
        this.mToken = MusicUtils.bindToService(this, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName classname, IBinder obj) {
                if ("android.intent.action.VIEW".equals(action)) {
                    Bundle b = intent.getExtras();
                    if (b == null) {
                        Log.w("PlaylistBrowserActivity", "Unexpected:getExtras() returns null.");
                    } else {
                        try {
                            long id = Long.parseLong(b.getString("playlist"));
                            if (id == -1) {
                                PlaylistBrowserActivity.this.playRecentlyAdded();
                            } else if (id == -3) {
                                PlaylistBrowserActivity.this.playPodcasts();
                            } else if (id == -2) {
                                long[] list = MusicUtils.getAllSongs(PlaylistBrowserActivity.this);
                                if (list != null) {
                                    MusicUtils.playAll(PlaylistBrowserActivity.this, list, 0);
                                }
                            } else {
                                MusicUtils.playPlaylist(PlaylistBrowserActivity.this, id);
                            }
                        } catch (NumberFormatException e) {
                            Log.w("PlaylistBrowserActivity", "Playlist id missing or broken");
                        }
                    }
                    PlaylistBrowserActivity.this.finish();
                    return;
                }
                MusicUtils.updateNowPlaying(PlaylistBrowserActivity.this);
            }

            @Override
            public void onServiceDisconnected(ComponentName classname) {
            }
        });
        IntentFilter f = new IntentFilter();
        f.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        f.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        f.addAction("android.intent.action.MEDIA_UNMOUNTED");
        f.addDataScheme("file");
        registerReceiver(this.mScanListener, f);
        setContentView(R.layout.media_picker_activity);
        MusicUtils.updateButtonBar(this, R.id.playlisttab);
        ListView lv = getListView();
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);
        this.mAdapter = (PlaylistListAdapter) getLastNonConfigurationInstance();
        if (this.mAdapter == null) {
            this.mAdapter = new PlaylistListAdapter(getApplication(), this, R.layout.track_list_item, this.mPlaylistCursor, new String[]{"name"}, new int[]{android.R.id.text1});
            setListAdapter(this.mAdapter);
            setTitle(R.string.working_playlists);
            getPlaylistCursor(this.mAdapter.getQueryHandler(), null);
            return;
        }
        this.mAdapter.setActivity(this);
        setListAdapter(this.mAdapter);
        this.mPlaylistCursor = this.mAdapter.getCursor();
        if (this.mPlaylistCursor != null) {
            init(this.mPlaylistCursor);
        } else {
            setTitle(R.string.working_playlists);
            getPlaylistCursor(this.mAdapter.getQueryHandler(), null);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        PlaylistListAdapter a = this.mAdapter;
        this.mAdapterSent = true;
        return a;
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
        MusicUtils.setSpinnerState(this);
        MusicUtils.updateNowPlaying(this);
    }

    @Override
    public void onPause() {
        this.mReScanHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    public void init(Cursor cursor) {
        if (this.mAdapter != null) {
            this.mAdapter.changeCursor(cursor);
            if (this.mPlaylistCursor == null) {
                MusicUtils.displayDatabaseError(this);
                closeContextMenu();
                this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
            } else {
                if (mLastListPosCourse >= 0) {
                    getListView().setSelectionFromTop(mLastListPosCourse, mLastListPosFine);
                    mLastListPosCourse = -1;
                }
                MusicUtils.hideDatabaseError(this);
                MusicUtils.updateButtonBar(this, R.id.playlisttab);
                setTitle();
            }
        }
    }

    private void setTitle() {
        setTitle(R.string.playlists_title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!this.mCreateShortcut) {
            menu.add(0, 8, 0, R.string.party_shuffle);
        }
        return super.onCreateOptionsMenu(menu);
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenu.ContextMenuInfo menuInfoIn) {
        if (!this.mCreateShortcut) {
            AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo) menuInfoIn;
            menu.add(0, 5, 0, R.string.play_selection);
            if (mi.id >= 0) {
                menu.add(0, 15, 0, R.string.delete_playlist_menu);
            }
            if (mi.id == -1) {
                menu.add(0, 16, 0, R.string.edit_playlist_menu);
            }
            if (mi.id >= 0) {
                menu.add(0, 17, 0, R.string.rename_playlist_menu);
            }
            this.mPlaylistCursor.moveToPosition(mi.position);
            menu.setHeaderTitle(this.mPlaylistCursor.getString(this.mPlaylistCursor.getColumnIndexOrThrow("name")));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo mi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case 5:
                if (mi.id == -1) {
                    playRecentlyAdded();
                } else if (mi.id == -3) {
                    playPodcasts();
                } else {
                    MusicUtils.playPlaylist(this, mi.id);
                }
                return true;
            case 15:
                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mi.id);
                getContentResolver().delete(uri, null, null);
                Toast.makeText(this, R.string.playlist_deleted_message, 0).show();
                if (this.mPlaylistCursor.getCount() == 0) {
                    setTitle(R.string.no_playlists_title);
                }
                return true;
            case 16:
                if (mi.id == -1) {
                    Intent intent = new Intent();
                    intent.setClass(this, WeekSelector.class);
                    startActivityForResult(intent, 18);
                } else {
                    Log.e("PlaylistBrowserActivity", "should not be here");
                }
                return true;
            case 17:
                Intent intent2 = new Intent();
                intent2.setClass(this, RenamePlaylist.class);
                intent2.putExtra("rename", mi.id);
                startActivityForResult(intent2, 17);
                return true;
            default:
                return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case 11:
                if (resultCode == 0) {
                    finish();
                } else if (this.mAdapter != null) {
                    getPlaylistCursor(this.mAdapter.getQueryHandler(), null);
                }
                break;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (this.mCreateShortcut) {
            Intent shortcut = new Intent();
            shortcut.setAction("android.intent.action.VIEW");
            shortcut.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/playlist");
            shortcut.putExtra("playlist", String.valueOf(id));
            Intent intent = new Intent();
            intent.putExtra("android.intent.extra.shortcut.INTENT", shortcut);
            intent.putExtra("android.intent.extra.shortcut.NAME", ((TextView) v.findViewById(R.id.line1)).getText());
            intent.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_launcher_shortcut_music_playlist));
            setResult(-1, intent);
            finish();
            return;
        }
        if (id == -1) {
            Intent intent2 = new Intent("android.intent.action.PICK");
            intent2.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
            intent2.putExtra("playlist", "recentlyadded");
            startActivity(intent2);
            return;
        }
        if (id == -3) {
            Intent intent3 = new Intent("android.intent.action.PICK");
            intent3.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
            intent3.putExtra("playlist", "podcasts");
            startActivity(intent3);
            return;
        }
        Intent intent4 = new Intent("android.intent.action.EDIT");
        intent4.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent4.putExtra("playlist", Long.valueOf(id).toString());
        startActivity(intent4);
    }

    private void playRecentlyAdded() {
        int X = MusicUtils.getIntPref(this, "numweeks", 2) * 604800;
        String[] ccols = {"_id"};
        String where = "date_added>" + ((System.currentTimeMillis() / 1000) - ((long) X));
        Cursor cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, where, null, "title_key");
        if (cursor != null) {
            try {
                int len = cursor.getCount();
                long[] list = new long[len];
                for (int i = 0; i < len; i++) {
                    cursor.moveToNext();
                    list[i] = cursor.getLong(0);
                }
                MusicUtils.playAll(this, list, 0);
            } catch (SQLiteException e) {
            } finally {
                cursor.close();
            }
        }
    }

    private void playPodcasts() {
        String[] ccols = {"_id"};
        Cursor cursor = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ccols, "is_podcast=1", null, "title_key");
        if (cursor != null) {
            try {
                int len = cursor.getCount();
                long[] list = new long[len];
                for (int i = 0; i < len; i++) {
                    cursor.moveToNext();
                    list[i] = cursor.getLong(0);
                }
                MusicUtils.playAll(this, list, 0);
            } catch (SQLiteException e) {
            } finally {
                cursor.close();
            }
        }
    }

    private Cursor getPlaylistCursor(AsyncQueryHandler async, String filterstring) {
        StringBuilder where = new StringBuilder();
        where.append("name != ''");
        String[] keywords = null;
        if (filterstring != null) {
            String[] searchWords = filterstring.split(" ");
            keywords = new String[searchWords.length];
            Collator col = Collator.getInstance();
            col.setStrength(0);
            for (int i = 0; i < searchWords.length; i++) {
                keywords[i] = '%' + searchWords[i] + '%';
            }
            for (int i2 = 0; i2 < searchWords.length; i2++) {
                where.append(" AND ");
                where.append("name LIKE ?");
            }
        }
        String whereclause = where.toString();
        if (async != null) {
            async.startQuery(0, null, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, this.mCols, whereclause, keywords, "name");
            return null;
        }
        Cursor c = MusicUtils.query(this, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, this.mCols, whereclause, keywords, "name");
        return mergedCursor(c);
    }

    private Cursor mergedCursor(Cursor c) {
        if (c == null) {
            return null;
        }
        if (c instanceof MergeCursor) {
            Log.d("PlaylistBrowserActivity", "Already wrapped");
            return c;
        }
        MatrixCursor autoplaylistscursor = new MatrixCursor(this.mCols);
        if (this.mCreateShortcut) {
            ArrayList<Object> all = new ArrayList<>(2);
            all.add(-2L);
            all.add(getString(R.string.play_all));
            autoplaylistscursor.addRow(all);
        }
        ArrayList<Object> recent = new ArrayList<>(2);
        recent.add(-1L);
        recent.add(getString(R.string.recentlyadded));
        autoplaylistscursor.addRow(recent);
        Cursor counter = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, new String[]{"count(*)"}, "is_podcast=1", null, null);
        if (counter != null) {
            counter.moveToFirst();
            int numpodcasts = counter.getInt(0);
            counter.close();
            if (numpodcasts > 0) {
                ArrayList<Object> podcasts = new ArrayList<>(2);
                podcasts.add(-3L);
                podcasts.add(getString(R.string.podcasts_listitem));
                autoplaylistscursor.addRow(podcasts);
            }
        }
        Cursor cc = new MergeCursor(new Cursor[]{autoplaylistscursor, c});
        return cc;
    }

    static class PlaylistListAdapter extends SimpleCursorAdapter {
        private PlaylistBrowserActivity mActivity;
        private String mConstraint;
        private boolean mConstraintIsValid;
        int mIdIdx;
        private AsyncQueryHandler mQueryHandler;
        int mTitleIdx;

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                if (cursor != null) {
                    cursor = PlaylistListAdapter.this.mActivity.mergedCursor(cursor);
                }
                PlaylistListAdapter.this.mActivity.init(cursor);
            }
        }

        PlaylistListAdapter(Context context, PlaylistBrowserActivity currentactivity, int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);
            this.mActivity = null;
            this.mConstraint = null;
            this.mConstraintIsValid = false;
            this.mActivity = currentactivity;
            getColumnIndices(cursor);
            this.mQueryHandler = new QueryHandler(context.getContentResolver());
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                this.mTitleIdx = cursor.getColumnIndexOrThrow("name");
                this.mIdIdx = cursor.getColumnIndexOrThrow("_id");
            }
        }

        public void setActivity(PlaylistBrowserActivity newactivity) {
            this.mActivity = newactivity;
        }

        public AsyncQueryHandler getQueryHandler() {
            return this.mQueryHandler;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv = (TextView) view.findViewById(R.id.line1);
            String name = cursor.getString(this.mTitleIdx);
            tv.setText(name);
            long id = cursor.getLong(this.mIdIdx);
            ImageView iv = (ImageView) view.findViewById(R.id.icon);
            if (id == -1) {
                iv.setImageResource(R.drawable.ic_mp_playlist_recently_added_list);
            } else {
                iv.setImageResource(R.drawable.ic_mp_playlist_list);
            }
            ViewGroup.LayoutParams p = iv.getLayoutParams();
            p.width = -2;
            p.height = -2;
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
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (!this.mConstraintIsValid || ((s != null || this.mConstraint != null) && (s == null || !s.equals(this.mConstraint)))) {
                Cursor playlistCursor = this.mActivity.getPlaylistCursor(null, s);
                this.mConstraint = s;
                this.mConstraintIsValid = true;
                return playlistCursor;
            }
            return getCursor();
        }
    }
}
