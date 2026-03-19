package gov.nist.javax.sip.address;

import gov.nist.core.Separators;

public final class UserInfo extends NetObject {
    public static final int TELEPHONE_SUBSCRIBER = 1;
    public static final int USER = 2;
    private static final long serialVersionUID = 7268593273924256144L;
    protected String password;
    protected String user;
    protected int userType;

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) {
            return false;
        }
        UserInfo userInfo = (UserInfo) obj;
        if (this.userType != userInfo.userType || !this.user.equalsIgnoreCase(userInfo.user)) {
            return false;
        }
        if (this.password != null && userInfo.password == null) {
            return false;
        }
        if (userInfo.password != null && this.password == null) {
            return false;
        }
        if (this.password == userInfo.password) {
            return true;
        }
        return this.password.equals(userInfo.password);
    }

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        if (this.password != null) {
            stringBuffer.append(this.user);
            stringBuffer.append(Separators.COLON);
            stringBuffer.append(this.password);
        } else {
            stringBuffer.append(this.user);
        }
        return stringBuffer;
    }

    public void clearPassword() {
        this.password = null;
    }

    public int getUserType() {
        return this.userType;
    }

    public String getUser() {
        return this.user;
    }

    public String getPassword() {
        return this.password;
    }

    public void setUser(String str) {
        this.user = str;
        if (str != null && (str.indexOf(Separators.POUND) >= 0 || str.indexOf(Separators.SEMICOLON) >= 0)) {
            setUserType(1);
        } else {
            setUserType(2);
        }
    }

    public void setPassword(String str) {
        this.password = str;
    }

    public void setUserType(int i) throws IllegalArgumentException {
        if (i != 1 && i != 2) {
            throw new IllegalArgumentException("Parameter not in range");
        }
        this.userType = i;
    }
}
