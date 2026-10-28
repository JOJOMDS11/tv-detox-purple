# 🎮 HaxBall DreamTeam - Admin Panel

Painel administrativo web para gerenciar o bot HaxBall DreamTeam.

## 🚀 Deploy no Vercel

### 1. Preparar o ambiente

```bash
cd admin-panel
npm install
```

### 2. Configurar variáveis de ambiente no Vercel

No dashboard do Vercel, adicione estas variáveis:

- `DB_HOST`: Host do banco MySQL
- `DB_USER`: Usuário do banco
- `DB_PASSWORD`: Senha do banco
- `DB_NAME`: Nome do banco
- `ADMIN_PASSWORD`: Senha para acessar o painel (ex: `haxball123`)

### 3. Deploy

```bash
vercel --prod
```

## 🎯 Funcionalidades

### 📊 Dashboard
- **Estatísticas gerais**: Total de usuários, times, cartas e templates
- **Visão em tempo real** dos dados do bot

### 👥 Gerenciar Times
- **Visualizar todos os times** criados
- **Ver Discord ID** de cada dono
- **Excluir times** problemáticos
- **Histórico** de vitórias/derrotas

### 🎴 Gerenciar Jogadores
- **Lista completa** de usuários do bot
- **Estatísticas de cartas** por raridade
- **Purple Coins** de cada usuário
- **Adicionar moedas** rapidamente

### ⚙️ Gerenciar Templates
- **Criar novos jogadores** (nome, posição, avatar)
- **Visualizar todos** os templates existentes
- **Excluir jogadores** (se não tiverem cartas)
- **Posições**: GK (Goleiro), VL (Volante), PV (Pivô)

### 💰 Gerenciar Purple Coins
- **Adicionar moedas** para qualquer usuário
- **Buscar usuário** por Discord ID
- **Ver estatísticas completas** do jogador

### 🔐 Segurança
- **Login com senha** de administrador
- **Sessão protegida**
- **Validações** de entrada

## 🎨 Interface

- **Design responsivo** para desktop e mobile
- **Tema moderno** com gradientes roxos
- **Tabelas organizadas** com scroll
- **Feedback visual** para todas as ações
- **Ícones intuitivos** para cada funcionalidade

## 📱 Como usar

1. **Acesse** o painel no Vercel
2. **Digite a senha** de administrador
3. **Navegue pelas abas**:
   - 👥 **Times**: Ver e gerenciar times
   - 🎴 **Jogadores**: Ver usuários e seus dados
   - ⚙️ **Templates**: Criar/excluir jogadores
   - 💰 **Moedas**: Adicionar Purple Coins

## 🔧 Tecnologias

- **Frontend**: HTML5, CSS3, JavaScript (Vanilla)
- **Backend**: Node.js + Vercel Serverless Functions
- **Banco**: MySQL (mesmo do bot)
- **Hospedagem**: Vercel

## 🚨 Importante

- **Use apenas** com a senha de admin configurada
- **Backup** dos dados antes de excluir
- **Teste** em ambiente de desenvolvimento primeiro
- **Não compartilhe** a URL do painel publicamente

## 🎯 Próximas funcionalidades

- [ ] **Logs** de atividades
- [ ] **Gráficos** de estatísticas
- [ ] **Backup/Restore** de dados
- [ ] **Gerenciar partidas** e resultados
- [ ] **Notificações** em tempo real
