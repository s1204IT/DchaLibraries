package com.mediatek.camera.feature.setting.dng;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

public class DngRestriction {
    private static RelationGroup sRelation = new RelationGroup();

    static {
        sRelation.setHeaderKey("key_dng");
        sRelation.setBodyKeys("key_hdr");
        sRelation.addRelation(new Relation.Builder("key_dng", "on").addBody("key_hdr", "off", "off, auto, on").build());
    }

    static RelationGroup getRestriction() {
        return sRelation;
    }
}
