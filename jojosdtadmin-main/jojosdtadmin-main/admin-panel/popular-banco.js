require('dotenv').config();
const { query } = require('./api/db');

async function popularBanco() {
    console.log('🔄 Populando banco de dados com dados de exemplo...\n');

    try {
        // 1. Criar templates de jogadores (usando estrutura atual da tabela)
        console.log('📝 Inserindo templates de jogadores...');
        
        const templates = [
            'Lionel Messi', 'Cristiano Ronaldo', 'Neymar Jr', 'Kevin De Bruyne', 
            'Virgil van Dijk', 'Manuel Neuer', 'Alisson Becker', 'Sergio Ramos', 
            'Mbappé', 'Haaland', 'Jojo', 'Modric', 'Pelé', 'Maradona', 'Buffon'
        ];

        for (const templateName of templates) {
            await query(
                'INSERT INTO player_templates (name, created_at) VALUES (?, NOW())',
                [templateName]
            );
        }
        console.log('✅ Templates inseridos com sucesso!');

        // 2. Criar jogadores de exemplo (usando estrutura atual)
        console.log('👥 Inserindo jogadores...');
        
        const players = [
            { discord_id: '123456789012345678', name: 'João Silva', purple_coins: 500 },
            { discord_id: '234567890123456789', name: 'Maria Santos', purple_coins: 750 },
            { discord_id: '345678901234567890', name: 'Pedro Oliveira', purple_coins: 300 },
            { discord_id: '456789012345678901', name: 'Ana Costa', purple_coins: 1200 },
            { discord_id: '567890123456789012', name: 'Carlos Lima', purple_coins: 650 }
        ];

        for (const player of players) {
            await query(
                'INSERT INTO players (discord_id, name, purple_coins, created_at) VALUES (?, ?, ?, NOW())',
                [player.discord_id, player.name, player.purple_coins]
            );
        }
        console.log('✅ Jogadores inseridos com sucesso!');

        // 3. Criar times (usando estrutura atual)
        console.log('🏆 Inserindo times...');
        
        const teams = [
            { 
                name: 'Barcelona FC', 
                owner_discord_id: '123456789012345678',
                wins: 15,
                losses: 3
            },
            { 
                name: 'Real Madrid', 
                owner_discord_id: '234567890123456789',
                wins: 12,
                losses: 6
            },
            { 
                name: 'Manchester City', 
                owner_discord_id: '345678901234567890',
                wins: 8,
                losses: 8
            },
            { 
                name: 'PSG', 
                owner_discord_id: '456789012345678901',
                wins: 20,
                losses: 1
            },
            { 
                name: 'Bayern Munich', 
                owner_discord_id: '567890123456789012',
                wins: 11,
                losses: 5
            }
        ];

        for (const team of teams) {
            await query(
                'INSERT INTO teams (name, owner_discord_id, wins, losses, created_at) VALUES (?, ?, ?, ?, NOW())',
                [team.name, team.owner_discord_id, team.wins, team.losses]
            );
        }
        console.log('✅ Times inseridos com sucesso!');

        // 4. Criar algumas cartas simples para demonstração
        console.log('🎴 Inserindo cartas...');
        
        let cardCount = 0;
        const cardNames = ['Messi', 'Ronaldo', 'Neymar', 'Neuer', 'De Bruyne'];

        for (const player of players) {
            // Cada jogador terá 5 cartas simples
            for (let i = 0; i < 5; i++) {
                const cardName = cardNames[i];
                
                await query(
                    'INSERT INTO user_cards (user_id, card_name, created_at) VALUES (?, ?, NOW())',
                    [1, cardName] // Usando user_id = 1 como exemplo
                );
                
                cardCount++;
            }
        }
        console.log(`✅ ${cardCount} cartas inseridas com sucesso!`);

        // 5. Verificar totais (removendo atualização de estatísticas complexas)
        console.log('\n📋 Resumo final:');
        const totalPlayers = await query('SELECT COUNT(*) as count FROM players');
        const totalTeams = await query('SELECT COUNT(*) as count FROM teams');
        const totalCards = await query('SELECT COUNT(*) as count FROM user_cards');
        const totalTemplates = await query('SELECT COUNT(*) as count FROM player_templates');

        console.log(`   👥 Jogadores: ${totalPlayers[0].count}`);
        console.log(`   🏆 Times: ${totalTeams[0].count}`);
        console.log(`   🎴 Cartas: ${totalCards[0].count}`);
        console.log(`   ⚙️ Templates: ${totalTemplates[0].count}`);

        console.log('\n🎉 Banco de dados populado com sucesso!');
        console.log('🌐 Agora você pode acessar o admin panel em http://localhost:3001');
        console.log('🔑 Senha: admin123');

    } catch (error) {
        console.error('❌ Erro ao popular banco:', error);
    }
}

// Executar
popularBanco().then(() => {
    console.log('\n✅ Script finalizado');
    process.exit(0);
}).catch(error => {
    console.error('❌ Erro no script:', error);
    process.exit(1);
});
