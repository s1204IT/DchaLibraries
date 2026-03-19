package com.mediatek.camera.feature.mode.vsdof.photo;

import android.hardware.camera2.CameraCharacteristics;
import com.mediatek.camera.common.relation.DataStore;
import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

public class SdofPhotoRestriction {
    private static CameraCharacteristics sCharacteristics = null;
    private static final String PHOTO_MODE_KEY = SdofPhotoMode.class.getName();
    private static DataStore sDataStore = null;
    private static RelationGroup sRelation = new RelationGroup();

    public static RelationGroup getRestriction() {
        sRelation.setHeaderKey(PHOTO_MODE_KEY);
        sRelation.setBodyKeys("key_continuous_shot,key_flash,key_zsd,key_dng,key_camera_switcher,key_camera_zoom,key_anti_flicker");
        sRelation.addRelation(new Relation.Builder(PHOTO_MODE_KEY, "on").addBody("key_continuous_shot", "off", "off").addBody("key_flash", "off", "off").addBody("key_zsd", "on", "on").addBody("key_dng", "off", "off").addBody("key_camera_switcher", "back", "back").addBody("key_camera_zoom", "off", "off").addBody("key_anti_flicker", "off", "off").build());
        return sRelation;
    }

    public static void setCameraCharacteristics(CameraCharacteristics cameraCharacteristics, DataStore dataStore) {
        sCharacteristics = cameraCharacteristics;
        sDataStore = dataStore;
    }
}
