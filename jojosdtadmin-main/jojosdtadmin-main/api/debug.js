// API Debug para verificar conexão e dados
const database = require('../admin-panel/api/database_vercel');

module.exports = async function handler(req, res) {
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    try {
        console.log('🔍 DEBUG - Testando conexão...');
        
        // Testar conexão
        await database.testConnection();
        
        // Verificar dados reais
        const playersCount = await database.executeQuery('SELECT COUNT(*) as count FROM players');
        const teamsCount = await database.executeQuery('SELECT COUNT(*) as count FROM teams');
        const templatesCount = await database.executeQuery('SELECT COUNT(*) as count FROM player_templates');
        
        const players = await database.executeQuery('SELECT id, discord_id, name FROM players LIMIT 5');
        const teams = await database.executeQuery('SELECT id, name, owner_discord_id FROM teams LIMIT 5');
        const templates = await database.executeQuery('SELECT id, name, position, avatar FROM player_templates LIMIT 5');
        
        return res.status(200).json({
            debug: true,
            connection: 'OK',
            database: process.env.DB_NAME || 'não definido',
            host: process.env.DB_HOST || 'não definido',
            counts: {
                players: playersCount[0]?.count || 0,
                teams: teamsCount[0]?.count || 0,
                templates: templatesCount[0]?.count || 0
            },
            sample_data: {
                players,
                teams,
                templates
            }
        });
        
    } catch (error) {
        console.error('❌ DEBUG ERROR:', error);
        return res.status(500).json({
            debug: true,
            error: error.message,
            stack: error.stack
        });
    }
};
