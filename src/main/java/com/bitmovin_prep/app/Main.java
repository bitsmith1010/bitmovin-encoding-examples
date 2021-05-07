package com.bitmovin_prep.app;

public class Main {
        public static void main(String[] args) throws
                InterruptedException, java.io.IOException
        {
            //PerTitleEncoding perTitleEncoding = new PerTitleEncoding();
            //perTitleEncoding.execute();
            //BasicEncodingClient basicEncodingClient = new BasicEncodingClient();
            //basicEncodingClient.execute();
          FiltersAndThumbnails filtersEncodingClient = new FiltersAndThumbnails();
          filtersEncodingClient.execute();

          //AutoTest test1 = new AutoTest();
            //test1.runTest();
        }
}
