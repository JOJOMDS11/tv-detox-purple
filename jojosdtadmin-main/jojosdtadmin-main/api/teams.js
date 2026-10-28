// API Teams para Vercel
const database = require('../admin-panel/api/database_vercel');

module.exports = async function handler(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    const adminKey = req.headers.authorization;
    if (adminKey !== 'Bearer admin123') {
        return res.status(401).json({ error: 'Não autorizado' });
    }

    try {
        if (req.method === 'GET') {
            try {
                const rows = await database.executeQuery(
                    'SELECT id, team_name, discord_id, wins, losses, created_at FROM teams ORDER BY created_at DESC'
                );
                return res.status(200).json(rows);
            } catch (error) {
                console.error('Error fetching teams:', error);
                return res.status(200).json([]);
            }
        } else if (req.method === 'DELETE') {
            const { id } = req.body;
            if (!id) {
                return res.status(400).json({ error: 'ID é obrigatório' });
            }
            try {
                await database.executeQuery('DELETE FROM teams WHERE id = ?', [id]);
                return res.status(200).json({ message: 'Time excluído com sucesso' });
            } catch (error) {
                console.error('Error deleting team:', error);
                return res.status(500).json({ error: 'Erro ao excluir time' });
            }
        } else {
            return res.status(405).json({ error: 'Método não permitido' });
        }
    } catch (error) {
        console.error('Database error:', error);
        return res.status(500).json({ error: 'Erro interno do servidor' });
    }
};
