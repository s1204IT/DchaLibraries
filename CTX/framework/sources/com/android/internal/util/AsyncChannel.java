package com.android.internal.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Slog;
import java.util.Objects;
import java.util.Stack;

public class AsyncChannel {
    private static final int BASE = 69632;
    public static final int CMD_CHANNEL_DISCONNECT = 69635;
    public static final int CMD_CHANNEL_DISCONNECTED = 69636;
    public static final int CMD_CHANNEL_FULLY_CONNECTED = 69634;
    public static final int CMD_CHANNEL_FULL_CONNECTION = 69633;
    public static final int CMD_CHANNEL_HALF_CONNECTED = 69632;
    private static final int CMD_TO_STRING_COUNT = 5;
    private static final boolean DBG = false;
    public static final int STATUS_BINDING_UNSUCCESSFUL = 1;
    public static final int STATUS_FULL_CONNECTION_REFUSED_ALREADY_CONNECTED = 3;
    public static final int STATUS_REMOTE_DISCONNECTION = 4;
    public static final int STATUS_SEND_UNSUCCESSFUL = 2;
    public static final int STATUS_SUCCESSFUL = 0;
    private static final String TAG = "AsyncChannel";
    private static String[] sCmdToString = new String[5];
    private AsyncChannelConnection mConnection;
    private DeathMonitor mDeathMonitor;
    private Messenger mDstMessenger;
    private Context mSrcContext;
    private Handler mSrcHandler;
    private Messenger mSrcMessenger;

    static {
        sCmdToString[0] = "CMD_CHANNEL_HALF_CONNECTED";
        sCmdToString[1] = "CMD_CHANNEL_FULL_CONNECTION";
        sCmdToString[2] = "CMD_CHANNEL_FULLY_CONNECTED";
        sCmdToString[3] = "CMD_CHANNEL_DISCONNECT";
        sCmdToString[4] = "CMD_CHANNEL_DISCONNECTED";
    }

    protected static String cmdToString(int i) {
        int i2 = i - 69632;
        if (i2 >= 0 && i2 < sCmdToString.length) {
            return sCmdToString[i2];
        }
        return null;
    }

    public int connectSrcHandlerToPackageSync(Context context, Handler handler, String str, String str2) {
        this.mConnection = new AsyncChannelConnection();
        this.mSrcContext = context;
        this.mSrcHandler = handler;
        this.mSrcMessenger = new Messenger(handler);
        this.mDstMessenger = null;
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(str, str2);
        return !context.bindService(intent, this.mConnection, 1) ? 1 : 0;
    }

    public int connectSync(Context context, Handler handler, Messenger messenger) {
        connected(context, handler, messenger);
        return 0;
    }

    public int connectSync(Context context, Handler handler, Handler handler2) {
        return connectSync(context, handler, new Messenger(handler2));
    }

    public int fullyConnectSync(Context context, Handler handler, Handler handler2) {
        int iConnectSync = connectSync(context, handler, handler2);
        if (iConnectSync == 0) {
            return sendMessageSynchronously(CMD_CHANNEL_FULL_CONNECTION).arg1;
        }
        return iConnectSync;
    }

    public void connect(Context context, Handler handler, String str, String str2) {
        new Thread(new Runnable(context, handler, str, str2) {
            String mDstClassName;
            String mDstPackageName;
            Context mSrcCtx;
            Handler mSrcHdlr;

            {
                this.mSrcCtx = context;
                this.mSrcHdlr = handler;
                this.mDstPackageName = str;
                this.mDstClassName = str2;
            }

            @Override
            public void run() {
                AsyncChannel.this.replyHalfConnected(AsyncChannel.this.connectSrcHandlerToPackageSync(this.mSrcCtx, this.mSrcHdlr, this.mDstPackageName, this.mDstClassName));
            }
        }).start();
    }

    public void connect(Context context, Handler handler, Class<?> cls) {
        connect(context, handler, cls.getPackage().getName(), cls.getName());
    }

    public void connect(Context context, Handler handler, Messenger messenger) {
        connected(context, handler, messenger);
        replyHalfConnected(0);
    }

    public void connected(Context context, Handler handler, Messenger messenger) {
        this.mSrcContext = context;
        this.mSrcHandler = handler;
        this.mSrcMessenger = new Messenger(this.mSrcHandler);
        this.mDstMessenger = messenger;
    }

