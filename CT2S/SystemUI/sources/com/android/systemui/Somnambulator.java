package com.android.systemui;

import android.app.Activity;
import android.content.Intent;
import android.service.dreams.Sandman;

public class Somnambulator extends Activity {
    @Override
    public void onStart() {
        super.onStart();
        Intent launchIntent = getIntent();
        String action = launchIntent.getAction();
        if ("android.intent.action.CREATE_SHORTCUT".equals(action)) {
            Intent shortcutIntent = new Intent(this, (Class<?>) Somnambulator.class);
            shortcutIntent.setFlags(276824064);
            Intent resultIntent = new Intent();
            resultIntent.putExtra("android.intent.extra.shortcut.ICON_RESOURCE", Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher_dreams));
            resultIntent.putExtra("android.intent.extra.shortcut.INTENT", shortcutIntent);
            resultIntent.putExtra("android.intent.extra.shortcut.NAME", getString(R.string.start_dreams));
            setResult(-1, resultIntent);
        } else {
            boolean docked = launchIntent.hasCategory("android.intent.category.DESK_DOCK");
            if (docked) {
                Sandman.startDreamWhenDockedIfAppropriate(this);
            } else {
                Sandman.startDreamByUserRequest(this);
            }
        }
        finish();
    }
}
