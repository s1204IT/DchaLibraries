package com.mediatek.gallery3d.video;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import com.android.gallery3d.R;
import com.android.gallery3d.app.MovieActivity;
import com.mediatek.galleryportable.Log;

public class BookmarkActivity extends Activity implements AdapterView.OnItemClickListener {
    public static final String KEY_LOGO_BITMAP = "logo-bitmap";
    private static final boolean LOG = true;
    private static final int MENU_DELETE_ALL = 1;
    private static final int MENU_DELETE_ONE = 2;
    private static final int MENU_EDIT = 3;
    private static final String TAG = "VP_BookmarkActivity";
    private BookmarkAdapter mAdapter;
    private BookmarkEnhance mBookmark;
    private Cursor mCursor;
    private TextView mEmptyView;
    private ListView mListView;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.m_bookmark);
        Bitmap bitmap = (Bitmap) getIntent().getParcelableExtra(KEY_LOGO_BITMAP);
        if (bitmap != null) {
            getActionBar().setLogo(new BitmapDrawable(getResources(), bitmap));
        }
        this.mListView = (ListView) findViewById(android.R.id.list);
        this.mEmptyView = (TextView) findViewById(android.R.id.empty);
        this.mBookmark = new BookmarkEnhance(this);
        this.mCursor = this.mBookmark.query();
        this.mAdapter = new BookmarkAdapter(this, R.layout.m_bookmark_item, null, new String[0], new int[0]);
        this.mListView.setEmptyView(this.mEmptyView);
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        this.mAdapter.changeCursor(this.mCursor);
        this.mListView.setOnItemClickListener(this);
        registerForContextMenu(this.mListView);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (this.mAdapter != null) {
            this.mAdapter.changeCursor(null);
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, 1, 0, R.string.delete_all).setIcon(android.R.drawable.ic_menu_delete);
        return LOG;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 1) {
            this.mBookmark.deleteAll();
            return LOG;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private class BookmarkAdapter extends SimpleCursorAdapter {
        public BookmarkAdapter(Context context, int i, Cursor cursor, String[] strArr, int[] iArr) {
            super(context, i, cursor, strArr, iArr);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
            View viewNewView = super.newView(context, cursor, viewGroup);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.mTitleView = (TextView) viewNewView.findViewById(R.id.title);
            viewHolder.mDataView = (TextView) viewNewView.findViewById(R.id.data);
            viewNewView.setTag(viewHolder);
            return viewNewView;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.mId = cursor.getLong(0);
            viewHolder.mTitle = cursor.getString(2);
            viewHolder.mData = cursor.getString(1);
            viewHolder.mMimetype = cursor.getString(4);
            viewHolder.mTitleView.setText(viewHolder.mTitle);
            viewHolder.mDataView.setText(viewHolder.mData);
        }

        @Override
        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
        }
    }

    private class ViewHolder {
        String mData;
        TextView mDataView;
        long mId;
        String mMimetype;
        String mTitle;
        TextView mTitleView;

        private ViewHolder() {
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
        ?? tag = view.getTag();
        if (tag instanceof ViewHolder) {
            finish();
            Intent intent = new Intent(this, (Class<?>) MovieActivity.class);
            intent.setFlags(67108864);
            String str = "video/*";
            if (tag.mMimetype != null && !"".equals(tag.mMimetype.trim())) {
                str = tag.mMimetype;
            }
            intent.setDataAndType(Uri.parse(tag.mData), str);
            startActivity(intent);
        }
        Log.v(TAG, "onItemClick(" + i + ", " + j + ")");
    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        super.onCreateContextMenu(contextMenu, view, contextMenuInfo);
        contextMenu.add(0, 2, 0, R.string.delete);
        contextMenu.add(0, 3, 0, R.string.edit);
    }

    @Override
    public boolean onContextItemSelected(MenuItem menuItem) {
        AdapterView.AdapterContextMenuInfo adapterContextMenuInfo = (AdapterView.AdapterContextMenuInfo) menuItem.getMenuInfo();
        switch (menuItem.getItemId()) {
            case 2:
                this.mBookmark.delete(adapterContextMenuInfo.id);
                return LOG;
            case 3:
                ?? tag = adapterContextMenuInfo.targetView.getTag();
                if (tag instanceof ViewHolder) {
                    showEditDialog(tag);
                } else {
                    Log.w(TAG, "wrong context item info " + adapterContextMenuInfo);
                }
                return LOG;
            default:
                return super.onContextItemSelected(menuItem);
        }
    }

    private void showEditDialog(final ViewHolder viewHolder) {
        Log.v(TAG, "showEditDialog(" + viewHolder + ")");
        if (viewHolder == null) {
            return;
        }
        View viewInflate = LayoutInflater.from(this).inflate(R.layout.m_bookmark_edit_dialog, (ViewGroup) null);
        final EditText editText = (EditText) viewInflate.findViewById(R.id.title);
        final EditText editText2 = (EditText) viewInflate.findViewById(R.id.data);
        editText.setText(viewHolder.mTitle);
        editText2.setText(viewHolder.mData);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.edit);
        builder.setView(viewInflate);
        builder.setIcon(R.drawable.m_ic_menu_display_bookmark);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                BookmarkActivity.this.mBookmark.update(viewHolder.mId, editText.getText().toString(), editText2.getText().toString(), 0);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, (DialogInterface.OnClickListener) null);
        AlertDialog alertDialogCreate = builder.create();
        alertDialogCreate.getWindow().setSoftInputMode(4);
        alertDialogCreate.setInverseBackgroundForced(LOG);
        alertDialogCreate.show();
    }
}
