package mf.org.apache.xerces.impl.xs;

import java.util.Hashtable;
import java.util.Vector;
import mf.org.apache.xerces.xni.QName;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSSimpleTypeDefinition;
import mf.org.apache.xerces.xs.XSTypeDefinition;

public class SubstitutionGroupHandler {
    private static final XSElementDecl[] EMPTY_GROUP = new XSElementDecl[0];
    private static final OneSubGroup[] EMPTY_VECTOR = new OneSubGroup[0];
    private final XSElementDeclHelper fXSElementDeclHelper;
    Hashtable fSubGroupsB = new Hashtable();
    Hashtable fSubGroups = new Hashtable();

    public SubstitutionGroupHandler(XSElementDeclHelper elementDeclHelper) {
        this.fXSElementDeclHelper = elementDeclHelper;
    }

    public XSElementDecl getMatchingElemDecl(QName element, XSElementDecl exemplar) {
        XSElementDecl eDecl;
        if (element.localpart == exemplar.fName && element.uri == exemplar.fTargetNamespace) {
            return exemplar;
        }
        if (exemplar.fScope == 1 && (exemplar.fBlock & 4) == 0 && (eDecl = this.fXSElementDeclHelper.getGlobalElementDecl(element)) != null && substitutionGroupOK(eDecl, exemplar, exemplar.fBlock)) {
            return eDecl;
        }
        return null;
    }

    protected boolean substitutionGroupOK(XSElementDecl element, XSElementDecl exemplar, short blockingConstraint) {
        if (element == exemplar) {
            return true;
        }
        if ((blockingConstraint & 4) != 0) {
            return false;
        }
        XSElementDecl subGroup = element.fSubGroup;
        while (subGroup != null && subGroup != exemplar) {
            subGroup = subGroup.fSubGroup;
        }
        if (subGroup == null) {
            return false;
        }
        return typeDerivationOK(element.fType, exemplar.fType, blockingConstraint);
    }

