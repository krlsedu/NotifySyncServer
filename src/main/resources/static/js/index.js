let stompClient = null;

function setConnected(connected) {

    document.getElementById('connect').disabled = connected;
    document.getElementById('disconnect').disabled = !connected;
    document.getElementById('conversationDiv').style.visibility = connected ? 'visible' : 'hidden';
    document.getElementById('response').innerHTML = '';
}

function connect() {
    socket = new WebSocket("wss://www.csctracker.com/stock-ticks/websocket");
}

function sendMessage() {
    let from = "chrome";
    let app = "Chrome";
    let text = document.getElementById('text').value;
    stompClient.send("/app/chat", {}, JSON.stringify({'from': from, 'text': text, 'app': app}));
}

function showMessageOutput(messageOutput) {
    let response = document.getElementById('response');
    let p = document.createElement('p');
    p.appendChild(document.createTextNode(messageOutput.from + ": " + messageOutput.text + " (" + messageOutput.time + ")"));
    response.appendChild(p);
    notifyMe(messageOutput)
}


document.addEventListener('DOMContentLoaded', function () {
    if (!Notification) {
        alert('Desktop notifications not available in your browser. Try Chromium.');
        return;
    }

    if (Notification.permission !== 'granted')
        Notification.requestPermission();
});


function notifyMe(messageOutput) {
    if (Notification.permission !== 'granted')
        Notification.requestPermission();
    else {
        const notification = new Notification('Notification incoming from ' + messageOutput.app, {
            icon: 'images/whats.png',
            body: messageOutput.from + ": " + messageOutput.text + " (" + messageOutput.time + ")",
        });
        notification.onclick = function () {
            window.open('https://notify.csctracker.com/');
        };
    }
}

function notifyMe(messageOutput, title) {
    if (Notification.permission !== 'granted')
        Notification.requestPermission();
    else {
        const notification = new Notification(title, {
            icon: 'images/whats.png',
            body: messageOutput,
        });
        notification.onclick = function () {
            window.open('https://notify.csctracker.com/');
        };
    }
}

let socket = new WebSocket("wss://www.csctracker.com/stock-ticks/websocket");

socket.onopen = function (e) {
    setConnected(true);
    console.log(e);
    notifyMe("[open] Connection established",'Notify-client');
    //  socket.send("My name is John");
};

socket.onmessage = function (event) {
    showMessageOutput(event.data);
};

socket.onclose = function (event) {

    setConnected(false);
    if (event.wasClean) {
        notifyMe(`[close] Connection closed cleanly, code=${event.code} reason=${event.reason}`, 'Notify-client');
    } else {
        // e.g. server process killed or network down
        // event.code is usually 1006 in this case
        notifyMe('[close] Connection died', 'Notify-client');
    }
};

socket.onerror = function (error) {

    setConnected(false);
    notifyMe(`[error] ${error.message}`, 'Notify-client');
};