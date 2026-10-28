require('dotenv').config();

console.log('=== Variáveis de ambiente ===');
console.log('DB_HOST:', process.env.DB_HOST);
console.log('DB_USER:', process.env.DB_USER);
console.log('DB_PASSWORD:', process.env.DB_PASSWORD ? '***' : 'undefined');
console.log('DB_NAME:', process.env.DB_NAME);
console.log('DB_PORT:', process.env.DB_PORT);
console.log('NODE_ENV:', process.env.NODE_ENV);

console.log('\n=== Testando conexão básica ===');
const mysql = require('mysql2/promise');

async function testConnection() {
    try {
        console.log('Tentando conectar...');
        const connection = await mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME,
            port: process.env.DB_PORT || 3306
        });
        
        console.log('✅ Conexão bem-sucedida!');
        
        // Teste simples
        const [rows] = await connection.execute('SELECT 1 as test');
        console.log('✅ Query teste funcionou:', rows);
        
        await connection.end();
        
    } catch (error) {
        console.error('❌ Erro na conexão:', error.message);
        console.error('Código:', error.code);
    }
}

testConnection();
