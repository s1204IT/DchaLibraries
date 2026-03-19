package com.android.server.usb.descriptors.report;

import com.android.server.usb.descriptors.UsbDescriptorParser;

public final class HTMLReportCanvas extends ReportCanvas {
    private static final String TAG = "HTMLReportCanvas";
    private final StringBuilder mStringBuilder;

    public HTMLReportCanvas(UsbDescriptorParser usbDescriptorParser, StringBuilder sb) {
        super(usbDescriptorParser);
        this.mStringBuilder = sb;
    }

    @Override
    public void write(String str) {
        this.mStringBuilder.append(str);
    }

    @Override
    public void openHeader(int i) {
        StringBuilder sb = this.mStringBuilder;
        sb.append("<h");
        sb.append(i);
        sb.append('>');
    }

    @Override
    public void closeHeader(int i) {
        StringBuilder sb = this.mStringBuilder;
        sb.append("</h");
        sb.append(i);
        sb.append('>');
    }

    @Override
    public void openParagraph(boolean z) {
        if (z) {
            this.mStringBuilder.append("<p style=\"color:red\">");
        } else {
            this.mStringBuilder.append("<p>");
        }
    }

    @Override
    public void closeParagraph() {
        this.mStringBuilder.append("</p>");
    }

    @Override
    public void writeParagraph(String str, boolean z) {
        openParagraph(z);
        this.mStringBuilder.append(str);
        closeParagraph();
    }

    @Override
    public void openList() {
        this.mStringBuilder.append("<ul>");
    }

    @Override
    public void closeList() {
        this.mStringBuilder.append("</ul>");
    }

    @Override
    public void openListItem() {
        this.mStringBuilder.append("<li>");
    }

    @Override
    public void closeListItem() {
        this.mStringBuilder.append("</li>");
    }
}
