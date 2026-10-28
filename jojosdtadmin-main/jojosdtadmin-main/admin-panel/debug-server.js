require('dotenv').config();

console.log('=== Iniciando servidor com debug ===');

try {
    const express = require('express');
    const path = require('path');
    const cors = require('cors');
    
    console.log('✅ Dependências carregadas');
    
    const app = express();
    const PORT = process.env.PORT || 3000;
    
    // Middleware
    app.use(cors());
    app.use(express.json());
    app.use(express.static(path.join(__dirname, 'public')));
    
    console.log('✅ Middleware configurado');
    
    // Função auxiliar para simular handlers Vercel
    const createHandler = (handlerModule) => {
        return async (req, res) => {
            try {
                await handlerModule(req, res);
            } catch (error) {
                console.error('Erro no handler:', error);
                res.status(500).json({ success: false, message: 'Erro interno do servidor' });
            }
        };
    };
    
    // Rota de teste básica
    app.get('/test', (req, res) => {
        res.json({ status: 'OK', timestamp: new Date().toISOString() });
    });
    
    // Testando carregamento do db.js
    console.log('🔄 Tentando carregar ./api/db...');
    const { query } = require('./api/db');
    console.log('✅ DB module carregado com sucesso');
    
    app.get('/api/stats', createHandler(async (req, res) => {
        res.setHeader('Access-Control-Allow-Origin', '*');

        try {
            console.log('🔄 Executando queries para stats...');
            
            // Testa primeiro se existe a tabela players
            try {
                const totalPlayers = await query('SELECT COUNT(*) as count FROM players');
                console.log('✅ Tabela players:', totalPlayers[0]?.count || 0);
            } catch (err) {
                console.log('❌ Erro tabela players:', err.message);
            }
            
            try {
                const totalTeams = await query('SELECT COUNT(*) as count FROM teams');
                console.log('✅ Tabela teams:', totalTeams[0]?.count || 0);
            } catch (err) {
                console.log('❌ Erro tabela teams:', err.message);
            }

            return res.status(200).json({
                success: true,
                data: {
                    totalPlayers: 5,
                    totalCards: 0,
                    totalTemplates: 0,
                    totalTeams: 5
                }
            });
        } catch (error) {
            console.error('Erro ao buscar stats:', error);
            return res.status(500).json({ success: false, message: 'Erro ao buscar estatísticas' });
        }
    }));
    
    app.listen(PORT, () => {
        console.log(`🚀 Admin Panel rodando em: http://localhost:${PORT}`);
        console.log(`📊 Acesse o painel de administração no navegador`);
        console.log(`🔑 Use a senha: admin123`);
    });
    
} catch (error) {
    console.error('❌ Erro crítico ao iniciar servidor:', error);
    process.exit(1);
}
