package com.bitmovin_prep.app;

public class Main {
        public static void main(String[] args) throws
                InterruptedException, java.io.IOException, Exception
        {
            PerTitleEncoding perTitleEncoding = new PerTitleEncoding();
            perTitleEncoding.execute();
            //BasicEncodingClient basicEncodingClient = new BasicEncodingClient();
            //basicEncodingClient.execute();
            //FiltersAndThumbnails filtersEncodingClient = new FiltersAndThumbnails();
            //filtersEncodingClient.execute();
        }
}
