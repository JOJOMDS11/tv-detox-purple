require('dotenv').config();
const mysql = require('mysql2/promise');

// Diferentes combinações de credenciais para testar
const credentials = [
    {
        name: 'Credenciais do .env',
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        port: process.env.DB_PORT || 3306
    },
    {
        name: 'Usuário admin',
        host: process.env.DB_HOST,
        user: 'admin',
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        port: process.env.DB_PORT || 3306
    },
    {
        name: 'Usuário root',
        host: process.env.DB_HOST,
        user: 'root',
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        port: process.env.DB_PORT || 3306
    },
    {
        name: 'Sem database específico',
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        port: process.env.DB_PORT || 3306
    }
];

async function testCredentials() {
    console.log('🔐 Testando diferentes credenciais...\n');
    
    for (const cred of credentials) {
        console.log(`📋 Testando: ${cred.name}`);
        console.log(`   Host: ${cred.host}`);
        console.log(`   User: ${cred.user}`);
        console.log(`   Database: ${cred.database || 'N/A'}`);
        
        try {
            const connection = await mysql.createConnection({
                host: cred.host,
                user: cred.user,
                password: cred.password,
                database: cred.database,
                port: cred.port,
                ssl: {
                    rejectUnauthorized: false
                },
                connectTimeout: 15000
            });
            
            console.log('   ✅ SUCESSO! Conexão estabelecida');
            
            // Teste básico
            try {
                const [rows] = await connection.execute('SELECT 1 as test');
                console.log('   ✅ Query de teste funcionou');
                
                // Listar databases
                try {
                    const [dbs] = await connection.execute('SHOW DATABASES');
                    console.log('   📊 Databases disponíveis:', dbs.map(db => db.Database).join(', '));
                } catch (e) {
                    console.log('   ⚠️ Não foi possível listar databases');
                }
                
            } catch (e) {
                console.log('   ⚠️ Query de teste falhou:', e.message);
            }
            
            await connection.end();
            
            // Se chegou aqui, encontrou credenciais válidas
            console.log('\n🎉 CREDENCIAIS VÁLIDAS ENCONTRADAS!');
            console.log('Use estas credenciais no .env:');
            console.log(`DB_HOST=${cred.host}`);
            console.log(`DB_USER=${cred.user}`);
            console.log(`DB_PASSWORD=${cred.password}`);
            console.log(`DB_NAME=${cred.database || ''}`);
            console.log(`DB_PORT=${cred.port}`);
            break;
            
        } catch (error) {
            console.log(`   ❌ Falhou: ${error.message}`);
        }
        
        console.log('');
    }
}

testCredentials().catch(console.error);
