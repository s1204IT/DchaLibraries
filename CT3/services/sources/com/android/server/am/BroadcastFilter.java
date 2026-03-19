package com.android.server.am;

import android.content.IntentFilter;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import java.io.PrintWriter;

final class BroadcastFilter extends IntentFilter {
    final int owningUid;
    final int owningUserId;
    final String packageName;
    final ReceiverList receiverList;
    final String requiredPermission;

    BroadcastFilter(IntentFilter _filter, ReceiverList _receiverList, String _packageName, String _requiredPermission, int _owningUid, int _userId) {
        super(_filter);
        this.receiverList = _receiverList;
        this.packageName = _packageName;
        this.requiredPermission = _requiredPermission;
        this.owningUid = _owningUid;
        this.owningUserId = _userId;
    }

    public void dump(PrintWriter pw, String prefix) {
        dumpInReceiverList(pw, new PrintWriterPrinter(pw), prefix);
        this.receiverList.dumpLocal(pw, prefix);
    }

    public void dumpBrief(PrintWriter pw, String prefix) {
        dumpBroadcastFilterState(pw, prefix);
    }

    public void dumpInReceiverList(PrintWriter pw, Printer pr, String prefix) {
        super.dump(pr, prefix);
        dumpBroadcastFilterState(pw, prefix);
    }

    void dumpBroadcastFilterState(PrintWriter pw, String prefix) {
        if (this.requiredPermission == null) {
            return;
        }
        pw.print(prefix);
        pw.print("requiredPermission=");
        pw.println(this.requiredPermission);
    }

    public String toString() {
        return "BroadcastFilter{" + Integer.toHexString(System.identityHashCode(this)) + " u" + this.owningUserId + ' ' + this.receiverList + '}';
    }
}
