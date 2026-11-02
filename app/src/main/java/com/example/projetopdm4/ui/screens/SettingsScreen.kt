package com.example.projetopdm4.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.projetopdm4.presentation.viewmodel.FinanceUiState
import com.example.projetopdm4.presentation.viewmodel.FinanceViewModel

@Composable
internal fun SettingsScreen(state: FinanceUiState, viewModel: FinanceViewModel) {
    val profileColors = listOf(
        Color(0xFF176B58),
        Color(0xFF4169B2),
        Color(0xFF8056A8),
        Color(0xFFCA7046),
        Color(0xFFB34F68)
    )
    var profileName by rememberSaveable(state.accountUid) { mutableStateOf(state.profileName) }
    var selectedColor by rememberSaveable { mutableStateOf(state.profileColor.coerceIn(profileColors.indices)) }
    var categoryName by rememberSaveable { mutableStateOf("") }
    var categoryError by rememberSaveable { mutableStateOf<String?>(null) }
    var profileError by rememberSaveable { mutableStateOf<String?>(null) }
    val displayName = state.profileName.ifBlank { "Seu perfil" }
    val context = androidx.compose.ui.platform.LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Não foi possível abrir a foto selecionada.")
            }.onSuccess(viewModel::saveProfilePhoto)
                .onFailure {
                    Toast.makeText(context, it.localizedMessage, Toast.LENGTH_LONG).show()
                }
        }
    }
    val profilePhoto = remember(state.profilePhotoBase64) {
        state.profilePhotoBase64?.let { encoded ->
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }
    }
    LaunchedEffect(state.profileName) {
        if (profileName.isBlank()) profileName = state.profileName
    }

    ScreenColumn {
        Text("Configurações", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Sua conta, perfil e categorias em um só lugar.")

        Text("Perfil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Card(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (profilePhoto != null) {
                        Image(
                            bitmap = profilePhoto,
                            contentDescription = "Foto do perfil",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(88.dp).clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(88.dp).background(profileColors[selectedColor], CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                displayName.take(1).uppercase(),
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { photoPicker.launch("image/*") },
                    enabled = !state.authBusy
                ) { Text("Adicionar ou alterar foto") }
                state.profileError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                    LaunchedEffect(it) { viewModel.clearProfileError() }
                }
                Text(displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    state.email ?: "Conta local",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Nome de exibição") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Cor do perfil", modifier = Modifier.align(Alignment.Start))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
                ) {
                    profileColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(if (selectedColor == index) 42.dp else 36.dp)
                                .background(color, CircleShape)
                                .clickable { selectedColor = index },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == index) Text("✓", color = Color.White)
                        }
                    }
                }
                Button(
                    onClick = {
                        if (profileName.isBlank()) profileError = "Informe um nome para o perfil."
                        else {
                            profileError = null
                            viewModel.saveProfile(profileName, selectedColor)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Salvar perfil") }
                profileError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }

        Text("Conta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Card(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Conectado como ${state.email.orEmpty()}")
                state.authError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                OutlinedButton(
                    onClick = { viewModel.signOut() },
                    enabled = !state.authBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sair da conta") }
                if (state.authBusy) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
        }

        Text("Categorias", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text("Crie categorias para receitas e despesas; elas ficam disponíveis nos lançamentos.")
        OutlinedTextField(
            value = categoryName,
            onValueChange = { categoryName = it },
            label = { Text("Nome da categoria") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    if (categoryName.isBlank()) categoryError = "Digite o nome da categoria."
                    else {
                        viewModel.saveCategory(categoryName, INCOME)
                        categoryName = ""
                        categoryError = null
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("+ Receita") }
            OutlinedButton(
                onClick = {
                    if (categoryName.isBlank()) categoryError = "Digite o nome da categoria."
                    else {
                        viewModel.saveCategory(categoryName, EXPENSE)
                        categoryName = ""
                        categoryError = null
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("+ Despesa") }
        }
        categoryError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.data.categories.forEach { category ->
            Card(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.weight(1f).padding(vertical = 12.dp)) {
                        Text(category.name, fontWeight = FontWeight.SemiBold)
                        Text(category.type.toTypeLabel(), style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { viewModel.deleteCategory(category.id) }) { Text("Remover") }
                }
            }
        }
        if (state.data.categories.isEmpty()) {
            Text("Nenhuma categoria criada.")
        }
    }
}
