import React, {
    Dispatch,
    MutableRefObject,
    SetStateAction,
    useEffect,
    useMemo,
    useReducer,
    useRef,
    useState
} from "react";
import {
    AppBar,
    Button,
    createStyles,
    FormControlLabel,
    Grid,
    IconButton,
    InputLabel,
    MenuItem,
    Select,
    Snackbar,
    Switch,
    Tab,
    Tabs,
    TextField,
    Theme,
    useTheme
} from "@material-ui/core";
import LoopIcon from '@material-ui/icons/Loop';
import CloseIcon from '@material-ui/icons/Close';
import ItemView, {Item, MessageType} from "./components/ItemView";
import {Service} from "./Service";
import {unstable_batchedUpdates} from "react-dom";
import {makeStyles} from "@material-ui/core/styles";
import {genTabProps, TabClazz, TabPanel} from "./components/TabPanel";
import SwipeableViews from 'react-swipeable-views';
import {HttpMessenger, WebSocketMessenger} from "./nexus";
import ReaderTab, {Book, Stage} from "./components/ReaderTab";
import ControlFab from "./components/ControlFab";
import Memory from "./Memory";
import {EditorTab} from "./components/EditorTab";

const useStyles = makeStyles((theme: Theme) =>
    createStyles({
        button: {
            width: 50,
            marginTop: 10,
        },
        root: {
            padding: theme.spacing(0),
        },
    })
);

export enum Mode {
    HTTP,
    WebSocket,
}

export const genAddr = (host: string, port: number): string => `${port === 443 ? 'https' : 'http'}://${host}:${port}`;

