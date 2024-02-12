package faang.school.postservice.dto.corrector;

import jdk.jfr.Description;
import lombok.Getter;

import java.util.List;
@Getter
public class TextGearsResponse {
    private boolean result;
    private List<Error> errors;

    @Getter
    public static class Error {
        private String id;
        private int offset;
        private int length;
        private Description description;
        private String bad;
        private List<String> better;
        private String type;

        @Getter
        public static class Description {
            private String en;
        }
    }
}