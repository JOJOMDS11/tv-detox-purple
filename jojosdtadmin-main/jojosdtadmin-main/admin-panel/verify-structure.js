require('dotenv').config();
const mysql = require('mysql2/promise');

async function verificarEstrutura() {
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

        console.log('✅ Conexão estabelecida!');
        
        // Verificar estrutura da tabela players
        console.log('\n📋 Estrutura da tabela PLAYERS:');
        const [playersStructure] = await connection.execute('DESCRIBE players');
        playersStructure.forEach(col => {
            console.log(`- ${col.Field}: ${col.Type} (${col.Null === 'YES' ? 'NULL' : 'NOT NULL'})`);
        });
        
        // Verificar estrutura da tabela teams
        console.log('\n🏆 Estrutura da tabela TEAMS:');
        const [teamsStructure] = await connection.execute('DESCRIBE teams');
        teamsStructure.forEach(col => {
            console.log(`- ${col.Field}: ${col.Type} (${col.Null === 'YES' ? 'NULL' : 'NOT NULL'})`);
        });
        
        // Verificar estrutura da tabela player_templates
        console.log('\n⚙️ Estrutura da tabela PLAYER_TEMPLATES:');
        const [templatesStructure] = await connection.execute('DESCRIBE player_templates');
        templatesStructure.forEach(col => {
            console.log(`- ${col.Field}: ${col.Type} (${col.Null === 'YES' ? 'NULL' : 'NOT NULL'})`);
        });
        
        // Buscar alguns registros específicos para ver os dados reais
        console.log('\n👥 Dados específicos dos jogadores:');
        const [playersData] = await connection.execute('SELECT * FROM players LIMIT 3');
        console.log('Primeiros 3 jogadores:', playersData);
        
        console.log('\n🏆 Dados específicos dos times:');
        const [teamsData] = await connection.execute('SELECT * FROM teams LIMIT 3');
        console.log('Primeiros 3 times:', teamsData);
        
        console.log('\n⚙️ Dados específicos dos templates:');
        const [templatesData] = await connection.execute('SELECT * FROM player_templates LIMIT 3');
        console.log('Primeiros 3 templates:', templatesData);
        
        await connection.end();
        
    } catch (error) {
        console.error('❌ Erro:', error.message);
    }
}

verificarEstrutura();
