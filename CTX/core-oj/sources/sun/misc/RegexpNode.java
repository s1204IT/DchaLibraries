package sun.misc;

import java.io.PrintStream;

class RegexpNode {
    char c;
    int depth;
    boolean exact;
    RegexpNode firstchild;
    RegexpNode nextsibling;
    String re;
    Object result;

    RegexpNode() {
        this.re = null;
        this.c = '#';
        this.depth = 0;
    }

    RegexpNode(char c, int i) {
        this.re = null;
        this.c = c;
        this.depth = i;
    }

    RegexpNode add(char c) {
        RegexpNode regexpNode;
        RegexpNode regexpNode2 = this.firstchild;
        if (regexpNode2 == null) {
            regexpNode = new RegexpNode(c, this.depth + 1);
        } else {
            while (regexpNode2 != null) {
                if (regexpNode2.c == c) {
                    return regexpNode2;
                }
                regexpNode2 = regexpNode2.nextsibling;
            }
            regexpNode = new RegexpNode(c, this.depth + 1);
            regexpNode.nextsibling = this.firstchild;
        }
        this.firstchild = regexpNode;
        return regexpNode;
    }

    RegexpNode find(char c) {
        for (RegexpNode regexpNode = this.firstchild; regexpNode != null; regexpNode = regexpNode.nextsibling) {
            if (regexpNode.c == c) {
                return regexpNode;
            }
        }
        return null;
    }

    void print(PrintStream printStream) {
        if (this.nextsibling != null) {
            printStream.print("(");
            RegexpNode regexpNode = this;
            while (regexpNode != null) {
                printStream.write(regexpNode.c);
                if (regexpNode.firstchild != null) {
                    regexpNode.firstchild.print(printStream);
                }
                regexpNode = regexpNode.nextsibling;
                printStream.write(regexpNode != null ? 124 : 41);
            }
            return;
        }
        printStream.write(this.c);
        if (this.firstchild != null) {
            this.firstchild.print(printStream);
        }
    }
}
