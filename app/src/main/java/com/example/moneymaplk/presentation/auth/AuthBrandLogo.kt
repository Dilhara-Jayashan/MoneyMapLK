package com.example.moneymaplk.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.example.moneymaplk.R

@Composable
internal fun MoneyMapAuthLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.moneymap_logo),
        contentDescription = "MoneyMap LK logo",
        modifier = modifier
    )
}
