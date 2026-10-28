const mysql = require('mysql2/promise');

async function fixNullAvatars() {
    const connection = await mysql.createConnection({
        host: 'jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com',
        user: 'admin',
        password: 'soufoda123',
        database: 'jojodreamteam'
    });
    
    // Corrigir templates com avatar 'null' para NULL
    const [result1] = await connection.execute("UPDATE player_templates SET avatar = NULL WHERE avatar = 'null'");
    console.log('Templates corrigidos:', result1.affectedRows);
    
    // Corrigir cartas com avatar 'null' para NULL  
    const [result2] = await connection.execute("UPDATE user_cards SET avatar = NULL WHERE avatar = 'null'");
    console.log('Cartas corrigidas:', result2.affectedRows);
    
    // Verificar resultado
    const [templates] = await connection.execute('SELECT id, name, avatar FROM player_templates ORDER BY id');
    console.log('\nTemplates após correção:');
    templates.forEach(template => {
        console.log('ID: ' + template.id + ', Nome: ' + template.name + ', Avatar: "' + template.avatar + '"');
    });
    
    connection.close();
}

fixNullAvatars().catch(console.error);
