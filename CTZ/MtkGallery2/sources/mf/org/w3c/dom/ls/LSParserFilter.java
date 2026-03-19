package mf.org.w3c.dom.ls;

import mf.org.w3c.dom.Element;
import mf.org.w3c.dom.Node;

public interface LSParserFilter {
    short acceptNode(Node node);

    int getWhatToShow();

    short startElement(Element element);
}
