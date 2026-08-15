package com.mk.mobilechan.ui.welcome

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mk.mobilechan.R
import com.mk.mobilechan.ui.navigation.AppModules
import com.mk.mobilechan.ui.theme.MobileChanTheme

private const val WELCOME_STEPS = 3

@Composable
fun WelcomeScreen(
    onFinished: (enabledModules: Set<AppModules>, isAdult: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var selectedNames by rememberSaveable {
        mutableStateOf(listOf(AppModules.FOUR_CHAN.name))
    }
    val selected = remember(selectedNames) {
        selectedNames.mapNotNull { name ->
            AppModules.entries.find { it.name == name }
        }.toSet()
    }

    BackHandler(enabled = step > 0) { step-- }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (step) {
                0 -> WelcomeIntro()
                1 -> WelcomeChans(
                    selected = selected,
                    onToggle = { module ->
                        val name = module.name
                        selectedNames = if (name in selectedNames) {
                            selectedNames - name
                        } else {
                            selectedNames + name
                        }
                    },
                )
                else -> WelcomeAge()
            }
        }
        StepDots(step = step, count = WELCOME_STEPS)
        Spacer(modifier = Modifier.height(20.dp))
        WelcomeActions(
            step = step,
            canContinue = selected.isNotEmpty(),
            onBack = { step-- },
            onContinue = { step++ },
            onFinished = { isAdult -> onFinished(selected, isAdult) },
        )
    }
}

@Composable
private fun WelcomeIntro() {
    Text(
        text = stringResource(R.string.welcome_title),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.welcome_message),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WelcomeChans(
    selected: Set<AppModules>,
    onToggle: (AppModules) -> Unit,
) {
    Text(
        text = stringResource(R.string.welcome_chans_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.welcome_chans_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppModules.entries.forEach { module ->
            ChanOption(
                module = module,
                selected = module in selected,
                onClick = { onToggle(module) },
            )
        }
    }
}

@Composable
private fun ChanOption(
    module: AppModules,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(2.dp, borderColor, shape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Checkbox,
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = selected,
            onCheckedChange = null,
        )
        Text(
            text = stringResource(module.labelRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun WelcomeAge() {
    Text(
        text = stringResource(R.string.welcome_age_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.welcome_age_message),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun WelcomeActions(
    step: Int,
    canContinue: Boolean,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onFinished: (Boolean) -> Unit,
) {
    when (step) {
        0 -> Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.welcome_continue))
        }
        1 -> Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.welcome_back))
            }
            Button(
                onClick = onContinue,
                enabled = canContinue,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(R.string.welcome_continue))
            }
        }
        else -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { onFinished(true) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.welcome_age_yes))
            }
            OutlinedButton(
                onClick = { onFinished(false) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.welcome_age_no))
            }
            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.welcome_back))
            }
        }
    }
}

@Composable
private fun StepDots(step: Int, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val selected = index == step
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (selected) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomeScreenPreview() {
    MobileChanTheme {
        WelcomeScreen(onFinished = { _, _ -> })
    }
}
