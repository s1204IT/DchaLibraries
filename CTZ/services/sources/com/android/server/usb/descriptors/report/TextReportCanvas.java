package com.android.server.usb.descriptors.report;

import com.android.server.usb.descriptors.UsbDescriptorParser;

public final class TextReportCanvas extends ReportCanvas {
    private static final int LIST_INDENT_AMNT = 2;
    private static final String TAG = "TextReportCanvas";
    private int mListIndent;
    private final StringBuilder mStringBuilder;

    public TextReportCanvas(UsbDescriptorParser usbDescriptorParser, StringBuilder sb) {
        super(usbDescriptorParser);
        this.mStringBuilder = sb;
    }

    private void writeListIndent() {
        for (int i = 0; i < this.mListIndent; i++) {
            this.mStringBuilder.append(" ");
        }
    }

    @Override
    public void write(String str) {
        this.mStringBuilder.append(str);
    }

    @Override
    public void openHeader(int i) {
        writeListIndent();
        this.mStringBuilder.append("[");
    }

    @Override
    public void closeHeader(int i) {
        this.mStringBuilder.append("]\n");
    }

    @Override
    public void openParagraph(boolean z) {
        writeListIndent();
    }

    @Override
    public void closeParagraph() {
        this.mStringBuilder.append("\n");
    }

    @Override
    public void writeParagraph(String str, boolean z) {
        openParagraph(z);
        if (z) {
            this.mStringBuilder.append("*" + str + "*");
        } else {
            this.mStringBuilder.append(str);
        }
        closeParagraph();
    }

    @Override
    public void openList() {
        this.mListIndent += 2;
    }

    @Override
    public void closeList() {
        this.mListIndent -= 2;
    }

    @Override
    public void openListItem() {
        writeListIndent();
        this.mStringBuilder.append("- ");
    }

    @Override
    public void closeListItem() {
        this.mStringBuilder.append("\n");
    }
}
