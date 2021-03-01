//2345678901234567890123456789012345678901234567890123456789012345678901234567890123456789
package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.model.AacAudioConfiguration;
import com.bitmovin.api.sdk.model.AclEntry;
import com.bitmovin.api.sdk.model.AclPermission;
import com.bitmovin.api.sdk.model.AutoRepresentation;
import com.bitmovin.api.sdk.model.DashManifestDefault;
import com.bitmovin.api.sdk.model.DashManifestDefaultVersion;
import com.bitmovin.api.sdk.model.Encoding;
import com.bitmovin.api.sdk.model.EncodingOutput;
import com.bitmovin.api.sdk.model.Fmp4Muxing;
import com.bitmovin.api.sdk.model.GcsInput;
import com.bitmovin.api.sdk.model.GcsOutput;
import com.bitmovin.api.sdk.model.H264PerTitleConfiguration;
import com.bitmovin.api.sdk.model.H264VideoConfiguration;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Properties;

public class SingleProcedureScript {
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

    BitmovinApi bitmovinApi = BitmovinApi.builder().withApiKey(
            config.getProperty("api_key")).build();
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

    // domain/bucket_name/encoding_tests/myprobe + /DATE
    String rootPath = Paths.get(config.getProperty("output_path"),
            new Date().toString().replace(" ", "_")).toString();

    Encoding encoding = new Encoding();
    encoding.setName("encoding-1");
    encoding.setDescription("per title encoding");
    String encodingId = bitmovinApi.encoding.encodings.create(encoding).getId();
    System.out.println( "encoding id: " + encodingId);

    // INPUT SOURCE
    StreamInput streamInput = new StreamInput();
    streamInput.setInputId(gcsInId);
    streamInput.setInputPath(config.getProperty("input_path"));
    streamInput.setSelectionMode(StreamSelectionMode.AUTO);

    // PRE- PER TITLE PROCESSING H264 CONFIGURATION
    H264VideoConfiguration h264ConfigurationInitial = new H264VideoConfiguration();
    h264ConfigurationInitial.setName("h264 configuration, per title");
    h264ConfigurationInitial.setPresetConfiguration(PresetConfiguration.VOD_STANDARD);
    h264ConfigurationInitial.setProfile(ProfileH264.HIGH);
    h264ConfigurationInitial.setWidth(1024);
    String h264ConfigurationInitialId = bitmovinApi.encoding.configurations
            .video.h264.create(h264ConfigurationInitial).getId();
    System.out.println( "h264 config initial: " + h264ConfigurationInitialId);

    // H264 STREAM AS A PRE- PER TITLE PROCESSING TEMPLATE
    Stream h264Stream = new Stream();
    h264Stream.addInputStreamsItem(streamInput);
    h264Stream.setCodecConfigId(h264ConfigurationInitialId);
    h264Stream.setMode(StreamMode.PER_TITLE_TEMPLATE);
    String h264StreamId =  bitmovinApi.encoding.encodings.streams
            .create(encodingId, h264Stream).getId();
    System.out.println("h264 stream id: " + h264StreamId);

    // H264 STREAM INPUT TO MUXING
    MuxingStream h264MuxingStream = new MuxingStream();
    h264MuxingStream.setStreamId(h264StreamId);

    AclEntry aclEntry = new AclEntry();
    aclEntry.setPermission(AclPermission.PUBLIC_READ);

    // OUTPUT OF H264 FMP4 MUXING
    EncodingOutput h264Fmp4Out = new EncodingOutput();
    h264Fmp4Out.setOutputPath(
            Paths.get(
                    rootPath,
                    "h264" + "_" + "{width}_{bitrate}_{uuid}",
                    "fmp4"
            ).toString()
    );
    h264Fmp4Out.setOutputId(gcsOutId);
    h264Fmp4Out.addAclItem(aclEntry);

    // FMP4 H264 MUXING
    Fmp4Muxing h264Fmp4Muxing = new Fmp4Muxing();
    h264Fmp4Muxing.addOutputsItem(h264Fmp4Out);
    h264Fmp4Muxing.addStreamsItem(h264MuxingStream);
    h264Fmp4Muxing.setSegmentLength(4D);
    String h264Fmp4MuxingId = bitmovinApi.encoding.encodings.muxings.fmp4
            .create(encodingId, h264Fmp4Muxing).getId();
    System.out.println("h264 fmp4 muxing: " + h264Fmp4MuxingId);

