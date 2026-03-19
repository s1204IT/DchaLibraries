package com.android.commands.monkey;

import android.app.IActivityManager;
import android.view.IWindowManager;
import java.io.FileOutputStream;
import java.io.IOException;

public class MonkeyFlipEvent extends MonkeyEvent {
    private static final byte[] FLIP_0 = {127, 6, 0, 0, -32, 57, 1, 0, 5, 0, 0, 0, 1, 0, 0, 0};
    private static final byte[] FLIP_1 = {-123, 6, 0, 0, -97, -91, 12, 0, 5, 0, 0, 0, 0, 0, 0, 0};
    private final boolean mKeyboardOpen;

    public MonkeyFlipEvent(boolean z) {
        super(5);
        this.mKeyboardOpen = z;
    }

    @Override
    public int injectEvent(IWindowManager iWindowManager, IActivityManager iActivityManager, int i) {
        if (i > 0) {
            Logger.out.println(":Sending Flip keyboardOpen=" + this.mKeyboardOpen);
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream("/dev/input/event0");
            fileOutputStream.write(this.mKeyboardOpen ? FLIP_0 : FLIP_1);
            fileOutputStream.close();
            return 1;
        } catch (IOException e) {
            Logger.out.println("Got IOException performing flip" + e);
            return 0;
        }
    }
}
