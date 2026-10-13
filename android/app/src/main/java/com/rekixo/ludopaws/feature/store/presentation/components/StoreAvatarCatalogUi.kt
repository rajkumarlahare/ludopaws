package com.rekixo.ludopaws.feature.store.presentation.components

import android.widget.LinearLayout
import com.rekixo.ludopaws.feature.store.domain.StoreCosmeticCatalog
import com.rekixo.ludopaws.feature.store.presentation.StoreActivity

internal fun StoreActivity.avatarCatalogContent():
    LinearLayout =
    cosmeticGrid(
        items =
            StoreCosmeticCatalog
                .avatars,
        borderColor =
            0xFFFF7C2A.toInt(),
    )
