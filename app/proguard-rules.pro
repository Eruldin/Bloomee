-dontwarn org.slf4j.**

# Keep Kotlin metadata + signatures so Room/Compose reflection-free paths and
# generic lookups keep working after shrinking.
-keepattributes Signature, *Annotation*, EnclosingMethod, InnerClasses, SourceFile, LineNumberTable

# Room entities are referenced from generated code; keep their names and members.
-keep class com.bloomee.app.data.local.** { *; }
