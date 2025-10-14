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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Retrofit and OkHttp classes
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Gson classes
-keep class com.google.gson.** { *; }

# Keep Hilt classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep Hilt ViewModels and Factories
-keep class * extends dagger.hilt.android.internal.lifecycle.HiltViewModelFactory { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep all Hilt generated classes
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }
-keep class **_GeneratedInjector { *; }
-keep class **Hilt_* { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep specific ProteinViewModel
-keep class com.avas.proteinviewer.presentation.ProteinViewModel { *; }
-keep class com.avas.proteinviewer.presentation.ProteinViewModel$* { *; }

# Keep Hilt Annotations
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes AnnotationDefault

# Keep data model classes
-keep class com.avas.proteinviewer.data.model.** { *; }
