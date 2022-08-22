package io.github.coolcrabs.quiltgoslightlyfasterlol;

import java.lang.management.ManagementFactory;
import java.nio.file.Paths;
import java.util.Map;

import com.sun.tools.attach.VirtualMachine;

public class Evil0 {
    public static void upToNoGood() {
        try {
            Map<String, String> savedProps = (Map<String, String>) Hax.savedProps.get();
            savedProps.put("jdk.attach.allowAttachSelf", "true");

            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
            String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));

            // load the agent
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(Paths.get(Evil.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString(), "");
            vm.detach();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
