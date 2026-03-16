package com.android.documentsui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.android.documentsui.DocumentsActivity;
import com.android.documentsui.model.DocumentStack;
import com.android.documentsui.model.DurableUtils;
import com.android.documentsui.model.RootInfo;
import com.google.android.collect.Lists;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import libcore.io.IoUtils;

public class RecentsCreateFragment extends Fragment {
    private DocumentStackAdapter mAdapter;
    private LoaderManager.LoaderCallbacks<List<DocumentStack>> mCallbacks;
    private View mEmptyView;
    private AdapterView.OnItemClickListener mItemListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            DocumentStack stack = RecentsCreateFragment.this.mAdapter.getItem(position);
            ((DocumentsActivity) RecentsCreateFragment.this.getActivity()).onStackPicked(stack);
        }
    };
    private ListView mListView;

    public static void show(FragmentManager fm) {
        RecentsCreateFragment fragment = new RecentsCreateFragment();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.container_directory, fragment);
        ft.commitAllowingStateLoss();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final Context context = inflater.getContext();
        View view = inflater.inflate(R.layout.fragment_directory, container, false);
        this.mEmptyView = view.findViewById(android.R.id.empty);
        this.mListView = (ListView) view.findViewById(R.id.list);
        this.mListView.setOnItemClickListener(this.mItemListener);
        this.mAdapter = new DocumentStackAdapter();
        this.mListView.setAdapter((ListAdapter) this.mAdapter);
        final RootsCache roots = DocumentsApplication.getRootsCache(context);
        final DocumentsActivity.State state = ((DocumentsActivity) getActivity()).getDisplayState();
        this.mCallbacks = new LoaderManager.LoaderCallbacks<List<DocumentStack>>() {
            @Override
            public Loader<List<DocumentStack>> onCreateLoader(int id, Bundle args) {
                return new RecentsCreateLoader(context, roots, state);
            }

            @Override
            public void onLoadFinished(Loader<List<DocumentStack>> loader, List<DocumentStack> data) {
                RecentsCreateFragment.this.mAdapter.swapStacks(data);
                if (RecentsCreateFragment.this.mAdapter.isEmpty() && !state.stackTouched) {
                    ((DocumentsActivity) context).setRootsDrawerOpen(true);
                }
            }

            @Override
            public void onLoaderReset(Loader<List<DocumentStack>> loader) {
                RecentsCreateFragment.this.mAdapter.swapStacks(null);
            }
        };
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        getLoaderManager().restartLoader(3, getArguments(), this.mCallbacks);
    }

    @Override
    public void onStop() {
        super.onStop();
        getLoaderManager().destroyLoader(3);
    }

    public static class RecentsCreateLoader extends UriDerivativeLoader<Uri, List<DocumentStack>> {
        private final RootsCache mRoots;
        private final DocumentsActivity.State mState;

        public RecentsCreateLoader(Context context, RootsCache roots, DocumentsActivity.State state) {
            super(context, RecentsProvider.buildRecent());
            this.mRoots = roots;
            this.mState = state;
        }

        @Override
        public List<DocumentStack> loadInBackground(Uri uri, CancellationSignal signal) {
            Collection<RootInfo> matchingRoots = this.mRoots.getMatchingRootsBlocking(this.mState);
            ArrayList<DocumentStack> result = Lists.newArrayList();
            ContentResolver resolver = getContext().getContentResolver();
            Cursor cursor = resolver.query(uri, null, null, null, "timestamp DESC", signal);
            while (cursor != null) {
                try {
                    if (!cursor.moveToNext()) {
                        break;
                    }
                    byte[] rawStack = cursor.getBlob(cursor.getColumnIndex("stack"));
                    try {
                        DocumentStack stack = new DocumentStack();
                        DurableUtils.readFromArray(rawStack, stack);
                        stack.updateRoot(matchingRoots);
                        result.add(stack);
                    } catch (IOException e) {
                        Log.w("Documents", "Failed to resolve stack: " + e);
                    }
                } finally {
                    IoUtils.closeQuietly(cursor);
                }
            }
            return result;
        }
    }

    private class DocumentStackAdapter extends BaseAdapter {
        private List<DocumentStack> mStacks;

        public DocumentStackAdapter() {
        }

        public void swapStacks(List<DocumentStack> stacks) {
            this.mStacks = stacks;
            if (isEmpty()) {
                RecentsCreateFragment.this.mEmptyView.setVisibility(0);
            } else {
                RecentsCreateFragment.this.mEmptyView.setVisibility(8);
            }
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Context context = parent.getContext();
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.item_doc_list, parent, false);
            }
            ImageView iconMime = (ImageView) convertView.findViewById(R.id.icon_mime);
            TextView title = (TextView) convertView.findViewById(android.R.id.title);
            View line2 = convertView.findViewById(R.id.line2);
            DocumentStack stack = getItem(position);
            iconMime.setImageDrawable(stack.root.loadIcon(context));
            Drawable crumb = context.getDrawable(R.drawable.ic_breadcrumb_arrow);
            crumb.setBounds(0, 0, crumb.getIntrinsicWidth(), crumb.getIntrinsicHeight());
            SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append((CharSequence) stack.root.title);
            for (int i = stack.size() - 2; i >= 0; i--) {
                RecentsCreateFragment.appendDrawable(builder, crumb);
                builder.append((CharSequence) stack.get(i).displayName);
            }
            title.setText(builder);
            title.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            if (line2 != null) {
                line2.setVisibility(8);
            }
            return convertView;
        }

        @Override
        public int getCount() {
            if (this.mStacks != null) {
                return this.mStacks.size();
            }
            return 0;
        }

        @Override
        public DocumentStack getItem(int position) {
            return this.mStacks.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }
    }

    private static void appendDrawable(SpannableStringBuilder b, Drawable d) {
        int length = b.length();
        b.append("〉");
        b.setSpan(new ImageSpan(d), length, b.length(), 33);
    }
}
