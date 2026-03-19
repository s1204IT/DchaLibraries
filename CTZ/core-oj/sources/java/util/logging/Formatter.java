package java.util.logging;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public abstract class Formatter {
    public abstract String format(LogRecord logRecord);

    protected Formatter() {
    }

    public String getHead(Handler handler) {
        return "";
    }

    public String getTail(Handler handler) {
        return "";
    }

    public synchronized String formatMessage(LogRecord logRecord) {
        Object[] parameters;
        String message = logRecord.getMessage();
        ResourceBundle resourceBundle = logRecord.getResourceBundle();
        if (resourceBundle != null) {
            try {
                message = resourceBundle.getString(logRecord.getMessage());
            } catch (MissingResourceException e) {
                message = logRecord.getMessage();
            }
            try {
                parameters = logRecord.getParameters();
                if (parameters != null && parameters.length != 0) {
                    if (message.indexOf("{0") >= 0 && message.indexOf("{1") < 0 && message.indexOf("{2") < 0 && message.indexOf("{3") < 0) {
                        return message;
                    }
                    return MessageFormat.format(message, parameters);
                }
                return message;
            } catch (Exception e2) {
                return message;
            }
        }
        parameters = logRecord.getParameters();
        if (parameters != null) {
            if (message.indexOf("{0") >= 0) {
            }
            return MessageFormat.format(message, parameters);
        }
        return message;
    }
}
