package gov.nist.javax.sip.address;

import gov.nist.core.Host;
import gov.nist.core.HostPort;
import gov.nist.core.Separators;

public class Authority extends NetObject {
    private static final long serialVersionUID = -3570349777347017894L;
    protected HostPort hostPort;
    protected UserInfo userInfo;

    @Override
    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    @Override
    public StringBuffer encode(StringBuffer stringBuffer) {
        if (this.userInfo != null) {
            this.userInfo.encode(stringBuffer);
            stringBuffer.append(Separators.AT);
            this.hostPort.encode(stringBuffer);
        } else {
            this.hostPort.encode(stringBuffer);
        }
        return stringBuffer;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }
        Authority authority = (Authority) obj;
        if (!this.hostPort.equals(authority.hostPort)) {
            return false;
        }
        if (this.userInfo != null && authority.userInfo != null && !this.userInfo.equals(authority.userInfo)) {
            return false;
        }
        return true;
    }

    public HostPort getHostPort() {
        return this.hostPort;
    }

    public UserInfo getUserInfo() {
        return this.userInfo;
    }

    public String getPassword() {
        if (this.userInfo == null) {
            return null;
        }
        return this.userInfo.password;
    }

    public String getUser() {
        if (this.userInfo != null) {
            return this.userInfo.user;
        }
        return null;
    }

    public Host getHost() {
        if (this.hostPort == null) {
            return null;
        }
        return this.hostPort.getHost();
    }

    public int getPort() {
        if (this.hostPort == null) {
            return -1;
        }
        return this.hostPort.getPort();
    }

    public void removePort() {
        if (this.hostPort != null) {
            this.hostPort.removePort();
        }
    }

    public void setPassword(String str) {
        if (this.userInfo == null) {
            this.userInfo = new UserInfo();
        }
        this.userInfo.setPassword(str);
    }

    public void setUser(String str) {
        if (this.userInfo == null) {
            this.userInfo = new UserInfo();
        }
        this.userInfo.setUser(str);
    }

    public void setHost(Host host) {
        if (this.hostPort == null) {
            this.hostPort = new HostPort();
        }
        this.hostPort.setHost(host);
    }

    public void setPort(int i) {
        if (this.hostPort == null) {
            this.hostPort = new HostPort();
        }
        this.hostPort.setPort(i);
    }

    public void setHostPort(HostPort hostPort) {
        this.hostPort = hostPort;
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public void removeUserInfo() {
        this.userInfo = null;
    }

    @Override
    public Object clone() {
        Authority authority = (Authority) super.clone();
        if (this.hostPort != null) {
            authority.hostPort = (HostPort) this.hostPort.clone();
        }
        if (this.userInfo != null) {
            authority.userInfo = (UserInfo) this.userInfo.clone();
        }
        return authority;
    }

    public int hashCode() {
        if (this.hostPort == null) {
            throw new UnsupportedOperationException("Null hostPort cannot compute hashcode");
        }
        return this.hostPort.encode().hashCode();
    }
}
