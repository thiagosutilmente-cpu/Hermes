# Regras de Ofuscação e Proteção Avançada contra Engenharia Reversa (R8/ProGuard)

# 1. Strip debug info: Remove atributos de arquivos fonte e números de linhas para impossibilitar mapeamento de stack traces legíveis
-keepattributes !SourceFile,!LineNumberTable

# 2. Ofuscação profunda: Renomeia pacotes e move todas as classes para o pacote raiz padrão
-repackageclasses ''

# 3. Permite a fusão e simplificação de heranças e modificadores de acesso para maior ofuscação
-allowaccessmodification

# 4. Esconde os nomes de arquivos originais das classes compiladas
-renamesourcefileattribute ""

# 5. Remoção automática de logs de depuração: Remove todas as chamadas para android.util.Log do código-fonte compilado em produção
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 6. Manter componentes essenciais do Android declarados no Manifest para o SO instanciá-los
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.accessibilityservice.AccessibilityService

# 7. Regras para o Room Database funcionar sem erros de reflexão
-keep class * extends androidx.room.RoomDatabase
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <methods>;
}

# 8. Regras de preservação para serialização de dados (Moshi / API)
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @com.squareup.moshi.JsonClass *;
}
-keep class com.example.api.** { *; }
-keep class com.example.coordinator.ActiveOffer { *; }
-keep class com.example.coordinator.RadarSettings { *; }
