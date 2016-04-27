/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author admin
 */
public class DetectDebugMode
{
    private static boolean DebugMeasured=false;
    private static boolean misDebug=false;
    public static boolean isDebug()
    {
        if(DebugMeasured) return misDebug;
        
        String options = java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments().toString();
        misDebug = options.indexOf("jdwp")>0;
        DebugMeasured = true;
        return misDebug;
    }
}
