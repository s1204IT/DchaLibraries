package com.mediatek.camera.common.relation;

import com.google.common.base.Splitter;
import com.mediatek.camera.common.debug.LogHelper;
import com.mediatek.camera.common.debug.LogUtil;
import com.mediatek.camera.common.relation.Relation;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class RelationGroup {
    private static final LogUtil.Tag TAG = new LogUtil.Tag(RelationGroup.class.getSimpleName());
    private String mHeaderKey;
    private final CopyOnWriteArrayList<String> mBodyKeys = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Relation> mRelationList = new CopyOnWriteArrayList<>();

    public void setHeaderKey(String str) {
        this.mHeaderKey = str;
    }

    public void setBodyKeys(String str) {
        if (str == null) {
            LogHelper.e(TAG, "[setBodyKeys] with Null bodyKeys!!!!!!");
            return;
        }
        Iterator<String> it = Splitter.on(",").trimResults().omitEmptyStrings().split(str).iterator();
        while (it.hasNext()) {
            this.mBodyKeys.add(it.next());
        }
    }

    public void addRelation(Relation relation) {
        this.mRelationList.add(relation);
    }

    public Relation getRelation(String str, boolean z) {
        Relation relationBuildNewRelation;
        Iterator<Relation> it = this.mRelationList.iterator();
        while (true) {
            if (it.hasNext()) {
                Relation next = it.next();
                if (next.getHeaderValue().equals(str)) {
                    relationBuildNewRelation = buildNewRelation(next);
                    break;
                }
            } else {
                relationBuildNewRelation = null;
                break;
            }
        }
        if (relationBuildNewRelation == null && z) {
            return buildEmptyRelation(this.mHeaderKey, str);
        }
        return relationBuildNewRelation;
    }

    private Relation buildNewRelation(Relation relation) {
        Relation.Builder builder = new Relation.Builder(relation.getHeaderKey(), relation.getHeaderValue());
        for (String str : this.mBodyKeys) {
            builder.addBody(str, relation.getBodyValue(str), relation.getBodyEntryValues(str));
        }
        return builder.build();
    }

    private Relation buildEmptyRelation(String str, String str2) {
        Relation.Builder builder = new Relation.Builder(str, str2);
        Iterator<String> it = this.mBodyKeys.iterator();
        while (it.hasNext()) {
            builder.addBody(it.next(), null, null);
        }
        return builder.build();
    }
}
