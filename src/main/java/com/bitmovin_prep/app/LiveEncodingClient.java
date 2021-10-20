//
//234567890123456789012345678901234567890123456789012345678901234567890123456789
//
package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.common.BitmovinException;
import com.bitmovin.api.sdk.model.AacAudioConfiguration;
import com.bitmovin.api.sdk.model.CloudRegion;
import com.bitmovin.api.sdk.model.Encoding;
import com.bitmovin.api.sdk.model.EncodingOutput;
import com.bitmovin.api.sdk.model.H264VideoConfiguration;
import com.bitmovin.api.sdk.model.LiveDashManifest;
import com.bitmovin.api.sdk.model.LiveHlsManifest;
import com.bitmovin.api.sdk.model.PresetConfiguration;
import com.bitmovin.api.sdk.model.StartLiveEncodingRequest;
import com.bitmovin.api.sdk.model.StreamInput;
import com.bitmovin.api.sdk.model.StreamSelectionMode;
import com.bitmovin.api.sdk.model.Thumbnail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class LiveEncodingClient extends BasicEncodingClient {

  public LiveEncodingClient() throws IOException {
  }

  public void execute() throws IOException
  {

    Properties config = getProperties();
    logger.info("cofiguration file: " + config.toString());

    bitmovinApi = createBitmovinApi(config.getProperty("api_key"));
    logger.info("created an instance of bitmovin api " +
            bitmovinApi.toString());

    String inRtmpId = getRtmpInputId();

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
        out1Id = !config.getProperty("out_1_id").equals("") ?
          config.getProperty("out_1_id") :
          createGcsOutput("resource-out-1",
            config.getProperty("gcs_output_access"),
            config.getProperty("gcs_output_secret"),
            config.getProperty("output_bucket_name"))
            .getId();
        logger.info("out id: " + out1Id);
        break;
      case "GCP-SERVICE-ACCOUNT":
        out1Id = !config.getProperty("out_1_id").equals("") ?
          config.getProperty("out_1_id") :
          createGcsServiceAccountOutput(
            "resource-out-1",
            config.getProperty("gcs_output_access"),
            config.getProperty("gcs_output_secret"),
            config.getProperty("output_bucket_name"));
        logger.info("out id: " + out1Id);
        break;
    }

    String h264ConfigForLiveId =
            ! config.getProperty("h264_config_1_id").equals("") ?
                    config.getProperty("h264_config_1_id") :
                    createH264ConfigForLive(
                            "h264-1",
                            Integer.parseInt(config.getProperty("h264_1_height")),
                            Long.parseLong(config.getProperty("h264_1_bitrate"))
                    );
    logger.info("video config id: " + h264ConfigForLiveId);

    String h264ConfigForLive2Id =
            ! config.getProperty("h264_config_2_id").equals("") ?
                    config.getProperty("h264_config_2_id") :
                    createH264ConfigForLive(
                            "h264-2",
                            Integer.parseInt(config.getProperty("h264_2_height")),
                            Long.parseLong(config.getProperty("h264_2_bitrate"))
                    );
    logger.info("video config 2 id: " + h264ConfigForLive2Id);

    String aacConfigForLiveId =
            ! config.getProperty("aac_config_1_id").equals("") ?
                    config.getProperty("aac_config_1_id") :
                    createAacConfigForLive(
                            "aac-1",
                            Long.parseLong(config.getProperty("aac_1_bitrate")),
                            Double.parseDouble(config.getProperty(
                                    "aac_1_sample_rate"))
                    );
    logger.info("audio config id: " + aacConfigForLiveId);

    // ----- distinct in live encoding ---- there is no use of
    //   this procedure for the purpose of to generate a manifest as
    //   the manifest is generated as part of the encoding process.
    String encodingId = "";
    if (config.getProperty("static_ip_id").equals(""))
      encodingId = createEncoding(config.getProperty("encoding_name"));
    else encodingId = createEncoding(
      config.getProperty("encoding_name"),
      config.getProperty("static_ip_id")
    );
    logger.info("encoding id: " + encodingId);

    StreamInput inputToH264Stream = createStreamRtmpInput(
        inRtmpId,
        Integer.parseInt(config.getProperty(
            "video_source_track_index"))
    );
    StreamInput inputToH264Stream2 = createStreamRtmpInput(
        inRtmpId,
        Integer.parseInt(config.getProperty(
            "video_source_track_index"))
    );
    StreamInput inputToAacStream = createStreamRtmpInput(
        inRtmpId,
        Integer.parseInt(config.getProperty(
            "audio_source_track_index"))
    );

    String h264StreamId = createStream(encodingId,
            h264ConfigForLiveId, inputToH264Stream);
    logger.info("video stream id: " + h264StreamId);

    String h264Stream2Id = createStream(encodingId,
            h264ConfigForLive2Id, inputToH264Stream2);
    logger.info("video stream 2 id: " + h264Stream2Id);

    String aacStreamId = createStream(encodingId,
            aacConfigForLiveId, inputToAacStream);
    logger.info("audio stream id: " + aacStreamId);

    String rootPath =
            ! config.getProperty("encoding_root_path").equals("") ?
                    config.getProperty("encoding_root_path") :
                    createRootPath(config);

    EncodingOutput fmp4H264Out = createEncodingOutput(
            out1Id, rootPath, "h264",
            config.getProperty("h264_1_height") + "_" +
                    config.getProperty("h264_1_bitrate"), "fmp4");
    logger.info("fmp4 h264 output: " + fmp4H264Out);

    EncodingOutput fmp4H264Out2 = createEncodingOutput(
            out1Id, rootPath, "h264",
            config.getProperty("h264_2_height") + "_" +
                    config.getProperty("h264_2_bitrate"), "fmp4");
    logger.info("fmp4 h264 output 2: " + fmp4H264Out);

    EncodingOutput fmp4AacOut = createEncodingOutput(
            out1Id, rootPath, "aac",
            config.getProperty("aac_1_bitrate"), "fmp4");
    logger.info("fmp4 aac output: " + fmp4AacOut);

    createFmp4Muxing(encodingId, fmp4H264Out, h264StreamId);

    createFmp4Muxing(encodingId, fmp4H264Out2, h264Stream2Id);

    createFmp4Muxing(encodingId, fmp4AacOut, aacStreamId);

//                // THUMBNAILS tested 04-05-21
//                List<EncodingOutput> thumbnailOut = new ArrayList<>();
//                thumbnailOut.add(fmp4H264Out);
//                String thumbnailId = createThumbnails(
//                            thumbnailOut, encodingId, h264StreamId);
//                logger.info("thumbnail ID: " + thumbnailId);

    EncodingOutput dashOut = createEncodingOutput(out1Id, rootPath);
    logger.info("manifest output: " + dashOut);

    // manifests created as platform resources, but files not generated
    // for live encodings this is done in the start encoding request.
    String dashId = createDashManifestDefault("manifest.mpd",
            encodingId, dashOut);
    logger.info("manifest id: " + dashId);

    LiveDashManifest liveDashManifestId = createLiveDashManifest(
            dashId, 90D, 300D);

    EncodingOutput hlsOut = createEncodingOutput(out1Id, rootPath);
    logger.info("manifest output: " + hlsOut);

    String hlsId = createHlsManifestDefault(
            "manifest.m3u8", encodingId, hlsOut);
    logger.info("manifest id: " + dashId);

    LiveHlsManifest liveHlsManifestId = createLiveHlsManifest(
            hlsId, 3600D);

    // ----- initialize encoding, generate manifest files---------
    //          -----------------------------------------

    startLiveEncoding(encodingId, liveDashManifestId, liveHlsManifestId,
            config.getProperty("live_key"));

    //#endmain
  }

  private static String getRtmpInputId()
          throws BitmovinException
  {
    return bitmovinApi.encoding.inputs.rtmp.list().getItems().get(0).getId();
  }

  public String createEncoding(
    String name, String ipId)
  {
    Encoding encoding = new Encoding();
    encoding.setStaticIpId(ipId);
    encoding.setName(name);
    encoding.setCloudRegion(CloudRegion.AUTO);
    return bitmovinApi.encoding.encodings.create(encoding).getId();
  }

  //#codecconfig
  private static String createH264ConfigForLive(
          String name, int height, long bitrate)
  {
    H264VideoConfiguration configuration = new H264VideoConfiguration();
    configuration.setName(name);
    configuration.setHeight(height);
    configuration.setWidth( (int)(Math.ceil(height * 16/9.0 * 1/2) * 2));
    configuration.setBitrate(bitrate);
    configuration.setPresetConfiguration(PresetConfiguration.LIVE_STANDARD);

    return bitmovinApi.encoding
            .configurations.video.h264.create(configuration).getId();
  }

  private static String createAacConfigForLive(
          String name, long bitrate, double sampleRate)
  {
    AacAudioConfiguration audioCodecConfiguration = new AacAudioConfiguration();
    audioCodecConfiguration.setName(name);
    audioCodecConfiguration.setBitrate(bitrate);
    audioCodecConfiguration.setRate(sampleRate);
    return bitmovinApi.encoding.configurations
            .audio.aac.create(audioCodecConfiguration).getId();
  }

  private static StreamInput createStreamRtmpInput(
          String resourceId, int trackIndex)
  {
    StreamInput streamInput = new StreamInput();
    streamInput.setSelectionMode(StreamSelectionMode.AUTO);
    streamInput.setInputPath("live");
    streamInput.setPosition(trackIndex);
    streamInput.setInputId(resourceId);
    return streamInput;
  }

  private static void startLiveEncoding(
          String encodingId, LiveDashManifest dashManifest,
          LiveHlsManifest hlsManifest, String liveKey)
  {
    StartLiveEncodingRequest encodingIni = new StartLiveEncodingRequest();
    encodingIni.addDashManifestsItem(dashManifest);
    encodingIni.addHlsManifestsItem(hlsManifest);
    encodingIni.setStreamKey(liveKey);
//    encodingIni.addHlsManifestsItem(hlsManifest);
    bitmovinApi.encoding.encodings.live.start(encodingId, encodingIni);
  }

  private static LiveDashManifest createLiveDashManifest(
          String manifestId, double offset, double timeshift)
  {
    LiveDashManifest manifestConfig = new LiveDashManifest();
    manifestConfig.setManifestId(manifestId);
    manifestConfig.setTimeshift(timeshift);
    manifestConfig.setLiveEdgeOffset(offset);
    return manifestConfig;
  }

  private static LiveHlsManifest createLiveHlsManifest(
          String manifestId, double timeshift)
  {
    LiveHlsManifest manifestConfig = new LiveHlsManifest();
    manifestConfig.setManifestId(manifestId);
    manifestConfig.setTimeshift(timeshift);
    // --- ADDED - SEGMENT TO EDGE TEST ---
    manifestConfig.setLiveEdgeOffset(10D);
    return manifestConfig;
  }

  private String createThumbnails(
    List<EncodingOutput> out, String encodingId, String streamId)
  {
    Thumbnail thumbnail = new Thumbnail();
    thumbnail.setHeight(320);
    thumbnail.setName("thumbnails-test1");
    thumbnail.setOutputs(out);
    thumbnail.setPattern("thumbnail-%number%.png");
    thumbnail.setInterval(4.0);

    return bitmovinApi.encoding.encodings.streams.thumbnails.create(
      encodingId, streamId, thumbnail).getId();
  }

}