package com.android.documentsui;

import android.app.ActivityManager;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.text.format.Time;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.android.documentsui.DocumentsActivity;
import com.android.documentsui.ProviderExecutor;
import com.android.documentsui.model.DocumentInfo;
import com.android.documentsui.model.RootInfo;
import com.google.android.collect.Lists;
import java.util.ArrayList;
import java.util.List;

public class DirectoryFragment extends Fragment {
    private DocumentsAdapter mAdapter;
    private LoaderManager.LoaderCallbacks<DirectoryResult> mCallbacks;
    private AbsListView mCurrentView;
    private View mEmptyView;
    private GridView mGridView;
    private ListView mListView;
    private String mStateKey;
    private boolean mSvelteRecents;
    private Point mThumbSize;
    private int mType = 1;
    private int mLastMode = 0;
    private int mLastSortOrder = 0;
    private boolean mLastShowSize = false;
    private boolean mHideGridTitles = false;
    private final int mLoaderId = 42;
    private AdapterView.OnItemClickListener mItemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Cursor cursor = DirectoryFragment.this.mAdapter.getItem(position);
            if (cursor != null) {
                String docMimeType = DocumentInfo.getCursorString(cursor, "mime_type");
                int docFlags = DocumentInfo.getCursorInt(cursor, "flags");
                if (DirectoryFragment.this.isDocumentEnabled(docMimeType, docFlags)) {
                    DocumentInfo doc = DocumentInfo.fromDirectoryCursor(cursor);
                    ((DocumentsActivity) DirectoryFragment.this.getActivity()).onDocumentPicked(doc);
                }
            }
        }
    };
    private AbsListView.MultiChoiceModeListener mMultiListener = new AbsListView.MultiChoiceModeListener() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.mode_directory, menu);
            mode.setTitle(DirectoryFragment.this.getResources().getString(R.string.mode_selected_count, Integer.valueOf(DirectoryFragment.this.mCurrentView.getCheckedItemCount())));
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            DocumentsActivity.State state = DirectoryFragment.getDisplayState(DirectoryFragment.this);
            MenuItem open = menu.findItem(R.id.menu_open);
            MenuItem share = menu.findItem(R.id.menu_share);
            MenuItem delete = menu.findItem(R.id.menu_delete);
            boolean manageMode = state.action == 5;
            open.setVisible(manageMode ? false : true);
            share.setVisible(manageMode);
            delete.setVisible(manageMode);
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            SparseBooleanArray checked = DirectoryFragment.this.mCurrentView.getCheckedItemPositions();
            ArrayList<DocumentInfo> docs = Lists.newArrayList();
            int size = checked.size();
            for (int i = 0; i < size; i++) {
                if (checked.valueAt(i)) {
                    Cursor cursor = DirectoryFragment.this.mAdapter.getItem(checked.keyAt(i));
                    DocumentInfo doc = DocumentInfo.fromDirectoryCursor(cursor);
                    docs.add(doc);
                }
            }
            int id = item.getItemId();
            if (id == R.id.menu_open) {
                DocumentsActivity.get(DirectoryFragment.this).onDocumentsPicked(docs);
                mode.finish();
                return true;
            }
            if (id == R.id.menu_share) {
                DirectoryFragment.this.onShareDocuments(docs);
                mode.finish();
                return true;
            }
            if (id == R.id.menu_delete) {
                DirectoryFragment.this.onDeleteDocuments(docs);
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            if (checked) {
                boolean valid = false;
                Cursor cursor = DirectoryFragment.this.mAdapter.getItem(position);
                if (cursor != null) {
                    String docMimeType = DocumentInfo.getCursorString(cursor, "mime_type");
                    int docFlags = DocumentInfo.getCursorInt(cursor, "flags");
                    if (!"vnd.android.document/directory".equals(docMimeType)) {
                        valid = DirectoryFragment.this.isDocumentEnabled(docMimeType, docFlags);
                    }
                }
                if (!valid) {
                    DirectoryFragment.this.mCurrentView.setItemChecked(position, false);
                }
            }
            mode.setTitle(DirectoryFragment.this.getResources().getString(R.string.mode_selected_count, Integer.valueOf(DirectoryFragment.this.mCurrentView.getCheckedItemCount())));
        }
    };
    private AbsListView.RecyclerListener mRecycleListener = new AbsListView.RecyclerListener() {
        @Override
        public void onMovedToScrapHeap(View view) {
            ThumbnailAsyncTask oldTask;
            ImageView iconThumb = (ImageView) view.findViewById(R.id.icon_thumb);
            if (iconThumb != null && (oldTask = (ThumbnailAsyncTask) iconThumb.getTag()) != null) {
                oldTask.preempt();
                iconThumb.setTag(null);
            }
        }
    };

    public static void showNormal(FragmentManager fm, RootInfo root, DocumentInfo doc, int anim) {
        show(fm, 1, root, doc, null, anim);
    }

    public static void showSearch(FragmentManager fm, RootInfo root, String query, int anim) {
        show(fm, 2, root, null, query, anim);
    }

    public static void showRecentsOpen(FragmentManager fm, int anim) {
        show(fm, 3, null, null, null, anim);
    }

    private static void show(FragmentManager fm, int type, RootInfo root, DocumentInfo doc, String query, int anim) {
        Bundle args = new Bundle();
        args.putInt("type", type);
        args.putParcelable("root", root);
        args.putParcelable("doc", doc);
        args.putString("query", query);
        FragmentTransaction ft = fm.beginTransaction();
        switch (anim) {
            case 2:
                args.putBoolean("ignoreState", true);
                break;
            case 3:
                args.putBoolean("ignoreState", true);
                ft.setCustomAnimations(R.animator.dir_down, R.animator.dir_frozen);
                break;
            case 4:
                ft.setCustomAnimations(R.animator.dir_frozen, R.animator.dir_up);
                break;
        }
        DirectoryFragment fragment = new DirectoryFragment();
        fragment.setArguments(args);
        ft.replace(R.id.container_directory, fragment);
        ft.commitAllowingStateLoss();
    }

    private static String buildStateKey(RootInfo root, DocumentInfo doc) {
        StringBuilder builder = new StringBuilder();
        builder.append(root != null ? root.authority : "null").append(';');
        builder.append(root != null ? root.rootId : "null").append(';');
        builder.append(doc != null ? doc.documentId : "null");
        return builder.toString();
    }

    public static DirectoryFragment get(FragmentManager fm) {
        return (DirectoryFragment) fm.findFragmentById(R.id.container_directory);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Context context = inflater.getContext();
        Resources res = context.getResources();
        View view = inflater.inflate(R.layout.fragment_directory, container, false);
        this.mEmptyView = view.findViewById(android.R.id.empty);
        this.mListView = (ListView) view.findViewById(R.id.list);
        this.mListView.setOnItemClickListener(this.mItemListener);
        this.mListView.setMultiChoiceModeListener(this.mMultiListener);
        this.mListView.setRecyclerListener(this.mRecycleListener);
        Drawable divider = this.mListView.getDivider();
        boolean insetLeft = res.getBoolean(R.bool.list_divider_inset_left);
        int insetSize = res.getDimensionPixelSize(R.dimen.list_divider_inset);
        if (insetLeft) {
            this.mListView.setDivider(new InsetDrawable(divider, insetSize, 0, 0, 0));
        } else {
            this.mListView.setDivider(new InsetDrawable(divider, 0, 0, insetSize, 0));
        }
        this.mGridView = (GridView) view.findViewById(R.id.grid);
        this.mGridView.setOnItemClickListener(this.mItemListener);
        this.mGridView.setMultiChoiceModeListener(this.mMultiListener);
        this.mGridView.setRecyclerListener(this.mRecycleListener);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ViewGroup target = this.mListView.getAdapter() != null ? this.mListView : this.mGridView;
        int count = target.getChildCount();
        for (int i = 0; i < count; i++) {
            View view = target.getChildAt(i);
            this.mRecycleListener.onMovedToScrapHeap(view);
        }
        this.mListView.setChoiceMode(0);
        this.mGridView.setChoiceMode(0);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        final Context context = getActivity();
        final DocumentsActivity.State state = getDisplayState(this);
        final RootInfo root = (RootInfo) getArguments().getParcelable("root");
        final DocumentInfo doc = (DocumentInfo) getArguments().getParcelable("doc");
        this.mAdapter = new DocumentsAdapter();
        this.mType = getArguments().getInt("type");
        this.mStateKey = buildStateKey(root, doc);
        if (this.mType == 3) {
            this.mHideGridTitles = MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, state.acceptMimes);
        } else {
            this.mHideGridTitles = doc != null && doc.isGridTitlesHidden();
        }
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        this.mSvelteRecents = am.isLowRamDevice() && this.mType == 3;
        this.mCallbacks = new LoaderManager.LoaderCallbacks<DirectoryResult>() {
            @Override
            public Loader<DirectoryResult> onCreateLoader(int id, Bundle args) {
                String query = DirectoryFragment.this.getArguments().getString("query");
                switch (DirectoryFragment.this.mType) {
                    case 1:
                        Uri contentsUri = DocumentsContract.buildChildDocumentsUri(doc.authority, doc.documentId);
                        if (state.action == 5) {
                            contentsUri = DocumentsContract.setManageMode(contentsUri);
                        }
                        return new DirectoryLoader(context, DirectoryFragment.this.mType, root, doc, contentsUri, state.userSortOrder);
                    case 2:
                        Uri contentsUri2 = DocumentsContract.buildSearchDocumentsUri(root.authority, root.rootId, query);
                        if (state.action == 5) {
                            contentsUri2 = DocumentsContract.setManageMode(contentsUri2);
                        }
                        return new DirectoryLoader(context, DirectoryFragment.this.mType, root, doc, contentsUri2, state.userSortOrder);
                    case 3:
                        RootsCache roots = DocumentsApplication.getRootsCache(context);
                        return new RecentLoader(context, roots, state);
                    default:
                        throw new IllegalStateException("Unknown type " + DirectoryFragment.this.mType);
                }
            }

            @Override
            public void onLoadFinished(Loader<DirectoryResult> loader, DirectoryResult result) {
                if (DirectoryFragment.this.isAdded()) {
                    DirectoryFragment.this.mAdapter.swapResult(result);
                    if (result.mode != 0) {
                        state.derivedMode = result.mode;
                    }
                    state.derivedSortOrder = result.sortOrder;
                    ((DocumentsActivity) context).onStateChanged();
                    DirectoryFragment.this.updateDisplayState();
                    if (DirectoryFragment.this.mType == 3 && DirectoryFragment.this.mAdapter.isEmpty() && !state.stackTouched) {
                        ((DocumentsActivity) context).setRootsDrawerOpen(true);
                    }
                    SparseArray<Parcelable> container = state.dirState.remove(DirectoryFragment.this.mStateKey);
                    if (container == null || DirectoryFragment.this.getArguments().getBoolean("ignoreState", false)) {
                        if (DirectoryFragment.this.mLastSortOrder != state.derivedSortOrder) {
                            DirectoryFragment.this.mListView.smoothScrollToPosition(0);
                            DirectoryFragment.this.mGridView.smoothScrollToPosition(0);
                        }
                    } else {
                        DirectoryFragment.this.getView().restoreHierarchyState(container);
                    }
                    DirectoryFragment.this.mLastSortOrder = state.derivedSortOrder;
                }
            }

            @Override
            public void onLoaderReset(Loader<DirectoryResult> loader) {
                DirectoryFragment.this.mAdapter.swapResult(null);
            }
        };
        getLoaderManager().restartLoader(42, null, this.mCallbacks);
        updateDisplayState();
    }

    @Override
    public void onStop() {
        super.onStop();
        SparseArray<Parcelable> container = new SparseArray<>();
        getView().saveHierarchyState(container);
        DocumentsActivity.State state = getDisplayState(this);
        state.dirState.put(this.mStateKey, container);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDisplayState();
    }

    public void onDisplayStateChanged() {
        updateDisplayState();
    }

    public void onUserSortOrderChanged() {
        getLoaderManager().restartLoader(42, null, this.mCallbacks);
    }

    public void onUserModeChanged() {
        final ContentResolver resolver = getActivity().getContentResolver();
        DocumentsActivity.State state = getDisplayState(this);
        RootInfo root = (RootInfo) getArguments().getParcelable("root");
        DocumentInfo doc = (DocumentInfo) getArguments().getParcelable("doc");
        if (root != null && doc != null) {
            final Uri stateUri = RecentsProvider.buildState(root.authority, root.rootId, doc.documentId);
            final ContentValues values = new ContentValues();
            values.put("mode", Integer.valueOf(state.userMode));
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... params) {
                    resolver.insert(stateUri, values);
                    return null;
                }
            }.execute(new Void[0]);
        }
        state.derivedMode = state.userMode;
        ((DocumentsActivity) getActivity()).onStateChanged();
        updateDisplayState();
    }

    private void updateDisplayState() {
        int choiceMode;
        int thumbSize;
        DocumentsActivity.State state = getDisplayState(this);
        if (this.mLastMode != state.derivedMode || this.mLastShowSize != state.showSize) {
            this.mLastMode = state.derivedMode;
            this.mLastShowSize = state.showSize;
            this.mListView.setVisibility(state.derivedMode == 1 ? 0 : 8);
            this.mGridView.setVisibility(state.derivedMode == 2 ? 0 : 8);
            if (state.allowMultiple) {
                choiceMode = 3;
            } else {
                choiceMode = 0;
            }
            if (state.derivedMode == 2) {
                thumbSize = getResources().getDimensionPixelSize(R.dimen.grid_width);
                this.mListView.setAdapter((ListAdapter) null);
                this.mListView.setChoiceMode(0);
                this.mGridView.setAdapter((ListAdapter) this.mAdapter);
                this.mGridView.setColumnWidth(getResources().getDimensionPixelSize(R.dimen.grid_width));
                this.mGridView.setNumColumns(-1);
                this.mGridView.setChoiceMode(choiceMode);
                this.mCurrentView = this.mGridView;
            } else if (state.derivedMode == 1) {
                thumbSize = getResources().getDimensionPixelSize(R.dimen.icon_size);
                this.mGridView.setAdapter((ListAdapter) null);
                this.mGridView.setChoiceMode(0);
                this.mListView.setAdapter((ListAdapter) this.mAdapter);
                this.mListView.setChoiceMode(choiceMode);
                this.mCurrentView = this.mListView;
            } else {
                throw new IllegalStateException("Unknown state " + state.derivedMode);
            }
            this.mThumbSize = new Point(thumbSize, thumbSize);
        }
    }

    private void onShareDocuments(List<DocumentInfo> docs) {
        Intent intent;
        if (docs.size() == 1) {
            DocumentInfo doc = docs.get(0);
            intent = new Intent("android.intent.action.SEND");
            intent.addFlags(1);
            intent.addCategory("android.intent.category.DEFAULT");
            intent.setType(doc.mimeType);
            intent.putExtra("android.intent.extra.STREAM", doc.derivedUri);
        } else if (docs.size() > 1) {
            intent = new Intent("android.intent.action.SEND_MULTIPLE");
            intent.addFlags(1);
            intent.addCategory("android.intent.category.DEFAULT");
            ArrayList<String> mimeTypes = Lists.newArrayList();
            ArrayList<Uri> uris = Lists.newArrayList();
            for (DocumentInfo doc2 : docs) {
                mimeTypes.add(doc2.mimeType);
                uris.add(doc2.derivedUri);
            }
            intent.setType(findCommonMimeType(mimeTypes));
            intent.putParcelableArrayListExtra("android.intent.extra.STREAM", uris);
        } else {
            return;
        }
        startActivity(Intent.createChooser(intent, getActivity().getText(R.string.share_via)));
    }

    private void onDeleteDocuments(List<DocumentInfo> docs) {
        Context context = getActivity();
        ContentResolver resolver = context.getContentResolver();
        boolean hadTrouble = false;
        for (DocumentInfo doc : docs) {
            if (!doc.isDeleteSupported()) {
                Log.w("Documents", "Skipping " + doc);
                hadTrouble = true;
            } else {
                ContentProviderClient client = null;
                try {
                    client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, doc.derivedUri.getAuthority());
                    DocumentsContract.deleteDocument(client, doc.derivedUri);
                } catch (Exception e) {
                    Log.w("Documents", "Failed to delete " + doc);
                    hadTrouble = true;
                } finally {
                    ContentProviderClient.releaseQuietly(client);
                }
            }
        }
        if (hadTrouble) {
            Toast.makeText(context, R.string.toast_failed_delete, 0).show();
        }
    }

    private static DocumentsActivity.State getDisplayState(Fragment fragment) {
        return ((DocumentsActivity) fragment.getActivity()).getDisplayState();
    }

    private static abstract class Footer {
        private final int mItemViewType;

        public abstract View getView(View view, ViewGroup viewGroup);

        public Footer(int itemViewType) {
            this.mItemViewType = itemViewType;
        }

        public int getItemViewType() {
            return this.mItemViewType;
        }
    }

    private class LoadingFooter extends Footer {
        public LoadingFooter() {
            super(1);
        }

        @Override
        public View getView(View convertView, ViewGroup parent) {
            Context context = parent.getContext();
            DocumentsActivity.State state = DirectoryFragment.getDisplayState(DirectoryFragment.this);
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                if (state.derivedMode == 1) {
                    return inflater.inflate(R.layout.item_loading_list, parent, false);
                }
                if (state.derivedMode == 2) {
                    return inflater.inflate(R.layout.item_loading_grid, parent, false);
                }
                throw new IllegalStateException();
            }
            return convertView;
        }
    }

    private class MessageFooter extends Footer {
        private final int mIcon;
        private final String mMessage;

        public MessageFooter(int itemViewType, int icon, String message) {
            super(itemViewType);
            this.mIcon = icon;
            this.mMessage = message;
        }

        @Override
        public View getView(View convertView, ViewGroup parent) {
            Context context = parent.getContext();
            DocumentsActivity.State state = DirectoryFragment.getDisplayState(DirectoryFragment.this);
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                if (state.derivedMode == 1) {
                    convertView = inflater.inflate(R.layout.item_message_list, parent, false);
                } else if (state.derivedMode == 2) {
                    convertView = inflater.inflate(R.layout.item_message_grid, parent, false);
                } else {
                    throw new IllegalStateException();
                }
            }
            ImageView icon = (ImageView) convertView.findViewById(android.R.id.icon);
            TextView title = (TextView) convertView.findViewById(android.R.id.title);
            icon.setImageResource(this.mIcon);
            title.setText(this.mMessage);
            return convertView;
        }
    }

    private class DocumentsAdapter extends BaseAdapter {
        private Cursor mCursor;
        private int mCursorCount;
        private List<Footer> mFooters;

        private DocumentsAdapter() {
            this.mFooters = Lists.newArrayList();
        }

        public void swapResult(DirectoryResult result) {
            this.mCursor = result != null ? result.cursor : null;
            this.mCursorCount = this.mCursor != null ? this.mCursor.getCount() : 0;
            this.mFooters.clear();
            Bundle extras = this.mCursor != null ? this.mCursor.getExtras() : null;
            if (extras != null) {
                String info = extras.getString("info");
                if (info != null) {
                    this.mFooters.add(DirectoryFragment.this.new MessageFooter(2, R.drawable.ic_dialog_info, info));
                }
                String error = extras.getString("error");
                if (error != null) {
                    this.mFooters.add(DirectoryFragment.this.new MessageFooter(3, R.drawable.ic_dialog_alert, error));
                }
                if (extras.getBoolean("loading", false)) {
                    this.mFooters.add(DirectoryFragment.this.new LoadingFooter());
                }
            }
            if (result != null && result.exception != null) {
                this.mFooters.add(DirectoryFragment.this.new MessageFooter(3, R.drawable.ic_dialog_alert, DirectoryFragment.this.getString(R.string.query_error)));
            }
            if (isEmpty()) {
                DirectoryFragment.this.mEmptyView.setVisibility(0);
            } else {
                DirectoryFragment.this.mEmptyView.setVisibility(8);
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position < this.mCursorCount) {
                return getDocumentView(position, convertView, parent);
            }
            View convertView2 = this.mFooters.get(position - this.mCursorCount).getView(convertView, parent);
            convertView2.setEnabled(false);
            return convertView2;
        }

        private View getDocumentView(int position, View convertView, ViewGroup parent) {
            Context context = parent.getContext();
            DocumentsActivity.State state = DirectoryFragment.getDisplayState(DirectoryFragment.this);
            RootsCache roots = DocumentsApplication.getRootsCache(context);
            ThumbnailCache thumbs = DocumentsApplication.getThumbnailsCache(context, DirectoryFragment.this.mThumbSize);
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                if (state.derivedMode == 1) {
                    convertView = inflater.inflate(R.layout.item_doc_list, parent, false);
                } else if (state.derivedMode == 2) {
                    convertView = inflater.inflate(R.layout.item_doc_grid, parent, false);
                } else {
                    throw new IllegalStateException();
                }
            }
            Cursor cursor = getItem(position);
            String docAuthority = DocumentInfo.getCursorString(cursor, "android:authority");
            String docRootId = DocumentInfo.getCursorString(cursor, "android:rootId");
            String docId = DocumentInfo.getCursorString(cursor, "document_id");
            String docMimeType = DocumentInfo.getCursorString(cursor, "mime_type");
            String docDisplayName = DocumentInfo.getCursorString(cursor, "_display_name");
            long docLastModified = DocumentInfo.getCursorLong(cursor, "last_modified");
            int docIcon = DocumentInfo.getCursorInt(cursor, "icon");
            int docFlags = DocumentInfo.getCursorInt(cursor, "flags");
            String docSummary = DocumentInfo.getCursorString(cursor, "summary");
            long docSize = DocumentInfo.getCursorLong(cursor, "_size");
            View line1 = convertView.findViewById(R.id.line1);
            View line2 = convertView.findViewById(R.id.line2);
            ImageView iconMime = (ImageView) convertView.findViewById(R.id.icon_mime);
            ImageView iconThumb = (ImageView) convertView.findViewById(R.id.icon_thumb);
            TextView title = (TextView) convertView.findViewById(android.R.id.title);
            ImageView icon1 = (ImageView) convertView.findViewById(android.R.id.icon1);
            ImageView icon2 = (ImageView) convertView.findViewById(android.R.id.icon2);
            TextView summary = (TextView) convertView.findViewById(android.R.id.summary);
            TextView date = (TextView) convertView.findViewById(R.id.date);
            TextView size = (TextView) convertView.findViewById(R.id.size);
            ThumbnailAsyncTask oldTask = (ThumbnailAsyncTask) iconThumb.getTag();
            if (oldTask != null) {
                oldTask.preempt();
                iconThumb.setTag(null);
            }
            iconMime.animate().cancel();
            iconThumb.animate().cancel();
            boolean supportsThumbnail = (docFlags & 1) != 0;
            boolean allowThumbnail = state.derivedMode == 2 || MimePredicate.mimeMatches(MimePredicate.VISUAL_MIMES, docMimeType);
            boolean showThumbnail = supportsThumbnail && allowThumbnail && !DirectoryFragment.this.mSvelteRecents;
            boolean enabled = DirectoryFragment.this.isDocumentEnabled(docMimeType, docFlags);
            float iconAlpha = (state.derivedMode != 1 || enabled) ? 1.0f : 0.5f;
            boolean cacheHit = false;
            if (showThumbnail) {
                Uri uri = DocumentsContract.buildDocumentUri(docAuthority, docId);
                Bitmap cachedResult = thumbs.get(uri);
                if (cachedResult != null) {
                    iconThumb.setImageBitmap(cachedResult);
                    cacheHit = true;
                } else {
                    iconThumb.setImageDrawable(null);
                    ThumbnailAsyncTask task = new ThumbnailAsyncTask(uri, iconMime, iconThumb, DirectoryFragment.this.mThumbSize, iconAlpha);
                    iconThumb.setTag(task);
                    ProviderExecutor.forAuthority(docAuthority).execute(task, new Uri[0]);
                }
            }
            if (cacheHit) {
                iconMime.setAlpha(0.0f);
                iconMime.setImageDrawable(null);
                iconThumb.setAlpha(1.0f);
            } else {
                iconMime.setAlpha(1.0f);
                iconThumb.setAlpha(0.0f);
                iconThumb.setImageDrawable(null);
                if (docIcon != 0) {
                    iconMime.setImageDrawable(IconUtils.loadPackageIcon(context, docAuthority, docIcon));
                } else {
                    iconMime.setImageDrawable(IconUtils.loadMimeIcon(context, docMimeType, docAuthority, docId, state.derivedMode));
                }
            }
            boolean hasLine1 = false;
            boolean hasLine2 = false;
            boolean hideTitle = state.derivedMode == 2 && DirectoryFragment.this.mHideGridTitles;
            if (!hideTitle) {
                title.setText(docDisplayName);
                hasLine1 = true;
            }
            Drawable iconDrawable = null;
            if (DirectoryFragment.this.mType == 3) {
                RootInfo root = roots.getRootBlocking(docAuthority, docRootId);
                if (state.derivedMode == 2) {
                    iconDrawable = root.loadGridIcon(context);
                } else {
                    iconDrawable = root.loadIcon(context);
                }
                if (summary != null) {
                    boolean alwaysShowSummary = DirectoryFragment.this.getResources().getBoolean(R.bool.always_show_summary);
                    if (alwaysShowSummary) {
                        summary.setText(root.getDirectoryString());
                        summary.setVisibility(0);
                        hasLine2 = true;
                    } else if (iconDrawable != null && roots.isIconUniqueBlocking(root)) {
                        summary.setVisibility(4);
                    } else {
                        summary.setText(root.getDirectoryString());
                        summary.setVisibility(0);
                        summary.setTextAlignment(3);
                        hasLine2 = true;
                    }
                }
            } else {
                if ("vnd.android.document/directory".equals(docMimeType) && state.derivedMode == 2 && showThumbnail) {
                    iconDrawable = IconUtils.applyTintAttr(context, R.drawable.ic_doc_folder, android.R.attr.textColorPrimaryInverse);
                }
                if (summary != null) {
                    if (docSummary != null) {
                        summary.setText(docSummary);
                        summary.setVisibility(0);
                        hasLine2 = true;
                    } else {
                        summary.setVisibility(4);
                    }
                }
            }
            if (icon1 != null) {
                icon1.setVisibility(8);
            }
            if (icon2 != null) {
                icon2.setVisibility(8);
            }
            if (iconDrawable != null) {
                if (hasLine1) {
                    icon1.setVisibility(0);
                    icon1.setImageDrawable(iconDrawable);
                } else {
                    icon2.setVisibility(0);
                    icon2.setImageDrawable(iconDrawable);
                }
            }
            if (docLastModified != -1) {
                date.setText(DirectoryFragment.formatTime(context, docLastModified));
                hasLine2 = true;
            } else {
                date.setText((CharSequence) null);
            }
            if (state.showSize) {
                size.setVisibility(0);
                if ("vnd.android.document/directory".equals(docMimeType) || docSize == -1) {
                    size.setText((CharSequence) null);
                } else {
                    size.setText(Formatter.formatFileSize(context, docSize));
                    hasLine2 = true;
                }
            } else {
                size.setVisibility(8);
            }
            if (line1 != null) {
                line1.setVisibility(hasLine1 ? 0 : 8);
            }
            if (line2 != null) {
                line2.setVisibility(hasLine2 ? 0 : 8);
            }
            DirectoryFragment.this.setEnabledRecursive(convertView, enabled);
            iconMime.setAlpha(iconAlpha);
            iconThumb.setAlpha(iconAlpha);
            if (icon1 != null) {
                icon1.setAlpha(iconAlpha);
            }
            if (icon2 != null) {
                icon2.setAlpha(iconAlpha);
            }
            return convertView;
        }

        @Override
        public int getCount() {
            return this.mCursorCount + this.mFooters.size();
        }

        @Override
        public Cursor getItem(int position) {
            if (position >= this.mCursorCount) {
                return null;
            }
            this.mCursor.moveToPosition(position);
            return this.mCursor;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getViewTypeCount() {
            return 4;
        }

        @Override
        public int getItemViewType(int position) {
            if (position < this.mCursorCount) {
                return 0;
            }
            return this.mFooters.get(position - this.mCursorCount).getItemViewType();
        }
    }

    private static class ThumbnailAsyncTask extends AsyncTask<Uri, Void, Bitmap> implements ProviderExecutor.Preemptable {
        private final ImageView mIconMime;
        private final ImageView mIconThumb;
        private final CancellationSignal mSignal = new CancellationSignal();
        private final float mTargetAlpha;
        private final Point mThumbSize;
        private final Uri mUri;

        public ThumbnailAsyncTask(Uri uri, ImageView iconMime, ImageView iconThumb, Point thumbSize, float targetAlpha) {
            this.mUri = uri;
            this.mIconMime = iconMime;
            this.mIconThumb = iconThumb;
            this.mThumbSize = thumbSize;
            this.mTargetAlpha = targetAlpha;
        }

        @Override
        public void preempt() {
            cancel(false);
            this.mSignal.cancel();
        }

        @Override
        protected Bitmap doInBackground(Uri... params) {
            if (isCancelled()) {
                return null;
            }
            Context context = this.mIconThumb.getContext();
            ContentResolver resolver = context.getContentResolver();
            ContentProviderClient client = null;
            Bitmap result = null;
            try {
                client = DocumentsApplication.acquireUnstableProviderOrThrow(resolver, this.mUri.getAuthority());
                result = DocumentsContract.getDocumentThumbnail(client, this.mUri, this.mThumbSize, this.mSignal);
                if (result != null) {
                    ThumbnailCache thumbs = DocumentsApplication.getThumbnailsCache(context, this.mThumbSize);
                    thumbs.put(this.mUri, result);
                }
                return result;
            } catch (Exception e) {
                if (!(e instanceof OperationCanceledException)) {
                    Log.w("Documents", "Failed to load thumbnail for " + this.mUri + ": " + e);
                }
                return result;
            } finally {
                ContentProviderClient.releaseQuietly(client);
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (this.mIconThumb.getTag() == this && result != null) {
                this.mIconThumb.setTag(null);
                this.mIconThumb.setImageBitmap(result);
                this.mIconMime.setAlpha(this.mTargetAlpha);
                this.mIconMime.animate().alpha(0.0f).start();
                this.mIconThumb.setAlpha(0.0f);
                this.mIconThumb.animate().alpha(this.mTargetAlpha).start();
            }
        }
    }

    private static String formatTime(Context context, long when) {
        int flags;
        Time then = new Time();
        then.set(when);
        Time now = new Time();
        now.setToNow();
        if (then.year != now.year) {
            flags = 526848 | 20;
        } else if (then.yearDay != now.yearDay) {
            flags = 526848 | 16;
        } else {
            flags = 526848 | 1;
        }
        return DateUtils.formatDateTime(context, when, flags);
    }

    private String findCommonMimeType(List<String> mimeTypes) {
        String[] commonType = mimeTypes.get(0).split("/");
        if (commonType.length != 2) {
            return "*/*";
        }
        int i = 1;
        while (true) {
            if (i >= mimeTypes.size()) {
                break;
            }
            String[] type = mimeTypes.get(i).split("/");
            if (type.length == 2) {
                if (!commonType[1].equals(type[1])) {
                    commonType[1] = "*";
                }
                if (!commonType[0].equals(type[0])) {
                    commonType[0] = "*";
                    commonType[1] = "*";
                    break;
                }
            }
            i++;
        }
        return commonType[0] + "/" + commonType[1];
    }

    private void setEnabledRecursive(View v, boolean enabled) {
        if (v != null && v.isEnabled() != enabled) {
            v.setEnabled(enabled);
            if (v instanceof ViewGroup) {
                ViewGroup vg = (ViewGroup) v;
                for (int i = vg.getChildCount() - 1; i >= 0; i--) {
                    setEnabledRecursive(vg.getChildAt(i), enabled);
                }
            }
        }
    }

    private boolean isDocumentEnabled(String docMimeType, int docFlags) {
        DocumentsActivity.State state = getDisplayState(this);
        if ("vnd.android.document/directory".equals(docMimeType)) {
            return true;
        }
        if (state.action == 2 && (docFlags & 2) == 0) {
            return false;
        }
        return MimePredicate.mimeMatches(state.acceptMimes, docMimeType);
    }
}
