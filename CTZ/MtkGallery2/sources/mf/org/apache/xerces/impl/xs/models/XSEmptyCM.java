package mf.org.apache.xerces.impl.xs.models;

import java.util.Vector;
import mf.org.apache.xerces.impl.xs.SubstitutionGroupHandler;
import mf.org.apache.xerces.impl.xs.XMLSchemaException;
import mf.org.apache.xerces.xni.QName;

public class XSEmptyCM implements XSCMValidator {
    private static final Vector EMPTY = new Vector(0);
    private static final short STATE_START = 0;

    @Override
    public int[] startContentModel() {
        return new int[1];
    }

    @Override
    public Object oneTransition(QName elementName, int[] currentState, SubstitutionGroupHandler subGroupHandler) {
        if (currentState[0] < 0) {
            currentState[0] = -2;
            return null;
        }
        currentState[0] = -1;
        return null;
    }

    @Override
    public boolean endContentModel(int[] currentState) {
        int state = currentState[0];
        if (state < 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean checkUniqueParticleAttribution(SubstitutionGroupHandler subGroupHandler) throws XMLSchemaException {
        return false;
    }

    @Override
    public Vector whatCanGoHere(int[] state) {
        return EMPTY;
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
