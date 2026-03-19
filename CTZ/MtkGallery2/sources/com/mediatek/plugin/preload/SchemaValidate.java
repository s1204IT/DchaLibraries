package com.mediatek.plugin.preload;

import com.mediatek.plugin.utils.Log;
import com.mediatek.plugin.utils.TraceHelper;
import java.io.IOException;
import java.io.InputStream;
import mf.javax.xml.transform.stream.StreamSource;
import mf.javax.xml.validation.Schema;
import mf.javax.xml.validation.Validator;
import mf.org.apache.xerces.jaxp.validation.XMLSchemaFactory;
import org.xml.sax.SAXException;

public class SchemaValidate {
    private static final String TAG = "PluginManager/SchemaValidate";

    public boolean validateXMLFile(InputStream inputStream, InputStream inputStream2) {
        boolean z;
        TraceHelper.beginSection(">>>>SchemaValidate-validateXMLFile");
        try {
            TraceHelper.beginSection(">>>>SchemaValidate-validateXMLFile-new-StreamSource-xsd");
            StreamSource streamSource = new StreamSource(inputStream);
            TraceHelper.endSection();
            TraceHelper.beginSection(">>>>SchemaValidate-validateXMLFile-new-StreamSource-xml");
            StreamSource streamSource2 = new StreamSource(inputStream2);
            TraceHelper.endSection();
            TraceHelper.beginSection(">>>>SchemaValidate-validateXMLFile-new-XMLSchemaFactory");
            XMLSchemaFactory xMLSchemaFactory = new XMLSchemaFactory();
            TraceHelper.endSection();
            TraceHelper.beginSection(">>>>SchemaValidate-validateXMLFile-newSchema");
            Schema schemaNewSchema = xMLSchemaFactory.newSchema(streamSource);
            TraceHelper.endSection();
            TraceHelper.beginSection(">>>>SchemaValidate-validateXMLFile-new-newValidator");
            Validator validatorNewValidator = schemaNewSchema.newValidator();
            TraceHelper.endSection();
            TraceHelper.beginSection(">>>>SchemaValidate-validateXMLFile-validate");
            validatorNewValidator.validate(streamSource2);
            TraceHelper.endSection();
            z = true;
        } catch (IOException e) {
            Log.e(TAG, "<validateXMLFile> IOException: " + e);
            z = false;
        } catch (SAXException e2) {
            Log.e(TAG, "<validateXMLFile> SAXException: ", e2);
            z = false;
        }
        TraceHelper.endSection();
        return z;
    }
}
