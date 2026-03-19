package com.mediatek.camera.feature.setting;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

public class ContinuousShotRestriction {
    private static RelationGroup sAsdRelation;
    private static RelationGroup sFocusSoundRelation;
    private static RelationGroup sFocusUiRelation;
    private static RelationGroup sRelation = new RelationGroup();

    static {
        sRelation.setHeaderKey("key_continuous_shot");
        sRelation.setBodyKeys("key_dng,key_face_detection,key_dual_zoom,key_zsd");
        sRelation.addRelation(new Relation.Builder("key_continuous_shot", "on").addBody("key_dng", "off", "off").addBody("key_face_detection", "off", "on, off").addBody("key_dual_zoom", "limit", "limit").build());
        sAsdRelation = new RelationGroup();
        sAsdRelation.setHeaderKey("key_continuous_shot");
        sAsdRelation.setBodyKeys("key_scene_mode");
        sAsdRelation.addRelation(new Relation.Builder("key_continuous_shot", "on").addBody("key_scene_mode", "off", "off, night, sunset, party, portrait, landscape, night-portrait, theatre, beach, snow, steadyphoto, fireworks, sports, candlelight").build());
        sFocusUiRelation = new RelationGroup();
        sFocusUiRelation.setHeaderKey("key_continuous_shot");
        sFocusUiRelation.setBodyKeys("key_focus");
        sFocusUiRelation.addRelation(new Relation.Builder("key_continuous_shot", "on").addBody("key_focus", "focus-ui", "false").build());
        sFocusUiRelation.addRelation(new Relation.Builder("key_continuous_shot", "off").addBody("key_focus", "focus-ui", "true").build());
        sFocusSoundRelation = new RelationGroup();
        sFocusSoundRelation.setHeaderKey("key_continuous_shot");
        sFocusSoundRelation.setBodyKeys("key_focus");
        sFocusSoundRelation.addRelation(new Relation.Builder("key_continuous_shot", "on").addBody("key_focus", "focus-sound", "false").build());
        sFocusSoundRelation.addRelation(new Relation.Builder("key_continuous_shot", "off").addBody("key_focus", "focus-sound", "true").build());
    }

    static RelationGroup getFocusUiRestriction() {
        return sFocusUiRelation;
    }

    static RelationGroup getFocusSoundRestriction() {
        return sFocusSoundRelation;
    }

    static RelationGroup getRestriction() {
        return sRelation;
    }

    static RelationGroup getAsdRestriction() {
        return sAsdRelation;
    }
}
