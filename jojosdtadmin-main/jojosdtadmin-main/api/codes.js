// API Códigos Purple Coins para Vercel
const database = require('../admin-panel/api/database_pix');

module.exports = async function handler(req, res) {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    // Verificar autenticação simples
    const adminKey = req.headers.authorization;
    if (adminKey !== 'Bearer admin123') {
        return res.status(401).json({ error: 'Não autorizado' });
    }

    try {
        if (req.method === 'GET') {
            // Get all códigos
            try {
                // First, ensure table exists
                await database.executeQuery(`
                    CREATE TABLE IF NOT EXISTS purple_coins_codes (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        code VARCHAR(255) UNIQUE NOT NULL,
                        coin_amount INT NOT NULL,
                        expiry_hours INT NOT NULL,
                        max_uses INT NOT NULL,
                        uses_count INT DEFAULT 0,
                        is_active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                `);

                const rows = await database.executeQuery(
                    'SELECT id, code, coin_amount, expiry_hours, max_uses, uses_count, created_at, is_active FROM purple_coins_codes ORDER BY created_at DESC'
                );
                return res.status(200).json(rows);
            } catch (error) {
                console.error('Error fetching códigos:', error);
                return res.status(200).json([]);
            }

        } else if (req.method === 'POST') {
            // Create new código
            const { coin_amount, expiry_hours, max_uses } = req.body;

            if (!coin_amount || !expiry_hours || !max_uses) {
                return res.status(400).json({ error: 'Valor, horas de expiração e máximo de usos são obrigatórios' });
            }

            try {
                // First, ensure table exists
                await database.executeQuery(`
                    CREATE TABLE IF NOT EXISTS purple_coins_codes (
                        id INT AUTO_INCREMENT PRIMARY KEY,
                        code VARCHAR(255) UNIQUE NOT NULL,
                        coin_amount INT NOT NULL,
                        expiry_hours INT NOT NULL,
                        max_uses INT NOT NULL,
                        uses_count INT DEFAULT 0,
                        is_active BOOLEAN DEFAULT TRUE,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                `);

                // Generate unique code
                const code = 'PC' + Date.now().toString().slice(-6) + Math.random().toString(36).substr(2, 4).toUpperCase();

                // Insert new código
                const result = await database.executeQuery(
                    'INSERT INTO purple_coins_codes (code, coin_amount, expiry_hours, max_uses, uses_count, created_at, is_active) VALUES (?, ?, ?, ?, 0, NOW(), 1)',
                    [code, coin_amount, expiry_hours, max_uses]
                );

                return res.status(201).json({
                    id: result.insertId,
                    code: code,
                    message: 'Código criado com sucesso'
                });
            } catch (error) {
                console.error('Error creating código:', error);
                return res.status(500).json({ error: 'Erro ao criar código: ' + error.message });
            }

        } else if (req.method === 'PUT') {
            // Verificar código
            const { code } = req.body;

            if (!code) {
                return res.status(400).json({ error: 'Código é obrigatório' });
            }

            try {
                // Check if code exists and is valid
                const existing = await database.executeQuery(
                    `SELECT id, coin_amount, max_uses, uses_count, 
                     TIMESTAMPDIFF(HOUR, created_at, NOW()) as hours_since_creation,
                     expiry_hours, is_active 
                     FROM purple_coins_codes WHERE code = ?`,
                    [code]
                );

                if (existing.length === 0) {
                    return res.status(404).json({ error: 'Código não encontrado' });
                }

                const codeData = existing[0];

                // Check if code is still active
                if (!codeData.is_active) {
                    return res.status(400).json({ error: 'Código desativado' });
                }

                // Check if code has expired
                if (codeData.hours_since_creation >= codeData.expiry_hours) {
                    return res.status(400).json({ error: 'Código expirado' });
                }

                // Check if code has reached max uses
                if (codeData.uses_count >= codeData.max_uses) {
                    return res.status(400).json({ error: 'Código esgotado' });
                }

                return res.status(200).json({
                    valid: true,
                    coin_amount: codeData.coin_amount,
                    remaining_uses: codeData.max_uses - codeData.uses_count,
                    message: 'Código válido'
                });

            } catch (error) {
                console.error('Error verifying código:', error);
                return res.status(500).json({ error: 'Erro ao verificar código' });
            }

        } else if (req.method === 'DELETE') {
            // Delete código
            const { id } = req.body;

            if (!id) {
                return res.status(400).json({ error: 'ID é obrigatório' });
            }

            try {
                // Delete código
                await database.executeQuery(
                    'DELETE FROM purple_coins_codes WHERE id = ?',
                    [id]
                );

                return res.status(200).json({ message: 'Código excluído com sucesso' });

            } catch (error) {
                console.error('Error deleting código:', error);
                return res.status(500).json({ error: 'Erro ao excluir código' });
            }

        } else {
            return res.status(405).json({ error: 'Método não permitido' });
        }

    } catch (error) {
        console.error('Database error:', error);
        return res.status(500).json({
            error: 'Erro interno do servidor',
            details: error.message
        });
    }
};
