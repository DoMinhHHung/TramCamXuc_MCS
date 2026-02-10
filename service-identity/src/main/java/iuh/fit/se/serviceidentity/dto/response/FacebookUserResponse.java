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

    / **
 * Gets the Facebook user's identifier.
 *
 * @return the user's Facebook ID
 */
public String getId() { return id; }
    /**
 * Get the user's email address as provided by Facebook.
 *
 * @return the user's email address, or null if not present
 */
public String getEmail() { return email; }
    /**
 * Gets the user's first name.
 *
 * @return the user's first name, or null if not set
 */
public String getFirstName() { return firstName; }
    /**
 * Gets the user's last name.
 *
 * @return the last name, or null if not set
 */
public String getLastName() { return lastName; }
    /**
 * Retrieves the nested picture metadata from the Facebook response.
 *
 * @return the {@code PictureData} containing the picture's data (URL, height, width), or {@code null} if not present
 */
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