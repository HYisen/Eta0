import React, {MutableRefObject, useState} from "react";
import {Messenger} from "../nexus";
import {Card, CardActionArea, CardContent, Grid} from "@material-ui/core";
import {unstable_batchedUpdates} from "react-dom";


enum Stage {
    Shelf,
    Book,
    Chapter
}

export interface Book {
    name: string;
    chapters: Chapter[] | null;
}

interface Chapter {
    title: string;
    content: string[] | null;
}

export interface ReaderTabProps {
    messenger: Messenger;
    data: MutableRefObject<Book[] | null>;
}

export default function ReaderTab({messenger, data}: ReaderTabProps) {
    const [loadingMessage, setLoadingMessage] = useState('');
    const [stage, setStage] = useState(Stage.Shelf);
    const [bookId, setBookId] = useState(-1);
    const [chapterId, setChapterId] = useState(-1);

    window.console.log("render REad");

    if (loadingMessage !== '') {
        return <p>{loadingMessage}</p>;
    }

    let cards: JSX.Element[] = [];
    let cnt: number = 0;
    switch (stage) {
        case Stage.Shelf:
            if (data.current === null) {
                setLoadingMessage(`loading shelf`);
                messenger.getShelf().then(content => {
                    data.current = content.map(name => ({name: name, chapters: null}));
                    setLoadingMessage('');
                });
            } else {
                let current: Book[] = data.current;
                cards.push(...current.map((book, id) => {
                    return <Grid item key={++cnt}>
                        <Card>
                            <CardActionArea onClick={
                                () => {
                                    console.log(`click ${id}`);
                                    unstable_batchedUpdates(() => {
                                        setBookId(id);
                                        setStage(Stage.Book);
                                    })
                                }
                            }>
                                <CardContent style={{textAlign: "center"}}>
                                    {`《${book.name}》`}
                                </CardContent>
                            </CardActionArea>
                        </Card>
                    </Grid>
                }));
            }
            break;
        case Stage.Book:
            // @ts-ignore
            let book: Book = data.current[bookId];

            if (book.chapters === null) {
                setLoadingMessage(`loading book ${book.name}`);
                messenger.getBook(bookId).then(content => {
                    book.chapters = content.map(title => ({title: title, content: null}));
                    setLoadingMessage('');
                });
            } else {
                cards.push(
                    <Grid item key={++cnt}>
                        <Card>
                            <CardActionArea onClick={
                                () => {
                                    unstable_batchedUpdates(() => {
                                        setStage(Stage.Shelf);
                                    })
                                }
                            }>
                                <CardContent style={{textAlign: "center"}}>
                                    {`Back to Shelf`}
                                </CardContent>
                            </CardActionArea>
                        </Card>
                    </Grid>);
                cards.push(...book.chapters.map((chapter, id) => {
                    return <Grid item key={++cnt}>
                        <Card>
                            <CardActionArea onClick={
                                () => {
                                    console.log(`click ${id}`);
                                    unstable_batchedUpdates(() => {
                                        setChapterId(id);
                                        setStage(Stage.Chapter);
                                    })
                                }
                            }>
                                <CardContent style={{textAlign: "center"}}>
                                    {`${chapter.title}`}
                                </CardContent>
                            </CardActionArea>
                        </Card>
                    </Grid>
                }));
            }
            break;
        case Stage.Chapter:
            // @ts-ignore
            let chapter: Chapter = data.current[bookId].chapters[chapterId];
            if (chapter.content === null) {
                setLoadingMessage(`loading chapter ${chapter.title}`);
                messenger.getChapter(bookId, chapterId).then(content => {
                    unstable_batchedUpdates(() => {
                        chapter.content = content;
                        setLoadingMessage('');
                    })
                });
            } else {
                // @ts-ignore
                const bookName = data.current[bookId].name;
                cards.push(
                    <Grid item key={++cnt}>
                        <Card>
                            <CardActionArea onClick={
                                () => {
                                    unstable_batchedUpdates(() => {
                                        setStage(Stage.Book);
                                    })
                                }
                            }>
                                <CardContent style={{textAlign: "center"}}>
                                    {`Back to Book《${bookName}》`}
                                </CardContent>
                            </CardActionArea>
                        </Card>
                    </Grid>);
                cards.push(
                    <Grid item key={++cnt}>
                        <Card>
                            <CardContent style={{textIndent: "2em"}}>
                                {chapter.content?.map(line => {
                                    return <p key={++cnt}>{line}</p>;
                                })}
                            </CardContent>
                        </Card>
                    </Grid>);
            }
            break;
        default:
            throw new Error(`unknown stage ${stage}`);
    }


    return <Grid
        container
        direction="column"
        justify="flex-start"
        alignItems="stretch"
        spacing={2}
    >
        {/*{this.bookmark.bookName !== undefined &&*/}
        {/*<Grid item key={++cnt}>*/}
        {/*    <Card className={classes.card}>*/}
        {/*        <CardActionArea onClick={() => {*/}
        {/*            this.setState({*/}
        {/*                item: `${this.bookmark.bookIndex}.${this.bookmark.chapterIndex}`,*/}
        {/*                loaded: false*/}
        {/*            });*/}
        {/*        }}>*/}
        {/*            <CardContent>*/}
        {/*                {`last read ->《${this.bookmark.bookName}》\t${this.bookmark.chapterName}`}*/}
        {/*            </CardContent>*/}
        {/*        </CardActionArea>*/}
        {/*    </Card>*/}
        {/*</Grid>*/}
        {/*}*/}
        {cards}
    </Grid>;
}