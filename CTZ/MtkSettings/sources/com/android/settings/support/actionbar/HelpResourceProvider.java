package com.android.settings.support.actionbar;

import com.android.settings.R;

public interface HelpResourceProvider {
    default int getHelpResource() {
        return R.string.help_uri_default;
    }
}
