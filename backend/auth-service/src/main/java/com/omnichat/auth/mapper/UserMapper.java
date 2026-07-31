package com.omnichat.auth.mapper;

import com.omnichat.auth.domain.entity.Role;
import com.omnichat.auth.domain.entity.User;
import com.omnichat.auth.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "teamName", source = "team.name")
    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRolesToStrings")
    UserDto toDto(User user);

    @Named("mapRolesToStrings")
    default Set<String> mapRolesToStrings(Set<Role> roles) {
        if (roles == null) return Set.of();
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
