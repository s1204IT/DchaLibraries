package android.test.mock;

import android.content.DialogInterface;

public class MockDialogInterface implements DialogInterface {
    @Override
    public void cancel() {
        throw new UnsupportedOperationException("not implemented yet");
    }

    @Override
    public void dismiss() {
        throw new UnsupportedOperationException("not implemented yet");
    }
}
