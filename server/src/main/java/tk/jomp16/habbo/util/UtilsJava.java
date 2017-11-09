package tk.jomp16.habbo.util;

import java.lang.invoke.MethodHandle;

public class UtilsJava {
    public static void invokeExact(final MethodHandle methodHandle, final Object clazz, final Object... objects) throws Throwable {
        methodHandle.invokeExact(clazz, objects);
    }
}
