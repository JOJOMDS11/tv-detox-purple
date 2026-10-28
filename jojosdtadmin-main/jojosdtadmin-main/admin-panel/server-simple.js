require('dotenv').config();
const express = require('express');
const path = require('path');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

// Importar rotas API
const playersApi = require('./api/players');
const teamsApi = require('./api/teams');
const statsApi = require('./api/stats');
const templatesApi = require('./api/templates');

// Login simples
app.post('/api/login', (req, res) => {
    const { password } = req.body;
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (req.method === 'OPTIONS') {
        return res.status(200).end();
    }

    if (password === (process.env.ADMIN_PASSWORD || 'admin123')) {
        return res.status(200).json({ success: true });
    } else {
        return res.status(401).json({ success: false, message: 'Senha incorreta' });
    }
});

// Rotas API
app.use('/api/players', (req, res) => playersApi(req, res));
app.use('/api/teams', (req, res) => teamsApi(req, res));
app.use('/api/stats', (req, res) => statsApi(req, res));
app.use('/api/templates', (req, res) => templatesApi(req, res));

// Rota principal
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

app.listen(PORT, () => {
    console.log(`🚀 Admin Panel rodando em: http://localhost:${PORT}`);
    console.log(`📊 Acesse o painel de administração no navegador`);
    console.log(`🔑 Use a senha: admin123`);
});

// Tratamento de erros não capturados
process.on('uncaughtException', (error) => {
    console.error('❌ Erro não capturado:', error);
});

process.on('unhandledRejection', (reason, promise) => {
    console.error('❌ Promise rejeitada:', reason);
});
