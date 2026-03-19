package mf.org.apache.xerces.impl.xs;

import mf.org.apache.xerces.impl.xs.util.StringListImpl;
import mf.org.apache.xerces.impl.xs.util.XSObjectListImpl;
import mf.org.apache.xerces.xs.StringList;
import mf.org.apache.xerces.xs.XSAnnotation;
import mf.org.apache.xerces.xs.XSNamespaceItem;
import mf.org.apache.xerces.xs.XSObjectList;
import mf.org.apache.xerces.xs.XSWildcard;

public class XSWildcardDecl implements XSWildcard {
    public static final String ABSENT = null;
    public String[] fNamespaceList;
    public short fType = 1;
    public short fProcessContents = 1;
    public XSObjectList fAnnotations = null;
    private String fDescription = null;

    public boolean allowNamespace(String namespace) {
        if (this.fType == 1) {
            return true;
        }
        if (this.fType == 2) {
            boolean found = false;
            int listNum = this.fNamespaceList.length;
            for (int i = 0; i < listNum && !found; i++) {
                if (namespace == this.fNamespaceList[i]) {
                    found = true;
                }
            }
            if (!found) {
                return true;
            }
        }
        if (this.fType == 3) {
            int listNum2 = this.fNamespaceList.length;
            for (int i2 = 0; i2 < listNum2; i2++) {
                if (namespace == this.fNamespaceList[i2]) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public boolean isSubsetOf(XSWildcardDecl superWildcard) {
        if (superWildcard == null) {
            return false;
        }
        if (superWildcard.fType == 1) {
            return true;
        }
        if (this.fType == 2 && superWildcard.fType == 2 && this.fNamespaceList[0] == superWildcard.fNamespaceList[0]) {
            return true;
        }
        if (this.fType == 3) {
            if (superWildcard.fType == 3 && subset2sets(this.fNamespaceList, superWildcard.fNamespaceList)) {
                return true;
            }
            if (superWildcard.fType == 2 && !elementInSet(superWildcard.fNamespaceList[0], this.fNamespaceList) && !elementInSet(ABSENT, this.fNamespaceList)) {
                return true;
            }
        }
        return false;
    }

    public boolean weakerProcessContents(XSWildcardDecl superWildcard) {
        return (this.fProcessContents == 3 && superWildcard.fProcessContents == 1) || (this.fProcessContents == 2 && superWildcard.fProcessContents != 2);
    }

    public XSWildcardDecl performUnionWith(XSWildcardDecl wildcard, short processContents) {
        String[] other;
        String[] list;
        if (wildcard == null) {
            return null;
        }
        XSWildcardDecl unionWildcard = new XSWildcardDecl();
        unionWildcard.fProcessContents = processContents;
        if (areSame(wildcard)) {
            unionWildcard.fType = this.fType;
            unionWildcard.fNamespaceList = this.fNamespaceList;
        } else if (this.fType == 1 || wildcard.fType == 1) {
            unionWildcard.fType = (short) 1;
        } else if (this.fType == 3 && wildcard.fType == 3) {
            unionWildcard.fType = (short) 3;
            unionWildcard.fNamespaceList = union2sets(this.fNamespaceList, wildcard.fNamespaceList);
        } else if (this.fType == 2 && wildcard.fType == 2) {
            unionWildcard.fType = (short) 2;
            unionWildcard.fNamespaceList = new String[2];
            unionWildcard.fNamespaceList[0] = ABSENT;
            unionWildcard.fNamespaceList[1] = ABSENT;
        } else if ((this.fType == 2 && wildcard.fType == 3) || (this.fType == 3 && wildcard.fType == 2)) {
            if (this.fType == 2) {
                other = this.fNamespaceList;
                list = wildcard.fNamespaceList;
            } else {
                other = wildcard.fNamespaceList;
                list = this.fNamespaceList;
            }
            boolean foundAbsent = elementInSet(ABSENT, list);
            if (other[0] != ABSENT) {
                boolean foundNS = elementInSet(other[0], list);
                if (foundNS && foundAbsent) {
                    unionWildcard.fType = (short) 1;
                } else if (foundNS && !foundAbsent) {
                    unionWildcard.fType = (short) 2;
                    unionWildcard.fNamespaceList = new String[2];
                    unionWildcard.fNamespaceList[0] = ABSENT;
                    unionWildcard.fNamespaceList[1] = ABSENT;
                } else {
                    if (!foundNS && foundAbsent) {
                        return null;
                    }
                    unionWildcard.fType = (short) 2;
                    unionWildcard.fNamespaceList = other;
                }
            } else if (foundAbsent) {
                unionWildcard.fType = (short) 1;
            } else {
                unionWildcard.fType = (short) 2;
                unionWildcard.fNamespaceList = other;
            }
        }
        return unionWildcard;
    }

    public XSWildcardDecl performIntersectionWith(XSWildcardDecl wildcard, short processContents) {
        String[] other;
        String[] list;
        if (wildcard == null) {
            return null;
        }
        XSWildcardDecl intersectWildcard = new XSWildcardDecl();
        intersectWildcard.fProcessContents = processContents;
        if (areSame(wildcard)) {
            intersectWildcard.fType = this.fType;
            intersectWildcard.fNamespaceList = this.fNamespaceList;
        } else if (this.fType == 1 || wildcard.fType == 1) {
            XSWildcardDecl other2 = this;
            if (this.fType == 1) {
                other2 = wildcard;
            }
            intersectWildcard.fType = other2.fType;
            intersectWildcard.fNamespaceList = other2.fNamespaceList;
        } else if ((this.fType == 2 && wildcard.fType == 3) || (this.fType == 3 && wildcard.fType == 2)) {
            if (this.fType == 2) {
                other = this.fNamespaceList;
                list = wildcard.fNamespaceList;
            } else {
                other = wildcard.fNamespaceList;
                list = this.fNamespaceList;
            }
            String[] other3 = other;
            String[] list2 = list;
            int listSize = list2.length;
            String[] intersect = new String[listSize];
            int newSize = 0;
            for (int i = 0; i < listSize; i++) {
                if (list2[i] != other3[0] && list2[i] != ABSENT) {
                    intersect[newSize] = list2[i];
                    newSize++;
                }
            }
            intersectWildcard.fType = (short) 3;
            intersectWildcard.fNamespaceList = new String[newSize];
            System.arraycopy(intersect, 0, intersectWildcard.fNamespaceList, 0, newSize);
        } else if (this.fType == 3 && wildcard.fType == 3) {
            intersectWildcard.fType = (short) 3;
            intersectWildcard.fNamespaceList = intersect2sets(this.fNamespaceList, wildcard.fNamespaceList);
        } else if (this.fType == 2 && wildcard.fType == 2) {
            if (this.fNamespaceList[0] != ABSENT && wildcard.fNamespaceList[0] != ABSENT) {
                return null;
            }
            XSWildcardDecl other4 = this;
            if (this.fNamespaceList[0] == ABSENT) {
                other4 = wildcard;
            }
            intersectWildcard.fType = other4.fType;
            intersectWildcard.fNamespaceList = other4.fNamespaceList;
        }
        return intersectWildcard;
    }

    private boolean areSame(XSWildcardDecl wildcard) {
        if (this.fType == wildcard.fType) {
            if (this.fType == 1) {
                return true;
            }
            if (this.fType == 2) {
                return this.fNamespaceList[0] == wildcard.fNamespaceList[0];
            }
            if (this.fNamespaceList.length == wildcard.fNamespaceList.length) {
                for (int i = 0; i < this.fNamespaceList.length; i++) {
                    if (!elementInSet(this.fNamespaceList[i], wildcard.fNamespaceList)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    String[] intersect2sets(String[] one, String[] theOther) {
        String[] result = new String[Math.min(one.length, theOther.length)];
        int count = 0;
        for (int i = 0; i < one.length; i++) {
            if (elementInSet(one[i], theOther)) {
                result[count] = one[i];
                count++;
            }
        }
        String[] result2 = new String[count];
        System.arraycopy(result, 0, result2, 0, count);
        return result2;
    }

    String[] union2sets(String[] one, String[] theOther) {
        String[] result1 = new String[one.length];
        int count = 0;
        for (int i = 0; i < one.length; i++) {
            if (!elementInSet(one[i], theOther)) {
                result1[count] = one[i];
                count++;
            }
        }
        int i2 = theOther.length;
        String[] result2 = new String[i2 + count];
        System.arraycopy(result1, 0, result2, 0, count);
        System.arraycopy(theOther, 0, result2, count, theOther.length);
        return result2;
    }

    boolean subset2sets(String[] subSet, String[] superSet) {
        for (String str : subSet) {
            if (!elementInSet(str, superSet)) {
                return false;
            }
        }
        return true;
    }

    boolean elementInSet(String ele, String[] set) {
        boolean found = false;
        for (int i = 0; i < set.length && !found; i++) {
            if (ele == set[i]) {
                found = true;
            }
        }
        return found;
    }

    public String toString() {
        if (this.fDescription == null) {
            StringBuffer buffer = new StringBuffer();
            buffer.append("WC[");
            switch (this.fType) {
                case 1:
                    buffer.append(SchemaSymbols.ATTVAL_TWOPOUNDANY);
                    break;
                case 2:
                    buffer.append(SchemaSymbols.ATTVAL_TWOPOUNDOTHER);
                    buffer.append(":\"");
                    if (this.fNamespaceList[0] != null) {
                        buffer.append(this.fNamespaceList[0]);
                    }
                    buffer.append("\"");
                    break;
                case 3:
                    if (this.fNamespaceList.length != 0) {
                        buffer.append("\"");
                        if (this.fNamespaceList[0] != null) {
                            buffer.append(this.fNamespaceList[0]);
                        }
                        buffer.append("\"");
                        for (int i = 1; i < this.fNamespaceList.length; i++) {
                            buffer.append(",\"");
                            if (this.fNamespaceList[i] != null) {
                                buffer.append(this.fNamespaceList[i]);
                            }
                            buffer.append("\"");
                        }
                    }
                    break;
            }
            buffer.append(']');
            this.fDescription = buffer.toString();
        }
        return this.fDescription;
    }

    @Override
    public short getType() {
        return (short) 9;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public short getConstraintType() {
        return this.fType;
    }

    @Override
    public StringList getNsConstraintList() {
        return new StringListImpl(this.fNamespaceList, this.fNamespaceList == null ? 0 : this.fNamespaceList.length);
    }

    @Override
    public short getProcessContents() {
        return this.fProcessContents;
    }

    public String getProcessContentsAsString() {
        switch (this.fProcessContents) {
            case 1:
                return SchemaSymbols.ATTVAL_STRICT;
            case 2:
                return SchemaSymbols.ATTVAL_SKIP;
            case 3:
                return SchemaSymbols.ATTVAL_LAX;
            default:
                return "invalid value";
        }
    }

    @Override
    public XSAnnotation getAnnotation() {
        if (this.fAnnotations != null) {
            return (XSAnnotation) this.fAnnotations.item(0);
        }
        return null;
    }

    @Override
    public XSObjectList getAnnotations() {
        return this.fAnnotations != null ? this.fAnnotations : XSObjectListImpl.EMPTY_LIST;
    }

    @Override
    public XSNamespaceItem getNamespaceItem() {
        return null;
    }
}
