package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbDeviceDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;
import java.util.Iterator;

public final class UsbDescriptorsDeviceNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsDeviceNode";
    private final ArrayList<UsbDescriptorsConfigNode> mConfigNodes = new ArrayList<>();
    private final UsbDeviceDescriptor mDeviceDescriptor;

    public UsbDescriptorsDeviceNode(UsbDeviceDescriptor usbDeviceDescriptor) {
        this.mDeviceDescriptor = usbDeviceDescriptor;
    }

    public void addConfigDescriptorNode(UsbDescriptorsConfigNode usbDescriptorsConfigNode) {
        this.mConfigNodes.add(usbDescriptorsConfigNode);
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        this.mDeviceDescriptor.report(reportCanvas);
        Iterator<UsbDescriptorsConfigNode> it = this.mConfigNodes.iterator();
        while (it.hasNext()) {
            it.next().report(reportCanvas);
        }
    }
}
