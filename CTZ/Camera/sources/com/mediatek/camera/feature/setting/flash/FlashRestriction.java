package com.mediatek.camera.feature.setting.flash;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

public class FlashRestriction {
    private static RelationGroup sRelationGroup = new RelationGroup();

    static {
        sRelationGroup.setHeaderKey("key_flash");
        sRelationGroup.setBodyKeys("key_hdr");
        sRelationGroup.addRelation(new Relation.Builder("key_flash", "on").addBody("key_hdr", "off", "off,on,auto").build());
        sRelationGroup.addRelation(new Relation.Builder("key_flash", "auto").addBody("key_hdr", "off", "off,on,auto").build());
        sRelationGroup.addRelation(new Relation.Builder("key_flash", "torch").addBody("key_hdr", "off", "off,on,auto").build());
    }

    static RelationGroup getFlashRestriction() {
        return sRelationGroup;
    }
}
