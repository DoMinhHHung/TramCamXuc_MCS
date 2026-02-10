package iuh.fit.se.serviceidentity.repository.httpclient;

import iuh.fit.se.serviceidentity.dto.response.FacebookUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "facebook-identity", url = "https://graph.facebook.com")
public interface FacebookIdentityClient {

    @GetMapping(value = "/me")
    FacebookUserResponse getUserInfo(
                                      @RequestParam("fields") String fields,
                                      @RequestParam("access_token") String accessToken
    );
}