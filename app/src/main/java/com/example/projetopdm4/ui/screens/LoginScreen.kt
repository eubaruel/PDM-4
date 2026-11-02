package com.example.projetopdm4.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.projetopdm4.presentation.viewmodel.FinanceUiState
import com.example.projetopdm4.presentation.viewmodel.FinanceViewModel

@Composable
internal fun LoginScreen(
    modifier: Modifier = Modifier,
    state: FinanceUiState,
    viewModel: FinanceViewModel
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var createAccount by rememberSaveable { mutableStateOf(false) }
    var validationError by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (createAccount) "Criar sua conta" else "Bem-vindo",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    if (createAccount) "Cadastre-se para começar a organizar suas finanças."
                    else "Entre com seu e-mail e senha para continuar.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail (usuário)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (createAccount) "Senha (mínimo 6 caracteres)" else "Senha") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                validationError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                state.authError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        when {
                            email.isBlank() || password.isBlank() ->
                                validationError = "Informe seu e-mail e sua senha."
                            createAccount && password.length < 6 ->
                                validationError = "A senha deve ter pelo menos 6 caracteres."
                            createAccount && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() ->
                                validationError = "Informe um endereço de e-mail válido."
                            else -> {
                                validationError = null
                                if (createAccount) viewModel.createAccount(email, password)
                                else viewModel.signIn(email, password)
                            }
                        }
                    },
                    enabled = !state.authBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (createAccount) "Criar conta" else "Entrar")
                }
                OutlinedButton(
                    onClick = {
                        createAccount = !createAccount
                        validationError = null
                    },
                    enabled = !state.authBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (createAccount) "Já tenho uma conta" else "Criar uma conta")
                }
                if (state.authBusy) CircularProgressIndicator()
            }
        }
    }
}
