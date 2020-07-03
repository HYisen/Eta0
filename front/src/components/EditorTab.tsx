import React, {forwardRef} from "react";
import MaterialTable, {Column, Icons} from "material-table";

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
import {Source} from "../Source";
import SaveIcon from '@material-ui/icons/Save';
import ReplayIcon from '@material-ui/icons/Replay';


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

export interface EditorTabProps {
    linkMethodNullable: (() => void) | null; // null if it is already linked therefore no need to linkMethodNullable.
}

export function EditorTab({linkMethodNullable}: EditorTabProps) {
    const source: Source = Source.Instance;
    if (linkMethodNullable) {
        linkMethodNullable();
    }

    return (
        <MaterialTable
            icons={tableIcons}
            title="Sources"
            columns={genColumns('name', 'path', 'link')}
            data={query => source.retrieve(query)}
            editable={{
                onRowAdd: newData => source.create(newData),
                onRowUpdate: (newData, oldData) => source.update(newData, oldData),
                onRowDelete: oldData => source.remove(oldData),
            }}
            actions={[
                {
                    icon: SaveIcon,
                    tooltip: 'Persist',
                    isFreeAction: true,
                    onClick: () => {
                        window.console.log("save start");

                        (async function () {
                            await source.save();
                        })();
                    }
                },
                {
                    icon: ReplayIcon,
                    tooltip: 'Reload',
                    isFreeAction: true,
                    onClick: () => {
                        window.console.log("load start");

                        (async function () {
                            await source.load();
                        })();
                    }
                }
            ]}
        />
    );

}