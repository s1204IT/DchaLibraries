package com.android.deskclock;

import android.support.annotation.NonNull;
import android.widget.Button;
import android.widget.ImageView;

public interface FabController {
    void onFabClick(@NonNull ImageView imageView);

    void onLeftButtonClick(@NonNull Button button);

    void onMorphFab(@NonNull ImageView imageView);

    void onRightButtonClick(@NonNull Button button);

    void onUpdateFab(@NonNull ImageView imageView);

    void onUpdateFabButtons(@NonNull Button button, @NonNull Button button2);
}
