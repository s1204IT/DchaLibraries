package com.android.music;

import android.app.ListActivity;
import android.content.AsyncQueryHandler;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;

public class MusicPicker extends ListActivity implements MediaPlayer.OnCompletionListener, View.OnClickListener {
    static final String[] CURSOR_COLS = {"_id", "title", "title_key", "_data", "album", "artist", "artist_id", "duration", "track"};
    static StringBuilder sFormatBuilder = new StringBuilder();
    static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    static final Object[] sTimeArgs = new Object[5];
    TrackListAdapter mAdapter;
    Uri mBaseUri;
    View mCancelButton;
    Cursor mCursor;
    View mListContainer;
    boolean mListHasFocus;
    boolean mListShown;
    MediaPlayer mMediaPlayer;
    View mOkayButton;
    View mProgressContainer;
    QueryHandler mQueryHandler;
    Uri mSelectedUri;
    String mSortOrder;
    Parcelable mListState = null;
    int mSortMode = -1;
    long mSelectedId = -1;
    long mPlayingId = -1;

    class TrackListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        private int mAlbumIdx;
        private int mArtistIdx;
        private final StringBuilder mBuilder;
        private int mDurationIdx;
        private int mIdIdx;
        private MusicAlphabetIndexer mIndexer;
        private int mIndexerSortMode;
        final ListView mListView;
        private boolean mLoading;
        private int mTitleIdx;
        private final String mUnknownAlbum;
        private final String mUnknownArtist;

        class ViewHolder {
            CharArrayBuffer buffer1;
            char[] buffer2;
            TextView duration;
            TextView line1;
            TextView line2;
            ImageView play_indicator;
            RadioButton radio;

            ViewHolder() {
            }
        }

        TrackListAdapter(Context context, ListView listView, int layout, String[] from, int[] to) {
            super(context, layout, null, from, to);
            this.mBuilder = new StringBuilder();
            this.mLoading = true;
            this.mListView = listView;
            this.mUnknownArtist = context.getString(R.string.unknown_artist_name);
            this.mUnknownAlbum = context.getString(R.string.unknown_album_name);
        }

        public void setLoading(boolean loading) {
            this.mLoading = loading;
        }

