package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.growurdhan.domain.finance.Onboarding
import `in`.growurdhan.domain.finance.Terms
import kotlinx.coroutines.launch

private fun onboardingIcon(name: String): ImageVector = when (name) {
    "phone" -> Icons.Outlined.Smartphone
    "lock" -> Icons.Outlined.Lock
    "file" -> Icons.Outlined.Description
    "users" -> Icons.Outlined.Group
    "rupee" -> Icons.Outlined.CurrencyRupee
    else -> Icons.Outlined.Shield
}

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
) {
    val slides = Onboarding.slides
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    val chrome = Onboarding.chrome()
    val pageScroll = remember { List(slides.size) { ScrollState(0) } }
    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text("GrowUrDhan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { (pager.currentPage + 1f) / slides.size },
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.tertiaryContainer,
        )
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
            val pageSlide = slides[page]
            val background = colors[page % colors.size]
            val ink = when (page % colors.size) {
                1 -> MaterialTheme.colorScheme.onTertiaryContainer
                2 -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            }
            Box(Modifier.fillMaxSize().background(background)) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .testTag("onboarding-page")
                        .verticalScroll(pageScroll[page])
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(onboardingIcon(pageSlide.icon), contentDescription = null, tint = ink, modifier = Modifier.size(40.dp))
                    Text(
                        pageSlide.enTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ink,
                    )
                    Text(
                        pageSlide.enBody,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ink,
                    )
                }
            }
        }
        if (pageScroll[pager.currentPage].canScrollForward) {
            Text(chrome.scrollCue, style = MaterialTheme.typography.labelLarge)
        }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(
                onClick = onFinished,
                modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp),
            ) { Text(chrome.skip) }
            if (pager.currentPage > 0) {
                OutlinedButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(chrome.back)
                }
            }
            Button(
                onClick = {
                    if (pager.currentPage == slides.lastIndex) onFinished()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp),
            ) { Text(if (pager.currentPage == slides.lastIndex) chrome.start else chrome.next) }
        }
    }
}

@Composable
fun TermsGateScreen(onAccept: () -> Unit, onDecline: () -> Unit) {
    val scroll = rememberScrollState()
    val atEnd = scroll.maxValue == 0 || (scroll.value > 0 && !scroll.canScrollForward)
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(Terms.TITLE, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("What this means", style = MaterialTheme.typography.titleMedium)
                Text("The family's books stay on this phone.")
                Text("This app does not give investment advice.")
                Text("Thirty days free, then ₹99 a year. You can cancel any time in Google Play.")
                Text("The legal text below is in English. Accept is off until you reach the end.")
            }
        }
        Text(
            if (atEnd) "You have reached the end."
            else "Read to the end to accept.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Column(
            Modifier.weight(1f).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Terms.sections.forEach { section ->
                Text(section.heading, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.tertiary)
                Text(section.body, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onDecline,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Don't accept") }
            Button(
                onClick = onAccept,
                enabled = atEnd,
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text("Accept") }
        }
    }
}
