import React, {forwardRef} from "react";
import MaterialTable, {Column, Icons, Query, QueryResult} from "material-table";

import AddBox from '@material-ui/icons/AddBox';
import ArrowDownward from '@material-ui/icons/ArrowDownward';
import Check from '@material-ui/icons/Check';
import ChevronLeft from '@material-ui/icons/ChevronLeft';
import ChevronRight from '@material-ui/icons/ChevronRight';
import Clear from '@material-ui/icons/Clear';
import DeleteOutline from '@material-ui/icons/DeleteOutline';
import Edit from '@material-ui/icons/Edit';
import FilterList from '@material-ui/icons/FilterList';
import FirstPage from '@material-ui/icons/FirstPage';
import LastPage from '@material-ui/icons/LastPage';
import Remove from '@material-ui/icons/Remove';
import SaveAlt from '@material-ui/icons/SaveAlt';
import Search from '@material-ui/icons/Search';
import ViewColumn from '@material-ui/icons/ViewColumn';
import {Service} from "../Service";

const tableIcons: Icons = {
    Add: forwardRef((props, ref) => <AddBox {...props} ref={ref}/>),
    Check: forwardRef((props, ref) => <Check {...props} ref={ref}/>),
    Clear: forwardRef((props, ref) => <Clear {...props} ref={ref}/>),
    Delete: forwardRef((props, ref) => <DeleteOutline {...props} ref={ref}/>),
    DetailPanel: forwardRef((props, ref) => <ChevronRight {...props} ref={ref}/>),
    Edit: forwardRef((props, ref) => <Edit {...props} ref={ref}/>),
    Export: forwardRef((props, ref) => <SaveAlt {...props} ref={ref}/>),
    Filter: forwardRef((props, ref) => <FilterList {...props} ref={ref}/>),
    FirstPage: forwardRef((props, ref) => <FirstPage {...props} ref={ref}/>),
    LastPage: forwardRef((props, ref) => <LastPage {...props} ref={ref}/>),
    NextPage: forwardRef((props, ref) => <ChevronRight {...props} ref={ref}/>),
    PreviousPage: forwardRef((props, ref) => <ChevronLeft {...props} ref={ref}/>),
    ResetSearch: forwardRef((props, ref) => <Clear {...props} ref={ref}/>),
    Search: forwardRef((props, ref) => <Search {...props} ref={ref}/>),
    SortArrow: forwardRef((props, ref) => <ArrowDownward {...props} ref={ref}/>),
    ThirdStateCheck: forwardRef((props, ref) => <Remove {...props} ref={ref}/>),
    ViewColumn: forwardRef((props, ref) => <ViewColumn {...props} ref={ref}/>)
};

function genColumns(...name: string[]): Column<any>[] {
    return name.map(v => {
        return {title: v, field: v};
    });
}

function sleep(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms));
}

interface Row {
    name: string;
    path: string;
    link: string;
}

interface TableState {
    columns: Array<Column<Row>>;
    data: Row[];
}

export function EditorTab() {
    const service = Service.Instance;

    const [state, setState] = React.useState<TableState>({
        columns: genColumns('name', 'path', 'link'),
        data: [
            {
                name: "bookName",
                path: "bookPath",
                link: "bookLink"
            },
        ],
    });

    const create = async (neoRow: Row) => {
        let resp = await service.ajax('post', neoRow, 'api/resource');
        if (!resp.ok) {
            alert(`failed to post ${JSON.stringify(neoRow)}`);
        }
    };

    const retrieve = async (query: Query<Row>): Promise<QueryResult<Row>> => {
        // a cache system would improve the performance,
        // which shall have the ability to get notified from front-end modification.
        window.console.log(query)
        let response = await service.ajax('get', null, 'api/resource');
        const data: Row[] = await JSON.parse(await response.text());
        const start = query.pageSize * query.page;
        return {data: data.slice(start, start + query.pageSize), page: query.page, totalCount: data.length};
    };

    function extractIndex(oldRow: Row) {
        // The ts of onRowUpdate is not specific enough.
        // Practice through `JSON.stringify(oldRow)` confirm that there is a index value in it.
        // [The doc from material-ui](https://material-ui.com/api/table/) use `data[data.indexOf(oldData)]`,
        // while [the doc from material-table](https://material-table.com/#/docs/features/editable) use current one.
        // It's a hack to the type system.
        // @ts-ignore
        return oldRow.tableData.id;
    }

    const update = async (neoRow: Row, oldRow?: Row) => {
        // When? I don't know. The demo use it, therefore I use it.
        if (!oldRow) {
            return
        }

        await sleep(500);
        setState(prevState => {
            const data = [...prevState.data];

            data[extractIndex(oldRow)] = neoRow;
            return {...prevState, data};
        });
    };

    const remove = async (oldRow: Row) => {
        // There has the information.
        // @ts-ignore
        const index = oldRow.tableData.id;
        let resp = await service.ajax('delete', null, `api/resource/${index}`);
        if (!resp.ok) {
            alert(`failed to delete ${JSON.stringify(oldRow)}`);
        }
    };

    return (
        <MaterialTable
            icons={tableIcons}
            title="Editable Example"
            columns={genColumns('name', 'path', 'link')}
            data={retrieve}
            editable={{
                onRowAdd: create,
                onRowUpdate: update,
                onRowDelete: remove,
            }}
        />
    );

}