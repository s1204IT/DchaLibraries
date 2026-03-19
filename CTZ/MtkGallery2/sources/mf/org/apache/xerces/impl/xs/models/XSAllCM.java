package mf.org.apache.xerces.impl.xs.models;

import java.util.Vector;
import mf.org.apache.xerces.impl.xs.SubstitutionGroupHandler;
import mf.org.apache.xerces.impl.xs.XMLSchemaException;
import mf.org.apache.xerces.impl.xs.XSConstraints;
import mf.org.apache.xerces.impl.xs.XSElementDecl;
import mf.org.apache.xerces.xni.QName;

public class XSAllCM implements XSCMValidator {
    private static final short STATE_CHILD = 1;
    private static final short STATE_START = 0;
    private static final short STATE_VALID = 1;
    private final XSElementDecl[] fAllElements;
    private final boolean fHasOptionalContent;
    private final boolean[] fIsOptionalElement;
    private int fNumElements = 0;

    public XSAllCM(boolean hasOptionalContent, int size) {
        this.fHasOptionalContent = hasOptionalContent;
        this.fAllElements = new XSElementDecl[size];
        this.fIsOptionalElement = new boolean[size];
    }

    public void addElement(XSElementDecl element, boolean isOptional) {
        this.fAllElements[this.fNumElements] = element;
        this.fIsOptionalElement[this.fNumElements] = isOptional;
        this.fNumElements++;
    }

    @Override
    public int[] startContentModel() {
        int[] state = new int[this.fNumElements + 1];
        for (int i = 0; i <= this.fNumElements; i++) {
            state[i] = 0;
        }
        return state;
    }

    Object findMatchingDecl(QName elementName, SubstitutionGroupHandler subGroupHandler) {
        Object matchingDecl = null;
        for (int i = 0; i < this.fNumElements && (matchingDecl = subGroupHandler.getMatchingElemDecl(elementName, this.fAllElements[i])) == null; i++) {
        }
        return matchingDecl;
    }

    @Override
    public Object oneTransition(QName elementName, int[] currentState, SubstitutionGroupHandler subGroupHandler) {
        Object matchingDecl;
        if (currentState[0] < 0) {
            currentState[0] = -2;
            return findMatchingDecl(elementName, subGroupHandler);
        }
        currentState[0] = 1;
        for (int i = 0; i < this.fNumElements; i++) {
            if (currentState[i + 1] == 0 && (matchingDecl = subGroupHandler.getMatchingElemDecl(elementName, this.fAllElements[i])) != null) {
                currentState[i + 1] = 1;
                return matchingDecl;
            }
        }
        currentState[0] = -1;
        return findMatchingDecl(elementName, subGroupHandler);
    }

    @Override
    public boolean endContentModel(int[] currentState) {
        int state = currentState[0];
        if (state == -1 || state == -2) {
            return false;
        }
        if (this.fHasOptionalContent && state == 0) {
            return true;
        }
        for (int i = 0; i < this.fNumElements; i++) {
            if (!this.fIsOptionalElement[i] && currentState[i + 1] == 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean checkUniqueParticleAttribution(SubstitutionGroupHandler subGroupHandler) throws XMLSchemaException {
        for (int i = 0; i < this.fNumElements; i++) {
            for (int j = i + 1; j < this.fNumElements; j++) {
                if (XSConstraints.overlapUPA(this.fAllElements[i], this.fAllElements[j], subGroupHandler)) {
                    throw new XMLSchemaException("cos-nonambig", new Object[]{this.fAllElements[i].toString(), this.fAllElements[j].toString()});
                }
            }
        }
        return false;
    }

    @Override
    public Vector whatCanGoHere(int[] state) {
        Vector ret = new Vector();
        for (int i = 0; i < this.fNumElements; i++) {
            if (state[i + 1] == 0) {
                ret.addElement(this.fAllElements[i]);
            }
        }
        return ret;
    }

    @Override
    public int[] occurenceInfo(int[] state) {
        return null;
    }

    @Override
    public String getTermName(int termId) {
        return null;
    }

    @Override
    public boolean isCompactedForUPA() {
        return false;
    }
}