    // H264 CONFIGURATION EXTENDS BASE CONFIGURATION IN PER TITLE PROCESSING
    H264PerTitleConfiguration h264ConfigurationPerTitle = new H264PerTitleConfiguration();
    h264ConfigurationPerTitle.setAutoRepresentations(new AutoRepresentation());

    // PER TITLE ENCODING PROPERTIES OBJECT
    PerTitle perTitle = new PerTitle();
    perTitle.setH264Configuration(h264ConfigurationPerTitle);

    // **************   AUDIO CONFIGURATION ******************
    // *******************************************************

    // AAC 16k CONFIGURATION
    AacAudioConfiguration aacConfiguration = new AacAudioConfiguration();
    aacConfiguration.setName("aac 16k configuration");
    aacConfiguration.setBitrate(64000L);
    String aacConfigurationId = bitmovinApi.encoding.configurations
            .audio.aac.create(aacConfiguration).getId();
    System.out.println( "aac config : " + aacConfigurationId);

    // AAC STREAM
    Stream aacStream = new Stream();
    aacStream.addInputStreamsItem(streamInput);
    aacStream.setCodecConfigId(aacConfigurationId);
    String aacStreamId =  bitmovinApi.encoding.encodings.streams
            .create(encodingId, aacStream).getId();
    System.out.println("aac stream id: " + aacStreamId);

    // Aac STREAM INPUT TO MUXING
    MuxingStream aacMuxingStream = new MuxingStream();
    aacMuxingStream.setStreamId(aacStreamId);

//    AclEntry aclEntry = new AclEntry();
//    aclEntry.setPermission(AclPermission.PUBLIC_READ);

    // OUTPUT OF AAC FMP4 MUXING
    EncodingOutput aacFmp4Out = new EncodingOutput();
    aacFmp4Out.setOutputPath(Paths.get( rootPath, "aac" ).toString());
    aacFmp4Out.setOutputId(gcsOutId);
    aacFmp4Out.addAclItem(aclEntry);

    // FMP4 Aac MUXING
    Fmp4Muxing aacFmp4Muxing = new Fmp4Muxing();
    aacFmp4Muxing.addOutputsItem(aacFmp4Out);
    aacFmp4Muxing.addStreamsItem(aacMuxingStream);
    aacFmp4Muxing.setSegmentLength(4D);
    String aacFmp4MuxingId = bitmovinApi.encoding.encodings.muxings.fmp4
            .create(encodingId, aacFmp4Muxing).getId();
    System.out.println("aac fmp4 muxing: " + aacFmp4MuxingId);


    // ************************ END AUDIO CONFIGURATION*****************
    // *******************************************************


    // INITIATE ENCODING REQUEST CONFIGURATION WITH PER TITLE OPTIMIZATION
    StartEncodingRequest encodingInitialize = new StartEncodingRequest();
    encodingInitialize.setPerTitle(perTitle);

    bitmovinApi.encoding.encodings.start(encodingId, encodingInitialize);

    awaitEncoding(encodingId, bitmovinApi);

    // DASH MANIFEST OUTPUT
    // OUTPUT OF H264 FMP4 MUXING
    EncodingOutput dashOut = new EncodingOutput();
    dashOut.setOutputPath( rootPath );
    dashOut.setOutputId(gcsOutId);
    dashOut.addAclItem(aclEntry);

    // DASH MANIFEST
    DashManifestDefault dashManifest = new DashManifestDefault();
    dashManifest.setEncodingId(encodingId);
    dashManifest.setManifestName("manifest.mpd");
    dashManifest.setVersion(DashManifestDefaultVersion.V1);
    dashManifest.addOutputsItem(dashOut);
    String dashManifestId = bitmovinApi.encoding.manifests
            .dash.defaultapi.create(dashManifest).getId();
    System.out.println("manifest id: " + dashManifestId);

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

  }