package utils;

import org.lwjgl.stb.STBVorbisInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import static java.sql.Types.NULL;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.stb.STBVorbis.*;

/**
 * Represents a single playable sound. Sound files should be formatted as .ogg files
 */
public class Sound {

    /**
     * Members
     */
    private final int bufferID; // the id of the openAL sound buffer
    private final int sourceID; // the id of the openAL source
    private ShortBuffer pcm;    // the sound's pulse-code modulation

    /**
     * Constructor
     * @param path the path to the sound file (should be in .ogg format)
     * @param resRelative whether the given path is resource-relative
     */
    public Sound(String path, boolean resRelative) {
        this.bufferID = alGenBuffers(); // generate the openAL sound buffer
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) { // allocate space for the vorbis info
            ShortBuffer pcm = readVorbis(path, resRelative, 32 * 1024, info); // read the vorbis
            alBufferData(this.bufferID, info.channels() == 1 ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16, pcm,
                    info.sample_rate()); // put the sound data into the openAL buffer
        }
        this.sourceID = alGenSources(); // generate an openAL sound source
        alSourcei(sourceID, AL_BUFFER, this.bufferID); // link the source to the buffer
    }

    /**
     * Updates whether or not the sound should loop when played
     * @param loop whether the sound should loop when played
     */
    public void setLoop(boolean loop) {
        alSourcei(this.sourceID, AL_LOOPING, loop ? AL_TRUE : AL_FALSE); // update openAL source setting
    }

    /**
     * Plays the sound
     */
    public void play() {
        alSourcePlay(this.bufferID); // play the openAL source
    }

    /**
     * Pauses the sound
     */
    public void pause() {
        alSourcePause(this.sourceID); // pause the openAL source
    }

    /**
     * Stops the sound
     */
    public void stop() {
        alSourceStop(this.bufferID); // stop the openAL source
    }

    /**
     * Reads the sound vorbix and creates the pulse-code modulation
     * @param path the path to the sound find (should be in .ogg format)
     * @param resRelative whether the given path is resource-relative or not
     * @param bufferSize the initial size to use for the vorbis buffer size
     * @param info the vorbis info
     * @return a short buffer containing the pulse-code modulation
     */
    private ShortBuffer readVorbis(String path, boolean resRelative, int bufferSize, STBVorbisInfo info) {
        MemoryStack stack = MemoryStack.stackPush(); // push a stack to memory to story error information
        ByteBuffer vorbis = Utils.fileToByteBuffer(path, resRelative, bufferSize); // read audio file into byte buffer
        IntBuffer error = stack.mallocInt(1); // allocate an int for an error code
        long dec = stb_vorbis_open_memory(vorbis, error, null); // open ogg vorbis file
        // if opening failed, crash program
        if (dec == NULL) Utils.handleException(new Exception("Failed to open Ogg Vorbis file. Error code: " +
                        error.get(0)), "utils.Sound", "readVorbis(String, boolean, int, STBVorbisInfo", true);
        stb_vorbis_get_info(dec, info); // get the vorbis info
        int channels = info.channels(); // get the amount of audio channels
        int length = stb_vorbis_stream_length_in_samples(dec); // get the length of the vorbis in samples
        pcm = MemoryUtil.memAllocShort(length); // allocate a short buffer for the samples (pcm)
        // put the samples into the pcm buffer
        pcm.limit(stb_vorbis_get_samples_short_interleaved(dec, channels, pcm) * channels);
        stb_vorbis_close(dec); // close the decoder
        return pcm; // return the pulse-code modulation buffer
    }

    /**
     * Cleans up a sound
     */
    public void cleanup() {
        this.stop(); // stop the sound if its playing
        alDeleteSources(this.sourceID); // delete the openAL source
        alDeleteBuffers(this.bufferID); // delete the openAL sound buffer
        if (pcm != null) MemoryUtil.memFree(pcm); // free the pulse-code modulation buffer
    }
}
