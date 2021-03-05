//
//234567890123456789012345678901234567890123456789012345678901234567890123456789
//
package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.common.BitmovinException;
import com.bitmovin.api.sdk.model.AacAudioConfiguration;
import com.bitmovin.api.sdk.model.AclEntry;
import com.bitmovin.api.sdk.model.AclPermission;
import com.bitmovin.api.sdk.model.CloudRegion;
import com.bitmovin.api.sdk.model.DashManifestDefault;
import com.bitmovin.api.sdk.model.DashManifestDefaultVersion;
import com.bitmovin.api.sdk.model.Encoding;
import com.bitmovin.api.sdk.model.EncodingOutput;
import com.bitmovin.api.sdk.model.Fmp4Muxing;
import com.bitmovin.api.sdk.model.GcsOutput;
import com.bitmovin.api.sdk.model.H264VideoConfiguration;
import com.bitmovin.api.sdk.model.HlsManifestDefault;
import com.bitmovin.api.sdk.model.HlsManifestDefaultVersion;
import com.bitmovin.api.sdk.model.LiveDashManifest;
import com.bitmovin.api.sdk.model.LiveHlsManifest;
import com.bitmovin.api.sdk.model.MuxingStream;
import com.bitmovin.api.sdk.model.PresetConfiguration;
import com.bitmovin.api.sdk.model.StartLiveEncodingRequest;
import com.bitmovin.api.sdk.model.Status;
import com.bitmovin.api.sdk.model.Stream;
import com.bitmovin.api.sdk.model.StreamInput;
import com.bitmovin.api.sdk.model.StreamSelectionMode;
import com.bitmovin.api.sdk.model.Task;
import feign.Logger;
import feign.slf4j.Slf4jLogger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;

public class LiveEncoding {

  private static final java.util.logging.Logger logger =
          java.util.logging.Logger.getLogger(LiveEncoding.class.getName());

  private static BitmovinApi bitmovinApi;

  public void main() throws IOException
  {

    Properties config = getProperties();
    logger.info("cofiguration file: " + config.toString());

    bitmovinApi = createBitmovinApi(config.getProperty("api_key"));
    logger.info("created an instance of bitmovin api " +
            bitmovinApi.toString());

    String inRtmpId = getRtmpInputId();

    String gcsOutId = ! config.getProperty("output_resource_id").equals("") ?
            config.getProperty("output_resource_id") :
            createGcsOutput("resource-out-1",
                    config.getProperty("gcs_output_access"),
                    config.getProperty("gcs_output_secret"),
                    config.getProperty("output_bucket_name"))
                    .getId();
    logger.info("out id: " + gcsOutId);

    String encodingId = createEncoding("encoding-per-title");
    logger.info("encoding id: " + encodingId);


    // --------- renditions cofiguration ----------------
    //-----------------------------------------

    String h264ConfigurationId =
            ! config.getProperty("h264_config_1_id").equals("") ?
                    config.getProperty("h264_config_1_id") :
                    createH264Configuration(
                            "h264-1",
                            Integer.parseInt(config.getProperty("h264_1_height")),
                            Long.parseLong(config.getProperty("h264_1_bitrate"))
                    );
    logger.info("video config id: " + h264ConfigurationId);

    String h264Configuration2Id =
            ! config.getProperty("h264_config_2_id").equals("") ?
                    config.getProperty("h264_config_2_id") :
                    createH264Configuration(
                            "h264-2",
                            Integer.parseInt(config.getProperty("h264_2_height")),
                            Long.parseLong(config.getProperty("h264_2_bitrate"))
                    );
    logger.info("video config id: " + h264Configuration2Id);

    String aacConfigurationId =
            ! config.getProperty("aac_config_1_id").equals("") ?
                    config.getProperty("aac_config_1_id") :
                    createAacConfiguration(
                            "aac-1",
                            Long.parseLong(config.getProperty("aac_1_bitrate")),
                            Double.parseDouble(config.getProperty(
                                    "aac_1_sample_rate"))
                    );
    logger.info("audio config id: " + aacConfigurationId);


    // --------- streams ----------------
    //-----------------------------------------

    String h264StreamId = createH264Stream(encodingId,
            h264ConfigurationId, inRtmpId);
    logger.info("video stream id: " + h264StreamId);

    String h264Stream2Id = createH264Stream(encodingId,
            h264Configuration2Id, inRtmpId);
    logger.info("video stream id: " + h264StreamId);

    String aacStreamId = createAacStream(encodingId,
            aacConfigurationId, inRtmpId);
    logger.info("audio stream id: " + aacStreamId);


    // --------- muxings ----------------
    //-----------------------------------------

    // rootPath format: domain/bucket_name/encoding_tests/myprobe/DATE
    String rootPath = Paths.get(config.getProperty("output_path"),
            new Date().toString().replace(" ", "_")).toString();

    EncodingOutput fmp4H264Out = createEncodingOutput(
            gcsOutId, rootPath, "h264",
            config.getProperty("h264_1_height") + "_" +
                    config.getProperty("h264_1_bitrate"), "fmp4");
    logger.info("fmp4 h264 output: " + fmp4H264Out);

    EncodingOutput fmp4H264Out2 = createEncodingOutput(
            gcsOutId, rootPath, "h264",
            config.getProperty("h264_2_height") + "_" +
                    config.getProperty("h264_2_bitrate"), "fmp4");
    logger.info("fmp4 h264 output 2: " + fmp4H264Out);

    EncodingOutput fmp4AacOut = createEncodingOutput(
            gcsOutId, rootPath, "aac",
            config.getProperty("aac_1_bitrate"), "fmp4");
    logger.info("fmp4 aac output: " + fmp4AacOut);

    createFmp4Muxing(encodingId, fmp4H264Out, h264StreamId);

    createFmp4Muxing(encodingId, fmp4H264Out2, h264Stream2Id);

    createFmp4Muxing(encodingId, fmp4AacOut, aacStreamId);


    // --------- manifests ----------------
    //-----------------------------------------

    EncodingOutput dashOut = createEncodingOutput(gcsOutId, rootPath);
    logger.info("manifest output: " + dashOut);

    String dashId = createDashManifestDefault("manifest.mpd",
            encodingId, dashOut);
    logger.info("manifest id: " + dashId);

    LiveDashManifest liveDashManifestId = createLiveDashManifest(
            dashId, 90D, 300D);

    EncodingOutput hlsOut = createEncodingOutput(gcsOutId, rootPath);
    logger.info("manifest output: " + hlsOut);

    String hlsId = createHlsManifestDefault(
            "manifest.m3u8", encodingId, hlsOut);
    logger.info("manifest id: " + dashId);

    LiveHlsManifest liveHlsManifestId = createLiveHlsManifest(
            hlsId, 90D);


    // --------- initialize encoding, generate manifest files---------------
    //-----------------------------------------

    startEncoding(encodingId, liveDashManifestId, liveHlsManifestId,
            config.getProperty("live_key"));

    //#endmain
  }

