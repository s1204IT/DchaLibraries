package com.mediatek.camera.feature.setting.focus;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

class FocusRestriction {
    private static RelationGroup sAeAfLockRelationGroup;
    private static RelationGroup sAfLockRelationGroup;
    private static RelationGroup sRelation = new RelationGroup();

    static {
        sRelation.setHeaderKey("key_focus");
        sRelation.setBodyKeys("key_face_detection");
        sRelation.addRelation(new Relation.Builder("key_focus", "auto").addBody("key_face_detection", "off", "off").build());
        sAfLockRelationGroup = new RelationGroup();
        sAfLockRelationGroup.setHeaderKey("key_focus");
        sAfLockRelationGroup.setBodyKeys("key_exposure");
        sAfLockRelationGroup.addRelation(new Relation.Builder("key_focus", "focus lock").addBody("key_exposure", "exposure-lock", "true").build());
        sAfLockRelationGroup.addRelation(new Relation.Builder("key_focus", "focus unlock").addBody("key_exposure", "exposure-lock", "false").build());
        sAeAfLockRelationGroup = new RelationGroup();
        sAeAfLockRelationGroup.setHeaderKey("key_focus");
        sAeAfLockRelationGroup.setBodyKeys("key_flash");
        sAeAfLockRelationGroup.addRelation(new Relation.Builder("key_focus", "focus lock").addBody("key_flash", "off", "off").build());
        sAeAfLockRelationGroup.addRelation(new Relation.Builder("key_focus", "focus unlock").build());
    }

    static RelationGroup getRestriction() {
        return sRelation;
    }

    static RelationGroup getAfLockRestriction() {
        return sAfLockRelationGroup;
    }

    static RelationGroup getAeAfLockRestriction() {
        return sAeAfLockRelationGroup;
    }
}
