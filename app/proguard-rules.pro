# ==============================================================================
# PROGUARD / R8 RULES - RADAR COORDINATOR / JARVIS NEURAL COCKPIT
# Ofuscação de Lógica Proprietária, Redes e Preservação de Frameworks
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. DIRETRIZES GERAIS DE OTIMIZAÇÃO E OFUSCAÇÃO AGRESSIVA
# ------------------------------------------------------------------------------
-optimizations !code/simplification/arithmetic,!code/allocation/variable
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'com.example.cockpit.internal'
-flattenpackagehierarchy 'com.example.cockpit.obf'
-overloadaggressively
-dontusemixedcaseclassnames

# Oculta nomes reais de arquivos-fonte nas stack traces compiladas
-renamesourcefileattribute 'CockpitSecure'
-keepattributes SourceFile,LineNumberTable

# Strip de logs em produção para não vazar telemetria e dados de corrida no Logcat
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(java.lang.String, java.lang.String);
}

# ------------------------------------------------------------------------------
# 2. OFUSCAÇÃO E BLINDAGEM DA CLASSE 'RideAnalysisData'
# ------------------------------------------------------------------------------
# Preserva atributos anotados para manter o mapeamento JSON (GSON) intacto
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Mantém a estrutura de dados para tráfego e serialização
-keep class com.example.RideAnalysisData {
    <fields>;
}

# Ofusca agressivamente métodos e lógicas de cálculo financeiro interno
-keepclassmembers,allowobfuscation class com.example.RideAnalysisData {
    public *** calculateNetProfit(...);
    public *** calculateNetRatePerKm(...);
    public *** isFinanciallyViable(...);
    public static *** processFinancials(...);
    public static *** fromJarvisRide(...);
}

# ------------------------------------------------------------------------------
# 3. OFUSCAÇÃO E PROTEÇÃO DA LÓGICA DE DECISÃO ('DecisionEngine')
# ------------------------------------------------------------------------------
# Blindagem contra engenharia reversa das fórmulas de corte, lucro e avaliação
-keepclassmembers,allowobfuscation class com.example.DecisionEngine {
    public static *** evaluate(...);
    public static *** calculateDecision(...);
    public static *** fetchSystemHealth(...);
    public static *** notifyStackAccepted(...);
    public static *** notifyStackDeclined(...);
}

-keepclassmembers,allowobfuscation class com.example.RadarDecisionEngine {
    public static *** evaluate(...);
    public static *** calculateDecision(...);
    public static *** fetchSystemHealth(...);
    public static *** notifyStackAccepted(...);
    public static *** notifyStackDeclined(...);
}

-keepclassmembers,allowobfuscation class com.example.JarvisNeuralCockpitEngine {
    public static *** processInput(...);
    public static *** processInputToJson(...);
    public static *** processAndVoice(...);
}

# Preserva os contratos de resposta das decisões
-keep class com.example.NeuralDecision { <fields>; }
-keep class com.example.RadarOffer { <fields>; }
-keep class com.example.DeliveryOffer { <fields>; }
-keep class com.example.JarvisNeuralCockpitEngine$* { <fields>; }
-keep class com.example.SmartMarketingMilestoneManager$* { <fields>; }
-keep class com.example.CampaignDataPoint { <fields>; }

# Preservar pontes JavascriptInterface para WebView (Recharts & D3)
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ------------------------------------------------------------------------------
# 4. COMUNICAÇÃO DE REDE: RETROFIT & OKHTTP
# ------------------------------------------------------------------------------
# Retrofit: Preservar interfaces de serviço e metadados de anotações HTTP
-keepattributes RuntimeVisible*Annotations,RuntimeInvisible*Annotations
-keepattributes RuntimeVisible*ParameterAnnotations,RuntimeInvisible*ParameterAnnotations

-keepclassmembers,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp: Preservar componentes essenciais e suprimir avisos de plataformas opcionais
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepclassmembers class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okhttp3.internal.platform.**
-dontwarn okio.**
-dontwarn retrofit2.**

# Classes e modelos de comunicação com a VPS e Gateway
-keep class com.example.JarvisVpsApiClient$* { <fields>; }
-keep class com.example.network.** { <fields>; }

# ------------------------------------------------------------------------------
# 5. FIREBASE: MANTER INTACTO (FCM, ANALYTICS E CORE)
# ------------------------------------------------------------------------------
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-keep public class * extends com.google.firebase.messaging.FirebaseMessagingService { *; }
-keep public class * extends com.google.firebase.iid.FirebaseInstanceIdService { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ------------------------------------------------------------------------------
# 6. WORKMANAGER: MANTER INTACTO (BACKGROUND JOBS & WORKERS)
# ------------------------------------------------------------------------------
-keep class androidx.work.** { *; }
-keep interface androidx.work.** { *; }
-keepclassmembers class androidx.work.** { *; }

-keep class * extends androidx.work.Worker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class * extends androidx.work.CoroutineWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-dontwarn androidx.work.**

# ------------------------------------------------------------------------------
# 7. JETPACK COMPOSE, COROUTINES E LIFECYCLE
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-dontwarn androidx.compose.**
-dontwarn kotlinx.coroutines.**

# ------------------------------------------------------------------------------
# 8. COMPONENTES DO ANDROID OS
# ------------------------------------------------------------------------------
-keep public class * extends android.app.Service
-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.service.notification.NotificationListenerService
-keep public class * extends androidx.car.app.CarAppService
