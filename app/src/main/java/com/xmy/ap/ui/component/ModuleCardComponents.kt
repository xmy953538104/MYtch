package com.xmy.ap.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xmy.ap.R

@Composable
fun KPModuleRemoveButton(
    enabled: Boolean, onClick: () -> Unit
) = FilledTonalButton(
    onClick = onClick, enabled = enabled, contentPadding = PaddingValues(12.dp)
) {
    Icon(
        modifier = Modifier.size(20.dp),
        painter = painterResource(id = R.drawable.trash),
        contentDescription = stringResource(id = R.string.kpm_unload)
    )
}
