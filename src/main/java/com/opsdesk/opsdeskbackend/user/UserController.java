package com.opsdesk.opsdeskbackend.user;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opsdesk.opsdeskbackend.common.ErrorResponse;
import com.opsdesk.opsdeskbackend.common.PageResponse;
import com.opsdesk.opsdeskbackend.common.QueryParams;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "List users", description = "ADMIN and MANAGER only.")
    @ApiResponse(responseCode = "200", description = "One page of users, oldest first.")
    @ApiResponse(responseCode = "400", description = "page or size is out of range.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "The caller is an AGENT or a USER.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public PageResponse<UserResponse> list(
            @Parameter(description = "The page, counting from 0.",
                    schema = @Schema(type = "integer", minimum = "0", defaultValue = "0"))
            @RequestParam(required = false) String page,
            @Parameter(description = "How many users on a page.",
                    schema = @Schema(type = "integer", minimum = "1", maximum = "100", defaultValue = "20"))
            @RequestParam(required = false) String size) {
        QueryParams params = new QueryParams();
        int pageNumber = params.page(page);
        int pageSize = params.size(size);
        params.throwIfInvalid();
        return userService.findPage(pageNumber, pageSize).map(UserResponse::from);
    }
}
