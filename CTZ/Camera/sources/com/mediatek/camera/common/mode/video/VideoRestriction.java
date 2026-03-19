package com.mediatek.camera.common.mode.video;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;
import com.mediatek.camera.portability.SystemProperties;

public class VideoRestriction {
    private static final String VIDEO_MODE_KEY = VideoMode.class.getName();
    private static RelationGroup sPreviewRelationGroup = new RelationGroup();
    private static RelationGroup sRecordingRelationGroupForMode = new RelationGroup();

    static {
        sPreviewRelationGroup.setHeaderKey(VIDEO_MODE_KEY);
        if (SystemProperties.getInt("vendor.mtk.camera.app.fd.video", 0) == 0) {
            sPreviewRelationGroup.setBodyKeys("key_focus,key_scene_mode,key_face_detection");
            sPreviewRelationGroup.addRelation(new Relation.Builder(VIDEO_MODE_KEY, "preview").addBody("key_focus", "continuous-video", "continuous-video,auto").addBody("key_scene_mode", "off", getVideoSceneRestriction()).addBody("key_face_detection", "off", "off").build());
        } else {
            sPreviewRelationGroup.setBodyKeys("key_focus,key_scene_mode");
            sPreviewRelationGroup.addRelation(new Relation.Builder(VIDEO_MODE_KEY, "preview").addBody("key_focus", "continuous-video", "continuous-video,auto").addBody("key_scene_mode", "off", getVideoSceneRestriction()).build());
        }
        sRecordingRelationGroupForMode.setHeaderKey(VIDEO_MODE_KEY);
        sRecordingRelationGroupForMode.setBodyKeys("key_focus");
        if (SystemProperties.getInt("vendor.mtk.camera.app.fd.video", 0) == 0) {
            sRecordingRelationGroupForMode.addRelation(new Relation.Builder(VIDEO_MODE_KEY, "recording").addBody("key_focus", "auto", "auto").build());
        }
        sRecordingRelationGroupForMode.addRelation(new Relation.Builder(VIDEO_MODE_KEY, "stop-recording").addBody("key_focus", "continuous-video", "continuous-video,auto").build());
    }

    static RelationGroup getPreviewRelation() {
        return sPreviewRelationGroup;
    }

    static RelationGroup getRecordingRelationForMode() {
        return sRecordingRelationGroupForMode;
    }

    static String getVideoSceneRestriction() {
        return "off,night,sunset,party,portrait,landscape,night-portrait,theatre,beach,snow,steadyphoto,sports,candlelight";
    }
}
