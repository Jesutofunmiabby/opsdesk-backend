package com.opsdesk.opsdeskbackend.user;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opsdesk.opsdeskbackend.common.PageResponse;
import com.opsdesk.opsdeskbackend.common.QueryParams;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> list(
            @RequestParam(required = false) String page,
            @RequestParam(required = false) String size) {
        QueryParams params = new QueryParams();
        int pageNumber = params.page(page);
        int pageSize = params.size(size);
        params.throwIfInvalid();
        return userService.findPage(pageNumber, pageSize).map(UserResponse::from);
    }
}
