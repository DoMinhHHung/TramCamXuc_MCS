package iuh.fit.se.serviceidentity.repository.httpclient;

import iuh.fit.se.serviceidentity.dto.response.OutboundUserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "outbound-identity", url = "https://www.googleapis.com")
public interface OutboundIdentityClient {

    @GetMapping(value = "/oauth2/v3/userinfo")
    OutboundUserResponse getUserInfo(@RequestParam("access_token") String accessToken);
}