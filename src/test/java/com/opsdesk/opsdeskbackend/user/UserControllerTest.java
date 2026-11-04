package com.opsdesk.opsdeskbackend.user;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.opsdesk.opsdeskbackend.common.PageResponse;
import com.opsdesk.opsdeskbackend.security.JsonAuthenticationEntryPoint;
import com.opsdesk.opsdeskbackend.security.JwtService;
import com.opsdesk.opsdeskbackend.security.SecurityConfig;
import com.opsdesk.opsdeskbackend.security.WithAuthUser;

// The "List users" row of the permission table.
@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtService.class, JsonAuthenticationEntryPoint.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    @WithAuthUser(role = UserRole.MANAGER)
    void aManagerCanListUsers() throws Exception {
        when(userService.findPage(anyInt(), anyInt())).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk());
    }

    @Test
    @WithAuthUser(role = UserRole.AGENT)
    void anAgentCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
        verify(userService, never()).findPage(anyInt(), anyInt());
    }

    @Test
    @WithAuthUser(role = UserRole.USER)
    void aUserCannotListUsers() throws Exception {
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isForbidden());
    }
}
