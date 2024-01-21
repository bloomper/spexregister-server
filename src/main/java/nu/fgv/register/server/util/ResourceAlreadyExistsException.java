package nu.fgv.register.server.util;

public class ResourceAlreadyExistsException extends RuntimeException {

    public ResourceAlreadyExistsException(final String message) {
        super(message);
    }
}