  private Properties getProperties() throws IOException
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

  private static String getRtmpInputId()
          throws BitmovinException
  {
    return bitmovinApi.encoding.inputs.rtmp.list().getItems().get(0).getId();
  }

  private static BitmovinApi createBitmovinApi(String key)
  {
    return BitmovinApi.builder()
            .withApiKey(key)
            .withLogger(new Slf4jLogger(), Logger.Level.BASIC)
            .build();
  }

  private static GcsOutput createGcsOutput(String name, String access,
                                           String secret, String bucketName)
  {
    GcsOutput output = new GcsOutput();
    output.setName(name);
    output.setAccessKey(access);
    output.setSecretKey(secret);
    output.setBucketName(bucketName);

    return bitmovinApi.encoding.outputs.gcs.create(output);
  }

  private static String createEncoding(String name)
  {
    Encoding encoding = new Encoding();
    encoding.setName(name);
    encoding.setCloudRegion(CloudRegion.AUTO);
    return bitmovinApi.encoding.encodings.create(encoding).getId();
  }

  //#codecconfig
  private static String createH264Configuration(
          String name, int height, long bitrate)
  {
    H264VideoConfiguration configuration = new H264VideoConfiguration();
    configuration.setName(name);
    configuration.setHeight(height);
    configuration.setWidth( (int)(Math.ceil(height * 16/9.0)) );
    configuration.setBitrate(bitrate);
    configuration.setPresetConfiguration(PresetConfiguration.LIVE_STANDARD);

    return bitmovinApi.encoding
            .configurations.video.h264.create(configuration).getId();
  }

  private static String createAacConfiguration(
          String name, long bitrate, double sampleRate)
  {
    AacAudioConfiguration audioCodecConfiguration = new AacAudioConfiguration();
    audioCodecConfiguration.setName(name);
    audioCodecConfiguration.setBitrate(bitrate);
    audioCodecConfiguration.setRate(sampleRate);
    return bitmovinApi.encoding.configurations
            .audio.aac.create(audioCodecConfiguration).getId();
  }

  private static StreamInput createStreamRtmpInput(String resourceId)
  {
    StreamInput streamInput = new StreamInput();
    streamInput.setSelectionMode(StreamSelectionMode.AUTO);
    streamInput.setInputPath("live");
    streamInput.setPosition(0);
    streamInput.setInputId(resourceId);
    return streamInput;
  }

  //#streamcreate

  private static String createH264Stream(String encodingId,
                                         String configurationId,
                                         String input)
  {
    StreamInput inputToStream = createStreamRtmpInput(input);
    Stream stream = new Stream();

    stream.setCodecConfigId(configurationId);
    stream.addInputStreamsItem(inputToStream);
    return bitmovinApi.encoding.encodings
            .streams.create(encodingId, stream).getId();
  }

