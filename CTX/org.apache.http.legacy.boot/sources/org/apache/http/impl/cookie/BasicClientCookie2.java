package org.apache.http.impl.cookie;

import java.util.Date;
import org.apache.http.cookie.SetCookie2;

@Deprecated
public class BasicClientCookie2 extends BasicClientCookie implements SetCookie2 {
    private String commentURL;
    private boolean discard;
    private int[] ports;

    public BasicClientCookie2(String str, String str2) {
        super(str, str2);
    }

    @Override
    public int[] getPorts() {
        return this.ports;
    }

    @Override
    public void setPorts(int[] iArr) {
        this.ports = iArr;
    }

    @Override
    public String getCommentURL() {
        return this.commentURL;
    }

    @Override
    public void setCommentURL(String str) {
        this.commentURL = str;
    }

    @Override
    public void setDiscard(boolean z) {
        this.discard = z;
    }

    @Override
    public boolean isPersistent() {
        return !this.discard && super.isPersistent();
    }

    @Override
    public boolean isExpired(Date date) {
        return this.discard || super.isExpired(date);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        BasicClientCookie2 basicClientCookie2 = (BasicClientCookie2) super.clone();
        basicClientCookie2.ports = (int[]) this.ports.clone();
        return basicClientCookie2;
    }
}
