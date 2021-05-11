//
//2345678901234567890123456789012345678901234567890123456789012345678901234
//
package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.model.AclEntry;
import com.bitmovin.api.sdk.model.AclPermission;
import com.bitmovin.api.sdk.model.AutoRepresentation;
import com.bitmovin.api.sdk.model.EncodingOutput;
import com.bitmovin.api.sdk.model.H264PerTitleConfiguration;
import com.bitmovin.api.sdk.model.H264VideoConfiguration;
import com.bitmovin.api.sdk.model.H265PerTitleConfiguration;
import com.bitmovin.api.sdk.model.H265VideoConfiguration;
import com.bitmovin.api.sdk.model.PerTitle;
import com.bitmovin.api.sdk.model.PresetConfiguration;
import com.bitmovin.api.sdk.model.StartEncodingRequest;
import com.bitmovin.api.sdk.model.Stream;
import com.bitmovin.api.sdk.model.StreamInput;
import com.bitmovin.api.sdk.model.StreamMode;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/*
  OVERWRITTEN:
    execute(), createH264Configuration(), createH265Configuration(),
    createEncodingOutput(), startEncoding()
  NEW METHODS: createPerTitleH265(), createPerTitleH264(),
      createStreamForPerTitle()

  CHANGE CODEC:
    switch codec specific methods in exec() and startEncoding()
 */

public class PerTitleEncoding extends BasicEncodingClient {

