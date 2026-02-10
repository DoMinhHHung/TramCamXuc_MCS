package iuh.fit.se.serviceidentity.dto.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeTokenRequest {
    String token;
}