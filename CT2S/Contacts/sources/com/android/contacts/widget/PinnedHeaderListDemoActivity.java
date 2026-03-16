package com.android.contacts.widget;

import android.app.ListActivity;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.contacts.R;
import com.android.contacts.common.list.PinnedHeaderListAdapter;

public class PinnedHeaderListDemoActivity extends ListActivity {
    private Handler mHandler = new Handler();

    public static final class TestPinnedHeaderListAdapter extends PinnedHeaderListAdapter {
        private String[] mHeaders;
        private int mPinnedHeaderCount;

        public TestPinnedHeaderListAdapter(Context context) {
            super(context);
            setPinnedPartitionHeadersEnabled(true);
        }

        public void setHeaders(String[] headers) {
            this.mHeaders = headers;
        }

        @Override
        protected View newHeaderView(Context context, int partition, Cursor cursor, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.list_section, (ViewGroup) null);
        }

        @Override
        protected void bindHeaderView(View view, int parition, Cursor cursor) {
            TextView headerText = (TextView) view.findViewById(R.id.header_text);
            headerText.setText(this.mHeaders[parition]);
        }

        @Override
        protected View newView(Context context, int partition, Cursor cursor, int position, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(android.R.layout.simple_list_item_1, (ViewGroup) null);
        }

        @Override
        protected void bindView(View v, int partition, Cursor cursor, int position) {
            TextView text = (TextView) v.findViewById(android.R.id.text1);
            text.setText(cursor.getString(1));
        }

        @Override
        public View getPinnedHeaderView(int viewIndex, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            View view = inflater.inflate(R.layout.list_section, parent, false);
            view.setFocusable(false);
            view.setEnabled(false);
            bindHeaderView(view, viewIndex, null);
            return view;
        }

        @Override
        public int getPinnedHeaderCount() {
            return this.mPinnedHeaderCount;
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.pinned_header_list_demo);
        final TestPinnedHeaderListAdapter adapter = new TestPinnedHeaderListAdapter(this);
        Bundle extras = getIntent().getExtras();
        int[] counts = extras.getIntArray("counts");
        String[] names = extras.getStringArray("names");
        boolean[] showIfEmpty = extras.getBooleanArray("showIfEmpty");
        extras.getBooleanArray("headers");
        int[] delays = extras.getIntArray("delays");
        if (counts == null || names == null || showIfEmpty == null || delays == null) {
            throw new IllegalArgumentException("Missing required extras");
        }
        adapter.setHeaders(names);
        for (int i = 0; i < counts.length; i++) {
            adapter.addPartition(showIfEmpty[i], names[i] != null);
            adapter.mPinnedHeaderCount = names.length;
        }
        setListAdapter(adapter);
        for (int i2 = 0; i2 < counts.length; i2++) {
            final int sectionId = i2;
            final Cursor cursor = makeCursor(names[i2], counts[i2]);
            this.mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    adapter.changeCursor(sectionId, cursor);
                }
            }, delays[i2]);
        }
    }

    private Cursor makeCursor(String name, int count) {
        MatrixCursor cursor = new MatrixCursor(new String[]{"_id", name});
        for (int i = 0; i < count; i++) {
            cursor.addRow(new Object[]{Integer.valueOf(i), name + "[" + i + "]"});
        }
        return cursor;
    }
}
