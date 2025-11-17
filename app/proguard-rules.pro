# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

#-keepclassmembers class ttgt.schedule.proto.OverrideHistoryElement$Builder { *; }
#-keepclassmembers class ttgt.schedule.proto.OverrideHistoryElement { *; }

# Protocol Buffers rules
#-keep class com.google.protobuf.** { *; }
#-keep class com.google.protobuf.DescriptorProtos { *; }
#-keep class com.google.protobuf.DescriptorProtos.** { *; }
-keep class ttgt.schedule.proto.** { *; }
#-keep class * extends com.google.protobuf.GeneratedMessage { *; }


# Official Protocol Buffers rules from Google
-keep class com.google.protobuf.** { *; }

-keepclassmembers class * extends com.google.protobuf.GeneratedMessageV3 {
  <fields>;
  <methods>;
}

-keepclassmembers class * implements com.google.protobuf.MessageOrBuilder {
  <fields>;
  <methods>;
}

#-keepclassmembers class * extends com.google.protobuf.GeneratedMessageV3 { *; }

#-keepclassmembers class com.google.protobuf.DescriptorProtos$FieldOptions$Builder { *; }
#-keepclassmembers class com.google.protobuf.DescriptorProtos$FieldOptions$CType { *; }
#-keepclassmembers class com.google.protobuf.DescriptorProtos$FieldOptions$JSType { *; }
#-keepclassmembers class com.google.protobuf.DescriptorProtos$FieldOptions$OptionRetention { *; }
#-keepclassmembers class com.google.protobuf.DescriptorProtos$FieldOptions { *; }

-keepnames class **.** { *; }

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile