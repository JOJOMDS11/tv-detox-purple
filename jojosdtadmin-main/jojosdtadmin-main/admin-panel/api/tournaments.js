const connection = require('./connection');

module.exports = async function handler(req, res) {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, DELETE, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    try {
        if (req.method === 'GET') {
            // Buscar torneios
            const [tournaments] = await connection.execute(`
                SELECT t.*, t.max_teams AS max_players,
                    (SELECT COUNT(*) FROM tournament_registrations r WHERE r.tournament_id = t.id) AS registered_teams
                FROM tournaments t
                ORDER BY t.created_at DESC
            `);

            return res.status(200).json({
                success: true,
                tournaments: tournaments
            });

        } else if (req.method === 'POST') {
            const { name, date, time, format, max_players, prize_1st, prize_2nd, prize_3rd } = req.body;

            if (!name || !date || !time) {
                return res.status(400).json({
                    success: false,
                    message: 'Nome, data e hora são obrigatórios'
                });
            }

            const slugName = String(name)
                .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
                .replace(/[^\w\s-]/g, '')
                .trim().toLowerCase().replace(/\s+/g, '_');

            const [existing] = await connection.execute(
                'SELECT id FROM tournaments WHERE name = ?',
                [slugName]
            );

            if (existing.length > 0) {
                return res.status(400).json({
                    success: false,
                    message: 'Já existe um torneio com este nome'
                });
            }

            // Criar torneio
            const [result] = await connection.execute(`
                INSERT INTO tournaments (name, date, time, format, max_teams, prize_1st, prize_2nd, prize_3rd, status, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'upcoming', NOW())
            `, [
                slugName,
                date, 
                time, 
                format || 'Mata-mata', 
                max_players || 8, 
                prize_1st || '1000', 
                prize_2nd || '500', 
                prize_3rd || '250'
            ]);

            return res.status(200).json({
                success: true,
                message: 'Torneio criado com sucesso!',
                tournament_id: result.insertId
            });

        } else if (req.method === 'DELETE') {
            // Extrair ID da URL
            const url = req.url || '';
            const match = url.match(/\/api\/tournaments\/(\d+)/);
            if (!match) {
                return res.status(400).json({
                    success: false,
                    message: 'ID do torneio não fornecido'
                });
            }

            const tournamentId = match[1];

            // Verificar se o torneio existe
            const [tournament] = await connection.execute(
                'SELECT id FROM tournaments WHERE id = ?',
                [tournamentId]
            );

            if (tournament.length === 0) {
                return res.status(404).json({
                    success: false,
                    message: 'Torneio não encontrado'
                });
            }

            // Deletar torneio
            await connection.execute('DELETE FROM tournaments WHERE id = ?', [tournamentId]);

            return res.status(200).json({
                success: true,
                message: 'Torneio deletado com sucesso!'
            });
        }

    } catch (error) {
        console.error('Erro na API de torneios:', error);
        return res.status(500).json({ 
            success: false, 
            message: 'Erro interno do servidor',
            details: error.message 
        });
    }
};
