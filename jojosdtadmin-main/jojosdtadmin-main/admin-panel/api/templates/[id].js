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

        // Verificar se existem cartas usando este template
        const cards = await query('SELECT COUNT(*) as count FROM user_cards WHERE template_id = ?', [id]);

        if (cards[0].count > 0) {
            return res.status(400).json({
                error: `Não é possível deletar. Existem ${cards[0].count} cartas usando este jogador.`
            });
        }

        const result = await query('DELETE FROM player_templates WHERE id = ?', [id]);

        if (result.affectedRows > 0) {
            res.json({ success: true, message: 'Jogador deletado com sucesso!' });
        } else {
            res.status(404).json({ error: 'Jogador não encontrado' });
        }

    } catch (error) {
        console.error('Erro ao deletar template:', error);
        res.status(500).json({ error: 'Erro interno do servidor' });
    }
}
