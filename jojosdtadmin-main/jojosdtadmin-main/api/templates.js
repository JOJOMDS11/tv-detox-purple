// API Templates para Vercel
const database = require('../admin-panel/api/database_vercel');

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
            // Get all templates
            try {
                const rows = await database.executeQuery(
                    'SELECT id, name, position, avatar, created_at FROM player_templates ORDER BY created_at DESC'
                );
                return res.status(200).json(rows);
            } catch (error) {
                console.error('Error fetching templates:', error);
                return res.status(200).json([]);
            }

        } else if (req.method === 'POST') {
            // Create new template
            const { name, position, avatar } = req.body;

            if (!name || !position) {
                return res.status(400).json({ error: 'Nome e posição são obrigatórios' });
            }

            try {
                // Check if name already exists
                const existing = await database.executeQuery(
                    'SELECT id FROM player_templates WHERE name = ?',
                    [name]
                );

                if (existing.length > 0) {
                    return res.status(400).json({ error: 'Já existe um template com este nome' });
                }

                // Insert new template
                const result = await database.executeQuery(
                    'INSERT INTO player_templates (name, position, avatar) VALUES (?, ?, ?)',
                    [name, position, avatar || '⚽']
                );

                return res.status(201).json({
                    id: result.insertId,
                    message: 'Template criado com sucesso'
                });
            } catch (error) {
                console.error('Error creating template:', error);
                return res.status(500).json({ error: 'Erro ao criar template' });
            }

        } else if (req.method === 'PUT') {
            // Edit existing template
            const { id, name, position, avatar } = req.body;

            if (!id || !name || !position) {
                return res.status(400).json({ error: 'ID, nome e posição são obrigatórios' });
            }

            try {
                // Check if template exists
                const existing = await database.executeQuery(
                    'SELECT id FROM player_templates WHERE id = ?',
                    [id]
                );

                if (existing.length === 0) {
                    return res.status(404).json({ error: 'Template não encontrado' });
                }

                // Update template
                await database.executeQuery(
                    'UPDATE player_templates SET name = ?, position = ?, avatar = ? WHERE id = ?',
                    [name, position, avatar || '⚽', id]
                );

                return res.status(200).json({ message: 'Template atualizado com sucesso' });

            } catch (error) {
                console.error('Error updating template:', error);
                return res.status(500).json({ error: 'Erro ao atualizar template' });
            }

        } else if (req.method === 'DELETE') {
            // Delete template
            const { id } = req.body;

            if (!id) {
                return res.status(400).json({ error: 'ID é obrigatório' });
            }

            try {
                // Delete template
                await database.executeQuery(
                    'DELETE FROM player_templates WHERE id = ?',
                    [id]
                );

                return res.status(200).json({ message: 'Template excluído com sucesso' });

            } catch (error) {
                console.error('Error deleting template:', error);
                return res.status(500).json({ error: 'Erro ao excluir template' });
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