  public void execute() throws IOException, InterruptedException, Exception {
    Properties config = getProperties();
    logger.info("cofiguration file: " + config.toString());

    bitmovinApi = createBitmovinApi(config.getProperty("api_key"));
    logger.info("created an instance of bitmovin api " +
      bitmovinApi.toString());

    String gcsInId = !config.getProperty("input_resource_id").equals("") ?
      config.getProperty("input_resource_id") :
      createGcsInput("resource-in-1",
        config.getProperty("gcs_input_access"),
        config.getProperty("gcs_input_secret"),
        config.getProperty("input_bucket_name"))
        .getId();
    logger.info("in id: " + gcsInId);

    String out1Id = "";
    String out1Type = config.getProperty("out_1_type");
    switch (out1Type) {
      case "S3":
        out1Id = !config.getProperty("out_1_id").equals("") ?
          config.getProperty("out_1_id") :
          createOutS3("out_aws_1",
            config.getProperty("aws_out_1_access"),
            config.getProperty("aws_out_1_secret"),
            config.getProperty("aws_out_1_bucket_name"))
            .getId();
        logger.info("in id: " + out1Id);
        break;
      case "GCP":
        out1Id = !config.getProperty("output_resource_id").equals("") ?
          config.getProperty("output_resource_id") :
          createGcsOutput("resource-out-1",
            config.getProperty("gcs_output_access"),
            config.getProperty("gcs_output_secret"),
            config.getProperty("output_bucket_name"))
            .getId();
        logger.info("out id: " + out1Id);
        break;
    }

    /*
      - 1 base template stream:
      h265ConfigurationTemplate                 ->
                                  stream PER_TITLE_TEMPLATE

      - for each resolution, create a new template stream:

      h265ConfigurationTemplateFixedResolution1 ->
                                  stream PER_TITLE_TEMPLATE_FIXED_RESOLUTION 1

      h265ConfigurationTemplateFixedResolution2 ->
                                  stream PER_TITLE_TEMPLATE_FIXED_RESOLUTION 2

                                  ...
     */

    String templateH265Configuration1Id = "";
    String templateFixedResolutionH265Configuration1Id = "";
    switch (config.getProperty("per_title_stream_mode")) {

      case "per_title_template":
        templateH265Configuration1Id =
          !config.getProperty("h265_config_1_id").equals("") ?
            config.getProperty("h265_config_1_id") :
            createH265Configuration("h265-template", config);
        logger.info("template video config id: " +
          templateH265Configuration1Id);

      case "per_title_template_fixed_resolution":
        templateH265Configuration1Id =
          !config.getProperty("h265_config_1_id").equals("") ?
            config.getProperty("h265_config_1_id") :
            createH265Configuration("h265-template");
        logger.info("template video config id: " +
          templateH265Configuration1Id);
        // TODO: abstract this fragment of specific h265 to general codec
        if (config.getProperty("h265_1_height").equals("")
          && config.getProperty("h265_1_width").equals(""))
          throw new Exception("[PerTitleEncoding demo] fixed resolution " +
            "template: required: property h265_1_height or h265_1_width");
        // TODO: function wrapper for the logger.info and the test
        //  for the object id
        templateFixedResolutionH265Configuration1Id =
          !config.getProperty("h265_config_1_id").equals("") ?
            config.getProperty("h265_config_1_id") :
            createH265Configuration(
              "h265-fixed-resolution-template-1", config);
        logger.info("fixed res. template config 1 id: " +
          templateFixedResolutionH265Configuration1Id);
        break;

      default: throw new Exception("[PerTitleEncoding demo] required " +
        "property per_title_stream_mode");
    }

    String aacConfigurationId =
      !config.getProperty("aac_config_1_id").equals("") ?
        config.getProperty("aac_config_1_id") :
        createAacConfiguration(
          "aac-1",
          Long.parseLong(config.getProperty("aac_1_bitrate")));
    logger.info("audio config id: " + aacConfigurationId);

    String rootPath =
      !config.getProperty("encoding_root_path").equals("") ?
        config.getProperty("encoding_root_path") :
        createRootPath(config);
    logger.info("root path: " + rootPath);

    String encodingId = "";
    if (!config.getProperty("encoding_id").equals(""))
      encodingId = config.getProperty("encoding_id");
    else {
      encodingId = createEncoding("encoding-per-title");
      logger.info("encoding id: " + encodingId);


      // ---------------------------------------------
      // SEGMENTED MP4 H265 MUXING: PER TITLE TEMPLATE
      // ---------------------------------------------

      StreamInput h265StreamInput1 = createStreamInput(
        config.getProperty("video_input_path"), gcsInId);
      logger.info("input to video stream: " +
        h265StreamInput1.getInputId());

      String h265TemplateStream1Id = createStreamForPerTitle(
        encodingId, templateH265Configuration1Id, h265StreamInput1,
        StreamMode.PER_TITLE_TEMPLATE);
      logger.info("video stream id: " + h265TemplateStream1Id);

      EncodingOutput fmp4H265Out1 = createEncodingOutput(
        out1Id,
        rootPath,
        "h265", "{width}_{bitrate}_{uuid}", "fmp4");
      logger.info("fmp4 h265 output: " + fmp4H265Out1);

      createFmp4Muxing(encodingId, fmp4H265Out1, h265TemplateStream1Id);


      // ------------------------
      // SEGMENTED MP4 AAC MUXING
      // ------------------------

      StreamInput aacStreamInput = createStreamInput(
        config.getProperty("audio_input_path"), gcsInId);
      logger.info("input to audio stream: " + aacStreamInput.getInputId());

      String aacStreamId = createStream(
        encodingId, aacConfigurationId, aacStreamInput);
      logger.info("audio stream id: " + aacStreamId);

      EncodingOutput fmp4AacOut = createEncodingOutput(
        out1Id, rootPath, "aac", "16000", "fmp4");
      logger.info("fmp4 aac output: " + fmp4AacOut);

      createFmp4Muxing(encodingId, fmp4AacOut, aacStreamId);


      // --------------------------------------------------------------
      // SEGMENTED MP4 H265 MUXING: PER TITLE FIXED RESOLUTION TEMPLATE
      // --------------------------------------------------------------
      String h265TemplateFixedResoluionStream1Id = "";
      if (config.getProperty("per_title_stream_mode")
        .equals("per_title_template_fixed_resolution")) {

        StreamInput h265StreamInput2 = createStreamInput(
          config.getProperty("video_input_path"), gcsInId);
        logger.info("input to video stream: " +
          h265StreamInput2.getInputId());

        h265TemplateFixedResoluionStream1Id = createStreamForPerTitle(
          encodingId,
          templateFixedResolutionH265Configuration1Id,
          h265StreamInput2,
          StreamMode.PER_TITLE_TEMPLATE_FIXED_RESOLUTION);
        logger.info("video stream id: " +
          h265TemplateFixedResoluionStream1Id);

        EncodingOutput fmp4H265Out2 = createEncodingOutput(
          out1Id,
          rootPath,
          "h265", "{width}_{bitrate}_{uuid}", "fmp4");
        logger.info("fmp4 h265 output: " + fmp4H265Out2);

        createFmp4Muxing(
          encodingId,
          fmp4H265Out2,
          h265TemplateFixedResoluionStream1Id);
      }


      // --------------------------------------------
      // START ENCODING REQUEST WITH PER-TITLE OBJECT
      // --------------------------------------------

      PerTitle perTitle = createPerTitleH265();
      logger.info("per title object: " + perTitle);

      startEncoding(encodingId, perTitle);
      awaitEncoding(encodingId);
    }
    logger.info("encoding id: " + encodingId);

    EncodingOutput manifestOut = createEncodingOutput(out1Id, rootPath);
    String manifestId = createDashManifestDefault(
      "manifest.mpd", encodingId, manifestOut);
    bitmovinApi.encoding.manifests.dash.start(manifestId);

    logger.info("manifest id: " + manifestId);

    //#endmain
  }

