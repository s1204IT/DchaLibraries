package com.mediatek.camera.common.mode.video.intentvideo;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;
import com.mediatek.camera.portability.SystemProperties;

public class IntentRestriction {
    private static RelationGroup sRelationGroup = new RelationGroup();
    private static RelationGroup sRecordingRelationGroupForMode = new RelationGroup();

    static {
        sRelationGroup.setHeaderKey("com.mediatek.camera.common.mode.video.intentvideo.IntentVideoMode");
        if (SystemProperties.getInt("vendor.mtk.camera.app.fd.video", 0) == 0) {
            sRelationGroup.setBodyKeys("key_video_quality,key_focus,key_face_detection");
            sRelationGroup.addRelation(new Relation.Builder("com.mediatek.camera.common.mode.video.intentvideo.IntentVideoMode", "preview").addBody("key_video_quality", "0", "0").addBody("key_focus", "continuous-video", "continuous-video,auto").addBody("key_face_detection", "off", "off").build());
        } else {
            sRelationGroup.setBodyKeys("key_video_quality,key_focus");
            sRelationGroup.addRelation(new Relation.Builder("com.mediatek.camera.common.mode.video.intentvideo.IntentVideoMode", "preview").addBody("key_video_quality", "0", "0").addBody("key_focus", "continuous-video", "continuous-video,auto").build());
        }
        sRecordingRelationGroupForMode.setHeaderKey("com.mediatek.camera.common.mode.video.intentvideo.IntentVideoMode");
        sRecordingRelationGroupForMode.setBodyKeys("key_focus");
        if (SystemProperties.getInt("vendor.mtk.camera.app.fd.video", 0) == 0) {
            sRecordingRelationGroupForMode.addRelation(new Relation.Builder("com.mediatek.camera.common.mode.video.intentvideo.IntentVideoMode", "recording").addBody("key_focus", "auto", "auto").build());
        }
        sRecordingRelationGroupForMode.addRelation(new Relation.Builder("com.mediatek.camera.common.mode.video.intentvideo.IntentVideoMode", "stop-recording").addBody("key_focus", "continuous-video", "continuous-video,auto").build());
    }

    static RelationGroup getPreviewRelation() {
        return sRelationGroup;
    }

    static RelationGroup getRecordingRelationGroupForMode() {
        return sRecordingRelationGroupForMode;
    }
}
