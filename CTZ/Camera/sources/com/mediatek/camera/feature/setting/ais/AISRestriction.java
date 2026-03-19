package com.mediatek.camera.feature.setting.ais;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

public class AISRestriction {
    private static RelationGroup sRelationGroup = new RelationGroup();

    static {
        sRelationGroup.setHeaderKey("key_ais");
        sRelationGroup.setBodyKeys("key_scene_mode,key_iso,key_dng");
        sRelationGroup.addRelation(new Relation.Builder("key_ais", "on").addBody("key_scene_mode", "off", "off").addBody("key_iso", "0", "0").addBody("key_dng", "off", "off").build());
    }

    static RelationGroup getRestrictionGroup() {
        return sRelationGroup;
    }
}
