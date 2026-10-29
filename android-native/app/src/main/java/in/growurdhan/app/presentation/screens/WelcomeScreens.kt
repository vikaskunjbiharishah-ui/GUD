package `in`.growurdhan.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import `in`.growurdhan.domain.finance.Onboarding
import `in`.growurdhan.domain.finance.Terms
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val slides = Onboarding.slides
    val pager = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    var hindi by remember { mutableStateOf(false) }
    val colors = listOf(
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer,
        MaterialTheme.colorScheme.secondaryContainer,
    )
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("GrowUrDhan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            TextButton(
                onClick = { hindi = !hindi },
                modifier = Modifier.heightIn(min = 48.dp).widthIn(min = 48.dp),
            ) { Text(if (hindi) "English" else "हिन्दी") }
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
            Surface(
                color = background,
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.fillMaxSize().padding(end = 8.dp),
            ) {
                Column(
                    Modifier.padding(22.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        if (hindi) pageSlide.hiTitle else pageSlide.enTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = ink,
                    )
                    Text(
                        if (hindi) pageSlide.hiBody else pageSlide.enBody,
                        style = MaterialTheme.typography.bodyLarge,
                        color = ink,
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (pager.currentPage > 0) {
                OutlinedButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text("Back")
                }
            }
            Button(
                onClick = {
                    if (pager.currentPage == slides.lastIndex) onFinished()
                    else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
            ) { Text(if (pager.currentPage == slides.lastIndex) "Continue" else "Next") }
        }
    }
}

@Composable
fun TermsGateScreen(onAccept: () -> Unit, onDecline: () -> Unit) {
    val scroll = rememberScrollState()
    val atEnd = scroll.maxValue == 0 || (scroll.value > 0 && !scroll.canScrollForward)
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(Terms.TITLE, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Effective ${Terms.EFFECTIVE}. Scroll to the end, then accept. English controls if Hindi and English differ.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
