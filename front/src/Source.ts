import {Service} from "./Service";
import {Query, QueryResult} from "material-table";

export interface Row {
    name: string;
    path: string;
    link: string;
}

export class Source {
    private static instance: Source;
    private service: Service = Service.Instance;

    private modified: boolean = true;
    private cache: Row[] = [];

    private constructor() {
    }

    static get Instance(): Source {
        if (this.instance == null) {
            this.instance = new Source();
        }
        return this.instance;
    }

    private static fillEmpty(target: any, ...attr: string[]) {
        for (let name of attr) {
            if (!target.hasOwnProperty(name) || target[name] === null) {
                target[name] = "";
            }
        }
    }

    private static extractDiff(neo: any, old: any, ...attrName: string[]): object {
        let diff: any = {};
        for (let name of attrName) {
            if (neo[name] !== old[name]) {
                diff[name] = neo[name];
            }
        }
        return diff;
    }

    async create(neoRow: Row) {
        Source.fillEmpty(neoRow, 'name', 'path', 'link');
        let resp = await this.service.ajax('post', neoRow, 'api/resource');
        if (!resp.ok) {
            alert(`failed to post ${JSON.stringify(neoRow)}`);
        }
        this.modified = true;
    }

    async download(): Promise<Row[]> {
        window.console.log("download sources");
        let resp = await this.service.ajax('get', null, 'api/resource');
        if (!resp.ok) {
            alert(`failed to download sources`);
        }
        return await JSON.parse(await resp.text());
    }

    async retrieve(query: Query<Row>): Promise<QueryResult<Row>> {
        // a cache system would improve the performance,
        // which shall have the ability to get notified from front-end modification.
        window.console.log(query)

        window.console.log(this);
        window.console.log(this.modified);
        if (this.modified) {
            this.modified = false;
            this.cache = await this.download();
        }

        let result: Row[] = [...this.cache];

        // filter
        if (query.search.length > 0) {
            result = result.filter(row => row.name?.includes(query.search)
                || row.link?.includes(query.search)
                || row.path?.includes(query.search));
        }

        // sort
        if (query.orderBy) {
            // If query.orderBy!==undefined, then orderBy.field is a string represent attr.
            // @ts-ignore
            const key: string = query.orderBy.field;
            const isAscending: boolean = query.orderDirection === 'asc';

            result.sort(((a, b) => {
                // Row must have attribute with name key.
                // @ts-ignore
                let result = a[key].localeCompare(b[key]);
                if (!isAscending) {
                    result = -result;
                }
                return result;
            }));
        }

        // build
        const start = query.pageSize * query.page;
        return {data: result.slice(start, start + query.pageSize), page: query.page, totalCount: result.length};
    }

    private findIndex(oldRow: Row): number {
        return this.cache.indexOf(oldRow);
    }

    async update(neoRow: Row, oldRow?: Row) {
        // When? I don't know. The demo use it, therefore I use it.
        if (!oldRow) {
            return
        }

        const diff = Source.extractDiff(neoRow, oldRow, 'name', 'path', 'link');
        let resp = await this.service.ajax('put', diff, `api/resource/${this.findIndex(oldRow)}`);
        if (!resp.ok) {
            alert(`failed to delete ${JSON.stringify(oldRow)}`);
        }
        this.modified = true;
    }

    async remove(oldRow: Row) {
        let resp = await this.service.ajax('delete', null, `api/resource/${this.findIndex(oldRow)}`);
        if (!resp.ok) {
            alert(`failed to delete ${JSON.stringify(oldRow)}`);
        }
        this.modified = true;
    };
}