require('dotenv').config();
const express = require('express');
const path = require('path');
const cors = require('cors');
const mysql = require('mysql2/promise');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Criação de conexão simples e funcional
async function createConnection() {
    return await mysql.createConnection({
        host: process.env.DB_HOST,
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME,
        port: process.env.DB_PORT || 3306,
        ssl: {
            rejectUnauthorized: false
        }
    });
}

// Login
app.post('/api/login', (req, res) => {
    const { password } = req.body;
    res.setHeader('Access-Control-Allow-Origin', '*');
    
    if (password === 'admin123') {
        return res.status(200).json({ success: true });
    } else {
        return res.status(401).json({ success: false, message: 'Senha incorreta' });
    }
});

// API Stats
app.get('/api/stats', async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    
    let connection;
    try {
        connection = await createConnection();
        
        // Contar players
        const [playersResult] = await connection.execute('SELECT COUNT(*) as count FROM players');
        const totalPlayers = playersResult[0]?.count || 0;
        
        // Contar teams
        const [teamsResult] = await connection.execute('SELECT COUNT(*) as count FROM teams');
        const totalTeams = teamsResult[0]?.count || 0;
        
        // Contar cartas
        const [cardsResult] = await connection.execute('SELECT COUNT(*) as count FROM user_cards');
        const totalCards = cardsResult[0]?.count || 0;
        
        // Contar templates
        const [templatesResult] = await connection.execute('SELECT COUNT(*) as count FROM player_templates');
        const totalTemplates = templatesResult[0]?.count || 0;
        
        return res.status(200).json({
            success: true,
            data: {
                totalPlayers,
                totalTeams,
                totalCards,
                totalTemplates
            }
        });
    } catch (error) {
        console.error('Erro stats:', error);
        return res.status(500).json({ success: false, message: 'Erro interno' });
    } finally {
        if (connection) await connection.end();
    }
});

// API Players
app.get('/api/players', async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    
    let connection;
    try {
        connection = await createConnection();
        
        const [rows] = await connection.execute(`
            SELECT 
                p.id,
                p.discord_id,
                p.name,
                p.team_id,
                p.wins,
                p.losses,
                p.purple_coins,
                p.packs_claimed,
                p.last_obtained_time,
                p.created_at,
                t.name as team_name
            FROM players p
            LEFT JOIN teams t ON p.team_id = t.id
            ORDER BY p.created_at DESC
        `);
        
        console.log(`📊 Retornando ${rows.length} jogadores`);
        return res.status(200).json(rows);
    } catch (error) {
        console.error('Erro players:', error);
        return res.status(500).json({ success: false, message: 'Erro interno' });
    } finally {
        if (connection) await connection.end();
    }
});

// API Teams
app.get('/api/teams', async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    
    let connection;
    try {
        connection = await createConnection();
        
        const [rows] = await connection.execute(`
            SELECT 
                id,
                name,
                owner_discord_id,
                gk_card_id,
                vl_card_id,
                pv_card_id,
                wins,
                losses,
                goals_for,
                goals_against,
                created_at
            FROM teams
            ORDER BY wins DESC
        `);
        
        console.log(`🏆 Retornando ${rows.length} times`);
        return res.status(200).json(rows);
    } catch (error) {
        console.error('Erro teams:', error);
        return res.status(500).json({ success: false, message: 'Erro interno' });
    } finally {
        if (connection) await connection.end();
    }
});

// API Templates
app.get('/api/templates', async (req, res) => {
    res.setHeader('Access-Control-Allow-Origin', '*');
    
    let connection;
    try {
        connection = await createConnection();
        
        const [rows] = await connection.execute('SELECT * FROM player_templates ORDER BY id DESC LIMIT 20');
        
        console.log(`⚙️ Retornando ${rows.length} templates`);
        return res.status(200).json(rows);
    } catch (error) {
        console.log('Tabela player_templates com erro, retornando vazio:', error.message);
        return res.status(200).json([]);
    } finally {
        if (connection) await connection.end();
    }
});

// Rota principal
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Tratamento de erro para rotas não encontradas
app.use((req, res) => {
    res.status(404).json({ error: 'Rota não encontrada' });
});

app.listen(PORT, () => {
    console.log(`🚀 Admin Panel rodando em: http://localhost:${PORT}`);
    console.log(`📊 Acesse o painel de administração no navegador`);
    console.log(`🔑 Use a senha: admin123`);
    console.log(`📋 Testando conexão inicial...`);
    
    // Teste inicial
    createConnection().then(async (conn) => {
        const [result] = await conn.execute('SELECT COUNT(*) as total FROM players');
        console.log(`✅ Conectado! Total de jogadores: ${result[0].total}`);
        await conn.end();
    }).catch(err => {
        console.error('❌ Erro na conexão inicial:', err.message);
    });
});

// Tratamento de erros não capturados
process.on('uncaughtException', (error) => {
    console.error('❌ Erro não capturado:', error);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('❌ Promise rejeitada:', reason);
});
