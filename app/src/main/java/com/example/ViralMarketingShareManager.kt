package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.widget.Toast
import java.util.UUID

/**
 * Gerenciador de Marketing Viral e Compartilhamento no WhatsApp do Jarvis.
 * Permite que motoboys compartilhem o app nos grupos da praça e ganhem dias grátis de Pro.
 */
object ViralMarketingShareManager {

    private const val PREFS_NAME = "jarvis_viral_marketing_prefs"
    private const val KEY_REFERRAL_CODE = "viral_referral_code"
    private const val KEY_INVITES_SHARED = "viral_invites_shared"
    private const val KEY_BONUS_DAYS_EARNED = "viral_bonus_days_earned"

    private const val BASE_LANDING_PAGE = "https://radar-jarvis.app/download"

    /**
     * Retorna o código exclusivo do entregador (ex: MOTO-78A2)
     */
    fun getReferralCode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var code = prefs.getString(KEY_REFERRAL_CODE, null)
        if (code == null) {
            val randomSuffix = UUID.randomUUID().toString().substring(0, 4).uppercase()
            code = "MOTO-$randomSuffix"
            prefs.edit().putString(KEY_REFERRAL_CODE, code).apply()
        }
        return code
    }

    /**
     * Retorna o link completo de instalação com o código do parceiro
     */
    fun getReferralLink(context: Context): String {
        val code = getReferralCode(context)
        return "$BASE_LANDING_PAGE?ref=$code"
    }

    /**
     * Mensagem formatada de alta conversão para Grupos de WhatsApp de Motoboys
     */
    fun getGroupWhatsAppMessage(context: Context): String {
        val link = getReferralLink(context)
        return """
🚨 *GALERA DA ENTREGA, SE LIGA NESSA!* 🚨

Liberaram *7 DIAS GRÁTIS* do aplicativo do Jarvis que:
✅ Calcula na hora se a corrida do iFood/99 dá lucro ou prejuízo (R$/km)
✅ Recusa corrida lixo de R$ 6 que dá prejuízo na gasolina
✅ Avisa *RADAR e BLITZ* no fone de ouvido antes de você passar
✅ Junta corridas da mesma rota pra dobrar o ganho da hora

Tô usando aqui na pista e economizando mais de R$ 150 de gasolina por semana.

👉 *Baixa grátis por 7 dias no link:*
$link
        """.trimIndent()
    }

    /**
     * Mensagem direta rápida para enviar no privado de amigos
     */
    fun getDirectFriendMessage(context: Context): String {
        val link = getReferralLink(context)
        return """
Fala parceiro! Baixa esse app do Jarvis que calcula o valor por km das corridas do iFood antes de aceitar e avisa blitz no fone. 
Liberaram 7 dias de graça: $link
        """.trimIndent()
    }

    /**
     * Mensagem para Status do WhatsApp / Stories
     */
    fun getStatusStoryMessage(context: Context): String {
        val link = getReferralLink(context)
        return "Cansado de corrida de R$ 6 no iFood? Tô usando o Jarvis com filtro de R$/km e aviso de blitz no fone. 7 dias grátis: $link"
    }

    /**
     * Compartilha a mensagem diretamente no WhatsApp do celular
     */
    fun shareToWhatsApp(context: Context, message: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            recordShareAction(context)
            true
        } catch (e: Exception) {
            // Se o WhatsApp não estiver com package fixado, abre o seletor padrão do Android
            try {
                val genericIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(genericIntent, "Compartilhar com parceiros de rota").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                recordShareAction(context)
                true
            } catch (ex: Exception) {
                Toast.makeText(context, "Erro ao abrir compartilhamento", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }

    /**
     * Copia o texto para a área de transferência com feedback tátil
     */
    fun copyToClipboard(context: Context, text: String, label: String = "Convite Jarvis") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        HapticFeedbackHelper.vibrateSuccess(context)
        Toast.makeText(context, "Mensagem copiada! Cole nos seus grupos de motoboy 🚀", Toast.LENGTH_LONG).show()
        recordShareAction(context)
    }

    /**
     * Registra o compartilhamento e bonifica o entregador com dias grátis
     */
    private fun recordShareAction(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentShared = prefs.getInt(KEY_INVITES_SHARED, 0) + 1
        prefs.edit().putInt(KEY_INVITES_SHARED, currentShared).apply()

        // Gamificação: A cada 2 compartilhamentos registrados, ganha +3 dias de bônus!
        if (currentShared % 2 == 0) {
            addBonusDays(context, 3)
        }
    }

    /**
     * Adiciona dias extras de Pro e notifica o entregador
     */
    fun addBonusDays(context: Context, days: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val totalDays = prefs.getInt(KEY_BONUS_DAYS_EARNED, 0) + days
        prefs.edit().putInt(KEY_BONUS_DAYS_EARNED, totalDays).apply()

        SubscriptionManager.addBonusTrialDays(days)
        Toast.makeText(context, "🎁 Bônus de Indicação: +$days dias de Jarvis Pro adicionados!", Toast.LENGTH_LONG).show()
    }

    fun getSharedCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_INVITES_SHARED, 0)
    }

    fun getBonusDaysEarned(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_BONUS_DAYS_EARNED, 0)
    }
}
