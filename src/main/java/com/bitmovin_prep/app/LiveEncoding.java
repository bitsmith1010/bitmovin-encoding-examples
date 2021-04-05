//
//234567890123456789012345678901234567890123456789012345678901234567890123456789
//
package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.common.BitmovinException;
import com.bitmovin.api.sdk.model.AacAudioConfiguration;
import com.bitmovin.api.sdk.model.EncodingOutput;
import com.bitmovin.api.sdk.model.H264VideoConfiguration;
import com.bitmovin.api.sdk.model.LiveDashManifest;
import com.bitmovin.api.sdk.model.LiveHlsManifest;
import com.bitmovin.api.sdk.model.PresetConfiguration;
import com.bitmovin.api.sdk.model.StartLiveEncodingRequest;
import com.bitmovin.api.sdk.model.StreamInput;
import com.bitmovin.api.sdk.model.StreamSelectionMode;
import java.io.IOException;
import java.util.Properties;

public class LiveEncoding extends BasicEncodingClient {

  public void execute() throws IOException
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
    logger.info("video config id: " + h264ConfigForLive2Id);

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
      String encodingId = createEncoding("encoding-per-title");
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

    EncodingOutput dashOut = createEncodingOutput(gcsOutId, rootPath);
    logger.info("manifest output: " + dashOut);

    // manifests created as platform resources, but files not generated
    // for live encodings this is done in the start encoding request.
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

  //#codecconfig
  private static String createH264ConfigForLive(
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
    return manifestConfig;
  }
}

