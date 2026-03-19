package com.android.bluetooth.map;

import android.util.Log;
import android.util.Xml;
import com.android.bluetooth.DeviceWorkArounds;
import com.android.internal.util.FastXmlSerializer;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.xmlpull.v1.XmlSerializer;

public class BluetoothMapMessageListing {
    private static final boolean D = BluetoothMapService.DEBUG;
    private static final String TAG = "BluetoothMapMessageListing";
    private boolean mHasUnread = false;
    private List<BluetoothMapMessageListingElement> mList = new ArrayList();

    public void add(BluetoothMapMessageListingElement bluetoothMapMessageListingElement) {
        this.mList.add(bluetoothMapMessageListingElement);
        if (!bluetoothMapMessageListingElement.getReadBool()) {
            this.mHasUnread = true;
        }
    }

    public int getCount() {
        if (this.mList != null) {
            return this.mList.size();
        }
        return 0;
    }

    public boolean hasUnread() {
        return this.mHasUnread;
    }

    public List<BluetoothMapMessageListingElement> getList() {
        return this.mList;
    }

    public byte[] encode(boolean z, String str) throws UnsupportedEncodingException {
        XmlSerializer fastXmlSerializer;
        StringWriter stringWriter = new StringWriter();
        try {
            if (DeviceWorkArounds.addressStartsWith(BluetoothMapService.getRemoteDevice().getAddress(), DeviceWorkArounds.MERCEDES_BENZ_CARKIT)) {
                Log.d(TAG, "java_interop: Remote is Mercedes Benz, using Xml Workaround.");
                fastXmlSerializer = Xml.newSerializer();
                fastXmlSerializer.setOutput(stringWriter);
                fastXmlSerializer.text("\n");
            } else {
                fastXmlSerializer = new FastXmlSerializer();
                fastXmlSerializer.setOutput(stringWriter);
                fastXmlSerializer.startDocument("UTF-8", true);
                fastXmlSerializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            }
            fastXmlSerializer.startTag(null, "MAP-msg-listing");
            fastXmlSerializer.attribute(null, "version", str);
            Iterator<BluetoothMapMessageListingElement> it = this.mList.iterator();
            while (it.hasNext()) {
                it.next().encode(fastXmlSerializer, z);
            }
            fastXmlSerializer.endTag(null, "MAP-msg-listing");
            fastXmlSerializer.endDocument();
        } catch (IOException e) {
            Log.w(TAG, e);
        } catch (IllegalArgumentException e2) {
            Log.w(TAG, e2);
        } catch (IllegalStateException e3) {
            Log.w(TAG, e3);
        }
        if (DeviceWorkArounds.addressStartsWith(BluetoothMapService.getRemoteDevice().getAddress(), DeviceWorkArounds.BREZZA_ZDI_CARKIT)) {
            return stringWriter.toString().replaceAll("&amp;", "&").replaceAll("&lt;", "<").replaceAll("&gt;", ">").getBytes("UTF-8");
        }
        return stringWriter.toString().getBytes("UTF-8");
    }

    public void sort() {
        Collections.sort(this.mList);
    }

    public void segment(int i, int i2) {
        int iMin = Math.min(i, this.mList.size() - i2);
        if (iMin > 0) {
            this.mList = this.mList.subList(i2, iMin + i2);
            if (this.mList == null) {
                this.mList = new ArrayList();
                return;
            }
            return;
        }
        if (i2 > this.mList.size()) {
            this.mList = new ArrayList();
            Log.d(TAG, "offset greater than list size. Returning empty list");
        } else {
            this.mList = this.mList.subList(i2, this.mList.size());
        }
    }
}
