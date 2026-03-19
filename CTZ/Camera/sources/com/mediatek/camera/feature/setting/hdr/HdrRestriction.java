package com.mediatek.camera.feature.setting.hdr;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;
import java.util.concurrent.CopyOnWriteArrayList;

class HdrRestriction {
    private static CopyOnWriteArrayList<Object> sExtendRelation = new CopyOnWriteArrayList<>();
    private static RelationGroup sRelationGroup = new RelationGroup();

    static {
        sRelationGroup.setHeaderKey("key_hdr");
        sRelationGroup.setBodyKeys("key_flash, key_scene_mode, key_dng, key_continuous_shot, key_white_balance, key_color_effect, key_zsd, key_iso, key_ais, key_asd,key_brightness,key_contrast,key_hue,key_saturation,key_sharpness");
        sRelationGroup.addRelation(new Relation.Builder("key_hdr", "on").addBody("key_flash", "off", "off,on,auto").addBody("key_scene_mode", "hdr", "hdr").addBody("key_dng", "off", "off,on").addBody("key_continuous_shot", "off", "off").addBody("key_white_balance", "auto", "auto").addBody("key_color_effect", "none", "none").addBody("key_iso", "0", "0").addBody("key_ais", "off", "off").addBody("key_asd", "off", "off").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "middle", "middle").build());
        sRelationGroup.addRelation(new Relation.Builder("key_hdr", "auto").addBody("key_flash", "off", "off,on,auto").addBody("key_scene_mode", "hdr", "hdr").addBody("key_dng", "off", "off,on").addBody("key_continuous_shot", "off", "off").addBody("key_white_balance", "auto", "auto").addBody("key_color_effect", "none", "none").addBody("key_iso", "0", "0").addBody("key_ais", "off", "off").addBody("key_asd", "off", "off").addBody("key_brightness", "middle", "middle").addBody("key_contrast", "middle", "middle").addBody("key_hue", "middle", "middle").addBody("key_saturation", "middle", "middle").addBody("key_sharpness", "middle", "middle").build());
    }

    static RelationGroup getHdrRestriction() {
        return sRelationGroup;
    }
}
