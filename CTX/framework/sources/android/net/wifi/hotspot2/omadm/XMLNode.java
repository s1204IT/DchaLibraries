package android.net.wifi.hotspot2.omadm;

import android.text.TextUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class XMLNode {
    private final XMLNode mParent;
    private final String mTag;
    private final List<XMLNode> mChildren = new ArrayList();
    private StringBuilder mTextBuilder = new StringBuilder();
    private String mText = null;

    public XMLNode(XMLNode xMLNode, String str) {
        this.mTag = str;
        this.mParent = xMLNode;
    }

    public void addText(String str) {
        this.mTextBuilder.append(str);
    }

    public void addChild(XMLNode xMLNode) {
        this.mChildren.add(xMLNode);
    }

    public void close() {
        this.mText = this.mTextBuilder.toString().trim();
        this.mTextBuilder = null;
    }

    public String getTag() {
        return this.mTag;
    }

    public XMLNode getParent() {
        return this.mParent;
    }

    public String getText() {
        return this.mText;
    }

    public List<XMLNode> getChildren() {
        return this.mChildren;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof XMLNode)) {
            return false;
        }
        XMLNode xMLNode = (XMLNode) obj;
        return TextUtils.equals(this.mTag, xMLNode.mTag) && TextUtils.equals(this.mText, xMLNode.mText) && this.mChildren.equals(xMLNode.mChildren);
    }

    public int hashCode() {
        return Objects.hash(this.mTag, this.mText, this.mChildren);
    }
}
