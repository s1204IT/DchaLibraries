package org.apache.xalan.templates;

import java.io.Serializable;

public class XMLNSDecl implements Serializable {
    static final long serialVersionUID = 6710237366877605097L;
    private boolean m_isExcluded;
    private String m_prefix;
    private String m_uri;

    public XMLNSDecl(String str, String str2, boolean z) {
        this.m_prefix = str;
        this.m_uri = str2;
        this.m_isExcluded = z;
    }

    public String getPrefix() {
        return this.m_prefix;
    }

    public String getURI() {
        return this.m_uri;
    }

    public boolean getIsExcluded() {
        return this.m_isExcluded;
    }
}
