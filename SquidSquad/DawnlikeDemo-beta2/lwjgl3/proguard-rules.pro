# proguard github example stuff for application
# https://github.com/Guardsquare/proguard/blob/master/examples/gradle/applications.gradle
-keepattributes '*Annotation*'

-keepclasseswithmembers public class * { public static void main(java.lang.String[]); }

-keepclasseswithmembernames class * { native <methods>; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep public class com.github.tommyettinger.lwjgl3.Lwjgl3Launcher {
    public static void main(java.lang.String[]);
}

-keep public class !com.github.tommyettinger.Data { *; }
-keep public class !com.github.tommyettinger.DawnlikeDemo { *; }

-forceprocessing
#-classobfuscationdictionary 'obfuscationClassNames.txt'
-ignorewarnings
-overloadaggressively
-mergeinterfacesaggressively
-repackageclasses ''
-allowaccessmodification

# FIELD ISSUE NPE
-optimizations !field/propagation/value

# DescriptiveColor fix
-optimizations !code/simplification/string

# LAMBDA FIX
-keepclassmembernames class * {
    private static synthetic *** lambda$*(...);
}

###### PROGUARD ANNOTATIONS END #####
-optimizationpasses 5
