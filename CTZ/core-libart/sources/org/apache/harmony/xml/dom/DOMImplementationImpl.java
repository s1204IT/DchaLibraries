package org.apache.harmony.xml.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;

public final class DOMImplementationImpl implements DOMImplementation {
    private static DOMImplementationImpl instance;

    DOMImplementationImpl() {
    }

    @Override
    public Document createDocument(String str, String str2, DocumentType documentType) throws DOMException {
        return new DocumentImpl(this, str, str2, documentType, null);
    }

    @Override
    public DocumentType createDocumentType(String str, String str2, String str3) throws DOMException {
        return new DocumentTypeImpl(null, str, str2, str3);
    }

    @Override
    public boolean hasFeature(String str, String str2) {
        boolean z = str2 == null || str2.length() == 0;
        if (str.startsWith("+")) {
            str = str.substring(1);
        }
        if (str.equalsIgnoreCase("Core")) {
            return z || str2.equals("1.0") || str2.equals("2.0") || str2.equals("3.0");
        }
        if (str.equalsIgnoreCase("XML")) {
            return z || str2.equals("1.0") || str2.equals("2.0") || str2.equals("3.0");
        }
        if (str.equalsIgnoreCase("XMLVersion")) {
            return z || str2.equals("1.0") || str2.equals("1.1");
        }
        return false;
    }

    public static DOMImplementationImpl getInstance() {
        if (instance == null) {
            instance = new DOMImplementationImpl();
        }
        return instance;
    }

    @Override
    public Object getFeature(String str, String str2) {
        if (hasFeature(str, str2)) {
            return this;
        }
        return null;
    }
}
