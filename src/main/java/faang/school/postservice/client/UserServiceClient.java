package faang.school.postservice.client;

import faang.school.postservice.dto.user.UserDto;
import faang.school.postservice.util.SimplePage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "user-service", url = "${user-service.host}:${user-service.port}")
public interface UserServiceClient {

    @GetMapping("/api/v1/users/{userId}/internal")
    UserDto getUserInternal(@PathVariable long userId);

    @PostMapping("/api/v1/users")
    List<UserDto> getUsersByIds(@RequestBody List<Long> ids);

    @GetMapping("/api/v1/users/{userId}")
    UserDto getUser(@RequestHeader("x-user-id")Long currentUserId, @PathVariable long userId);

    @GetMapping("/api/v1/users")
    SimplePage<UserDto> getAllUsers(Pageable pageable);
}