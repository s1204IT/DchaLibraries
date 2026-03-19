package java.util.logging;

import java.nio.charset.Charset;
import java.util.GregorianCalendar;
import java.util.ResourceBundle;

public class XMLFormatter extends Formatter {
    private LogManager manager = LogManager.getLogManager();

    private void a2(StringBuilder sb, int i) {
        if (i < 10) {
            sb.append('0');
        }
        sb.append(i);
    }

    private void appendISO8601(StringBuilder sb, long j) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(j);
        sb.append(gregorianCalendar.get(1));
        sb.append('-');
        a2(sb, gregorianCalendar.get(2) + 1);
        sb.append('-');
        a2(sb, gregorianCalendar.get(5));
        sb.append('T');
        a2(sb, gregorianCalendar.get(11));
        sb.append(':');
        a2(sb, gregorianCalendar.get(12));
        sb.append(':');
        a2(sb, gregorianCalendar.get(13));
    }

    private void escape(StringBuilder sb, String str) {
        if (str == null) {
            str = "<null>";
        }
        for (int i = 0; i < str.length(); i++) {
            char cCharAt = str.charAt(i);
            if (cCharAt == '<') {
                sb.append("&lt;");
            } else if (cCharAt == '>') {
                sb.append("&gt;");
            } else if (cCharAt == '&') {
                sb.append("&amp;");
            } else {
                sb.append(cCharAt);
            }
        }
    }

    @Override
    public String format(LogRecord logRecord) {
        StringBuilder sb = new StringBuilder(500);
        sb.append("<record>\n");
        sb.append("  <date>");
        appendISO8601(sb, logRecord.getMillis());
        sb.append("</date>\n");
        sb.append("  <millis>");
        sb.append(logRecord.getMillis());
        sb.append("</millis>\n");
        sb.append("  <sequence>");
        sb.append(logRecord.getSequenceNumber());
        sb.append("</sequence>\n");
        String loggerName = logRecord.getLoggerName();
        if (loggerName != null) {
            sb.append("  <logger>");
            escape(sb, loggerName);
            sb.append("</logger>\n");
        }
        sb.append("  <level>");
        escape(sb, logRecord.getLevel().toString());
        sb.append("</level>\n");
        if (logRecord.getSourceClassName() != null) {
            sb.append("  <class>");
            escape(sb, logRecord.getSourceClassName());
            sb.append("</class>\n");
        }
        if (logRecord.getSourceMethodName() != null) {
            sb.append("  <method>");
            escape(sb, logRecord.getSourceMethodName());
            sb.append("</method>\n");
        }
        sb.append("  <thread>");
        sb.append(logRecord.getThreadID());
        sb.append("</thread>\n");
        if (logRecord.getMessage() != null) {
            String message = formatMessage(logRecord);
            sb.append("  <message>");
            escape(sb, message);
            sb.append("</message>");
            sb.append("\n");
        } else {
            sb.append("<message/>");
            sb.append("\n");
        }
        ResourceBundle resourceBundle = logRecord.getResourceBundle();
        if (resourceBundle != null) {
            try {
                if (resourceBundle.getString(logRecord.getMessage()) != null) {
                    sb.append("  <key>");
                    escape(sb, logRecord.getMessage());
                    sb.append("</key>\n");
                    sb.append("  <catalog>");
                    escape(sb, logRecord.getResourceBundleName());
                    sb.append("</catalog>\n");
                }
            } catch (Exception e) {
            }
        }
        Object[] parameters = logRecord.getParameters();
        if (parameters != null && parameters.length != 0 && logRecord.getMessage().indexOf("{") == -1) {
            for (Object obj : parameters) {
                sb.append("  <param>");
                try {
                    escape(sb, obj.toString());
                } catch (Exception e2) {
                    sb.append("???");
                }
                sb.append("</param>\n");
            }
        }
        if (logRecord.getThrown() != null) {
            Throwable thrown = logRecord.getThrown();
            sb.append("  <exception>\n");
            sb.append("    <message>");
            escape(sb, thrown.toString());
            sb.append("</message>\n");
            for (StackTraceElement stackTraceElement : thrown.getStackTrace()) {
                sb.append("    <frame>\n");
                sb.append("      <class>");
                escape(sb, stackTraceElement.getClassName());
                sb.append("</class>\n");
                sb.append("      <method>");
                escape(sb, stackTraceElement.getMethodName());
                sb.append("</method>\n");
                if (stackTraceElement.getLineNumber() >= 0) {
                    sb.append("      <line>");
                    sb.append(stackTraceElement.getLineNumber());
                    sb.append("</line>\n");
                }
                sb.append("    </frame>\n");
            }
            sb.append("  </exception>\n");
        }
        sb.append("</record>\n");
        return sb.toString();
    }

    @Override
    public String getHead(Handler handler) {
        String strName;
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\"");
        if (handler != null) {
            strName = handler.getEncoding();
        } else {
            strName = null;
        }
        if (strName == null) {
            strName = Charset.defaultCharset().name();
        }
        try {
            strName = Charset.forName(strName).name();
        } catch (Exception e) {
        }
        sb.append(" encoding=\"");
        sb.append(strName);
        sb.append("\"");
        sb.append(" standalone=\"no\"?>\n");
        sb.append("<!DOCTYPE log SYSTEM \"logger.dtd\">\n");
        sb.append("<log>\n");
        return sb.toString();
    }

    @Override
    public String getTail(Handler handler) {
        return "</log>\n";
    }
}
