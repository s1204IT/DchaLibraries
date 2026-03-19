package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbEndpointDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;

public final class UsbDescriptorsEndpointNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsEndpointNode";
    private final UsbEndpointDescriptor mEndpointDescriptor;

    public UsbDescriptorsEndpointNode(UsbEndpointDescriptor usbEndpointDescriptor) {
        this.mEndpointDescriptor = usbEndpointDescriptor;
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        this.mEndpointDescriptor.report(reportCanvas);
    }
}
