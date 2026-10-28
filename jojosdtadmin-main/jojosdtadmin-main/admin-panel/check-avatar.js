const mysql = require('mysql2/promise');

async function checkTable() {
    const connection = await mysql.createConnection({
        host: 'jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com',
        user: 'admin',
        password: 'soufoda123',
        database: 'jojodreamteam'
    });
    
    const [results] = await connection.execute('DESCRIBE user_cards');
    console.log('Estrutura da tabela user_cards:');
    results.forEach(row => {
        console.log('- ' + row.Field + ': ' + row.Type + ' (' + row.Null + ', ' + row.Key + ', ' + row.Default + ')');
    });
    
    // Verificar algumas cartas recentes para ver os avatars
    const [cards] = await connection.execute('SELECT id, player_name, avatar, template_id, created_at FROM user_cards ORDER BY created_at DESC LIMIT 5');
    console.log('\nÚltimas 5 cartas criadas:');
    cards.forEach(card => {
        console.log(`ID: ${card.id}, Nome: ${card.player_name}, Avatar: "${card.avatar}", Template: ${card.template_id}, Data: ${card.created_at}`);
    });
    
    connection.close();
}

checkTable().catch(console.error);
