package com.bazaarvoice.ostrich.perftest;

import com.bazaarvoice.ostrich.perftest.utils.Arguments;
import com.bazaarvoice.ostrich.perftest.utils.LoadRunner;
import org.apache.commons.cli.ParseException;

public class Main {

    public static void main(String args[]) throws InterruptedException, ParseException {

        Arguments arguments = new Arguments(args);
        LoadRunner loadRunner = new LoadRunner(arguments);

        loadRunner.printHeaders();
        do {
            loadRunner.printLog();
        } while(loadRunner.shouldContinue());
    }
}
