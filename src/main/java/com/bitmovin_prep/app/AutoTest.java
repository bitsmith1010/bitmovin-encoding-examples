package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.model.*;

import java.io.IOException;
import java.util.*;

public class AutoTest extends BasicEncodingClient {
  private BitmovinApi bitmovinApi;

  /** generated test based on Encoding with
   * id: 432d6442-5358-4003-9191-9e8c57248601
   * type: VOD
   * encoderVersion: STABLE
   * selectedEncoderVersion: 2.65.0
   * selectedEncodingMode: THREE_PASS
   * selectedCloudRegion: AWS_US_EAST_1
   * createdAt:  19-03-2021T22:05:13Z
   * startedAt:  19-03-2021T22:05:18Z
   * queuedAt:   19-03-2021T22:05:21Z
   * runningAt:  19-03-2021T22:06:56Z
   * finishedAt: 19-03-2021T22:37:36Z
   * status: ERROR
   */
//  @Test
  public void runTest() throws IOException, InterruptedException
  {
    Properties config = getProperties();
    String API_KEY =
      config.getProperty("api_key");

    bitmovinApi = BitmovinApi.builder().withApiKey(API_KEY).build();

    Encoding encoding = new Encoding();
    encoding.setCloudRegion(CloudRegion.AWS_US_EAST_1);
    encoding.setDescription("Encoding job for Dev_5975_dezmund_hls_master_20210319220512668");
    encoding.setEncoderVersion("2.65.0");
    encoding.setName("starz60_sethdrfalse_nomaxminbitrate");
    encoding = bitmovinApi.encoding.encodings.create(encoding);


    String s3RoleBasedInput = !config.getProperty("input_resource_id").equals("") ?
      config.getProperty("input_resource_id") :
      createGcsInput("resource-in-1",
        config.getProperty("gcs_input_access"),
        config.getProperty("gcs_input_secret"),
        config.getProperty("input_bucket_name"))
        .getId();
    logger.info("in id: " + s3RoleBasedInput);

    String s3Output = !config.getProperty("output_resource_id").equals("") ?
      config.getProperty("output_resource_id") :
      createGcsOutput("resource-out-1",
        config.getProperty("gcs_output_access"),
        config.getProperty("gcs_output_secret"),
        config.getProperty("output_bucket_name"))
        .getId();
    logger.info("out id: " + s3Output);

    /*
    S3RoleBasedInput s3RoleBasedInput = new S3RoleBasedInput();
    s3RoleBasedInput.setBucketName("stz-l1-transcode-in-dev");
    s3RoleBasedInput.setCloudRegion(AwsCloudRegion.US_EAST_1);
    s3RoleBasedInput.setDescription("Role based input for AWS02 bucket - stz-l1-transcode-in-dev");
    s3RoleBasedInput.setExternalId("<PLACEHOLDER>");
    s3RoleBasedInput.setName("Dev-AWS02-Input-Role-Based");
    s3RoleBasedInput.setRoleArn("<PLACEHOLDER>");
    s3RoleBasedInput = bitmovinApi.encoding.inputs.s3RoleBased.create(s3RoleBasedInput);

    S3Output s3Output = new S3Output();
    s3Output.setAccessKey("<PLACEHOLDER>");
    s3Output.setBucketName("stz-abr-dev-us-east-1");
    s3Output.setCloudRegion(AwsCloudRegion.US_EAST_1);
    s3Output.setDescription("IAM Credentials for the stz-abr-dev-us-east-1 bucket to be used as an output.");
    s3Output.setName("AWS02-IAM-Credentials-stz-abr-dev-us-east-1");
    s3Output.setSecretKey("<PLACEHOLDER>");
    s3Output = bitmovinApi.encoding.outputs.s3.create(s3Output);
   */
    AacAudioConfiguration aacAudioConfiguration = new AacAudioConfiguration();
    aacAudioConfiguration.setBitrate(128000L);
    aacAudioConfiguration.setChannelLayout(AacChannelLayout.NONE);
    aacAudioConfiguration.setName("AAC");
    aacAudioConfiguration.setRate(48000.0d);
    aacAudioConfiguration = bitmovinApi.encoding.configurations.audio.aac.create(aacAudioConfiguration);
    
    Stream stream = new Stream();
    stream.setCodecConfigId(aacAudioConfiguration.getId());
    stream.setCreateQualityMetaData(false);
    StreamInput stream_streamInput = new StreamInput();
    stream_streamInput.setInputId(s3RoleBasedInput);
    stream_streamInput.setInputPath("UHD/c31a4720-e46f-46ae-b880-52e614b0e019.ATS.wav");
    stream_streamInput.setPosition(0);
    stream_streamInput.setSelectionMode(StreamSelectionMode.AUDIO_RELATIVE);
    stream.setInputStreams(Arrays.asList(stream_streamInput));
    StreamMetadata stream_streamMetadata = new StreamMetadata();
    stream_streamMetadata.setLanguage("en-US");
    stream.setMetadata(stream_streamMetadata);
    stream.setMode(StreamMode.STANDARD);
    stream.setName("AUDIO-en-US-Stereo-AAC");
    stream = bitmovinApi.encoding.encodings.streams.create(encoding.getId(), stream);
    
    Mp4Muxing mp4Muxing = new Mp4Muxing();
    mp4Muxing.setFilename("audio.mp4");
    mp4Muxing.setFragmentDuration(4000);
    mp4Muxing.setFragmentedMP4MuxingManifestType(FragmentedMp4MuxingManifestType.HLS_BYTE_RANGES_AND_IFRAME_PLAYLIST);
    mp4Muxing.setName("audio.mp4");
    mp4Muxing.setStreamConditionsMode(StreamConditionsMode.DROP_MUXING);
    MuxingStream mp4Muxing_muxingStream = new MuxingStream();
    mp4Muxing_muxingStream.setStreamId(stream.getId());
    mp4Muxing.setStreams(Arrays.asList(mp4Muxing_muxingStream));
    mp4Muxing = bitmovinApi.encoding.encodings.muxings.mp4.create(encoding.getId(), mp4Muxing);
    
    CencDrm cencDrm = new CencDrm();
    cencDrm.setEnablePiffCompatibility(false);
    cencDrm.setEncryptionMode(EncryptionMode.CBC);
    CencFairPlay cencDrm_cencFairPlay = new CencFairPlay();
    cencDrm_cencFairPlay.setIv("f16292cb858f3bca628240e0f4e819da");
    cencDrm_cencFairPlay.setUri("skd://dezmund:5975_1616191512668");
    cencDrm.setFairPlay(cencDrm_cencFairPlay);
    cencDrm.setIvSize(IvSize.IV_16_BYTES);
    cencDrm.setKey("661f1be84c051b612d59a2524bc0795e");
    cencDrm.setKid("b5f84c35522042ad8f3c8c8203ec1a0a");
    cencDrm.setName("mp4_AUDIO-en-US-Stereo-AAC");
    EncodingOutput cencDrm_encodingOutput = new EncodingOutput();
    AclEntry cencDrm_encodingOutput_aclEntry = new AclEntry();
    cencDrm_encodingOutput_aclEntry.setPermission(AclPermission.PRIVATE);
    cencDrm_encodingOutput.setAcl(Arrays.asList(cencDrm_encodingOutput_aclEntry));
    cencDrm_encodingOutput.setOutputId(s3Output);
    cencDrm_encodingOutput.setOutputPath("assets/5975/20210319220512668/Apple/audio/en-US_Stereo/");
    cencDrm.setOutputs(Arrays.asList(cencDrm_encodingOutput));
    cencDrm = bitmovinApi.encoding.encodings.muxings.mp4.drm.cenc.create(encoding.getId(), mp4Muxing.getId(), cencDrm);
    
    Stream stream1 = new Stream();
    stream1.setCodecConfigId(aacAudioConfiguration.getId());
    stream1.setCreateQualityMetaData(false);
    StreamInput stream1_streamInput = new StreamInput();
    stream1_streamInput.setInputId(s3RoleBasedInput);
    stream1_streamInput.setInputPath("UHD/b885dc93-79d6-4057-bec5-50b731a0a309.ATS.wav");
    stream1_streamInput.setPosition(0);
    stream1_streamInput.setSelectionMode(StreamSelectionMode.AUDIO_RELATIVE);
    stream1.setInputStreams(Arrays.asList(stream1_streamInput));
    StreamMetadata stream1_streamMetadata = new StreamMetadata();
    stream1_streamMetadata.setLanguage("es-419");
    stream1.setMetadata(stream1_streamMetadata);
    stream1.setMode(StreamMode.STANDARD);
    stream1.setName("AUDIO-es-419-Stereo-AAC");
    stream1 = bitmovinApi.encoding.encodings.streams.create(encoding.getId(), stream1);
    
    Mp4Muxing mp4Muxing1 = new Mp4Muxing();
    mp4Muxing1.setFilename("audio.mp4");
    mp4Muxing1.setFragmentDuration(4000);
    mp4Muxing1.setFragmentedMP4MuxingManifestType(FragmentedMp4MuxingManifestType.HLS_BYTE_RANGES_AND_IFRAME_PLAYLIST);
    mp4Muxing1.setName("audio.mp4");
    mp4Muxing1.setStreamConditionsMode(StreamConditionsMode.DROP_MUXING);
    MuxingStream mp4Muxing1_muxingStream = new MuxingStream();
    mp4Muxing1_muxingStream.setStreamId(stream1.getId());
    mp4Muxing1.setStreams(Arrays.asList(mp4Muxing1_muxingStream));
    mp4Muxing1 = bitmovinApi.encoding.encodings.muxings.mp4.create(encoding.getId(), mp4Muxing1);
    
    CencDrm cencDrm1 = new CencDrm();
    cencDrm1.setEnablePiffCompatibility(false);
    cencDrm1.setEncryptionMode(EncryptionMode.CBC);
    CencFairPlay cencDrm1_cencFairPlay = new CencFairPlay();
    cencDrm1_cencFairPlay.setIv("f16292cb858f3bca628240e0f4e819da");
    cencDrm1_cencFairPlay.setUri("skd://dezmund:5975_1616191512668");
    cencDrm1.setFairPlay(cencDrm1_cencFairPlay);
    cencDrm1.setIvSize(IvSize.IV_16_BYTES);
    cencDrm1.setKey("661f1be84c051b612d59a2524bc0795e");
    cencDrm1.setKid("b5f84c35522042ad8f3c8c8203ec1a0a");
    cencDrm1.setName("mp4_AUDIO-es-419-Stereo-AAC");
    EncodingOutput cencDrm1_encodingOutput = new EncodingOutput();
    AclEntry cencDrm1_encodingOutput_aclEntry = new AclEntry();
    cencDrm1_encodingOutput_aclEntry.setPermission(AclPermission.PRIVATE);
    cencDrm1_encodingOutput.setAcl(Arrays.asList(cencDrm1_encodingOutput_aclEntry));
    cencDrm1_encodingOutput.setOutputId(s3Output);
    cencDrm1_encodingOutput.setOutputPath("assets/5975/20210319220512668/Apple/audio/es-419_Stereo/");
    cencDrm1.setOutputs(Arrays.asList(cencDrm1_encodingOutput));
    cencDrm1 = bitmovinApi.encoding.encodings.muxings.mp4.drm.cenc.create(encoding.getId(), mp4Muxing1.getId(), cencDrm1);
    
    H265VideoConfiguration h265VideoConfiguration = new H265VideoConfiguration();
    h265VideoConfiguration.setBAdapt(BAdapt.FULL);
    h265VideoConfiguration.setBframes(4);
    h265VideoConfiguration.setDescription("HDR/UHD config for H265 streams");
    h265VideoConfiguration.setHdr(true);
    h265VideoConfiguration.setMaxCTUSize(MaxCtuSize.S64);
    h265VideoConfiguration.setMotionSearch(MotionSearch.STAR);
    h265VideoConfiguration.setMotionSearchRange(57);
    h265VideoConfiguration.setName("H265_HDR_4k");
    h265VideoConfiguration.setProfile(ProfileH265.MAIN);
    h265VideoConfiguration.setRcLookahead(25);
    h265VideoConfiguration.setRefFrames(4);
    h265VideoConfiguration.setSao(true);
    h265VideoConfiguration.setSubMe(3);
    h265VideoConfiguration.setTuInterDepth(TuInterDepth.D1);
    h265VideoConfiguration.setTuIntraDepth(TuIntraDepth.D1);
    h265VideoConfiguration.setWeightPredictionOnBSlice(false);
    h265VideoConfiguration.setWeightPredictionOnPSlice(true);
//    h265VideoConfiguration.setWidth(3840);
    h265VideoConfiguration = bitmovinApi.encoding.configurations.video.h265
      .create(h265VideoConfiguration);

    Stream stream2 = new Stream();
    stream2.setCodecConfigId(h265VideoConfiguration.getId());
    stream2.setCreateQualityMetaData(false);
    StreamInput stream2_streamInput = new StreamInput();
    stream2_streamInput.setInputId(s3RoleBasedInput);
    stream2_streamInput.setInputPath("UHD/b15e9109-ee81-4185-b56e-b4becb7e62cf.mxf");
    stream2_streamInput.setPosition(0);
    stream2_streamInput.setSelectionMode(StreamSelectionMode.VIDEO_RELATIVE);
    stream2.setInputStreams(Arrays.asList(stream2_streamInput));
    stream2.setMode(StreamMode.PER_TITLE_TEMPLATE);
    stream2 = bitmovinApi.encoding.encodings.streams.create(encoding.getId(), stream2);
    
    Bif bif = new Bif();
    bif.setDistance(10.0d);
    bif.setFilename("thumbnails.bif");
    bif.setHeight(180);
    bif.setName("Roku BIF");
    EncodingOutput bif_encodingOutput = new EncodingOutput();
    AclEntry bif_encodingOutput_aclEntry = new AclEntry();
    bif_encodingOutput_aclEntry.setPermission(AclPermission.PRIVATE);
    bif_encodingOutput.setAcl(Arrays.asList(bif_encodingOutput_aclEntry));
    bif_encodingOutput.setOutputId(s3Output);
    bif_encodingOutput.setOutputPath("assets/5975/20210319220512668/");
    bif.setOutputs(Arrays.asList(bif_encodingOutput));
    bif.setWidth(320);
    bif = bitmovinApi.encoding.encodings.streams.bifs.create(encoding.getId(), stream2.getId(), bif);
    
    Thumbnail thumbnail = new Thumbnail();
    thumbnail.setHeight(180);
    thumbnail.setName("thumbnail_gen_assets/5975/20210319220512668");
    EncodingOutput thumbnail_encodingOutput = new EncodingOutput();
    AclEntry thumbnail_encodingOutput_aclEntry = new AclEntry();
    thumbnail_encodingOutput_aclEntry.setPermission(AclPermission.PRIVATE);
    thumbnail_encodingOutput.setAcl(Arrays.asList(thumbnail_encodingOutput_aclEntry));
    thumbnail_encodingOutput.setOutputId(s3Output);
    thumbnail_encodingOutput.setOutputPath("assets/5975/20210319220512668/Thumbnails/");
    thumbnail.setOutputs(Arrays.asList(thumbnail_encodingOutput));
    thumbnail.setPattern("thumbnail-%number%.jpg");
    thumbnail.setPositions(Arrays.asList(10.0d, 20.0d, 30.0d, 40.0d, 50.0d, 60.0d, 70.0d, 80.0d, 90.0d, 100.0d, 110.0d, 120.0d, 130.0d, 140.0d, 150.0d, 160.0d, 170.0d, 180.0d, 190.0d, 200.0d, 210.0d, 220.0d, 230.0d, 240.0d, 250.0d, 260.0d, 270.0d, 280.0d, 290.0d, 300.0d, 310.0d, 320.0d, 330.0d, 340.0d, 350.0d, 360.0d, 370.0d, 380.0d, 390.0d, 400.0d, 410.0d, 420.0d, 430.0d, 440.0d, 450.0d, 460.0d, 470.0d, 480.0d, 490.0d, 500.0d, 510.0d, 520.0d, 530.0d, 540.0d, 550.0d, 560.0d, 570.0d, 580.0d, 590.0d, 600.0d, 610.0d, 620.0d, 630.0d, 640.0d, 650.0d, 660.0d, 670.0d, 680.0d, 690.0d, 700.0d, 710.0d, 720.0d, 730.0d, 740.0d, 750.0d, 760.0d, 770.0d, 780.0d, 790.0d, 800.0d, 810.0d, 820.0d, 830.0d, 840.0d, 850.0d, 860.0d, 870.0d, 880.0d, 890.0d, 900.0d, 910.0d, 920.0d, 930.0d, 940.0d, 950.0d, 960.0d, 970.0d, 980.0d, 990.0d, 1000.0d, 1010.0d, 1020.0d, 1030.0d, 1040.0d, 1050.0d, 1060.0d, 1070.0d, 1080.0d, 1090.0d, 1100.0d, 1110.0d, 1120.0d, 1130.0d, 1140.0d, 1150.0d, 1160.0d, 1170.0d, 1180.0d, 1190.0d, 1200.0d, 1210.0d, 1220.0d, 1230.0d, 1240.0d, 1250.0d, 1260.0d, 1270.0d, 1280.0d, 1290.0d, 1300.0d, 1310.0d, 1320.0d, 1330.0d, 1340.0d, 1350.0d, 1360.0d, 1370.0d, 1380.0d, 1390.0d, 1400.0d, 1410.0d, 1420.0d, 1430.0d, 1440.0d, 1450.0d, 1460.0d, 1470.0d, 1480.0d, 1490.0d, 1500.0d, 1510.0d, 1520.0d, 1530.0d, 1540.0d, 1550.0d, 1560.0d, 1570.0d, 1580.0d, 1590.0d, 1600.0d, 1610.0d, 1620.0d, 1630.0d, 1640.0d, 1650.0d, 1660.0d, 1670.0d, 1680.0d, 1690.0d, 1700.0d, 1710.0d, 1720.0d, 1730.0d, 1740.0d, 1750.0d, 1760.0d, 1770.0d, 1780.0d, 1790.0d, 1800.0d, 1810.0d, 1820.0d, 1830.0d, 1840.0d, 1850.0d, 1860.0d, 1870.0d, 1880.0d, 1890.0d, 1900.0d, 1910.0d, 1920.0d, 1930.0d, 1940.0d, 1950.0d, 1960.0d, 1970.0d, 1980.0d, 1990.0d, 2000.0d, 2010.0d, 2020.0d, 2030.0d, 2040.0d, 2060.0d, 2150.0d, 2050.0d, 2100.0d, 2110.0d, 2080.0d, 2090.0d, 2070.0d, 2160.0d, 2170.0d, 2120.0d, 2130.0d, 2140.0d, 2190.0d, 2280.0d, 2180.0d, 2230.0d, 2210.0d, 2220.0d, 2200.0d, 2290.0d, 2300.0d, 2240.0d, 2250.0d, 2260.0d, 2270.0d, 2320.0d, 2330.0d, 2310.0d, 2360.0d, 2340.0d, 2350.0d, 2400.0d, 2410.0d, 2420.0d, 2430.0d, 2370.0d, 2380.0d, 2390.0d, 2450.0d, 2530.0d, 2440.0d, 2490.0d, 2470.0d, 2480.0d, 2460.0d, 2540.0d, 2550.0d, 2500.0d, 2510.0d, 2520.0d, 2570.0d, 2560.0d, 2580.0d, 2590.0d, 2600.0d, 2610.0d, 2620.0d, 2630.0d, 2640.0d, 2650.0d, 2680.0d, 2660.0d, 2670.0d, 2700.0d, 2790.0d, 2690.0d, 2740.0d, 2750.0d, 2720.0d, 2730.0d, 2710.0d, 2800.0d, 2810.0d, 2760.0d, 2770.0d, 2780.0d, 2830.0d, 2920.0d, 2820.0d, 2870.0d, 2850.0d, 2860.0d, 2840.0d, 2930.0d, 2940.0d, 2880.0d, 2890.0d, 2900.0d, 2910.0d, 2960.0d, 2970.0d, 2950.0d, 3000.0d, 2980.0d, 2990.0d, 3040.0d, 3050.0d, 3060.0d, 3070.0d, 3010.0d, 3020.0d, 3030.0d, 3090.0d, 3170.0d, 3080.0d, 3130.0d, 3110.0d, 3120.0d, 3100.0d, 3180.0d, 3190.0d, 3140.0d, 3150.0d, 3160.0d, 3210.0d, 3200.0d, 3220.0d, 3230.0d, 3240.0d, 3250.0d, 3260.0d, 3270.0d, 3280.0d, 3290.0d, 3300.0d));
    thumbnail.setUnit(ThumbnailUnit.SECONDS);
    thumbnail = bitmovinApi.encoding.encodings.streams.thumbnails.create(encoding.getId(), stream2.getId(), thumbnail);
    
    Mp4Muxing mp4Muxing2 = new Mp4Muxing();
    mp4Muxing2.setFilename("video.mp4");
    mp4Muxing2.setFragmentDuration(4000);
    mp4Muxing2.setFragmentedMP4MuxingManifestType(FragmentedMp4MuxingManifestType.HLS_BYTE_RANGES_AND_IFRAME_PLAYLIST);
    mp4Muxing2.setName("video.mp4");
    mp4Muxing2.setStreamConditionsMode(StreamConditionsMode.DROP_MUXING);
    MuxingStream mp4Muxing2_muxingStream = new MuxingStream();
    mp4Muxing2_muxingStream.setStreamId(stream2.getId());
    mp4Muxing2.setStreams(Arrays.asList(mp4Muxing2_muxingStream));
    mp4Muxing2 = bitmovinApi.encoding.encodings.muxings.mp4.create(encoding.getId(), mp4Muxing2);
    
    CencDrm cencDrm2 = new CencDrm();
    cencDrm2.setEnablePiffCompatibility(false);
    cencDrm2.setEncryptionMode(EncryptionMode.CBC);
    CencFairPlay cencDrm2_cencFairPlay = new CencFairPlay();
    cencDrm2_cencFairPlay.setIv("f16292cb858f3bca628240e0f4e819da");
    cencDrm2_cencFairPlay.setUri("skd://dezmund:5975_1616191512668");
    cencDrm2.setFairPlay(cencDrm2_cencFairPlay);
    cencDrm2.setIvSize(IvSize.IV_16_BYTES);
    cencDrm2.setKey("661f1be84c051b612d59a2524bc0795e");
    cencDrm2.setKid("b5f84c35522042ad8f3c8c8203ec1a0a");
    cencDrm2.setName("mp4_{width}w_{bitrate}bps");
    EncodingOutput cencDrm2_encodingOutput = new EncodingOutput();
    AclEntry cencDrm2_encodingOutput_aclEntry = new AclEntry();
    cencDrm2_encodingOutput_aclEntry.setPermission(AclPermission.PRIVATE);
    cencDrm2_encodingOutput.setAcl(Arrays.asList(cencDrm2_encodingOutput_aclEntry));
    cencDrm2_encodingOutput.setOutputId(s3Output);
    cencDrm2_encodingOutput.setOutputPath("assets/5975/20210319220512668/Apple/video/hls_master/{width}w_{bitrate}bps/");
    cencDrm2.setOutputs(Arrays.asList(cencDrm2_encodingOutput));
    cencDrm2 = bitmovinApi.encoding.encodings.muxings.mp4.drm.cenc.create(encoding.getId(), mp4Muxing2.getId(), cencDrm2);
    
    Ac3AudioConfiguration ac3AudioConfiguration = new Ac3AudioConfiguration();
    ac3AudioConfiguration.setBitrate(384000L);
    ac3AudioConfiguration.setChannelLayout(Ac3ChannelLayout.CL_5_1);
    ac3AudioConfiguration.setName("A1_AC3_384k_51");
    ac3AudioConfiguration.setRate(48000.0d);
    ac3AudioConfiguration = bitmovinApi.encoding.configurations.audio.ac3.create(ac3AudioConfiguration);
    
    Stream stream3 = new Stream();
    stream3.setCodecConfigId(ac3AudioConfiguration.getId());
    stream3.setCreateQualityMetaData(false);
    StreamInput stream3_streamInput = new StreamInput();
    stream3_streamInput.setInputId(s3RoleBasedInput);
    stream3_streamInput.setInputPath("UHD/ffabe59a-1221-4330-9aa3-f81c217205c7.wav");
    stream3_streamInput.setPosition(0);
    stream3_streamInput.setSelectionMode(StreamSelectionMode.AUDIO_RELATIVE);
    stream3.setInputStreams(Arrays.asList(stream3_streamInput));
    StreamMetadata stream3_streamMetadata = new StreamMetadata();
    stream3_streamMetadata.setLanguage("en-US");
    stream3.setMetadata(stream3_streamMetadata);
    stream3.setMode(StreamMode.STANDARD);
    stream3.setName("AUDIO-en-US-5.1 Surround-A1_AC3_384k_51");
    stream3 = bitmovinApi.encoding.encodings.streams.create(encoding.getId(), stream3);
    
    Mp4Muxing mp4Muxing3 = new Mp4Muxing();
    mp4Muxing3.setFilename("audio.mp4");
    mp4Muxing3.setFragmentDuration(4000);
    mp4Muxing3.setFragmentedMP4MuxingManifestType(FragmentedMp4MuxingManifestType.HLS_BYTE_RANGES_AND_IFRAME_PLAYLIST);
    mp4Muxing3.setName("audio.mp4");
    mp4Muxing3.setStreamConditionsMode(StreamConditionsMode.DROP_MUXING);
    MuxingStream mp4Muxing3_muxingStream = new MuxingStream();
    mp4Muxing3_muxingStream.setStreamId(stream3.getId());
    mp4Muxing3.setStreams(Arrays.asList(mp4Muxing3_muxingStream));
    mp4Muxing3 = bitmovinApi.encoding.encodings.muxings.mp4.create(encoding.getId(), mp4Muxing3);
    
    CencDrm cencDrm3 = new CencDrm();
    cencDrm3.setEnablePiffCompatibility(false);
    cencDrm3.setEncryptionMode(EncryptionMode.CBC);
    CencFairPlay cencDrm3_cencFairPlay = new CencFairPlay();
    cencDrm3_cencFairPlay.setIv("f16292cb858f3bca628240e0f4e819da");
    cencDrm3_cencFairPlay.setUri("skd://dezmund:5975_1616191512668");
    cencDrm3.setFairPlay(cencDrm3_cencFairPlay);
    cencDrm3.setIvSize(IvSize.IV_16_BYTES);
    cencDrm3.setKey("661f1be84c051b612d59a2524bc0795e");
    cencDrm3.setKid("b5f84c35522042ad8f3c8c8203ec1a0a");
    cencDrm3.setName("mp4_AUDIO-en-US-5.1 Surround-A1_AC3_384k_51");
    EncodingOutput cencDrm3_encodingOutput = new EncodingOutput();
    AclEntry cencDrm3_encodingOutput_aclEntry = new AclEntry();
    cencDrm3_encodingOutput_aclEntry.setPermission(AclPermission.PRIVATE);
    cencDrm3_encodingOutput.setAcl(Arrays.asList(cencDrm3_encodingOutput_aclEntry));
    cencDrm3_encodingOutput.setOutputId(s3Output);
    cencDrm3_encodingOutput.setOutputPath("assets/5975/20210319220512668/Apple/audio/en-US_5_1_Surround/");
    cencDrm3.setOutputs(Arrays.asList(cencDrm3_encodingOutput));
    cencDrm3 = bitmovinApi.encoding.encodings.muxings.mp4.drm.cenc.create(encoding.getId(), mp4Muxing3.getId(), cencDrm3);
    
    StartEncodingRequest startEncodingRequest = new StartEncodingRequest();
    startEncodingRequest.setEncodingMode(EncodingMode.THREE_PASS);
    PerTitle startEncodingRequest_perTitle = new PerTitle();
    H265PerTitleConfiguration startEncodingRequest_perTitle_h265PerTitleConfiguration = new H265PerTitleConfiguration();
//    startEncodingRequest_perTitle_h265PerTitleConfiguration.setCodecBufsizeFactor(2.0d);
//    startEncodingRequest_perTitle_h265PerTitleConfiguration.setCodecMaxBitrateFactor(2.0d);
//    startEncodingRequest_perTitle_h265PerTitleConfiguration.setCodecMinBitrateFactor(0.5d);
//    startEncodingRequest_perTitle_h265PerTitleConfiguration.setMaxBitrate(30000000);
//    startEncodingRequest_perTitle_h265PerTitleConfiguration.setMinBitrate(240000);
    startEncodingRequest_perTitle.setH265Configuration(startEncodingRequest_perTitle_h265PerTitleConfiguration);
    startEncodingRequest.setPerTitle(startEncodingRequest_perTitle);
    Scheduling startEncodingRequest_scheduling = new Scheduling();
    startEncodingRequest_scheduling.setPriority(60);
    startEncodingRequest.setScheduling(startEncodingRequest_scheduling);
    bitmovinApi.encoding.encodings.start(encoding.getId(), startEncodingRequest);
    
    waitToFinish(encoding.getId());
  }

  private void waitToFinish(String encodingId) throws InterruptedException {
    List<Status> errorStates = Arrays.asList(Status.ERROR, Status.TRANSFER_ERROR, Status.CANCELED);
    Task task;
    do {
      Thread.sleep(5000);
      task = bitmovinApi.encoding.encodings.status(encodingId);
      System.out.printf("Encoding status is %s (progress: %s %%)%n", task.getStatus(), task.getProgress());
    } while (!Status.FINISHED.equals(task.getStatus()) && !errorStates.contains(task.getStatus()));

    if (errorStates.contains(task.getStatus())) {
      task.getMessages().stream()
          .filter(msg -> msg.getType() == MessageType.ERROR)
          .forEach(msg -> System.out.println(msg.getText()));
      throw new RuntimeException("Encoding failed");
    }
    System.out.println("Encoding finished successfully");
  }
}
