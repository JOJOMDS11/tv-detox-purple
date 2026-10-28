const connection = require('./connection');

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
            // Get all templates - with multiple fallback strategies
            try {
                // Try to check if table exists first
                const [tables] = await connection.execute(
                    "SHOW TABLES LIKE 'player_templates'"
                );

                if (tables.length === 0) {
                    console.log('player_templates table does not exist');
                    return res.status(200).json([]);
                }

                // Table exists, try to get columns
                const [columns] = await connection.execute(
                    "SHOW COLUMNS FROM player_templates"
                );

                const columnNames = columns.map(col => col.Field);
                console.log('Available columns:', columnNames);

                // Build query based on available columns
                let selectFields = 'id, name, position';
                if (columnNames.includes('avatar')) selectFields += ', avatar';
                if (columnNames.includes('created_at')) selectFields += ', created_at';

                const orderBy = columnNames.includes('created_at') ? 'created_at DESC' : 'id DESC';

                const [rows] = await connection.execute(
                    `SELECT ${selectFields} FROM player_templates ORDER BY ${orderBy}`
                );

                return res.status(200).json(rows);

            } catch (error) {
                console.error('Error fetching templates:', error);
                // Return empty array if any error occurs
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
                const [existing] = await connection.execute(
                    'SELECT id FROM player_templates WHERE name = ?',
                    [name]
                );

                if (existing.length > 0) {
                    return res.status(400).json({ error: 'Já existe um template com este nome' });
                }

                // Insert new template
                const [result] = await connection.execute(
                    'INSERT INTO player_templates (name, position, avatar) VALUES (?, ?, ?)',
                    [name, position, avatar || '⚽']
                );

                return res.status(201).json({
                    id: result.insertId,
                    message: 'Template criado com sucesso'
                });
            } catch (insertError) {
                console.error('Error creating template:', insertError);

                // Try alternative insert if table structure is different
                try {
                    const [result] = await connection.execute(
                        'INSERT INTO player_templates (name, position, avatar, created_at) VALUES (?, ?, ?, NOW())',
                        [name, position, avatar || '⚽']
                    );

                    return res.status(201).json({
                        id: result.insertId,
                        message: 'Template criado com sucesso'
                    });
                } catch (fallbackError) {
                    throw fallbackError;
                }
            }

        } else if (req.method === 'PUT') {
            // Edit existing template
            const { id, name, position, avatar } = req.body;

            if (!id || !name || !position) {
                return res.status(400).json({ error: 'ID, nome e posição são obrigatórios' });
            }

            try {
                // Check if template exists
                const [existing] = await connection.execute(
                    'SELECT id FROM player_templates WHERE id = ?',
                    [id]
                );

                if (existing.length === 0) {
                    return res.status(404).json({ error: 'Template não encontrado' });
                }

                // Check if name already exists for other templates
                const [nameCheck] = await connection.execute(
                    'SELECT id FROM player_templates WHERE name = ? AND id != ?',
                    [name, id]
                );

                if (nameCheck.length > 0) {
                    return res.status(400).json({ error: 'Já existe outro template com este nome' });
                }

                // Update template
                await connection.execute(
                    'UPDATE player_templates SET name = ?, position = ?, avatar = ? WHERE id = ?',
                    [name, position, avatar || '⚽', id]
                );

                return res.status(200).json({
                    message: 'Template atualizado com sucesso'
                });

            } catch (updateError) {
                console.error('Error updating template:', updateError);
                throw updateError;
            }

        } else if (req.method === 'DELETE') {
            // Delete template
            const { id } = req.body;

            if (!id) {
                return res.status(400).json({ error: 'ID é obrigatório' });
            }

            try {
                // Check if template exists
                const [existing] = await connection.execute(
                    'SELECT id FROM player_templates WHERE id = ?',
                    [id]
                );

                if (existing.length === 0) {
                    return res.status(404).json({ error: 'Template não encontrado' });
                }

                // Delete template
                await connection.execute(
                    'DELETE FROM player_templates WHERE id = ?',
                    [id]
                );

                return res.status(200).json({
                    message: 'Template excluído com sucesso'
                });

            } catch (deleteError) {
                console.error('Error deleting template:', deleteError);
                throw deleteError;
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
