const mysql = require('mysql2/promise');

async function testTemplates() {
    try {
        console.log('🔍 Testando conexão e busca de templates...');
        
        const connection = await mysql.createConnection({
            host: 'jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com',
            user: 'admin',
            password: 'soufoda123',
            database: 'jojodreamteam',
            port: 3306
        });

        console.log('✅ Conectado ao banco!');

        // Verificar se tabela existe
        const [tables] = await connection.execute("SHOW TABLES LIKE 'player_templates'");
        console.log('📋 Tabelas encontradas:', tables);

        if (tables.length > 0) {
            // Verificar estrutura da tabela
            const [columns] = await connection.execute("SHOW COLUMNS FROM player_templates");
            console.log('📊 Colunas da tabela player_templates:');
            columns.forEach(col => {
                console.log(`   - ${col.Field} (${col.Type})`);
            });

            // Buscar todos os templates
            const [templates] = await connection.execute("SELECT * FROM player_templates ORDER BY id DESC");
            console.log(`\n🎯 Encontrados ${templates.length} templates:`);
            
            templates.forEach(template => {
                console.log(`   ID: ${template.id} | Nome: ${template.name} | Posição: ${template.position} | Avatar: ${template.avatar || 'N/A'}`);
            });

            // Buscar especificamente "Esquece" VL
            const [esquece] = await connection.execute("SELECT * FROM player_templates WHERE name LIKE '%Esquece%' AND position = 'VL'");
            console.log(`\n🔍 Busca específica por "Esquece" VL:`, esquece);

            // Buscar times
            console.log('\n🏆 Verificando times...');
            const [teams] = await connection.execute("SELECT * FROM teams WHERE name LIKE '%jojos%'");
            console.log('Times encontrados:', teams);

            // Buscar cartas do jogador específico
            console.log('\n🎴 Verificando cartas...');
            const [cards] = await connection.execute("SELECT * FROM user_cards WHERE player_name LIKE '%Esquece%' OR player_name LIKE '%jojo%'");
            console.log('Cartas encontradas:', cards);

        } else {
            console.log('❌ Tabela player_templates não encontrada!');
        }

        await connection.end();
        console.log('✅ Teste concluído!');

    } catch (error) {
        console.error('❌ Erro no teste:', error);
    }
}

testTemplates();
