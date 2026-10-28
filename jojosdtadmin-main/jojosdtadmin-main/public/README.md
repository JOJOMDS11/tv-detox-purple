# 🎮 HaxBall DreamTeam - Admin Panel para Vercel

## 📋 Passo a Passo para Deploy

### 1. **Copiar a pasta `vercel-admin` para seu novo projeto**

### 2. **Instalar dependências**
```bash
cd vercel-admin
npm install
```

### 3. **Configurar variáveis de ambiente no Vercel**
No dashboard do Vercel, adicione estas variáveis:

- **DB_HOST**: `jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com`
- **DB_USER**: `jojodreamteam`
- **DB_PASSWORD**: `[SUA_SENHA_DO_BANCO]`
- **DB_NAME**: `jojodreamteam`
- **DB_PORT**: `3306`

### 4. **Deploy no Vercel**
```bash
npx vercel --prod
```

### 5. **Funcionalidades Incluídas**

#### 🔐 **Login Administrativo**
- **Senha**: `eojojos` (conforme solicitado)

#### 📊 **Dashboard**
- Estatísticas gerais do bot
- Total de jogadores, times, cartas, purple coins

#### 👥 **Gestão de Times**
- Visualizar todos os times
- Estatísticas de vitórias/derrotas

#### 🎴 **Gestão de Jogadores** 
- Buscar jogadores por Discord ID ou nome
- Adicionar Purple Coins diretamente

#### ⚙️ **Templates de Jogadores**
- Criar novos templates (nome, posição, avatar)
- Excluir templates existentes

#### 💰 **Sistema Purple Coins**
- Adicionar moedas para qualquer jogador
- Buscar jogador específico

#### 🎟️ **Sistema de Códigos**
- **Gerar códigos** de Purple Coins
- **Códigos personalizados** ou automáticos
- **Histórico completo** de códigos usados
- **Quem usou cada código** e quando

#### 🎨 **Emoji Converter**
- **URL**: `/emoji-converter`
- Converter emojis para códigos hexadecimais
- **Salvar automaticamente** no banco de dados
- Histórico local de conversões
- Copiar códigos facilmente

#### 💎 **Simulação Econômica**
- **Valor sugerido**: R$ 0,05 por Purple Coin
- **Ganho diário**: 50-150 Purple Coins
- **Potencial de renda**: R$ 2,50 - R$ 7,50/dia
- **Pacotes sugeridos** com descontos

### 6. **Como o Sistema de Códigos Funciona**

1. **Admin gera código** no painel (ex: `PC12345678`)
2. **Jogador compra** através de site externo
3. **Recebe o código** por email/WhatsApp
4. **Usa no Discord**: `/codigo PC12345678`
5. **Purple Coins adicionadas** automaticamente
6. **Admin vê no painel** quem usou e quando

### 7. **URLs Importantes**

- **Admin Panel**: `https://seu-dominio.vercel.app/`
- **Emoji Converter**: `https://seu-dominio.vercel.app/emoji-converter`

### 8. **Segurança**

- ✅ Senha de admin protegida (`eojojos`)
- ✅ CORS configurado
- ✅ Validações de entrada
- ✅ Códigos únicos
- ✅ Histórico completo

### 9. **Banco de Dados**

O sistema cria automaticamente as tabelas necessárias:

```sql
-- Tabela para códigos de Purple Coins
CREATE TABLE purple_coin_codes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    code VARCHAR(50) UNIQUE NOT NULL,
    purple_coins_value INT NOT NULL DEFAULT 100,
    used_by_discord_id VARCHAR(50) NULL,
    used_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NULL
);

-- Tabela para histórico de emojis
CREATE TABLE emoji_codes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    emoji VARCHAR(10) NOT NULL,
    hex_code VARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

### 10. **Próximos Passos**

1. ✅ Copie esta pasta para seu novo projeto
2. ✅ Configure as variáveis no Vercel  
3. ✅ Faça o deploy
4. ✅ Teste com a senha `eojojos`
5. ✅ Acesse `/emoji-converter` para converter emojis
6. ✅ Configure o comando `/codigo` no bot Discord

---

**🎯 Tudo pronto para controlar o bot pelo Vercel de forma completa e profissional!**
