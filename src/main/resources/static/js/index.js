let stompClient = null;

function setConnected(connected) {
    document.getElementById('connect').disabled = connected;
    document.getElementById('disconnect').disabled = !connected;
}

let socket = null;
let server = 'notify.csctracker.com';
let secure = 's';
let urlBase = 'http' + secure + '://' + server + '/';

function connect() {
    let user = JSON.parse(localStorage.getItem('user'));
    console.log(user);
    if (user == null) {
        setVar('token', false);
        getUser().then(function (response) {
            user = response;
            setVar('token', true);
            connectToSocket(user);
        })
    } else {
        connectToSocket(user);
    }
    setVar('text', false);
}

function setVar(variable, hide) {
    let text = document.getElementById(variable).value;
    if (!isEmpty(text)) {
        localStorage.setItem(variable, text);
        if (hide) {
            let element = document.getElementById(variable);
            $(element).hide();
        }

    }
}

function connectToSocket(user) {
    socket = new WebSocket('ws' + secure + '://' + server + '/stock-ticks/websocket');
    // socket = new WebSocket('ws://127.0.0.1:8890/stock-ticks/websocket');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {
        setConnected(true);
        console.log('Connected: ' + frame);
        stompClient.subscribe('/topic/' + user.email, function (messageOutput) {
            msgRecived(JSON.parse(messageOutput.body));
        });
    });
}

function getUser() {
    return new Promise(((resolve, reject) => {
        get(urlBase + 'user').then(function (response) {
            console.log(response);
            localStorage.setItem('user', JSON.stringify(response));
            resolve(response)
        }).catch(reason => {
            console.log(reason)
            reject(reason)
        });
    }))
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

function msgRecived(messageOutput) {
    getmessage(messageOutput.id);
}

function getmessage(id) {
    get(urlBase + 'message/' + id).then(function (message) {
        console.log(message);
        showMessageOutput(message);
    });
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

    let textText = document.getElementById('text');
    let text = localStorage.getItem('text');
    if (text !== null) {
        $(textText).val(text)
    }

    let tokenText = document.getElementById('token');
    let token = localStorage.getItem('token');
    $(tokenText).val(token)
    if (token === null) {
        $(tokenText).show();
    } else {
        $(tokenText).hide();
        connect();
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
            localStorage.setItem('text', text);
            const notification = new Notification('Notification incoming from ' + messageOutput.app, {
                icon: 'images/csctracker-desktop-plugin.png',
                body: messageOutput.from + ": " + messageOutput.text + " (" + messageOutput.time + ")",
            });
            notification.onclick = function () {
                window.open('http' + secure + '://' + server + '/');
            };
        }
    }
}

function get(URL, data) {
    return new Promise(((resolve, reject) => {
        try {
            console.log(data)
            fetch(URL, {
                method: 'GET',
                headers: {
                    'Accept': 'application/json',
                    'Authorization': 'Bearer ' + document.getElementById('token').value,
                    'Content-Type': 'application/json'
                }
            }).then(value => {
                let user = value.json();
                resolve(user);
            }).catch(reason => {
                console.log(reason)
                reject(reason)
            })
        } catch (e) {
            console.log(e)
            reject("Opssssssssssss")
        }
    }))
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
            window.open(urlBase);
        };
    }
}

