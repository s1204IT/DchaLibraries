package mf.org.apache.xerces.impl;

import java.util.Hashtable;
import java.util.Locale;
import mf.org.apache.xerces.util.DefaultErrorHandler;
import mf.org.apache.xerces.util.ErrorHandlerProxy;
import mf.org.apache.xerces.util.MessageFormatter;
import mf.org.apache.xerces.xni.XMLLocator;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.parser.XMLComponent;
import mf.org.apache.xerces.xni.parser.XMLComponentManager;
import mf.org.apache.xerces.xni.parser.XMLConfigurationException;
import mf.org.apache.xerces.xni.parser.XMLErrorHandler;
import mf.org.apache.xerces.xni.parser.XMLParseException;
import org.xml.sax.ErrorHandler;

public class XMLErrorReporter implements XMLComponent {
    public static final short SEVERITY_ERROR = 1;
    public static final short SEVERITY_FATAL_ERROR = 2;
    public static final short SEVERITY_WARNING = 0;
    protected boolean fContinueAfterFatalError;
    protected XMLErrorHandler fDefaultErrorHandler;
    protected XMLErrorHandler fErrorHandler;
    protected Locale fLocale;
    protected XMLLocator fLocator;
    protected static final String CONTINUE_AFTER_FATAL_ERROR = "http://apache.org/xml/features/continue-after-fatal-error";
    private static final String[] RECOGNIZED_FEATURES = {CONTINUE_AFTER_FATAL_ERROR};
    private static final Boolean[] FEATURE_DEFAULTS = new Boolean[1];
    protected static final String ERROR_HANDLER = "http://apache.org/xml/properties/internal/error-handler";
    private static final String[] RECOGNIZED_PROPERTIES = {ERROR_HANDLER};
    private static final Object[] PROPERTY_DEFAULTS = new Object[1];
    private ErrorHandler fSaxProxy = null;
    protected Hashtable fMessageFormatters = new Hashtable();

    public void setLocale(Locale locale) {
        this.fLocale = locale;
    }

    public Locale getLocale() {
        return this.fLocale;
    }

    public void setDocumentLocator(XMLLocator locator) {
        this.fLocator = locator;
    }

    public void putMessageFormatter(String domain, MessageFormatter messageFormatter) {
        this.fMessageFormatters.put(domain, messageFormatter);
    }

    public MessageFormatter getMessageFormatter(String domain) {
        return (MessageFormatter) this.fMessageFormatters.get(domain);
    }

    public MessageFormatter removeMessageFormatter(String domain) {
        return (MessageFormatter) this.fMessageFormatters.remove(domain);
    }

    public String reportError(String domain, String key, Object[] arguments, short severity) throws XNIException {
        return reportError(this.fLocator, domain, key, arguments, severity);
    }

    public String reportError(String domain, String key, Object[] arguments, short severity, Exception exception) throws XNIException {
        return reportError(this.fLocator, domain, key, arguments, severity, exception);
    }

    public String reportError(XMLLocator location, String domain, String key, Object[] arguments, short severity) throws XNIException {
        return reportError(location, domain, key, arguments, severity, null);
    }

    public String reportError(XMLLocator location, String domain, String key, Object[] arguments, short severity, Exception exception) throws XNIException {
        String message;
        XMLParseException parseException;
        MessageFormatter messageFormatter = getMessageFormatter(domain);
        if (messageFormatter != null) {
            message = messageFormatter.formatMessage(this.fLocale, key, arguments);
        } else {
            StringBuffer str = new StringBuffer();
            str.append(domain);
            str.append('#');
            str.append(key);
            int argCount = arguments != null ? arguments.length : 0;
            if (argCount > 0) {
                str.append('?');
                for (int i = 0; i < argCount; i++) {
                    str.append(arguments[i]);
                    if (i < argCount - 1) {
                        str.append('&');
                    }
                }
            }
            message = str.toString();
        }
        if (exception != null) {
            parseException = new XMLParseException(location, message, exception);
        } else {
            parseException = new XMLParseException(location, message);
        }
        XMLErrorHandler errorHandler = this.fErrorHandler;
        if (errorHandler == null) {
            if (this.fDefaultErrorHandler == null) {
                this.fDefaultErrorHandler = new DefaultErrorHandler();
            }
            errorHandler = this.fDefaultErrorHandler;
        }
        switch (severity) {
            case 0:
                errorHandler.warning(domain, key, parseException);
                return message;
            case 1:
                errorHandler.error(domain, key, parseException);
                return message;
            case 2:
                errorHandler.fatalError(domain, key, parseException);
                if (!this.fContinueAfterFatalError) {
                    throw parseException;
                }
                return message;
            default:
                return message;
        }
    }

