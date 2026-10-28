require('dotenv').config();
const mysql = require('mysql2/promise');

async function testarConexao() {
    try {
        console.log('🔍 Testando conexão com as credenciais:');
        console.log('DB_HOST:', process.env.DB_HOST);
        console.log('DB_USER:', process.env.DB_USER);
        console.log('DB_NAME:', process.env.DB_NAME);
        console.log('DB_PASSWORD:', process.env.DB_PASSWORD ? '***DEFINIDA***' : 'NÃO DEFINIDA');
        
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

        console.log('✅ Conexão estabelecida com sucesso!');
        
        // Verificar tabelas existentes
        console.log('\n📋 Verificando tabelas...');
        const [tables] = await connection.execute('SHOW TABLES');
        console.log('Tabelas encontradas:', tables.map(t => Object.values(t)[0]));
        
        // Buscar jogadores
        console.log('\n👥 Buscando jogadores...');
        const [players] = await connection.execute('SELECT * FROM players ORDER BY created_at DESC');
        console.log('Jogadores encontrados:', players.length);
        players.forEach(player => {
            console.log(`- ID: ${player.discord_id}, Nome: ${player.username}, Purple Coins: ${player.purple_coins || 0}`);
        });
        
        // Buscar times
        console.log('\n🏆 Buscando times...');
        const [teams] = await connection.execute('SELECT * FROM teams ORDER BY created_at DESC');
        console.log('Times encontrados:', teams.length);
        teams.forEach(team => {
            console.log(`- ID: ${team.id}, Nome: ${team.name}, Dono: ${team.owner_discord_id}`);
        });
        
        // Buscar cartas
        console.log('\n🎴 Buscando cartas...');
        const [cards] = await connection.execute('SELECT COUNT(*) as total FROM user_cards');
        console.log('Total de cartas:', cards[0].total);
        
        // Buscar templates
        console.log('\n⚙️ Buscando templates...');
        const [templates] = await connection.execute('SELECT * FROM player_templates ORDER BY created_at DESC');
        console.log('Templates encontrados:', templates.length);
        templates.forEach(template => {
            console.log(`- ID: ${template.id}, Nome: ${template.name}, Posição: ${template.position}, Avatar: ${template.avatar}`);
        });
        
        await connection.end();
        console.log('\n✅ Teste concluído com sucesso!');
        
    } catch (error) {
        console.error('❌ Erro no teste:', error.message);
    }
}

testarConexao();
