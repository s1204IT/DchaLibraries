package com.android.server.usb.descriptors.tree;

import com.android.server.usb.descriptors.UsbACInterface;
import com.android.server.usb.descriptors.UsbConfigDescriptor;
import com.android.server.usb.descriptors.UsbDescriptor;
import com.android.server.usb.descriptors.UsbDescriptorParser;
import com.android.server.usb.descriptors.UsbDeviceDescriptor;
import com.android.server.usb.descriptors.UsbEndpointDescriptor;
import com.android.server.usb.descriptors.UsbInterfaceDescriptor;
import com.android.server.usb.descriptors.report.ReportCanvas;
import java.util.ArrayList;

public final class UsbDescriptorsTree {
    private static final String TAG = "UsbDescriptorsTree";
    private UsbDescriptorsConfigNode mConfigNode;
    private UsbDescriptorsDeviceNode mDeviceNode;
    private UsbDescriptorsInterfaceNode mInterfaceNode;

    private void addDeviceDescriptor(UsbDeviceDescriptor usbDeviceDescriptor) {
        this.mDeviceNode = new UsbDescriptorsDeviceNode(usbDeviceDescriptor);
    }

    private void addConfigDescriptor(UsbConfigDescriptor usbConfigDescriptor) {
        this.mConfigNode = new UsbDescriptorsConfigNode(usbConfigDescriptor);
        this.mDeviceNode.addConfigDescriptorNode(this.mConfigNode);
    }

    private void addInterfaceDescriptor(UsbInterfaceDescriptor usbInterfaceDescriptor) {
        this.mInterfaceNode = new UsbDescriptorsInterfaceNode(usbInterfaceDescriptor);
        this.mConfigNode.addInterfaceNode(this.mInterfaceNode);
    }

    private void addEndpointDescriptor(UsbEndpointDescriptor usbEndpointDescriptor) {
        this.mInterfaceNode.addEndpointNode(new UsbDescriptorsEndpointNode(usbEndpointDescriptor));
    }

    private void addACInterface(UsbACInterface usbACInterface) {
        this.mInterfaceNode.addACInterfaceNode(new UsbDescriptorsACInterfaceNode(usbACInterface));
    }

    public void parse(UsbDescriptorParser usbDescriptorParser) {
        ArrayList<UsbDescriptor> descriptors = usbDescriptorParser.getDescriptors();
        for (int i = 0; i < descriptors.size(); i++) {
            UsbDescriptor usbDescriptor = descriptors.get(i);
            switch (usbDescriptor.getType()) {
                case 1:
                    addDeviceDescriptor((UsbDeviceDescriptor) usbDescriptor);
                    break;
                case 2:
                    addConfigDescriptor((UsbConfigDescriptor) usbDescriptor);
                    break;
                case 4:
                    addInterfaceDescriptor((UsbInterfaceDescriptor) usbDescriptor);
                    break;
                case 5:
                    addEndpointDescriptor((UsbEndpointDescriptor) usbDescriptor);
                    break;
                case 36:
                    addACInterface((UsbACInterface) usbDescriptor);
                    break;
            }
        }
    }

    public void report(ReportCanvas reportCanvas) {
        this.mDeviceNode.report(reportCanvas);
    }
}
