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
        const adminPassword = process.env.ADMIN_PASSWORD || 'admin123';

        if (password === adminPassword) {
            // Retorna o token simples, sem "Bearer" (será adicionado no frontend)
            return res.status(200).json({ success: true, token: adminPassword });
        } else {
            return res.status(200).json({ success: false, error: 'Senha incorreta' });
        }
    } catch (error) {
        console.error('Erro no login:', error);
        return res.status(500).json({ success: false, error: 'Erro interno do servidor' });
    }
};
