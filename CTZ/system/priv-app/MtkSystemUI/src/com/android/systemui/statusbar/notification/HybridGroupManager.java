package com.android.systemui.statusbar.notification;

import android.app.Notification;
import android.content.Context;
import android.content.res.Resources;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.systemui.R;
import java.util.function.Consumer;

/* loaded from: classes.dex */
public class HybridGroupManager {
    private final Context mContext;
    private float mDarkAmount = 0.0f;
    private final NotificationDozeHelper mDozer = new NotificationDozeHelper();
    private int mOverflowNumberColor;
    private int mOverflowNumberColorDark;
    private int mOverflowNumberPadding;
    private int mOverflowNumberPaddingDark;
    private float mOverflowNumberSize;
    private float mOverflowNumberSizeDark;
    private final ViewGroup mParent;

    public HybridGroupManager(Context context, ViewGroup viewGroup) {
        this.mContext = context;
        this.mParent = viewGroup;
        initDimens();
    }

    public void initDimens() {
        Resources resources = this.mContext.getResources();
        this.mOverflowNumberSize = resources.getDimensionPixelSize(R.dimen.group_overflow_number_size);
        this.mOverflowNumberSizeDark = resources.getDimensionPixelSize(R.dimen.group_overflow_number_size_dark);
        this.mOverflowNumberPadding = resources.getDimensionPixelSize(R.dimen.group_overflow_number_padding);
        this.mOverflowNumberPaddingDark = this.mOverflowNumberPadding + resources.getDimensionPixelSize(R.dimen.group_overflow_number_extra_padding_dark);
    }

    private HybridNotificationView inflateHybridViewWithStyle(int i) {
        HybridNotificationView hybridNotificationView = (HybridNotificationView) ((LayoutInflater) new ContextThemeWrapper(this.mContext, i).getSystemService(LayoutInflater.class)).inflate(R.layout.hybrid_notification, this.mParent, false);
        this.mParent.addView(hybridNotificationView);
        return hybridNotificationView;
    }

    private TextView inflateOverflowNumber() {
        TextView textView = (TextView) ((LayoutInflater) this.mContext.getSystemService(LayoutInflater.class)).inflate(R.layout.hybrid_overflow_number, this.mParent, false);
        this.mParent.addView(textView);
        updateOverFlowNumberColor(textView);
        return textView;
    }

    private void updateOverFlowNumberColor(TextView textView) {
        textView.setTextColor(NotificationUtils.interpolateColors(this.mOverflowNumberColor, this.mOverflowNumberColorDark, this.mDarkAmount));
    }

    public void setOverflowNumberColor(TextView textView, int i, int i2) {
        this.mOverflowNumberColor = i;
        this.mOverflowNumberColorDark = i2;
        if (textView != null) {
            updateOverFlowNumberColor(textView);
        }
    }

    public HybridNotificationView bindFromNotification(HybridNotificationView hybridNotificationView, Notification notification) {
        return bindFromNotificationWithStyle(hybridNotificationView, notification, R.style.HybridNotification);
    }

    public HybridNotificationView bindAmbientFromNotification(HybridNotificationView hybridNotificationView, Notification notification) {
        return bindFromNotificationWithStyle(hybridNotificationView, notification, 2131886288);
    }

    private HybridNotificationView bindFromNotificationWithStyle(HybridNotificationView hybridNotificationView, Notification notification, int i) {
        if (hybridNotificationView == null) {
            hybridNotificationView = inflateHybridViewWithStyle(i);
        }
        hybridNotificationView.bind(resolveTitle(notification), resolveText(notification));
        return hybridNotificationView;
    }

    private CharSequence resolveText(Notification notification) {
        CharSequence charSequence = notification.extras.getCharSequence("android.text");
        if (charSequence == null) {
            return notification.extras.getCharSequence("android.bigText");
        }
        return charSequence;
    }

    private CharSequence resolveTitle(Notification notification) {
        CharSequence charSequence = notification.extras.getCharSequence("android.title");
        if (charSequence == null) {
            return notification.extras.getCharSequence("android.title.big");
        }
        return charSequence;
    }

    public TextView bindOverflowNumber(TextView textView, int i) throws Resources.NotFoundException {
        if (textView == null) {
            textView = inflateOverflowNumber();
        }
        String string = this.mContext.getResources().getString(R.string.notification_group_overflow_indicator, Integer.valueOf(i));
        if (!string.equals(textView.getText())) {
            textView.setText(string);
        }
        textView.setContentDescription(String.format(this.mContext.getResources().getQuantityString(R.plurals.notification_group_overflow_description, i), Integer.valueOf(i)));
        return textView;
    }

    public TextView bindOverflowNumberAmbient(TextView textView, Notification notification, int i) throws Resources.NotFoundException {
        String string = this.mContext.getResources().getString(R.string.notification_group_overflow_indicator_ambient, resolveTitle(notification), Integer.valueOf(i));
        if (!string.equals(textView.getText())) {
            textView.setText(string);
        }
        return textView;
    }

    public void setOverflowNumberDark(final TextView textView, boolean z, boolean z2, long j) {
        this.mDozer.setIntensityDark(new Consumer() { // from class: com.android.systemui.statusbar.notification.-$$Lambda$HybridGroupManager$F4-Ues96OqS7pldtinZIwLPoyPg
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                HybridGroupManager.lambda$setOverflowNumberDark$0(this.f$0, textView, (Float) obj);
            }
        }, z, z2, j, textView);
        textView.setTextSize(0, z ? this.mOverflowNumberSizeDark : this.mOverflowNumberSize);
        textView.setPaddingRelative(textView.getPaddingStart(), textView.getPaddingTop(), z ? this.mOverflowNumberPaddingDark : this.mOverflowNumberPadding, textView.getPaddingBottom());
    }

    public static /* synthetic */ void lambda$setOverflowNumberDark$0(HybridGroupManager hybridGroupManager, TextView textView, Float f) {
        hybridGroupManager.mDarkAmount = f.floatValue();
        hybridGroupManager.updateOverFlowNumberColor(textView);
    }
}
