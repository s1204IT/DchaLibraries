package com.android.settings.inputmethod;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;
import com.android.settings.R;

public class UserDictionaryAddWordActivity extends Activity {
    private UserDictionaryAddWordContents mContents;

    @Override
    public void onCreate(Bundle bundle) {
        int i;
        super.onCreate(bundle);
        setContentView(R.layout.user_dictionary_add_word);
        Intent intent = getIntent();
        String action = intent.getAction();
        if ("com.android.settings.USER_DICTIONARY_EDIT".equals(action)) {
            i = 0;
        } else if ("com.android.settings.USER_DICTIONARY_INSERT".equals(action)) {
            i = 1;
        } else {
            throw new RuntimeException("Unsupported action: " + action);
        }
        Bundle extras = intent.getExtras();
        if (extras == null) {
            extras = new Bundle();
        }
        extras.putInt("mode", i);
        if (bundle != null) {
            extras.putAll(bundle);
        }
        this.mContents = new UserDictionaryAddWordContents(getWindow().getDecorView(), extras);
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        this.mContents.saveStateIntoBundle(bundle);
    }

    private void reportBackToCaller(int i, Bundle bundle) {
        Intent intent = getIntent();
        if (intent.getExtras() == null) {
            return;
        }
        Object obj = intent.getExtras().get("listener");
        if (obj instanceof Messenger) {
            Messenger messenger = (Messenger) obj;
            Message messageObtain = Message.obtain();
            messageObtain.obj = bundle;
            messageObtain.what = i;
            try {
                messenger.send(messageObtain);
            } catch (RemoteException e) {
            }
        }
    }

    public void onClickCancel(View view) {
        reportBackToCaller(1, null);
        finish();
    }

    public void onClickConfirm(View view) {
        Bundle bundle = new Bundle();
        reportBackToCaller(this.mContents.apply(this, bundle), bundle);
        finish();
    }
}
