const pg = require('./pg');

module.exports = {
    execute: pg.execute,
    query: pg.query,
    ping: pg.ping,
    end: pg.end
};
