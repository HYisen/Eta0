import Memory from "./Memory";
import {genAddr} from "./Main";

export class Service {
    private static instance: Service;
    private ws: WebSocket | null = null;
    private readonly memory: Memory = Memory.Instance;
    username: string = this.memory.username;
    password: string = this.memory.password;
    addr: string = genAddr(this.memory.host, this.memory.port);
    private token: string | null = null;

    private constructor() {
    }

    static get Instance(): Service {
        if (this.instance == null) {
            this.instance = new Service();
        }
        return this.instance;
    }

    async ajax(method: string, payloadNullable: any | null, path: string, canRefreshToken = true): Promise<Response> {
        if (this.token === null) {
            this.token = await this.fetchToken();
        }

        let init: RequestInit = {
            method: method,
            mode: 'cors',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            }
        };
        if (this.token !== null) {
            // @ts-ignore
            init.headers['credential'] = this.token;
        }
        if (payloadNullable !== null) {
            init.body = JSON.stringify(payloadNullable);
        }

        window.console.log(init);
        const response = await fetch(`${this.addr}/${path}`, init);

        // Token may be invalid as time goes by, try to refresh once if it's forbidden.
        if (response.status === 403 && canRefreshToken) {
            this.token = await this.fetchToken();
            return this.ajax(method, payloadNullable, path, false);
        } else {
            return response;
        }
    }

    async fetchToken() {
        if (this.username === "" || this.password === "") {
            return null;
        }

        const url = `${this.addr}/api/auth`;
        const json = JSON.stringify({username: this.username, password: this.password});
        let init: RequestInit = {
            method: 'post',
            mode: 'cors',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json',
            },
            body: json
        };
        let response = await fetch(url, init);
        if (!response.ok) {
            window.console.log("failed to get token by " + json);
            return null;
        }
        return await response.text();
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