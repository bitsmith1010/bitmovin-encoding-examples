//2345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.model.AacAudioConfiguration;
import com.bitmovin.api.sdk.model.AclEntry;
import com.bitmovin.api.sdk.model.AclPermission;
import com.bitmovin.api.sdk.model.AutoRepresentation;
import com.bitmovin.api.sdk.model.CloudRegion;
import com.bitmovin.api.sdk.model.DashManifestDefault;
import com.bitmovin.api.sdk.model.DashManifestDefaultVersion;
import com.bitmovin.api.sdk.model.Encoding;
//import com.bitmovin.api.sdk.model.EncodingMode;
import com.bitmovin.api.sdk.model.EncodingOutput;
import com.bitmovin.api.sdk.model.Fmp4Muxing;
import com.bitmovin.api.sdk.model.GcsInput;
import com.bitmovin.api.sdk.model.GcsOutput;
import com.bitmovin.api.sdk.model.H264PerTitleConfiguration;
import com.bitmovin.api.sdk.model.H264VideoConfiguration;
import com.bitmovin.api.sdk.model.MuxingStream;
import com.bitmovin.api.sdk.model.PerTitle;
import com.bitmovin.api.sdk.model.PresetConfiguration;
//import com.bitmovin.api.sdk.model.ProfileH264;
import com.bitmovin.api.sdk.model.StartEncodingRequest;
import com.bitmovin.api.sdk.model.Status;
import com.bitmovin.api.sdk.model.Stream;
import com.bitmovin.api.sdk.model.StreamInput;
import com.bitmovin.api.sdk.model.StreamMode;
import com.bitmovin.api.sdk.model.StreamSelectionMode;
import com.bitmovin.api.sdk.model.Task;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class App {
  public void main() throws IOException, InterruptedException
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

    System.out.printf("cofiguration file: %s\n", config.toString());

    BitmovinApi bitmovinApi = createBitmovinApi(config.getProperty("api_key"));
    System.out.println(bitmovinApi.toString());

    String gcsInId = ! config.getProperty("input_resource_id").equals("") ?
            config.getProperty("input_resource_id") :
            createGcsInput("resource-in-1",
                    config.getProperty("gcs_input_access"),
                    config.getProperty("gcs_input_secret"),
                    config.getProperty("input_bucket_name"), bitmovinApi)
            .getId();
    System.out.println("in id: " + gcsInId);

    String gcsOutId = ! config.getProperty("output_resource_id").equals("") ?
            config.getProperty("output_resource_id") :
            createGcsOutput("resource-out-1",
                    config.getProperty("gcs_output_access"),
                    config.getProperty("gcs_output_secret"),
                    config.getProperty("output_bucket_name"), bitmovinApi)
            .getId();
    System.out.println("out id: " + gcsOutId);

    String encodingId = createEncoding("encoding-per-title", bitmovinApi);
    System.out.println("encoding id: " + encodingId);

    StreamInput h264StreamInput = createStreamInput(
            config.getProperty("input_path"), gcsInId);
    System.out.println("input to video stream: " + h264StreamInput.getInputId());

    StreamInput aacStreamInput = createStreamInput(
            config.getProperty("input_path"), gcsInId);
    System.out.println("input to audio stream: " + aacStreamInput.getInputId());

    String h264ConfigurationId = createH264Configuration(
            "h264-1",
            Integer.parseInt(config.getProperty("h264_1_width")),
            bitmovinApi);
    System.out.println("video config id: " + h264ConfigurationId);

    String aacConfigurationId = createAacConfiguration(
            "aac-1",
            Long.parseLong(config.getProperty("aac_1_bitrate")), bitmovinApi);
    System.out.println("audio config id: " + aacConfigurationId);

    String aacStreamId = createAacStream(encodingId, aacStreamInput,
            aacConfigurationId, bitmovinApi);
    System.out.println("audio stream id: " + aacStreamId);

    String h264StreamId = createH264Stream(encodingId, h264StreamInput,
            h264ConfigurationId, bitmovinApi);
    System.out.println("video stream id: " + h264StreamId);

    // rootPath format: domain/bucket_name/encoding_tests/myprobe + /DATE
    String rootPath = Paths.get(config.getProperty("output_path"),
            new Date().toString().replace(" ", "_")).toString();

    EncodingOutput fmp4H264Out = createEncodingOutput(
            gcsOutId, rootPath, "h264",
            "{width}_{bitrate}_{uuid}", "fmp4");
    System.out.println("fmp4 h264 output: " + fmp4H264Out);

    EncodingOutput fmp4AacOut = createEncodingOutput(
            gcsOutId, rootPath, "aac", "16000", "fmp4");
    System.out.println("fmp4 aac output: " + fmp4AacOut);

    createFmp4Muxing(encodingId, fmp4H264Out, h264StreamId, bitmovinApi);

    createFmp4Muxing(encodingId, fmp4AacOut, aacStreamId, bitmovinApi);

    startEncoding(encodingId, bitmovinApi);
    awaitEncoding(encodingId, bitmovinApi);

    EncodingOutput manifestOut = createEncodingOutput(gcsOutId, rootPath);
    String manifestId = createDashManifestDefault("manifest.mpd", encodingId,
      manifestOut, bitmovinApi);
    System.out.println("manifest id: " + manifestId);

    //#endmain
  }

  private static BitmovinApi createBitmovinApi(String key) {
    return BitmovinApi.builder()
            .withApiKey(key).build();
  }

  private static GcsInput createGcsInput(String name, String access, String secret,
                                        String bucketName, BitmovinApi bitmovinApi) {
    GcsInput input = new GcsInput();
    input.setName(name);
    input.setAccessKey(access);
    input.setSecretKey(secret);
    input.setBucketName(bucketName);

    return bitmovinApi.encoding.inputs.gcs.create(input);
  }

  private static GcsOutput createGcsOutput(String name, String access, String secret,
                                          String bucketName, BitmovinApi bitmovinApi) {
    GcsOutput output = new GcsOutput();
    output.setName(name);
    output.setAccessKey(access);
    output.setSecretKey(secret);
    output.setBucketName(bucketName);

    return bitmovinApi.encoding.outputs.gcs.create(output);
  }

  private static String createEncoding(String name, BitmovinApi bitmovinApi) {
    Encoding encoding = new Encoding();
    encoding.setName(name);
    encoding.setCloudRegion(CloudRegion.AUTO);
    return bitmovinApi.encoding.encodings.create(encoding).getId();
  }

  //#codecconfig
  private static String createH264Configuration(
          String name, int width, BitmovinApi bitmovinApi) {
    H264VideoConfiguration configuration = new H264VideoConfiguration();
    configuration.setName(name);
//    videoCodecConfiguration.setProfile(ProfileH264.HIGH);
    // as it appears, it's possible to set a target resolution
    configuration.setWidth(1024);
    configuration.setPresetConfiguration(PresetConfiguration.VOD_STANDARD);

    return bitmovinApi.encoding
            .configurations.video.h264.create(configuration).getId();
  }

  private static String createAacConfiguration(
          String name, long bitrate, BitmovinApi bitmovinApi) {
    AacAudioConfiguration audioCodecConfiguration = new AacAudioConfiguration();
    audioCodecConfiguration.setName(name);
    audioCodecConfiguration.setBitrate(bitrate);

    return bitmovinApi.encoding.configurations
            .audio.aac.create(audioCodecConfiguration).getId();
  }

  private static StreamInput createStreamInput(String path, String resourceId) {
    StreamInput streamInput = new StreamInput();
    streamInput.setSelectionMode(StreamSelectionMode.AUTO);
    streamInput.setInputPath(path);
    streamInput.setInputId(resourceId);
    return streamInput;
  }

  //#streamcreate
  private static String createAacStream(String encodingId, StreamInput streamInput,
                                          String configId, BitmovinApi bitmovinApi) {
    Stream stream = new Stream();
    stream.setCodecConfigId(configId);
    stream.addInputStreamsItem(streamInput);
    stream = bitmovinApi.encoding.encodings.streams.create(encodingId, stream);
    return stream.getId();
  }

  private static String createH264Stream(String encodingId, StreamInput streamInput,
                                        String configurationId, BitmovinApi bitmovinApi)
  {
    Stream stream = new Stream();
    stream.setCodecConfigId(configurationId);
    stream.addInputStreamsItem(streamInput);
    stream.setMode(StreamMode.PER_TITLE_TEMPLATE);
    return bitmovinApi.encoding.encodings.streams.create(encodingId, stream).getId();
  }

  private enum createEncodingOutputVariableParameterIndex
    {CODEC, PER_TITLE_TEMPLATE, CONTAINER_FORMAT}
  private static EncodingOutput createEncodingOutput(
          String resourceId, String rootPath, String... pathComponents)
  {
    AclEntry aclEntry = new AclEntry();
    aclEntry.setPermission(AclPermission.PUBLIC_READ);
    List<AclEntry> aclEntryList = new ArrayList<AclEntry>();
    aclEntryList.add(aclEntry);

    EncodingOutput out = new EncodingOutput();
    out.setAcl(aclEntryList);
    out.setOutputId(resourceId);
    if( pathComponents.length > 0 ) {
      out.setOutputPath(Paths.get(
              rootPath,
              pathComponents[
                      createEncodingOutputVariableParameterIndex
                              .CODEC.ordinal()] + "_" + pathComponents[
                              createEncodingOutputVariableParameterIndex
                                      .PER_TITLE_TEMPLATE.ordinal()],
              pathComponents[createEncodingOutputVariableParameterIndex
                      .CONTAINER_FORMAT.ordinal()]
      ).toString());
    }
    else out.setOutputPath( rootPath );

    return out;
  }

  //#muxing
  private static String createFmp4Muxing(String encodingId,
                                                EncodingOutput fmp4Output,
                                      String streamId, BitmovinApi bitmovinApi)
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

  private static PerTitle createPerTitle()
  {
    AutoRepresentation autoRepresentation = new AutoRepresentation();

    H264PerTitleConfiguration h264PerTitleConfiguration = new H264PerTitleConfiguration();
    h264PerTitleConfiguration.setAutoRepresentations(autoRepresentation);

    PerTitle perTitle = new PerTitle();
    perTitle.setH264Configuration(h264PerTitleConfiguration);

    return perTitle;
  }

    //#encoding-start
  private static void startEncoding(String encodingId, BitmovinApi bitmovinApi)
  {

    StartEncodingRequest startEncodingRequest = new StartEncodingRequest();
    startEncodingRequest.setPerTitle(createPerTitle());

    // this is suggested in the guide and example. Here an error resulted.
    // startEncodingRequest.setEncodingMode(EncodingMode.SINGLE_PASS);

    bitmovinApi.encoding.encodings.start(encodingId, startEncodingRequest);
  }

  private void awaitEncoding( String encodingId, BitmovinApi bitmovinApi )
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
      System.out.println("Encoding: An error has occurred");
      return;
    }

    System.out.println("Encoding: finished successfully.");

  }

  //#manifest
  private static String createDashManifestDefault(
          String name, String encodingId, EncodingOutput out, BitmovinApi bitmovinApi)
  {
    DashManifestDefault manifest = new DashManifestDefault();

    manifest.addOutputsItem(out);
    manifest.setEncodingId(encodingId);
    manifest.setManifestName(name);
    manifest.setVersion(DashManifestDefaultVersion.V1);
//    dashManifestDefault.addOutputsItem(buildEncodingOutput(output, outputPath));
//    manifest.addOutputsItem(encodingOutput1);
//    manifest.addOutputsItem(encodingOutput2);
    manifest = bitmovinApi.encoding.manifests.dash.defaultapi.create(manifest);
//  executeDashManifestCreation(dashManifestDefault);
    bitmovinApi.encoding.manifests.dash.start(manifest.getId());
    return manifest.getId();
  }

}
