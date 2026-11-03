# Projeto PDM 4 — Finanças pessoais

Aplicativo Android para acompanhar receitas e despesas, organizar categorias, planejar orçamentos e metas e consultar relatórios. Os dados são armazenados localmente com Room e sincronizados com o Firebase quando a conta está autenticada e a conexão está disponível.

## Funcionalidades

- Login com e-mail e senha do Firebase Authentication.
- Cadastro de conta pela tela inicial, sem acesso ao aplicativo antes da autenticação.
- Encerramento da sessão pela aba **Configurações**.
- Resumo do saldo, das receitas e das despesas.
- Registro, edição e exclusão de movimentações.
- Filtro de movimentações por período.
- Categorias separadas entre receitas e despesas, gerenciadas em **Configurações**.
- Orçamentos mensais por categoria e metas financeiras.
- Relatórios com despesas agrupadas por categoria.
- Exportação de movimentações em CSV e relatório mensal em PDF.
- Personalização do perfil com nome, cor e foto.
- Navegação inferior somente com ícones, com descrições acessíveis.

## Tecnologias

- Kotlin
- Android SDK: `compileSdk 36`, `minSdk 24`, `targetSdk 36`
- Java 11
- Jetpack Compose e Material 3
- Navigation Compose
- AndroidX ViewModel e Kotlin Coroutines/Flow
- Room para persistência local
- Firebase Authentication para cadastro, login e sessão
- Firebase Realtime Database para sincronização dos dados e do perfil
- Gradle Kotlin DSL, Version Catalog e KSP

## Arquitetura

O projeto separa apresentação, domínio e dados. A interface envia ações ao `ViewModel`; o `ViewModel` coordena os casos de uso; os casos de uso dependem de contratos do domínio; e as implementações de repositório conectam esses contratos ao Room e ao Firebase.

```text
app/src/main/java/com/example/projetopdm4/
├── MainActivity.kt
├── data/
│   ├── export/
│   │   └── FinanceReportExporter.kt
│   ├── firebase/
│   │   └── FirebaseTask.kt
│   ├── local/
│   │   ├── FinanceDatabase.kt
│   │   └── FinanceMappers.kt
│   └── repository/
│       ├── FinanceRepositoryImpl.kt
│       └── FirebaseProfileRepository.kt
├── domain/
│   ├── model/
│   │   ├── FinanceModels.kt
│   │   └── UserProfile.kt
│   ├── repository/
│   │   ├── FinanceRepository.kt
│   │   └── ProfileRepository.kt
│   └── usecase/
│       └── FinanceUseCases.kt
├── presentation/
│   └── viewmodel/
│       ├── FinanceViewModel.kt
│       └── FinanceViewModelFactory.kt
└── ui/
    ├── screens/
    │   ├── FinanceFormatting.kt
    │   ├── FinanceNavigation.kt
    │   ├── HomeScreen.kt
    │   ├── LoginScreen.kt
    │   ├── PlanningScreen.kt
    │   ├── ReportsScreen.kt
    │   ├── SettingsScreen.kt
    │   ├── TransactionComponents.kt
    │   └── TransactionsScreen.kt
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

### Responsabilidades das camadas

- **`ui/`**: telas Compose, navegação, componentes e formatação visual. Não acessa Room ou Firebase diretamente.
- **`presentation/viewmodel/`**: mantém o estado observado pela interface, recebe as ações da UI e coordena as operações.
- **`domain/model/`**: modelos de negócio independentes de Android, Room e Firebase.
- **`domain/repository/`**: contratos que definem as operações disponíveis para o domínio.
- **`domain/usecase/`**: operações de negócio, como salvar movimentações, categorias, orçamentos e metas.
- **`data/local/`**: entidades Room, DAO, banco de dados e mapeadores entre entidades de persistência e modelos de domínio.
- **`data/repository/`**: implementações dos contratos com Room, Firebase Authentication, Realtime Database e preferências locais do perfil.
- **`data/export/`**: geração de relatórios CSV e PDF.
- **`MainActivity.kt`**: ponto de entrada Android; configura o tema e exibe a aplicação.

O `FinanceViewModelFactory` cria as implementações necessárias e injeta os casos de uso e repositórios no `FinanceViewModel`.

## Telas

### Login

É a primeira tela exibida quando não há uma sessão autenticada. Permite entrar com e-mail e senha ou criar uma conta. O aplicativo não apresenta opção de continuar sem conta.

O campo identificado como usuário aceita **e-mail**, pois o projeto usa o provedor de e-mail e senha do Firebase Authentication.

### Resumo

Mostra o saldo total, as receitas e despesas do mês, e movimentações recentes. Também permite iniciar um lançamento de receita ou despesa.

### Movimentações

Lista os lançamentos, permite filtrar por datas, abrir um lançamento para edição e excluí-lo por meio da ação de pressionar e segurar.

### Planejamento

Permite criar e editar metas financeiras, além de registrar orçamentos mensais associados a categorias de despesas.

### Relatórios

Exibe receitas, despesas e total por categoria no mês atual. Permite exportar todos os lançamentos para CSV e gerar um PDF mensal. O PDF lista até os 35 lançamentos mais recentes do período.

### Configurações

Agrupa o perfil e as categorias. Permite personalizar nome e cor, escolher uma foto e sair da conta. A foto é compactada antes de ser enviada ao Firebase.

## Configuração do Firebase

O projeto depende de um projeto Firebase configurado para o aplicativo Android.

1. Registre o aplicativo Android no Firebase com o identificador:

   ```text
   com.example.projetopdm4
   ```

2. Baixe o arquivo de configuração `google-services.json` do projeto Firebase e coloque-o em:

   ```text
   app/google-services.json
   ```

   Use o arquivo correspondente ao seu projeto Firebase; não compartilhe credenciais ou arquivos de configuração privados publicamente.

3. No Firebase Console, habilite **Authentication → E-mail/senha**.

4. Crie ou habilite o **Realtime Database**.

5. Configure a URL do Realtime Database por uma das opções abaixo:

   **`local.properties`**, na raiz do projeto:

   ```properties
   firebaseDatabaseUrl=https://SEU-PROJETO-default-rtdb.firebaseio.com/
   ```

   **Propriedade Gradle**, em `gradle.properties` local ou na chamada do Gradle:

   ```text
   -PfirebaseDatabaseUrl=https://SEU-PROJETO-default-rtdb.firebaseio.com/
   ```

   **Variável de ambiente:**

   ```text
   FIREBASE_DATABASE_URL
   ```

   A prioridade de leitura é: propriedade Gradle, variável de ambiente e, por último, `local.properties`.

6. Publique regras que restrinjam a leitura e a escrita ao usuário autenticado. O arquivo `database.rules.json` contém a regra usada pelo projeto:

   ```json
   {
     "rules": {
       "users": {
         "$uid": {
           ".read": "auth != null && auth.uid === $uid",
           ".write": "auth != null && auth.uid === $uid"
         }
       }
     }
   }
   ```

As informações financeiras são organizadas no caminho `users/{uid}/finance`; as informações do perfil ficam em `users/{uid}/profile`. As regras acima limitam o acesso ao nó de cada usuário autenticado.

## Persistência e sincronização

- O banco local Room usa o arquivo `finance.db`.
- As tabelas locais armazenam movimentações, categorias, orçamentos e metas.
- O repositório observa os dados locais com `Flow` e publica o estado combinado para o `ViewModel`.
- Com uma conta autenticada e a URL do Realtime Database configurada, os dados são sincronizados no caminho associado ao UID do usuário.
- Os perfis usam o UID como chave. Nome e cor também são mantidos em preferências locais; nome, cor e foto são sincronizados com o Realtime Database.
- A foto é reduzida para no máximo 512 pixels por dimensão, compactada em JPEG e armazenada no Firebase como Base64. Como Base64 aumenta o tamanho do conteúdo, evite fotos grandes ou de alta resolução.
- Sem conexão, os dados já persistidos localmente continuam disponíveis; a sincronização remota depende da conexão e da configuração do Firebase.
- Senhas são gerenciadas pelo Firebase Authentication; o aplicativo não as armazena em preferências próprias.

## Executar o projeto

### Android Studio

1. Abra a pasta raiz do projeto no Android Studio.
2. Configure o Firebase seguindo as instruções acima.
3. Aguarde a sincronização do Gradle.
4. Execute a configuração `app` em um emulador ou dispositivo Android.

### Windows — linha de comando

Na raiz do projeto, compile o Kotlin:

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-daemon
```

