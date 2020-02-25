import {Service} from "./Service";

// extraction of how to acquire the data
export interface Messenger {
    getShelf: () => Promise<string[]>
    getBook: (bookId: number) => Promise<string[]>
    getChapter: (bookId: number, chapterId: number) => Promise<string>
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

    if (response.headers.get('Content-Type') === 'application/json') {
        return await response.json();
    } else {
        return {};
    }
};

const joinChapter = (content: string[]) => {
    return content.join('\n');
};

export class HttpMessenger implements Messenger {
    private readonly addr: string;

    constructor(addr: string) {
        this.addr = addr;
    }

    get = async (path: string) => {
        let envelop: Envelop = await ajax('GET', null, `${this.addr}/${path}`);
        return envelop.content;
    };

    getBook = async (bookId: number) => {
        return this.get(`${bookId}`);
    };
    getChapter = async (bookId: number, chapterId: number) => {
        return joinChapter(await this.get(`${bookId}/${chapterId}`));
    };
    getShelf = async () => {
        return this.get(``);
    };
}

export class WebSocketMessenger implements Messenger {
    private readonly service: Service;

    constructor(service: Service) {
        this.service = service;
    }

    round = async (command: string) => {
        return new Promise<String>(resolve => {
            this.service.addEventListener('message', ev => {
                resolve(ev.data);
            }, true);
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
        return joinChapter(await this.access(`get ${bookId}.${chapterId}`));
    };
    getShelf = async () => {
        return this.access(`ls .`);
    };
}