  private static String createAacStream(String encodingId,
                                        String configId, String input)
  {
    StreamInput inputToStream = createStreamRtmpInput(input);
    Stream stream = new Stream();

    stream.setCodecConfigId(configId);
    stream.addInputStreamsItem(inputToStream);
    stream = bitmovinApi.encoding.encodings.streams.create(encodingId, stream);

    return stream.getId();
  }

  private enum createEncodingOutputVariableParameterIndex
  {CODEC, PER_TITLE_TEMPLATE, CONTAINER_FORMAT}
  private static EncodingOutput createEncodingOutput(
          String resourceId, String rootPath, String... pathComponents)
  {
    AclEntry aclEntry = new AclEntry();
    aclEntry.setPermission(AclPermission.PUBLIC_READ);

    EncodingOutput out = new EncodingOutput();
    out.addAclItem(aclEntry);
    out.setOutputId(resourceId);
    if( pathComponents.length > 0 )
      out.setOutputPath(Paths.get(
              rootPath,
              pathComponents[LiveEncoding
                      .createEncodingOutputVariableParameterIndex
                      .CODEC.ordinal()] +
                      "_" +
                      pathComponents[LiveEncoding
                              .createEncodingOutputVariableParameterIndex
                              .PER_TITLE_TEMPLATE.ordinal()],
              pathComponents[LiveEncoding
                      .createEncodingOutputVariableParameterIndex
                      .CONTAINER_FORMAT.ordinal()]
      ).toString());
    else out.setOutputPath( rootPath );

    return out;
  }

  //#muxing
  private static String createFmp4Muxing(String encodingId,
                                         EncodingOutput fmp4Output,
                                         String streamId)
  {
    Fmp4Muxing fmp4Muxing = new Fmp4Muxing();

    MuxingStream stream = new MuxingStream();
    stream.setStreamId(streamId);

    fmp4Muxing.setSegmentLength(4D);
    fmp4Muxing.addStreamsItem(stream);

    fmp4Muxing.addOutputsItem(fmp4Output);
    bitmovinApi.encoding.encodings.muxings.fmp4.create(encodingId, fmp4Muxing);
    return fmp4Muxing.getId();
  }

  //#encoding-start
  private static void startEncoding(String encodingId,
                                    LiveDashManifest dashManifest,
                                    LiveHlsManifest hlsManifest,
                                    String liveKey)
  {
    StartLiveEncodingRequest encodingIni = new StartLiveEncodingRequest();
    encodingIni.addDashManifestsItem(dashManifest);
    encodingIni.addHlsManifestsItem(hlsManifest);
    encodingIni.setStreamKey(liveKey);
//    encodingIni.addHlsManifestsItem(hlsManifest);
    bitmovinApi.encoding.encodings.live.start(encodingId, encodingIni);
  }

  private void awaitEncoding( String encodingId )
          throws InterruptedException
  {

    Task status;
    do
    {
      Thread.sleep(2500);
      status = bitmovinApi.encoding.encodings.status(encodingId);
    } while (status.getStatus() != Status.FINISHED && status.getStatus() != Status.ERROR);

    if (status.getStatus() == Status.ERROR)
    {
      logger.info("Encoding: An error has occurred");
      return;
    }

    logger.info("Encoding: finished successfully.");

  }

  //#manifest
  private static String createDashManifestDefault(
          String name, String encodingId, EncodingOutput out)
  {
    DashManifestDefault manifest = new DashManifestDefault();
    manifest.addOutputsItem(out);
    manifest.setEncodingId(encodingId);
    manifest.setManifestName(name);
    manifest.setVersion(DashManifestDefaultVersion.V1);
    return bitmovinApi.encoding.manifests.dash.defaultapi.create(manifest)
            .getId();
//    bitmovinApi.encoding.manifests.dash.start(manifest.getId());
//    return manifest.getId();
  }

  private static LiveDashManifest createLiveDashManifest(String manifestId,
                                                         double offset,
                                                         double timeshift)
  {
    LiveDashManifest manifestConfig = new LiveDashManifest();
    manifestConfig.setManifestId(manifestId);
    manifestConfig.setTimeshift(timeshift);
    manifestConfig.setLiveEdgeOffset(offset);
    return manifestConfig;
  }
  //#endclass

  private static String createHlsManifestDefault(
          String name, String encodingId, EncodingOutput out)
  {
    HlsManifestDefault manifest = new HlsManifestDefault();
    manifest.setEncodingId(encodingId);
    manifest.addOutputsItem(out);
    manifest.setName(name);
    manifest.setVersion(HlsManifestDefaultVersion.V1);
    return bitmovinApi.encoding.manifests
          .hls.defaultapi.create(manifest).getId();
  }

  private static LiveHlsManifest createLiveHlsManifest(String manifestId,
                                                         double timeshift)
  {
    LiveHlsManifest manifestConfig = new LiveHlsManifest();
    manifestConfig.setManifestId(manifestId);
    manifestConfig.setTimeshift(timeshift);
    return manifestConfig;
  }

}

