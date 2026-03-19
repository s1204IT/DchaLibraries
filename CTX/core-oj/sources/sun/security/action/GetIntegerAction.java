package sun.security.action;

import java.security.PrivilegedAction;

public class GetIntegerAction implements PrivilegedAction<Integer> {
    private boolean defaultSet;
    private int defaultVal;
    private String theProp;

    public GetIntegerAction(String str) {
        this.defaultSet = false;
        this.theProp = str;
    }

    public GetIntegerAction(String str, int i) {
        this.defaultSet = false;
        this.theProp = str;
        this.defaultVal = i;
        this.defaultSet = true;
    }

    @Override
    public Integer run() {
        Integer integer = Integer.getInteger(this.theProp);
        if (integer == null && this.defaultSet) {
            return new Integer(this.defaultVal);
        }
        return integer;
    }
}
