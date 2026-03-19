package mf.org.apache.xerces.impl.xs.traversers;

import mf.org.apache.xerces.impl.xs.opti.ElementImpl;
import mf.org.w3c.dom.Element;

final class XSAnnotationInfo {
    String fAnnotation;
    int fCharOffset;
    int fColumn;
    int fLine;
    XSAnnotationInfo next;

    XSAnnotationInfo(String annotation, int line, int column, int charOffset) {
        this.fAnnotation = annotation;
        this.fLine = line;
        this.fColumn = column;
        this.fCharOffset = charOffset;
    }

    XSAnnotationInfo(String annotation, Element element) {
        this.fAnnotation = annotation;
        if (element instanceof ElementImpl) {
            this.fLine = element.getLineNumber();
            this.fColumn = element.getColumnNumber();
            this.fCharOffset = element.getCharacterOffset();
        } else {
            this.fLine = -1;
            this.fColumn = -1;
            this.fCharOffset = -1;
        }
    }
}
