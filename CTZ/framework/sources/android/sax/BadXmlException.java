package android.sax;

import org.xml.sax.Locator;
import org.xml.sax.SAXParseException;

class BadXmlException extends SAXParseException {
    public BadXmlException(String str, Locator locator) {
        super(str, locator);
    }

    @Override
    public String getMessage() {
        return "Line " + getLineNumber() + ": " + super.getMessage();
    }
}
