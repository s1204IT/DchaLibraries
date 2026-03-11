package com.android.browser;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Property;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import com.android.browser.provider.SnapshotProvider;
import java.text.DateFormat;
import java.util.Date;

public class BrowserSnapshotPage extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener {
    private static final String[] PROJECTION = {"_id", "title", "viewstate_size", "thumbnail", "favicon", "url", "date_created", "viewstate_path", "progress", "job_id"};
    SnapshotAdapter mAdapter;
    long mAnimateId;
    CombinedBookmarksCallbacks mCallback;
    View mEmpty;
    GridView mGrid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mCallback = (CombinedBookmarksCallbacks) getActivity();
        this.mAnimateId = getArguments().getLong("animate_id");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.snapshots, container, false);
        this.mEmpty = view.findViewById(android.R.id.empty);
        this.mGrid = (GridView) view.findViewById(R.id.grid);
        setupGrid(inflater);
        getLoaderManager().initLoader(1, null, this);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getLoaderManager().destroyLoader(1);
        if (this.mAdapter == null) {
            return;
        }
        this.mAdapter.changeCursor(null);
        this.mAdapter = null;
    }

    void setupGrid(LayoutInflater inflater) {
        View item = inflater.inflate(R.layout.snapshot_item, (ViewGroup) this.mGrid, false);
        int mspec = View.MeasureSpec.makeMeasureSpec(0, 0);
        item.measure(mspec, mspec);
        int width = item.getMeasuredWidth();
        this.mGrid.setColumnWidth(width);
        this.mGrid.setOnItemClickListener(this);
        this.mGrid.setOnCreateContextMenuListener(this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == 1) {
            return new CursorLoader(getActivity(), SnapshotProvider.Snapshots.CONTENT_URI, PROJECTION, null, null, "date_created DESC");
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (loader.getId() != 1) {
            return;
        }
        if (this.mAdapter == null) {
            this.mAdapter = new SnapshotAdapter(getActivity(), data);
            this.mGrid.setAdapter((ListAdapter) this.mAdapter);
        } else {
            this.mAdapter.changeCursor(data);
        }
        if (this.mAnimateId > 0) {
            this.mAdapter.animateIn(this.mAnimateId);
            this.mAnimateId = 0L;
            getArguments().remove("animate_id");
        }
        boolean empty = this.mAdapter.isEmpty();
        this.mGrid.setVisibility(empty ? 8 : 0);
        this.mEmpty.setVisibility(empty ? 0 : 8);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.snapshots_context, menu);
        BookmarkItem header = new BookmarkItem(getActivity());
        header.setEnableScrolling(true);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        populateBookmarkItem(this.mAdapter.getItem(info.position), header);
        menu.setHeaderView(header);
    }

    private void populateBookmarkItem(Cursor cursor, BookmarkItem item) {
        item.setName(cursor.getString(1));
        item.setUrl(cursor.getString(5));
        item.setFavicon(getBitmap(cursor, 4));
    }

    static Bitmap getBitmap(Cursor cursor, int columnIndex) {
        byte[] data = cursor.getBlob(columnIndex);
        if (data == null) {
            return null;
        }
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    View getTargetView(ContextMenu.ContextMenuInfo menuInfo) {
        if (menuInfo instanceof AdapterView.AdapterContextMenuInfo) {
            return ((AdapterView.AdapterContextMenuInfo) menuInfo).targetView;
        }
        if (menuInfo instanceof ExpandableListView.ExpandableListContextMenuInfo) {
            return ((ExpandableListView.ExpandableListContextMenuInfo) menuInfo).targetView;
        }
        return null;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.delete_context_menu_id) {
            ContextMenu.ContextMenuInfo menuInfo = item.getMenuInfo();
            if (menuInfo == null) {
                return false;
            }
            View targetView = getTargetView(menuInfo);
            if ((targetView instanceof HistoryItem) || !(item.getMenuInfo() instanceof AdapterView.AdapterContextMenuInfo)) {
                return false;
            }
            deleteSnapshot(((AdapterView.AdapterContextMenuInfo) item.getMenuInfo()).id);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    void deleteSnapshot(long id) {
        final Uri uri = ContentUris.withAppendedId(SnapshotProvider.Snapshots.CONTENT_URI, id);
        final ContentResolver cr = getActivity().getContentResolver();
        new Thread() {
            @Override
            public void run() {
                cr.delete(uri, null, null);
            }
        }.start();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Cursor c = this.mAdapter.getItem(position);
        String title = c.getString(1);
        String url = "file://" + c.getString(7);
        int job = c.getInt(9);
        if (job == -1) {
            url = c.getString(5);
        }
        this.mCallback.openSnapshot(id, title, url);
    }

    private static class SnapshotAdapter extends ResourceCursorAdapter {
        private long mAnimateId;
        private AnimatorSet mAnimation;
        private View mAnimationTarget;

        public SnapshotAdapter(Context context, Cursor c) {
            super(context, R.layout.snapshot_item, c, 0);
            this.mAnimation = new AnimatorSet();
            this.mAnimation.playTogether(ObjectAnimator.ofFloat((Object) null, (Property<Object, Float>) View.SCALE_X, 0.0f, 1.0f), ObjectAnimator.ofFloat((Object) null, (Property<Object, Float>) View.SCALE_Y, 0.0f, 1.0f));
            this.mAnimation.setStartDelay(100L);
            this.mAnimation.setDuration(400L);
            this.mAnimation.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    SnapshotAdapter.this.mAnimateId = 0L;
                    SnapshotAdapter.this.mAnimationTarget = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }
            });
        }

        public void animateIn(long id) {
            this.mAnimateId = id;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            long id = cursor.getLong(0);
            if (id == this.mAnimateId) {
                if (this.mAnimationTarget != view) {
                    float scale = 0.0f;
                    if (this.mAnimationTarget != null) {
                        scale = this.mAnimationTarget.getScaleX();
                        this.mAnimationTarget.setScaleX(1.0f);
                        this.mAnimationTarget.setScaleY(1.0f);
                    }
                    view.setScaleX(scale);
                    view.setScaleY(scale);
                }
                this.mAnimation.setTarget(view);
                this.mAnimationTarget = view;
                if (!this.mAnimation.isRunning()) {
                    this.mAnimation.start();
                }
            }
            ImageView thumbnail = (ImageView) view.findViewById(R.id.thumb);
            byte[] thumbBlob = cursor.getBlob(3);
            if (thumbBlob == null) {
                thumbnail.setImageResource(R.drawable.browser_thumbnail);
            } else {
                Bitmap thumbBitmap = BitmapFactory.decodeByteArray(thumbBlob, 0, thumbBlob.length);
                thumbnail.setImageBitmap(thumbBitmap);
            }
            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(cursor.getString(1));
            TextView size = (TextView) view.findViewById(R.id.size);
            if (size != null) {
                int stateLen = cursor.getInt(2);
                size.setText(String.format("%.2fMB", Float.valueOf((stateLen / 1024.0f) / 1024.0f)));
            }
            long timestamp = cursor.getLong(6);
            TextView date = (TextView) view.findViewById(R.id.date);
            DateFormat dateFormat = DateFormat.getDateInstance(3);
            date.setText(dateFormat.format(new Date(timestamp)));
            ProgressBar bar = (ProgressBar) view.findViewById(R.id.download_progress);
            int progress = cursor.getInt(8);
            bar.setProgress(progress);
            ImageView ground = (ImageView) view.findViewById(R.id.gray_foreground);
            if (bar.getProgress() < 100) {
                ground.setPadding(thumbnail.getPaddingStart(), thumbnail.getPaddingTop(), thumbnail.getPaddingEnd(), thumbnail.getPaddingBottom());
                ground.setVisibility(0);
            } else {
                ground.setVisibility(8);
            }
        }

        @Override
        public Cursor getItem(int position) {
            return (Cursor) super.getItem(position);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            Cursor c = getItem(position);
            int progress = c.getInt(8);
            if (progress < 100) {
                return false;
            }
            return true;
        }
    }
}
