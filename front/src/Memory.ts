const ITEM_NAME = 'memory';

export default class Memory {
    private static instance: Memory | null = null;
    host: string;
    port: number;
    lastBookName: string;
    lastChapterId: any;

    constructor(host: string, port: number, lastBookName: string, lastChapterId: any) {
        this.host = host;
        this.port = port;
        this.lastBookName = lastBookName;
        this.lastChapterId = lastChapterId;
    }

    static get Instance() {
        if (this.instance === null) {
            let neo: Memory | null = this.load();
            if (neo === null) {
                window.console.log("gen neo Memory");
                neo = new Memory('localhost', 8964, '', {});
            }
            this.instance = neo;
        }
        return this.instance;
    }

    private static load() {
        let contentNullable: string | null = window.localStorage.getItem(ITEM_NAME);
        if (contentNullable !== null) {
            let data: Memory = JSON.parse(contentNullable);
            console.log(`reconstruct shiori from:`);
            console.log(data);
            return new Memory(data.host, data.port, data.lastBookName, data.lastChapterId);
        }
        return null;
    }

    save() {
        window.localStorage.setItem(ITEM_NAME, JSON.stringify(this));
    }
}