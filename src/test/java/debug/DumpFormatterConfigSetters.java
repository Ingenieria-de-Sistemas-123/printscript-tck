package debug;

import org.printscript.formatter.config.FormatterConfig;

import java.lang.reflect.Method;
import java.util.Arrays;

public class DumpFormatterConfigSetters {
    public static void main(String[] args) {
        Method[] methods = FormatterConfig.class.getMethods();
        Arrays.stream(methods)
                .filter(m -> m.getName().startsWith("set") && m.getParameterCount() == 1)
                .sorted((a,b) -> a.getName().compareTo(b.getName()))
                .forEach(m -> System.out.println(m.getName() + "(" + m.getParameterTypes()[0].getSimpleName() + ")"));
    }
}

