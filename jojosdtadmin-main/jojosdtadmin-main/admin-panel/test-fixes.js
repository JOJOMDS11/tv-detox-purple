// Test script para verificar as correções implementadas
const mysql = require('mysql2/promise');
require('dotenv').config();

const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: process.env.DB_PORT || 3306
};

async function testFixes() {
    console.log('🔍 Testando as correções implementadas...\n');

    try {
        const connection = await mysql.createConnection(dbConfig);
        console.log('✅ Conectado ao banco de dados\n');

        // Teste 1: Verificar nomes de usuários
        console.log('📝 Teste 1: Verificando nomes de usuários');
        const [users] = await connection.execute(`
      SELECT 
        discord_id,
        name,
        CASE 
          WHEN name IS NOT NULL AND name != '' THEN name
          WHEN name IS NULL OR name = '' THEN CONCAT('User_', RIGHT(discord_id, 4))
          ELSE 'Usuário Desconhecido'
        END as display_name
      FROM players
    `);

        users.forEach(user => {
            console.log(`   ID: ${user.discord_id} | Nome: ${user.name || 'NULL'} | Display: ${user.display_name}`);
        });

        // Teste 2: Contagem por raridade
        console.log('\n📊 Teste 2: Contagem de cartas por raridade');
        const [rarityCount] = await connection.execute(`
      SELECT 
        LOWER(rarity) as rarity_lower,
        COUNT(*) as count
      FROM user_cards 
      WHERE rarity IS NOT NULL
      GROUP BY LOWER(rarity)
      ORDER BY count DESC
    `);

        const rarityStats = {
            total: 0,
            prime: 0,
            goat: 0,
            medio: 0,
            bagre: 0
        };

        rarityCount.forEach(row => {
            const rarity = row.rarity_lower;
            const count = parseInt(row.count);
            rarityStats.total += count;

            if (rarity === 'prime') rarityStats.prime = count;
            else if (rarity === 'goat') rarityStats.goat = count;
            else if (rarity === 'médio' || rarity === 'medio') rarityStats.medio = count;
            else if (rarity === 'bagre') rarityStats.bagre = count;

            console.log(`   ${rarity}: ${count} cartas`);
        });

        console.log('\n📋 Estatísticas organizadas:');
        console.log(`   Total: ${rarityStats.total}`);
        console.log(`   Prime: ${rarityStats.prime}`);
        console.log(`   GOAT: ${rarityStats.goat}`);
        console.log(`   Médio: ${rarityStats.medio}`);
        console.log(`   Bagre: ${rarityStats.bagre}`);

        // Teste 3: Verificar colunas de goleiro
        console.log('\n🥅 Teste 3: Verificando estatísticas de goleiro');
        const [goalkeeperStats] = await connection.execute(`
      SELECT 
        player_name,
        position,
        goals_conceded,
        clean_sheets
      FROM user_cards 
      WHERE position = 'GK'
      LIMIT 5
    `);

        if (goalkeeperStats.length > 0) {
            goalkeeperStats.forEach(gk => {
                console.log(`   ${gk.player_name} (${gk.position}) - Gols sofridos: ${gk.goals_conceded}, Jogos sem sofrer: ${gk.clean_sheets}`);
            });
        } else {
            console.log('   Nenhum goleiro encontrado na base de dados');
        }

        // Teste 4: Simular dados do endpoint /api/users
        console.log('\n👥 Teste 4: Simulando resposta do endpoint /api/users');
        const mockApiResponse = {
            users: users.map(user => ({
                discord_id: user.discord_id,
                name: user.display_name,
                cards: {
                    total: rarityStats.total,
                    prime: rarityStats.prime,
                    goat: rarityStats.goat,
                    medio: rarityStats.medio,
                    bagre: rarityStats.bagre
                }
            }))
        };

        console.log('   Resposta da API simulada:');
        console.log(JSON.stringify(mockApiResponse, null, 2));

        await connection.end();
        console.log('\n✅ Todos os testes concluídos com sucesso!');

    } catch (error) {
        console.error('❌ Erro durante os testes:', error.message);
    }
}

testFixes();
