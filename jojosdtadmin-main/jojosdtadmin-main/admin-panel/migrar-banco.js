require('dotenv').config();
const { query } = require('./api/db');

async function migrarBanco() {
    console.log('🔄 Executando migração do banco de dados...\n');

    try {
        // 1. Verificar se as tabelas existem
        console.log('📋 Verificando estrutura atual...');
        
        const tables = await query("SHOW TABLES");
        console.log('✅ Tabelas existentes:', tables.map(t => Object.values(t)[0]).join(', '));

        // 2. Criar tabela player_templates se não existir
        console.log('\n🔧 Criando tabela player_templates...');
        await query(`
            CREATE TABLE IF NOT EXISTS player_templates (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) NOT NULL UNIQUE,
                position ENUM('GK', 'VL', 'PV') NOT NULL,
                avatar VARCHAR(10) DEFAULT '⚽',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        `);
        console.log('✅ Tabela player_templates criada/verificada!');

        // 3. Verificar estrutura da tabela user_cards
        console.log('\n🔧 Verificando/criando tabela user_cards...');
        await query(`
            CREATE TABLE IF NOT EXISTS user_cards (
                id INT AUTO_INCREMENT PRIMARY KEY,
                discord_id VARCHAR(20) NOT NULL,
                player_name VARCHAR(100) NOT NULL,
                position ENUM('GK', 'VL', 'PV') NOT NULL,
                avatar VARCHAR(10) DEFAULT '⚽',
                rarity ENUM('Prime', 'GOAT', 'Médio', 'Bagre') NOT NULL DEFAULT 'Bagre',
                overall_rating INT NOT NULL DEFAULT 50,
                posicionamento INT DEFAULT 50,
                finalizacao INT DEFAULT 50,
                drible INT DEFAULT 50,
                defesa INT DEFAULT 50,
                passe INT DEFAULT 50,
                fisico INT DEFAULT 50,
                saidaDeBola INT DEFAULT 50,
                price DECIMAL(10,2) DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        `);
        console.log('✅ Tabela user_cards criada/verificada!');

        // 4. Verificar/criar tabela players
        console.log('\n🔧 Verificando/criando tabela players...');
        await query(`
            CREATE TABLE IF NOT EXISTS players (
                id INT AUTO_INCREMENT PRIMARY KEY,
                discord_id VARCHAR(20) NOT NULL UNIQUE,
                username VARCHAR(100) NOT NULL,
                purple_coins INT DEFAULT 0,
                total_cards INT DEFAULT 0,
                prime_cards INT DEFAULT 0,
                goat_cards INT DEFAULT 0,
                medio_cards INT DEFAULT 0,
                bagre_cards INT DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        `);
        console.log('✅ Tabela players criada/verificada!');

        // 5. Verificar/criar tabela teams
        console.log('\n🔧 Verificando/criando tabela teams...');
        await query(`
            CREATE TABLE IF NOT EXISTS teams (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(100) NOT NULL,
                owner_discord_id VARCHAR(20) NOT NULL,
                username VARCHAR(100),
                wins INT DEFAULT 0,
                losses INT DEFAULT 0,
                gk_card_id INT,
                vl_card_id INT,
                pv_card_id INT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                UNIQUE KEY unique_owner (owner_discord_id),
                INDEX idx_owner (owner_discord_id)
            )
        `);
        console.log('✅ Tabela teams criada/verificada!');

        // 6. Criar índices para performance
        console.log('\n📊 Criando índices...');
        
        try {
            await query('CREATE INDEX IF NOT EXISTS idx_user_cards_discord_id ON user_cards(discord_id)');
            await query('CREATE INDEX IF NOT EXISTS idx_user_cards_rarity ON user_cards(rarity)');
            await query('CREATE INDEX IF NOT EXISTS idx_player_templates_position ON player_templates(position)');
        } catch (error) {
            // Índices podem já existir, não é um erro crítico
            console.log('ℹ️ Alguns índices já existem (normal)');
        }
        console.log('✅ Índices criados/verificados!');

        // 7. Verificar estrutura final
        console.log('\n📋 Verificando estrutura final...');
        
        const finalTables = await query("SHOW TABLES");
        console.log('✅ Tabelas finais:', finalTables.map(t => Object.values(t)[0]).join(', '));

        // Verificar contadores
        const counts = {
            players: await query('SELECT COUNT(*) as count FROM players'),
            teams: await query('SELECT COUNT(*) as count FROM teams'),
            cards: await query('SELECT COUNT(*) as count FROM user_cards'),
            templates: await query('SELECT COUNT(*) as count FROM player_templates')
        };

        console.log('\n📊 Estado atual do banco:');
        console.log(`   👥 Jogadores: ${counts.players[0].count}`);
        console.log(`   🏆 Times: ${counts.teams[0].count}`);
        console.log(`   🎴 Cartas: ${counts.cards[0].count}`);
        console.log(`   ⚙️ Templates: ${counts.templates[0].count}`);

        console.log('\n🎉 Migração concluída com sucesso!');
        console.log('✅ Banco está pronto para ser populado!');

    } catch (error) {
        console.error('❌ Erro na migração:', error);
        throw error;
    }
}

// Executar
migrarBanco().then(() => {
    console.log('\n✅ Script de migração finalizado');
    process.exit(0);
}).catch(error => {
    console.error('❌ Erro no script de migração:', error);
    process.exit(1);
});
