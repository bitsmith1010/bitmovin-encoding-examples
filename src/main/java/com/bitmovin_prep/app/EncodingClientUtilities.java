package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.model.Status;
import com.bitmovin.api.sdk.model.Task;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;

public class EncodingClientUtilities {

    public static final java.util.logging.Logger logger =
            java.util.logging.Logger.getLogger(PerTitleEncoding.class.getName());

    public static BitmovinApi bitmovinApi;

    public Properties getProperties() throws IOException
    {
        Properties config = new Properties();
        InputStream configFile = getClass().getResourceAsStream(
                "/META-INF/application.properties");
        config.load(configFile);
        configFile.close();

        configFile = getClass().getResourceAsStream(
                "/META-INF/application_private.properties");
        config.load(configFile);
        configFile.close();

        return config;
    }

    // rootPath format: domain/bucket_name/encoding_tests/myprobe/DATE
    public String createRootPath(Properties config) {
        return Paths.get(config.getProperty("output_path"),
                new Date().toString().replace(" ", "_"))
                .toString();
    }

    public void awaitEncoding(String encodingId)
            throws InterruptedException {

        Task status;
        do {
            Thread.sleep(2500);
            status = bitmovinApi.encoding.encodings.status(encodingId);
        } while (status.getStatus() != Status.FINISHED && status.getStatus() != Status.ERROR);

        if (status.getStatus() == Status.ERROR) {
            logger.info("Encoding: An error has occurred");
            return;
        }

        logger.info("Encoding: finished successfully.");

    }
}