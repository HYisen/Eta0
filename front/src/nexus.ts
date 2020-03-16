import {Service} from "./Service";

// extraction of how to acquire the data
export interface Messenger {
    getShelf: () => Promise<string[]>
    getBook: (bookId: number) => Promise<string[]>
    getChapter: (bookId: number, chapterId: number) => Promise<string[]>
    isBad: () => boolean
}

interface Envelop {
    type: "SHELF" | "BOOK" | "CHAPTER";
    name?: string;
    content: string[];
}

const ajax = async (method: string, payloadNullable: any | null, url: string) => {
    let init: RequestInit = {
        method: method,
        mode: 'cors',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        }
    };
    if (payloadNullable !== null) {
        init.body = JSON.stringify(payloadNullable);
    }
    let response = await fetch(url, init);

    return response.json();
};

export class HttpMessenger implements Messenger {
    private readonly addr: string;

    constructor(addr: string) {
        this.addr = addr;
    }

    private get = async (path: string) => {
        let envelop: Envelop = await ajax('GET', null, `${this.addr}/db/${path}`);
        return envelop.content;
    };

    getBook = async (bookId: number) => {
        return this.get(`${bookId}`);
    };
    getChapter = async (bookId: number, chapterId: number) => {
        return this.get(`${bookId}/${chapterId}`);
    };
    getShelf = async () => {
        return this.get(``);
    };
    isBad = () => false;
}

export class WebSocketMessenger implements Messenger {
    private readonly service: Service;

    constructor(service: Service) {
        this.service = service;
    }

    round = async (command: string) => {
        return new Promise<string>(resolve => {
            const listener = (ev: MessageEvent) => {
                const msg: string = ev.data;
                if (msg.startsWith("Welcome.")) {
                    window.console.log("skip potential welcome message:\n" + msg);
                } else {
                    resolve(msg);
                }
            };
            this.service.addEventListener('message', listener, true);
            this.service.send(command);
        });
    };

    access = async (command: string) => {
        const data: String = await this.round(command);
        const envelop: Envelop = JSON.parse(data.toString());
        return envelop.content;
    };

    getBook = async (bookId: number) => {
        return this.access(`ls ${bookId}`);
    };
    getChapter = async (bookId: number, chapterId: number) => {
        return this.access(`get ${bookId}.${chapterId}`);
    };
    getShelf = async () => {
        return this.access(`ls .`);
    };
    isBad = () => this.service.bad;
}