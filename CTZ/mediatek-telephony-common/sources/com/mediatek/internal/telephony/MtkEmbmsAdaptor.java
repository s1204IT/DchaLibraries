package com.mediatek.internal.telephony;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.CommandException;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.SubscriptionController;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MtkEmbmsAdaptor {
    private static final int MSG_ID_EVENT_IND = 2;
    private static final int MSG_ID_EVENT_REQUEST = 0;
    private static final int MSG_ID_EVENT_RESPONSE = 1;
    private static final String TAG = "MtkEmbmsAdaptor";
    private static MtkEmbmsAdaptor sInstance = null;
    private MtkEmbmsAdaptEventHandler mEventHandler;
    private SubscriptionController mSubscriptionController;

    private MtkEmbmsAdaptor(Context context, CommandsInterface[] commandsInterfaceArr) {
        this.mEventHandler = null;
        Rlog.i(TAG, "construtor 2 parameter is called - start");
        this.mEventHandler = new MtkEmbmsAdaptEventHandler();
        this.mEventHandler.setRil(context, commandsInterfaceArr);
        this.mSubscriptionController = SubscriptionController.getInstance();
        new Thread() {
            @Override
            public void run() throws Throwable {
                MtkEmbmsAdaptor.this.new ServerTask().listenConnection(MtkEmbmsAdaptor.this.mEventHandler);
            }
        }.start();
        int phoneCount = TelephonyManager.getDefault().getPhoneCount();
        for (int i = 0; i < phoneCount; i++) {
            ((MtkRIL) commandsInterfaceArr[i]).setAtInfoNotification(this.mEventHandler, 2, Integer.valueOf(i));
        }
        Rlog.i(TAG, "construtor is called - end");
    }

    public static MtkEmbmsAdaptor getDefault(Context context, CommandsInterface[] commandsInterfaceArr) {
        Rlog.d(TAG, "getDefault()");
        if (sInstance == null) {
            sInstance = new MtkEmbmsAdaptor(context, commandsInterfaceArr);
        }
        return sInstance;
    }

    private String messageToString(Message message) {
        switch (message.what) {
            case 0:
                return "MSG_ID_EVENT_REQUEST";
            case 1:
                return "MSG_ID_EVENT_RESPONSE";
            case 2:
                return "MSG_ID_EVENT_IND";
            default:
                return "UNKNOWN";
        }
    }

    public class ServerTask {
        public static final String HOST_NAME = "/dev/socket/embmsd";

        public ServerTask() {
        }

        public void listenConnection(MtkEmbmsAdaptEventHandler mtkEmbmsAdaptEventHandler) throws Throwable {
            LocalServerSocket localServerSocket;
            Rlog.i(MtkEmbmsAdaptor.TAG, "listenConnection() - start");
            ExecutorService executorServiceNewCachedThreadPool = Executors.newCachedThreadPool();
            LocalServerSocket localServerSocket2 = null;
            try {
                try {
                    try {
                        localServerSocket = new LocalServerSocket(HOST_NAME);
                        while (true) {
                            try {
                                LocalSocket localSocketAccept = localServerSocket.accept();
                                Rlog.d(MtkEmbmsAdaptor.TAG, "There is a client is accepted: " + localSocketAccept.toString());
                                executorServiceNewCachedThreadPool.execute(MtkEmbmsAdaptor.this.new ConnectionHandler(localSocketAccept, mtkEmbmsAdaptEventHandler));
                            } catch (IOException e) {
                                e = e;
                                localServerSocket2 = localServerSocket;
                                Rlog.e(MtkEmbmsAdaptor.TAG, "listenConnection catch IOException");
                                e.printStackTrace();
                                Rlog.d(MtkEmbmsAdaptor.TAG, "listenConnection finally!!");
                                if (executorServiceNewCachedThreadPool != null) {
                                    executorServiceNewCachedThreadPool.shutdown();
                                }
                                if (localServerSocket2 != null) {
                                    localServerSocket2.close();
                                }
                                Rlog.i(MtkEmbmsAdaptor.TAG, "listenConnection() - end");
                                return;
                            } catch (Exception e2) {
                                e = e2;
                                localServerSocket2 = localServerSocket;
                                Rlog.e(MtkEmbmsAdaptor.TAG, "listenConnection catch Exception");
                                e.printStackTrace();
                                Rlog.d(MtkEmbmsAdaptor.TAG, "listenConnection finally!!");
                                if (executorServiceNewCachedThreadPool != null) {
                                    executorServiceNewCachedThreadPool.shutdown();
                                }
                                if (localServerSocket2 != null) {
                                    localServerSocket2.close();
                                }
                                Rlog.i(MtkEmbmsAdaptor.TAG, "listenConnection() - end");
                                return;
                            } catch (Throwable th) {
                                th = th;
                                Rlog.d(MtkEmbmsAdaptor.TAG, "listenConnection finally!!");
                                if (executorServiceNewCachedThreadPool != null) {
                                    executorServiceNewCachedThreadPool.shutdown();
                                }
                                if (localServerSocket != null) {
                                    try {
                                        localServerSocket.close();
                                    } catch (IOException e3) {
                                        e3.printStackTrace();
                                    }
                                }
                                throw th;
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                        localServerSocket = null;
                    }
                } catch (IOException e4) {
                    e = e4;
                } catch (Exception e5) {
                    e = e5;
                }
            } catch (IOException e6) {
                e6.printStackTrace();
            }
        }
    }

    public class ConnectionHandler implements Runnable {
        private MtkEmbmsAdaptEventHandler mEventHandler;
        private LocalSocket mSocket;

        public ConnectionHandler(LocalSocket localSocket, MtkEmbmsAdaptEventHandler mtkEmbmsAdaptEventHandler) {
            this.mSocket = localSocket;
            this.mEventHandler = mtkEmbmsAdaptEventHandler;
        }

        @Override
        public void run() {
            Rlog.i(MtkEmbmsAdaptor.TAG, "New connection: " + this.mSocket.toString());
            try {
                MtkEmbmsAdaptIoThread mtkEmbmsAdaptIoThread = MtkEmbmsAdaptor.this.new MtkEmbmsAdaptIoThread(ServerTask.HOST_NAME, this.mSocket.getInputStream(), this.mSocket.getOutputStream(), this.mEventHandler);
                this.mEventHandler.setDataStream(mtkEmbmsAdaptIoThread);
                mtkEmbmsAdaptIoThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class MtkEmbmsAdaptIoThread extends Thread {
        private static final int MAX_DATA_LENGTH = 4096;
        private MtkEmbmsAdaptEventHandler mEventHandler;
        private InputStream mInput;
        private String mName;
        private OutputStream mOutput;
        private byte[] readBuffer;
        private boolean mIsContinue = true;
        private final Object mOutputLock = new Object();

        public MtkEmbmsAdaptIoThread(String str, InputStream inputStream, OutputStream outputStream, MtkEmbmsAdaptEventHandler mtkEmbmsAdaptEventHandler) {
            this.mName = "";
            this.mInput = null;
            this.mOutput = null;
            this.mEventHandler = null;
            this.readBuffer = null;
            this.mName = str;
            this.mInput = inputStream;
            this.mOutput = outputStream;
            this.mEventHandler = mtkEmbmsAdaptEventHandler;
            Rlog.i(MtkEmbmsAdaptor.TAG, "MtkEmbmsAdaptIoThread constructor is called.");
            this.readBuffer = new byte[4096];
        }

        public void terminate() {
            Rlog.i(MtkEmbmsAdaptor.TAG, "MtkEmbmsAdaptIoThread terminate.");
            this.mIsContinue = false;
        }

        @Override
        public void run() {
            Rlog.i(MtkEmbmsAdaptor.TAG, "MtkEmbmsAdaptIoThread running.");
            while (this.mIsContinue) {
                try {
                    try {
                        int i = this.mInput.read(this.readBuffer, 0, 4096);
                        if (i < 0) {
                            Rlog.e(MtkEmbmsAdaptor.TAG, "readEvent(), fail to read and throw exception");
                            return;
                        } else if (i > 0) {
                            try {
                                handleInput(new String(this.readBuffer, 0, i));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (IOException e2) {
                        Rlog.e(MtkEmbmsAdaptor.TAG, "MtkEmbmsAdaptIoThread IOException.");
                        e2.printStackTrace();
                        Rlog.e(MtkEmbmsAdaptor.TAG, "Socket disconnected.");
                        terminate();
                    }
                } catch (Exception e3) {
                    Rlog.e(MtkEmbmsAdaptor.TAG, "MtkEmbmsAdaptIoThread Exception.");
                    e3.printStackTrace();
                }
            }
        }

        protected void handleInput(String str) {
            Rlog.d(MtkEmbmsAdaptor.TAG, "process input: RCV <-(" + str + "),length:" + str.length());
            this.mEventHandler.sendMessage(this.mEventHandler.obtainMessage(0, str.trim()));
        }

        public void sendCommand(String str) {
            Rlog.d(MtkEmbmsAdaptor.TAG, "SND -> (" + str + ")");
            synchronized (this.mOutputLock) {
                if (this.mOutput == null) {
                    Rlog.e(MtkEmbmsAdaptor.TAG, "missing SIM output stream");
                } else {
                    try {
                        this.mOutput.write(str.getBytes(StandardCharsets.UTF_8));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public class MtkEmbmsAdaptEventHandler extends Handler {
        private MtkEmbmsAdaptIoThread mAdaptorIoThread = null;
        private CommandsInterface[] mCis;
        private Context mContext;

        public MtkEmbmsAdaptEventHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            Rlog.d(MtkEmbmsAdaptor.TAG, "handleMessage: " + MtkEmbmsAdaptor.this.messageToString(message) + " = " + message);
            switch (message.what) {
                case 0:
                    String str = (String) message.obj;
                    Rlog.i(MtkEmbmsAdaptor.TAG, "MSG_ID_EVENT_REQUEST data: " + str);
                    int defaultDataSubId = MtkEmbmsAdaptor.this.mSubscriptionController.getDefaultDataSubId();
                    int slotIndex = -1;
                    if (defaultDataSubId != -1) {
                        slotIndex = MtkEmbmsAdaptor.this.mSubscriptionController.getSlotIndex(defaultDataSubId);
                    } else {
                        Rlog.e(MtkEmbmsAdaptor.TAG, "getDefaultDataSubId fail: " + defaultDataSubId);
                    }
                    if (!SubscriptionManager.isValidSlotIndex(slotIndex)) {
                        Rlog.e(MtkEmbmsAdaptor.TAG, "inValidSlotIndex:" + slotIndex);
                        sendFailureCmd();
                    } else {
                        this.mCis[slotIndex].sendEmbmsAtCommand(str, obtainMessage(1, Integer.valueOf(slotIndex)));
                    }
                    break;
                case 1:
                    AsyncResult asyncResult = (AsyncResult) message.obj;
                    ((Integer) asyncResult.userObj).intValue();
                    String str2 = (String) asyncResult.result;
                    Rlog.i(MtkEmbmsAdaptor.TAG, "MSG_ID_EVENT_RESPONSE data: " + str2);
                    if ((asyncResult.exception instanceof CommandException) && asyncResult.exception.getCommandError() == CommandException.Error.RADIO_NOT_AVAILABLE) {
                        Rlog.e(MtkEmbmsAdaptor.TAG, "MSG_ID_EVENT_RESPONSE exception: " + asyncResult.exception.getCommandError());
                        sendFailureCmd();
                    } else if (str2 != null) {
                        sendCommand(str2);
                    } else {
                        sendFailureCmd();
                    }
                    break;
                case 2:
                    AsyncResult asyncResult2 = (AsyncResult) message.obj;
                    ((Integer) asyncResult2.userObj).intValue();
                    String str3 = (String) asyncResult2.result;
                    Rlog.i(MtkEmbmsAdaptor.TAG, "MSG_ID_EVENT_IND data: " + str3);
                    sendCommand(str3);
                    break;
            }
        }

        private void setDataStream(MtkEmbmsAdaptIoThread mtkEmbmsAdaptIoThread) {
            this.mAdaptorIoThread = mtkEmbmsAdaptIoThread;
            Rlog.d(MtkEmbmsAdaptor.TAG, "MtkEmbmsAdaptEventHandler setDataStream done.");
        }

        private void setRil(Context context, CommandsInterface[] commandsInterfaceArr) {
            this.mContext = context;
            this.mCis = commandsInterfaceArr;
            Rlog.d(MtkEmbmsAdaptor.TAG, "MtkEmbmsAdaptEventHandler setRil done.");
        }

        public void sendCommand(String str) {
            if (this.mAdaptorIoThread != null) {
                this.mAdaptorIoThread.sendCommand(str);
            } else {
                Rlog.e(MtkEmbmsAdaptor.TAG, "sendCommand fail!! mAdaptorIoThread is null!");
            }
        }

        public void sendFailureCmd() {
            sendCommand(String.format("ERROR\n", new Object[0]));
        }
    }
}
