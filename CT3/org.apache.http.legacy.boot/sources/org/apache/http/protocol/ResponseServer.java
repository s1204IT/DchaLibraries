package org.apache.http.protocol;

import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.params.CoreProtocolPNames;

@Deprecated
public class ResponseServer implements HttpResponseInterceptor {
    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {
        String s;
        if (response == null) {
            throw new IllegalArgumentException("HTTP request may not be null");
        }
        if (response.containsHeader(HTTP.SERVER_HEADER) || (s = (String) response.getParams().getParameter(CoreProtocolPNames.ORIGIN_SERVER)) == null) {
            return;
        }
        response.addHeader(HTTP.SERVER_HEADER, s);
    }
}
