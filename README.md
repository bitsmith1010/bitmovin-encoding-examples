## bitmovin encoding sdk examples

basic encoding clients with minimal configurations

constructors factored to `BasicEncodingClient` so as to
emphasize the special requirements of the various encoding types

### usage

1. enter any credentials required and that are null in
    `/src/main/resources/META-INF/application.properties` to a
    file
    `/src/main/resources/META-INF/application_private.properties`
  
    _note:_ there is to improve the documentation of the
    properties of `application.properties`, at present
    it could result unclear. 
  
     - the current version uses google input and output sources,
     so if you need a different type of source you can:
  
         - enter existing sources id to the properties
           `input_resource_id`, `output_resource_id properties`
  
         - or, modify the source files

2. (optional) modify the configurations in `applications.properties`

3. in `com.bitmovin_prep.app.Main.java`
   keep uncommented only the class of the
   desired encoding client