    @Override
    public void reset(XMLComponentManager componentManager) throws XNIException {
        try {
            this.fContinueAfterFatalError = componentManager.getFeature(CONTINUE_AFTER_FATAL_ERROR);
        } catch (XNIException e) {
            this.fContinueAfterFatalError = false;
        }
        this.fErrorHandler = (XMLErrorHandler) componentManager.getProperty(ERROR_HANDLER);
    }

    @Override
    public String[] getRecognizedFeatures() {
        return (String[]) RECOGNIZED_FEATURES.clone();
    }

    @Override
    public void setFeature(String featureId, boolean state) throws XMLConfigurationException {
        if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
            int suffixLength = featureId.length() - Constants.XERCES_FEATURE_PREFIX.length();
            if (suffixLength == Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE.length() && featureId.endsWith(Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE)) {
                this.fContinueAfterFatalError = state;
            }
        }
    }

    public boolean getFeature(String featureId) throws XMLConfigurationException {
        if (featureId.startsWith(Constants.XERCES_FEATURE_PREFIX)) {
            int suffixLength = featureId.length() - Constants.XERCES_FEATURE_PREFIX.length();
            if (suffixLength == Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE.length() && featureId.endsWith(Constants.CONTINUE_AFTER_FATAL_ERROR_FEATURE)) {
                return this.fContinueAfterFatalError;
            }
            return false;
        }
        return false;
    }

    @Override
    public String[] getRecognizedProperties() {
        return (String[]) RECOGNIZED_PROPERTIES.clone();
    }

    @Override
    public void setProperty(String propertyId, Object value) throws XMLConfigurationException {
        if (propertyId.startsWith(Constants.XERCES_PROPERTY_PREFIX)) {
            int suffixLength = propertyId.length() - Constants.XERCES_PROPERTY_PREFIX.length();
            if (suffixLength == Constants.ERROR_HANDLER_PROPERTY.length() && propertyId.endsWith(Constants.ERROR_HANDLER_PROPERTY)) {
                this.fErrorHandler = (XMLErrorHandler) value;
            }
        }
    }

    @Override
    public Boolean getFeatureDefault(String featureId) {
        for (int i = 0; i < RECOGNIZED_FEATURES.length; i++) {
            if (RECOGNIZED_FEATURES[i].equals(featureId)) {
                return FEATURE_DEFAULTS[i];
            }
        }
        return null;
    }

    @Override
    public Object getPropertyDefault(String propertyId) {
        for (int i = 0; i < RECOGNIZED_PROPERTIES.length; i++) {
            if (RECOGNIZED_PROPERTIES[i].equals(propertyId)) {
                return PROPERTY_DEFAULTS[i];
            }
        }
        return null;
    }

    public XMLErrorHandler getErrorHandler() {
        return this.fErrorHandler;
    }

    public ErrorHandler getSAXErrorHandler() {
        if (this.fSaxProxy == null) {
            this.fSaxProxy = new ErrorHandlerProxy() {
                @Override
                protected XMLErrorHandler getErrorHandler() {
                    return XMLErrorReporter.this.fErrorHandler;
                }
            };
        }
        return this.fSaxProxy;
    }
}
