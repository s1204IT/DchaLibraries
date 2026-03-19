package org.apache.http.protocol;

import java.io.IOException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;

@Deprecated
public class RequestDate implements HttpRequestInterceptor {
    private static final HttpDateGenerator DATE_GENERATOR = new HttpDateGenerator();

    @Override
    public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
        if (request == null) {
            throw new IllegalArgumentException("HTTP request may not be null.");
        }
        if (!(request instanceof HttpEntityEnclosingRequest) || request.containsHeader(HTTP.DATE_HEADER)) {
            return;
        }
        String httpdate = DATE_GENERATOR.getCurrentDate();
        request.setHeader(HTTP.DATE_HEADER, httpdate);
    }
}
