const messagesContainer = document.getElementById('messages');
const messageInput = document.getElementById('message-input');
const sendButton = document.getElementById('send-button');

// Connect to the WebSocket endpoint
const ws = new WebSocket('ws://' + location.host + '/websocket-chat');

ws.onopen = () => {
    console.log('Connected to chat service');
    addMessage('System', 'Welcome! How can I help you today?');
};

ws.onmessage = event => {
    addMessage('Bot', event.data);
};

ws.onerror = (error) => {
    console.error('WebSocket error:', error);
    addMessage('System', 'Connection error. Please refresh the page.');
};

ws.onclose = () => {
    console.log('Disconnected from chat service');
    addMessage('System', 'Disconnected. Please refresh the page to reconnect.');
};

function addMessage(sender, text) {
    const message = document.createElement('div');
    message.className = 'message';
    message.innerHTML = `<strong>${sender}:</strong> ${text.replace(/\n/g, '<br>')}`;
    messagesContainer.appendChild(message);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
}

sendButton.addEventListener('click', () => {
    const message = messageInput.value.trim();
    if (message && ws.readyState === WebSocket.OPEN) {
        addMessage('You', message);
        ws.send(message);
        messageInput.value = '';
    }
});

messageInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendButton.click();
    }
});