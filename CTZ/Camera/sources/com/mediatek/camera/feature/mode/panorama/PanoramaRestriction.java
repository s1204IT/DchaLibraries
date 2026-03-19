package com.mediatek.camera.feature.mode.panorama;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

public class PanoramaRestriction {
    private static RelationGroup s3aRelation;
    private static final String KEY_PANORAMA = PanoramaMode.class.getName();
    private static RelationGroup sRelation = new RelationGroup();

    static {
        sRelation.setHeaderKey(KEY_PANORAMA);
        sRelation.setBodyKeys("key_continuous_shot,key_flash,key_face_detection,key_hdr,key_zsd,key_dng,key_self_timer,key_camera_switcher,key_scene_mode,key_color_effect,key_ais");
        sRelation.addRelation(new Relation.Builder(KEY_PANORAMA, "on").addBody("key_continuous_shot", "off", "off").addBody("key_flash", "off", "off").addBody("key_face_detection", "off", "off").addBody("key_hdr", "off", "off").addBody("key_zsd", "off", "off").addBody("key_dng", "off", "off").addBody("key_self_timer", "0", "0").addBody("key_camera_switcher", "back", "back").addBody("key_scene_mode", "off", "off").addBody("key_color_effect", "none", "none").addBody("key_ais", "off", "off").build());
        s3aRelation = new RelationGroup();
        s3aRelation.setHeaderKey(KEY_PANORAMA);
        s3aRelation.setBodyKeys("key_focus,key_exposure,key_white_balance,key_dual_zoom");
        s3aRelation.addRelation(new Relation.Builder(KEY_PANORAMA, "on").addBody("key_focus", "auto", "auto").addBody("key_exposure", "exposure-lock", "true").addBody("key_white_balance", "white-balance-lock", "true").addBody("key_dual_zoom", "limit", "limit").build());
        s3aRelation.addRelation(new Relation.Builder(KEY_PANORAMA, "off").addBody("key_focus", null, null).addBody("key_exposure", "exposure-lock", "false").addBody("key_white_balance", "white-balance-lock", "false").addBody("key_dual_zoom", "on", "on, off").build());
    }

    static RelationGroup getRestriction() {
        return sRelation;
    }

    static RelationGroup get3aRestriction() {
        return s3aRelation;
    }
}
