package iuh.fit.se.servicepayment.entity.json;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanFeatures implements Serializable {
    private String quality;
    private boolean isArtist;
    private boolean noAds;
    private boolean offlineMode;
}