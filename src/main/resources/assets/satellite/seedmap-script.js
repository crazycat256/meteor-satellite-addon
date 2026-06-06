(function() {
    function setReactValue(input, value) {
        const setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
        setter.call(input, value);
        input.dispatchEvent(new Event('input', {bubbles: true}));
    }

    const dimensionSelect = document.querySelector('[data-testid="dimension-select"]');
    const mapGotoX = document.querySelector('[data-testid="map-goto-x"]');
    const mapGotoZ = document.querySelector('[data-testid="map-goto-z"]');
    const goButton = document.querySelector('[data-testid="map-goto-go"]');

    const styleElem = document.createElement('style');
    document.head.appendChild(styleElem);

    document.addEventListener('click', function() {
        styleElem.textContent = '';
    })

    let socket;
    function connect() {
        try {
            socket = new WebSocket('ws://localhost:%s');
        } catch (e) {
            console.error('WebSocket connection failed:', e);
        }

        socket.onopen = function() {
            console.log('Connected to server');
        };

        socket.onmessage = function(event) {
            const {x, z, dimension, tooltip} = JSON.parse(event.data);

            if (x) {
                setReactValue(mapGotoX, x);
            }

            if (z) {
                setReactValue(mapGotoZ, z);
            }

            if (dimension) {
                const selectSetter = Object.getOwnPropertyDescriptor(window.HTMLSelectElement.prototype, 'value').set;
                selectSetter.call(dimensionSelect, dimension);
                dimensionSelect.dispatchEvent(new Event('change', {bubbles: true}));
            }

            if (x || z) {
                goButton.dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true}));
            }

            if (tooltip === true) {
                styleElem.textContent = '';
            } else if (tooltip === false) {
                styleElem.textContent = 'div:has(> div[data-testid="pin-popover-content"]) { display: none !important; }';
            }
        };

        socket.onclose = function () {
            console.log('WebSocket closed, reconnecting in 2 seconds...');
            if (socket && socket.readyState === WebSocket.OPEN)
                return;
            setTimeout(connect, 2000);
        };

        socket.onerror = function (event) {
            console.error('WebSocket error observed:', event);
            socket.close();
        }
    }
    connect();
})();
