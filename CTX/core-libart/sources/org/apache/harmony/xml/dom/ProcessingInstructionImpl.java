package org.apache.harmony.xml.dom;

import org.w3c.dom.DOMException;
import org.w3c.dom.ProcessingInstruction;

public final class ProcessingInstructionImpl extends LeafNodeImpl implements ProcessingInstruction {
    private String data;
    private String target;

    ProcessingInstructionImpl(DocumentImpl documentImpl, String str, String str2) {
        super(documentImpl);
        this.target = str;
        this.data = str2;
    }

    @Override
    public String getData() {
        return this.data;
    }

    @Override
    public String getNodeName() {
        return this.target;
    }

    @Override
    public short getNodeType() {
        return (short) 7;
    }

    @Override
    public String getNodeValue() {
        return this.data;
    }

    @Override
    public String getTarget() {
        return this.target;
    }

    @Override
    public void setData(String str) throws DOMException {
        this.data = str;
    }
}
