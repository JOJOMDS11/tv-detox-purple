// API Login para Vercel Serverless
const database = require('../admin-panel/api/database_vercel');

module.exports = async function handler(req, res) {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (req.method !== 'POST') {
        return res.status(405).json({ error: 'Método não permitido' });
    }

    const { password } = req.body;

    console.log('🔐 [VERCEL] Tentativa de login...');

    // Primeiro testa a conexão com o banco
    try {
        const connectionTest = await database.testConnection();
        if (!connectionTest) {
            console.log('❌ [VERCEL] Falha na conexão com banco de dados');
            return res.status(500).json({ success: false, message: 'Erro de conexão com banco de dados!' });
        }
        console.log('✅ [VERCEL] Conexão com banco OK');
    } catch (error) {
        console.error('❌ [VERCEL] Erro de conexão:', error.message);
        return res.status(500).json({ success: false, message: 'Erro de conexão!' });
    }

    // Verificar senha
    if (password === process.env.ADMIN_PASSWORD || password === 'admin123') {
        console.log('✅ [VERCEL] Login bem-sucedido!');
        return res.status(200).json({ success: true, message: 'Login realizado com sucesso!' });
    } else {
        console.log('❌ [VERCEL] Senha incorreta');
        return res.status(401).json({ success: false, message: 'Senha incorreta!' });
    }
};
