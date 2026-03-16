package com.android.documentsui.model;

import android.content.ContentResolver;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ProtocolException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class DocumentStack extends LinkedList<DocumentInfo> implements Durable {
    public RootInfo root;

    public void updateRoot(Collection<RootInfo> matchingRoots) throws FileNotFoundException {
        for (RootInfo root : matchingRoots) {
            if (root.equals(this.root)) {
                this.root = root;
                return;
            }
        }
        throw new FileNotFoundException("Failed to find matching root for " + this.root);
    }

    public void updateDocuments(ContentResolver resolver) throws FileNotFoundException {
        Iterator i$ = iterator();
        while (i$.hasNext()) {
            DocumentInfo info = (DocumentInfo) i$.next();
            info.updateSelf(resolver);
        }
    }

    public String buildKey() {
        StringBuilder builder = new StringBuilder();
        if (this.root != null) {
            builder.append(this.root.authority).append('#');
            builder.append(this.root.rootId).append('#');
        } else {
            builder.append("[null]").append('#');
        }
        Iterator i$ = iterator();
        while (i$.hasNext()) {
            DocumentInfo doc = (DocumentInfo) i$.next();
            builder.append(doc.documentId).append('#');
        }
        return builder.toString();
    }

    @Override
    public void reset() {
        clear();
        this.root = null;
    }

    @Override
    public void read(DataInputStream in) throws IOException {
        int version = in.readInt();
        switch (version) {
            case 1:
                throw new ProtocolException("Ignored upgrade");
            case 2:
                if (in.readBoolean()) {
                    this.root = new RootInfo();
                    this.root.read(in);
                }
                int size = in.readInt();
                for (int i = 0; i < size; i++) {
                    DocumentInfo doc = new DocumentInfo();
                    doc.read(in);
                    add(doc);
                }
                return;
            default:
                throw new ProtocolException("Unknown version " + version);
        }
    }

    @Override
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(2);
        if (this.root != null) {
            out.writeBoolean(true);
            this.root.write(out);
        } else {
            out.writeBoolean(false);
        }
        int size = size();
        out.writeInt(size);
        for (int i = 0; i < size; i++) {
            DocumentInfo doc = get(i);
            doc.write(out);
        }
    }
}
