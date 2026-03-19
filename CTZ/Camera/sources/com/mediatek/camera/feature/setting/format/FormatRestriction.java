package com.mediatek.camera.feature.setting.format;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

public class FormatRestriction {
    private static RelationGroup sRelation = new RelationGroup();

    static {
        sRelation.setHeaderKey("key_format");
        sRelation.setBodyKeys("key_hdr");
        sRelation.setBodyKeys("key_dng");
        sRelation.setBodyKeys("key_continuous_shot");
        sRelation.addRelation(new Relation.Builder("key_format", "heif").addBody("key_hdr", "off", "off").addBody("key_dng", "off", "off").addBody("key_continuous_shot", "off", "off").build());
    }

    static RelationGroup getRestriction() {
        return sRelation;
    }
}
