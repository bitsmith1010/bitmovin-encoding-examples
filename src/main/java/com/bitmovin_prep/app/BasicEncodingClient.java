package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.model.AacAudioConfiguration;
import com.bitmovin.api.sdk.model.GcsInput;
import com.bitmovin.api.sdk.model.HlsManifestDefault;
import com.bitmovin.api.sdk.model.HlsManifestDefaultVersion;
import com.bitmovin.api.sdk.model.S3Output;
import com.bitmovin.api.sdk.model.StartEncodingRequest;
import com.bitmovin.api.sdk.model.StreamInput;
import com.bitmovin.api.sdk.model.StreamSelectionMode;
import com.bitmovin.api.sdk.model.MuxingStream;
import com.bitmovin.api.sdk.model.GcsOutput;
import com.bitmovin.api.sdk.model.CloudRegion;
import com.bitmovin.api.sdk.model.DashManifestDefault;
import com.bitmovin.api.sdk.model.DashManifestDefaultVersion;
import com.bitmovin.api.sdk.model.Encoding;
import com.bitmovin.api.sdk.model.Fmp4Muxing;
import com.bitmovin.api.sdk.model.AclEntry;
import com.bitmovin.api.sdk.model.AclPermission;
import com.bitmovin.api.sdk.model.EncodingOutput;
import com.bitmovin.api.sdk.model.H264VideoConfiguration;
import com.bitmovin.api.sdk.model.PresetConfiguration;
import com.bitmovin.api.sdk.model.Stream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import feign.Logger;
import feign.slf4j.Slf4jLogger;


public class BasicEncodingClient extends EncodingClientUtilities {

    public void execute() throws IOException, InterruptedException, Exception {

        Properties config = getProperties();
        logger.info("cofiguration file: " + config.toString());

        bitmovinApi = createBitmovinApi(config.getProperty("api_key"));
        logger.info("created an instance of bitmovin api " +
          bitmovinApi.toString());

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

        String in1Id = !config.getProperty("input_resource_id").equals("") ?
          config.getProperty("input_resource_id") :
          createGcsInput("resource-in-1",
            config.getProperty("gcs_input_access"),
            config.getProperty("gcs_input_secret"),
            config.getProperty("input_bucket_name"))
            .getId();
        logger.info("in id: " + in1Id);

        String h264ConfigurationId =
          !config.getProperty("h264_config_1_id").equals("") ?
            config.getProperty("h264_config_1_id") :
            createH264Configuration(
              "h264-1",
              Integer.parseInt(config.getProperty("h264_1_height")),
              Integer.parseInt(config.getProperty("h264_1_bitrate"))
            );
        logger.info("video config id: " + h264ConfigurationId);

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
            encodingId = createEncoding("encoding-basic");
            logger.info("encoding id: " + encodingId);

            StreamInput h264StreamInput = createStreamInput(
              config.getProperty("video_input_path"), in1Id);
            logger.info("input to video stream: " + h264StreamInput);

            StreamInput aacStreamInput = createStreamInput(
              config.getProperty("audio_input_path"), in1Id);
            logger.info("input to audio stream: " + aacStreamInput);

            String aacStreamId = createStream(encodingId,
              aacConfigurationId, aacStreamInput);
            logger.info("audio stream id: " + aacStreamId);

            String h264StreamId = createStream(encodingId,
              h264ConfigurationId, h264StreamInput);
            logger.info("video stream id: " + h264StreamId);

            //CODEC, RENDITION, CONTAINER_FORMAT
            EncodingOutput fmp4H264Out = createEncodingOutput(
              out1Id, rootPath,
              "h264",
              config.getProperty("h264_1_height") + "_" +
                config.getProperty("h264_1_bitrate"),
              "fmp4");
            logger.info("fmp4 h264 output: " + fmp4H264Out);

            EncodingOutput fmp4AacOut = createEncodingOutput(
              out1Id, rootPath, "aac", "16000", "fmp4");
            logger.info("fmp4 aac output: " + fmp4AacOut);

            createFmp4Muxing(encodingId, fmp4H264Out, h264StreamId);

            createFmp4Muxing(encodingId, fmp4AacOut, aacStreamId);

            startEncoding(encodingId);
            awaitEncoding(encodingId);
        }
        logger.info("encoding id: " + encodingId);

        EncodingOutput manifestOut = createEncodingOutput(out1Id, rootPath);

