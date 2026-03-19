package com.android.server.wifi.util;

import android.os.Message;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.util.AsyncChannel;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiLog;

public class WifiAsyncChannel extends AsyncChannel {
    private static final String LOG_TAG = "WifiAsyncChannel";
    private WifiLog mLog;
    private String mTag;

    public WifiAsyncChannel(String str) {
        this.mTag = "WifiAsyncChannel." + str;
    }

    private WifiLog getOrInitLog() {
        if (this.mLog == null) {
            this.mLog = WifiInjector.getInstance().makeLog(this.mTag);
        }
        return this.mLog;
    }

    public void sendMessage(Message message) {
        getOrInitLog().trace("sendMessage message=%").c(message.what).flush();
        super.sendMessage(message);
    }

    public void replyToMessage(Message message, Message message2) {
        getOrInitLog().trace("replyToMessage recvdMessage=% sendingUid=% sentMessage=%").c(message.what).c(message.sendingUid).c(message2.what).flush();
        super.replyToMessage(message, message2);
    }

    public Message sendMessageSynchronously(Message message) {
        getOrInitLog().trace("sendMessageSynchronously.send message=%").c(message.what).flush();
        Message messageSendMessageSynchronously = super.sendMessageSynchronously(message);
        if (messageSendMessageSynchronously != null) {
            getOrInitLog().trace("sendMessageSynchronously.recv message=% sendingUid=%").c(messageSendMessageSynchronously.what).c(messageSendMessageSynchronously.sendingUid).flush();
        }
        return messageSendMessageSynchronously;
    }

    @VisibleForTesting
    public void setWifiLog(WifiLog wifiLog) {
        this.mLog = wifiLog;
    }
}
