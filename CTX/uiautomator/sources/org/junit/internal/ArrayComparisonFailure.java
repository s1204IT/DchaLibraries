package org.junit.internal;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrayComparisonFailure extends AssertionError {
    private static final long serialVersionUID = 1;
    private final List<Integer> fIndices = new ArrayList();
    private final String fMessage;

    public ArrayComparisonFailure(String str, AssertionError assertionError, int i) {
        this.fMessage = str;
        initCause(assertionError);
        addDimension(i);
    }

    public void addDimension(int i) {
        this.fIndices.add(0, Integer.valueOf(i));
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (this.fMessage != null) {
            sb.append(this.fMessage);
        }
        sb.append("arrays first differed at element ");
        Iterator<Integer> it = this.fIndices.iterator();
        while (it.hasNext()) {
            int iIntValue = it.next().intValue();
            sb.append("[");
            sb.append(iIntValue);
            sb.append("]");
        }
        sb.append("; ");
        sb.append(getCause().getMessage());
        return sb.toString();
    }

    @Override
    public String toString() {
        return getMessage();
    }
}
