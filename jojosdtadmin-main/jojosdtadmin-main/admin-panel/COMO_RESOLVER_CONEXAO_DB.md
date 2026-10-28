# 🔧 GUIA PARA RESOLVER PROBLEMA DE CONEXÃO COM RDS

## ❌ Erro Atual
```
Access denied for user 'jojodreamteam'@'177.22.183.18' (using password: YES)
```

## 📊 Informações Detectadas
- **Seu IP atual:** 177.22.183.18
- **Hostname do banco:** jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com
- **Usuário:** jojodreamteam
- **Porta:** 3306

## 🎯 Soluções Possíveis

### 1. **CONFIGURAR SECURITY GROUP DO RDS** (Mais Provável)

O Security Group do RDS não está permitindo conexões do seu IP. Você precisa:

1. **Acessar o Console AWS:**
   - Vá para: https://console.aws.amazon.com/rds/
   - Faça login na sua conta AWS

2. **Encontrar sua instância RDS:**
   - Clique em "Databases" no menu lateral
   - Procure por "jojodreamteam"

3. **Editar Security Group:**
   - Clique na instância
   - Na aba "Connectivity & security"
   - Clique no Security Group (algo como sg-xxxxxxxxx)

4. **Adicionar regra de entrada:**
   - Clique em "Edit inbound rules"
   - Clique "Add rule"
   - **Type:** MySQL/Aurora
   - **Port:** 3306
   - **Source:** My IP (isso vai adicionar automaticamente seu IP: 177.22.183.18/32)
   - Clique "Save rules"

### 2. **VERIFICAR CREDENCIAIS**

Confirme se as credenciais estão corretas:
- **Usuário:** jojodreamteam
- **Senha:** soufoda123 (conforme .env)

### 3. **TESTAR CONEXÃO APÓS MUDANÇAS**

Após fazer as mudanças no Security Group, teste:

```bash
npm run test-db
```

### 4. **SE AINDA NÃO FUNCIONAR**

Tente estas alternativas:

#### Opção A: Permitir qualquer IP (TEMPORÁRIO - apenas para teste)
- No Security Group, use Source: 0.0.0.0/0
- ⚠️ ATENÇÃO: Isso é inseguro! Use apenas para teste e reverta depois.

#### Opção B: Verificar se a instância RDS está ativa
- No console RDS, verifique se o status é "Available"
- Se estiver "Stopped", clique em "Start"

#### Opção C: Criar um novo usuário no banco
Se você tem acesso ao banco por outro meio, pode criar um usuário específico:

```sql
CREATE USER 'local_admin'@'%' IDENTIFIED BY 'nova_senha';
GRANT ALL PRIVILEGES ON jojodreamteam.* TO 'local_admin'@'%';
FLUSH PRIVILEGES;
```

## 🚨 IMPORTANTE

1. **O erro "Access denied" significa que a rede está OK**, mas as permissões não
2. **Seu IP (177.22.183.18) precisa estar no Security Group**
3. **Após adicionar no Security Group, a conexão deve funcionar imediatamente**

## 📞 Próximos Passos

1. Configure o Security Group conforme instruções acima
2. Execute: `npm run test-db`
3. Se funcionar, execute: `npm run dev`
4. Acesse: http://localhost:3000

## 🔍 Verificação Adicional

Se você não tem acesso ao AWS Console, precisa:
- Contatar o administrador da conta AWS
- Ou obter as credenciais de acesso ao console AWS
- Ou pedir para alguém com acesso configurar o Security Group para seu IP
