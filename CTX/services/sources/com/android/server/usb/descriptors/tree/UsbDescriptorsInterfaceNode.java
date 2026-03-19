package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbInterfaceDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;
import java.util.Iterator;

public final class UsbDescriptorsInterfaceNode extends UsbDescriptorsTreeNode {
    private static final String TAG = "UsbDescriptorsInterfaceNode";
    private final UsbInterfaceDescriptor mInterfaceDescriptor;
    private final ArrayList<UsbDescriptorsEndpointNode> mEndpointNodes = new ArrayList<>();
    private final ArrayList<UsbDescriptorsACInterfaceNode> mACInterfaceNodes = new ArrayList<>();

    public UsbDescriptorsInterfaceNode(UsbInterfaceDescriptor usbInterfaceDescriptor) {
        this.mInterfaceDescriptor = usbInterfaceDescriptor;
    }

    public void addEndpointNode(UsbDescriptorsEndpointNode usbDescriptorsEndpointNode) {
        this.mEndpointNodes.add(usbDescriptorsEndpointNode);
    }

    public void addACInterfaceNode(UsbDescriptorsACInterfaceNode usbDescriptorsACInterfaceNode) {
        this.mACInterfaceNodes.add(usbDescriptorsACInterfaceNode);
    }

    @Override
    public void report(ReportCanvas reportCanvas) {
        this.mInterfaceDescriptor.report(reportCanvas);
        if (this.mACInterfaceNodes.size() > 0) {
            reportCanvas.writeParagraph("Audio Class Interfaces", false);
            reportCanvas.openList();
            Iterator<UsbDescriptorsACInterfaceNode> it = this.mACInterfaceNodes.iterator();
            while (it.hasNext()) {
                it.next().report(reportCanvas);
            }
            reportCanvas.closeList();
        }
        if (this.mEndpointNodes.size() > 0) {
            reportCanvas.writeParagraph("Endpoints", false);
            reportCanvas.openList();
            Iterator<UsbDescriptorsEndpointNode> it2 = this.mEndpointNodes.iterator();
            while (it2.hasNext()) {
                it2.next().report(reportCanvas);
            }
            reportCanvas.closeList();
        }
    }
}
