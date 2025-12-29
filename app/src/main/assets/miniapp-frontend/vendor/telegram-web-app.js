window.Telegram = {
    WebApp: {
        ready: function () {
        },
        expand: function () {
        },
        close: function () {
            if (window.MindWarrior && typeof window.MindWarrior.close === 'function') {
                window.MindWarrior.close();
            }
        },
        initDataUnsafe: {
            query_id: 'foobar'
        }
    }
}
