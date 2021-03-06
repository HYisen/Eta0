import React, {MutableRefObject, useState} from "react";
import {Messenger} from "../nexus";
import {Card, CardActionArea, CardContent, Grid} from "@material-ui/core";
import {unstable_batchedUpdates} from "react-dom";
import Memory from "../Memory";

export enum Stage {
    Shelf,
    Book,
    Chapter,
    Download,
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
    stage: Stage;
    bookId: number;
    chapterId: number;
    update: (stage: Stage, bookId: number, chapterId: number) => void;
    prefetch: boolean;
}

export default function ReaderTab({messenger, data, stage, bookId, chapterId, update, prefetch}: ReaderTabProps) {
    const [loadingMessage, setLoadingMessage] = useState('');
    const [downloadStatus, setDownloadStatus] = useState('');

    const memory = Memory.Instance;

    window.console.log("render reader");

    if (messenger.isBad()) {
        return <p>Service is Bad</p>
    }

    if (loadingMessage !== '') {
        return <p>{loadingMessage}</p>;
    }

    let cards: JSX.Element[] = [];
    let cnt: number = 0;

    function pushBackToShelfCard() {
        cards.push(
            <Grid item key={++cnt}>
                <Card>
                    <CardActionArea onClick={() => update(Stage.Shelf, bookId, chapterId)}>
                        <CardContent style={{textAlign: "center"}}>
                            {`Back to Shelf`}
                        </CardContent>
                    </CardActionArea>
                </Card>
            </Grid>
        );
    }

    switch (stage) {
        case Stage.Shelf:
            if (data.current === null) {
                setLoadingMessage(`loading shelf`);
                messenger.getShelf().then(content => {
                    data.current = content.map(name => ({name: name, chapters: null}));
                    setLoadingMessage('');
                });
            } else {
                const current: Book[] = data.current;
                cards.push(...current.map((book, id) => {
                    return <Grid item key={++cnt}>
                        <Card>
                            <CardActionArea onClick={
                                () => {
                                    console.log(`click ${id}`);
                                    update(Stage.Book, id, chapterId);
                                }
                            }>
                                <CardContent style={{textAlign: "center"}}>
                                    {`《${book.name}》${book.name === memory.lastBookName ? '*' : ' '}`}
                                </CardContent>
                            </CardActionArea>
                        </Card>
                    </Grid>
                }));
            }
            break;
        case Stage.Book:
            let shelf = data.current;
            if (shelf === null) {
                setLoadingMessage(`reloading book and shelf`);
                (async function () {
                    const books: Book[] = (await messenger.getShelf()).map(name => ({name: name, chapters: null}));
                    books[bookId].chapters = (await messenger.getBook(bookId)).map(title => ({
                        title: title,
                        content: null
                    }));
                    data.current = books;
                    setLoadingMessage('');
                })();
                break;
            }
            let book: Book = shelf[bookId];

            if (book.chapters === null) {
                setLoadingMessage(`loading book ${book.name}`);
                messenger.getBook(bookId).then(content => {
                    book.chapters = content.map(title => ({title: title, content: null}));
                    setLoadingMessage('');
                });
            } else {
                pushBackToShelfCard();
                if (memory.lastChapterId.hasOwnProperty(book.name)) {
                    const oldChapterId: number = memory.lastChapterId[book.name];
                    if (oldChapterId < book.chapters.length) {
                        cards.push(
                            <Grid item key={++cnt}>
                                <Card>
                                    <CardActionArea onClick={() => update(Stage.Chapter, bookId, oldChapterId)}>
                                        <CardContent>
                                            {`last read -> ${book.chapters[oldChapterId].title}`}
                                        </CardContent>
                                    </CardActionArea>
                                </Card>
                            </Grid>
                        );
                    }
                }
                let chapterCards = book.chapters.map((chapter, id) => {
                    return (
                        <Grid item key={++cnt}>
                            <Card>
                                <CardActionArea onClick={
                                    () => {
                                        console.log(`click ${id}`);
                                        update(Stage.Chapter, bookId, id);
                                    }
                                }>
                                    <CardContent style={{textAlign: "center"}}>
                                        {`${chapter.title}`}
                                    </CardContent>
                                </CardActionArea>
                            </Card>
                        </Grid>
                    );
                });
                if (memory.reverse) {
                    chapterCards.reverse();
                }
                cards.push(...chapterCards);

                cards.push(
                    <Grid item key={++cnt}>
                        <Card>
                            <CardActionArea onClick={
                                () => {
                                    update(Stage.Download, bookId, chapterId);
                                }
                            }>
                                <CardContent style={{textAlign: "center"}}>
                                    Download
                                </CardContent>
                            </CardActionArea>
                        </Card>
                    </Grid>);
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

                        // uncached chapter title would have suffix " *", remove it when download is completed.
                        if (chapter.title.endsWith(" *")) {
                            chapter.title = chapter.title.substr(0, chapter.title.length - 2);
                        }

                        setLoadingMessage('');
                    })
                });
            } else {
                // @ts-ignore
                const book: Book = data.current[bookId];
                cards.push(
                    <Grid item key={++cnt}>
                        <Card>
                            <CardActionArea onClick={() => update(Stage.Book, bookId, chapterId)}>
                                <CardContent style={{textAlign: "center"}}>
                                    {`《${book.name}》 ${chapter.title}`}
                                </CardContent>
                            </CardActionArea>
                        </Card>
                    </Grid>);
                cards.push(
                    <Grid item key={++cnt}>
                        <Card>
                            <CardContent style={{textIndent: "2em", padding: "8px"}}>
                                {chapter.content?.map(line => {
                                    return <p key={++cnt}>{line}</p>;
                                })}
                            </CardContent>
                        </Card>
                    </Grid>);
                // @ts-ignore
                if (book.chapters.length > chapterId + 1) {
                    if (prefetch) {
                        setTimeout(() => {
                            messenger.getChapter(bookId, chapterId + 1).then(content => {
                                if (book != null && book.chapters != null && book.chapters[chapterId + 1] != null) {
                                    book.chapters[chapterId + 1].content = content;
                                }
                            });
                        }, 0);
                    }
                    cards.push(
                        <Grid item key={++cnt}>
                            <Card>
                                <CardActionArea onClick={() => {
                                    update(stage, bookId, chapterId + 1);
                                    window.scrollTo(0, 50);
                                }}>
                                    <CardContent style={{textAlign: "center"}}>
                                        {`NEXT`}
                                    </CardContent>
                                </CardActionArea>
                            </Card>
                        </Grid>);
                } else {
                    pushBackToShelfCard();
                }
            }
            break;
        case Stage.Download:
            if (downloadStatus.length === 0) {
                messenger.getDownloadLink(bookId, setDownloadStatus, setDownloadStatus);
            } else {
                if (downloadStatus.startsWith("finished")) {
                    cards.push(<Grid item key={++cnt}>
                        <Card><CardContent style={{textAlign: "center"}}>
                            {downloadStatus}
                        </CardContent></Card>

                    </Grid>);
                } else {
                    cards.push(<Grid item key={++cnt}>
                        <Card>
                            <CardContent style={{textAlign: "center"}}>
                                <a download="book.txt" href={downloadStatus}>download</a>
                            </CardContent>
                        </Card>

                    </Grid>);
                }
            }
            cards.push(<Grid item key={++cnt}>
                <Card>
                    <CardActionArea onClick={() => unstable_batchedUpdates(() => {
                        update(Stage.Book, bookId, chapterId);
                        setDownloadStatus('');
                    })}>
                        <CardContent style={{textAlign: "center"}}>
                            Back
                        </CardContent>
                    </CardActionArea>
                </Card>
            </Grid>);
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
        {cards}
    </Grid>;
};
