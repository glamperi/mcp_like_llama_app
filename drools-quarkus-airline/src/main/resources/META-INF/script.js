const messagesContainer = document.getElementById('messages');
const messageInput = document.getElementById('message-input');
const sendButton = document.getElementById('send-button');

const ws = new WebSocket('ws://' + location.host + '/chat');

ws.onmessage = event => {
    const message = document.createElement('div');
    message.textContent = event.data;
    messagesContainer.appendChild(message);
    messagesContainer.scrollTop = messagesContainer.scrollHeight;
};

sendButton.onclick = () => {
    const message = messageInput.value;
    if (message) {
        ws.send(message);
        messageInput.value = '';
    }
};

messageInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        sendButton.click();
    }
});