        String manifestId = "";
        switch (config.getProperty("manifest_type")) {
            case "hls":
                manifestId = createHlsManifestDefault(
                  "manifest.m3u8", encodingId, manifestOut);
                bitmovinApi.encoding.manifests.hls.start(manifestId);
                logger.info("manifest id: " + manifestId);
                break;
            case "dash":
                manifestId = createDashManifestDefault(
                  "manifest.mpd", encodingId, manifestOut);
                bitmovinApi.encoding.manifests.dash.start(manifestId);
                logger.info("manifest id: " + manifestId);
                break;
        }
    }
    //#endmain

    public BitmovinApi createBitmovinApi(String key)
    {
        return BitmovinApi.builder()
          .withApiKey(key)
          .withLogger(new Slf4jLogger(), feign.Logger.Level.FULL)
          .build();
    }

    public GcsInput createGcsInput(String name, String access, String secret,
                                   String bucketName)
    {
        GcsInput input = new GcsInput();
        input.setName(name);
        input.setAccessKey(access);
        input.setSecretKey(secret);
        input.setBucketName(bucketName);

        return bitmovinApi.encoding.inputs.gcs.create(input);
    }

    public GcsOutput createGcsOutput(String name, String access,
                                     String secret, String bucketName)
    {
        GcsOutput output = new GcsOutput();
        output.setName(name);
        output.setAccessKey(access);
        output.setSecretKey(secret);
        output.setBucketName(bucketName);

        return bitmovinApi.encoding.outputs.gcs.create(output);
    }

    public S3Output createOutS3(String name, String access,
                                    String secret, String bucketName)
    {
        S3Output output = new S3Output();
        output.setName(name);
        output.setAccessKey(access);
        output.setSecretKey(secret);
        output.setBucketName(bucketName);

        return bitmovinApi.encoding.outputs.s3.create(output);
    }

    public String createEncoding(String name)
    {
        Encoding encoding = new Encoding();
        encoding.setName(name);
        encoding.setCloudRegion(CloudRegion.AUTO);
        return bitmovinApi.encoding.encodings.create(encoding).getId();
    }

    public String createAacConfiguration(
      String name, long bitrate)
    {
        AacAudioConfiguration audioCodecConfiguration = new AacAudioConfiguration();
        audioCodecConfiguration.setName(name);
        audioCodecConfiguration.setBitrate(bitrate);

        return bitmovinApi.encoding.configurations
          .audio.aac.create(audioCodecConfiguration).getId();
    }


    public String createH264Configuration(
      String name, int height, long bitrate)
    {
        H264VideoConfiguration configuration = new H264VideoConfiguration();
        configuration.setName(name);
        configuration.setHeight(height);
        configuration.setBitrate(bitrate);
        configuration.setPresetConfiguration(PresetConfiguration.VOD_STANDARD);
        return bitmovinApi.encoding
          .configurations.video.h264.create(configuration).getId();
    }

    public StreamInput createStreamInput(String path, String resourceId)
    {
        StreamInput streamInput = new StreamInput();
        streamInput.setSelectionMode(StreamSelectionMode.AUTO);
        streamInput.setInputPath(path);
        streamInput.setInputId(resourceId);
        return streamInput;
    }

    public String createStream(String encodingId,
                               String configId,
                               StreamInput input)
    {
        Stream stream = new Stream();
        stream.setCodecConfigId(configId);
        stream.addInputStreamsItem(input);
        stream = bitmovinApi.encoding.encodings.streams.create(encodingId, stream);
        return stream.getId();
    }

    public String createFmp4Muxing(String encodingId,
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


    public String createHlsManifestDefault(
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

    public String createDashManifestDefault(
      String name, String encodingId, EncodingOutput out)
    {
        DashManifestDefault manifest = new DashManifestDefault();
        manifest.addOutputsItem(out);
        manifest.setEncodingId(encodingId);
        manifest.setManifestName(name);
        manifest.setVersion(DashManifestDefaultVersion.V1);
        return bitmovinApi.encoding.manifests.dash.defaultapi
          .create(manifest).getId();
    }

    public enum createEncodingOutputVariableParameterIndex
    {CODEC, RENDITION, CONTAINER_FORMAT}
    public EncodingOutput createEncodingOutput(
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
              pathComponents[BasicEncodingClient
                .createEncodingOutputVariableParameterIndex
                .CODEC.ordinal()] +
                "_" +
                pathComponents[BasicEncodingClient
                  .createEncodingOutputVariableParameterIndex
                  .RENDITION.ordinal()],
              pathComponents[BasicEncodingClient
                .createEncodingOutputVariableParameterIndex
                .CONTAINER_FORMAT.ordinal()]
            ).toString());
        else out.setOutputPath( rootPath );

        return out;
    }

    public void startEncoding(String encodingId)
    {

        StartEncodingRequest startEncodingRequest = new StartEncodingRequest();
        // this is suggested in the guide and example. Here an error resulted.
        // startEncodingRequest.setEncodingMode(EncodingMode.SINGLE_PASS);
        bitmovinApi.encoding.encodings.start(encodingId, startEncodingRequest);
    }


}
