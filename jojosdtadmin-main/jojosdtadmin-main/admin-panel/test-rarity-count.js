require('dotenv').config();
const { query } = require('./api/db');

async function testRarityCount() {
    console.log('🔍 Testando contagem por raridade...');

    try {
        const discordId = '366001292251889696';

        // Buscar contagem total
        const totalCards = await query(`
            SELECT COUNT(*) as total
            FROM user_cards 
            WHERE discord_id = ?
        `, [discordId]);

        console.log(`📊 Total de cartas: ${totalCards[0]?.total || 0}`);

        // Buscar contagem por raridade
        const cardsByRarity = await query(`
            SELECT 
                rarity,
                COUNT(*) as count
            FROM user_cards 
            WHERE discord_id = ?
            GROUP BY rarity
        `, [discordId]);

        console.log('📋 Contagem por raridade:');
        cardsByRarity.forEach(row => {
            console.log(`   ${row.rarity}: ${row.count} cartas`);
        });

        // Organizar como no código
        const cardStats = {
            total: totalCards[0]?.total || 0,
            prime: 0,
            goat: 0,
            medio: 0,
            bagre: 0
        };

        cardsByRarity.forEach(row => {
            switch (row.rarity.toLowerCase()) {
                case 'prime':
                    cardStats.prime = row.count;
                    break;
                case 'goat':
                    cardStats.goat = row.count;
                    break;
                case 'médio':
                case 'medio':
                    cardStats.medio = row.count;
                    break;
                case 'bagre':
                    cardStats.bagre = row.count;
                    break;
            }
        });

        console.log('\n📊 Resultado final organizado:');
        console.log(JSON.stringify(cardStats, null, 2));

    } catch (error) {
        console.error('❌ Erro:', error);
    }
}

testRarityCount().then(() => {
    process.exit(0);
});
