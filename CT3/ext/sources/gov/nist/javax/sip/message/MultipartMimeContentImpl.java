package gov.nist.javax.sip.message;

import gov.nist.core.Separators;
import gov.nist.javax.sip.header.HeaderFactoryExt;
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
        for (Content content : this.contentList) {
            stringBuffer.append(content.toString());
        }
        return stringBuffer.toString();
    }

    public void createContentList(String body) throws ParseException {
        String nextPart;
        try {
            HeaderFactoryExt headerFactory = new HeaderFactoryImpl();
            String delimiter = getContentTypeHeader().getParameter(BOUNDARY);
            if (delimiter == null) {
                this.contentList = new LinkedList();
                ContentImpl content = new ContentImpl(body, delimiter);
                content.setContentTypeHeader(getContentTypeHeader());
                this.contentList.add(content);
                return;
            }
            String[] fragments = body.split("--" + delimiter + Separators.NEWLINE);
            int length = fragments.length;
            for (int i = 0; i < length && (nextPart = fragments[i]) != null; i++) {
                StringBuffer strbuf = new StringBuffer(nextPart);
                while (strbuf.length() > 0 && (strbuf.charAt(0) == '\r' || strbuf.charAt(0) == '\n')) {
                    strbuf.deleteCharAt(0);
                }
                if (strbuf.length() != 0) {
                    String nextPart2 = strbuf.toString();
                    int position = nextPart2.indexOf("\r\n\r\n");
                    int off = 4;
                    if (position == -1) {
                        position = nextPart2.indexOf(Separators.RETURN);
                        off = 2;
                    }
                    if (position == -1) {
                        throw new ParseException("no content type header found in " + nextPart2, 0);
                    }
                    String rest = nextPart2.substring(position + off);
                    if (rest == null) {
                        throw new ParseException("No content [" + nextPart2 + "]", 0);
                    }
                    String headers = nextPart2.substring(0, position);
                    ContentImpl content2 = new ContentImpl(rest, this.boundary);
                    String[] headerArray = headers.split(Separators.NEWLINE);
                    for (String hdr : headerArray) {
                        Header header = headerFactory.createHeader(hdr);
                        if (header instanceof ContentTypeHeader) {
                            content2.setContentTypeHeader((ContentTypeHeader) header);
                        } else if (header instanceof ContentDispositionHeader) {
                            content2.setContentDispositionHeader((ContentDispositionHeader) header);
                        } else {
                            throw new ParseException("Unexpected header type " + header.getName(), 0);
                        }
                        this.contentList.add(content2);
                    }
                }
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ParseException("Invalid Multipart mime format", 0);
        }
    }

    public Content getContentByType(String contentType, String contentSubtype) {
        if (this.contentList == null) {
            return null;
        }
        for (Content content : this.contentList) {
            if (content.getContentTypeHeader().getContentType().equalsIgnoreCase(contentType) && content.getContentTypeHeader().getContentSubType().equalsIgnoreCase(contentSubtype)) {
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
