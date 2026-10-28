require('dotenv').config();
const mysql = require('mysql2/promise');

async function createCorrectData() {
    try {
        const connection = await mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME,
            port: process.env.DB_PORT || 3306,
            ssl: { rejectUnauthorized: false }
        });
        
        console.log('🔄 Criando jogadores jojo e esquece...');
        
        // Limpar dados existentes primeiro
        await connection.execute('DELETE FROM players WHERE discord_id IN (?, ?)', ['1234567890', '0987654321']);
        await connection.execute('DELETE FROM teams WHERE name = ?', ['jojos fc']);
        
        // Criar jogador JOJO
        await connection.execute(`
            INSERT INTO players (discord_id, name, team_id, wins, losses, purple_coins, packs_claimed, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        `, ['1234567890', 'jojo', null, 5, 2, 1000, 10]);
        
        // Criar jogador ESQUECE  
        await connection.execute(`
            INSERT INTO players (discord_id, name, team_id, wins, losses, purple_coins, packs_claimed, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
        `, ['0987654321', 'esquece', null, 8, 3, 1500, 15]);
        
        console.log('✅ Jogadores criados!');
        
        // Criar time JOJOS FC
        await connection.execute(`
            INSERT INTO teams (name, owner_discord_id, gk_card_id, vl_card_id, pv_card_id, wins, losses, goals_for, goals_against, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
        `, ['jojos fc', '1234567890', null, null, null, 10, 5, 25, 18]);
        
        console.log('✅ Time "jojos fc" criado!');
        
        // Atualizar o jogador jojo para estar no time
        const [teamResult] = await connection.execute('SELECT id FROM teams WHERE name = ?', ['jojos fc']);
        const teamId = teamResult[0].id;
        
        await connection.execute('UPDATE players SET team_id = ? WHERE discord_id = ?', [teamId, '1234567890']);
        
        console.log('✅ Jogador jojo adicionado ao time!');
        
        // Verificar dados criados
        console.log('\n📋 VERIFICANDO DADOS CRIADOS:');
        const [players] = await connection.execute('SELECT * FROM players WHERE discord_id IN (?, ?)', ['1234567890', '0987654321']);
        console.log('Jogadores:', players.map(p => ({ nome: p.name, discord: p.discord_id, coins: p.purple_coins })));
        
        const [teams] = await connection.execute('SELECT * FROM teams WHERE name = ?', ['jojos fc']);
        console.log('Time:', teams.map(t => ({ nome: t.name, owner: t.owner_discord_id, wins: t.wins })));
        
        await connection.end();
        console.log('🎉 Dados criados com sucesso!');
        
    } catch (error) {
        console.error('❌ Erro:', error);
    }
}

createCorrectData();
