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
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
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
        if (this.mCursor == null || this.mCursor.getCount() == 0) {
            return;
        }
        setListAdapter(new PickListAdapter(this, R.layout.track_list_item, this.mCursor, new String[0], new int[0]));
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int i, long j) {
        Uri uri;
        long j2;
        this.mCursor.moveToPosition(i);
        String string = this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("mime_type"));
        if ("android.intent.action.GET_CONTENT".equals(getIntent().getAction())) {
            if (string.startsWith("video")) {
                uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                j2 = this.mCursor.getLong(this.mCursor.getColumnIndexOrThrow("_id"));
            } else {
                uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                j2 = this.mCursor.getLong(this.mCursor.getColumnIndexOrThrow("_id"));
            }
            setResult(-1, new Intent().setData(ContentUris.withAppendedId(uri, j2)));
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
        intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, j), string);
        startActivity(intent);
    }

    private void MakeCursor() {
        Cursor cursorQuery;
        String[] strArr = {"_id", "artist", "album", "title", "_data", "mime_type", "year"};
        String[] strArr2 = {"_id", "title", "artist", "album", "title", "_data", "mime_type"};
        ArrayList arrayList = new ArrayList();
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
            Cursor cursorQuery2 = MusicUtils.query(this, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, strArr2, null, null, this.mSortOrder);
            if (cursorQuery2 != null) {
                arrayList.add(cursorQuery2);
            }
        } else {
            Cursor cursorQuery3 = MusicUtils.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, strArr, this.mWhereClause, null, this.mSortOrder);
            if (cursorQuery3 != null) {
                arrayList.add(cursorQuery3);
            }
            if (this.mFirstYear == null && intent.getType().equals("media/*") && (cursorQuery = MusicUtils.query(this, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, strArr2, null, null, this.mSortOrder)) != null) {
                arrayList.add(cursorQuery);
            }
        }
        int size = arrayList.size();
        if (size == 0) {
            this.mCursor = null;
        } else {
            this.mCursor = new SortCursor((Cursor[]) arrayList.toArray(new Cursor[size]), "title");
        }
    }

    static class PickListAdapter extends SimpleCursorAdapter {
        int mAlbumIdx;
        int mArtistIdx;
        int mMimeIdx;
        int mTitleIdx;

        PickListAdapter(Context context, int i, Cursor cursor, String[] strArr, int[] iArr) {
            super(context, i, cursor, strArr, iArr);
            this.mTitleIdx = cursor.getColumnIndexOrThrow("title");
            this.mArtistIdx = cursor.getColumnIndexOrThrow("artist");
            this.mAlbumIdx = cursor.getColumnIndexOrThrow("album");
            this.mMimeIdx = cursor.getColumnIndexOrThrow("mime_type");
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View viewNewView = super.newView(context, cursor, viewGroup);
            ImageView imageView = (ImageView) viewNewView.findViewById(R.id.icon);
            imageView.setVisibility(0);
            ViewGroup.LayoutParams layoutParams = imageView.getLayoutParams();
            layoutParams.width = -2;
            layoutParams.height = -2;
            ((TextView) viewNewView.findViewById(R.id.duration)).setVisibility(8);
            ((ImageView) viewNewView.findViewById(R.id.play_indicator)).setVisibility(8);
            return viewNewView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.line1)).setText(cursor.getString(this.mTitleIdx));
            TextView textView = (TextView) view.findViewById(R.id.line2);
            String string = cursor.getString(this.mAlbumIdx);
            StringBuilder sb = new StringBuilder();
            if (string == null || string.equals("<unknown>")) {
                sb.append(context.getString(R.string.unknown_album_name));
            } else {
                sb.append(string);
            }
            sb.append("\n");
            String string2 = cursor.getString(this.mArtistIdx);
            if (string2 == null || string2.equals("<unknown>")) {
                sb.append(context.getString(R.string.unknown_artist_name));
            } else {
                sb.append(string2);
            }
            textView.setText(sb.toString());
            String string3 = cursor.getString(this.mMimeIdx);
            ImageView imageView = (ImageView) view.findViewById(R.id.icon);
            if ("audio/midi".equals(string3)) {
                imageView.setImageResource(R.drawable.midi);
                return;
            }
            if (string3 != null && (string3.startsWith("audio") || string3.equals("application/ogg") || string3.equals("application/x-ogg"))) {
                imageView.setImageResource(R.drawable.ic_search_category_music_song);
            } else if (string3 != null && string3.startsWith("video")) {
                imageView.setImageResource(R.drawable.movie);
            } else {
                imageView.setImageResource(0);
            }
        }
    }
}
