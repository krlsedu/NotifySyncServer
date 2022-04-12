let stompClient = null;

function setConnected(connected) {
    document.getElementById('connect').disabled = connected;
    document.getElementById('disconnect').disabled = !connected;
}

let socket = null;

function connect() {
    socket = new WebSocket('wss://notify.csctracker.com/stock-ticks/websocket');
    // socket = new WebSocket('ws://127.0.0.1:8890/stock-ticks/websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/messages', function (messageOutput) {
            showMessageOutput(JSON.parse(messageOutput.body));
        });
    });
}

function disconnect() {
    if (stompClient != null) {
        stompClient.disconnect();
    }
    setConnected(false);
    console.log("Disconnected");
}

function sendMessage() {
    let from = "chrome";
    let app = "Chrome";
    let text = document.getElementById('text').value;
    stompClient.send("/app/chat", {}, JSON.stringify({'from': from, 'text': text, 'app': app}));
}

function showMessageOutput(messageOutput) {
    var ul = document.querySelector("ul");
    var li = document.createElement("li");
    li.className = 'list-group-item';
    li.textContent = messageOutput.app + " - " + messageOutput.from + ": " + messageOutput.text + " (" + messageOutput.time + ")";
    ul.appendChild(li);
    notify(messageOutput)
}


document.addEventListener('DOMContentLoaded', function () {
    if (!Notification) {
        alert('Desktop notifications not available in your browser. Try Chromium.');
        return;
    }

    if (Notification.permission !== 'granted')
        Notification.requestPermission();
});


function notify(messageOutput) {
    if (Notification.permission !== 'granted')
        Notification.requestPermission();
    else {
        let text = document.getElementById('text').value;

        if ((!isEmpty(text) && messageOutput.from.includes(text)) || text === '*') {
            const notification = new Notification('Notification incoming from ' + messageOutput.app, {
                icon: 'images/csctracker-desktop-plugin.png',
                body: messageOutput.from + ": " + messageOutput.text + " (" + messageOutput.time + ")",
            });
            notification.onclick = function () {
                window.open('https://notify.csctracker.com/');
            };
        }
    }
}

function isEmpty(str) {
    return (!str || str.trim() === "");
}

function notifyMe(messageOutput, title) {
    if (Notification.permission !== 'granted')
        Notification.requestPermission();
    else {
        const notification = new Notification(title, {
            icon: 'images/csctracker-desktop-plugin.png',
            body: messageOutput,
        });
        notification.onclick = function () {
            window.open('https://notify.csctracker.com/');
        };
    }
}