    public void connect(Context context, Handler handler, Handler handler2) {
        connect(context, handler, new Messenger(handler2));
    }

    public void connect(AsyncService asyncService, Messenger messenger) {
        connect(asyncService, asyncService.getHandler(), messenger);
    }

    public void disconnected() {
        this.mSrcContext = null;
        this.mSrcHandler = null;
        this.mSrcMessenger = null;
        this.mDstMessenger = null;
        this.mDeathMonitor = null;
        this.mConnection = null;
    }

    public void disconnect() {
        if (this.mConnection != null && this.mSrcContext != null) {
            this.mSrcContext.unbindService(this.mConnection);
            this.mConnection = null;
        }
        try {
            Message messageObtain = Message.obtain();
            messageObtain.what = CMD_CHANNEL_DISCONNECTED;
            messageObtain.replyTo = this.mSrcMessenger;
            this.mDstMessenger.send(messageObtain);
        } catch (Exception e) {
        }
        replyDisconnected(0);
        this.mSrcHandler = null;
        if (this.mConnection == null && this.mDstMessenger != null && this.mDeathMonitor != null) {
            this.mDstMessenger.getBinder().unlinkToDeath(this.mDeathMonitor, 0);
            this.mDeathMonitor = null;
        }
    }

    public void sendMessage(Message message) {
        message.replyTo = this.mSrcMessenger;
        try {
            this.mDstMessenger.send(message);
        } catch (RemoteException e) {
            replyDisconnected(2);
        }
    }

