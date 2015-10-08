/*
*  Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/
package org.wso2.carbon.identity.authenticator.backend.oauth.validator.impl;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.httpclient.Header;
import org.wso2.carbon.identity.oauth2.stub.OAuth2TokenValidationServiceStub;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuth2ClientApplicationDTO;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuth2TokenValidationRequestDTO_OAuth2AccessToken;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuth2TokenValidationRequestDTO_TokenValidationContextParam;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import org.wso2.carbon.identity.authenticator.backend.oauth.OauthAuthenticatorConstants;
import org.wso2.carbon.identity.authenticator.backend.oauth.validator.OAuth2TokenValidator;
import org.wso2.carbon.identity.authenticator.backend.oauth.validator.OAuthValidationRespond;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles the Authentication form external IDP servers.
 * Currently only supports WSO2 IS.
 * External IDP support is planned for future.
 */
public class ExternalOAuthValidator implements OAuth2TokenValidator{
    protected String hostURL ;

    public ExternalOAuthValidator(String hostURL) {
        this.hostURL = hostURL;
    }
    /**
     * This method gets a string accessToken and validates it and generate the OAuth2ClientApplicationDTO
     * containing the validity and user details if valid.
     *
     * @param token which need to be validated.
     * @return OAuthValidationRespond with the validated results.
     */
    public OAuthValidationRespond validateToken(String token) throws RemoteException {

        // create an OAuth token validating request DTO
        OAuth2TokenValidationRequestDTO validationRequest = new OAuth2TokenValidationRequestDTO();

        // create access token object to validate and populate it
        OAuth2TokenValidationRequestDTO_OAuth2AccessToken accessToken =
                new OAuth2TokenValidationRequestDTO_OAuth2AccessToken();
        accessToken.setTokenType(OauthAuthenticatorConstants.BEARER_TOKEN_TYPE);
        accessToken.setIdentifier(token);
        OAuth2TokenValidationRequestDTO_TokenValidationContextParam tokenValidationContextParam[] =
                new OAuth2TokenValidationRequestDTO_TokenValidationContextParam[1];
        validationRequest.setContext(tokenValidationContextParam);

        //set the token to the validation request
        validationRequest.setAccessToken(accessToken);
        OAuth2TokenValidationServiceStub validationService =
                new OAuth2TokenValidationServiceStub(hostURL);
        ServiceClient client = validationService._getServiceClient();
        Options options = client.getOptions();
        List<Header> list = new ArrayList<>();
        Header header = new Header();
        header.setName(HTTPConstants.HEADER_AUTHORIZATION);
        header.setValue(OauthAuthenticatorConstants.AUTHORIZATION_HEADER_PREFIX_BEARER+ " " + token);
        list.add(header);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.HTTP_HEADERS, list);
        client.setOptions(options);
        OAuth2ClientApplicationDTO respond =
                validationService.findOAuthConsumerIfTokenIsValid(validationRequest);
        boolean isValid = respond.getAccessTokenValidationResponse().getValid();
        String userName = null;
        String tenantDomain = null;

        if(isValid){
            userName = MultitenantUtils.getTenantAwareUsername(
                    respond.getAccessTokenValidationResponse().getAuthorizedUser());
            tenantDomain =
                    MultitenantUtils.getTenantDomain(respond.getAccessTokenValidationResponse().getAuthorizedUser());
        }

        return new OAuthValidationRespond(userName,tenantDomain,isValid);
    }
}
