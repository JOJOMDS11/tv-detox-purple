// API Stats para Vercel
const database = require('../admin-panel/api/database_vercel');

module.exports = async function handler(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
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
                // Get stats with fallback
                const stats = {
                    totalUsers: 0,
                    totalTeams: 0,
                    totalCards: 0,
                    totalTemplates: 0
                };

                try {
                    const [users] = await Promise.all([
                        database.executeQuery('SELECT COUNT(*) as count FROM players')
                    ]);
                    stats.totalUsers = users[0]?.count || 0;
                } catch (e) {
                    console.log('Players table not accessible');
                }

                try {
                    const [teams] = await Promise.all([
                        database.executeQuery('SELECT COUNT(*) as count FROM teams')
                    ]);
                    stats.totalTeams = teams[0]?.count || 0;
                } catch (e) {
                    console.log('Teams table not accessible');
                }

                try {
                    const [templates] = await Promise.all([
                        database.executeQuery('SELECT COUNT(*) as count FROM player_templates')
                    ]);
                    stats.totalTemplates = templates[0]?.count || 0;
                } catch (e) {
                    console.log('Templates table not accessible');
                }

                return res.status(200).json(stats);
            } catch (error) {
                console.error('Error fetching stats:', error);
                return res.status(200).json({
                    totalUsers: 0,
                    totalTeams: 0,
                    totalCards: 0,
                    totalTemplates: 0
                });
            }
        } else {
            return res.status(405).json({ error: 'Método não permitido' });
        }
    } catch (error) {
        console.error('Database error:', error);
        return res.status(500).json({ error: 'Erro interno do servidor' });
    }
};
