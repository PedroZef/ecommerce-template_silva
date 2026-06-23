document.addEventListener('DOMContentLoaded', () => {
    // API Endpoints
    const VOICE_API = '/api/assistant/voice';
    const TEXT_API = '/api/assistant/text';
    const LATEST_API = '/api/assistant/latest';
    const INTERACTIONS_API = '/api/assistant/interactions';

    // State Variables
    let mediaRecorder = null;
    let audioChunks = [];
    let isRecording = false;
    let recordingTimerInterval = null;
    let recordingSeconds = 0;
    let lastInteractionId = null;
    let pollingInterval = null;
    const pageLoadTime = Date.now();

    // DOM Elements
    const widgetContainer = document.createElement('div');
    widgetContainer.className = 'assistant-widget';
    widgetContainer.innerHTML = `
        <button id="assistant-trigger" class="assistant-btn" title="Falar com Assistente IA">
            <!-- Icone do Robo/IA -->
            <svg viewBox="0 0 24 24">
                <path d="M19 8h-1.18c-.46-1.62-1.54-2.99-3.02-3.73L16 2.59 14.59 1.18 12.18 4H11.8L9.41 1.18 8 2.59 9.2 4.27C7.72 5.01 6.64 6.38 6.18 8H5c-1.1 0-2 .9-2 2v3c0 1.1.9 2 2 2h1v2c0 1.66 1.34 3 3 3h8c1.66 0 3-1.34 3-3v-2h1c1.1 0 2-.9 2-2v-3c0-1.1-.9-2-2-2zm-2 9c0 .55-.45 1-1 1H8c-.55 0-1-.45-1-1v-2h10v2zm2-4H5v-3h14v3zM9 10.5c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zm6 0c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5z"/>
            </svg>
        </button>
        <div id="assistant-panel" class="assistant-chat-panel">
            <div class="assistant-header">
                <div class="assistant-title-container">
                    <h3 class="assistant-title">Assistente Inteligente</h3>
                    <span class="assistant-status">Online (Llama 3.3)</span>
                </div>
            </div>
            <div id="assistant-chat-body" class="assistant-body">
                <div class="message-bubble message-ai">
                    Olá! Sou o Assistente IA do E-Commerce. Você pode me fazer perguntas por texto ou gravar um áudio.
                    
                    Exemplos:
                    • "Como está o estoque do Smartphone?"
                    • "Lista os produtos da categoria Roupas"
                    • "Qual o faturamento total das vendas?"
                    • "Cadastrar produto Relógio Inteligente custando 350 com estoque 15 na categoria Eletrônicos"
                </div>
            </div>
            <div class="assistant-footer">
                <div id="assistant-mic-status" class="assistant-audio-status">
                    <span>Gravando comando...</span>
                    <span id="assistant-mic-timer" class="assistant-audio-timer">00:00</span>
                </div>
                <form id="assistant-chat-form" class="assistant-form">
                    <input type="text" id="assistant-input-field" class="assistant-input" placeholder="Pergunte algo ao sistema..." required autocomplete="off">
                    <button type="button" id="assistant-mic-btn" class="voice-record-btn" title="Gravar voz">
                        <svg viewBox="0 0 24 24">
                            <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.3-3c0 3-2.54 5.1-5.3 5.1S6.7 14 6.7 11H5c0 3.41 2.72 6.23 6 6.72V21h2v-3.28c3.28-.48 6-3.3 6-6.72h-1.7z"/>
                        </svg>
                    </button>
                </form>
            </div>
        </div>
    `;

    document.body.appendChild(widgetContainer);

    const triggerBtn = document.getElementById('assistant-trigger');
    const panel = document.getElementById('assistant-panel');
    const chatBody = document.getElementById('assistant-chat-body');
    const chatForm = document.getElementById('assistant-chat-form');
    const inputField = document.getElementById('assistant-input-field');
    const micBtn = document.getElementById('assistant-mic-btn');
    const micStatus = document.getElementById('assistant-mic-status');
    const micTimer = document.getElementById('assistant-mic-timer');

    // Toggle panel view
    triggerBtn.addEventListener('click', () => {
        panel.classList.toggle('show');
        triggerBtn.classList.toggle('open');
        if (panel.classList.contains('show')) {
            inputField.focus();
            scrollToBottom();
            // Start polling when panel is open
            startPolling();
        } else {
            stopPolling();
        }
    });

    // Handle form submit (Text command)
    chatForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const text = inputField.value.trim();
        if (!text) return;

        appendMessage('user', text);
        inputField.value = '';

        const aiBubble = appendMessage('ai', 'Pensando...');

        try {
            const response = await fetch(TEXT_API, {
                method: 'POST',
                headers: { 'Content-Type': 'text/plain' },
                body: text
            });

            const data = await response.text();
            if (!response.ok) throw new Error(data || 'Servidor retornou erro.');
            
            aiBubble.innerText = data;
            
            // Sync polling id
            await syncLatestInteraction();
        } catch (err) {
            console.error('Erro na requisição de texto:', err);
            aiBubble.innerText = 'Erro ao processar consulta: ' + err.message;
        }
        scrollToBottom();
    });

    // Audio recording logic
    micBtn.addEventListener('click', async () => {
        if (!isRecording) {
            await startRecording();
        } else {
            stopRecording();
        }
    });

    async function startRecording() {
        audioChunks = [];
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            
            // Determine appropriate mime type
            let options = { mimeType: 'audio/webm' };
            if (!MediaRecorder.isTypeSupported(options.mimeType)) {
                options = { mimeType: 'audio/ogg' };
            }
            if (!MediaRecorder.isTypeSupported(options.mimeType)) {
                options = { mimeType: 'audio/mp4' };
            }
            if (!MediaRecorder.isTypeSupported(options.mimeType)) {
                options = {}; // fallback Browser default
            }

            mediaRecorder = new MediaRecorder(stream, options);
            
            mediaRecorder.addEventListener('dataavailable', event => {
                if (event.data.size > 0) {
                    audioChunks.push(event.data);
                }
            });

            mediaRecorder.addEventListener('stop', async () => {
                // Determine audio extension
                let extension = 'webm';
                if (options.mimeType) {
                    const match = options.mimeType.match(/audio\/([^;]+)/);
                    if (match) extension = match[1];
                }
                if (extension === 'xm4a' || extension === 'mp4') extension = 'm4a';

                const audioBlob = new Blob(audioChunks, { type: options.mimeType || 'audio/webm' });
                
                // Close mic track
                stream.getTracks().forEach(track => track.stop());

                await sendAudio(audioBlob, extension);
            });

            mediaRecorder.start();
            isRecording = true;
            micBtn.classList.add('recording');
            micStatus.classList.add('show');
            
            recordingSeconds = 0;
            micTimer.innerText = '00:00';
            recordingTimerInterval = setInterval(() => {
                recordingSeconds++;
                const mins = String(Math.floor(recordingSeconds / 60)).padStart(2, '0');
                const secs = String(recordingSeconds % 60).padStart(2, '0');
                micTimer.innerText = `${mins}:${secs}`;
            }, 1000);

        } catch (err) {
            console.error('Acesso ao microfone negado ou indisponível:', err);
            alert('Não foi possível acessar o microfone para gravação de áudio.');
        }
    }

    function stopRecording() {
        if (mediaRecorder && isRecording) {
            mediaRecorder.stop();
            isRecording = false;
            micBtn.classList.remove('recording');
            micStatus.classList.remove('show');
            clearInterval(recordingTimerInterval);
        }
    }

    async function sendAudio(blob, extension) {
        const formData = new FormData();
        formData.append('audio', blob, `voice_command.${extension}`);

        const aiBubble = appendMessage('ai', 'Transcrevendo e processando áudio...');

        try {
            const response = await fetch(VOICE_API, {
                method: 'POST',
                body: formData
            });

            const data = await response.text();
            if (!response.ok) throw new Error(data || 'Servidor retornou erro.');

            aiBubble.innerText = data;
            
            // Sync polling id
            await syncLatestInteraction();
        } catch (err) {
            console.error('Erro na requisição de áudio:', err);
            aiBubble.innerText = 'Erro ao processar áudio: ' + err.message;
        }
        scrollToBottom();
    }

    // Helper to append message bubbles
    function appendMessage(sender, text) {
        const bubble = document.createElement('div');
        bubble.className = `message-bubble message-${sender}`;
        bubble.innerText = text;
        chatBody.appendChild(bubble);
        scrollToBottom();
        return bubble;
    }

    function scrollToBottom() {
        chatBody.scrollTop = chatBody.scrollHeight;
    }

    // Polling background operations (detect swagger/external calls)
    function startPolling() {
        // Sync once at open
        syncLatestInteraction();
        
        // Interval checking every 5 seconds
        if (!pollingInterval) {
            pollingInterval = setInterval(checkLatestInteraction, 5000);
        }
    }

    function stopPolling() {
        if (pollingInterval) {
            clearInterval(pollingInterval);
            pollingInterval = null;
        }
    }

    async function checkLatestInteraction() {
        try {
            const response = await fetch(LATEST_API);
            if (response.status === 204) return;
            if (!response.ok) return;

            const data = await response.json();
            if (!data || !data.id) return;

            if (data.id !== lastInteractionId) {
                lastInteractionId = data.id;

                // If interaction was triggered while this page was loaded (with 5s buffer)
                if (data.timestamp > pageLoadTime - 5000) {
                    const container = document.createElement('div');
                    container.className = 'message-bubble message-ai';
                    container.innerHTML = `
                        <div class="external-interaction">
                            <strong style="color: #6366f1; display: block; font-size: 0.75rem; margin-bottom: 4px;">⚡ COMANDO EXTERNO DETECTADO:</strong>
                            <div style="font-style: italic; margin-bottom: 6px; font-size: 0.8rem;">"${data.query}"</div>
                            <div style="border-top: 1px solid rgba(255,255,255,0.08); padding-top: 6px;">
                                ${data.response}
                            </div>
                        </div>
                    `;
                    chatBody.appendChild(container);
                    scrollToBottom();
                    
                    // If on dashboard, reload state or trigger page update if available
                    if (window.location.pathname === '/' || window.location.pathname.includes('/home') || window.location.pathname.includes('/dashboard')) {
                        // Quick delay then refresh page to reflect changes
                        setTimeout(() => {
                            window.location.reload();
                        }, 2000);
                    }
                }
            }
        } catch (e) {
            console.warn('Erro polling de interações:', e);
        }
    }

    async function syncLatestInteraction() {
        try {
            const response = await fetch(LATEST_API);
            if (response.ok && response.status !== 204) {
                const data = await response.json();
                if (data && data.id) {
                    lastInteractionId = data.id;
                }
            }
        } catch (e) {
            console.warn('Erro ao sincronizar interações:', e);
        }
    }
});
