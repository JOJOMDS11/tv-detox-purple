const express = require('express');
const router = express.Router();
const { query } = require('./db');

// POST /api/avatars
router.post('/', async (req, res) => {
    const { emoji, code, description } = req.body;
    if (!emoji || !code) {
        return res.status(400).json({ error: 'emoji e code são obrigatórios' });
    }
    try {
        // Evitar duplicidade
        const exists = await query('SELECT id FROM avatars WHERE code = ?', [code]);
        if (exists.length > 0) {
            return res.status(409).json({ error: 'Já existe esse código' });
        }
        await query('INSERT INTO avatars (emoji, code, description) VALUES (?, ?, ?)', [emoji, code, description || null]);
        res.status(201).json({ success: true });
    } catch (e) {
        res.status(500).json({ error: 'Erro ao inserir no banco', details: e.message });
    }
});

module.exports = router;
