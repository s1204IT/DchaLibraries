package gov.nist.javax.sip.parser;

import gov.nist.core.Debug;
import gov.nist.core.InternalErrorHandler;
import gov.nist.core.Separators;
import gov.nist.javax.sip.header.ContentLength;
import gov.nist.javax.sip.message.SIPMessage;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

public final class PipelinedMsgParser implements Runnable {
    private static int uid = 0;
    private int maxMessageSize;
    private Thread mythread;
    private Pipeline rawInputStream;
    protected SIPMessageListener sipMessageListener;
    private int sizeCounter;

    protected PipelinedMsgParser() {
    }

    private static synchronized int getNewUid() {
        int i;
        i = uid;
        uid = i + 1;
        return i;
    }

    public PipelinedMsgParser(SIPMessageListener sIPMessageListener, Pipeline pipeline, boolean z, int i) {
        this();
        this.sipMessageListener = sIPMessageListener;
        this.rawInputStream = pipeline;
        this.maxMessageSize = i;
        this.mythread = new Thread(this);
        this.mythread.setName("PipelineThread-" + getNewUid());
    }

    public PipelinedMsgParser(SIPMessageListener sIPMessageListener, Pipeline pipeline, int i) {
        this(sIPMessageListener, pipeline, false, i);
    }

    public PipelinedMsgParser(Pipeline pipeline) {
        this(null, pipeline, false, 0);
    }

    public void processInput() {
        this.mythread.start();
    }

    protected Object clone() {
        PipelinedMsgParser pipelinedMsgParser = new PipelinedMsgParser();
        pipelinedMsgParser.rawInputStream = this.rawInputStream;
        pipelinedMsgParser.sipMessageListener = this.sipMessageListener;
        new Thread(pipelinedMsgParser).setName("PipelineThread");
        return pipelinedMsgParser;
    }

    public void setMessageListener(SIPMessageListener sIPMessageListener) {
        this.sipMessageListener = sIPMessageListener;
    }

    private String readLine(InputStream inputStream) throws IOException {
        char c;
        StringBuffer stringBuffer = new StringBuffer("");
        do {
            int i = inputStream.read();
            if (i == -1) {
                throw new IOException("End of stream");
            }
            c = (char) i;
            if (this.maxMessageSize > 0) {
                this.sizeCounter--;
                if (this.sizeCounter <= 0) {
                    throw new IOException("Max size exceeded!");
                }
            }
            if (c != '\r') {
                stringBuffer.append(c);
            }
        } while (c != '\n');
        return stringBuffer.toString();
    }

    @Override
    public void run() {
        String line;
        String line2;
        Pipeline pipeline;
        int i;
        Pipeline pipeline2 = this.rawInputStream;
        while (true) {
            try {
                this.sizeCounter = this.maxMessageSize;
                StringBuffer stringBuffer = new StringBuffer();
                if (Debug.parserDebug) {
                    Debug.println("Starting parse!");
                }
                while (true) {
                    try {
                        line = readLine(pipeline2);
                        if (!line.equals(Separators.RETURN)) {
                            break;
                        } else if (Debug.parserDebug) {
                            Debug.println("Discarding blank line. ");
                        }
                    } catch (IOException e) {
                        Debug.printStackTrace(e);
                        try {
                            pipeline2.close();
                            return;
                        } catch (IOException e2) {
                            InternalErrorHandler.handleException(e2);
                            return;
                        }
                    }
                }
                stringBuffer.append(line);
                this.rawInputStream.startTimer();
                Debug.println("Reading Input Stream");
                do {
                    try {
                        line2 = readLine(pipeline2);
                        stringBuffer.append(line2);
                    } catch (IOException e3) {
                        this.rawInputStream.stopTimer();
                        Debug.printStackTrace(e3);
                        try {
                            pipeline2.close();
                            return;
                        } catch (IOException e4) {
                            InternalErrorHandler.handleException(e4);
                            return;
                        }
                    }
                } while (!line2.trim().equals(""));
                this.rawInputStream.stopTimer();
                stringBuffer.append(line2);
                StringMsgParser stringMsgParser = new StringMsgParser(this.sipMessageListener);
                int i2 = 0;
                stringMsgParser.readBody = false;
                try {
                    if (Debug.debug) {
                        Debug.println("About to parse : " + stringBuffer.toString());
                    }
                    SIPMessage sIPMessage = stringMsgParser.parseSIPMessage(stringBuffer.toString());
                    if (sIPMessage != null) {
                        if (Debug.debug) {
                            Debug.println("Completed parsing message");
                        }
                        ContentLength contentLength = (ContentLength) sIPMessage.getContentLength();
                        int contentLength2 = contentLength != null ? contentLength.getContentLength() : 0;
                        if (Debug.debug) {
                            Debug.println("contentLength " + contentLength2);
                        }
                        if (contentLength2 == 0) {
                            sIPMessage.removeContent();
                        } else if (this.maxMessageSize == 0 || contentLength2 < this.sizeCounter) {
                            byte[] bArr = new byte[contentLength2];
                            while (i2 < contentLength2) {
                                this.rawInputStream.startTimer();
                                try {
                                    try {
                                        i = pipeline2.read(bArr, i2, contentLength2 - i2);
                                    } catch (IOException e5) {
                                        Debug.logError("Exception Reading Content", e5);
                                        pipeline = this.rawInputStream;
                                    }
                                    if (i <= 0) {
                                        pipeline = this.rawInputStream;
                                        pipeline.stopTimer();
                                        break;
                                    } else {
                                        i2 += i;
                                        this.rawInputStream.stopTimer();
                                    }
                                } finally {
                                    this.rawInputStream.stopTimer();
                                }
                            }
                            sIPMessage.setMessageContent(bArr);
                        }
                        if (this.sipMessageListener != null) {
                            try {
                                this.sipMessageListener.processMessage(sIPMessage);
                            } catch (Exception e6) {
                                try {
                                    pipeline2.close();
                                    return;
                                } catch (IOException e7) {
                                    InternalErrorHandler.handleException(e7);
                                    return;
                                }
                            }
                        } else {
                            continue;
                        }
                    }
                } catch (ParseException e8) {
                    Debug.logError("Detected a parse error", e8);
                }
            } catch (Throwable th) {
                try {
                    pipeline2.close();
                } catch (IOException e9) {
                    InternalErrorHandler.handleException(e9);
                }
                throw th;
            }
        }
    }

    public void close() {
        try {
            this.rawInputStream.close();
        } catch (IOException e) {
        }
    }
}
