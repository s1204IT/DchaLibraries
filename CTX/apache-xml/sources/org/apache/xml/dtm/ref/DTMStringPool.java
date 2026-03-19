package org.apache.xml.dtm.ref;

import java.util.Vector;
import org.apache.xml.utils.IntVector;

public class DTMStringPool {
    static final int HASHPRIME = 101;
    public static final int NULL = -1;
    IntVector m_hashChain;
    int[] m_hashStart;
    Vector m_intToString;

    public DTMStringPool(int i) {
        this.m_hashStart = new int[HASHPRIME];
        this.m_intToString = new Vector();
        this.m_hashChain = new IntVector(i);
        removeAllElements();
        stringToIndex("");
    }

    public DTMStringPool() {
        this(512);
    }

    public void removeAllElements() {
        this.m_intToString.removeAllElements();
        for (int i = 0; i < HASHPRIME; i++) {
            this.m_hashStart[i] = -1;
        }
        this.m_hashChain.removeAllElements();
    }

    public String indexToString(int i) throws ArrayIndexOutOfBoundsException {
        if (i == -1) {
            return null;
        }
        return (String) this.m_intToString.elementAt(i);
    }

    public int stringToIndex(String str) {
        if (str == null) {
            return -1;
        }
        int iHashCode = str.hashCode() % HASHPRIME;
        if (iHashCode < 0) {
            iHashCode = -iHashCode;
        }
        int iElementAt = this.m_hashStart[iHashCode];
        int i = iElementAt;
        while (iElementAt != -1) {
            if (this.m_intToString.elementAt(iElementAt).equals(str)) {
                return iElementAt;
            }
            i = iElementAt;
            iElementAt = this.m_hashChain.elementAt(iElementAt);
        }
        int size = this.m_intToString.size();
        this.m_intToString.addElement(str);
        this.m_hashChain.addElement(-1);
        if (i == -1) {
            this.m_hashStart[iHashCode] = size;
        } else {
            this.m_hashChain.setElementAt(size, i);
        }
        return size;
    }

    public static void main(String[] strArr) {
        String[] strArr2 = {"Zero", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen", "Twenty", "Twenty-One", "Twenty-Two", "Twenty-Three", "Twenty-Four", "Twenty-Five", "Twenty-Six", "Twenty-Seven", "Twenty-Eight", "Twenty-Nine", "Thirty", "Thirty-One", "Thirty-Two", "Thirty-Three", "Thirty-Four", "Thirty-Five", "Thirty-Six", "Thirty-Seven", "Thirty-Eight", "Thirty-Nine"};
        DTMStringPool dTMStringPool = new DTMStringPool();
        System.out.println("If no complaints are printed below, we passed initial test.");
        for (int i = 0; i <= 1; i++) {
            for (int i2 = 0; i2 < strArr2.length; i2++) {
                int iStringToIndex = dTMStringPool.stringToIndex(strArr2[i2]);
                if (iStringToIndex != i2) {
                    System.out.println("\tMismatch populating pool: assigned " + iStringToIndex + " for create " + i2);
                }
            }
            for (int i3 = 0; i3 < strArr2.length; i3++) {
                int iStringToIndex2 = dTMStringPool.stringToIndex(strArr2[i3]);
                if (iStringToIndex2 != i3) {
                    System.out.println("\tMismatch in stringToIndex: returned " + iStringToIndex2 + " for lookup " + i3);
                }
            }
            for (int i4 = 0; i4 < strArr2.length; i4++) {
                String strIndexToString = dTMStringPool.indexToString(i4);
                if (!strArr2[i4].equals(strIndexToString)) {
                    System.out.println("\tMismatch in indexToString: returned" + strIndexToString + " for lookup " + i4);
                }
            }
            dTMStringPool.removeAllElements();
            System.out.println("\nPass " + i + " complete\n");
        }
    }
}
