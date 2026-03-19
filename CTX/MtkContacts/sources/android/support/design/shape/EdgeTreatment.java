package android.support.design.shape;

import com.android.contacts.ContactPhotoManager;

public class EdgeTreatment {
    public void getEdgePath(float length, float interpolation, ShapePath shapePath) {
        shapePath.lineTo(length, ContactPhotoManager.OFFSET_DEFAULT);
    }
}
