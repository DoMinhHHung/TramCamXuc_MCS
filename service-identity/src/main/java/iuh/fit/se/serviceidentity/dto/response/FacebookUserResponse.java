package iuh.fit.se.serviceidentity.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FacebookUserResponse {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private PictureData picture;

    public String getId() { return id; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public PictureData getPicture() { return picture; }
    // ------------------------------------------------

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PictureData {
        Data data;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {
        String url;
        int height;
        int width;
    }
}