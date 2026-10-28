# INSTRUÇÕES PARA CONFIGURAR VARIÁVEIS DE AMBIENTE NO VERCEL

## ⚠️ IMPORTANTE: Configure estas variáveis no painel do Vercel

Acesse: https://vercel.com/joao-pedros-projects-4a1cf1cf/adminsjojos/settings/environment-variables

## Variáveis que DEVEM ser configuradas:

### 1. DB_HOST
- **Name:** DB_HOST
- **Value:** jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com
- **Environment:** Production, Preview, Development

### 2. DB_USER  
- **Name:** DB_USER
- **Value:** jojodreamteam
- **Environment:** Production, Preview, Development

### 3. DB_PASSWORD
- **Name:** DB_PASSWORD
- **Value:** [SUA_SENHA_DO_BANCO_AQUI]
- **Environment:** Production, Preview, Development

### 4. DB_NAME
- **Name:** DB_NAME
- **Value:** jojodreamteam
- **Environment:** Production, Preview, Development

### 5. DB_PORT
- **Name:** DB_PORT
- **Value:** 3306
- **Environment:** Production, Preview, Development

## Como configurar:

1. Acesse o link acima
2. Clique em "Add Variable"
3. Digite o Name e Value
4. Selecione todos os environments (Production, Preview, Development)
5. Clique "Save"
6. Repita para todas as 5 variáveis

## Após configurar:

Execute novamente: `npx vercel --prod`

## ⚠️ ERRO ATUAL:
```
Error: Environment Variable "DB_HOST" references Secret "db_host", which does not exist.
```

**CAUSA:** As variáveis de ambiente não estão configuradas no painel do Vercel.
**SOLUÇÃO:** Configure conforme as instruções acima.
