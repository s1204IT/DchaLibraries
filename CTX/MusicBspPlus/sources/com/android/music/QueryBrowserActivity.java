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
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
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
    private boolean mResetSdStatus = false;
    private boolean mIsMounted = true;
    private TextView mEmptyView = null;
    private final String[] mCursorCols = {"_id", "mime_type", "artist", "album", "title", "data1", "data2"};
    private BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MusicLogUtils.v("QueryBrowser", "mScanListener.onReceive:" + action + ", status = " + Environment.getExternalStorageState());
            if ("android.intent.action.MEDIA_SCANNER_STARTED".equals(action)) {
                MusicUtils.setSpinnerState(QueryBrowserActivity.this);
                QueryBrowserActivity.this.mResetSdStatus = true;
                QueryBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                QueryBrowserActivity.this.mReScanHandler.sendMessage(QueryBrowserActivity.this.mReScanHandler.obtainMessage(9));
                QueryBrowserActivity.this.mReScanHandler.sendMessageDelayed(QueryBrowserActivity.this.mReScanHandler.obtainMessage(9), 1000L);
                return;
            }
            if ("android.intent.action.MEDIA_UNMOUNTED".equals(action)) {
                QueryBrowserActivity.this.mIsMounted = false;
                QueryBrowserActivity.this.mResetSdStatus = true;
                QueryBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                QueryBrowserActivity.this.mReScanHandler.sendMessage(QueryBrowserActivity.this.mReScanHandler.obtainMessage(9));
                QueryBrowserActivity.this.closeContextMenu();
                QueryBrowserActivity.this.closeOptionsMenu();
                QueryBrowserActivity.this.mReScanHandler.sendMessageDelayed(QueryBrowserActivity.this.mReScanHandler.obtainMessage(9), 1000L);
                return;
            }
            if ("android.intent.action.MEDIA_MOUNTED".equals(action)) {
                QueryBrowserActivity.this.mIsMounted = true;
                QueryBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                QueryBrowserActivity.this.mReScanHandler.sendMessage(QueryBrowserActivity.this.mReScanHandler.obtainMessage(9));
            } else if ("android.intent.action.MEDIA_SCANNER_FINISHED".equals(action)) {
                QueryBrowserActivity.this.mReScanHandler.removeCallbacksAndMessages(null);
                QueryBrowserActivity.this.mReScanHandler.sendMessage(QueryBrowserActivity.this.mReScanHandler.obtainMessage(9));
            }
        }
    };
    private Handler mReScanHandler = new Handler() {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 7:
                    QueryBrowserActivity.this.mTrackList.setVisibility(0);
                    QueryBrowserActivity.this.mEmptyView.setVisibility(8);
                    break;
                case 8:
                    QueryBrowserActivity.this.mTrackList.setVisibility(8);
                    QueryBrowserActivity.this.mEmptyView.setVisibility(0);
                    break;
                case 9:
                    if (QueryBrowserActivity.this.mAdapter != null) {
                        QueryBrowserActivity.this.getQueryCursor(QueryBrowserActivity.this.mAdapter.getQueryHandler(), QueryBrowserActivity.this.mFilterString);
                    }
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setVolumeControlStream(3);
        this.mAdapter = (QueryListAdapter) getLastNonConfigurationInstance();
        this.mToken = MusicUtils.bindToService(this, this);
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        intentFilter.addDataScheme("file");
        registerReceiver(this.mScanListener, intentFilter);
        Intent intent = getIntent();
        String action = intent != null ? intent.getAction() : null;
        if ("android.intent.action.VIEW".equals(action)) {
            Uri data = intent.getData();
            String string = data.toString();
            if (string.startsWith("content://media/external/audio/media/")) {
                MusicUtils.playAll(this, new long[]{Long.valueOf(data.getLastPathSegment()).longValue()}, 0);
                finish();
                return;
            }
            if (string.startsWith("content://media/external/audio/albums/")) {
                Intent intent2 = new Intent("android.intent.action.PICK");
                intent2.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
                intent2.putExtra("album", data.getLastPathSegment());
                startActivity(intent2);
                finish();
                return;
            }
            if (string.startsWith("content://media/external/audio/artists/")) {
                Intent intent3 = new Intent("android.intent.action.PICK");
                intent3.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
                intent3.putExtra("artist", data.getLastPathSegment());
                startActivity(intent3);
                finish();
                return;
            }
        }
        this.mFilterString = intent.getStringExtra("query");
        if ("android.intent.action.MEDIA_SEARCH".equals(action)) {
            String stringExtra = intent.getStringExtra("android.intent.extra.focus");
            String stringExtra2 = intent.getStringExtra("android.intent.extra.artist");
            String stringExtra3 = intent.getStringExtra("android.intent.extra.album");
            String stringExtra4 = intent.getStringExtra("android.intent.extra.title");
            if (stringExtra != null) {
                if (stringExtra.startsWith("audio/") && stringExtra4 != null) {
                    this.mFilterString = stringExtra4;
                } else if (stringExtra.equals("vnd.android.cursor.item/album")) {
                    if (stringExtra3 != null) {
                        this.mFilterString = stringExtra3;
                        if (stringExtra2 != null) {
                            this.mFilterString += " " + stringExtra2;
                        }
                    }
                } else if (stringExtra.equals("vnd.android.cursor.item/artist") && stringExtra2 != null) {
                    this.mFilterString = stringExtra2;
                }
            }
        }
        setContentView(R.layout.query_activity);
        this.mTrackList = getListView();
        this.mTrackList.setTextFilterEnabled(true);
        this.mEmptyView = (TextView) findViewById(R.id.empty_show);
        if (this.mAdapter == null) {
            this.mAdapter = new QueryListAdapter(getApplication(), this, R.layout.track_list_item, null, new String[0], new int[0]);
            setListAdapter(this.mAdapter);
            if (TextUtils.isEmpty(this.mFilterString)) {
                getQueryCursor(this.mAdapter.getQueryHandler(), null);
                return;
            } else {
                this.mTrackList.setFilterText(this.mFilterString);
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
    public void onServiceDisconnected(ComponentName componentName) {
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        this.mAdapterSent = true;
        return this.mAdapter;
    }

    @Override
    public void onPause() {
        MusicLogUtils.v("QueryBrowser", "onPause()");
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
    protected void onActivityResult(int i, int i2, Intent intent) {
        if (i == 11) {
            if (i2 == 0) {
                finish();
            } else {
                getQueryCursor(this.mAdapter.getQueryHandler(), null);
            }
        }
    }

    public void init(Cursor cursor) {
        if (this.mAdapter == null) {
            return;
        }
        this.mAdapter.changeCursor(cursor);
        if (this.mQueryCursor == null) {
            if (this.mResetSdStatus) {
                MusicUtils.resetSdStatus();
                this.mResetSdStatus = false;
            }
            if (this.mEmptyView.getVisibility() == 0) {
                this.mEmptyView.setVisibility(8);
            }
            MusicUtils.displayDatabaseError(this, this.mIsMounted);
            setListAdapter(null);
            this.mReScanHandler.sendMessageDelayed(this.mReScanHandler.obtainMessage(9), 1000L);
            return;
        }
        MusicUtils.hideDatabaseError(this);
        MusicLogUtils.v("QueryBrowser", "c.getCount() " + cursor.getCount() + "getListView().getCount()" + getListView().getCount());
        this.mReScanHandler.sendMessage(this.mReScanHandler.obtainMessage(cursor.getCount() != 0 ? 7 : 8));
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        this.mQueryCursor.moveToPosition(i);
        if (this.mQueryCursor.isBeforeFirst() || this.mQueryCursor.isAfterLast()) {
            return;
        }
        String string = this.mQueryCursor.getString(this.mQueryCursor.getColumnIndexOrThrow("mime_type"));
        if ("artist".equals(string)) {
            Intent intent = new Intent("android.intent.action.PICK");
            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
            intent.putExtra("artist", Long.valueOf(j).toString());
            startActivity(intent);
            return;
        }
        if ("album".equals(string)) {
            Intent intent2 = new Intent("android.intent.action.PICK");
            intent2.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
            intent2.putExtra("album", Long.valueOf(j).toString());
            startActivity(intent2);
            return;
        }
        if (i >= 0 && j >= 0) {
            MusicUtils.playAll(this, new long[]{j}, 0);
            return;
        }
        MusicLogUtils.v("QueryBrowser", "invalid position/id: " + i + "/" + j);
    }

    private Cursor getQueryCursor(AsyncQueryHandler asyncQueryHandler, String str) {
        if (str == null) {
            str = "";
        }
        Uri uri = Uri.parse("content://media/external/audio/search/fancy/" + Uri.encode(str));
        Cursor cursorQuery = null;
        if (asyncQueryHandler != null) {
            asyncQueryHandler.startQuery(0, null, uri, this.mCursorCols, null, null, null);
        } else {
            cursorQuery = MusicUtils.query(this, uri, this.mCursorCols, null, null, null);
        }
        if (cursorQuery != null) {
            MusicLogUtils.v("QueryBrowser", " ret != null  getQueryCursor: Count=" + cursorQuery.getCount() + " search=" + str);
            this.mReScanHandler.sendMessage(this.mReScanHandler.obtainMessage(cursorQuery.getCount() == 0 ? 8 : 7));
        }
        return cursorQuery;
    }

    static class QueryListAdapter extends SimpleCursorAdapter {
        private QueryBrowserActivity mActivity;
        private String mConstraint;
        private boolean mConstraintIsValid;
        private AsyncQueryHandler mQueryHandler;

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver contentResolver) {
                super(contentResolver);
            }

            @Override
            protected void onQueryComplete(int i, Object obj, Cursor cursor) {
                if (cursor != null) {
                    MusicLogUtils.v("QueryBrowser", "onQueryComplete: Conut=" + cursor.getCount());
                }
                if (cursor != null) {
                    QueryListAdapter.this.mActivity.init(cursor);
                }
            }
        }

        QueryListAdapter(Context context, QueryBrowserActivity queryBrowserActivity, int i, Cursor cursor, String[] strArr, int[] iArr) {
            super(context, i, cursor, strArr, iArr);
            this.mActivity = null;
            this.mConstraint = null;
            this.mConstraintIsValid = false;
            this.mActivity = queryBrowserActivity;
            this.mQueryHandler = new QueryHandler(context.getContentResolver());
        }

        public void setActivity(QueryBrowserActivity queryBrowserActivity) {
            this.mActivity = queryBrowserActivity;
        }

        public AsyncQueryHandler getQueryHandler() {
            return this.mQueryHandler;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView textView = (TextView) view.findViewById(R.id.line1);
            TextView textView2 = (TextView) view.findViewById(R.id.line2);
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
            if (layoutParams == null) {
                DatabaseUtils.dumpCursor(cursor);
                return;
            }
            layoutParams.width = -2;
            layoutParams.height = -2;
            String string = cursor.getString(cursor.getColumnIndexOrThrow("mime_type"));
            MusicLogUtils.v("QueryBrowser", "bindView: mimetype=" + string);
            if (string == null) {
                string = "audio/";
            }
            if (string.equals("artist")) {
                imageView.setImageResource(R.drawable.ic_mp_artist_list);
                String string2 = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                boolean z = false;
                if (string2 == null || string2.equals("<unknown>")) {
                    string2 = context.getString(R.string.unknown_artist_name);
                    z = true;
                }
                textView.setText(string2);
                textView2.setText(MusicUtils.makeAlbumsSongsLabel(context, cursor.getInt(cursor.getColumnIndexOrThrow("data1")), cursor.getInt(cursor.getColumnIndexOrThrow("data2")), z));
                return;
            }
            if (string.equals("album")) {
                imageView.setImageResource(R.drawable.albumart_mp_unknown_list);
                String string3 = cursor.getString(cursor.getColumnIndexOrThrow("album"));
                if (string3 == null || string3.equals("<unknown>")) {
                    string3 = context.getString(R.string.unknown_album_name);
                }
                textView.setText(string3);
                String string4 = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                if (string4 == null || string4.equals("<unknown>")) {
                    string4 = context.getString(R.string.unknown_artist_name);
                }
                textView2.setText(string4);
                return;
            }
            if (string.startsWith("audio/") || string.equals("application/ogg") || string.equals("application/x-ogg")) {
                imageView.setImageResource(R.drawable.ic_mp_song_list);
                textView.setText(cursor.getString(cursor.getColumnIndexOrThrow("title")));
                String string5 = cursor.getString(cursor.getColumnIndexOrThrow("artist"));
                if (string5 == null || string5.equals("<unknown>")) {
                    string5 = context.getString(R.string.unknown_artist_name);
                }
                String string6 = cursor.getString(cursor.getColumnIndexOrThrow("album"));
                if (string6 == null || string6.equals("<unknown>")) {
                    string6 = context.getString(R.string.unknown_album_name);
                }
                textView2.setText(string5 + " - " + string6);
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
        public Cursor runQueryOnBackgroundThread(CharSequence charSequence) {
            String string = charSequence.toString();
            if (!this.mConstraintIsValid || ((string != null || this.mConstraint != null) && (string == null || !string.equals(this.mConstraint)))) {
                Cursor queryCursor = this.mActivity.getQueryCursor(null, string);
                this.mConstraint = string;
                this.mConstraintIsValid = true;
                return queryCursor;
            }
            return getCursor();
        }
    }
}
