package jp.co.omronsoft.WnnConnector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.android.common.speech.LoggingEvents;

public class WnnConnectorActivity extends Activity {
    private static final String KEY_SEND = "modifiedtext";
    private static final String KEY_WORD = "text";
    public static final double VERSION = 1.0d;
    private CharSequence mCharSequence = LoggingEvents.EXTRA_CALLING_APP_NAME;
    private CharSequence mModifiedCharSequence = LoggingEvents.EXTRA_CALLING_APP_NAME;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        if (intent != null) {
            this.mCharSequence = intent.getCharSequenceExtra(KEY_WORD);
            this.mModifiedCharSequence = this.mCharSequence;
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        if (this.mModifiedCharSequence == null) {
            this.mModifiedCharSequence = LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
        intent.putExtra(KEY_SEND, this.mModifiedCharSequence);
        setResult(-1, intent);
        super.finish();
    }

    public CharSequence getText() {
        return this.mCharSequence;
    }

    public void setModifiedText(CharSequence c) {
        this.mModifiedCharSequence = c;
    }
}
