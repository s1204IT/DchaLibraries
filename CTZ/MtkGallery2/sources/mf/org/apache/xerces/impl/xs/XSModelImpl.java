package mf.org.apache.xerces.impl.xs;

import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Vector;
import mf.org.apache.xerces.impl.xs.util.StringListImpl;
import mf.org.apache.xerces.impl.xs.util.XSNamedMap4Types;
import mf.org.apache.xerces.impl.xs.util.XSNamedMapImpl;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.util.SymbolHash;
import mf.org.apache.xerces.util.XMLSymbols;
import mf.org.apache.xerces.xs.StringList;
import mf.org.apache.xerces.xs.XSAttributeDeclaration;
import mf.org.apache.xerces.xs.XSAttributeGroupDefinition;
import mf.org.apache.xerces.xs.XSElementDeclaration;
import mf.org.apache.xerces.xs.XSIDCDefinition;
import mf.org.apache.xerces.xs.XSModel;
import mf.org.apache.xerces.xs.XSModelGroupDefinition;
import mf.org.apache.xerces.xs.XSNamedMap;
import mf.org.apache.xerces.xs.XSNamespaceItem;
import mf.org.apache.xerces.xs.XSNamespaceItemList;
import mf.org.apache.xerces.xs.XSNotationDeclaration;
import mf.org.apache.xerces.xs.XSObject;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSTypeDefinition;

public final class XSModelImpl extends AbstractList implements XSModel, XSNamespaceItemList {
    private static final boolean[] GLOBAL_COMP = {false, true, true, true, false, true, true, false, false, false, true, true, false, false, false, true, true};
    private static final short MAX_COMP_IDX = 16;
    private XSObjectList fAnnotations;
    private final XSNamedMap[] fGlobalComponents;
    private final int fGrammarCount;
    private final SchemaGrammar[] fGrammarList;
    private final SymbolHash fGrammarMap;
    private final boolean fHasIDC;
    private final XSNamedMap[][] fNSComponents;
    private final String[] fNamespaces;
    private final StringList fNamespacesList;
    private final SymbolHash fSubGroupMap;

    public XSModelImpl(SchemaGrammar[] grammars) {
        this(grammars, (short) 1);
    }

    public XSModelImpl(SchemaGrammar[] grammars, short s4sVersion) {
        this.fAnnotations = null;
        int len = grammars.length;
        int initialSize = Math.max(len + 1, 5);
        String[] namespaces = new String[initialSize];
        SchemaGrammar[] grammarList = new SchemaGrammar[initialSize];
        boolean hasS4S = false;
        for (int i = 0; i < len; i++) {
            SchemaGrammar sg = grammars[i];
            String tns = sg.getTargetNamespace();
            namespaces[i] = tns;
            grammarList[i] = sg;
            if (tns == SchemaSymbols.URI_SCHEMAFORSCHEMA) {
                hasS4S = true;
            }
        }
        if (!hasS4S) {
            namespaces[len] = SchemaSymbols.URI_SCHEMAFORSCHEMA;
            grammarList[len] = SchemaGrammar.getS4SGrammar(s4sVersion);
            len++;
        }
        int i2 = 0;
        while (i2 < len) {
            SchemaGrammar sg1 = grammarList[i2];
            Vector gs = sg1.getImportedGrammars();
            int j = gs == null ? -1 : gs.size() - 1;
            int len2 = len;
            String[] namespaces2 = namespaces;
            SchemaGrammar[] grammarList2 = grammarList;
            for (int j2 = j; j2 >= 0; j2--) {
                SchemaGrammar sg2 = (SchemaGrammar) gs.elementAt(j2);
                int k = 0;
                while (k < len2 && sg2 != grammarList2[k]) {
                    k++;
                }
                if (k == len2) {
                    if (len2 == grammarList2.length) {
                        String[] newSA = new String[len2 * 2];
                        System.arraycopy(namespaces2, 0, newSA, 0, len2);
                        namespaces2 = newSA;
                        SchemaGrammar[] newGA = new SchemaGrammar[len2 * 2];
                        System.arraycopy(grammarList2, 0, newGA, 0, len2);
                        grammarList2 = newGA;
                    }
                    namespaces2[len2] = sg2.getTargetNamespace();
                    grammarList2[len2] = sg2;
                    len2++;
                }
            }
            i2++;
            len = len2;
            namespaces = namespaces2;
            grammarList = grammarList2;
        }
        this.fNamespaces = namespaces;
        this.fGrammarList = grammarList;
        boolean hasIDC = false;
        this.fGrammarMap = new SymbolHash(len * 2);
        for (int i3 = 0; i3 < len; i3++) {
            this.fGrammarMap.put(null2EmptyString(this.fNamespaces[i3]), this.fGrammarList[i3]);
            if (this.fGrammarList[i3].hasIDConstraints()) {
                hasIDC = true;
            }
        }
        this.fHasIDC = hasIDC;
        this.fGrammarCount = len;
        this.fGlobalComponents = new XSNamedMap[17];
        this.fNSComponents = (XSNamedMap[][]) Array.newInstance((Class<?>) XSNamedMap.class, len, 17);
        this.fNamespacesList = new StringListImpl(this.fNamespaces, this.fGrammarCount);
        this.fSubGroupMap = buildSubGroups();
    }

