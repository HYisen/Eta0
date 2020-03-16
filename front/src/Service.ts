export class Service {
    private static instance: Service;
    private ws: WebSocket | null = null;

    private constructor() {
    }

    static get Instance(): Service {
        if (this.instance == null) {
            this.instance = new Service();
        }
        return this.instance;
    }

    link(host: string, port: number, isSafe?: boolean) {
        if (isSafe === undefined) {
            isSafe = port === 443;
        }

        if (this.ws != null) {
            throw new Error('previous ws exists');
        }

        let protocol = isSafe ? 'wss' : 'ws';
        let address = `${protocol}://${host}:${port}/ws`;
        this.ws = new WebSocket(address);
    }

    addEventListener<K extends keyof WebSocketEventMap>(type: K, listener: (this: WebSocket, ev: WebSocketEventMap[K]) => any, once?: boolean): void {
        this.ws?.addEventListener(type, listener, once);
    }

    removeEventListener<K extends keyof WebSocketEventMap>(type: K, listener: (this: WebSocket, ev: WebSocketEventMap[K]) => any): void {
        this.ws?.removeEventListener(type, listener);
    }

    close(code?: number, reason?: string): void {
        if (this.ws == null) {
            throw new Error('ws does not exist');
        }
        this.ws.close(code, reason);
        this.ws = null;
    }

    send(data: string | ArrayBufferLike | Blob | ArrayBufferView): void {
        if (this.ws == null) {
            throw new Error('ws does not exist');
        }
        this.ws.send(data);
    }

    get bad(): boolean {
        return this.ws == null || this.ws.readyState !== WebSocket.OPEN;
    }
}