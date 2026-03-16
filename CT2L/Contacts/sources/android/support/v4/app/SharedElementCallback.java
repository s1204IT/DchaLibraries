package android.support.v4.app;

import android.view.View;
import java.util.List;
import java.util.Map;

public abstract class SharedElementCallback {
    private static int MAX_IMAGE_SIZE = 1048576;

    public void onSharedElementStart(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
    }

    public void onSharedElementEnd(List<String> sharedElementNames, List<View> sharedElements, List<View> sharedElementSnapshots) {
    }

    public void onMapSharedElements(List<String> names, Map<String, View> sharedElements) {
    }
}
