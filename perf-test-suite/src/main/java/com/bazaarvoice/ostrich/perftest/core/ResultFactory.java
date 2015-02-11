package com.bazaarvoice.ostrich.perftest.core;

public interface ResultFactory<T> {
    Result<T> createResponse(T result);
    Result<T> createResponse(Exception error);
}
