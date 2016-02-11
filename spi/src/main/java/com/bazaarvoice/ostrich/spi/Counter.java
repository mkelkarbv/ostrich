package com.bazaarvoice.ostrich.spi;

public interface Counter {
    void dec(int size);
    void inc();
    void dec();
}
