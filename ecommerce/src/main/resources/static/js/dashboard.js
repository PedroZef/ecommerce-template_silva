document.addEventListener('DOMContentLoaded', () => {
    // API Endpoints
    const PRODUCTS_API = '/api/produtos';
    const ORDERS_API = '/api/pedidos';
    const VOICE_API = '/api/assistant/voice';
    const TEXT_API = '/api/assistant/text';

    // State Variables
    let mediaRecorder = null;
    let audioChunks = [];
    let isRecording = false;
    let recordingTimerInterval = null;
    let recordingSeconds = 0;

    let categoryChartInstance = null;
    let trendChartInstance = null;

    // DOM Elements - Voice Assistant (Inline)
    const recordBtn = document.getElementById('record-btn');
    const timerDisplay = document.getElementById('recording-timer');
    const voiceStatus = document.getElementById('voice-status');
    const responseBox = document.getElementById('response-box');
    const responseText = document.getElementById('response-text');
    const queryForm = document.getElementById('query-form');
    const queryInput = document.getElementById('query-input');

    // DOM Elements - Recent Orders Table
    const orderList = document.getElementById('order-list');

    // 1. Initial Load of Charts and Table
    fetchDashboardData();

    // 2. Text Query Submission (Inline Assistant)
    if (queryForm) {
        queryForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const text = queryInput.value.trim();
            if (!text) return;

            voiceStatus.innerText = 'Processando com a IA...';
            responseText.innerText = '';
            responseBox.classList.add('hidden');

            try {
                const response = await fetch(TEXT_API, {
                    method: 'POST',
                    headers: { 'Content-Type': 'text/plain' },
                    body: text
                });

                const data = await response.text();
                if (!response.ok) throw new Error(data || 'Falha no servidor.');

                responseText.innerText = data;
                responseBox.classList.remove('hidden');
                voiceStatus.innerText = 'Consulta processada com sucesso!';
                queryInput.value = '';
                
                // Refresh data in case orders/products were updated
                fetchDashboardData();
            } catch (err) {
                console.error(err);
                voiceStatus.innerText = 'Erro ao processar consulta.';
                responseText.innerText = 'Erro: ' + err.message;
                responseBox.classList.remove('hidden');
            }
        });
    }

    // 3. Audio Recording (Inline Assistant)
    if (recordBtn) {
        recordBtn.addEventListener('click', async () => {
            if (!isRecording) {
                await startRecording();
            } else {
                stopRecording();
            }
        });
    }

    async function startRecording() {
        audioChunks = [];
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            
            let options = { mimeType: 'audio/webm' };
            if (!MediaRecorder.isTypeSupported(options.mimeType)) {
                options = { mimeType: 'audio/ogg' };
            }
            if (!MediaRecorder.isTypeSupported(options.mimeType)) {
                options = { mimeType: 'audio/mp4' };
            }
            if (!MediaRecorder.isTypeSupported(options.mimeType)) {
                options = {};
            }

            mediaRecorder = new MediaRecorder(stream, options);
            mediaRecorder.addEventListener('dataavailable', event => {
                if (event.data.size > 0) audioChunks.push(event.data);
            });

            mediaRecorder.addEventListener('stop', async () => {
                let extension = 'webm';
                if (options.mimeType) {
                    const match = options.mimeType.match(/audio\/([^;]+)/);
                    if (match) extension = match[1];
                }
                if (extension === 'xm4a' || extension === 'mp4') extension = 'm4a';

                const audioBlob = new Blob(audioChunks, { type: options.mimeType || 'audio/webm' });
                stream.getTracks().forEach(track => track.stop());

                await sendAudio(audioBlob, extension);
            });

            mediaRecorder.start();
            isRecording = true;
            recordBtn.classList.add('recording');
            voiceStatus.innerText = 'Ouvindo... Fale agora!';
            
            recordingSeconds = 0;
            timerDisplay.innerText = '00:00';
            recordingTimerInterval = setInterval(() => {
                recordingSeconds++;
                const mins = String(Math.floor(recordingSeconds / 60)).padStart(2, '0');
                const secs = String(recordingSeconds % 60).padStart(2, '0');
                timerDisplay.innerText = `${mins}:${secs}`;
            }, 1000);

        } catch (err) {
            console.error('Erro de permissão do microfone:', err);
            voiceStatus.innerText = 'Acesso ao microfone negado.';
        }
    }

    function stopRecording() {
        if (mediaRecorder && isRecording) {
            mediaRecorder.stop();
            isRecording = false;
            recordBtn.classList.remove('recording');
            clearInterval(recordingTimerInterval);
            voiceStatus.innerText = 'Processando áudio com a IA...';
        }
    }

    async function sendAudio(blob, extension) {
        const formData = new FormData();
        formData.append('audio', blob, `voice_command.${extension}`);

        responseText.innerText = '';
        responseBox.classList.add('hidden');

        try {
            const response = await fetch(VOICE_API, {
                method: 'POST',
                body: formData
            });

            const data = await response.text();
            if (!response.ok) throw new Error(data || 'Falha no servidor.');

            responseText.innerText = data;
            responseBox.classList.remove('hidden');
            voiceStatus.innerText = 'Áudio processado com sucesso!';
            
            // Refresh data
            fetchDashboardData();
        } catch (err) {
            console.error(err);
            voiceStatus.innerText = 'Erro ao processar áudio.';
            responseText.innerText = 'Erro: ' + err.message;
            responseBox.classList.remove('hidden');
        }
    }

    // 4. Fetch and render data
    async function fetchDashboardData() {
        try {
            const [productsRes, ordersRes] = await Promise.all([
                fetch(PRODUCTS_API),
                fetch(ORDERS_API)
            ]);

            if (!productsRes.ok || !ordersRes.ok) return;

            const products = await productsRes.json();
            const orders = await ordersRes.json();

            renderCharts(products, orders);
            renderRecentOrders(orders);
        } catch (err) {
            console.error('Erro ao buscar dados do dashboard:', err);
        }
    }

    function renderCharts(products, orders) {
        // Group products by Category
        const categoryData = {};
        products.forEach(p => {
            const catName = p.categoria ? p.categoria.nome : 'Sem Categoria';
            categoryData[catName] = (categoryData[catName] || 0) + 1;
        });

        const catLabels = Object.keys(categoryData);
        const catValues = Object.values(categoryData);

        // Chart 1: Doughnut Chart
        const ctxCat = document.getElementById('categoryChart');
        if (ctxCat) {
            if (categoryChartInstance) categoryChartInstance.destroy();
            categoryChartInstance = new Chart(ctxCat, {
                type: 'doughnut',
                data: {
                    labels: catLabels,
                    datasets: [{
                        data: catValues,
                        backgroundColor: [
                            '#6366f1', '#a855f7', '#ec4899', '#f59e0b', '#10b981', '#3b82f6'
                        ],
                        borderWidth: 1,
                        borderColor: 'rgba(255, 255, 255, 0.1)'
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'bottom',
                            labels: { color: '#9fa6bc', font: { family: 'Outfit' } }
                        },
                        title: {
                            display: true,
                            text: 'Produtos por Categoria',
                            color: '#f8f9fa',
                            font: { size: 14, weight: 'bold', family: 'Outfit' }
                        }
                    }
                }
            });
        }

        // Group orders faturamento by date (format date yyyy-MM-dd)
        const orderDataByDate = {};
        orders.forEach(o => {
            if (!o.dataPedido) return;
            const dateStr = o.dataPedido.substring(0, 10);
            orderDataByDate[dateStr] = (orderDataByDate[dateStr] || 0) + parseFloat(o.total || 0);
        });

        // Sort dates
        const sortedDates = Object.keys(orderDataByDate).sort();
        const faturamentoValues = sortedDates.map(d => orderDataByDate[d]);

        // Reformat dates to local format (dd/MM)
        const formattedDates = sortedDates.map(d => {
            const parts = d.split('-');
            return parts.length === 3 ? `${parts[2]}/${parts[1]}` : d;
        });

        // Chart 2: Line Chart for Faturamento
        const ctxTrend = document.getElementById('trendChart');
        if (ctxTrend) {
            if (trendChartInstance) trendChartInstance.destroy();
            trendChartInstance = new Chart(ctxTrend, {
                type: 'line',
                data: {
                    labels: formattedDates.length > 0 ? formattedDates : ['Sem Pedidos'],
                    datasets: [{
                        label: 'Faturamento (R$)',
                        data: faturamentoValues.length > 0 ? faturamentoValues : [0],
                        borderColor: '#05e695',
                        backgroundColor: 'rgba(5, 230, 149, 0.1)',
                        fill: true,
                        tension: 0.4,
                        borderWidth: 2
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false },
                        title: {
                            display: true,
                            text: 'Faturamento de Vendas por Data',
                            color: '#f8f9fa',
                            font: { size: 14, weight: 'bold', family: 'Outfit' }
                        }
                    },
                    scales: {
                        y: {
                            grid: { color: 'rgba(255, 255, 255, 0.05)' },
                            ticks: { color: '#9fa6bc', font: { family: 'Outfit' } }
                        },
                        x: {
                            grid: { display: false },
                            ticks: { color: '#9fa6bc', font: { family: 'Outfit' } }
                        }
                    }
                }
            });
        }
    }

    function renderRecentOrders(orders) {
        if (!orderList) return;
        orderList.innerHTML = '';

        if (orders.length === 0) {
            orderList.innerHTML = `
                <tr>
                    <td colspan="5" style="text-align: center; color: #9fa6bc; padding: 20px;">
                        Nenhum pedido registrado no sistema.
                    </td>
                </tr>
            `;
            return;
        }

        // Sort orders desc by ID and limit to 5
        const recent = orders.sort((a, b) => b.id - a.id).slice(0, 5);

        recent.forEach(o => {
            const dateObj = new Date(o.dataPedido);
            const formattedDate = o.dataPedido ? 
                `${String(dateObj.getDate()).padStart(2, '0')}/${String(dateObj.getMonth() + 1).padStart(2, '0')} ${String(dateObj.getHours()).padStart(2, '0')}:${String(dateObj.getMinutes()).padStart(2, '0')}` : 
                'N/A';

            let statusClass = 'badge-warning';
            if (o.status === 'CONCLUIDO') {
                statusClass = 'badge-success';
            } else if (o.status === 'CANCELADO') {
                statusClass = 'badge-danger';
            }

            const row = document.createElement('tr');
            row.innerHTML = `
                <td>#${o.id}</td>
                <td>${formattedDate}</td>
                <td>${o.cliente ? o.cliente.nome : 'N/A'}</td>
                <td><strong>R$ ${parseFloat(o.total || 0).toFixed(2).replace('.', ',')}</strong></td>
                <td><span class="badge ${statusClass}">${o.status}</span></td>
            `;
            orderList.appendChild(row);
        });
    }
});
