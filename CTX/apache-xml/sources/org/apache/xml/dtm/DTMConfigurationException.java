package org.apache.xml.dtm;

import javax.xml.transform.SourceLocator;

public class DTMConfigurationException extends DTMException {
    static final long serialVersionUID = -4607874078818418046L;

    public DTMConfigurationException() {
        super("Configuration Error");
    }

    public DTMConfigurationException(String str) {
        super(str);
    }

    public DTMConfigurationException(Throwable th) {
        super(th);
    }

    public DTMConfigurationException(String str, Throwable th) {
        super(str, th);
    }

    public DTMConfigurationException(String str, SourceLocator sourceLocator) {
        super(str, sourceLocator);
    }

    public DTMConfigurationException(String str, SourceLocator sourceLocator, Throwable th) {
        super(str, sourceLocator, th);
    }
}
