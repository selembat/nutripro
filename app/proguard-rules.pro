# NutriPro ProGuard Rules

# Keep kotlinx.serialization classes
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep NutriPro model classes (serializable)
-keep,includedescriptorclasses class com.nutripro.model.**$$serializer { *; }
-keepclassmembers class com.nutripro.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.nutripro.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep karoo-ext classes
-keep class io.hammerhead.karooext.** { *; }

# Keep NutriPro extension and receiver
-keep class com.nutripro.karoo.NutriProExtension { *; }
-keep class com.nutripro.BootReceiver { *; }

# Remove debug logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
}
-assumenosideeffects class timber.log.Timber {
    public static void v(...);
    public static void d(...);
}
