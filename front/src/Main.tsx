import React, {useReducer, useState} from "react";
import {Button, TextField} from "@material-ui/core";
import LoopIcon from '@material-ui/icons/Loop';
import ItemView, {Item, MessageType} from "./components/ItemView";


function Main() {
    const [host, setHost] = useState("hyisen.net");
    const [port, setPort] = useState(443);
    const [linked, setLinked] = useState(false);
    const [linking, setLinking] = useState(false);
    const [message, setMessage] = useState("");

    const [items, addItem] = useReducer((state: Item[], action: Item) => state.concat(action), []);


    function addMessage(cls: MessageType, msg: string): void {
        addItem({id: items.length, type: cls, message: msg, timestamp: Date.now()});
    }

    return (
        <div>
            <TextField
                id="host"
                label="host"
                value={host}
                onChange={(event: React.ChangeEvent<HTMLInputElement>) => setHost(event.target.value)}
            />
            <TextField
                id="port"
                label="port"
                type="number"
                value={port}
                onChange={(event: React.ChangeEvent<HTMLInputElement>) => setPort(Number(event.target.value))}
            />
            <Button onClick={() => {
                if (linking) {
                    return;
                }

                if (!linked) {
                    setLinking(true);
                    setTimeout(() => {
                        setLinked(true);
                        setLinking(false);
                    }, 1000);
                } else {
                    setLinking(true);
                    setTimeout(() => {
                        setLinked(false);
                        setLinking(false);
                    }, 1000);
                }
            }}
                    variant="contained"
                    color={linked ? "secondary" : "primary"}
            >{linking ? <LoopIcon/> : linked ? "OFF" : "ON"}</Button>
            <TextField
                id="message"
                label="message"
                value={message}
                variant="outlined"
                onChange={(event: React.ChangeEvent<HTMLInputElement>) => setMessage(event.target.value)}
            />
            <Button onClick={() => {
                window.console.log(`send ${message}`);
                addMessage(MessageType.Info, message);
                setMessage("");
            }}
                    variant="contained"
                    color="primary"
            >SEND</Button>
            <ItemView data={items}/>
        </div>
    );
}

export default Main;