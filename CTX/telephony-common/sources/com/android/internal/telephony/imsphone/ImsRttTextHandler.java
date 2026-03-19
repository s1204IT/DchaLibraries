package com.android.internal.telephony.imsphone;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telecom.Connection;
import android.telephony.Rlog;
import com.android.internal.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

public class ImsRttTextHandler extends Handler {
    protected static final int APPEND_TO_NETWORK_BUFFER = 2;
    public static final int ATTEMPT_SEND_TO_NETWORK = 4;
    public static final int EXPIRE_SENT_CODEPOINT_COUNT = 5;
    protected static final int INITIALIZE = 1;
    private static final String LOG_TAG = "ImsRttTextHandler";
    public static final int MAX_BUFFERED_CHARACTER_COUNT = 5;
    public static final int MAX_BUFFERING_DELAY_MILLIS = 200;
    public static final int MAX_CODEPOINTS_PER_SECOND = 30;
    public static final int MILLIS_PER_SECOND = 1000;
    public static final int SEND_TO_INCALL = 3;
    protected static final int TEARDOWN = 9999;
    private StringBuffer mBufferedTextToIncall;
    public StringBuffer mBufferedTextToNetwork;
    public int mCodepointsAvailableForTransmission;
    public final NetworkWriter mNetworkWriter;
    private CountDownLatch mReadNotifier;
    protected InCallReaderThread mReaderThread;
    protected Connection.RttTextStream mRttTextStream;

    public interface NetworkWriter {
        void write(String str);
    }

    protected class InCallReaderThread extends Thread {
        protected final Connection.RttTextStream mReaderThreadRttTextStream;

        public InCallReaderThread(Connection.RttTextStream rttTextStream) {
            this.mReaderThreadRttTextStream = rttTextStream;
        }

        @Override
        public void run() {
            while (true) {
                try {
                    String str = this.mReaderThreadRttTextStream.read();
                    if (str == null) {
                        break;
                    }
                    if (str.length() != 0) {
                        ImsRttTextHandler.this.obtainMessage(2, str).sendToTarget();
                        if (ImsRttTextHandler.this.mReadNotifier != null) {
                            ImsRttTextHandler.this.mReadNotifier.countDown();
                        }
                    }
                } catch (IOException e) {
                    Rlog.e(ImsRttTextHandler.LOG_TAG, "RttReaderThread - IOException encountered reading from in-call: %s", e);
                    ImsRttTextHandler.this.obtainMessage(ImsRttTextHandler.TEARDOWN).sendToTarget();
                    return;
                }
            }
            if (Thread.currentThread().isInterrupted()) {
                Rlog.i(ImsRttTextHandler.LOG_TAG, "RttReaderThread - Thread interrupted. Finishing.");
            } else {
                Rlog.e(ImsRttTextHandler.LOG_TAG, "RttReaderThread - Stream closed unexpectedly. Attempt to reinitialize.");
                ImsRttTextHandler.this.obtainMessage(ImsRttTextHandler.TEARDOWN).sendToTarget();
            }
        }
    }

    @Override
    public void handleMessage(Message message) {
        int i = message.what;
        if (i != TEARDOWN) {
            switch (i) {
                case 1:
                    if (this.mRttTextStream != null || this.mReaderThread != null) {
                        Rlog.e(LOG_TAG, "RTT text stream already initialized. Ignoring.");
                    } else {
                        this.mRttTextStream = (Connection.RttTextStream) message.obj;
                        this.mReaderThread = new InCallReaderThread(this.mRttTextStream);
                        this.mReaderThread.start();
                    }
                    break;
                case 2:
                    this.mBufferedTextToNetwork.append((String) message.obj);
                    if (this.mBufferedTextToNetwork.codePointCount(0, this.mBufferedTextToNetwork.length()) >= 5) {
                        sendMessageAtFrontOfQueue(obtainMessage(4));
                    } else {
                        sendEmptyMessageDelayed(4, 200L);
                    }
                    break;
                case 3:
                    String str = (String) message.obj;
                    try {
                        this.mRttTextStream.write(str);
                    } catch (IOException e) {
                        Rlog.e(LOG_TAG, "IOException encountered writing to in-call: %s", e);
                        obtainMessage(TEARDOWN).sendToTarget();
                        this.mBufferedTextToIncall.append(str);
                        return;
                    }
                    break;
                case 4:
                    int iMin = Math.min(this.mBufferedTextToNetwork.codePointCount(0, this.mBufferedTextToNetwork.length()), this.mCodepointsAvailableForTransmission);
                    if (iMin != 0) {
                        int iOffsetByCodePoints = this.mBufferedTextToNetwork.offsetByCodePoints(0, iMin);
                        String strSubstring = this.mBufferedTextToNetwork.substring(0, iOffsetByCodePoints);
                        this.mBufferedTextToNetwork.delete(0, iOffsetByCodePoints);
                        this.mNetworkWriter.write(strSubstring);
                        this.mCodepointsAvailableForTransmission -= iMin;
                        sendMessageDelayed(obtainMessage(5, iMin, 0), 1000L);
                        break;
                    }
                    break;
                case 5:
                    this.mCodepointsAvailableForTransmission += message.arg1;
                    if (this.mCodepointsAvailableForTransmission > 0) {
                        sendMessageAtFrontOfQueue(obtainMessage(4));
                    }
                    break;
            }
            return;
        }
        try {
            if (this.mReaderThread != null) {
                this.mReaderThread.join(1000L);
            }
        } catch (InterruptedException e2) {
        }
        this.mReaderThread = null;
        this.mRttTextStream = null;
    }

    public ImsRttTextHandler(Looper looper, NetworkWriter networkWriter) {
        super(looper);
        this.mCodepointsAvailableForTransmission = 30;
        this.mBufferedTextToNetwork = new StringBuffer();
        this.mBufferedTextToIncall = new StringBuffer();
        this.mNetworkWriter = networkWriter;
    }

    public void sendToInCall(String str) {
        obtainMessage(3, str).sendToTarget();
    }

    public void initialize(Connection.RttTextStream rttTextStream) {
        obtainMessage(1, rttTextStream).sendToTarget();
    }

    public void tearDown() {
        obtainMessage(TEARDOWN).sendToTarget();
    }

    @VisibleForTesting
    public void setReadNotifier(CountDownLatch countDownLatch) {
        this.mReadNotifier = countDownLatch;
    }

    public String getNetworkBufferText() {
        return this.mBufferedTextToNetwork.toString();
    }
}
