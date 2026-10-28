const connection = require('./connection');

const calculatePoints = (wins, draws) => (wins * 3 + draws);

module.exports = async function handler(req, res) {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (req.method !== 'GET') {
        return res.status(405).json({ error: 'Método não permitido' });
    }

    // Verificar autenticação simples
    const adminKey = req.headers.authorization;
    const expected = `Bearer ${process.env.ADMIN_PASSWORD || 'admin123'}`;
    if (adminKey !== expected) {
        return res.status(401).json({ error: 'Não autorizado' });
    }

    try {
        // Get all statistics - with table existence checks
        const stats = {};

        // Check which tables exist
        const [allTables] = await connection.execute("SHOW TABLES");
        const tableNames = allTables.map(row => Object.values(row)[0].toLowerCase());
        console.log('Available tables:', tableNames);

        try {
            // Total templates
            if (tableNames.includes('player_templates')) {
                const [templatesCount] = await connection.execute(
                    'SELECT COUNT(*) as count FROM player_templates'
                );
                stats.totalTemplates = templatesCount[0].count;
            } else {
                stats.totalTemplates = 0;
                console.log('player_templates table does not exist');
            }
        } catch (error) {
            console.log('Error counting templates:', error.message);
            stats.totalTemplates = 0;
        }

        try {
            // Total teams/players
            if (tableNames.includes('players')) {
                const [teamsCount] = await connection.execute(
                    'SELECT COUNT(*) as count FROM players'
                );
                stats.totalTeams = teamsCount[0].count;
            } else {
                stats.totalTeams = 0;
                console.log('players table does not exist');
            }
        } catch (error) {
            console.log('Error counting players:', error.message);
            stats.totalTeams = 0;
        }

        try {
            // Total cards
            if (tableNames.includes('user_cards')) {
                const [cardsCount] = await connection.execute(
                    'SELECT COUNT(*) as count FROM user_cards'
                );
                stats.totalCards = cardsCount[0].count;
            } else {
                stats.totalCards = 0;
                console.log('user_cards table does not exist');
            }
        } catch (error) {
            console.log('Error counting cards:', error.message);
            stats.totalCards = 0;
        }

        try {
            // Total purple coins in circulation
            if (tableNames.includes('players')) {
                // Check if purple_coins column exists
                const [columns] = await connection.execute("SHOW COLUMNS FROM players LIKE 'purple_coins'");
                if (columns.length > 0) {
                    const [coinsSum] = await connection.execute(
                        'SELECT SUM(purple_coins) as total FROM players WHERE purple_coins IS NOT NULL'
                    );
                    stats.totalCoins = coinsSum[0].total || 0;
                } else {
                    stats.totalCoins = 0;
                    console.log('purple_coins column does not exist in players table');
                }
            } else {
                stats.totalCoins = 0;
            }
        } catch (error) {
            console.log('Error calculating total coins:', error.message);
            stats.totalCoins = 0;
        }

        try {
            // Templates by position
            if (tableNames.includes('player_templates')) {
                const [positionStats] = await connection.execute(`
                    SELECT position, COUNT(*) as count 
                    FROM player_templates 
                    GROUP BY position
                `);
                stats.templatesByPosition = positionStats;
            } else {
                stats.templatesByPosition = [];
            }
        } catch (error) {
            console.log('Error getting templates by position:', error.message);
            stats.templatesByPosition = [];
        }

        try {
            // Cards by rarity
            if (tableNames.includes('user_cards')) {
                const [rarityStats] = await connection.execute(`
                    SELECT rarity, COUNT(*) as count 
                    FROM user_cards 
                    GROUP BY rarity
                `);
                stats.cardsByRarity = rarityStats;
            } else {
                stats.cardsByRarity = [];
            }
        } catch (error) {
            console.log('Error getting cards by rarity:', error.message);
            stats.cardsByRarity = [];
        }

        try {
            // Recent activity
            if (tableNames.includes('user_cards')) {
                // usar created_at (coluna real) em vez de obtained_at
                const [recentActivity] = await connection.execute(`
                    SELECT COUNT(*) as count 
                    FROM user_cards 
                    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
                `);
                stats.cardsLast24h = recentActivity[0].count;
            } else {
                stats.cardsLast24h = 0;
            }
        } catch (error) {
            console.log('Error getting recent activity:', error.message);
            stats.cardsLast24h = 0;
        }

        return res.status(200).json(stats);

    } catch (error) {
        console.error('Database connection error:', error);
        return res.status(500).json({
            error: 'Erro de conexão com banco de dados',
            details: error.message
        });
    }
};
