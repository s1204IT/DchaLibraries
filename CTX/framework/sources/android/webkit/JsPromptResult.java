package android.webkit;

import android.annotation.SystemApi;
import android.webkit.JsResult;

public class JsPromptResult extends JsResult {
    private String mStringResult;

    public void confirm(String str) {
        this.mStringResult = str;
        confirm();
    }

    @SystemApi
    public JsPromptResult(JsResult.ResultReceiver resultReceiver) {
        super(resultReceiver);
    }

    @SystemApi
    public String getStringResult() {
        return this.mStringResult;
    }
}
