import React, {useEffect, useReducer, useState} from "react";
import {Button, createStyles, Grid, TextField, Theme} from "@material-ui/core";
import LoopIcon from '@material-ui/icons/Loop';
import ItemView, {Item, MessageType} from "./components/ItemView";
import {Service} from "./Service";
import {unstable_batchedUpdates} from "react-dom";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        button: {
            width: 50,
            marginTop: 10,
        },
        root: {
            padding: theme.spacing(1)
        }
    }),
);

function Main() {
    const classes = useStyles({});

    const [host, setHost] = useState("hyisen.net");
    const [port, setPort] = useState(443);
    const [linked, setLinked] = useState(false);
    const [linking, setLinking] = useState(false);
    const [message, setMessage] = useState("");

    const [items, addItem] = useReducer((state: Item[], action: Item) => state.concat(action), []);

    function addMessage(cls: MessageType, msg: string): void {
        addItem({id: items.length, type: cls, message: msg, timestamp: Date.now()});
    }

    useEffect(() => {
        service.addEventListener('message', onMessageAction);
        service.addEventListener('error', onErrorAction);
        return () => {
            service.removeEventListener('message', onMessageAction);
            service.removeEventListener('error', onErrorAction);
        };
    });

    const onMessageAction = (event: MessageEvent) => {
        // addMessage(MessageType.Server, event.data.toString());
        addItem({id: items.length, type: MessageType.Server, message: event.data.toString(), timestamp: Date.now()});
    };

    const onErrorAction = (event: Event) => {
        addMessage(MessageType.Info, `error = ${event}`);
    };

    const service = Service.Instance;

    return (
        <div>
            <Grid container className={classes.root} spacing={4}>
                <Grid item>
                    <TextField
                        id="host"
                        label="host"
                        value={host}
                        onChange={(event: React.ChangeEvent<HTMLInputElement>) => setHost(event.target.value)}
                        style={{width: 200}}
                    />
                </Grid>
                <Grid item>
                    <TextField
                        id="port"
                        label="port"
                        type="number"
                        value={port}
                        onChange={(event: React.ChangeEvent<HTMLInputElement>) => setPort(Number(event.target.value))}
                        style={{width: 78}}
                    />
                </Grid>
                <Grid item>
                    <Button className={classes.button}
                            onClick={() => {
                                if (linking) {
                                    return;
                                }

                                if (!linked) {
                                    setLinking(true);

                                    service.link(host, port, true);
                                    service.addEventListener('open', () => {
                                        unstable_batchedUpdates(() => {
                                            // Hope it would be automatically in future.
                                            setLinked(true);
                                            setLinking(false);
                                        });
                                    }, true);
                                } else {
                                    setLinking(true);
                                    service.addEventListener('close', () => {
                                        unstable_batchedUpdates(() => {
                                            setLinked(false);
                                            setLinking(false);
                                        });
                                    }, true);
                                }
                            }}
                            variant="contained"
                            color={linked ? "primary" : "secondary"}
                    >{linking ? <LoopIcon/> : linked ? "ON" : "OFF"}</Button>
                </Grid>

                <Grid item>
                    <TextField
                        autoComplete="off"
                        id="message"
                        label="message"
                        value={message}
                        variant="outlined"
                        onChange={(event: React.ChangeEvent<HTMLInputElement>) => setMessage(event.target.value)}
                        style={{width: 295}}
                    />
                </Grid>
                <Grid item>
                    <Button className={classes.button}
                            onClick={() => {
                                addMessage(MessageType.Client, message);
                                service.send(message);
                                setMessage("");
                            }}
                            variant="contained"
                            color="primary"
                    >SEND</Button>
                </Grid>
                <Grid item xs={12}>
                    <ItemView data={items}/>
                </Grid>
            </Grid>
        </div>
    );
}

export default Main;