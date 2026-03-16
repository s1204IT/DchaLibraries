package com.android.internal.telephony.cat;

class SetRefreshParams extends CommandParams {
    boolean fileChanged;
    int[] fileList;

    SetRefreshParams(CommandDetails cmdDet, boolean fileChanged, int[] filelist) {
        super(cmdDet);
        this.fileChanged = fileChanged;
        this.fileList = filelist;
    }
}
