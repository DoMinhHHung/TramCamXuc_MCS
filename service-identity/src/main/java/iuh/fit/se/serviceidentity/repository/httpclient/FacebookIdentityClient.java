package iuh.fit.se.serviceidentity.repository.httpclient;

import iuh.fit.se.serviceidentity.dto.response.FacebookUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "facebook-identity", url = "https://graph.facebook.com")
public interface FacebookIdentityClient {

    /**
     * Fetches Facebook user profile information for the given access token and field list.
     *
     * @param fields      comma-separated list of profile fields to include in the response (e.g. "id,name,email")
     * @param accessToken Facebook access token used to authenticate the request
     * @return            a FacebookUserResponse containing the requested user fields
     */
    @GetMapping(value = "/me")
    FacebookUserResponse getUserInfo(
                                      @RequestParam("fields") String fields,
                                      @RequestParam("access_token") String accessToken
    );
}