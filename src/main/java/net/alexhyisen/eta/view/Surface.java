package net.alexhyisen.eta.view;

import net.alexhyisen.eta.model.*;
import net.alexhyisen.eta.model.mailer.Mail;
import net.alexhyisen.eta.model.mailer.MailService;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Created by Alex on 2017/3/13.
 * The entrance of the project.
 */
public class Surface {
    private static void showInstruction(){
        System.out.println("usage:");
        System.out.println("-a --auto Auto Mode");
        System.out.println("-c --cli  open Command Line Interface");
    }
    private static void runAutoMode(){
        System.out.println("Auto Mode");
        System.out.println("read default config & source");
        System.out.println("config path = .\\config");
        System.out.println("source path = .\\source");
        System.out.println("update all the books and save them");
        System.out.println("mail the newly chapters to catcher");

        Config config=new Config();
        config.load();
        MailService ms=new MailService(config);
        Source source=new Source();
        source.load();
        for(Book book:source.getData()){
            book.read(20);
            book.save().forEach(chapter->{
                Mail mail=new Mail(config,book.getName(),chapter);
                try {
                    ms.send(mail);
                    Utility.log("transmitted "+mail.getSubject());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        System.out.println("finished");
    }

    //transform Predicate<Integer> to Predicate<String> using Integer.valueOf()
    private static Predicate<String> tran(Predicate<Integer> orig){
        return str->{
            try {
                int value=Integer.valueOf(str);
                return orig.test(value);
            } catch (NumberFormatException e) {
                return false;
            }
        };
    }

    private static String getInput(String name, Predicate<String> IsValid){
        String rtn;
        do{
            System.out.print(name+" = ");
            rtn=new Scanner(System.in).nextLine();
        }while (!IsValid.test(rtn));
        return rtn;
    }

    private static <E extends Enum<E>> E getInput(String name,Class<E> enumType){
        String input;
        E rtn;
        do{
            System.out.print(name+" = ");
            input=new Scanner(System.in).nextLine().toUpperCase();
            try {
                rtn=E.valueOf(enumType,input);
                return rtn;
            } catch (IllegalArgumentException ignored) {
            }
        }while (true);
    }

    //return null if the quit id, which is -1, is selected
    @Nullable
    private static <T> T select(List<T> candidates, Function<T,String> getNameMethod){
        for (int k = 0; k < candidates.size(); k++) {
            System.out.printf("\t%4d -> %s\n",k,getNameMethod.apply(candidates.get(k)));
        }
        int choice=Integer.valueOf(getInput("selected id",tran(v->v>=-1&&v<candidates.size())));
        return choice==-1?null:candidates.get(choice);
    }

    private enum Command{
        N("next chapter"),
        C("select chapter"),
        B("select book"),
        Q("quit the program");

        String info;

        Command(String info) {
            this.info = info;
        }

        public String getInfo() {
            return info;
        }
    }

    private static void printChapter(Book book,Chapter chapter){
        System.out.printf("《%s》 %s\n",book.getName(),chapter.getName());
        chapter.download();
        Arrays.stream(chapter.getData()).forEach(v-> System.out.println("    "+v));
    }

    private static void runCLI(){
        System.out.println("Welcome to Eta Command Line Interface");
        System.out.println("loading default source file from .\\source");

        System.out.println();
        System.out.println("when facing requirement command =");
        Arrays.stream(Command.values()).forEach(v-> System.out.println(v.toString()+" means "+v.getInfo()));
        System.out.println("input one of thr previous commands");

        System.out.println();
        System.out.println("when facing requirement command =");
        Arrays.stream(Command.values()).forEach(v-> System.out.println(v.toString()+" means "+v.getInfo()));
        System.out.println("input one of thr previous commands");

        System.out.println();
        System.out.println("when facing requirement selected id =");
        System.out.println("just input the id you selected");
        System.out.println("input -1 will forced it back to the upper menu");

        Source source=new Source();
        source.load();

        selectBook:
        while(true){
            System.out.println();
            System.out.println("please selected the book");
            Book book=select(source.getData(),Book::getName);
            if(book==null){
                break;
            }
            System.out.println("opening book "+book.getName()+" from "+book.getSource());
            book.open();

            selectChapter:
            while(true){
                System.out.println();
                System.out.println("please selected the chapter");
                Chapter chapter=select(book.getChapters(),Chapter::getName);
                if(chapter==null){
                    continue selectBook;
                }
                printChapter(book,chapter);

                while (true){
                    switch (getInput("command",Command.class)){
                        case N:
                            int i=book.getChapters().indexOf(chapter);
                            ++i;
                            if(i!=book.getChapters().size()){//max index should be size-1
                                chapter=book.getChapters().get(i);
                                printChapter(book,chapter);
                            }else {
                                System.out.println("no more chapters");
                            }
                            break;
                        case C:
                            continue selectChapter;
                        case B:
                            continue selectBook;
                        case Q:
                            break selectBook;
                    }
                }
            }
        }

    }

    public static void main(String[] args) {
        if(args.length==0){
            showInstruction();
        }else if(args[0].equals("-a")||args[0].equals("--auto")){
            runAutoMode();
        }else if(args[0].equals("-c")||args[0].equals("--cli")){
            runCLI();
        }
    }
}
