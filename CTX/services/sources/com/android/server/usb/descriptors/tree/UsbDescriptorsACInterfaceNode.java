package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.report.ReportCanvas;

public final class UsbDescriptorsACInterfaceNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsACInterfaceNode";
    private final UsbACInterface mACInterface;

    public UsbDescriptorsACInterfaceNode(UsbACInterface usbACInterface) {
        this.mACInterface = usbACInterface;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        reportCanvas.writeListItem("AC Interface type: 0x" + Integer.toHexString(this.mACInterface.getSubtype()));
        reportCanvas.openList();
        this.mACInterface.report(reportCanvas);
        reportCanvas.closeList();
    }
}
