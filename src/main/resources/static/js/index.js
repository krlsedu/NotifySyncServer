let stompClient = null;

function setConnected(connected) {

    document.getElementById('connect').disabled = connected;
    document.getElementById('disconnect').disabled = !connected;
    document.getElementById('conversationDiv').style.visibility = connected ? 'visible' : 'hidden';
    document.getElementById('response').innerHTML = '';
}

function connect() {
    let socket = new SockJS('/chat');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/messages', function (messageOutput) {
            showMessageOutput(JSON.parse(messageOutput.body));
        });
    });
}

function reconnect() {
    disconnect();
    connect()
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
            window.open('http://localhost:8890');
        };
    }
    post(messageOutput);
}

function post(app) {
    return new Promise(((resolve, reject) => {
        try {
            console.log(app)
            fetch("http://127.0.0.1:8777/notificar",{
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(app)
            }).then(value =>{
                resolve(value);
            }).catch(reason => {
                reject(reason)
            })
        } catch (e) {
            console.log(e)
            reject("Opssssssssssss")
        }
    }))
}