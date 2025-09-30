package interpreter;

public class EnvProvider {
    public EnvProvider() {
    }
    public String getEnv(String var) {
        return System.getenv().get(var);
    }
}