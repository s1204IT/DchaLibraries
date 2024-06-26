package com.android.systemui;

import android.service.dreams.DreamService;
import com.android.systemui.DessertCaseView;
/* loaded from: a.zip:com/android/systemui/DessertCaseDream.class */
public class DessertCaseDream extends DreamService {
    private DessertCaseView.RescalingContainer mContainer;
    private DessertCaseView mView;

    @Override // android.service.dreams.DreamService, android.view.Window.Callback
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        this.mView = new DessertCaseView(this);
        this.mContainer = new DessertCaseView.RescalingContainer(this);
        this.mContainer.setView(this.mView);
        setContentView(this.mContainer);
    }

    @Override // android.service.dreams.DreamService
    public void onDreamingStarted() {
        super.onDreamingStarted();
        this.mView.postDelayed(new Runnable(this) { // from class: com.android.systemui.DessertCaseDream.1
            final DessertCaseDream this$0;

            {
                this.this$0 = this;
            }

            @Override // java.lang.Runnable
            public void run() {
                this.this$0.mView.start();
            }
        }, 1000L);
    }

    @Override // android.service.dreams.DreamService
    public void onDreamingStopped() {
        super.onDreamingStopped();
        this.mView.stop();
    }
}
