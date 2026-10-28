const mysql = require('mysql2/promise');

async function fixNullAvatars() {
    const connection = await mysql.createConnection({
        host: 'jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com',
        user: 'admin',
        password: 'soufoda123',
        database: 'jojodreamteam'
    });
    
    // Atualizar templates com avatar NULL para avatar baseado na posição
    const [result1] = await connection.execute(`
        UPDATE player_templates 
        SET avatar = CASE 
            WHEN position = 'GK' THEN '🥅'
            WHEN position = 'VL' THEN '⚽'
            WHEN position = 'PV' THEN '🎯'
            ELSE '⚽'
        END
        WHERE avatar IS NULL
    `);
    console.log('Templates corrigidos:', result1.affectedRows);
    
    // Verificar resultado
    const [templates] = await connection.execute('SELECT id, name, position, avatar FROM player_templates ORDER BY id');
    console.log('\nTemplates após correção:');
    templates.forEach(template => {
        console.log('ID: ' + template.id + ', Nome: ' + template.name + ', Posição: ' + template.position + ', Avatar: "' + template.avatar + '"');
    });
    
    connection.close();
}

fixNullAvatars().catch(console.error);
