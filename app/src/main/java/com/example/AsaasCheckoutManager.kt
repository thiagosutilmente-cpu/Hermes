package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Gerenciador de links de Checkout do Asaas (Hospedado / Web).
 * Permite abrir o link de pagamento oficial do Asaas no navegador do usuário
 * para qualquer um dos planos (Semanal, Mensal, Anual VIP) ou link geral de campanha.
 */
object AsaasCheckoutManager {

    private const val PREFS_NAME = "jarvis_asaas_checkout_prefs"
    private const val KEY_CHECKOUT_WEEKLY = "key_checkout_weekly"
    private const val KEY_CHECKOUT_MONTHLY = "key_checkout_monthly"
    private const val KEY_CHECKOUT_ANNUAL = "key_checkout_annual"
    private const val KEY_CHECKOUT_GENERAL = "key_checkout_general"

    const val DEFAULT_URL_WEEKLY = "https://www.asaas.com/c/jarvis-pro-semanal"
    const val DEFAULT_URL_MONTHLY = "https://www.asaas.com/c/jarvis-pro-mensal"
    const val DEFAULT_URL_ANNUAL = "https://www.asaas.com/c/jarvis-pro-anual"
    const val DEFAULT_URL_GENERAL = "https://www.asaas.com/c/jarvis-pro-oficial"

    var urlWeekly: String = DEFAULT_URL_WEEKLY
        private set
    var urlMonthly: String = DEFAULT_URL_MONTHLY
        private set
    var urlAnnual: String = DEFAULT_URL_ANNUAL
        private set
    var urlGeneral: String = DEFAULT_URL_GENERAL
        private set

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        urlWeekly = prefs.getString(KEY_CHECKOUT_WEEKLY, DEFAULT_URL_WEEKLY) ?: DEFAULT_URL_WEEKLY
        urlMonthly = prefs.getString(KEY_CHECKOUT_MONTHLY, DEFAULT_URL_MONTHLY) ?: DEFAULT_URL_MONTHLY
        urlAnnual = prefs.getString(KEY_CHECKOUT_ANNUAL, DEFAULT_URL_ANNUAL) ?: DEFAULT_URL_ANNUAL
        urlGeneral = prefs.getString(KEY_CHECKOUT_GENERAL, DEFAULT_URL_GENERAL) ?: DEFAULT_URL_GENERAL
    }

    fun getCheckoutUrl(planId: String): String {
        return when (planId.lowercase()) {
            "weekly", "semanal", "pro_weekly" -> urlWeekly
            "monthly", "mensal", "pro_monthly" -> urlMonthly
            "annual", "anual", "pro_annual" -> urlAnnual
            else -> urlGeneral
        }
    }

    fun updateCheckoutUrl(context: Context, planId: String, newUrl: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        when (planId.lowercase()) {
            "weekly", "semanal" -> {
                urlWeekly = newUrl
                editor.putString(KEY_CHECKOUT_WEEKLY, newUrl)
            }
            "monthly", "mensal" -> {
                urlMonthly = newUrl
                editor.putString(KEY_CHECKOUT_MONTHLY, newUrl)
            }
            "annual", "anual" -> {
                urlAnnual = newUrl
                editor.putString(KEY_CHECKOUT_ANNUAL, newUrl)
            }
            else -> {
                urlGeneral = newUrl
                editor.putString(KEY_CHECKOUT_GENERAL, newUrl)
            }
        }
        editor.apply()
    }

    /**
     * Abre a URL oficial do Asaas no navegador do smartphone.
     */
    fun openCheckout(context: Context, planId: String = "general", planTitle: String = "Jarvis Pro"): Boolean {
        val targetUrl = getCheckoutUrl(planId)
        return try {
            FirebaseAnalyticsManager.logScreenView("AsaasCheckoutBrowser", planTitle)
            
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Abrindo Checkout Seguro Asaas...", Toast.LENGTH_SHORT).show()
            true
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Não foi possível abrir o navegador. Copie o link: $targetUrl",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }
}
