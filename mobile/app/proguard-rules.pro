# Keep Retrofit interfaces
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }

# Keep Gson data classes
-keepclassmembers class com.qme.mobile.data.model.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
