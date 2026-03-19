package gov.nist.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.NoSuchElementException;

public abstract class GenericObjectList extends LinkedList<GenericObject> implements Serializable, Cloneable {
    protected static final String AND = "&";
    protected static final String AT = "@";
    protected static final String COLON = ":";
    protected static final String COMMA = ",";
    protected static final String DOT = ".";
    protected static final String DOUBLE_QUOTE = "\"";
    protected static final String EQUALS = "=";
    protected static final String GREATER_THAN = ">";
    protected static final String HT = "\t";
    protected static final String LESS_THAN = "<";
    protected static final String LPAREN = "(";
    protected static final String NEWLINE = "\r\n";
    protected static final String PERCENT = "%";
    protected static final String POUND = "#";
    protected static final String QUESTION = "?";
    protected static final String QUOTE = "'";
    protected static final String RETURN = "\n";
    protected static final String RPAREN = ")";
    protected static final String SEMICOLON = ";";
    protected static final String SLASH = "/";
    protected static final String SP = " ";
    protected static final String STAR = "*";
    protected int indentation;
    protected String listName;
    protected Class<?> myClass;
    private ListIterator<? extends GenericObject> myListIterator;
    protected String separator;
    private String stringRep;

    protected String getIndentation() {
        char[] chars = new char[this.indentation];
        Arrays.fill(chars, ' ');
        return new String(chars);
    }

    protected static boolean isCloneable(Object obj) {
        return obj instanceof Cloneable;
    }

    public static boolean isMySubclass(Class<?> other) {
        return GenericObjectList.class.isAssignableFrom(other);
    }

    @Override
    public Object clone() {
        GenericObjectList retval = (GenericObjectList) super.clone();
        ListIterator<GenericObject> iter = retval.listIterator();
        while (iter.hasNext()) {
            GenericObject obj = (GenericObject) iter.next().clone();
            iter.set(obj);
        }
        return retval;
    }

    public void setMyClass(Class cl) {
        this.myClass = cl;
    }

    protected GenericObjectList() {
        this.listName = null;
        this.stringRep = "";
        this.separator = ";";
    }

    protected GenericObjectList(String lname) {
        this();
        this.listName = lname;
    }

    protected GenericObjectList(String lname, String classname) {
        this(lname);
        try {
            this.myClass = Class.forName(classname);
        } catch (ClassNotFoundException ex) {
            InternalErrorHandler.handleException(ex);
        }
    }

    protected GenericObjectList(String lname, Class objclass) {
        this(lname);
        this.myClass = objclass;
    }

