package com.mediatek.camera.feature.mode.longexposure;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

class LongExposureRestriction {
    private static RelationGroup sRelationGroup = new RelationGroup();

    static {
        sRelationGroup.setHeaderKey("com.mediatek.camera.feature.mode.longexposure.LongExposureMode");
        sRelationGroup.setBodyKeys("key_eis,key_scene_mode,key_microphone,key_hdr,key_color_effect,key_flash,key_white_balance,key_noise_reduction,key_camera_switcher,key_anti_flicker,key_image_properties,key_brightness,key_contrast,key_hue,key_saturation,key_sharpness,key_face_detection,key_ais,key_dng,key_self_timer,key_exposure,key_zsd,key_dual_zoom,key_continuous_shot");
        sRelationGroup.addRelation(new Relation.Builder("com.mediatek.camera.feature.mode.longexposure.LongExposureMode", "on").addBody("key_eis", "off", "off").addBody("key_scene_mode", "off", "off").addBody("key_hdr", "off", "off").addBody("key_noise_reduction", "off", "off").addBody("key_microphone", "off", "off").addBody("key_flash", "off", "off").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "middle", "middle").addBody("key_white_balance", "auto", "auto").addBody("key_anti_flicker", "auto", "auto").addBody("key_camera_switcher", "back", "back").addBody("key_color_effect", "none", "none").addBody("key_face_detection", "off", "off").addBody("key_ais", "off", "off").addBody("key_dng", "off", "off").addBody("key_self_timer", "0", "0").addBody("key_exposure", "0", "0").addBody("key_zsd", "off", "off").addBody("key_dual_zoom", "off", "off").addBody("key_continuous_shot", "off", "off").build());
    }

    static RelationGroup getRestriction() {
        return sRelationGroup;
    }
}
