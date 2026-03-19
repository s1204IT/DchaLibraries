package org.apache.xalan.processor;

import java.util.Vector;
import javax.xml.transform.TransformerException;
import org.apache.xalan.templates.Constants;
import org.apache.xalan.templates.OutputProperties;
import org.apache.xml.serializer.OutputPropertiesFactory;
import org.apache.xml.utils.QName;
import org.apache.xml.utils.SystemIDResolver;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

class ProcessorOutputElem extends XSLTElementProcessor {
    static final long serialVersionUID = 3513742319582547590L;
    private OutputProperties m_outputProperties;

    ProcessorOutputElem() {
    }

    public void setCdataSectionElements(Vector vector) {
        this.m_outputProperties.setQNameProperties(Constants.ATTRNAME_OUTPUT_CDATA_SECTION_ELEMENTS, vector);
    }

    public void setDoctypePublic(String str) {
        this.m_outputProperties.setProperty(Constants.ATTRNAME_OUTPUT_DOCTYPE_PUBLIC, str);
    }

    public void setDoctypeSystem(String str) {
        this.m_outputProperties.setProperty(Constants.ATTRNAME_OUTPUT_DOCTYPE_SYSTEM, str);
    }

    public void setEncoding(String str) {
        this.m_outputProperties.setProperty("encoding", str);
    }

    public void setIndent(boolean z) {
        this.m_outputProperties.setBooleanProperty("indent", z);
    }

    public void setMediaType(String str) {
        this.m_outputProperties.setProperty(Constants.ATTRNAME_OUTPUT_MEDIATYPE, str);
    }

    public void setMethod(QName qName) {
        this.m_outputProperties.setQNameProperty(Constants.ATTRNAME_OUTPUT_METHOD, qName);
    }

    public void setOmitXmlDeclaration(boolean z) {
        this.m_outputProperties.setBooleanProperty("omit-xml-declaration", z);
    }

    public void setStandalone(boolean z) {
        this.m_outputProperties.setBooleanProperty(Constants.ATTRNAME_OUTPUT_STANDALONE, z);
    }

    public void setVersion(String str) {
        this.m_outputProperties.setProperty("version", str);
    }

    public void setForeignAttr(String str, String str2, String str3, String str4) {
        this.m_outputProperties.setProperty(new QName(str, str2), str4);
    }

    public void addLiteralResultAttribute(String str, String str2, String str3, String str4) {
        this.m_outputProperties.setProperty(new QName(str, str2), str4);
    }

    @Override
    public void startElement(StylesheetHandler stylesheetHandler, String str, String str2, String str3, Attributes attributes) throws SAXException {
        this.m_outputProperties = new OutputProperties();
        this.m_outputProperties.setDOMBackPointer(stylesheetHandler.getOriginatingNode());
        this.m_outputProperties.setLocaterInfo(stylesheetHandler.getLocator());
        this.m_outputProperties.setUid(stylesheetHandler.nextUid());
        setPropertiesFromAttributes(stylesheetHandler, str3, attributes, this);
        String str4 = (String) this.m_outputProperties.getProperties().get(OutputPropertiesFactory.S_KEY_ENTITIES);
        if (str4 != null) {
            try {
                this.m_outputProperties.getProperties().put(OutputPropertiesFactory.S_KEY_ENTITIES, SystemIDResolver.getAbsoluteURI(str4, stylesheetHandler.getBaseIdentifier()));
            } catch (TransformerException e) {
                stylesheetHandler.error(e.getMessage(), e);
            }
        }
        stylesheetHandler.getStylesheet().setOutput(this.m_outputProperties);
        stylesheetHandler.getElemTemplateElement().appendChild(this.m_outputProperties);
        this.m_outputProperties = null;
    }
}
