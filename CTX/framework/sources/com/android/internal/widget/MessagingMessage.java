package com.android.internal.widget;

import android.app.ActivityManager;
import android.app.Notification;
import android.view.View;
import com.android.internal.widget.MessagingLinearLayout;
import java.util.Objects;

public interface MessagingMessage extends MessagingLinearLayout.MessagingChild {
    public static final String IMAGE_MIME_TYPE_PREFIX = "image/";

    MessagingMessageState getState();

    int getVisibility();

    void setVisibility(int i);

    static MessagingMessage createMessage(MessagingLayout messagingLayout, Notification.MessagingStyle.Message message) {
        if (hasImage(message) && !ActivityManager.isLowRamDeviceStatic()) {
            return MessagingImageMessage.createMessage(messagingLayout, message);
        }
        return MessagingTextMessage.createMessage(messagingLayout, message);
    }

    static void dropCache() {
        MessagingTextMessage.dropCache();
        MessagingImageMessage.dropCache();
    }

    static boolean hasImage(Notification.MessagingStyle.Message message) {
        return (message.getDataUri() == null || message.getDataMimeType() == null || !message.getDataMimeType().startsWith(IMAGE_MIME_TYPE_PREFIX)) ? false : true;
    }

    default boolean setMessage(Notification.MessagingStyle.Message message) {
        getState().setMessage(message);
        return true;
    }

    default Notification.MessagingStyle.Message getMessage() {
        return getState().getMessage();
    }

    default boolean sameAs(Notification.MessagingStyle.Message message) {
        Notification.MessagingStyle.Message message2 = getMessage();
        if (Objects.equals(message.getText(), message2.getText()) && Objects.equals(message.getSender(), message2.getSender())) {
            return ((message.isRemoteInputHistory() != message2.isRemoteInputHistory()) || Objects.equals(Long.valueOf(message.getTimestamp()), Long.valueOf(message2.getTimestamp()))) && Objects.equals(message.getDataMimeType(), message2.getDataMimeType()) && Objects.equals(message.getDataUri(), message2.getDataUri());
        }
        return false;
    }

    default boolean sameAs(MessagingMessage messagingMessage) {
        return sameAs(messagingMessage.getMessage());
    }

    default void removeMessage() {
        getGroup().removeMessage(this);
    }

    default void setMessagingGroup(MessagingGroup messagingGroup) {
        getState().setGroup(messagingGroup);
    }

    default void setIsHistoric(boolean z) {
        getState().setIsHistoric(z);
    }

    default MessagingGroup getGroup() {
        return getState().getGroup();
    }

    default void setIsHidingAnimated(boolean z) {
        getState().setIsHidingAnimated(z);
    }

    @Override
    default boolean isHidingAnimated() {
        return getState().isHidingAnimated();
    }

    @Override
    default void hideAnimated() {
        setIsHidingAnimated(true);
        getGroup().performRemoveAnimation(getView(), new Runnable() {
            @Override
            public final void run() {
                this.f$0.setIsHidingAnimated(false);
            }
        });
    }

    default boolean hasOverlappingRendering() {
        return false;
    }

    default void recycle() {
        getState().recycle();
    }

    default View getView() {
        return (View) this;
    }

    default void setColor(int i) {
    }
}