    private SymbolHash buildSubGroups_Org() {
        SubstitutionGroupHandler sgHandler = new SubstitutionGroupHandler(null);
        for (int i = 0; i < this.fGrammarCount; i++) {
            sgHandler.addSubstitutionGroup(this.fGrammarList[i].getSubstitutionGroups());
        }
        XSNamedMap elements = getComponents((short) 2);
        int len = elements.getLength();
        SymbolHash subGroupMap = new SymbolHash(len * 2);
        for (int i2 = 0; i2 < len; i2++) {
            XSElementDecl head = (XSElementDecl) elements.item(i2);
            XSElementDeclaration[] subGroup = sgHandler.getSubstitutionGroup(head);
            subGroupMap.put(head, subGroup.length > 0 ? new XSObjectListImpl(subGroup, subGroup.length) : XSObjectListImpl.EMPTY_LIST);
        }
        return subGroupMap;
    }

    private SymbolHash buildSubGroups() {
        SubstitutionGroupHandler sgHandler = new SubstitutionGroupHandler(null);
        for (int i = 0; i < this.fGrammarCount; i++) {
            sgHandler.addSubstitutionGroup(this.fGrammarList[i].getSubstitutionGroups());
        }
        XSObjectListImpl elements = getGlobalElements();
        int len = elements.getLength();
        SymbolHash subGroupMap = new SymbolHash(len * 2);
        for (int i2 = 0; i2 < len; i2++) {
            XSElementDecl head = (XSElementDecl) elements.item(i2);
            XSElementDeclaration[] subGroup = sgHandler.getSubstitutionGroup(head);
            subGroupMap.put(head, subGroup.length > 0 ? new XSObjectListImpl(subGroup, subGroup.length) : XSObjectListImpl.EMPTY_LIST);
        }
        return subGroupMap;
    }

    private XSObjectListImpl getGlobalElements() {
        SymbolHash[] tables = new SymbolHash[this.fGrammarCount];
        int length = 0;
        for (int i = 0; i < this.fGrammarCount; i++) {
            tables[i] = this.fGrammarList[i].fAllGlobalElemDecls;
            length += tables[i].getLength();
        }
        if (length == 0) {
            return XSObjectListImpl.EMPTY_LIST;
        }
        XSObject[] components = new XSObject[length];
        int start = 0;
        for (int i2 = 0; i2 < this.fGrammarCount; i2++) {
            tables[i2].getValues(components, start);
            start += tables[i2].getLength();
        }
        return new XSObjectListImpl(components, length);
    }

    @Override
    public StringList getNamespaces() {
        return this.fNamespacesList;
    }

    @Override
    public XSNamespaceItemList getNamespaceItems() {
        return this;
    }

    @Override
    public synchronized XSNamedMap getComponents(short objectType) {
        if (objectType > 0 && objectType <= 16) {
            if (GLOBAL_COMP[objectType]) {
                SymbolHash[] tables = new SymbolHash[this.fGrammarCount];
                if (this.fGlobalComponents[objectType] == null) {
                    for (int i = 0; i < this.fGrammarCount; i++) {
                        switch (objectType) {
                            case 1:
                                tables[i] = this.fGrammarList[i].fGlobalAttrDecls;
                                break;
                            case 2:
                                tables[i] = this.fGrammarList[i].fGlobalElemDecls;
                                break;
                            case 3:
                            case 15:
                            case 16:
                                tables[i] = this.fGrammarList[i].fGlobalTypeDecls;
                                break;
                            case 5:
                                tables[i] = this.fGrammarList[i].fGlobalAttrGrpDecls;
                                break;
                            case 6:
                                tables[i] = this.fGrammarList[i].fGlobalGroupDecls;
                                break;
                            case 10:
                                tables[i] = this.fGrammarList[i].fGlobalIDConstraintDecls;
                                break;
                            case 11:
                                tables[i] = this.fGrammarList[i].fGlobalNotationDecls;
                                break;
                        }
                    }
                    if (objectType == 15 || objectType == 16) {
                        this.fGlobalComponents[objectType] = new XSNamedMap4Types(this.fNamespaces, tables, this.fGrammarCount, objectType);
                    } else {
                        this.fGlobalComponents[objectType] = new XSNamedMapImpl(this.fNamespaces, tables, this.fGrammarCount);
                    }
                }
                return this.fGlobalComponents[objectType];
            }
        }
        return XSNamedMapImpl.EMPTY_MAP;
    }

