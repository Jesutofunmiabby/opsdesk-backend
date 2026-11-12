package com.opsdesk.opsdeskbackend;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// The API documentation and the Swagger page open without a login, describe
// the bearer token, and say the same as API_CONTRACT.md on the points that are
// easy to get wrong: status codes, required and nullable fields, and limits.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiDocsTest {

    private static final String PATHS = "$.paths['/api/v1/";
    private static final String SCHEMAS = "$.components.schemas.";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void apiDocsNeedNoLoginAndDescribeTheBearerToken() throws Exception {
        String json = docs()
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.security[0].bearerAuth").exists())
                // Register and login turn the token off for themselves.
                .andExpect(jsonPath(PATHS + "auth/register'].post.security").isEmpty())
                .andExpect(jsonPath(PATHS + "auth/login'].post.security").isEmpty())
                .andExpect(jsonPath(PATHS + "auth/me'].get.security").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        // A copy to read or compare with the contract: target/openapi.json.
        Files.writeString(Path.of("target", "openapi.json"), json);
    }

    @Test
    void statusCodesMatchTheContract() throws Exception {
        docs()
                .andExpect(jsonPath(PATHS + "tickets'].post.responses.201.headers.Location").exists())
                .andExpect(jsonPath(PATHS + "tickets'].post.responses.200").doesNotExist())
                .andExpect(jsonPath(PATHS + "auth/register'].post.responses.201").exists())
                .andExpect(jsonPath(PATHS + "auth/register'].post.responses.409").exists())
                .andExpect(jsonPath(PATHS + "tickets/{id}'].delete.responses.204").exists())
                .andExpect(jsonPath(PATHS + "tickets/{id}'].delete.responses.403").exists())
                .andExpect(jsonPath(PATHS + "tickets/{id}'].get.responses.401.content['application/json']").exists())
                // Register and login need no token, so they never answer
                // "Log in to continue." (login's 401 is a wrong password).
                .andExpect(jsonPath(PATHS + "auth/register'].post.responses.401").doesNotExist());
    }

    @Test
    void listParametersAreTypedAsTheContractSays() throws Exception {
        String list = PATHS + "tickets'].get.parameters";
        docs()
                .andExpect(jsonPath(list + "[?(@.name == 'page')].schema.type", contains("integer")))
                .andExpect(jsonPath(list + "[?(@.name == 'size')].schema.maximum", contains(100)))
                .andExpect(jsonPath(list + "[?(@.name == 'status')].schema.enum[*]",
                        containsInAnyOrder("OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED")));
    }

    @Test
    void schemasMatchTheContract() throws Exception {
        docs()
                .andExpect(jsonPath(SCHEMAS + "CreateTicketRequest.properties.title.minLength").value(3))
                .andExpect(jsonPath(SCHEMAS + "CreateTicketRequest.properties.title.maxLength").value(120))
                .andExpect(jsonPath(SCHEMAS + "CreateTicketRequest.properties.description.minLength").value(1))
                .andExpect(jsonPath(SCHEMAS + "RegisterRequest.properties.password.minLength").value(8))
                .andExpect(jsonPath(SCHEMAS + "RegisterRequest.properties.password.maxLength").value(128))
                .andExpect(jsonPath(SCHEMAS + "Ticket.required", hasItems("id", "assigneeId", "createdByName")))
                .andExpect(jsonPath(SCHEMAS + "Ticket.properties.assigneeId.type", hasItems("integer", "null")))
                .andExpect(jsonPath(SCHEMAS + "Ticket.properties.createdAt.format").value("date-time"))
                .andExpect(jsonPath(SCHEMAS + "Project.properties.dueDate.format").value("date"))
                .andExpect(jsonPath(SCHEMAS + "ErrorResponse.required", containsInAnyOrder("error", "message")))
                // A password, or anything from it, never appears in a response.
                .andExpect(jsonPath(SCHEMAS + "User.properties.password").doesNotExist());
    }

    @Test
    void swaggerPageNeedsNoLogin() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void endpointsStillNeedALogin() throws Exception {
        mockMvc.perform(get("/api/v1/tickets")).andExpect(status().isUnauthorized());
    }

    private ResultActions docs() throws Exception {
        return mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }
}
