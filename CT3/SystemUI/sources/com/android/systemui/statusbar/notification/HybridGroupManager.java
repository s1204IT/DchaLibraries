package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.systemui.R;

public class HybridGroupManager {
    private final Context mContext;
    private int mOverflowNumberColor;
    private ViewGroup mParent;

    public HybridGroupManager(Context ctx, ViewGroup parent) {
        this.mContext = ctx;
        this.mParent = parent;
    }

    private HybridNotificationView inflateHybridView() {
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(LayoutInflater.class);
        HybridNotificationView hybrid = (HybridNotificationView) inflater.inflate(R.layout.hybrid_notification, this.mParent, false);
        this.mParent.addView(hybrid);
        return hybrid;
    }

    private TextView inflateOverflowNumber() {
        LayoutInflater inflater = (LayoutInflater) this.mContext.getSystemService(LayoutInflater.class);
        TextView numberView = (TextView) inflater.inflate(R.layout.hybrid_overflow_number, this.mParent, false);
        this.mParent.addView(numberView);
        updateOverFlowNumberColor(numberView);
        return numberView;
    }

    private void updateOverFlowNumberColor(TextView numberView) {
        numberView.setTextColor(this.mOverflowNumberColor);
    }

    public void setOverflowNumberColor(TextView numberView, int overflowNumberColor) {
        this.mOverflowNumberColor = overflowNumberColor;
        if (numberView == null) {
            return;
        }
        updateOverFlowNumberColor(numberView);
    }

    public HybridNotificationView bindFromNotification(HybridNotificationView reusableView, Notification notification) {
        if (reusableView == null) {
            reusableView = inflateHybridView();
        }
        CharSequence titleText = resolveTitle(notification);
        CharSequence contentText = resolveText(notification);
        reusableView.bind(titleText, contentText);
        return reusableView;
    }

    private CharSequence resolveText(Notification notification) {
        CharSequence contentText = notification.extras.getCharSequence("android.text");
        if (contentText == null) {
            return notification.extras.getCharSequence("android.bigText");
        }
        return contentText;
    }

    private CharSequence resolveTitle(Notification notification) {
        CharSequence titleText = notification.extras.getCharSequence("android.title");
        if (titleText == null) {
            return notification.extras.getCharSequence("android.title.big");
        }
        return titleText;
    }

    public TextView bindOverflowNumber(TextView reusableView, int number) {
        if (reusableView == null) {
            reusableView = inflateOverflowNumber();
        }
        String text = this.mContext.getResources().getString(R.string.notification_group_overflow_indicator, Integer.valueOf(number));
        if (!text.equals(reusableView.getText())) {
            reusableView.setText(text);
        }
        String contentDescription = String.format(this.mContext.getResources().getQuantityString(R.plurals.notification_group_overflow_description, number), Integer.valueOf(number));
        reusableView.setContentDescription(contentDescription);
        return reusableView;
    }
}
