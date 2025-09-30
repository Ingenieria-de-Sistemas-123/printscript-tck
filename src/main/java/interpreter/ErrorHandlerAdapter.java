package interpreter;

public class ErrorHandlerAdapter {
    private final ErrorHandler handler;
    public ErrorHandlerAdapter(ErrorHandler handler) {
        this.handler = handler;
    }

    public void notifyChange (String result) {
        handler.reportError(result);
    }
}