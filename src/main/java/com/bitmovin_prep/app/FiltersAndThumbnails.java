package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.common.BitmovinException;
import com.bitmovin.api.sdk.model.AacAudioConfiguration;
import com.bitmovin.api.sdk.model.Filter;
import com.bitmovin.api.sdk.model.GcsInput;
import com.bitmovin.api.sdk.model.HlsManifestDefault;
import com.bitmovin.api.sdk.model.HlsManifestDefaultVersion;
import com.bitmovin.api.sdk.model.S3Output;
import com.bitmovin.api.sdk.model.Sprite;
import com.bitmovin.api.sdk.model.StartEncodingRequest;
import com.bitmovin.api.sdk.model.StreamFilter;
import com.bitmovin.api.sdk.model.StreamFilterList;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.bitmovin.api.sdk.model.Thumbnail;
import com.bitmovin.api.sdk.model.WatermarkFilter;
import feign.Logger;
import feign.slf4j.Slf4jLogger;

public class FiltersAndThumbnails extends BasicEncodingClient {

    public void execute() throws IOException, InterruptedException {

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

        String h264Configuration2Id =
          !config.getProperty("h264_config_2_id").equals("") ?
            config.getProperty("h264_config_2_id") :
            createH264Configuration(
              "h264-2",
              Integer.parseInt(config.getProperty("h264_2_height")),
              Integer.parseInt(config.getProperty("h264_2_bitrate"))
            );
        logger.info("video config 2 id: " + h264ConfigurationId);

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
            encodingId = createEncoding(config.getProperty("encoding_name"));
            logger.info("encoding id: " + encodingId);

            StreamInput h264StreamInput = createStreamInput(
              config.getProperty("video_input_path"), in1Id);
            logger.info("input to video stream: " + h264StreamInput.getInputId());

            StreamInput h264Stream2Input = createStreamInput(
              config.getProperty("video_input_path"), in1Id);
            logger.info("input to video stream 2: " + h264Stream2Input.getInputId());

            StreamInput aacStreamInput = createStreamInput(
              config.getProperty("audio_input_path"), in1Id);
            logger.info("input to audio stream: " + aacStreamInput.getInputId());

            String aacStreamId = createStream(encodingId,
              aacConfigurationId, aacStreamInput);
            logger.info("audio stream id: " + aacStreamId);

            String h264StreamId = createStream(encodingId,
              h264ConfigurationId, h264StreamInput);
            logger.info("video stream id: " + h264StreamId);

            String h264Stream2Id = createStream(encodingId,
              h264Configuration2Id, h264Stream2Input);
            logger.info("video stream id: " + h264Stream2Id);


            // FILTERS
            List<String> filters = new ArrayList<>();
            filters.add(createWatermarkFilter(config.getProperty("watermarks_url")));
            createStreamFilterList(encodingId, h264StreamId, filters);

            //CODEC, RENDITION, CONTAINER_FORMAT
            EncodingOutput fmp4H264Out = createEncodingOutput(
              out1Id, rootPath,
              "h264",
              config.getProperty("h264_1_height") + "_" +
                config.getProperty("h264_1_bitrate"),
              "fmp4");
            logger.info("fmp4 h264 output: " + fmp4H264Out);

            EncodingOutput fmp4H264Out2 = createEncodingOutput(
              out1Id, rootPath,
              "h264",
              config.getProperty("h264_2_height") + "_" +
                config.getProperty("h264_2_bitrate"),
              "fmp4");
            logger.info("fmp4 h264 output 2: " + fmp4H264Out2);

            EncodingOutput fmp4AacOut = createEncodingOutput(
              out1Id, rootPath, "aac", "16000", "fmp4");
            logger.info("fmp4 aac output: " + fmp4AacOut);

            createFmp4Muxing(encodingId, fmp4H264Out, h264StreamId);

            createFmp4Muxing(encodingId, fmp4H264Out2, h264Stream2Id);

            createFmp4Muxing(encodingId, fmp4AacOut, aacStreamId);


//            // THUMBNAILS tested 04-05-21
//            List<EncodingOutput> thumbnailOut = new ArrayList<>();
//            thumbnailOut.add(fmp4H264Out);
//            String thumbnailId = createThumbnails(
//                        thumbnailOut, encodingId, h264StreamId);
//            logger.info("thumbnail ID: " + thumbnailId);

            // SPRITES
            List<EncodingOutput> spriteOut = new ArrayList<>();
            spriteOut.add(fmp4H264Out);
            String spriteId = createSprites(
              spriteOut, encodingId, h264Stream2Id);
            logger.info("sprite ID: " + spriteId);

            startEncoding(encodingId);
            awaitEncoding(encodingId);
        }
        logger.info("encoding id: " + encodingId);

        EncodingOutput manifestOut = createEncodingOutput(out1Id, rootPath);

        String manifestId = "";
        switch (config.getProperty("manifest_type"))
        {
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


    private String createWatermarkFilter(String url) throws BitmovinException
    {
        WatermarkFilter watermarkFilter = new WatermarkFilter();
        watermarkFilter.setImage(url);
        watermarkFilter.setTop(10);
        watermarkFilter.setLeft(10);

        return bitmovinApi.encoding.filters
          .watermark.create(watermarkFilter).getId();
    }

    private StreamFilterList createStreamFilterList(
      String encodingId, String streamId, List<String> filters)
    {
        int position = 0;
        List<StreamFilter> streamFilters = new ArrayList<>();

        for (String filterId : filters) {
            StreamFilter streamFilter = new StreamFilter();
            streamFilter.setId(filterId);
            streamFilter.setPosition(position++);
            streamFilters.add(streamFilter);
        }

        return bitmovinApi.encoding.encodings.streams.filters.create(
          encodingId, streamId, streamFilters);
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

    private String createSprites(
      List<EncodingOutput> out, String encodingId, String streamId
    )
    {
        Sprite sprite = new Sprite();
        sprite.setHeight(240);
        sprite.setWidth(320);
        sprite.setDistance(5D);
        sprite.setSpriteName("seek-images-%number%.jpg");
        sprite.setImagesPerFile(10);
        sprite.setVttName("seek-images.vtt");
        sprite.setOutputs(out);

        return bitmovinApi.encoding.encodings.streams.sprites.create(
          encodingId, streamId, sprite
        ).getId();
    }

}