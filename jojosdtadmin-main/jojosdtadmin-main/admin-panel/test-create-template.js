const mysql = require('mysql2/promise');

async function testCreateTemplate() {
    const connection = await mysql.createConnection({
        host: 'jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com',
        user: 'admin',
        password: 'soufoda123',
        database: 'jojodreamteam'
    });
    
    // Teste: criar template com emoji manualmente
    const testData = {
        name: 'TestePK',
        position: 'GK',
        avatar: '🤏'
    };
    
    console.log('Dados que vamos inserir:');
    console.log('Nome:', testData.name);
    console.log('Posição:', testData.position);
    console.log('Avatar:', testData.avatar, '(length:', testData.avatar.length, ')');
    
    const sql = `INSERT INTO player_templates (name, position, avatar) VALUES (?, ?, ?)`;
    const values = [testData.name, testData.position, testData.avatar];
    
    try {
        const [result] = await connection.execute(sql, values);
        console.log('✅ Template criado com ID:', result.insertId);
        
        // Verificar o que foi salvo
        const [check] = await connection.execute('SELECT * FROM player_templates WHERE id = ?', [result.insertId]);
        const saved = check[0];
        console.log('✅ Verificação do que foi salvo:');
        console.log('Nome:', saved.name);
        console.log('Posição:', saved.position);
        console.log('Avatar salvo:', saved.avatar, '(length:', saved.avatar ? saved.avatar.length : 0, ')');
        
        // Deletar o template de teste
        await connection.execute('DELETE FROM player_templates WHERE id = ?', [result.insertId]);
        console.log('🗑️ Template de teste removido');
        
    } catch (error) {
        console.error('❌ Erro ao criar template:', error);
    }
    
    connection.close();
}

testCreateTemplate().catch(console.error);
