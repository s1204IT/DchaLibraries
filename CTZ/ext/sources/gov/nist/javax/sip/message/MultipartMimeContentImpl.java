package gov.nist.javax.sip.message;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.HeaderFactoryImpl;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.sip.header.ContentDispositionHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.Header;

public class MultipartMimeContentImpl implements MultipartMimeContent {
    public static String BOUNDARY = "boundary";
    private String boundary;
    private List<Content> contentList = new LinkedList();
    private ContentTypeHeader multipartMimeContentTypeHeader;

    public MultipartMimeContentImpl(ContentTypeHeader contentTypeHeader) {
        this.multipartMimeContentTypeHeader = contentTypeHeader;
        this.boundary = contentTypeHeader.getParameter(BOUNDARY);
    }

    @Override
    public boolean add(Content content) {
        return this.contentList.add((ContentImpl) content);
    }

    @Override
    public ContentTypeHeader getContentTypeHeader() {
        return this.multipartMimeContentTypeHeader;
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        Iterator<Content> it = this.contentList.iterator();
        while (it.hasNext()) {
            stringBuffer.append(it.next().toString());
        }
        return stringBuffer.toString();
    }

    public void createContentList(String str) throws ParseException {
        try {
            HeaderFactoryImpl headerFactoryImpl = new HeaderFactoryImpl();
            String parameter = getContentTypeHeader().getParameter(BOUNDARY);
            if (parameter == null) {
                this.contentList = new LinkedList();
                ContentImpl contentImpl = new ContentImpl(str, parameter);
                contentImpl.setContentTypeHeader(getContentTypeHeader());
                this.contentList.add(contentImpl);
                return;
            }
            String[] strArrSplit = str.split("--" + parameter + Separators.NEWLINE);
            int length = strArrSplit.length;
            for (int i = 0; i < length; i++) {
                String str2 = strArrSplit[i];
                if (str2 == null) {
                    return;
                }
                StringBuffer stringBuffer = new StringBuffer(str2);
                while (stringBuffer.length() > 0 && (stringBuffer.charAt(0) == '\r' || stringBuffer.charAt(0) == '\n')) {
                    stringBuffer.deleteCharAt(0);
                }
                if (stringBuffer.length() != 0) {
                    String string = stringBuffer.toString();
                    int iIndexOf = string.indexOf("\r\n\r\n");
                    int i2 = 4;
                    if (iIndexOf == -1) {
                        iIndexOf = string.indexOf(Separators.RETURN);
                        i2 = 2;
                    }
                    if (iIndexOf == -1) {
                        throw new ParseException("no content type header found in " + string, 0);
                    }
                    String strSubstring = string.substring(i2 + iIndexOf);
                    if (strSubstring == null) {
                        throw new ParseException("No content [" + string + "]", 0);
                    }
                    String strSubstring2 = string.substring(0, iIndexOf);
                    ContentImpl contentImpl2 = new ContentImpl(strSubstring, this.boundary);
                    for (String str3 : strSubstring2.split(Separators.NEWLINE)) {
                        Header headerCreateHeader = headerFactoryImpl.createHeader(str3);
                        if (headerCreateHeader instanceof ContentTypeHeader) {
                            contentImpl2.setContentTypeHeader((ContentTypeHeader) headerCreateHeader);
                        } else if (headerCreateHeader instanceof ContentDispositionHeader) {
                            contentImpl2.setContentDispositionHeader((ContentDispositionHeader) headerCreateHeader);
                        } else {
                            throw new ParseException("Unexpected header type " + headerCreateHeader.getName(), 0);
                        }
                        this.contentList.add(contentImpl2);
                    }
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ParseException("Invalid Multipart mime format", 0);
        }
    }

    public Content getContentByType(String str, String str2) {
        if (this.contentList == null) {
            return null;
        }
        for (Content content : this.contentList) {
            if (content.getContentTypeHeader().getContentType().equalsIgnoreCase(str) && content.getContentTypeHeader().getContentSubType().equalsIgnoreCase(str2)) {
                return content;
            }
        }
        return null;
    }

    @Override
    public void addContent(Content content) {
        add(content);
    }

    @Override
    public Iterator<Content> getContents() {
        return this.contentList.iterator();
    }

    @Override
    public int getContentCount() {
        return this.contentList.size();
    }
}
