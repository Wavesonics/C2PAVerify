# JNA binds to native code by reflection over these types; R8 must not rename or strip them.
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { *; }
-keep class * implements com.sun.jna.Library { *; }
-keep class * extends com.sun.jna.Structure { *; }

# c2pa-android's JNI/JNA bindings.
-keep class org.contentauth.** { *; }
-keep class com.github.contentauth.** { *; }

# JNA references desktop AWT classes that do not exist on Android.
-dontwarn java.awt.**

# BouncyCastle providers are resolved by name.
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**
