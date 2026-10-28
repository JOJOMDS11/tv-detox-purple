const connection = require('./connection');

module.exports = async function handler(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (req.method !== 'GET') {
        return res.status(405).json({ error: 'Método não permitido' });
    }

    // Autenticação simples
    const adminKey = req.headers.authorization;
    const expected = `Bearer ${process.env.ADMIN_PASSWORD || 'admin123'}`;
    if (adminKey !== expected) {
        return res.status(401).json({ error: 'Não autorizado' });
    }

    try {
        // Retornar lista de jogadores a partir da tabela `players`.
        // Evitamos aqui consultas adicionais à tabela `user_cards` que
        // referenciavam colunas que não existem no schema atual.
        const [players] = await connection.execute(`
            SELECT 
                discord_id,
                name,
                purple_coins,
                created_at,
                NULL::text AS position,
                NULL::text AS avatar
            FROM players 
            ORDER BY purple_coins DESC
        `);

        return res.status(200).json({ success: true, data: players });
    } catch (error) {
        console.error('Erro ao buscar jogadores:', error);
        return res.status(500).json({ success: false, error: 'Erro ao buscar jogadores', details: error.message });
    }
};
