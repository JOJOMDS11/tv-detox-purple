const { query } = require('./db');

module.exports = async function handler(req, res) {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (req.method !== 'POST') {
        return res.status(405).json({ error: 'Método não permitido' });
    }

    try {
        const { password } = req.body;

        // Senha de admin (pode ser configurada como variável de ambiente)
        const adminPassword = process.env.ADMIN_PASSWORD || 'admin123';

        console.log('Tentativa de login:', {
            password_received: password ? '***' : 'empty',
            timestamp: new Date().toISOString()
        });

        if (!password) {
            console.log('Senha não fornecida');
            return res.status(400).json({
                success: false,
                error: 'Senha é obrigatória'
            });
        }

        if (password === adminPassword) {
            console.log('Login bem-sucedido!');

            // Opcionalmente, teste a conexão com o banco
            try {
                await query('SELECT 1 as test');
                console.log('Conexão com banco OK');
            } catch (dbError) {
                console.warn('Aviso: Conexão com banco falhou:', dbError.message);
                // Mas ainda permite o login
            }

            return res.status(200).json({
                success: true,
                message: 'Login realizado com sucesso'
            });
        } else {
            console.log('Senha incorreta');
            return res.status(200).json({
                success: false,
                error: 'Senha incorreta'
            });
        }
    } catch (error) {
        console.error('Erro no login:', error);
        return res.status(500).json({
            success: false,
            error: 'Erro interno do servidor: ' + error.message
        });
    }
};