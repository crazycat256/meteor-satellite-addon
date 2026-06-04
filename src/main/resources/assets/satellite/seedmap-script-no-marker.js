let socket;

function connect() {
    try {
        socket = new WebSocket('ws://localhost:%s');
    } catch (e) {
        console.error("WebSocket connection failed:", e);
    }

    socket.onopen = function(event) {
        console.log("Connected to server");
    };

    socket.onmessage = function (event) {
        const { x, z, dimension } = JSON.parse(event.data);

        const params = new URLSearchParams(window.location.hash.substring(1));

        if (x) params.set('x', x);
        if (z) params.set('z', z);
        if (dimension) params.set('dimension', dimension);

        window.location.hash = params.toString();
    };

    socket.onclose = function (event) {
        console.log("WebSocket closed, reconnecting in 2 seconds...");
        if (socket && socket.readyState === WebSocket.OPEN)
            return;
        setTimeout(connect, 2000);
    };

    socket.onerror = function (event) {
        console.error("WebSocket error observed:", event);
        socket.close();
    }
}

connect();
