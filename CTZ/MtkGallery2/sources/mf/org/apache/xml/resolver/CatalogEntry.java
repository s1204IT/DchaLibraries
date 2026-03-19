package mf.org.apache.xml.resolver;

import java.util.Hashtable;
import java.util.Vector;

public class CatalogEntry {
    protected static int nextEntry = 0;
    protected static Hashtable entryTypes = new Hashtable();
    protected static Vector entryArgs = new Vector();
    protected int entryType = 0;
    protected Vector args = null;

    public static int addEntryType(String name, int numArgs) {
        entryTypes.put(name, new Integer(nextEntry));
        entryArgs.add(nextEntry, new Integer(numArgs));
        nextEntry++;
        return nextEntry - 1;
    }

    public int getEntryType() {
        return this.entryType;
    }

    public String getEntryArg(int argNum) {
        try {
            String arg = (String) this.args.get(argNum);
            return arg;
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }
}
