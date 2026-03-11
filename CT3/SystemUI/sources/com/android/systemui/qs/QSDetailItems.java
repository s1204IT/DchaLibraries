package com.android.systemui.qs;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;

public class QSDetailItems extends FrameLayout {
    private static final boolean DEBUG = Log.isLoggable("QSDetailItems", 3);
    private final Adapter mAdapter;
    private Callback mCallback;
    private final Context mContext;
    private View mEmpty;
    private ImageView mEmptyIcon;
    private TextView mEmptyText;
    private final H mHandler;
    private AutoSizingList mItemList;
    private Item[] mItems;
    private boolean mItemsVisible;
    private String mTag;

    public interface Callback {
        void onDetailItemClick(Item item);

        void onDetailItemDisconnect(Item item);
    }

    public static class Item {
        public boolean canDisconnect;
        public int icon;
        public CharSequence line1;
        public CharSequence line2;
        public Drawable overlay;
        public Object tag;
    }

    public QSDetailItems(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHandler = new H();
        this.mAdapter = new Adapter(this, null);
        this.mItemsVisible = true;
        this.mContext = context;
        this.mTag = "QSDetailItems";
    }

    public static QSDetailItems convertOrInflate(Context context, View convert, ViewGroup parent) {
        if (convert instanceof QSDetailItems) {
            return (QSDetailItems) convert;
        }
        return (QSDetailItems) LayoutInflater.from(context).inflate(R.layout.qs_detail_items, parent, false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mItemList = (AutoSizingList) findViewById(android.R.id.list);
        this.mItemList.setVisibility(8);
        this.mItemList.setAdapter(this.mAdapter);
        this.mEmpty = findViewById(android.R.id.empty);
        this.mEmpty.setVisibility(8);
        this.mEmptyText = (TextView) this.mEmpty.findViewById(android.R.id.title);
        this.mEmptyIcon = (ImageView) this.mEmpty.findViewById(android.R.id.icon);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this.mEmptyText, R.dimen.qs_detail_empty_text_size);
        int count = this.mItemList.getChildCount();
        for (int i = 0; i < count; i++) {
            View item = this.mItemList.getChildAt(i);
            FontSizeUtils.updateFontSize(item, android.R.id.title, R.dimen.qs_detail_item_primary_text_size);
            FontSizeUtils.updateFontSize(item, android.R.id.summary, R.dimen.qs_detail_item_secondary_text_size);
        }
    }

    public void setTagSuffix(String suffix) {
        this.mTag = "QSDetailItems." + suffix;
    }

    public void setEmptyState(int icon, int text) {
        this.mEmptyIcon.setImageResource(icon);
        this.mEmptyText.setText(text);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onAttachedToWindow");
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (DEBUG) {
            Log.d(this.mTag, "onDetachedFromWindow");
        }
        this.mCallback = null;
    }

    public void setCallback(Callback callback) {
        this.mHandler.removeMessages(2);
        this.mHandler.obtainMessage(2, callback).sendToTarget();
    }

    public void setItems(Item[] items) {
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1, items).sendToTarget();
    }

    public void setItemsVisible(boolean visible) {
        this.mHandler.removeMessages(3);
        this.mHandler.obtainMessage(3, visible ? 1 : 0, 0).sendToTarget();
    }

    public void handleSetCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void handleSetItems(Item[] items) {
        int itemCount = items != null ? items.length : 0;
        this.mEmpty.setVisibility(itemCount == 0 ? 0 : 8);
        this.mItemList.setVisibility(itemCount != 0 ? 0 : 8);
        this.mItems = items;
        this.mAdapter.notifyDataSetChanged();
    }

    public void handleSetItemsVisible(boolean visible) {
        if (this.mItemsVisible == visible) {
            return;
        }
        this.mItemsVisible = visible;
        for (int i = 0; i < this.mItemList.getChildCount(); i++) {
            this.mItemList.getChildAt(i).setVisibility(this.mItemsVisible ? 0 : 4);
        }
    }

    private class Adapter extends BaseAdapter {
        Adapter(QSDetailItems this$0, Adapter adapter) {
            this();
        }

        private Adapter() {
        }

        @Override
        public int getCount() {
            if (QSDetailItems.this.mItems != null) {
                return QSDetailItems.this.mItems.length;
            }
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return QSDetailItems.this.mItems[position];
        }

        @Override
        public long getItemId(int position) {
            return 0L;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            final Item item = QSDetailItems.this.mItems[position];
            if (view == null) {
                view = LayoutInflater.from(QSDetailItems.this.mContext).inflate(R.layout.qs_detail_item, parent, false);
            }
            view.setVisibility(QSDetailItems.this.mItemsVisible ? 0 : 4);
            ImageView iv = (ImageView) view.findViewById(android.R.id.icon);
            iv.setImageResource(item.icon);
            iv.getOverlay().clear();
            if (item.overlay != null) {
                item.overlay.setBounds(0, 0, item.overlay.getIntrinsicWidth(), item.overlay.getIntrinsicHeight());
                iv.getOverlay().add(item.overlay);
            }
            TextView title = (TextView) view.findViewById(android.R.id.title);
            title.setText(item.line1);
            TextView summary = (TextView) view.findViewById(android.R.id.summary);
            boolean twoLines = !TextUtils.isEmpty(item.line2);
            title.setMaxLines(twoLines ? 1 : 2);
            summary.setVisibility(twoLines ? 0 : 8);
            summary.setText(twoLines ? item.line2 : null);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (QSDetailItems.this.mCallback == null) {
                        return;
                    }
                    QSDetailItems.this.mCallback.onDetailItemClick(item);
                }
            });
            ImageView disconnect = (ImageView) view.findViewById(android.R.id.icon2);
            disconnect.setVisibility(item.canDisconnect ? 0 : 8);
            disconnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (QSDetailItems.this.mCallback == null) {
                        return;
                    }
                    QSDetailItems.this.mCallback.onDetailItemDisconnect(item);
                }
            });
            return view;
        }
    }

    private class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                QSDetailItems.this.handleSetItems((Item[]) msg.obj);
            } else if (msg.what == 2) {
                QSDetailItems.this.handleSetCallback((Callback) msg.obj);
            } else {
                if (msg.what != 3) {
                    return;
                }
                QSDetailItems.this.handleSetItemsVisible(msg.arg1 != 0);
            }
        }
    }
}
