import RefreshIcon from "@material-ui/icons/Refresh";
import {createStyles, Fab, Theme, useTheme, Zoom} from "@material-ui/core";
import React from "react";
import {makeStyles} from "@material-ui/core/styles";
import {Stage} from "./ReaderTab";

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        fab: {
            margin: theme.spacing(1),
            top: theme.spacing(1),
            right: theme.spacing(1),
            bottom: 'auto',
            left: 'auto',
            position: 'fixed',
            zIndex: 65535,
        }
    })
);

export interface ControlFabProps {
    canShow: boolean;
    stage: Stage;
    linked: boolean;
    link: () => void;
    load: () => void
}

export default function ControlFab({canShow, stage, linked, link, load}: ControlFabProps) {
    const classes = useStyles({});
    const theme = useTheme();

    const transitionDuration = {
        enter: theme.transitions.duration.enteringScreen,
        exit: theme.transitions.duration.leavingScreen,
    };

    let fab = linked ? (
        <Fab color="primary" aria-label="ReLoad"
             className={classes.fab} onClick={load}>
            <RefreshIcon/>
        </Fab>
    ) : (
        <Fab color="secondary" aria-label="ReLink"
             className={classes.fab} onClick={link}>
            <RefreshIcon/>
        </Fab>
    );

    return (
        <Zoom
            in={canShow && stage !== Stage.Chapter}
            timeout={transitionDuration}
        >
            {fab}
        </Zoom>
    );
};