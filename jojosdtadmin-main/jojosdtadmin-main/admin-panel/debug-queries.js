require('dotenv').config();
const { query } = require('./api/db');

async function testQueries() {
    console.log('🔍 Testando queries específicas do painel...\n');
    
    const queries = [
        {
            name: 'Total Players',
            sql: 'SELECT COUNT(*) as count FROM players',
            expectedField: 'count'
        },
        {
            name: 'Total Cards', 
            sql: 'SELECT COUNT(*) as count FROM user_cards',
            expectedField: 'count'
        },
        {
            name: 'Total Templates',
            sql: 'SELECT COUNT(*) as count FROM player_templates', 
            expectedField: 'count'
        },
        {
            name: 'Total Teams',
            sql: 'SELECT COUNT(*) as count FROM teams',
            expectedField: 'count'
        }
    ];
    
    for (const queryTest of queries) {
        try {
            console.log(`📊 Testando: ${queryTest.name}`);
            console.log(`   SQL: ${queryTest.sql}`);
            
            const result = await query(queryTest.sql);
            console.log(`   ✅ Resultado completo:`, JSON.stringify(result, null, 2));
            
            if (result && result.length > 0) {
                const value = result[0][queryTest.expectedField];
                console.log(`   📈 Valor extraído: ${value} (tipo: ${typeof value})`);
                
                if (value !== undefined && value !== null) {
                    console.log(`   ✅ SUCCESS: ${queryTest.name} = ${value}`);
                } else {
                    console.log(`   ❌ PROBLEMA: Campo '${queryTest.expectedField}' é undefined/null`);
                }
            } else {
                console.log(`   ❌ PROBLEMA: Query não retornou resultados`);
            }
            
        } catch (error) {
            console.log(`   ❌ ERRO: ${error.message}`);
        }
        
        console.log('');
    }
    
    // Teste adicional: verificar estrutura das tabelas
    console.log('📋 Verificando estrutura das tabelas...\n');
    
    const tables = ['players', 'user_cards', 'player_templates', 'teams'];
    
    for (const table of tables) {
        try {
            console.log(`🔍 Estrutura da tabela: ${table}`);
            const structure = await query(`DESCRIBE ${table}`);
            console.log(`   Colunas:`, structure.map(col => col.Field).join(', '));
            
            // Mostrar alguns dados de exemplo
            const sample = await query(`SELECT * FROM ${table} LIMIT 3`);
            console.log(`   Exemplo de dados:`, JSON.stringify(sample, null, 2));
            
        } catch (error) {
            console.log(`   ❌ Erro ao verificar ${table}: ${error.message}`);
        }
        
        console.log('');
    }
}

testQueries().catch(console.error);
