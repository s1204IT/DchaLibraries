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
            if (PopupList.this.mPopupWindow != null) {
                PopupList.this.mPopupWindow = null;
                ViewTreeObserver observer = PopupList.this.mAnchorView.getViewTreeObserver();
                if (observer.isAlive()) {
                    observer.removeGlobalOnLayoutListener(PopupList.this.mOnGLobalLayoutListener);
                }
            }
        }
    };
    private final AdapterView.OnItemClickListener mOnItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (PopupList.this.mPopupWindow != null) {
                PopupList.this.mPopupWindow.dismiss();
                if (PopupList.this.mOnPopupItemClickListener != null) {
                    PopupList.this.mOnPopupItemClickListener.onPopupItemClick((int) id);
                }
            }
        }
    };
    private final ViewTreeObserver.OnGlobalLayoutListener mOnGLobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (PopupList.this.mPopupWindow != null) {
                PopupList.this.updatePopupLayoutParams();
                PopupList.this.mPopupWindow.update(PopupList.this.mAnchorView, PopupList.this.mPopupOffsetX, PopupList.this.mPopupOffsetY, PopupList.this.mPopupWidth, PopupList.this.mPopupHeight);
            }
        }
    };

    public interface OnPopupItemClickListener {
        boolean onPopupItemClick(int i);
    }

    public static class Item {
        public final int id;
        public String title;

        public Item(int id, String title) {
            this.id = id;
            this.title = title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    public PopupList(Context context, View anchorView) {
        this.mContext = context;
        this.mAnchorView = anchorView;
    }

    public void setOnPopupItemClickListener(OnPopupItemClickListener listener) {
        this.mOnPopupItemClickListener = listener;
    }

    public void addItem(int id, String title) {
        this.mItems.add(new Item(id, title));
    }

    public void show() {
        if (this.mPopupWindow == null) {
            this.mAnchorView.getViewTreeObserver().addOnGlobalLayoutListener(this.mOnGLobalLayoutListener);
            this.mPopupWindow = createPopupWindow();
            updatePopupLayoutParams();
            this.mPopupWindow.setWidth(this.mPopupWidth);
            this.mPopupWindow.setHeight(this.mPopupHeight);
            this.mPopupWindow.showAsDropDown(this.mAnchorView, this.mPopupOffsetX, this.mPopupOffsetY);
        }
    }

    private void updatePopupLayoutParams() {
        ListView content = this.mContentList;
        PopupWindow popup = this.mPopupWindow;
        Rect p = new Rect();
        popup.getBackground().getPadding(p);
        int maxHeight = (this.mPopupWindow.getMaxAvailableHeight(this.mAnchorView) - p.top) - p.bottom;
        this.mContentList.measure(View.MeasureSpec.makeMeasureSpec(0, 0), View.MeasureSpec.makeMeasureSpec(maxHeight, Integer.MIN_VALUE));
        this.mPopupWidth = content.getMeasuredWidth() + p.top + p.bottom;
        this.mPopupHeight = Math.min(maxHeight, content.getMeasuredHeight() + p.left + p.right);
        this.mPopupOffsetX = -p.left;
        this.mPopupOffsetY = -p.top;
    }

    private PopupWindow createPopupWindow() {
        PopupWindow popup = new PopupWindow(this.mContext);
        popup.setOnDismissListener(this.mOnDismissListener);
        popup.setBackgroundDrawable(this.mContext.getResources().getDrawable(R.drawable.menu_dropdown_panel_holo_dark));
        this.mContentList = new ListView(this.mContext, null, android.R.attr.dropDownListViewStyle);
        this.mContentList.setAdapter((ListAdapter) new ItemDataAdapter());
        this.mContentList.setOnItemClickListener(this.mOnItemClickListener);
        popup.setContentView(this.mContentList);
        popup.setFocusable(true);
        popup.setOutsideTouchable(true);
        return popup;
    }

    public Item findItem(int id) {
        for (Item item : this.mItems) {
            if (item.id == id) {
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
        public Object getItem(int position) {
            return PopupList.this.mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return ((Item) PopupList.this.mItems.get(position)).id;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(PopupList.this.mContext).inflate(R.layout.popup_list_item, (ViewGroup) null);
            }
            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(((Item) PopupList.this.mItems.get(position)).title);
            return convertView;
        }
    }
}
