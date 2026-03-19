package com.android.server.wifi.hotspot2.omadm;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class OMAConstructed extends OMANode {
    private final MultiValueMap<OMANode> mChildren;

    public OMAConstructed(OMAConstructed parent, String name, String context, String... avps) {
        this(parent, name, context, new MultiValueMap(), buildAttributes(avps));
    }

    protected OMAConstructed(OMAConstructed parent, String name, String context, MultiValueMap<OMANode> children, Map<String, String> avps) {
        super(parent, name, context, avps);
        this.mChildren = children;
    }

    @Override
    public OMANode addChild(String name, String context, String value, String pathString) throws IOException {
        OMANode child;
        if (pathString == null) {
            if (value != null) {
                child = new OMAScalar(this, name, context, value, new String[0]);
            } else {
                child = new OMAConstructed(this, name, context, new String[0]);
            }
            this.mChildren.put(name, child);
            return child;
        }
        OMANode target = this;
        while (target.getParent() != null) {
            target = target.getParent();
        }
        for (String element : pathString.split("/")) {
            target = target.getChild(element);
            if (target == null) {
                throw new IOException("No child node '" + element + "' in " + getPathString());
            }
            if (target.isLeaf()) {
                throw new IOException("Cannot add child to leaf node: " + getPathString());
            }
        }
        return target.addChild(name, context, value, null);
    }

    @Override
    public OMAConstructed reparent(OMAConstructed parent) {
        return new OMAConstructed(parent, getName(), getContext(), this.mChildren, getAttributes());
    }

    public void addChild(OMANode child) {
        this.mChildren.put(child.getName(), child.reparent(this));
    }

    @Override
    public String getScalarValue(Iterator<String> path) throws OMAException {
        if (!path.hasNext()) {
            throw new OMAException("Path too short for " + getPathString());
        }
        String tag = path.next();
        OMANode child = this.mChildren.get(tag);
        if (child != null) {
            return child.getScalarValue(path);
        }
        return null;
    }

    @Override
    public OMANode getListValue(Iterator<String> path) throws OMAException {
        OMANode child;
        if (!path.hasNext()) {
            return null;
        }
        String tag = path.next();
        if (tag.equals("?")) {
            child = this.mChildren.getSingletonValue();
        } else {
            child = this.mChildren.get(tag);
        }
        if (child == null) {
            return null;
        }
        if (path.hasNext()) {
            return child.getListValue(path);
        }
        return child;
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    @Override
    public Collection<OMANode> getChildren() {
        return Collections.unmodifiableCollection(this.mChildren.values());
    }

    @Override
    public OMANode getChild(String name) {
        return this.mChildren.get(name);
    }

    public OMANode replaceNode(OMANode oldNode, OMANode newNode) {
        return this.mChildren.replace(oldNode.getName(), oldNode, newNode);
    }

    public OMANode removeNode(String key, OMANode node) {
        if (key.equals("?")) {
            return this.mChildren.remove(node);
        }
        return this.mChildren.remove(key, node);
    }

    @Override
    public String getValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void toString(StringBuilder sb, int level) {
        sb.append(getPathString());
        if (getContext() != null) {
            sb.append(" (").append(getContext()).append(')');
        }
        sb.append('\n');
        for (OMANode node : this.mChildren.values()) {
            node.toString(sb, level + 1);
        }
    }

    @Override
    public void marshal(OutputStream out, int level) throws IOException {
        OMAConstants.indent(level, out);
        OMAConstants.serializeString(getName(), out);
        if (getContext() != null) {
            out.write(String.format("(%s)", getContext()).getBytes(StandardCharsets.UTF_8));
        }
        out.write(new byte[]{43, 10});
        for (OMANode child : this.mChildren.values()) {
            child.marshal(out, level + 1);
        }
        OMAConstants.indent(level, out);
        out.write(".\n".getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void fillPayload(StringBuilder sb) {
        if (getContext() != null) {
            sb.append('<').append(MOTree.RTPropTag).append(">\n");
            sb.append('<').append(MOTree.TypeTag).append(">\n");
            sb.append('<').append(MOTree.DDFNameTag).append(">");
            sb.append(getContext());
            sb.append("</").append(MOTree.DDFNameTag).append(">\n");
            sb.append("</").append(MOTree.TypeTag).append(">\n");
            sb.append("</").append(MOTree.RTPropTag).append(">\n");
        }
        for (OMANode child : getChildren()) {
            child.toXml(sb);
        }
    }
}
