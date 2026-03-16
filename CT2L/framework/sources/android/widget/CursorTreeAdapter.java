package android.widget;

import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.net.ProxyInfo;
import android.os.Handler;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorFilter;

public abstract class CursorTreeAdapter extends BaseExpandableListAdapter implements Filterable, CursorFilter.CursorFilterClient {
    private boolean mAutoRequery;
    SparseArray<MyCursorHelper> mChildrenCursorHelpers;
    private Context mContext;
    CursorFilter mCursorFilter;
    FilterQueryProvider mFilterQueryProvider;
    MyCursorHelper mGroupCursorHelper;
    private Handler mHandler;

    protected abstract void bindChildView(View view, Context context, Cursor cursor, boolean z);

    protected abstract void bindGroupView(View view, Context context, Cursor cursor, boolean z);

    protected abstract Cursor getChildrenCursor(Cursor cursor);

    protected abstract View newChildView(Context context, Cursor cursor, boolean z, ViewGroup viewGroup);

    protected abstract View newGroupView(Context context, Cursor cursor, boolean z, ViewGroup viewGroup);

    public CursorTreeAdapter(Cursor cursor, Context context) {
        init(cursor, context, true);
    }

    public CursorTreeAdapter(Cursor cursor, Context context, boolean autoRequery) {
        init(cursor, context, autoRequery);
    }

    private void init(Cursor cursor, Context context, boolean autoRequery) {
        this.mContext = context;
        this.mHandler = new Handler();
        this.mAutoRequery = autoRequery;
        this.mGroupCursorHelper = new MyCursorHelper(cursor);
        this.mChildrenCursorHelpers = new SparseArray<>();
    }

    synchronized MyCursorHelper getChildrenCursorHelper(int groupPosition, boolean requestCursor) {
        MyCursorHelper myCursorHelper;
        MyCursorHelper cursorHelper = this.mChildrenCursorHelpers.get(groupPosition);
        if (cursorHelper != null) {
            myCursorHelper = cursorHelper;
        } else if (this.mGroupCursorHelper.moveTo(groupPosition) == null) {
            myCursorHelper = null;
        } else {
            Cursor cursor = getChildrenCursor(this.mGroupCursorHelper.getCursor());
            cursorHelper = new MyCursorHelper(cursor);
            this.mChildrenCursorHelpers.put(groupPosition, cursorHelper);
            myCursorHelper = cursorHelper;
        }
        return myCursorHelper;
    }

    public void setGroupCursor(Cursor cursor) {
        this.mGroupCursorHelper.changeCursor(cursor, false);
    }

    public void setChildrenCursor(int groupPosition, Cursor childrenCursor) {
        MyCursorHelper childrenCursorHelper = getChildrenCursorHelper(groupPosition, false);
        childrenCursorHelper.changeCursor(childrenCursor, false);
    }

