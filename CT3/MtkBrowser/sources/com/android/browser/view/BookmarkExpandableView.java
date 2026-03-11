package com.android.browser.view;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.browser.BreadCrumbView;
import com.android.browser.BrowserBookmarksAdapter;
import com.android.browser.R;
import com.android.browser.provider.BrowserContract;
import com.android.internal.view.menu.MenuBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import org.json.JSONException;
import org.json.JSONObject;

public class BookmarkExpandableView extends ExpandableListView implements BreadCrumbView.Controller {
    private BookmarkAccountAdapter mAdapter;
    private BreadCrumbView.Controller mBreadcrumbController;
    private View.OnClickListener mChildClickListener;
    private int mColumnWidth;
    private Context mContext;
    private ContextMenu.ContextMenuInfo mContextMenuInfo;
    private View.OnClickListener mGroupOnClickListener;
    private boolean mLongClickable;
    private int mMaxColumnCount;
    private ExpandableListView.OnChildClickListener mOnChildClickListener;
    private View.OnCreateContextMenuListener mOnCreateContextMenuListener;

    public BookmarkExpandableView(Context context) {
        super(context);
        this.mContextMenuInfo = null;
        this.mChildClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getVisibility() != 0) {
                    return;
                }
                int groupPosition = ((Integer) v.getTag(R.id.group_position)).intValue();
                int childPosition = ((Integer) v.getTag(R.id.child_position)).intValue();
                if (BookmarkExpandableView.this.mAdapter.getGroupCount() <= groupPosition || BookmarkExpandableView.this.mAdapter.mChildren.get(groupPosition).getCount() <= childPosition) {
                    return;
                }
                long id = BookmarkExpandableView.this.mAdapter.mChildren.get(groupPosition).getItemId(childPosition);
                if (BookmarkExpandableView.this.mOnChildClickListener == null) {
                    return;
                }
                BookmarkExpandableView.this.mOnChildClickListener.onChildClick(BookmarkExpandableView.this, v, groupPosition, childPosition, id);
            }
        };
        this.mGroupOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int groupPosition = ((Integer) v.getTag(R.id.group_position)).intValue();
                if (BookmarkExpandableView.this.isGroupExpanded(groupPosition)) {
                    BookmarkExpandableView.this.collapseGroup(groupPosition);
                } else {
                    BookmarkExpandableView.this.hideAllGroups();
                    BookmarkExpandableView.this.expandGroup(groupPosition, true);
                }
            }
        };
        init(context);
    }

    public BookmarkExpandableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContextMenuInfo = null;
        this.mChildClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getVisibility() != 0) {
                    return;
                }
                int groupPosition = ((Integer) v.getTag(R.id.group_position)).intValue();
                int childPosition = ((Integer) v.getTag(R.id.child_position)).intValue();
                if (BookmarkExpandableView.this.mAdapter.getGroupCount() <= groupPosition || BookmarkExpandableView.this.mAdapter.mChildren.get(groupPosition).getCount() <= childPosition) {
                    return;
                }
                long id = BookmarkExpandableView.this.mAdapter.mChildren.get(groupPosition).getItemId(childPosition);
                if (BookmarkExpandableView.this.mOnChildClickListener == null) {
                    return;
                }
                BookmarkExpandableView.this.mOnChildClickListener.onChildClick(BookmarkExpandableView.this, v, groupPosition, childPosition, id);
            }
        };
        this.mGroupOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int groupPosition = ((Integer) v.getTag(R.id.group_position)).intValue();
                if (BookmarkExpandableView.this.isGroupExpanded(groupPosition)) {
                    BookmarkExpandableView.this.collapseGroup(groupPosition);
                } else {
                    BookmarkExpandableView.this.hideAllGroups();
                    BookmarkExpandableView.this.expandGroup(groupPosition, true);
                }
            }
        };
        init(context);
    }

    public BookmarkExpandableView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mContextMenuInfo = null;
        this.mChildClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getVisibility() != 0) {
                    return;
                }
                int groupPosition = ((Integer) v.getTag(R.id.group_position)).intValue();
                int childPosition = ((Integer) v.getTag(R.id.child_position)).intValue();
                if (BookmarkExpandableView.this.mAdapter.getGroupCount() <= groupPosition || BookmarkExpandableView.this.mAdapter.mChildren.get(groupPosition).getCount() <= childPosition) {
                    return;
                }
                long id = BookmarkExpandableView.this.mAdapter.mChildren.get(groupPosition).getItemId(childPosition);
                if (BookmarkExpandableView.this.mOnChildClickListener == null) {
                    return;
                }
                BookmarkExpandableView.this.mOnChildClickListener.onChildClick(BookmarkExpandableView.this, v, groupPosition, childPosition, id);
            }
        };
        this.mGroupOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int groupPosition = ((Integer) v.getTag(R.id.group_position)).intValue();
                if (BookmarkExpandableView.this.isGroupExpanded(groupPosition)) {
                    BookmarkExpandableView.this.collapseGroup(groupPosition);
                } else {
                    BookmarkExpandableView.this.hideAllGroups();
                    BookmarkExpandableView.this.expandGroup(groupPosition, true);
                }
            }
        };
        init(context);
    }

    void init(Context context) {
        this.mContext = context;
        setItemsCanFocus(true);
        setLongClickable(false);
        this.mMaxColumnCount = this.mContext.getResources().getInteger(R.integer.max_bookmark_columns);
        setScrollBarStyle(33554432);
        this.mAdapter = new BookmarkAccountAdapter(this.mContext);
        super.setAdapter(this.mAdapter);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = View.MeasureSpec.getMode(widthMeasureSpec);
        if (width > 0) {
            this.mAdapter.measureChildren(width);
            setPadding(this.mAdapter.mRowPadding, 0, this.mAdapter.mRowPadding, 0);
            widthMeasureSpec = View.MeasureSpec.makeMeasureSpec(width, widthMode);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (width == getMeasuredWidth()) {
            return;
        }
        this.mAdapter.measureChildren(getMeasuredWidth());
    }

    @Override
    public void setAdapter(ExpandableListAdapter adapter) {
        throw new RuntimeException("Not supported");
    }

    public void setColumnWidthFromLayout(int layout) {
        LayoutInflater infalter = LayoutInflater.from(this.mContext);
        View v = infalter.inflate(layout, (ViewGroup) this, false);
        v.measure(0, 0);
        this.mColumnWidth = v.getMeasuredWidth();
    }

    public void clearAccounts() {
        this.mAdapter.clear();
    }

    public void addAccount(String accountName, BrowserBookmarksAdapter adapter, boolean expandGroup) {
        int indexOf = this.mAdapter.mGroups.indexOf(accountName);
        if (indexOf >= 0) {
            BrowserBookmarksAdapter existing = this.mAdapter.mChildren.get(indexOf);
            if (existing != adapter) {
                existing.unregisterDataSetObserver(this.mAdapter.mObserver);
                this.mAdapter.mChildren.remove(indexOf);
                this.mAdapter.mChildren.add(indexOf, adapter);
                adapter.registerDataSetObserver(this.mAdapter.mObserver);
            }
        } else {
            this.mAdapter.mGroups.add(accountName);
            this.mAdapter.mChildren.add(adapter);
            adapter.registerDataSetObserver(this.mAdapter.mObserver);
        }
        this.mAdapter.notifyDataSetChanged();
        if (!expandGroup) {
            return;
        }
        expandGroup(this.mAdapter.getGroupCount() - 1);
    }

    public void hideAllGroups() {
        for (int i = 0; i < this.mAdapter.getGroupCount(); i++) {
            collapseGroup(i);
        }
    }

    @Override
    public void setOnChildClickListener(ExpandableListView.OnChildClickListener onChildClickListener) {
        this.mOnChildClickListener = onChildClickListener;
    }

    @Override
    public void setOnCreateContextMenuListener(View.OnCreateContextMenuListener l) {
        this.mOnCreateContextMenuListener = l;
        if (this.mLongClickable) {
            return;
        }
        this.mLongClickable = true;
        if (this.mAdapter == null) {
            return;
        }
        this.mAdapter.notifyDataSetChanged();
    }

    @Override
    public void createContextMenu(ContextMenu menu) {
        ContextMenu.ContextMenuInfo menuInfo = getContextMenuInfo();
        ((MenuBuilder) menu).setCurrentMenuInfo(menuInfo);
        onCreateContextMenu(menu);
        if (this.mOnCreateContextMenuListener != null) {
            this.mOnCreateContextMenuListener.onCreateContextMenu(menu, this, menuInfo);
        }
        ((MenuBuilder) menu).setCurrentMenuInfo((ContextMenu.ContextMenuInfo) null);
        if (this.mParent == null) {
            return;
        }
        this.mParent.createContextMenu(menu);
    }

    @Override
    public boolean showContextMenuForChild(View originalView) {
        BookmarkContextMenuInfo bookmarkContextMenuInfo = null;
        Integer groupPosition = (Integer) originalView.getTag(R.id.group_position);
        Integer childPosition = (Integer) originalView.getTag(R.id.child_position);
        if (groupPosition == null || childPosition == null) {
            return false;
        }
        this.mContextMenuInfo = new BookmarkContextMenuInfo(childPosition.intValue(), groupPosition.intValue(), bookmarkContextMenuInfo);
        if (getParent() != null) {
            getParent().showContextMenuForChild(this);
            return true;
        }
        return true;
    }

    @Override
    public void onTop(BreadCrumbView view, int level, Object data) {
        if (this.mBreadcrumbController == null) {
            return;
        }
        this.mBreadcrumbController.onTop(view, level, data);
    }

    public void setBreadcrumbController(BreadCrumbView.Controller controller) {
        this.mBreadcrumbController = controller;
    }

    @Override
    protected ContextMenu.ContextMenuInfo getContextMenuInfo() {
        return this.mContextMenuInfo;
    }

    public BrowserBookmarksAdapter getChildAdapter(int groupPosition) {
        return this.mAdapter.mChildren.get(groupPosition);
    }

    public BreadCrumbView getBreadCrumbs(int groupPosition) {
        return this.mAdapter.getBreadCrumbView(groupPosition);
    }

    public JSONObject saveGroupState() throws JSONException {
        JSONObject obj = new JSONObject();
        int count = this.mAdapter.getGroupCount();
        for (int i = 0; i < count; i++) {
            String acctName = this.mAdapter.mGroups.get(i);
            if (!isGroupExpanded(i)) {
                if (acctName == null) {
                    acctName = "local";
                }
                obj.put(acctName, false);
            }
        }
        return obj;
    }

    class BookmarkAccountAdapter extends BaseExpandableListAdapter {
        ArrayList<BrowserBookmarksAdapter> mChildren;
        ArrayList<String> mGroups;
        LayoutInflater mInflater;
        HashMap<Integer, BreadCrumbView> mBreadcrumbs = new HashMap<>();
        int mRowCount = 1;
        int mLastViewWidth = -1;
        int mRowPadding = -1;
        DataSetObserver mObserver = new DataSetObserver() {
            @Override
            public void onChanged() {
                BookmarkAccountAdapter.this.notifyDataSetChanged();
            }

            @Override
            public void onInvalidated() {
                BookmarkAccountAdapter.this.notifyDataSetInvalidated();
            }
        };

        public BookmarkAccountAdapter(Context context) {
            BookmarkExpandableView.this.mContext = context;
            this.mInflater = LayoutInflater.from(BookmarkExpandableView.this.mContext);
            this.mChildren = new ArrayList<>();
            this.mGroups = new ArrayList<>();
        }

        public void clear() {
            this.mGroups.clear();
            this.mChildren.clear();
            notifyDataSetChanged();
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return this.mChildren.get(groupPosition).getItem(childPosition);
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = this.mInflater.inflate(R.layout.bookmark_grid_row, parent, false);
            }
            BrowserBookmarksAdapter childAdapter = this.mChildren.get(groupPosition);
            int rowCount = this.mRowCount;
            LinearLayout row = (LinearLayout) convertView;
            if (row.getChildCount() > rowCount) {
                row.removeViews(rowCount, row.getChildCount() - rowCount);
            }
            for (int i = 0; i < rowCount; i++) {
                View cv = null;
                if (row.getChildCount() > i) {
                    cv = row.getChildAt(i);
                }
                int realChildPosition = (childPosition * rowCount) + i;
                if (realChildPosition < childAdapter.getCount()) {
                    View v = childAdapter.getView(realChildPosition, cv, row);
                    v.setTag(R.id.group_position, Integer.valueOf(groupPosition));
                    v.setTag(R.id.child_position, Integer.valueOf(realChildPosition));
                    v.setOnClickListener(BookmarkExpandableView.this.mChildClickListener);
                    v.setLongClickable(BookmarkExpandableView.this.mLongClickable);
                    if (row.getChildCount() > 1) {
                        v.setPadding(row.getChildAt(0).getPaddingLeft(), row.getChildAt(0).getPaddingTop(), row.getChildAt(0).getPaddingRight(), row.getChildAt(0).getPaddingBottom());
                    }
                    if (cv == null) {
                        row.addView(v);
                    } else if (cv != v) {
                        row.removeViewAt(i);
                        row.addView(v, i);
                    } else {
                        cv.setVisibility(0);
                    }
                } else if (cv != null) {
                    cv.setVisibility(8);
                }
            }
            return row;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            BrowserBookmarksAdapter adapter = this.mChildren.get(groupPosition);
            return (int) Math.ceil(adapter.getCount() / this.mRowCount);
        }

        @Override
        public Object getGroup(int groupPosition) {
            return this.mChildren.get(groupPosition);
        }

        @Override
        public int getGroupCount() {
            return this.mGroups.size();
        }

        public void measureChildren(int viewWidth) {
            if (this.mLastViewWidth == viewWidth) {
                return;
            }
            int rowCount = viewWidth / BookmarkExpandableView.this.mColumnWidth;
            if (BookmarkExpandableView.this.mMaxColumnCount > 0) {
                rowCount = Math.min(rowCount, BookmarkExpandableView.this.mMaxColumnCount);
            }
            int rowPadding = (viewWidth - (BookmarkExpandableView.this.mColumnWidth * rowCount)) / 2;
            boolean notify = (rowCount == this.mRowCount && rowPadding == this.mRowPadding) ? false : true;
            this.mRowCount = rowCount;
            this.mRowPadding = rowPadding;
            this.mLastViewWidth = viewWidth;
            if (!notify) {
                return;
            }
            notifyDataSetChanged();
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup parent) {
            if (view == null) {
                view = this.mInflater.inflate(R.layout.bookmark_group_view, parent, false);
                view.setOnClickListener(BookmarkExpandableView.this.mGroupOnClickListener);
            }
            view.setTag(R.id.group_position, Integer.valueOf(groupPosition));
            FrameLayout crumbHolder = (FrameLayout) view.findViewById(R.id.crumb_holder);
            crumbHolder.removeAllViews();
            BreadCrumbView crumbs = getBreadCrumbView(groupPosition);
            if (crumbs.getParent() != null) {
                ((ViewGroup) crumbs.getParent()).removeView(crumbs);
            }
            crumbHolder.addView(crumbs);
            TextView name = (TextView) view.findViewById(R.id.group_name);
            String groupName = this.mGroups.get(groupPosition);
            if (groupName == null) {
                groupName = BookmarkExpandableView.this.mContext.getString(R.string.local_bookmarks);
            }
            name.setText(groupName);
            return view;
        }

        public BreadCrumbView getBreadCrumbView(int groupPosition) {
            BreadCrumbView crumbs = this.mBreadcrumbs.get(Integer.valueOf(groupPosition));
            if (crumbs == null) {
                BreadCrumbView crumbs2 = (BreadCrumbView) this.mInflater.inflate(R.layout.bookmarks_header, (ViewGroup) null);
                crumbs2.setController(BookmarkExpandableView.this);
                crumbs2.setUseBackButton(true);
                crumbs2.setMaxVisible(2);
                String bookmarks = BookmarkExpandableView.this.mContext.getString(R.string.bookmarks);
                crumbs2.pushView(bookmarks, false, BrowserContract.Bookmarks.CONTENT_URI_DEFAULT_FOLDER);
                crumbs2.setTag(R.id.group_position, Integer.valueOf(groupPosition));
                crumbs2.setVisibility(8);
                this.mBreadcrumbs.put(Integer.valueOf(groupPosition), crumbs2);
                return crumbs2;
            }
            return crumbs;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    }

    public static class BookmarkContextMenuInfo implements ContextMenu.ContextMenuInfo {
        public int childPosition;
        public int groupPosition;

        BookmarkContextMenuInfo(int childPosition, int groupPosition, BookmarkContextMenuInfo bookmarkContextMenuInfo) {
            this(childPosition, groupPosition);
        }

        private BookmarkContextMenuInfo(int childPosition, int groupPosition) {
            this.childPosition = childPosition;
            this.groupPosition = groupPosition;
        }
    }
}
