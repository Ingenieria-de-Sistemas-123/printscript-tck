package implementation;

import interpreter.ErrorHandler;
import interpreter.PrintEmitter;

final class AdapterUtils {
    private static final ErrorHandler NO_OP_HANDLER = message -> { };
    private static final PrintEmitter NO_OP_EMITTER = message -> { };

    private AdapterUtils() {
    }

    static ErrorHandler safeHandler(ErrorHandler handler) {
        return handler != null ? handler : NO_OP_HANDLER;
    }

    static PrintEmitter safeEmitter(PrintEmitter emitter) {
        return emitter != null ? emitter : NO_OP_EMITTER;
    }

    static String messageOrDefault(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return throwable.getClass().getSimpleName();
        }
        return message;
    }
}