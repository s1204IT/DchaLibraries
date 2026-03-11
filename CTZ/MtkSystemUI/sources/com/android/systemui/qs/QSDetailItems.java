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
import com.android.systemui.plugins.qs.QSTile;

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
    private final int mQsDetailIconOverlaySize;
    private String mTag;

    public interface Callback {
        void onDetailItemClick(Item item);

        void onDetailItemDisconnect(Item item);
    }

    public static class Item {
        public boolean canDisconnect;
        public QSTile.Icon icon;
        public int icon2 = -1;
        public int iconResId;
        public CharSequence line1;
        public CharSequence line2;
        public Drawable overlay;
        public Object tag;
    }

    public QSDetailItems(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHandler = new H();
        this.mAdapter = new Adapter();
        this.mItemsVisible = true;
        this.mContext = context;
        this.mTag = "QSDetailItems";
        this.mQsDetailIconOverlaySize = (int) getResources().getDimension(R.dimen.qs_detail_icon_overlay_size);
    }

    public static QSDetailItems convertOrInflate(Context context, View view, ViewGroup viewGroup) {
        if (view instanceof QSDetailItems) {
            return (QSDetailItems) view;
        }
        return (QSDetailItems) LayoutInflater.from(context).inflate(R.layout.qs_detail_items, viewGroup, false);
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
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        FontSizeUtils.updateFontSize(this.mEmptyText, R.dimen.qs_detail_empty_text_size);
        int childCount = this.mItemList.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = this.mItemList.getChildAt(i);
            FontSizeUtils.updateFontSize(childAt, android.R.id.title, R.dimen.qs_detail_item_primary_text_size);
            FontSizeUtils.updateFontSize(childAt, android.R.id.summary, R.dimen.qs_detail_item_secondary_text_size);
        }
    }

    public void setTagSuffix(String str) {
        this.mTag = "QSDetailItems." + str;
    }

    public void setEmptyState(final int i, final int i2) {
        this.mEmptyIcon.post(new Runnable() {
            @Override
            public final void run() {
                QSDetailItems.lambda$setEmptyState$0(this.f$0, i, i2);
            }
        });
    }

    public static void lambda$setEmptyState$0(QSDetailItems qSDetailItems, int i, int i2) {
        qSDetailItems.mEmptyIcon.setImageResource(i);
        qSDetailItems.mEmptyText.setText(i2);
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

    public void setItems(Item[] itemArr) {
        this.mHandler.removeMessages(1);
        this.mHandler.obtainMessage(1, itemArr).sendToTarget();
    }

    public void setItemsVisible(boolean z) {
        this.mHandler.removeMessages(3);
        this.mHandler.obtainMessage(3, z ? 1 : 0, 0).sendToTarget();
    }

    public void handleSetCallback(Callback callback) {
        this.mCallback = callback;
    }

    public void handleSetItems(Item[] itemArr) {
        int length = itemArr != null ? itemArr.length : 0;
        this.mEmpty.setVisibility(length == 0 ? 0 : 8);
        this.mItemList.setVisibility(length == 0 ? 8 : 0);
        this.mItems = itemArr;
        this.mAdapter.notifyDataSetChanged();
    }

    public void handleSetItemsVisible(boolean z) {
        if (this.mItemsVisible == z) {
            return;
        }
        this.mItemsVisible = z;
        for (int i = 0; i < this.mItemList.getChildCount(); i++) {
            this.mItemList.getChildAt(i).setVisibility(this.mItemsVisible ? 0 : 4);
        }
    }

    private class Adapter extends BaseAdapter {
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
        public Object getItem(int i) {
            return QSDetailItems.this.mItems[i];
        }

        @Override
        public long getItemId(int i) {
            return 0L;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int i2;
            final Item item = QSDetailItems.this.mItems[i];
            if (view == null) {
                view = LayoutInflater.from(QSDetailItems.this.mContext).inflate(R.layout.qs_detail_item, viewGroup, false);
            }
            view.setVisibility(QSDetailItems.this.mItemsVisible ? 0 : 4);
            ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
            if (item.icon != null) {
                imageView.setImageDrawable(item.icon.getDrawable(imageView.getContext()));
            } else {
                imageView.setImageResource(item.iconResId);
            }
            imageView.getOverlay().clear();
            if (item.overlay != null) {
                item.overlay.setBounds(0, 0, QSDetailItems.this.mQsDetailIconOverlaySize, QSDetailItems.this.mQsDetailIconOverlaySize);
                imageView.getOverlay().add(item.overlay);
            }
            TextView textView = (TextView) view.findViewById(android.R.id.title);
            textView.setText(item.line1);
            TextView textView2 = (TextView) view.findViewById(android.R.id.summary);
            boolean z = !TextUtils.isEmpty(item.line2);
            textView.setMaxLines(z ? 1 : 2);
            if (z) {
                i2 = 0;
            } else {
                i2 = 8;
            }
            textView2.setVisibility(i2);
            textView2.setText(z ? item.line2 : null);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view2) {
                    if (QSDetailItems.this.mCallback != null) {
                        QSDetailItems.this.mCallback.onDetailItemClick(item);
                    }
                }
            });
            ImageView imageView2 = (ImageView) view.findViewById(android.R.id.icon2);
            if (item.canDisconnect) {
                imageView2.setImageResource(R.drawable.ic_qs_cancel);
                imageView2.setVisibility(0);
                imageView2.setClickable(true);
                imageView2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view2) {
                        if (QSDetailItems.this.mCallback != null) {
                            QSDetailItems.this.mCallback.onDetailItemDisconnect(item);
                        }
                    }
                });
            } else if (item.icon2 != -1) {
                imageView2.setVisibility(0);
                imageView2.setImageResource(item.icon2);
                imageView2.setClickable(false);
            } else {
                imageView2.setVisibility(8);
            }
            return view;
        }
    }

    private class H extends Handler {
        public H() {
            super(Looper.getMainLooper());
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 1) {
                QSDetailItems.this.handleSetItems((Item[]) message.obj);
            } else if (message.what == 2) {
                QSDetailItems.this.handleSetCallback((Callback) message.obj);
            } else if (message.what == 3) {
                QSDetailItems.this.handleSetItemsVisible(message.arg1 != 0);
            }
        }
    }
}
