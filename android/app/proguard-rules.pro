# gomobile exports public Java entry points that are loaded through reflection.
-keep class mobile.** { *; }
-keep class go.** { *; }

# Callback interfaces are implemented by java.lang.reflect.Proxy and invoked from JNI.
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,AnnotationDefault
-keepnames class idont.trust.atrust.core.GoMobileCoreBridge
