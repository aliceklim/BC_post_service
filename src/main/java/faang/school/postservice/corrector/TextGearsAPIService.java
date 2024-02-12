package faang.school.postservice.corrector;

import com.fasterxml.jackson.databind.ObjectMapper;
import faang.school.postservice.config.AIApiConfig;
import faang.school.postservice.dto.corrector.TextGearsRequest;
import faang.school.postservice.dto.corrector.TextGearsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.NetworkException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.awt.SystemColor.text;


@Service
@Slf4j
@RequiredArgsConstructor
public class TextGearsAPIService {

    private final RestTemplate restTemplate;
    private final AIApiConfig aiApiConfig;
    private Language language = Language.EN_US;

    private static final String TEXT_GEARS_API_URL = "https://api.textgears.com/check.php";


    @Retryable(value = {NetworkException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public String correctText(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("X-RapidAPI-Key", aiApiConfig.getKey());
        headers.set("X-RapidAPI-Host", aiApiConfig.getHost());

        MultiValueMap<String, String> requestBody = new LinkedMultiValueMap<>();
        requestBody.add("text", content);
        requestBody.add("language", language.getCode());

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        //String url = aiApiConfig.getHost() + aiApiConfig.getPath();
        String url = "https://api.textgears.com/check.php?key=" + aiApiConfig.getKey() + "&text=" + content;

        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

        if (responseEntity.getStatusCode() == HttpStatus.OK) {
            String responseBody = responseEntity.getBody();
            return extractCorrectedTextFromJson(responseBody, content);
        } else {
            throw new NetworkException("Failed to correct text using AI service.");
        }
    }

    private String extractCorrectedTextFromJson(String jsonResponse, String originalText) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            TextGearsResponse response = objectMapper.readValue(jsonResponse, TextGearsResponse.class);

                StringBuilder correctedTextBuilder = new StringBuilder(originalText);
                List<TextGearsResponse.Error> errors = response.getErrors();

                for (TextGearsResponse.Error error : errors) {
                    int offset = error.getOffset();
                    List<String> betterArray = error.getBetter();

                    if (betterArray != null && !betterArray.isEmpty()) {
                        String betterSuggestion = betterArray.get(0);
                        if (betterSuggestion != null && !betterSuggestion.isEmpty()) {
                            correctedTextBuilder.replace(offset, offset + betterSuggestion.length(), betterSuggestion);
                        }
                    }
                }

                return correctedTextBuilder.toString();

        } catch (Exception e) {
            throw new NetworkException("Error parsing JSON response: " + e.getMessage());
        }
    }
}