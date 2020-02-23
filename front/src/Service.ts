export class Service {
    private static instance: Service;
    private static cnt = 0;
    private ws: WebSocket | null = null;

    private constructor() {
    }

    static get Instance(): Service {
        if (this.instance == null) {
            this.instance = new Service();
        }
        return this.instance;
    }

    static get Count(): number {
        return ++this.cnt;
    }

    link(host: string, port: number, isSafe: boolean) {
        if (this.ws != null) {
            throw new Error('previous ws exists');
        }

        let protocol = isSafe ? 'wss' : 'ws';
        let address = `${protocol}://${host}:${port}/ws`;
        this.ws = new WebSocket(address);
        window.console.log(this.ws.protocol);
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
}