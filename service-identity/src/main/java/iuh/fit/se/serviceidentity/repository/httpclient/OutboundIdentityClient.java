package iuh.fit.se.serviceidentity.repository.httpclient;

import iuh.fit.se.serviceidentity.dto.response.OutboundUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "outbound-identity", url = "https://www.googleapis.com")
public interface OutboundIdentityClient {

    /**
     * Retrieve the authenticated user's profile information from Google's OAuth2 userinfo endpoint.
     *
     * @param accessToken the OAuth2 access token used to authorize the request
     * @return an OutboundUserResponse containing the user's profile information
     */
    @GetMapping(value = "/oauth2/v3/userinfo")
    OutboundUserResponse getUserInfo(@RequestParam("access_token") String accessToken);
}