        @Override
        public boolean isEmpty() {
            if (this.mLoading) {
                return false;
            }
            return super.isEmpty();
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            ViewHolder vh = new ViewHolder();
            vh.line1 = (TextView) v.findViewById(R.id.line1);
            vh.line2 = (TextView) v.findViewById(R.id.line2);
            vh.duration = (TextView) v.findViewById(R.id.duration);
            vh.radio = (RadioButton) v.findViewById(R.id.radio);
            vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
            vh.buffer1 = new CharArrayBuffer(100);
            vh.buffer2 = new char[200];
            v.setTag(vh);
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder vh = (ViewHolder) view.getTag();
            cursor.copyStringToBuffer(this.mTitleIdx, vh.buffer1);
            vh.line1.setText(vh.buffer1.data, 0, vh.buffer1.sizeCopied);
            int secs = cursor.getInt(this.mDurationIdx) / 1000;
            if (secs == 0) {
                vh.duration.setText("");
            } else {
                vh.duration.setText(MusicUtils.makeTimeString(context, secs));
            }
            StringBuilder builder = this.mBuilder;
            builder.delete(0, builder.length());
            String name = cursor.getString(this.mAlbumIdx);
            if (name == null || name.equals("<unknown>")) {
                builder.append(this.mUnknownAlbum);
            } else {
                builder.append(name);
            }
            builder.append('\n');
            String name2 = cursor.getString(this.mArtistIdx);
            if (name2 == null || name2.equals("<unknown>")) {
                builder.append(this.mUnknownArtist);
            } else {
                builder.append(name2);
            }
            int len = builder.length();
            if (vh.buffer2.length < len) {
                vh.buffer2 = new char[len];
            }
            builder.getChars(0, len, vh.buffer2, 0);
            vh.line2.setText(vh.buffer2, 0, len);
            long id = cursor.getLong(this.mIdIdx);
            vh.radio.setChecked(id == MusicPicker.this.mSelectedId);
            ImageView iv = vh.play_indicator;
            if (id == MusicPicker.this.mPlayingId) {
                iv.setImageResource(R.drawable.indicator_ic_mp_playing_list);
                iv.setVisibility(0);
            } else {
                iv.setVisibility(8);
            }
        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            MusicPicker.this.mCursor = cursor;
            if (cursor != null) {
                this.mIdIdx = cursor.getColumnIndex("_id");
                this.mTitleIdx = cursor.getColumnIndex("title");
                this.mArtistIdx = cursor.getColumnIndex("artist");
                this.mAlbumIdx = cursor.getColumnIndex("album");
                this.mDurationIdx = cursor.getColumnIndex("duration");
                if (this.mIndexerSortMode != MusicPicker.this.mSortMode || this.mIndexer == null) {
                    this.mIndexerSortMode = MusicPicker.this.mSortMode;
                    int idx = this.mTitleIdx;
                    switch (this.mIndexerSortMode) {
                        case 2:
                            idx = this.mAlbumIdx;
                            break;
                        case 3:
                            idx = this.mArtistIdx;
                            break;
                    }
                    this.mIndexer = new MusicAlphabetIndexer(cursor, idx, MusicPicker.this.getResources().getString(R.string.fast_scroll_alphabet));
                } else {
                    this.mIndexer.setCursor(cursor);
                }
            }
            MusicPicker.this.makeListShown();
        }

        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
            return MusicPicker.this.doQuery(true, constraint.toString());
        }

        @Override
        public int getPositionForSection(int section) {
            Cursor cursor = getCursor();
            if (cursor == null) {
                return 0;
            }
            return this.mIndexer.getPositionForSection(section);
        }

        @Override
        public int getSectionForPosition(int position) {
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
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            if (!MusicPicker.this.isFinishing()) {
                MusicPicker.this.mAdapter.setLoading(false);
                MusicPicker.this.mAdapter.changeCursor(cursor);
                MusicPicker.this.setProgressBarIndeterminateVisibility(false);
                if (MusicPicker.this.mListState != null) {
                    MusicPicker.this.getListView().onRestoreInstanceState(MusicPicker.this.mListState);
                    if (MusicPicker.this.mListHasFocus) {
                        MusicPicker.this.getListView().requestFocus();
                    }
                    MusicPicker.this.mListHasFocus = false;
                    MusicPicker.this.mListState = null;
                    return;
                }
                return;
            }
            cursor.close();
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        requestWindowFeature(5);
        int sortMode = 1;
        if (icicle == null) {
            this.mSelectedUri = (Uri) getIntent().getParcelableExtra("android.intent.extra.ringtone.EXISTING_URI");
        } else {
            this.mSelectedUri = (Uri) icicle.getParcelable("android.intent.extra.ringtone.EXISTING_URI");
            this.mListState = icicle.getParcelable("liststate");
            this.mListHasFocus = icicle.getBoolean("focused");
            sortMode = icicle.getInt("sortMode", 1);
        }
        if ("android.intent.action.GET_CONTENT".equals(getIntent().getAction())) {
            this.mBaseUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        } else {
            this.mBaseUri = getIntent().getData();
            if (this.mBaseUri == null) {
                Log.w("MusicPicker", "No data URI given to PICK action");
                finish();
                return;
            }
        }
        setContentView(R.layout.music_picker);
        this.mSortOrder = "title_key";
        ListView listView = getListView();
        listView.setItemsCanFocus(false);
        this.mAdapter = new TrackListAdapter(this, listView, R.layout.music_picker_item, new String[0], new int[0]);
        setListAdapter(this.mAdapter);
        listView.setTextFilterEnabled(true);
        listView.setSaveEnabled(false);
        this.mQueryHandler = new QueryHandler(this);
        this.mProgressContainer = findViewById(R.id.progressContainer);
        this.mListContainer = findViewById(R.id.listContainer);
        this.mOkayButton = findViewById(R.id.okayButton);
        this.mOkayButton.setOnClickListener(this);
        this.mCancelButton = findViewById(R.id.cancelButton);
        this.mCancelButton.setOnClickListener(this);
        if (this.mSelectedUri != null) {
            Uri.Builder builder = this.mSelectedUri.buildUpon();
            String path = this.mSelectedUri.getEncodedPath();
            int idx = path.lastIndexOf(47);
            if (idx >= 0) {
                path = path.substring(0, idx);
            }
            builder.encodedPath(path);
            Uri baseSelectedUri = builder.build();
            if (baseSelectedUri.equals(this.mBaseUri)) {
                this.mSelectedId = ContentUris.parseId(this.mSelectedUri);
            }
        }
        setSortMode(sortMode);
    }

    @Override
    public void onRestart() {
        super.onRestart();
        doQuery(false, null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (setSortMode(item.getItemId())) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 0, R.string.sort_by_track);
        menu.add(0, 2, 0, R.string.sort_by_album);
        menu.add(0, 3, 0, R.string.sort_by_artist);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle icicle) {
        super.onSaveInstanceState(icicle);
        icicle.putParcelable("liststate", getListView().onSaveInstanceState());
        icicle.putBoolean("focused", getListView().hasFocus());
        icicle.putInt("sortMode", this.mSortMode);
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

    boolean setSortMode(int sortMode) {
        if (sortMode != this.mSortMode) {
            switch (sortMode) {
                case 1:
                    this.mSortMode = sortMode;
                    this.mSortOrder = "title_key";
                    doQuery(false, null);
                    return true;
                case 2:
                    this.mSortMode = sortMode;
                    this.mSortOrder = "album_key ASC, track ASC, title_key ASC";
                    doQuery(false, null);
                    return true;
                case 3:
                    this.mSortMode = sortMode;
                    this.mSortOrder = "artist_key ASC, album_key ASC, track ASC, title_key ASC";
                    doQuery(false, null);
                    return true;
            }
        }
        return false;
    }

    void makeListShown() {
        if (!this.mListShown) {
            this.mListShown = true;
            this.mProgressContainer.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
            this.mProgressContainer.setVisibility(8);
            this.mListContainer.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_in));
            this.mListContainer.setVisibility(0);
        }
    }

    Cursor doQuery(boolean sync, String filterstring) {
        this.mQueryHandler.cancelOperation(42);
        StringBuilder where = new StringBuilder();
        where.append("title != ''");
        Uri uri = this.mBaseUri;
        if (!TextUtils.isEmpty(filterstring)) {
            uri = uri.buildUpon().appendQueryParameter("filter", Uri.encode(filterstring)).build();
        }
        if (sync) {
            try {
                return getContentResolver().query(uri, CURSOR_COLS, where.toString(), null, this.mSortOrder);
            } catch (UnsupportedOperationException e) {
            }
        } else {
            this.mAdapter.setLoading(true);
            setProgressBarIndeterminateVisibility(true);
            this.mQueryHandler.startQuery(42, null, uri, CURSOR_COLS, where.toString(), null, this.mSortOrder);
        }
        return null;
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        this.mCursor.moveToPosition(position);
        setSelected(this.mCursor);
    }

    void setSelected(Cursor c) {
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        long newId = this.mCursor.getLong(this.mCursor.getColumnIndex("_id"));
        this.mSelectedUri = ContentUris.withAppendedId(uri, newId);
        this.mSelectedId = newId;
        if (newId != this.mPlayingId || this.mMediaPlayer == null) {
            stopMediaPlayer();
            this.mMediaPlayer = new MediaPlayer();
            try {
                this.mMediaPlayer.setDataSource(this, this.mSelectedUri);
                this.mMediaPlayer.setOnCompletionListener(this);
                this.mMediaPlayer.setAudioStreamType(2);
                this.mMediaPlayer.prepare();
                this.mMediaPlayer.start();
                this.mPlayingId = newId;
                getListView().invalidateViews();
                return;
            } catch (IOException e) {
                Log.w("MusicPicker", "Unable to play track", e);
                return;
            }
        }
        if (this.mMediaPlayer != null) {
            stopMediaPlayer();
            getListView().invalidateViews();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (this.mMediaPlayer == mp) {
            mp.stop();
            mp.release();
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.okayButton:
                if (this.mSelectedId >= 0) {
                    setResult(-1, new Intent().setData(this.mSelectedUri));
                    finish();
                }
                break;
            case R.id.cancelButton:
                finish();
                break;
        }
    }
}
