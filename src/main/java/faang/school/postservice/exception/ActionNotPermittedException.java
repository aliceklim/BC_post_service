package faang.school.postservice.exception;

public class ActionNotPermittedException extends RuntimeException{
    public ActionNotPermittedException(String message){
        super(message);
    }
}
