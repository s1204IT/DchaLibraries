package gov.nist.javax.sip.stack;

import gov.nist.core.Separators;
import gov.nist.javax.sip.LogRecord;

class MessageLog implements LogRecord {
    private String callId;
    private String destination;
    private String firstLine;
    private boolean isSender;
    private String message;
    private String source;
    private String tid;
    private long timeStamp;
    private long timeStampHeaderValue;

    @Override
    public boolean equals(Object obj) {
        return (obj instanceof MessageLog) && obj.message.equals(this.message) && obj.timeStamp == this.timeStamp;
    }

    public MessageLog(String message, String source, String destination, String timeStamp, boolean isSender, String firstLine, String tid, String callId, long timeStampHeaderValue) {
        if (message == null || message.equals("")) {
            throw new IllegalArgumentException("null msg");
        }
        this.message = message;
        this.source = source;
        this.destination = destination;
        try {
            long ts = Long.parseLong(timeStamp);
            if (ts < 0) {
                throw new IllegalArgumentException("Bad time stamp ");
            }
            this.timeStamp = ts;
            this.isSender = isSender;
            this.firstLine = firstLine;
            this.tid = tid;
            this.callId = callId;
            this.timeStampHeaderValue = timeStampHeaderValue;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad number format " + timeStamp);
        }
    }

    public MessageLog(String message, String source, String destination, long timeStamp, boolean isSender, String firstLine, String tid, String callId, long timestampVal) {
        if (message == null || message.equals("")) {
            throw new IllegalArgumentException("null msg");
        }
        this.message = message;
        this.source = source;
        this.destination = destination;
        if (timeStamp < 0) {
            throw new IllegalArgumentException("negative ts");
        }
        this.timeStamp = timeStamp;
        this.isSender = isSender;
        this.firstLine = firstLine;
        this.tid = tid;
        this.callId = callId;
        this.timeStampHeaderValue = timestampVal;
    }

    @Override
    public String toString() {
        String log = "<message\nfrom=\"" + this.source + "\" \nto=\"" + this.destination + "\" \ntime=\"" + this.timeStamp + Separators.DOUBLE_QUOTE + (this.timeStampHeaderValue != 0 ? "\ntimeStamp = \"" + this.timeStampHeaderValue + Separators.DOUBLE_QUOTE : "") + "\nisSender=\"" + this.isSender + "\" \ntransactionId=\"" + this.tid + "\" \ncallId=\"" + this.callId + "\" \nfirstLine=\"" + this.firstLine.trim() + Separators.DOUBLE_QUOTE + " \n>\n";
        return (((log + "<![CDATA[") + this.message) + "]]>\n") + "</message>\n";
    }
}
