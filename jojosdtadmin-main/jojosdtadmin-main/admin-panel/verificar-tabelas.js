require('dotenv').config();
const { query } = require('./api/db');

async function verificarTabelas() {
    console.log('🔍 Verificando estrutura das tabelas...\n');

    try {
        // Verificar estrutura da tabela player_templates
        console.log('📋 Estrutura da tabela player_templates:');
        const templatesStructure = await query('DESCRIBE player_templates');
        console.table(templatesStructure);

        console.log('\n📋 Estrutura da tabela user_cards:');
        const cardsStructure = await query('DESCRIBE user_cards');
        console.table(cardsStructure);

        console.log('\n📋 Estrutura da tabela players:');
        const playersStructure = await query('DESCRIBE players');
        console.table(playersStructure);

        console.log('\n📋 Estrutura da tabela teams:');
        const teamsStructure = await query('DESCRIBE teams');
        console.table(teamsStructure);

    } catch (error) {
        console.error('❌ Erro ao verificar tabelas:', error);
    }
}

verificarTabelas().then(() => {
    console.log('\n✅ Verificação finalizada');
    process.exit(0);
}).catch(error => {
    console.error('❌ Erro:', error);
    process.exit(1);
});
