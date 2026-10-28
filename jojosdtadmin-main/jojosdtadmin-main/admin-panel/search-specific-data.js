require('dotenv').config();
const mysql = require('mysql2/promise');

async function buscarDadosEspecificos() {
    try {
        const connection = await mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME,
            port: process.env.DB_PORT || 3306,
            ssl: {
                rejectUnauthorized: false
            },
            connectTimeout: 30000
        });

        console.log('✅ Conectado! Buscando seus dados específicos...\n');
        
        // Buscar jogadores com nomes parecidos com "esquece" e "jojo"
        console.log('🔍 Buscando jogadores "esquece" e "jojo":');
        const [playersSearch] = await connection.execute(`
            SELECT * FROM players 
            WHERE LOWER(name) LIKE '%esquece%' 
               OR LOWER(name) LIKE '%jojo%'
               OR LOWER(name) LIKE '%joao%'
               OR LOWER(name) LIKE '%joão%'
            ORDER BY created_at DESC
        `);
        
        if (playersSearch.length > 0) {
            playersSearch.forEach(player => {
                console.log(`✅ JOGADOR: ${player.name} (ID: ${player.discord_id}, Coins: ${player.purple_coins})`);
            });
        } else {
            console.log('❌ Nenhum jogador encontrado com esses nomes.');
        }
        
        // Buscar times com nomes parecidos com "jojos fc" e "jajas"
        console.log('\n🔍 Buscando times "jojos fc" e "jajas":');
        const [teamsSearch] = await connection.execute(`
            SELECT * FROM teams 
            WHERE LOWER(name) LIKE '%jojo%' 
               OR LOWER(name) LIKE '%jaja%'
               OR LOWER(name) LIKE '%fc%'
            ORDER BY created_at DESC
        `);
        
        if (teamsSearch.length > 0) {
            teamsSearch.forEach(team => {
                console.log(`✅ TIME: ${team.name} (ID: ${team.id}, Dono: ${team.owner_discord_id})`);
            });
        } else {
            console.log('❌ Nenhum time encontrado com esses nomes.');
        }
        
        // Buscar templates com nome "jojo"
        console.log('\n🔍 Buscando template "jojo":');
        const [templatesSearch] = await connection.execute(`
            SELECT * FROM player_templates 
            WHERE LOWER(name) LIKE '%jojo%'
            ORDER BY created_at DESC
        `);
        
        if (templatesSearch.length > 0) {
            templatesSearch.forEach(template => {
                console.log(`✅ TEMPLATE: ${template.name} (ID: ${template.id})`);
            });
        } else {
            console.log('❌ Nenhum template encontrado com nome "jojo".');
        }
        
        // Mostrar TODOS os jogadores para você ver
        console.log('\n📋 TODOS OS JOGADORES NO BANCO:');
        const [allPlayers] = await connection.execute('SELECT * FROM players ORDER BY created_at DESC');
        allPlayers.forEach((player, index) => {
            console.log(`${index + 1}. Nome: "${player.name}" | Discord ID: ${player.discord_id} | Coins: ${player.purple_coins}`);
        });
        
        // Mostrar TODOS os times para você ver
        console.log('\n🏆 TODOS OS TIMES NO BANCO:');
        const [allTeams] = await connection.execute('SELECT * FROM teams ORDER BY created_at DESC');
        allTeams.forEach((team, index) => {
            console.log(`${index + 1}. Nome: "${team.name}" | Dono: ${team.owner_discord_id}`);
        });
        
        await connection.end();
        
    } catch (error) {
        console.error('❌ Erro:', error.message);
    }
}

buscarDadosEspecificos();
