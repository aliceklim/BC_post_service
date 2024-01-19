package faang.school.postservice.service.moderation;

import faang.school.postservice.model.Comment;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

@Component
@Setter
@Slf4j
public class ModerationDictionary {

    private final Set<String> profanityWords = new HashSet<>();
    @Value("classpath:profanity-words.txt")
    private Resource profanityWordsFile;

    public boolean checkWordContent(String content) {
        String[] resultStrings = content.replaceAll("[^a-zA-ZА-Яа-я0-9\s]", "")
                .toLowerCase()
                .split(" ");

        return Stream.of(resultStrings)
                .anyMatch(word -> profanityWords.contains(word));
    }

    public void checkComment(Comment comment) {
        String[] words = comment.getContent().toLowerCase().split("\\s+");
        comment.setVerifiedDate(LocalDateTime.now());
        for (String word : words) {
            if (profanityWords.contains(word)) {
                comment.setVerified(false);
                return;
            }
        }
        comment.setVerified(true);
    }

    @PostConstruct
    public void initProfanityWords() throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(profanityWordsFile.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                profanityWords.add(line.trim().toLowerCase());
            }
        }
        log.info("Dictionary of obscene words has initialized successfully");
    }
}