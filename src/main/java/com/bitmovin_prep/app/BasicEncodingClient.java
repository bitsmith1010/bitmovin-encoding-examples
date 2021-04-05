package com.bitmovin_prep.app;

import com.bitmovin.api.sdk.BitmovinApi;
import com.bitmovin.api.sdk.model.AacAudioConfiguration;
import com.bitmovin.api.sdk.model.GcsInput;
import com.bitmovin.api.sdk.model.HlsManifestDefault;
import com.bitmovin.api.sdk.model.HlsManifestDefaultVersion;
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
import feign.Logger;
import feign.slf4j.Slf4jLogger;

public class BasicEncodingClient extends EncodingClientUtilities {

    public void execute() throws IOException, InterruptedException
    {}

    public BitmovinApi createBitmovinApi(String key)
    {
        return BitmovinApi.builder()
                .withApiKey(key)
                .withLogger(new Slf4jLogger(), Logger.Level.BASIC)
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


    private static String createH264Configuration(
            String name, int width, long bitrate) {
        H264VideoConfiguration configuration = new H264VideoConfiguration();
        configuration.setName(name);
        configuration.setWidth(width);
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

}
