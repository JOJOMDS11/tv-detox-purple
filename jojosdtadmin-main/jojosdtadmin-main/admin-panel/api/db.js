const pg = require('./pg');

async function query(sql, params = []) {
    return pg.rows(sql, params);
}

async function testConnection() {
    try {
        await pg.ping();
        return true;
    } catch (error) {
        console.error('Falha no teste de conexão:', error.message);
        return false;
    }
}

async function closeConnection() {
    await pg.end();
}

module.exports = { query, testConnection, closeConnection };