    @Override
    public synchronized XSNamedMap getComponentsByNamespace(short objectType, String namespace) {
        if (objectType > 0 && objectType <= 16) {
            if (GLOBAL_COMP[objectType]) {
                int i = 0;
                if (namespace != null) {
                    while (i < this.fGrammarCount && !namespace.equals(this.fNamespaces[i])) {
                        i++;
                    }
                } else {
                    while (i < this.fGrammarCount && this.fNamespaces[i] != null) {
                        i++;
                    }
                }
                if (i == this.fGrammarCount) {
                    return XSNamedMapImpl.EMPTY_MAP;
                }
                if (this.fNSComponents[i][objectType] == null) {
                    SymbolHash table = null;
                    switch (objectType) {
                        case 1:
                            table = this.fGrammarList[i].fGlobalAttrDecls;
                            break;
                        case 2:
                            table = this.fGrammarList[i].fGlobalElemDecls;
                            break;
                        case 3:
                        case 15:
                        case 16:
                            table = this.fGrammarList[i].fGlobalTypeDecls;
                            break;
                        case 5:
                            table = this.fGrammarList[i].fGlobalAttrGrpDecls;
                            break;
                        case 6:
                            table = this.fGrammarList[i].fGlobalGroupDecls;
                            break;
                        case 10:
                            table = this.fGrammarList[i].fGlobalIDConstraintDecls;
                            break;
                        case 11:
                            table = this.fGrammarList[i].fGlobalNotationDecls;
                            break;
                    }
                    if (objectType == 15 || objectType == 16) {
                        this.fNSComponents[i][objectType] = new XSNamedMap4Types(namespace, table, objectType);
                    } else {
                        this.fNSComponents[i][objectType] = new XSNamedMapImpl(namespace, table);
                    }
                }
                return this.fNSComponents[i][objectType];
            }
        }
        return XSNamedMapImpl.EMPTY_MAP;
    }

