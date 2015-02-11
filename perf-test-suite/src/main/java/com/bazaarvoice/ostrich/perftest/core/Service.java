package com.bazaarvoice.ostrich.perftest.core;

public interface Service<W, R> {

    public R process(W work);

    void initialize();

    void destroy();

}


