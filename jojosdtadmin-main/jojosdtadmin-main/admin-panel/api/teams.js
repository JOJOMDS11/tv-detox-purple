const connection = require('./connection');

module.exports = async function handler(req, res) {
    // Enable CORS
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, PUT, OPTIONS');
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
            // Get all registered teams - with dynamic column detection
            try {
                // Check if teams table exists
                const [tables] = await connection.execute(
                    "SHOW TABLES LIKE 'teams'"
                );

                if (tables.length === 0) {
                    console.log('teams table does not exist');
                    return res.status(200).json([]);
                }

                // Get available columns
                const [columns] = await connection.execute(
                    "SHOW COLUMNS FROM teams"
                );

                const columnNames = columns.map(col => col.Field);
                console.log('Available columns in teams table:', columnNames);

                // Build query based on available columns
                let selectFields = [];
                const possibleColumns = [
                    'id', 'discord_id', 'name', 'wins', 'losses', 'draws',
                    'goals_for', 'goals_against', 'goal_difference', 'points',
                    'purple_coins', 'created_at', 'updated_at'
                ];

                possibleColumns.forEach(col => {
                    if (columnNames.includes(col)) {
                        selectFields.push(col);
                    }
                });

                if (selectFields.length === 0) {
                    selectFields = ['*']; // fallback to all columns
                }

                const orderBy = columnNames.includes('created_at') ? 'created_at DESC' :
                    columnNames.includes('id') ? 'id' : '1';

                const query = `
                    SELECT 
                        t.id,
                        t.name AS team_name,
                        t.owner_discord_id,
                        t.wins,
                        t.losses,
                        t.draws,
                        (t.wins * 3 + t.draws) AS points,
                        t.created_at,
                        t.updated_at
                    FROM teams t
                    LEFT JOIN players p ON t.owner_discord_id = p.discord_id
                    GROUP BY t.id, t.name, t.owner_discord_id,
                             t.wins, t.losses, t.draws, t.created_at, t.updated_at
                    ORDER BY points DESC, t.wins DESC, t.created_at DESC`;

                // Get teams with player information
                const [teams] = await connection.execute(query);

                return res.status(200).json(teams);

            } catch (error) {
                console.error('Error fetching teams:', error);
                return res.status(200).json([]);
            }

        } else {
            return res.status(405).json({ error: 'Método não permitido' });
        }

    } catch (error) {
        console.error('Database connection error:', error);
        return res.status(500).json({
            error: 'Erro de conexão com banco de dados',
            details: error.message
        });
    }
};
