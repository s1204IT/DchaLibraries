package com.android.music;

import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.android.music.MusicUtils;
import java.util.ArrayList;

public class MediaPickerActivity extends ListActivity {
    private Cursor mCursor;
    private String mFirstYear;
    private String mLastYear;
    private String mSortOrder = "title COLLATE UNICODE";
    private MusicUtils.ServiceToken mToken;
    private String mWhereClause;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mFirstYear = getIntent().getStringExtra("firstyear");
        this.mLastYear = getIntent().getStringExtra("lastyear");
        if (this.mFirstYear == null) {
            setTitle(R.string.all_title);
        } else if (this.mFirstYear.equals(this.mLastYear)) {
            setTitle(this.mFirstYear);
        } else {
            setTitle(this.mFirstYear + "-" + this.mLastYear);
        }
        this.mToken = MusicUtils.bindToService(this);
        init();
    }

    @Override
    public void onDestroy() {
        MusicUtils.unbindFromService(this.mToken);
        super.onDestroy();
        if (this.mCursor != null) {
            this.mCursor.close();
        }
    }

    public void init() {
        setContentView(R.layout.media_picker_activity);
        MakeCursor();
        if (this.mCursor != null && this.mCursor.getCount() != 0) {
            PickListAdapter adapter = new PickListAdapter(this, R.layout.track_list_item, this.mCursor, new String[0], new int[0]);
            setListAdapter(adapter);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri;
        long mediaId;
        this.mCursor.moveToPosition(position);
        String type = this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("mime_type"));
        String action = getIntent().getAction();
        if ("android.intent.action.GET_CONTENT".equals(action)) {
            if (type.startsWith("video")) {
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                mediaId = this.mCursor.getLong(this.mCursor.getColumnIndexOrThrow("_id"));
            } else {
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                mediaId = this.mCursor.getLong(this.mCursor.getColumnIndexOrThrow("_id"));
            }
            setResult(-1, new Intent().setData(ContentUris.withAppendedId(uri, mediaId)));
            finish();
            return;
        }
        if (MusicUtils.sService != null) {
            try {
                MusicUtils.sService.stop();
            } catch (RemoteException e) {
            }
        }
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id), type);
        startActivity(intent);
    }

    private void MakeCursor() {
        Cursor c;
        String[] audiocols = {"_id", "artist", "album", "title", "_data", "mime_type", "year"};
        String[] videocols = {"_id", "title", "artist", "album", "title", "_data", "mime_type"};
        ArrayList<Cursor> cList = new ArrayList<>();
        Intent intent = getIntent();
        String type = intent.getType();
        if (this.mFirstYear != null) {
            if (type.equals("video/*")) {
                this.mCursor = null;
                return;
            }
            this.mWhereClause = "year>=" + this.mFirstYear + " AND year<=" + this.mLastYear;
        }
        if (type.equals("video/*")) {
            Cursor c2 = MusicUtils.query(this, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videocols, null, null, this.mSortOrder);
            if (c2 != null) {
                cList.add(c2);
            }
        } else {
            Cursor c3 = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, audiocols, this.mWhereClause, null, this.mSortOrder);
            if (c3 != null) {
                cList.add(c3);
            }
            if (this.mFirstYear == null && intent.getType().equals("media/*") && (c = MusicUtils.query(this, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, videocols, null, null, this.mSortOrder)) != null) {
                cList.add(c);
            }
        }
        int size = cList.size();
        if (size == 0) {
            this.mCursor = null;
        } else {
            Cursor[] cs = new Cursor[size];
            this.mCursor = new SortCursor((Cursor[]) cList.toArray(cs), "title");
        }
    }

    static class PickListAdapter extends SimpleCursorAdapter {
        int mAlbumIdx;
        int mArtistIdx;
        int mMimeIdx;
        int mTitleIdx;

        PickListAdapter(Context context, int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);
            this.mTitleIdx = cursor.getColumnIndexOrThrow("title");
            this.mArtistIdx = cursor.getColumnIndexOrThrow("artist");
            this.mAlbumIdx = cursor.getColumnIndexOrThrow("album");
            this.mMimeIdx = cursor.getColumnIndexOrThrow("mime_type");
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View v = super.newView(context, cursor, parent);
            ImageView iv = (ImageView) v.findViewById(R.id.icon);
            iv.setVisibility(0);
            ViewGroup.LayoutParams p = iv.getLayoutParams();
            p.width = -2;
            p.height = -2;
            TextView tv = (TextView) v.findViewById(R.id.duration);
            tv.setVisibility(8);
            ((ImageView) v.findViewById(R.id.play_indicator)).setVisibility(8);
            return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView tv = (TextView) view.findViewById(R.id.line1);
            tv.setText(cursor.getString(this.mTitleIdx));
            TextView tv2 = (TextView) view.findViewById(R.id.line2);
            String name = cursor.getString(this.mAlbumIdx);
            StringBuilder builder = new StringBuilder();
            if (name == null || name.equals("<unknown>")) {
                builder.append(context.getString(R.string.unknown_album_name));
            } else {
                builder.append(name);
            }
            builder.append("\n");
            String name2 = cursor.getString(this.mArtistIdx);
            if (name2 == null || name2.equals("<unknown>")) {
                builder.append(context.getString(R.string.unknown_artist_name));
            } else {
                builder.append(name2);
            }
            tv2.setText(builder.toString());
            String text = cursor.getString(this.mMimeIdx);
            ImageView iv = (ImageView) view.findViewById(R.id.icon);
            if ("audio/midi".equals(text)) {
                iv.setImageResource(R.drawable.midi);
                return;
            }
            if (text != null && (text.startsWith("audio") || text.equals("application/ogg") || text.equals("application/x-ogg"))) {
                iv.setImageResource(R.drawable.ic_search_category_music_song);
            } else if (text != null && text.startsWith("video")) {
                iv.setImageResource(R.drawable.movie);
            } else {
                iv.setImageResource(0);
            }
        }
    }
}
