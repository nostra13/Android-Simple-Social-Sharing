package com.nostra13.socialsharing.twitter.extpack.oauth.signpost.commonshttp;

import java.io.IOException;
import java.io.InputStream;

import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.http.HttpResponse;


public class HttpResponseAdapter implements HttpResponse {

    private org.apache.http.HttpResponse response;

    public HttpResponseAdapter(org.apache.http.HttpResponse response) {
        this.response = response;
    }

    public InputStream getContent() throws IOException {
        return response.getEntity().getContent();
    }

    public int getStatusCode() throws IOException {
        return response.getStatusLine().getStatusCode();
    }

    public String getReasonPhrase() throws Exception {
        return response.getStatusLine().getReasonPhrase();
    }

    public Object unwrap() {
        return response;
    }
}
