package org.apache.xpath.domapi;

import javax.xml.transform.SourceLocator;
import javax.xml.transform.TransformerException;

public final class XPathStylesheetDOM3Exception extends TransformerException {
    public XPathStylesheetDOM3Exception(String str, SourceLocator sourceLocator) {
        super(str, sourceLocator);
    }
}
