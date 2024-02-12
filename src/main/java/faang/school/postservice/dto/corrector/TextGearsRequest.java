package faang.school.postservice.dto.corrector;

public class TextGearsRequest {
    private String text;

    public TextGearsRequest(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
