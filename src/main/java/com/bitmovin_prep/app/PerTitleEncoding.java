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
import java.util.Date;
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

  public void execute() throws IOException, InterruptedException
  {
    Properties config = getProperties();
    logger.info("cofiguration file: " + config.toString());

    bitmovinApi = createBitmovinApi(config.getProperty("api_key"));
    logger.info("created an instance of bitmovin api " +
            bitmovinApi.toString());

    String gcsInId = ! config.getProperty("input_resource_id").equals("") ?
            config.getProperty("input_resource_id") :
            createGcsInput("resource-in-1",
                    config.getProperty("gcs_input_access"),
                    config.getProperty("gcs_input_secret"),
                    config.getProperty("input_bucket_name"))
            .getId();
    logger.info("in id: " + gcsInId);

    String gcsOutId = ! config.getProperty("output_resource_id").equals("") ?
            config.getProperty("output_resource_id") :
            createGcsOutput("resource-out-1",
                    config.getProperty("gcs_output_access"),
                    config.getProperty("gcs_output_secret"),
                    config.getProperty("output_bucket_name"))
                    .getId();
    logger.info("out id: " + gcsOutId);

    String h265ConfigurationId =
            ! config.getProperty("h265_config_1_id").equals("") ?
                    config.getProperty("h265_config_1_id") :
            createH265Configuration(
            "h265-1",
            Integer.parseInt(config.getProperty("h265_1_width")));
    logger.info("video config id: " + h265ConfigurationId);

    String aacConfigurationId =
            ! config.getProperty("aac_config_1_id").equals("") ?
                    config.getProperty("aac_config_1_id") :
            createAacConfiguration(
            "aac-1",
            Long.parseLong(config.getProperty("aac_1_bitrate")));
    logger.info("audio config id: " + aacConfigurationId);

    String rootPath =
            ! config.getProperty("encoding_root_path").equals("") ?
                    config.getProperty("encoding_root_path") :
                    createRootPath(config);
    logger.info("root path: " + rootPath);

    String encodingId = "";
    if (!config.getProperty("encoding_id").equals(""))
      encodingId = config.getProperty("encoding_id");
    else {
      encodingId = createEncoding("encoding-per-title");
      logger.info("encoding id: " + encodingId);

      StreamInput h265StreamInput = createStreamInput(
              config.getProperty("video_input_path"), gcsInId);
      logger.info("input to video stream: " + h265StreamInput.getInputId());

      StreamInput aacStreamInput = createStreamInput(
              config.getProperty("audio_input_path"), gcsInId);
      logger.info("input to audio stream: " + aacStreamInput.getInputId());

      String aacStreamId = createStream(encodingId,
              aacConfigurationId, aacStreamInput);
      logger.info("audio stream id: " + aacStreamId);

      String h265StreamId = createStreamForPerTitle(encodingId,
              h265ConfigurationId, h265StreamInput);
      logger.info("video stream id: " + h265StreamId);

      EncodingOutput fmp4H265Out = createEncodingOutput(
              gcsOutId, rootPath,
              "h265", "{width}_{bitrate}_{uuid}", "fmp4");
      logger.info("fmp4 h265 output: " + fmp4H265Out);

      EncodingOutput fmp4AacOut = createEncodingOutput(
              gcsOutId, rootPath, "aac", "16000", "fmp4");
      logger.info("fmp4 aac output: " + fmp4AacOut);

      createFmp4Muxing(encodingId, fmp4H265Out, h265StreamId);

      createFmp4Muxing(encodingId, fmp4AacOut, aacStreamId);

      startEncoding(encodingId);
      awaitEncoding(encodingId);
    }
    logger.info("encoding id: " + encodingId);

    EncodingOutput manifestOut = createEncodingOutput(gcsOutId, rootPath);
    String manifestId = createDashManifestDefault(
            "manifest.mpd", encodingId, manifestOut);
    bitmovinApi.encoding.manifests.dash.start(manifestId);

    logger.info("manifest id: " + manifestId);

    //#endmain
  }

  // differs from basic setup: bitrate not set
  public String createH264Configuration(
          String name, int width)
  {
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
    String name, int width)
  {
    H265VideoConfiguration configuration = new H265VideoConfiguration();
    configuration.setName(name);
    configuration.setWidth(width);
    configuration.setPresetConfiguration(PresetConfiguration.VOD_STANDARD);
    return bitmovinApi.encoding
      .configurations.video.h265.create(configuration).getId();
  }

  // differs to basic encoding: mode attr = StreamMode.PER_TITLE_TEMPLATE
  public String createStreamForPerTitle(
          String encodingId, String configurationId,
          StreamInput streamInput)
  {
    Stream stream = new Stream();
    stream.setCodecConfigId(configurationId);
    stream.addInputStreamsItem(streamInput);
    stream.setMode(StreamMode.PER_TITLE_TEMPLATE);
    return bitmovinApi.encoding.encodings.streams.create(encodingId, stream).getId();
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

  public void startEncoding(String encodingId)
  {

    StartEncodingRequest startEncodingRequest = new StartEncodingRequest();
    startEncodingRequest.setPerTitle(createPerTitleH265());

    // this is suggested in the guide and example. Here an error resulted.
    // startEncodingRequest.setEncodingMode(EncodingMode.SINGLE_PASS);

    bitmovinApi.encoding.encodings.start(encodingId, startEncodingRequest);
  }
}