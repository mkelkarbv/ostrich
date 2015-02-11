package com.bazaarvoice.ostrich.perftest.core;

public interface Result<R> {

    boolean hasError();

    Exception getException();

    R getResult();
}