  // differs from basic setup: bitrate not set
  public String createH264Configuration(
    String name, int width) {
    H264VideoConfiguration configuration = new H264VideoConfiguration();
    configuration.setName(name);
    //configuration.setProfile(ProfileH264.HIGH);
    // as it appears, it's possible to set a target resolution:
    configuration.setWidth(width);
    configuration.setPresetConfiguration(PresetConfiguration.VOD_STANDARD);

    return bitmovinApi.encoding
      .configurations.video.h264.create(configuration).getId();
  }

  public String createH265Configuration(
    String name, Properties... moreConfig)
  {
    H265VideoConfiguration codecConfig = new H265VideoConfiguration();
    codecConfig.setName(name);

    if (moreConfig.length > 0) {
      if (!moreConfig[0].getProperty("h265_1_height").equals(""))
        codecConfig.setHeight(
          Integer.parseInt(moreConfig[0].getProperty("h265_1_height")));

      if (!moreConfig[0].getProperty("h265_1_width").equals(""))
        codecConfig.setWidth(
          Integer.parseInt(moreConfig[0].getProperty("h265_1_width")));
    }

    codecConfig.setPresetConfiguration(PresetConfiguration.VOD_STANDARD);
    return bitmovinApi.encoding
      .configurations.video.h265.create(codecConfig).getId();
  }

  // differs to basic encoding: mode attr StreamMode has other values
  public String createStreamForPerTitle(
    String encodingId,
    String configurationId,
    StreamInput streamInput,
    StreamMode mode)
  {
    Stream stream = new Stream();
    stream.setCodecConfigId(configurationId);
    stream.addInputStreamsItem(streamInput);
    stream.setMode(mode);
    return bitmovinApi.encoding.encodings.streams
      .create(encodingId, stream).getId();
  }

  // note: this is identical to the default constructor
  //        save the enum: "RENDITION" changed to "PER_TITLE_TEMPLATE"
  public enum createEncodingOutputVariableParameterIndex
  {CODEC, PER_TITLE_TEMPLATE, CONTAINER_FORMAT}
  public EncodingOutput createEncodingOutput(
    String resourceId, String rootPath, String... pathComponents)
  {
    AclEntry aclEntry = new AclEntry();
    aclEntry.setPermission(AclPermission.PUBLIC_READ);
    List<AclEntry> aclEntryList = new ArrayList<AclEntry>();
    aclEntryList.add(aclEntry);

    EncodingOutput out = new EncodingOutput();
    out.setAcl(aclEntryList);
    out.setOutputId(resourceId);
    if( pathComponents.length > 0 )
      out.setOutputPath(Paths.get(
        rootPath,
        pathComponents[createEncodingOutputVariableParameterIndex
          .CODEC.ordinal()] +
          "_" +
          pathComponents[createEncodingOutputVariableParameterIndex
            .PER_TITLE_TEMPLATE.ordinal()],
        pathComponents[createEncodingOutputVariableParameterIndex
          .CONTAINER_FORMAT.ordinal()]
      ).toString());
    else out.setOutputPath( rootPath );

    return out;
  }

  public PerTitle createPerTitleH264()
  {
    AutoRepresentation autoRepresentation = new AutoRepresentation();

    H264PerTitleConfiguration h264PerTitleConfiguration =
      new H264PerTitleConfiguration();
    h264PerTitleConfiguration.setAutoRepresentations(autoRepresentation);

    PerTitle perTitle = new PerTitle();
    perTitle.setH264Configuration(h264PerTitleConfiguration);

    return perTitle;
  }

  public PerTitle createPerTitleH265()
  {
    AutoRepresentation autoRepresentation = new AutoRepresentation();

    H265PerTitleConfiguration h265PerTitleConfiguration =
      new H265PerTitleConfiguration();
    h265PerTitleConfiguration.setAutoRepresentations(autoRepresentation);

    PerTitle perTitle = new PerTitle();
    perTitle.setH265Configuration(h265PerTitleConfiguration);

    return perTitle;
  }

  public void startEncoding(String encodingId, PerTitle perTitle)
  {

    StartEncodingRequest startEncodingRequest = new StartEncodingRequest();
    startEncodingRequest.setPerTitle(perTitle);

    // this is suggested in the guide and example. Here an error resulted.
    // startEncodingRequest.setEncodingMode(EncodingMode.SINGLE_PASS);

    bitmovinApi.encoding.encodings.start(encodingId, startEncodingRequest);
  }
}