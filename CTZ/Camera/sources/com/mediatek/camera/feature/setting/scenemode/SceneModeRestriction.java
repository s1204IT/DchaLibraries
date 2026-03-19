package com.mediatek.camera.feature.setting.scenemode;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

public class SceneModeRestriction {
    private static RelationGroup sRelationGroup = new RelationGroup();
    private static RelationGroup sAsdRelationGroup = new RelationGroup();

    static {
        sRelationGroup.setHeaderKey("key_scene_mode");
        sRelationGroup.setBodyKeys("key_iso,key_exposure,key_flash,key_white_balance,key_brightness,key_contrast,key_hue,key_saturation,key_sharpness,key_color_effect");
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "night").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "low", "low").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "sunset").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_white_balance", "daylight", "daylight").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "high", "high").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "party").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "middle", "middle").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "portrait").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "low", "low").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "landscape").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_white_balance", "daylight", "daylight").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "high", "high").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "night-portrait").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "low", "low").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "theatre").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "low", "low").addBody("key_sharpness", "high", "high").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "beach").addBody("key_iso", "0", "0").addBody("key_exposure", "1", "1").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "high", "high").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "snow").addBody("key_iso", "0", "0").addBody("key_exposure", "1", "1").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "high", "high").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "steadyphoto").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "middle", "middle").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "fireworks").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_flash", "off", "off").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "middle", "middle").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "sports").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "middle", "middle").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "candlelight").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_white_balance", "incandescent", "incandescent").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "middle", "middle").build());
        sRelationGroup.addRelation(new Relation.Builder("key_scene_mode", "auto-scene-detection").addBody("key_iso", "0", "0").addBody("key_exposure", "0", "0").addBody("key_white_balance", "auto", "auto").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "middle", "middle").addBody("key_color_effect", "none", "none").build());
    }

    static RelationGroup getRestrictionGroup() {
        return sRelationGroup;
    }
}