    private boolean typeDerivationOK(XSTypeDefinition derived, XSTypeDefinition base, short blockingConstraint) {
        short devMethod = 0;
        short blockConstraint = blockingConstraint;
        XSTypeDefinition type = derived;
        while (type != base && type != SchemaGrammar.fAnyType) {
            if (type.getTypeCategory() == 15) {
                devMethod = (short) (((XSComplexTypeDecl) type).fDerivedBy | devMethod);
            } else {
                devMethod = (short) (devMethod | 2);
            }
            type = type.getBaseType();
            if (type == null) {
                type = SchemaGrammar.fAnyType;
            }
            if (type.getTypeCategory() == 15) {
                blockConstraint = (short) (((XSComplexTypeDecl) type).fBlock | blockConstraint);
            }
        }
        if (type == base) {
            return (devMethod & blockConstraint) == 0;
        }
        if (base.getTypeCategory() == 16) {
            XSSimpleTypeDefinition st = (XSSimpleTypeDefinition) base;
            if (st.getVariety() == 3) {
                XSObjectList memberTypes = st.getMemberTypes();
                int length = memberTypes.getLength();
                for (int i = 0; i < length; i++) {
                    if (typeDerivationOK(derived, (XSTypeDefinition) memberTypes.item(i), blockingConstraint)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean inSubstitutionGroup(XSElementDecl element, XSElementDecl exemplar) {
        return substitutionGroupOK(element, exemplar, exemplar.fBlock);
    }

    public void reset() {
        this.fSubGroupsB.clear();
        this.fSubGroups.clear();
    }

    public void addSubstitutionGroup(XSElementDecl[] elements) {
        for (int i = elements.length - 1; i >= 0; i--) {
            XSElementDecl element = elements[i];
            XSElementDecl subHead = element.fSubGroup;
            Vector subGroup = (Vector) this.fSubGroupsB.get(subHead);
            if (subGroup == null) {
                subGroup = new Vector();
                this.fSubGroupsB.put(subHead, subGroup);
            }
            subGroup.addElement(element);
        }
    }

    public XSElementDecl[] getSubstitutionGroup(XSElementDecl element) {
        Object subGroup = this.fSubGroups.get(element);
        if (subGroup != null) {
            return (XSElementDecl[]) subGroup;
        }
        if ((element.fBlock & 4) != 0) {
            this.fSubGroups.put(element, EMPTY_GROUP);
            return EMPTY_GROUP;
        }
        OneSubGroup[] groupB = getSubGroupB(element, new OneSubGroup());
        int len = groupB.length;
        int rlen = 0;
        XSElementDecl[] ret = new XSElementDecl[len];
        for (int i = 0; i < len; i++) {
            if ((element.fBlock & groupB[i].dMethod) == 0) {
                ret[rlen] = groupB[i].sub;
                rlen++;
            }
        }
        if (rlen < len) {
            XSElementDecl[] ret1 = new XSElementDecl[rlen];
            System.arraycopy(ret, 0, ret1, 0, rlen);
            ret = ret1;
        }
        this.fSubGroups.put(element, ret);
        return ret;
    }

    private OneSubGroup[] getSubGroupB(XSElementDecl element, OneSubGroup methods) {
        ?? r0 = this.fSubGroupsB.get(element);
        if (r0 == 0) {
            this.fSubGroupsB.put(element, EMPTY_VECTOR);
            return EMPTY_VECTOR;
        }
        if (r0 instanceof OneSubGroup[]) {
            return r0;
        }
        Vector group = (Vector) r0;
        Vector newGroup = new Vector();
        for (int i = group.size() - 1; i >= 0; i--) {
            XSElementDecl sub = (XSElementDecl) group.elementAt(i);
            if (getDBMethods(sub.fType, element.fType, methods)) {
                short dMethod = methods.dMethod;
                short bMethod = methods.bMethod;
                newGroup.addElement(new OneSubGroup(sub, methods.dMethod, methods.bMethod));
                OneSubGroup[] group1 = getSubGroupB(sub, methods);
                for (int j = group1.length - 1; j >= 0; j--) {
                    short dSubMethod = (short) (group1[j].dMethod | dMethod);
                    short bSubMethod = (short) (group1[j].bMethod | bMethod);
                    if ((dSubMethod & bSubMethod) == 0) {
                        newGroup.addElement(new OneSubGroup(group1[j].sub, dSubMethod, bSubMethod));
                    }
                }
            }
        }
        int i2 = newGroup.size();
        OneSubGroup[] ret = new OneSubGroup[i2];
        for (int i3 = newGroup.size() - 1; i3 >= 0; i3--) {
            ret[i3] = (OneSubGroup) newGroup.elementAt(i3);
        }
        this.fSubGroupsB.put(element, ret);
        return ret;
    }

    private boolean getDBMethods(XSTypeDefinition typed, XSTypeDefinition typeb, OneSubGroup methods) {
        short dMethod = 0;
        short bMethod = 0;
        while (typed != typeb && typed != SchemaGrammar.fAnyType) {
            if (typed.getTypeCategory() == 15) {
                dMethod = (short) (((XSComplexTypeDecl) typed).fDerivedBy | dMethod);
            } else {
                dMethod = (short) (dMethod | 2);
            }
            typed = typed.getBaseType();
            if (typed == null) {
                typed = SchemaGrammar.fAnyType;
            }
            if (typed.getTypeCategory() == 15) {
                bMethod = (short) (((XSComplexTypeDecl) typed).fBlock | bMethod);
            }
        }
        if (typed != typeb || (dMethod & bMethod) != 0) {
            return false;
        }
        methods.dMethod = dMethod;
        methods.bMethod = bMethod;
        return true;
    }

    private static final class OneSubGroup {
        short bMethod;
        short dMethod;
        XSElementDecl sub;

        OneSubGroup() {
        }

        OneSubGroup(XSElementDecl sub, short dMethod, short bMethod) {
            this.sub = sub;
            this.dMethod = dMethod;
            this.bMethod = bMethod;
        }
    }
}
