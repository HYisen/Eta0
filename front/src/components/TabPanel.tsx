import React from "react";
import {Box, Typography} from "@material-ui/core";

export enum TabClazz {
    Config,
    Reader
}

function toLabel(clz: TabClazz) {
    switch (clz) {
        case TabClazz.Config:
            return "config";
        case TabClazz.Reader:
            return "reader";
    }
}

export interface TabPanelProps {
    children?: React.ReactNode;
    dir?: string;
    index: TabClazz;
    value: TabClazz;
}

export function TabPanel(props: TabPanelProps) {
    const {children, value, index, ...other} = props;

    return (
        <Typography
            component="div"
            role="tabpanel"
            hidden={value !== index}
            id={`full-width-tabpanel-${index}`}
            aria-labelledby={`full-width-tab-${index}`}
            {...other}
        >
            {value === index && <Box p={2}>{children}</Box>}
        </Typography>
    );
}

export function genTabProps(index: TabClazz) {
    return {
        label: toLabel(index),
        id: `full-width-tab-${index}`,
        'aria-controls': `full-width-tabpanel-${index}`,
    };
}