package com.bazaarvoice.ostrich.perftest.utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.util.Random;

/**
 * Various singleton hash function to mimic workload
 */
public enum HashFunction {
    SHA1 {
        @Override
        public String process(String work) {
            return DigestUtils.sha1Hex(work);
        }
    },
    SHA256 {
        @Override
        public String process(String work) {
            return DigestUtils.sha256Hex(work);
        }
    },
    SHA384 {
        @Override
        public String process(String work) {
            return DigestUtils.sha384Hex(work);
        }
    },
    SHA512 {
        @Override
        public String process(String work) {
            return DigestUtils.sha512Hex(work);
        }
    },
    MD2 {
        @Override
        public String process(String work) {
            return DigestUtils.md2Hex(work);
        }
    },
    MD5 {
        @Override
        public String process(String work) {
            return DigestUtils.md5Hex(work);
        }
    };

    public abstract String process(String work);

    private static final int RANDOM_HASH_FUNCTION_LIMIT = HashFunction.values().length;
    private static final Random RANDOM = new Random();
    public static HashFunction getRandomHashFunction() {
        return HashFunction.values()[RANDOM.nextInt(RANDOM_HASH_FUNCTION_LIMIT)];
    }
}
