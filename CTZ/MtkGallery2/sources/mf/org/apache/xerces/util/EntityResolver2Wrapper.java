package mf.org.apache.xerces.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import mf.org.apache.xerces.impl.ExternalSubsetResolver;
import mf.org.apache.xerces.impl.XMLEntityDescription;
import mf.org.apache.xerces.xni.XMLResourceIdentifier;
import mf.org.apache.xerces.xni.XNIException;
import mf.org.apache.xerces.xni.grammars.XMLDTDDescription;
import mf.org.apache.xerces.xni.parser.XMLInputSource;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

public class EntityResolver2Wrapper implements ExternalSubsetResolver {
    protected EntityResolver2 fEntityResolver;

    public EntityResolver2Wrapper() {
    }

    public EntityResolver2Wrapper(EntityResolver2 entityResolver) {
        setEntityResolver(entityResolver);
    }

    public void setEntityResolver(EntityResolver2 entityResolver) {
        this.fEntityResolver = entityResolver;
    }

    public EntityResolver2 getEntityResolver() {
        return this.fEntityResolver;
    }

    @Override
    public XMLInputSource getExternalSubset(XMLDTDDescription grammarDescription) throws IOException, XNIException {
        if (this.fEntityResolver == null) {
            return null;
        }
        String name = grammarDescription.getRootName();
        String baseURI = grammarDescription.getBaseSystemId();
        try {
            InputSource inputSource = this.fEntityResolver.getExternalSubset(name, baseURI);
            if (inputSource != null) {
                return createXMLInputSource(inputSource, baseURI);
            }
            return null;
        } catch (SAXException e) {
            Exception ex = e.getException();
            if (ex == null) {
                ex = e;
            }
            throw new XNIException(ex);
        }
    }

    @Override
    public XMLInputSource resolveEntity(XMLResourceIdentifier resourceIdentifier) throws IOException, XNIException {
        if (this.fEntityResolver == null) {
            return null;
        }
        String pubId = resourceIdentifier.getPublicId();
        String sysId = resourceIdentifier.getLiteralSystemId();
        String baseURI = resourceIdentifier.getBaseSystemId();
        String name = null;
        if (resourceIdentifier instanceof XMLDTDDescription) {
            name = "[dtd]";
        } else if (resourceIdentifier instanceof XMLEntityDescription) {
            name = ((XMLEntityDescription) resourceIdentifier).getEntityName();
        }
        if (pubId == null && sysId == null) {
            return null;
        }
        try {
            InputSource inputSource = this.fEntityResolver.resolveEntity(name, pubId, baseURI, sysId);
            if (inputSource != null) {
                return createXMLInputSource(inputSource, baseURI);
            }
            return null;
        } catch (SAXException e) {
            Exception ex = e.getException();
            if (ex == null) {
                ex = e;
            }
            throw new XNIException(ex);
        }
    }

    private XMLInputSource createXMLInputSource(InputSource source, String baseURI) {
        String publicId = source.getPublicId();
        String systemId = source.getSystemId();
        InputStream byteStream = source.getByteStream();
        Reader charStream = source.getCharacterStream();
        String encoding = source.getEncoding();
        XMLInputSource xmlInputSource = new XMLInputSource(publicId, systemId, baseURI);
        xmlInputSource.setByteStream(byteStream);
        xmlInputSource.setCharacterStream(charStream);
        xmlInputSource.setEncoding(encoding);
        return xmlInputSource;
    }
}