    @Override
    public XSTypeDefinition getTypeDefinition(String name, String namespace) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSTypeDefinition) sg.fGlobalTypeDecls.get(name);
    }

    public XSTypeDefinition getTypeDefinition(String name, String namespace, String loc) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalTypeDecl(name, loc);
    }

    @Override
    public XSAttributeDeclaration getAttributeDeclaration(String name, String namespace) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSAttributeDeclaration) sg.fGlobalAttrDecls.get(name);
    }

    public XSAttributeDeclaration getAttributeDeclaration(String name, String namespace, String loc) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalAttributeDecl(name, loc);
    }

    @Override
    public XSElementDeclaration getElementDeclaration(String name, String namespace) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSElementDeclaration) sg.fGlobalElemDecls.get(name);
    }

    public XSElementDeclaration getElementDeclaration(String name, String namespace, String loc) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalElementDecl(name, loc);
    }

    @Override
    public XSAttributeGroupDefinition getAttributeGroup(String name, String namespace) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSAttributeGroupDefinition) sg.fGlobalAttrGrpDecls.get(name);
    }

    public XSAttributeGroupDefinition getAttributeGroup(String name, String namespace, String loc) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalAttributeGroupDecl(name, loc);
    }

    @Override
    public XSModelGroupDefinition getModelGroupDefinition(String name, String namespace) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSModelGroupDefinition) sg.fGlobalGroupDecls.get(name);
    }

    public XSModelGroupDefinition getModelGroupDefinition(String name, String namespace, String loc) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalGroupDecl(name, loc);
    }

    @Override
    public XSIDCDefinition getIDCDefinition(String name, String namespace) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSIDCDefinition) sg.fGlobalIDConstraintDecls.get(name);
    }

    public XSIDCDefinition getIDCDefinition(String name, String namespace, String loc) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getIDConstraintDecl(name, loc);
    }

    @Override
    public XSNotationDeclaration getNotationDeclaration(String name, String namespace) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return (XSNotationDeclaration) sg.fGlobalNotationDecls.get(name);
    }

    public XSNotationDeclaration getNotationDeclaration(String name, String namespace, String loc) {
        SchemaGrammar sg = (SchemaGrammar) this.fGrammarMap.get(null2EmptyString(namespace));
        if (sg == null) {
            return null;
        }
        return sg.getGlobalNotationDecl(name, loc);
    }

    @Override
    public synchronized XSObjectList getAnnotations() {
        if (this.fAnnotations != null) {
            return this.fAnnotations;
        }
        int totalAnnotations = 0;
        for (int i = 0; i < this.fGrammarCount; i++) {
            totalAnnotations += this.fGrammarList[i].fNumAnnotations;
        }
        if (totalAnnotations == 0) {
            this.fAnnotations = XSObjectListImpl.EMPTY_LIST;
            return this.fAnnotations;
        }
        XSAnnotationImpl[] annotations = new XSAnnotationImpl[totalAnnotations];
        int currPos = 0;
        for (int i2 = 0; i2 < this.fGrammarCount; i2++) {
            SchemaGrammar currGrammar = this.fGrammarList[i2];
            if (currGrammar.fNumAnnotations > 0) {
                System.arraycopy(currGrammar.fAnnotations, 0, annotations, currPos, currGrammar.fNumAnnotations);
                currPos += currGrammar.fNumAnnotations;
            }
        }
        this.fAnnotations = new XSObjectListImpl(annotations, annotations.length);
        return this.fAnnotations;
    }

    private static final String null2EmptyString(String str) {
        return str == null ? XMLSymbols.EMPTY_STRING : str;
    }

    public boolean hasIDConstraints() {
        return this.fHasIDC;
    }

    @Override
    public XSObjectList getSubstitutionGroup(XSElementDeclaration head) {
        return (XSObjectList) this.fSubGroupMap.get(head);
    }

    @Override
    public int getLength() {
        return this.fGrammarCount;
    }

    @Override
    public XSNamespaceItem item(int index) {
        if (index < 0 || index >= this.fGrammarCount) {
            return null;
        }
        return this.fGrammarList[index];
    }

    @Override
    public Object get(int index) {
        if (index >= 0 && index < this.fGrammarCount) {
            return this.fGrammarList[index];
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    @Override
    public int size() {
        return getLength();
    }

    @Override
    public Iterator iterator() {
        return listIterator0(0);
    }

    @Override
    public ListIterator listIterator() {
        return listIterator0(0);
    }

    @Override
    public ListIterator listIterator(int index) {
        if (index >= 0 && index < this.fGrammarCount) {
            return listIterator0(index);
        }
        throw new IndexOutOfBoundsException("Index: " + index);
    }

    private ListIterator listIterator0(int index) {
        return new XSNamespaceItemListIterator(index);
    }

    @Override
    public Object[] toArray() {
        Object[] a = new Object[this.fGrammarCount];
        toArray0(a);
        return a;
    }

    @Override
    public Object[] toArray(Object[] a) {
        if (a.length < this.fGrammarCount) {
            a = (Object[]) Array.newInstance(a.getClass().getComponentType(), this.fGrammarCount);
        }
        toArray0(a);
        if (a.length > this.fGrammarCount) {
            a[this.fGrammarCount] = null;
        }
        return a;
    }

    private void toArray0(Object[] a) {
        if (this.fGrammarCount > 0) {
            System.arraycopy(this.fGrammarList, 0, a, 0, this.fGrammarCount);
        }
    }

    private final class XSNamespaceItemListIterator implements ListIterator {
        private int index;

        public XSNamespaceItemListIterator(int index) {
            this.index = index;
        }

        @Override
        public boolean hasNext() {
            return this.index < XSModelImpl.this.fGrammarCount;
        }

        @Override
        public Object next() {
            if (this.index < XSModelImpl.this.fGrammarCount) {
                SchemaGrammar[] schemaGrammarArr = XSModelImpl.this.fGrammarList;
                int i = this.index;
                this.index = i + 1;
                return schemaGrammarArr[i];
            }
            throw new NoSuchElementException();
        }

        @Override
        public boolean hasPrevious() {
            return this.index > 0;
        }

        @Override
        public Object previous() {
            if (this.index > 0) {
                SchemaGrammar[] schemaGrammarArr = XSModelImpl.this.fGrammarList;
                int i = this.index - 1;
                this.index = i;
                return schemaGrammarArr[i];
            }
            throw new NoSuchElementException();
        }

        @Override
        public int nextIndex() {
            return this.index;
        }

        @Override
        public int previousIndex() {
            return this.index - 1;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void set(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void add(Object o) {
            throw new UnsupportedOperationException();
        }
    }
}
