package microbat.instrumentation;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

public class PremainWrapper {

    private static int reentranceCount = 0;

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        if (reentranceCount > 0) {
            throw new IllegalStateException("Reentrant call to premain");
        }
        reentranceCount += 1;

        // Determine if loaded form bootstrap classloader
        Class<PremainWrapper> agentClass = PremainWrapper.class;
        ClassLoader currentLoader = agentClass.getClassLoader();
        if (currentLoader != null) {
            System.out.println("Agent loaded by classloader: " + currentLoader);

            // if not, add the agent to the bootstrap classloader
            String agentPath = agentClass.getProtectionDomain().getCodeSource().getLocation().getPath();
            JarFile agentJar = new JarFile(agentPath);
            inst.appendToBootstrapClassLoaderSearch(agentJar);

            // jump to bootstrap classloader
            Class<?> agentClassBootstrap = Class.forName(agentClass.getName(), true, null);
            Method premainMethod = agentClassBootstrap.getDeclaredMethod(
                    "premain",
                    String.class,
                    Instrumentation.class);

            ClassLoader currentContextLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(null);
            try {
                premainMethod.invoke(null, agentArgs, inst);
            } finally {
                Thread.currentThread().setContextClassLoader(currentContextLoader);
            }
        } else {
            System.out.println("Agent loaded by bootstrap classloader");

            // If we are in the bootstrap classloader, we can start the agent
            // DO NOT USE microbat.instrumentation.Premain.class here,
            // as it will cause to load the class
            Class<?> realAgent = Class.forName("microbat.instrumentation.Premain", true, null);
            Method premainMethod = realAgent.getDeclaredMethod(
                    "premain",
                    String.class,
                    Instrumentation.class);
            premainMethod.invoke(null, agentArgs, inst);
        }
    }
}
