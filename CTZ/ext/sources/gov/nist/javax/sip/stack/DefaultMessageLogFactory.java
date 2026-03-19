package gov.nist.javax.sip.stack;

import gov.nist.javax.sip.LogRecord;
import gov.nist.javax.sip.LogRecordFactory;

public class DefaultMessageLogFactory implements LogRecordFactory {
    public LogRecord createLogRecord(String str, String str2, String str3, String str4, boolean z, String str5, String str6, String str7, long j) {
        return new MessageLog(str, str2, str3, str4, z, str5, str6, str7, j);
    }

    @Override
    public LogRecord createLogRecord(String str, String str2, String str3, long j, boolean z, String str4, String str5, String str6, long j2) {
        return new MessageLog(str, str2, str3, j, z, str4, str5, str6, j2);
    }
}
