package com.rekixo.ludopaws

import android.content.Context
import com.rekixo.ludopaws.ui.dialogs.showNaturalWorldAuditDialog
import com.rekixo.ludopaws.ui.dialogs.showProofHistoryDialog
import com.rekixo.ludopaws.ui.dialogs.showSettingsDialog

object ArcadeDialogs {
    fun showSettings(
        context: Context,
        onChanged: (() -> Unit)? = null,
    ) = showSettingsDialog(context, onChanged)

    fun showNaturalWorldAudit(
        context: Context,
        audit: OfflineRandomnessAudit?,
    ) = showNaturalWorldAuditDialog(context, audit)

    fun showProofHistory(
        context: Context,
        title: String,
        body: String,
    ) = showProofHistoryDialog(context, title, body)
}