    @Override
    public Cursor getChild(int groupPosition, int childPosition) {
        return getChildrenCursorHelper(groupPosition, true).moveTo(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return getChildrenCursorHelper(groupPosition, true).getId(childPosition);
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        MyCursorHelper helper = getChildrenCursorHelper(groupPosition, true);
        if (!this.mGroupCursorHelper.isValid() || helper == null) {
            return 0;
        }
        return helper.getCount();
    }

    @Override
    public Cursor getGroup(int groupPosition) {
        return this.mGroupCursorHelper.moveTo(groupPosition);
    }

    @Override
    public int getGroupCount() {
        return this.mGroupCursorHelper.getCount();
    }

    @Override
    public long getGroupId(int groupPosition) {
        return this.mGroupCursorHelper.getId(groupPosition);
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        View v;
        Cursor cursor = this.mGroupCursorHelper.moveTo(groupPosition);
        if (cursor == null) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (convertView == null) {
            v = newGroupView(this.mContext, cursor, isExpanded, parent);
        } else {
            v = convertView;
        }
        bindGroupView(v, this.mContext, cursor, isExpanded);
        return v;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        View v;
        MyCursorHelper cursorHelper = getChildrenCursorHelper(groupPosition, true);
        Cursor cursor = cursorHelper.moveTo(childPosition);
        if (cursor == null) {
            throw new IllegalStateException("this should only be called when the cursor is valid");
        }
        if (convertView == null) {
            v = newChildView(this.mContext, cursor, isLastChild, parent);
        } else {
            v = convertView;
        }
        bindChildView(v, this.mContext, cursor, isLastChild);
        return v;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private synchronized void releaseCursorHelpers() {
        for (int pos = this.mChildrenCursorHelpers.size() - 1; pos >= 0; pos--) {
            this.mChildrenCursorHelpers.valueAt(pos).deactivate();
        }
        this.mChildrenCursorHelpers.clear();
    }

    @Override
    public void notifyDataSetChanged() {
        notifyDataSetChanged(true);
    }

    public void notifyDataSetChanged(boolean releaseCursors) {
        if (releaseCursors) {
            releaseCursorHelpers();
        }
        super.notifyDataSetChanged();
    }

    @Override
    public void notifyDataSetInvalidated() {
        releaseCursorHelpers();
        super.notifyDataSetInvalidated();
    }

    @Override
    public void onGroupCollapsed(int groupPosition) {
        deactivateChildrenCursorHelper(groupPosition);
    }

    synchronized void deactivateChildrenCursorHelper(int groupPosition) {
        MyCursorHelper cursorHelper = getChildrenCursorHelper(groupPosition, true);
        this.mChildrenCursorHelpers.remove(groupPosition);
        cursorHelper.deactivate();
    }

    @Override
    public String convertToString(Cursor cursor) {
        return cursor == null ? ProxyInfo.LOCAL_EXCL_LIST : cursor.toString();
    }

    @Override
    public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
        return this.mFilterQueryProvider != null ? this.mFilterQueryProvider.runQuery(constraint) : this.mGroupCursorHelper.getCursor();
    }

    @Override
    public Filter getFilter() {
        if (this.mCursorFilter == null) {
            this.mCursorFilter = new CursorFilter(this);
        }
        return this.mCursorFilter;
    }

    public FilterQueryProvider getFilterQueryProvider() {
        return this.mFilterQueryProvider;
    }

    public void setFilterQueryProvider(FilterQueryProvider filterQueryProvider) {
        this.mFilterQueryProvider = filterQueryProvider;
    }

    @Override
    public void changeCursor(Cursor cursor) {
        this.mGroupCursorHelper.changeCursor(cursor, true);
    }

    @Override
    public Cursor getCursor() {
        return this.mGroupCursorHelper.getCursor();
    }

    class MyCursorHelper {
        private MyContentObserver mContentObserver;
        private Cursor mCursor;
        private MyDataSetObserver mDataSetObserver;
        private boolean mDataValid;
        private int mRowIDColumn;

        MyCursorHelper(Cursor cursor) {
            boolean cursorPresent = cursor != null;
            this.mCursor = cursor;
            this.mDataValid = cursorPresent;
            this.mRowIDColumn = cursorPresent ? cursor.getColumnIndex("_id") : -1;
            this.mContentObserver = new MyContentObserver();
            this.mDataSetObserver = new MyDataSetObserver();
            if (cursorPresent) {
                cursor.registerContentObserver(this.mContentObserver);
                cursor.registerDataSetObserver(this.mDataSetObserver);
            }
        }

        Cursor getCursor() {
            return this.mCursor;
        }

        int getCount() {
            if (!this.mDataValid || this.mCursor == null) {
                return 0;
            }
            return this.mCursor.getCount();
        }

        long getId(int position) {
            if (this.mDataValid && this.mCursor != null && this.mCursor.moveToPosition(position)) {
                return this.mCursor.getLong(this.mRowIDColumn);
            }
            return 0L;
        }

        Cursor moveTo(int position) {
            if (this.mDataValid && this.mCursor != null && this.mCursor.moveToPosition(position)) {
                return this.mCursor;
            }
            return null;
        }

        void changeCursor(Cursor cursor, boolean releaseCursors) {
            if (cursor != this.mCursor) {
                deactivate();
                this.mCursor = cursor;
                if (cursor != null) {
                    cursor.registerContentObserver(this.mContentObserver);
                    cursor.registerDataSetObserver(this.mDataSetObserver);
                    this.mRowIDColumn = cursor.getColumnIndex("_id");
                    this.mDataValid = true;
                    CursorTreeAdapter.this.notifyDataSetChanged(releaseCursors);
                    return;
                }
                this.mRowIDColumn = -1;
                this.mDataValid = false;
                CursorTreeAdapter.this.notifyDataSetInvalidated();
            }
        }

        void deactivate() {
            if (this.mCursor != null) {
                this.mCursor.unregisterContentObserver(this.mContentObserver);
                this.mCursor.unregisterDataSetObserver(this.mDataSetObserver);
                this.mCursor.close();
                this.mCursor = null;
            }
        }

        boolean isValid() {
            return this.mDataValid && this.mCursor != null;
        }

        private class MyContentObserver extends ContentObserver {
            public MyContentObserver() {
                super(CursorTreeAdapter.this.mHandler);
            }

            @Override
            public boolean deliverSelfNotifications() {
                return true;
            }

            @Override
            public void onChange(boolean selfChange) {
                if (CursorTreeAdapter.this.mAutoRequery && MyCursorHelper.this.mCursor != null && !MyCursorHelper.this.mCursor.isClosed()) {
                    MyCursorHelper.this.mDataValid = MyCursorHelper.this.mCursor.requery();
                }
            }
        }

        private class MyDataSetObserver extends DataSetObserver {
            private MyDataSetObserver() {
            }

            @Override
            public void onChanged() {
                MyCursorHelper.this.mDataValid = true;
                CursorTreeAdapter.this.notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                MyCursorHelper.this.mDataValid = false;
                CursorTreeAdapter.this.notifyDataSetInvalidated();
            }
        }
    }
}
