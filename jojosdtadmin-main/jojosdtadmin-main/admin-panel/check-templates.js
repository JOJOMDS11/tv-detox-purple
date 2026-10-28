const mysql = require('mysql2/promise');

async function checkTemplates() {
    const connection = await mysql.createConnection({
        host: 'jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com',
        user: 'admin',
        password: 'soufoda123',
        database: 'jojodreamteam'
    });
    
    const [templates] = await connection.execute('SELECT id, name, position, avatar FROM player_templates ORDER BY id');
    console.log('Templates disponíveis:');
    templates.forEach(template => {
        console.log('ID: ' + template.id + ', Nome: ' + template.name + ', Posição: ' + template.position + ', Avatar: "' + template.avatar + '"');
    });
    
    connection.close();
}

checkTemplates().catch(console.error);
