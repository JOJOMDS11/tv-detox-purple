require('dotenv').config();
const { Pool, types } = require('pg');

types.setTypeParser(20, (value) => parseInt(value, 10));

let pool = null;

const buildConfig = () => {
    const url = process.env.DATABASE_URL || process.env.SUPABASE_DB_URL;
    const sslDisabled = String(process.env.DB_SSL || '').toLowerCase() === 'false';
    const base = {
        max: parseInt(process.env.DB_POOL_MAX || '3', 10),
        idleTimeoutMillis: 20000,
        connectionTimeoutMillis: 15000
    };
    if (url) {
        return { ...base, connectionString: url, ssl: sslDisabled ? false : { rejectUnauthorized: false } };
    }
    return {
        ...base,
        host: process.env.DB_HOST,
        port: parseInt(process.env.DB_PORT || '5432', 10),
        user: process.env.DB_USER,
        password: process.env.DB_PASSWORD,
        database: process.env.DB_NAME || 'postgres',
        ssl: sslDisabled ? false : { rejectUnauthorized: false }
    };
};

const getPool = () => {
    if (!pool) {
        pool = new Pool(buildConfig());
        pool.on('error', (error) => console.error('Erro inesperado no pool Postgres:', error.message));
    }
    return pool;
};

const convertPlaceholders = (sql) => {
    let out = '';
    let index = 0;
    let quote = null;
    for (let i = 0; i < sql.length; i++) {
        const ch = sql[i];
        if (quote) {
            out += ch;
            if (ch === quote) {
                if (sql[i + 1] === quote) {
                    out += sql[++i];
                } else {
                    quote = null;
                }
            }
            continue;
        }
        if (ch === "'" || ch === '"') {
            quote = ch;
            out += ch;
            continue;
        }
        if (ch === '?') {
            index += 1;
            out += `$${index}`;
            continue;
        }
        out += ch;
    }
    return out;
};

const translate = (original) => {
    let sql = String(original).trim().replace(/;+\s*$/, '');

    const showTablesLike = sql.match(/^SHOW\s+TABLES\s+LIKE\s+'([^']+)'$/i);
    if (showTablesLike) {
        return { sql: `SELECT table_name AS "Tables" FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = '${showTablesLike[1].toLowerCase()}'`, kind: 'select' };
    }
    if (/^SHOW\s+TABLES$/i.test(sql)) {
        return { sql: 'SELECT table_name AS "Tables" FROM information_schema.tables WHERE table_schema = current_schema() ORDER BY table_name', kind: 'select' };
    }
    const showColumns = sql.match(/^SHOW\s+COLUMNS\s+FROM\s+`?(\w+)`?(?:\s+LIKE\s+'([^']+)')?$/i);
    if (showColumns) {
        const extra = showColumns[2] ? ` AND column_name = '${showColumns[2].toLowerCase()}'` : '';
        return { sql: `SELECT column_name AS "Field" FROM information_schema.columns WHERE table_schema = current_schema() AND table_name = '${showColumns[1].toLowerCase()}'${extra} ORDER BY ordinal_position`, kind: 'select' };
    }

    sql = sql.replace(/`/g, '');
    sql = sql.replace(/\bRAND\s*\(\s*\)/gi, 'RANDOM()');
    sql = sql.replace(/DATE_SUB\(\s*NOW\(\)\s*,\s*INTERVAL\s+(\d+)\s+(SECOND|MINUTE|HOUR|DAY)S?\s*\)/gi, (m, n, unit) => `(NOW() - INTERVAL '${n} ${unit.toLowerCase()}s')`);

    let kind = 'other';
    if (/^\s*(SELECT|WITH)\b/i.test(sql)) {
        kind = 'select';
    } else if (/^\s*INSERT\b/i.test(sql)) {
        kind = 'insert';
        if (/^\s*INSERT\s+IGNORE\b/i.test(sql)) {
            sql = sql.replace(/^\s*INSERT\s+IGNORE\b/i, 'INSERT');
            if (!/ON\s+CONFLICT/i.test(sql)) sql += ' ON CONFLICT DO NOTHING';
        }
        if (!/\bRETURNING\b/i.test(sql)) sql += ' RETURNING id';
    } else if (/^\s*(UPDATE|DELETE)\b/i.test(sql)) {
        kind = 'modify';
    }

    return { sql: convertPlaceholders(sql), kind };
};

const run = async (executor, text, params = []) => {
    const { sql, kind } = translate(text);
    const result = await executor.query(sql, params);
    if (kind === 'select') {
        return [result.rows, result.fields];
    }
    if (kind === 'insert') {
        const first = result.rows && result.rows[0];
        return [{ insertId: first && first.id !== undefined ? Number(first.id) : 0, affectedRows: result.rowCount, rowCount: result.rowCount }];
    }
    return [{ insertId: 0, affectedRows: result.rowCount || 0, rowCount: result.rowCount || 0 }];
};

const execute = (text, params) => run(getPool(), text, params);

const query = async (text, params) => {
    const [value] = await execute(text, params);
    return value;
};

const ping = () => getPool().query('SELECT 1');

const end = async () => {
    if (pool) {
        await pool.end();
        pool = null;
    }
};

module.exports = { execute, query: execute, ping, end, rows: query, translate };
