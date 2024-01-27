package faang.school.postservice.service.moderation;

import faang.school.postservice.model.Comment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.test.util.ReflectionTestUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.stream.Stream;
import static org.junit.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
class ModerationDictionaryTest {
    @InjectMocks
    private ModerationDictionary moderationDictionary;

    static Stream<Arguments> argsProvider() {
        return Stream.of(
                Arguments.of("fuck", "fuck you idiot", false),
                Arguments.of("fuck", "Zhenya <3", true),
                Arguments.of("shit", "shit happens", false)
        );
    }

    @BeforeEach
    public void setUp() {
        moderationDictionary = new ModerationDictionary();
        moderationDictionary.initDictionary();
        ReflectionTestUtils.setField(moderationDictionary, "profanityWords", Set.of("fuck", "shit"));
    }

    @ParameterizedTest
    @MethodSource("argsProvider")
    public void checkCommentWithSwearWordTest(String word, String content, boolean isVerified) throws IOException {
        InputStream swearWord = new ByteArrayInputStream(word.getBytes());
        Comment comment = Comment.builder().content(content).build();

        moderationDictionary.checkComment(comment);

        assertEquals(isVerified, comment.isVerified());
    }

    @Test
    void checkWordContentTest() {
        ReflectionTestUtils.setField(moderationDictionary, "obsceneWordsDictionary", getListOfObsceneWords());
        boolean actual1 = moderationDictionary.checkWordContent("you are piece of shit");
        boolean actual2 = moderationDictionary.checkWordContent("this is good content");
        boolean actual3 = moderationDictionary.checkWordContent("drug in this post");
        boolean actual4 = moderationDictionary.checkWordContent("f.u.c.k.");

        assertEquals(true, actual1);
        assertEquals(false, actual2);
        assertEquals(true, actual3);
        assertEquals(true, actual4);
    }

    private Set<String> getListOfObsceneWords() {
        return Set.of("shit", "drug", "fuck");
    }
}