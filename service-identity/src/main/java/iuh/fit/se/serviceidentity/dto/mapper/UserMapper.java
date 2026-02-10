package iuh.fit.se.serviceidentity.dto.mapper;

import iuh.fit.se.serviceidentity.dto.request.UserCreationRequest;
import iuh.fit.se.serviceidentity.dto.response.UserResponse;
import iuh.fit.se.serviceidentity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    /**
     * Create a User entity from a UserCreationRequest.
     *
     * The returned User is populated from the request; the fields id, role, status,
     * provider, providerId, avatarUrl, createdAt, and updatedAt are not set by this mapping.
     *
     * @param request the source DTO containing user-provided creation data
     * @return the User entity populated from the request with ignored fields left unset
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(UserCreationRequest request);

    /**
 * Converts a User entity to a UserResponse DTO.
 *
 * @param user the source User entity to map
 * @return the mapped UserResponse DTO
 */
UserResponse toUserResponse(User user);
}