    protected GenericObject next(ListIterator iterator) {
        try {
            return (GenericObject) iterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    protected GenericObject first() {
        this.myListIterator = listIterator(0);
        try {
            return this.myListIterator.next();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    protected GenericObject next() {
        if (this.myListIterator == null) {
            this.myListIterator = listIterator(0);
        }
        try {
            return this.myListIterator.next();
        } catch (NoSuchElementException e) {
            this.myListIterator = null;
            return null;
        }
    }

    protected void concatenate(GenericObjectList objList) {
        concatenate(objList, false);
    }

    protected void concatenate(GenericObjectList objList, boolean topFlag) {
        if (!topFlag) {
            addAll(objList);
        } else {
            addAll(0, objList);
        }
    }

    private void sprint(String s) {
        if (s == null) {
            this.stringRep += getIndentation();
            this.stringRep += "<null>\n";
            return;
        }
        if (s.compareTo("}") == 0 || s.compareTo("]") == 0) {
            this.indentation--;
        }
        this.stringRep += getIndentation();
        this.stringRep += s;
        this.stringRep += "\n";
        if (s.compareTo("{") != 0 && s.compareTo("[") != 0) {
            return;
        }
        this.indentation++;
    }

    public String debugDump() {
        this.stringRep = "";
        GenericObject genericObjectFirst = first();
        if (genericObjectFirst == null) {
            return "<null>";
        }
        sprint("listName:");
        sprint(this.listName);
        sprint("{");
        while (genericObjectFirst != null) {
            sprint("[");
            sprint(genericObjectFirst.debugDump(this.indentation));
            genericObjectFirst = next();
            sprint("]");
        }
        sprint("}");
        return this.stringRep;
    }

    public String debugDump(int indent) {
        int save = this.indentation;
        this.indentation = indent;
        String retval = debugDump();
        this.indentation = save;
        return retval;
    }

    @Override
    public void addFirst(GenericObject objToAdd) {
        if (this.myClass == null) {
            this.myClass = objToAdd.getClass();
        } else {
            super.addFirst(objToAdd);
        }
    }

    public void mergeObjects(GenericObjectList mergeList) {
        if (mergeList == null) {
            return;
        }
        Iterator it1 = listIterator();
        Iterator it2 = mergeList.listIterator();
        while (it1.hasNext()) {
            GenericObject outerObj = it1.next();
            while (it2.hasNext()) {
                Object innerObj = it2.next();
                outerObj.merge(innerObj);
            }
        }
    }

    public String encode() {
        if (isEmpty()) {
            return "";
        }
        StringBuffer encoding = new StringBuffer();
        ListIterator<GenericObject> listIterator = listIterator();
        if (listIterator.hasNext()) {
            while (true) {
                GenericObject next = listIterator.next();
                if (next instanceof GenericObject) {
                    encoding.append(next.encode());
                } else {
                    encoding.append(next.toString());
                }
                if (!listIterator.hasNext()) {
                    break;
                }
                encoding.append(this.separator);
            }
        }
        return encoding.toString();
    }

    @Override
    public String toString() {
        return encode();
    }

    public void setSeparator(String sep) {
        this.separator = sep;
    }

    @Override
    public int hashCode() {
        return 42;
    }

    @Override
    public boolean equals(Object other) {
        Object myobj;
        Object hisobj;
        if (other == null || !getClass().equals(other.getClass())) {
            return false;
        }
        GenericObjectList that = (GenericObjectList) other;
        if (size() != that.size()) {
            return false;
        }
        ListIterator<GenericObject> listIterator = listIterator();
        while (listIterator.hasNext()) {
            Object myobj2 = listIterator.next();
            ListIterator<GenericObject> listIterator2 = that.listIterator();
            do {
                try {
                    hisobj = listIterator2.next();
                } catch (NoSuchElementException e) {
                    return false;
                }
            } while (!myobj2.equals(hisobj));
        }
        ListIterator<GenericObject> listIterator3 = that.listIterator();
        while (listIterator3.hasNext()) {
            Object hisobj2 = listIterator3.next();
            ListIterator<GenericObject> listIterator4 = listIterator();
            do {
                try {
                    myobj = listIterator4.next();
                } catch (NoSuchElementException e2) {
                    return false;
                }
            } while (!hisobj2.equals(myobj));
        }
        return true;
    }

    public boolean match(Object other) {
        if (!getClass().equals(other.getClass())) {
            return false;
        }
        GenericObjectList that = (GenericObjectList) other;
        ListIterator<GenericObject> listIterator = that.listIterator();
        if (listIterator.hasNext()) {
            Object hisobj = listIterator.next();
            ListIterator<GenericObject> listIterator2 = listIterator();
            while (listIterator2.hasNext()) {
                GenericObject next = listIterator2.next();
                if (next instanceof GenericObject) {
                    System.out.println("Trying to match  = " + next.encode());
                }
                if (!GenericObject.isMySubclass(next.getClass()) || !next.match(hisobj)) {
                    if (isMySubclass(next.getClass()) && ((GenericObjectList) next).match(hisobj)) {
                        return true;
                    }
                } else {
                    return true;
                }
            }
            System.out.println(((GenericObject) hisobj).encode());
            return false;
        }
        return true;
    }
}
