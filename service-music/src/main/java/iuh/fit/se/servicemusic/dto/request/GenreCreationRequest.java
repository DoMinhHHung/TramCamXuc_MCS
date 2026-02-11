package iuh.fit.se.servicemusic.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenreCreationRequest {
    @NotBlank(message = "Tên thể loại không được để trống")
    private String name;
    @NotBlank(message = "Key thể loại không được để trống")
    private String key;
}
