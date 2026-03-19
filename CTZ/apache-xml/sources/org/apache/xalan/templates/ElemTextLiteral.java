package org.apache.xalan.templates;

import javax.xml.transform.TransformerException;
import org.apache.xalan.transformer.TransformerImpl;
import org.apache.xml.serializer.SerializationHandler;
import org.xml.sax.SAXException;

public class ElemTextLiteral extends ElemTemplateElement {
    static final long serialVersionUID = -7872620006767660088L;
    private char[] m_ch;
    private boolean m_disableOutputEscaping = false;
    private boolean m_preserveSpace;
    private String m_str;

    public void setPreserveSpace(boolean z) {
        this.m_preserveSpace = z;
    }

    public boolean getPreserveSpace() {
        return this.m_preserveSpace;
    }

    public void setChars(char[] cArr) {
        this.m_ch = cArr;
    }

    public char[] getChars() {
        return this.m_ch;
    }

    @Override
    public synchronized String getNodeValue() {
        if (this.m_str == null) {
            this.m_str = new String(this.m_ch);
        }
        return this.m_str;
    }

    public void setDisableOutputEscaping(boolean z) {
        this.m_disableOutputEscaping = z;
    }

    public boolean getDisableOutputEscaping() {
        return this.m_disableOutputEscaping;
    }

    @Override
    public int getXSLToken() {
        return 78;
    }

    @Override
    public String getNodeName() {
        return "#Text";
    }

    @Override
    public void execute(TransformerImpl transformerImpl) throws TransformerException {
        try {
            SerializationHandler resultTreeHandler = transformerImpl.getResultTreeHandler();
            if (this.m_disableOutputEscaping) {
                resultTreeHandler.processingInstruction("javax.xml.transform.disable-output-escaping", "");
            }
            resultTreeHandler.characters(this.m_ch, 0, this.m_ch.length);
            if (this.m_disableOutputEscaping) {
                resultTreeHandler.processingInstruction("javax.xml.transform.enable-output-escaping", "");
            }
        } catch (SAXException e) {
            throw new TransformerException(e);
        }
    }
}
