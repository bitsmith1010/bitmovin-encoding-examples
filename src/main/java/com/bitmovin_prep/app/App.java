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
import com.bitmovin.api.sdk.model.EncodingMode;
import com.bitmovin.api.sdk.model.EncodingOutput;
import com.bitmovin.api.sdk.model.Fmp4Muxing;
import com.bitmovin.api.sdk.model.GcsInput;
import com.bitmovin.api.sdk.model.GcsOutput;
import com.bitmovin.api.sdk.model.H264PerTitleConfiguration;
import com.bitmovin.api.sdk.model.H264VideoConfiguration;
import com.bitmovin.api.sdk.model.Mp4Muxing;
import com.bitmovin.api.sdk.model.MuxingStream;
import com.bitmovin.api.sdk.model.PerTitle;
import com.bitmovin.api.sdk.model.PresetConfiguration;
import com.bitmovin.api.sdk.model.ProfileH264;
import com.bitmovin.api.sdk.model.StartEncodingRequest;
import com.bitmovin.api.sdk.model.Status;
import com.bitmovin.api.sdk.model.Stream;
import com.bitmovin.api.sdk.model.StreamInput;
import com.bitmovin.api.sdk.model.StreamMode;
import com.bitmovin.api.sdk.model.StreamSelectionMode;
import com.bitmovin.api.sdk.model.Task;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Hello world!
 *
 */
public class App {
  public void main() throws java.io.IOException, java.lang.InterruptedException
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

    BitmovinApi bitmovinApi = createBitmovinApi(config.getProperty("api_key"));
    System.out.println(bitmovinApi.toString());

//    GcsInput gcsInput = createGcsInput("resource-in-1",
//            config.getProperty("gcs_input_access"),
//            config.getProperty("gcs_input_secret"),
//            config.getProperty("input_bucket_name"), bitmovinApi);
//    System.out.println(gcsInput.getId());

    GcsInput gcsInput = bitmovinApi.encoding
            .inputs.gcs.get(config.getProperty("input_resource_id"));
    System.out.println("in id: " + gcsInput.getId());

//    GcsOutput gcsOutput = createGcsOutput("resource-out-1",
//            config.getProperty("gcs_output_access"),
//            config.getProperty("gcs_output_secret"),
//            config.getProperty("output_bucket_name"), bitmovinApi);
//    System.out.println(gcsOutput.getId());

    GcsOutput gcsOutput = bitmovinApi.encoding.outputs
            .gcs.get(config.getProperty("output_resource_id"));
    System.out.println("out id: " + gcsOutput.getId());

    Encoding encoding = createEncoding("encoding-per-title", bitmovinApi);
    System.out.println("encoding id: " + encoding.getId());

    StreamInput videoStreamInput = createStreamInput(
            config.getProperty("input_path"), config.getProperty("input_resource_id"));
    StreamInput audioStreamInput = createStreamInput(
            config.getProperty("input_path"), config.getProperty("input_resource_id"));
    System.out.println("input to video stream: " + videoStreamInput.getInputId());
    System.out.println("input to audio stream: " + audioStreamInput.getInputId());

    H264VideoConfiguration h264Configuration = createH264Configuration(
            "h264-1",
            Integer.parseInt(config.getProperty("h264_1_width")),
            Long.parseLong(config.getProperty("h264_1_bitrate")), bitmovinApi);

    System.out.println("video config id: " + h264Configuration.getId());

    AacAudioConfiguration aacConfiguration = createAacConfiguration(
            "aac-1",
            Long.parseLong(config.getProperty("aac_1_bitrate")), bitmovinApi);

    System.out.println("audio config id: " + aacConfiguration.getId());

    Stream audioStream = createAacStream(encoding.getId(), audioStreamInput,
            aacConfiguration.getId(), bitmovinApi);
    System.out.println("audio stream id: " + audioStream.getId());

    Stream videoStream = createH264Stream(encoding.getId(), videoStreamInput,
            h264Configuration.getId(), bitmovinApi);
    System.out.println("video stream id: " + videoStream.getId());

    EncodingOutput fmp4H264Out = createFmp4Muxing(encoding.getId(), gcsOutput.getId(),
            config.getProperty("output_path") +
                    new Date().toString().replace(" ", "_"),
            "h264", videoStream.getId(), "", bitmovinApi);
    System.out.println("fmp4 h264 output: " + fmp4H264Out.toString());

    EncodingOutput fmp4AacOut = createFmp4Muxing(encoding.getId(), gcsOutput.getId(),
            config.getProperty("output_path") +
                    new Date().toString().replace(" ", "_"),
            "aac", audioStream.getId(), "", bitmovinApi);
    System.out.println("mp4 encoding output: " + fmp4AacOut.toString());

//
//    createFmp4Muxing(encoding.getId(), gcsOutput.getId(),
//            config.getProperty("output_path") +
//                    new Date().toString().replace(" ", "_"),
//            audioStream.getId(), audioStream, bitmovinApi);

//    createMp4Muxing(encoding.getId(), gcsOutput.getId(),
//            config.getProperty("output_path") +
//                    new Date().toString().replace(" ", "_"),
//            videoStream.getId(), "", bitmovinApi);

//    EncodingOutput mp4Out = createMp4Muxing(encoding.getId(), gcsOutput.getId(),
//            config.getProperty("output_path") +
//                    new Date().toString().replace(" ", "_"),
//            videoStream.getId(), audioStream.getId(), bitmovinApi);

