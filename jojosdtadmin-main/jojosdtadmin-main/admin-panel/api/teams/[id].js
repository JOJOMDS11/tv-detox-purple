const { query } = require('../db');

export default async function handler(req, res) {
    if (req.method !== 'DELETE') {
        return res.status(405).json({ error: 'Method not allowed' });
    }

    try {
        const { id } = req.query;

        if (!id) {
            return res.status(400).json({ error: 'ID é obrigatório' });
        }

        const result = await query('DELETE FROM teams WHERE id = ?', [id]);

        if (result.affectedRows > 0) {
            res.json({ success: true, message: 'Time deletado com sucesso!' });
        } else {
            res.status(404).json({ error: 'Time não encontrado' });
        }

    } catch (error) {
        console.error('Erro ao deletar time:', error);
        res.status(500).json({ error: 'Erro interno do servidor' });
    }
}
