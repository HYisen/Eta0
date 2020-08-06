import React from "react";
import {createStyles, Grid, Paper, Theme, Typography} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";

export enum MessageType {
    Server = 'server',
    Client = 'client',
    Info = 'info',
}

export interface Item {
    id: number,
    type: MessageType,
    message: string,
    timestamp: number;
}

function getClsColor(type: MessageType):
    | 'initial'
    | 'inherit'
    | 'primary'
    | 'secondary'
    | 'textPrimary'
    | 'textSecondary'
    | 'error' {
    switch (type) {
        case MessageType.Server:
            return "primary";
        case MessageType.Client:
            return "secondary";
        case MessageType.Info:
            return 'initial';
        default:
            return 'error';
    }
}

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        paper: {
            padding: theme.spacing(1),
            textAlign: 'center',
            color: theme.palette.text.secondary,
        }
    }),
);

function ItemView(props: { data: Item[] }) {
    const classes = useStyles({});

    return (
        <Grid
            container
            direction="column-reverse"
            justify="flex-start"
            alignItems="stretch"
            spacing={1}
        >
            {props.data.map(one =>
                <Grid item key={one.id}>
                    <Paper className={classes.paper}>
                        <Grid container
                              direction="column"
                              justify="flex-start"
                              alignItems="flex-start">
                            <Grid item container
                                  direction="row"
                                  justify="space-between"
                                  alignItems="center"
                            >
                                <Grid item>
                                    <Typography variant="subtitle1" component="h4" color={getClsColor(one.type)}>
                                        {one.type}
                                    </Typography>
                                </Grid>
                                <Grid item>
                                    <Typography variant="subtitle2" component="h6">
                                        {new Date(one.timestamp).toLocaleString()}
                                    </Typography>
                                </Grid>
                            </Grid>
                            <Grid item>
                                <Typography variant="body1" component="p">
                                    {one.message}
                                </Typography>
                            </Grid>
                        </Grid>
                    </Paper>
                </Grid>
            )}
        </Grid>
    );
}

export default ItemView;
