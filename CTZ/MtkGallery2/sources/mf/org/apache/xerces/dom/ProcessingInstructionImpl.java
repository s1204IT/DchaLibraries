package mf.org.apache.xerces.dom;

import mf.org.w3c.dom.ProcessingInstruction;

public class ProcessingInstructionImpl extends CharacterDataImpl implements ProcessingInstruction {
    static final long serialVersionUID = 7554435174099981510L;
    protected String target;

    public ProcessingInstructionImpl(CoreDocumentImpl ownerDoc, String target, String data) {
        super(ownerDoc, data);
        this.target = target;
    }

    @Override
    public short getNodeType() {
        return (short) 7;
    }

    @Override
    public String getNodeName() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.target;
    }

    @Override
    public String getTarget() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.target;
    }

    @Override
    public String getBaseURI() {
        if (needsSyncData()) {
            synchronizeData();
        }
        return this.ownerNode.getBaseURI();
    }
}
