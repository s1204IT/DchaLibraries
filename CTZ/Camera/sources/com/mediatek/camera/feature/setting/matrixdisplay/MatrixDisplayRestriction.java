package com.mediatek.camera.feature.setting.matrixdisplay;

import com.mediatek.camera.common.relation.Relation;
import com.mediatek.camera.common.relation.RelationGroup;

public class MatrixDisplayRestriction {
    private static RelationGroup sRelationGroup = new RelationGroup();

    static {
        sRelationGroup.setHeaderKey("key_matrix_display");
        sRelationGroup.setBodyKeys("key_face_detection,key_noise_reduction,key_eis");
        sRelationGroup.addRelation(new Relation.Builder("key_matrix_display", "on").addBody("key_face_detection", "off", "off").addBody("key_noise_reduction", "off", "off").addBody("key_eis", "off", "off").build());
    }

    static RelationGroup getRestrictionGroup() {
        return sRelationGroup;
    }
}
