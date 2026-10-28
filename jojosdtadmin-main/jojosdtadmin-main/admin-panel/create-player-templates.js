require('dotenv').config();
const mysql = require('mysql2/promise');

async function createPlayerTemplates() {
    try {
        const connection = await mysql.createConnection({
            host: process.env.DB_HOST,
            user: process.env.DB_USER,
            password: process.env.DB_PASSWORD,
            database: process.env.DB_NAME,
            port: process.env.DB_PORT || 3306,
            ssl: { rejectUnauthorized: false }
        });
        
        console.log('⚙️ VERIFICANDO TEMPLATES ATUAIS:');
        const [currentTemplates] = await connection.execute('SELECT * FROM player_templates ORDER BY id');
        currentTemplates.forEach(t => console.log(`ID: ${t.id} - Nome: ${t.name}`));
        
        console.log('\n🔍 Procurando jojo e esquece:');
        const [found] = await connection.execute('SELECT * FROM player_templates WHERE name LIKE ? OR name LIKE ?', ['%jojo%', '%esquece%']);
        
        if (found.length === 0) {
            console.log('❌ Jogadores jojo e esquece não encontrados! Criando...');
            
            // Verificar se existe template "Jojo" (pode estar com J maiúsculo)
            const [jojoExists] = await connection.execute('SELECT * FROM player_templates WHERE name = ?', ['Jojo']);
            if (jojoExists.length === 0) {
                await connection.execute('INSERT INTO player_templates (name, created_at) VALUES (?, NOW())', ['jojo']);
                console.log('✅ Template "jojo" criado!');
            } else {
                console.log('✅ Template "Jojo" já existe!');
            }
            
            // Criar template "esquece"
            await connection.execute('INSERT INTO player_templates (name, created_at) VALUES (?, NOW())', ['esquece']);
            console.log('✅ Template "esquece" criado!');
            
        } else {
            console.log('✅ Jogadores encontrados:', found.map(f => f.name));
        }
        
        // Verificar templates finais
        console.log('\n📋 TEMPLATES FINAIS:');
        const [finalTemplates] = await connection.execute('SELECT * FROM player_templates ORDER BY id');
        finalTemplates.forEach(t => console.log(`ID: ${t.id} - Nome: ${t.name}`));
        
        await connection.end();
        console.log('\n🎉 Processamento concluído!');
        
    } catch (error) {
        console.error('❌ Erro:', error);
    }
}

createPlayerTemplates();
