package team.projectpulse.security;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import team.projectpulse.AbstractIntegrationTest;
import team.projectpulse.system.StatusCode;
import team.projectpulse.user.PeerEvaluationUser;
import team.projectpulse.user.UserRepository;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * FR-SEC-active-account (UC-STU-deactivate-student POST-2): a token issued before its account was deactivated stops
 * working on the next request, for students and instructors alike, while an active account's token is unaffected.
 *
 * <p>The account is deactivated through the repository, inside the test's own rolled-back transaction, so the
 * check is exercised independently of any deactivation endpoint.
 */
@DisplayName("Integration tests for the per-request active-account check")
class ActiveAccountIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    UserRepository userRepository;

    @Value("${api.endpoint.base-url}")
    String baseUrl;

    Integer johnId;


    @BeforeEach
    void setUp() {
        this.johnId = this.userRepository.findByUsername("j.smith@abc.edu").orElseThrow().getId();
    }

    private String login(String username) throws Exception {
        String body = this.mockMvc.perform(post(this.baseUrl + "/users/login").with(httpBasic(username, "123456")))
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + new JSONObject(body).getJSONObject("data").getString("token");
    }

    private void deactivate(String username) {
        PeerEvaluationUser user = this.userRepository.findByUsername(username).orElseThrow();
        user.setEnabled(false);
        this.userRepository.saveAndFlush(user);
    }

    @Test
    void anActiveAccountsTokenStillWorks() throws Exception {
        String johnToken = login("j.smith@abc.edu");

        this.mockMvc.perform(get(this.baseUrl + "/students/" + this.johnId).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, johnToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true))
                .andExpect(jsonPath("$.code").value(StatusCode.SUCCESS));
    }

    @Test
    void aDeactivatedStudentsLiveTokenIsRefused() throws Exception {
        String johnToken = login("j.smith@abc.edu");
        deactivate("j.smith@abc.edu");

        this.mockMvc.perform(get(this.baseUrl + "/students/" + this.johnId).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, johnToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void aDeactivatedInstructorsLiveTokenIsRefused() throws Exception {
        String billToken = login("b.gates@abc.edu");
        deactivate("b.gates@abc.edu");

        this.mockMvc.perform(get(this.baseUrl + "/students/" + this.johnId).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, billToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.flag").value(false))
                .andExpect(jsonPath("$.code").value(StatusCode.UNAUTHORIZED));
    }

    @Test
    void anotherAccountsDeactivationLeavesThisTokenAlone() throws Exception {
        String billToken = login("b.gates@abc.edu");
        deactivate("j.smith@abc.edu");

        this.mockMvc.perform(get(this.baseUrl + "/students/" + this.johnId).accept(MediaType.APPLICATION_JSON).header(HttpHeaders.AUTHORIZATION, billToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flag").value(true));
    }

}
