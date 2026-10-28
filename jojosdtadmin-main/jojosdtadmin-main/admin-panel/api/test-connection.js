const connection = require('./connection');

module.exports = async function handler(req, res) {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (req.method !== 'GET') {
        return res.status(405).json({ error: 'Método não permitido' });
    }

    try {
        console.log('=== TESTE DE CONEXÃO ===');
        console.log('Variáveis de ambiente:');
        console.log('DB_HOST:', process.env.DB_HOST ? '✅ Definida' : '❌ Não definida');
        console.log('DB_USER:', process.env.DB_USER ? '✅ Definida' : '❌ Não definida');
        console.log('DB_PASSWORD:', process.env.DB_PASSWORD ? '✅ Definida' : '❌ Não definida');
        console.log('DB_NAME:', process.env.DB_NAME ? '✅ Definida' : '❌ Não definida');
        console.log('DB_PORT:', process.env.DB_PORT ? '✅ Definida' : '❌ Não definida');

        // Test database connection
        console.log('Testando ping no banco...');
        await connection.ping();
        console.log('Ping realizado com sucesso!');

        // Test a simple query
        console.log('Executando query de teste...');
        const [result] = await connection.execute('SELECT 1 as test, NOW() as current_time');
        console.log('Query executada com sucesso:', result);

        return res.status(200).json({
            status: 'success',
            message: 'Conexão com banco de dados OK',
            timestamp: new Date().toISOString(),
            test_query: result[0].test === 1 ? 'OK' : 'FAILED',
            current_time: result[0].current_time,
            env_vars: {
                DB_HOST: process.env.DB_HOST ? 'SET' : 'NOT_SET',
                DB_USER: process.env.DB_USER ? 'SET' : 'NOT_SET',
                DB_PASSWORD: process.env.DB_PASSWORD ? 'SET' : 'NOT_SET',
                DB_NAME: process.env.DB_NAME ? 'SET' : 'NOT_SET',
                DB_PORT: process.env.DB_PORT ? 'SET' : 'NOT_SET'
            }
        });

    } catch (error) {
        console.error('Database connection test failed:', error);
        return res.status(500).json({
            status: 'error',
            message: 'Erro na conexão com banco de dados',
            error: error.message,
            code: error.code,
            timestamp: new Date().toISOString(),
            env_vars: {
                DB_HOST: process.env.DB_HOST ? 'SET' : 'NOT_SET',
                DB_USER: process.env.DB_USER ? 'SET' : 'NOT_SET',
                DB_PASSWORD: process.env.DB_PASSWORD ? 'SET' : 'NOT_SET',
                DB_NAME: process.env.DB_NAME ? 'SET' : 'NOT_SET',
                DB_PORT: process.env.DB_PORT ? 'SET' : 'NOT_SET'
            }
        });
    }
};
