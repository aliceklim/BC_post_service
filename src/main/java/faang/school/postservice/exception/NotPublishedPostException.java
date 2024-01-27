package faang.school.postservice.exception;

public class NotPublishedPostException extends RuntimeException{
    public NotPublishedPostException (String message){
        super(message);
    }
}
