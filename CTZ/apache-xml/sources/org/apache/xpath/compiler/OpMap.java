package org.apache.xpath.compiler;

import javax.xml.transform.TransformerException;
import org.apache.xalan.res.XSLMessages;
import org.apache.xml.utils.ObjectVector;
import org.apache.xpath.res.XPATHErrorResources;

public class OpMap {
    static final int BLOCKTOKENQUEUESIZE = 500;
    public static final int MAPINDEX_LENGTH = 1;
    static final int MAXTOKENQUEUESIZE = 500;
    protected String m_currentPattern;
    ObjectVector m_tokenQueue = new ObjectVector(500, 500);
    OpMapVector m_opMap = null;

    public String toString() {
        return this.m_currentPattern;
    }

    public String getPatternString() {
        return this.m_currentPattern;
    }

    public ObjectVector getTokenQueue() {
        return this.m_tokenQueue;
    }

    public Object getToken(int i) {
        return this.m_tokenQueue.elementAt(i);
    }

    public int getTokenQueueSize() {
        return this.m_tokenQueue.size();
    }

    public OpMapVector getOpMap() {
        return this.m_opMap;
    }

    void shrink() {
        int iElementAt = this.m_opMap.elementAt(1);
        this.m_opMap.setToSize(iElementAt + 4);
        this.m_opMap.setElementAt(0, iElementAt);
        this.m_opMap.setElementAt(0, iElementAt + 1);
        this.m_opMap.setElementAt(0, iElementAt + 2);
        int size = this.m_tokenQueue.size();
        this.m_tokenQueue.setToSize(size + 4);
        this.m_tokenQueue.setElementAt(null, size);
        this.m_tokenQueue.setElementAt(null, size + 1);
        this.m_tokenQueue.setElementAt(null, size + 2);
    }

    public int getOp(int i) {
        return this.m_opMap.elementAt(i);
    }

    public void setOp(int i, int i2) {
        this.m_opMap.setElementAt(i2, i);
    }

    public int getNextOpPos(int i) {
        return i + this.m_opMap.elementAt(i + 1);
    }

    public int getNextStepPos(int i) {
        int op = getOp(i);
        if (op >= 37 && op <= 53) {
            return getNextOpPos(i);
        }
        if (op >= 22 && op <= 25) {
            int nextOpPos = getNextOpPos(i);
            while (29 == getOp(nextOpPos)) {
                nextOpPos = getNextOpPos(nextOpPos);
            }
            int op2 = getOp(nextOpPos);
            if (op2 < 37 || op2 > 53) {
                return -1;
            }
            return nextOpPos;
        }
        throw new RuntimeException(XSLMessages.createXPATHMessage(XPATHErrorResources.ER_UNKNOWN_STEP, new Object[]{String.valueOf(op)}));
    }

    public static int getNextOpPos(int[] iArr, int i) {
        return i + iArr[i + 1];
    }

    public int getFirstPredicateOpPos(int i) throws TransformerException {
        int iElementAt = this.m_opMap.elementAt(i);
        if (iElementAt >= 37 && iElementAt <= 53) {
            return i + this.m_opMap.elementAt(i + 2);
        }
        if (iElementAt >= 22 && iElementAt <= 25) {
            return i + this.m_opMap.elementAt(i + 1);
        }
        if (-2 == iElementAt) {
            return -2;
        }
        error(XPATHErrorResources.ER_UNKNOWN_OPCODE, new Object[]{String.valueOf(iElementAt)});
        return -1;
    }

    public void error(String str, Object[] objArr) throws TransformerException {
        throw new TransformerException(XSLMessages.createXPATHMessage(str, objArr));
    }

    public static int getFirstChildPos(int i) {
        return i + 2;
    }

    public int getArgLength(int i) {
        return this.m_opMap.elementAt(i + 1);
    }

    public int getArgLengthOfStep(int i) {
        return this.m_opMap.elementAt((i + 1) + 1) - 3;
    }

    public static int getFirstChildPosOfStep(int i) {
        return i + 3;
    }

    public int getStepTestType(int i) {
        return this.m_opMap.elementAt(i + 3);
    }

    public String getStepNS(int i) {
        if (getArgLengthOfStep(i) != 3) {
            return null;
        }
        int iElementAt = this.m_opMap.elementAt(i + 4);
        if (iElementAt >= 0) {
            return (String) this.m_tokenQueue.elementAt(iElementAt);
        }
        if (-3 != iElementAt) {
            return null;
        }
        return "*";
    }

    public String getStepLocalName(int i) {
        int iElementAt;
        switch (getArgLengthOfStep(i)) {
            case 0:
            default:
                iElementAt = -2;
                break;
            case 1:
                iElementAt = -3;
                break;
            case 2:
                iElementAt = this.m_opMap.elementAt(i + 4);
                break;
            case 3:
                iElementAt = this.m_opMap.elementAt(i + 5);
                break;
        }
        if (iElementAt >= 0) {
            return this.m_tokenQueue.elementAt(iElementAt).toString();
        }
        if (-3 == iElementAt) {
            return "*";
        }
        return null;
    }
}
