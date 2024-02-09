package faang.school.postservice.controller;

import faang.school.postservice.dto.FeedDto;
import faang.school.postservice.service.FeedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
public class FeedControllerTest {

    @Mock
    private FeedService feedService;
    @InjectMocks
    private FeedController feedController;
    private MockMvc mockMvc;
    private Long postId = 1L;

    @BeforeEach
    void setUp(){
        mockMvc = MockMvcBuilders.standaloneSetup(feedController).build();
    }

    @Test
    void getFeedTest() throws Exception {

        when(feedService.getUserFeedBy(postId)).thenReturn(any(FeedDto.class));

        mockMvc.perform(get("/api/v1/feed")
                .param("postId", String.valueOf(postId))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect((status().isOk()));

        verify(feedService, times(1)).getUserFeedBy(postId);
    }
}