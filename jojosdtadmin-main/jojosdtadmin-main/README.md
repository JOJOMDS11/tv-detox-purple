# HaxBall Admin Panel

Projeto do painel administrativo para deploy no Vercel com PostgreSQL/Supabase.

## Variáveis de ambiente no Vercel

Adicionar no painel do Vercel em Project > Settings > Environment Variables:

- `DATABASE_URL` = `postgresql://postgres:SEU_PASSWORD@SEU_HOST:5432/postgres?sslmode=require`
- `ADMIN_PASSWORD` = `sua_senha_secreta`
- `NODE_ENV` = `production`

Opcional:

- `DB_HOST`
- `DB_PORT`
- `DB_USER`
- `DB_PASSWORD`
- `DB_NAME`
- `DB_SSL`
- `DB_POOL_MAX`

## Deploy

1. Conectar o repositório no Vercel
2. Definir o framework como `Other` ou `Node.js`
3. Usar a pasta raiz do projeto
4. Configurar as variáveis acima
5. Fazer deploy

## Senha padrão para login

Se não configurar `ADMIN_PASSWORD`, a app usa `admin123` como fallback.

## Importante

Nunca commitar arquivos `.env` reais. Use `.env.example` como modelo.

