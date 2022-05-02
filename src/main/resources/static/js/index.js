let stompClient = null;

function setConnected(connected) {
    document.getElementById('connect').disabled = connected;
    document.getElementById('disconnect').disabled = !connected;
}

let socket = null;
let server = 'notify.csctracker.com';
let secure = 's';
let urlBase = 'http' + secure + '://' + server + '/';
let urlBaseNoSlash = 'http' + secure + '://' + server;
let configs;

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
    getConfigs().then(function (response) {
        configs = response;
        localStorage.setItem('configs', configs);
        let textText = document.getElementById('text');
        $(textText).val(configs.favoriteContact);
        let appNotify = document.getElementById('app-notify');
        $(appNotify).val(configs.applicationNotify);
    })
    localStorage.setItem('text', document.getElementById('text').value);
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
    stompClient.heartbeat.outgoing = 5000;
    stompClient.heartbeat.incoming = 0;
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

function getConfigs() {
    return new Promise(((resolve, reject) => {
        get(urlBase + 'configs').then(function (response) {
            console.log(response);
            localStorage.setItem('configs', JSON.stringify(response));
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
    var li = document.createElement("li");
    if (isFavoriteContact(messageOutput)) {
        li.className = 'list-group-item bg-green';
    } else {
        if (isFavoriteApp(messageOutput)) {
            li.className = 'list-group-item bg-blue';
        } else {
            li.className = 'list-group-item';
        }
    }
    li.textContent = messageOutput.app + " - " + messageOutput.from + ": " + messageOutput.text + " (" + messageOutput.time + ")";
    $("ul").prepend(li);
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
    let data = tokenText.value;
    $(tokenText).val(token);
    if (token === null && isEmpty(data)) {
        window.location.replace("https://accounts.google.com/o/oauth2/auth?approval_prompt=force&scope=email&client_id=92132449986-f85cvq6rmtl8u7g24on48g562p98db8p.apps.googleusercontent.com&redirect_uri=" + urlBaseNoSlash + "&response_type=code&access_type=offline");
    } else {
        if (!isEmpty(data)) {
            $(tokenText).val(data);
            localStorage.setItem('token', data);
        }
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
        if (isNotify(messageOutput)) {
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

function isFavoriteContact(messageOutput) {
    let text = document.getElementById('text').value;
    return ((!isEmpty(text) && messageOutput.from.includes(text)) || text === '*')
}

function isFavoriteApp(messageOutput) {
    let text = configs.applicationNotify;
    return ((!isEmpty(text) && messageOutput.app === text) || text === '*')
}

function isNotify(messageOutput) {
    return isFavoriteContact(messageOutput) || isFavoriteApp(messageOutput);
}

function saveConfigs() {
    configs.applicationNotify = document.getElementById('app-notify').value;
    configs.favoriteContact = document.getElementById('text').value;
    post(urlBase + 'configs', configs);
}

function get(URL) {
    return new Promise(((resolve, reject) => {
        try {
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

function post(URL, info) {
    return new Promise(((resolve, reject) => {
        try {
            console.log(JSON.stringify(info))
            fetch(URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': 'Bearer ' + document.getElementById('token').value
                },
                body: JSON.stringify(info)
            }).then(value => {
                resolve(value);
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

