package faang.school.postservice.exception;

import org.apache.kafka.common.protocol.types.Field;

public class UpdatePostException extends RuntimeException{
    public UpdatePostException (String message){
        super(message);
    }
}
