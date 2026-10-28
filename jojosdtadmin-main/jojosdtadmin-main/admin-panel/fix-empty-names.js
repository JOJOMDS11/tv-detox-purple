require('dotenv').config();
const { query } = require('./api/db');

async function fixEmptyNames() {
    console.log('🔧 Iniciando correção de nomes vazios...');

    try {
        // Buscar jogadores com nomes vazios
        const playersWithEmptyNames = await query(`
            SELECT discord_id, name 
            FROM players 
            WHERE name = '' OR name IS NULL
        `);

        console.log(`📊 Encontrados ${playersWithEmptyNames.length} jogadores com nomes vazios`);

        if (playersWithEmptyNames.length === 0) {
            console.log('✅ Nenhum jogador com nome vazio encontrado!');
            return;
        }

        // Para cada jogador, definir um nome placeholder
        for (const player of playersWithEmptyNames) {
            const placeholderName = `User_${player.discord_id.slice(-4)}`;

            await query(`
                UPDATE players 
                SET name = ? 
                WHERE discord_id = ?
            `, [placeholderName, player.discord_id]);

            console.log(`✅ Atualizado: ${player.discord_id} -> ${placeholderName}`);
        }

        console.log('🎉 Correção concluída com sucesso!');

        // Verificar resultado
        const updatedPlayers = await query(`
            SELECT discord_id, name 
            FROM players 
            ORDER BY created_at DESC
        `);

        console.log('\n📋 Estado atual dos jogadores:');
        updatedPlayers.forEach(player => {
            console.log(`   ${player.discord_id}: "${player.name}"`);
        });

    } catch (error) {
        console.error('❌ Erro ao corrigir nomes:', error);
    }
}

// Executar se for chamado diretamente
if (require.main === module) {
    fixEmptyNames().then(() => {
        console.log('\n🏁 Script finalizado!');
        process.exit(0);
    });
}

module.exports = { fixEmptyNames };
