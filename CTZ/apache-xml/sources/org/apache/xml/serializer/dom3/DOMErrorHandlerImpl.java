package org.apache.xml.serializer.dom3;

import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;

final class DOMErrorHandlerImpl implements DOMErrorHandler {
    DOMErrorHandlerImpl() {
    }

    @Override
    public boolean handleError(DOMError dOMError) {
        String str;
        boolean z = true;
        if (dOMError.getSeverity() == 1) {
            z = false;
            str = "[Warning]";
        } else if (dOMError.getSeverity() == 2) {
            str = "[Error]";
        } else if (dOMError.getSeverity() == 3) {
            str = "[Fatal Error]";
        } else {
            str = null;
        }
        System.err.println(str + ": " + dOMError.getMessage() + "\t");
        System.err.println("Type : " + dOMError.getType() + "\tRelated Data: " + dOMError.getRelatedData() + "\tRelated Exception: " + dOMError.getRelatedException());
        return z;
    }
}
