package com.android.music;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.drm.DrmManagerClient;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.mediatek.omadrm.OmaDrmUtils;
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;

public class MusicPicker extends ListActivity implements MediaPlayer.OnCompletionListener, View.OnClickListener {
    static final String[] CURSOR_COLS = {"_id", "title", "_data", "album", "artist", "artist_id", "duration", "track", "is_drm"};
    static StringBuilder sFormatBuilder = new StringBuilder();
    static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    static final Object[] sTimeArgs = new Object[5];
    TrackListAdapter mAdapter;
    Uri mBaseUri;
    View mCancelButton;
    Cursor mCursor;
    boolean mListHasFocus;
    MediaPlayer mMediaPlayer;
    View mOkayButton;
    QueryHandler mQueryHandler;
    Uri mSelectedUri;
    String mSortOrder;
    Parcelable mListState = null;
    int mSortMode = -1;
    long mSelectedId = -1;
    long mPlayingId = -1;
    int mPrevSelectedPos = -1;
    int mSelectedPos = -1;
    int mDrmLevel = -1;
    private DrmManagerClient mDrmClient = null;
    private boolean mIsBroadcastReg = false;
    private final BroadcastReceiver mScanListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            MusicLogUtils.v("MusicPicker", "mScanListener.onReceive:" + action + ", status = " + Environment.getExternalStorageState());
            if ("android.intent.action.MEDIA_SCANNER_STARTED".equals(action) || "android.intent.action.MEDIA_SCANNER_FINISHED".equals(action)) {
                MusicUtils.setSpinnerState(MusicPicker.this);
            }
            MusicPicker.this.doQuery(false, null);
        }
    };

    class TrackListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        private int mAlbumIdx;
        private int mArtistIdx;
        private final StringBuilder mBuilder;
        private int mDrmMethodIdx;
        private int mDurationIdx;
        private int mIdIdx;
        private MusicAlphabetIndexer mIndexer;
        private int mIndexerSortMode;
        private int mIsDrmIdx;
        final ListView mListView;
        private boolean mLoading;
        private int mTitleIdx;
        private final String mUnknownAlbum;
        private final String mUnknownArtist;

        class ViewHolder {
            CharArrayBuffer buffer1;
            char[] buffer2;
            ImageView drmLock;
            TextView duration;
            TextView line1;
            TextView line2;
            ImageView play_indicator;
            RadioButton radio;

            ViewHolder() {
            }
        }

        TrackListAdapter(Context context, ListView listView, int i, String[] strArr, int[] iArr) {
            super(context, i, null, strArr, iArr);
            this.mBuilder = new StringBuilder();
            this.mLoading = true;
            this.mListView = listView;
            this.mUnknownArtist = context.getString(R.string.unknown_artist_name);
            this.mUnknownAlbum = context.getString(R.string.unknown_album_name);
        }

        public void setLoading(boolean z) {
            this.mLoading = z;
        }

        @Override
        public boolean isEmpty() {
            if (this.mLoading) {
                return false;
            }
            return super.isEmpty();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View viewNewView = super.newView(context, cursor, viewGroup);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.line1 = (TextView) viewNewView.findViewById(R.id.line1);
            viewHolder.line2 = (TextView) viewNewView.findViewById(R.id.line2);
            viewHolder.duration = (TextView) viewNewView.findViewById(R.id.duration);
            viewHolder.radio = (RadioButton) viewNewView.findViewById(R.id.radio);
            viewHolder.play_indicator = (ImageView) viewNewView.findViewById(R.id.play_indicator);
            viewHolder.buffer1 = new CharArrayBuffer(100);
            viewHolder.buffer2 = new char[200];
            viewNewView.setTag(viewHolder);
            viewHolder.drmLock = (ImageView) viewNewView.findViewById(R.id.drm_lock);
            return viewNewView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            cursor.copyStringToBuffer(this.mTitleIdx, viewHolder.buffer1);
            viewHolder.line1.setText(viewHolder.buffer1.data, 0, viewHolder.buffer1.sizeCopied);
            int i = cursor.getInt(this.mDurationIdx) / 1000;
            if (i == 0) {
                viewHolder.duration.setText("");
            } else {
                viewHolder.duration.setText(MusicUtils.makeTimeString(context, i));
            }
            StringBuilder sb = this.mBuilder;
            sb.delete(0, sb.length());
            String string = cursor.getString(this.mAlbumIdx);
            if (string == null || string.equals("<unknown>")) {
                sb.append(this.mUnknownAlbum);
            } else {
                sb.append(string);
            }
            sb.append('\n');
            String string2 = cursor.getString(this.mArtistIdx);
            if (string2 == null || string2.equals("<unknown>")) {
                sb.append(this.mUnknownArtist);
            } else {
                sb.append(string2);
            }
            int length = sb.length();
            if (viewHolder.buffer2.length < length) {
                viewHolder.buffer2 = new char[length];
            }
            sb.getChars(0, length, viewHolder.buffer2, 0);
            viewHolder.line2.setText(viewHolder.buffer2, 0, length);
            long j = cursor.getLong(this.mIdIdx);
            viewHolder.radio.setChecked(j == MusicPicker.this.mSelectedId);
            MusicLogUtils.v("MusicPicker", "Binding id=" + j + " sel=" + MusicPicker.this.mSelectedId + " playing=" + MusicPicker.this.mPlayingId + " cursor=" + cursor);
            ImageView imageView = viewHolder.play_indicator;
            if (j == MusicPicker.this.mPlayingId) {
                imageView.setVisibility(0);
            } else {
                imageView.setVisibility(8);
            }
            updateDrmLockIcon(viewHolder.drmLock, cursor, j);
        }

        private void updateDrmLockIcon(ImageView imageView, Cursor cursor, long j) {
            int iCheckRightsStatus;
            int i = 0;
            if (OmaDrmUtils.isOmaDrmEnabled()) {
                int i2 = cursor.getInt(this.mIsDrmIdx);
                cursor.getInt(this.mDrmMethodIdx);
                if (i2 == 1 && (iCheckRightsStatus = MusicPicker.this.mDrmClient.checkRightsStatus(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, j), 1)) >= 0) {
                    if (iCheckRightsStatus == 0) {
                        imageView.setImageResource(134348871);
                    } else {
                        imageView.setImageResource(134348872);
                    }
                } else {
                    i = 8;
                }
            }
            imageView.setVisibility(i);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            MusicLogUtils.v("MusicPicker", "changeCursor cursor to: " + cursor + " from: " + MusicPicker.this.mCursor);
            MusicPicker.this.mCursor = cursor;
            if (cursor != null) {
                this.mIdIdx = cursor.getColumnIndex("_id");
                this.mTitleIdx = cursor.getColumnIndex("title");
                this.mArtistIdx = cursor.getColumnIndex("artist");
                this.mAlbumIdx = cursor.getColumnIndex("album");
                this.mDurationIdx = cursor.getColumnIndex("duration");
                this.mIsDrmIdx = cursor.getColumnIndex("is_drm");
                if (this.mIndexerSortMode != MusicPicker.this.mSortMode || this.mIndexer == null) {
                    this.mIndexerSortMode = MusicPicker.this.mSortMode;
                    int columnIndexOrThrow = 0;
                    switch (this.mIndexerSortMode) {
                        case 2:
                            columnIndexOrThrow = cursor.getColumnIndexOrThrow("album_key");
                            break;
                        case 3:
                            columnIndexOrThrow = cursor.getColumnIndexOrThrow("artist_key");
                            break;
                    }
                    this.mIndexer = new MusicAlphabetIndexer(cursor, columnIndexOrThrow, MusicPicker.this.getResources().getString(R.string.fast_scroll_alphabet));
                    return;
                }
                this.mIndexer.setCursor(cursor);
            }
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence charSequence) {
            MusicLogUtils.v("MusicPicker", "Getting new cursor...");
            return MusicPicker.this.doQuery(true, charSequence.toString());
        }

        @Override
        public int getPositionForSection(int i) {
            if (getCursor() == null || this.mIndexer == null) {
                return 0;
            }
            return this.mIndexer.getPositionForSection(i);
        }

        @Override
        public int getSectionForPosition(int i) {
            if (this.mIndexer != null) {
                return this.mIndexer.getSectionForPosition(i);
            }
            return 0;
        }

        @Override
        public Object[] getSections() {
            if (this.mIndexer != null) {
                return this.mIndexer.getSections();
            }
            return null;
        }
    }

    private final class QueryHandler extends AsyncQueryHandler {
        public QueryHandler(Context context) {
            super(context.getContentResolver());
        }

        @Override
        protected void onQueryComplete(int i, Object obj, Cursor cursor) {
            if (cursor == null) {
                MusicPicker.this.mOkayButton.setEnabled(false);
                MusicUtils.resetSdStatus();
                MusicUtils.displayDatabaseError(MusicPicker.this, false);
                return;
            }
            if (!MusicPicker.this.isFinishing()) {
                MusicUtils.emptyShow(MusicPicker.this.getListView(), MusicPicker.this);
                MusicPicker.this.mOkayButton.setEnabled(cursor.getCount() > 0);
                MusicPicker.this.mAdapter.setLoading(false);
                MusicPicker.this.mAdapter.changeCursor(cursor);
                if (MusicPicker.this.getListView().getCount() != 0) {
                    MusicPicker.this.setProgressBarIndeterminateVisibility(false);
                }
                if (MusicPicker.this.mListState != null) {
                    MusicPicker.this.getListView().onRestoreInstanceState(MusicPicker.this.mListState);
                    if (MusicPicker.this.mListHasFocus) {
                        MusicPicker.this.getListView().requestFocus();
                    }
                    MusicPicker.this.mListHasFocus = false;
                    MusicPicker.this.mListState = null;
                }
                MusicUtils.hideDatabaseError(MusicPicker.this);
                MusicPicker.this.setTitle(R.string.music_picker_title);
                return;
            }
            cursor.close();
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        int i;
        super.onCreate(bundle);
        requestWindowFeature(5);
        setVolumeControlStream(3);
        if (bundle == null) {
            this.mSelectedUri = (Uri) getIntent().getParcelableExtra("android.intent.extra.ringtone.EXISTING_URI");
            this.mDrmLevel = getIntent().getIntExtra("android.intent.extra.drm_level", -1);
            MusicLogUtils.v("MusicPicker", "onCreate: drmlevel=" + this.mDrmLevel);
            i = 1;
        } else {
            this.mSelectedUri = (Uri) bundle.getParcelable("android.intent.extra.ringtone.EXISTING_URI");
            this.mListState = bundle.getParcelable("liststate");
            this.mListHasFocus = bundle.getBoolean("focused");
            i = bundle.getInt("sortMode", 1);
            this.mPrevSelectedPos = bundle.getInt("selectedpos", -1);
            this.mDrmLevel = bundle.getInt("drmlevel", -1);
            MusicLogUtils.v("MusicPicker", "onCreate: drmlevel(restored) = " + this.mDrmLevel + ", mSelectedUri = " + this.mSelectedUri);
        }
        if ("android.intent.action.GET_CONTENT".equals(getIntent().getAction())) {
            this.mBaseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else {
            this.mBaseUri = getIntent().getData();
            if (this.mBaseUri == null) {
                MusicLogUtils.v("MusicPicker", "No data URI given to PICK action");
                finish();
                return;
            }
        }
        setContentView(R.layout.music_picker);
        if (OmaDrmUtils.isOmaDrmEnabled()) {
            this.mDrmClient = new DrmManagerClient(this);
        }
        this.mSortOrder = null;
        ListView listView = getListView();
        listView.setItemsCanFocus(false);
        this.mAdapter = new TrackListAdapter(this, listView, R.layout.music_picker_item, new String[0], new int[0]);
        setListAdapter(this.mAdapter);
        listView.setTextFilterEnabled(true);
        listView.setSaveEnabled(false);
        this.mQueryHandler = new QueryHandler(this);
        this.mOkayButton = findViewById(R.id.okayButton);
        this.mOkayButton.setOnClickListener(this);
        this.mOkayButton.setEnabled(false);
        this.mCancelButton = findViewById(R.id.cancelButton);
        this.mCancelButton.setOnClickListener(this);
        if (this.mSelectedUri != null) {
            Uri.Builder builderBuildUpon = this.mSelectedUri.buildUpon();
            String encodedPath = this.mSelectedUri.getEncodedPath();
            int iLastIndexOf = encodedPath.lastIndexOf(47);
            if (iLastIndexOf >= 0) {
                encodedPath = encodedPath.substring(0, iLastIndexOf);
            }
            builderBuildUpon.encodedPath(encodedPath);
            Uri uriBuild = builderBuildUpon.build();
            MusicLogUtils.v("MusicPicker", "Selected Uri: " + this.mSelectedUri);
            MusicLogUtils.v("MusicPicker", "Selected base Uri: " + uriBuild);
            MusicLogUtils.v("MusicPicker", "Base Uri: " + this.mBaseUri);
            if (uriBuild.equals(this.mBaseUri)) {
                this.mSelectedId = ContentUris.parseId(this.mSelectedUri);
            }
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addDataScheme("file");
        registerReceiver(this.mScanListener, intentFilter);
        this.mIsBroadcastReg = true;
        setSortMode(i);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        doQuery(false, null);
    }

    @Override
    public void onResume() {
        super.onResume();
        getListView().invalidateViews();
        this.mOkayButton.setEnabled(this.mSelectedId >= 0);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (setSortMode(menuItem.getItemId())) {
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putParcelable("liststate", getListView().onSaveInstanceState());
        bundle.putBoolean("focused", getListView().hasFocus());
        bundle.putInt("sortMode", this.mSortMode);
        bundle.putParcelable("android.intent.extra.ringtone.EXISTING_URI", this.mSelectedUri);
        bundle.putInt("drmlevel", this.mDrmLevel);
    }

    @Override
    public void onPause() {
        super.onPause();
        stopMediaPlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.mAdapter.setLoading(true);
        this.mAdapter.changeCursor(null);
    }

    boolean setSortMode(int i) {
        if (i != this.mSortMode) {
            switch (i) {
                case 1:
                    this.mSortMode = i;
                    this.mSortOrder = null;
                    doQuery(false, null);
                    break;
                case 2:
                    this.mSortMode = i;
                    this.mSortOrder = "track ASC, ";
                    doQuery(false, null);
                    break;
                case 3:
                    this.mSortMode = i;
                    this.mSortOrder = "track ASC, ";
                    doQuery(false, null);
                    break;
            }
            return true;
        }
        return false;
    }

    Cursor doQuery(boolean z, String str) {
        MusicLogUtils.v("MusicPicker", "doQuery(" + z + ", " + str + ")");
        this.mQueryHandler.cancelOperation(42);
        StringBuilder sb = new StringBuilder();
        sb.append("title != ''");
        if (OmaDrmUtils.isOmaDrmEnabled()) {
            int i = this.mDrmLevel;
            if (i != 4) {
                switch (i) {
                    case 1:
                        sb.append(" AND (is_drm!=1 OR (is_drm=1 AND drm_method=1))");
                        break;
                    case 2:
                        sb.append(" AND (is_drm!=1 OR (is_drm=1 AND drm_method=4))");
                        break;
                    default:
                        sb.append(" AND is_drm!=1");
                        break;
                }
            }
            MusicLogUtils.v("MusicPicker", "doQuery: where=" + ((Object) sb));
        }
        Uri uriBuild = this.mBaseUri;
        if (!TextUtils.isEmpty(str)) {
            uriBuild = uriBuild.buildUpon().appendQueryParameter("filter", Uri.encode(str)).build();
        }
        Uri uri = uriBuild;
        if (z) {
            try {
                return getContentResolver().query(uri, CURSOR_COLS, sb.toString(), null, this.mSortOrder);
            } catch (UnsupportedOperationException e) {
                return null;
            }
        }
        this.mAdapter.setLoading(true);
        this.mQueryHandler.startQuery(42, null, uri, CURSOR_COLS, sb.toString(), null, this.mSortOrder);
        return null;
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        this.mCursor.moveToPosition(i);
        this.mSelectedPos = i;
        MusicLogUtils.v("MusicPicker", "Click on " + i + " (id=" + j + ", cursid=" + this.mCursor.getLong(this.mCursor.getColumnIndex("_id")) + ") in cursor " + this.mCursor + " adapter=" + listView.getAdapter());
        setSelected(this.mCursor);
        this.mOkayButton.setEnabled(true);
    }

    void setSelected(Cursor cursor) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        long j = this.mCursor.getLong(this.mCursor.getColumnIndex("_id"));
        this.mSelectedUri = ContentUris.withAppendedId(uri, j);
        this.mSelectedId = j;
        if (j != this.mPlayingId || this.mMediaPlayer == null) {
            stopMediaPlayer();
            this.mMediaPlayer = new MediaPlayer();
            try {
                try {
                    this.mMediaPlayer.setDataSource(this, this.mSelectedUri);
                    this.mMediaPlayer.setOnCompletionListener(this);
                    this.mMediaPlayer.setAudioStreamType(3);
                    this.mMediaPlayer.prepare();
                    this.mMediaPlayer.start();
                    this.mPlayingId = j;
                } catch (IOException e) {
                    MusicLogUtils.v("MusicPicker", "Unable to play track", e);
                }
                return;
            } finally {
                getListView().invalidateViews();
            }
        }
        if (this.mMediaPlayer != null) {
            stopMediaPlayer();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (this.mMediaPlayer == mediaPlayer) {
            mediaPlayer.stop();
            mediaPlayer.release();
            this.mMediaPlayer = null;
            this.mPlayingId = -1L;
            getListView().invalidateViews();
        }
    }

    void stopMediaPlayer() {
        if (this.mMediaPlayer != null) {
            this.mMediaPlayer.stop();
            this.mMediaPlayer.release();
            this.mMediaPlayer = null;
            this.mPlayingId = -1L;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.cancelButton:
                finish();
                break;
            case R.id.okayButton:
                if (this.mSelectedId >= 0) {
                    setResult(-1, new Intent().setData(this.mSelectedUri));
                    finish();
                }
                break;
        }
    }

    @Override
    public boolean onSearchRequested() {
        return false;
    }

    @Override
    protected void onDestroy() {
        if (this.mIsBroadcastReg) {
            unregisterReceiver(this.mScanListener);
        }
        if (this.mDrmClient != null) {
            this.mDrmClient.release();
            this.mDrmClient = null;
        }
        super.onDestroy();
    }
}
