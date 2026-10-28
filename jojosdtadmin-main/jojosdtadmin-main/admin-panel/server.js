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

// Função auxiliar para carregar handlers das API
const createHandler = (handlerPath) => {
    return async (req, res) => {
        try {
            const handler = require(handlerPath);
            await handler(req, res);
        } catch (error) {
            console.error('Erro no handler:', error);
            res.status(500).json({ success: false, message: 'Erro interno do servidor' });
        }
    };
};

// Rotas da API
app.post('/api/login', createHandler('./api/login'));
app.get('/api/health', createHandler('./api/health'));
app.get('/api/stats', createHandler('./api/stats'));
app.get('/api/teams', createHandler('./api/teams'));
app.post('/api/teams', createHandler('./api/teams'));
app.get('/api/players', createHandler('./api/players'));
app.post('/api/players', createHandler('./api/players'));
app.get('/api/templates', createHandler('./api/templates'));
app.post('/api/templates', createHandler('./api/templates'));
app.put('/api/templates/:id', createHandler('./api/templates'));
app.delete('/api/templates/:id', createHandler('./api/templates'));
app.get('/api/tournaments', createHandler('./api/tournaments'));
app.post('/api/tournaments', createHandler('./api/tournaments'));
app.delete('/api/tournaments/:id', createHandler('./api/tournaments'));
app.get('/api/codes', createHandler('./api/coins'));
app.post('/api/codes', createHandler('./api/coins'));
app.delete('/api/codes/:id', createHandler('./api/coins'));
app.get('/api/user/:discordId', createHandler('./api/user/[discordId]'));

// Rota para servir o HTML principal
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'public', 'index.html'));
});

// Iniciar servidor
app.listen(PORT, () => {
    console.log(`🚀 Admin Panel rodando em: http://localhost:${PORT}`);
    console.log(`📊 Acesse o painel de administração no navegador`);
    console.log(`🔑 Use a senha: ${process.env.ADMIN_PASSWORD || 'admin123'}`);
});

module.exports = app;
