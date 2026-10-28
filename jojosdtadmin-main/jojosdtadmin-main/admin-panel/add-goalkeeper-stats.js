require('dotenv').config();
const { query } = require('./api/db');

async function addGoalkeeperStats() {
    console.log('🥅 Adicionando estatísticas de goleiro ao banco de dados...');

    try {
        // Verificar se as colunas já existem
        const checkColumns = await query(`
            SHOW COLUMNS FROM user_cards LIKE 'goals_conceded'
        `);

        if (checkColumns.length > 0) {
            console.log('✅ Colunas de goleiro já existem!');
            return;
        }

        // Adicionar novas colunas para estatísticas de goleiro
        console.log('➕ Adicionando coluna goals_conceded...');
        await query(`
            ALTER TABLE user_cards 
            ADD COLUMN goals_conceded INT DEFAULT 0
        `);

        console.log('➕ Adicionando coluna clean_sheets...');
        await query(`
            ALTER TABLE user_cards 
            ADD COLUMN clean_sheets INT DEFAULT 0
        `);

        console.log('➕ Adicionando coluna own_goals...');
        await query(`
            ALTER TABLE user_cards 
            ADD COLUMN own_goals INT DEFAULT 0
        `);

        console.log('✅ Todas as colunas adicionadas com sucesso!');

        // Verificar estrutura atualizada
        const columns = await query(`
            SHOW COLUMNS FROM user_cards 
            WHERE Field IN ('goals_conceded', 'clean_sheets', 'own_goals')
        `);

        console.log('📋 Novas colunas criadas:');
        columns.forEach(col => {
            console.log(`   - ${col.Field}: ${col.Type} (Default: ${col.Default})`);
        });

    } catch (error) {
        console.error('❌ Erro ao adicionar colunas:', error);
        throw error;
    }
}

// Executar se for chamado diretamente
if (require.main === module) {
    addGoalkeeperStats().then(() => {
        console.log('\n🏁 Script de atualização finalizado!');
        process.exit(0);
    }).catch(error => {
        console.error('❌ Erro no script:', error);
        process.exit(1);
    });
}

module.exports = { addGoalkeeperStats };
