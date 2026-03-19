package com.android.documentsui.picker;

import com.android.documentsui.ActivityConfig;
import com.android.documentsui.base.MimeTypes;
import com.android.documentsui.base.State;

final class Config extends ActivityConfig {
    Config() {
    }

    @Override
    public boolean canSelectType(String str, int i, State state) {
        return (!isDocumentEnabled(str, i, state) || MimeTypes.isDirectoryType(str) || state.action == 6 || state.action == 2) ? false : true;
    }

    @Override
    public boolean isDocumentEnabled(String str, int i, State state) {
        if (MimeTypes.isDirectoryType(str)) {
            return true;
        }
        switch (state.action) {
            case 4:
                if ((i & 2) == 0) {
                    return false;
                }
            case 3:
            case 5:
                if (((i & 512) != 0) && state.openableOnly) {
                    return false;
                }
            default:
                return MimeTypes.mimeMatches(state.acceptMimes, str);
        }
    }
}
