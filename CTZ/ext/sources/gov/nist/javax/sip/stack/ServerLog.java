package gov.nist.javax.sip.stack;

import gov.nist.core.Separators;
import gov.nist.core.ServerLogger;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.LogRecord;
import gov.nist.javax.sip.header.CallID;
import gov.nist.javax.sip.message.SIPMessage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Properties;
import javax.sip.SipStack;
import javax.sip.header.TimeStampHeader;
import javax.sip.message.Request;

public class ServerLog implements ServerLogger {
    private String auxInfo;
    private Properties configurationProperties;
    private String description;
    private boolean logContent;
    private String logFileName;
    private PrintWriter printWriter;
    private SIPTransactionStack sipStack;
    private String stackIpAddress;
    protected StackLogger stackLogger;
    protected int traceLevel = 16;

    private void setProperties(Properties properties) {
        int i;
        this.configurationProperties = properties;
        this.description = properties.getProperty("javax.sip.STACK_NAME");
        this.stackIpAddress = properties.getProperty("javax.sip.IP_ADDRESS");
        this.logFileName = properties.getProperty("gov.nist.javax.sip.SERVER_LOG");
        String property = properties.getProperty("gov.nist.javax.sip.TRACE_LEVEL");
        String property2 = properties.getProperty("gov.nist.javax.sip.LOG_MESSAGE_CONTENT");
        this.logContent = property2 != null && property2.equals("true");
        if (property != null && !property.equals("LOG4J")) {
            try {
                if (property.equals("DEBUG")) {
                    i = 32;
                } else if (property.equals(Request.INFO)) {
                    i = 16;
                } else if (property.equals("ERROR")) {
                    i = 4;
                } else if (!property.equals("NONE") && !property.equals("OFF")) {
                    i = Integer.parseInt(property);
                } else {
                    i = 0;
                }
                setTraceLevel(i);
            } catch (NumberFormatException e) {
                System.out.println("ServerLog: WARNING Bad integer " + property);
                System.out.println("logging dislabled ");
                setTraceLevel(0);
            }
        }
        checkLogFile();
    }

    public void setStackIpAddress(String str) {
        this.stackIpAddress = str;
    }

    @Override
    public synchronized void closeLogFile() {
        if (this.printWriter != null) {
            this.printWriter.close();
            this.printWriter = null;
        }
    }

