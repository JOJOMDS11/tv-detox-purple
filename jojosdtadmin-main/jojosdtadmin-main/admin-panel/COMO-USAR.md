# 🎮 HaxBall DreamTeam - Admin Panel

## 🚀 Como Usar

### 📋 Pré-requisitos
- Node.js instalado
- Conexão com internet

### 🎯 Iniciar o Painel

**Opção 1 - Duplo Clique (Mais Fácil):**
1. Dê duplo clique no arquivo `iniciar-painel.bat`
2. Aguarde o servidor iniciar
3. Acesse: `http://localhost:3000` no navegador
4. Use a senha: `admin123`

**Opção 2 - Terminal:**
1. Abra o terminal/cmd nesta pasta
2. Execute: `npm run dev`
3. Acesse: `http://localhost:3000` no navegador
4. Use a senha: `admin123`

### 🔐 Login
- **URL:** http://localhost:3000
- **Senha:** admin123

### 📊 Funcionalidades

#### 👥 Times
- Ver lista de todos os times
- Ver vitórias, derrotas e informações do proprietário
- Excluir times

#### 🎴 Jogadores  
- Buscar jogador pelo nome do time
- Ver cartas do jogador (Prime, GOAT, Médio, Bagre)
- Ver Purple Coins do jogador

#### ⚙️ Templates
- Criar novos templates de jogadores
- Usar emoji ou texto como avatar
- Ver todos os templates criados

#### 💰 Moedas
- Buscar usuário por Discord ID
- Adicionar Purple Coins para qualquer usuário
- Ver estatísticas do usuário

### 🛑 Para Parar o Servidor
- Feche a janela do terminal ou
- Pressione `Ctrl + C` no terminal

### 🔧 Problemas Comuns

**Porta já em uso:**
- Se der erro de porta, mude no arquivo `.env` ou
- Use: `$env:PORT=3001; npm run dev` (PowerShell)

**Erro de conexão:**
- Verifique se está conectado à internet
- Verifique as credenciais no arquivo `.env`

### 📞 Suporte
Se algo não funcionar, verifique:
1. Node.js está instalado
2. Internet está funcionando  
3. Arquivo `.env` está correto