function Main() {
    const classes = useStyles({});
    const theme = useTheme();

    const memory: Memory = Memory.Instance;

    const [host, setHost] = useState(memory.host);
    const [port, setPort] = useState(memory.port);
    const [mode, setMode] = useState(memory.mode);
    const [dedicate, setDedicate] = useState(memory.dedicate);
    const [reverse, setReverse] = useState(memory.reverse);
    const [prefetch, setPrefetch] = useState(memory.prefetch);

    const [linked, setLinked] = useState(false);
    const [linking, setLinking] = useState(false);
    const [message, setMessage] = useState("");

    const [username, setUsername] = useState(memory.username);
    const [password, setPassword] = useState(memory.password);
    const [open, setOpen] = useState(false);
    const [info, setInfo] = useState('fuck');
    const [countdown, setCountDown] = useState(0);

    const [items, addItem] = useReducer((state: Item[], action: Item) => state.concat(action), []);

    const [value, setValue] = React.useState(TabClazz.Config);

    const [stage, setStage] = useState(Stage.Shelf);
    const [bookId, setBookId] = useState(-1);
    const [chapterId, setChapterId] = useState(-1);

    const messenger = useMemo(() => {
        switch (mode) {
            case Mode.HTTP:
                return new HttpMessenger(genAddr(host, port));
            case Mode.WebSocket:
                return new WebSocketMessenger(Service.Instance);
        }
    }, [host, port, mode]);
    const data: MutableRefObject<Book[] | null> = useRef(null);


    const update = (neoStage: Stage, neoBookId: number, neoChapterId: number) => {
        unstable_batchedUpdates(() => {
            setStage(neoStage);
            setBookId(neoBookId);
            setChapterId(neoChapterId);
        });
        if (neoBookId === bookId && neoChapterId !== chapterId) {
            // @ts-ignore
            memory.lastBookName = data.current[bookId].name;
            memory.lastChapterId[memory.lastBookName] = neoChapterId;
            memory.save();
        }
    };

    const link = () => {
        if (linking) {
            return;
        }

        if (!linked) {
            setLinking(true);

            service.username = username;
            service.password = password;
            service.addr = genAddr(host, port);

            service.link(host, port);
            service.addEventListener('open', () => {
                unstable_batchedUpdates(() => {
                    // Hope it would be automatically in future.
                    setLinked(true);
                    setLinking(false);
                });

                memory.host = host;
                memory.port = port;
                memory.save();
            }, true);
            service.addEventListener('close', () => {
                unstable_batchedUpdates(() => {
                    setLinked(false);
                    setLinking(false);
                });
            }, true);
        } else {
            setLinking(true);
            service.close();
        }
    };

    const load = () => {
        service.send('reload');
        service.addEventListener("message", (ev) => {
            window.console.log(`reload request reply = ${ev.data}`);
            data.current = null;
        }, true);
    };

    function addMessage(cls: MessageType, msg: string): void {
        addItem({id: items.length, type: cls, message: msg, timestamp: Date.now()});
    }

    // I don't use a Component because extra incarnation to solve change from uncontrolled problem is ugly.
    const genConfigSwitchGridItem = (propertyName: string, value: boolean, setValue: Dispatch<SetStateAction<boolean>>,
                                     width: number) => (
        <Grid item>
            <FormControlLabel
                control={
                    <Switch checked={value} onChange={(() => {
                        // @ts-ignore
                        memory[propertyName] = !value;
                        memory.save();
                        setValue(!value);
                    })} value={propertyName}/>
                }
                label={propertyName}
                style={{width: width, marginTop: 10}}
            />
        </Grid>
    );

    useEffect(() => {
        let shouldMonitor: boolean = value === TabClazz.Config || !dedicate;
        if (shouldMonitor) {
            service.addEventListener('message', onMessageAction);
        }
        service.addEventListener('error', onErrorAction);
        return () => {
            if (shouldMonitor) {
                service.removeEventListener('message', onMessageAction);
            }
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
            <AppBar position="static">
                <Tabs
                    value={value}
                    onChange={(event, value) => setValue(value)}
                    variant="fullWidth"
                    aria-label="full width tabs"
                >
                    <Tab {...genTabProps(TabClazz.Config)} />
                    <Tab {...genTabProps(TabClazz.Reader)} />
                    <Tab {...genTabProps(TabClazz.Editor)} />
                </Tabs>
            </AppBar>
            <ControlFab canShow={value === TabClazz.Reader} stage={stage} linked={linked} link={link} load={load}/>
            <SwipeableViews
                axis={theme.direction === 'rtl' ? 'x-reverse' : 'x'}
                index={value}
                onChangeIndex={(index) => setValue(index)}
            >
                <TabPanel value={value} index={TabClazz.Config} dir={theme.direction}>
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
                                style={{width: 80}}
                            />
                        </Grid>
                        <Grid item>
                            <Button className={classes.button}
                                    onClick={link}
                                    variant="contained"
                                    color={linked ? "primary" : "secondary"}
                            >{linking ? <LoopIcon/> : linked ? "ON" : "OFF"}</Button>
                        </Grid>

                        <Grid item>
                            <InputLabel id="mode-select-label">Reader Mode</InputLabel>
                            <Select
                                labelId="mode-select-label"
                                id="mode-select"
                                value={mode}
                                onChange={(event => {
                                    // @ts-ignore
                                    let neoMode: Mode = event.target.value;

                                    memory.mode = neoMode;
                                    memory.save();

                                    setMode(neoMode);
                                })}
                                style={{width: 120}}
                            >
                                <MenuItem value={Mode.HTTP}>HTTP</MenuItem>
                                <MenuItem value={Mode.WebSocket}>WebSocket</MenuItem>
                            </Select>
                        </Grid>
                        {genConfigSwitchGridItem('dedicate', dedicate, setDedicate, 100)}
                        {genConfigSwitchGridItem('reverse', reverse, setReverse, 100)}
                        {genConfigSwitchGridItem('prefetch', prefetch, setPrefetch, 100)}
                        <Grid item>
                            <Button className={classes.button}
                                    style={{width: 160}}
                                    onClick={() => memory.clear()}
                                    variant="contained"
                                    color="default"
                            >ERASE MEMORY</Button>
                        </Grid>

                        <Grid item>
                            <TextField
                                autoComplete="off"
                                id="message"
                                label="message"
                                value={message}
                                variant="outlined"
                                onChange={(event: React.ChangeEvent<HTMLInputElement>) => setMessage(event.target.value)}
                                style={{width: 312}}
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

                        <Grid item>
                            <TextField
                                id="username"
                                label="username"
                                value={username}
                                onChange={(event: React.ChangeEvent<HTMLInputElement>) => setUsername(event.target.value)}
                                style={{width: 200}}
                            />
                        </Grid>
                        <Grid item>
                            <TextField
                                id="password"
                                label="password"
                                value={password}
                                onChange={(event: React.ChangeEvent<HTMLInputElement>) => setPassword(event.target.value)}
                                style={{width: 200}}
                            />
                        </Grid>
                        <Grid item>
                            <Button className={classes.button}
                                    onClick={() => {
                                        (async function () {
                                            let resp = await service.ajax('put', {
                                                username: username,
                                                password: password
                                            }, 'api/auth');

                                            if (resp.ok) {
                                                window.console.log("succeed to auth");
                                                setInfo("succeed to auth");

                                                const duration = 5;
                                                for (let delay = 0; delay < duration; ++delay) {
                                                    setTimeout(() => {
                                                        setCountDown(duration - delay);
                                                    }, 1000 * delay);
                                                }

                                                setOpen(true);

                                                memory.username = username;
                                                memory.password = password;
                                                memory.save();
                                            }
                                        })();
                                    }}
                                    variant="contained"
                                    color="default"
                            >AUTH</Button>
                        </Grid>
                        <Snackbar
                            anchorOrigin={{
                                vertical: 'bottom',
                                horizontal: 'left',
                            }}
                            open={open}
                            autoHideDuration={5000}
                            onClose={(event: React.SyntheticEvent<any>, reason: string) => {
                                if (reason === 'clickaway') {
                                    return;
                                }
                                setOpen(false);
                            }}
                            message={`${info} (${countdown}s left)`}
                            action={
                                <React.Fragment>
                                    <IconButton size="small" aria-label="close" color="inherit" onClick={() => {
                                        setOpen(false);
                                    }}>
                                        <CloseIcon fontSize="small"/>
                                    </IconButton>
                                </React.Fragment>
                            }
                        />

                        <Grid item xs={12}>
                            <ItemView data={items}/>
                        </Grid>
                    </Grid>
                </TabPanel>
                <TabPanel value={value} index={TabClazz.Reader} dir={theme.direction}>
                    <ReaderTab messenger={messenger} data={data}
                               stage={stage} bookId={bookId} chapterId={chapterId} update={update} prefetch={prefetch}/>
                </TabPanel>
                <TabPanel value={value} index={TabClazz.Editor} dir={theme.direction}>
                    <EditorTab link={linked ? null : link}/>
                </TabPanel>
            </SwipeableViews>
        </div>
    );
}

export default Main;