    startEncoding(encoding.getId(), bitmovinApi);
    awaitEncoding(encoding.getId(), bitmovinApi);
    createDashManifestDefault(encoding.getId(), fmp4AacOut, fmp4H264Out, bitmovinApi);

    //#endmain
  }

  public static BitmovinApi createBitmovinApi(String key) {
    return BitmovinApi.builder()
            .withApiKey(key).build();
  }

  public static GcsInput createGcsInput(String name, String access, String secret,
                                        String bucketName, BitmovinApi bitmovinApi) {
    GcsInput input = new GcsInput();
    input.setName(name);
    input.setAccessKey(access);
    input.setSecretKey(secret);
    input.setBucketName(bucketName);

    return bitmovinApi.encoding.inputs.gcs.create(input);
  }

  public static GcsOutput createGcsOutput(String name, String access, String secret,
                                          String bucketName, BitmovinApi bitmovinApi) {
    GcsOutput output = new GcsOutput();
    output.setName(name);
    output.setAccessKey(access);
    output.setSecretKey(secret);
    output.setBucketName(bucketName);

    return bitmovinApi.encoding.outputs.gcs.create(output);
  }

  public static Encoding createEncoding(String name, BitmovinApi bitmovinApi) {
    Encoding encoding = new Encoding();
    encoding.setName(name);
    encoding.setCloudRegion(CloudRegion.AUTO);
    return bitmovinApi.encoding.encodings.create(encoding);
  }

  //#VIDEO-CONFIG
  public static H264VideoConfiguration createH264Configuration(
          String name, int width, long bitrate, BitmovinApi bitmovinApi) {
    H264VideoConfiguration videoCodecConfiguration = new H264VideoConfiguration();
    videoCodecConfiguration.setName(name);
    videoCodecConfiguration.setProfile(ProfileH264.HIGH);
//    videoCodecConfiguration.setBitrate(1500000L);
//    videoCodecConfiguration.setWidth(1024);
    videoCodecConfiguration.setPresetConfiguration(PresetConfiguration.VOD_STANDARD);

    return bitmovinApi.encoding.configurations.video.h264.create(videoCodecConfiguration);
  }

  public static AacAudioConfiguration createAacConfiguration(
          String name, long bitrate, BitmovinApi bitmovinApi) {
    AacAudioConfiguration audioCodecConfiguration = new AacAudioConfiguration();
    audioCodecConfiguration.setName(name);
    audioCodecConfiguration.setBitrate(bitrate);

    return bitmovinApi.encoding.configurations
            .audio.aac.create(audioCodecConfiguration);
  }

  public static StreamInput createStreamInput(String path, String resourceId) {
    StreamInput streamInput = new StreamInput();
    streamInput.setSelectionMode(StreamSelectionMode.AUTO);
    streamInput.setInputPath(path);
    streamInput.setInputId(resourceId);
    return streamInput;
  }

  public static Stream createAacStream(String encodingId, StreamInput streamInput,
                                          String configId,
                                          BitmovinApi bitmovinApi) {
    Stream stream = new Stream();
    stream.setCodecConfigId(configId);
    stream.addInputStreamsItem(streamInput);
    stream = bitmovinApi.encoding.encodings.streams.create(encodingId, stream);
    return stream;
  }

  public static Stream createH264Stream(String encodingId,
                                              StreamInput streamInput,
                                              String configurationId, BitmovinApi bitmovinApi)
  {
    Stream stream = new Stream();
    stream.setCodecConfigId(configurationId);
    stream.addInputStreamsItem(streamInput);
    stream.setMode(StreamMode.PER_TITLE_TEMPLATE);
    return bitmovinApi.encoding.encodings.streams.create(encodingId, stream);
  }

  //#MUXING
  public static EncodingOutput createFmp4Muxing(String encodingId,
                                                String resourceId, String basePath,
                                      String codecName,
                                      String videoStreamId, String audioStreamId,
                                      BitmovinApi bitmovinApi)
  {
    AclEntry aclEntry = new AclEntry();
    aclEntry.setPermission(AclPermission.PUBLIC_READ);
    List<AclEntry> aclEntryList = new ArrayList<AclEntry>();
    aclEntryList.add(aclEntry);

    EncodingOutput fmp4Output = new EncodingOutput();
    fmp4Output.setAcl(aclEntryList);
    fmp4Output.setOutputId(resourceId);
    fmp4Output.setOutputPath(String.format(
            "%s/%s_{width}_{bitrate}_{uuid}/fmp4", basePath, codecName));

    Fmp4Muxing fmp4Muxing = new Fmp4Muxing();

    MuxingStream conf1 = new MuxingStream();
//    MuxingStream conf2 = new MuxingStream();
    conf1.setStreamId(videoStreamId);
//    conf2.setStreamId(audioStreamId);

    fmp4Muxing.setSegmentLength(4D);
    fmp4Muxing.addStreamsItem(conf1);
//    fmp4Muxing.addStreamsItem(conf2);

    fmp4Muxing.addOutputsItem(fmp4Output);
    bitmovinApi.encoding.encodings.muxings.fmp4.create(encodingId, fmp4Muxing);
    return fmp4Output;
    //bitmovinApi.encoding.muxing.addMp4MuxingToEncoding(encoding, fmp4Muxing);
}

  public static EncodingOutput createMp4Muxing(String encodingId, String resourceId,
                                               String basePath,
                                       String videoStreamId, String audioStreamId,
                                       BitmovinApi bitmovinApi)
  {
    AclEntry aclEntry = new AclEntry();
    aclEntry.setPermission(AclPermission.PUBLIC_READ);
    List<AclEntry> aclEntryList = new ArrayList<AclEntry>();
    aclEntryList.add(aclEntry);

    EncodingOutput mp4EncodingOutput = new EncodingOutput();
    mp4EncodingOutput.setAcl(aclEntryList);
    mp4EncodingOutput.setOutputId(resourceId);
    mp4EncodingOutput.setOutputPath(String.format(
            "%s{width}_{bitrate}_{uuid}/mp4", basePath));

    Mp4Muxing mp4Muxing = new Mp4Muxing();
    MuxingStream conf1 = new MuxingStream();
//    MuxingStream conf2 = new MuxingStream();

    conf1.setStreamId(videoStreamId);
//    conf2.setStreamId(audioStreamId);

    mp4Muxing.addStreamsItem(conf1);
//    mp4Muxing.addStreamsItem(conf2);
    mp4Muxing.setFilename("per_title_mp4.mp4");


//    fmp4Muxing.addStreamsItem(new MuxingStream().setStreamId(videoStreamId));
//    fmp4Muxing.addStreamsItem(new MuxingStream().setStreamId(audioStreamId));

//    fmp4Muxing.setStreams(
//            Arrays.asList(
//                    new MuxingStream(videoStream.getId()),
//                    new MuxingStream(audioStream.getId())
//            )
//    );
//    fmp4Muxing.set .setFilename("per_title_mp4.mp4");
    mp4Muxing.addOutputsItem(mp4EncodingOutput);
    bitmovinApi.encoding.encodings.muxings.mp4.create(encodingId, mp4Muxing);
    //bitmovinApi.encoding.muxing.addMp4MuxingToEncoding(encoding, mp4Muxing);
    return mp4EncodingOutput;
  }

  //#ENCODING-OUTPUT
  public static void startEncoding(String encodingId, BitmovinApi bitmovinApi)
  {
    AutoRepresentation autoRepresentation = new AutoRepresentation();

    H264PerTitleConfiguration h264PerTitleConfiguration = new H264PerTitleConfiguration();
    h264PerTitleConfiguration.setAutoRepresentations(autoRepresentation);

    PerTitle perTitle = new PerTitle();
    perTitle.setH264Configuration(h264PerTitleConfiguration);

    StartEncodingRequest startEncodingRequest = new StartEncodingRequest();
    startEncodingRequest.setPerTitle(perTitle);
//    startEncodingRequest.setEncodingMode(EncodingMode.THREE_PASS);

    bitmovinApi.encoding.encodings.start(encodingId, startEncodingRequest);
  }

  public void awaitEncoding( String encodingId, BitmovinApi bitmovinApi )
          throws java.lang.InterruptedException
  {

    Task status;
    do
    {
      Thread.sleep(2500);
      status = bitmovinApi.encoding.encodings.status(encodingId);
    } while (status.getStatus() != Status.FINISHED && status.getStatus() != Status.ERROR);

    if (status.getStatus() == Status.ERROR)
    {
      System.out.println("An error has occurred");
      return;
    }

    System.out.println("Encoding has been finished successfully.");

  }

  public void createDashManifestDefault(String encodingId, EncodingOutput encodingOutput1,
          EncodingOutput encodingOutput2, BitmovinApi bitmovinApi)
  {
    DashManifestDefault manifest = new DashManifestDefault();
    manifest.setEncodingId(encodingId);
    manifest.setManifestName("manifest.mpd");
    manifest.setVersion(DashManifestDefaultVersion.V1);
//    dashManifestDefault.addOutputsItem(buildEncodingOutput(output, outputPath));
    manifest.addOutputsItem(encodingOutput1);
    manifest.addOutputsItem(encodingOutput2);
    manifest = bitmovinApi.encoding.manifests.dash.defaultapi.create(manifest);
//  executeDashManifestCreation(dashManifestDefault);
    bitmovinApi.encoding.manifests.dash.start(manifest.getId());
  }

}