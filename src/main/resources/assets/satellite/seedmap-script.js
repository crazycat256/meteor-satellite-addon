const biomeDimensionSelect = document.getElementById("biome-dimension-select");
const mapGotoX = document.getElementById("map-goto-x");
const mapGotoZ = document.getElementById("map-goto-z");
const goButton = document.getElementById("map-goto-go");

let socket;

function connect() {
    try {
        socket = new WebSocket('ws://localhost:%s');
    } catch (e) {
        e.preventDefault();
    }

    socket.onopen = function(event) {
        console.log("Connected to server");
    };

    socket.onmessage = function (event) {
        const message = event.data;
        let packet = JSON.parse(message);

        if (packet.x)
            mapGotoX.value = packet.x;
        if (packet.z)
            mapGotoZ.value = packet.z;
        if (packet.dimension)
            biomeDimensionSelect.value = packet.dimension;
        biomeDimensionSelect.dispatchEvent(new Event("change"))

        if (packet.x || packet.z)
            goButton.click();
    };

    socket.onclose = function (event) {
        event.preventDefault();
        if (socket && socket.readyState === WebSocket.OPEN)
            return;
        setTimeout(connect, 2000);
    };

    socket.onerror = function (event) {
        event.preventDefault();
        socket.close();
    }
}

connect();
