package org.apache.http.impl.cookie;

import java.util.Date;
import org.apache.http.cookie.SetCookie2;

@Deprecated
public class BasicClientCookie2 extends BasicClientCookie implements SetCookie2 {
    private String commentURL;
    private boolean discard;
    private int[] ports;

    public BasicClientCookie2(String name, String value) {
        super(name, value);
    }

    @Override
    public int[] getPorts() {
        return this.ports;
    }

    @Override
    public void setPorts(int[] ports) {
        this.ports = ports;
    }

    @Override
    public String getCommentURL() {
        return this.commentURL;
    }

    @Override
    public void setCommentURL(String commentURL) {
        this.commentURL = commentURL;
    }

    @Override
    public void setDiscard(boolean discard) {
        this.discard = discard;
    }

    @Override
    public boolean isPersistent() {
        if (this.discard) {
            return false;
        }
        return super.isPersistent();
    }

    @Override
    public boolean isExpired(Date date) {
        if (this.discard) {
            return true;
        }
        return super.isExpired(date);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        BasicClientCookie2 clone = (BasicClientCookie2) super.clone();
        clone.ports = (int[]) this.ports.clone();
        return clone;
    }
}
