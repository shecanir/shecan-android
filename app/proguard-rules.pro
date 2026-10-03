# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in E:\development\android-sdk-windows/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Keep all native methods
-keepclasseswithmembernames class * { native <methods>; }

# Keep your app's package and all its classes
-keep class ir.shecan.** { *; }

# Keep DNS or VPN-related classes (Modify based on your actual package names)
-keep class ir.shecan.core.provider.** { *; }
-keep class ir.shecan.core.receiver.** { *; }
-keep class ir.shecan.core.service.** { *; }
-keep class ir.shecan.core.util.** { *; }

# Keep all classes that use reflection
-keepattributes *Annotation*
-keepclassmembers class * { @android.annotation.Keep *; }
-keep class * extends android.app.Service { *; }

# Keep WorkManager and JobScheduler services if used
-keep class androidx.work.** { *; }

# Keep networking libraries like OkHttp, Retrofit if used
-keep class okhttp3.** { *; }
-keep class retrofit2.** { *; }
-keep class org.xbill.DNS.** { *; }  # If using dnsjava

# Keep Pcap4j classes and related components (packet capture)
-keep class org.pcap4j.** { *; }
-keep class org.pcap4j.packet.** { *; }
-keep class org.pcap4j.core.** { *; }
-keep class org.pcap4j.util.** { *; }

#-keep class com.pushpole.sdk.** { *; }
-keepattributes Signature
-dontwarn sun.misc
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# Keep native methods if Pcap4j is using JNI
-keepclasseswithmembernames class * { native <methods>; }

# Keep MiniDNS classes
-keep class de.measite.minidns.** { *; }

# Keep Myket billing callbacks, receiver, proxy activity, and purchase models.
-keep class ir.myket.billingclient.** { *; }
-keep class com.android.vending.billing.** { *; }

# Suppress warning about missing SLF4J binding
-dontwarn org.slf4j.impl.StaticLoggerBinder
