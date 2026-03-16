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
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.android.music.MusicUtils;

public class QueryBrowserActivity extends ListActivity implements ServiceConnection {
    private QueryListAdapter mAdapter;
    private boolean mAdapterSent;
    private Cursor mQueryCursor;
    private MusicUtils.ServiceToken mToken;
    private ListView mTrackList;
    private String mFilterString = "";
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MusicUtils.setSpinnerState(QueryBrowserActivity.this);
            QueryBrowserActivity.this.mReScanHandler.sendEmptyMessage(0);
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (QueryBrowserActivity.this.mAdapter != null) {
                QueryBrowserActivity.this.getQueryCursor(QueryBrowserActivity.this.mAdapter.getQueryHandler(), null);
            }
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(3);
        this.mAdapter = (QueryListAdapter) getLastNonConfigurationInstance();
        this.mToken = MusicUtils.bindToService(this, this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        IntentFilter f = new IntentFilter();
        f.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        f.addAction("android.intent.action.MEDIA_UNMOUNTED");
        f.addDataScheme("file");
        registerReceiver(this.mScanListener, f);
        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;
        if ("android.intent.action.VIEW".equals(action)) {
            Uri uri = intent.getData();
            String path = uri.toString();
            if (path.startsWith("content://media/external/audio/media/")) {
                String id = uri.getLastPathSegment();
                long[] list = {Long.valueOf(id).longValue()};
                MusicUtils.playAll(this, list, 0);
                finish();
                return;
            }
            if (path.startsWith("content://media/external/audio/albums/")) {
                Intent i = new Intent("android.intent.action.PICK");
                i.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
                i.putExtra("album", uri.getLastPathSegment());
                startActivity(i);
                finish();
                return;
            }
            if (path.startsWith("content://media/external/audio/artists/")) {
                Intent i2 = new Intent("android.intent.action.PICK");
                i2.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
                i2.putExtra("artist", uri.getLastPathSegment());
                startActivity(i2);
                finish();
                return;
            }
        }
        this.mFilterString = intent.getStringExtra("query");
        if ("android.intent.action.MEDIA_SEARCH".equals(action)) {
            String focus = intent.getStringExtra("android.intent.extra.focus");
            String artist = intent.getStringExtra("android.intent.extra.artist");
            String album = intent.getStringExtra("android.intent.extra.album");
            String title = intent.getStringExtra("android.intent.extra.title");
            if (focus != null) {
                if (focus.startsWith("audio/") && title != null) {
                    this.mFilterString = title;
                } else if (focus.equals("vnd.android.cursor.item/album")) {
                    if (album != null) {
                        this.mFilterString = album;
                        if (artist != null) {
                            this.mFilterString += " " + artist;
                        }
                    }
                } else if (focus.equals("vnd.android.cursor.item/artist") && artist != null) {
                    this.mFilterString = artist;
                }
            }
        }
        setContentView(R.layout.query_activity);
        this.mTrackList = getListView();
        this.mTrackList.setTextFilterEnabled(true);
        if (this.mAdapter == null) {
            this.mAdapter = new QueryListAdapter(getApplication(), this, R.layout.track_list_item, null, new String[0], new int[0]);
            setListAdapter(this.mAdapter);
            if (TextUtils.isEmpty(this.mFilterString)) {
                getQueryCursor(this.mAdapter.getQueryHandler(), null);
                return;
            } else {
                this.mTrackList.setFilterText(this.mFilterString);
                this.mFilterString = null;
                return;
            }
        }
        this.mAdapter.setActivity(this);
        setListAdapter(this.mAdapter);
        this.mQueryCursor = this.mAdapter.getCursor();
        if (this.mQueryCursor != null) {
            init(this.mQueryCursor);
        } else {
            getQueryCursor(this.mAdapter.getQueryHandler(), this.mFilterString);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        this.mAdapterSent = true;
        return this.mAdapter;
    }

    @Override
    public void onPause() {
        this.mReScanHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onDestroy() {
        MusicUtils.unbindFromService(this.mToken);
        unregisterReceiver(this.mScanListener);
        if (!this.mAdapterSent && this.mAdapter != null) {
            this.mAdapter.changeCursor(null);
        }
        if (getListView() != null) {
            setListAdapter(null);
        }
        this.mAdapter = null;
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case 11:
                if (resultCode == 0) {
                    finish();
                } else {
                    getQueryCursor(this.mAdapter.getQueryHandler(), null);
                }
                break;
        }
    }

    public void init(Cursor c) {
        if (this.mAdapter != null) {
            this.mAdapter.changeCursor(c);
            if (this.mQueryCursor == null) {
                MusicUtils.displayDatabaseError(this);
                setListAdapter(null);
                this.mReScanHandler.sendEmptyMessageDelayed(0, 1000L);
                return;
            }
            MusicUtils.hideDatabaseError(this);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        this.mQueryCursor.moveToPosition(position);
        if (!this.mQueryCursor.isBeforeFirst() && !this.mQueryCursor.isAfterLast()) {
            String selectedType = this.mQueryCursor.getString(this.mQueryCursor.getColumnIndexOrThrow("mime_type"));
            if ("artist".equals(selectedType)) {
                Intent intent = new Intent("android.intent.action.PICK");
                intent.addFlags(67108864);
                intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
                intent.putExtra("artist", Long.valueOf(id).toString());
                startActivity(intent);
                return;
            }
            if ("album".equals(selectedType)) {
                Intent intent2 = new Intent("android.intent.action.PICK");
                intent2.addFlags(67108864);
                intent2.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
                intent2.putExtra("album", Long.valueOf(id).toString());
                startActivity(intent2);
                return;
            }
            if (position >= 0 && id >= 0) {
                long[] list = {id};
                MusicUtils.playAll(this, list, 0);
            } else {
                Log.e("QueryBrowser", "invalid position/id: " + position + "/" + id);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 2:
                MusicUtils.setRingtone(this, this.mTrackList.getSelectedItemId());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private Cursor getQueryCursor(AsyncQueryHandler async, String filter) {
        if (filter == null) {
            filter = "";
        }
        String[] ccols = {"_id", "mime_type", "artist", "album", "title", "data1", "data2"};
        Uri search = Uri.parse("content://media/external/audio/search/fancy/" + Uri.encode(filter));
        if (async != null) {
            async.startQuery(0, null, search, ccols, null, null, null);
            return null;
        }
        Cursor ret = MusicUtils.query(this, search, ccols, null, null, null);
        return ret;
    }

    static class QueryListAdapter extends SimpleCursorAdapter {
        private QueryBrowserActivity mActivity;
        private String mConstraint;
        private boolean mConstraintIsValid;
        private AsyncQueryHandler mQueryHandler;

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }

            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                QueryListAdapter.this.mActivity.init(cursor);
            }
        }

        QueryListAdapter(Context context, QueryBrowserActivity currentactivity, int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);
            this.mActivity = null;
            this.mConstraint = null;
            this.mConstraintIsValid = false;
            this.mActivity = currentactivity;
            this.mQueryHandler = new QueryHandler(context.getContentResolver());
        }

        public void setActivity(QueryBrowserActivity newactivity) {
            this.mActivity = newactivity;
        }

        public AsyncQueryHandler getQueryHandler() {
            return this.mQueryHandler;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv1 = (TextView) view.findViewById(R.id.line1);
            TextView tv2 = (TextView) view.findViewById(R.id.line2);
            ImageView iv = (ImageView) view.findViewById(R.id.icon);
            ViewGroup.LayoutParams p = iv.getLayoutParams();
            if (p == null) {
                DatabaseUtils.dumpCursor(cursor);
                return;
            }
            p.width = -2;
            p.height = -2;
            String mimetype = cursor.getString(cursor.getColumnIndexOrThrow("mime_type"));
            if (mimetype == null) {
                mimetype = "audio/";
            }
            if (mimetype.equals("artist")) {
                iv.setImageResource(R.drawable.ic_mp_artist_list);
                String name = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                String displayname = name;
                boolean isunknown = false;
                if (name == null || name.equals("<unknown>")) {
                    displayname = context.getString(R.string.unknown_artist_name);
                    isunknown = true;
                }
                tv1.setText(displayname);
                int numalbums = cursor.getInt(cursor.getColumnIndexOrThrow("data1"));
                int numsongs = cursor.getInt(cursor.getColumnIndexOrThrow("data2"));
                String songs_albums = MusicUtils.makeAlbumsSongsLabel(context, numalbums, numsongs, isunknown);
                tv2.setText(songs_albums);
                return;
            }
            if (mimetype.equals("album")) {
                iv.setImageResource(R.drawable.albumart_mp_unknown_list);
                String name2 = cursor.getString(cursor.getColumnIndexOrThrow("album"));
                String displayname2 = name2;
                if (name2 == null || name2.equals("<unknown>")) {
                    displayname2 = context.getString(R.string.unknown_album_name);
                }
                tv1.setText(displayname2);
                String name3 = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                String displayname3 = name3;
                if (name3 == null || name3.equals("<unknown>")) {
                    displayname3 = context.getString(R.string.unknown_artist_name);
                }
                tv2.setText(displayname3);
                return;
            }
            if (mimetype.startsWith("audio/") || mimetype.equals("application/ogg") || mimetype.equals("application/x-ogg")) {
                iv.setImageResource(R.drawable.ic_mp_song_list);
                tv1.setText(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                String displayname4 = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                if (displayname4 == null || displayname4.equals("<unknown>")) {
                    displayname4 = context.getString(R.string.unknown_artist_name);
                }
                String name4 = cursor.getString(cursor.getColumnIndexOrThrow("album"));
                if (name4 == null || name4.equals("<unknown>")) {
                    name4 = context.getString(R.string.unknown_album_name);
                }
                tv2.setText(displayname4 + " - " + name4);
            }
        }

        @Override
        public void changeCursor(Cursor cursor) {
            if (this.mActivity.isFinishing() && cursor != null) {
                cursor.close();
                cursor = null;
            }
            if (cursor != this.mActivity.mQueryCursor) {
                this.mActivity.mQueryCursor = cursor;
                super.changeCursor(cursor);
            }
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            String s = constraint.toString();
            if (!this.mConstraintIsValid || ((s != null || this.mConstraint != null) && (s == null || !s.equals(this.mConstraint)))) {
                Cursor queryCursor = this.mActivity.getQueryCursor(null, s);
                this.mConstraint = s;
                this.mConstraintIsValid = true;
                return queryCursor;
            }
            return getCursor();
        }
    }
}
