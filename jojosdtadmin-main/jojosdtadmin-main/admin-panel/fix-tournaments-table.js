// Script para verificar e corrigir a estrutura da tabela tournaments
const mysql = require('mysql2/promise');
require('dotenv').config();

const dbConfig = {
    host: process.env.DB_HOST,
    user: process.env.DB_USER,
    password: process.env.DB_PASSWORD,
    database: process.env.DB_NAME,
    port: process.env.DB_PORT || 3306
};

async function fixTournamentsTable() {
    console.log('🔍 Verificando estrutura da tabela tournaments...\n');

    try {
        const connection = await mysql.createConnection(dbConfig);
        console.log('✅ Conectado ao banco de dados\n');

        // Verificar se a tabela existe
        const [tables] = await connection.execute(`
      SELECT TABLE_NAME 
      FROM INFORMATION_SCHEMA.TABLES 
      WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'tournaments'
    `, [process.env.DB_NAME]);

        if (tables.length === 0) {
            console.log('❌ Tabela tournaments não existe. Criando...');

            // Criar tabela tournaments
            await connection.execute(`
        CREATE TABLE tournaments (
          id INT AUTO_INCREMENT PRIMARY KEY,
          name VARCHAR(255) NOT NULL,
          tournament_date DATE NOT NULL,
          tournament_time TIME NOT NULL,
          format ENUM('knockout', 'groups', 'league') DEFAULT 'knockout',
          max_players INT DEFAULT 8,
          prize_1st INT DEFAULT 0,
          prize_2nd INT DEFAULT 0,
          prize_3rd INT DEFAULT 0,
          status ENUM('open', 'closed', 'in_progress', 'finished') DEFAULT 'open',
          created_by VARCHAR(255),
          channel_id VARCHAR(255),
          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
        )
      `);

            console.log('✅ Tabela tournaments criada com sucesso!');
        } else {
            console.log('✅ Tabela tournaments existe. Verificando colunas...');

            // Verificar estrutura atual
            const [columns] = await connection.execute(`
        SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
        FROM INFORMATION_SCHEMA.COLUMNS 
        WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'tournaments'
        ORDER BY ORDINAL_POSITION
      `, [process.env.DB_NAME]);

            console.log('📋 Colunas atuais:');
            columns.forEach(col => {
                console.log(`   ${col.COLUMN_NAME} (${col.DATA_TYPE}) - ${col.IS_NULLABLE === 'YES' ? 'NULL' : 'NOT NULL'}`);
            });

            // Verificar se precisa adicionar colunas
            const columnNames = columns.map(col => col.COLUMN_NAME);

            const requiredColumns = [
                { name: 'tournament_date', type: 'DATE NOT NULL' },
                { name: 'tournament_time', type: 'TIME NOT NULL' },
                { name: 'format', type: "ENUM('knockout', 'groups', 'league') DEFAULT 'knockout'" },
                { name: 'max_players', type: 'INT DEFAULT 8' },
                { name: 'prize_1st', type: 'INT DEFAULT 0' },
                { name: 'prize_2nd', type: 'INT DEFAULT 0' },
                { name: 'prize_3rd', type: 'INT DEFAULT 0' },
                { name: 'status', type: "ENUM('open', 'closed', 'in_progress', 'finished') DEFAULT 'open'" },
                { name: 'created_by', type: 'VARCHAR(255)' },
                { name: 'channel_id', type: 'VARCHAR(255)' }
            ];

            for (const col of requiredColumns) {
                if (!columnNames.includes(col.name)) {
                    console.log(`➕ Adicionando coluna: ${col.name}`);
                    await connection.execute(`
            ALTER TABLE tournaments ADD COLUMN ${col.name} ${col.type}
          `);
                }
            }

            // Remover colunas problemáticas se existirem
            if (columnNames.includes('date')) {
                console.log('🔄 Renomeando coluna "date" para "tournament_date"');
                await connection.execute(`
          ALTER TABLE tournaments CHANGE COLUMN date tournament_date DATE NOT NULL
        `);
            }

            if (columnNames.includes('time')) {
                console.log('🔄 Renomeando coluna "time" para "tournament_time"');
                await connection.execute(`
          ALTER TABLE tournaments CHANGE COLUMN time tournament_time TIME NOT NULL
        `);
            }
        }

        // Verificar estrutura final
        const [finalColumns] = await connection.execute(`
      SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
      FROM INFORMATION_SCHEMA.COLUMNS 
      WHERE TABLE_SCHEMA = ? AND TABLE_NAME = 'tournaments'
      ORDER BY ORDINAL_POSITION
    `, [process.env.DB_NAME]);

        console.log('\n✅ Estrutura final da tabela tournaments:');
        finalColumns.forEach(col => {
            console.log(`   ${col.COLUMN_NAME} (${col.DATA_TYPE}) - ${col.IS_NULLABLE === 'YES' ? 'NULL' : 'NOT NULL'}`);
        });

        await connection.end();
        console.log('\n🎉 Correção da tabela tournaments concluída!');

    } catch (error) {
        console.error('❌ Erro:', error.message);
    }
}

fixTournamentsTable();
