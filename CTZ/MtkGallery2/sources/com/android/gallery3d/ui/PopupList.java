package com.android.gallery3d.ui;

import android.content.Context;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.android.gallery3d.R;
import java.util.ArrayList;

public class PopupList {
    private final View mAnchorView;
    private ListView mContentList;
    private final Context mContext;
    private OnPopupItemClickListener mOnPopupItemClickListener;
    private int mPopupHeight;
    private int mPopupOffsetX;
    private int mPopupOffsetY;
    private int mPopupWidth;
    private PopupWindow mPopupWindow;
    private final ArrayList<Item> mItems = new ArrayList<>();
    private final PopupWindow.OnDismissListener mOnDismissListener = new PopupWindow.OnDismissListener() {
        @Override
        public void onDismiss() {
            if (PopupList.this.mPopupWindow == null) {
                return;
            }
            PopupList.this.mPopupWindow = null;
            ViewTreeObserver viewTreeObserver = PopupList.this.mAnchorView.getViewTreeObserver();
            if (viewTreeObserver.isAlive()) {
                viewTreeObserver.removeGlobalOnLayoutListener(PopupList.this.mOnGLobalLayoutListener);
            }
        }
    };
    private final AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long j) {
            if (PopupList.this.mPopupWindow == null) {
                return;
            }
            PopupList.this.mPopupWindow.dismiss();
            if (PopupList.this.mOnPopupItemClickListener != null) {
                PopupList.this.mOnPopupItemClickListener.onPopupItemClick((int) j);
            }
        }
    };
    private final ViewTreeObserver.OnGlobalLayoutListener mOnGLobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (PopupList.this.mPopupWindow == null) {
                return;
            }
            PopupList.this.updatePopupLayoutParams();
            PopupList.this.mPopupWindow.update(PopupList.this.mAnchorView, PopupList.this.mPopupOffsetX, PopupList.this.mPopupOffsetY, PopupList.this.mPopupWidth, PopupList.this.mPopupHeight);
        }
    };

    public interface OnPopupItemClickListener {
        boolean onPopupItemClick(int i);
    }

    public class Item {
        public final int id;
        public String title;

        public Item(int i, String str) {
            this.id = i;
            this.title = str;
        }

        public void setTitle(String str) {
            this.title = str;
            if (PopupList.this.mContentList != null) {
                PopupList.this.mContentList.invalidateViews();
            }
        }
    }

    public PopupList(Context context, View view) {
        this.mContext = context;
        this.mAnchorView = view;
    }

    public void setOnPopupItemClickListener(OnPopupItemClickListener onPopupItemClickListener) {
        this.mOnPopupItemClickListener = onPopupItemClickListener;
    }

    public void addItem(int i, String str) {
        this.mItems.add(new Item(i, str));
    }

    public void clearItems() {
        this.mItems.clear();
    }

    public void show() {
        if (this.mPopupWindow != null) {
            return;
        }
        this.mAnchorView.getViewTreeObserver().addOnGlobalLayoutListener(this.mOnGLobalLayoutListener);
        this.mPopupWindow = createPopupWindow();
        updatePopupLayoutParams();
        this.mPopupWindow.setWidth(this.mPopupWidth);
        this.mPopupWindow.setHeight(this.mPopupHeight);
        this.mPopupWindow.showAsDropDown(this.mAnchorView, this.mPopupOffsetX, this.mPopupOffsetY);
    }

    private void updatePopupLayoutParams() {
        ListView listView = this.mContentList;
        PopupWindow popupWindow = this.mPopupWindow;
        Rect rect = new Rect();
        popupWindow.getBackground().getPadding(rect);
        int maxAvailableHeight = (this.mPopupWindow.getMaxAvailableHeight(this.mAnchorView) - rect.top) - rect.bottom;
        this.mContentList.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(maxAvailableHeight, Integer.MIN_VALUE));
        this.mPopupWidth = listView.getMeasuredWidth() + rect.top + rect.bottom;
        this.mPopupHeight = Math.min(maxAvailableHeight, listView.getMeasuredHeight() + rect.left + rect.right);
        this.mPopupOffsetX = -rect.left;
        this.mPopupOffsetY = -rect.top;
    }

    private PopupWindow createPopupWindow() {
        PopupWindow popupWindow = new PopupWindow(this.mContext);
        popupWindow.setOnDismissListener(this.mOnDismissListener);
        popupWindow.setBackgroundDrawable(this.mContext.getResources().getDrawable(R.drawable.menu_dropdown_panel_holo_dark));
        this.mContentList = new ListView(this.mContext, null, android.R.attr.dropDownListViewStyle);
        this.mContentList.setAdapter((ListAdapter) new ItemDataAdapter());
        this.mContentList.setOnItemClickListener(this.mOnItemClickListener);
        popupWindow.setContentView(this.mContentList);
        popupWindow.setFocusable(true);
        popupWindow.setOutsideTouchable(true);
        return popupWindow;
    }

    public Item findItem(int i) {
        for (Item item : this.mItems) {
            if (item.id == i) {
                return item;
            }
        }
        return null;
    }

    private class ItemDataAdapter extends BaseAdapter {
        private ItemDataAdapter() {
        }

        @Override
        public int getCount() {
            return PopupList.this.mItems.size();
        }

        @Override
        public Object getItem(int i) {
            return PopupList.this.mItems.get(i);
        }

        @Override
        public long getItemId(int i) {
            return ((Item) PopupList.this.mItems.get(i)).id;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = LayoutInflater.from(PopupList.this.mContext).inflate(R.layout.popup_list_item, (ViewGroup) null);
            }
            ((TextView) view.findViewById(android.R.id.text1)).setText(((Item) PopupList.this.mItems.get(i)).title);
            return view;
        }
    }

    public void finish() {
        if (this.mPopupWindow != null) {
            this.mPopupWindow.dismiss();
        }
    }
}
