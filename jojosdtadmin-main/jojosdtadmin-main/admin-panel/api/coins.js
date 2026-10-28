const connection = require('./connection');

module.exports = async function handler(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    try {
        if (req.method === 'GET') {
            const [codes] = await connection.execute(`
                SELECT
                    id,
                    code,
                    purple_coins_value,
                    used_by_discord_id,
                    used_at,
                    created_at,
                    expires_at,
                    created_by,
                    description,
                    CASE
                        WHEN used_by_discord_id IS NOT NULL THEN 'usado'
                        WHEN expires_at < NOW() THEN 'expirado'
                        ELSE 'ativo'
                    END as status
                FROM purple_coin_codes
                ORDER BY created_at DESC
            `);

            return res.status(200).json({ success: true, codes });
        }

        if (req.method === 'POST') {
            const { code, purple_coins_value, expires_at, description } = req.body;

            if (!code || !purple_coins_value) {
                return res.status(400).json({ success: false, message: 'Código e valor são obrigatórios' });
            }

            const [existing] = await connection.execute('SELECT id FROM purple_coin_codes WHERE code = ?', [code]);

            if (existing.length > 0) {
                return res.status(400).json({ success: false, message: 'Este código já existe' });
            }

            await connection.execute(`
                INSERT INTO purple_coin_codes (code, purple_coins_value, amount, expires_at, created_by, description)
                VALUES (?, ?, ?, ?, 'ADMIN_PANEL', ?)
            `, [code, purple_coins_value, purple_coins_value, expires_at || null, description || null]);

            return res.status(200).json({ success: true, message: 'Código criado com sucesso!' });
        }

        if (req.method === 'DELETE') {
            const match = (req.url || '').match(/\/api\/codes\/(\d+)/);
            if (!match) {
                return res.status(400).json({ success: false, message: 'ID do código não fornecido' });
            }

            await connection.execute('DELETE FROM purple_coin_codes WHERE id = ?', [match[1]]);

            return res.status(200).json({ success: true, message: 'Código excluído com sucesso!' });
        }

        return res.status(405).json({ success: false, message: 'Método não permitido' });
    } catch (error) {
        console.error('Erro na API de códigos:', error);
        return res.status(500).json({ success: false, message: 'Erro interno do servidor', details: error.message });
    }
};
