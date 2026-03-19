package sun.misc;

import java.io.PrintStream;

public class RegexpPool {
    private static final int BIG = Integer.MAX_VALUE;
    private RegexpNode prefixMachine = new RegexpNode();
    private RegexpNode suffixMachine = new RegexpNode();
    private int lastDepth = Integer.MAX_VALUE;

    public void add(String str, Object obj) throws REException {
        add(str, obj, false);
    }

    public void replace(String str, Object obj) {
        try {
            add(str, obj, true);
        } catch (Exception e) {
        }
    }

    public Object delete(String str) {
        RegexpNode regexpNodeFind = this.prefixMachine;
        boolean z = true;
        int length = str.length() - 1;
        if (!str.startsWith("*") || !str.endsWith("*")) {
            length++;
        }
        if (length <= 0) {
            return null;
        }
        RegexpNode regexpNode = regexpNodeFind;
        int i = 0;
        while (regexpNodeFind != null) {
            if (regexpNodeFind.result != null && regexpNodeFind.depth < Integer.MAX_VALUE && (!regexpNodeFind.exact || i == length)) {
                regexpNode = regexpNodeFind;
            }
            if (i >= length) {
                break;
            }
            regexpNodeFind = regexpNodeFind.find(str.charAt(i));
            i++;
        }
        RegexpNode regexpNodeFind2 = this.suffixMachine;
        while (true) {
            length--;
            if (length < 0 || regexpNodeFind2 == null) {
                break;
            }
            if (regexpNodeFind2.result != null && regexpNodeFind2.depth < Integer.MAX_VALUE) {
                regexpNode = regexpNodeFind2;
                z = false;
            }
            regexpNodeFind2 = regexpNodeFind2.find(str.charAt(length));
        }
        if (z) {
            if (str.equals(regexpNode.re)) {
                Object obj = regexpNode.result;
                regexpNode.result = null;
                return obj;
            }
        } else if (str.equals(regexpNode.re)) {
            Object obj2 = regexpNode.result;
            regexpNode.result = null;
            return obj2;
        }
        return null;
    }

    public Object match(String str) {
        return matchAfter(str, Integer.MAX_VALUE);
    }

    public Object matchNext(String str) {
        return matchAfter(str, this.lastDepth);
    }

    private void add(String str, Object obj, boolean z) throws REException {
        RegexpNode regexpNodeAdd;
        int length = str.length();
        boolean z2 = true;
        if (str.charAt(0) == '*') {
            regexpNodeAdd = this.suffixMachine;
            while (length > 1) {
                length--;
                regexpNodeAdd = regexpNodeAdd.add(str.charAt(length));
            }
        } else {
            if (str.charAt(length - 1) == '*') {
                length--;
                z2 = false;
            }
            RegexpNode regexpNodeAdd2 = this.prefixMachine;
            for (int i = 0; i < length; i++) {
                regexpNodeAdd2 = regexpNodeAdd2.add(str.charAt(i));
            }
            regexpNodeAdd2.exact = z2;
            regexpNodeAdd = regexpNodeAdd2;
        }
        if (regexpNodeAdd.result != null && !z) {
            throw new REException(str + " is a duplicate");
        }
        regexpNodeAdd.re = str;
        regexpNodeAdd.result = obj;
    }

    private Object matchAfter(String str, int i) {
        RegexpNode regexpNodeFind = this.prefixMachine;
        int length = str.length();
        if (length <= 0) {
            return null;
        }
        RegexpNode regexpNode = regexpNodeFind;
        int i2 = 0;
        int i3 = 0;
        int i4 = 0;
        while (regexpNodeFind != null) {
            if (regexpNodeFind.result != null && regexpNodeFind.depth < i && (!regexpNodeFind.exact || i2 == length)) {
                this.lastDepth = regexpNodeFind.depth;
                regexpNode = regexpNodeFind;
                i4 = length;
                i3 = i2;
            }
            if (i2 >= length) {
                break;
            }
            regexpNodeFind = regexpNodeFind.find(str.charAt(i2));
            i2++;
        }
        RegexpNode regexpNodeFind2 = this.suffixMachine;
        while (true) {
            length--;
            if (length < 0 || regexpNodeFind2 == null) {
                break;
            }
            if (regexpNodeFind2.result != null && regexpNodeFind2.depth < i) {
                this.lastDepth = regexpNodeFind2.depth;
                regexpNode = regexpNodeFind2;
                i3 = 0;
                i4 = length + 1;
            }
            regexpNodeFind2 = regexpNodeFind2.find(str.charAt(length));
        }
        Object obj = regexpNode.result;
        if (obj != null && (obj instanceof RegexpTarget)) {
            return ((RegexpTarget) obj).found(str.substring(i3, i4));
        }
        return obj;
    }

    public void reset() {
        this.lastDepth = Integer.MAX_VALUE;
    }

    public void print(PrintStream printStream) {
        printStream.print("Regexp pool:\n");
        if (this.suffixMachine.firstchild != null) {
            printStream.print(" Suffix machine: ");
            this.suffixMachine.firstchild.print(printStream);
            printStream.print("\n");
        }
        if (this.prefixMachine.firstchild != null) {
            printStream.print(" Prefix machine: ");
            this.prefixMachine.firstchild.print(printStream);
            printStream.print("\n");
        }
    }
}
