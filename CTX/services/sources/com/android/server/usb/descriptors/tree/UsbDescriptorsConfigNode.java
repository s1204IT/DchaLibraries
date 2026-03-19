package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbConfigDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;
import java.util.Iterator;

public final class UsbDescriptorsConfigNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsConfigNode";
    private final UsbConfigDescriptor mConfigDescriptor;
    private final ArrayList<UsbDescriptorsInterfaceNode> mInterfaceNodes = new ArrayList<>();

    public UsbDescriptorsConfigNode(UsbConfigDescriptor usbConfigDescriptor) {
        this.mConfigDescriptor = usbConfigDescriptor;
    }

    public void addInterfaceNode(UsbDescriptorsInterfaceNode usbDescriptorsInterfaceNode) {
        this.mInterfaceNodes.add(usbDescriptorsInterfaceNode);
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        this.mConfigDescriptor.report(reportCanvas);
        reportCanvas.openList();
        Iterator<UsbDescriptorsInterfaceNode> it = this.mInterfaceNodes.iterator();
        while (it.hasNext()) {
            it.next().report(reportCanvas);
        }
        reportCanvas.closeList();
    }
}
