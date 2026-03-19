package mf.org.apache.xerces.dom;

import java.util.Vector;
import mf.org.apache.xerces.dom3.as.ASAttributeDeclaration;
import mf.org.apache.xerces.dom3.as.ASContentModel;
import mf.org.apache.xerces.dom3.as.ASElementDeclaration;
import mf.org.apache.xerces.dom3.as.ASEntityDeclaration;
import mf.org.apache.xerces.dom3.as.ASModel;
import mf.org.apache.xerces.dom3.as.ASNamedObjectMap;
import mf.org.apache.xerces.dom3.as.ASNotationDeclaration;
import mf.org.apache.xerces.dom3.as.ASObject;
import mf.org.apache.xerces.dom3.as.ASObjectList;
import mf.org.apache.xerces.dom3.as.DOMASException;
import mf.org.apache.xerces.impl.xs.SchemaGrammar;
import mf.org.w3c.dom.DOMException;

public class ASModelImpl implements ASModel {
    protected Vector fASModels;
    protected SchemaGrammar fGrammar;
    boolean fNamespaceAware;

    public ASModelImpl() {
        this.fNamespaceAware = true;
        this.fGrammar = null;
        this.fASModels = new Vector();
    }

    public ASModelImpl(boolean isNamespaceAware) {
        this.fNamespaceAware = true;
        this.fGrammar = null;
        this.fASModels = new Vector();
        this.fNamespaceAware = isNamespaceAware;
    }

    @Override
    public short getAsNodeType() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASModel getOwnerASModel() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public void setOwnerASModel(ASModel ownerASModel) {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public String getNodeName() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public void setNodeName(String nodeName) {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public String getPrefix() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public void setPrefix(String prefix) {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public String getLocalName() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public void setLocalName(String localName) {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public String getNamespaceURI() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public void setNamespaceURI(String namespaceURI) {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASObject cloneASObject(boolean deep) {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public boolean getIsNamespaceAware() {
        return this.fNamespaceAware;
    }

    @Override
    public short getUsageLocation() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public String getAsLocation() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public void setAsLocation(String asLocation) {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public String getAsHint() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public void setAsHint(String asHint) {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    public boolean getContainer() {
        return this.fGrammar != null;
    }

    @Override
    public ASNamedObjectMap getElementDeclarations() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASNamedObjectMap getAttributeDeclarations() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASNamedObjectMap getNotationDeclarations() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASNamedObjectMap getEntityDeclarations() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASNamedObjectMap getContentModelDeclarations() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public void addASModel(ASModel abstractSchema) {
        this.fASModels.addElement(abstractSchema);
    }

    @Override
    public ASObjectList getASModels() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public void removeAS(ASModel as) {
        this.fASModels.removeElement(as);
    }

    @Override
    public boolean validate() {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    public void importASObject(ASObject asobject) {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    public void insertASObject(ASObject asobject) {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASElementDeclaration createASElementDeclaration(String namespaceURI, String name) throws DOMException {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASAttributeDeclaration createASAttributeDeclaration(String namespaceURI, String name) throws DOMException {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASNotationDeclaration createASNotationDeclaration(String namespaceURI, String name, String systemId, String publicId) throws DOMException {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASEntityDeclaration createASEntityDeclaration(String name) throws DOMException {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    @Override
    public ASContentModel createASContentModel(int minOccurs, int maxOccurs, short operator) throws DOMASException {
        String msg = DOMMessageFormatter.formatMessage(DOMMessageFormatter.DOM_DOMAIN, "NOT_SUPPORTED_ERR", null);
        throw new DOMException((short) 9, msg);
    }

    public SchemaGrammar getGrammar() {
        return this.fGrammar;
    }

    public void setGrammar(SchemaGrammar grammar) {
        this.fGrammar = grammar;
    }

    public Vector getInternalASModels() {
        return this.fASModels;
    }
}
