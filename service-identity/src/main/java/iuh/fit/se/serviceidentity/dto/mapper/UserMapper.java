package iuh.fit.se.serviceidentity.dto.mapper;

import iuh.fit.se.serviceidentity.dto.request.UserCreationRequest;
import iuh.fit.se.serviceidentity.dto.response.UserResponse;
import iuh.fit.se.serviceidentity.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "provider", ignore = true)
    @Mapping(target = "providerId", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(UserCreationRequest request);

    UserResponse toUserResponse(User user);
}