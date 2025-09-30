package interpreter;


public class InputProviderAdapter implements InputProvider {
    private final InputProvider inputProvider;

    public InputProviderAdapter(InputProvider inputProvider) {
        this.inputProvider = inputProvider;
    }

    @Override
    public String input(String name) {
        return inputProvider.input(name);
    }
}
