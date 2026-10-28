require('dotenv').config();
const { testConnection, query, closeConnection } = require('./api/db');

async function testDatabase() {
    console.log('🔄 Testando conexão com o banco de dados...');
    console.log('📋 Configurações:');
    console.log('   Host:', process.env.DB_HOST);
    console.log('   User:', process.env.DB_USER);
    console.log('   Database:', process.env.DB_NAME);
    console.log('   Port:', process.env.DB_PORT);
    console.log('');

    try {
        // Teste básico de conexão
        const isConnected = await testConnection();
        
        if (isConnected) {
            console.log('🎉 Conexão estabelecida com sucesso!');
            
            // Teste das tabelas principais
            console.log('\n📊 Testando consultas nas tabelas:');
            
            try {
                const playersCount = await query('SELECT COUNT(*) as count FROM players');
                console.log(`   ✅ players: ${playersCount[0]?.count || 0} registros`);
            } catch (error) {
                console.log(`   ❌ players: Erro - ${error.message}`);
            }

            try {
                const teamsCount = await query('SELECT COUNT(*) as count FROM teams');
                console.log(`   ✅ teams: ${teamsCount[0]?.count || 0} registros`);
            } catch (error) {
                console.log(`   ❌ teams: Erro - ${error.message}`);
            }

            try {
                const templatesCount = await query('SELECT COUNT(*) as count FROM player_templates');
                console.log(`   ✅ player_templates: ${templatesCount[0]?.count || 0} registros`);
            } catch (error) {
                console.log(`   ❌ player_templates: Erro - ${error.message}`);
            }

            try {
                const cardsCount = await query('SELECT COUNT(*) as count FROM user_cards');
                console.log(`   ✅ user_cards: ${cardsCount[0]?.count || 0} registros`);
            } catch (error) {
                console.log(`   ❌ user_cards: Erro - ${error.message}`);
            }

        } else {
            console.log('❌ Falha na conexão com o banco de dados');
        }

    } catch (error) {
        console.error('💥 Erro durante o teste:', error.message);
    } finally {
        await closeConnection();
        console.log('\n🔚 Teste finalizado');
        process.exit(0);
    }
}

testDatabase();
