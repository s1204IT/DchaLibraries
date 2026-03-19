package gov.nist.javax.sip.header;

import java.util.Arrays;

class Indentation {
    private int indentation;

    protected Indentation() {
        this.indentation = 0;
    }

    protected Indentation(int i) {
        this.indentation = i;
    }

    protected void setIndentation(int i) {
        this.indentation = i;
    }

    protected int getCount() {
        return this.indentation;
    }

    protected void increment() {
        this.indentation++;
    }

    protected void decrement() {
        this.indentation--;
    }

    protected String getIndentation() {
        char[] cArr = new char[this.indentation];
        Arrays.fill(cArr, ' ');
        return new String(cArr);
    }
}
