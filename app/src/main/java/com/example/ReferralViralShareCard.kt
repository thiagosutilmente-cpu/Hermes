package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Cores táticas Uber / 99 Dark
private val DarkCardSurface = Color(0xFF11141C)
private val DarkCardBorder = Color(0xFF222938)
private val WhatsAppGreen = Color(0xFF25D366)
private val AmberGold = Color(0xFFFFB800)
private val TextMuted = Color(0xFF8C9BAE)

/**
 * Card de Indicação Viral no WhatsApp para o Jarvis Neural Cockpit.
 * Estilo Uber Base / 99 Driver com alto contraste para motoboys em rota.
 */
@Composable
fun ReferralViralShareCard(
    modifier: Modifier = Modifier,
    onOpenMarketingHub: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val referralCode = remember { ViralMarketingShareManager.getReferralCode(context) }
    var sharedCount by remember { mutableIntStateOf(ViralMarketingShareManager.getSharedCount(context)) }
    var bonusDays by remember { mutableIntStateOf(ViralMarketingShareManager.getBonusDaysEarned(context)) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(WhatsAppGreen.copy(alpha = 0.6f), AmberGold.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("card_referral_viral_share"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Cabeçalho com Ícone e Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.15f))
                            .border(1.dp, WhatsAppGreen.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartilhar",
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = "INDIQUE A FROTA",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Ganhe +3 Dias de Jarvis Pro",
                            color = WhatsAppGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Badge de Bônus
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(AmberGold.copy(alpha = 0.15f))
                        .border(1.dp, AmberGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "+$bonusDays DIAS",
                            color = AmberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Mande nos grupos de WhatsApp de motoboy e praças da sua cidade. Quando a galera instalar para testar o filtro anti-prejuízo, você ganha dias grátis de Pro!",
                color = Color.LightGray,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Box do Código de Indicação
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF090B10))
                    .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SEU CÓDIGO DE PARCEIRO",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = referralCode,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            ViralMarketingShareManager.copyToClipboard(context, referralCode, "Código de Parceiro")
                        }
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar Código",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copiar",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Botão Principal: Enviar no WhatsApp
            Button(
                onClick = {
                    val message = ViralMarketingShareManager.getGroupWhatsAppMessage(context)
                    ViralMarketingShareManager.shareToWhatsApp(context, message)
                    sharedCount = ViralMarketingShareManager.getSharedCount(context)
                    bonusDays = ViralMarketingShareManager.getBonusDaysEarned(context)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_share_whatsapp_group"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WhatsAppGreen,
                    contentColor = Color.Black
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Compartilhar nos Grupos de Zap",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Copiar Mensagem Pronta
                OutlinedButton(
                    onClick = {
                        val message = ViralMarketingShareManager.getGroupWhatsAppMessage(context)
                        ViralMarketingShareManager.copyToClipboard(context, message, "Mensagem Grupos Zap")
                        sharedCount = ViralMarketingShareManager.getSharedCount(context)
                        bonusDays = ViralMarketingShareManager.getBonusDaysEarned(context)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_copy_viral_message"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text(text = "Copiar Mensagem", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Abrir Central de Marketing se fornecido
                if (onOpenMarketingHub != null) {
                    OutlinedButton(
                        onClick = onOpenMarketingHub,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_open_marketing_hub"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberGold)
                    ) {
                        Text(text = "Roteiros & TikTok 🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
