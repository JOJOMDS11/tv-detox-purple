const mysql = require('mysql2/promise');

async function checkAvatarProblems() {
    const connection = await mysql.createConnection({
        host: 'jojodreamteam.cgv8aga22uxg.us-east-1.rds.amazonaws.com',
        user: 'admin',
        password: 'soufoda123',
        database: 'jojodreamteam'
    });
    
    // Verificar templates com avatars estranhos
    const [templates] = await connection.execute('SELECT id, name, position, avatar FROM player_templates ORDER BY id');
    console.log('Templates com possíveis problemas:');
    templates.forEach(template => {
        if (template.avatar && template.avatar.length > 3) {
            console.log(`⚠️ ID: ${template.id}, Nome: ${template.name}, Avatar: "${template.avatar}" (${template.avatar.length} chars)`);
        } else {
            console.log(`✅ ID: ${template.id}, Nome: ${template.name}, Avatar: "${template.avatar}"`);
        }
    });
    
    // Verificar cartas recentes com avatars problemáticos
    const [cards] = await connection.execute(`
        SELECT uc.id, uc.player_name, uc.avatar, pt.avatar as template_avatar, pt.name as template_name
        FROM user_cards uc 
        LEFT JOIN player_templates pt ON uc.template_id = pt.id 
        ORDER BY uc.created_at DESC 
        LIMIT 10
    `);
    
    console.log('\nÚltimas 10 cartas criadas:');
    cards.forEach(card => {
        const cardAvatar = card.avatar || 'NULL';
        const templateAvatar = card.template_avatar || 'NULL';
        console.log(`Carta #${card.id}: ${card.player_name} | Avatar: "${cardAvatar}" | Template: "${templateAvatar}"`);
    });
    
    connection.close();
}

checkAvatarProblems().catch(console.error);
