package com.android.internal.telephony.test;

import android.os.HandlerThread;
import android.os.Looper;
import android.telephony.Rlog;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class ModelInterpreter implements Runnable, SimulatedRadioControl {
    static final int CONNECTING_PAUSE_MSEC = 500;
    static final String LOG_TAG = "ModelInterpreter";
    static final int MAX_CALLS = 6;
    static final int PROGRESS_CALL_STATE = 1;
    static final String[][] sDefaultResponses = {new String[]{"E0Q0V1", null}, new String[]{"+CMEE=2", null}, new String[]{"+CREG=2", null}, new String[]{"+CGREG=2", null}, new String[]{"+CCWA=1", null}, new String[]{"+COPS=0", null}, new String[]{"+CFUN=1", null}, new String[]{"+CGMI", "+CGMI: Android Model AT Interpreter\r"}, new String[]{"+CGMM", "+CGMM: Android Model AT Interpreter\r"}, new String[]{"+CGMR", "+CGMR: 1.0\r"}, new String[]{"+CGSN", "000000000000000\r"}, new String[]{"+CIMI", "320720000000000\r"}, new String[]{"+CSCS=?", "+CSCS: (\"HEX\",\"UCS2\")\r"}, new String[]{"+CFUN?", "+CFUN: 1\r"}, new String[]{"+COPS=3,0;+COPS?;+COPS=3,1;+COPS?;+COPS=3,2;+COPS?", "+COPS: 0,0,\"Android\"\r+COPS: 0,1,\"Android\"\r+COPS: 0,2,\"310995\"\r"}, new String[]{"+CREG?", "+CREG: 2,5, \"0113\", \"6614\"\r"}, new String[]{"+CGREG?", "+CGREG: 2,0\r"}, new String[]{"+CSQ", "+CSQ: 16,99\r"}, new String[]{"+CNMI?", "+CNMI: 1,2,2,1,1\r"}, new String[]{"+CLIR?", "+CLIR: 1,3\r"}, new String[]{"%CPVWI=2", "%CPVWI: 0\r"}, new String[]{"+CUSD=1,\"#646#\"", "+CUSD=0,\"You have used 23 minutes\"\r"}, new String[]{"+CRSM=176,12258,0,0,10", "+CRSM: 144,0,981062200050259429F6\r"}, new String[]{"+CRSM=192,12258,0,0,15", "+CRSM: 144,0,0000000A2FE204000FF55501020000\r"}, new String[]{"+CRSM=192,28474,0,0,15", "+CRSM: 144,0,0000005a6f3a040011f5220102011e\r"}, new String[]{"+CRSM=178,28474,1,4,30", "+CRSM: 144,0,437573746f6d65722043617265ffffff07818100398799f7ffffffffffff\r"}, new String[]{"+CRSM=178,28474,2,4,30", "+CRSM: 144,0,566f696365204d61696cffffffffffff07918150367742f3ffffffffffff\r"}, new String[]{"+CRSM=178,28474,3,4,30", "+CRSM: 144,0,4164676a6dffffffffffffffffffffff0b918188551512c221436587ff01\r"}, new String[]{"+CRSM=178,28474,4,4,30", "+CRSM: 144,0,810101c1ffffffffffffffffffffffff068114455245f8ffffffffffffff\r"}, new String[]{"+CRSM=192,28490,0,0,15", "+CRSM: 144,0,000000416f4a040011f5550102010d\r"}, new String[]{"+CRSM=178,28490,1,4,13", "+CRSM: 144,0,0206092143658709ffffffffff\r"}};
    private String mFinalResponse;
    HandlerThread mHandlerThread;
    InputStream mIn;
    LineReader mLineReader;
    OutputStream mOut;
    int mPausedResponseCount;
    Object mPausedResponseMonitor;
    ServerSocket mSS;
    SimulatedGsmCallState mSimulatedCallState;

    public ModelInterpreter(InputStream inputStream, OutputStream outputStream) {
        this.mPausedResponseMonitor = new Object();
        this.mIn = inputStream;
        this.mOut = outputStream;
        init();
    }

    public ModelInterpreter(InetSocketAddress inetSocketAddress) throws IOException {
        this.mPausedResponseMonitor = new Object();
        this.mSS = new ServerSocket();
        this.mSS.setReuseAddress(true);
        this.mSS.bind(inetSocketAddress);
        init();
    }

    private void init() {
        new Thread(this, LOG_TAG).start();
        this.mHandlerThread = new HandlerThread(LOG_TAG);
        this.mHandlerThread.start();
        this.mSimulatedCallState = new SimulatedGsmCallState(this.mHandlerThread.getLooper());
    }

    @Override
    public void run() {
        while (true) {
            if (this.mSS != null) {
                try {
                    Socket socketAccept = this.mSS.accept();
                    try {
                        this.mIn = socketAccept.getInputStream();
                        this.mOut = socketAccept.getOutputStream();
                        Rlog.i(LOG_TAG, "New connection accepted");
                    } catch (IOException e) {
                        Rlog.w(LOG_TAG, "IOException on accepted socket(); re-listening", e);
                    }
                } catch (IOException e2) {
                    Rlog.w(LOG_TAG, "IOException on socket.accept(); stopping", e2);
                    return;
                }
            }
            this.mLineReader = new LineReader(this.mIn);
            println("Welcome");
            while (true) {
                String nextLine = this.mLineReader.getNextLine();
                if (nextLine == null) {
                    break;
                }
                synchronized (this.mPausedResponseMonitor) {
                    while (this.mPausedResponseCount > 0) {
                        try {
                            this.mPausedResponseMonitor.wait();
                        } catch (InterruptedException e3) {
                        }
                    }
                }
                synchronized (this) {
                    try {
                        this.mFinalResponse = "OK";
                        processLine(nextLine);
                        println(this.mFinalResponse);
                    } catch (InterpreterEx e4) {
                        println(e4.mResult);
                    } catch (RuntimeException e5) {
                        e5.printStackTrace();
                        println("ERROR");
                    }
                }
            }
            Rlog.i(LOG_TAG, "Disconnected");
            if (this.mSS == null) {
                return;
            }
        }
    }

    @Override
    public void triggerRing(String str) {
        synchronized (this) {
            if (this.mSimulatedCallState.triggerRing(str)) {
                println("RING");
            }
        }
    }

    @Override
    public void progressConnectingCallState() {
        this.mSimulatedCallState.progressConnectingCallState();
    }

    @Override
    public void progressConnectingToActive() {
        this.mSimulatedCallState.progressConnectingToActive();
    }

    @Override
    public void setAutoProgressConnectingCall(boolean z) {
        this.mSimulatedCallState.setAutoProgressConnectingCall(z);
    }

    @Override
    public void setNextDialFailImmediately(boolean z) {
        this.mSimulatedCallState.setNextDialFailImmediately(z);
    }

    @Override
    public void setNextCallFailCause(int i) {
    }

    @Override
    public void triggerHangupForeground() {
        if (this.mSimulatedCallState.triggerHangupForeground()) {
            println("NO CARRIER");
        }
    }

    @Override
    public void triggerHangupBackground() {
        if (this.mSimulatedCallState.triggerHangupBackground()) {
            println("NO CARRIER");
        }
    }

    @Override
    public void triggerHangupAll() {
        if (this.mSimulatedCallState.triggerHangupAll()) {
            println("NO CARRIER");
        }
    }

    public void sendUnsolicited(String str) {
        synchronized (this) {
            println(str);
        }
    }

    @Override
    public void triggerSsn(int i, int i2) {
    }

    @Override
    public void triggerIncomingUssd(String str, String str2) {
    }

    @Override
    public void triggerIncomingSMS(String str) {
    }

    @Override
    public void pauseResponses() {
        synchronized (this.mPausedResponseMonitor) {
            this.mPausedResponseCount++;
        }
    }

    @Override
    public void resumeResponses() {
        synchronized (this.mPausedResponseMonitor) {
            this.mPausedResponseCount--;
            if (this.mPausedResponseCount == 0) {
                this.mPausedResponseMonitor.notifyAll();
            }
        }
    }

    private void onAnswer() throws InterpreterEx {
        if (!this.mSimulatedCallState.onAnswer()) {
            throw new InterpreterEx("ERROR");
        }
    }

    private void onHangup() throws InterpreterEx {
        if (!this.mSimulatedCallState.onAnswer()) {
            throw new InterpreterEx("ERROR");
        }
        this.mFinalResponse = "NO CARRIER";
    }

    private void onCHLD(String str) throws InterpreterEx {
        char cCharAt;
        char cCharAt2 = str.charAt(6);
        if (str.length() >= 8) {
            cCharAt = str.charAt(7);
        } else {
            cCharAt = 0;
        }
        if (!this.mSimulatedCallState.onChld(cCharAt2, cCharAt)) {
            throw new InterpreterEx("ERROR");
        }
    }

    private void onDial(String str) throws InterpreterEx {
        if (!this.mSimulatedCallState.onDial(str.substring(1))) {
            throw new InterpreterEx("ERROR");
        }
    }

    private void onCLCC() {
        List<String> clccLines = this.mSimulatedCallState.getClccLines();
        int size = clccLines.size();
        for (int i = 0; i < size; i++) {
            println(clccLines.get(i));
        }
    }

    private void onSMSSend(String str) {
        print("> ");
        this.mLineReader.getNextLineCtrlZ();
        println("+CMGS: 1");
    }

    void processLine(String str) throws InterpreterEx {
        boolean z;
        for (String str2 : splitCommands(str)) {
            if (str2.equals("A")) {
                onAnswer();
            } else if (str2.equals("H")) {
                onHangup();
            } else if (str2.startsWith("+CHLD=")) {
                onCHLD(str2);
            } else if (str2.equals("+CLCC")) {
                onCLCC();
            } else if (str2.startsWith("D")) {
                onDial(str2);
            } else if (str2.startsWith("+CMGS=")) {
                onSMSSend(str2);
            } else {
                int i = 0;
                while (true) {
                    z = true;
                    if (i < sDefaultResponses.length) {
                        if (!str2.equals(sDefaultResponses[i][0])) {
                            i++;
                        } else {
                            String str3 = sDefaultResponses[i][1];
                            if (str3 != null) {
                                println(str3);
                            }
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (!z) {
                    throw new InterpreterEx("ERROR");
                }
            }
        }
    }

    String[] splitCommands(String str) throws InterpreterEx {
        if (!str.startsWith("AT")) {
            throw new InterpreterEx("ERROR");
        }
        if (str.length() == 2) {
            return new String[0];
        }
        return new String[]{str.substring(2)};
    }

    void println(String str) {
        synchronized (this) {
            try {
                this.mOut.write(str.getBytes("US-ASCII"));
                this.mOut.write(13);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    void print(String str) {
        synchronized (this) {
            try {
                this.mOut.write(str.getBytes("US-ASCII"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void shutdown() {
        Looper looper = this.mHandlerThread.getLooper();
        if (looper != null) {
            looper.quit();
        }
        try {
            this.mIn.close();
        } catch (IOException e) {
        }
        try {
            this.mOut.close();
        } catch (IOException e2) {
        }
    }
}
