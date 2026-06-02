package com.osroyale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jire
 */
public enum Main {

    ;

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Starting OS Royale server...");
        new Thread(new Starter(), Starter.class.getSimpleName()).start();
    }

}
