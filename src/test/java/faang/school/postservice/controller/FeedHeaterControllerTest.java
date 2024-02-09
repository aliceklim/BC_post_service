package faang.school.postservice.controller;

import faang.school.postservice.service.FeedHeaterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
public class FeedHeaterControllerTest {
    @Mock
    private FeedHeaterService feedHeaterService;
    @InjectMocks
    private FeedHeaterController feedHeaterController;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(feedHeaterController).build();
    }

    @Test
    void heatFeedTest() throws Exception {
        mockMvc.perform(post("/api/v1/heat-feed")).andExpect(status().isOk());

        verify(feedHeaterService, times(1)).heatFeed();
    }
}