    public void checkLogFile() {
        if (this.logFileName == null || this.traceLevel < 16) {
            return;
        }
        try {
            File file = new File(this.logFileName);
            if (!file.exists()) {
                file.createNewFile();
                this.printWriter = null;
            }
            if (this.printWriter == null) {
                this.printWriter = new PrintWriter((Writer) new FileWriter(this.logFileName, !Boolean.valueOf(this.configurationProperties.getProperty("gov.nist.javax.sip.SERVER_LOG_OVERWRITE")).booleanValue()), true);
                this.printWriter.println("<!-- Use the  Trace Viewer in src/tools/tracesviewer to view this  trace  \nHere are the stack configuration properties \njavax.sip.IP_ADDRESS= " + this.configurationProperties.getProperty("javax.sip.IP_ADDRESS") + "\njavax.sip.STACK_NAME= " + this.configurationProperties.getProperty("javax.sip.STACK_NAME") + "\njavax.sip.ROUTER_PATH= " + this.configurationProperties.getProperty("javax.sip.ROUTER_PATH") + "\njavax.sip.OUTBOUND_PROXY= " + this.configurationProperties.getProperty("javax.sip.OUTBOUND_PROXY") + "\n-->");
                PrintWriter printWriter = this.printWriter;
                StringBuilder sb = new StringBuilder();
                sb.append("<description\n logDescription=\"");
                sb.append(this.description);
                sb.append("\"\n name=\"");
                sb.append(this.configurationProperties.getProperty("javax.sip.STACK_NAME"));
                sb.append("\"\n auxInfo=\"");
                sb.append(this.auxInfo);
                sb.append("\"/>\n ");
                printWriter.println(sb.toString());
                if (this.auxInfo != null) {
                    if (this.sipStack.isLoggingEnabled()) {
                        this.stackLogger.logDebug("Here are the stack configuration properties \njavax.sip.IP_ADDRESS= " + this.configurationProperties.getProperty("javax.sip.IP_ADDRESS") + "\njavax.sip.ROUTER_PATH= " + this.configurationProperties.getProperty("javax.sip.ROUTER_PATH") + "\njavax.sip.OUTBOUND_PROXY= " + this.configurationProperties.getProperty("javax.sip.OUTBOUND_PROXY") + "\ngov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS= " + this.configurationProperties.getProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS") + "\ngov.nist.javax.sip.CACHE_SERVER_CONNECTIONS= " + this.configurationProperties.getProperty("gov.nist.javax.sip.CACHE_SERVER_CONNECTIONS") + "\ngov.nist.javax.sip.REENTRANT_LISTENER= " + this.configurationProperties.getProperty("gov.nist.javax.sip.REENTRANT_LISTENER") + "gov.nist.javax.sip.THREAD_POOL_SIZE= " + this.configurationProperties.getProperty("gov.nist.javax.sip.THREAD_POOL_SIZE") + Separators.RETURN);
                        this.stackLogger.logDebug(" ]]> ");
                        this.stackLogger.logDebug("</debug>");
                        StackLogger stackLogger = this.stackLogger;
                        StringBuilder sb2 = new StringBuilder();
                        sb2.append("<description\n logDescription=\"");
                        sb2.append(this.description);
                        sb2.append("\"\n name=\"");
                        sb2.append(this.stackIpAddress);
                        sb2.append("\"\n auxInfo=\"");
                        sb2.append(this.auxInfo);
                        sb2.append("\"/>\n ");
                        stackLogger.logDebug(sb2.toString());
                        this.stackLogger.logDebug("<debug>");
                        this.stackLogger.logDebug("<![CDATA[ ");
                        return;
                    }
                    return;
                }
                if (this.sipStack.isLoggingEnabled()) {
                    this.stackLogger.logDebug("Here are the stack configuration properties \n" + this.configurationProperties + Separators.RETURN);
                    this.stackLogger.logDebug(" ]]>");
                    this.stackLogger.logDebug("</debug>");
                    this.stackLogger.logDebug("<description\n logDescription=\"" + this.description + "\"\n name=\"" + this.stackIpAddress + "\" />\n");
                    this.stackLogger.logDebug("<debug>");
                    this.stackLogger.logDebug("<![CDATA[ ");
                }
            }
        } catch (IOException e) {
        }
    }

    public boolean needsLogging() {
        return this.logFileName != null;
    }

    public void setLogFileName(String str) {
        this.logFileName = str;
    }

    public String getLogFileName() {
        return this.logFileName;
    }

    private void logMessage(String str) {
        checkLogFile();
        if (this.printWriter != null) {
            this.printWriter.println(str);
        }
        if (this.sipStack.isLoggingEnabled()) {
            this.stackLogger.logInfo(str);
        }
    }

    private void logMessage(String str, String str2, String str3, boolean z, String str4, String str5, String str6, String str7, long j, long j2) {
        LogRecord logRecordCreateLogRecord = this.sipStack.logRecordFactory.createLogRecord(str, str2, str3, j, z, str5, str7, str4, j2);
        if (logRecordCreateLogRecord != null) {
            logMessage(logRecordCreateLogRecord.toString());
        }
    }

    @Override
    public void logMessage(SIPMessage sIPMessage, String str, String str2, boolean z, long j) {
        checkLogFile();
        if (sIPMessage.getFirstLine() == null) {
            return;
        }
        CallID callID = (CallID) sIPMessage.getCallId();
        String callId = null;
        if (callID != null) {
            callId = callID.getCallId();
        }
        String str3 = callId;
        String strTrim = sIPMessage.getFirstLine().trim();
        String strEncode = this.logContent ? sIPMessage.encode() : sIPMessage.encodeMessage();
        String transactionId = sIPMessage.getTransactionId();
        TimeStampHeader timeStampHeader = (TimeStampHeader) sIPMessage.getHeader("Timestamp");
        logMessage(strEncode, str, str2, z, str3, strTrim, null, transactionId, j, timeStampHeader == null ? 0L : timeStampHeader.getTime());
    }

    @Override
    public void logMessage(SIPMessage sIPMessage, String str, String str2, String str3, boolean z, long j) {
        String callId;
        checkLogFile();
        CallID callID = (CallID) sIPMessage.getCallId();
        if (callID != null) {
            callId = callID.getCallId();
        } else {
            callId = null;
        }
        String str4 = callId;
        String strTrim = sIPMessage.getFirstLine().trim();
        String strEncode = this.logContent ? sIPMessage.encode() : sIPMessage.encodeMessage();
        String transactionId = sIPMessage.getTransactionId();
        TimeStampHeader timeStampHeader = (TimeStampHeader) sIPMessage.getHeader("Timestamp");
        logMessage(strEncode, str, str2, z, str4, strTrim, str3, transactionId, j, timeStampHeader == null ? 0L : timeStampHeader.getTime());
    }

    @Override
    public void logMessage(SIPMessage sIPMessage, String str, String str2, String str3, boolean z) {
        logMessage(sIPMessage, str, str2, str3, z, System.currentTimeMillis());
    }

    @Override
    public void logException(Exception exc) {
        if (this.traceLevel >= 4) {
            checkLogFile();
            exc.printStackTrace();
            if (this.printWriter != null) {
                exc.printStackTrace(this.printWriter);
            }
        }
    }

    public void setTraceLevel(int i) {
        this.traceLevel = i;
    }

    public int getTraceLevel() {
        return this.traceLevel;
    }

    public void setAuxInfo(String str) {
        this.auxInfo = str;
    }

    @Override
    public void setSipStack(SipStack sipStack) {
        if (sipStack instanceof SIPTransactionStack) {
            this.sipStack = (SIPTransactionStack) sipStack;
            this.stackLogger = this.sipStack.getStackLogger();
            return;
        }
        throw new IllegalArgumentException("sipStack must be a SIPTransactionStack");
    }

    @Override
    public void setStackProperties(Properties properties) {
        setProperties(properties);
    }

    public void setLevel(int i) {
    }
}
