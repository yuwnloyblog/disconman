package com.yuwnloy.disconman.utils;

import java.io.UnsupportedEncodingException;

import com.yuwnloy.disconman.exceptions.EncodeException;


public class SafeEncoder {
	public static byte[] encode(final Object obj) {
        try {
            if (obj == null) {
                throw new EncodeException(
                        "value sent to redis cannot be null");
            }
            String str = String.valueOf(obj);
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new EncodeException(e);
        }
    }
}