Gere o APK de depuração:

```powershell
.\gradlew.bat :app:assembleDebug --no-daemon
```

Execute os testes unitários:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

O APK de depuração, quando gerado, fica em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Estrutura de dados principal

### Movimentação (`FinanceTransaction`)

- `id`: identificador local.
- `type`: tipo do lançamento, `INCOME` ou `EXPENSE`.
- `amountCents`: valor inteiro em centavos.
- `categoryId` e `categoryName`: categoria do lançamento.
- `description`: descrição opcional.
- `occurredAt`: data e hora em milissegundos.

### Categoria (`FinanceCategory`)

- `id`: identificador.
- `name`: nome apresentado na interface.
- `type`: `INCOME` ou `EXPENSE`.

### Orçamento (`CategoryBudget`)

- `categoryId`: categoria de despesa associada.
- `categoryName`: nome da categoria.
- `month`: mês no formato `AAAA-MM`.
- `limitCents`: limite em centavos.

### Meta (`SavingsGoal`)

- `id`: identificador.
- `name`: nome da meta.
- `targetCents`: valor-alvo em centavos.
- `savedCents`: valor acumulado em centavos.
- `targetDate`: data opcional.

Valores financeiros são armazenados em centavos para evitar imprecisão de ponto flutuante.

## Desenvolvimento

- Mantenha a interface em `ui/`; não coloque chamadas de Firebase ou acesso ao DAO em funções Composable.
- Adicione regras de negócio em `domain/` e injete repositórios por contrato.
- Use modelos de domínio independentes das entidades Room; converta entre eles em `FinanceMappers.kt`.
- Atualize os testes correspondentes quando mudar a lógica.
- Não armazene senhas, chaves ou credenciais no código-fonte.
- Não publique `local.properties` ou credenciais do Firebase em repositórios públicos.

## Testes

Os testes unitários ficam em:

```text
app/src/test/java/com/example/projetopdm4/
```

Para executá-los no Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest --no-daemon
```

A compilação Kotlin pode ser validada com:

```powershell
.\gradlew.bat :app:compileDebugKotlin --no-daemon
```



#### Criado por Lucas Baruel, Carlos Oliveira e Mario Rodrigues
