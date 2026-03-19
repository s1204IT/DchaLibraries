package org.apache.http.protocol;

import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;

@Deprecated
public class ResponseDate implements HttpResponseInterceptor {
    private static final HttpDateGenerator DATE_GENERATOR = new HttpDateGenerator();

    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        if (response == null) {
            throw new IllegalArgumentException("HTTP response may not be null.");
        }
        int status = response.getStatusLine().getStatusCode();
        if (status < 200 || response.containsHeader(HTTP.DATE_HEADER)) {
            return;
        }
        String httpdate = DATE_GENERATOR.getCurrentDate();
        response.setHeader(HTTP.DATE_HEADER, httpdate);
    }
}
