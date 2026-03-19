package mf.org.apache.xerces.dom3.as;

import mf.org.w3c.dom.ls.LSInput;
import mf.org.w3c.dom.ls.LSParser;

public interface DOMASBuilder extends LSParser {
    ASModel getAbstractSchema();

    ASModel parseASInputSource(LSInput lSInput) throws Exception;

    ASModel parseASURI(String str) throws Exception;

    void setAbstractSchema(ASModel aSModel);
}
