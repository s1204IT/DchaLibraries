package com.android.internal.widget;

import android.app.Notification;
import android.content.Context;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Pools;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import com.android.internal.R;

@RemoteViews.RemoteView
public class MessagingTextMessage extends ImageFloatingTextView implements MessagingMessage {
    private static Pools.SimplePool<MessagingTextMessage> sInstancePool = new Pools.SynchronizedPool(20);
    private final MessagingMessageState mState;

    public MessagingTextMessage(Context context) {
        super(context);
        this.mState = new MessagingMessageState(this);
    }

    public MessagingTextMessage(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mState = new MessagingMessageState(this);
    }

    public MessagingTextMessage(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mState = new MessagingMessageState(this);
    }

    public MessagingTextMessage(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mState = new MessagingMessageState(this);
    }

    @Override
    public MessagingMessageState getState() {
        return this.mState;
    }

    @Override
    public boolean setMessage(Notification.MessagingStyle.Message message) {
        super.setMessage(message);
        setText(message.getText());
        return true;
    }

    static MessagingMessage createMessage(MessagingLayout messagingLayout, Notification.MessagingStyle.Message message) {
        MessagingLinearLayout messagingLinearLayout = messagingLayout.getMessagingLinearLayout();
        MessagingTextMessage messagingTextMessageAcquire = sInstancePool.acquire();
        if (messagingTextMessageAcquire == null) {
            messagingTextMessageAcquire = (MessagingTextMessage) LayoutInflater.from(messagingLayout.getContext()).inflate(R.layout.notification_template_messaging_text_message, (ViewGroup) messagingLinearLayout, false);
            messagingTextMessageAcquire.addOnLayoutChangeListener(MessagingLayout.MESSAGING_PROPERTY_ANIMATOR);
        }
        messagingTextMessageAcquire.setMessage(message);
        return messagingTextMessageAcquire;
    }

    @Override
    public void recycle() {
        super.recycle();
        sInstancePool.release(this);
    }

    public static void dropCache() {
        sInstancePool = new Pools.SynchronizedPool(10);
    }

    @Override
    public int getMeasuredType() {
        Layout layout;
        if ((!(getMeasuredHeight() < (getLayoutHeight() + getPaddingTop()) + getPaddingBottom()) || getLineCount() > 1) && (layout = getLayout()) != null) {
            return layout.getEllipsisCount(layout.getLineCount() - 1) > 0 ? 1 : 0;
        }
        return 2;
    }

    @Override
    public void setMaxDisplayedLines(int i) {
        setMaxLines(i);
    }

    @Override
    public int getConsumedLines() {
        return getLineCount();
    }

    public int getLayoutHeight() {
        Layout layout = getLayout();
        if (layout == null) {
            return 0;
        }
        return layout.getHeight();
    }

    @Override
    public void setColor(int i) {
        setTextColor(i);
    }
}
