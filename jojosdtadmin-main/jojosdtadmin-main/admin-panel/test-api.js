const http = require('http');

async function testAPI(endpoint) {
    return new Promise((resolve, reject) => {
        const options = {
            hostname: 'localhost',
            port: 3001,
            path: endpoint,
            method: 'GET'
        };

        const req = http.request(options, (res) => {
            let data = '';
            res.on('data', (chunk) => {
                data += chunk;
            });
            res.on('end', () => {
                resolve({
                    status: res.statusCode,
                    data: data
                });
            });
        });

        req.on('error', (err) => {
            reject(err);
        });

        req.end();
    });
}

async function runTests() {
    const endpoints = ['/api/stats', '/api/players', '/api/teams', '/api/templates'];
    
    console.log('🧪 Testando APIs do Admin Panel...\n');
    
    for (const endpoint of endpoints) {
        try {
            console.log(`📡 Testando ${endpoint}...`);
            const result = await testAPI(endpoint);
            console.log(`   Status: ${result.status}`);
            
            if (result.status === 200) {
                const jsonData = JSON.parse(result.data);
                console.log(`   ✅ Resposta: ${JSON.stringify(jsonData, null, 2)}`);
            } else {
                console.log(`   ❌ Erro: ${result.data}`);
            }
            
        } catch (error) {
            console.log(`   ❌ Erro na requisição: ${error.message}`);
        }
        console.log('');
    }
}

runTests();
