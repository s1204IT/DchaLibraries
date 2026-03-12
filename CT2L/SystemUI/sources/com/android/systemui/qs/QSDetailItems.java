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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;

public class QSDetailItems extends FrameLayout {
    private static final boolean DEBUG = Log.isLoggable("QSDetailItems", 3);
    private Callback mCallback;
    private final Context mContext;
    private View mEmpty;
    private ImageView mEmptyIcon;
    private TextView mEmptyText;
    private final H mHandler;
    private LinearLayout mItems;
    private boolean mItemsVisible;
    private int mMaxItems;
    private View mMinHeightSpacer;
    private String mTag;

    public interface Callback {
        void onDetailItemClick(Item item);

        void onDetailItemDisconnect(Item item);
    }

    public static class Item {
        public boolean canDisconnect;
        public int icon;
        public String line1;
        public String line2;
        public Drawable overlay;
        public Object tag;
    }

    public QSDetailItems(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mHandler = new H();
        this.mItemsVisible = true;
        this.mContext = context;
        this.mTag = "QSDetailItems";
    }

    public static QSDetailItems convertOrInflate(Context context, View convert, ViewGroup parent) {
        return convert instanceof QSDetailItems ? (QSDetailItems) convert : (QSDetailItems) LayoutInflater.from(context).inflate(R.layout.qs_detail_items, parent, false);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mItems = (LinearLayout) findViewById(android.R.id.list);
        this.mItems.setVisibility(8);
        this.mEmpty = findViewById(android.R.id.empty);
        this.mEmpty.setVisibility(8);
        this.mEmptyText = (TextView) this.mEmpty.findViewById(android.R.id.title);
        this.mEmptyIcon = (ImageView) this.mEmpty.findViewById(android.R.id.icon);
        this.mMinHeightSpacer = findViewById(R.id.min_height_spacer);
        this.mMaxItems = getResources().getInteger(R.integer.quick_settings_detail_max_item_count);
        setMinHeightInItems(this.mMaxItems);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        FontSizeUtils.updateFontSize(this.mEmptyText, R.dimen.qs_detail_empty_text_size);
        int count = this.mItems.getChildCount();
        for (int i = 0; i < count; i++) {
            View item = this.mItems.getChildAt(i);
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

    public void setMinHeightInItems(int minHeightInItems) {
        ViewGroup.LayoutParams lp = this.mMinHeightSpacer.getLayoutParams();
        lp.height = getResources().getDimensionPixelSize(R.dimen.qs_detail_item_height) * minHeightInItems;
        this.mMinHeightSpacer.setLayoutParams(lp);
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
        int itemCount = items != null ? Math.min(items.length, this.mMaxItems) : 0;
        this.mEmpty.setVisibility(itemCount == 0 ? 0 : 8);
        this.mItems.setVisibility(itemCount != 0 ? 0 : 8);
        for (int i = this.mItems.getChildCount() - 1; i >= itemCount; i--) {
            this.mItems.removeViewAt(i);
        }
        for (int i2 = 0; i2 < itemCount; i2++) {
            bind(items[i2], this.mItems.getChildAt(i2));
        }
    }

    public void handleSetItemsVisible(boolean visible) {
        if (this.mItemsVisible != visible) {
            this.mItemsVisible = visible;
            for (int i = 0; i < this.mItems.getChildCount(); i++) {
                this.mItems.getChildAt(i).setVisibility(this.mItemsVisible ? 0 : 4);
            }
        }
    }

    private void bind(final Item item, View view) {
        if (view == null) {
            view = LayoutInflater.from(this.mContext).inflate(R.layout.qs_detail_item, (ViewGroup) this, false);
            this.mItems.addView(view);
        }
        view.setVisibility(this.mItemsVisible ? 0 : 4);
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
        view.setMinimumHeight(this.mContext.getResources().getDimensionPixelSize(twoLines ? R.dimen.qs_detail_item_height_twoline : R.dimen.qs_detail_item_height));
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (QSDetailItems.this.mCallback != null) {
                    QSDetailItems.this.mCallback.onDetailItemClick(item);
                }
            }
        });
        ImageView disconnect = (ImageView) view.findViewById(android.R.id.icon2);
        disconnect.setVisibility(item.canDisconnect ? 0 : 8);
        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (QSDetailItems.this.mCallback != null) {
                    QSDetailItems.this.mCallback.onDetailItemDisconnect(item);
                }
            }
        });
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
            } else if (msg.what == 3) {
                QSDetailItems.this.handleSetItemsVisible(msg.arg1 != 0);
            }
        }
    }
}
