package com.example.jobtracker.controller;

import com.example.jobtracker.service.GmailAuthService;
import com.example.jobtracker.service.GmailUpdateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalControllerTest {

    private static final String SECRET = "test-scheduler-secret";

    private GmailUpdateService gmailUpdateService;
    private GmailAuthService gmailAuthService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        gmailUpdateService = mock(GmailUpdateService.class);
        gmailAuthService = mock(GmailAuthService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new InternalController(
                                gmailUpdateService,
                                gmailAuthService,
                                SECRET
                        )
                )
                .build();
    }

    @Test
    void rejectsRequestsWithoutTheSchedulerSecret()
            throws Exception {

        mockMvc.perform(
                        post("/api/internal/check-gmail")
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(
                gmailUpdateService,
                gmailAuthService
        );
    }

    @Test
    void reportsWhenGmailIsNotConnected()
            throws Exception {

        when(gmailAuthService.isConnected())
                .thenReturn(false);

        mockMvc.perform(
                        post("/api/internal/check-gmail")
                                .header(
                                        "X-Scheduler-Secret",
                                        SECRET
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.error")
                                .value("Gmail is not connected")
                );
    }

    @Test
    void synchronizesGmailForAnAuthorizedRequest()
            throws Exception {

        when(gmailAuthService.isConnected())
                .thenReturn(true);
        when(gmailUpdateService.startUpdateCheck())
                .thenReturn(true);

        mockMvc.perform(
                        post("/api/internal/check-gmail")
                                .header(
                                        "X-Scheduler-Secret",
                                        SECRET
                                )
                )
                .andExpect(status().isAccepted())
                .andExpect(
                        jsonPath("$.status")
                                .value("accepted")
                );
    }
}
