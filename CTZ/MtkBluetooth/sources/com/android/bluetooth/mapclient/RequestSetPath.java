package com.android.bluetooth.mapclient;

import java.io.IOException;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;

class RequestSetPath extends Request {
    SetPathDir mDir;
    String mName;

    enum SetPathDir {
        ROOT,
        UP,
        DOWN
    }

    RequestSetPath(String str) {
        this.mDir = SetPathDir.DOWN;
        this.mName = str;
        this.mHeaderSet.setHeader(1, str);
    }

    RequestSetPath(boolean z) {
        this.mHeaderSet.setEmptyNameHeader();
        if (z) {
            this.mDir = SetPathDir.ROOT;
        } else {
            this.mDir = SetPathDir.UP;
        }
    }

    @Override
    public void execute(ClientSession clientSession) {
        HeaderSet path;
        try {
            switch (AnonymousClass1.$SwitchMap$com$android$bluetooth$mapclient$RequestSetPath$SetPathDir[this.mDir.ordinal()]) {
                case 1:
                case 2:
                    path = clientSession.setPath(this.mHeaderSet, false, false);
                    break;
                case 3:
                    path = clientSession.setPath(this.mHeaderSet, true, false);
                    break;
                default:
                    path = null;
                    break;
            }
            this.mResponseCode = path.getResponseCode();
        } catch (IOException e) {
            this.mResponseCode = 208;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$bluetooth$mapclient$RequestSetPath$SetPathDir = new int[SetPathDir.values().length];

        static {
            try {
                $SwitchMap$com$android$bluetooth$mapclient$RequestSetPath$SetPathDir[SetPathDir.ROOT.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$bluetooth$mapclient$RequestSetPath$SetPathDir[SetPathDir.DOWN.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$bluetooth$mapclient$RequestSetPath$SetPathDir[SetPathDir.UP.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
        }
    }
}
