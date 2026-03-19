package sun.security.action;

import java.security.PrivilegedAction;

public class GetBooleanAction implements PrivilegedAction<Boolean> {
    private String theProp;

    public GetBooleanAction(String str) {
        this.theProp = str;
    }

    @Override
    public Boolean run() {
        return Boolean.valueOf(Boolean.getBoolean(this.theProp));
    }
}
