-allowaccessmodification
-repackageclasses

-overloadaggressively

-keepclassmembers class **$WhenMappings { <fields>; }

-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ietf.jgss.**

-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}
