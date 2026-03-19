package com.mediatek.camera.common.relation;

import com.mediatek.camera.common.debug.LogUtil;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Relation {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(Relation.class.getSimpleName());
    private final CopyOnWriteArrayList<Body> mBodyList;
    private Header mHeader;

    public static class Builder {
        private final Relation mRelation = new Relation();

        public Builder(String str, String str2) {
            this.mRelation.createHeader(str, str2);
        }

        public Builder addBody(String str, String str2, String str3) {
            this.mRelation.addBody(str, str2, str3);
            return this;
        }

        public Relation build() {
            return this.mRelation.copy();
        }
    }

    private class Header {
        public final String key;
        public final String value;

        public Header(String str, String str2) {
            this.key = str;
            this.value = str2;
        }

        public Header copy() {
            return Relation.this.new Header(this.key, this.value);
        }
    }

    private class Body {
        public String entryValues;
        public final String key;
        public String value;

        public Body(String str, String str2, String str3) {
            this.key = str;
            this.value = str2;
            this.entryValues = str3;
        }

        public Body copy() {
            return Relation.this.new Body(this.key, this.value, this.entryValues);
        }
    }

    private Relation() {
        this.mBodyList = new CopyOnWriteArrayList<>();
    }

    public String getHeaderKey() {
        return this.mHeader.key;
    }

    public String getHeaderValue() {
        return this.mHeader.value;
    }

    public List<String> getBodyKeys() {
        CopyOnWriteArrayList copyOnWriteArrayList = new CopyOnWriteArrayList();
        Iterator<Body> it = this.mBodyList.iterator();
        while (it.hasNext()) {
            copyOnWriteArrayList.add(it.next().key);
        }
        return copyOnWriteArrayList;
    }

    public String getBodyValue(String str) {
        Body bodyFindBody = findBody(str);
        if (bodyFindBody != null) {
            return bodyFindBody.value;
        }
        return null;
    }

    public String getBodyEntryValues(String str) {
        Body bodyFindBody = findBody(str);
        if (bodyFindBody != null) {
            return bodyFindBody.entryValues;
        }
        return null;
    }

    public Relation copy() {
        Relation relation = new Relation();
        relation.setHeader(this.mHeader.copy());
        Iterator<Body> it = this.mBodyList.iterator();
        while (it.hasNext()) {
            relation.addBody(it.next().copy());
        }
        return relation;
    }

    public void addBody(String str, String str2, String str3) {
        Body bodyFindBody = findBody(str);
        if (bodyFindBody != null) {
            bodyFindBody.value = str2;
            bodyFindBody.entryValues = str3;
        } else {
            this.mBodyList.add(new Body(str, str2, str3));
        }
    }

    public void removeBody(String str) {
        Body bodyFindBody = findBody(str);
        if (bodyFindBody != null) {
            this.mBodyList.remove(bodyFindBody);
        }
    }

    private void createHeader(String str, String str2) {
        this.mHeader = new Header(str, str2);
    }

    private void setHeader(Header header) {
        this.mHeader = header;
    }

    private Body findBody(String str) {
        for (int i = 0; i < this.mBodyList.size(); i++) {
            Body body = this.mBodyList.get(i);
            if (body.key.equals(str)) {
                return body;
            }
        }
        return null;
    }

    private void addBody(Body body) {
        this.mBodyList.add(body);
    }
}
