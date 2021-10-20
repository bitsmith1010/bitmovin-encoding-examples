package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.model.CencDrm;
import com.bitmovin.api.sdk.model.CencMarlin;
import com.bitmovin.api.sdk.model.CencPlayReady;
import com.bitmovin.api.sdk.model.CencWidevine;
import com.bitmovin.api.sdk.model.StreamInput;
import com.bitmovin.api.sdk.model.MuxingStream;
import com.bitmovin.api.sdk.model.Fmp4Muxing;
import com.bitmovin.api.sdk.model.EncodingOutput;
import java.io.IOException;

public class EncodingWithCencDrm extends BasicEncodingClient {

    public EncodingWithCencDrm() throws IOException {
    }

    public void execute() throws Exception
    {

        //Properties config = getProperties();
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
                logger.info("out id: " + out1Id);
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
        }
        String in1Id = getIn1Id();

        /*
        String in1Id = !config.getProperty("input_resource_id").equals("") ?
          config.getProperty("input_resource_id") :
          createGcsInput("resource-in-1",
            config.getProperty("gcs_input_access"),
            config.getProperty("gcs_input_secret"),
            config.getProperty("input_bucket_name"))
            .getId();

         */
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

            // *** create muxing, then add DRM configuration and API request
            String h264MuxingId = createFmp4Muxing(encodingId, h264StreamId);
            logger.info("fmp4 h264 muxing id: " + h264MuxingId);
            String drmId = createCencDrmExpressPlay(
              config.getProperty("widevine_pssh_key"),
              config.getProperty("cenc_kid"),
              config.getProperty("cenc_key"),
              config.getProperty("playready_la_url"),
              encodingId, h264MuxingId, fmp4H264Out);
            logger.info("cenc drm (encoded muxing?) id: " + drmId);

            createFmp4Muxing(encodingId, fmp4AacOut, aacStreamId);

            startEncoding(encodingId);
            awaitEncoding(encodingId);
        }
        logger.info("encoding id: " + encodingId);

        EncodingOutput hlsOut = createEncodingOutput(out1Id, rootPath);
        String manifestHlsId = "";
        if (config.getProperty("gen_hls").equals("+")) {
            manifestHlsId = createHlsManifestDefault(
              "manifest.m3u8", encodingId, hlsOut);
            bitmovinApi.encoding.manifests.hls.start(manifestHlsId);
            logger.info("manifest id: " + manifestHlsId);
        }

        EncodingOutput dashOut = createEncodingOutput(out1Id, rootPath);
        String manifestDashId = "";
        if (config.getProperty("gen_dash").equals("+")) {
            manifestDashId = createDashManifestDefault(
              "manifest.mpd", encodingId, dashOut);
            bitmovinApi.encoding.manifests.dash.start(manifestDashId);
            logger.info("manifest id: " + manifestDashId);
        }

    }
    //#endmain

    public String createFmp4Muxing(String encodingId, String streamId)
    {
        Fmp4Muxing fmp4Muxing = new Fmp4Muxing();

        MuxingStream stream = new MuxingStream();
        stream.setStreamId(streamId);

        fmp4Muxing.setSegmentLength(4D);
        fmp4Muxing.addStreamsItem(stream);

        return bitmovinApi.encoding.encodings.muxings.fmp4.create(
          encodingId, fmp4Muxing).getId();
    }

    public String createCencDrmExpressPlay(
      String widevinePsshKey,
      String cencKid,
      String cencKey,
      String laUrl,
      String encodingId,
      String muxingId,
      EncodingOutput out)
    {
        CencDrm cencDrm = new CencDrm();
        cencDrm.addOutputsItem(out);
        cencDrm.setKid(cencKid);
        cencDrm.setKey(cencKey);
        CencWidevine widevineDrm = new CencWidevine();
        widevineDrm.setPssh(widevinePsshKey);
        cencDrm.setWidevine(widevineDrm);

       CencPlayReady playReady = new CencPlayReady();
       playReady.setLaUrl(laUrl);
       cencDrm.setPlayReady(playReady);

//       cencDrm.setMarlin(new CencMarlin());

        return bitmovinApi.encoding.encodings.muxings.fmp4.drm.cenc.create(
          encodingId, muxingId, cencDrm).getId();
    }
}
