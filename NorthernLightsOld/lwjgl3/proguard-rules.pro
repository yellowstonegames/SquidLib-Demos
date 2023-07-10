# proguard github example stuff for application
# https://github.com/Guardsquare/proguard/blob/master/examples/gradle/applications.gradle
-keepattributes '*Annotation*'

-keepclasseswithmembers public class * { public static void main(java.lang.String[]); }

-keepclasseswithmembernames class * { native <methods>; }

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
         static final long serialVersionUID;
         static final java.io.ObjectStreamField[] serialPersistentFields;
         private void writeObject(java.io.ObjectOutputStream);
         private void readObject(java.io.ObjectInputStream);
         java.lang.Object writeReplace();
         java.lang.Object readResolve();
     }

-keep public class com.squidpony.demo.lwjgl3.Lwjgl3Launcher {
    public static void main(java.lang.String[]);
}

-keep public class !com.github.tommyettinger** { *; }

-forceprocessing
#-classobfuscationdictionary 'obfuscationClassNames.txt'
-ignorewarnings
-overloadaggressively
-mergeinterfacesaggressively
-repackageclasses ''
-allowaccessmodification

# FIELD ISSUE NPE
-optimizations !field/propagation/value

# ColorTools fix
-optimizations !code/simplification/string

# LAMBDA FIX
-keepclassmembernames class * {
    private static synthetic *** lambda$*(...);
}

###### PROGUARD ANNOTATIONS END #####
-optimizationpasses 5
