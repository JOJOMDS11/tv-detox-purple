const mysql = require('mysql2/promise');

async function checkPKTemplate() {
    const connection = await mysql.createConnection({
        host: 'jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com',
        user: 'admin',
        password: 'soufoda123',
        database: 'jojodreamteam'
    });
    
    // Verificar o template PK recém-criado
    const [templates] = await connection.execute('SELECT id, name, position, avatar FROM player_templates WHERE name = "PK" ORDER BY id DESC LIMIT 1');
    
    if (templates.length > 0) {
        const pk = templates[0];
        console.log(`Template PK encontrado:`);
        console.log(`ID: ${pk.id}`);
        console.log(`Nome: ${pk.name}`);
        console.log(`Posição: ${pk.position}`);
        console.log(`Avatar: "${pk.avatar}" (length: ${pk.avatar ? pk.avatar.length : 0})`);
        
        if (!pk.avatar || pk.avatar === 'null' || pk.avatar.trim() === '') {
            console.log('❌ PROBLEMA: Avatar não foi salvo corretamente!');
            
            // Corrigir o avatar
            const [result] = await connection.execute('UPDATE player_templates SET avatar = ? WHERE id = ?', ['🤏', pk.id]);
            console.log(`✅ Avatar corrigido! Linhas afetadas: ${result.affectedRows}`);
            
            // Verificar novamente
            const [updated] = await connection.execute('SELECT avatar FROM player_templates WHERE id = ?', [pk.id]);
            console.log(`✅ Novo avatar: "${updated[0].avatar}"`);
        } else {
            console.log('✅ Avatar está correto!');
        }
    } else {
        console.log('❌ Template PK não encontrado!');
    }
    
    connection.close();
}

checkPKTemplate().catch(console.error);
