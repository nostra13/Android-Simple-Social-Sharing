/* Copyright (c) 2009 Matthias Kaeppler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nostra13.socialsharing.twitter.extpack.oauth.signpost.signature;

import java.io.IOException;
import java.io.Serializable;

import com.nostra13.socialsharing.twitter.extpack.lgpl.haustein.Base64Encoder;
import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.exception.OAuthMessageSignerException;
import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.http.HttpParameters;
import com.nostra13.socialsharing.twitter.extpack.oauth.signpost.http.HttpRequest;



public abstract class OAuthMessageSigner implements Serializable {

    private static final long serialVersionUID = 4445779788786131202L;

    private String consumerSecret;

    private String tokenSecret;

    public abstract String sign(HttpRequest request, HttpParameters requestParameters)
            throws OAuthMessageSignerException;

    public abstract String getSignatureMethod();

    public String getConsumerSecret() {
        return consumerSecret;
    }

    public String getTokenSecret() {
        return tokenSecret;
    }

    public void setConsumerSecret(String consumerSecret) {
        this.consumerSecret = consumerSecret;
    }

    public void setTokenSecret(String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    protected byte[] decodeBase64(String s) {
        return Base64Encoder.decode(s);
    }

    protected String base64Encode(byte[] b) {
        return new String(Base64Encoder.encode(b));
    }

    private void readObject(java.io.ObjectInputStream stream)
            throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
    }
}
