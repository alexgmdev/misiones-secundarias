package com.groovycow.misionessecundarias.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.groovycow.misionessecundarias.ui.auth.AuthBackground
import com.groovycow.misionessecundarias.ui.auth.AuthPanel
import com.groovycow.misionessecundarias.ui.auth.AuthTitle
import com.groovycow.misionessecundarias.ui.auth.ButtonTextShadow
import com.groovycow.misionessecundarias.ui.auth.ForestInk
import com.groovycow.misionessecundarias.ui.auth.GameCream
import com.groovycow.misionessecundarias.ui.auth.GameGold
import com.groovycow.misionessecundarias.ui.auth.SoftTextShadow
import kotlinx.coroutines.launch

private val RiverTeal = Color(0xFF38A89D)
private val SunYellow = Color(0xFFF2C94C)

/** Las cuatro páginas de la guía inicial (HU-013). */
internal val ONBOARDING_PAGES: List<OnboardingPage> =
    listOf(
        OnboardingPage(
            title = "Misiones de campo",
            body = "Explora el mapa de Cali y encuentra misiones presenciales cerca de ti para ayudar a otras personas.",
            icon = Icons.Default.LocationOn,
        ),
        OnboardingPage(
            title = "Misiones remotas",
            body = "Algunas misiones se resuelven desde cualquier lugar: ayuda a distancia sin moverte de casa.",
            icon = Icons.Default.Home,
        ),
        OnboardingPage(
            title = "Postulaciones",
            body =
                "Postúlate a las misiones que te interesen y el publicador elegirá a quién aceptar. " +
                    "Sigue su estado en Mis postulaciones.",
            icon = Icons.Default.Person,
        ),
        OnboardingPage(
            title = "Moderación y convivencia",
            body =
                "Trata a todos con respeto. Las misiones y los usuarios pueden ser reportados y revisados " +
                    "para mantener una comunidad segura.",
            icon = Icons.Default.Warning,
        ),
    )

/**
 * Guía de bienvenida de 4 páginas (HU-013), sin persistencia.
 *
 * @param fromSettings `true` si se abrió desde Ajustes → Ayuda.
 * @param onFinish Se invoca al terminar u omitir (y al volver) con el destino resuelto.
 */
@Composable
fun OnboardingScreen(
    fromSettings: Boolean,
    onFinish: (fromSettings: Boolean) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGES.size })
    val scope = rememberCoroutineScope()
    val close = { onFinish(fromSettings) }
    BackHandler(onBack = close)

    AuthBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                OnboardingPageContent(ONBOARDING_PAGES[index])
            }
            PageIndicators(current = pagerState.currentPage, total = ONBOARDING_PAGES.size)
            Spacer(modifier = Modifier.height(16.dp))
            val current = pagerState.currentPage
            Button(
                onClick = {
                    if (OnboardingFlow.isLastPage(current)) {
                        close()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(OnboardingFlow.nextPage(current)) }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GameGold, contentColor = ForestInk),
            ) {
                Text(
                    OnboardingFlow.primaryLabel(current),
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium.copy(shadow = ButtonTextShadow),
                )
            }
            if (OnboardingFlow.showSkip(current)) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = close,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(2.dp, GameCream),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GameCream),
                ) {
                    Text("Omitir", fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(modifier = Modifier.height(62.dp))
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AuthPanel {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(112.dp)
                            .border(4.dp, SunYellow, CircleShape)
                            .padding(5.dp)
                            .border(3.dp, RiverTeal, CircleShape)
                            .background(GameCream.copy(alpha = 0.95f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(page.icon, contentDescription = null, tint = ForestInk, modifier = Modifier.size(56.dp))
                }
                AuthTitle(page.title)
                Text(
                    text = page.body,
                    color = GameCream,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge.copy(shadow = SoftTextShadow),
                )
            }
        }
    }
}

@Composable
private fun PageIndicators(
    current: Int,
    total: Int,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            Box(
                modifier =
                    Modifier
                        .size(if (i == current) 12.dp else 9.dp)
                        .background(if (i == current) GameGold else GameCream.copy(alpha = 0.45f), CircleShape),
            )
        }
    }
}
