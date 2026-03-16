package com.android.music;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class VideoBrowserActivity extends ListActivity {
    private Cursor mCursor;
    private String mSortOrder;
    private String mWhereClause;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setVolumeControlStream(3);
        init();
    }

    public void init() {
        setContentView(R.layout.media_picker_activity);
        MakeCursor();
        if (this.mCursor == null) {
            MusicUtils.displayDatabaseError(this);
            return;
        }
        if (this.mCursor.getCount() > 0) {
            setTitle(R.string.videos_title);
        } else {
            setTitle(R.string.no_videos_title);
        }
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, this.mCursor, new String[]{"title"}, new int[]{android.R.id.text1});
        setListAdapter(adapter);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent("android.intent.action.VIEW");
        this.mCursor.moveToPosition(position);
        String type = this.mCursor.getString(this.mCursor.getColumnIndexOrThrow("mime_type"));
        intent.setDataAndType(ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id), type);
        startActivity(intent);
    }

    private void MakeCursor() {
        String[] cols = {"_id", "title", "_data", "mime_type", "artist"};
        ContentResolver resolver = getContentResolver();
        if (resolver == null) {
            System.out.println("resolver = null");
            return;
        }
        this.mSortOrder = "title COLLATE UNICODE";
        this.mWhereClause = "title != ''";
        this.mCursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, cols, this.mWhereClause, null, this.mSortOrder);
    }

    @Override
    public void onDestroy() {
        if (this.mCursor != null) {
            this.mCursor.close();
        }
        super.onDestroy();
    }
}
