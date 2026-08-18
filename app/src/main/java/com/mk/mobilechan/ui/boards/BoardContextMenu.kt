package com.mk.mobilechan.ui.boards

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mk.mobilechan.R
import com.mk.mobilechan.data.Board

@Composable
fun BoardContextMenu(
    board: Board,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onExcludeBoard: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.board_menu_exclude)) },
            onClick = {
                onDismissRequest()
                onExcludeBoard(board.board)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Block,
                    contentDescription = null,
                )
            },
        )
    }
}