    public void sendMessage(int i) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        sendMessage(messageObtain);
    }

    public void sendMessage(int i, int i2) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        sendMessage(messageObtain);
    }

    public void sendMessage(int i, int i2, int i3) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        messageObtain.arg2 = i3;
        sendMessage(messageObtain);
    }

    public void sendMessage(int i, int i2, int i3, Object obj) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        messageObtain.arg2 = i3;
        messageObtain.obj = obj;
        sendMessage(messageObtain);
    }

    public void sendMessage(int i, Object obj) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.obj = obj;
        sendMessage(messageObtain);
    }

    public void replyToMessage(Message message, Message message2) {
        try {
            message2.replyTo = this.mSrcMessenger;
            message.replyTo.send(message2);
        } catch (RemoteException e) {
            log("TODO: handle replyToMessage RemoteException" + e);
            e.printStackTrace();
        }
    }

    public void replyToMessage(Message message, int i) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        replyToMessage(message, messageObtain);
    }

    public void replyToMessage(Message message, int i, int i2) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        replyToMessage(message, messageObtain);
    }

    public void replyToMessage(Message message, int i, int i2, int i3) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        messageObtain.arg2 = i3;
        replyToMessage(message, messageObtain);
    }

    public void replyToMessage(Message message, int i, int i2, int i3, Object obj) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        messageObtain.arg2 = i3;
        messageObtain.obj = obj;
        replyToMessage(message, messageObtain);
    }

    public void replyToMessage(Message message, int i, Object obj) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.obj = obj;
        replyToMessage(message, messageObtain);
    }

    public Message sendMessageSynchronously(Message message) {
        return SyncMessenger.sendMessageSynchronously(this.mDstMessenger, message);
    }

    public Message sendMessageSynchronously(int i) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        return sendMessageSynchronously(messageObtain);
    }

    public Message sendMessageSynchronously(int i, int i2) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        return sendMessageSynchronously(messageObtain);
    }

    public Message sendMessageSynchronously(int i, int i2, int i3) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        messageObtain.arg2 = i3;
        return sendMessageSynchronously(messageObtain);
    }

    public Message sendMessageSynchronously(int i, int i2, int i3, Object obj) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.arg1 = i2;
        messageObtain.arg2 = i3;
        messageObtain.obj = obj;
        return sendMessageSynchronously(messageObtain);
    }

    public Message sendMessageSynchronously(int i, Object obj) {
        Message messageObtain = Message.obtain();
        messageObtain.what = i;
        messageObtain.obj = obj;
        return sendMessageSynchronously(messageObtain);
    }

    private static class SyncMessenger {
        private SyncHandler mHandler;
        private HandlerThread mHandlerThread;
        private Messenger mMessenger;
        private static Stack<SyncMessenger> sStack = new Stack<>();
        private static int sCount = 0;

        private SyncMessenger() {
        }

        private class SyncHandler extends Handler {
            private Object mLockObject;
            private Message mResultMsg;

            private SyncHandler(Looper looper) {
                super(looper);
                this.mLockObject = new Object();
            }

            @Override
            public void handleMessage(Message message) {
                Message messageObtain = Message.obtain();
                messageObtain.copyFrom(message);
                synchronized (this.mLockObject) {
                    this.mResultMsg = messageObtain;
                    this.mLockObject.notify();
                }
            }
        }

        private static SyncMessenger obtain() {
            SyncMessenger syncMessengerPop;
            synchronized (sStack) {
                if (sStack.isEmpty()) {
                    syncMessengerPop = new SyncMessenger();
                    StringBuilder sb = new StringBuilder();
                    sb.append("SyncHandler-");
                    int i = sCount;
                    sCount = i + 1;
                    sb.append(i);
                    syncMessengerPop.mHandlerThread = new HandlerThread(sb.toString());
                    syncMessengerPop.mHandlerThread.start();
                    Objects.requireNonNull(syncMessengerPop);
                    syncMessengerPop.mHandler = new SyncHandler(syncMessengerPop.mHandlerThread.getLooper());
                    syncMessengerPop.mMessenger = new Messenger(syncMessengerPop.mHandler);
                } else {
                    syncMessengerPop = sStack.pop();
                }
            }
            return syncMessengerPop;
        }

        private void recycle() {
            synchronized (sStack) {
                sStack.push(this);
            }
        }

        private static android.os.Message sendMessageSynchronously(android.os.Messenger r5, android.os.Message r6) {
            r0 = obtain();
            r1 = null;
            if (r5 != null && r6 != null) {
                r6.replyTo = r0.mMessenger;
                r2 = r0.mHandler.mLockObject;
                synchronized (r2) {
                    ;
                    if (r0.mHandler.mResultMsg != null) {
                        android.util.Slog.wtf(com.android.internal.util.AsyncChannel.TAG, "mResultMsg should be null here");
                        r0.mHandler.mResultMsg = null;
                    }
                    r5.send(r6);
                    r0.mHandler.mLockObject.wait();
                    r5 = r0.mHandler.mResultMsg;
                    r0.mHandler.mResultMsg = null;
                    r1 = r5;
                }
            }
            r0.recycle();
            return r1;
        }
    }

    private void replyHalfConnected(int i) {
        Message messageObtainMessage = this.mSrcHandler.obtainMessage(69632);
        messageObtainMessage.arg1 = i;
        messageObtainMessage.obj = this;
        messageObtainMessage.replyTo = this.mDstMessenger;
        if (!linkToDeathMonitor()) {
            messageObtainMessage.arg1 = 1;
        }
        this.mSrcHandler.sendMessage(messageObtainMessage);
    }

    private boolean linkToDeathMonitor() {
        if (this.mConnection == null && this.mDeathMonitor == null) {
            this.mDeathMonitor = new DeathMonitor();
            try {
                this.mDstMessenger.getBinder().linkToDeath(this.mDeathMonitor, 0);
                return true;
            } catch (RemoteException e) {
                this.mDeathMonitor = null;
                return false;
            }
        }
        return true;
    }

    private void replyDisconnected(int i) {
        if (this.mSrcHandler == null) {
            return;
        }
        Message messageObtainMessage = this.mSrcHandler.obtainMessage(CMD_CHANNEL_DISCONNECTED);
        messageObtainMessage.arg1 = i;
        messageObtainMessage.obj = this;
        messageObtainMessage.replyTo = this.mDstMessenger;
        this.mSrcHandler.sendMessage(messageObtainMessage);
    }

    class AsyncChannelConnection implements ServiceConnection {
        AsyncChannelConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            AsyncChannel.this.mDstMessenger = new Messenger(iBinder);
            AsyncChannel.this.replyHalfConnected(0);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            AsyncChannel.this.replyDisconnected(0);
        }
    }

    private static void log(String str) {
        Slog.d(TAG, str);
    }

    private final class DeathMonitor implements IBinder.DeathRecipient {
        DeathMonitor() {
        }

        @Override
        public void binderDied() {
            AsyncChannel.this.replyDisconnected(4);
        }
    }
}
