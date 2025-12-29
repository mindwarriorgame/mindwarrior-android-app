
// TODO move to a separate file maybe?
window.getServerBaseUrl = function() {
    let baseUrl = "https://boo.great-site.net";
    if (window.location.href.indexOf("ru.mindwarriorgame.org") >= 0) {
        baseUrl = "https://ru.mindwarriorgame.org/miniapp-backend";
    } else if (window.location.href.indexOf("file:") == 0) {
        baseUrl = undefined;
    }
    console.log("Base URL detected", baseUrl, window.location.href);
    return baseUrl;
}

class ServerTime {
    constructor() {
        this.clientStartedAt = parseInt(window.localStorage.getItem('clientTime') || Date.now());
        this.serverStartedAt = parseInt(window.localStorage.getItem('serverTime') || Date.now());
        let syncOffset = () => {};
        syncOffset = () => {
            const started = Date.now();
            let finished = Date.now();
            const baseUrl = window.getServerBaseUrl();
            if (!baseUrl) {
                return;
            }
            fetch(window.getServerBaseUrl() + '/time.php', {method: 'POST', mode: 'cors'})
                .then(response => {
                    if (response.ok) {
                        finished = Date.now();
                        return response.text();
                    } else {
                        throw new Error('Network response was not ok');
                    }
                })
                .then(serverTime => {
                    this.serverStartedAt = parseInt(serverTime) * 1000;
                    this.clientStartedAt = Date.now() - Math.floor((finished - started) / 2);

                    window.localStorage.setItem('serverTime', this.serverStartedAt);
                    window.localStorage.setItem('clientTime', this.clientStartedAt);

                    console.log("Server time offset: " + (this.serverStartedAt - this.clientStartedAt) + "ms");
                }).catch(err => {
                console.error(err);
                setTimeout(syncOffset, 1000);
            });
        };
        syncOffset();
    }

    now() {
        return this.serverStartedAt + (Date.now() - this.clientStartedAt);
    }
}