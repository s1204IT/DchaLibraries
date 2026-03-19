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
        if (!(obj instanceof MessageLog)) {
            return false;
        }
        MessageLog messageLog = (MessageLog) obj;
        return messageLog.message.equals(this.message) && messageLog.timeStamp == this.timeStamp;
    }

    public MessageLog(String str, String str2, String str3, String str4, boolean z, String str5, String str6, String str7, long j) {
        if (str == null || str.equals("")) {
            throw new IllegalArgumentException("null msg");
        }
        this.message = str;
        this.source = str2;
        this.destination = str3;
        try {
            long j2 = Long.parseLong(str4);
            if (j2 < 0) {
                throw new IllegalArgumentException("Bad time stamp ");
            }
            this.timeStamp = j2;
            this.isSender = z;
            this.firstLine = str5;
            this.tid = str6;
            this.callId = str7;
            this.timeStampHeaderValue = j;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Bad number format " + str4);
        }
    }

    public MessageLog(String str, String str2, String str3, long j, boolean z, String str4, String str5, String str6, long j2) {
        if (str == null || str.equals("")) {
            throw new IllegalArgumentException("null msg");
        }
        this.message = str;
        this.source = str2;
        this.destination = str3;
        if (j < 0) {
            throw new IllegalArgumentException("negative ts");
        }
        this.timeStamp = j;
        this.isSender = z;
        this.firstLine = str4;
        this.tid = str5;
        this.callId = str6;
        this.timeStampHeaderValue = j2;
    }

    @Override
    public String toString() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("<message\nfrom=\"");
        sb.append(this.source);
        sb.append("\" \nto=\"");
        sb.append(this.destination);
        sb.append("\" \ntime=\"");
        sb.append(this.timeStamp);
        sb.append(Separators.DOUBLE_QUOTE);
        if (this.timeStampHeaderValue != 0) {
            str = "\ntimeStamp = \"" + this.timeStampHeaderValue + Separators.DOUBLE_QUOTE;
        } else {
            str = "";
        }
        sb.append(str);
        sb.append("\nisSender=\"");
        sb.append(this.isSender);
        sb.append("\" \ntransactionId=\"");
        sb.append(this.tid);
        sb.append("\" \ncallId=\"");
        sb.append(this.callId);
        sb.append("\" \nfirstLine=\"");
        sb.append(this.firstLine.trim());
        sb.append("\" \n>\n");
        return (((sb.toString() + "<![CDATA[") + this.message) + "]]>\n") + "</message>\n";
    }
}
