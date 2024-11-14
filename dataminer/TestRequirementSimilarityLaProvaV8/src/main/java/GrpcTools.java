import io.grpc.Status;

public class GrpcTools {
    
    public static RuntimeException internalServerError(String message, Exception e) {
        return Status.INTERNAL.withDescription(message).withCause(e).asRuntimeException();
    }
}
