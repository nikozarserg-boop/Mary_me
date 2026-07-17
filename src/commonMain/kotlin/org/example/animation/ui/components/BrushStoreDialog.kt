package org.example.animation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import org.example.animation.io.BrushStoreCategory
import org.example.animation.io.BrushStoreItem
import org.example.animation.io.BrushStoreService
import org.example.animation.localization.EditorStrings
import org.example.animation.model.BrushPreset
import org.example.animation.ui.theme.*

@Composable
fun BrushStoreDialog(
    onDismiss: () -> Unit,
    onBrushInstalled: (BrushPreset) -> Unit
) {
    var brushes by remember { mutableStateOf<List<BrushStoreItem>>(emptyList()) }
    var categories by remember { mutableStateOf<List<BrushStoreCategory>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<BrushStoreCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var installingBrushId by remember { mutableStateOf<String?>(null) }
    
    val service = remember { BrushStoreService() }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        try {
            val categoriesResult = service.getCategories()
            categories = categoriesResult
            if (categoriesResult.isNotEmpty()) {
                selectedCategory = categoriesResult.first()
            }
            scope.launch {
                loadBrushes(service, selectedCategory?.id, null, { brushes = it }, { errorMessage = it })
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Error loading store"
        } finally {
            isLoading = false
        }
    }
    
    DisposableEffect(Unit) {
        onDispose { service.close() }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = EditorColors.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Header(onDismiss = onDismiss)
                
                if (errorMessage != null) {
                    ErrorView(errorMessage!!, onRetry = { 
                        errorMessage = null
                        isLoading = true
                        scope.launch {
                            loadBrushes(service, selectedCategory?.id, searchQuery.ifEmpty { null }, 
                                { brushes = it }, { errorMessage = it })
                        }
                    })
                } else if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = EditorColors.accent)
                    }
                } else {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { 
                            searchQuery = it
                            scope.launch {
                                loadBrushes(service, selectedCategory?.id, it.ifEmpty { null },
                                    { brushes = it }, { errorMessage = it })
                            }
                        }
                    )
                    
                    Row(modifier = Modifier.fillMaxSize()) {
                        CategorySidebar(
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { 
                                selectedCategory = it
                                scope.launch {
                                    loadBrushes(service, it.id, searchQuery.ifEmpty { null },
                                        { brushes = it }, { errorMessage = it })
                                }
                            }
                        )
                        
                        Divider(modifier = Modifier.width(1.dp).fillMaxHeight())
                        
                        BrushGrid(
                            brushes = brushes,
                            onDownload = { brush ->
                                installingBrushId = brush.id
                                scope.launch {
                                    try {
                                        val data = service.downloadBrush(brush.id)
                                        val preset = BrushPreset(
                                            name = brush.name,
                                            id = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
                                        )
                                        onBrushInstalled(preset)
                                    } catch (e: Exception) {
                                        errorMessage = e.message
                                    } finally {
                                        installingBrushId = null
                                    }
                                }
                            },
                            installingBrushId = installingBrushId
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(EditorColors.panelHeader).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = EditorStrings.observeString("store.title"),
            style = EditorTypography.panelTitle(),
            modifier = Modifier.weight(1f)
        )
        IconButton(onDismiss) {
            Icon(Icons.Default.Close, contentDescription = EditorStrings.observeString("close"))
        }
    }
}

@Composable
private fun SearchBar(query: String, onQueryChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(EditorStrings.observeString("store.search")) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                backgroundColor = EditorColors.surface,
                focusedBorderColor = EditorColors.accent,
                unfocusedBorderColor = EditorColors.divider
            )
        )
    }
}

@Composable
private fun CategorySidebar(
    categories: List<BrushStoreCategory>,
    selectedCategory: BrushStoreCategory?,
    onCategorySelected: (BrushStoreCategory) -> Unit
) {
    Column(
        modifier = Modifier.width(200.dp).fillMaxHeight().background(EditorColors.panelBackground).verticalScroll(rememberScrollState())
    ) {
        Text(
            text = EditorStrings.observeString("store.categories"),
            style = EditorTypography.caption().copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(16.dp)
        )
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategorySelected(category) }
                    .background(if (isSelected) EditorColors.accent.copy(alpha = 0.1f) else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (isSelected) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = EditorColors.accent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = category.name,
                    color = if (isSelected) EditorColors.accent else EditorColors.textPrimary,
                    style = if (isSelected) EditorTypography.mono() else EditorTypography.body()
                )
            }
        }
    }
}

@Composable
private fun BrushGrid(
    brushes: List<BrushStoreItem>,
    onDownload: (BrushStoreItem) -> Unit,
    installingBrushId: String?
) {
    val columns = 4
    val rows = (brushes.size + columns - 1) / columns
    
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < brushes.size) {
                        val brush = brushes[index]
                        BrushCard(
                            brush = brush,
                            onDownload = onDownload,
                            isInstalling = installingBrushId == brush.id,
                            modifier = Modifier.weight(1f).padding(4.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f).padding(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun BrushCard(
    brush: BrushStoreItem,
    onDownload: (BrushStoreItem) -> Unit,
    isInstalling: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.aspectRatio(1f),
        elevation = 2.dp,
        backgroundColor = EditorColors.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                brush.previewPng?.let { previewUrl ->
                    Box(
                        modifier = Modifier.fillMaxSize().background(EditorColors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = brush.name,
                            style = EditorTypography.caption(),
                            color = EditorColors.textSecondary
                        )
                    }
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize().background(EditorColors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = EditorColors.textMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = brush.name,
                    style = EditorTypography.caption().copy(fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(
                    text = brush.author,
                    style = EditorTypography.caption(),
                    color = EditorColors.textSecondary,
                    maxLines = 1
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${brush.rating} ★",
                        style = EditorTypography.caption(),
                        color = Color.Yellow
                    )
                    if (isInstalling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = EditorColors.accent
                        )
                    } else {
                        Button(
                            onClick = { onDownload(brush) },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accent)
                        ) {
                            Text("${EditorStrings.observeString("store.download")}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = message, color = EditorColors.accentRed, style = EditorTypography.body())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(backgroundColor = EditorColors.accent)) {
            Text(EditorStrings.observeString("store.retry"))
        }
    }
}

private suspend fun loadBrushes(
    service: BrushStoreService,
    categoryId: String?,
    query: String?,
    onSuccess: (List<BrushStoreItem>) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val response = service.getBrushes(category = categoryId, query = query)
        onSuccess(response.items)
    } catch (e: Exception) {
        onError(e.message ?: "Unknown error")
    }
}